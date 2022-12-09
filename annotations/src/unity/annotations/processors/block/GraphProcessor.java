package unity.annotations.processors.block;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import com.squareup.javapoet.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import unity.annotations.Annotations.*;
import unity.annotations.processors.*;

import java.util.*;

import static javax.lang.model.element.ElementKind.*;
import static javax.lang.model.element.Modifier.*;

/**
 * A specialized annotation processor used to generate graph block boilerplates.
 * @author GlennFolker
 */
public class GraphProcessor extends BaseProcessor{
    public static final String packageName = packageRoot + ".graph";
    public static final ClassName graphInfoCont = ClassName.get(packageName, "Graphs");
    public static final ClassName graphInfo = ClassName.get(packageName, "Graphs", "GraphInfo");

    protected ClassSymbol graphBlockBase, graphEntBase;
    protected ClassSymbol graphBase;
    protected ClassSymbol nodeTypeBase, nodeBase;
    protected ClassSymbol connectorTypeBase;
    protected OrderedMap<String, GraphEntry> entries = new OrderedMap<>();

    /** (Modify as needed!) Fields that are present in implementations of GraphBlock. */
    protected ObjectMap<String, Seq<FieldSpec>> blockFields;
    /** (Modify as needed!) Fields that are present in implementations of GraphBuild. */
    protected ObjectMap<String, Seq<FieldSpec>> buildFields;
    /**
     * (Modify as needed!) Boilerplate methods that are overriden to interop with the graph system in implementations
     * of GraphBlock.
     */
    protected ObjectMap<String, Seq<Implement>> blockImpls;
    /**
     * (Modify as needed!) Boilerplate methods that are overriden to interop with the graph system in implementations
     * of GraphBuild.
     */
    protected ObjectMap<String, Seq<Implement>> buildImpls;

    {
        rounds = 1;
    }

