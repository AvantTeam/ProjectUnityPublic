package unity.annotations.processors.entity;

import arc.func.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.pooling.Pool.*;
import com.squareup.javapoet.*;
import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import mindustry.gen.*;
import mindustry.type.*;
import unity.annotations.Annotations.*;
import unity.annotations.Annotations.Resolve.*;
import unity.annotations.processors.*;
import unity.annotations.processors.util.*;
import unity.annotations.processors.util.TypeIOResolver.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.util.*;

import static javax.lang.model.type.TypeKind.*;

/**
 * @author Anuke
 * @author GlennFolker
 */
@SuppressWarnings("all")
@SupportedAnnotationTypes({
    "unity.annotations.Annotations.EntityComponent",
    "unity.annotations.Annotations.EntityBaseComponent",
    "unity.annotations.Annotations.EntityDef",
    "unity.annotations.Annotations.EntityPoint"
})
public class EntityProcessor extends BaseProcessor{
    Seq<TypeElement> comps = new Seq<>();
    Seq<TypeElement> baseComps = new Seq<>();
    Seq<Element> pointers = new Seq<>();
    Seq<TypeSpec.Builder> baseClasses = new Seq<>();
    ObjectMap<TypeElement, ObjectSet<TypeElement>> baseClassDeps = new ObjectMap<>();
    ObjectMap<TypeElement, Seq<TypeElement>> componentDependencies = new ObjectMap<>();
    ObjectMap<TypeElement, ObjectMap<String, Seq<ExecutableElement>>> inserters = new ObjectMap<>();
    ObjectMap<TypeElement, ObjectMap<String, Seq<ExecutableElement>>> wrappers = new ObjectMap<>();
    Seq<TypeElement> inters = new Seq<>();
    Seq<Element> defs = new Seq<>();
    Seq<EntityDefinition> definitions = new Seq<>();

    StringMap varInitializers = new StringMap();
    StringMap methodBlocks = new StringMap();
    ObjectMap<String, Seq<String>> imports = new ObjectMap<>();
    ObjectMap<TypeElement, String> groups;
    ClassSerializer serializer;

    {
        rounds = 3;
    }

