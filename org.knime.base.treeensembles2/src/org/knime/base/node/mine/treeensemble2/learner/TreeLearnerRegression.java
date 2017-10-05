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
 *   Jan 2, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.RegressionPriors;
import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnData;
import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.data.memberships.IDataIndexManager;
import org.knime.base.node.mine.treeensemble2.data.memberships.RootDataMemberships;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeTrueCondition;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;
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
public class TreeLearnerRegression extends AbstractTreeLearner {

    private List<TreeNodeRegression> m_leafs;

    /**
     * Constructor for TreeLearnerRegression
     *
     * @param config holds the configuration information
     * @param data holds the data
     * @param indexManager needed for datamemberships
     * @param signatureFactory factory for TreeNodeSignatures
     * @param randomData needed for randomization (for example random tie breaking, column sampling, ...)
     */
    public TreeLearnerRegression(final TreeEnsembleLearnerConfiguration config, final TreeData data,
      	final IDataIndexManager indexManager, final TreeNodeSignatureFactory signatureFactory,
        final RandomData randomData, final RowSample rowSample) {
        super(config, data, indexManager, signatureFactory, randomData, rowSample);
        if (!(data.getTargetColumn() instanceof TreeTargetNumericColumnData)) {
            throw new IllegalStateException("Can't learn regression model on categorical target");
        }
    }

    private TreeTargetNumericColumnData getTargetData() {
        return (TreeTargetNumericColumnData)getData().getTargetColumn();
    }

    /** {@inheritDoc} */
    @Override
    public TreeModelRegression learnSingleTree(final ExecutionMonitor exec, final RandomData rd)
        throws CanceledExecutionException {
        final TreeTargetNumericColumnData targetColumn = getTargetData();
        final TreeData data = getData();
        final RowSample rowSampling = getRowSampling();
        final TreeEnsembleLearnerConfiguration config = getConfig();
        final IDataIndexManager indexManager = getIndexManager();
        DataMemberships rootDataMemberships = new RootDataMemberships(rowSampling, data, indexManager);
        RegressionPriors targetPriors = targetColumn.getPriors(rootDataMemberships, config);
        BitSet forbiddenColumnSet = new BitSet(data.getNrAttributes());
        boolean isGradientBoosting = config instanceof GradientBoostingLearnerConfiguration;
        if (isGradientBoosting) {
            m_leafs = new ArrayList<TreeNodeRegression>();
        }
        final TreeNodeSignature rootSignature = TreeNodeSignature.ROOT_SIGNATURE;
        final ColumnSample rootColumnSample = getColSamplingStrategy().getColumnSampleForTreeNode(rootSignature);
        TreeNodeRegression rootNode = buildTreeNode(exec, 0, rootDataMemberships, rootColumnSample, getSignatureFactory().getRootSignature(),
            targetPriors, forbiddenColumnSet);
        assert forbiddenColumnSet.cardinality() == 0;
        rootNode.setTreeNodeCondition(TreeNodeTrueCondition.INSTANCE);
        if (isGradientBoosting) {
            return new TreeModelRegression(rootNode, m_leafs);
        }
        return new TreeModelRegression(rootNode);
    }

    private SplitCandidate findBestSplitRegression(final int currentDepth, final DataMemberships dataMemberships, final ColumnSample columnSample,
        final RegressionPriors targetPriors,
        final BitSet forbiddenColumnSet) {
        final TreeData data = getData();
        final RandomData rd = getRandomData();
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
        final double priorSquaredDeviation = targetPriors.getSumSquaredDeviation();
        if (priorSquaredDeviation < TreeColumnData.EPSILON) {
            return null;
        }
        final TreeTargetNumericColumnData targetColumn = getTargetData();
        SplitCandidate splitCandidate = null;
        if (currentDepth == 0 && config.getHardCodedRootColumn() != null) {
            final TreeAttributeColumnData rootColumn = data.getColumn(config.getHardCodedRootColumn());
            return rootColumn.calcBestSplitRegression(dataMemberships, targetPriors, targetColumn, rd);
        } else {
            double bestGainValue = 0.0;
            for (TreeAttributeColumnData col : columnSample) {
                if (forbiddenColumnSet.get(col.getMetaData().getAttributeIndex())) {
                    continue;
                }
                SplitCandidate currentColSplit =
                    col.calcBestSplitRegression(dataMemberships, targetPriors, targetColumn, rd);
                if (currentColSplit != null) {
                    double gainValue = currentColSplit.getGainValue();
                    if (gainValue > bestGainValue) {
                        bestGainValue = gainValue;
                        splitCandidate = currentColSplit;
                    }
                }
            }
            return splitCandidate;
        }
    }

