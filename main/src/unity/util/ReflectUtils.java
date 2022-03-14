package unity.util;

import mindustry.ctype.*;
//import sun.misc.*;

import java.lang.reflect.*;

import static mindustry.Vars.*;

/** @author GlennFolker  Xelo was here */
@SuppressWarnings("unchecked")
public final class ReflectUtils{
    private ReflectUtils(){
        throw new AssertionError();
    }

    public static Class<?> box(Class<?> type){
        if(type == boolean.class) return Boolean.class;
        if(type == byte.class) return Byte.class;
        if(type == char.class) return Character.class;
        if(type == short.class) return Short.class;
        if(type == int.class) return Integer.class;
        if(type == float.class) return Float.class;
        if(type == long.class) return Long.class;
        if(type == double.class) return Double.class;
        return type;
    }

    public static Class<?> unbox(Class<?> type){
        if(type == Boolean.class) return boolean.class;
        if(type == Byte.class) return byte.class;
        if(type == Character.class) return char.class;
        if(type == Short.class) return short.class;
        if(type == Integer.class) return int.class;
        if(type == Float.class) return float.class;
        if(type == Long.class) return long.class;
        if(type == Double.class) return double.class;
        return type;
    }

    public static String def(Class<?> type){
        String t = unbox(type).getSimpleName();
        return switch(t){
            case "boolean" -> "false";
            case "byte", "char", "short", "int", "long" -> "0";
            case "float", "double" -> "0.0";
            default -> "null";
        };
    }

    /** Finds a class from the parent classes that has a specific field. */
    public static Class<?> findClassf(Class<?> type, String field){
        for(type = type.isAnonymousClass() ? type.getSuperclass() : type; type != null; type = type.getSuperclass()){
            try{
                type.getDeclaredField(field);
                break;
            }catch(NoSuchFieldException ignored){
            }
        }

        return type;
    }

    /** Finds a class from the parent classes that has a specific method. */
    public static Class<?> findClassm(Class<?> type, String method, Class<?>... args){
        for(type = type.isAnonymousClass() ? type.getSuperclass() : type; type != null; type = type.getSuperclass()){
            try{
                type.getDeclaredMethod(method, args);
                break;
            }catch(NoSuchMethodException ignored){
            }
        }

        return type;
    }

    /** Finds a class from the parent classes that has a specific constructor. */
    public static Class<?> findClassc(Class<?> type, Class<?>... args){
        for(type = type.isAnonymousClass() ? type.getSuperclass() : type; type != null; type = type.getSuperclass()){
            try{
                type.getDeclaredConstructor(args);
                break;
            }catch(NoSuchMethodException ignored){
            }
        }

        return type;
    }

    public static Class<?> findClass(String name){
        try{
            return Class.forName(name, true, mods.mainLoader());
        }catch(ClassNotFoundException | NoClassDefFoundError e){
            throw new RuntimeException(e);
        }
    }

    /** A utility function to find a field without throwing exceptions. */
    public static Field findField(Class<?> type, String field, boolean access){
        try{
            Field f = findClassf(type, field).getDeclaredField(field);
            if(access) f.setAccessible(true);

            return f;
        }catch(NoSuchFieldException e){
            throw new RuntimeException(e);
        }
    }

    /** Sets a field of an model without throwing exceptions. */
    public static void setField(Object object, Field field, Object value){
        try{
            field.setAccessible(true);
            field.set(object, value);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    /** Gets a value from a field of an model without throwing exceptions. */
    public static <T> T getFieldValue(Object object, Field field){
        try{
            field.setAccessible(true);
            return (T)field.get(object);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    /** Gets a value from a field of an model without throwing exceptions. */
    public static <T, F extends U, U> T getFieldValue(F object, Class<U> subclass, String fieldname){
        try{
            Field field = subclass.getDeclaredField(fieldname);
            field.setAccessible(true);
            return (T)field.get(object);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    /** Gets a field of an model without throwing exceptions. */
    public static Field getField(Object object, String field){
        try{
            return object.getClass().getDeclaredField(field);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }


    /** A utility function to find a method without throwing exceptions. */
    public static Method findMethod(Class<?> type, String methodName, boolean access, Class<?>... args){
        try{
            Method m = findClassm(type, methodName, args).getDeclaredMethod(methodName, args);
            if(access) m.setAccessible(true);

            return m;
        }catch(NoSuchMethodException e){
            throw new RuntimeException(e);
        }
    }
    /** Reflectively invokes a method without throwing exceptions. */
    public static <T> T invokeMethod(Object object, String method, Object... args){
        try{
            return (T)findMethod(object.getClass(),method,true).invoke(object, args);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    /** Reflectively invokes a method without throwing exceptions. */
    public static <T> T invokeMethod(Object object, Method method, Object... args){
        try{
            return (T)method.invoke(object, args);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    /** A utility function to find a constructor without throwing exceptions. */
    public static <T> Constructor<T> findConstructor(Class<T> type, boolean access, Class<?>... args){
        try{
            Constructor<T> c = ((Class<T>)findClassc(type, args)).getDeclaredConstructor(args);
            if(access) c.setAccessible(true);

            return c;
        }catch(NoSuchMethodException e){
            throw new RuntimeException(e);
        }
    }

    /** Reflectively instantiates a type without throwing exceptions. */
    public static <T> T newInstance(Constructor<T> constructor, Object... args){
        try{
            return constructor.newInstance(args);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public static Class<?> classCaller(){
        Thread thread = Thread.currentThread();
        StackTraceElement[] trace = thread.getStackTrace();
        try{
            return Class.forName(trace[3].getClassName(), false, mods.mainLoader());
        }catch(ClassNotFoundException e){
            return null;
        }
    }

}
