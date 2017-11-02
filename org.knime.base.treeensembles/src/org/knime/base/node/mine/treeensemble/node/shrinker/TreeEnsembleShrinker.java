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
 *   May 11, 2015 (winter): created
 */
package org.knime.base.node.mine.treeensemble.node.shrinker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.base.node.mine.treeensemble.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble.model.TreeEnsembleModel.TreeType;
import org.knime.base.node.mine.treeensemble.model.TreeNodeClassification;
import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataRow;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;

/**
 * @author Patrick Winter, University of Konstanz
 */
class TreeEnsembleShrinker {

    private TreeEnsembleModel m_initialEnsemble;

    private Map<AbstractTreeModel<?>, List<String>> m_predictions;

    private List<String> m_correctClasses;

    private List<AbstractTreeModel<?>> m_currentTrees;

    private double m_currentAccuracy;

    private ExecutionContext m_exec;

    private ExecutionMonitor m_shrinkProgress;

    /**
     * Create a shrinker for the given ensemble.
     *
     * Note: This constructor will already create predictions for every tree on the given evaluation data (which might be time consuming).
     *
     * @param initialEnsemble The ensemble to shrink
     * @param evaluationData The data to evaluate on
     * @param targetColumn Target column in the evaluationData
     * @param exec The execution context
     * @throws CanceledExecutionException If execution has been canceled
     */
    public TreeEnsembleShrinker(final TreeEnsembleModel initialEnsemble, final BufferedDataTable evaluationData,
        final String targetColumn, final ExecutionContext exec) throws CanceledExecutionException {
        m_exec = exec;
        m_initialEnsemble = initialEnsemble;
        int targetIndex = evaluationData.getDataTableSpec().findColumnIndex(targetColumn);
        // Create list containing the correct classes
        m_correctClasses = new ArrayList<String>();
        for (DataRow row : evaluationData) {
            m_exec.checkCanceled();
            m_correctClasses.add(((StringValue)row.getCell(targetIndex)).getStringValue());
        }
        // Create predictions for each tree
        // Predictions for a single tree don't change only for the full ensemble
        // We only have to do the predictions once and can save time this way
        m_predictions = new HashMap<AbstractTreeModel<?>, List<String>>();
        m_currentTrees = new ArrayList<AbstractTreeModel<?>>();
        for (int i = 0; i < m_initialEnsemble.getNrModels(); i++) {
            AbstractTreeModel<?> tree = m_initialEnsemble.getTreeModel(i);
            m_currentTrees.add(tree);
            List<String> predictions = new ArrayList<String>();
            m_predictions.put(tree, predictions);
        }
        m_exec.setMessage("Predicting");
        ExecutionMonitor predictionProgress = m_exec.createSubProgress(0.5);
        int current = 0;
        for (DataRow row : evaluationData) {
            predictionProgress.setMessage("Predicting row " + (current + 1) + " of " + evaluationData.size());
            predictionProgress.setProgress(current++/(double)evaluationData.size());
            PredictorRecord record =
                m_initialEnsemble.createPredictorRecord(row,
                    m_initialEnsemble.getLearnAttributeSpec(evaluationData.getDataTableSpec()));
            for (AbstractTreeModel<?> tree : m_currentTrees) {
                m_exec.checkCanceled();
                TreeNodeClassification match = (TreeNodeClassification)tree.findMatchingNode(record);
                m_predictions.get(tree).add(match.getMajorityClassName());
            }
        }
        predictionProgress.setProgress(1);
        m_shrinkProgress = m_exec.createSubProgress(0.5);
        // Calculate accuracy for the current set of trees
        m_currentAccuracy = calcAccuracy(null);
    }

    /**
     * Automatically shrink to the best size found.
     *
     * Does a greedy shrink and picks the sub forest that scores the best accuracy.
     * Note: This will have the same runtime as shrinkTo(1).
     *
     * @throws CanceledExecutionException If execution has been canceled
     */
    public void autoShrink() throws CanceledExecutionException {
        m_exec.setMessage("Shrinking");
        // We want to keep track of the best accuracy we had and what ensemble produced it
        double bestAccuracy = m_currentAccuracy;
        List<AbstractTreeModel<?>> bestEnsemble = new ArrayList<AbstractTreeModel<?>>(m_currentTrees);
        // Calculate the number of calcAccuracy() calls so we can update the progress
        int numberOfCalcs = numberOfCalculations(m_currentTrees.size(), 1);
        int calcsDone = 0;
        while (m_currentTrees.size() > 1) {
            // Keep track of the best accuracy and what tree was removed to produce it
            AbstractTreeModel<?> treeToRemove = null;
            double maxAccuracy = -1;
            // For each tree in the current ensemble we want to know how the accuracy changes
            for (AbstractTreeModel<?> tree : m_currentTrees) {
                m_exec.checkCanceled();
                m_shrinkProgress.setMessage(calcsDone + " of " + numberOfCalcs + " calculations done");
                m_shrinkProgress.setProgress(calcsDone++/(double)numberOfCalcs);
                // Calculate accuracy without this tree
                double newAccuracy = calcAccuracy(tree);
                // If accuracy is better than what we had so far this is our current best selection
                if (newAccuracy > maxAccuracy) {
                    maxAccuracy = newAccuracy;
                    treeToRemove = tree;
                }
            }
            // Remove the tree that had the least negative impact on removal
            m_currentTrees.remove(treeToRemove);
            m_currentAccuracy = maxAccuracy;
            // If accuracy of the resulting ensemble is better than what we had so far this is our current best selection
            if (m_currentAccuracy >= bestAccuracy) {
                bestAccuracy = m_currentAccuracy;
                bestEnsemble = new ArrayList<AbstractTreeModel<?>>(m_currentTrees);
            }
        }
        // The ensemble with the best accuracy is our winner
        m_currentAccuracy = bestAccuracy;
        m_currentTrees = bestEnsemble;
    }

