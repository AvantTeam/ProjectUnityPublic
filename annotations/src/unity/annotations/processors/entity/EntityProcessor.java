package unity.annotations.processors.entity;

import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import com.squareup.javapoet.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Attribute.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import mindustry.gen.*;
import unity.annotations.Annotations.*;
import unity.annotations.processors.*;
import unity.annotations.processors.util.*;
import unity.annotations.processors.util.TypeIOResolver.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.element.Modifier;
import java.io.*;
import java.lang.*;
import java.lang.Class;
import java.util.*;

import static javax.lang.model.element.Modifier.*;
import static javax.lang.model.type.TypeKind.*;
import static javax.lang.model.element.ElementKind.*;

/**
 * @author Anuke
 * @author GlennFolker
 */
public class EntityProcessor extends BaseProcessor{
    public static final String packageName = packageRoot + ".entities";
    private static final Seq<MethodSymbol> tmpMethods = new Seq<>();
    private static final Seq<Object> tmpArgs = new Seq<>();
    private static final Comparator<MethodSymbol> methodSorter = Structs.comps(Structs.comparingInt(m -> {
        MethodPriority priority = anno(m, MethodPriority.class);
        return priority == null ? 0 : priority.value();
    }), Structs.comparing(BaseProcessor::name));

    protected Fi revDir;

    protected OrderedMap<String, ClassSymbol> comps = new OrderedMap<>();
    protected OrderedMap<String, ClassSymbol> inters = new OrderedMap<>();
    protected Seq<ClassSymbol> baseComps = new Seq<>();
    protected OrderedMap<String, TypeSpec.Builder> baseClasses = new OrderedMap<>();
    protected ObjectMap<String, ClassSymbol> baseClassTypes = new ObjectMap<>();
    protected ObjectMap<ClassSymbol, String> groups = new ObjectMap<>();
    protected Seq<Symbol> defs = new Seq<>();

    protected ObjectMap<ClassSymbol, OrderedMap<String, Seq<MethodSymbol>>> inserters = new OrderedMap<>();
    protected ObjectMap<ClassSymbol, OrderedMap<String, Seq<MethodSymbol>>> wrappers = new OrderedMap<>();
    protected Seq<ClassSymbol> pointers = new Seq<>();

    protected ObjectMap<String, JCExpression> varInitializers = new ObjectMap<>();
    protected ObjectMap<String, JCBlock> methodBlocks = new ObjectMap<>();
    protected ObjectMap<ClassSymbol, Seq<String>> imports = new ObjectMap<>();
    protected ObjectMap<ClassSymbol, Seq<ClassSymbol>> dependencies = new ObjectMap<>();
    protected ObjectMap<ClassSymbol, ObjectSet<ClassSymbol>> baseDependencies = new ObjectMap<>();

    protected Seq<EntityDefinition> definitions = new Seq<>();

    protected ClassSerializer serializer;

    {
        rounds = 2;

        comps.orderedKeys().ordered = false;
        inters.orderedKeys().ordered = false;
        baseClasses.orderedKeys().ordered = false;
        baseComps.ordered = false;
        defs.ordered = false;
        pointers.ordered = false;
    }

    @Override
    public synchronized void init(ProcessingEnvironment env){
        super.init(env);
        revDir = Fi.get(env.getOptions().get("revisionDirectory"));
    }

