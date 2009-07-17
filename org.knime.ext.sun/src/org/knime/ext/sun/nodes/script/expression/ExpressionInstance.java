/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.ext.sun.nodes.script.expression;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.knime.ext.sun.nodes.script.expression.Expression.ExpressionField;
import org.knime.ext.sun.nodes.script.expression.Expression.InputField;

/**
 * An expression instance combines the compiled source code along with some
 * access method to set fields and to get the evaluation result from.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ExpressionInstance {
    private final Map<InputField, ExpressionField> m_fieldMap;

    private final Object m_compiled;

    /**
     * Creates new expression instance wrapping a compiled object with that has
     * fields according to the properties argument.
     * 
     * @param compiled the object being wrapped. Must have an
     *            <code>internalEvaluate</code> method.
     * @param fieldMap map of field name to field class
     */
    protected ExpressionInstance(final Object compiled,
            final Map<InputField, ExpressionField> fieldMap) {
        m_compiled = compiled;
        m_fieldMap = fieldMap;
    }

    /**
     * The evaluation of the concrete expression instance.
     * 
     * @return the result of the evaluation
     * @throws EvaluationFailedException if the evaluation fails for any reason
     */
    public final Object evaluate() throws EvaluationFailedException {
        try {
            Method eval = m_compiled.getClass().getMethod("internalEvaluate",
                    (Class[])null);
            Object result = eval.invoke(m_compiled);
            return result;
        } catch (Throwable throwable) {
            throw new EvaluationFailedException(throwable);
        }

    }
    
    /** Is the input field denoted by the argument actually used by the 
     * expression.
     * @param inField To check for.
     * @return <code>true</code> when used, <code>false</code> otherwise.
     */
    public boolean needsInputField(final InputField inField) {
        return m_fieldMap.containsKey(inField);
    }
    
    /**
     * @return the fieldMap
     */
    public Map<InputField, ExpressionField> getFieldMap() {
        return m_fieldMap;
    }

    /**
     * Sets field values.
     * 
     * @param property2ValueMap containing properties -&gt; values
     * @throws IllegalPropertyException if a field is unkown or a value is
     *             incompatible
     */
    public final void set(final Map<InputField, Object> property2ValueMap)
            throws IllegalPropertyException {
        // Prepare the values by looking at what properties where
        // specified in the constructor
        for (Map.Entry<InputField, ExpressionField> entry 
                : m_fieldMap.entrySet()) {
            InputField field = entry.getKey();
            ExpressionField expressionField = entry.getValue();
            Object value = property2ValueMap.get(field);
            if (value == null) {
                throw new IllegalPropertyException(
                        "No value for field " + field);
            }
            if (!expressionField.getFieldClass().isInstance(value)) {
                throw new IllegalPropertyException(
                        "Type for field \"" + field + "\" not matched: got "
                                + value.getClass().getName() + " but expected "
                                + expressionField.getFieldClass().getName());
            }
            setField(expressionField.getFieldNameInJava(), value);
        }
    }

    /*
     * Sets a field on m_compiled using reflection.
     */
    private void setField(final String property, final Object value)
            throws IllegalPropertyException {
        String fieldType = "<UNKNOWN>";
        try {
            Class<?> type = m_compiled.getClass();
            Field f = type.getDeclaredField(property);
            fieldType = f.getType().getName();
            f.set(m_compiled, value);
        } catch (NoSuchFieldException e) {
            throw new IllegalPropertyException("Unknown Field: " + property, e);
        } catch (IllegalAccessException e) {
            throw new IllegalPropertyException("Field couldn't be accessed: "
                    + property, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalPropertyException("Field type " + fieldType
                    + " doesn't match value type " + value.getClass().getName()
                    + ".", e);
        }
    }
}
