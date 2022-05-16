package unity.annotations.processors.entity;

import arc.func.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import com.squareup.javapoet.*;
import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import mindustry.gen.*;
import unity.annotations.Annotations.*;
import unity.annotations.Annotations.Resolve.*;
import unity.annotations.processors.*;
import unity.annotations.processors.util.*;
import unity.annotations.processors.util.TypeIOResolver.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.util.*;
import java.util.regex.*;

import static javax.lang.model.type.TypeKind.*;

/** @author sunny
 * Dupes a class, but simply changes its superclass to the one specified. Also omits methods and fields marked with @Ignore. */
@SuppressWarnings("all")
@SupportedAnnotationTypes({
        "unity.annotations.Annotations.Dupe",
        "unity.annotations.Annotations.DupeComponent",
        "unity.annotations.Annotations.DupeInterface"
})
public class DupeProcessor extends BaseProcessor{
    Seq<TypeElement> comps = new Seq<>();
    Seq<TypeElement> inters = new Seq<>();
    Seq<Element> defs = new Seq<>();
    ObjectMap<TypeElement, ObjectMap<String, Seq<ExecutableElement>>> inserters = new ObjectMap<>();
    StringMap varInitializers = new StringMap();
    StringMap methodBlocks = new StringMap();
    ObjectMap<String, Seq<String>> imports = new ObjectMap<>();
    ObjectMap<TypeElement, Seq<TypeElement>> componentDependencies = new ObjectMap<>();
    Seq<DupeDefinition> definitions = new Seq<>();
    ClassSerializer serializer;

    {
        rounds = 2;
    }