    @Override
    protected void process() throws Exception{
        switch(round){
            case 1 -> {
                for(ClassSymbol t : this.<ClassSymbol>with(EntityComponent.class)) comps.put(name(t), t);
                for(ClassSymbol t : this.<ClassSymbol>with(EntityBaseComponent.class)) baseComps.add(t);
                for(Symbol s : with(EntityDef.class)) defs.add(s);
                for(ClassSymbol s : this.<ClassSymbol>with(EntityPoint.class)) pointers.add(s);

                for(MethodSymbol e : this.<MethodSymbol>with(Insert.class)){
                    if(!e.params.isEmpty()) throw err("All @Insert methods must not have parameters");

                    ClassSymbol type = comps.get(name(e.enclClass()));
                    if(type == null) continue;

                    inserters.get(type, () -> {
                        OrderedMap<String, Seq<MethodSymbol>> map = new OrderedMap<>();
                        map.orderedKeys().ordered = false;
                        return map;
                    }).get(anno(e, Insert.class).value(), () -> new Seq<>(false)).add(e);
                }

                for(MethodSymbol e : this.<MethodSymbol>with(Wrap.class)){
                    if(!e.params.isEmpty()) throw err("All @Wrap methods must not have parameters");
                    if(e.getReturnType().getKind() != BOOLEAN) throw err("All @Wrap methods must have boolean return type");

                    ClassSymbol type = comps.get(name(e.enclClass()));
                    if(type == null) continue;

                    wrappers.get(type, () -> {
                        OrderedMap<String, Seq<MethodSymbol>> map = new OrderedMap<>();
                        map.orderedKeys().ordered = false;
                        return map;
                    }).get(anno(e, Wrap.class).value(), () -> new Seq<>(false)).add(e);
                }

                serializer = TypeIOResolver.resolve(this);
                groups.putAll(
                    comp(Entityc.class), "all",
                    comp(Playerc.class), "player",
                    comp(Bulletc.class), "bullet",
                    comp(Unitc.class), "unit",
                    comp(Buildingc.class), "build",
                    comp(Syncc.class), "sync",
                    comp(Drawc.class), "draw",
                    comp(Firec.class), "fire",
                    comp(Puddlec.class), "puddle"
                );

                for(Symbol s : elements.getPackageElement("mindustry.gen").getEnclosedElements()){
                    String name = name(s);
                    if(name.endsWith("c") && s.getKind() == INTERFACE && comp(compName(name)) != null){
                        inters.put(name, (ClassSymbol)s);
                    }
                }

                for(ClassSymbol comp : comps.values()){
                    for(Symbol s : comp.getEnclosedElements()){
                        if(s.getKind() == FIELD){
                            JCVariableDecl tree = (JCVariableDecl)trees.getTree(s);
                            if(tree == null) continue;

                            JCExpression init = tree.init;
                            if(init != null) varInitializers.put(desc(s), init);
                        }else if(s.getKind() == METHOD && s.getKind() != CONSTRUCTOR){
                            MethodSymbol m = (MethodSymbol)s;
                            if(isAny(m, ABSTRACT, NATIVE)) continue;

                            JCMethodDecl tree = trees.getTree(m);
                            if(tree == null) continue;

                            methodBlocks.put(desc(m), tree.body);
                        }
                    }

                    imports.put(comp, imports(comp));
                    Seq<ClassSymbol> deps = dependencies(comp);

                    EntityComponent compAnno = anno(comp, EntityComponent.class);
                    if(!compAnno.vanilla()){
                        TypeSpec.Builder intBuilder = TypeSpec.interfaceBuilder(intName(comp))
                            .addOriginatingElement(comp)
                            .addModifiers(PUBLIC, ABSTRACT)
                            .addAnnotation(spec(EntityInterface.class))
                            .addAnnotation(
                                AnnotationSpec.builder(spec(SuppressWarnings.class))
                                    .addMember("value", "{$S, $S, $S}", "all", "unchecked", "deprecation")
                                .build()
                            );

                        for(Type ext : comp.getInterfaces()) if(!isCompInter(conv(ext))) intBuilder.addSuperinterface(spec(ext));
                        for(ClassSymbol dep : deps) intBuilder.addSuperinterface(procName(dep, this::intName));

                        ObjectSet<String> signatures = new ObjectSet<>();
                        for(Symbol s : comp.getEnclosedElements()){
                            if(isAny(s, PRIVATE, STATIC)) continue;
                            if(s.getKind() == METHOD){
                                MethodSymbol e = (MethodSymbol)s;

                                signatures.add(sigName(e));
                                if(anno(e, Override.class) == null){
                                    MethodSpec.Builder methBuilder = MethodSpec.methodBuilder(name(e))
                                        .addModifiers(PUBLIC, ABSTRACT)
                                        .returns(spec(e.getReturnType()));

                                    for(TypeVariableSymbol t : e.getTypeParameters()) methBuilder.addTypeVariable(spec(t));
                                    for(Type t : e.getThrownTypes()) methBuilder.addException(spec(t));
                                    for(VarSymbol v : e.getParameters()) methBuilder.addParameter(spec(v));
                                    intBuilder.addMethod(methBuilder.build());
                                }
                            }else if(s.getKind() == FIELD && anno(s, Import.class) == null){
                                VarSymbol v = (VarSymbol)s;
                                String name = name(v);

                                if(!signatures.contains(name + "()")){
                                    MethodSpec.Builder getter = MethodSpec.methodBuilder(name)
                                        .addModifiers(PUBLIC, ABSTRACT)
                                        .returns(spec(v.type));

                                    for(Compound anno : v.getAnnotationMirrors()){
                                        String aname = name(anno.type.tsym);
                                        if(aname.contains("Null") || aname.contains("Deprecated")) getter.addAnnotation(spec((TypeElement)anno.type.tsym));
                                    }

                                    intBuilder.addMethod(getter.build());
                                }

                                if(!is(v, FINAL) && anno(v, ReadOnly.class) == null && !signatures.contains(name + "(" + v.type + ")")){
                                    MethodSpec.Builder setter = MethodSpec.methodBuilder(name)
                                        .addModifiers(PUBLIC, ABSTRACT)
                                        .returns(TypeName.VOID);

                                    ParameterSpec.Builder param = ParameterSpec.builder(spec(v.type), name(v));
                                    for(Compound anno : v.getAnnotationMirrors()){
                                        String aname = name(anno.type.tsym);
                                        if(aname.contains("Null") || aname.contains("Deprecated")) param.addAnnotation(spec((TypeElement)anno.type.tsym));
                                    }

                                    intBuilder.addMethod(setter.addParameter(param.build()).build());
                                }
                            }
                        }

                        write(packageName, intBuilder, imports.get(comp));
                        if(compAnno.base()){
                            Seq<ClassSymbol> baseDeps = deps.copy().add(comp);
                            baseDependencies.get(comp, ObjectSet::new).addAll(baseDeps);

                            if(anno(comp, EntityDef.class) == null){
                                String tname = baseName(comp);

                                TypeSpec.Builder base = TypeSpec.classBuilder(tname)
                                    .addModifiers(PUBLIC, ABSTRACT)
                                    .addOriginatingElement(comp);

                                for(ClassSymbol dep : baseDeps){
                                    for(Symbol s : dep.getEnclosedElements()){
                                        if(s.getKind() == FIELD && !isAny(s, PRIVATE, STATIC) && anno(s, Import.class) == null && anno(s, ReadOnly.class) == null){
                                            VarSymbol v = (VarSymbol)s;
                                            FieldSpec.Builder field = FieldSpec.builder(spec(v.type), name(v), PUBLIC);

                                            if(is(v, TRANSIENT)) field.addModifiers(TRANSIENT);
                                            if(is(v, VOLATILE)) field.addModifiers(VOLATILE);
                                            for(Compound anno : v.getAnnotationMirrors()) field.addAnnotation(spec(anno));

                                            JCExpression init = varInitializers.get(desc(v));
                                            if(init != null) field.initializer(init.toString());

                                            base.addField(field.build());
                                        }
                                    }

                                    base.addSuperinterface(procName(dep, this::intName));
                                }

                                baseClasses.put(tname, base);
                            }
                        }
                    }else if(compAnno.base()){
                        ObjectSet<ClassSymbol> baseDeps = baseDependencies.get(comp, ObjectSet::new);
                        baseDeps.add(comp);
                        baseDeps.addAll(deps);

                        String name = baseName(comp);
                        ClassSymbol base = elements.getTypeElement("mindustry.gen." + name);
                        if(base != null) baseClassTypes.put(name, base);
                    }
                }
            }

            case 2 -> {
                for(ClassSymbol t : this.<ClassSymbol>with(EntityInterface.class)) inters.put(name(t), t);

                OrderedSet<String> registers = new OrderedSet<>();
                registers.orderedItems().ordered = false;

                OrderedMap<String, ClassSymbol> defComps = new OrderedMap<>();
                ObjectMap<String, ClassSymbol> defCompsResolve = new ObjectMap<>();

                Seq<String> defGroups = new Seq<>(false);
                ObjectSet<ClassSymbol> excludeGroups = new ObjectSet<>();

                ObjectMap<String, Seq<MethodSymbol>> methods = new ObjectMap<>();
                ObjectMap<FieldSpec, VarSymbol> specVariables = new ObjectMap<>();
                ObjectSet<String> usedFields = new ObjectSet<>();

                Seq<VarSymbol> syncedFields = new Seq<>();
                Seq<VarSymbol> allFields = new Seq<>();
                Seq<FieldSpec> allFieldSpecs = new Seq<>();

                ObjectMap<String, Seq<MethodSymbol>> allInserters = new ObjectMap<>();
                ObjectMap<String, Seq<MethodSymbol>> allWrappers = new ObjectMap<>();
                Seq<MethodSymbol> bypass = new Seq<>(), inserts = new Seq<>(), wraps = new Seq<>();
                OrderedSet<MethodSymbol> standaloneInserts = new OrderedSet<>(), standaloneWraps = new OrderedSet<>();

                ObjectSet<MethodSymbol> removal = new ObjectSet<>();
                for(Symbol def : defs){
                    EntityDef defAnno = anno(def, EntityDef.class);

                    defComps.clear();
                    for(ClassSymbol comp : types(defAnno::value)){
                        String name = name(comp);

                        ClassSymbol inter = inter(name);
                        if(inter != null) defComps.put(compName(name), comp(inter));
                    }

                    if(defComps.isEmpty()) continue;

                    ClassSymbol baseClassType = null;
                    for(ClassSymbol comp : defComps.values()){
                        if(anno(comp, EntityComponent.class).base()){
                            if(baseClassType == null){
                                baseClassType = comp;
                            }else{
                                throw err("Can't have more than one base classes.", def);
                            }
                        }
                    }

                    boolean typeIsBase = baseClassType != null && anno(def, EntityComponent.class) != null && anno(def, EntityComponent.class).base();
                    String name = def instanceof ClassSymbol ? baseName(name(def)) : createName(defComps);

                    if(!typeIsBase && baseClassType != null && name.equals(baseName(baseClassType))) name += "Entity";
                    if(!registers.add(name)) continue;

                    defCompsResolve.clear();
                    for(ClassSymbol comp : defComps.values()) for(ClassSymbol dep : dependencies(comp)) defCompsResolve.put(name(dep), dep);
                    defComps.putAll(defCompsResolve);

                    defGroups.clear();
                    excludeGroups.clear();
                    for(ClassSymbol comp : defComps.values()){
                        ExcludeGroups ex = anno(comp, ExcludeGroups.class);
                        if(ex != null){
                            for(ClassSymbol i : types(ex::value)){
                                ClassSymbol t = comp(i);
                                if(t != null) excludeGroups.add(t);
                            }
                        }
                    }

                    for(ClassSymbol comp : defComps.values()) if(!excludeGroups.contains(comp) && groups.containsKey(comp)) defGroups.add(groups.get(comp));

                    methods.clear();
                    specVariables.clear();
                    usedFields.clear();

                    TypeSpec.Builder builder = TypeSpec.classBuilder(name)
                        .addModifiers(PUBLIC)
                        .addAnnotation(
                            AnnotationSpec.builder(SuppressWarnings.class)
                                .addMember("value", "{$S, $S, $S}", "all", "unchecked", "deprecation")
                            .build()
                        );

                    if(def instanceof ClassSymbol){
                        builder.addOriginatingElement(def);
                    }else{
                        for(ClassSymbol comp : defComps.values()) builder.addOriginatingElement(comp);
                    }

                    syncedFields.clear();
                    allFields.clear();
                    allFieldSpecs.clear();

                    allInserters.clear();
                    allWrappers.clear();

                    boolean isSync = defComps.containsKey("SyncComp");
                    for(ClassSymbol comp : defComps.values()){
                        OrderedMap<String, Seq<MethodSymbol>> tmp = inserters.get(comp);
                        if(tmp != null) for(String key : tmp.orderedKeys()) allInserters.get(key, Seq::new).addAll(tmp.get(key));

                        tmp = wrappers.get(comp);
                        if(tmp != null) for(String key : tmp.orderedKeys()) allWrappers.get(key, Seq::new).addAll(tmp.get(key));

                        boolean isShadowed = baseClassType != null && !typeIsBase && baseDependencies.get(baseClassType).contains(comp);
                        for(Symbol s : comp.getEnclosedElements()){
                            if(s.getKind() == FIELD && anno(s, Import.class) == null){
                                VarSymbol v = (VarSymbol)s;

                                String fname = name(v);
                                if(!usedFields.add(fname)) throw err("Duplicate field names: '" + fname + "'.", def);

                                FieldSpec.Builder field = FieldSpec.builder(spec(v.type), fname);
                                if(is(v, STATIC)){
                                    field.addModifiers(STATIC);
                                    if(is(v, FINAL)) field.addModifiers(FINAL);
                                }

                                if(is(v, TRANSIENT)) field.addModifiers(TRANSIENT);
                                if(is(v, VOLATILE)) field.addModifiers(VOLATILE);
                                if(is(v, PRIVATE)){
                                    field.addModifiers(PRIVATE);
                                }else if(anno(v, ReadOnly.class) != null){
                                    field.addModifiers(PROTECTED);
                                }else{
                                    field.addModifiers(PUBLIC);
                                }

                                JCExpression init = varInitializers.get(desc(v));
                                if(init != null) field.initializer(init.toString());

                                for(Compound anno : v.getAnnotationMirrors()) field.addAnnotation(spec(anno));
                                FieldSpec spec = field.build();

                                boolean isVisible = !isAny(v, STATIC, PRIVATE) && anno(v, ReadOnly.class) == null;
                                if(!isShadowed || !isVisible) builder.addField(spec);

                                specVariables.put(spec, v);
                                allFieldSpecs.add(spec);
                                allFields.add(v);

                                if(isSync && anno(v, SyncField.class) != null){
                                    if(v.type.getKind() != FLOAT) throw err("All @SyncFields must be float.", def);

                                    syncedFields.add(v);
                                    builder.addField(FieldSpec.builder(TypeName.FLOAT, fname + EntityIO.targetSuffix, TRANSIENT, PRIVATE).build());
                                    builder.addField(FieldSpec.builder(TypeName.FLOAT, fname + EntityIO.lastSuffix, TRANSIENT, PRIVATE).build());
                                }
                            }else if(s.getKind() == METHOD){
                                MethodSymbol m = (MethodSymbol)s;
                                methods.get(sigName(m), Seq::new).add(m);
                            }
                        }
                    }

                    if(!methods.containsKey("toString()")){
                        builder.addMethod(
                            MethodSpec.methodBuilder("toString")
                                .addAnnotation(spec(Override.class))
                                .returns(spec(String.class))
                                .addModifiers(PUBLIC)
                                .addStatement("return $S + $L", name + "#", "id")
                            .build()
                        );
                    }

                    EntityIO io = new EntityIO(this, name, builder, allFieldSpecs, serializer, revDir.child(name));
                    boolean hasIO = defAnno.genIO() && (isSync || defAnno.serialize());

                    boolean serializeOverride = false;
                    for(Entry<String, Seq<MethodSymbol>> entry : methods.entries()){
                        String key = entry.key;
                        Seq<MethodSymbol> entries = entry.value;

                        MethodSymbol topReplacer = entries.max(m -> {
                            Replace rep = anno(m, Replace.class);
                            return rep == null ? -1 : rep.value();
                        });

                        Replace topReplace;
                        if(topReplacer != null && (topReplace = anno(topReplacer, Replace.class)) != null){
                            int max = topReplace.value();
                            if(topReplacer.getReturnType().getKind() == VOID){
                                entries.removeAll(m -> {
                                    Replace rep = anno(m, Replace.class);
                                    return rep == null || rep.value() != max;
                                });
                            }else{
                                if(entries.count(m -> {
                                    Replace rep = anno(m, Replace.class);
                                    return rep != null && rep.value() == max;
                                }) > 1) throw err("Type " + name + " has multiple components replacing non-void method " + key + " with similar priorities.", def);

                                entries.clear();
                                entries.add(topReplacer);
                            }
                        }

                        removal.clear();
                        for(MethodSymbol m : entries){
                            Remove rem = anno(m, Remove.class);
                            if(rem != null){
                                if(removal.contains(m)) throw err(sigName(m) + " is already @Remove-d by another method.", def);

                                MethodSymbol removed = entries.find(e -> same(e.enclClass(), type(rem::value)));
                                if(removed != null) removal.add(removed);
                            }
                        }

                        Iterator<MethodSymbol> it = entries.iterator();
                        while(it.hasNext()) if(removal.contains(it.next())) it.remove();

                        if(entries.count(m -> !isAny(m, ABSTRACT, NATIVE) && m.getReturnType().getKind() != VOID) > 1){
                            throw err("Type " + name + " has multiple components implementing non-void method " + entry.key + ".", def);
                        }

                        entries.sort(methodSorter);

                        MethodSymbol m = entries.first();
                        String mname = name(m);

                        if(entries.size > 1 && mname.equals("serialize") && m.getReturnType().getKind() == BOOLEAN && m.params.size() == 0){
                            serializeOverride = true;
                        }else if(anno(m, InternalImpl.class) != null){
                            continue;
                        }

                        boolean isPrivate = is(m, PRIVATE);
                        MethodSpec.Builder methBuilder = MethodSpec.methodBuilder(mname)
                            .addModifiers(isPrivate ? PRIVATE : PUBLIC)
                            .returns(spec(m.getReturnType()));

                        if(!isPrivate && !is(m, STATIC)) methBuilder.addAnnotation(spec(Override.class));
                        if(is(m, STATIC)) methBuilder.addModifiers(STATIC);

                        for(TypeVariableSymbol t : m.getTypeParameters()) methBuilder.addTypeVariable(spec(t));
                        for(Type t : m.getThrownTypes()) methBuilder.addException(spec(t));
                        for(VarSymbol v : m.params) methBuilder.addParameter(spec(v));

                        boolean writeBlock = m.getReturnType().getKind() == VOID && entries.size > 1;
                        if(isAny(m, ABSTRACT, NATIVE) && entries.size == 1 && anno(m, InternalImpl.class) == null){
                            throw err(desc(m) + " is an abstract method and must be implemented in some component.", def);
                        }

                        inserts.set(allInserters.get(key, inserts.clear()));
                        wraps.set(allWrappers.get(key, wraps.clear()));
                        if(m.getReturnType().getKind() != VOID){
                            if(!inserts.isEmpty()) throw err("Method " + sigName(m) + " is not void; no methods can @Insert to it.", def);
                            if(!wraps.isEmpty()) throw err("Method " + sigName(m) + " is not void; no methods can @Wrap it.", def);
                        }

                        standaloneInserts.clear();
                        standaloneWraps.clear();

                        it = inserts.iterator();
                        while(it.hasNext()){
                            MethodSymbol next = it.next();
                            if(same(type(anno(next, Insert.class)::block), conv(Void.class))){
                                it.remove();
                                standaloneInserts.add(next);
                            }
                        }

                        it = wraps.iterator();
                        while(it.hasNext()){
                            MethodSymbol next = it.next();
                            if(same(type(anno(next, Wrap.class)::block), conv(Void.class))){
                                it.remove();
                                standaloneWraps.add(next);
                            }
                        }

                        standaloneInserts.orderedItems().sort(methodSorter);
                        inserts.removeAll(standaloneInserts::contains);
                        wraps.removeAll(standaloneWraps::contains);
                        inserts.sort(methodSorter);

                        if(mname.equals("add") || mname.equals("remove")){
                            bypass.selectFrom(entries, e -> anno(e, BypassGroupCheck.class) != null);
                            entries.removeAll(bypass);

                            bypass.sort(methodSorter);

                            Seq<MethodSymbol> priorBypass = tmpMethods.selectFrom(standaloneInserts.orderedItems(), e -> !anno(e, Insert.class).after() && anno(e, BypassGroupCheck.class) != null);
                            if(priorBypass.any()){
                                standaloneInserts.removeAll(priorBypass);
                                for(MethodSymbol e : priorBypass) methBuilder.addStatement("this.$L()", name(e));
                            }

                            append(methBuilder, defComps.values(), bypass, inserts, wraps, writeBlock);

                            methBuilder.addStatement("if($Ladded) return", mname.equals("add") ? "" : "!");
                            for(String group : defGroups) methBuilder.addStatement("$T.$L.$L(this)", spec(Groups.class), group, mname);
                        }

                        if(!standaloneWraps.isEmpty()){
                            Seq<MethodSymbol> arr = standaloneWraps.orderedItems();

                            StringBuilder format = new StringBuilder("if(this.$L()");
                            tmpArgs.clear().add(name(arr.first()));

                            for(int i = 1; i < arr.size; i++){
                                format.append(" && this.$L()");
                                tmpArgs.add(name(arr.get(i)));
                            }

                            methBuilder.beginControlFlow(format.append(")").toString(), tmpArgs.toArray());
                        }

                        for(MethodSymbol e : standaloneInserts) if(!anno(e, Insert.class).after()) methBuilder.addStatement("this.$L()", name(e));

                        allFields.sortComparing(BaseProcessor::name);
                        syncedFields.sortComparing(BaseProcessor::name);

                        if(hasIO){
                            if((mname.equals("read") || mname.equals("write"))){
                                io.write(methBuilder, mname.equals("write"));
                            }

                            if((mname.equals("readSync") || mname.equals("writeSync"))){
                                io.writeSync(methBuilder, mname.equals("writeSync"), /*syncedFields,*/ allFields);
                            }

                            if((mname.equals("readSyncManual") || mname.equals("writeSyncManual"))){
                                io.writeSyncManual(methBuilder, mname.equals("writeSyncManual"), syncedFields);
                            }

                            if(mname.equals("interpolate")){
                                io.writeInterpolate(methBuilder, syncedFields);
                            }

                            if(mname.equals("snapSync")){
                                methBuilder.addStatement("updateSpacing = 16");
                                methBuilder.addStatement("lastUpdated = $T.millis()", Time.class);
                                for(VarSymbol v : syncedFields){
                                    methBuilder.addStatement("$L = $L", name(v) + EntityIO.lastSuffix, name(v) + EntityIO.targetSuffix);
                                    methBuilder.addStatement("$L = $L", name(v), name(v) + EntityIO.targetSuffix);
                                }
                            }

                            if(mname.equals("snapInterpolation")){
                                methBuilder.addStatement("updateSpacing = 16");
                                methBuilder.addStatement("lastUpdated = $T.millis()", Time.class);
                                for(VarSymbol v : syncedFields){
                                    methBuilder.addStatement("$L = $L", name(v) + EntityIO.lastSuffix, name(v));
                                    methBuilder.addStatement("$L = $L", name(v) + EntityIO.targetSuffix, name(v));
                                }
                            }
                        }

                        append(methBuilder, defComps.values(), entries, inserts, wraps, writeBlock);

                        for(MethodSymbol e : standaloneInserts) if(anno(e, Insert.class).after()) methBuilder.addStatement("this.$L()", name(e));
                        if(defAnno.pooled() && mname.equals("remove")) methBuilder.addStatement("$T.queueFree(this)", spec(Groups.class));

                        if(!standaloneWraps.isEmpty()) methBuilder.endControlFlow();
                        builder.addMethod(methBuilder.build());
                    }

                    if(!serializeOverride){
                        builder.addMethod(
                            MethodSpec.methodBuilder("serialize").addModifiers(Modifier.PUBLIC)
                                .addAnnotation(spec(Override.class))
                                .returns(TypeName.BOOLEAN)
                                .addStatement("return " + defAnno.serialize())
                            .build()
                        );
                    }

                    if(defAnno.pooled()){
                        builder.addSuperinterface(Poolable.class);

                        MethodSpec.Builder resetBuilder = MethodSpec.methodBuilder("reset")
                            .addModifiers(PUBLIC)
                            .addAnnotation(spec(Override.class));

                        allFieldSpecs.sortComparing(s -> s.name);
                        for(FieldSpec spec : allFieldSpecs){
                            VarSymbol v = specVariables.get(spec);
                            if(v == null || isAny(v, STATIC, FINAL)) continue;

                            String desc = desc(v);
                            if(spec.type.isPrimitive()){
                                resetBuilder.addStatement("this.$L = $L", spec.name, varInitializers.containsKey(desc) ? varInitializers.get(desc) : getDefault(spec.type.toString()));
                            }else{
                                if(!varInitializers.containsKey(desc)){
                                    resetBuilder.addStatement("$L = null", spec.name);
                                } //TODO else
                            }
                        }

                        builder.addMethod(resetBuilder.build());
                    }

                    MethodSpec.Builder creator = MethodSpec.methodBuilder("create")
                        .addModifiers(PUBLIC, STATIC)
                        .returns(ClassName.get(packageName, name));

                    if(defAnno.pooled()){
                        creator.addStatement("return $T.obtain($T.class, $T::new)", spec(Pools.class), ClassName.get(packageName, name), ClassName.get(packageName, name));
                    }else{
                        creator.addStatement("return new $T()", ClassName.get(packageName, name));
                    }

                    builder
                        .addMethod(MethodSpec.constructorBuilder().addModifiers(PROTECTED).build())
                        .addMethod(creator.build());

                    definitions.add(new EntityDefinition(name, builder, def, typeIsBase ? null : baseClassType, defComps.values().toSeq(), allFieldSpecs.copy()));
                }

                TypeSpec.Builder registry = TypeSpec.classBuilder("EntityRegistry")
                    .addModifiers(PUBLIC, FINAL)
                    .addAnnotation(
                        AnnotationSpec.builder(spec(SuppressWarnings.class))
                            .addMember("value", "$S", "unchecked")
                        .build()
                    )
                    .addField(
                        FieldSpec.builder(
                            paramSpec(spec(ObjectMap.class), spec(String.class), paramSpec(spec(Prov.class), subSpec(spec(Object.class)))),
                            "map",
                            PRIVATE, STATIC, FINAL
                        )
                        .initializer("new $T<>()", spec(ObjectMap.class))
                        .build()
                    )
                    .addField(
                        FieldSpec.builder(
                            paramSpec(spec(ObjectIntMap.class), paramSpec(spec(Class.class), subSpec(spec(Object.class)))),
                            "ids",
                            PRIVATE, STATIC, FINAL
                        )
                        .initializer("new $T<>()", spec(ObjectIntMap.class)).
                        build()
                    )
                    .addMethod(
                        MethodSpec.constructorBuilder()
                            .addModifiers(PRIVATE)
                            .addStatement("throw new $T()", spec(AssertionError.class))
                        .build()
                    )
                    .addMethod(
                        MethodSpec.methodBuilder("get")
                            .addModifiers(PUBLIC, STATIC)
                            .addTypeVariable(tvSpec("T"))
                            .returns(paramSpec(spec(Prov.class), subSpec(tvSpec("T"))))
                            .addParameter(paramSpec(spec(Class.class), tvSpec("T")), "type")
                            .addStatement("return get(type.getCanonicalName())")
                        .build()
                    )
                    .addMethod(
                        MethodSpec.methodBuilder("get")
                            .addModifiers(PUBLIC, STATIC)
                            .addTypeVariable(tvSpec("T"))
                            .returns(paramSpec(spec(Prov.class), subSpec(tvSpec("T"))))
                            .addParameter(spec(String.class), "name")
                            .addStatement("return ($T)map.get(name)", paramSpec(spec(Prov.class), subSpec(tvSpec("T"))))
                        .build()
                    )
                    .addMethod(
                        MethodSpec.methodBuilder("getID")
                            .addModifiers(PUBLIC, STATIC)
                            .returns(TypeName.INT)
                            .addParameter(paramSpec(spec(Class.class), subSpec(spec(Object.class))), "type")
                            .addStatement("return ids.get(type, -1)")
                        .build()
                    )
                    .addMethod(
                        MethodSpec.methodBuilder("register")
                            .addModifiers(PUBLIC, STATIC)
                            .addTypeVariable(tvSpec("T"))
                            .returns(TypeName.VOID)
                            .addParameter(spec(String.class), "name")
                            .addParameter(paramSpec(spec(Class.class), tvSpec("T")), "type")
                            .addParameter(paramSpec(spec(Prov.class), subSpec(tvSpec("T"))), "prov")
                            .addStatement("map.put(name, prov)")
                            .addStatement("ids.put(type, $T.register(name, prov))", spec(EntityMapping.class))
                        .build()
                    )
                    .addMethod(
                        MethodSpec.methodBuilder("content")
                            .addModifiers(PUBLIC, STATIC)
                            .addTypeVariable(tvSpec("T"))
                            .addTypeVariable(tvSpec("E"))
                            .returns(tvSpec("T"))
                            .addParameter(spec(String.class), "name")
                            .addParameter(paramSpec(spec(Class.class), tvSpec("E")), "type")
                            .addParameter(paramSpec(spec(Func.class), spec(String.class), subSpec(tvSpec("T"))), "create")
                            .addStatement("$T.nameMap.put($S + name, get(type))", spec(EntityMapping.class), "unity-")
                            .addStatement("return create.get(name)")
                        .build()
                    );

                MethodSpec.Builder register = MethodSpec.methodBuilder("register")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(TypeName.VOID);

                pointers.sort(Structs.comparing(BaseProcessor::fName));
                for(ClassSymbol point : pointers){
                    registry.addOriginatingElement(point);

                    ClassName name = ClassName.get(point);
                    register.addStatement("register($S, $T.class, $T::new)", name.canonicalName(), name, name);
                }

                definitions.sort(Structs.comparing(def -> def.name));
                definitions.flatMap(def -> def.components).distinct().each(registry::addOriginatingElement);

                Seq<String> imports = new Seq<>();
                for(EntityDefinition def : definitions){
                    imports.clear();

                    ClassName name = ClassName.get(packageName, def.name);
                    register.addStatement("register($S, $T.class, $T::new)", name.canonicalName(), name, name);

                    def.builder.addMethod(
                    MethodSpec.methodBuilder("classId")
                        .addModifiers(PUBLIC)
                            .addAnnotation(spec(Override.class))
                            .returns(TypeName.INT)
                            .addStatement("return $T.getID($T.class)", ClassName.get(packageName, "EntityRegistry"), name)
                        .build()
                    );

                    ClassSymbol ext = null;
                    if(def.extend != null){
                        ext = baseClassTypes.get(baseName(def.extend));
                        def.builder.superclass(spec(ext));
                    }

                    ObjectSet<String> methodNames = def.components.flatMap(t -> {
                        Seq<String> out = new Seq<>();
                        for(Symbol s : t.getEnclosedElements()) if(s.getKind() == METHOD) out.add(sigName((MethodSymbol)s));

                        return out;
                    }).<String>as().asSet();

                    TypeSpec.Builder superclass = null;
                    if(ext != null) superclass = baseClasses.get(name(ext));

                    for(ClassSymbol comp : def.components){
                        imports.addAll(this.imports.get(comp));

                        ClassSymbol inter = inter(comp);
                        if(inter == null) throw err("Failed to implement for '" + comp + "'", def.naming);

                        def.builder.addSuperinterface(spec(inter));
                        for(Symbol s : inter.getEnclosedElements()){
                            if(s.getKind() == METHOD){
                                MethodSymbol m = (MethodSymbol)s;

                                String var = name(m);
                                FieldSpec field = def.fieldSpecs.find(f -> f.name.equals(var));
                                if(field == null || methodNames.contains(sigName(m))) continue;

                                MethodSpec result = null;
                                if(m.getReturnType().getKind() != VOID){
                                    result = MethodSpec.methodBuilder(var)
                                        .addModifiers(PUBLIC)
                                        .addAnnotation(spec(Override.class))
                                        .returns(spec(m.getReturnType()))
                                        .addStatement("return $L", var)
                                    .build();
                                }else if(!Seq.with(field.annotations).contains(f -> f.type.toString().equals("@" + fName(ReadOnly.class)))){
                                    result = MethodSpec.methodBuilder(var)
                                        .addModifiers(PUBLIC)
                                        .addAnnotation(spec(Override.class))
                                        .returns(TypeName.VOID)
                                        .addParameter(field.type, var)
                                        .addStatement("this.$L = $L", var, var)
                                    .build();
                                }

                                if(result != null){
                                    if(superclass != null){
                                        FieldSpec superField = Seq.with(superclass.fieldSpecs).find(f -> f.name.equals(var));
                                        if(superField != null){
                                            TypeName ret = result.returnType;
                                            MethodSpec targetMethod = Seq.with(superclass.methodSpecs).find(e -> e.name.equals(var) && e.returnType.equals(ret));

                                            if(targetMethod == null) superclass.addMethod(result);
                                            continue;
                                        }
                                    }else if(ext != null){
                                        VarSymbol superField = null;
                                        for(Symbol sym : ext.getEnclosedElements()){
                                            if(sym.getKind() == FIELD && name(sym).equals(var)) superField = (VarSymbol)sym;
                                        }

                                        if(superField != null){
                                            MethodSymbol targetMethod = null;
                                            for(Symbol sym : ext.getEnclosedElements()){
                                                if(sym.getKind() == METHOD && name(sym).equals(var)){
                                                    MethodSymbol msym = (MethodSymbol)sym;
                                                    if(spec(msym.getReturnType()).equals(result.returnType)) targetMethod = msym;
                                                }
                                            }

                                            if(targetMethod != null) continue;
                                        }
                                    }
                                }

                                if(result != null) def.builder.addMethod(result);
                            }
                        }
                    }

                    write(packageName, def.builder, imports);
                }

                for(TypeSpec.Builder base : baseClasses.values()){
                    imports.clear();
                    for(ClassSymbol dep : baseDependencies.get(comp(Reflect.get(base, "name") + "Comp"))) imports.addAll(this.imports.get(dep));

                    write(packageName, base, imports);
                }

                write(packageName, registry.addMethod(register.build()), null);
            }
        }
    }

