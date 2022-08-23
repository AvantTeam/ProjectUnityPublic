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

/** @author GlennFolker */
@SuppressWarnings("all")
@SupportedAnnotationTypes({
    "unity.annotations.Annotations.Merge",
    "unity.annotations.Annotations.MergeComponent",
    "unity.annotations.Annotations.MergeInterface"
})
public class MergeProcessor extends BaseProcessor{
    Seq<TypeElement> comps = new Seq<>();
    Seq<TypeElement> inters = new Seq<>();
    Seq<Element> defs = new Seq<>();
    ObjectMap<TypeElement, ObjectMap<String, Seq<ExecutableElement>>> inserters = new ObjectMap<>();
    StringMap varInitializers = new StringMap();
    StringMap methodBlocks = new StringMap();
    ObjectMap<String, Seq<String>> imports = new ObjectMap<>();
    ObjectMap<TypeElement, Seq<TypeElement>> componentDependencies = new ObjectMap<>();
    Seq<MergeDefinition> definitions = new Seq<>();
    ClassSerializer serializer;

    {
        rounds = 3;
    }

    @Override
    public void process(RoundEnvironment roundEnv) throws Exception{
        comps = comps.addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(MergeComponent.class)).flatMap(t -> Seq.with(t).add(types(t)));
        inters = inters.addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(MergeInterface.class)).flatMap(t -> Seq.with(t).add(types(t)));
        defs.addAll(roundEnv.getElementsAnnotatedWith(Merge.class));

        for(ExecutableElement e : (Set<ExecutableElement>)roundEnv.getElementsAnnotatedWith(Insert.class)){
            if(!e.getParameters().isEmpty()) throw new IllegalStateException("All @Insert methods must not have parameters");

            TypeElement type = comps.find(c -> simpleName(c).equals(simpleName(e.getEnclosingElement())));
            if(type == null) continue;

            Insert ann = annotation(e, Insert.class);
            inserters
                .get(type, ObjectMap::new)
                .get(ann.value(), Seq::new)
                .add(e);
        }

        if(round == 1){
            serializer = TypeIOResolver.resolve(this);

            for(TypeElement comp : comps.select(t -> t.getEnclosingElement() instanceof PackageElement)){
                constructor(comp);

                TypeSpec.Builder builder = toInterface(comp, getDependencies(comp))
                    .addAnnotation(
                        AnnotationSpec.builder(cName(SuppressWarnings.class))
                            .addMember("value", "$S", "all")
                        .build()
                    );

                Seq<TypeElement> types = types(comp);
                if(types.size > 1){
                    throw new IllegalStateException("@MergeComponent can't have more than 1 nested class! The nested class must be the building type");
                }

                TypeElement buildType = types.isEmpty() ? null : types.first();
                if(buildType != null){
                    if(!this.types.isAssignable(
                        buildType.asType(),
                        toType(Building.class).asType()
                    )){
                        throw new IllegalStateException("@MergeComponent class' nested class must be the building type");
                    }

                    TypeSpec.Builder subBuilder = toInterface(buildType, getDependencies(buildType));
                    subBuilder.addSuperinterface(cName(Buildingc.class));
                    builder
                        .addType(subBuilder.build())
                        .addAnnotation(cName(MergeInterface.class));
                }

                write(builder.build());
            }
        }else if(round == 2){
            ObjectMap<String, Element> usedNames = new ObjectMap<>();
            for(Element def : defs){
                Merge ann = annotation(def, Merge.class);

                Seq<TypeElement> defComps = elements(ann::value)
                    .map(t -> inters.find(i -> simpleName(i).equals(simpleName(t))))
                    .select(t -> t != null && t.getEnclosingElement() instanceof PackageElement)
                    .map(this::toComp);

                Seq<TypeElement> defCompsBuild = defComps
                    .map(t -> comps.find(i -> simpleName(i).equals(simpleName(findBuild(t)))))
                    .select(Objects::nonNull);

                if(defComps.isEmpty()) continue;

                MergeDefinition definition = toClass(def, usedNames, defComps);
                if(definition == null) continue;

                MergeDefinition buildDefinition = toClass(def, usedNames, defCompsBuild);
                buildDefinition.parent = definition;

                definitions.add(buildDefinition, definition);
            }
        }else if(round == 3){
            for(MergeDefinition def : definitions){
                if(processDefinition(def)){
                    write(def.builder.build(), def.components.flatMap(comp -> imports.get(interfaceName(comp))));
                }
            }
        }
    }

    TypeSpec.Builder toInterface(TypeElement comp, Seq<TypeElement> depends){
        boolean isBuild = comp.getEnclosingElement() instanceof TypeElement;
        TypeSpec.Builder inter = TypeSpec.interfaceBuilder(interfaceName(comp)).addModifiers(Modifier.PUBLIC);

        if(isBuild){
            inter.addModifiers(Modifier.STATIC);
        }

        for(TypeElement extraInterface : Seq.with(comp.getInterfaces()).map(this::toEl).select(i -> !isCompInterface(i))){
            inter.addSuperinterface(cName(extraInterface));
        }

        for(TypeElement type : depends){
            inter.addSuperinterface(procName(type, this::interfaceName));
        }

        if(!isBuild){
            ExecutableElement constructor = constructor(comp);

            String block = procBlock(trees.getTree(constructor).getBody().toString());
            if(block.startsWith("super(")) block = block.substring(block.indexOf("\n") + 1);

            methodBlocks.put(descString(constructor), block);
        }

        for(ExecutableElement m : methods(comp).select(t -> !isConstructor(t))){
            if(is(m, Modifier.ABSTRACT, Modifier.NATIVE)) continue;

            methodBlocks.put(descString(m), procBlock(trees.getTree(m).getBody().toString()));
        }

        for(VariableElement var : vars(comp)){
            VariableTree tree = (VariableTree) trees.getTree(var);
            if(tree.getInitializer() != null){
                varInitializers.put(descString(var), tree.getInitializer().toString());
            }
        }

        if(!isBuild) imports.put(interfaceName(comp), getImports(comp));
        ObjectSet<String> preserved = new ObjectSet<>();

        for(ExecutableElement m : methods(comp).select(me -> !isConstructor(me) && !is(me, Modifier.PRIVATE, Modifier.STATIC))){
            String name = simpleName(m);
            preserved.add(m.toString());

            if(annotation(m, Override.class) == null){
                inter.addMethod(
                    MethodSpec.methodBuilder(name)
                        .addTypeVariables(Seq.with(m.getTypeParameters()).map(TypeVariableName::get))
                        .addExceptions(Seq.with(m.getThrownTypes()).map(TypeName::get))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameters(Seq.with(m.getParameters()).map(ParameterSpec::get))
                        .returns(TypeName.get(m.getReturnType()))
                    .build()
                );
            }
        }

        for(VariableElement var : vars(comp).select(v -> !is(v, Modifier.STATIC) && !is(v, Modifier.PRIVATE) && annotation(v, Import.class) == null)){
            String name = simpleName(var);

            if(!preserved.contains(name + "()")){
                inter.addMethod(
                    MethodSpec.methodBuilder(name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(tName(var))
                    .build()
                );
            }

            if(
                !is(var, Modifier.FINAL) &&
                    !preserved.contains(name + "(" + var.asType().toString() + ")") &&
                    annotation(var, ReadOnly.class) == null
            ){
                inter.addMethod(
                    MethodSpec.methodBuilder(name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(tName(var), name)
                        .returns(TypeName.VOID)
                    .build()
                );
            }
        }

        return inter;
    }

    MergeDefinition toClass(Element def, ObjectMap<String, Element> usedNames, Seq<TypeElement> defComps){
        ObjectMap<String, Seq<ExecutableElement>> methods = new ObjectMap<>();
        Seq<ExecutableElement> constructors = new Seq<>();
        ObjectMap<FieldSpec, VariableElement> specVariables = new ObjectMap<>();
        ObjectSet<String> usedFields = new ObjectSet<>();

        boolean isBuild = defComps.contains(t -> t.getEnclosingElement() instanceof TypeElement);

        Seq<TypeElement> naming = Seq.with(defComps);
        TypeElement baseClass = elements(annotation(def, Merge.class)::base).first();
        if(!isBuild){
            naming.insert(0, baseClass);
        }else{
            naming.insert(0, findBuild(baseClass));
        }

        String name = createName(naming);
        if(isBuild) name += "Build";
        if(usedNames.containsKey(name)) return null;
        usedNames.put(name, def);

        defComps.addAll(defComps.copy().flatMap(this::getDependencies)).distinct();

        TypeSpec.Builder builder = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC);

        Seq<VariableElement> allFields = new Seq<>();
        Seq<FieldSpec> allFieldSpecs = new Seq<>();

        ObjectMap<String, Seq<ExecutableElement>> ins = new ObjectMap<>();

        for(TypeElement comp : defComps){
            ObjectMap<String, Seq<ExecutableElement>> insComp = inserters.get(comp, ObjectMap::new);
            for(String s : insComp.keys()){
                ins.get(s, Seq::new).addAll(insComp.get(s));
            }

            Seq<VariableElement> fields = vars(comp).select(v -> annotation(v, Import.class) == null);
            for(VariableElement field : fields){
                if(!usedFields.add(simpleName(field))){
                    throw new IllegalStateException("Field '" + simpleName(field) + "' of component '" + simpleName(comp) + "' redefines a field in entity '" + simpleName(def) + "'");
                }

                FieldSpec.Builder fbuilder = FieldSpec.builder(tName(field), simpleName(field));

                if(is(field, Modifier.STATIC)){
                    fbuilder.addModifiers(Modifier.STATIC);
                    if(is(field, Modifier.FINAL)) fbuilder.addModifiers(Modifier.FINAL);
                }

                if(is(field, Modifier.TRANSIENT)) fbuilder.addModifiers(Modifier.TRANSIENT);
                if(is(field, Modifier.VOLATILE)) fbuilder.addModifiers(Modifier.VOLATILE);

                if(varInitializers.containsKey(descString(field))){
                    fbuilder.initializer(varInitializers.get(descString(field)));
                }

                if(is(field, Modifier.PRIVATE)){
                    fbuilder.addModifiers(Modifier.PRIVATE);
                }else{
                    fbuilder.addModifiers(annotation(field, ReadOnly.class) != null ? Modifier.PROTECTED : Modifier.PUBLIC);
                }

                fbuilder.addAnnotations(Seq.with(field.getAnnotationMirrors()).map(AnnotationSpec::get));
                FieldSpec spec = fbuilder.build();

                builder.addField(spec);

                specVariables.put(spec, field);
                allFieldSpecs.add(spec);
                allFields.add(field);
            }

            for(ExecutableElement elem : methods(comp).select(m -> !isConstructor(m))){
                methods.get(elem.toString(), Seq::new).add(elem);
            }

            if(!isBuild){
                constructors.add(constructor(comp));
            }
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

            boolean firstc = append(mbuilder, constructors, inserts, writeBlock, false);

            if(!firstc && !noCompAfter.isEmpty()) mbuilder.addCode(lnew());
            for(ExecutableElement e : noCompAfter){
                mbuilder.addStatement("this.$L()", simpleName(e));
            }

            builder.addMethod(mbuilder.build());
        }

        EntityIO io = new EntityIO(simpleName(def), builder, serializer);
        Seq<ExecutableElement> removal = new Seq<>();

        for(Entry<String, Seq<ExecutableElement>> entry : methods){
            boolean hasCombiner = entry.value.contains(m -> annotation(m, Combine.class) != null);
            if(hasCombiner && entry.value.first().getReturnType().getKind() == VOID){
                throw new IllegalStateException("Void method cannot have @Combine.");
            }

            if(hasCombiner && entry.value.count(m -> annotation(m, Combine.class) != null) != 1){
                throw new IllegalStateException("Multiple @Combine methods.");
            }

            if(entry.value.contains(m -> annotation(m, Replace.class) != null)){
                int max = annotation(entry.value.max(m -> annotation(m, Replace.class) == null ? -1 : annotation(m, Replace.class).value()), Replace.class).value();
                if(entry.value.first().getReturnType().getKind() == VOID){
                    entry.value = entry.value.select(m -> annotation(m, Replace.class) != null && annotation(m, Replace.class).value() == max);
                }else{
                    if(entry.value.count(m -> annotation(m, Replace.class) != null && annotation(m, Replace.class).value() == max) != 1){
                        throw new IllegalStateException("Type " + simpleName(def) + " has multiple components replacing non-void method " + entry.key + " with similar priorities. Use `value=<priority>` to bypass this.");
                    }

                    ExecutableElement base = entry.value.max(m -> annotation(m, Replace.class) == null ? -1 : annotation(m, Replace.class).value());
                    entry.value.clear();
                    entry.value.add(base);
                }
            }

            removal.clear();
            for(ExecutableElement elem : entry.value){
                Remove rem = annotation(elem, Remove.class);
                if(rem != null){
                    if(removal.contains(elem)){
                        throw new IllegalStateException(elem + " is already @Remove'd by another method");
                    }

                    ExecutableElement removed = entry.value.find(m -> types.isSameType(
                        m.getEnclosingElement().asType(),
                        elements(rem::value).first().asType()
                    ));

                    if(removed != null) removal.add(removed);
                }
            }
            entry.value.removeAll(removal);

            if(!hasCombiner && entry.value.count(m -> !is(m, Modifier.NATIVE, Modifier.ABSTRACT) && m.getReturnType().getKind() != VOID) > 1){
                throw new IllegalStateException("Type " + simpleName(def) + " has multiple components implementing non-void method " + entry.key + ".");
            }

            entry.value.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

            ExecutableElement first = entry.value.first();

            boolean superCall =
                !entry.value.contains(m -> annotation(m, Replace.class) != null) &&
                hasMethod(isBuild ? findBuild(baseClass) : baseClass, first);

            if(annotation(first, InternalImpl.class) != null) continue;

            boolean isPrivate = is(first, Modifier.PRIVATE);
            MethodSpec.Builder mbuilder = MethodSpec.methodBuilder(simpleName(first)).addModifiers(isPrivate ? Modifier.PRIVATE : Modifier.PUBLIC);
            if(!isPrivate && !is(first, Modifier.STATIC)) mbuilder.addAnnotation(cName(Override.class));

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

            if((simpleName(first).equals("read") && first.getParameters().size() == 2) || simpleName(first).equals("write")){
                Seq<ParameterSpec> params = Seq.with(mbuilder.parameters);
                String argLiteral = params.toString(", ", e -> "$L");
                Object[] args = Seq.with(simpleName(first)).add(params.map(p -> p.name)).toArray(Object.class);

                mbuilder.addStatement("super.$L(" + argLiteral + ")", args);

                io.write(this, mbuilder, simpleName(first).equals("write"), allFields);
            }

            boolean firstc = append(mbuilder, entry.value, inserts, writeBlock, superCall);

            if(!firstc && !noCompAfter.isEmpty()) mbuilder.addCode(lnew());
            for(ExecutableElement e : noCompAfter){
                mbuilder.addStatement("this.$L()", simpleName(e));
            }

            builder.addMethod(mbuilder.build());
        }

        return new MergeDefinition(packageName + "." + name, builder, def, defComps, allFieldSpecs);
    }

    boolean processDefinition(MergeDefinition def){
        ObjectSet<String> methodNames = def.components.flatMap(type -> methods(type).map(BaseProcessor::simpleString)).asSet();

        for(TypeElement comp : def.components){
            TypeElement inter = inters.find(i -> simpleName(i).equals(interfaceName(comp)));
            if(inter == null){
                throw new IllegalStateException("Failed to generate interface for " + simpleName(comp));
            }

            def.builder.addSuperinterface(cName(inter));

            for(ExecutableElement method : methods(inter)){
                String var = simpleName(method);
                FieldSpec field = Seq.with(def.fieldSpecs).find(f -> f.name.equals(var));

                if(field == null || methodNames.contains(simpleString(method))) continue;

                if(method.getReturnType().getKind() != VOID){
                    def.builder.addMethod(
                        MethodSpec.methodBuilder(var).addModifiers(Modifier.PUBLIC)
                            .returns(TypeName.get(method.getReturnType()))
                            .addAnnotation(cName(Override.class))
                            .addStatement("return $L", var)
                        .build()
                    );
                }

                if(method.getReturnType().getKind() == VOID && !Seq.with(field.annotations).contains(f -> f.type.toString().equals("@unity.annotations.Annotations.ReadOnly"))){
                    def.builder.addMethod(
                        MethodSpec.methodBuilder(var).addModifiers(Modifier.PUBLIC)
                            .returns(TypeName.VOID)
                            .addAnnotation(cName(Override.class))
                            .addParameter(field.type, var)
                            .addStatement("this.$L = $L", var, var)
                        .build()
                    );
                }
            }
        }

        TypeElement block = elements(annotation(def.naming, Merge.class)::base).first();
        if(def.parent != null){
            def.builder.superclass(cName(findBuild(block)));
            def.parent.builder.addType(def.builder.build());
        }else{
            def.builder.superclass(tName(block));
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
        if(!name.endsWith("Comp")){
            throw new IllegalStateException("All types annotated with @MergeComponent must have 'Comp' as the name's suffix: '" + name + "'");
        }

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
        rev.reverse();

        return rev.toString("", s -> {
            String name = simpleName(s);
            if(name.endsWith("Comp")) name = name.substring(0, name.length() - 4);
            if(name.endsWith("Building")) name = name.substring(0, name.length() - 8);
            if(name.endsWith("Build")) name = name.substring(0, name.length() - 5);
            return name;
        });
    }

    boolean append(MethodSpec.Builder mbuilder, Seq<ExecutableElement> values, Seq<ExecutableElement> inserts, boolean writeBlock, boolean superCall){
        boolean firstc = true;
        boolean superCalled = false;

        if(values.contains(m -> annotation(m, Combine.class) != null)){
            ExecutableElement base = values.find(m -> annotation(m, Combine.class) != null);

            CodeBlock.Builder cbuilder = CodeBlock.builder();
            for(JCStatement stat : trees.getTree(base).getBody().getStatements()){
                if(stat instanceof JCVariableDecl){
                    JCVariableDecl var = (JCVariableDecl)stat;
                    Seq<? extends AnnotationTree> annos = Seq.with(var.getModifiers().getAnnotations());

                    AnnotationTree anno = annos.find(a -> a.getAnnotationType().toString().equals(Resolve.class.getSimpleName()));
                    if(anno == null){
                        cbuilder.add(stat.toString() + lnew());
                    }else{
                        String type = var.vartype.toString();

                        JCExpression value = (JCExpression)anno.getArguments().get(0);
                        if(value instanceof JCAssign){
                            JCAssign assign = (JCAssign)value;
                            if(assign.lhs.toString().equals("value")) value = assign.rhs;
                        }

                        Name name;
                        if(value instanceof JCFieldAccess){
                            name = ((JCFieldAccess)value).name;
                        }else if(value instanceof JCIdent){
                            name = ((JCIdent)value).name;
                        }else{
                            throw new IllegalArgumentException(value.getClass().getSimpleName());
                        }

                        Method method = Method.valueOf(name.toString());

                        if(values.size == 1){
                            cbuilder.addStatement("$L $L = $L", type, var.name.toString(), var.init.toString());
                        }else{
                            cbuilder.addStatement("$L $L", type, var.name.toString());
                            cbuilder.beginControlFlow(simpleName(base) + "_" + var.name.toString() + "_RESOLVER_:");

                            Seq<String> varNames = new Seq<>();
                            if((isNumeric(type) && !method.bool) || (isBool(type) && method.bool)){
                                for(ExecutableElement elem : values){
                                    if(elem == base) continue;

                                    String blockName = simpleName(elem.getEnclosingElement()).toLowerCase().replace("comp", "");
                                    String varName = blockName + "_" + simpleName(base) + "_";
                                    varNames.add(varName);

                                    List<JCStatement> rstats = trees.getTree(elem).getBody().getStatements();
                                    if(rstats.size() == 1){
                                        cbuilder.addStatement("$L $L = $L", type, varName, ((JCReturn)rstats.get(0)).expr.toString());
                                    }else{
                                        cbuilder.addStatement("$L $L", type, varName);

                                        cbuilder.beginControlFlow("$L:", blockName);
                                        for(JCStatement istate : trees.getTree(elem).getBody().getStatements()){
                                            if(istate instanceof JCReturn){
                                                cbuilder.addStatement("$L = $L", varName, ((JCReturn)istate).expr.toString());
                                            }else{
                                                cbuilder.addStatement(istate.toString());
                                            }
                                        }
                                        cbuilder.endControlFlow();
                                    }
                                }
                            }else{
                                throw new IllegalStateException("Invalid use of @Resolve: " + type + " is incompatible with Method#" + method.name());
                            }

                            method.compute.get(varNames, (format, args) -> cbuilder.addStatement(var.name.toString() + " = " + format, args.toArray()));
                            cbuilder.endControlFlow();
                        }
                    }
                }else{
                    cbuilder.add(stat.toString() + lnew());
                }
            }

            mbuilder.addCode(procBlock("{" + cbuilder.build() + "}"));
        }else{
            for(ExecutableElement elem : values){
                int priority = annotation(elem, MethodPriority.class) == null ? 0 : annotation(elem, MethodPriority.class).value();
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
                }

                String descStr = descString(elem);
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

            ExecutableElement elem = values.first();
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
            }
        }

        return firstc;
    }

    private static class MergeDefinition{
        MergeDefinition parent;
        final Seq<TypeElement> components;
        final Seq<FieldSpec> fieldSpecs;
        final TypeSpec.Builder builder;
        final Element naming;
        final String name;

        MergeDefinition(String name, TypeSpec.Builder builder, Element naming, Seq<TypeElement> components, Seq<FieldSpec> fieldSpec){
            this.builder = builder;
            this.name = name;
            this.naming = naming;
            this.components = components;
            this.fieldSpecs = fieldSpec;
        }

        @Override
        public String toString(){
            return
            "MergeDefinition{" +
                "components=" + components +
                ", base=" + naming +
            '}';
        }
    }
}
