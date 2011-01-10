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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.node.config;

import java.io.Serializable;
import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * Abstract Config entry holding only a Config entry type. Deriving classes must
 * store the corresponding value and implement.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
abstract class AbstractConfigEntry implements Serializable, TreeNode {
    
    /** The type of the stored value. */
    private final ConfigEntries m_type;
    
    private AbstractConfigEntry m_parent = null;

    private String m_key;
    
    /**
     * Creates a new Config entry by the given key and type.
     * @param type enum type within the <code>ConfigEntries</code>
     * @param key The key under which this value is added.
     * @throws IllegalArgumentException if the type or key is null
     */
    AbstractConfigEntry(final ConfigEntries type, final String key) {
        if (type == null) {
            throw new IllegalArgumentException(
                    "Config entry type can't be null.");
        }
        m_type = type;
        m_key = checkKey(key);
    }
    
    /**
     * Check key for null value.
     * @param key The key to check.
     * @return The original key.
     */
    private static final String checkKey(final String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null!");
        }
        return key;
    }
    
    /**
     * Returns the key for this entry.
     * 
     * @return the key
     */
    public final String getKey() {
        return m_key;
    }
    
    /** Set a new key.
     * @param key the key to set
     * @throws IllegalArgumentException If argument is null. 
     */
    final void setKey(final String key) {
        m_key = checkKey(key);
    }
    
    /**
     * Sets the parent for this entry which is null if not available.
     * @param parent The new parent of the entry.
     */
    final void setParent(final AbstractConfigEntry parent) {
        m_parent = parent;
    }
    
    /*
     * Reviewers (PO and CS): The current key should be renamed to "identifier"
     * or something similar. A new getKey method should return a key for the
     * hashmap, which must be a combination of the type and the identifier.
     * This will resolve the problem of replacing entries with the same 
     * identifier but different keys. The equals and hashCode methods must be 
     * adopted accordingly.
     */
    
    /**
     * @return This Config's type.
     */
    final ConfigEntries getType() {
        return m_type;
    }
    
    /**
     * String summary of this object including key, type, and value.
     * @return key + type + value
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getKey() + " [" + getType().name() + "] -> " + toStringValue();
    }
    
    /**
     * Returns a String representation for this Config entry which is the used 
     * to re-load this Config entry.
     * @return A String representing this Config entry which can be null.
     */
    abstract String toStringValue();
    
    /**
     * Config entries are equal if they are identical.
     * @param o The other object to check against.
     * @return true, if <code>isIdentical(AbstractConfigEntry)</code> returns 
     *         true.
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!o.getClass().equals(this.getClass())) {
            return false;
        }
        return isIdentical((AbstractConfigEntry) o);
    }

    /**
     * Checks the identity of two config entries. They are identical if they 
     * have the same name, are of the same name, and have identical values which
     * is checked by <code>hasIdenticalValue</code>, implemented in the derived 
     * classes.
     * @param ace Entry to check if identical.
     * @return true, if name, type, and value are identical.
     */
    public final boolean isIdentical(final AbstractConfigEntry ace) {
        return m_type.name() == ace.m_type.name()
            && m_key.equals(ace.m_key) 
            && hasIdenticalValue(ace);
    }
    
    /**
     * Derived classes must compare their value with the value in the passed
     * argument (on equality). They can safely assume that the specified object
     * has the same java class, the same type and key.
     * 
     * @param ace the argument to compare the value with
     * @return true if the specified argument stores the same value as this.
     */
    abstract boolean hasIdenticalValue(AbstractConfigEntry ace);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_key.hashCode() ^ m_type.hashCode();
    }
    
    // tree node methods
    
    /**
     * @param childIndex Not is use.
     * @return null, always.
     */
    public TreeNode getChildAt(final int childIndex) {
        return null;
    }

    /**
     * @return 0, always.
     * @see javax.swing.tree.TreeNode#getChildCount()
     */
    public int getChildCount() {
        return 0;
    }

    
    /**
     * @return The parent Config of this object.
     * @see javax.swing.tree.TreeNode#getParent()
     */
    public final TreeNode getParent() {
        return m_parent;
    }

    /**
     * @param node Not in use.
     * @return -1, always.
     * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
     */
    public int getIndex(final TreeNode node) {
        return -1;
    }

    /**
     * @return true, if not a leaf, always.
     * @see javax.swing.tree.TreeNode#getAllowsChildren()
     */
    public final boolean getAllowsChildren() {
        return !isLeaf();
    }

    /**
     * @return true, always.
     * @see javax.swing.tree.TreeNode#isLeaf()
     */
    public boolean isLeaf() {
        return true;
    }

    /**
     * @return An empty enumeration.
     * @see DefaultMutableTreeNode#EMPTY_ENUMERATION
     * @see javax.swing.tree.TreeNode#children()
     */
    public Enumeration<TreeNode> children() {
        return DefaultMutableTreeNode.EMPTY_ENUMERATION;
    }
    
}
