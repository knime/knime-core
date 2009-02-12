/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
