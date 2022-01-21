package unity.annotations.processors.impl;

import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import com.squareup.javapoet.*;
import com.sun.source.tree.*;
import unity.annotations.Annotations.*;
import unity.annotations.Annotations.StructField.*;
import unity.annotations.processors.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.util.*;

/**
 * @author Anuke
 * @author GlennFolker
 */
@SuppressWarnings("unchecked")
@SupportedAnnotationTypes({
    "unity.annotations.Annotations.Struct",
    "unity.annotations.Annotations.StructWrap"
})
public class StructProcessor extends BaseProcessor{
    ObjectMap<Element, OrderedMap<VariableElement, SInfo>> structs = new ObjectMap<>();

    {
        rounds = 2;
    }

    @Override
    public void process(RoundEnvironment roundEnv) throws Exception{
        if(round == 1){ // Round 1: Register all struct informations
            // Gather all struct type and wrappers
            Seq<TypeElement> defs = Seq.with((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(Struct.class));
            Seq<VariableElement> wrappers = Seq.with((Set<VariableElement>)roundEnv.getElementsAnnotatedWith(StructWrap.class));

            for(TypeElement e : defs){
                if(!simpleName(e).endsWith("Struct")) throw new IllegalArgumentException(e + ": All @Struct types name must end with 'Struct;");

                OrderedMap<VariableElement, SInfo> fields = new OrderedMap<>();
                for(VariableElement v : vars(e)){
                    StructField anno = annotation(v, StructField.class);
                    FloatPacker pack = anno == null ? FloatPacker.def : anno.packer();

                    TypeKind kind = v.asType().getKind();
                    int defSize = sizeOf(kind);
                    int size = anno == null ? defSize : kind == TypeKind.FLOAT ? pack.size : anno.value() <= 0 ? defSize : anno.value();

                    fields.put(v, new SInfo(typeOf(kind), size, pack));
                }

                structs.put(e, fields);
            }

            for(VariableElement e : wrappers){
                StructWrap anno = annotation(e, StructWrap.class);
                if(anno == null) continue;

                boolean left = anno.left();

                StructField[] vals = anno.value();
                TypeElement type = toEl(e.asType());

                OrderedMap<VariableElement, SInfo> fields = new OrderedMap<>();
                for(int i = left ? (vals.length - 1) : 0; left ? i >= 0 : i < vals.length; i += left ? -1 : 1){
                    StructField val = vals[i];

                    String fname = val.name();
                    FloatPacker pack = val.packer();

                    // Must be a public instance field
                    VariableElement field = vars(type).find(f -> simpleName(f).equals(fname));
                    if(field == null) throw new IllegalArgumentException(type + "#" + fname + " does not exist");
                    if(!is(field, Modifier.PUBLIC) || is(field, Modifier.STATIC)) throw new IllegalArgumentException(type + "#" + fname + " must be an instance public field");

                    TypeKind kind = field.asType().getKind();
                    int defSize = sizeOf(kind);
                    int size = kind == TypeKind.FLOAT ? pack.size : val.value() <= 0 ? defSize : val.value();

                    if(defSize < size) throw new IllegalArgumentException(kind + ": Size can't be greater than " + defSize + ": " + size);
                    if(kind == TypeKind.BOOLEAN && size != 1) throw new IllegalArgumentException(kind + ": Size must be 1");

                    fields.put(field, new SInfo(typeOf(kind), size, pack));
                }

                structs.put(e, fields);
            }
        }else if(round == 2){ // Round 2: Generate struct classes with bit shifting and optional additional methods
            Seq<TypeSpec> specs = new Seq<>();

            structs.each((e, infos) -> {
                String cname = "";
                if(e instanceof VariableElement){
                    cname = "S" + simpleName(toEl(e.asType()));
                }else if(e instanceof TypeElement){
                    cname = simpleName(e);
                    cname = cname.substring(0, cname.length() - "Struct".length());
                }

                TypeSpec.Builder builder = TypeSpec.classBuilder(cname).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                Class<?> structType;
                int structSize = infos.values().toSeq().sum(i -> i.size);
                if(structSize <= 8){
                    structType = byte.class;
                }else if(structSize <= 16){
                    structType = short.class;
                }else if(structSize <= 32){
                    structType = int.class;
                }else if(structSize <= 64){
                    structType = long.class;
                }else{
                    throw new IllegalStateException("Struct size cannot go over 64");
                }

                int structTotalSize = structSize <= 8 ? 8 : structSize <= 16 ? 16 : structSize <= 32 ? 32 : 64;
                String structParam = cname.toLowerCase(Locale.ROOT);

                StringBuilder cons = new StringBuilder();
                MethodSpec.Builder constructor = MethodSpec.methodBuilder("construct")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(structType);

                int offset = 0;
                for(Entry<VariableElement, SInfo> entry : infos.entries()){
                    VariableElement f = entry.key;
                    SInfo info = entry.value;

                    TypeName ftype = tName(f);
                    String fname = simpleName(f);

                    constructor.addParameter(ftype, fname);

                    MethodSpec.Builder getter = MethodSpec.methodBuilder(fname)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ftype)
                        .addParameter(structType, structParam);

                    MethodSpec.Builder setter = MethodSpec.methodBuilder(fname)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(structType)
                        .addParameter(structType, structParam).addParameter(ftype, "value");

                    if(ftype == TypeName.BOOLEAN){
                        getter.addStatement("return ($L & (1L << $L)) != 0", structParam, offset);
                    }else if(ftype == TypeName.FLOAT){
                        getter.addStatement("return " + info.pack.unpacker.get("(int)(($L >>> $L) & $L)"), structParam, offset, bitString(info.size, structTotalSize));
                    }else{
                        getter.addStatement("return ($T)(($L >>> $L) & $L)", ftype, structParam, offset, bitString(info.size, structTotalSize));
                    }

                    if(ftype == TypeName.BOOLEAN){
                        cons.append(" | (").append(fname).append(" ? ").append("1L << ").append(offset).append("L : 0)");

                        setter.beginControlFlow("if(!value)")
                            .addStatement("return ($T)(($L & ~(1L << $LL)))", structType, structParam, offset)
                        .nextControlFlow("else")
                            .addStatement("return ($T)(($L & ~(1L << $LL)) | (1L << $LL))", structType, structParam, offset, offset)
                        .endControlFlow();
                    }else if(ftype == TypeName.FLOAT){
                        cons.append(" | ((").append(structType).append(")")
                            .append(info.pack.packer.get(fname))
                            .append(" << ").append(offset).append("L)");

                        setter.addStatement("return ($T)(($L & ~$L) | (($T)" + info.pack.packer.get("value") + " << $LL))", structType, structParam, bitString(offset, info.size, structTotalSize), structType, offset);
                    }else{
                        cons.append(" | (")
                            .append("(")
                                .append("(").append(structType).append(")")
                                .append(fname).append(" << ").append(offset).append("L")
                            .append(")").append(" & ").append(bitString(offset, info.size, structTotalSize))
                        .append(")");

                        setter.addStatement("return ($T)(($L & ~$L) | (($T)value << $LL))", structType, structParam, bitString(offset, info.size, structTotalSize), structType, offset);
                    }

                    builder.addMethod(getter.build());
                    builder.addMethod(setter.build());

                    offset += info.size;
                }

                constructor.addStatement("return ($T)($L)", structType, cons.substring(3));

                StructWrap anno = annotation(e, StructWrap.class);
                if(anno != null){
                    if(anno.left()) Collections.reverse(constructor.parameters);

                    MethodSpec.Builder shortConst = MethodSpec.methodBuilder("construct")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(structType)
                        .addParameter(tName(e), structParam);

                    StringBuilder format = new StringBuilder("return construct(");

                    Seq<VariableElement> params = infos.orderedKeys();
                    if(anno.left()){
                        params = Seq.with(params);
                        params.reverse();
                    }

                    for(int i = 0; i < params.size; i++){
                        if(i > 0) format.append(", ");
                        format.append(structParam)
                            .append(".")
                            .append(simpleName(params.get(i)));
                    }
                    format.append(")");

                    shortConst.addStatement(format.toString());
                    builder.addMethod(shortConst.build());

                    TypeElement type = toEl(e.asType());

                    VariableTree init = (VariableTree)trees.getTree(e);
                    builder.addField(
                        FieldSpec.builder(tName(type), "STRUCT_LOCK")
                            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer(init == null ? "new $T()" : init.getInitializer() == null ? "new $T()" : init.getInitializer().toString(), tName(type))
                        .build()
                    );

                    for(ExecutableElement m : methods(type)){
                        Seq<? extends VariableElement> mparams = Seq.with(m.getParameters());
                        if(
                            isConstructor(m) ||
                            !is(m, Modifier.PUBLIC) ||
                            is(m, Modifier.STATIC, Modifier.NATIVE) ||

                            // Try to avoid built-in getter/setters
                            params.contains(p ->
                                // Like in Color, this will ignore r(float), g(float), or such
                                // Or getX() and getY() in Vec2
                                (
                                    simpleName(p).equals(simpleName(m)) ||
                                    ("get" + Strings.capitalize(simpleName(p))).equals(simpleName(m)) ||
                                    ("set" + Strings.capitalize(simpleName(p))).equals(simpleName(m))
                                ) &&
                                (
                                    (m.getParameters().size() == 0 && types.isSameType(p.asType(), m.getReturnType())) ||
                                    (m.getParameters().size() == 1 && types.isSameType(p.asType(), m.getParameters().get(0).asType()))
                                )
                            ) ||

                            // Try to avoid copy function, value-types are copied between function passes anyway
                            ((simpleName(m).equals("cpy") || simpleName(m).equals("copy")) && m.getParameters().size() == 0) ||

                            // Try to avoid functions with params as the declared type representative of this struct
                            mparams.contains(p -> types.isSameType(type.asType(), compOf(p.asType())))
                        ) continue;

                        // Call construct() to recreate the struct type, if the return type is the actual type
                        boolean returns = m.getReturnType().getKind() != TypeKind.VOID;
                        boolean reinterpret = types.isSameType(m.getReturnType(), type.asType());

                        MethodSpec.Builder method = MethodSpec.methodBuilder(simpleName(m))
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .addParameter(structType, structParam)
                            .beginControlFlow("synchronized(STRUCT_LOCK)");

                        if(returns){
                            if(reinterpret){
                                method.returns(structType);
                            }else{
                                method.returns(TypeName.get(m.getReturnType()));
                            }
                        }

                        for(VariableElement p : mparams){
                            method.addParameter(tName(p), "p" + simpleName(p));
                        }

                        for(VariableElement p : params){
                            method.addStatement("STRUCT_LOCK.$L = $L($L)", simpleName(p), simpleName(p), structParam);
                        }
                        method.addCode(lnew());

                        StringBuilder call = new StringBuilder("STRUCT_LOCK." + simpleName(m) + "(");

                        for(int i = 0; i < mparams.size; i++){
                            if(i > 0) call.append(", ");
                            call.append("p").append(simpleName(mparams.get(i)));
                        }
                        call.append(")");

                        if(returns){
                            method.addStatement("return " + (reinterpret ? "construct(" : "") + call + (reinterpret ? ")" : ""));
                        }else{
                            method.addStatement(call.toString());
                        }

                        method.endControlFlow();
                        builder.addMethod(method.build());
                    }
                }

                builder.addMethod(constructor.build());
                specs.add(builder.build());
            });

            for(TypeSpec spec : specs) write(spec);
        }
    }

