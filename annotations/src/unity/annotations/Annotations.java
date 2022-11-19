package unity.annotations;

import arc.func.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Attribute.*;
import com.sun.tools.javac.code.Scope.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.Name;
import sun.reflect.annotation.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.lang.*;
import java.lang.Class;
import java.lang.Enum;
import java.lang.annotation.*;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;
import java.util.Map.*;

@SuppressWarnings({"unchecked", "UnusedReturnValue"})
public final class Annotations{
    private Annotations(){
        throw new AssertionError();
    }

    /** Defines a class providing static entries of IO handlers. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    public @interface TypeIOHandler{}

    /** Indicates that this class is an entities component. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    public @interface EntityComponent{
        /** @return Whether this is a fetched component; in that case, do not generate interfaces. */
        boolean vanilla() default false;

        /** @return Whether the component should generate a base class for itself. */
        boolean base() default false;
    }

    /** All entities components will inherit from this. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    public @interface EntityBaseComponent{}

    /** Whether this interface wraps an entities component. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    public @interface EntityInterface{}

    /** Generates an entities definition from given components. */
    @Retention(RetentionPolicy.CLASS)
    public @interface EntityDef{
        /** @return The interfaces that will be inherited by the generated entities class. */
        Class<?>[] value();

        /** @return Whether the class can serialize itself. */
        boolean serialize() default true;

        /** @return Whether the class can write/read to/from save files. */
        boolean genIO() default true;

        /** @return Whether the class is poolable. */
        boolean pooled() default false;
    }

    /** Indicates that this entities (!) class should be mapped. */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.CLASS)
    public @interface EntityPoint{}

    /** Indicates that a field will be interpolated when synced. */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.CLASS)
    public @interface SyncField{
        /** @return True if the field is linearly interpolated. Otherwise, it's interpolated as an angle. */
        boolean value();

        /** @return True if the field is clamped to 0-1. */
        boolean clamped() default false;
    }

    /** Indicates that a field will not be read from the server when syncing the local player state. */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.CLASS)
    public @interface SyncLocal{}

    /** Indicates that the field annotated with this came from another component class. */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Import{}

    /** Won't generate a setter for this field. */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.CLASS)
    public @interface ReadOnly{}

    /** Whether this method replaces the actual method in the base class. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Replace{
        /** @return The priority of this replacer. */
        int value() default 0;
    }

    /** Whether this method is implemented in compile-time. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface InternalImpl{}

    /** Used for method appender sorting. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface MethodPriority{
        /** @return The priority. */
        int value();
    }

    /** Appends this {@code add()}/{@code remove()} method before the {@code if([!]added)} check. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface BypassGroupCheck{}

    /** Will not replace {@code return;} to {@code break [block];}, hence breaking the entire method statement. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface BreakAll{}

    /** Removes a component-specific method implementation. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Remove{
        /** @return The component specification to remove. */
        Class<?> value();
    }

    /** Will only implement this method if the entities inherits this certain component. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Extend{
        /** @return The component specification to check. */ //TODO should this be a Class<?>[]?
        Class<?> value();
    }

    /** Inserts this parameter-less method into another void method. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Insert{
        /**
         * @return The target method described in {@link String} with the format {@code <methodName>(<paramType>...)}.
         * For example, when targeting {@code void call(String arg, int prior)}, the target descriptor must be
         * {@code call(java.lang.String, int)}
         */
        String value();

        /** @return The component-specific method implementation to target. */
        Class<?> block() default Void.class;

        /** @return Whether the call to this method is after the default or not. */
        boolean after() default true;
    }

    /** Wraps a component-specific method implementation with this boolean parameterless method. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Wrap{
        /**
         * @return The target method described in {@link String} with the format {@code <methodName>(<paramType>...)}.
         * For example, when targeting {@code void call(String arg, int prior)}, the target descriptor must be
         * {@code call(java.lang.String, int)}
         */
        String value();

        /** @return The component-specific method implementation to target. */
        Class<?> block() default Void.class;
    }

    /** Prevents this component from getting added into an entities group, specified by the group's element type. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    public @interface ExcludeGroups{
        /** @return The excluded group's element type. */
        Class<?>[] value();
    }

    /**
     * Reliably generates a proxy handling an annotation type from model elements.
     * @author Anuke
     */
    public static class AnnotationProxyMaker{
        private final Compound anno;
        private final Class<? extends Annotation> type;

        private AnnotationProxyMaker(Compound anno, Class<? extends Annotation> type){
            this.anno = anno;
            this.type = type;
        }

        public static <A extends Annotation> A generateAnnotation(Compound anno, Class<A> type){
            if(anno == null) return null;
            return type.cast(new AnnotationProxyMaker(anno, type).generateAnnotation());
        }

        private Annotation generateAnnotation(){
            return AnnotationParser.annotationForMap(type, getAllReflectedValues());
        }

        private Map<String, Object> getAllReflectedValues(){
            Map<String, Object> res = new LinkedHashMap<>();

            for(Entry<MethodSymbol, Attribute> entry : getAllValues().entrySet()){
                MethodSymbol meth = entry.getKey();
                Object value = generateValue(meth, entry.getValue());
                if(value != null){
                    res.put(meth.name.toString(), value);
                }
            }

            return res;
        }

        private Map<MethodSymbol, Attribute> getAllValues(){
            Map<MethodSymbol, Attribute> map = new LinkedHashMap<>();
            ClassSymbol cl = (ClassSymbol)anno.type.tsym;

            try{
                Class<?> entryClass = Class.forName("com.sun.tools.javac.code.Scope$Entry");
                Field siblingField = entryClass.getField("sibling");
                Field symField = entryClass.getField("sym");

                WriteableScope members = cl.members();
                Field field = members.getClass().getField("elems");
                Object elems = field.get(members);

                for(Object currEntry = elems; currEntry != null; currEntry = siblingField.get(currEntry)){
                    handleSymbol((Symbol)symField.get(currEntry), map);
                }
            }catch(Throwable e){
                try{
                    Class<?> lookupClass = Class.forName("com.sun.tools.javac.code.Scope$LookupKind");
                    Field nonRecField = lookupClass.getField("NON_RECURSIVE");
                    Object nonRec = nonRecField.get(null);

                    WriteableScope scope = cl.members();
                    Method getSyms = scope.getClass().getMethod("getSymbols", lookupClass);
                    Iterable<Symbol> it = (Iterable<Symbol>)getSyms.invoke(scope, nonRec);
                    for(Symbol symbol : it){
                        handleSymbol(symbol, map);
                    }
                }catch(Throwable death){
                    throw new RuntimeException(death);
                }
            }

            for(Pair<MethodSymbol, Attribute> pair : anno.values){
                map.put(pair.fst, pair.snd);
            }

            return map;
        }

        private <T extends Symbol> void handleSymbol(Symbol sym, Map<T, Attribute> map){
            if(sym.getKind() == ElementKind.METHOD){
                MethodSymbol meth = (MethodSymbol)sym;

                Attribute def = meth.getDefaultValue();
                if(def != null) map.put((T)meth, def);
            }
        }

        private Object generateValue(MethodSymbol meth, Attribute attrib){
            return new ValueVisitor(meth).getValue(attrib);
        }

        private class ValueVisitor implements Attribute.Visitor{
            private final MethodSymbol meth;
            private Class<?> returnClass;
            private Object value;

            ValueVisitor(MethodSymbol meth){
                this.meth = meth;
            }

            Object getValue(Attribute attrib){
                Method meth;
                try{
                    meth = type.getMethod(this.meth.name.toString());
                }catch(NoSuchMethodException e){
                    return null;
                }

                returnClass = meth.getReturnType();
                attrib.accept(this);

                if(!(value instanceof ExceptionProxy) && !AnnotationType.invocationHandlerReturnType(returnClass).isInstance(value)) typeMismatch(meth, attrib);
                return value;
            }

            @Override
            public void visitConstant(Constant constant){
                value = constant.getValue();
            }

            @Override
            public void visitClass(Attribute.Class type){
                value = mirrorProxy(type.classType);
            }

            @Override
            public void visitArray(Attribute.Array arr){
                Name name = ((Type.ArrayType)arr.type).elemtype.tsym.getQualifiedName();

                if(name.equals(name.table.names.java_lang_Class)){
                    ListBuffer<Type> list = new ListBuffer<>();
                    for(Attribute attrib : arr.values){
                        Type type = attrib instanceof UnresolvedClass ? ((UnresolvedClass)attrib).classType :
                        ((Attribute.Class)attrib).classType;

                        list.append(type);
                    }

                    value = mirrorProxy(list.toList());
                }else{
                    Class<?> arrType = returnClass;
                    returnClass = returnClass.getComponentType();

                    try{
                        Object inst = Array.newInstance(returnClass, arr.values.length);
                        for(int i = 0; i < arr.values.length; i++){
                            arr.values[i].accept(this);
                            if(value == null || value instanceof ExceptionProxy) return;

                            try{
                                Array.set(inst, i, value);
                            }catch(IllegalArgumentException e){
                                value = null;
                                return;
                            }
                        }

                        value = inst;
                    }finally{
                        returnClass = arrType;
                    }
                }
            }

            @Override
            @SuppressWarnings("rawtypes")
            public void visitEnum(Attribute.Enum enumType){
                if(returnClass.isEnum()){
                    String name = enumType.value.toString();
                    try{
                        value = Enum.valueOf((Class)returnClass, name);
                    }catch(IllegalArgumentException e){
                        value = proxify(() -> new EnumConstantNotPresentException((Class)returnClass, name));
                    }
                }else{
                    value = null;
                }
            }

            @Override
            public void visitCompound(Compound anno){
                try{
                    Class<? extends Annotation> type = returnClass.asSubclass(Annotation.class);
                    value = generateAnnotation(anno, type);
                }catch(ClassCastException e){
                    value = null;
                }

            }

            @Override
            public void visitError(Attribute.Error err){
                if(err instanceof UnresolvedClass){
                    value = mirrorProxy(((UnresolvedClass)err).classType);
                }else{
                    value = null;
                }
            }

            private void typeMismatch(Method meth, Attribute attrib){
                value = proxify(() -> new AnnotationTypeMismatchException(meth, attrib.type.toString()));
            }
        }

        private static Object mirrorProxy(Type t){
            return proxify(() -> new MirroredTypeException(t));
        }

        private static Object mirrorProxy(List<Type> t){
            return proxify(() -> new MirroredTypesException(t));
        }

        private static <T extends Throwable> Object proxify(Prov<T> prov){
            try{
                return new ExceptionProxy(){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected RuntimeException generateException(){
                        return (RuntimeException)prov.get();
                    }
                };
            }catch(Throwable t){
                throw new RuntimeException(t);
            }
        }
    }
}
