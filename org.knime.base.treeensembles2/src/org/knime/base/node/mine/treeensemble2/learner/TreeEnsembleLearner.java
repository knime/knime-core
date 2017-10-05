/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 1, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.memberships.BitVectorDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.DefaultDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.IDataIndexManager;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeEnsembleModel.TreeType;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.sample.column.ColumnSample;
import org.knime.base.node.mine.treeensemble2.sample.column.ColumnSampleStrategy;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSampler;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.ThreadPool;

import com.google.common.math.IntMath;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeEnsembleLearner {

    private static final int REPORT_LEVEL = 3;

    private final TreeEnsembleLearnerConfiguration m_config;

    private final TreeData m_data;

    private RowSample[] m_rowSamples;

    private ColumnSampleStrategy[] m_columnSampleStrategies;

    private TreeEnsembleModel m_ensembleModel;

    private final IDataIndexManager m_indexManager;

    private final TreeNodeSignatureFactory m_signatureFactory;

    private final RowSampler m_rowSampler;

    /**
     * @param config
     * @param data
     * */
    public TreeEnsembleLearner(final TreeEnsembleLearnerConfiguration config, final TreeData data) {
        m_config = config;
        m_data = data;
        if (data.getTreeType() == TreeType.BitVector) {
            m_indexManager = new BitVectorDataIndexManager(m_data.getNrRows());
        } else {
            m_indexManager = new DefaultDataIndexManager(m_data);
        }
        m_rowSampler = config.createRowSampler(data);
        int maxLevel = config.getMaxLevels();
        if (maxLevel < TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) {
            // provided we have a binary tree (which is the default)
            // a tree can't have more than capacity nodes
            int capacity = IntMath.pow(2, maxLevel - 1);
            m_signatureFactory = new TreeNodeSignatureFactory(capacity);
        } else {
            m_signatureFactory = new TreeNodeSignatureFactory();
        }
    }

    public TreeEnsembleModel learnEnsemble(final ExecutionMonitor exec) throws CanceledExecutionException,
        ExecutionException {
        final int nrModels = m_config.getNrModels();
        final RandomData rd = m_config.createRandomData();
        final ThreadPool tp = KNIMEConstants.GLOBAL_THREAD_POOL;
        final AtomicReference<Throwable> learnThrowableRef = new AtomicReference<Throwable>();
        @SuppressWarnings("unchecked")
        final Future<TreeLearnerResult>[] modelFutures = new Future[nrModels];
        final int procCount = 3 * Runtime.getRuntime().availableProcessors() / 2;
        final Semaphore semaphore = new Semaphore(procCount);
        Callable<TreeLearnerResult[]> learnCallable = new Callable<TreeLearnerResult[]>() {
            @Override
            public TreeLearnerResult[] call() throws Exception {
                final TreeLearnerResult[] results = new TreeLearnerResult[nrModels];
                for (int i = 0; i < nrModels; i++) {
                    semaphore.acquire();
                    finishedTree(i - procCount, exec);
                    checkThrowable(learnThrowableRef);
                    RandomData rdSingle =
                        TreeEnsembleLearnerConfiguration.createRandomData(rd.nextLong(Long.MIN_VALUE, Long.MAX_VALUE));
                    ExecutionMonitor subExec = exec.createSubProgress(0.0);
                    modelFutures[i] =
                        tp.enqueue(new TreeLearnerCallable(subExec, rdSingle, learnThrowableRef, semaphore));
                }
                for (int i = 0; i < procCount; i++) {
                    semaphore.acquire();
                    finishedTree(nrModels - 1 + i - procCount, exec);
                }
                for (int i = 0; i < nrModels; i++) {
                    try {
                        results[i] = modelFutures[i].get();
                    } catch (Exception e) {
                        learnThrowableRef.compareAndSet(null, e);
                    }
                }
                return results;
            }

            private void finishedTree(final int treeIndex, final ExecutionMonitor progMon) {
                if (treeIndex > 0) {
                    progMon.setProgress(treeIndex / (double)nrModels, "Tree " + treeIndex + "/" + nrModels);
                }
            }
        };
        TreeLearnerResult[] modelResults = tp.runInvisible(learnCallable);
        checkThrowable(learnThrowableRef);
        AbstractTreeModel[] models = new AbstractTreeModel[nrModels];
        m_rowSamples = new RowSample[nrModels];
        m_columnSampleStrategies = new ColumnSampleStrategy[nrModels];
        for (int i = 0; i < nrModels; i++) {
            models[i] = modelResults[i].m_treeModel;
            m_rowSamples[i] = modelResults[i].m_rowSample;
            m_columnSampleStrategies[i] = modelResults[i].m_rootColumnSampleStrategy;
        }
        m_ensembleModel = new TreeEnsembleModel(m_config, m_data.getMetaData(), models, m_data.getTreeType());
        return m_ensembleModel;
    }

    /** @return the rowSamples */
    public RowSample[] getRowSamples() {
        return m_rowSamples;
    }

    public BufferedDataTable createColumnStatisticTable(final ExecutionContext exec) throws CanceledExecutionException {
        BufferedDataContainer c = exec.createDataContainer(getColumnStatisticTableSpec());
        final int nrModels = m_ensembleModel.getNrModels();
        final TreeAttributeColumnData[] columns = m_data.getColumns();
        final int nrAttributes = columns.length;
        int[][] columnOnLevelCounts = new int[REPORT_LEVEL][nrAttributes];
        int[][] columnInLevelSampleCounts = new int[REPORT_LEVEL][nrAttributes];
        for (int i = 0; i < nrModels; i++) {
            final AbstractTreeModel<?> treeModel = m_ensembleModel.getTreeModel(i);
            for (int level = 0; level < REPORT_LEVEL; level++) {
                for (AbstractTreeNode treeNodeOnLevel : treeModel.getTreeNodes(level)) {
                    TreeNodeSignature sig = treeNodeOnLevel.getSignature();
                    ColumnSampleStrategy colStrat = m_columnSampleStrategies[i];
                    ColumnSample cs = colStrat.getColumnSampleForTreeNode(sig);
                    for (TreeAttributeColumnData col : cs) {
                        final int index = col.getMetaData().getAttributeIndex();
                        columnInLevelSampleCounts[level][index] += 1;
                    }
                    int splitAttIdx = treeNodeOnLevel.getSplitAttributeIndex();
                    if (splitAttIdx >= 0) {
                        columnOnLevelCounts[level][splitAttIdx] += 1;
                    }

                }
            }
        }

        for (int i = 0; i < nrAttributes; i++) {
            String name = columns[i].getMetaData().getAttributeName();
            int[] counts = new int[2 * REPORT_LEVEL];
            for (int level = 0; level < REPORT_LEVEL; level++) {
                counts[level] = columnOnLevelCounts[level][i];
                counts[REPORT_LEVEL + level] = columnInLevelSampleCounts[level][i];
            }
            DataRow row = new DefaultRow(name, counts);
            c.addRowToTable(row);
            exec.checkCanceled();
        }
        c.close();
        return c.getTable();
    }

    private static DataTableSpec COLUMN_STAT_TABLE_SPEC;

    public synchronized static DataTableSpec getColumnStatisticTableSpec() {
        if (COLUMN_STAT_TABLE_SPEC == null) {
            DataColumnSpec[] cols = new DataColumnSpec[2 * REPORT_LEVEL];
            for (int level = 0; level < REPORT_LEVEL; level++) {
                String splitName = "#splits (level " + level + ")";
                String candidateName = "#candidates (level " + level + ")";
                cols[level] = new DataColumnSpecCreator(splitName, IntCell.TYPE).createSpec();
                cols[REPORT_LEVEL + level] = new DataColumnSpecCreator(candidateName, IntCell.TYPE).createSpec();
            }
            COLUMN_STAT_TABLE_SPEC = new DataTableSpec("Tree Ensemble Column Statistic", cols);
        }
        return COLUMN_STAT_TABLE_SPEC;
    }

    private void checkThrowable(final AtomicReference<Throwable> learnThrowableRef) throws CanceledExecutionException {
        Throwable th = learnThrowableRef.get();
        if (th != null) {
            if (th instanceof CanceledExecutionException) {
                throw (CanceledExecutionException)th;
            } else if (th instanceof RuntimeException) {
                throw (RuntimeException)th;
            } else {
                throw new RuntimeException(th);
            }
        }
    }

    private final class TreeLearnerCallable implements Callable<TreeLearnerResult> {

        private final ExecutionMonitor m_exec;

        private final RandomData m_rd;

        private final Semaphore m_releaseSemaphore;

        private final AtomicReference<Throwable> m_throwableReference;

        /**
         *  */
        public TreeLearnerCallable(final ExecutionMonitor exec, final RandomData rd,
            final AtomicReference<Throwable> th, final Semaphore semaphore) {
            m_exec = exec;
            m_rd = rd;
            m_throwableReference = th;
            m_releaseSemaphore = semaphore;
        }

        /** {@inheritDoc} */
        @Override
        public TreeLearnerResult call() throws Exception {
            try {
                AbstractTreeLearner learner;
                final RowSample rowSample = m_rowSampler.createRowSample(m_rd);
                if (m_data.getMetaData().isRegression()) {
                    learner = new TreeLearnerRegression(m_config, m_data, m_indexManager, m_signatureFactory, m_rd, rowSample);
                } else {
                    learner = new TreeLearnerClassification(m_config, m_data, m_indexManager, m_signatureFactory, m_rd, rowSample);
                }
                AbstractTreeModel model = learner.learnSingleTree(m_exec, m_rd);
                final ColumnSampleStrategy colSamplingStrategy = learner.getColSamplingStrategy();
                TreeLearnerResult result = new TreeLearnerResult(model, rowSample, colSamplingStrategy);
                m_exec.setProgress(1.0);
                return result;
            } catch (Throwable t) {
                m_throwableReference.compareAndSet(null, t);
                return null;
            } finally {
                m_releaseSemaphore.release();
            }
        }
    }

    private final static class TreeLearnerResult {
        private final AbstractTreeModel m_treeModel;

        private final RowSample m_rowSample;

        private final ColumnSampleStrategy m_rootColumnSampleStrategy;

        /**
         * @param treeModel
         * @param rowSample
         * @param columnSampleStrategy
         */
        private TreeLearnerResult(final AbstractTreeModel treeModel, final RowSample rowSample,
            final ColumnSampleStrategy columnSampleStrategy) {
            m_treeModel = treeModel;
            m_rowSample = rowSample;
            m_rootColumnSampleStrategy = columnSampleStrategy;
        }

    }

}