    protected void initImpls(){
        blockFields = ObjectMap.of(
        "Graph", Seq.with()
        );

        buildFields = ObjectMap.of(
        "Graph", Seq.with(
            FieldSpec.builder(TypeName.INT, "prevTileRotation").initializer("-1").build(),
            FieldSpec.builder(TypeName.BOOLEAN, "placed").initializer("false").build(),
            FieldSpec.builder(TypeName.BOOLEAN, "graphInitialized").initializer("false").build()
        ));

        blockImpls = ObjectMap.of(
        "Graph", Seq.with(
            new Implement((method, callSuper, entries) -> {
                callSuper.get(null, null);
                method.addStatement("setGraphStats(stats)");
            }, "setStats", TypeName.VOID, true),

            new Implement((method, callSuper, entries) -> {
                callSuper.get(null, null);
                method.addStatement("drawConnectionPoints(req, list)");
            }, "drawPlanRegion", TypeName.VOID, true,
            spec(BuildPlan.class), "req",
            paramSpec(spec(Eachable.class), spec(BuildPlan.class)), "list"),

            new Implement((method, callSuper, entries) -> {
                for(var entry : entries){
                    entry = entry.toLowerCase();
                    method.addStatement("cons.get($T.$L, $LNodeConfig)", graphInfoCont, entry, entry);
                }
            }, "eachNodeType", TypeName.VOID, true,
            paramSpec(ClassName.get("unity.func", "IntObjc"), paramSpec(spec(nodeTypeBase), subSpec(spec(Object.class)))), "cons"),

            new Implement((method, callSuper, entries) -> {
                for(var entry : entries){
                    entry = entry.toLowerCase();
                    method.addStatement("for(var conn : $LConnectorConfigs) cons.get($T.$L, conn)", entry, graphInfoCont, entry);
                }
            }, "eachConnectorType", TypeName.VOID, true,
            paramSpec(ClassName.get("unity.func", "IntObjc"), paramSpec(spec(connectorTypeBase), subSpec(spec(Object.class)))), "cons")
        ));

        buildImpls = ObjectMap.of(
        "Graph", Seq.with(
            new Implement((method, callSuper, entries) -> {
                callSuper.get(null, "b");
                method.addStatement("if(b instanceof $T build) build.initGraph()", graphEntBase);
                method.addStatement("return b");
            }, "create", spec(Building.class), true,
            spec(Block.class), "block",
            spec(Team.class), "team"),

            new Implement((method, callSuper, entries) -> {
                callSuper.get(null, null);
                method.addStatement("initGraph()");
            }, "created", TypeName.VOID, true),

            new Implement((method, callSuper, entries) -> {
                callSuper.get(null, null);
                method
                    .beginControlFlow("if(!placed)")
                        .addStatement("placed = true")
                        .addStatement("connectToGraph()")
                    .endControlFlow();
            }, "placed", TypeName.VOID, true),

            new Implement((method, callSuper, entries) -> {
                method
                    .addStatement("disconnectFromGraph()")
                    .addStatement("placed = false");

                callSuper.get(null, null);
            }, "pickedUp", TypeName.VOID, true),

            new Implement((method, callSuper, entries) -> {
                method.addStatement("disconnectFromGraph()");
                callSuper.get(null, null);
            }, "onRemoved", TypeName.VOID, true),

            new Implement((method, callSuper, entries) -> {
                method.addStatement("disconnectFromGraph()");
                callSuper.get(null, null);
            }, "onDestroyed", TypeName.VOID, true),

            new Implement((method, callSuper, entries) -> {
                method
                    .beginControlFlow("if(!placed)")
                        .addStatement("placed = true")
                        .addStatement("connectToGraph()")
                    .endControlFlow();

                callSuper.get(null, null);

                method.addStatement("updateGraphs()");
            }, "updateTile", TypeName.VOID, true),

            new Implement((method, callSuper, entries) -> {
                method.addStatement("return prevTileRotation");
            }, "prevRotation", TypeName.INT, true),

            new Implement((method, callSuper, entries) -> {
                method.addStatement("prevTileRotation = prevRotation");
            }, "prevRotation", TypeName.VOID, true,
            TypeName.INT, "prevRotation"),

            new Implement((method, callSuper, entries) -> {
                method.addStatement("return graphInitialized");
            }, "graphInitialized", TypeName.BOOLEAN, true),

            new Implement((method, callSuper, entries) -> {
                callSuper.get(null, null);
                method.addStatement("displayGraphBars(table)");
            }, "displayBars", TypeName.VOID, true,
            spec(Table.class), "table"),

            new Implement((method, callSuper, entries) -> {
                callSuper.get(null, null);
                method.addStatement("writeGraphs(write)");
            }, "write", TypeName.VOID, true,
            spec(Writes.class), "write"),

            new Implement((method, callSuper, entries) -> {
                callSuper.get(null, null);
                method.addStatement("readGraphs(read)");
            }, "read", TypeName.VOID, true,
            spec(Reads.class), "read",
            TypeName.BYTE, "revision"),

            new Implement((method, callSuper, entries) -> {
                for(var entry : entries){
                    entry = entry.toLowerCase();
                    method.addStatement("cons.get($T.$L, $LNode)", graphInfoCont, entry, entry);
                }
            }, "eachNode", TypeName.VOID, true,
            paramSpec(ClassName.get("unity.func", "IntObjc"), paramSpec(spec(nodeBase), subSpec(spec(Object.class)))), "cons"),

            new Implement((method, callSuper, entries) -> {
                method
                    //.addTypeVariable(tvSpec("T", paramSpec(spec(graphBase), tvSpec("T"))))
                    .addTypeVariable(tvSpec("N", paramSpec(spec(nodeBase), subSpec(paramSpec(spec(graphBase), subSpec(spec(Object.class)))))))
                    .beginControlFlow("switch(type)");

                for(var entry : entries){
                    entry = entry.toLowerCase();
                    method.addStatement("case $T.$L: return (N)$LNode", graphInfoCont, entry, entry);
                }

                method
                    .addStatement("default: return null")
                    .endControlFlow();
            }, "graphNode", tvSpec("N"), true,
            TypeName.INT, "type"),

            new Implement((method, callSuper, entries) -> method
                .addStatement("if(graphInitialized) return")
                .addStatement("graphInitialized = true")
                .addStatement("prevTileRotation = rotation")
                .addStatement("initGraphNodes()")
                .addStatement("onGraphInit()"),
            "initGraph", TypeName.VOID, true),

            new Implement((method, callSuper, entries) -> {
                for(var entry : entries){
                    entry = entry.toLowerCase();
                    method
                        .addStatement("$LNode = $LNodeConfig.create(this)", entry, entry)
                        .addStatement("for(var conn : $LConnectorConfigs) $LNode.addConnector(conn.create($LNode))", entry, entry, entry);
                }
            }, "initGraphNodes", TypeName.VOID, true)
        ));
    }

