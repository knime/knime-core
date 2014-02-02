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
 *   Jan 5, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeLeaf;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitPMML;
import org.knime.base.node.mine.treeensemble.data.RegressionPriors;
import org.knime.base.node.mine.treeensemble.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble.data.TreeTargetNumericColumnMetaData;
import org.knime.base.node.util.DoubleFormat;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.MutableInteger;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeNodeRegression extends AbstractTreeNode {

    private static final TreeNodeRegression[] EMPTY_CHILD_ARRAY = new TreeNodeRegression[0];

    private final double m_mean;
    private final double m_sumSquaredDeviation;
    private final double m_totalSum;


    public TreeNodeRegression(final TreeNodeSignature signature,
            final RegressionPriors targetPriors) {
        this(signature, targetPriors, EMPTY_CHILD_ARRAY);
    }

    public TreeNodeRegression(final TreeNodeSignature signature,
            final RegressionPriors targetPriors,
            final TreeNodeRegression[] childNodes) {
        super(signature, targetPriors.getTargetMetaData(), childNodes);
        m_mean = targetPriors.getMean();
        m_totalSum = targetPriors.getNrRecords();
        m_sumSquaredDeviation = targetPriors.getSumSquaredDeviation();
    }

    /**
     * @param in
     * @param metaData
     * @throws IOException */
    public TreeNodeRegression(final TreeModelDataInputStream in, final TreeMetaData metaData) throws IOException {
        super(in, metaData);
        m_mean = in.readDouble();
        m_totalSum = in.readDouble();
        m_sumSquaredDeviation = in.readDouble();
    }

    /** {@inheritDoc} */
    @Override
    public TreeNodeRegression getChild(final int index) {
        return (TreeNodeRegression)super.getChild(index);
    }

    /** {@inheritDoc} */
    @Override
    public TreeTargetNumericColumnMetaData getTargetMetaData() {
        return (TreeTargetNumericColumnMetaData)super.getTargetMetaData();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return toStringRecursion("");
    }

    /** @return the mean */
    public double getMean() {
        return m_mean;
    }

    public String toStringRecursion(final String indent) {
        StringBuilder b = new StringBuilder();
        TreeNodeCondition condition = getCondition();
        if (condition != null) {
            b.append(indent).append(condition).append(" --> ");
        } else {
            b.append(indent);
        }
        b.append(DoubleFormat.formatDouble(m_mean));
        b.append(" (variance: ").append(DoubleFormat.formatDouble(m_sumSquaredDeviation / m_totalSum));
        b.append("; #records: ").append(m_totalSum).append(")");
        String childIndent = indent.concat("   ");
        for (int i = 0; i < getNrChildren(); i++) {
            b.append("\n");
            b.append(getChild(i).toStringRecursion(childIndent));
        }
        return b.toString();
    }

    private static final LinkedHashMap<DataCell, Double> EMPTY_MAP = new LinkedHashMap<DataCell, Double>();

    /**
     * @param metaData
     * @return */
    public DecisionTreeNode createDecisionTreeNode(
            final MutableInteger idGenerator, final TreeMetaData metaData) {
        DataCell majorityCell = new StringCell(DoubleFormat.formatDouble(m_mean));
        final int nrChildren = getNrChildren();
        LinkedHashMap<DataCell, Double> distributionMap = new LinkedHashMap<DataCell, Double>();
        distributionMap.put(majorityCell, m_totalSum);
        if (nrChildren == 0) {
            return new DecisionTreeNodeLeaf(idGenerator.inc(), majorityCell, distributionMap);
        } else {
            int id = idGenerator.inc();
            DecisionTreeNode[] childNodes = new DecisionTreeNode[nrChildren];
            int splitAttributeIndex = getSplitAttributeIndex();
            assert splitAttributeIndex >= 0 : "non-leaf node has no split";
            String splitAttribute = metaData.getAttributeMetaData(
                    splitAttributeIndex).getAttributeName();
            PMMLPredicate[] childPredicates = new PMMLPredicate[nrChildren];
            for (int i = 0; i < nrChildren; i++) {
                final TreeNodeRegression treeNode = getChild(i);
                TreeNodeCondition cond = treeNode.getCondition();
                childPredicates[i] = cond.toPMMLPredicate();
                childNodes[i] = treeNode.createDecisionTreeNode(
                        idGenerator, metaData);
            }
            return new DecisionTreeNodeSplitPMML(id,
                    majorityCell, distributionMap, splitAttribute,
                    childPredicates, childNodes);
        }
    }

    @Override
    public void saveInSubclass(final DataOutputStream out) throws IOException {
        // length is equally to target value list length (no need to store)
        out.writeDouble(m_mean);
        out.writeDouble(m_totalSum);
        out.writeDouble(m_sumSquaredDeviation);
    }

    public static TreeNodeRegression load(final TreeModelDataInputStream in,
            final TreeMetaData metaData) throws IOException {
        return new TreeNodeRegression(in, metaData);
    }

    /** {@inheritDoc} */
    @Override
    TreeNodeRegression loadChild(final TreeModelDataInputStream in, final TreeMetaData metaData) throws IOException {
        return TreeNodeRegression.load(in, metaData);
    }

    @Override
    public int getSplitAttributeIndex() {
        final int nrChildren = getNrChildren();
        int splitAttributeIndex = -1;
        for (int i = 0; i < nrChildren; i++) {
            final TreeNodeRegression treeNode = getChild(i);
            TreeNodeCondition cond = treeNode.getCondition();
            if (cond instanceof TreeNodeColumnCondition) {
                int s = ((TreeNodeColumnCondition)cond)
                    .getColumnMetaData().getAttributeIndex();
                if (splitAttributeIndex == -1) {
                    splitAttributeIndex = s;
                } else if (splitAttributeIndex != s) {
                    assert false : "Confusing split column in node's childrin: "
                        + "\"" + splitAttributeIndex + "\" vs. \"" + s + "\"";
                }
            }
        }
        return splitAttributeIndex;
    }

}