    /**
     * Shrinks the ensemble to the given size.
     *
     * @param numberOfTrees The number of trees left after shrinking
     * @throws CanceledExecutionException If execution has been canceled
     */
    public void shrinkTo(final int numberOfTrees) throws CanceledExecutionException {
        m_exec.setMessage("Shrinking");
        // Calculate the number of calcAccuracy() calls so we can update the progress
        int numberOfCalcs = numberOfCalculations(m_currentTrees.size(), numberOfTrees);
        int calcsDone = 0;
        // Run this until we only have the desired number of trees left
        while (m_currentTrees.size() > numberOfTrees) {
            // Keep track of the best accuracy and what tree was removed to produce it
            AbstractTreeModel<?> treeToRemove = null;
            double maxAccuracy = -1;
            for (AbstractTreeModel<?> tree : m_currentTrees) {
                m_exec.checkCanceled();
                m_shrinkProgress.setMessage(calcsDone + " of " + numberOfCalcs + " calculations done");
                m_shrinkProgress.setProgress(calcsDone++/(double)numberOfCalcs);
                double newAccuracy = calcAccuracy(tree);
                // If accuracy is better than what we had so far this is our current best selection
                if (newAccuracy > maxAccuracy) {
                    maxAccuracy = newAccuracy;
                    treeToRemove = tree;
                }
            }
            // Remove the tree that had the least negative impact on removal
            m_currentTrees.remove(treeToRemove);
            m_currentAccuracy = maxAccuracy;
        }
    }

    /**
     * Determine how many times calcAccuracy() is called.
     *
     * @param treesInEnsemble The number of trees in the ensemble
     * @param targetNumberOfTrees The number of trees left after shrinking
     * @return Number of times calcAccuracy() will have to be called
     */
    private int numberOfCalculations(final int treesInEnsemble, final int targetNumberOfTrees) {
        int treesToRemove = treesInEnsemble - targetNumberOfTrees;
        // We have to calculate the accuracy for every tree and repeat that as many times as the number of trees that we want to remove
        // The number of trees to calculate the accuracy for is decreased by one after each remove
        // 1+2+...+n = n)*(n+1)/2
        int numberOfCalcs = treesInEnsemble*(treesToRemove)-((treesToRemove-1)*treesToRemove/2);
        return numberOfCalcs;
    }

    /**
     * Returns the shrunk tree ensemble model.
     *
     * Note: If autoShrink() or shrinkTo() have not been called yet the model will contain the same trees as the initial model.
     *
     * @return The tree ensemble model
     */
    public TreeEnsembleModel getModel() {
        // Build a model based on the meta data of our initial model and the currently selected trees
        TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(false);
        config.setSaveTargetDistributionInNodes(true);
        return new TreeEnsembleModel(config, m_initialEnsemble.getMetaData(),
            m_currentTrees.toArray(new AbstractTreeModel[m_currentTrees.size()]), TreeType.Ordinary);
    }

    /**
     * @return The accuracy of the current model on the evaluation data
     */
    public double getAccuracy() {
        return m_currentAccuracy;
    }

    /**
     * Calculates the accuracy on the current ensemble when leaving the given tree out.
     *
     * @param excludedTree The tree to leave out of the prediction or null if no tree should be left out
     * @return The prediction accuracy
     */
    private double calcAccuracy(final AbstractTreeModel<?> excludedTree) {
        int correct = 0;
        for (int row = 0; row < m_correctClasses.size(); row++) {
            // Keep track of classes and how often they were voted
            Map<String, Integer> predictions = new HashMap<String, Integer>();
            // Collect the predictions of each tree
            for (AbstractTreeModel<?> tree : m_currentTrees) {
                // Check if this is the excluded tree
                if (!tree.equals(excludedTree)) {
                    String prediction = m_predictions.get(tree).get(row);
                    int predictionCount = predictions.containsKey(prediction) ? predictions.get(prediction) : 0;
                    predictions.put(prediction, predictionCount + 1);
                }
            }
            // Find the winner
            String finalPrediction = "";
            int maxCount = -1;
            for (Entry<String, Integer> entry : predictions.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    finalPrediction = entry.getKey();
                }
            }
            // If winner was the correct class count the number of correct predictions for the ensemble up by one
            if (finalPrediction.equals(m_correctClasses.get(row))) {
                correct++;
            }
        }
        // Return correct predictions in relation to number of rows in the evaluation data set
        return correct / (double)m_correctClasses.size();
    }

}
