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
package org.knime.base.node.mine.treeensemble2.learner.gradientboosting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataIndexManager;
import org.knime.base.node.mine.treeensemble2.learner.TreeLearnerRegression;
import org.knime.base.node.mine.treeensemble2.learner.TreeNodeSignatureFactory;
import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostedTreesModel;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

import com.google.common.math.IntMath;

/**
 *
 * @author Adrian Nembach
 */
public abstract class AbstractGradientBoostedTreesLearner extends AbstractGradientBoostingLearner {

    /**
     * @param config
     * @param data
     */
    public AbstractGradientBoostedTreesLearner(final GradientBoostingLearnerConfiguration config, final TreeData data) {
        super(config, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractGradientBoostingModel learn(final ExecutionMonitor exec) throws CanceledExecutionException {
        GradientBoostingLearnerConfiguration config = getConfig();
        int nrModels = config.getNrModels();
        ArrayList<TreeModelRegression> models = new ArrayList<TreeModelRegression>(nrModels);
        ArrayList<Map<TreeNodeSignature, Double>> coefficientMaps =
            new ArrayList<Map<TreeNodeSignature, Double>>(nrModels);

        TreeNodeSignatureFactory signatureFactory = null;
        int maxLevels = config.getMaxLevels();
        if (maxLevels < TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) {
            int capacity = IntMath.pow(2, maxLevels - 1);
            signatureFactory = new TreeNodeSignatureFactory(capacity);
        } else {
            signatureFactory = new TreeNodeSignatureFactory();
        }

        double initialValue = getInitialValue();
        double[] predictionPrev = new double[getTarget().getNrRows()];
        Arrays.fill(predictionPrev, initialValue);
        TreeData residualData;
        final RandomData rd = config.createRandomData();
        for (int i = 0; i < nrModels; i++) {
            residualData = calculateResidualData(predictionPrev, getLossFunction());
            RandomData rdSingle =
                TreeEnsembleLearnerConfiguration.createRandomData(rd.nextLong(Long.MIN_VALUE, Long.MAX_VALUE));
            TreeLearnerRegression treeLearner =
                new TreeLearnerRegression(config, residualData, getIndexManager(), signatureFactory, rdSingle);
            TreeModelRegression tree = treeLearner.learnSingleTree(exec, rdSingle);
            Map<TreeNodeSignature, Double> coefficientMap = calculateCoefficientMap(predictionPrev, tree, residualData);
            adaptPreviousPrediction(predictionPrev, tree, coefficientMap);
            models.add(tree);
            coefficientMaps.add(coefficientMap);
        }
        return new GradientBoostedTreesModel(getConfig(), getData().getMetaData(),
            models.toArray(new TreeModelRegression[models.size()]), getData().getTreeType(), initialValue,
            coefficientMaps);
    }

    protected abstract LossFunction getLossFunction();

    protected void adaptPreviousPrediction(final double[] previousPrediction, final TreeModelRegression tree,
        final Map<TreeNodeSignature, Double> coefficientMap) {
        TreeData data = getData();
        DataIndexManager indexManager = getIndexManager();
        for (int i = 0; i < data.getNrRows(); i++) {
            PredictorRecord record = createPredictorRecord(data, indexManager, i);
            previousPrediction[i] += coefficientMap.get(tree.findMatchingNode(record).getSignature());
        }
    }

    protected abstract Map<TreeNodeSignature, Double> calculateCoefficientMap(double[] previousPrediction,
        TreeModelRegression tree, TreeData residualData);


    protected abstract double getInitialValue();

}
