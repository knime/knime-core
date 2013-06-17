/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * Created on 2013.06.12. by Gabor
 */
package org.knime.base.data.statistics;

import java.util.Collection;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * A {@link BufferedDataTable}-based {@link SelectRank} implementation.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
class BufferedSelectRank extends SelectRank<BufferedDataContainer, BufferedDataTable> {

    /** Used to create temporary and final output table. */
    private ExecutionContext m_execContext;

    /**
     * @param inputTable The input table.
     * @param inclList the selected column names.
     * @param k The indices to select for each included columns. This should be rectangular, the second dimension is for
     *            the column indices (the position within {@code inclList}).
     */
    public BufferedSelectRank(final BufferedDataTable inputTable, final Collection<String> inclList, final int[][] k) {
        super(inputTable, inputTable.getRowCount(), inclList, k);
    }

    /**
     * Selects values for indices from the table passed in the constructor according to the settings and returns the
     * values in the output table.
     *
     * @param ctx To report progress & create temporary and final output tables.
     * @return The selected values.
     * @throws CanceledExecutionException If canceled.
     */
    public BufferedDataTable select(final ExecutionContext ctx) throws CanceledExecutionException {
        if (ctx == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_execContext = ctx;
        try {
            return (BufferedDataTable)super.selectInternal(ctx);
        } finally {
            m_execContext = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    BufferedDataContainer createDataContainer(final DataTableSpec spec, final boolean forceOnDisk) {
        return m_execContext.createDataContainer(spec, true, forceOnDisk ? 0 : -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void clearTable(final DataTable table) {
        if (!(table instanceof BufferedDataTable)) {
            NodeLogger.getLogger(getClass()).warn(
                "Can't clear table instance " + "of \"" + table.getClass().getSimpleName() + "\" - expected \""
                    + BufferedDataTable.class.getSimpleName() + "\"");
        } else {
            m_execContext.clearTable((BufferedDataTable)table);
        }
    }
}
