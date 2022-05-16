package unity.annotations.processors;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import com.squareup.javapoet.*;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Attribute.*;
import com.sun.tools.javac.model.*;
import com.sun.tools.javac.processing.*;
import mindustry.*;
import unity.annotations.Annotations.AnnotationProxyMaker;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.*;
import java.io.*;
import java.lang.Class;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import static javax.lang.model.type.TypeKind.*;

/**
 * @author Anuke
 * @author GlennFolker
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class BaseProcessor extends AbstractProcessor{
    public static final String packageName = "unity.gen";

    public JavacElements elements;
    public JavacTrees trees;
    public JavacTypes types;
    public JavacFiler filer;
    public static Fi rootDir;

    protected int round;
    protected int rounds = 1;

    static{
        Vars.loadLogger();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv){
        super.init(processingEnv);

        JavacProcessingEnvironment javacProcessingEnv = (JavacProcessingEnvironment)processingEnv;

        elements = javacProcessingEnv.getElementUtils();
        trees = JavacTrees.instance(javacProcessingEnv);
        types = javacProcessingEnv.getTypeUtils();
        filer = javacProcessingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        if(round++ >= rounds) return false;
        if(rootDir == null){
            try{
                String path = Fi.get(filer.getResource(StandardLocation.CLASS_OUTPUT, "no", "no")
                    .toUri().toURL().toString().substring(OS.isWindows ? 6 : "file:".length()))
                    .parent().parent().parent().parent().parent().parent().parent().toString().replace("%20", " ");

                rootDir = Fi.get(path);
            }catch(IOException e){
                Throwable finalCause = Strings.getFinalCause(e);

                Log.err(finalCause);
                throw new RuntimeException(finalCause);
            }
        }

        try{
            process(roundEnv);
        }catch(Exception e){
            Throwable finalCause = Strings.getFinalCause(e);

            Log.err(finalCause);
            throw new RuntimeException(finalCause);
        }

        return true;
    }

    public abstract void process(RoundEnvironment roundEnv) throws Exception;

    public void write(TypeSpec spec) throws Exception{
        write(spec, null);
    }

    public void write(TypeSpec spec, Seq<String> imports) throws Exception{
        try{
            JavaFile file = JavaFile.builder(packageName, spec)
                .indent("    ")
                .skipJavaLangImports(true)
                .build();

            if(imports == null || imports.isEmpty()){
                file.writeTo(filer);
            }else{
                imports.distinct();

                Seq<String> statics = imports.select(i -> i.contains("import static ")).sort();
                imports = imports.select(i -> !statics.contains(s -> s.equals(i))).sort();
                if(!statics.isEmpty()){
                    imports = statics.addAll("\n").add(imports);
                }

                String rawSource = file.toString();
                Seq<String> source = Seq.with(rawSource.split("\n", -1));
                Seq<String> result = new Seq<>();
                for(int i = 0; i < source.size; i++){
                    String s = source.get(i);

                    result.add(s);
                    if(s.startsWith("package ")){
                        source.remove(i + 1);
                        result.add("");
                        for(String im : imports){
                            result.add(im.replace("\n", ""));
                        }
                    }
                }

                String out = result.toString("\n");
                JavaFileObject object = filer.createSourceFile(file.packageName + "." + file.typeSpec.name, file.typeSpec.originatingElements.toArray(new Element[0]));
                OutputStream stream = object.openOutputStream();
                stream.write(out.getBytes());
                stream.close();
            }
        }catch(FilerException e){
            throw new Exception("Misbehaving files prevent annotation processing from being done. Try running `gradlew clean`");
        }
    }

    public TypeElement toEl(TypeMirror t){
        return (TypeElement)types.asElement(t);
    }

    public Seq<TypeElement> elements(Runnable run){
        try{
            run.run();
        }catch(MirroredTypesException ex){
            return Seq.with(ex.getTypeMirrors()).map(this::toEl);
        }

        return Seq.with();
    }

    public static TypeName tName(Class<?> type){
        return ClassName.get(type).box();
    }

    public static TypeName tName(Element e){
        return e == null ? TypeName.VOID : TypeName.get(e.asType());
    }

    public static ClassName cName(Class<?> type){
        return ClassName.get(type);
    }

    public static ClassName cName(String canonical){
        canonical = canonical.replace("<any?>", "unity.gen");

        Matcher matcher = Pattern.compile("\\.[A-Z]").matcher(canonical);
        boolean find = matcher.find();
        int offset = find ? matcher.start() : 0;

        String pkgName = canonical.substring(0, offset);
        Seq<String> simpleNames = Seq.with(canonical.substring(offset + 1).split("\\."));
        simpleNames.reverse();
        String simpleName = simpleNames.pop();
        simpleNames.reverse();

        return ClassName.get(pkgName.isEmpty() ? packageName : pkgName, simpleName, simpleNames.toArray());
    }

    public static ClassName cName(Element e){
        return cName(stripTV(e.asType().toString()));
    }

    public static TypeVariableName tvName(String name, TypeName... bounds){
        return TypeVariableName.get(name, bounds);
    }

    public static String stripTV(String canonical){
        return canonical.replaceAll("<[A-Z]+>", "");
    }

    public static String lnew(){
        return Character.toString('\n');
    }

    public Seq<VariableElement> vars(TypeElement t){
        return Seq.with(t.getEnclosedElements()).select(e -> e instanceof VariableElement).map(e -> (VariableElement)e);
    }

    public Seq<ExecutableElement> methods(TypeElement t){
        return Seq.with(t.getEnclosedElements()).select(e -> e instanceof ExecutableElement).map(e -> (ExecutableElement)e);
    }

    public Seq<TypeElement> types(TypeElement t){
        return Seq.with(t.getEnclosedElements()).select(e -> e instanceof TypeElement).map(e -> (TypeElement)e);
    }

    public String descString(VariableElement v){
        return v.getEnclosingElement().toString() + "#" + v.getSimpleName().toString();
    }

    public String descString(ExecutableElement m){
        String params = Arrays.toString(m.getParameters().toArray());
        params = params.substring(1, params.length() - 1);

        return m.getEnclosingElement().toString() + "#" + simpleName(m) + "(" + params + ")";
    }

    public boolean is(Element e, Modifier... modifiers){
        for(Modifier m : modifiers){
            if(e.getModifiers().contains(m)){
                return true;
            }
        }
        return false;
    }

    public static boolean isConstructor(ExecutableElement e){
        return
            simpleName(e).equals("<init>") ||
                simpleName(e).equals("<clinit>");
    }

    public boolean hasMethod(TypeElement type, ExecutableElement method){
        for(; !(type.getSuperclass() instanceof NoType); type = toEl(type.getSuperclass())){
            if(method(type, simpleName(method), method.getReturnType(), method.getParameters()) != null){
                return true;
            }
        }
        return false;
    }

    public ExecutableElement method(TypeElement type, String name, TypeMirror retType, List<? extends VariableElement> params){
        return methods(type).find(m -> {
            List<? extends VariableElement> realParams = m.getParameters();

            return
                simpleName(m).equals(name) &&
                    (retType == null || types.isSameType(m.getReturnType(), retType)) &&
                    paramEquals(realParams, params);
        });
    }

    public boolean paramEquals(List<? extends VariableElement> first, List<? extends VariableElement> second){
        if(first.size() != second.size()) return false;

        boolean same = true;
        for(int i = 0; same && i < first.size(); i++){
            VariableElement a = first.get(i);
            VariableElement b = second.get(i);

            if(!types.isSameType(a.asType(), b.asType())) same = false;
        }

        return same;
    }

    public Seq<String> getImports(Element e){
        return Seq.with(trees.getPath(e).getCompilationUnit().getImports()).map(Object::toString);
    }

    public static String getDefault(String value){
        switch(value){
            case "float":
            case "double":
            case "int":
            case "long":
            case "short":
            case "char":
            case "byte":
                return "0";
            case "boolean":
                return "false";
            default:
                return "null";
        }
    }

    public boolean instanceOf(String type, String other){
        TypeElement a = elements.getTypeElement(type);
        TypeElement b = elements.getTypeElement(other);
        return a != null && b != null && types.isSubtype(a.asType(), b.asType());
    }

    public static boolean isPrimitive(String type){
        return type.equals("boolean") || type.equals("byte") || type.equals("short") || type.equals("int")
            || type.equals("long") || type.equals("float") || type.equals("double") || type.equals("char");
    }

    public boolean isNumeric(TypeMirror type){
        try{
            switch(types.unboxedType(type).getKind()){
                case BYTE:
                case SHORT:
                case INT:
                case FLOAT:
                case LONG:
                case DOUBLE: return true;
                default: return false;
            }
        }catch(IllegalArgumentException t){
            return false;
        }
    }

    public boolean isNumeric(String type){
        return type.equals("byte") || type.equals("short") || type.equals("int") || type.equals("float")
            || type.equals("long") || type.equals("double") || type.equals("Byte") || type.equals("Short")
            || type.equals("Integer") || type.equals("Float") || type.equals("Long") || type.equals("Double");
    }

    public boolean isBool(TypeMirror type){
        try{
            return types.unboxedType(type).getKind() == BOOLEAN;
        }catch(IllegalArgumentException t){
            return false;
        }
    }

    public boolean isBool(String type){
        return type.equals("boolean") || type.equals("Boolean");
    }

    public static <A extends Annotation> A annotation(Element e, Class<A> annotation){
        try{
            Method m = AnnoConstruct.class.getDeclaredMethod("getAttribute", Class.class);
            m.setAccessible(true);
            Compound compound = (Compound)m.invoke(e, annotation);
            return compound == null ? null : AnnotationProxyMaker.generateAnnotation(compound, annotation);
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    public TypeElement toType(Class<?> type){
        return elements.getTypeElement(type.getCanonicalName());
    }

    public static String fullName(Element e){
        return e.asType().toString();
    }

    public static String simpleName(Element e){
        return simpleName(e.getSimpleName().toString());
    }

    public static String simpleName(String canonical){
        if(canonical.contains(".")){
            canonical = canonical.substring(canonical.lastIndexOf(".") + 1);
        }
        return canonical;
    }

    public static String simpleString(ExecutableElement e){
        return simpleName(e) + "(" + Seq.with(e.getParameters()).toString(", ", p -> simpleName(p.asType().toString())) + ")";
    }

    public static String procBlock(String methodBlock){
        StringBuilder builder = new StringBuilder();
        String[] lines = methodBlock.split("\n");

        for(String line : lines){
            if(line.startsWith("    ")) line = line.substring(4);

            line = line
                .replaceAll("this\\.<(.*)>self\\(\\)", "this")
                .replaceAll("self\\(\\)(?!\\s+instanceof)", "this")
                .replaceAll(" yield ", "")
                .replaceAll("/\\*missing\\*/", "var");

            builder.append(line).append('\n');
        }

        String result = builder.toString();
        return result.substring(result.indexOf("{") + 1, result.lastIndexOf("}")).trim() + "\n";
    }

    public boolean isVoid(TypeElement e){
        return types.isSameType(e.asType(), toType(Void.class).asType());
    }

    public static TypeMirror compOf(TypeMirror type){
        while(type instanceof ArrayType){
            type = ((ArrayType)type).getComponentType();
        }

        return type;
    }

    @Override
    public SourceVersion getSupportedSourceVersion(){
        return SourceVersion.RELEASE_8;
    }
}
