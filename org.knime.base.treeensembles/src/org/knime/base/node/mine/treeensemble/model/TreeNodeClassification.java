/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeLeaf;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitPMML;
import org.knime.base.node.mine.treeensemble.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble.data.PriorDistribution;
import org.knime.base.node.mine.treeensemble.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble.data.TreeTargetNominalColumnMetaData;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.MutableInteger;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeNodeClassification {

    private static final TreeNodeClassification[] EMPTY_CHILD_ARRAY = new TreeNodeClassification[0];

    private final TreeNodeSignature m_signature;
    private final TreeTargetNominalColumnMetaData m_targetMetaData;
    private final int m_majorityIndex;
    private final double[] m_targetDistribution;
    private final TreeNodeClassification[] m_childNodes;
    private TreeNodeCondition m_condition;


    public TreeNodeClassification(final TreeNodeSignature signature,
            final PriorDistribution targetPriors) {
        this(signature, targetPriors, EMPTY_CHILD_ARRAY);
    }

    public TreeNodeClassification(final TreeNodeSignature signature,
            final PriorDistribution targetPriors,
            final TreeNodeClassification[] childNodes) {
        m_signature = signature;
        if (targetPriors == null || childNodes == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_targetDistribution = targetPriors.getDistribution();
        m_majorityIndex = targetPriors.getMajorityIndex();
        m_childNodes = childNodes;
        m_targetMetaData = targetPriors.getTargetMetaData();
    }

    private TreeNodeClassification(final TreeNodeSignature signature,
            final TreeTargetNominalColumnMetaData targetMetaData,
            final int majorityIndex,
            final double[] targetDistribution,
            final TreeNodeClassification[] childNodes,
            final TreeNodeCondition condition) {
        m_signature = signature;
        m_targetMetaData = targetMetaData;
        m_majorityIndex = majorityIndex;
        m_targetDistribution = targetDistribution;
        m_childNodes = childNodes;
        m_condition = condition;
    }

    /** @param condition the condition to set */
    public void setTreeNodeCondition(final TreeNodeCondition condition) {
        m_condition = condition;
    }

    /** @return the condition */
    public final TreeNodeCondition getCondition() {
        return m_condition;
    }

    public int getNrChildren() {
        return m_childNodes.length;
    }

    public TreeNodeClassification findMatchingChild(final PredictorRecord record) {
        for (TreeNodeClassification child : m_childNodes) {
            TreeNodeCondition childCondition = child.getCondition();
            if (childCondition.testCondition(record)) {
                return child;
            }
        }
        return null;
    }

    /** @return the signature */
    public TreeNodeSignature getSignature() {
        return m_signature;
    }

    public TreeNodeClassification getChild(final int index) {
        return m_childNodes[index];
    }

    public List<TreeNodeClassification> getChildren() {
        return Collections.unmodifiableList(Arrays.asList(m_childNodes));
    }

    /** @return the majorityClassName */
    public String getMajorityClassName() {
        return m_targetMetaData.getValues()[m_majorityIndex].getNominalValue();
    }

    /** @return the targetMetaData */
    public TreeTargetNominalColumnMetaData getTargetMetaData() {
        return m_targetMetaData;
    }

    /** @return the targetDistribution */
    public double[] getTargetDistribution() {
        return m_targetDistribution;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return toStringRecursion("");
    }

    public String toStringRecursion(final String indent) {
        StringBuilder b = new StringBuilder();
        if (m_condition != null) {
            b.append(indent).append(m_condition).append(" --> ");
        } else {
            b.append(indent);
        }
        // e.g. "Iris-Setosa (50/150)"
        double majorityWeight = m_targetDistribution[m_majorityIndex];
        double weightSum = 0.0;
        for (double v : m_targetDistribution) {
            weightSum += v;
        }
        b.append("\"").append(getMajorityClassName()).append("\" (");
        NumberFormat format = NumberFormat.getInstance();
        b.append(format.format(majorityWeight)).append("/");
        b.append(format.format(weightSum)).append(")");
        String childIndent = indent.concat("   ");
        for (int i = 0; i < m_childNodes.length; i++) {
            b.append("\n");
            b.append(m_childNodes[i].toStringRecursion(childIndent));
        }
        return b.toString();
    }

    public void save(final DataOutputStream out) throws IOException {
        if (m_condition == null) {
            throw new IllegalStateException("Can't save tree, tree node \""
                    + m_signature + "\" has no condition assigned");
        }
        m_signature.save(out);
        m_condition.save(out);

        // length is equally to target value list length (no need to store)
        for (int i = 0; i < m_targetDistribution.length; i++) {
            out.writeDouble(m_targetDistribution[i]);
        }
        out.writeInt(m_childNodes.length);
        for (TreeNodeClassification child : m_childNodes) {
            child.save(out);
        }
    }

    public static TreeNodeClassification load(final DataInputStream in,
            final TreeMetaData metaData) throws IOException {
        TreeNodeSignature signature = TreeNodeSignature.load(in);
        TreeNodeCondition condition = TreeNodeCondition.load(in, metaData);

        TreeTargetNominalColumnMetaData targetMetaData =
            (TreeTargetNominalColumnMetaData)metaData.getTargetMetaData();
        int targetLength = targetMetaData.getValues().length;
        double[] targetDistribution = new double[targetLength];
        int majorityIndex = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < targetLength; i++) {
            final double d = in.readDouble();
            if (d > max) { // strictly larger, see also PriorDistribution
                majorityIndex = i;
                max = d;
            }
            targetDistribution[i] = d;
        }

        int childCount = in.readInt();
        TreeNodeClassification[] children = new TreeNodeClassification[childCount];
        for (int i = 0; i < childCount; i++) {
            children[i] = TreeNodeClassification.load(in, metaData);
        }
        return new TreeNodeClassification(signature, targetMetaData, majorityIndex,
                targetDistribution, children, condition);
    }

    public int getSplitAttributeIndex() {
        final int nrChildren = getNrChildren();
        int splitAttributeIndex = -1;
        for (int i = 0; i < nrChildren; i++) {
            final TreeNodeClassification treeNode = m_childNodes[i];
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

    /**
     * @param metaData
     * @return */
    public DecisionTreeNode createDecisionTreeNode(
            final MutableInteger idGenerator, final TreeMetaData metaData) {
        DataCell majorityCell = new StringCell(getMajorityClassName());
        double[] targetDistribution = getTargetDistribution();
        int initSize = (int)(targetDistribution.length / 0.75 + 1.0);
        LinkedHashMap<DataCell, Double> scoreDistributionMap =
            new LinkedHashMap<DataCell, Double>(initSize);
        NominalValueRepresentation[] targets = getTargetMetaData().getValues();
        for (int i = 0; i < targetDistribution.length; i++) {
            String cl = targets[i].getNominalValue();
            double d = targetDistribution[i];
            scoreDistributionMap.put(new StringCell(cl), d);
        }
        final int nrChildren = m_childNodes.length;
        if (nrChildren == 0) {
            return new DecisionTreeNodeLeaf(idGenerator.inc(),
                    majorityCell, scoreDistributionMap);
        } else {
            int id = idGenerator.inc();
            DecisionTreeNode[] childNodes = new DecisionTreeNode[nrChildren];
            int splitAttributeIndex = getSplitAttributeIndex();
            assert splitAttributeIndex >= 0 : "non-leaf node has no split";
            String splitAttribute = metaData.getAttributeMetaData(
                    splitAttributeIndex).getAttributeName();
            PMMLPredicate[] childPredicates = new PMMLPredicate[nrChildren];
            for (int i = 0; i < nrChildren; i++) {
                final TreeNodeClassification treeNode = m_childNodes[i];
                TreeNodeCondition cond = treeNode.getCondition();
                childPredicates[i] = cond.toPMMLPredicate();
                childNodes[i] = treeNode.createDecisionTreeNode(
                        idGenerator, metaData);
            }
            return new DecisionTreeNodeSplitPMML(id,
                    majorityCell, scoreDistributionMap, splitAttribute,
                    childPredicates, childNodes);
        }
    }


}
