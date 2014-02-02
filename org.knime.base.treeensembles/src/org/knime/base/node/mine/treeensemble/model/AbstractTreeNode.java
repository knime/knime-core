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
 *   Oct 20, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.mine.treeensemble.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble.data.TreeTargetColumnMetaData;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractTreeNode {

    private final TreeNodeSignature m_signature;
    private final TreeTargetColumnMetaData m_targetMetaData;
    private final AbstractTreeNode[] m_childNodes;
    private TreeNodeCondition m_condition;

    /**
     *  */
    public AbstractTreeNode(final TreeNodeSignature signature, final TreeTargetColumnMetaData targetMetaData,
            final AbstractTreeNode[] childNodes) {
        m_signature = signature;
        if (targetMetaData == null || childNodes == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_childNodes = childNodes;
        m_targetMetaData = targetMetaData;
    }

    /**
     *  */
    AbstractTreeNode(final TreeModelDataInputStream in, final TreeMetaData metaData) throws IOException {
        m_signature = TreeNodeSignature.load(in);
        m_condition = TreeNodeCondition.load(in, metaData);
        m_targetMetaData = metaData.getTargetMetaData();

        int childCount = in.readInt();

        AbstractTreeNode[] children = (AbstractTreeNode[])Array.newInstance(getClass(), childCount);
        for (int i = 0; i < childCount; i++) {
            children[i] = loadChild(in, metaData);
        }
        m_childNodes = children;
    }

    /** @param condition the condition to set */
    public void setTreeNodeCondition(final TreeNodeCondition condition) {
        m_condition = condition;
    }

    /** @return the condition */
    public final TreeNodeCondition getCondition() {
        return m_condition;
    }

    public int getSplitAttributeIndex() {
        final int nrChildren = getNrChildren();
        int splitAttributeIndex = -1;
        for (int i = 0; i < nrChildren; i++) {
            final AbstractTreeNode treeNode = getChild(i);
            TreeNodeCondition cond = treeNode.getCondition();
            if (cond instanceof TreeNodeColumnCondition) {
                int s = ((TreeNodeColumnCondition)cond)
                    .getColumnMetaData().getAttributeIndex();
                if (splitAttributeIndex == -1) {
                    splitAttributeIndex = s;
                } else if (splitAttributeIndex != s) {
                    assert false : "Confusing split column in node's children: "
                        + "\"" + splitAttributeIndex + "\" vs. \"" + s + "\"";
                }
            }
        }
        return splitAttributeIndex;
    }


    public int getNrChildren() {
        return m_childNodes.length;
    }

    /** @return the signature */
    public TreeNodeSignature getSignature() {
        return m_signature;
    }

    public AbstractTreeNode getChild(final int index) {
        return m_childNodes[index];
    }

    public <T extends AbstractTreeNode> List<T> getChildren() {
        return (List<T>)Collections.unmodifiableList(Arrays.asList(m_childNodes));
    }

    public final <T extends AbstractTreeNode> T findMatchingChild(final PredictorRecord record) {
        List<T> children = getChildren();
        for (T child : children) {
            TreeNodeCondition childCondition = child.getCondition();
            if (childCondition.testCondition(record)) {
                return child;
            }
        }
        return null;
    }

    /** @return the targetMetaData */
    public TreeTargetColumnMetaData getTargetMetaData() {
        return m_targetMetaData;
    }

    public final void save(final DataOutputStream out) throws IOException {
        if (m_condition == null) {
            throw new IllegalStateException("Can't save tree, tree node \""
                    + m_signature + "\" has no condition assigned");
        }
        m_signature.save(out);
        m_condition.save(out);

        out.writeInt(m_childNodes.length);
        for (AbstractTreeNode child : m_childNodes) {
            child.save(out);
        }

        saveInSubclass(out);
    }

    abstract void saveInSubclass(final DataOutputStream out) throws IOException;

    abstract AbstractTreeNode loadChild(final TreeModelDataInputStream in, final TreeMetaData metaData) throws IOException;

}
