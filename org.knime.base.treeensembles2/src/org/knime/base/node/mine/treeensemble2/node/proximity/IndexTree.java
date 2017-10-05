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
 *   08.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.proximity;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 *
 * @author Adrian Nembach
 */
public class IndexTree {

    private int[] m_data;
    private Node m_root;
    private int m_indexPointer;

    public IndexTree(final int numInstances) {
        m_data = new int[numInstances];
        m_root = new Node();
        m_indexPointer = 0;
    }

    public void addIndex(final int index, final TreePath path) {
        if (m_indexPointer >= m_data.length) {
            throw new IllegalStateException("It is not possible to add another index.");
        }
        m_data[m_indexPointer] = index;
        m_root.addIndex(path, m_indexPointer++, 0);
    }

    public double[] getAllPathProximities(final TreePath path) {
        double pathLength = path.size();
        int[] levelsSamePath = new int[m_indexPointer];
        double[] pathProximities = new double[m_indexPointer];
        Node parent = m_root;

        for (int level = 0, childIndex = path.getChild(0); childIndex >= 0; level++, childIndex = path.getChild(level)) {
            Node child = parent.getChild(childIndex);
            if (child == null) {
                break;
            } else {
                BitSet diffParent = child.m_diffParent;
                for (int i = diffParent.nextSetBit(0); i >= 0; i = diffParent.nextSetBit(i + 1)) {
                    levelsSamePath[i]++;
                }
            }
            parent = child;
        }

        for (int i = 0; i < levelsSamePath.length; i++) {
            pathProximities[i] = levelsSamePath[i] / pathLength;
        }

        return pathProximities;
    }

    public double getPathProximity(final TreePath path, final int rootIndex) {
        int levelsSamePath = 0;
        Node parent = m_root;
        for (int level = 0, childIndex = path.getChild(0); childIndex >= 0; level++, childIndex = path.getChild(level)) {
            Node child = parent.getChild(childIndex);
            if (child == null) {
                break;
            } else if (!child.m_diffParent.get(rootIndex)) {
                break;
            } else {
                levelsSamePath += 1;
                parent = child;
            }
        }

        return ((double)levelsSamePath) / path.size();
    }

    public static class Node {
        private List<Node> m_children;
        // the set bits mark which indices from the parent are in this node
        private BitSet m_diffParent;

        public Node() {
            m_children = new ArrayList<Node>();
            m_diffParent = new BitSet();
        }

        public Node getChild(final int childIndex) {
            if (childIndex >= m_children.size()) {
                return null;
            } else {
                return m_children.get(childIndex);
            }
        }

        public void addIndex(final TreePath path, final int parentIndex, final int level) {
            m_diffParent.set(parentIndex);
            int child = path.getChild(level);
            if (child == -1) {
                return;
            }
            int childDiff = child - m_children.size();
            if (childDiff >= 0) {
                for (int i = 0; i <= childDiff; i++) {
                    m_children.add(new Node());
                }
            }
            m_children.get(child).addIndex(path, parentIndex, level + 1);
        }

    }

}
