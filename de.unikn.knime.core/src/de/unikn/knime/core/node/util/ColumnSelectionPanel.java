/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node.util;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.Border;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DataValue;

/**
 * Class implements a panel to choose a column of a certain type retrieved from
 * the <code>DataTableSpec</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 *  
 */
public class ColumnSelectionPanel extends JPanel {

    /** Contains all column names for the given given filter class. */
    private final JComboBox m_chooser;

    /** Show only columns of types that are compatible 
     * to one of theses classes. */
    private final Class<? extends DataValue>[] m_filterClasses;
    
    /**
     * Creates new Panel that will filter columns for particular value classes.
     * The panel will have a titled border with name "Column Selection".
     * 
     * @param filterValueClasses classes derived from DataValue. The combo box
     *            will allow to select only columns compatible with one of 
     *            these classes. All other columns will be ignored.
     * 
     * @see #update(DataTableSpec,DataCell)
     */
    public ColumnSelectionPanel(
            final Class<? extends DataValue>... filterValueClasses) {
            this(" Column Selection ", filterValueClasses);
    }

    /**
     * Creates a new column selection panel with the given border title; all
     * column are included in the combox box.
     * @param borderTitle The border title.
     */
    public ColumnSelectionPanel(final String borderTitle) {
        this(borderTitle, DataValue.class);
    }
            
    /**
     * Creates new Panel that will filter columns for particular value classes.
     * The panel will have a title border with a given title.
     * 
     * @param filterValueClasses a class derived from DataValue. The combo box
     *            will allow to select only columns compatible with one of 
     *            these classes. All other columns will be ignored.
     * @param borderTitle The title of the border
     * 
     * @see #update(DataTableSpec,DataCell)
     */
    public ColumnSelectionPanel(final String borderTitle,
            final Class<? extends DataValue>... filterValueClasses) {
        this(BorderFactory.createTitledBorder(borderTitle), filterValueClasses);
    }
    
    /**
     * Creates new Panel that will filter columns for particular value classes.
     * The panel will have a border as given. If null, no border is set.
     * 
     * @param filterValueClasses classes derived from DataValue. The combo box
     *            will allow to select only columns compatible with one of
     *            theses classes. All other columns will be ignored.
     * @param border Border for the panel or null to have no border.
     * 
     * @see #update(DataTableSpec,DataCell)
     */
    public ColumnSelectionPanel(final Border border,
            final Class<? extends DataValue>... filterValueClasses) {
        super(new FlowLayout());
        if (filterValueClasses == null || filterValueClasses.length == 0) {
            throw new NullPointerException("Classes must not be null");
        }
        List<Class<? extends DataValue>> list = 
            Arrays.asList(filterValueClasses);
        if (list.contains(null)) {
            throw new NullPointerException("List of value classes must not " 
                    + "contain null elements.");
        }
        m_filterClasses = filterValueClasses;
        if (border != null) {
            setBorder(border);
        }
        m_chooser = new JComboBox();
        m_chooser.setRenderer(new DataColumnSpecListCellRenderer());
        m_chooser.setMinimumSize(new Dimension(100, 25));
        add(m_chooser);
    }
    
    
    
    /**
     * Updates this filter panel by removing all current items and adding the
     * columns according to the content of the argument <code>spec</code>. If
     * a column name is provided and it is not filtered out the corresponding
     * item in the combo box will be selected.
     * 
     * @param spec To get the column names, types and the current index from.
     * @param selColName The column name to be set as chosen.
     */
    public final void update(final DataTableSpec spec, 
            final DataCell selColName) {
        m_chooser.removeAllItems();
        if (spec != null) {
            DataColumnSpec selectMe = null;
            for (int c = 0; c < spec.getNumColumns(); c++) {
                DataColumnSpec current = spec.getColumnSpec(c);
                DataType type = current.getType();
                for (Class<? extends DataValue> cl : m_filterClasses) {
                    if (type.isCompatible(cl)) {
                        m_chooser.addItem(current);
                        if (current.getName().equals(selColName)) {
                            selectMe = current;
                        }
                        break;
                    }
                }
            }
            if (selectMe != null) {
                m_chooser.setSelectedItem(selectMe);
            } else {
                // select last element
                int size = m_chooser.getItemCount();
                if (size > 0) {
                    m_chooser.setSelectedIndex(size - 1);
                }
            }
        }
    }

    /**
     * Gets the selected column.
     * 
     * @return The cell that is currently being selected.
     */
    public final DataCell getSelectedColumn() {
        DataColumnSpec selected = (DataColumnSpec)m_chooser.getSelectedItem();
        if (selected != null) {
            return selected.getName();
        }
        return null;
    }
    
    /**
     * Selects the given index in the combo box.
     * @param index Select this item.
     */
    public final void setSelectedIndex(final int index) {
        m_chooser.setSelectedIndex(index);
    }
    
    /**
     * @param enabled true if enabled otherwise false.
     * @see java.awt.Component#setEnabled(boolean)
     */
    @Override
    public void setEnabled(final boolean enabled) {
        m_chooser.setEnabled(enabled);
    }

    /**
     * Adds an item listener to the underlying combo box.
     * @param aListener The listener to be registered
     * @see JComboBox#addItemListener(ItemListener)
     */
    public void addItemListener(final ItemListener aListener) {
        m_chooser.addItemListener(aListener);
    }

    /**
     * Removes an item listener to the underlying combo box.
     * @param aListener The listener to be unregistered
     * @see JComboBox#removeItemListener(ItemListener)
     */
    public void removeItemListener(final ItemListener aListener) {
        m_chooser.removeItemListener(aListener);
    }

    /**
     * Delegate method to the underlying combo box.
     * @param l The action listener being added from the combo box.
     * @see JComboBox#addActionListener(ActionListener)
     */
    public void addActionListener(final ActionListener l) {
        m_chooser.addActionListener(l);
    }

    /**
     * Delegate method to the underlying combo box.
     * @param l The action listener being removed from the combo box.
     * @see JComboBox#removeActionListener(ActionListener)
     */
    public void removeActionListener(final ActionListener l) {
        m_chooser.removeActionListener(l);
    }
    
    

} // ColumnSelectionPanel
