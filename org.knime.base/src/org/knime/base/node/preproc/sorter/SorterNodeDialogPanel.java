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
 * History
 *   08.02.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;


/**
 * Extension to serve the purpose of Column Sorting.
 * 
 * @see SorterNodeDialogPanel
 * 
 * @author Nicolas Cebron, University of Konstanz
 */

public final class SorterNodeDialogPanel extends JPanel {
    private static final long serialVersionUID = -4370835468477694930L;

    /*
     * Include list and model.
     */
    private final JList m_inclList;

    private final DefaultListModel m_inclMdl;

    /*
     * Exclude list and model.
     */
    private final JList m_exclList;

    private final DefaultListModel m_exclMdl;

    /*
     * Ascending/Descending chooser
     */
    private final JComboBox m_sortorderComboBox;

    /*
     * Model for m_sortorderComboBox
     */
    private final DefaultComboBoxModel m_comboModel;

    /*
     * Entries for m_sortorderComboBox
     */
    private final String m_asc = "Ascending";

    private final String m_desc = "Descending";

    /*
     * Data table spec used to keep initial column ordering.
     */
    private DataTableSpec m_spec = null;

    /*
     * Vector containing information about the sorting order for each Row true:
     * ascending false: descending
     */
    private Vector<Boolean> m_sortOrder = new Vector<Boolean>();

    /**
     * Creates a new panel consisting of
     * <ul>
     * <li>include list (columns to include in the sorting procedure)</li>
     * <li>exclude list (columns to exclude in the sorting procedure)</li>
     * <li>ComboBox to choose between ascending/descending sort order for each
     * column</li>
     * </ul>
     * The communication between the {@link SorterNodeDialogPanel} and the
     * {@link SorterNodeDialog} is implemented with update and getter-methods.
     * 
     * @see #update(DataTableSpec, List, boolean[])
     * @see #getIncludedColumnList()
     * @see #getSortOrder()
     */