    protected void append(MethodSpec.Builder methBuilder, Iterable<ClassSymbol> defComps, Seq<MethodSymbol> entries, Seq<MethodSymbol> inserts, Seq<MethodSymbol> wraps, boolean writeBlock){
        for(MethodSymbol m : entries){
            if(!ext(m, defComps)) continue;
            String blockName = baseName(m.enclClass()).toLowerCase();

            Seq<MethodSymbol> wrapComp = tmpMethods.selectFrom(wraps, e -> baseName(comp(type(anno(e, Wrap.class)::block))).toLowerCase().equals(blockName));

            boolean wrapped = !wrapComp.isEmpty();
            if(wrapped){
                StringBuilder format = new StringBuilder("if(this.$L()");
                tmpArgs.clear().add(name(wrapComp.first()));

                for(int i = 1; i < wrapComp.size; i++){
                    format.append(" && this.$L()");
                    tmpArgs.add(name(wrapComp.get(i)));
                }

                methBuilder.beginControlFlow(format.append(")").toString(), tmpArgs.toArray());
            }

            Seq<MethodSymbol> insertComp = tmpMethods.selectFrom(inserts, e -> baseName(comp(type(anno(e, Insert.class)::block))).toLowerCase().equals(blockName));
            for(MethodSymbol e : insertComp) if(!anno(e, Insert.class).after()) methBuilder.addStatement("this.$L()", name(e));

            String desc = desc(m);
            if(!isAny(m, ABSTRACT, NATIVE) && methodBlocks.containsKey(desc)){
                String block = str(methodBlocks.get(desc), (writeBlock && anno(m, BreakAll.class) == null) ? blockName : null);
                if(!block.replace("\n", "").replaceAll("\\s+", "").isEmpty()){
                    if(writeBlock) methBuilder.beginControlFlow("$L:", blockName);
                    methBuilder.addCode(block);
                    if(writeBlock) methBuilder.endControlFlow();
                }
            }

            for(MethodSymbol e : insertComp) if(anno(e, Insert.class).after()) methBuilder.addStatement("this.$L()", name(e));
            if(wrapped) methBuilder.endControlFlow();
        }
    }

