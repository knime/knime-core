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
 *   22.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.ClassificationPriors;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNodeSurrogateCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeColumnCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSurrogateCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSurrogateOnlyDefDirCondition;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.sample.column.ColumnSample;
import org.knime.core.data.RowKey;

/**
 *
 * @author Adrian Nembach
 */
public class Surrogates {

    private static final TreeTargetNominalColumnMetaData SURROGATE_TARGET_META =
        new TreeTargetNominalColumnMetaData("SurrogateTarget", new NominalValueRepresentation[]{
            new NominalValueRepresentation("L", 0, -1), new NominalValueRepresentation("R", 1, -1)});

    /**
     * Creates a surrogate split that only contains the best split and the default (majority) direction. It does
     * <b>NOT</b> calculate any surrogate splits (and is therefore more efficient).
     *
     * @param dataMemberships
     * @param bestSplit
     * @return SurrogateSplit with conditions for both children. The conditions only contain the condition for the best
     *         split and the default condition (true for the child the most records go to and false for the other one).
     */
    public static SurrogateSplit createSurrogateSplitWithDefaultDirection(final DataMemberships dataMemberships,
        final SplitCandidate bestSplit) {
        TreeAttributeColumnData col = bestSplit.getColumnData();
        TreeNodeCondition[] conditions = bestSplit.getChildConditions();
        // get child marker for best split
        BitSet left = col.updateChildMemberships(conditions[0], dataMemberships);
        BitSet right = col.updateChildMemberships(conditions[1], dataMemberships);
        // decide which child the majority of the records goes to
        boolean majorityGoesLeft = left.cardinality() < right.cardinality() ? false : true;
        // create surrogate conditions
        TreeNodeSurrogateOnlyDefDirCondition condLeft =
            new TreeNodeSurrogateOnlyDefDirCondition((TreeNodeColumnCondition)conditions[0], majorityGoesLeft);
        TreeNodeSurrogateOnlyDefDirCondition condRight =
            new TreeNodeSurrogateOnlyDefDirCondition((TreeNodeColumnCondition)conditions[1], !majorityGoesLeft);
        BitSet[] childMarkers = new BitSet[]{left,right};
        fillInMissingChildMarkersWithDefault(bestSplit, childMarkers, majorityGoesLeft);
        return new SurrogateSplit(new AbstractTreeNodeSurrogateCondition[]{condLeft, condRight},
            new BitSet[]{left, right});
    }

