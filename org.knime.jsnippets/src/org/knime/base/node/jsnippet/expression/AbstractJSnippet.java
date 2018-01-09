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
 * ------------------------------------------------------------------------
 *
 * History
 *   25.11.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.expression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.node.jsnippet.type.ConverterUtil;
import org.knime.base.node.jsnippet.util.FlowVariableRepository;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Heiko Hofer
 */
public abstract class AbstractJSnippet {
    private static NodeLogger LOGGER = NodeLogger.getLogger(AbstractJSnippet.class);

    /** the id of the current row. */
    public String ROWID = "";

    /** the index of the current row. */
    public int ROWINDEX = -1;

    /** the number of rows of the input. */
    public int ROWCOUNT = -1;

    private DataTableSpec m_inSpec;
    private Map<String, Cell> m_cellsMap;
    private List<Cell> m_cells;
    private List<String> m_columns;
    private FlowVariableRepository m_flowVars;

    private NodeLogger m_logger;

    /**
     * Create new instance.
     */
    public AbstractJSnippet() {
        m_logger = LOGGER;
    }

    /**
     * Get the cell contents of the given column.
     *
     * @param <T> the expected type
     * @param col the name of the column
     * @param t the type to be returned
     * @return the value of the cell of the given column
     * @throws TypeException if the column cannot provide the given type
     * @throws ColumnException if the column does not exist
     */
    @SuppressWarnings("unchecked")
    protected <T> T getCell(final String col, final T t) throws TypeException,
            ColumnException {
        if (m_cellsMap.containsKey(col)) {
            Cell cell = m_cellsMap.get(col);
            return (T)cell.getValueAs(Type.getMembersClass(t));
        } else {
            throw new ColumnException("The column " + col + " does not exist.");
        }
    }


    /**
     * Get the cell contents of the column with the given index.
     *
     * @param <T> the expected type
     * @param col the index of the column
     * @param t the type to be returned
     * @return the value of the cell of the given column
     * @throws TypeException if the column cannot provide the given type
     * @throws ColumnException if the column does not exist
     */
    @SuppressWarnings("unchecked")
    protected <T> T getCell(final int col, final T t) throws TypeException,
            ColumnException {
        if (col >= 0 || col < m_cells.size()) {
            Cell cell = m_cells.get(col);
            return (T)cell.getValueAs(Type.getMembersClass(t));
        } else {
            throw new ColumnException("The column index " + col
                    + " is out of the allowed range"
                    + " [0," + m_cells.size() + "].");
        }
    }

    /**
     * Returns true when the column is of the given type.
     *
     * @param <T> the expected type
     * @param column the name of the column
     * @param t the type to test for
     * @return true when the column is of the given type
     * @throws ColumnException if the column does not exist
     */
    protected <T> boolean isType(final String column, final T t)
            throws ColumnException {
        if (m_cellsMap.containsKey(column)) {
            return canProvideJavaType(m_inSpec.getColumnSpec(column),
                                      Type.getMembersClass(t));
        } else {
            throw new ColumnException("The column " + column
                    + " does not exist.");
        }
    }


    /**
     * Returns true when the column is of the given type.
     *
     * @param <T> the expected type
     * @param column the index of the column
     * @param t the type to test for
     * @return true when the column is of the given type
     * @throws ColumnException if the column does not exist
     */
    protected <T> boolean isType(final int column, final T t)
            throws ColumnException {
        if (column >= 0 || column < m_cells.size()) {
            return canProvideJavaType(m_inSpec.getColumnSpec(column),
                                      Type.getMembersClass(t));
        } else {
            throw new ColumnException("The column index " + column
                    + " is out of the allowed range"
                    + " [0," + m_cells.size() + "].");
        }
    }

    /**
     * Returns true when the cell of the given column is a missing cell.
     *
     * @param column the name of the column
     * @return true when the column is a missing cell
     * @throws ColumnException if the column does not exist
     */
    protected boolean isMissing(final String column)
            throws ColumnException {
        if (m_cellsMap.containsKey(column)) {
            Cell cell = m_cellsMap.get(column);
            return cell.isMissing();
        } else {
            throw new ColumnException("The column " + column
                    + " does not exist.");
        }
    }


    /**
     * Returns true when the cell of the given column is a missing cell.
     *
     * @param column the index of the column
     * @return true when the column is a missing cell
     * @throws ColumnException if the column does not exist
     */
    protected boolean isMissing(final int column)
            throws ColumnException {
        if (column >= 0 || column < m_cells.size()) {
            Cell cell = m_cells.get(column);
            return cell.isMissing();
        } else {
            throw new ColumnException("The column index " + column
                    + " is out of the allowed range"
                    + " [0," + m_cells.size() + "].");
        }
    }

