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
 *   18.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.random.RandomData;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnData;
import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

import com.google.common.math.IntMath;

/**
 *
 * @author Adrian Nembach
 */
public class GeneralGradientBoostingLearner extends AbstractGradientBoostingLearner {

    /**
     * @param config
     * @param data
     */
    public GeneralGradientBoostingLearner(final GradientBoostingLearnerConfiguration config, final TreeData data) {
        super(config, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractGradientBoostingModel learn(final ExecutionMonitor exec) throws CanceledExecutionException {
     // start with actual target
        List<TreeModelRegression> models = new ArrayList<TreeModelRegression>();
        List<Double> coefficients = new ArrayList<Double>();
        TreeData gradient = getData();
        double[] predictionPrev = calculateMeanPrediction((TreeTargetNumericColumnData)getData().getTargetColumn());
        // get initial value (same for all datapoints)
        double initialVal = predictionPrev[0];
        final RandomData rd = getConfig().createRandomData();
        // TODO resolve dummy parameters with real ones
        LossFunction lossFunction = LeastSquares.INSTANCE;
        TreeNodeSignatureFactory signatureFactory = null;
        int maxLevels = getConfig().getMaxLevels();
        if (maxLevels < TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) {
            int capacity = IntMath.pow(2, maxLevels - 1);
            signatureFactory = new TreeNodeSignatureFactory(capacity);
        } else {
            signatureFactory = new TreeNodeSignatureFactory();
        }
        for (int i = 0; i < getConfig().getNrModels(); i++) {
            //            predictionPrev = learningModel.predict(getData(), m_indexManager);
            gradient = calculateResidualData(predictionPrev, lossFunction);
            RandomData rdSingle =
                TreeEnsembleLearnerConfiguration.createRandomData(rd.nextLong(Long.MIN_VALUE, Long.MAX_VALUE));
            TreeLearnerRegression treeLearner =
                new TreeLearnerRegression(getConfig(), gradient, getIndexManager(), signatureFactory, rdSingle);
            TreeModelRegression tree = treeLearner.learnSingleTree(exec, rdSingle);
            double[] predictionTree = predictTreeModel(tree);
            double coefficient = calculateCoefficient(predictionPrev, predictionTree, lossFunction, 0.001);
            models.add(tree);
            coefficients.add(coefficient);
            adaptPredictionPrev(predictionPrev, predictionTree, coefficient);
        }
        double[] coefficientsArray = new double[coefficients.size()];
        for (int i = 0; i < coefficientsArray.length; i++) {
            coefficientsArray[i] = coefficients.get(i);
        }

        return new GradientBoostingModel(getConfig(), getData().getMetaData(), models.toArray(new TreeModelRegression[models.size()]),
            getData().getTreeType(), initialVal, coefficientsArray);
    }

    private double calculateCoefficient(final double[] previousPrediction, final double[] predictionNewModel,
        final LossFunction lossFunction, final double stepsize) {
        UnivariateFunction f = new UnivariateFunction() {

            @Override
            public double value(final double x) {
                double sum = 0;
                TreeTargetNumericColumnData target = (TreeTargetNumericColumnData)getData().getTargetColumn();
                for (int i = 0; i < previousPrediction.length; i++) {
                    sum += lossFunction.calculateLoss(target.getValueFor(i), previousPrediction[i] + x * predictionNewModel[i]);
                }
                return sum;
            }
        };

        BrentOptimizer optimizer = new BrentOptimizer(1e-6, 1e-12);
        UnivariatePointValuePair coefficient = optimizer.optimize(new UnivariateObjectiveFunction(f), new MaxEval(100), GoalType.MINIMIZE, new SearchInterval(-100, 100));
        System.out.println(coefficient.getPoint() + " : " + coefficient.getValue());
        return coefficient.getPoint();
    }



}