    protected String str(JCBlock block, String blockName){
        StringWriter writer = new StringWriter();
        try{
            (blockName == null ? new Pretty(writer, true) : new Pretty(writer, true){
                int innerLevel;

                @Override
                public void visitClassDef(JCClassDecl tree){
                    innerLevel++;
                    super.visitClassDef(tree);
                    innerLevel--;
                }

                @Override
                public void visitLambda(JCLambda tree){
                    innerLevel++;
                    super.visitLambda(tree);
                    innerLevel--;
                }

                @Override
                public void visitReturn(JCReturn tree){
                    if(innerLevel > 0){
                        super.visitReturn(tree);
                    }else{
                        try{
                            print("break ");
                            print(blockName);
                            print(";");
                        }catch(IOException ignored){}
                    }
                }
            }).printStats(block.stats);
        }catch(IOException ignored){}
        return writer.toString()
            .replaceAll("this\\.<(.*)>self\\(\\)", "this")
            .replaceAll("self\\(\\)(?!\\s+instanceof)", "this")
            .replaceAll(" yield ", "")
            .replaceAll("/\\*missing\\*/", "var");
    }

    protected boolean ext(ExecutableElement e, Iterable<ClassSymbol> defComps){
        Extend ext = anno(e, Extend.class);
        if(ext == null) return true;

        ClassSymbol target = comp(type(ext::value));
        for(ClassSymbol comp : defComps) if(comp == target) return true;
        return false;
    }

