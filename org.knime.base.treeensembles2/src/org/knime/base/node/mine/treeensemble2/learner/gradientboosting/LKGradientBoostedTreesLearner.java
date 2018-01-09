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
 *   20.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.learner.gradientboosting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.memberships.IDataIndexManager;
import org.knime.base.node.mine.treeensemble2.learner.TreeLearnerRegression;
import org.knime.base.node.mine.treeensemble2.learner.TreeNodeSignatureFactory;
import org.knime.base.node.mine.treeensemble2.model.MultiClassGradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.Pair;
import org.knime.core.util.ThreadPool;

import com.google.common.math.IntMath;

/**
 * This class learns a {@link MultiClassGradientBoostedTreesModel}.
 *
 * @author Adrian Nembach, KNIME.com
 */
public final class LKGradientBoostedTreesLearner extends AbstractGradientBoostingLearner {

    /**
     * @param config configuration of learner
     * @param data data to learn on
     */
    public LKGradientBoostedTreesLearner(final GradientBoostingLearnerConfiguration config, final TreeData data) {
        super(config, data);
    }

    /**
     * {@inheritDoc}
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public MultiClassGradientBoostedTreesModel learn(final ExecutionMonitor exec)
        throws CanceledExecutionException, InterruptedException, ExecutionException {
        final TreeData data = getData();
        final TreeTargetNominalColumnData target = (TreeTargetNominalColumnData)data.getTargetColumn();
        final NominalValueRepresentation[] classNomVals = target.getMetaData().getValues();
        final int numClasses = classNomVals.length;
        final String[] classLabels = new String[numClasses];
        final int nrModels = getConfig().getNrModels();
        final int nrRows = target.getNrRows();
        final TreeModelRegression[][] models = new TreeModelRegression[nrModels][numClasses];
        final ArrayList<ArrayList<Map<TreeNodeSignature, Double>>> coefficientMaps =
            new ArrayList<ArrayList<Map<TreeNodeSignature, Double>>>(nrModels);
        // variables for parallelization
        final ThreadPool tp = KNIMEConstants.GLOBAL_THREAD_POOL;
        final AtomicReference<Throwable> learnThrowableRef = new AtomicReference<Throwable>();
        final int procCount = 3 * Runtime.getRuntime().availableProcessors() / 2;

        exec.setMessage("Transforming problem");
        // transform the original k class classification problem into k regression problems
        final TreeData[] actual = new TreeData[numClasses];
        for (int i = 0; i < numClasses; i++) {
            final double[] newTarget = calculateNewTarget(target, i);
            actual[i] = createNumericDataFromArray(newTarget);
            classLabels[i] = classNomVals[i].getNominalValue();
        }

        final RandomData rd = getConfig().createRandomData();

        final double[][] previousFunctions = new double[numClasses][nrRows];

        TreeNodeSignatureFactory signatureFactory = null;
        final int maxLevels = getConfig().getMaxLevels();
        if (maxLevels < TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) {
            int capacity = IntMath.pow(2, maxLevels - 1);
            signatureFactory = new TreeNodeSignatureFactory(capacity);
        } else {
            signatureFactory = new TreeNodeSignatureFactory();
        }

        exec.setMessage("Learn trees");
        for (int i = 0; i < nrModels; i++) {
            final Semaphore semaphore = new Semaphore(procCount);
            final ArrayList<Map<TreeNodeSignature, Double>> classCoefficientMaps =
                new ArrayList<Map<TreeNodeSignature, Double>>(numClasses);
            // prepare calculation of pseudoResiduals
            final double[][] probs = new double[numClasses][nrRows];
            for (int r = 0; r < nrRows; r++) {
                double sumExpF = 0;
                for (int j = 0; j < numClasses; j++) {
                    sumExpF += Math.exp(previousFunctions[j][r]);
                }
                for (int j = 0; j < numClasses; j++) {
                    probs[j][r] = Math.exp(previousFunctions[j][r]) / sumExpF;
                }
            }

            final Future<?>[] treeCoefficientMapPairs = new Future<?>[numClasses];
            for (int j = 0; j < numClasses; j++) {
                checkThrowable(learnThrowableRef);
                final RandomData rdSingle =
                    TreeEnsembleLearnerConfiguration.createRandomData(rd.nextLong(Long.MIN_VALUE, Long.MAX_VALUE));
                final ExecutionMonitor subExec = exec.createSubProgress(0.0);
                semaphore.acquire();
                treeCoefficientMapPairs[j] = tp.enqueue(new TreeLearnerCallable(rdSingle, probs[j], actual[j], subExec,
                    numClasses, previousFunctions[j], semaphore, learnThrowableRef, signatureFactory));
            }
            for (int j = 0; j < numClasses; j++) {
                checkThrowable(learnThrowableRef);
                semaphore.acquire();
                final Pair<TreeModelRegression, Map<TreeNodeSignature, Double>> pair =
                    (Pair<TreeModelRegression, Map<TreeNodeSignature, Double>>)treeCoefficientMapPairs[j].get();
                models[i][j] = pair.getFirst();
                classCoefficientMaps.add(pair.getSecond());
                semaphore.release();
            }
            checkThrowable(learnThrowableRef);
            coefficientMaps.add(classCoefficientMaps);
            exec.setProgress((double)i / nrModels, "Finished level " + i + "/" + nrModels);
        }

        return MultiClassGradientBoostedTreesModel.createMultiClassGradientBoostedTreesModel(getConfig(),
            data.getMetaData(), models, data.getTreeType(), 0, numClasses, coefficientMaps, classLabels);
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

    private class TreeLearnerCallable implements Callable<Pair<TreeModelRegression, Map<TreeNodeSignature, Double>>> {

        private final RandomData m_rd;

        private final double[] m_probs;

        private final TreeData m_actual;

        private final ExecutionMonitor m_subExec;

        private final int m_numClasses;

        private final double[] m_previousFunction;

        private final Semaphore m_releaseSemaphore;

        private final AtomicReference<Throwable> m_learnThrowableRef;

        private final TreeNodeSignatureFactory m_signatureFactory;

        public TreeLearnerCallable(final RandomData rd, final double[] probs, final TreeData actual,
            final ExecutionMonitor subExec, final int numClasses, final double[] previousFunction,
            final Semaphore releaseSemaphore, final AtomicReference<Throwable> learnThrowableRef, final TreeNodeSignatureFactory signatureFactory) {
            m_rd = rd;
            m_probs = probs;
            m_actual = actual;
            m_subExec = subExec;
            m_numClasses = numClasses;
            m_previousFunction = previousFunction;
            m_releaseSemaphore = releaseSemaphore;
            m_learnThrowableRef = learnThrowableRef;
            m_signatureFactory = signatureFactory;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Pair<TreeModelRegression, Map<TreeNodeSignature, Double>> call() throws Exception {
            try {
                final int nrRows = m_probs.length;
                final double residualData[] = new double[nrRows];
                final TreeTargetNumericColumnData classProbTarget = (TreeTargetNumericColumnData)m_actual.getTargetColumn();
                for (int r = 0; r < nrRows; r++) {
                    residualData[r] = classProbTarget.getValueFor(r) - m_probs[r];
                }
                final TreeData pseudoResiduals = createResidualDataFromArray(residualData, m_actual);
                final RowSample rowSample = getRowSampler().createRowSample(m_rd);
                final TreeLearnerRegression treeLearner =
                    new TreeLearnerRegression(getConfig(), pseudoResiduals, getIndexManager(), m_signatureFactory, m_rd, rowSample);
                final TreeModelRegression tree = treeLearner.learnSingleTree(m_subExec, m_rd);
                final Map<TreeNodeSignature, Double> coefficientMap =
                    calculateCoefficientMap(tree, pseudoResiduals, m_numClasses);
                adaptPreviousFunction(m_previousFunction, tree, coefficientMap);
                return new Pair<TreeModelRegression, Map<TreeNodeSignature, Double>>(tree, coefficientMap);
            } catch (Throwable t) {
                m_learnThrowableRef.compareAndSet(null, t);
                return null;
            } finally {
                m_releaseSemaphore.release();
            }
        }

    }

    private void adaptPreviousFunction(final double[] previousFunction, final TreeModelRegression tree,
        final Map<TreeNodeSignature, Double> coefficientMap) {
        final TreeData data = getData();
        final IDataIndexManager indexManager = getIndexManager();
        for (int i = 0; i < previousFunction.length; i++) {
            final PredictorRecord record = createPredictorRecord(data, indexManager, i);
            final TreeNodeSignature signature = tree.findMatchingNode(record).getSignature();
            previousFunction[i] += coefficientMap.get(signature);
        }
    }

    private Map<TreeNodeSignature, Double> calculateCoefficientMap(final TreeModelRegression tree,
        final TreeData pseudoResiduals, final double numClasses) {

        final List<TreeNodeRegression> leafs = tree.getLeafs();
        final Map<TreeNodeSignature, Double> coefficientMap = new HashMap<TreeNodeSignature, Double>();
        final TreeTargetNumericColumnData pseudoTarget = (TreeTargetNumericColumnData)pseudoResiduals.getTargetColumn();
        double learningRate = getConfig().getLearningRate();
        for (TreeNodeRegression leaf : leafs) {
            final int[] indices = leaf.getRowIndicesInTreeData();
            double sumTop = 0;
            double sumBottom = 0;
            for (int index : indices) {
                double val = pseudoTarget.getValueFor(index);
                sumTop += val;
                double absVal = Math.abs(val);
                sumBottom += Math.abs(absVal) * (1 - Math.abs(absVal));
            }
            final double coefficient = (numClasses - 1) / numClasses * (sumTop / sumBottom);
            coefficientMap.put(leaf.getSignature(), learningRate * coefficient);
        }
        return coefficientMap;
    }

    private double[] calculateNewTarget(final TreeTargetNominalColumnData oldTarget, final int classNomValIdx) {
        double[] newTarget = new double[oldTarget.getNrRows()];
        for (int i = 0; i < newTarget.length; i++) {
            newTarget[i] = oldTarget.getValueFor(i) == classNomValIdx ? 1.0 : 0.0;
        }
        return newTarget;
    }

    private TreeData createNumericDataFromArray(final double[] numericData) {
        TreeData data = getData();
        TreeTargetNominalColumnData nominalTarget = (TreeTargetNominalColumnData)data.getTargetColumn();
        TreeTargetNumericColumnMetaData newMeta =
            new TreeTargetNumericColumnMetaData(nominalTarget.getMetaData().getAttributeName());
        TreeTargetNumericColumnData newTarget =
            new TreeTargetNumericColumnData(newMeta, nominalTarget.getRowKeys(), numericData);
        return new TreeData(data.getColumns(), newTarget, data.getTreeType());
    }

}
