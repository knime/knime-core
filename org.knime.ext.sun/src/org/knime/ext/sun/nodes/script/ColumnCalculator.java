/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.ext.sun.nodes.script;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.knime.core.data.TimestampValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.TimestampCell;
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
        boolean isArrayReturn = m_model.isArrayReturn();
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
                } else if (cellType.isCompatible(TimestampValue.class)) {
                    if (isArray) {
                        cellVal = asDateArray((CollectionDataValue)cell);
                    } else {
                        // keep it read-only and clone it
                        cellVal = ((TimestampValue)cell).getDate().clone();
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
            // class correctness is asserted by compiler
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
            } else if (isArrayReturn) {
                result = asListCell((Integer[])o);
            } else {
                result = new IntCell(((Integer)o).intValue());
            }
        } else if (returnType.equals(Double.class)) {
            if (o == null) {
                result = DataType.getMissingCell();
            } else if (isArrayReturn) {
                result = asListCell((Double[])o);
            } else if (((Double)o).isNaN()) {
                result = DataType.getMissingCell();
            } else {
                result = new DoubleCell(((Double)o).doubleValue());
            }
        } else if (returnType.equals(Date.class)) {
            if (o == null) {
                result = DataType.getMissingCell();
            } else if (isArrayReturn) {
                result = asListCell((Date[])o);
            } else {
                result = new TimestampCell((Date)o);
            }
        } else if (returnType.equals(String.class)) {
            if (o == null) {
                result = DataType.getMissingCell();
            } else if (isArrayReturn) {
                result = asListCell((String[])o);
            } else {
                result = new StringCell((String)o);
            }
        } else {
            throw new InternalError();
        }
        return result;
    }

    private static Integer[] asIntArray(final CollectionDataValue cellValue) {
        Integer[] result = new Integer[cellValue.size()];
        int i = 0;
        for (DataCell c : cellValue) {
            result[i++] = c.isMissing() ? null : ((IntValue)c).getIntValue();
        }
        return result;
    }
    
    private static ListCell asListCell(final Integer[] result) {
        List<DataCell> asCellColl = new ArrayList<DataCell>();
        for (Integer i : result) {
            asCellColl.add(i == null 
                    ? DataType.getMissingCell() : new IntCell(i));
        }
        return CollectionCellFactory.createListCell(asCellColl);
    }
    
    private static Double[] asDoubleArray(final CollectionDataValue cellValue) {
        Double[] result = new Double[cellValue.size()];
        int i = 0;
        for (DataCell c : cellValue) {
            result[i++] = 
                c.isMissing() ? null : ((DoubleValue)c).getDoubleValue();
        }
        return result;
    }
    
    private static ListCell asListCell(final Double[] result) {
        List<DataCell> asCellColl = new ArrayList<DataCell>();
        for (Double d : result) {
            if (d == null || Double.isNaN(d)) {
                asCellColl.add(DataType.getMissingCell()); 
            } else {
                asCellColl.add(new DoubleCell(d));
            }
        }
        return CollectionCellFactory.createListCell(asCellColl);
    }
    
    private static Date[] asDateArray(final CollectionDataValue cellValue) {
        Date[] result = new Date[cellValue.size()];
        int i = 0;
        for (DataCell c : cellValue) {
            if (c.isMissing()) {
                result[i] = null;
            } else {
                // clone to ensure immutable property of cell
                result[i++] = (Date)((TimestampCell)c).getDate().clone();
            }
            i++;
        }
        return result;
    }
    
    private static ListCell asListCell(final Date[] result) {
        List<DataCell> asCellColl = new ArrayList<DataCell>();
        for (Date s : result) {
            asCellColl.add(s == null 
                    ? DataType.getMissingCell() : new TimestampCell(s));
        }
        return CollectionCellFactory.createListCell(asCellColl);
    }
    
    private static String[] asStringArray(final CollectionDataValue cellValue) {
        String[] result = new String[cellValue.size()];
        int i = 0;
        for (DataCell c : cellValue) {
            result[i++] = c.isMissing() ? null : c.toString();
        }
        return result;
    }
    
    private static ListCell asListCell(final String[] result) {
        List<DataCell> asCellColl = new ArrayList<DataCell>();
        for (String s : result) {
            asCellColl.add(s == null 
                    ? DataType.getMissingCell() : new StringCell(s));
        }
        return CollectionCellFactory.createListCell(asCellColl);
    }
    
}