    public SorterNodeDialogPanel() {
        // keeps buttons 'add', 'add all', 'remove', 'remove all'
        // 'Move Up' and 'Move Down' and 'ascending/descending'
        final JPanel buttonPan = new JPanel();
        GridLayout gl = new GridLayout(7, 1);
        buttonPan.setLayout(gl);

        final JButton remButton = new JButton("remove >>");
        remButton.setPreferredSize(new Dimension(125, 25));
        buttonPan.add(remButton);
        remButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemIt();
            }
        });

        final JButton remAllButton = new JButton("remove all >>");
        remAllButton.setPreferredSize(new Dimension(125, 25));
        buttonPan.add(remAllButton);
        remAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemAll();
            }
        });

        final JButton addButton = new JButton("<< add");
        addButton.setPreferredSize(new Dimension(125, 25));
        buttonPan.add(addButton);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddIt();
            }
        });

        final JButton addAllButton = new JButton("<< add all");
        addAllButton.setPreferredSize(new Dimension(125, 25));
        buttonPan.add(addAllButton);
        addAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddAll();
            }
        });

        final JButton upButton = new JButton("Move up");
        upButton.setToolTipText("Move the currently selected list item higher");
        upButton.setPreferredSize(new Dimension(125, 25));
        buttonPan.add(upButton);
        upButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onUp();
            }
        });

        final JButton downButton = new JButton("Move down");
        downButton
                .setToolTipText("Move the currently selected list item lower");
        downButton.setPreferredSize(new Dimension(125, 25));
        buttonPan.add(downButton);
        downButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onDown();
            }
        });

        // Sorting order list
        m_comboModel = new DefaultComboBoxModel();
        m_comboModel.addElement(m_asc);
        m_comboModel.addElement(m_desc);
        m_sortorderComboBox = new JComboBox(m_comboModel);
        m_sortorderComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onsortOrder();
            }
        });
        buttonPan.add(m_sortorderComboBox, BorderLayout.SOUTH);

        // include list
        m_inclMdl = new DefaultListModel();
        m_inclList = new JList(m_inclMdl);
        m_inclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_inclList.addListSelectionListener(new ListSelectionListener() {
            /**
             * Listener method for list selection changes
             * 
             * @param e the ListSelectionEvent
             * @see javax.swing.event.ListSelectionListener#valueChanged
             *      (javax.swing.event.ListSelectionEvent)
             */

            public void valueChanged(final ListSelectionEvent e) {
                if (!(e.getValueIsAdjusting())) {
                    showSortOrder();
                }
            }
        });

        final JScrollPane jspIncl = new JScrollPane(m_inclList);
        jspIncl.setPreferredSize(new Dimension(200, 225));
        jspIncl.setBorder(BorderFactory
                .createTitledBorder(" Columns to sort (Order)"));

        // exclude list
        m_exclMdl = new DefaultListModel();
        m_exclList = new JList(m_exclMdl);
        m_exclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        final JScrollPane jspExcl = new JScrollPane(m_exclList);
        jspExcl.setPreferredSize(new Dimension(200, 250));
        jspExcl
                .setBorder(BorderFactory
                        .createTitledBorder(" Exclude Columns "));

        // adds include, button, exclude component
        GridLayout gl1 = new GridLayout(1, 3);
        super.setLayout(gl1);
        super.add(jspIncl);
        JPanel buttonPan2 = new JPanel(new FlowLayout());
        buttonPan2.add(buttonPan);
        super.add(buttonPan2);
        super.add(jspExcl);
    }

    /*
     * Called by the 'remove >>' button to exclude the selected elements from
     * the include list.
     */
    private void onRemIt() {
        Object[] incls = m_inclList.getSelectedValues();
        int[] selInd = m_inclList.getSelectedIndices();
        Arrays.sort(selInd);
        int delta = 0;
        if (m_spec != null) {
            for (int i = 0; i < incls.length; i++) {
                m_exclMdl.addElement(incls[i]);
                m_inclMdl.removeElement(incls[i]);
                m_sortOrder.remove(selInd[i] - delta);
                delta++;
            }

            List<Object> l = Arrays.asList(m_exclMdl.toArray());
            m_exclMdl.removeAllElements();
            for (int i = 0; i < m_spec.getNumColumns(); i++) {
                String c = m_spec.getColumnSpec(i).getName();
                if (l.contains(c)) {
                    m_exclMdl.addElement(c);
                }
            }
        }
    }

    /*
     * Called by the 'remove >>' button to exclude all elements from the include
     * list.
     */
    private void onRemAll() {
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        m_sortOrder.removeAllElements();
        if (m_spec != null) {
            for (int c = 0; c < m_spec.getNumColumns(); c++) {
                m_exclMdl.addElement(m_spec.getColumnSpec(c).getName());
            }
        }
    }

    /*
     * Called by the ' < < add' button to include the selected elements from the
     * exclude list.
     */
    private void onAddIt() {
        // add all selected elements from the exclude to the include list
        Object[] o = m_exclList.getSelectedValues();
        if (o != null) {
            for (int i = 0; i < o.length; i++) {
                m_inclMdl.addElement(o[i]);
                m_exclMdl.removeElement(o[i]);
            }
        }
        // again, remove all from the include list and start adding them from
        // the table spec by double-checking the include list
        List<Object> l = Arrays.asList(m_inclMdl.toArray());
        m_inclMdl.removeAllElements();
        if (m_spec != null) {
            for (int i = 0; i < m_spec.getNumColumns(); i++) {
                String c = m_spec.getColumnSpec(i).getName();
                if (l.contains(c)) {
                    m_inclMdl.addElement(c);
                    m_sortOrder.addElement(true);
                }
            }
        }
    }

    /*
     * Called by the ' < < add all' button to include all elements from the
     * exclude list.
     * 
     */
    private void onAddAll() {
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        if (m_spec != null) {
            for (int c = 0; c < m_spec.getNumColumns(); c++) {
                m_inclMdl.addElement(m_spec.getColumnSpec(c).getName());
                m_sortOrder.addElement(true);
            }
        }
    }

    /*
     * Called by the move up button.
     */
    private void onUp() {
        int[] moveMe = m_inclList.getSelectedIndices();
        if (moveMe.length > 0) {
            // not already at top
            if (moveMe[0] > 0) {
                for (int i = 0; i < moveMe.length; i++) {
                    swap(moveMe[i], moveMe[i] - 1);
                    swapsortOrder(moveMe[i], moveMe[i] - 1);
                    moveMe[i] = moveMe[i] - 1;
                    m_inclList.ensureIndexIsVisible(moveMe[i]);
                }
            }
            m_inclList.setSelectedIndices(moveMe);
        }
        m_inclList.requestFocus();
    }

    /*
     * Called by the move down button.
     */
    private void onDown() {
        int[] moveMe = m_inclList.getSelectedIndices();
        if (moveMe.length > 0) {
            if (moveMe[moveMe.length - 1] < m_inclMdl.getSize() - 1) {
                for (int i = moveMe.length - 1; i >= 0; i--) {
                    swap(moveMe[i], moveMe[i] + 1);
                    swapsortOrder(moveMe[i], moveMe[i] + 1);
                    moveMe[i] = moveMe[i] + 1;
                    m_inclList.ensureIndexIsVisible(moveMe[i]);
                }
            }
            m_inclList.setSelectedIndices(moveMe);
        }
        m_inclList.requestFocus();
    }

    /*
     * Called by the sortOrder ComboBox. Writes the changes in the m_sortOrder
     * Vector.
     */
    private void onsortOrder() {
        int[] changeMe = m_inclList.getSelectedIndices();
        if ((m_inclMdl.getSize() > 0) && (changeMe.length > 0)) {
            for (int i = 0; i < changeMe.length; i++) {
                if (m_comboModel.getSelectedItem().equals(m_asc)) {
                    m_sortOrder.set(changeMe[i], true);
                } else {
                    m_sortOrder.set(changeMe[i], false);
                }
            }
        }
        // set the focus back on the list
        m_inclList.requestFocus();
    }

    /*
     * Each time, an item on the include list is selected, the ComboBox shows
     * the corresponding sort order.
     */
    private void showSortOrder() {
        if (m_inclList.getSelectedIndex() >= 0) {
            int selectedIndex = m_inclList.getSelectedIndex();
            Boolean bool = m_sortOrder.get(selectedIndex);
            if (bool.booleanValue()) {
                m_comboModel.setSelectedItem(m_asc);
            } else {
                m_comboModel.setSelectedItem(m_desc);
            }
        }
    }

    /*
     * Swap two elements in the includelist
     */
    private void swap(final int a, final int b) {
        Object aObject = m_inclMdl.getElementAt(a);
        Object bObject = m_inclMdl.getElementAt(b);
        m_inclMdl.set(a, bObject);
        m_inclMdl.set(b, aObject);
    }

    /*
     * Swap two elements in the sortOrder Vector
     */
    private void swapsortOrder(final int a, final int b) {
        Boolean ab = m_sortOrder.get(a);
        Boolean bb = m_sortOrder.get(b);
        m_sortOrder.set(a, bb);
        m_sortOrder.set(b, ab);
    }

    /**
     * Updates this panel by removing all current selections from the include
     * and exclude list. The include list will contain all column names from the
     * include list afterwards and the corresponding sort order for each column.
     * If the include list is null, all column names from the
     * {@link DataTableSpec} are added to the include list and the sort order is
     * set to 'ascending' for each column.
     * 
     * @param spec the spec to retrieve the column names from
     * @param incl the list of columns to include
     * @param sortOrder ascending/descending order for the included columns
     */
    public void update(final DataTableSpec spec, final List<DataCell> incl,
            final boolean[] sortOrder) {
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        m_sortOrder.removeAllElements();
        int interncounter = 0;
        if (spec != null) {
            m_spec = spec;

            if ((incl == null) && (sortOrder == null)) {
                for (int i = 0; i < m_spec.getNumColumns(); i++) {
                    final String c = spec.getColumnSpec(i).getName();
                    m_inclMdl.addElement(c);
                    m_sortOrder.add(true);
                }
            } else {
                for (int i = 0; i < m_spec.getNumColumns(); i++) {
                    final String c = spec.getColumnSpec(i).getName();
                    if (incl.contains(c)) {
                        m_inclMdl.addElement(c);
                        if (sortOrder[interncounter]) {
                            m_sortOrder.add(true);
                        } else {
                            m_sortOrder.add(false);
                        }
                        interncounter++;
                    } else {
                        m_exclMdl.addElement(c);
                    }
                }
            }
            repaint();
        }
    }

    /**
     * Returns all columns from the include list.
     * 
     * @return a list of all columns from the include list
     */
    public List<DataCell> getIncludedColumnList() {
        final ArrayList<DataCell> list = new ArrayList<DataCell>();
        for (int i = 0; i < m_inclMdl.getSize(); i++) {
            list.add((DataCell)m_inclMdl.get(i));
        }
        return list;
    }

    /**
     * Returns the sortOrder array.
     * 
     * @return m_sortOrder
     */
    public boolean[] getSortOrder() {

        boolean[] retSortOrder = new boolean[m_sortOrder.size()];
        for (int i = 0; i < m_sortOrder.size(); i++) {
            retSortOrder[i] = m_sortOrder.get(i);
        }
        return retSortOrder;
    }
}