    /**
     * Get the number of columns.
     * @return the number of columns.
     */
    protected int getColumnCount() {
        return m_cells.size();
    }

    /**
     * Get the name of the column at the specified index.
     * @param index the index
     * @return the name of the column at the given index
     */
    protected String getColumnName(final int index) {
        return m_columns.get(index);
    }


    /**
     * Returns true when a column with the given name exists.
     * @param column the column to test for
     * @return true when a column with the given name exists
     */
    protected boolean columnExists(final String column) {
        return m_cellsMap.get(column) != null;
    }

    /**
     * Returns true when a column with the given index exists.
     * @param index the index of the column
     * @return true when a column with the given index exists
     */
    protected boolean columnExists(final int index) {
        return index >= 0 && index < getColumnCount();
    }

    /**
     * Get the value of the flow variable.
     *
     * @param <T> the expected type
     * @param var the name of the flow variable
     * @param t the type to be returned
     * @return the flow variable with given name
     * @throws TypeException if the given type do not match to the type of
     * the flow variable
     * @throws FlowVariableException if the flow variable does not exist
     */
    protected <T> T getFlowVariable(final String var, final T t)
        throws TypeException, FlowVariableException {
        return m_flowVars.getValueAs(var, t);
    }

    /**
     * Get all flow variables of the given type.
     *
     * @param <T> the expected type
     * @param t the type to be returned
     * @return the flow variables of the given type.
     */
    protected <T> Map<String, T> getFlowVariables(final T t) {
        Map<String, T> flowVars = new LinkedHashMap<>();
        for (String s : m_flowVars.getFlowVariables(t.getClass())) {
            flowVars.put(s, getFlowVariable(s, t));
        }
        return flowVars;
    }

    /**
     * Check if a flow variable with given name exists.
     * @param name the name to test for
     * @return true when a flow variable with given name exists
     */
    protected boolean flowVariableExists(final String name) {
        return m_flowVars.getFlowVariable(name) != null;
    }

    /**
     * Check if a flow variable is of given type.
     * @param name the name of the flow variable
     * @param t the type to test for
     * @return true when a flow variable is of given type
     */
    protected boolean isFlowVariableOfType(final String name, final Object t) {
        return m_flowVars.isOfType(name, t.getClass());
    }

    /** Returns true when the cells of the column can provide the given type. */
    private boolean canProvideJavaType(final DataColumnSpec colSpec,
            @SuppressWarnings("rawtypes") final Class c) {
        DataType type = colSpec.getType();
        return ConverterUtil.getConverterFactory(type, c).isPresent();
    }

    /**
     * The method for custom code.
     *
     * @throws TypeException if a type mismatch with columns or flow variables
     * occurs
     * @throws ColumnException if an expected column does not exist
     * @throws Abort if node execution should be stopped
     */
    public abstract void snippet() throws TypeException, ColumnException, Abort;


    /**
     * Attach logger to be used by this java snippet instance.
     * @param logger the node logger
     */
    public void attachLogger(final NodeLogger logger) {
        m_logger = logger;
    }

    /**
     * Write warning message to the logger.
     *
     * @param o The object to print.
     */
    protected void logWarn(final Object o) {
        m_logger.warn(o);
    }

    /**
     * Write debugging message to the logger.
     *
     * @param o The object to print.
     */
    protected void logDebug(final Object o) {
        m_logger.debug(o);
    }

    /**
     * Write info message to the logger.
     *
     * @param o The object to print.
     */
    protected void logInfo(final Object o) {
        m_logger.info(o);
    }

    /**
     * Write error message to the logger.
     *
     * @param o The object to print.
     */
    protected void logError(final Object o) {
        m_logger.error(o);
    }

    /**
     * Write fatal error message to the logger.
     *
     * @param o The object to print.
     */
    protected void logFatal(final Object o) {
        m_logger.fatal(o);
    }

    /**
     * Write warning message and throwable to the logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    protected void logWarn(final Object o, final Throwable t) {
        m_logger.warn(o, t);
    }

    /**
     * Write debugging message and throwable to the logger.
     *
     * @param o The object to print.
     * @param t The exception to log, including its stack trace.
     */
    protected void logDebug(final Object o, final Throwable t) {
        m_logger.debug(o, t);
    }

    /**
     * Write info message and throwable to the logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    protected void logInfo(final Object o, final Throwable t) {
        m_logger.info(o, t);
    }

    /**
     * Write error message and throwable to the logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    protected void logError(final Object o, final Throwable t) {
        m_logger.error(o, t);
    }

    /**
     * Write fatal error message and throwable to the logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    protected void logFatal(final Object o, final Throwable t) {
        m_logger.fatal(o, t);
    }
}
