/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 */
package org.knime.ext.sun.nodes.script.expression;

import java.lang.reflect.Field;
import java.util.Map;

import org.knime.ext.sun.nodes.script.expression.Expression.ExpressionField;
import org.knime.ext.sun.nodes.script.expression.Expression.InputField;

/**
 * An expression instance combines the compiled source code along with some
 * access method to set fields and to get the evaluation result from.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ExpressionInstance {
    private final Map<InputField, ExpressionField> m_fieldMap;

    private final AbstractSnippetExpression m_abstractExpression;

    /**
     * Creates new expression instance wrapping a compiled object with that has
     * fields according to the properties argument.
     *
     * @param abstracExpression the object being wrapped.
     * @param fieldMap map of field name to field class
     */
    ExpressionInstance(final AbstractSnippetExpression abstracExpression,
        final Map<InputField, ExpressionField> fieldMap) {
        m_abstractExpression = abstracExpression;
        m_fieldMap = fieldMap;
    }

    /**
     * The evaluation of the concrete expression instance.
     *
     * @return the result of the evaluation
     * @throws EvaluationFailedException if the evaluation fails for any reason
     * @throws Abort If snippet code throws Abort exception.
     */
    public final Object evaluate() throws EvaluationFailedException, Abort {
        try {
            return m_abstractExpression.internalEvaluate();
        } catch (Abort a) {
            throw a;
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
                // could be that there is no entry or the value is null
                // this could also be an assertion
                if (property2ValueMap.containsKey(value)) {
                    throw new IllegalPropertyException(
                            "No value for field " + field);
                } else {
                    // null represents missing value
                }
            }
            if (value != null
                    && !expressionField.getFieldClass().isInstance(value)) {
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
            Class<?> type = m_abstractExpression.getClass();
            Field f = type.getDeclaredField(property);
            fieldType = f.getType().getName();
            f.set(m_abstractExpression, value);
        } catch (NoSuchFieldException e) {
            throw new IllegalPropertyException("Unknown Field: " + property, e);
        } catch (IllegalAccessException e) {
            throw new IllegalPropertyException("Field couldn't be accessed: "
                    + property, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalPropertyException("Field type " + fieldType
                    + " doesn't match value type "
                    + value != null ? value.getClass().getName() : "<null>"
                    + ".", e);
        }
    }
}