    @Override
    public void process(RoundEnvironment roundEnv) throws Exception{
        comps = comps.addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(DupeComponent.class)).flatMap(t -> Seq.with(t).add(types(t)));
        inters = inters.addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(DupeInterface.class)).flatMap(t -> Seq.with(t).add(types(t)));
        defs.addAll(roundEnv.getElementsAnnotatedWith(Dupe.class));

        if(round == 1){
            ObjectMap<String, Element> usedNames = new ObjectMap<>();
            for(Element def : defs){
                Dupe ann = annotation(def, Dupe.class);

                DupeDefinition definition = toClass(def, usedNames, false);
                if(definition == null) continue;

                DupeDefinition buildDefinition = toClass(def, usedNames, true);
                buildDefinition.parent = definition;

                definitions.add(buildDefinition, definition);
            }
        }else if(round == 2){
            for(DupeDefinition def : definitions){
                if(processDefinition(def)){
                    write(def.builder.build(), imports.get(def.baseName));
                }
            }
        }
    }

    DupeDefinition toClass(Element def, ObjectMap<String, Element> usedNames, boolean isBuild){
        ObjectMap<String, Seq<ExecutableElement>> methods = new ObjectMap<>();
        Seq<ExecutableElement> constructors = new Seq<>();
        ObjectMap<FieldSpec, VariableElement> specVariables = new ObjectMap<>();
        ObjectSet<String> usedFields = new ObjectSet<>();

        //boolean isBuild = defComps.contains(t -> t.getEnclosingElement() instanceof TypeElement);

        Seq<TypeElement> naming = Seq.with();
        TypeElement baseClass = elements(annotation(def, Dupe.class)::base).first();
        TypeElement parentClass = elements(annotation(def, Dupe.class)::parent).first();
        /*if(!isBuild){
            naming.add(baseClass, parentClass);
        }else{
            naming.add(findBuild(baseClass), findBuild(parentClass));
        }*/
        naming.add(baseClass, parentClass);

        String customName = annotation(def, Dupe.class).name();
        String name = customName.equals("") ? createName(naming) : customName;
        String rawName = name + "";
        if(isBuild) name += "Build";
        if(usedNames.containsKey(name)) return null;
        usedNames.put(name, def);

        TypeSpec.Builder builder = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC);
        TypeElement comp = isBuild ? findBuild(baseClass) : baseClass;

        Seq<VariableElement> allFields = new Seq<>();
        Seq<FieldSpec> allFieldSpecs = new Seq<>();

        ObjectMap<String, Seq<ExecutableElement>> ins = new ObjectMap<>();

        //find defaults (code from toInterface)
        for(ExecutableElement m : methods(comp).select(t -> !isConstructor(t))){
            if(is(m, Modifier.ABSTRACT, Modifier.NATIVE)) continue;

            //parse @Actually and replace all occurences of baseClass and baseBuild with the new ones
            methodBlocks.put(descString(m) + (isBuild ? "BUILD" : ""), annotation(m, Actually.class) != null ? annotation(m, Actually.class).value() : procBlock(trees.getTree(m).getBody().toString()).replaceAll(
                    "(?<=^|\\W)"+simpleName(baseClass)+"(?=\\W|$)", rawName
            ).replaceAll(
                    "(?<=^|\\W)"+simpleName(baseClass)+"Build(?=\\W|$)", rawName+"Build"
            ));
        }

        for(VariableElement var : vars(comp)){
            VariableTree tree = (VariableTree) trees.getTree(var);
            if(tree.getInitializer() != null){
                varInitializers.put(descString(var), tree.getInitializer().toString());
            }
        }
        if(!isBuild) imports.put(simpleName(baseClass), getImports(comp));

        //actually do something
        Seq<VariableElement> fields = vars(comp).select(v -> annotation(v, Ignore.class) == null);
        for(VariableElement field : fields){
            if(!usedFields.add(simpleName(field))){
                throw new IllegalStateException("Field '" + simpleName(field) + "' of component '" + simpleName(baseClass) + "' redefines a field in entity '" + simpleName(def) + "'");
            }

            //change all fields of type base to type of the generated block
            TypeName ttype = tName(field);
            if(!isBuild && types.isSameType(field.asType(), baseClass.asType())){
                ttype = ClassName.bestGuess(rawName);
            }

            FieldSpec.Builder fbuilder = FieldSpec.builder(ttype, simpleName(field));

            if(varInitializers.containsKey(descString(field))){
                fbuilder.initializer(varInitializers.get(descString(field)));
            }

            if(is(field, Modifier.STATIC)){
                fbuilder.addModifiers(Modifier.STATIC);
            }

            if(is(field, Modifier.FINAL)) fbuilder.addModifiers(Modifier.FINAL);
            if(is(field, Modifier.TRANSIENT)) fbuilder.addModifiers(Modifier.TRANSIENT);
            if(is(field, Modifier.VOLATILE)) fbuilder.addModifiers(Modifier.VOLATILE);

            if(is(field, Modifier.PRIVATE)){
                fbuilder.addModifiers(Modifier.PRIVATE);
            }else{
                fbuilder.addModifiers(is(field, Modifier.PROTECTED) ? Modifier.PROTECTED : Modifier.PUBLIC);
            }

            fbuilder.addAnnotations(Seq.with(field.getAnnotationMirrors()).map(AnnotationSpec::get));
            FieldSpec spec = fbuilder.build();

            builder.addField(spec);

            specVariables.put(spec, field);
            allFieldSpecs.add(spec);
            allFields.add(field);
        }

        for(ExecutableElement elem : methods(comp).select(m -> !isConstructor(m) && annotation(m, Ignore.class) == null)){
            methods.get(elem.toString(), Seq::new).add(elem);
        }

        if(!isBuild){
            constructors.add(constructor(baseClass));
        }
        if(isBuild){
            builder.addMethod(
                    MethodSpec.methodBuilder("toString")
                            .addAnnotation(cName(Override.class))
                            .returns(String.class)
                            .addModifiers(Modifier.PUBLIC)
                            .addStatement("return $S + $L", name + "#", "id")
                            .build()
            );
        }

        if(!isBuild){
            MethodSpec.Builder mbuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(cName(String.class), "name")
                    .addStatement("super(name)");

            boolean writeBlock = constructors.size > 1;

            Seq<ExecutableElement> inserts = ins.get("constructor", Seq::new);

            Seq<ExecutableElement> noComp = inserts.select(e -> isVoid(elements(annotation(e, Insert.class)::block).first()));

            Seq<ExecutableElement> noCompBefore = noComp.select(e -> !annotation(e, Insert.class).after());
            noCompBefore.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

            Seq<ExecutableElement> noCompAfter = noComp.select(e -> annotation(e, Insert.class).after());
            noCompAfter.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

            inserts = inserts.select(e -> !noComp.contains(e));

            for(ExecutableElement e : noCompBefore){
                mbuilder.addStatement("this.$L()", simpleName(e));
            }

            boolean firstc = append(mbuilder, constructors, inserts, writeBlock, false, false);

            if(!firstc && !noCompAfter.isEmpty()) mbuilder.addCode(lnew());
            for(ExecutableElement e : noCompAfter){
                mbuilder.addStatement("this.$L()", simpleName(e));
            }

            builder.addMethod(mbuilder.build());
        }

        EntityIO io = new EntityIO(simpleName(def), builder, serializer);
        Seq<ExecutableElement> removal = new Seq<>();

        //build methods
        for(Entry<String, Seq<ExecutableElement>> entry : methods){
            ExecutableElement first = entry.value.first();

            boolean superCall =
                    !entry.value.contains(m -> annotation(m, Replace.class) != null) &&
                            hasMethod(isBuild ? findBuild(baseClass) : baseClass, first);

            if(annotation(first, InternalImpl.class) != null) continue;

            boolean isPrivate = is(first, Modifier.PRIVATE);
            MethodSpec.Builder mbuilder = MethodSpec.methodBuilder(simpleName(first)).addModifiers(isPrivate ? Modifier.PRIVATE : Modifier.PUBLIC);
            mbuilder.addAnnotations(Seq.with(first.getAnnotationMirrors()).map(AnnotationSpec::get).removeAll(a -> a.type.equals(tName(Actually.class))));

            if(is(first, Modifier.STATIC)) mbuilder.addModifiers(Modifier.STATIC);
            mbuilder.addTypeVariables(Seq.with(first.getTypeParameters()).map(TypeVariableName::get));
            mbuilder.returns(TypeName.get(first.getReturnType()));
            mbuilder.addExceptions(Seq.with(first.getThrownTypes()).map(TypeName::get));

            for(VariableElement var : first.getParameters()){
                mbuilder.addParameter(tName(var), simpleName(var));
            }

            boolean writeBlock = first.getReturnType().getKind() == VOID && entry.value.size > 1;

            if((is(entry.value.first(), Modifier.ABSTRACT) || is(entry.value.first(), Modifier.NATIVE)) && entry.value.size == 1 && annotation(entry.value.first(), InternalImpl.class) == null){
                throw new IllegalStateException(simpleName(entry.value.first().getEnclosingElement()) + "#" + entry.value.first() + " is an abstract method and must be implemented in some component");
            }

            Seq<ExecutableElement> inserts = ins.get(entry.key, Seq::new);
            if(first.getReturnType().getKind() != VOID && !inserts.isEmpty()){
                throw new IllegalStateException("Method " + entry.key + " is not void, therefore no methods can @Insert to it");
            }

            Seq<ExecutableElement> noComp = inserts.select(e -> isVoid(elements(annotation(e, Insert.class)::block).first()));

            Seq<ExecutableElement> noCompBefore = noComp.select(e -> !annotation(e, Insert.class).after());
            noCompBefore.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

            Seq<ExecutableElement> noCompAfter = noComp.select(e -> annotation(e, Insert.class).after());
            noCompAfter.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

            inserts = inserts.select(e -> !noComp.contains(e));

            for(ExecutableElement e : noCompBefore){
                mbuilder.addStatement("this.$L()", simpleName(e));
            }

            boolean firstc = append(mbuilder, entry.value, inserts, writeBlock, superCall, isBuild);

            if(!firstc && !noCompAfter.isEmpty()) mbuilder.addCode(lnew());
            for(ExecutableElement e : noCompAfter){
                mbuilder.addStatement("this.$L()", simpleName(e));
            }

            builder.addMethod(mbuilder.build());
        }

        return new DupeDefinition(packageName + "." + name, builder, def, allFieldSpecs, simpleName(baseClass));
    }

    boolean processDefinition(DupeDefinition def){
        TypeElement block = elements(annotation(def.naming, Dupe.class)::parent).first();
        TypeElement based = elements(annotation(def.naming, Dupe.class)::base).first();
        if(def.parent != null){
            def.builder.superclass(cName(findBuild(block)));
            for(TypeElement extraInterface : Seq.with(findBuild(based).getInterfaces()).map(this::toEl).select(i -> !isCompInterface(i))){
                def.builder.addSuperinterface(cName(extraInterface));
            }
            def.parent.builder.addType(def.builder.build());
        }else{
            def.builder.superclass(tName(block));
            for(TypeElement extraInterface : Seq.with(based.getInterfaces()).map(this::toEl).select(i -> !isCompInterface(i))){
                def.builder.addSuperinterface(cName(extraInterface));
            }
        }

        return def.parent == null;
    }

    ExecutableElement constructor(TypeElement comp){
        ExecutableElement cons = null;
        for(ExecutableElement e : methods(comp).select(BaseProcessor::isConstructor)){
            if(e.getParameters().size() == 1 && e.getParameters().get(0).asType().toString().equals("java.lang.String")){
                cons = e;
            }else{
                throw new IllegalStateException("Invalid constructor: " + e + ". Valid constructor is Comp(java.lang.String)");
            }
        }

        return cons;
    }

    boolean isCompInterface(TypeElement type){
        return toComp(type) != null;
    }

    TypeName procName(TypeElement comp, Func<TypeElement, String> name){
        return ClassName.get(
                comp.getEnclosingElement().toString().contains("fetched") ? "mindustry.gen" : packageName,
                name.get(comp)
        );
    }

    String interfaceName(TypeElement type){
        return baseName(type) + "c";
    }

    String baseName(TypeElement type){
        String name = simpleName(type);

        return name.substring(0, name.length() - 4);
    }

    TypeElement findBuild(TypeElement block){
        TypeElement building = toType(Building.class);
        for(TypeElement type : types(block)){
            if(types.isAssignable(
                    type.asType(),
                    building.asType()
            )){
                return type;
            }
        }
        return building;
    }

    String compName(String interfaceName){
        return interfaceName.substring(0, interfaceName.length() - 1) + "Comp";
    }

    TypeElement toComp(TypeElement inter){
        String name = simpleName(inter);
        if(!name.endsWith("c")) return null;

        return toComp(compName(name));
    }

    TypeElement toComp(String compName){
        return comps.find(t -> simpleName(t).equals(compName));
    }

    Seq<TypeElement> getDependencies(TypeElement component){
        if(!componentDependencies.containsKey(component)){
            ObjectSet<TypeElement> out = new ObjectSet<>();

            Seq<TypeElement> list = Seq.with(component.getInterfaces())
                    .map(i -> toComp(compName(simpleName(toEl(i)))))
                    .select(Objects::nonNull);

            out.addAll(list);
            out.remove(component);

            ObjectSet<TypeElement> result = new ObjectSet<>();
            for(TypeElement type : out){
                result.add(type);
                result.addAll(getDependencies(type));
            }

            out.remove(component);
            componentDependencies.put(component, result.toSeq());
        }

        return componentDependencies.get(component);
    }

    String createName(Seq<TypeElement> comps){
        Seq<TypeElement> rev = comps.copy();
        String p = simpleName(rev.pop());
        if(p.endsWith("Building")) p = p.substring(0, p.length() - 8);
        if(p.endsWith("Build")) p = p.substring(0, p.length() - 5);
        String bef = rev.toString("", s -> {
            String name = simpleName(s);
            if(name.endsWith("Building")) name = name.substring(0, name.length() - 8);
            if(name.endsWith("Build")) name = name.substring(0, name.length() - 5);
            return name.split("(?<=[a-z])([A-Z])", 2)[0];
        });
        return bef + p;
    }

    boolean append(MethodSpec.Builder mbuilder, Seq<ExecutableElement> values, Seq<ExecutableElement> inserts, boolean writeBlock, boolean superCall, boolean isBuild){
        boolean firstc = true;
        boolean superCalled = false;
        for(ExecutableElement elem : values){
            int priority = annotation(elem, MethodPriority.class) == null ? 0 : annotation(elem, MethodPriority.class).value();
            //pre-super() addition
            /*
            if(
                    annotation(elem, Override.class) != null &&
                            elem.getReturnType().getKind() == VOID &&
                            !(
                                    simpleName(elem).equals("write") ||
                                            (simpleName(elem).equals("read") && elem.getParameters().size() == 2)
                            ) &&
                            superCall && !superCalled && priority == 0
            ){
                superCalled = true;

                Seq<ParameterSpec> params = Seq.with(mbuilder.parameters);
                String argLiteral = params.toString(", ", e -> "$L");
                Object[] args = Seq.with(simpleName(elem)).add(params.map(p -> p.name)).toArray(Object.class);

                mbuilder.addStatement("super.$L(" + argLiteral + ")", args);
            }*/

            String descStr = descString(elem) + (isBuild ? "BUILD" : "");
            String blockName = simpleName(elem.getEnclosingElement()).toLowerCase().replace("comp", "");

            Seq<ExecutableElement> insertComp = inserts.select(e ->
                    simpleName(toComp(elements(annotation(e, Insert.class)::block).first()))
                            .toLowerCase().replace("comp", "")
                            .equals(blockName)
            );

            if(is(elem, Modifier.ABSTRACT) || is(elem, Modifier.NATIVE) || (!methodBlocks.containsKey(descStr) && insertComp.isEmpty())) continue;
            firstc = false;

            Seq<ExecutableElement> compBefore = insertComp.select(e -> !annotation(e, Insert.class).after());
            compBefore.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

            Seq<ExecutableElement> compAfter = insertComp.select(e -> annotation(e, Insert.class).after());
            compAfter.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

            String str = methodBlocks.get(descStr);

            boolean newlined = false;
            if(!firstc && !compBefore.isEmpty()){
                mbuilder.addCode(lnew());
                newlined = true;
            }

            for(ExecutableElement e : compBefore){
                mbuilder.addStatement("this.$L()", simpleName(e));
            }

            if(str
                    .replaceAll("\\s+", "")
                    .replace("\n", "")
                    .isEmpty()
            ) continue;

            if(writeBlock){
                if(annotation(elem, BreakAll.class) == null){
                    str = str.replace("return;", "break " + blockName + ";");
                }

                if(!firstc && !newlined){
                    mbuilder.addCode(lnew());
                    newlined = true;
                }

                mbuilder.beginControlFlow("$L:", blockName);
            }

            Seq<Object> arguments = new Seq<>();

            Pattern fixer = Pattern.compile("\"\\$.");
            String fixed = new String(str);

            Matcher matcher = fixer.matcher(str);
            while(matcher.find()){
                String snip = matcher.group();
                fixed = fixed.replace(snip, "$L");
                arguments.add(snip);
            }

            mbuilder.addCode(fixed, arguments.toArray());

            if(writeBlock) mbuilder.endControlFlow();
            for(ExecutableElement e : compAfter) mbuilder.addStatement("this.$L()", simpleName(e));
        }

        //post-super() addition, unnessesary since this is a class-to-class append
        /*ExecutableElement elem = values.first();
        if(
                annotation(elem, Override.class) != null &&
                        elem.getReturnType().getKind() == VOID &&
                        !(
                                simpleName(elem).equals("write") ||
                                        (simpleName(elem).equals("read") && elem.getParameters().size() == 2)
                        ) &&
                        superCall && !superCalled
        ){
            Seq<ParameterSpec> params = Seq.with(mbuilder.parameters);
            String argLiteral = params.toString(", ", e -> "$L");
            Object[] args = Seq.with(simpleName(values.first())).add(params.map(p -> p.name)).toArray(Object.class);

            mbuilder.addStatement("super.$L(" + argLiteral + ")", args);
        }*/

        return firstc;
    }

    private static class DupeDefinition{
        DupeDefinition parent;
        final Seq<FieldSpec> fieldSpecs;
        final TypeSpec.Builder builder;
        final Element naming;
        final String name;
        final String baseName;



        DupeDefinition(String name, TypeSpec.Builder builder, Element naming, Seq<FieldSpec> fieldSpec, String baseName){
            this.builder = builder;
            this.name = name;
            this.naming = naming;
            this.fieldSpecs = fieldSpec;
            this.baseName = baseName;
        }

        @Override
        public String toString(){
            return
                    "DupeDefinition{" +
                            ", base=" + naming +
                            '}';
        }
    }
}
