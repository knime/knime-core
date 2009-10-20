/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
