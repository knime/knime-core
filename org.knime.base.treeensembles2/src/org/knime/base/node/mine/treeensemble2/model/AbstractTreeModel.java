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
 *   Oct 18, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.node.proximity.ArrayListTreePath;
import org.knime.base.node.mine.treeensemble2.node.proximity.TreePath;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @param <N> Implementation of AbstractTreeNode (Regression or Classification)
 */
public abstract class AbstractTreeModel<N extends AbstractTreeNode> {

    private final N m_rootNode;

    /**
     * @param rootNode
     */
    AbstractTreeModel(final N rootNode) {
        m_rootNode = rootNode;
    }

    /** @return the rootNode */
    public final N getRootNode() {
        return m_rootNode;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_rootNode.toString();
    }

    /**
     * Finds the matching node for <b>record</b>
     *
     * @param record
     * @return TreeNode that matches the values of <b>record</b>
     */
    public N findMatchingNode(final PredictorRecord record) {
        N matchingNode = m_rootNode;
        N nextChild;
        while ((nextChild = matchingNode.findMatchingChild(record)) != null) {
            matchingNode = nextChild;
        }
        assert matchingNode.getNrChildren() == 0 : "The found node is not a leaf node!";
        return matchingNode;
    }

    /**
     * Finds the path that <b>record</b> took through the tree
     *
     * @param record
     * @return path that the record took in the tree
     */
    public TreePath getTreePath(final PredictorRecord record) {
        ArrayList<Integer> path = new ArrayList<Integer>();
        N currentNode = m_rootNode;
        int nextTurn;
        while ((nextTurn = currentNode.findNextPathTurn(record)) >= 0) {
            path.add(nextTurn);
            currentNode = currentNode.findMatchingChild(record);
        }
        return new ArrayListTreePath(path);
    }

    /**
     * @param level
     * @return the TreeNodes on <b>level</b>
     */
    public Iterable<N> getTreeNodes(final int level) {
        if (level == 0) {
            return Collections.singleton(m_rootNode);
        }
        List<N> result = new ArrayList<N>();
        for (N parent : getTreeNodes(level - 1)) {
            final List<N> children = parent.getChildren();
            result.addAll(children);
        }
        return result;
    }

    /**
     * Saves the model to DataOutputStream <b>out</b>
     *
     * @param out
     * @throws IOException
     */
    public void save(final DataOutputStream out) throws IOException {
        m_rootNode.save(out);
    }
}
