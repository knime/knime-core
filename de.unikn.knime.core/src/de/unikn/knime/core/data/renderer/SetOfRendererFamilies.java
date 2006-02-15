/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.core.data.renderer;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.JList;
import javax.swing.JTable;

/**
 * Container for <code>DataCellRendererFamily</code> that is by itself a 
 * renderer family (yes, now it becomes complicated). This class is used in 
 * <code>DataType</code> when all available native renderer are gathered
 * and returned as DataCellRendererFamily. 
 * 
 * <p><strong>Note:</strong>This is a helper class that shouldn't be 
 * any useful for you.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SetOfRendererFamilies implements DataCellRendererFamily {
    
    private final List<DataCellRendererFamily> m_list;
    private DataCellRenderer m_active;
    
    /**
     * Constructs a new set from a list of renderer families given in a list.
     * The active renderer will be the first one that is available.
     * @param fams All renderer in a list (type DataCellRendererFamily)
     * @throws IllegalArgumentException If list is empty
     * @throws NullPointerException If argument is null
     * @throws ClassCastException If list contains unexpected classes.
     */
    public SetOfRendererFamilies(final List<DataCellRendererFamily> fams) {
        if (fams.isEmpty()) {
            throw new IllegalArgumentException("No renderer available");
        }
        for (DataCellRendererFamily e : fams) {
            if (m_active == null) {
                m_active = e;
            }
        }
        m_list = fams;
    }

    /**
     * @see DataCellRendererFamily#getRendererDescriptions()
     */
    public String[] getRendererDescriptions() {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (DataCellRendererFamily e : m_list) {
            set.addAll(Arrays.asList(e.getRendererDescriptions()));
        }
        return set.toArray(new String[0]);
    }

    /**
     * @see DataCellRendererFamily#setActiveRenderer(java.lang.String)
     */
    public void setActiveRenderer(final String desc) {
        for (DataCellRendererFamily e : m_list) {
            if (Arrays.asList(e.getRendererDescriptions()).contains(desc)) {
                e.setActiveRenderer(desc);
                m_active = e;
                return;
            }
        }
    }

    /**
     * @see DataCellRenderer#getDescription()
     */
    public String getDescription() {
        return m_active.getDescription();
    }

    /**
     * @see DataCellRenderer#getPreferredSize()
     */
    public Dimension getPreferredSize() {
        return m_active.getPreferredSize();
    }

    /**
     * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(
     * javax.swing.JTable, Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(
            final JTable table, final Object value, final boolean isSelected, 
            final boolean hasFocus, final int row, final int column) {
        return m_active.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
    }

    /**
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(
     * javax.swing.JList, java.lang.Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, 
            final boolean isSelected, final boolean cellHasFocus) {
        return m_active.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
    }

}
