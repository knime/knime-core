/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   25.11.2005 (Fabian Dill): created
 */
package org.knime.base.node.mine.subgroupminer.apriori;

/**
 * An ArrayPrefixTreeNode contains an array containing the counter for the
 * items, where the array position serves as the item identifier. Each item may
 * have a child, again, the array position serves as the identifier for that
 * child, that is, the child for item x is in children[x]. Although this
 * implicates a waste of storage the accessing time is linear. Additionally, a
 * link to the parent node and the referring index to the prefix is stored.
 * 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ArrayPrefixTreeNode {

    private int[] m_itemCounter;

    private ArrayPrefixTreeNode[] m_children;

    private int m_length;

    private ArrayPrefixTreeNode m_parent;

    private int m_parentIndex;

    /**
     * Constructs an ArrayPrefixTreeNode with the length specifying the size of
     * the array, that is, the number of countable items in that node.
     * Additionally, a link to the parent node with the index of the predecessor
     * item, that is, the prefix.
     * 
     * @param length the number of countable items in that node
     * @param parent the parent node
     * @param parentIndex the index of the prefix item
     */
    public ArrayPrefixTreeNode(final int length,
            final ArrayPrefixTreeNode parent, final int parentIndex) {
        m_length = length;
        m_itemCounter = new int[m_length];
        m_parent = parent;
        m_parentIndex = parentIndex;
    }

    /**
     * Returns the parent index, the prefix.
     * 
     * @return the parent index, the prefix
     */
    public int getParentIndex() {
        return m_parentIndex;
    }

    /**
     * Retuns a link to the parent node.
     * 
     * @return a link to the parent node
     */
    public ArrayPrefixTreeNode getParent() {
        return m_parent;
    }

    /**
     * Returns the length of the array, that is, the umber of countable items.
     * 
     * @return the number of countable items
     */
    public int getLength() {
        return m_length;
    }

    /**
     * Increments the counter for the item with id = pos.
     * 
     * @param pos the identifier for that item equal to its position in the
     *            array
     */
    public void increment(final int pos) {
        m_itemCounter[pos]++;
    }

    /**
     * Returns the counter, the support, for the item with identifier pos.
     * 
     * @param pos the identifier for that item equal to its position in the
     *            array
     * @return the counter or support for the item specified by pos
     */
    public int getCounterFor(final int pos) {
        return m_itemCounter[pos];
    }

    /**
     * Returns the sum of that counter for a pruning heuristic.
     * 
     * @return the sum of all counters
     */
    public int getSumOfCounter() {
        int sum = 0;
        for (int i = 0; i < m_itemCounter.length; i++) {
            sum += m_itemCounter[i];
        }
        return sum;
    }

    /**
     * Deletes the child for the item specified by its position pos.
     * 
     * @param pos the position of the item whose child should be deleted
     */
    public void deleteChild(final int pos) {
        m_children[pos] = null;
    }

    /**
     * Returns the child for that given item specified by pos.
     * 
     * @param pos the identifier, position of the item, whose child should be
     *            returned
     * @return the child of the item specified by pos
     */
    public ArrayPrefixTreeNode getChild(final int pos) {
        if (m_children == null) {
            return null;
        }
        return m_children[pos];
    }

    /**
     * Creates an empty child for the item specified by pos.
     * 
     * @param pos the identifier, position of the item, for which a child should
     *            be created
     */
    public void createChildAt(final int pos) {
        if (m_children == null) {
            m_children = new ArrayPrefixTreeNode[m_length];
        }
        m_children[pos] = new ArrayPrefixTreeNode(m_length, this, pos);
    }

    /**
     * Creates emtpy children for all items, whose support is greater or equal
     * to minSupport.
     * 
     * @param minSupport the support which an item should at least have that a
     *            child for is created
     * @return true if any child was created, false otherwise
     */
    public boolean createChildren(final int minSupport) {
        boolean childCreated = false;
        for (int i = 0; i < m_itemCounter.length; i++) {
            if (i >= minSupport) {
                createChildAt(i);
                childCreated = true;
            }
        }
        return childCreated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer("item counter:");
        for (int i = 0; i < m_itemCounter.length; i++) {
            buff.append(m_itemCounter[i] + " ");
        }
        return buff.toString();
    }
}
