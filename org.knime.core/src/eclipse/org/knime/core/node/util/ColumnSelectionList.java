/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * History
 *   27.02.2008 (thor): created
 */
package org.knime.core.node.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;

/**
 * This class show columns from a {@link DataTableSpec} inside a {@link JList}.
 * The usual renderer for {@link DataColumnSpec}s is used and the list can be
 * made non-selectable without disabling it, so that the entries are still shown
 * in black and not gray.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ColumnSelectionList extends JList {
    private final DefaultListModel m_listModel;

    private boolean m_selectionChangeAllowed;

    /**
     * Selection model allows list selection changes only if
     * m_selectionChangeAllowed is <code>true</code>.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    private class MyListSelectionModel extends DefaultListSelectionModel {
        /**
         * {@inheritDoc}
         */
        @Override
        public void insertIndexInterval(final int index, final int length,
                final boolean before) {
            if (m_selectionChangeAllowed) {
                super.insertIndexInterval(index, length, before);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void moveLeadSelectionIndex(final int leadIndex) {
            if (m_selectionChangeAllowed) {
                super.moveLeadSelectionIndex(leadIndex);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeIndexInterval(final int index0, final int index1) {
            if (m_selectionChangeAllowed) {
                super.removeIndexInterval(index0, index1);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setAnchorSelectionIndex(final int anchorIndex) {
            if (m_selectionChangeAllowed) {
                super.setAnchorSelectionIndex(anchorIndex);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setLeadSelectionIndex(final int leadIndex) {
            if (m_selectionChangeAllowed) {
                super.setLeadSelectionIndex(leadIndex);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setSelectionInterval(final int index0, final int index1) {
            if (m_selectionChangeAllowed) {
                super.setSelectionInterval(index0, index1);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addSelectionInterval(final int index0, final int index1) {
            if (m_selectionChangeAllowed) {
                super.addSelectionInterval(index0, index1);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeSelectionInterval(final int idx0, final int idx1) {
            if (m_selectionChangeAllowed) {
                super.removeSelectionInterval(idx0, idx1);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSelectionModel(final ListSelectionModel selectionModel) {
        throw new UnsupportedOperationException("This list does not support "
                + "changing the list selection model");
    }

    /**
     * Creates a new column selection list. By default the selection can't be
     * changed.
     */
    public ColumnSelectionList() {
        super(new DefaultListModel());
        m_selectionChangeAllowed = false;
        m_listModel = (DefaultListModel)getModel();
        setCellRenderer(new DataColumnSpecListCellRenderer());
        super.setSelectionModel(new MyListSelectionModel());
    }

    /**
     * Updates the list model by first removing all entries and then inserting
     * all columns from the given spec. Several columns can be pre-selected.
     *
     * @param spec a data table spec
     * @param selectedCols an array with selected column names.
     */
    public void update(final DataTableSpec spec, final String... selectedCols) {
        m_listModel.clear();
        for (DataColumnSpec cs : spec) {
            m_listModel.addElement(cs);
        }
        if (selectedCols != null) {
            setSelectedColumns(selectedCols);
        }
    }

    /**
     * Updates the list model by first removing all entries and then inserting
     * all columns from the given spec. Several columns can be pre-selected.
     *
     * @param spec a data table spec
     * @param selectedCols a collection with selected column names. Must not be
     *            <code>null</code>.
     */
    public void update(final DataTableSpec spec,
            final Collection<String> selectedCols) {
        update(spec, selectedCols.toArray(new String[selectedCols.size()]));
    }

    /**
     * Updates the list model by first removing all entries and then inserting
     * all columns from the given collection.
     *
     * @param spec a data table spec
     */
    public void update(final DataTableSpec spec) {
        update(spec, (String[])null);
    }

    /**
     * Returns a collection with the names of all currently selected columns.
     *
     * @return a collection with column names
     */
    public Collection<String> getSelectedColumns() {
        List<String> selCols = new ArrayList<String>();
        for (int i : getSelectedIndices()) {
            selCols.add(((DataColumnSpec)m_listModel.get(i)).getName());
        }

        return selCols;
    }

    /**
     * Selects all given columns in the list. Non-existing columns are ignored.
     *
     * @param selCols a collection with column names. Must not be
     *            <code>null</code>.
     */
    public void setSelectedColumns(final Collection<String> selCols) {

        setSelectedColumns(selCols.toArray(new String[selCols.size()]));

    }

    /**
     * Selects all given columns in the list. Non-existing columns are ignored.
     *
     * @param selCols an array with column names. Must not be <code>null</code>.
     */
    public void setSelectedColumns(final String... selCols) {

        ListSelectionModel selModel = getSelectionModel();

        boolean oldvalue = m_selectionChangeAllowed;
        m_selectionChangeAllowed = true;

        selModel.clearSelection();

        for (int i = 0; i < m_listModel.getSize(); i++) {
            String name = ((DataColumnSpec)m_listModel.get(i)).getName();
            for (int j = 0; j < selCols.length; j++) {
                if (name.equals(selCols[j])) {
                    selModel.addSelectionInterval(i, i);
                    break;
                }
            }
        }

        m_selectionChangeAllowed = oldvalue;
    }

    /**
     * Makes the list selection (un)changeable by the user.
     *
     * @param b <code>true</code> if the user can change the selection,
     *            <code>false</code> otherwise
     */
    public void setUserSelectionAllowed(final boolean b) {
        m_selectionChangeAllowed = b;
    }

    /**
     * Returns if the user can change the list selection or not.
     *
     * @return <code>true</code> if the user can change the selection,
     *            <code>false</code> otherwise
     */
    public boolean isUserSelectionAllowed() {
        return m_selectionChangeAllowed;
    }
}
