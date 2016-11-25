/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * History
 *   19.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.learner.gradientboosting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnData;
import org.knime.base.node.mine.treeensemble2.learner.TreeLearnerRegression;
import org.knime.base.node.mine.treeensemble2.learner.TreeNodeSignatureFactory;
import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

import com.google.common.math.IntMath;
import com.google.common.primitives.Doubles;

/**
 * This class learns a Gradient Boosted Trees model for regression using the Huber loss.
 *
 * @author Adrian Nembach, KNIME.com
 */
public final class MGradientBoostedTreesLearner extends AbstractGradientBoostedTreesLearner {


    /**
     * @param config the configuration for the learner
     * @param data the data on which to learn on
     */
    public MGradientBoostedTreesLearner(final GradientBoostingLearnerConfiguration config, final TreeData data) {
        super(config, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractGradientBoostingModel learn(final ExecutionMonitor exec) throws CanceledExecutionException {
        final TreeData actualData = getData();
        final GradientBoostingLearnerConfiguration config = getConfig();
        final int nrModels = config.getNrModels();
        final TreeTargetNumericColumnData actualTarget = getTarget();
        final double initialValue = actualTarget.getMedian();
        final ArrayList<TreeModelRegression> models = new ArrayList<TreeModelRegression>(nrModels);
        final ArrayList<Map<TreeNodeSignature, Double>> coefficientMaps =
            new ArrayList<Map<TreeNodeSignature, Double>>(nrModels);
        final double[] previousPrediction = new double[actualTarget.getNrRows()];
        Arrays.fill(previousPrediction, initialValue);
        final RandomData rd = config.createRandomData();
        final double alpha = config.getAlpha();
        TreeNodeSignatureFactory signatureFactory = null;
        final int maxLevels = config.getMaxLevels();
        // this should be the default
        if (maxLevels < TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) {
            final int capacity = IntMath.pow(2, maxLevels - 1);
            signatureFactory = new TreeNodeSignatureFactory(capacity);
        } else {
            signatureFactory = new TreeNodeSignatureFactory();
        }
        exec.setMessage("Learning model");
        TreeData residualData;
        for (int i = 0; i < nrModels; i++) {
            final double[] residuals = new double[actualTarget.getNrRows()];
            for (int j = 0; j < actualTarget.getNrRows(); j++) {
                residuals[j] = actualTarget.getValueFor(j) - previousPrediction[j];
            }
            final double quantile = calculateAlphaQuantile(residuals, alpha);
            final double[] gradients = new double[residuals.length];
            for (int j = 0; j < gradients.length; j++) {
                gradients[j] = Math.abs(residuals[j]) <= quantile ? residuals[j] : quantile * Math.signum(residuals[j]);
            }
            residualData = createResidualDataFromArray(gradients, actualData);
            final RandomData rdSingle =
                TreeEnsembleLearnerConfiguration.createRandomData(rd.nextLong(Long.MIN_VALUE, Long.MAX_VALUE));
            final RowSample rowSample = getRowSampler().createRowSample(rdSingle);
            final TreeLearnerRegression treeLearner =
                new TreeLearnerRegression(getConfig(), residualData, getIndexManager(), signatureFactory, rdSingle, rowSample);
            final TreeModelRegression tree = treeLearner.learnSingleTree(exec, rdSingle);
            final Map<TreeNodeSignature, Double> coefficientMap = calcCoefficientMap(residuals, quantile, tree);
            adaptPreviousPrediction(previousPrediction, tree, coefficientMap);
            models.add(tree);
            coefficientMaps.add(coefficientMap);
            exec.setProgress(((double)i) / nrModels, "Finished level " + i +"/" + nrModels);
        }

        return new GradientBoostedTreesModel(getConfig(), actualData.getMetaData(),
            models.toArray(new TreeModelRegression[models.size()]), actualData.getTreeType(), initialValue,
            coefficientMaps);
    }

    private Map<TreeNodeSignature, Double> calcCoefficientMap(final double[] residuals, final double quantile,
        final TreeModelRegression tree) {
        final List<TreeNodeRegression> leafs = tree.getLeafs();
        final Map<TreeNodeSignature, Double> coefficientMap =
            new HashMap<TreeNodeSignature, Double>((int)(leafs.size() / 0.75 + 1));
        final double learningRate = getConfig().getLearningRate();
        for (TreeNodeRegression leaf : leafs) {
            final int[] indices = leaf.getRowIndicesInTreeData();
            final double[] values = new double[indices.length];
            for (int i = 0; i < indices.length; i++) {
                values[i] = residuals[indices[i]];
            }
            final double median = calcMedian(values);
            double sum = 0;
            for (int i = 0; i < values.length; i++) {
                sum += Math.signum(values[i] - median) * Math.min(quantile, Math.abs(values[i] - median));
            }
            final double coefficient = median + (1.0 / values.length) * sum;
            coefficientMap.put(leaf.getSignature(), coefficient * learningRate);
        }
        return coefficientMap;
    }

    private static double calculateAlphaQuantile(final double[] array, final double alpha) {
        final Integer[] idx = new Integer[array.length];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = i;
        }
        Comparator<Integer> idxComp = new Comparator<Integer>() {

            @Override
            public int compare(final Integer arg0, final Integer arg1) {
                return Doubles.compare(array[arg0], array[arg1]);
            }

        };
        Arrays.sort(idx, idxComp);
        final int quantileIndex = (int)(alpha * array.length);
        return array[idx[quantileIndex]];
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<TreeNodeSignature, Double> calculateCoefficientMap(final double[] previousPrediction,
        final TreeModelRegression tree, final TreeData residualData) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double getInitialValue() {
        return 0;
    }

}
