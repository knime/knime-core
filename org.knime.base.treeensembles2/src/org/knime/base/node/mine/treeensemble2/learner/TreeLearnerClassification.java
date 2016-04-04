/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 2, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.ClassificationPriors;
import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnData;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.RootDataMemberships;
import org.knime.base.node.mine.treeensemble2.model.TreeModelClassification;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeClassification;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeTrueCondition;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.MissingValueHandling;
import org.knime.base.node.mine.treeensemble2.sample.column.ColumnSample;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class TreeLearnerClassification extends AbstractTreeLearner {

    /**
     * @param config
     * @param data
     */
    TreeLearnerClassification(final TreeEnsembleLearnerConfiguration config, final TreeData data,
        final DataIndexManager indexManager, final TreeNodeSignatureFactory signatureFactory,
        final RandomData randomData) {
        super(config, data, indexManager, signatureFactory, randomData);
        if (config.isRegression()) {
            throw new IllegalStateException("Can't learn classification model on numeric target");
        }
    }

    @Override
    public TreeModelClassification learnSingleTree(final ExecutionMonitor exec, final RandomData rd)
        throws CanceledExecutionException {
        return learnSingleTreeRecursive(exec, rd);
    }

    private TreeModelClassification learnSingleTreeRecursive(final ExecutionMonitor exec, final RandomData rd)
        throws CanceledExecutionException {
        final TreeData data = getData();
        final RowSample rowSampling = getRowSampling();
        final TreeEnsembleLearnerConfiguration config = getConfig();
        final TreeTargetNominalColumnData targetColumn = (TreeTargetNominalColumnData)data.getTargetColumn();
        final DataMemberships rootDataMemberships = //new RootDataMem(rowSampling, getIndexManager());
                        new RootDataMemberships(rowSampling, data, getIndexManager());
        ClassificationPriors targetPriors = targetColumn.getDistribution(rootDataMemberships, config);
        BitSet forbiddenColumnSet = new BitSet(data.getNrAttributes());
        //        final DataMemberships rootDataMemberships = new IntArrayDataMemberships(sampleWeights, data);
        final TreeNodeSignature rootSignature = TreeNodeSignature.ROOT_SIGNATURE;
        final ColumnSample rootColumnSample = getColSamplingStrategy().getColumnSampleForTreeNode(rootSignature);
        TreeNodeClassification rootNode = null;
        rootNode = buildTreeNode(exec, 0, rootDataMemberships, rootColumnSample, rootSignature, targetPriors,
            forbiddenColumnSet);
        assert forbiddenColumnSet.cardinality() == 0;
        rootNode.setTreeNodeCondition(TreeNodeTrueCondition.INSTANCE);
        return new TreeModelClassification(rootNode);
    }

    private TreeNodeClassification buildTreeNode(final ExecutionMonitor exec, final int currentDepth,
        final DataMemberships dataMemberships, final ColumnSample columnSample,
        final TreeNodeSignature treeNodeSignature, final ClassificationPriors targetPriors,
        final BitSet forbiddenColumnSet) throws CanceledExecutionException {
        final TreeData data = getData();
        final TreeEnsembleLearnerConfiguration config = getConfig();
        exec.checkCanceled();
        final boolean useSurrogates = getConfig().getMissingValueHandling() == MissingValueHandling.Surrogate;
        TreeNodeCondition[] childConditions;
        boolean markAttributeAsForbidden = false;
        final TreeTargetNominalColumnData targetColumn = (TreeTargetNominalColumnData)data.getTargetColumn();
        TreeNodeClassification[] childNodes;
        int attributeIndex = -1;
        if (useSurrogates) {
            SplitCandidate[] candidates = findBestSplitsClassification(currentDepth, dataMemberships, columnSample,
                treeNodeSignature, targetPriors, forbiddenColumnSet);
            if (candidates == null) {
                return new TreeNodeClassification(treeNodeSignature, targetPriors, config);
            }
            SurrogateSplit surrogateSplit =
                Surrogates.learnSurrogates(dataMemberships, candidates[0], data, columnSample, config, getRandomData());
            childConditions = surrogateSplit.getChildConditions();
            BitSet[] childMarkers = surrogateSplit.getChildMarkers();
            childNodes = new TreeNodeClassification[2];
            for (int i = 0; i < 2; i++) {
                DataMemberships childMemberships = dataMemberships.createChildMemberships(childMarkers[i]);
                ClassificationPriors childTargetPriors = targetColumn.getDistribution(childMemberships, config);
                TreeNodeSignature childSignature =
                    getSignatureFactory().getChildSignatureFor(treeNodeSignature, (byte)i);
                ColumnSample childColumnSample = getColSamplingStrategy().getColumnSampleForTreeNode(childSignature);
                childNodes[i] = buildTreeNode(exec, currentDepth + 1, childMemberships, childColumnSample,
                    childSignature, childTargetPriors, forbiddenColumnSet);
                childNodes[i].setTreeNodeCondition(childConditions[i]);
            }
        } else {
            // handle non surrogate case
            SplitCandidate bestSplit = findBestSplitClassification(currentDepth, dataMemberships, columnSample,
                treeNodeSignature, targetPriors, forbiddenColumnSet);
            if (bestSplit == null) {
                return new TreeNodeClassification(treeNodeSignature, targetPriors, config);
            }
            TreeAttributeColumnData splitColumn = bestSplit.getColumnData();
            attributeIndex = splitColumn.getMetaData().getAttributeIndex();
            markAttributeAsForbidden = !bestSplit.canColumnBeSplitFurther();
            forbiddenColumnSet.set(attributeIndex, markAttributeAsForbidden);

            childConditions = bestSplit.getChildConditions();
            childNodes = new TreeNodeClassification[childConditions.length];
            if (childConditions.length > Short.MAX_VALUE) {
                throw new RuntimeException(
                    "Too many children when splitting " + "attribute " + bestSplit.getColumnData()
                        + " (maximum supported: " + Short.MAX_VALUE + "): " + childConditions.length);
            }
            // Build child nodes
            for (int i = 0; i < childConditions.length; i++) {
                DataMemberships childMemberships = null;
                TreeNodeCondition cond = childConditions[i];
                childMemberships =
                    dataMemberships.createChildMemberships(splitColumn.updateChildMemberships(cond, dataMemberships));
                ClassificationPriors childTargetPriors = targetColumn.getDistribution(childMemberships, config);
                TreeNodeSignature childSignature = treeNodeSignature.createChildSignature((byte)i);
                ColumnSample childColumnSample = getColSamplingStrategy().getColumnSampleForTreeNode(childSignature);
                childNodes[i] = buildTreeNode(exec, currentDepth + 1, childMemberships, childColumnSample,
                    childSignature, childTargetPriors, forbiddenColumnSet);
                childNodes[i].setTreeNodeCondition(cond);
            }
        }
        if (markAttributeAsForbidden) {
            forbiddenColumnSet.set(attributeIndex, false);
        }
        return new TreeNodeClassification(treeNodeSignature, targetPriors, childNodes, getConfig());
    }

    /**
     * Returns a list of SplitCandidates sorted (descending) by their gain
     *
     * @param currentDepth
     * @param rowSampleWeights
     * @param treeNodeSignature
     * @param targetPriors
     * @param forbiddenColumnSet
     * @param membershipController
     * @return
     */
    private SplitCandidate[] findBestSplitsClassification(final int currentDepth, final DataMemberships dataMemberships,
        final ColumnSample columnSample, final TreeNodeSignature treeNodeSignature,
        final ClassificationPriors targetPriors, final BitSet forbiddenColumnSet) {
        final TreeData data = getData();
        final RandomData rd = getRandomData();
        //        final ColumnSampleStrategy colSamplingStrategy = getColSamplingStrategy();
        final TreeEnsembleLearnerConfiguration config = getConfig();
        final int maxLevels = config.getMaxLevels();
        if (maxLevels != TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE && currentDepth >= maxLevels) {
            return null;
        }
        final int minNodeSize = config.getMinNodeSize();
        if (minNodeSize != TreeEnsembleLearnerConfiguration.MIN_NODE_SIZE_UNDEFINED) {
            if (targetPriors.getNrRecords() < minNodeSize) {
                return null;
            }
        }
        final double priorImpurity = targetPriors.getPriorImpurity();
        if (priorImpurity < TreeColumnData.EPSILON) {
            return null;
        }
        final TreeTargetNominalColumnData targetColumn = (TreeTargetNominalColumnData)data.getTargetColumn();
        SplitCandidate splitCandidate = null;
        if (currentDepth == 0 && config.getHardCodedRootColumn() != null) {
            final TreeAttributeColumnData rootColumn = data.getColumn(config.getHardCodedRootColumn());
            // TODO discuss whether this option makes sense with surrogates
            return new SplitCandidate[]{
                rootColumn.calcBestSplitClassification(dataMemberships, targetPriors, targetColumn, rd)};
        }
        double bestGainValue = 0.0;
        final Comparator<SplitCandidate> comp = new Comparator<SplitCandidate>() {

            @Override
            public int compare(final SplitCandidate o1, final SplitCandidate o2) {
                int compareDouble = -Double.compare(o1.getGainValue(), o2.getGainValue());
                return compareDouble;
            }

        };
        ArrayList<SplitCandidate> candidates = new ArrayList<SplitCandidate>(columnSample.getNumCols());
        for (TreeAttributeColumnData col : columnSample) {
            if (forbiddenColumnSet.get(col.getMetaData().getAttributeIndex())) {
                continue;
            }
            SplitCandidate currentColSplit =
                col.calcBestSplitClassification(dataMemberships, targetPriors, targetColumn, rd);
            if (currentColSplit != null) {
                candidates.add(currentColSplit);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(comp);
        return candidates.toArray(new SplitCandidate[candidates.size()]);
    }

    private SplitCandidate findBestSplitClassification(final int currentDepth, final DataMemberships dataMemberships,
        final ColumnSample columnSample, final TreeNodeSignature treeNodeSignature,
        final ClassificationPriors targetPriors, final BitSet forbiddenColumnSet) {
        final TreeData data = getData();
        final RandomData rd = getRandomData();
        //        final ColumnSampleStrategy colSamplingStrategy = getColSamplingStrategy();
        final TreeEnsembleLearnerConfiguration config = getConfig();
        final int maxLevels = config.getMaxLevels();
        if (maxLevels != TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE && currentDepth >= maxLevels) {
            return null;
        }
        final int minNodeSize = config.getMinNodeSize();
        if (minNodeSize != TreeEnsembleLearnerConfiguration.MIN_NODE_SIZE_UNDEFINED) {
            if (targetPriors.getNrRecords() < minNodeSize) {
                return null;
            }
        }
        final double priorImpurity = targetPriors.getPriorImpurity();
        if (priorImpurity < TreeColumnData.EPSILON) {
            return null;
        }
        final TreeTargetNominalColumnData targetColumn = (TreeTargetNominalColumnData)data.getTargetColumn();
        SplitCandidate splitCandidate = null;
        if (currentDepth == 0 && config.getHardCodedRootColumn() != null) {
            final TreeAttributeColumnData rootColumn = data.getColumn(config.getHardCodedRootColumn());
            // TODO discuss whether this option makes sense with surrogates
            return rootColumn.calcBestSplitClassification(dataMemberships, targetPriors, targetColumn, rd);
        }
        double bestGainValue = 0.0;
        for (TreeAttributeColumnData col : columnSample) {
            if (forbiddenColumnSet.get(col.getMetaData().getAttributeIndex())) {
                continue;
            }
            final SplitCandidate currentColSplit =
                col.calcBestSplitClassification(dataMemberships, targetPriors, targetColumn, rd);
            if (currentColSplit != null) {
                final double currentGain = currentColSplit.getGainValue();
                final boolean tiebreaker = currentGain == bestGainValue ? (rd.nextInt(0, 1) == 0) : false;
                assert currentGain <= 1.0 : "The gain can never be larger than 1.0 (but was " + currentGain + ")";
                if (currentColSplit.getGainValue() > bestGainValue || tiebreaker) {
                    splitCandidate = currentColSplit;
                    bestGainValue = currentGain;
                }
            }
        }
        return splitCandidate;
    }

    private class BuildTreeNodeInParallel extends RecursiveTask<TreeNodeClassification> {
        private final ExecutionMonitor m_exec;

        private final int m_currentDepth;

        private final DataMemberships m_dataMemberships;

        private final ColumnSample m_columnSample;

        private final TreeNodeSignature m_treeNodeSignature;

        private final ClassificationPriors m_targetPriors;

        private final BitSet m_forbiddenColumnSet;

        public BuildTreeNodeInParallel(final ExecutionMonitor exec, final int currentDepth,
            final DataMemberships dataMemberships, final ColumnSample columnSample,
            final TreeNodeSignature treeNodeSignature, final ClassificationPriors targetPriors,
            final BitSet forbiddenColumnSet) {
            m_exec = exec;
            m_currentDepth = currentDepth;
            m_dataMemberships = dataMemberships;
            m_columnSample = columnSample;
            m_treeNodeSignature = treeNodeSignature;
            m_targetPriors = targetPriors;
            m_forbiddenColumnSet = forbiddenColumnSet;
        }

        /**
         * {@inheritDoc}
         *
         * @return
         */
        @Override
        protected TreeNodeClassification compute() {
            //            m_exec.checkCanceled();
            SplitCandidate bestSplit = findBestSplitsClassification(m_currentDepth, m_dataMemberships, m_columnSample,
                m_treeNodeSignature, m_targetPriors, m_forbiddenColumnSet)[0];
            if (bestSplit == null) {
                return new TreeNodeClassification(m_treeNodeSignature, m_targetPriors, getConfig());
            }
            TreeAttributeColumnData splitColumn = bestSplit.getColumnData();
            final int attributeIndex = splitColumn.getMetaData().getAttributeIndex();
            boolean markAttributeAsForbidden = !bestSplit.canColumnBeSplitFurther();
            m_forbiddenColumnSet.set(attributeIndex, markAttributeAsForbidden);

            TreeNodeCondition[] childConditions = bestSplit.getChildConditions();
            if (childConditions.length > Short.MAX_VALUE) {
                throw new RuntimeException(
                    "Too many children when splitting " + "attribute " + bestSplit.getColumnData()
                        + " (maximum supported: " + Short.MAX_VALUE + "): " + childConditions.length);
            }
            final TreeData data = getData();
            final TreeEnsembleLearnerConfiguration config = getConfig();
            final TreeTargetNominalColumnData targetColumn = (TreeTargetNominalColumnData)data.getTargetColumn();
            BuildTreeNodeInParallel[] buildTasks = new BuildTreeNodeInParallel[childConditions.length];
            for (int i = 0; i < childConditions.length; i++) {
                DataMemberships childMemberships = null;
                TreeNodeCondition cond = childConditions[i];
                //                if (isBinary) {
                //                    childMemberships = childDataMemberships[i];
                //                } else {
                childMemberships = m_dataMemberships
                    .createChildMemberships(splitColumn.updateChildMemberships(cond, m_dataMemberships));
                //                }
                ClassificationPriors childTargetPriors = targetColumn.getDistribution(childMemberships, config);
                TreeNodeSignature childSignature = m_treeNodeSignature.createChildSignature((byte)i);
                ColumnSample childColumnSample = getColSamplingStrategy().getColumnSampleForTreeNode(childSignature);
                buildTasks[i] = new BuildTreeNodeInParallel(m_exec, m_currentDepth + 1, childMemberships,
                    childColumnSample, childSignature, childTargetPriors, m_forbiddenColumnSet);
            }
            invokeAll(buildTasks);

            TreeNodeClassification[] childNodes = new TreeNodeClassification[childConditions.length];
            for (int i = 0; i < buildTasks.length; i++) {
                try {
                    childNodes[i] = buildTasks[i].get();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                }
                childNodes[i].setTreeNodeCondition(childConditions[i]);
            }
            if (markAttributeAsForbidden) {
                m_forbiddenColumnSet.set(attributeIndex, false);
            }
            return new TreeNodeClassification(m_treeNodeSignature, m_targetPriors, childNodes, getConfig());

        }

    }

}
