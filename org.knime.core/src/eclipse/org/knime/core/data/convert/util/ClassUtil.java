package org.knime.core.data.convert.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.knime.core.data.DataType;
import org.knime.core.util.Pair;

/**
 *
 * @author Jonathan Hale
 * @since 3.2
 */
public class ClassUtil {

    /**
     * Execute a consumer for a class, every interface and its superclass recursively.
     *
     * @param cls Class to map to
     * @param lambda {@link Consumer} to execute for every {@link Class}
     */
    public static <T> void recursiveMapToClassHierarchy(final Class<T> cls, final Consumer<Class<?>> lambda) {
        // recursively apply lambda to the class hierarchy of only the given
        // class
        recursiveMapToClassHierarchies(Collections.singleton(cls), lambda);
    }

    /**
     * Execute a consumer for the {@link DataType#getPreferredValueClass() prefferedValueClass} and interfaces of a
     * DataType, every superinterface and its superclass recursively.
     *
     * @param dataType Class to map to
     * @param lambda {@link Consumer} to execute for every {@link Class}
     */
    public static void recursiveMapToDataTypeClasses(final DataType dataType, final Consumer<Class<?>> lambda) {
        final ArrayList<Class<?>> classes = new ArrayList<>();
        classes.add(dataType.getPreferredValueClass());
        classes.addAll(dataType.getValueClasses());

        recursiveMapToClassHierarchies(classes, lambda);
    }

    /**
     * Execute a consumer for a collection of classes, every interface and their superclass recursively.
     *
     * @param clss Classes to map to
     * @param lambda {@link Consumer} to execute for every {@link Class}
     */
    public static void recursiveMapToClassHierarchies(final Collection<Class<?>> clss,
        final Consumer<Class<?>> lambda) {
        // remaining classes to apply the lambda to
        final LinkedBlockingQueue<Class<?>> classes = new LinkedBlockingQueue<>();

        // start with the given class itself
        classes.addAll(clss);

        Class<?> curClass = null;
        while ((curClass = classes.poll()) != null) {
            // run the consumer
            lambda.accept(curClass);

            // add all interfaces, then superclass to ensure Object is the last
            // Class we execute the lambda on
            classes.addAll(Arrays.asList(curClass.getInterfaces()));

            if (curClass.getSuperclass() != null) {
                // Object does not have a superclass, so we need to check for
                // null
                classes.add(curClass.getSuperclass());
            }
        }
    }

    /**
     * Get a Collection of methods which are annotated with the given {@link Annotation} paired with the Annotation
     * itself.
     *
     * @param cls Class to get annotated methods from
     * @param annotationClass the annotation
     * @return Collection of pairs of methods together with the instance of <code>annotationClass</code> they are
     *         annotated with
     */
    public static <A extends Annotation> Collection<Pair<Method, A>> getMethodsWithAnnotation(final Class<?> cls,
        final Class<A> annotationClass) {
        // result list of annotated methods
        final ArrayList<Pair<Method, A>> annotatedMethods = new ArrayList<>();
        // all methods declared in exactly this class (not in superclasses)
        final Method[] declaredMethods = cls.getDeclaredMethods();

        for (final Method m : declaredMethods) {
            // get an instance of the given annotation
            final A a = m.getAnnotation(annotationClass);
            if (a != null) {
                // present, therefore add to the list
                annotatedMethods.add(new Pair<>(m, a));
            }
        }

        return annotatedMethods;
    }

    /*
     * Conversion from primitives to boxing types used in
     * Activator#parseAnnotations()
     */
    private static final HashMap<Class<?>, Class<? extends Object>> PRIMITIVE_TO_BOXING_TYPE = new HashMap<>();
    static {
        PRIMITIVE_TO_BOXING_TYPE.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_BOXING_TYPE.put(byte.class, Byte.class);
        PRIMITIVE_TO_BOXING_TYPE.put(char.class, Character.class);
        PRIMITIVE_TO_BOXING_TYPE.put(int.class, Integer.class);
        PRIMITIVE_TO_BOXING_TYPE.put(short.class, Short.class);
        PRIMITIVE_TO_BOXING_TYPE.put(long.class, Long.class);
        PRIMITIVE_TO_BOXING_TYPE.put(float.class, Float.class);
        PRIMITIVE_TO_BOXING_TYPE.put(double.class, Double.class);
        PRIMITIVE_TO_BOXING_TYPE.put(void.class, Void.class);
    }

    /**
     * Check if the given class is a Object. If it is not (aka a primitive type), the boxing type is returned.
     *
     * @param in type to check
     * @return <code>in</code> or a type which boxes the primitive type described by <code>in</code>
     */
    public static Class<? extends Object> ensureObjectType(final Class<?> in) {
        if (Object.class.isAssignableFrom(in)) {
            return in;
        }

        return PRIMITIVE_TO_BOXING_TYPE.get(in);
    }

    /**
     * Get the class of an array with given element type.
     *
     * @param destElementType element type for the array
     * @return the class of ElementType[]
     */
    public static <ARRAY_TYPE> Class<ARRAY_TYPE> getArrayType(final Class<?> destElementType) {
        return (Class<ARRAY_TYPE>)Array.newInstance(destElementType, 0).getClass();
    }
}
