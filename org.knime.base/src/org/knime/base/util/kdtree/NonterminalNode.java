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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.util.kdtree;

/**
 * This class represents non-terminal nodes inside the k-d tree. A non-terminal
 * node defines a split of the data in its two sub-trees. The index of the split
 * attribute and the corresponding split-value are stored inside the node,
 * together with the two child nodes.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
class NonterminalNode implements Node {
    private final int m_splitAttribute;

    private final double m_splitValue;

    private final Node m_left, m_right;

    /**
     * Creates a new non-terminal node.
     * 
     * @param splitAttribute the index of the split attribute
     * @param splitValue the split value
     * @param left the left child, can be <code>null</code>
     * @param right the right child, can be <code>null</code>
     */
    public NonterminalNode(final int splitAttribute, final double splitValue,
            final Node left, final Node right) {
        m_splitAttribute = splitAttribute;
        m_splitValue = splitValue;
        m_left = left;
        m_right = right;
    }

    /**
     * Returns the attribute index that is used for the split.
     * 
     * @return the split attribute index
     */
    public int getSplitAttribute() {
        return m_splitAttribute;
    }

    /**
     * Returns the split value.
     * 
     * @return the split value
     */
    public double getSplitValue() {
        return m_splitValue;
    }

    /**
     * Returns the left child node. Can be <code>null</code>.
     * 
     * @return the left child
     */
    public Node getLeft() {
        return m_left;
    }

    /**
     * Returns the right child node. Can be <code>null</code>.
     * 
     * @return the right child
     */
    public Node getRight() {
        return m_right;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "(" + m_splitAttribute + ", " + m_splitValue + ")";
    }
}
