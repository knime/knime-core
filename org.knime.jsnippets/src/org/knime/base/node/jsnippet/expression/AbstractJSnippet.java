/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   25.11.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.expression;

import java.util.List;
import java.util.Map;

import org.knime.base.node.jsnippet.FlowVariableRepository;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.type.data.DataValueToJava;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;

/**
 *
 * @author Heiko Hofer
 */
public abstract class AbstractJSnippet {
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
    protected <T> T getCell(final String col, final T t) throws TypeException,
            ColumnException {
        if (m_cellsMap.containsKey(col)) {
            Cell cell = m_cellsMap.get(col);
            return cell.getValueAs(t);
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
    protected <T> T getCell(final int col, final T t) throws TypeException,
            ColumnException {
        if (col >= 0 || col < m_cells.size()) {
            Cell cell = m_cells.get(col);
            return cell.getValueAs(t);
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
                    t.getClass());
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
                    t.getClass());
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

    /** Returns true when the cells of the column can provide the given type. */
    private boolean canProvideJavaType(final DataColumnSpec colSpec,
            @SuppressWarnings("rawtypes") final Class c) {
        TypeProvider p = TypeProvider.getDefault();
        DataType type = colSpec.getType();
        DataType elemType = type.isCollectionType()
            ? type.getCollectionElementType() : type;
        DataValueToJava conv = p.getDataValueToJava(elemType,
                type.isCollectionType());
        return conv.canProvideJavaType(c);
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
}