    static String bitString(int offset, int size, int totalSize){
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < offset; i++) builder.append('0');
        for(int i = 0; i < size; i++) builder.append('1');
        for(int i = 0; i < totalSize - size - offset; i++) builder.append('0');
        return "0b" + builder.reverse() + "L";
    }

    static String bitString(int size, int totalSize){
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < size; i++) builder.append('1');
        for(int i = 0; i < totalSize - size; i++) builder.append('0');
        return "0b" + builder.reverse() + "L";
    }

    static int sizeOf(TypeKind kind){
        switch(kind){
            case BOOLEAN: return 1;
            case BYTE:
            case CHAR: return 8;
            case SHORT: return 16;
            case INT:
            case FLOAT: return 32;
            default: throw new IllegalArgumentException("Illegal kind: " + kind + ". Must be primitive and takes less than 64 bits");
        }
    }

    static Class<?> typeOf(TypeKind kind){
        switch(kind){
            case BOOLEAN: return boolean.class;
            case BYTE: return byte.class;
            case CHAR: return char.class;
            case SHORT: return short.class;
            case INT: return int.class;
            case FLOAT: return float.class;
            default: throw new RuntimeException("Invalid type: " + kind + ". Must be primitive and takes less than 64 bits");
        }
    }

    static class SInfo{
        final Class<?> type;
        final int size;
        final FloatPacker pack;

        SInfo(Class<?> type, int size, FloatPacker pack){
            this.type = type;
            this.size = size;
            this.pack = pack;
        }
    }
}
