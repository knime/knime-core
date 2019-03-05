/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   27 Feb 2019 (albrecht): created
 */
package org.knime.core.data.transform;

import java.util.LinkedList;
import java.util.Queue;

import org.knime.core.data.DirectAccessTable;
import org.knime.core.data.sort.TableSortInformation;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.LockedQueue;

/**
 * Class which can execute a well defined queue of table transformations to a given original table. The executor can
 * be created with a {@link TableTransformationExecutorBuilder} to which transformations can be added consecutively.
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @since 3.8
 * @see TableTransformation
 *
 * @noextend This class is not intended to be subclassed by clients. Pending API
 * @noreference This class is not intended to be referenced by clients. Pending API
 */
public abstract class TableTransformationExecutor {

    private final DirectAccessTable m_table;
    private Queue<TableTransformation> m_transformations;

    /**
     * Creates a new {@link TableTransformationExecutor} by providing a non-null {@link DirectAccessTable} to which
     * transformations should be applied.
     * @param table the unaltered table, not null
     */
    protected TableTransformationExecutor(final DirectAccessTable table) {
        if (table == null) {
            throw new NullPointerException();
        }
        m_table = table;
        m_transformations = new LockedQueue<TableTransformation>(new LinkedList<TableTransformation>());
    }

    /**
     * Sets the queue of table transformations which should be applied to the original table.
     * @param transformations the transformations to be applied
     */
    protected void setTableTranformations(final Queue<TableTransformation> transformations) {
        if (transformations instanceof LockedQueue) {
            m_transformations = transformations;
        } else {
            m_transformations = new LockedQueue<TableTransformation>(transformations);
        }
    }

    /**
     * Retrieves the original table to which no transformations have been applied.
     * @return the original table
     */
    public DirectAccessTable getOriginalTable() {
        return m_table;
    }

    /**
     * returns the current queue of transformations to be applied to the table
     * @return the table transformation queue
     */
    public Queue<TableTransformation> getTableTransformations() {
        assert m_transformations instanceof LockedQueue;
        return m_transformations;
    }

    /**
     * Retrieves the next {@link TableTransformation} to be performed.
     * @return the next {@link TableTransformation}
     */
    public TableTransformation getNextTableTransformation() {
        return m_transformations.peek();
    }

    /**
     * Returns {@code true} if the queue of transformations has more elements.
     * @return {@code true} if the queue has more elements, {@code false} otherwise
     */
    public boolean hasNext() {
        return !m_transformations.isEmpty();
    }

    /**
     * Executes the next table transformation in the queue.
     * @param exec an execution monitor to set progress and check for cancellation
     * @return a {@link DirectAccessTable} to which the next transformation has been applied
     */
    public DirectAccessTable executeNext(final ExecutionMonitor exec) throws CanceledExecutionException {
        Queue<TableTransformation> queue = getTableTransformations();
        if (queue.isEmpty()) {
            return getOriginalTable();
        }
        return queue.poll().transform(exec);
    }

    /**
     * Executes all table transformations in the queue successively.
     * @param exec an execution monitor to set progress and check for cancellation
     * @return a {@link DirectAccessTable} to which all transformations have been applied
     */
    public DirectAccessTable execute(final ExecutionMonitor exec) throws CanceledExecutionException {
        Queue<TableTransformation> queue = getTableTransformations();
        if (queue.isEmpty()) {
            return getOriginalTable();
        }
        final int numTransforms = queue.size();
        DirectAccessTable transformedTable = getOriginalTable();
        while (!queue.isEmpty()) {
            ExecutionMonitor subProgress = exec.createSubProgress(1 - (queue.size() - numTransforms));
            transformedTable = queue.poll().transform(subProgress);
        }
        return transformedTable;
    }

    /**
     * Builder to create a {@link TableTransformationExecutor} by consecutively adding table transformations.
     *
     * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
     * @since 3.8
     *
     * @noextend This class is not intended to be subclassed by clients. Pending API
     * @noreference This class is not intended to be referenced by clients. Pending API
     */
    public static abstract class TableTransformationExecutorBuilder {

        private final DirectAccessTable m_originalTable;
        private final Queue<TableTransformation> m_transformations;

        protected TableTransformationExecutorBuilder(final DirectAccessTable originalTable) {
            m_originalTable = originalTable;
            m_transformations = new LinkedList<TableTransformation>();
        }

        /**
         * Adds a transformation to the current queue of transformations
         * @param transformation the transformation to add
         * @return this builder instance
         */
        protected TableTransformationExecutorBuilder addTransformation(final TableTransformation transformation) {
            m_transformations.add(transformation);
            return this;
        }

        /**
         * Adds a sort transformation to the current queue of transformations
         * @param sortInformation the sort to add
         * @return this builder instance
         */
        public abstract TableTransformationExecutorBuilder sort(final TableSortInformation sortInformation);

        /**
         * Adds a filter transformation to the current queue of transformations
         * @param filter the filter to add
         * @return this builder instance
         */
        public abstract TableTransformationExecutorBuilder filter(final TableFilterTransformation filter);

        /**
         * Returns the table before any transformations have been applied.
         * @return the original table
         */
        protected DirectAccessTable getOriginalTable() {
            return m_originalTable;
        }

        /**
         * Retrieves the current queue of transformations in a locked state. Adding and removing elements from this
         * queue is not possible, except processing transformations with {@link Queue#poll()}.
         * @return the queue of transformations
         * @see LockedQueue
         */
        protected Queue<TableTransformation> getLockedTransformations() {
            return new LockedQueue<TableTransformation>(m_transformations);
        }

        /**
         * Creates the {@link TableTransformationExecutor} which can execute the transformations added to the builder
         * in a FIFO fashion.
         * @return a new {@link TableTransformationExecutor} instance
         */
        public abstract TableTransformationExecutor build();
    }
}
