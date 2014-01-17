/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * ---------------------------------------------------------------------
 *
 * Created on 03.12.2013 by NanoTec
 */
package org.knime.core.node.util;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;

/**
 * A {@link ListModel} which provides a view on a subset of the model. The view can be created by
 * {@link #filterIndices(int[])} and reset by {@link #showAll()}.
 *
 * @author Marcel Hanser, University of Konstanz
 * @since 2.10
 */
final class FilterableListModel extends DefaultListModel {

    private static final long serialVersionUID = 1L;

    private final DefaultListModel m_originalObjects;

    private boolean m_filtered = false;

    /**
     *
     */
    public FilterableListModel() {
        super();
        this.m_originalObjects = new DefaultListModel();
    }

    /**
     * Creates a view containing the elements defined by the given index array of the original added elements.
     *
     * @param indices the indices to show
     */
    public void filterIndices(final int[] indices) {
        super.clear();
        m_filtered = true;
        for (int i : indices) {
            super.addElement(m_originalObjects.get(i));
        }
    }

    /**
     * Resets the view.
     */
    public void showAll() {
        super.clear();
        m_filtered = false;
        for (int i = 0; i < m_originalObjects.size(); i++) {
            super.addElement(m_originalObjects.get(i));
        }
    }

    /**
     * @return the original model containing all elements
     */
    public ListModel getUnfilteredModel() {
        return m_originalObjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        super.clear();
        m_originalObjects.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeElementAt(final int index) {
        checkUnfiltered();
        super.removeElementAt(index);
        m_originalObjects.removeElementAt(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addElement(final Object obj) {
        super.addElement(obj);
        m_originalObjects.addElement(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeElement(final Object obj) {
        super.removeElement(obj);
        return m_originalObjects.removeElement(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllElements() {
        super.removeAllElements();
        m_originalObjects.removeAllElements();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final int index, final Object element) {
        checkUnfiltered();
        super.add(index, element);
        m_originalObjects.add(index, element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object remove(final int index) {
        checkUnfiltered();
        super.remove(index);
        return m_originalObjects.remove(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRange(final int fromIndex, final int toIndex) {
        checkUnfiltered();
        super.removeRange(fromIndex, toIndex);
        m_originalObjects.removeRange(fromIndex, toIndex);
    }

    private void checkUnfiltered() {
        if (m_filtered) {
            throw new IllegalStateException(
                "Managing the filtered model with indices while it is filtered leads to unexpected behavior");
        }
    }
}
