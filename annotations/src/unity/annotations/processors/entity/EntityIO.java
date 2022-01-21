package unity.annotations.processors.entity;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import com.squareup.javapoet.*;
import mindustry.*;
import mindustry.ctype.*;
import unity.annotations.Annotations.*;
import unity.annotations.processors.*;
import unity.annotations.processors.util.TypeIOResolver.*;

import javax.lang.model.element.*;

public class EntityIO{
    final String name;
    final TypeSpec.Builder type;
    final ClassSerializer serializer;

    MethodSpec.Builder method;
    boolean write;

    public EntityIO(String name, TypeSpec.Builder type, ClassSerializer serializer){
        this.name = name;
        this.type = type;
        this.serializer = serializer;
    }

    public Seq<VariableElement> sel(Seq<VariableElement> fields){
        return fields.select(f ->
            !f.getModifiers().contains(Modifier.TRANSIENT) &&
                !f.getModifiers().contains(Modifier.STATIC) &&
                !f.getModifiers().contains(Modifier.FINAL)
        ).sortComparing(BaseProcessor::simpleName);
    }

    public void write(BaseProcessor proc, MethodSpec.Builder method, boolean write, Seq<VariableElement> fields){
        this.method = method;
        this.write = write;

        for(VariableElement e : sel(fields)){
            io(proc, e.asType().toString(), "this." + BaseProcessor.simpleName(e) + (write ? "" : " = "));
        }
    }

    public void writeSync(BaseProcessor proc, MethodSpec.Builder method, boolean write, Seq<VariableElement> syncFields, Seq<VariableElement> allFields){
        this.method = method;
        this.write = write;

        if(write){
            for(VariableElement e : sel(allFields)){
                io(proc, e.asType().toString(), "this." + BaseProcessor.simpleName(e));
            }
        }else{
            st("if(lastUpdated != 0) updateSpacing = $T.timeSinceMillis(lastUpdated)", Time.class);
            st("lastUpdated = $T.millis()", Time.class);
            st("boolean islocal = isLocal()");

            for(VariableElement e : sel(allFields)){
                boolean sf = BaseProcessor.annotation(e, SyncField.class) != null;
                boolean sl = BaseProcessor.annotation(e, SyncLocal.class) != null;

                if(sl) cont("if(!islocal)");

                if(sf){
                    st(BaseProcessor.simpleName(e) + "_LAST_" + " = this." + BaseProcessor.simpleName(e));
                }

                io(proc, e.asType().toString(), "this." + (sf ? BaseProcessor.simpleName(e) + "_TARGET_" : BaseProcessor.simpleName(e)) + " = ");

                if(sl){
                    ncont("else" );

                    io(proc, e.asType().toString(), "");

                    if(sf){
                        st(BaseProcessor.simpleName(e) + "_LAST_" + " = this." + BaseProcessor.simpleName(e));
                        st(BaseProcessor.simpleName(e) + "_TARGET_" + " = this." + BaseProcessor.simpleName(e));
                    }

                    econt();
                }
            }

            st("afterSync()");
        }
    }

    public void writeSyncManual(MethodSpec.Builder method, boolean write, Seq<VariableElement> syncFields) throws Exception{
        this.method = method;
        this.write = write;

        if(write){
            for(VariableElement field : syncFields){
                st("buffer.put(this.$L)", BaseProcessor.simpleName(field));
            }
        }else{
            st("if(lastUpdated != 0) updateSpacing = $T.timeSinceMillis(lastUpdated)", Time.class);
            st("lastUpdated = $T.millis()", Time.class);

            for(VariableElement field : syncFields){
                st("this.$L = this.$L", BaseProcessor.simpleName(field) + "_LAST_", BaseProcessor.simpleName(field));
                st("this.$L = buffer.get()", BaseProcessor.simpleName(field) + "_TARGET_");
            }
        }
    }

    public void writeInterpolate(MethodSpec.Builder method, Seq<VariableElement> fields){
        this.method = method;

        cont("if(lastUpdated != 0 && updateSpacing != 0)");

        st("float timeSinceUpdate = Time.timeSinceMillis(lastUpdated)");
        st("float alpha = Math.min(timeSinceUpdate / updateSpacing, 2f)");

        for(VariableElement field : fields){
            String name = BaseProcessor.simpleName(field);
            String targetName = name + "_TARGET_";
            String lastName = name + "_LAST_";
            st("$L = $L($T.$L($L, $L, alpha))", name, BaseProcessor.annotation(field, SyncField.class).clamped() ? "arc.math.Mathf.clamp" : "", BaseProcessor.cName(Mathf.class), BaseProcessor.annotation(field, SyncField.class).value() ? "lerp" : "slerp", lastName, targetName);
        }

        ncont("else if(lastUpdated != 0)");

        for(VariableElement field : fields){
            st("$L = $L", BaseProcessor.simpleName(field), BaseProcessor.simpleName(field) + "_TARGET_");
        }

        econt();
    }

