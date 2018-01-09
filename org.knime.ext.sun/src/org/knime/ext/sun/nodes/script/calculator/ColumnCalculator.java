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
package org.knime.ext.sun.nodes.script.calculator;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.ext.sun.nodes.script.expression.Abort;
import org.knime.ext.sun.nodes.script.expression.EvaluationFailedException;
import org.knime.ext.sun.nodes.script.expression.Expression;
import org.knime.ext.sun.nodes.script.expression.Expression.ExpressionField;
import org.knime.ext.sun.nodes.script.expression.Expression.FieldType;
import org.knime.ext.sun.nodes.script.expression.Expression.InputField;
import org.knime.ext.sun.nodes.script.expression.ExpressionInstance;
import org.knime.ext.sun.nodes.script.expression.IllegalPropertyException;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingSettings;
import org.knime.ext.sun.nodes.script.settings.JavaSnippetType;

/**
 * Interface implementation that executes the java code snippet and calculates
 * the new column, either appended or replaced.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ColumnCalculator implements CellFactory {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ColumnCalculator.class);

    private final JavaScriptingSettings m_settings;
    private final ExpressionInstance m_expression;
    private final FlowVariableProvider m_flowVarProvider;
    private boolean m_hasReportedMissing = false;

    private final DataColumnSpec[] m_colSpec;

    private Map<InputField, Object> m_flowVarAssignmentMap;

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
     * @param settings settings & other infos (e.g. return type)
     * @param flowVarProvider Accessor for flow variables (the NodeModel)
     * @throws InstantiationException if the instance cannot be instantiated.
     * @throws InvalidSettingsException If settings invalid.
     */
    public ColumnCalculator(final JavaScriptingSettings settings,
            final FlowVariableProvider flowVarProvider)
            throws InstantiationException, InvalidSettingsException {
        m_settings = settings;
        m_flowVarProvider = flowVarProvider;
        Expression compiledExpression = settings.getCompiledExpression();
        if (compiledExpression == null) {
            throw new InstantiationException(
                    "No compiled expression in settings");
        }
        m_expression = compiledExpression.getInstance();
        m_colSpec = new DataColumnSpec[]{m_settings.getNewColSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        return m_colSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        return new DataCell[]{calculate(row)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
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
        if (m_flowVarAssignmentMap == null) {
            m_flowVarAssignmentMap = new HashMap<InputField, Object>();
            for (Map.Entry<InputField, ExpressionField> e
                    : m_expression.getFieldMap().entrySet()) {
                InputField f = e.getKey();
                if (f.getFieldType().equals(FieldType.Variable)) {
                    Class<?> c = e.getValue().getFieldClass();
                    m_flowVarAssignmentMap.put(f,
                            m_flowVarProvider.readVariable(
                                    f.getColOrVarName(), c));
                }
            }
        }
        DataTableSpec spec = m_settings.getInputSpec();
        Class<?> returnType = m_settings.getReturnType();
        boolean isArrayReturn = m_settings.isArrayReturn();
        Map<InputField, Object> nameValueMap =
            new HashMap<InputField, Object>();
        nameValueMap.put(new InputField(Expression.ROWINDEX,
                FieldType.TableConstant), m_lastProcessedRow++);
        nameValueMap.put(new InputField(Expression.ROWID,
                FieldType.TableConstant), row.getKey().getString());
        nameValueMap.put(new InputField(Expression.ROWCOUNT,
                FieldType.TableConstant), m_flowVarProvider.getRowCount());
        nameValueMap.putAll(m_flowVarAssignmentMap);
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
            if (cell.isMissing()) {
                if (m_settings.isInsertMissingAsNull()) {
                    // leave value as null
                } else {
                    String message = "Row \"" + row.getKey() + "\" "
                        + "contains missing value in column \""
                        + columnSpec.getName() + "\" - returning missing";
                    if (!m_hasReportedMissing) {
                        m_hasReportedMissing = true;
                        LOGGER.warn(message + " (omitting further warnings)");
                    } else {
                        LOGGER.debug(message);
                    }
                    return DataType.getMissingCell();
                }
            } else {
                for (JavaSnippetType<?, ?, ?> t : JavaSnippetType.TYPES) {
                    if (t.checkCompatibility(cellType)) {
                        if (isArray) {
                            cellVal = t.asJavaArray((CollectionDataValue)cell);
                        } else {
                            cellVal = t.asJavaObject(cell);
                        }
                        break;
                    }
                }
            }
            nameValueMap.put(inputField, cellVal);
        }
        Object o = null;
        try {
            m_expression.set(nameValueMap);
            o = m_expression.evaluate();
            // class correctness is asserted by compiler
        } catch (Abort ee) {
            StringBuilder builder = new StringBuilder("Calculation aborted: ");
            String message = ee.getMessage();
            builder.append(message == null ? "<no details>" : message);
            throw new RuntimeException(builder.toString(), ee);
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
        DataCell result = null;
        for (JavaSnippetType<?, ?, ?> t : JavaSnippetType.TYPES) {
            if (returnType.equals(t.getJavaClass(false))) {
                if (o == null) {
                    result = DataType.getMissingCell();
                } else if (isArrayReturn) {
                    result = t.asKNIMEListCell((Object[])o);
                } else {
                    result = t.asKNIMECell(o);
                }
                break;
            }
        }
        if (result == null) {
            throw new InternalError("No mapping for objects of class "
                    + o.getClass().getName());
        }
        return result;
    }

}
