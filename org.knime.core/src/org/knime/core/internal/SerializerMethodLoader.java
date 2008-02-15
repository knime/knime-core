/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   Feb 14, 2008 (wiswedel): created
 */
package org.knime.core.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.knime.core.node.NodeLogger;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public final class SerializerMethodLoader {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(SerializerMethodLoader.class);

    private SerializerMethodLoader() {
    }
    
    @SuppressWarnings("unchecked") // access to CLASS_TO_SERIALIZER_MAP
    public static <T, V extends Serializer<T>> V getSerializer(
            final Class<T> encapsulatingClass, final Class<V> desiredReturnType,
            final String methodName) throws NoSuchMethodException {
        if (encapsulatingClass == null || desiredReturnType == null) {
            throw new NullPointerException(
                    "Class argument must not be null.");
        }
        if (methodName == null) {
            throw new NullPointerException("Method name must not be null");
        }
        V result = null;
        Exception exception = null;
        try {
            Method method = encapsulatingClass.getDeclaredMethod(methodName);
            method.setAccessible(true);
            Class rType = method.getReturnType();
            /* The following test realizes 
             * PortObjectSerializer<T>.class.isAssignableFrom(rType).
             * Unfortunately one can't check the generic(!) return type as
             * above since the type information is lost at compile time. We
             * have to make sure here that the runtime class of the return
             * value matches the class information of the PortObject class
             * as PortObjects may potentially be overwritten and we do not
             * accept the implementation of the superclass as we lose
             * information of the more specialized class when we use the
             * superclass' serializer.
             */ 
            boolean isAssignable = Serializer.class.isAssignableFrom(rType);
            boolean hasRType = false;
            if (isAssignable) {
                Type genType = method.getGenericReturnType();
                hasRType = isSerializer(rType, encapsulatingClass) 
                    || isSerializer(genType, encapsulatingClass);
                if (!hasRType) {
                    Type[] ins = rType.getGenericInterfaces();
                    for (int i = 0; (i < ins.length) && !hasRType; i++) {
                        hasRType = isSerializer(ins[i], encapsulatingClass);
                    }
                }
            }
            if (!hasRType) {
                throw new NoSuchMethodException("Class \"" 
                        + encapsulatingClass.getSimpleName()
                        + "\" defines method \"" + methodName + "\" "
                        + "but the method has the wrong return type (\""
                        + method.getGenericReturnType() + "\", expected \""
                        + desiredReturnType.getName() 
                        + "<" + encapsulatingClass.getName() + ">\").");
            } else {
                Object typeObject = method.invoke(null);
                result = desiredReturnType.cast(typeObject);
            }
        } catch (InvocationTargetException ite) {
            exception = ite;
        } catch (NullPointerException npe) {
            exception = npe;
        } catch (IllegalAccessException iae) {
            exception = iae;
        } catch (ClassCastException cce) {
            exception = cce;
        }
        if (exception != null) {
            LOGGER.coding("Class \"" + encapsulatingClass.getSimpleName()
                    + "\" defines method \"getPortObjectSerializer\" but there "
                    + "was a problem invoking it", exception);
            result = null;
        }
        return result;
    }
    
    /**
     * Helper method that checks if the passed Type is a parameterized
     * type (like <code>DataCellSerializer&lt;someType&gt;</code> and that it 
     * is assignable from the given {@link org.knime.core.data.DataCell} class. 
     * This method is used to check if the return class of 
     * <code>getCellSerializer()</code> in a 
     * {@link org.knime.core.data.DataCell} has the correct signature.
     */
    private static <T, V> boolean isSerializer(
            final Type c, final Class<T> cellClass) {
        boolean b = c instanceof ParameterizedType;
        if (b) {
            ParameterizedType parType = (ParameterizedType)c;
            Type[] args = parType.getActualTypeArguments();
            b = b && (args.length >= 1);
            b = b && (args[0] instanceof Class);
            b = b && cellClass.isAssignableFrom((Class<?>)args[0]);
        }
        return b;
    }
    
    public static interface Serializer<T> {
    }

}
