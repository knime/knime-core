/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 2, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.learner;

import java.util.BitSet;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble.data.RegressionPriors;
import org.knime.base.node.mine.treeensemble.data.TreeAttributeColumnData;
import org.knime.base.node.mine.treeensemble.data.TreeColumnData;
import org.knime.base.node.mine.treeensemble.data.TreeData;
import org.knime.base.node.mine.treeensemble.data.TreeTargetNumericColumnData;
import org.knime.base.node.mine.treeensemble.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble.model.TreeNodeRegression;
import org.knime.base.node.mine.treeensemble.model.TreeNodeSignature;
import org.knime.base.node.mine.treeensemble.model.TreeNodeTrueCondition;
import org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble.sample.column.ColumnSample;
import org.knime.base.node.mine.treeensemble.sample.column.ColumnSampleStrategy;
import org.knime.base.node.mine.treeensemble.sample.row.RowSample;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * 
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeLearnerRegression extends AbstractTreeLearner {

    /**
     * @param config
     * @param data
     */
    public TreeLearnerRegression(final TreeEnsembleLearnerConfiguration config, final TreeData data,
        final RandomData randomData) {
        super(config, data, randomData);
        if (!config.isRegression()) {
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
        double[] dataMemberships = new double[data.getNrRows()];
        for (int i = 0; i < dataMemberships.length; i++) {
            dataMemberships[i] = rowSampling.getCountFor(i);
        }
        RegressionPriors targetPriors = targetColumn.getPriors(dataMemberships, config);
        BitSet forbiddenColumnSet = new BitSet(data.getNrAttributes());
        TreeNodeRegression rootNode =
            buildTreeNode(exec, 0, dataMemberships, TreeNodeSignature.ROOT_SIGNATURE, targetPriors, forbiddenColumnSet);
        assert forbiddenColumnSet.cardinality() == 0;
        rootNode.setTreeNodeCondition(TreeNodeTrueCondition.INSTANCE);
        return new TreeModelRegression(rootNode);
    }

    private SplitCandidate
        findBestSplitRegression(final int currentDepth, final double[] rowSampleWeights,
            final TreeNodeSignature treeNodeSignature, final RegressionPriors targetPriors,
            final BitSet forbiddenColumnSet) {
        final TreeData data = getData();
        final ColumnSampleStrategy colSamplingStrategy = getColSamplingStrategy();
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
            return rootColumn.calcBestSplitRegression(rowSampleWeights, targetPriors, targetColumn);
        } else {
            double bestGainValue = 0.0;
            final ColumnSample columnSample = colSamplingStrategy.getColumnSampleForTreeNode(treeNodeSignature);
            for (TreeAttributeColumnData col : columnSample) {
                if (forbiddenColumnSet.get(col.getMetaData().getAttributeIndex())) {
                    continue;
                }
                SplitCandidate currentColSplit =
                    col.calcBestSplitRegression(rowSampleWeights, targetPriors, targetColumn);
                if (currentColSplit != null) {
                    double gainValue = currentColSplit.getGainValue();
                    if (gainValue > bestGainValue) {
                        bestGainValue = gainValue;
                        splitCandidate = currentColSplit;
                    }
                }
            }
        }
        return splitCandidate;
    }

    private TreeNodeRegression buildTreeNode(final ExecutionMonitor exec, final int currentDepth,
        final double[] rowSampleWeights, final TreeNodeSignature treeNodeSignature,
        final RegressionPriors targetPriors, final BitSet forbiddenColumnSet) throws CanceledExecutionException {
        final TreeData data = getData();
        final TreeEnsembleLearnerConfiguration config = getConfig();
        exec.checkCanceled();
        SplitCandidate bestSplit =
            findBestSplitRegression(currentDepth, rowSampleWeights, treeNodeSignature, targetPriors, forbiddenColumnSet);
        if (bestSplit == null) {
            return new TreeNodeRegression(treeNodeSignature, targetPriors);
        }
        TreeAttributeColumnData splitColumn = bestSplit.getColumnData();
        final int attributeIndex = splitColumn.getMetaData().getAttributeIndex();
        boolean markAttributeAsForbidden = !bestSplit.canColumnBeSplitFurther();
        forbiddenColumnSet.set(attributeIndex, markAttributeAsForbidden);

        TreeNodeCondition[] childConditions = bestSplit.getChildConditions();
        if (childConditions.length > Short.MAX_VALUE) {
            throw new RuntimeException("Too many children when splitting " + "attribute " + bestSplit.getColumnData()
                + " (maximum supported: " + Short.MAX_VALUE + "): " + childConditions.length);
        }
        TreeNodeRegression[] childNodes = new TreeNodeRegression[childConditions.length];
        final double[] dataMemberships = rowSampleWeights;
        final double[] childMemberships = new double[dataMemberships.length];
        final TreeTargetNumericColumnData targetColumn = (TreeTargetNumericColumnData)data.getTargetColumn();
        for (int i = 0; i < childConditions.length; i++) {
            System.arraycopy(dataMemberships, 0, childMemberships, 0, dataMemberships.length);
            TreeNodeCondition cond = childConditions[i];
            splitColumn.updateChildMemberships(cond, dataMemberships, childMemberships);
            RegressionPriors childTargetPriors = targetColumn.getPriors(childMemberships, config);
            TreeNodeSignature childSignature = treeNodeSignature.createChildSignature((short)i);
            childNodes[i] =
                buildTreeNode(exec, currentDepth + 1, childMemberships, childSignature, childTargetPriors,
                    forbiddenColumnSet);
            childNodes[i].setTreeNodeCondition(cond);
        }
        if (markAttributeAsForbidden) {
            forbiddenColumnSet.set(attributeIndex, false);
        }
        return new TreeNodeRegression(treeNodeSignature, targetPriors, childNodes);
    }

}
