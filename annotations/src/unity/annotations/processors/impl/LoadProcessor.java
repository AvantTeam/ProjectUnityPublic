package unity.annotations.processors.impl;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import com.squareup.javapoet.*;
import unity.annotations.Annotations.*;
import unity.annotations.processors.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.lang.annotation.*;

/** @author GlennFolker */
@SupportedAnnotationTypes("unity.annotations.Annotations.LoadRegs")
public class LoadProcessor extends BaseProcessor{
    {
        rounds = 1;
    }

    @Override
    public void process(RoundEnvironment roundEnv) throws Exception{
        if(round == 1){
            TypeSpec outline = TypeSpec.annotationBuilder("Outline").addModifiers(Modifier.PUBLIC)
                .addAnnotation(
                    AnnotationSpec.builder(cName(Target.class))
                        .addMember("value", "$T.$L", cName(ElementType.class), "FIELD")
                    .build()
                )
                .addAnnotation(
                    AnnotationSpec.builder(cName(Retention.class))
                        .addMember("value", "$T.$L", cName(RetentionPolicy.class), "RUNTIME")
                    .build()
                )
                .addMethod(
                    MethodSpec.methodBuilder("color")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(cName(String.class))
                        .build()
                )
                .addMethod(
                    MethodSpec.methodBuilder("radius")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(TypeName.INT)
                        .build()
                )
                .build();

            TypeSpec.Builder spec = TypeSpec.classBuilder("Regions").addModifiers(Modifier.PUBLIC)
                .addJavadoc("Generic texture regions")
                .addType(outline);

            MethodSpec.Builder load = MethodSpec.methodBuilder("load").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Loads the texture regions");

            ObjectSet<String> processed = new ObjectSet<>();
            for(Element e : roundEnv.getElementsAnnotatedWith(LoadRegs.class)){
                LoadRegs ann = annotation(e, LoadRegs.class);

                for(String reg : ann.value()){
                    if(!processed.add(reg)) continue;

                    String name = Strings.kebabToCamel(reg);

                    spec.addField(
                        FieldSpec.builder(
                            cName(TextureRegion.class),
                            name + "Region",
                            Modifier.PUBLIC, Modifier.STATIC
                        ).build()
                    );
                    load.addStatement("$L = $T.atlas.find($S)", name + "Region", cName(Core.class), "unity-" + reg);

                    if(ann.outline()){
                        spec.addField(
                            FieldSpec.builder(
                                cName(TextureRegion.class),
                                name + "OutlineRegion",
                                Modifier.PUBLIC, Modifier.STATIC
                            )
                                .addAnnotation(
                                    AnnotationSpec.builder(ClassName.get(packageName, "Regions", outline.name))
                                        .addMember("color", "$S", ann.outlineColor())
                                        .addMember("radius", "$L", ann.outlineRadius())
                                        .build()
                                )
                                .build()
                        );

                        load.addStatement("$L = $T.atlas.find($S)", name + "OutlineRegion", cName(Core.class), "unity-" + reg + "-outline");
                    }
                }
            }

            write(spec
                .addMethod(load.build())
                .build()
            );
        }
    }
}
