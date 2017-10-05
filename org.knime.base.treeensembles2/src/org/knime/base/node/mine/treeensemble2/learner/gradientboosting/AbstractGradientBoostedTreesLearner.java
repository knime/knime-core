/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.learner.gradientboosting;

import java.util.Map;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.memberships.IDataIndexManager;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public abstract class AbstractGradientBoostedTreesLearner extends AbstractGradientBoostingLearner {

    /**
     * @param config the configuration for the learner
     * @param data the data as it is provided by the user
     */
    public AbstractGradientBoostedTreesLearner(final GradientBoostingLearnerConfiguration config, final TreeData data) {
        super(config, data);
    }


    /**
     * Adapts the previous prediction by adding the predictions of the <b>tree</b> regulated by the respective
     * coefficients in <b>coefficientMap</b>.
     *
     * @param previousPrediction Prediction of the previous steps
     * @param tree the tree of the current iteration
     * @param coefficientMap contains the coefficients for the leafs of the tree
     */
    protected void adaptPreviousPrediction(final double[] previousPrediction, final TreeModelRegression tree,
        final Map<TreeNodeSignature, Double> coefficientMap) {
        TreeData data = getData();
        IDataIndexManager indexManager = getIndexManager();
        for (int i = 0; i < data.getNrRows(); i++) {
            PredictorRecord record = createPredictorRecord(data, indexManager, i);
            previousPrediction[i] += coefficientMap.get(tree.findMatchingNode(record).getSignature());
        }
    }

    /**
     * Calculates the coefficients for all the leafs of the <b>tree</b>
     *
     * @param previousPrediction the prediction of the previous iterations
     * @param tree tree of the current iteration
     * @param residualData the residual data for the current iteration
     * @return a map containing the coefficients for all leafs of <b>tree</b>
     */
    protected abstract Map<TreeNodeSignature, Double> calculateCoefficientMap(final double[] previousPrediction,
        final TreeModelRegression tree, final TreeData residualData);


    /**
     * @return the initial value for the first iteration
     */
    protected abstract double getInitialValue();

}
