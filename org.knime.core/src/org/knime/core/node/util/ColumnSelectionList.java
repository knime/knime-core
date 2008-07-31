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
