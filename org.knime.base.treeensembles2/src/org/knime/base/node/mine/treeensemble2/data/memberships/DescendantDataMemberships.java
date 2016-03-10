/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   11.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

import java.util.BitSet;
import java.util.HashMap;

/**
 *
 * @author Adrian Nembach
 */
public class DescendantDataMemberships implements DataMemberships{

    private final RootDataMemberships m_root;
    private final int[] m_indicesInRoot;

    private HashMap<Integer, ColumnMemberships> m_locallyCachedColMems;

    public DescendantDataMemberships(final RootDataMemberships root, final int[] indicesInRoot) {
        m_root = root;
        m_indicesInRoot = indicesInRoot;
        m_locallyCachedColMems = new HashMap<Integer, ColumnMemberships>();
    }

    @Override
    public ColumnMemberships getColumnMemberships(final int index) {
        ColumnMemberships colMem = m_locallyCachedColMems.get(index);
        if (colMem == null) {
            colMem = m_root.descendantGetColumnMemberships(index, m_indicesInRoot);
            m_locallyCachedColMems.put(index, colMem);
        }
        return colMem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getRowWeights() {
        final double[] weights = new double[m_indicesInRoot.length];
        for (int i = 0; i < m_indicesInRoot.length; i++) {
            weights[i] = m_root.getRowWeight(m_indicesInRoot[i]);
        }
        return weights;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getOriginalIndices() {
        final int[] originalIndices = new int[m_indicesInRoot.length];
        for (int i = 0; i < m_indicesInRoot.length; i++) {
            originalIndices[i] = m_root.getOriginalIndex(m_indicesInRoot[i]);
        }
        return originalIndices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataMemberships createChildMemberships(final BitSet inChild) {
        return m_root.createDescendantMemberships(inChild);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double getRowWeight(final int index) {
        return m_root.getRowWeight(m_indicesInRoot[index]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginalIndex(final int index) {
        return m_root.getOriginalIndex(m_indicesInRoot[index]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_indicesInRoot.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCountInRoot() {
        return m_root.getRowCountInRoot();
    }

}
