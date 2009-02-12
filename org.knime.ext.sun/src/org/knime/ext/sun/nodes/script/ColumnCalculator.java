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
package org.knime.ext.sun.nodes.script;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.ext.sun.nodes.script.expression.EvaluationFailedException;
import org.knime.ext.sun.nodes.script.expression.Expression;
import org.knime.ext.sun.nodes.script.expression.ExpressionInstance;
import org.knime.ext.sun.nodes.script.expression.IllegalPropertyException;
import org.knime.ext.sun.nodes.script.expression.Expression.ExpressionField;
import org.knime.ext.sun.nodes.script.expression.Expression.FieldType;
import org.knime.ext.sun.nodes.script.expression.Expression.InputField;

/**
 * Interface implementation that executes the java code snippet and calculates
 * the new column, either appended or replaced.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
class ColumnCalculator implements CellFactory {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ColumnCalculator.class);

    private final ExpressionInstance m_expression;
    
    private final JavaScriptingNodeModel m_model;

    private final DataColumnSpec[] m_colSpec;
    
    private Map<InputField, Object> m_scopeVarAssignmentMap; 

    /**
     * The row index may be used for calculation. Need to be set immediately
     * before calculate is called.
     */
    private int m_lastProcessedRow = 0;

    /**
     * Creates new factory for a column appender. It creates an instance of the
     * temporary java code, sets the fields dynamically and evaluates the
     * expression.
     * 
     * @param model contributes information regarding return type, etc.
     * @param newColSpec the column spec for the newly generated column
     * @throws InstantiationException if the instance cannot be instantiated.
     */
    ColumnCalculator(final JavaScriptingNodeModel model,
            final DataColumnSpec newColSpec)
            throws InstantiationException {
        m_model = model;
        m_expression = model.getCompiledExpression().getInstance();
        m_colSpec = new DataColumnSpec[]{newColSpec};
    }

    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        return m_colSpec;
    }

    /**
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        return new DataCell[]{calculate(row)};
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        m_lastProcessedRow = curRowNr;
        exec.setProgress(curRowNr / (double)rowCount, "Calculated row "
                + curRowNr + " (\"" + lastKey + "\")");
    }

    /**
     * Performs the calculation.
     * 
     * @param row the row to process
     * @return the resulting cell
     */
    public DataCell calculate(final DataRow row) {
        if (m_scopeVarAssignmentMap == null) {
            m_scopeVarAssignmentMap = new HashMap<InputField, Object>();
            for (Map.Entry<InputField, ExpressionField> e 
                    : m_expression.getFieldMap().entrySet()) {
                InputField f = e.getKey();
                if (f.getFieldType().equals(FieldType.Variable)) {
                    Class<?> c = e.getValue().getFieldClass();
                    m_scopeVarAssignmentMap.put(f, 
                            m_model.readVariable(f.getColOrVarName(), c));
                }
            }
        }
        DataTableSpec spec = m_model.getInputSpec();
        Class<?> returnType = m_model.getReturnType();
        Map<InputField, Object> nameValueMap = 
            new HashMap<InputField, Object>();
        nameValueMap.put(new InputField(Expression.ROWINDEX, 
                FieldType.TableConstant), m_lastProcessedRow);
        nameValueMap.put(new InputField(Expression.ROWID, 
                FieldType.TableConstant), row.getKey().getString());
        nameValueMap.put(new InputField(Expression.ROWCOUNT, 
                FieldType.TableConstant), m_model.getRowCount());
        nameValueMap.putAll(m_scopeVarAssignmentMap);
        for (int i = 0; i < row.getNumCells(); i++) {
            DataColumnSpec columnSpec = spec.getColumnSpec(i);
            InputField inputField = 
                new InputField(columnSpec.getName(), FieldType.Column); 
            if (!m_expression.needsInputField(inputField)) {
                continue;
            }
            DataCell cell = row.getCell(i);
            DataType cellType = columnSpec.getType();
            boolean isArray = cellType.isCollectionType();
            if (isArray) {
                cellType = cellType.getCollectionElementType();
            }
            Object cellVal = null;
            if (!cell.isMissing()) {
                if (cellType.isCompatible(IntValue.class)) {
                    if (isArray) {
                        cellVal = asIntArray((CollectionDataValue)cell);
                    } else {
                        cellVal = new Integer(((IntValue)cell).getIntValue());
                    }
                } else if (cellType.isCompatible(DoubleValue.class)) {
                    if (isArray) {
                        cellVal = asDoubleArray((CollectionDataValue)cell);
                    } else {
                        cellVal = new Double(
                                ((DoubleValue)cell).getDoubleValue());
                    }
                } else if (cellType.isCompatible(StringValue.class)) {
                    if (isArray) {
                        cellVal = asStringArray((CollectionDataValue)cell);
                    } else {
                        cellVal = ((StringValue)cell).getStringValue();
                    }
                } else {
                    if (isArray) {
                        cellVal = asStringArray((CollectionDataValue)cell);
                    } else {
                        cellVal = cell.toString();
                    }
                }
            }
            if (cellVal != null) {
                nameValueMap.put(inputField, cellVal);
            }
        }
        Object o = null;
        try {
            m_expression.set(nameValueMap);
            o = m_expression.evaluate();
            if (o != null && !returnType.isAssignableFrom(o.getClass())) {
                LOGGER.warn("Unable to cast return type of expression \""
                        + o.getClass().getName() + "\" to desired output \"" 
                        + returnType.getName() 
                        + "\" - putting missing value instead.");
                o = null;
            }
        } catch (EvaluationFailedException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof InvocationTargetException) {
                cause = ((InvocationTargetException)cause).getCause();
            }
            String message = 
                cause != null ? cause.getMessage() : ee.getMessage();
            LOGGER.warn("Evaluation of expression failed for row \""
                    + row.getKey() + "\": " + message, ee);
        } catch (IllegalPropertyException ipe) {
            LOGGER.warn("Evaluation of expression failed for row \""
                    + row.getKey() + "\": " + ipe.getMessage(), ipe);
        }
        DataCell result;
        if (returnType.equals(Integer.class)) {
            if (o == null) {
                result = DataType.getMissingCell();
            } else {
                result = new IntCell(((Integer)o).intValue());
            }
        } else if (returnType.equals(Double.class)) {
            if (o == null || ((Double)o).isNaN()) {
                result = DataType.getMissingCell();
            } else {
                result = new DoubleCell(((Double)o).doubleValue());
            }
        } else if (returnType.equals(String.class)) {
            if (o == null) {
                result = DataType.getMissingCell();
            } else {
                result = new StringCell((String)o);
            }
        } else {
            throw new InternalError();
        }
        return result;
    }

    private static int[] asIntArray(final CollectionDataValue cellValue) {
        int[] result = new int[cellValue.size()];
        int i = 0;
        for (DataCell c : cellValue) {
            if (c.isMissing()) {
                return null;
            }
            result[i++] = ((IntValue)c).getIntValue();
        }
        return result;
    }
    
    private static double[] asDoubleArray(final CollectionDataValue cellValue) {
        double[] result = new double[cellValue.size()];
        int i = 0;
        for (DataCell c : cellValue) {
            if (c.isMissing()) {
                return null;
            }
            result[i++] = ((DoubleValue)c).getDoubleValue();
        }
        return result;
    }
    
    private static String[] asStringArray(final CollectionDataValue cellValue) {
        String[] result = new String[cellValue.size()];
        int i = 0;
        for (DataCell c : cellValue) {
            if (c.isMissing()) {
                return null;
            }
            result[i++] = c.toString();
        }
        return result;
    }
}
