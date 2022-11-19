package unity.annotations.processors;

import arc.struct.*;
import arc.util.Log;
import arc.util.*;
import com.squareup.javapoet.*;
import com.sun.source.tree.*;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.model.*;
import com.sun.tools.javac.processing.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import mindustry.*;
import unity.annotations.Annotations.AnnotationProxyMaker;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.Diagnostic.*;
import javax.tools.*;
import java.io.*;
import java.lang.annotation.*;
import java.util.List;
import java.util.*;
import java.util.regex.*;

/**
 * @author Anuke
 * @author GlennFolker
 */
@SuppressWarnings("unchecked")
public abstract class BaseProcessor implements Processor{
    public static final String packageRoot = "unity.gen";
    public static final Pattern genStrip = Pattern.compile("unity\\.gen\\.[^A-Z]*");

    private static final ObjectMap<Element, ObjectMap<Class<? extends Annotation>, Annotation>> annotations = new ObjectMap<>();

    public Filer filer;
    public Messager messager;

    public JavacElements elements;
    public JavacTypes types;
    public JavacTrees trees;
    public TreeMaker maker;

    public JavacProcessingEnvironment procEnv;
    public JavacRoundEnvironment roundEnv;

    protected int round = 0, rounds = 1;
    protected long initTime;

    static{
        Vars.loadLogger();
    }