    private SplitCandidate[] findBestSplitsRegression(final int currentDepth, final DataMemberships dataMemberships, final ColumnSample columnSample,
        final RegressionPriors targetPriors,
        final BitSet forbiddenColumnSet) {
        final TreeData data = getData();
        final RandomData rd = getRandomData();
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
        final double priorSquaredDeviation = targetPriors.getSumSquaredDeviation();
        if (priorSquaredDeviation < TreeColumnData.EPSILON) {
            return null;
        }
        final TreeTargetNumericColumnData targetColumn = getTargetData();
        ArrayList<SplitCandidate> splitCandidates = null;
        if (currentDepth == 0 && config.getHardCodedRootColumn() != null) {
            final TreeAttributeColumnData rootColumn = data.getColumn(config.getHardCodedRootColumn());
            return new SplitCandidate[]{
                rootColumn.calcBestSplitRegression(dataMemberships, targetPriors, targetColumn, rd)};
        } else {
            splitCandidates = new ArrayList<SplitCandidate>(columnSample.getNumCols());
            for (TreeAttributeColumnData col : columnSample) {
                if (forbiddenColumnSet.get(col.getMetaData().getAttributeIndex())) {
                    continue;
                }
                SplitCandidate currentColSplit =
                    col.calcBestSplitRegression(dataMemberships, targetPriors, targetColumn, rd);
                if (currentColSplit != null) {
                    splitCandidates.add(currentColSplit);
                }
            }
        }
        Comparator<SplitCandidate> comp = new Comparator<SplitCandidate>() {
            @Override
            public int compare(final SplitCandidate arg0, final SplitCandidate arg1) {
                int compareDouble = -Double.compare(arg0.getGainValue(), arg1.getGainValue());
                return compareDouble;
            }
        };
        if (splitCandidates.isEmpty()) {
            return null;
        }
        splitCandidates.sort(comp);
        return splitCandidates.toArray(new SplitCandidate[splitCandidates.size()]);
    }

    private TreeNodeRegression buildTreeNode(final ExecutionMonitor exec, final int currentDepth,
        final DataMemberships dataMemberships, final ColumnSample columnSample, final TreeNodeSignature treeNodeSignature,
        final RegressionPriors targetPriors, final BitSet forbiddenColumnSet) throws CanceledExecutionException {
        final TreeData data = getData();
        final RandomData rd = getRandomData();
        final TreeEnsembleLearnerConfiguration config = getConfig();
        exec.checkCanceled();
        final SplitCandidate candidate =
            findBestSplitRegression(currentDepth, dataMemberships, columnSample, targetPriors, forbiddenColumnSet);
        if (candidate == null) {
            if (config instanceof GradientBoostingLearnerConfiguration) {
                TreeNodeRegression leaf =
                    new TreeNodeRegression(treeNodeSignature, targetPriors, dataMemberships.getOriginalIndices());
                addToLeafList(leaf);
                return leaf;
            }
            return new TreeNodeRegression(treeNodeSignature, targetPriors);
        }
        final TreeTargetNumericColumnData targetColumn = (TreeTargetNumericColumnData)data.getTargetColumn();
        boolean useSurrogates = config.getMissingValueHandling() == MissingValueHandling.Surrogate;
        TreeNodeCondition[] childConditions;
        TreeNodeRegression[] childNodes;
        if (useSurrogates) {
            SurrogateSplit surrogateSplit = Surrogates.learnSurrogates(dataMemberships, candidate, data, columnSample, config, rd);
            childConditions = surrogateSplit.getChildConditions();
            BitSet[] childMarkers = surrogateSplit.getChildMarkers();
            assert childMarkers[0].cardinality() + childMarkers[1].cardinality() == dataMemberships.getRowCount(): "Sum of rows in children does not add up to number of rows in parent.";
            childNodes = new TreeNodeRegression[2];
            for (int i = 0; i < 2; i++) {
                DataMemberships childMemberships = dataMemberships.createChildMemberships(childMarkers[i]);
                TreeNodeSignature childSignature = getSignatureFactory().getChildSignatureFor(treeNodeSignature, (byte)i);
                ColumnSample childColumnSample = getColSamplingStrategy().getColumnSampleForTreeNode(childSignature);
                RegressionPriors childTargetPriors = targetColumn.getPriors(childMemberships, config);
                childNodes[i] = buildTreeNode(exec, currentDepth + 1, childMemberships, childColumnSample, childSignature,
                    childTargetPriors, forbiddenColumnSet);
                childNodes[i].setTreeNodeCondition(childConditions[i]);
            }
        } else {
            SplitCandidate bestSplit = candidate;
            TreeAttributeColumnData splitColumn = bestSplit.getColumnData();
            final int attributeIndex = splitColumn.getMetaData().getAttributeIndex();
            boolean markAttributeAsForbidden = !bestSplit.canColumnBeSplitFurther();
            forbiddenColumnSet.set(attributeIndex, markAttributeAsForbidden);
            childConditions = bestSplit.getChildConditions();
            if (childConditions.length > Short.MAX_VALUE) {
                throw new RuntimeException(
                    "Too many children when splitting " + "attribute " + bestSplit.getColumnData()
                        + " (maximum supported: " + Short.MAX_VALUE + "): " + childConditions.length);
            }
            childNodes = new TreeNodeRegression[childConditions.length];
            for (int i = 0; i < childConditions.length; i++) {
                TreeNodeCondition cond = childConditions[i];
                DataMemberships childMemberships =
                    dataMemberships.createChildMemberships(splitColumn.updateChildMemberships(cond, dataMemberships));
                RegressionPriors childTargetPriors = targetColumn.getPriors(childMemberships, config);
                TreeNodeSignature childSignature = treeNodeSignature.createChildSignature((byte)i);
                ColumnSample childColumnSample = getColSamplingStrategy().getColumnSampleForTreeNode(childSignature);
                childNodes[i] = buildTreeNode(exec, currentDepth + 1, childMemberships, childColumnSample, childSignature,
                    childTargetPriors, forbiddenColumnSet);
                childNodes[i].setTreeNodeCondition(cond);
            }
            if (markAttributeAsForbidden) {
                forbiddenColumnSet.set(attributeIndex, false);
            }
        }
        return new TreeNodeRegression(treeNodeSignature, targetPriors, childNodes);
    }

    private synchronized void addToLeafList(final TreeNodeRegression leaf) {
        m_leafs.add(leaf);
    }
}