    protected ClassSymbol comp(Class<?> type){
        return comp(compName(name(type)));
    }

    protected ClassSymbol comp(ClassSymbol inter){
        String name = name(inter);
        if(!name.endsWith("c")) return null;

        return comp(compName(name));
    }

    protected ClassSymbol comp(String compName){
        return comps.get(compName);
    }

    protected ClassSymbol inter(ClassSymbol comp){
        String name = name(comp);
        if(!name.endsWith("Comp")) return null;

        return inter(intName(name));
    }

    protected ClassSymbol inter(String intName){
        return inters.get(intName);
    }

    protected String intName(ClassSymbol comp){
        return intName(name(comp));
    }

    protected String intName(String compName){
        return baseName(compName) + "c";
    }

    protected String baseName(ClassSymbol comp){
        return baseName(name(comp));
    }

    protected String baseName(String compName){
        if(!compName.endsWith("Comp")) throw err("All types annotated with @EntityComponent must have 'Comp' as the name's suffix: '" + compName + "'.");
        return compName.substring(0, compName.length() - 4);
    }

    protected String compName(String intName){
        if(!intName.endsWith("c")) throw err("Interface names must end with 'c': '" + intName + "'.");
        return intName.substring(0, intName.length() - 1) + "Comp";
    }

    protected boolean isCompInter(ClassSymbol inter){
        return comp(inter) != null;
    }