    @Override
    public synchronized void init(ProcessingEnvironment env){
        procEnv = (JavacProcessingEnvironment)env;
        Context context = procEnv.getContext();

        filer = procEnv.getFiler();
        messager = procEnv.getMessager();
        elements = procEnv.getElementUtils();
        types = procEnv.getTypeUtils();
        trees = JavacTrees.instance(context);
        maker = TreeMaker.instance(context);

        initTime = Time.millis();
        Log.info("@ started.", getClass().getSimpleName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        this.roundEnv = (JavacRoundEnvironment)roundEnv;
        if(roundEnv.processingOver()) Log.info("Time taken for @: @s", getClass().getSimpleName(), (Time.millis() - initTime) / 1000f);

        if(round++ >= rounds) return true;

        try{
            process();
        }catch(Exception e){
            Log.err(e);
            throw new RuntimeException(e);
        }

        return true;
    }

    protected void process() throws Exception{}

    protected void write(String packageName, TypeSpec.Builder builder, Seq<String> imports) throws Exception{
        builder.superinterfaces.sort(Structs.comparing(TypeName::toString));
        builder.methodSpecs.sort(Structs.comparing(MethodSpec::toString));
        builder.fieldSpecs.sort(Structs.comparing(f -> f.name));

        JavaFile file = JavaFile.builder(packageName, builder.build())
        .indent("    ")
        .skipJavaLangImports(true)
        .build();

        if(imports == null || imports.isEmpty()){
            file.writeTo(filer);
        }else{
            imports = imports.map(m -> Seq.with(m.split("\n")).sort().toString("\n"));
            imports.sort().distinct();

            String rawSource = file.toString();
            Seq<String> result = new Seq<>();
            for(String s : rawSource.split("\n", -1)){
                result.add(s);
                if(s.startsWith("package ")){
                    result.add("");
                    for(String i : imports) result.add(i);
                }
            }

            JavaFileObject object = filer.createSourceFile(file.packageName + "." + file.typeSpec.name, file.typeSpec.originatingElements.toArray(new Element[0]));
            Writer stream = object.openWriter();
            stream.write(result.toString("\n"));
            stream.close();
        }
    }

    public RuntimeException err(String message){
        messager.printMessage(Kind.ERROR, message);
        return new IllegalArgumentException(message);
    }

    public RuntimeException err(String message, Element elem){
        messager.printMessage(Kind.ERROR, message, elem);
        return new IllegalArgumentException(message);
    }

    public Seq<String> imports(Element e){
        Seq<String> out = new Seq<>();
        for(ImportTree t : trees.getPath(e).getCompilationUnit().getImports()) out.add(t.toString());
        return out;
    }

    public <T extends Symbol> Set<T> with(Class<? extends Annotation> annotation){
        return (Set<T>)roundEnv.getElementsAnnotatedWith(annotation);
    }

    public boolean instanceOf(String type, String other){
        ClassSymbol a = elements.getTypeElement(type);
        ClassSymbol b = elements.getTypeElement(other);
        return a != null && b != null && types.isSubtype(a.type, b.type);
    }

    public boolean same(TypeMirror a, TypeMirror b){
        return types.isSameType(a, b);
    }

    public boolean same(TypeMirror a, TypeElement b){
        return same(a, b.asType());
    }

    public boolean same(TypeElement a, TypeMirror b){
        return same(a.asType(), b);
    }

    public boolean same(TypeElement a, TypeElement b){
        return same(a.asType(), b.asType());
    }

    public ClassSymbol conv(Class<?> type){
        return elements.getTypeElement(fName(type));
    }

    public <T extends TypeSymbol> T conv(TypeMirror type){
        return (T)types.asElement(type);
    }

    public static boolean isPrimitive(String type){
        return switch(type){
            case "boolean", "byte", "short", "int", "long", "float", "double", "char" -> true;
            default -> false;
        };
    }

    public static String getDefault(String value){
        return switch(value){
            case "float", "double", "int", "long", "short", "char", "byte" -> "0";
            case "boolean" -> "false";
            default -> "null";
        };
    }

    public static boolean is(Element e, Modifier... modifiers){
        Set<Modifier> set = e.getModifiers();
        for(Modifier modifier : modifiers) if(!set.contains(modifier)) return false;

        return true;
    }

    public static boolean isAny(Element e, Modifier... modifiers){
        Set<Modifier> set = e.getModifiers();
        for(Modifier modifier : modifiers) if(set.contains(modifier)) return true;

        return false;
    }

    public static <T extends Annotation> T anno(Element e, Class<T> type){
        if(annotations.containsKey(e)){
            return (T)annotations.get(e).get(type, () -> createAnno(e, type));
        }else{
            ObjectMap<Class<? extends Annotation>, Annotation> map;
            annotations.put(e, map = new ObjectMap<>());

            T anno;
            map.put(type, anno = createAnno(e, type));

            return anno;
        }
    }

    private static <T extends Annotation> T createAnno(Element e, Class<T> type){
        return AnnotationProxyMaker.generateAnnotation(Reflect.invoke(
        AnnoConstruct.class, e, "getAttribute",
        new Object[]{type}, Class.class
        ), type);
    }

    public static ClassName spec(Class<?> type){
        return ClassName.get(type);
    }

    public static ClassName spec(TypeElement type){
        return ClassName.get(type);
    }

    public static TypeName spec(TypeMirror type){
        return TypeName.get(type);
    }

    public static TypeVariableName spec(TypeVariable type){
        return TypeVariableName.get(type);
    }

    public static TypeVariableName spec(TypeParameterElement type){
        return TypeVariableName.get(type);
    }

    public static ParameterSpec spec(VariableElement var){
        return ParameterSpec.get(var);
    }

    public static AnnotationSpec spec(AnnotationMirror anno){
        return AnnotationSpec.get(anno);
    }

    public static ParameterizedTypeName paramSpec(ClassName type, TypeName... types){
        return ParameterizedTypeName.get(type, types);
    }

    public static WildcardTypeName subSpec(TypeName type){
        return WildcardTypeName.subtypeOf(type);
    }

    public static WildcardTypeName superSpec(TypeName type){
        return WildcardTypeName.supertypeOf(type);
    }

    public static TypeVariableName tvSpec(String name){
        return TypeVariableName.get(name);
    }

    public static String fName(Class<?> type){
        return type.getCanonicalName();
    }

    public static String fName(TypeElement e){
        return e.getQualifiedName().toString();
    }

    public static String name(Class<?> type){
        return type.getSimpleName();
    }

    public static String name(Element e){
        return e.getSimpleName().toString();
    }

    public static String name(String canonical){
        return canonical.contains(".") ? canonical.substring(canonical.lastIndexOf('.') + 1) : canonical;
    }

    public Seq<ClassSymbol> types(Runnable run){
        try{
            run.run();
        }catch(MirroredTypesException e){
            return Seq.with(e.getTypeMirrors()).map(this::conv);
        }

        throw new IllegalArgumentException("types() is used for getting annotation values of class types.");
    }

    public ClassSymbol type(Runnable run){
        try{
            run.run();
        }catch(MirroredTypeException e){
            return conv(e.getTypeMirror());
        }

        throw new IllegalArgumentException("type() is used for getting annotation values of a class type.");
    }

    public String desc(Element e){
        return switch(e.getKind()){
            case FIELD, LOCAL_VARIABLE -> desc((VariableElement)e);
            case METHOD -> desc((ExecutableElement)e);
            default -> throw err("desc() only accepts variable and method elements.", e);
        };
    }

    public String desc(VariableElement e){
        return e.getEnclosingElement().toString() + "#" + name(e);
    }

    public String desc(ExecutableElement e){
        return e.getEnclosingElement().toString() + "#" + sigName(e);
    }

    public String sigName(ExecutableElement e){
        List<? extends VariableElement> params = e.getParameters();
        if(params.size() == 0) return name(e) + "()";

        StringBuilder builder = new StringBuilder(name(e))
            .append("(")
            .append(params.get(0).asType());

        for(int i = 1; i < params.size(); i++) builder.append(", ").append(params.get(i).asType());
        return builder.append(")").toString();
    }

    public static String fixName(String canonical){
        Matcher matcher = genStrip.matcher(canonical);
        if(matcher.find() && matcher.start() == 0){
            return canonical.substring(matcher.end());
        }else{
            return canonical;
        }
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText){
        return Collections.emptyList();
    }

    @Override
    public SourceVersion getSupportedSourceVersion(){
        return SourceVersion.RELEASE_17;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes(){
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedOptions(){
        return Collections.emptySet();
    }
}
