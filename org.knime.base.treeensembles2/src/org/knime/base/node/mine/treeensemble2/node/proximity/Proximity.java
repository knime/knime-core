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
 *   05.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.proximity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.ThreadPool;

import com.google.common.collect.ArrayListMultimap;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class Proximity extends AbstractProximity{

    public static ProximityMatrix calcProximities(final BufferedDataTable[] tables,
        final TreeEnsembleModelPortObject modelPortObject, final ExecutionContext exec)
            throws InvalidSettingsException, InterruptedException, ExecutionException, CanceledExecutionException {

        ProximityMatrix proximityMatrix = null;
        boolean optionalTable = false;
        switch (tables.length) {
            case 1:
                if (tables[0].size() <= 65500) {
                    proximityMatrix = new SingleTableProximityMatrix(tables[0]);
                } else {
                    // this is unfortunate and we should maybe think of a different solution
                    proximityMatrix = new TwoTablesProximityMatrix(tables[0], tables[0]);
                }
                break;
            case 2:
                optionalTable = true;
                proximityMatrix = new TwoTablesProximityMatrix(tables[0], tables[1]);
                break;
            default:
                throw new IllegalArgumentException("Currently only up to two tables are supported.");
        }

        final TreeEnsembleModelPortObjectSpec modelSpec = modelPortObject.getSpec();
        final TreeEnsembleModel ensembleModel = modelPortObject.getEnsembleModel();
        int[][] learnColIndicesInTables = null;
        if (optionalTable) {
            learnColIndicesInTables = new int[][]{modelSpec.calculateFilterIndices(tables[0].getDataTableSpec()),
                modelSpec.calculateFilterIndices(tables[1].getDataTableSpec())};
        } else {
            learnColIndicesInTables = new int[][]{modelSpec.calculateFilterIndices(tables[0].getDataTableSpec())};
        }

        final ThreadPool tp = KNIMEConstants.GLOBAL_THREAD_POOL;
        final int procCount = 3 * Runtime.getRuntime().availableProcessors() / 2;
        final Semaphore semaphore = new Semaphore(procCount);
        final AtomicReference<Throwable> proxThrowableRef = new AtomicReference<Throwable>();

        final int nrTrees = ensembleModel.getNrModels();
        final Future<?>[] calcFutures = new Future<?>[nrTrees];
        exec.setProgress(0, "Starting proximity calculation per tree.");
        for (int i = 0; i < nrTrees; i++) {
            semaphore.acquire();
            finishedTree(i, exec, nrTrees);
            checkThrowable(proxThrowableRef);
            AbstractTreeModel treeModel = ensembleModel.getTreeModel(i);
            ExecutionMonitor subExec = exec.createSubProgress(0.0);
            if (optionalTable) {
                calcFutures[i] = tp.enqueue(new TwoTablesProximityCalcRunnable(proximityMatrix, tables,
                    learnColIndicesInTables, treeModel, modelPortObject, semaphore, proxThrowableRef, subExec));
            } else {
                calcFutures[i] = tp.enqueue(new SingleTableProximityCalcRunnable(proximityMatrix, tables,
                    learnColIndicesInTables, treeModel, modelPortObject, semaphore, proxThrowableRef, subExec));
            }
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



    private static class SingleTableProximityCalcRunnable extends ProximityCalcRunnable {

        /**
         * @param proximityMatrix
         * @param tables
         * @param learnColIndicesInTables
         * @param treeModel
         * @param ensembleModel
         * @param semaphore
         * @param proxThrowableRef
         * @param subExec
         */
        public SingleTableProximityCalcRunnable(final ProximityMatrix proximityMatrix, final BufferedDataTable[] tables,
            final int[][] learnColIndicesInTables, final AbstractTreeModel treeModel,
            final TreeEnsembleModelPortObject ensembleModel, final Semaphore semaphore,
            final AtomicReference<Throwable> proxThrowableRef, final ExecutionMonitor subExec) {
            super(proximityMatrix, tables, learnColIndicesInTables, treeModel, ensembleModel, semaphore,
                proxThrowableRef, subExec);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected int[][] getUpdateIndexPairs(final ArrayListMultimap<TreeNodeSignature, Integer> leafMap) {
            Collection<Collection<Integer>> leafs = leafMap.asMap().values();
            ArrayList<int[]> indexPairs = new ArrayList<int[]>();
            for (Collection<Integer> leaf : leafs) {
                Integer[] leafArray = leaf.toArray(new Integer[leaf.size()]);
                for (int i = 0; i < leafArray.length; i++) {
                    for (int j = i; j < leafArray.length; j++) {
                        indexPairs.add(new int[]{leafArray[i], leafArray[j]});
                    }
                }
            }

            return indexPairs.toArray(new int[indexPairs.size()][]);
        }

    }

    private abstract static class ProximityCalcRunnable implements Runnable {

        private final ProximityMatrix m_matrix;

        private final BufferedDataTable[] m_tables;

        private final int[][] m_learnColIndicesInTables;

        private final AbstractTreeModel m_treeModel;

        private final TreeEnsembleModelPortObject m_ensembleModel;

        private final Semaphore m_semaphore;

        private final AtomicReference<Throwable> m_proxThrowableRef;

        private final ExecutionMonitor m_exec;

        public BufferedDataTable[] getTables() {
            return m_tables;
        }

        public int[][] getLearnColIndices() {
            return m_learnColIndicesInTables;
        }

        public AbstractTreeModel getTreeModel() {
            return m_treeModel;
        }

        public TreeEnsembleModelPortObject getEnsemblePortObject() {
            return m_ensembleModel;
        }

        public ProximityCalcRunnable(final ProximityMatrix proximityMatrix, final BufferedDataTable[] tables,
            final int[][] learnColIndicesInTables, final AbstractTreeModel treeModel,
            final TreeEnsembleModelPortObject ensembleModel, final Semaphore semaphore,
            final AtomicReference<Throwable> proxThrowableRef, final ExecutionMonitor subExec) {
            m_matrix = proximityMatrix;
            m_tables = tables;
            m_learnColIndicesInTables = learnColIndicesInTables;
            m_treeModel = treeModel;
            m_ensembleModel = ensembleModel;
            m_semaphore = semaphore;
            m_proxThrowableRef = proxThrowableRef;
            m_exec = subExec;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                m_exec.checkCanceled();
                final ArrayListMultimap<TreeNodeSignature, Integer> leaf2InstancesMap =
                    computeLeafMap(m_ensembleModel, m_tables[0], m_learnColIndicesInTables[0], m_treeModel);
                m_exec.checkCanceled();
                m_matrix.incrementSync(getUpdateIndexPairs(leaf2InstancesMap));
            } catch (Throwable t) {
                m_proxThrowableRef.compareAndSet(null, t);
            } finally {
                m_semaphore.release();
            }
        }

        protected abstract int[][] getUpdateIndexPairs(ArrayListMultimap<TreeNodeSignature, Integer> leafMap);

        protected static ArrayListMultimap<TreeNodeSignature, Integer> computeLeafMap(
            final TreeEnsembleModelPortObject ensembleModel, final BufferedDataTable table,
            final int[] learnColIndicesInTable, final AbstractTreeModel treeModel) {
            final ArrayListMultimap<TreeNodeSignature, Integer> leaf2InstancesMap = ArrayListMultimap.create();
            DataTableSpec learnSpec = ensembleModel.getSpec().getLearnTableSpec();
            TreeEnsembleModel ensemble = ensembleModel.getEnsembleModel();
            // fill map with table1
            int indexTable1 = 0;
            for (DataRow row : table) {
                DataRow filterRow = new FilterColumnRow(row, learnColIndicesInTable);
                PredictorRecord record = ensemble.createPredictorRecord(filterRow, learnSpec);
                AbstractTreeNode treeNode = treeModel.findMatchingNode(record);
                TreeNodeSignature nodeSignature = treeNode.getSignature();
                leaf2InstancesMap.put(nodeSignature, indexTable1++);
            }
            return leaf2InstancesMap;
        }

    }

    private static class TwoTablesProximityCalcRunnable extends ProximityCalcRunnable {

        /**
         * @param proximityMatrix
         * @param tables
         * @param learnColIndicesInTables
         * @param treeModel
         * @param ensembleModel
         * @param semaphore
         * @param proxThrowableRef
         * @param subExec
         */
        public TwoTablesProximityCalcRunnable(final ProximityMatrix proximityMatrix, final BufferedDataTable[] tables,
            final int[][] learnColIndicesInTables, final AbstractTreeModel treeModel,
            final TreeEnsembleModelPortObject ensembleModel, final Semaphore semaphore,
            final AtomicReference<Throwable> proxThrowableRef, final ExecutionMonitor subExec) {
            super(proximityMatrix, tables, learnColIndicesInTables, treeModel, ensembleModel, semaphore,
                proxThrowableRef, subExec);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected int[][] getUpdateIndexPairs(final ArrayListMultimap<TreeNodeSignature, Integer> leafMap) {
            ArrayList<int[]> incrementIndicesList = new ArrayList<int[]>();
            // go through table2
            int indexTable2 = 0;
            TreeEnsembleModel ensemble = getEnsemblePortObject().getEnsembleModel();
            DataTableSpec learnSpec = getEnsemblePortObject().getSpec().getLearnTableSpec();
            BufferedDataTable table2 = getTables()[1];
            AbstractTreeModel treeModel = getTreeModel();
            int[] learnColIndicesInTable2 = getLearnColIndices()[1];
            for (DataRow row : table2) {
                DataRow filterRow = new FilterColumnRow(row, learnColIndicesInTable2);
                PredictorRecord record = ensemble.createPredictorRecord(filterRow, learnSpec);
                AbstractTreeNode treeNode = treeModel.findMatchingNode(record);
                TreeNodeSignature nodeSignature = treeNode.getSignature();
                List<Integer> instanceList = leafMap.get(nodeSignature);
                for (Integer table1Index : instanceList) {
                    incrementIndicesList.add(new int[]{table1Index, indexTable2});
                }
                indexTable2++;
            }
            return incrementIndicesList.toArray(new int[incrementIndicesList.size()][]);
        }

    }

}
