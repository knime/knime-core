/*
 * ------------------------------------------------------------------ *
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   14.03.2007 (mb): created
 */
package org.knime.core.node.workflow;

/**
 * Holds hierarchical ID of a node. The hierarchy models nested meta nodes.
 * All IDs will have one static instance of ROOTID as their top ID in this
 * hierarchy.
 *
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 */
public class NodeID implements Comparable<NodeID> {
    private final NodeID m_prefix;
    private final int m_index;

    static final NodeID ROOTID = new NodeID();

    /** Creates now NodeID object based on a predefined prefix (usually the
     * ID of the encapsulating project or metanode) and the node's ID itself.
     *
     * @param prefix of ID
     * @param ix itself
     */
    public NodeID(final NodeID prefix, final int ix) {
        assert ix >= 0;
        assert prefix != null;
        m_prefix = prefix;
        m_index = ix;
    }

    /** Creates top level NodeID object.
     *
     * @param ix itself
     */
    public NodeID(final int ix) {
        assert ix >= 0;
        m_prefix = ROOTID;
        m_index = ix;
    }

    /* Create root node id.
     */
    private NodeID() {
        m_prefix = null;
        m_index = 0;
    }

    /**
     * @return prefix of this node's ID.
     */
    public NodeID getPrefix() {
        return m_prefix;
    }

    /**
     * @return index of this node (without prefix!).
     */
    public int getIndex() {
        return m_index;
    }
    
    public String getIDWithoutRoot() {
        String id = toString();
        String withoutRoot = id.substring(id.indexOf(":") + 1);
        return withoutRoot;
    }

    /** Checks for exact matching prefixes.
     *
     * @param prefix to check
     * @return true if prefix are the same
     */
    public boolean hasSamePrefix(final NodeID prefix) {
        return m_prefix.equals(prefix);
    }

    /** Checks for matching prefix (this node prefix can be longer, though).
     *
     * @param prefix to check
     * @return true if prefix are the same
     */
    public boolean hasPrefix(final NodeID prefix) {
        if (m_prefix.equals(prefix)) {
            return true;
        }
        if (m_prefix == ROOTID) {
            return false;
        }
        return m_prefix.hasPrefix(prefix);
    }

    /** Returns on string representation of index.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        if (m_prefix != null) {
            return m_prefix + ":" + m_index;
        }
        return Integer.toString(m_index);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof NodeID)) {
            return false;
        }
        NodeID objID = (NodeID)obj;
        return this.compareTo(objID) == 0;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        // TODO make more efficient and smarter?
        return this.toString().hashCode();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final NodeID o) {
        if (this == o) {
            return 0;
        }
        if (this == ROOTID) {
            return -1;
        }
        if (o == ROOTID) {
            return +1;
        }
        int prefixComp = this.m_prefix.compareTo(o.m_prefix);
        if (prefixComp != 0) {
            return prefixComp;
        }
        return m_index - o.m_index;
    }

}
