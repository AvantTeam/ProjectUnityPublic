package unity.util;

import arc.util.*;

import java.lang.reflect.*;

import static mindustry.Vars.mods;

/**
 * Shared utility access for reflective operations, without throwing any checked exceptions.
 * @author GlennFolker
 */
@SuppressWarnings("unchecked")
public final class ReflectUtils{
    private ReflectUtils(){
        throw new AssertionError();
    }

    public static <T> Class<T> known(Class<?> anon){
        return (Class<T>)(anon.isAnonymousClass() ? anon.getSuperclass() : anon);
    }

    public static <T> Class<T> findc(String name){
        try{
            return (Class<T>)Class.forName(name, true, mods.mainLoader());
        }catch(Throwable t){
            return null;
        }
    }

    public static Field findf(Class<?> type, String name){
        for(Class<?> t = type; t != Object.class; t = t.getSuperclass()){
            try{
                Field f = t.getDeclaredField(name);
                f.setAccessible(true);

                return f;
            }catch(Throwable ignored){}
        }

        throw new IllegalArgumentException("Field '" + name + "' not found in '" + type.getName() + "'.");
    }

    public static Method findm(Class<?> type, String name, Class<?>... params){
        for(Class<?> t = type; t != Object.class; t = t.getSuperclass()){
            try{
                Method m = t.getDeclaredMethod(name, params);
                m.setAccessible(true);

                return m;
            }catch(Throwable ignored){}
        }

        throw new IllegalArgumentException("Method '" + name + "(" + str(params) + ")' not found in '" + type.getName() + "'.");
    }

    public static <T> Constructor<T> findct(Class<?> type, Class<?>... params){
        for(Class<?> t = type; t != Object.class; t = t.getSuperclass()){
            try{
                Constructor<T> c = (Constructor<T>)type.getDeclaredConstructor(params);
                c.setAccessible(true);

                return c;
            }catch(Throwable ignored){}
        }

        throw new IllegalArgumentException("Constructor '" + type.getName() + "(" + str(params) + ")' not found.");
    }

    public static <T> T get(Object inst, String name){
        return get(inst, findf(inst.getClass(), name));
    }

    public static <T> T get(Class<?> type, Object inst, String name){
        return get(inst, findf(type, name));
    }

    public static <T> T get(Object inst, Field field){
        try{
            return (T)field.get(inst);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static void set(Object inst, String name, Object value){
        set(inst, findf(inst.getClass(), name), value);
    }

    public static void set(Class<?> type, Object inst, String name, Object value){
        set(inst, findf(type, name), value);
    }

    public static void set(Object inst, Field field, Object value){
        try{
            field.set(inst, value);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static <T> T invoke(Object inst, String name, Object[] args, @Nullable Class<?>... params){
        return invoke(inst.getClass(), inst, name, args, params);
    }

    public static <T> T invoke(Class<?> type, String name, Object[] args, @Nullable Class<?>... params){
        return invoke(type, null, name, args, params);
    }

    public static <T> T invoke(Class<?> type, Object inst, String name, Object[] args, @Nullable Class<?>... params){
        if(params == null && args != null){
            params = new Class[args.length];
            for(int i = 0; i < params.length; i++) params[i] = args[i].getClass();
        }

        return invoke(inst, findm(type, name, params), args);
    }

    public static <T> T invoke(Object inst, Method method, Object... args){
        try{
            return (T)method.invoke(inst, args);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static <T> T inst(Class<?> type, Object... args){
        return inst(type, args, (Class<?>[])null);
    }

    public static <T> T inst(Class<?> type, Object[] args, @Nullable Class<?>... params){
        if(params == null){
            params = new Class[args.length];
            for(int i = 0; i < params.length; i++) params[i] = args[i].getClass();
        }

        return inst(findct(type, params), args);
    }

    public static <T> T inst(Constructor<T> constr, Object... args){
        try{
            return constr.newInstance(args);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static String str(Class<?>... types){
        if(types.length == 0) return "";
        new Exception().printStackTrace();

        StringBuilder builder = new StringBuilder(types[0].getName());
        for(int i = 1; i < types.length; i++) builder.append(", ").append(types[i].getName());
        return builder.toString();
    }

    public static <T> Class<T> caller(){
        Thread thread = Thread.currentThread();
        StackTraceElement[] trace = thread.getStackTrace();

        return findc(trace[3].getClassName());
    }

    public static Class<?> box(Class<?> unboxed){
        return switch(unboxed.getName()){
            case "void" -> Void.class;
            case "byte" -> Byte.class;
            case "char" -> Character.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            default -> unboxed;
        };
    }

    public static Class<?> unbox(Class<?> boxed){
        return switch(boxed.getSimpleName()){
            case "Void" -> void.class;
            case "Byte" -> byte.class;
            case "Character" -> char.class;
            case "Short" -> short.class;
            case "Integer" -> int.class;
            case "Long" -> long.class;
            case "Float" -> float.class;
            case "Double" -> double.class;
            default -> boxed;
        };
    }
}
