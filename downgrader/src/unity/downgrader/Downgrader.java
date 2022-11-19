package unity.downgrader;

import arc.util.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Source.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.*;
import java.util.*;

import static com.sun.tools.javac.code.Source.Feature.*;

/**
 * Alternative to Jabel (which takes a really long time to start); faster, but potentially unreliable. Might not work if some
 * illegal access operations aren't permitted.
 * @author GlennFolker
 */
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("*")
public class Downgrader extends AbstractProcessor{
    static{
        try{
            // Get the trusted private lookup.
            Lookup lookup = Reflect.get(Lookup.class, "IMPL_LOOKUP");
            // Get the minimum level setter, to force certain features to qualify as a J8 feature.
            MethodHandle set = lookup.findSetter(Feature.class, "minLevel", Source.class);

            // Downgrade most J8-compatible features.
            for(Feature feature : new Feature[]{
                EFFECTIVELY_FINAL_VARIABLES_IN_TRY_WITH_RESOURCES,
                PRIVATE_SAFE_VARARGS,
                DIAMOND_WITH_ANONYMOUS_CLASS_CREATION,
                LOCAL_VARIABLE_TYPE_INFERENCE,
                VAR_SYNTAX_IMPLICIT_LAMBDAS,
                SWITCH_MULTIPLE_CASE_LABELS,
                SWITCH_RULE,
                SWITCH_EXPRESSION,
                TEXT_BLOCKS,
                PATTERN_MATCHING_IN_INSTANCEOF,
                REIFIABLE_TYPES_INSTANCEOF
            }) set.invokeExact(feature, Source.JDK8);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        return false;
    }
}
