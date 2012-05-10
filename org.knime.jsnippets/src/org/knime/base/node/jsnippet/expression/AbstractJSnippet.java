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

import java.util.Map;

import org.knime.base.node.jsnippet.FlowVariableRepository;

/**
 *
 * @author Heiko Hofer
 */
public abstract class AbstractJSnippet {
    /** the id of the current row. */
    protected String ROWID = "";
    /** the index of the current row. */
    protected int ROWINDEX = -1;
    /** the number of rows of the input. */
    protected int ROWCOUNT = -1;

    private Map<String, Cell> m_cells;
    private FlowVariableRepository m_flowVars;

    /**
     * @param <T>
     * @param col
     * @param t
     * @return
     * @throws TypeException
     * @throws ColumnException
     */
    protected <T> T getCell(final String col, final T t) throws TypeException,
            ColumnException {
        if (m_cells.containsKey(col)) {
            Cell cell = m_cells.get(col);
            return cell.getValueAs(t);
        } else {
            throw new ColumnException("The column " + col + " does not exist.");
        }
    }

    protected <T> T getFlowVariable(final String var, final T t)
        throws TypeException, FlowVariableException {
        return m_flowVars.getValueAs(var, t);
    }


    public abstract void snippet() throws TypeException, ColumnException;
}
