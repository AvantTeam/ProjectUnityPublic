package unity.annotations.processors.impl;

import arc.audio.*;
import arc.struct.*;
import com.squareup.javapoet.*;
import mindustry.*;
import mindustry.ctype.*;
import unity.annotations.Annotations.*;
import unity.annotations.processors.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.util.*;

/** @author GlennFolker */
@SuppressWarnings({"unchecked"})
@SupportedAnnotationTypes({
    "unity.annotations.Annotations.FactionDef",
    "unity.annotations.Annotations.FactionBase"
})
public class FactionProcessor extends BaseProcessor{
    Seq<VariableElement> factions = new Seq<>();
    TypeElement faction;

    @Override
    public void process(RoundEnvironment roundEnv) throws Exception{
        factions.addAll((Set<VariableElement>)roundEnv.getElementsAnnotatedWith(FactionDef.class));
        if(faction == null){
            Seq<TypeElement> seq = Seq.with(roundEnv.getElementsAnnotatedWith(FactionBase.class)).map(e -> (TypeElement)e);
            if(seq.size > 1){
                throw new IllegalArgumentException("Always one type may be annotated by 'FactionBase', no more, no less");
            }

            faction = seq.first();
        }

        if(round == 1){
            processFactions();
        }
    }

    protected void processFactions() throws Exception{
        TypeSpec.Builder facMeta = TypeSpec.classBuilder("FactionMeta").addModifiers(Modifier.PUBLIC)
            .addAnnotation(
                AnnotationSpec.builder(cName(SuppressWarnings.class))
                    .addMember("value", "$S", "unchecked")
                .build()
            )
            .addJavadoc("Modifies content fields based on its {@link $T}", tName(faction))
            .addField(
                FieldSpec.builder(
                    ParameterizedTypeName.get(
                        cName(ObjectMap.class),
                        cName(Object.class),
                        tName(faction)
                    ),
                    "map",
                    Modifier.PRIVATE, Modifier.STATIC
                )
                    .addJavadoc("Maps {@link $T} with {@link $T}", cName(Object.class), tName(faction))
                    .initializer("new $T<>()", cName(ObjectMap.class))
                .build()
            )
            .addField(
                FieldSpec.builder(
                    ParameterizedTypeName.get(
                        cName(ObjectMap.class),
                        tName(Music.class),
                        tName(String.class)
                    ),
                    "music", Modifier.PRIVATE, Modifier.STATIC
                )
                    .addJavadoc("Maps {@link $T} with its category", cName(Music.class))
                    .initializer("new $T<>()", cName(ObjectMap.class))
                .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("map").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addJavadoc(
                        CodeBlock.builder()
                            .add("Gets a {@link $T} with the given content as a key" + lnew(), tName(faction))
                            .add("@param content The content object" + lnew())
                            .add("@return The {@link $T}", tName(faction))
                        .build()
                    )
                    .returns(tName(faction))
                    .addParameter(cName(Object.class), "content")
                    .addStatement("return map.get(content)")
                .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("put").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addJavadoc(
                        CodeBlock.builder()
                            .add("Puts and handles this content with the given {@link $T}" + lnew(), tName(faction))
                            .add("@param content The content object" + lnew())
                            .add("@param faction The {@link $T}", tName(faction))
                        .build()
                    )
                    .returns(TypeName.VOID)
                    .addParameter(cName(Object.class), "content")
                    .addParameter(tName(faction), "faction")
                    .addStatement("map.put(content, faction)")
                    .beginControlFlow("if(content instanceof $T unlockable)", cName(UnlockableContent.class))
                        .addStatement("unlockable.description += $S + $S + faction.localizedName", "\n", "[gray]Faction:[] ")
                    .endControlFlow()
                .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("getByFaction").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addJavadoc(
                        CodeBlock.builder()
                            .add("Returns specific {@link $T}s with the given {@link $T}" + lnew(), cName(Object.class), tName(faction))
                            .add("@param <$T> The generic type to filter" + lnew(), tvName("T"))
                            .add("@param faction The {@link $T}" + lnew(), tName(faction))
                            .add("@param type The generic type class" + lnew())
                            .add("@return {@link $T} filled with the filtered objects", cName(Seq.class))
                        .build()
                    )
                    .addTypeVariable(tvName("T"))
                    .returns(ParameterizedTypeName.get(cName(Seq.class), tvName("T")))
                    .addParameter(tName(faction), "faction")
                    .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(cName(Class.class), tvName("T")), "type").build())
                    .addStatement("$T<$T> contents = new $T<>()", cName(Seq.class), tvName("T"), cName(Seq.class))
                    .addStatement("map.keys().toSeq().select(o -> map.get(o).equals(faction) && type.isAssignableFrom(o.getClass())).each(o -> contents.add(($T)o))", tvName("T"))
                    .addCode(lnew())
                    .addStatement("return contents")
                .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("getByCtype").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addJavadoc(
                        CodeBlock.builder()
                            .add("Returns all {@link $T}s with the given {@link $T}" + lnew(), cName(Content.class), cName(ContentType.class))
                            .add("@param <$T> The generic type to filter" + lnew(), tvName("T"))
                            .add("@param ctype The {@link $T}" + lnew(), cName(ContentType.class))
                            .add("@return {@link $T} filled with the filtered objects", cName(Seq.class))
                        .build()
                    )
                    .addTypeVariable(tvName("T", cName(Content.class)))
                    .returns(ParameterizedTypeName.get(cName(Seq.class), tvName("T")))
                    .addParameter(cName(ContentType.class), "ctype")
                    .addStatement("$T<$T> contents = new $T<>()", cName(Seq.class), tvName("T"), cName(Seq.class))
                    .beginControlFlow("for($T o : map.keys().toSeq())", cName(Object.class))
                        .beginControlFlow("if(o instanceof $T c && c.getContentType().equals(ctype))", cName(Content.class))
                            .addStatement("contents.add(($T)c)", tvName("T"))
                        .endControlFlow()
                    .endControlFlow()
                    .addCode(lnew())
                    .addStatement("return contents")
                .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("getMusicCategory").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addJavadoc(
                        CodeBlock.builder()
                            .add("Gets the category of a specific {@link $T}" + lnew(), cName(Music.class))
                            .add("@param mus The {@link $T}" + lnew(), cName(Music.class))
                            .add("@return The category")
                        .build()
                    )
                    .returns(ParameterizedTypeName.get(cName(Seq.class), cName(Music.class)))
                    .addParameter(cName(Music.class), "mus")
                    .addStatement("$T category = music.get(mus)", cName(String.class))
                    .addStatement("if(category == null) return $T.control.sound.ambientMusic", cName(Vars.class))
                    .addCode(lnew())
                    .beginControlFlow("return switch(category)")
                        .addStatement("case $S -> $T.control.sound.ambientMusic", "ambient", cName(Vars.class))
                        .addStatement("case $S -> $T.control.sound.darkMusic", "dark", cName(Vars.class))
                        .addStatement("case $S -> $T.control.sound.bossMusic", "boss", cName(Vars.class))
                        .addCode(lnew())
                        .addStatement("default -> throw new $T($S + category)", cName(IllegalArgumentException.class), "Unknown category: ")
                    .endControlFlow("")
                .build()
            );

        MethodSpec.Builder initializer = MethodSpec.methodBuilder("init").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TypeName.VOID)
            .addJavadoc("Initializes all content whose fields are to be modified");

        String before = null;
        for(VariableElement e : factions){
            TypeName up = tName(e.getEnclosingElement());
            String c = e.getSimpleName().toString();
            TypeName upf = tName(faction);
            FactionDef def = annotation(e, FactionDef.class);
            String fac = def.value();

            if(before != null && !fac.equals(before)){
                initializer.addCode(lnew());
            }
            before = fac;

            initializer.addStatement("put($T.$L, $T.$L)", up, c, upf, fac);
        }

        facMeta.addMethod(initializer.build());
        write(facMeta.build());
    }
}