    protected Seq<ClassSymbol> dependencies(ClassSymbol comp){
        if(!dependencies.containsKey(comp)){
            ObjectSet<ClassSymbol> out = new ObjectSet<>();
            for(Type type : comp.getInterfaces()){
                String name = name(type.tsym);
                if(name.endsWith("c")){
                    ClassSymbol dep = comp(compName(name));
                    if(dep != null && dep != comp) out.add(dep);
                }
            }

            ObjectSet<ClassSymbol> result = new ObjectSet<>();
            for(ClassSymbol type : out){
                result.add(type);
                result.addAll(dependencies(type));
            }

            if(anno(comp, EntityBaseComponent.class) == null) result.addAll(baseComps);
            dependencies.put(comp, result.toSeq());
        }

        return dependencies.get(comp);
    }

    protected ClassName procName(ClassSymbol comp, Func<ClassSymbol, String> name){
        return ClassName.get(
            comp.packge().toString().contains("fetched") ? "mindustry.gen" : packageName,
            name.get(comp)
        );
    }

    protected String createName(OrderedMap<String, ClassSymbol> comps){
        Seq<String> keys = comps.orderedKeys();

        StringBuilder builder = new StringBuilder();
        for(int i = keys.size - 1; i >= 0; i--) builder.append(baseName(comps.get(keys.get(i))));

        return builder.toString();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes(){
        return Set.of(
            fName(EntityComponent.class),
            fName(EntityBaseComponent.class),
            fName(EntityDef.class),
            fName(EntityPoint.class)
        );
    }

    @Override
    public Set<String> getSupportedOptions(){
        return Set.of("revisionDirectory");
    }

    protected static class EntityDefinition{
        protected final String name;
        protected final TypeSpec.Builder builder;
        protected final Symbol naming;
        protected final @Nullable ClassSymbol extend;
        protected final Seq<ClassSymbol> components;
        protected final Seq<FieldSpec> fieldSpecs;

        public EntityDefinition(String name, TypeSpec.Builder builder, Symbol naming, ClassSymbol extend, Seq<ClassSymbol> components, Seq<FieldSpec> fieldSpecs){
            this.name = name;
            this.builder = builder;
            this.naming = naming;
            this.extend = extend;
            this.components = components;
            this.fieldSpecs = fieldSpecs;
        }

        @Override
        public String toString(){
            return "EntityDefinition{" + "components=" + components + ", fieldSpecs=" + fieldSpecs + ", builder=" + builder + ", naming=" + naming + ", name='" + name + '\'' + ", extend=" + extend + '}';
        }
    }
}
