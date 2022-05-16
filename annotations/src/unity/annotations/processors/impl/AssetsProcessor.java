package unity.annotations.processors.impl;

import arc.*;
import arc.assets.loaders.SoundLoader.*;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import com.squareup.javapoet.*;
import mindustry.*;
import unity.annotations.processors.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;

/** @author GlennFolker */
@SuppressWarnings("unused")
@SupportedAnnotationTypes("java.lang.Override")
public class AssetsProcessor extends BaseProcessor{
    Seq<Asset> assets = new Seq<>();

    {
        rounds = 2;
    }

    @Override
    public void process(RoundEnvironment roundEnv) throws Exception{
        if(round == 1){
            assets.clear().addAll(
                // Sounds
                new Asset(){
                    @Override
                    public TypeElement type(){
                        return toType(Sound.class);
                    }

                    @Override
                    public String directory(){
                        return "sounds";
                    }

                    @Override
                    public String name(){
                        return "UnitySounds";
                    }

                    @Override
                    public boolean valid(Fi file){
                        return file.extEquals("ogg") || file.extEquals("mp3");
                    }

                    @Override
                    public void load(MethodSpec.Builder builder){
                        builder.addStatement("var n = $S + name", directory() + "/")
                            .addStatement("var path = $T.tree.get(n + $S).exists() ? n + $S : n + $S", cName(Vars.class), ".ogg", ".ogg", ".mp3")
                            .addCode(lnew())
                            .addStatement("var sound = new $T()", cName(Sound.class))
                            .addCode(lnew())
                            .addStatement("var desc = $T.assets.load(path, $T.class, new $T(sound))", cName(Core.class), cName(Sound.class), cName(SoundParameter.class))
                            .addStatement("desc.errored = e -> $T.err(($T)e)", cName(Log.class), cName(Throwable.class))
                            .addCode(lnew())
                            .addStatement("return sound");
                    }
                }
            );
        }else if(round == 2){
            for(Asset a : assets){
                TypeElement type = a.type();

                TypeSpec.Builder spec = TypeSpec.classBuilder(a.name()).addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(
                        MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE)
                            .addStatement("throw new $T()", cName(AssertionError.class))
                        .build()
                    );

                MethodSpec.Builder specLoad = MethodSpec.methodBuilder("load").addModifiers(Modifier.PROTECTED, Modifier.STATIC)
                    .returns(tName(type))
                    .addParameter(cName(String.class), "name");

                a.load(specLoad);
                spec.addMethod(specLoad.build());

                MethodSpec.Builder globalLoad = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(TypeName.VOID)
                    .addStatement("if($T.headless) return", cName(Vars.class));

                boolean useProp = a.properties();

                Fi propFile = rootDir.child("main/assets/" + a.directory() + "/" + a.propertyFile());
                Log.info("Asset:"+"main/assets/" + a.directory() + "/" + a.propertyFile());
                ObjectMap<String, String> temp = null;
                if(useProp) PropertiesUtils.load(temp = new ObjectMap<>(), propFile.reader());

                ObjectMap<String, String> properties = temp; // Implicitly final, for use in lambda statements

                String dir = "main/assets/" + a.directory();
                rootDir.child(dir).walk(path -> {
                    if((a.properties() && path.equals(propFile)) || !a.valid(path)) return;

                    String p = path.absolutePath();
                    String name = p.substring(p.lastIndexOf(dir) + dir.length() + 1);
                    String fieldName = Strings.kebabToCamel(path.nameWithoutExtension());
                    int ex = path.extension().length() + 1;

                    spec.addField(
                        FieldSpec.builder(tName(type), fieldName)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .initializer(a.initializer())
                        .build()
                    );

                    String stripped = name.substring(0, name.length() - ex);
                    globalLoad.addStatement("$L = load($S)", fieldName, stripped);

                    if(a.properties()){
                        Seq<String> props = properties.keys().toSeq().select(prop -> prop.split("\\.")[1].equals(stripped));
                        for(String prop : props){
                            String field = prop.split("\\.")[2];
                            String val = properties.get(prop);

                            if(!val.startsWith("[")){
                                globalLoad.addStatement("$L.$L = $L", fieldName, field, val);
                            }else{
                                Seq<String> rawargs = Seq.with(val.substring(1, val.length() - 1).split("\\s*,\\s*"));
                                String format = rawargs.remove(0);

                                Seq<Object> args = rawargs.map(elements::getTypeElement);
                                args.insert(0, fieldName);
                                args.insert(1, field);

                                globalLoad.addStatement("$L.$L = " + format, args.toArray());
                            }
                        }
                    }
                });

                spec.addMethod(globalLoad.build());
                write(spec.build());
            }
        }
    }

    interface Asset{
        /** @return The type of the asset */
        TypeElement type();

        /** @return The asset directory, must not be surrounded with {@code /} */
        String directory();

        /** @return The class name */
        String name();

        /** @return Whether to apply custom properties to the asset */
        default boolean properties(){
            return false;
        }

        /**
         * @return The property tile, looked up if {@link #properties()} is true. This file's path is relative to
         * {@link #directory()} and must not be surrounded with {@code /}
         */
        default String propertyFile(){
            return "";
        }

        /** File checker, use to prevent unrelated files getting parsed into assets */
        boolean valid(Fi file);

        /** Method builder for asset loading */
        void load(MethodSpec.Builder builder);

        default CodeBlock initializer(){
            return CodeBlock.builder().addStatement("new $T()", tName(type())).build();
        }
    }
}
