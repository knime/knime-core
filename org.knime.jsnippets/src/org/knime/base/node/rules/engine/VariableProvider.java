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
 * ---------------------------------------------------------------------
 *
 * Created on 2013.04.26. by Gabor
 */
package org.knime.base.node.rules.engine;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.ext.sun.nodes.script.calculator.FlowVariableProvider;

/**
 * Extension of {@link FlowVariableProvider} to be able to get the row index too. <br/>
 * Please override {@link #getRowIndexLong()} and provide an always failing implementation for {@link #getRowIndex()}
 * for new implementations.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public interface VariableProvider extends FlowVariableProvider {
    /**
     * Simple implementation of the {@link VariableProvider} and a {@link SingleCellFactory}.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    abstract class SingleCellFactoryProto extends SingleCellFactory implements VariableProvider {
        private long m_index = 0L;

        /**
         * Constructs {@link SingleCellFactoryProto}.
         *
         * @param newColSpec The new column's specification.
         * @see SingleCellFactory#SingleCellFactory(DataColumnSpec)
         */
        public SingleCellFactoryProto(final DataColumnSpec newColSpec) {
            super(newColSpec);
        }

        /**
         * Constructs {@link SingleCellFactoryProto}.
         *
         * @param processConcurrently If to process the rows concurrently (must only be true if there are no
         *            interdependency between the rows).
         * @param newColSpec The new column's specification.
         * @see SingleCellFactory#SingleCellFactory(boolean, DataColumnSpec)
         */
        public SingleCellFactoryProto(final boolean processConcurrently, final DataColumnSpec newColSpec) {
            super(processConcurrently, newColSpec);
        }

        /**
         * Constructs {@link SingleCellFactoryProto}.
         *
         * @param processConcurrently If to process the rows concurrently (must only be true if there are no
         *            interdependency between the rows).
         * @param workerCount Number of workers.
         * @param maxQueueSize Maximum size of work queue.
         * @param newColSpec The new column's specification.
         * @see SingleCellFactory#SingleCellFactory(boolean, int, int, DataColumnSpec)
         */
        public SingleCellFactoryProto(final boolean processConcurrently, final int workerCount, final int maxQueueSize,
            final DataColumnSpec newColSpec) {
            super(processConcurrently, workerCount, maxQueueSize, newColSpec);
        }

        /**
         * {@inheritDoc}
         *
         * @deprecated Use {@link #getRowIndexLong()}
         */
        @Override
        @Deprecated
        public int getRowIndex() {
            return (int)m_index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getRowIndexLong() {
            return m_index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProgress(final long curRowNr, final long rowCount, final RowKey lastKey,
            final ExecutionMonitor exec) {
            m_index = curRowNr;
            super.setProgress(curRowNr, rowCount, lastKey, exec);
        }
    }

    /**
     * @return The row index (the first row is {@code 0}.
     * @deprecated Use {@link #getRowIndexLong()}.
     */
    @Deprecated
    int getRowIndex();

    /**
     * @return The row index (the first row is {@code 0}.
     * @since 3.2
     */
    default long getRowIndexLong() {
        return getRowIndex();
    }
}
