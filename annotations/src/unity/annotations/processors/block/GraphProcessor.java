package unity.annotations.processors.block;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
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

    protected ClassSymbol graphBase, graphEntBase;
    protected ClassSymbol connectorBase;
    protected OrderedMap<String, GraphEntry> entries = new OrderedMap<>();

    /** (Modify as needed!) Fields that are present in implementations of GraphBlock. */
    protected static final Seq<FieldSpec> blockFields = Seq.with();

    /** (Modify as needed!) Fields that are present in implementations of GraphBuild. */
    protected static final Seq<FieldSpec> buildFields = Seq.with(
        FieldSpec.builder(TypeName.INT, "prevTileRotation").initializer("-1").build(),
        FieldSpec.builder(TypeName.BOOLEAN, "placed").initializer("false").build()
    );

    /**
     * (Modify as needed!) Boilerplate methods that are overriden to interop with the graph system in implementations
     * of GraphBlock. These methods are only added to "root" graph blocks, as in graph block classes that do not
     * derive from other graph block classes.
     */
    protected static final Seq<Implement> blockInjects = Seq.with(
        
    );

    /**
     * (Modify as needed!) Boilerplate methods that are overriden to interop with the graph system in implementations
     * of GraphBuild. These methods are only added to "root" graph builds, as in graph build classes that do not
     * derive from other graph build classes.
     */
    protected static final Seq<Implement> buildInjects = Seq.with(
        
    );

    /**
     * (Modify as needed!) Implementations for the non-default methods in the graph and related interfaces. These are
     * present in every generated graph block composition. Super-calls aren't allowed.
     */
    protected static final Seq<Implement> blockImpls = Seq.with(

    );

    /**
     * (Modify as needed!) Implementations for the non-default methods in the graph and related interfaces. These are
     * present in every generated graph build composition. Super-calls aren't allowed.
     */
    protected static final Seq<Implement> buildImpls = Seq.with(

    );

    {
        rounds = 1;
    }

    @Override
    protected void process() throws Exception{
        for(var t : this.<ClassSymbol>with(GraphBase.class)){
            if(graphBase == null){
                graphBase = t;
                for(var s : t.getEnclosedElements()){
                    if(s.getKind() == INTERFACE && name(s).equals("GraphBuild")){
                        graphEntBase = (ClassSymbol)s;
                        break;
                    }
                } if(graphEntBase == null) throw new IllegalStateException("GraphBuild not found");
            }else{
                throw new IllegalStateException("Only one type may be annotated with @GraphBase");
            }
        } if(graphBase == null) throw new IllegalStateException("There must be one type annotated with @GraphBase");

        for(var t : this.<ClassSymbol>with(GraphConnectorBase.class)){
            if(connectorBase == null){
                connectorBase = t;
            }else{
                throw new IllegalStateException("Only one type may be annotated with @GraphConnectorBase");
            }
        } if(connectorBase == null) throw new IllegalStateException("There must be one type annotated with @GraphConnectorBase");

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

        var graphInfo = ClassName.get(packageName, "Graphs", "GraphInfo");
        TypeSpec.Builder graphsBuilder = TypeSpec.classBuilder("Graphs")
            .addModifiers(PUBLIC, FINAL)
            .addField(
                FieldSpec.builder(paramSpec(spec(LongMap.class), graphInfo), "allInfo", PUBLIC, STATIC, FINAL)
                    .initializer("new $T<>()", spec(LongMap.class))
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
                    .addField(TypeName.LONG, "type", PUBLIC, FINAL)
                    .addField(spec(String.class), "name", PUBLIC, FINAL)
                    .addMethod(
                        MethodSpec.constructorBuilder()
                            .addModifiers(PROTECTED)
                            .addParameter(TypeName.LONG, "type")
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
            .addParameter(TypeName.LONG, "type")
            .beginControlFlow("return switch(type)");

        var entryKeys = entries.orderedKeys();
        entryKeys.sort();
        for(int i = 0, len = entryKeys.size; i < len; i++){
            String name = entryKeys.get(i);
            graphsBuilder
                .addOriginatingElement(entries.get(name).graph)
                .addField(
                    FieldSpec.builder(TypeName.LONG, (name = name.toLowerCase()), PUBLIC, STATIC, FINAL)
                        .initializer("1 << $L", i)
                    .build()
                )
                .addField(graphInfo, name + "Info", PUBLIC, STATIC, FINAL);

            init.addStatement("allInfo.put($L, $LInfo = new $T($L, $S))", name, name, graphInfo, name, name);
            load.addStatement("$LInfo.load()", name);
            get.addStatement("case $L -> $LInfo", name, name);
        }

        write(packageName, graphsBuilder
            .addStaticBlock(init.build())
            .addMethod(load.build())
            .addMethod(get
                .addStatement("default -> throw new $T($S + type)", spec(IllegalArgumentException.class), "Invalid flag: ")
                .endControlFlow("")
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

            boolean isRoot = true;
            for(Type type = parent.getSuperclass();
                type != Type.noType && type.tsym != null;
                type = ((ClassSymbol)type.tsym).getSuperclass())
            {
                var def = anno(type.tsym, GraphDef.class);
                if(def != null){
                    isRoot = false;

                    int i = 0;
                    for(var prop : types(def::value)){
                        String name = graphName(prop);

                        GraphEntry e = entries.get(name);
                        if(e == null) throw new IllegalStateException("Invalid graph type: " + name(prop));

                        if(!props.containsKey(name)){
                            props.put(name, e);

                            var keys = props.orderedKeys();
                            keys.remove(keys.size - 1);
                            keys.insert(i++, name);
                        }
                    }
                }
            }

            StringBuilder prefixBuilder = new StringBuilder();
            for(String key : props.orderedKeys()) prefixBuilder.append(key);

            String prefix = prefixBuilder.toString();
            if(!usedNames.add(prefix + name(parent))) continue;

            TypeSpec.Builder builder = TypeSpec.classBuilder(prefix + name(parent))
                .superclass(spec(parent))
                .addSuperinterface(spec(graphBase))
                .addModifiers(PUBLIC)
                .addAnnotation(
                    AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "{$S, $S, $S}", "all", "unchecked", "deprecation")
                    .build()
                );

            MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(spec(String.class), "name")
                .addStatement("super(name)");

            if(isRoot) constructor.addStatement("update = true");
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
                    .addField(spec(node), name + "NodeConfig", PUBLIC)
                    .addField(
                        FieldSpec.builder(paramSpec(spec(Seq.class), subSpec(spec(connectorBase))), name + "ConnectorConfigs", PUBLIC)
                            .initializer("new $T<>()", spec(Seq.class))
                        .build()
                    );

                buildBuilder.addField(spec(nodeEnt), name + "Node", PUBLIC);
            }

            Implementor implementor = (impl, target, allowSuper) -> {
                MethodSpec.Builder methBuilder = MethodSpec.methodBuilder(impl.name)
                    .addAnnotation(spec(Override.class))
                    .addModifiers(impl.isPublic ? PUBLIC : PROTECTED)
                    .returns(impl.ret);

                for(var arg : impl.args) methBuilder.addParameter(arg.type, arg.name);
                impl.impl.get(
                    methBuilder,
                    allowSuper ? str -> {
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

                        methBuilder.addStatement("$L", stat.append(')').toString());
                    } : str -> { throw new IllegalStateException("Super-calling isn't allowed."); },
                    props.orderedKeys()
                );

                target.addMethod(methBuilder.build());
            };

            if(isRoot){
                blockFields.each(builder::addField);
                buildFields.each(buildBuilder::addField);

                blockInjects.each(impl -> implementor.get(impl, builder, true));
                buildInjects.each(impl -> implementor.get(impl, buildBuilder, true));
            }

            blockImpls.each(impl -> implementor.get(impl, builder, false));
            buildImpls.each(impl -> implementor.get(impl, buildBuilder, false));

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
            fName(GraphBase.class),
            fName(GraphDef.class),
            fName(GraphNodeDef.class),
            fName(GraphConnectorBase.class),
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
        protected final Cons3<MethodSpec.Builder, Cons<String>, Seq<String>> impl;
        protected final String name;
        protected final TypeName ret;
        protected final boolean isPublic;
        protected final Arg[] args;

        protected Implement(Cons3<MethodSpec.Builder, Cons<String>, Seq<String>> impl, String name, TypeName ret, boolean isPublic, Object... args){
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
        void get(Implement impl, TypeSpec.Builder target, boolean allowSuper);
    }
}