    @Override
    protected void process() throws Exception{
        for(var t : this.<ClassSymbol>with(GraphBlockBase.class)){
            if(graphBlockBase == null){
                graphBlockBase = t;
                for(var s : t.getEnclosedElements()){
                    if(s.getKind() == INTERFACE && name(s).equals("GraphBuild")){
                        graphEntBase = (ClassSymbol)s;
                        break;
                    }
                } if(graphEntBase == null) throw new IllegalStateException("GraphBuild not found");
            }else{
                throw new IllegalStateException("Only one type may be annotated with @GraphBlockBase");
            }
        } if(graphBlockBase == null) throw new IllegalStateException("There must be one type annotated with @GraphBlockBase");

        for(var t : this.<ClassSymbol>with(GraphBase.class)){
            if(graphBase == null){
                graphBase = t;
            }else{
                throw new IllegalStateException("Only one type may be annotated with @GraphBase");
            }
        } if(graphBase == null) throw new IllegalStateException("There must be one type annotated with @GraphBase");

        for(var t : this.<ClassSymbol>with(GraphNodeBase.class)){
            if(nodeTypeBase == null){
                nodeTypeBase = t;
                for(var s : t.getEnclosedElements()){
                    if(s.getKind() == INTERFACE && name(s).equals("GraphNodeI")){
                        nodeBase = (ClassSymbol)s;
                        break;
                    }
                } if(nodeBase == null) throw new IllegalStateException("GraphNodeI not found");
            }else{
                throw new IllegalStateException("Only one type may be annotated with @GraphNodeBase");
            }
        } if(nodeTypeBase == null) throw new IllegalStateException("There must be one type annotated with @GraphNodeBase");

        for(var t : this.<ClassSymbol>with(GraphConnectorBase.class)){
            if(connectorTypeBase == null){
                connectorTypeBase = t;
            }else{
                throw new IllegalStateException("Only one type may be annotated with @GraphConnectorBase");
            }
        } if(connectorTypeBase == null) throw new IllegalStateException("There must be one type annotated with @GraphConnectorBase");

        initImpls();
        ObjectMap<String, ClassSymbol>
            graphs = new OrderedMap<>(),
            nodes = new ObjectMap<>();

        for(var t : this.<ClassSymbol>with(GraphDef.class)) graphs.put(name(t), t);
        for(var t : this.<ClassSymbol>with(GraphNodeDef.class)) nodes.put(name(t), t);
        for(var e : graphs){
            ClassSymbol graph = e.value;
            GraphDef anno = anno(graph, GraphDef.class);

            ClassSymbol node = nodes.remove(name(type(anno::value)));
            if(node == null) throw new IllegalStateException("Invalid node class for " + name(graph) + ": " + name(type(anno::value)));

            String gn = graphName(graph);
            if(entries.containsKey(gn)) throw new IllegalStateException("Graph entry redefinitinon for " + gn);
            if(!gn.equals(nodeName(node))) throw new IllegalStateException("Graph mismatch; " + name(node) + " used for " + name(graph));

            ClassSymbol nodeEnt = null;
            for(var s : node.getEnclosedElements()){
                if(s.getKind() == CLASS || s.getKind() == INTERFACE){
                    ClassSymbol t = (ClassSymbol)s;
                    if(name(t).equals(nodeName(node) + "NodeI")){
                        nodeEnt = t;
                        break;
                    }
                }
            } if(nodeEnt == null) throw new IllegalStateException(nodeName(node) + "NodeI class not found in " + name(node));
            entries.put(gn, new GraphEntry(graph, node, nodeEnt));
        }

        TypeSpec.Builder graphsBuilder = TypeSpec.classBuilder("Graphs")
            .addModifiers(PUBLIC, FINAL)
            .addField(
                FieldSpec.builder(paramSpec(spec(IntMap.class), graphInfo), "allInfo", PUBLIC, STATIC, FINAL)
                    .initializer("new $T<>()", spec(IntMap.class))
                .build()
            )
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PRIVATE)
                    .addStatement("throw new $T()", spec(AssertionError.class))
                .build()
            )
            .addType(
                TypeSpec.classBuilder("GraphInfo")
                    .addModifiers(PUBLIC, STATIC, FINAL)
                    .addField(spec(TextureRegion.class), "icon", PUBLIC)
                    .addField(TypeName.INT, "type", PUBLIC, FINAL)
                    .addField(spec(String.class), "name", PUBLIC, FINAL)
                    .addMethod(
                        MethodSpec.constructorBuilder()
                            .addModifiers(PROTECTED)
                            .addParameter(TypeName.INT, "type")
                            .addParameter(spec(String.class), "name")
                            .addStatement("this.type = type")
                            .addStatement("this.name = name")
                        .build()
                    )
                    .addMethod(
                        MethodSpec.methodBuilder("load")
                            .addModifiers(PROTECTED)
                            .returns(TypeName.VOID)
                            .addStatement("icon = $T.atlas.find($S + name + $S)", spec(Core.class), "unity-graph-", "-icon")
                        .build()
                    )
                .build()
            );

        CodeBlock.Builder init = CodeBlock.builder();
        MethodSpec.Builder load = MethodSpec.methodBuilder("load")
            .addModifiers(PUBLIC, STATIC)
            .returns(TypeName.VOID)
            .addStatement("if($T.headless) return", spec(Vars.class));
        MethodSpec.Builder get = MethodSpec.methodBuilder("info")
            .addModifiers(PUBLIC, STATIC)
            .returns(graphInfo)
            .addParameter(TypeName.INT, "type")
            .beginControlFlow("switch(type)");

        var entryKeys = entries.orderedKeys();
        entryKeys.sort();
        for(int i = 0, len = entryKeys.size; i < len; i++){
            String name = entryKeys.get(i);
            graphsBuilder
                .addOriginatingElement(entries.get(name).graph)
                .addField(
                    FieldSpec.builder(TypeName.INT, (name = name.toLowerCase()), PUBLIC, STATIC, FINAL)
                        .initializer("1 << $L", i)
                    .build()
                )
                .addField(graphInfo, name + "Info", PUBLIC, STATIC, FINAL);

            init.addStatement("allInfo.put($L, $LInfo = new $T($L, $S))", name, name, graphInfo, name, name);
            load.addStatement("$LInfo.load()", name);
            get.addStatement("case $L: return $LInfo", name, name);
        }

        write(packageName, graphsBuilder
            .addStaticBlock(init.build())
            .addMethod(load.build())
            .addMethod(get
                .addStatement("default: throw new $T($S + type)", spec(IllegalArgumentException.class), "Invalid type: ")
                .endControlFlow()
                .build()
            ),
            null
        );

        OrderedMap<String, GraphEntry> props = new OrderedMap<>();
        ObjectSet<String> usedNames = new ObjectSet<>();

        for(var composer : with(GraphCompose.class)){
            GraphCompose compose = anno(composer, GraphCompose.class);

            props.clear();
            for(var prop : types(compose::value)){
                GraphEntry e = entries.get(graphName(prop));
                if(e == null) throw new IllegalStateException("Invalid graph type: " + name(prop));

                props.put(graphName(prop), e);
            }

            if(props.isEmpty()) throw new IllegalStateException("Must contain at least 1 graph type.");

            ClassSymbol parent = type(compose::parent);
            ClassSymbol parentBuild = null;
            for(Symbol s : parent.getEnclosedElements()){
                if(s.getKind() == CLASS){
                    ClassSymbol t = (ClassSymbol)s;
                    if(name(t).endsWith("Build")){
                        parentBuild = t;
                        break;
                    }
                }
            } if(parentBuild == null) parentBuild = conv(Building.class);

            StringBuilder prefixBuilder = new StringBuilder();
            for(String key : props.orderedKeys()) prefixBuilder.append(key);

            String prefix = prefixBuilder.toString();
            if(!usedNames.add(prefix + name(parent))) continue;

            TypeSpec.Builder builder = TypeSpec.classBuilder(prefix + name(parent))
                .superclass(spec(parent))
                .addSuperinterface(spec(graphBlockBase))
                .addModifiers(PUBLIC)
                .addAnnotation(
                    AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "{$S, $S, $S}", "all", "unchecked", "deprecation")
                    .build()
                );

            MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(spec(String.class), "name")
                .addStatement("super(name)")
                .addStatement("update = true");

            builder.addMethod(constructor.build());

            String buildName = name(parentBuild).equals("Building") ? "Build" : name(parentBuild);
            TypeSpec.Builder buildBuilder = TypeSpec.classBuilder(prefix + buildName)
                .superclass(spec(parentBuild))
                .addSuperinterface(spec(graphEntBase))
                .addModifiers(PUBLIC);

            for(var prop : props.values()){
                ClassSymbol graph = prop.graph, node = prop.node, nodeEnt = prop.nodeEnt;
                builder.addOriginatingElement(graph);
                buildBuilder.addOriginatingElement(graph);

                String name = nodeName(node).toLowerCase();
                builder
                    .addField(paramSpec(spec(node), subSpec(paramSpec(spec(graph), subSpec(spec(Object.class))))), name + "NodeConfig", PUBLIC)
                    .addField(
                        FieldSpec.builder(paramSpec(spec(Seq.class), paramSpec(spec(connectorTypeBase), subSpec(paramSpec(spec(graph), subSpec(spec(Object.class)))))), name + "ConnectorConfigs", PUBLIC)
                            .initializer("new $T<>()", spec(Seq.class))
                        .build()
                    );

                buildBuilder.addField(spec(nodeEnt), name + "Node", PUBLIC);
            }

            Implementor implementor = (impl, target) -> {
                MethodSpec.Builder methBuilder = MethodSpec.methodBuilder(impl.name)
                    .addAnnotation(spec(Override.class))
                    .addModifiers(impl.isPublic ? PUBLIC : PROTECTED)
                    .returns(impl.ret);

                for(var arg : impl.args) methBuilder.addParameter(arg.type, arg.name);
                impl.impl.get(
                    methBuilder,
                    (type, str) -> {
                        StringBuilder stat = new StringBuilder("super.")
                            .append(impl.name)
                            .append('(');

                        if(impl.args.length > 0){
                            stat.append(impl.args[0].name);
                            for(int i = 1; i < impl.args.length; i++){
                                stat.append(", ")
                                    .append(impl.args[i].name);
                            }
                        }

                        stat.append(')');
                        if(str == null){
                            methBuilder.addStatement("$L", stat.toString());
                        }else if(type == null){
                            methBuilder.addStatement("var $L = $L", str, stat.toString());
                        }else{
                            methBuilder.addStatement("$T $L = $L", type, str, stat.toString());
                        }
                    },
                    props.orderedKeys()
                );

                target.addMethod(methBuilder.build());
            };

            for(var e : blockFields.entries()){
                if(e.key.equals("Graph") || props.containsKey(e.key)){
                    e.value.each(builder::addField);
                }
            }

            for(var e : buildFields.entries()){
                if(e.key.equals("Graph") || props.containsKey(e.key)){
                    e.value.each(buildBuilder::addField);
                }
            }

            for(var e : blockImpls.entries()){
                if(e.key.equals("Graph") || props.containsKey(e.key)){
                    e.value.each(impl -> implementor.get(impl, builder));
                }
            }

            for(var e : buildImpls.entries()){
                if(e.key.equals("Graph") || props.containsKey(e.key)){
                    e.value.each(impl -> implementor.get(impl, buildBuilder));
                }
            }

            write(packageName, builder.addType(buildBuilder.build()), null);
        }
    }

    protected String graphName(ClassSymbol t){
        String name = name(t);
        if(!name.endsWith("GraphI")) throw new IllegalStateException(name + " doesn't end with 'GraphI'.");
        return name.substring(0, name.length() - 6);
    }

    protected String nodeName(ClassSymbol t){
        String name = name(t);
        if(!name.endsWith("NodeTypeI")) throw new IllegalStateException(name + " doesn't end with 'NodeTypeI'.");
        return name.substring(0, name.length() - 9);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes(){
        return Set.of(
            fName(GraphBlockBase.class),
            fName(GraphBase.class), fName(GraphNodeBase.class), fName(GraphConnectorBase.class),
            fName(GraphDef.class), fName(GraphNodeDef.class),
            fName(GraphCompose.class)
        );
    }

    protected static class GraphEntry{
        protected final ClassSymbol graph;
        protected final ClassSymbol node, nodeEnt;

        protected GraphEntry(ClassSymbol graph, ClassSymbol node, ClassSymbol nodeEnt){
            this.graph = graph;
            this.node = node;
            this.nodeEnt = nodeEnt;
        }
    }

    protected static class Implement{
        protected final Cons3<MethodSpec.Builder, Cons2<TypeName, String>, Seq<String>> impl;
        protected final String name;
        protected final TypeName ret;
        protected final boolean isPublic;
        protected final Arg[] args;

        protected Implement(Cons3<MethodSpec.Builder, Cons2<TypeName, String>, Seq<String>> impl, String name, TypeName ret, boolean isPublic, Object... args){
            this.impl = impl;
            this.name = name;
            this.ret = ret;
            this.isPublic = isPublic;

            this.args = new Arg[args.length / 2];
            for(int i = 0; i < args.length; i += 2){
                this.args[i / 2] = new Arg((TypeName)args[i], (String)args[i + 1]);
            }
        }

        protected static class Arg{
            protected final TypeName type;
            protected final String name;

            protected Arg(TypeName type, String name){
                this.type = type;
                this.name = name;
            }
        }
    }

    protected interface Implementor{
        void get(Implement impl, TypeSpec.Builder target);
    }
}
