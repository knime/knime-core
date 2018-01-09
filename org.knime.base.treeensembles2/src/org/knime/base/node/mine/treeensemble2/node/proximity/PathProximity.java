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
 *   08.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.proximity;

import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.ThreadPool;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class PathProximity extends AbstractProximity {

    private BufferedDataTable[] m_tables;

    private int[][] m_learnIndicesInTables;

    private TreeEnsembleModelPortObject m_modelPO;

    private DataTableSpec m_learnSpec;

    public PathProximity(final BufferedDataTable[] tables, final TreeEnsembleModelPortObject modelPO)
        throws InvalidSettingsException {
        int tableCount = tables.length;
        TreeEnsembleModelPortObjectSpec modelSpec = modelPO.getSpec();
        m_learnIndicesInTables = new int[2][];
        m_learnIndicesInTables[0] = modelSpec.calculateFilterIndices(tables[0].getDataTableSpec());
        if (tableCount == 1) {
            m_tables = new BufferedDataTable[]{tables[0], tables[0]};
            m_learnIndicesInTables[1] = m_learnIndicesInTables[0];
        } else if (tableCount == 2) {
            m_tables = tables;
            m_learnIndicesInTables[1] = modelSpec.calculateFilterIndices(m_tables[1].getDataTableSpec());
        } else {
            throw new IllegalArgumentException("More than two tables are currently not supported.");
        }
        m_modelPO = modelPO;
        m_learnSpec = modelSpec.getLearnTableSpec();
        if (tableCount == 2) {
        }
    }

    public ProximityMatrix calculatePathProximities(final ExecutionContext exec)
        throws InterruptedException, CanceledExecutionException {
        final ThreadPool tp = KNIMEConstants.GLOBAL_THREAD_POOL;
        final int procCount = 3 * Runtime.getRuntime().availableProcessors() / 2;
        final Semaphore semaphore = new Semaphore(procCount);
        final AtomicReference<Throwable> proxThrowableRef = new AtomicReference<Throwable>();

        // The path proximity matrix is not symmetric if applied for a single table
        // therefore we have to use the two table approach even it is only a single table
        ProximityMatrix proximityMatrix = new TwoTablesProximityMatrix(m_tables[0], m_tables[1]);

        final int nrTrees = m_modelPO.getEnsembleModel().getNrModels();
        final Future<?>[] calcFutures = new Future<?>[nrTrees];
        exec.setProgress(0, "Starting proximity calculation per tree.");
        for (int i = 0; i < nrTrees; i++) {
            semaphore.acquire();
            finishedTree(i, exec, nrTrees);
            checkThrowable(proxThrowableRef);
            ExecutionMonitor subExec = exec.createSubProgress(0.0);
            calcFutures[i] =
                tp.enqueue(new PathProximityCalcRunnable(i, proximityMatrix, semaphore, proxThrowableRef, subExec));
        }

        for (int i = 0; i < procCount; i++) {
            semaphore.acquire();
            finishedTree(nrTrees - procCount + i, exec, nrTrees);
        }

        for (Future<?> future : calcFutures) {
            try {
                future.get();
            } catch (Exception e) {
                proxThrowableRef.compareAndSet(null, e);
            }
        }

        checkThrowable(proxThrowableRef);

        proximityMatrix.normalize(1.0 / nrTrees);

        return proximityMatrix;
    }

    private class PathProximityCalcRunnable implements Runnable {

        private int m_treeIdx;

        private ProximityMatrix m_proximityMatrix;

        private Semaphore m_semaphore;

        private AtomicReference<Throwable> m_proxThrowableRef;

        private ExecutionMonitor m_subExec;

        public PathProximityCalcRunnable(final int treeIdx, final ProximityMatrix proximityMatrix,
            final Semaphore semaphore, final AtomicReference<Throwable> proxThrowableRef,
            final ExecutionMonitor subExec) {
            m_proximityMatrix = proximityMatrix;
            m_treeIdx = treeIdx;
            m_semaphore = semaphore;
            m_proxThrowableRef = proxThrowableRef;
            m_subExec = subExec;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                TreeEnsembleModel ensemble = m_modelPO.getEnsembleModel();
                AbstractTreeModel tree = ensemble.getTreeModel(m_treeIdx);
                int table2Length = (int)m_tables[1].size();
                IndexTree dataTree = new IndexTree(table2Length);
                // fill dataTree with second table
                int indexSecondTable = 0;
                for (DataRow row : m_tables[1]) {
                    DataRow filterRow = new FilterColumnRow(row, m_learnIndicesInTables[1]);
                    PredictorRecord pred = ensemble.createPredictorRecord(filterRow, m_learnSpec);
                    dataTree.addIndex(indexSecondTable, tree.getTreePath(pred));
                    indexSecondTable++;
                }

                int table1Length = (int)m_tables[0].size();

                //                double[][] incrementMatrix = new double[table1Length][table2Length];

                int indexFirstTable = 0;
                for (DataRow row : m_tables[0]) {
                    DataRow filterRow = new FilterColumnRow(row, m_learnIndicesInTables[0]);
                    PredictorRecord pred = ensemble.createPredictorRecord(filterRow, m_learnSpec);
                    double[] incrementRow = dataTree.getAllPathProximities(tree.getTreePath(pred));
                    m_proximityMatrix.incrementSync(indexFirstTable, incrementRow);
                    indexFirstTable++;
                }
                //                m_proximityMatrix.incrementSync(incrementMatrix);
            } catch (Throwable t) {
                m_proxThrowableRef.compareAndSet(null, t);
            } finally {
                m_semaphore.release();
            }
        }

    }
}