    /**
     * This function searches for splits in the remaining columns of <b>colSample</b>. It is doing so by taking the
     * directions (left or right) that are induced by the <b>bestSplit</b> as new target.
     *
     * @param dataMemberships provides information which rows are in the current branch
     * @param bestSplit the best split for the current node
     * @param oldData the TreeData object that contains all attributes and the target
     * @param colSample provides information which columns are to be considered as surrogates
     * @param config the configuration
     * @param rd
     * @return a SurrogateSplit that contains the conditions for both children
     */
    public static SurrogateSplit learnSurrogates(final DataMemberships dataMemberships, final SplitCandidate bestSplit,
        final TreeData oldData, final ColumnSample colSample, final TreeEnsembleLearnerConfiguration config,
        final RandomData rd) {

        TreeAttributeColumnData bestSplitCol = bestSplit.getColumnData();
        TreeNodeCondition[] bestSplitChildConditions = bestSplit.getChildConditions();

        // calculate new Target
        BitSet bestSplitLeft = bestSplitCol.updateChildMemberships(bestSplitChildConditions[0], dataMemberships);
        BitSet bestSplitRight = bestSplitCol.updateChildMemberships(bestSplitChildConditions[1], dataMemberships);
        // create DataMemberships that only contains the instances that are not missed by bestSplit
        BitSet surrogateBitSet = (BitSet)bestSplitLeft.clone();
        surrogateBitSet.or(bestSplitRight);

        DataMemberships surrogateCalcDataMemberships = dataMemberships.createChildMemberships(surrogateBitSet);
        TreeTargetNominalColumnData newTarget =
            createNewTargetColumn(bestSplitLeft, bestSplitRight, oldData.getNrRows(), surrogateCalcDataMemberships);

        // find best splits on new target
        ArrayList<SplitCandidate> candidates = new ArrayList<SplitCandidate>();
        ClassificationPriors newTargetPriors = newTarget.getDistribution(surrogateCalcDataMemberships, config);
        for (TreeAttributeColumnData col : colSample) {
            if (col != bestSplitCol) {
                SplitCandidate candidate =
                    col.calcBestSplitClassification(surrogateCalcDataMemberships, newTargetPriors, newTarget, rd);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        }

        SplitCandidate[] candidatesWithBestAtHead = new SplitCandidate[candidates.size() + 1];
        candidatesWithBestAtHead[0] = bestSplit;
        for (int i = 1; i < candidatesWithBestAtHead.length; i++) {
            candidatesWithBestAtHead[i] = candidates.get(i - 1);
        }

        return calculateSurrogates(dataMemberships, candidatesWithBestAtHead);
    }

    private static TreeTargetNominalColumnData createNewTargetColumn(final BitSet bestSplitLeft,
        final BitSet bestSplitRight, final int nrRows, final DataMemberships dataMemberships) {

        final int[] relevantIndices = dataMemberships.getOriginalIndices();
        final int[] data = new int[nrRows];
        for (int i = 0; i < relevantIndices.length; i++) {
            if (bestSplitLeft.get(i)) {
                // corresponds to nominal value "L"
                data[i] = 0;
            } else if (bestSplitRight.get(i)) {
                // corresponds to nominal value "R"
                data[i] = 1;
            }
            // all other indices are left 0 but they will not be encountered because
            // we only look at the instances indexed by the dataMemberships
        }

        return new TreeTargetNominalColumnData(SURROGATE_TARGET_META, new RowKey[nrRows], data);
    }

    /**
     * This function finds the splits (in <b>candidates</b>) that best mirror the best split (<b>candidates[0]</b>). The
     * splits are compared to the so called <i>majority split</i> that sends all records to the child that the most rows
     * in the best split are sent to. This <i>majority split</i> is also always the last surrogate to guarantee that
     * every record is sent to a child even if all surrogate attributes are also missing.
     *
     * @param dataMemberships
     * @param candidates the first candidate must be the best split
     * @return A SplitCandidate containing surrogates
     */
    public static SurrogateSplit calculateSurrogates(final DataMemberships dataMemberships,
        final SplitCandidate[] candidates) {
        final SplitCandidate bestSplit = candidates[0];
        TreeAttributeColumnData bestSplitCol = bestSplit.getColumnData();
        TreeNodeCondition[] bestSplitChildConditions = bestSplit.getChildConditions();
        if (bestSplitChildConditions.length != 2) {
            throw new IllegalArgumentException("Surrogates can only be calculated for binary splits.");
        }

        BitSet bestSplitLeft = bestSplitCol.updateChildMemberships(bestSplitChildConditions[0], dataMemberships);
        BitSet bestSplitRight = bestSplitCol.updateChildMemberships(bestSplitChildConditions[1], dataMemberships);

        final double numRowsInNode = dataMemberships.getRowCount();
        // probability for a row to be in the current node
        final double probInNode = numRowsInNode / dataMemberships.getRowCountInRoot();
        // probability for a row to go left according to the best split
        final double bestSplitProbLeft = bestSplitLeft.cardinality() / numRowsInNode;
        // probability for a row to go right according to the best split
        final double bestSplitProbRight = bestSplitRight.cardinality() / numRowsInNode;

        // the majority rule is always the last surrogate and defines a default direction if all other
        // surrogates fail
        final boolean majorityGoesLeft = bestSplitProbRight > bestSplitProbLeft ? false : true;
        // see calculatAssociationMeasure() for more information
        final double errorMajorityRule = majorityGoesLeft ? bestSplitProbRight : bestSplitProbLeft;

        // stores association measure for candidates
        ArrayList<SurrogateCandidate> surrogateCandidates = new ArrayList<SurrogateCandidate>();

        for (int i = 1; i < candidates.length; i++) {
            SplitCandidate surrogate = candidates[i];
            TreeAttributeColumnData surrogateCol = surrogate.getColumnData();
            TreeNodeCondition[] surrogateChildConditions = surrogate.getChildConditions();
            if (surrogateChildConditions.length != 2) {
                throw new IllegalArgumentException("Surrogates can only be calculated for binary splits.");
            }
            BitSet surrogateLeft = surrogateCol.updateChildMemberships(surrogateChildConditions[0], dataMemberships);
            BitSet surrogateRight = surrogateCol.updateChildMemberships(surrogateChildConditions[1], dataMemberships);

            BitSet bothLeft = (BitSet)bestSplitLeft.clone();
            bothLeft.and(surrogateLeft);
            BitSet bothRight = (BitSet)bestSplitRight.clone();
            bothRight.and(surrogateRight);
            // the complement of a split (switching the children) has the same gain value as the original split
            BitSet complementBothLeft = (BitSet)bestSplitLeft.clone();
            complementBothLeft.and(surrogateRight);
            BitSet complementBothRight = (BitSet)bestSplitRight.clone();
            complementBothRight.and(surrogateLeft);

            // calculating the probability that the surrogate candidate and the best split send a case both in the same
            // direction is necessary because there might be missing values which are not send in either direction
            double probBothLeft = (bothLeft.cardinality() / numRowsInNode);
            double probBothRight = (bothRight.cardinality() / numRowsInNode);
            // the relative probability that the surrogate predicts the best split correctly
            double predictProb = probBothLeft + probBothRight;
            double probComplementBothLeft = (complementBothLeft.cardinality() / numRowsInNode);
            double probComplementBothRight = (complementBothRight.cardinality() / numRowsInNode);
            double complementPredictProb = probComplementBothLeft + probComplementBothRight;

            double associationMeasure = calculateAssociationMeasure(errorMajorityRule, predictProb);
            double complementAssociationMeasure = calculateAssociationMeasure(errorMajorityRule, complementPredictProb);
            boolean useComplement = complementAssociationMeasure > associationMeasure ? true : false;
            double betterAssociationMeasure = useComplement ? complementAssociationMeasure : associationMeasure;
            assert betterAssociationMeasure <= 1 : "Association measure can not be greater than 1.";
            if (betterAssociationMeasure > 0) {
                BitSet[] childMarkers = new BitSet[]{surrogateLeft, surrogateRight};
                surrogateCandidates
                    .add(new SurrogateCandidate(surrogate, useComplement, betterAssociationMeasure, childMarkers));
            }
        }

        BitSet[] childMarkers = new BitSet[]{bestSplitLeft, bestSplitRight};
        // if there are no surrogates, create condition with default rule as only surrogate
        if (surrogateCandidates.isEmpty()) {
            fillInMissingChildMarkers(bestSplit, childMarkers, surrogateCandidates, majorityGoesLeft);
            return new SurrogateSplit(new AbstractTreeNodeSurrogateCondition[]{
                new TreeNodeSurrogateOnlyDefDirCondition((TreeNodeColumnCondition)bestSplitChildConditions[0],
                    majorityGoesLeft),
                new TreeNodeSurrogateOnlyDefDirCondition((TreeNodeColumnCondition)bestSplitChildConditions[1],
                    !majorityGoesLeft)},
                childMarkers);
        }

        surrogateCandidates.sort(null);

        int condSize = surrogateCandidates.size() + 1;

        TreeNodeColumnCondition[] conditionsLeftChild = new TreeNodeColumnCondition[condSize];
        TreeNodeColumnCondition[] conditionsRightChild = new TreeNodeColumnCondition[condSize];

        conditionsLeftChild[0] = (TreeNodeColumnCondition)bestSplitChildConditions[0];
        conditionsRightChild[0] = (TreeNodeColumnCondition)bestSplitChildConditions[1];

        for (int i = 0; i < surrogateCandidates.size(); i++) {
            SurrogateCandidate surrogateCandidate = surrogateCandidates.get(i);
            TreeNodeCondition[] surrogateConditions = surrogateCandidate.getSplitCandidate().getChildConditions();
            if (surrogateCandidate.m_useComplement) {
                conditionsLeftChild[i + 1] = (TreeNodeColumnCondition)surrogateConditions[1];
                conditionsRightChild[i + 1] = (TreeNodeColumnCondition)surrogateConditions[0];
            } else {
                conditionsLeftChild[i + 1] = (TreeNodeColumnCondition)surrogateConditions[0];
                conditionsRightChild[i + 1] = (TreeNodeColumnCondition)surrogateConditions[1];
            }
        }

        // check if there are any rows missing in the best split
        if (!bestSplit.getMissedRows().isEmpty()) {
            // fill in any missing child markers
            fillInMissingChildMarkers(bestSplit, childMarkers, surrogateCandidates, majorityGoesLeft);
        }

        return new SurrogateSplit(
            new TreeNodeSurrogateCondition[]{new TreeNodeSurrogateCondition(conditionsLeftChild, majorityGoesLeft),
                new TreeNodeSurrogateCondition(conditionsRightChild, !majorityGoesLeft)},
            childMarkers);
    }

    /**
     * Only call this method if there are missing values in the bestSplit. Fills in the missing child markers from the
     * surrogates
     *
     * @param bestSplit
     * @param bestSplitChildMarkers
     * @param surrogates
     * @param majorityGoesLeft
     */
    private static void fillInMissingChildMarkers(final SplitCandidate bestSplit, final BitSet[] bestSplitChildMarkers,
        final List<SurrogateCandidate> surrogates, final boolean majorityGoesLeft) {
        BitSet bestSplitMissedRows = bestSplit.getMissedRows();
        BitSet leftChildMarker = bestSplitChildMarkers[0];
        BitSet rightChildMarker = bestSplitChildMarkers[1];

        for (int i = bestSplitMissedRows.nextSetBit(0); i >= 0; i = bestSplitMissedRows.nextSetBit(i + 1)) {
            boolean foundFill = false;
            // fill in missing child marker from surrogates
            for (SurrogateCandidate surrogate : surrogates) {
                BitSet surrogateLeftChildMarker = surrogate.getLeftChildMarker();
                BitSet surrogateRightChildMarker = surrogate.getRightChildMarker();
                if (surrogateLeftChildMarker.get(i)) {
                    leftChildMarker.set(i);
                    foundFill = true;
                    break;
                } else if (surrogateRightChildMarker.get(i)) {
                    rightChildMarker.set(i);
                    foundFill = true;
                    break;
                }
            }
            if (foundFill){
                continue;
            }
            // the child marker was also missing in all surrogates => use majority rule
            if (majorityGoesLeft) {
                leftChildMarker.set(i);
            } else {
                rightChildMarker.set(i);
            }

            // check for possible overflow
            if (i >= Integer.MAX_VALUE) {
                throw new RuntimeException(
                    "Possible overflow detected while iterating over a BitSet. Please check the implementation.");
            }
        }
    }

    private static void fillInMissingChildMarkersWithDefault(final SplitCandidate bestSplit, final BitSet[] childMarkers, final boolean majorityGoesLeft) {
        BitSet bestSplitMissedRows = bestSplit.getMissedRows();
        BitSet leftChildMarker = childMarkers[0];
        BitSet rightChildMarker = childMarkers[1];

        for (int i = bestSplitMissedRows.nextSetBit(0); i >= 0; i = bestSplitMissedRows.nextSetBit(i + 1)) {
            // fill in with default rule
            if (majorityGoesLeft) {
                leftChildMarker.set(i);
            } else {
                rightChildMarker.set(i);
            }

            // check for possible overflow
            if (i >= Integer.MAX_VALUE) {
                throw new RuntimeException(
                    "Possible overflow detected while iterating over a BitSet. Please check the implementation.");
            }
        }
    }

    /**
     *
     * @param errorMajorityRule is the error made (in relation to the best split) if we take the direction with the
     *            higher probability based on the best split.
     * @param predictProb the probability that the currently looked at surrogate candidate correctly predicts the best
     *            split
     * @return The association measure of the surrogate candidate with respect to the best split
     */
    private static double calculateAssociationMeasure(final double errorMajorityRule, final double predictProb) {
        // Suppose the best split sends 60% of the cases in the node left and 40% right
        // the majority rule would be to go left
        // if we do so we would be wrong in 40% of the cases in the node,
        // and these 40% (or 0.4) would be the errorMajorityRule

        // predictProb is the probability that the surrogate candidate sends a case to the same leaf as the
        // best split
        return (errorMajorityRule - (1 - predictProb)) / errorMajorityRule;
    }

    public static class SurrogateCandidate implements Comparable<SurrogateCandidate> {
        private final SplitCandidate m_splitCandidate;

        private final boolean m_useComplement;

        private final double m_associationMeasure;

        private final BitSet[] m_childMarkers;

        private SurrogateCandidate(final SplitCandidate splitCandidate, final boolean useComplement,
            final double associationMeasure, final BitSet[] childMarkers) {
            m_splitCandidate = splitCandidate;
            m_useComplement = useComplement;
            m_associationMeasure = associationMeasure;
            m_childMarkers = childMarkers;
        }

        public boolean getUseComplement() {
            return m_useComplement;
        }

        public SplitCandidate getSplitCandidate() {
            return m_splitCandidate;
        }

        public BitSet getLeftChildMarker() {
            return m_childMarkers[m_useComplement ? 1 : 0];
        }

        public BitSet getRightChildMarker() {
            return m_childMarkers[m_useComplement ? 0 : 1];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final SurrogateCandidate that) {
            // we want the first element in a sorted list to have the largest association measure
            return -Double.compare(m_associationMeasure, that.m_associationMeasure);
        }
    }
}
