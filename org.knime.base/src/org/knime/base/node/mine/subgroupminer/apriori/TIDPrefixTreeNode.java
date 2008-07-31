/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   17.12.2005 (Fabian Dill): created
 */
package org.knime.base.node.mine.subgroupminer.apriori;

import java.util.ArrayList;
import java.util.List;

/**
 * An TIDPrefixTreeNode consists of a TIDItemset, the items in the node, and a
 * list of children of this node.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class TIDPrefixTreeNode {
    private TIDItemSet m_itemSet;

    private List<TIDPrefixTreeNode> m_children;

    /**
     * Creates an instance of a TIDPrefixTreeNode with a given set of items.
     * 
     * @param items the items in that node
     */
    public TIDPrefixTreeNode(final TIDItemSet items) {
        m_itemSet = items;
    }

    /**
     * Adds a child to that node.
     * 
     * @param child the child to be added
     */
    public void addChild(final TIDPrefixTreeNode child) {
        if (m_children == null) {
            m_children = new ArrayList<TIDPrefixTreeNode>();
        }
        m_children.add(child);
    }

    /**
     * Returns the list of children.
     * 
     * @return the list of children of this node
     */
    public List<TIDPrefixTreeNode> getChildren() {
        return m_children;
    }

    /**
     * 
     * @return the items in this node
     */
    public TIDItemSet getItemSet() {
        return m_itemSet.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer(m_itemSet.toString() + "(");
        if (m_children != null) {
            for (TIDPrefixTreeNode child : m_children) {
                buff.append(" " + child);
            }
        }
        buff.append(")");
        return buff.toString();
    }
}
