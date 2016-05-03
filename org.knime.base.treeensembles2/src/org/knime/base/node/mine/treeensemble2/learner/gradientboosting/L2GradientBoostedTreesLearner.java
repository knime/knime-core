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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnData;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataIndexManager;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;

import com.google.common.collect.ArrayListMultimap;

/**
 *
 * @author Adrian Nembach
 */
public class L2GradientBoostedTreesLearner extends AbstractGradientBoostedTreesLearner {

    private final LossFunction m_lossFunction = NegBinomLogLikelihood.INSTANCE;

    /**
     * @param config
     * @param data
     */
    public L2GradientBoostedTreesLearner(final GradientBoostingLearnerConfiguration config, final TreeData data) {
        super(config, data);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected LossFunction getLossFunction() {
        return m_lossFunction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<TreeNodeSignature, Double> calculateCoefficientMap(final double[] previousPrediction,
        final TreeModelRegression tree, final TreeData residualData) {

        ArrayListMultimap<TreeNodeSignature, Double> multiLeafMap = ArrayListMultimap.create();
        DataIndexManager indexManager = getIndexManager();
        TreeTargetNumericColumnData residualTarget = (TreeTargetNumericColumnData)residualData.getTargetColumn();
        for (int i = 0; i < residualData.getNrRows(); i++) {
            // we can use the residual data because it only differs in the target column from the actual data
            PredictorRecord record = createPredictorRecord(residualData, indexManager, i);
            multiLeafMap.put(tree.findMatchingNode(record).getSignature(), residualTarget.getValueFor(i));
        }
        Set<Map.Entry<TreeNodeSignature, Collection<Double>>> leafSet = multiLeafMap.asMap().entrySet();
        HashMap<TreeNodeSignature, Double> coefficientMap =
            new HashMap<TreeNodeSignature, Double>((int)(leafSet.size() / 0.75 + 1));
        for (Map.Entry<TreeNodeSignature, Collection<Double>> leaf : leafSet) {
            Collection<Double> values = leaf.getValue();
            double ySum = 0;
            double other = 0;
            for (Double val : values) {
                ySum += val;
                double absVal = Math.abs(val);
                other += absVal * (2 - absVal);
            }
            double coefficient = ySum / other;
            coefficientMap.put(leaf.getKey(), coefficient);
        }
        return coefficientMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double getInitialValue() {
        TreeTargetNumericColumnData target = getTarget();
        double mean = 0;
        for (int i = 0; i < target.getNrRows(); i++) {
            double val = target.getValueFor(i);
            if (val != 1 && val != -1) {
                throw new IllegalStateException(
                    "The L2GradientBoostedTrees algorithm only works with values 1 and -1 as target values.");
            }
            mean += val;
        }
        return mean / target.getNrRows();
    }

}