    public void io(BaseProcessor proc, String type, String field){
        type = type.replace("mindustry.gen.", "").replace("unity.gen.", "");

        if(BaseProcessor.isPrimitive(type)){
            s(type.equals("boolean") ? "bool" : type.charAt(0) + "", field);
        }else if(proc.instanceOf(type, "mindustry.ctype.Content")){
            if(write){
                s("s", field + ".id");
            }else{
                st(field + "$T.content.getByID($T.$L, read.s())", BaseProcessor.cName(Vars.class), BaseProcessor.cName(ContentType.class), BaseProcessor.simpleName(type).toLowerCase().replace("type", ""));
            }
        }else if(serializer.writers.containsKey(type) && write){
            st("$L(write, $L)", serializer.writers.get(type), field);
        }else if(serializer.mutatorReaders.containsKey(type) && !write && !field.replace(" = ", "").contains(" ") && !field.isEmpty()){
            st("$L$L(read, $L)", field, serializer.mutatorReaders.get(type), field.replace(" = ", ""));
        }else if(serializer.readers.containsKey(type) && !write){
            st("$L$L(read)", field, serializer.readers.get(type));
        }else if(type.endsWith("[]")){
            String rawType = type.substring(0, type.length() - 2);

            if(write){
                s("i", field + ".length");
                cont("for(int INDEX = 0; INDEX < $L.length; INDEX ++)", field);
                io(proc, rawType, field + "[INDEX]");
            }else{
                String fieldName = field.replace(" = ", "").replace("this.", "");
                String lenf = fieldName + "_LENGTH";
                s("i", "int " + lenf + " = ");
                if(!field.isEmpty()){
                    st("$Lnew $L[$L]", field, type.replace("[]", ""), lenf);
                }
                cont("for(int INDEX = 0; INDEX < $L; INDEX ++)", lenf);
                io(proc, rawType, field.replace(" = ", "[INDEX] = "));
            }

            econt();
        }else if(type.startsWith("arc.struct") && type.contains("<")){ //it's some type of data structure
            String struct = type.substring(0, type.indexOf("<"));
            String generic = type.substring(type.indexOf("<") + 1, type.indexOf(">"));

            if(struct.equals("arc.struct.Queue") || struct.equals("arc.struct.Seq")){
                if(write){
                    s("i", field + ".size");
                    cont("for(int INDEX = 0; INDEX < $L.size; INDEX ++)", field);
                    io(proc, generic, field + ".get(INDEX)");
                }else{
                    String fieldName = field.replace(" = ", "").replace("this.", "");
                    String lenf = fieldName + "_LENGTH";
                    s("i", "int " + lenf + " = ");
                    if(!field.isEmpty()){
                        st("$L.clear()", field.replace(" = ", ""));
                    }
                    cont("for(int INDEX = 0; INDEX < $L; INDEX ++)", lenf);
                    io(proc, generic, field.replace(" = ", "_ITEM = ").replace("this.", generic + " "));
                    if(!field.isEmpty()){
                        String temp = field.replace(" = ", "_ITEM").replace("this.", "");
                        st("if($L != null) $L.add($L)", temp, field.replace(" = ", ""), temp);
                    }
                }

                econt();
            }else{
                Log.warn("Missing serialization code for collection '@' in '@'", type, name);
            }
        }else{
            Log.warn("Missing serialization code for type '@' in '@'", type, name);
        }
    }

    public void cont(String text, Object... fmt){
        method.beginControlFlow(text, fmt);
    }

    public void econt(){
        method.endControlFlow();
    }

    public void ncont(String text, Object... fmt){
        method.nextControlFlow(text, fmt);
    }

    public void st(String text, Object... args){
        method.addStatement(text, args);
    }

    public void s(String type, String field){
        if(write){
            method.addStatement("write.$L($L)", type, field);
        }else{
            method.addStatement("$Lread.$L()", field, type);
        }
    }
}
