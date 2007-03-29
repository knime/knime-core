/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   14.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;


/**
 * This Panel holds subpanels consisting of SortItems.
 * 
 * @see SortItem
 * @author Nicolas Cebron, University of Konstanz
 */
public class SorterNodeDialogPanel2 extends JPanel {
    private static final long serialVersionUID = -1757898824881266019L;

    /*
     * The entry in the JComboBox for not sorting a column
     */
    private static final String NOSORT = "- DO NOT SORT -";

    /*
     * Keeps track of the components on this JPanel
     */
    private Vector<SortItem> m_components;

    /*
     * The DataTableSpec
     */
    private DataTableSpec m_spec;
    
    /*
     * Flag for whether to perform the sorting in memory or not.
     */
    private boolean m_memory;
    
    /*
     * Corresponding checkbox
     */
    private JCheckBox m_memorycheckb;

    /**
     * Constructs a new empty JPanel used for displaying the three first
     * selected columns in the according order and the sorting order for each.
     * 
     */
    SorterNodeDialogPanel2() {
        BoxLayout bl = new BoxLayout(this, BoxLayout.Y_AXIS);
        super.setLayout(bl);
        m_components = new Vector<SortItem>();
        m_memory = false;
    }

    /**
     * Updates this panel based on the DataTableSpec, the list of columns to
     * include and the corresponding sorting order.
     * 
     * @param spec the DataTableSpec
     * @param incl the list to include
     * @param sortOrder the sorting order
     * @param nrsortitems the inital number of sortitems to be shown
     * @param sortInMemory whether to perform the sorting in memory or not
     */
    public void update(final DataTableSpec spec, final List<String> incl,
            final boolean[] sortOrder, final int nrsortitems,
            final boolean sortInMemory) {
        m_spec = spec;
        m_memory = sortInMemory;
        super.removeAll();
        m_components.removeAllElements();
        int interncounter = 0;

        if (spec != null) {
            Vector<String> values = new Vector<String>();
            values.add(NOSORT);
            for (int j = 0; j < spec.getNumColumns(); j++) {
                values.add(spec.getColumnSpec(j).getName());
            }
            if ((incl == null) && (sortOrder == null)) {

                for (int i = 0; i < nrsortitems 
                && i < spec.getNumColumns(); i++) {
                    Object selected = (i == 0) ? values.get(i + 1) : values
                            .get(0);
                    SortItem temp = new SortItem(i, values, selected, true);
                    super.add(temp);
                    m_components.add(temp);
                }
            } else {
                for (int i = 0; i < incl.size(); i++) {
                    String toInclude = incl.get(i);
                    if (spec.findColumnIndex(toInclude) != -1) {
                        SortItem temp = new SortItem(interncounter, values,
                                toInclude, sortOrder[interncounter]);
                        super.add(temp);
                        m_components.add(temp);
                        interncounter++;
                    } else if (toInclude.toString().equals(NOSORT)) {
                        SortItem temp = new SortItem(interncounter, values,
                                toInclude, sortOrder[interncounter]);
                        super.add(temp);
                        m_components.add(temp);
                        interncounter++;
                    }

                }
            }
            Box buttonbox = Box.createHorizontalBox();
            Border addColumnBorder = BorderFactory
                    .createTitledBorder("Add columns");
            buttonbox.setBorder(addColumnBorder);
            int maxCols = m_spec.getNumColumns() - m_components.size();
            SpinnerNumberModel snm = new SpinnerNumberModel(0, 0, maxCols, 1);
            final JSpinner spinner = new JSpinner(snm);
            spinner.setMaximumSize(new Dimension(100, 30));
            spinner.setPreferredSize(new Dimension(100, 30));
            JButton addSortItemButton = new JButton("new columns");
            addSortItemButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent ae) {
                    ArrayList<String> newlist = new ArrayList<String>();
                    for (int i = 0; i < m_components.size(); i++) {
                        SortItem temp = m_components.get(i);
                        newlist.add(temp.getSelectedColumn().toString());
                    }
                    int oldsize = m_components.size();
                    String temp = spinner.getValue().toString();
                    int newsize = Integer.parseInt(temp);
                    for (int n = oldsize; n < oldsize + newsize; n++) {
                        newlist.add(m_spec.getColumnSpec(n).getName());
                    }
                    boolean[] oldbool = new boolean[oldsize];
                    boolean[] newbool = new boolean[oldsize + newsize];
                    // copy old values
                    for (int i = 0; i < m_components.size(); i++) {
                        SortItem temp2 = m_components.get(i);
                        newbool[i] = temp2.getSortOrder();
                    }
                    // create new values
                    for (int i = oldbool.length; i < newbool.length; i++) {
                        newbool[i] = true;
                    }
                    update(m_spec, newlist, newbool, (oldsize + newsize),
                            m_memory);
                }
            });
            buttonbox.add(spinner);
            buttonbox.add(Box.createHorizontalStrut(10));
            buttonbox.add(addSortItemButton);
            super.add(buttonbox);
            Box memorybox = Box.createHorizontalBox();
            m_memorycheckb = new JCheckBox("Sort in memory", m_memory);
            m_memorycheckb.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent ae) {
                    if (m_memorycheckb.isSelected()) {
                        m_memory = true;
                    } else {
                        m_memory = false;
                    }
                }
            });
            memorybox.add(m_memorycheckb);
            super.add(memorybox);
            revalidate();
        }
    }

    /**
     * Returns all columns from the include list.
     * 
     * @return a list of all columns from the include list
     */
    public List<String> getIncludedColumnList() {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < m_components.size(); i++) {
            SortItem temp = m_components.get(i);
            if (!(temp.getSelectedColumn().toString().equals(NOSORT))) {
                list.add(temp.getSelectedColumn().toString());
            }
        }
        return list;
    }

    /**
     * Returns the sortOrder array.
     * 
     * @return sortOrder
     */
    public boolean[] getSortOrder() {
        Vector<Boolean> boolvector = new Vector<Boolean>();
        for (int i = 0; i < m_components.size(); i++) {
            SortItem temp = m_components.get(i);
            if (!(temp.getSelectedColumn().toString().equals(NOSORT))) {
                boolvector.add(temp.getSortOrder());
            }
        }
        boolean[] boolarray = new boolean[boolvector.size()];
        for (int i = 0; i < boolarray.length; i++) {
            boolarray[i] = boolvector.get(i);
        }
        return boolarray;
    }
    
    /**
     * @return whether to perform the sorting in memory or not.
     */
    public boolean sortInMemory() {
        return m_memory;
    }
}