    @Override
    public void process(RoundEnvironment roundEnv) throws Exception{
        comps.addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(EntityComponent.class));
        baseComps.addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(EntityBaseComponent.class));
        inters.addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(EntityInterface.class));
        defs.addAll(roundEnv.getElementsAnnotatedWith(EntityDef.class));
        pointers.addAll(roundEnv.getElementsAnnotatedWith(EntityPoint.class));

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

        for(ExecutableElement e : (Set<ExecutableElement>)roundEnv.getElementsAnnotatedWith(Wrap.class)){
            if(!e.getParameters().isEmpty()) throw new IllegalStateException("All @Wrap methods must not have parameters");
            if(e.getReturnType().getKind() != BOOLEAN) throw new IllegalStateException("All @Wrap methods must have boolean return type");

            TypeElement type = comps.find(c -> simpleName(c).equals(simpleName(e.getEnclosingElement())));
            if(type == null) continue;

            Wrap ann = annotation(e, Wrap.class);
            wrappers
                .get(type, ObjectMap::new)
                .get(ann.value(), Seq::new)
                .add(e);
        }

        if(round == 1){
            serializer = TypeIOResolver.resolve(this);
            groups = ObjectMap.of(
                toComp(Entityc.class), "all",
                toComp(Playerc.class), "player",
                toComp(Bulletc.class), "bullet",
                toComp(Unitc.class), "unit",
                toComp(Buildingc.class), "build",
                toComp(Syncc.class), "sync",
                toComp(Drawc.class), "draw",
                toComp(Firec.class), "fire",
                toComp(Puddlec.class), "puddle"
            );

            for(TypeElement inter : (List<TypeElement>)((PackageElement) elements.getPackageElement("mindustry.gen")).getEnclosedElements()){
                if(
                    simpleName(inter).endsWith("c") &&
                    inter.getKind() == ElementKind.INTERFACE
                ){
                    inters.add(inter);
                }
            }

            for(TypeElement comp : comps){
                for(ExecutableElement m : methods(comp)){
                    if(is(m, Modifier.ABSTRACT, Modifier.NATIVE)) continue;

                    methodBlocks.put(descString(m), procBlock(trees.getTree(m).getBody().toString()));
                }

                for(VariableElement var : vars(comp)){
                    VariableTree tree = (VariableTree) trees.getTree(var);
                    if(tree.getInitializer() != null){
                        varInitializers.put(descString(var), tree.getInitializer().toString());
                    }
                }

                imports.put(interfaceName(comp), getImports(comp));
                Seq<TypeElement> depends = getDependencies(comp);

                EntityComponent compAnno = annotation(comp, EntityComponent.class);
                if(compAnno.write()){
                    TypeSpec.Builder inter = TypeSpec.interfaceBuilder(interfaceName(comp))
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(cName(EntityInterface.class))
                        .addAnnotation(
                            AnnotationSpec.builder(cName(SuppressWarnings.class))
                                .addMember("value", "{$S, $S}", "all", "deprecation")
                            .build()
                        );

                    for(TypeElement extraInterface : Seq.with(comp.getInterfaces()).map(this::toEl).select(i -> !isCompInterface(i))){
                        inter.addSuperinterface(cName(extraInterface));
                    }

                    for(TypeElement type : depends){
                        inter.addSuperinterface(procName(type, this::interfaceName));
                    }

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

                    write(inter.build(), getImports(comp));

                    if(compAnno.base()){
                        Seq<TypeElement> deps = depends.copy().add(comp);
                        baseClassDeps.get(comp, ObjectSet::new).addAll(deps);

                        if(annotation(comp, EntityDef.class) == null){
                            TypeSpec.Builder base = TypeSpec.classBuilder(baseName(comp)).addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

                            for(TypeElement dep : deps){
                                for(VariableElement var : vars(dep).select(v -> !is(v, Modifier.PRIVATE) && !is(v, Modifier.STATIC) && annotation(v, Import.class) == null && annotation(v, ReadOnly.class) == null)){
                                    FieldSpec.Builder field = FieldSpec.builder(tName(var), simpleName(var), Modifier.PUBLIC);

                                    if(is(var, Modifier.TRANSIENT)) field.addModifiers(Modifier.TRANSIENT);
                                    if(is(var, Modifier.VOLATILE)) field.addModifiers(Modifier.VOLATILE);
                                    field.addAnnotations(Seq.with(var.getAnnotationMirrors()).map(AnnotationSpec::get));

                                    if(varInitializers.containsKey(descString(var))){
                                        field.initializer(varInitializers.get(descString(var)));
                                    }

                                    base.addField(field.build());
                                }

                                base.addSuperinterface(procName(dep, this::interfaceName));
                            }

                            baseClasses.add(base);
                        }
                    }
                }else if(compAnno.base()){
                    Seq<TypeElement> deps = depends.copy().add(comp);
                    baseClassDeps.get(comp, ObjectSet::new).addAll(deps);
                }
            }
        }else if(round == 2){
            ObjectMap<String, Element> usedNames = new ObjectMap<>();
            for(Element def : defs){
                EntityDef ann = annotation(def, EntityDef.class);

                Seq<TypeElement> defComps = elements(ann::value)
                    .map(t -> inters.find(i -> simpleName(i).equals(simpleName(t))))
                    .select(Objects::nonNull)
                    .map(this::toComp);

                if(defComps.isEmpty()) continue;

                ObjectMap<String, Seq<ExecutableElement>> methods = new ObjectMap<>();
                ObjectMap<FieldSpec, VariableElement> specVariables = new ObjectMap<>();
                ObjectSet<String> usedFields = new ObjectSet<>();

                Seq<TypeElement> baseClasses = defComps.select(s -> annotation(s, EntityComponent.class).base());
                if(baseClasses.size > 2){
                    throw new IllegalStateException("No entity may have more than 2 base classes.");
                }

                TypeElement baseClassType = baseClasses.any() ? baseClasses.first() : null;
                TypeName baseClass = baseClasses.any()
                ?   procName(baseClassType, this::baseName)
                :   null;

                boolean typeIsBase = baseClassType != null && annotation(def, EntityComponent.class) != null && annotation(def, EntityComponent.class).base();

                if(def instanceof TypeElement && !simpleName(def).endsWith("Comp")){
                    throw new IllegalStateException("All entity def names must end with 'Comp'");
                }

                String name = def instanceof TypeElement ?
                    simpleName(def).replace("Comp", "") :
                    createName(defComps);

                defComps.addAll(defComps.copy().flatMap(this::getDependencies)).distinct();
                Seq<TypeElement> empty = Seq.with();
                Seq<TypeElement> excludeGroups = Seq.with(defComps)
                    .flatMap(t -> annotation(t, ExcludeGroups.class) != null ? elements(annotation(t, ExcludeGroups.class)::value) : empty)
                    .distinct()
                    .map(this::toComp);

                Seq<String> defGroups = groups.values().toSeq().select(val -> {
                    TypeElement type = groups.findKey(val, false);
                    return
                        defComps.contains(type) &&
                        !excludeGroups.contains(type);
                    }
                );

                if(!typeIsBase && baseClass != null && name.equals(baseName(baseClassType))){
                    name += "Entity";
                }

                if(usedNames.containsKey(name)) continue;

                TypeSpec.Builder builder = TypeSpec.classBuilder(name)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(
                        AnnotationSpec.builder(SuppressWarnings.class)
                            .addMember("value", "{$S, $S}", "all", "deprecation")
                        .build()
                    );

                Seq<VariableElement> syncedFields = new Seq<>();
                Seq<VariableElement> allFields = new Seq<>();
                Seq<FieldSpec> allFieldSpecs = new Seq<>();

                boolean isSync = defComps.contains(s -> simpleName(s).contains("Sync"));

                ObjectMap<String, Seq<ExecutableElement>> ins = new ObjectMap<>();
                ObjectMap<String, Seq<ExecutableElement>> wraps = new ObjectMap<>();

                for(TypeElement comp : defComps){
                    ObjectMap<String, Seq<ExecutableElement>> insComp = inserters.get(comp, ObjectMap::new);
                    for(String s : insComp.keys()){
                        ins.get(s, Seq::new).addAll(insComp.get(s));
                    }

                    ObjectMap<String, Seq<ExecutableElement>> wrapComp = wrappers.get(comp, ObjectMap::new);
                    for(String s : wrapComp.keys()){
                        wraps.get(s, Seq::new).addAll(wrapComp.get(s));
                    }

                    boolean isShadowed = baseClass != null && !typeIsBase && baseClassDeps.get(baseClassType, ObjectSet::new).contains(comp);

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

                        boolean isVisible = !is(field, Modifier.STATIC) && !is(field, Modifier.PRIVATE) && annotation(field, ReadOnly.class) == null;

                        if(!isShadowed || !isVisible){
                            builder.addField(spec);
                        }

                        specVariables.put(spec, field);

                        allFieldSpecs.add(spec);
                        allFields.add(field);

                        if(annotation(field, SyncField.class) != null && isSync){
                            if(field.asType().getKind() != FLOAT) throw new IllegalStateException("All SyncFields must be of type float");

                            syncedFields.add(field);
                            builder.addField(FieldSpec.builder(TypeName.FLOAT, simpleName(field) + "_TARGET_").addModifiers(Modifier.TRANSIENT, Modifier.PRIVATE).build());
                            builder.addField(FieldSpec.builder(TypeName.FLOAT, simpleName(field) + "_LAST_").addModifiers(Modifier.TRANSIENT, Modifier.PRIVATE).build());
                        }
                    }

                    for(ExecutableElement elem : methods(comp).select(m -> !isConstructor(m))){
                        methods.get(elem.toString(), Seq::new).add(elem);
                    }
                }

                if(!methods.containsKey("toString()")){
                    builder.addMethod(
                        MethodSpec.methodBuilder("toString")
                            .addAnnotation(cName(Override.class))
                            .returns(String.class)
                            .addModifiers(Modifier.PUBLIC)
                            .addStatement("return $S + $L", name + "#", "id")
                            .build()
                    );
                }

                EntityIO io = new EntityIO(simpleName(def), builder, serializer);
                boolean hasIO = ann.genio() && (defComps.contains(s -> simpleName(s).contains("Sync")) || ann.serialize());

                Seq<ExecutableElement> removal = new Seq<>();
                boolean serializeOverride = false;

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
                    if(entry.value.size > 1 && simpleName(first).equals("serialize") && first.getReturnType().getKind() == BOOLEAN && first.getParameters().size() == 0){
                        serializeOverride = true;
                    }else if(annotation(first, InternalImpl.class) != null){
                        continue;
                    }

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

                    Seq<ExecutableElement> inserts = ins.get(entry.key, Seq::new).select(e -> ext(e, defComps));
                    if(first.getReturnType().getKind() != VOID && !inserts.isEmpty()){
                        throw new IllegalStateException("Method " + entry.key + " is not void, therefore no methods can @Insert to it");
                    }

                    Seq<ExecutableElement> noCompInserts = inserts.select(e -> types.isSameType(
                        elements(annotation(e, Insert.class)::block).first().asType(),
                        toType(Void.class).asType()
                    ));

                    Seq<ExecutableElement> noCompBefore = noCompInserts.select(e -> !annotation(e, Insert.class).after());
                    noCompBefore.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

                    Seq<ExecutableElement> noCompAfter = noCompInserts.select(e -> annotation(e, Insert.class).after());
                    noCompAfter.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

                    inserts = inserts.select(e -> !noCompInserts.contains(e));

                    Seq<ExecutableElement> methodWrappers = wraps.get(entry.key, Seq::new).select(e -> ext(e, defComps));
                    if(first.getReturnType().getKind() != VOID && !methodWrappers.isEmpty()){
                        throw new IllegalStateException("Method " + entry.key + " is not void, therefore no methods can @Wrap it");
                    }

                    Seq<ExecutableElement> noCompWrappers = methodWrappers.select(e -> types.isSameType(
                        elements(annotation(e, Wrap.class)::block).first().asType(),
                        toType(Void.class).asType()
                    ));

                    methodWrappers = methodWrappers.select(e -> !noCompWrappers.contains(e));

                    if(simpleName(first).equals("add") || simpleName(first).equals("remove")){
                        Seq<ExecutableElement> bypass = entry.value.select(m -> annotation(m, BypassGroupCheck.class) != null);
                        entry.value.removeAll(bypass);

                        bypass.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

                        Seq<ExecutableElement> noCompBeforeBypass = noCompBefore.select(m -> annotation(m, BypassGroupCheck.class) != null);
                        if(noCompBeforeBypass.any()){
                            noCompBefore.removeAll(noCompBeforeBypass);
                            for(ExecutableElement e : noCompBeforeBypass){
                                mbuilder.addStatement("this.$L()", simpleName(e));
                            }
                        }

                        boolean firstc = append(mbuilder, defComps, bypass, inserts, methodWrappers, writeBlock);
                        if(!firstc) mbuilder.addCode(lnew());

                        mbuilder.addStatement("if($Ladded) return", simpleName(first).equals("add") ? "" : "!");

                        for(String group : defGroups){
                            mbuilder.addStatement("Groups.$L.$L(this)", group, simpleName(first));
                        }
                        mbuilder.addCode(lnew());
                    }

                    if(!noCompWrappers.isEmpty()){
                        StringBuilder format = new StringBuilder("if(");
                        Seq<Object> args = new Seq<>();

                        for(int i = 0; i < noCompWrappers.size; i++){
                            ExecutableElement e = noCompWrappers.get(i);

                            format.append("this.$L()");
                            args.add(simpleName(e));

                            if(i < noCompWrappers.size - 1) format.append(" && ");
                        }

                        format.append(")");
                        mbuilder.beginControlFlow(format.toString(), args.toArray());
                    }

                    for(ExecutableElement e : noCompBefore){
                        mbuilder.addStatement("this.$L()", simpleName(e));
                    }

                    allFields.sortComparing(BaseProcessor::simpleName);
                    syncedFields.sortComparing(BaseProcessor::simpleName);

                    if(hasIO){
                        if(simpleName(first).equals("read") || simpleName(first).equals("write")){
                            io.write(this, mbuilder, simpleName(first).equals("write"), allFields);
                        }

                        if(simpleName(first).equals("readSync") || simpleName(first).equals("writeSync")){
                            io.writeSync(this, mbuilder, simpleName(first).equals("writeSync"), syncedFields, allFields);
                        }

                        if(simpleName(first).equals("readSyncManual") || simpleName(first).equals("writeSyncManual")){
                            io.writeSyncManual(mbuilder, simpleName(first).equals("writeSyncManual"), syncedFields);
                        }

                        if(simpleName(first).equals("interpolate")){
                            io.writeInterpolate(mbuilder, syncedFields);
                        }

                        if(simpleName(first).equals("snapSync")){
                            mbuilder.addStatement("updateSpacing = 16");
                            mbuilder.addStatement("lastUpdated = $T.millis()", Time.class);
                            for(VariableElement field : syncedFields){
                                mbuilder.addStatement("$L = $L", simpleName(field) + "_LAST_", simpleName(field) + "_TARGET_");
                                mbuilder.addStatement("$L = $L", simpleName(field), simpleName(field) + "_TARGET_");
                            }
                        }

                        if(simpleName(first).equals("snapInterpolation")){
                            mbuilder.addStatement("updateSpacing = 16");
                            mbuilder.addStatement("lastUpdated = $T.millis()", Time.class);
                            for(VariableElement field : syncedFields){
                                mbuilder.addStatement("$L = $L", simpleName(field) + "_LAST_", simpleName(field));
                                mbuilder.addStatement("$L = $L", simpleName(field) + "_TARGET_", simpleName(field));
                            }
                        }
                    }

                    boolean firstc = append(mbuilder, defComps, entry.value, inserts, methodWrappers, writeBlock);

                    if(!firstc && !noCompAfter.isEmpty()) mbuilder.addCode(lnew());
                    for(ExecutableElement e : noCompAfter){
                        mbuilder.addStatement("this.$L()", simpleName(e));
                    }

                    if(simpleName(first).equals("remove") && ann.pooled()){
                        mbuilder.addStatement("$T.queueFree(this)", cName(Groups.class));
                    }

                    if(!noCompWrappers.isEmpty()){
                        mbuilder.endControlFlow();
                    }

                    builder.addMethod(mbuilder.build());
                }

                if(!serializeOverride){
                    builder.addMethod(
                        MethodSpec.methodBuilder("serialize").addModifiers(Modifier.PUBLIC)
                            .addAnnotation(cName(Override.class))
                            .returns(TypeName.BOOLEAN)
                            .addStatement("return " + ann.serialize())
                        .build()
                    );
                }

                if(ann.pooled()){
                    builder.addSuperinterface(Poolable.class);

                    MethodSpec.Builder resetBuilder = MethodSpec.methodBuilder("reset")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(cName(Override.class));

                    for(FieldSpec spec : allFieldSpecs){
                        VariableElement variable = specVariables.get(spec);
                        if(variable == null || is(variable, Modifier.STATIC, Modifier.FINAL)) continue;

                        String desc = descString(variable);

                        if(spec.type.isPrimitive()){
                            resetBuilder.addStatement("$L = $L", spec.name, varInitializers.containsKey(desc) ? varInitializers.get(desc) : getDefault(spec.type.toString()));
                        }else{
                            String init = varInitializers.get(desc);
                            if(init == null || init.equals("null")){
                                resetBuilder.addStatement("$L = null", spec.name);
                            }
                        }
                    }

                    builder.addMethod(resetBuilder.build());
                }

                builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PROTECTED).build());

                builder.addMethod(
                    MethodSpec.methodBuilder("create").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get(packageName, name))
                        .addStatement(ann.pooled() ? "return arc.util.pooling.Pools.obtain($L.class, " + name + "::new)" : "return new $L()", name)
                    .build()
                );

                definitions.add(new EntityDefinition(packageName + "." + name, builder, def, typeIsBase ? null : baseClass, defComps, defGroups, allFieldSpecs));
            }
        }else if(round == 3){
            TypeSpec.Builder map = TypeSpec.classBuilder("UnityEntityMapping").addModifiers(Modifier.PUBLIC)
                .addField(
                    FieldSpec.builder(ParameterizedTypeName.get(
                        cName(ObjectIntMap.class),
                        ParameterizedTypeName.get(
                            cName(Class.class),
                            WildcardTypeName.subtypeOf(cName(Entityc.class))
                        )
                    ), "ids")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", cName(ObjectIntMap.class))
                    .build()
                )
                .addField(
                    FieldSpec.builder(TypeName.INT, "last")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.VOLATILE)
                        .initializer("0")
                    .build()
                )
                .addMethod(
                    MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(TypeName.VOID)
                        .addTypeVariable(tvName("T", cName(Entityc.class)))
                        .addParameter(
                            ParameterizedTypeName.get(cName(Class.class), tvName("T")),
                            "type"
                        )
                        .addParameter(
                            ParameterizedTypeName.get(cName(Prov.class), tvName("T")),
                            "prov"
                        )
                        .beginControlFlow("synchronized($T.class)", ClassName.get(packageName, "UnityEntityMapping"))
                            .addStatement("if(ids.containsKey(type) || $T.nameMap.containsKey(type.getSimpleName())) return", cName(EntityMapping.class))
                            .addCode(lnew())
                            .beginControlFlow("for(; last < $T.idMap.length; last++)", cName(EntityMapping.class))
                                .beginControlFlow("if($T.idMap[last] == null)", cName(EntityMapping.class))
                                    .addStatement("$T.idMap[last] = prov", cName(EntityMapping.class))
                                    .addStatement("ids.put(type, last)")
                                    .addCode(lnew())
                                    .addStatement("$T.nameMap.put(type.getSimpleName(), prov)", cName(EntityMapping.class))
                                    .addStatement("$T.nameMap.put($T.camelToKebab(type.getSimpleName()), prov)", cName(EntityMapping.class), cName(Strings.class))
                                    .addCode(lnew())
                                    .addStatement("break")
                                .endControlFlow()
                            .endControlFlow()
                        .endControlFlow()
                    .build()
                )
                .addMethod(
                    MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(TypeName.VOID)
                        .addTypeVariable(tvName("T", cName(Entityc.class)))
                        .addParameter(cName(String.class), "name")
                        .addParameter(
                            ParameterizedTypeName.get(cName(Class.class), tvName("T")),
                            "type"
                        )
                        .addParameter(
                            ParameterizedTypeName.get(cName(Prov.class), tvName("T")),
                            "prov"
                        )
                        .addStatement("register(type, prov)")
                        .addStatement("$T.nameMap.put(name, prov)", cName(EntityMapping.class))
                        .addCode(lnew())
                        .addStatement("int id = classId(type)")
                        .beginControlFlow("if(id != -1)")
                            .addStatement("$T.customIdMap.put(classId(type), name)", cName(EntityMapping.class))
                        .endControlFlow()
                    .build()
                )
                .addMethod(
                    MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(TypeName.VOID)
                        .addTypeVariable(tvName("T", cName(Unit.class)))
                        .addParameter(cName(UnitType.class), "unit")
                        .addParameter(
                            ParameterizedTypeName.get(cName(Class.class), tvName("T")),
                            "type"
                        )
                        .addParameter(
                            ParameterizedTypeName.get(cName(Prov.class), tvName("T")),
                            "prov"
                        )
                        .addStatement("register(unit.name, type, prov)")
                        .addStatement("unit.constructor = prov")
                    .build()
                )
                .addMethod(
                    MethodSpec.methodBuilder("classId")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addTypeVariable(tvName("T", cName(Entityc.class)))
                        .addParameter(
                            ParameterizedTypeName.get(cName(Class.class), tvName("T")),
                            "type"
                        )
                        .returns(TypeName.INT)
                        .addStatement("return ids.get(type, -1)")
                    .build()
                );

            MethodSpec.Builder init = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID);

            for(EntityDefinition def : definitions){
                ClassName type = ClassName.get(packageName, def.name);

                def.builder.addMethod(
                    MethodSpec.methodBuilder("classId").addModifiers(Modifier.PUBLIC)
                        .addAnnotation(cName(Override.class))
                        .returns(TypeName.INT)
                        .addStatement("return $T.classId($T.class)", ClassName.get(packageName, "UnityEntityMapping"), type)
                    .build()
                );

                if(def.naming instanceof VariableElement){
                    TypeMirror up = def.naming.getEnclosingElement().asType();
                    String c = simpleName(def.naming);
                    init.addStatement("register($T.$L, $T.class, $T::create)", TypeName.get(up), c, type, type);
                }else{
                    init.addStatement("register($T.class, $T::create)", type, type);
                }
            }

            ObjectSet<String> usedNames = new ObjectSet<>();
            for(Element e : pointers){
                EntityPoint point = annotation(e, EntityPoint.class);
                boolean isUnit = e instanceof VariableElement;

                TypeElement type = toEl(isUnit ? elements(point::value).first().asType() : e.asType());
                ExecutableElement create = method(type, "create", type.asType(), Collections.emptyList());
                String constructor = create == null ? "new" : "create";

                if(isUnit){
                    TypeMirror up = e.getEnclosingElement().asType();
                    String c = simpleName(e);
                    init.addStatement("register($T.$L, $T.class, $T::$L)", TypeName.get(up), c, cName(type), cName(type), constructor);

                    usedNames.add(simpleName(type));
                }else if(!usedNames.contains(simpleName(type))){
                    init.addStatement("register($T.class, $T::$L)", cName(type), cName(type), constructor);

                    usedNames.add(simpleName(type));
                }
            }

            write(map
                .addMethod(init.build())
                .build()
            );

            ObjectSet<String> usedCNames = new ObjectSet<>();
            for(EntityDefinition def : definitions){
                if(!usedCNames.add(Reflect.get(TypeSpec.Builder.class, def.builder, "name"))) continue;

                ObjectSet<String> methodNames = def.components.flatMap(type -> methods(type).map(BaseProcessor::simpleString)).asSet();

                if(def.extend != null){
                    def.builder.superclass(def.extend);
                }

                for(TypeElement comp : def.components){
                    TypeElement inter = inters.find(i -> simpleName(i).equals(interfaceName(comp)));
                    if(inter == null){
                        throw new IllegalStateException("Failed to generate interface for " + comp);
                    }

                    def.builder.addSuperinterface(cName(inter));

                    TypeSpec.Builder superclass = null;
                    TypeElement superclassVanilla = null;

                    if(def.extend != null){
                        superclass = baseClasses.find(b -> (packageName + "." + Reflect.get(b, "name")).equals(def.extend.toString()));
                        if(superclass == null){
                            superclassVanilla = elements.getTypeElement(def.extend.toString());
                        }
                    }

                    for(ExecutableElement method : methods(inter)){
                        String var = simpleName(method);
                        FieldSpec field = def.fieldSpecs.find(f -> f.name.equals(var));

                        if(field == null || methodNames.contains(simpleString(method))) continue;

                        MethodSpec result = null;

                        if(method.getReturnType().getKind() != VOID){
                            result = MethodSpec.overriding(method).addStatement("return " + var).build();
                        }

                        if(method.getReturnType().getKind() == VOID && !Seq.with(field.annotations).contains(f -> f.type.toString().equals("@unity.annotations.Annotations.ReadOnly"))){
                            result = MethodSpec.overriding(method).addStatement("this." + var + " = " + var).build();
                        }

                        if(result != null){
                            if(superclass != null){
                                FieldSpec superField = Seq.with(superclass.fieldSpecs).find(f -> f.name.equals(var));

                                if(superField != null){
                                    MethodSpec fr = result;
                                    MethodSpec targetMethod = Seq.with(superclass.methodSpecs).find(m -> m.name.equals(var) && m.returnType.equals(fr.returnType));
                                    if(targetMethod == null){
                                        superclass.addMethod(result);
                                    }

                                    continue;
                                }
                            }else if(superclassVanilla != null){
                                VariableElement superField = vars(superclassVanilla).find(f -> simpleName(f).equals(var));
                                if(superField != null) continue;
                            }

                            def.builder.addMethod(result);
                        }
                    }
                }

                write(def.builder.build(), def.components.flatMap(comp -> imports.get(interfaceName(comp))));
            }

            for(TypeSpec.Builder b : baseClasses){
                TypeSpec spec = b.build();
                write(spec, imports.get(spec.name));
            }
        }
    }

    boolean ext(ExecutableElement e, Seq<TypeElement> defComps){
        Extend ext = annotation(e, Extend.class);
        if(ext == null) return true;

        return defComps.contains(toComp(elements(ext::value).first()));
    }

    boolean append(MethodSpec.Builder mbuilder, Seq<TypeElement> defComps, Seq<ExecutableElement> values, Seq<ExecutableElement> inserts, Seq<ExecutableElement> wrappers, boolean writeBlock){
        boolean firstc = true;
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
                if(!ext(elem, defComps)) continue;

                String descStr = descString(elem);
                String blockName = simpleName(elem.getEnclosingElement()).toLowerCase().replace("comp", "");

                Seq<ExecutableElement> insertComp = inserts.select(e ->
                    simpleName(toComp(elements(annotation(e, Insert.class)::block).first()))
                        .toLowerCase().replace("comp", "")
                        .equals(blockName)
                );

                Seq<ExecutableElement> wrapComp = wrappers.select(e ->
                    simpleName(toComp(elements(annotation(e, Wrap.class)::block).first()))
                        .toLowerCase().replace("comp", "")
                        .equals(blockName)
                );

                if(is(elem, Modifier.ABSTRACT) || is(elem, Modifier.NATIVE) || (!methodBlocks.containsKey(descStr) && insertComp.isEmpty())) continue;

                Seq<ExecutableElement> compBefore = insertComp.select(e -> !annotation(e, Insert.class).after());
                compBefore.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

                Seq<ExecutableElement> compAfter = insertComp.select(e -> annotation(e, Insert.class).after());
                compAfter.sort(Structs.comps(Structs.comparingFloat(m -> annotation(m, MethodPriority.class) != null ? annotation(m, MethodPriority.class).value() : 0), Structs.comparing(BaseProcessor::simpleName)));

                String str = methodBlocks.get(descStr);

                boolean newlined = false;
                if(!wrapComp.isEmpty()){
                    if(!firstc){
                        mbuilder.addCode(lnew());
                        newlined = true;
                    }

                    StringBuilder format = new StringBuilder("if(");
                    Seq<Object> args = new Seq<>();

                    for(int i = 0; i < wrapComp.size; i++){
                        ExecutableElement e = wrapComp.get(i);

                        format.append("this.$L()");
                        args.add(simpleName(e));

                        if(i < wrapComp.size - 1) format.append(" && ");
                    }

                    format.append(")");
                    mbuilder.beginControlFlow(format.toString(), args.toArray());
                }

                if(!firstc && !newlined && !compBefore.isEmpty()) mbuilder.addCode(lnew());
                for(ExecutableElement e : compBefore) mbuilder.addStatement("this.$L()", simpleName(e));

                if(writeBlock){
                    if(annotation(elem, BreakAll.class) == null) str = str.replace("return;", "break " + blockName + ";");

                    if(str
                        .replaceAll("\\s+", "")
                        .replace("\n", "")
                        .isEmpty()
                    ) continue;

                    if(!firstc && !newlined){
                        mbuilder.addCode(lnew());
                        newlined = true;
                    }

                    mbuilder.beginControlFlow("$L:", blockName);
                }

                mbuilder.addCode(str);

                if(writeBlock) mbuilder.endControlFlow();
                for(ExecutableElement e : compAfter) mbuilder.addStatement("this.$L()", simpleName(e));

                if(!wrapComp.isEmpty()) mbuilder.endControlFlow();
                firstc = false;
            }
        }

        return firstc;
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

            if(annotation(component, EntityBaseComponent.class) == null){
                result.addAll(baseComps);
            }

            out.remove(component);
            componentDependencies.put(component, result.toSeq());
        }

        return componentDependencies.get(component);
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

    TypeElement toComp(Class<?> inter){
        return toComp(toType(inter));
    }

    String interfaceName(TypeElement type){
        return baseName(type) + "c";
    }

    String baseName(TypeElement type){
        String name = simpleName(type);
        if(!name.endsWith("Comp")){
            throw new IllegalStateException("All types annotated with @EntityComponent must have 'Comp' as the name's suffix");
        }

        return name.substring(0, name.length() - 4);
    }

    String createName(Seq<TypeElement> comps){
        Seq<TypeElement> rev = comps.copy();
        rev.reverse();

        return rev.toString("", s -> simpleName(s).replace("Comp", ""));
    }

    private static class EntityDefinition{
        final Seq<String> groups;
        final Seq<TypeElement> components;
        final Seq<FieldSpec> fieldSpecs;
        final TypeSpec.Builder builder;
        final Element naming;
        final String name;
        final TypeName extend;

        EntityDefinition(String name, TypeSpec.Builder builder, Element naming, TypeName extend, Seq<TypeElement> components, Seq<String> groups, Seq<FieldSpec> fieldSpec){
            this.builder = builder;
            this.name = name;
            this.naming = naming;
            this.groups = groups;
            this.components = components;
            this.extend = extend;
            this.fieldSpecs = fieldSpec;
        }

        @Override
        public String toString(){
            return
            "EntityDefinition{" +
                "groups=" + groups +
                "components=" + components +
                ", base=" + naming +
            '}';
        }
    }
}
