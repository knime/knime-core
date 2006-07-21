/*
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
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.border.Border;

import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DataValue;
import de.unikn.knime.core.node.NotConfigurableException;

/**
 * Class extends a JComboxBox to choose a column of a certain type retrieved
 * from the <code>DataTableSpec</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class ColumnSelectionComboxBox extends JComboBox {

    private static final long serialVersionUID = 5797563450894378207L;

    /**
     * Show only columns of types that are compatible to one of theses classes.
     */
    private final Class<? extends DataValue>[] m_filterClasses;

    /**
     * Creates new Panel that will filter columns for particular value classes.
     * The panel will have a titled border with name "Column Selection".
     * 
     * @param filterValueClasses classes derived from DataValue. The combo box
     *            will allow to select only columns compatible with one of these
     *            classes. All other columns will be ignored.
     * 
     * @see #update(DataTableSpec, String)
     */
    public ColumnSelectionComboxBox(
            final Class<? extends DataValue>... filterValueClasses) {
        this(" Column Selection ", filterValueClasses);
    }

    /**
     * Creates a new column selection panel with the given border title; all
     * column are included in the combox box.
     * 
     * @param borderTitle The border title.
     */
    public ColumnSelectionComboxBox(final String borderTitle) {
        this(borderTitle, DataValue.class);
    }

    /**
     * Creates new Panel that will filter columns for particular value classes.
     * The panel will have a title border with a given title.
     * 
     * @param filterValueClasses a class derived from DataValue. The combo box
     *            will allow to select only columns compatible with one of these
     *            classes. All other columns will be ignored.
     * @param borderTitle The title of the border
     * 
     * @see #update(DataTableSpec, String)
     */
    public ColumnSelectionComboxBox(final String borderTitle,
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
     * @see #update(DataTableSpec, String)
     */
    public ColumnSelectionComboxBox(final Border border,
            final Class<? extends DataValue>... filterValueClasses) {
        if (filterValueClasses == null || filterValueClasses.length == 0) {
            throw new NullPointerException("Classes must not be null");
        }
        List<Class<? extends DataValue>> list = Arrays
                .asList(filterValueClasses);
        if (list.contains(null)) {
            throw new NullPointerException("List of value classes must not "
                    + "contain null elements.");
        }
        m_filterClasses = filterValueClasses;
        if (border != null) {
            setBorder(border);
        }
        setRenderer(new DataColumnSpecListCellRenderer());
        setMinimumSize(new Dimension(100, 25));
    }

    
    /**
     * Updates this filter panel by removing all current items and adding the
     * columns according to the content of the argument <code>spec</code>. If
     * a column name is provided and it is not filtered out the corresponding
     * item in the combo box will be selected.
     * 
     * @param sp To get the column names, types and the current index from.
     * @param selColName The column name to be set as chosen.
     * @throws NotConfigurableException If the spec does not contain any column
     * compatible to the target value class(es) as given in constructor.       
     */
    public final void update(final DataTableSpec sp, final String selColName) 
        throws NotConfigurableException {
        update(sp, selColName, false);
    }
    
    /**
     * Updates this filter panel by removing all current items and adding the
     * columns according to the content of the argument <code>spec</code>. If
     * a column name is provided and it is not filtered out the corresponding
     * item in the combo box will be selected.
     * 
     * @param spec To get the column names, types and the current index from.
     * @param selColName The column name to be set as chosen.
     * @param suppressEvents <code>true</code> if events caused by adding items 
     *        to the combo box should be suppressed, <code>false</code> 
     *        otherwise.
     * @throws NotConfigurableException If the spec does not contain any column
     * compatible to the target value class(es) as given in constructor.       
     */
    public final void update(final DataTableSpec spec, 
            final String selColName, final boolean suppressEvents) 
        throws NotConfigurableException {
        ItemListener[] itemListeners = null;
        ActionListener[] actionListeners = null;
        
        if (suppressEvents) {
            itemListeners = getListeners(ItemListener.class);
            for (ItemListener il : itemListeners) {
                removeItemListener(il);
            }
            
            actionListeners = getListeners(ActionListener.class);
            for (ActionListener al : actionListeners) {
                removeActionListener(al);
            }
        }
        
        removeAllItems();
        DataColumnSpec selectMe = null;
        if (spec != null) {
            for (int c = 0; c < spec.getNumColumns(); c++) {
                DataColumnSpec current = spec.getColumnSpec(c);
                DataType type = current.getType();
                for (Class<? extends DataValue> cl : m_filterClasses) {
                    if (type.isCompatible(cl)) {
                        addItem(current);
                        if (current.getName().equals(selColName)) {
                            selectMe = current;
                        }
                        break;
                    }
                }
            }
            setSelectedItem(null);
        }

        if (suppressEvents) {
            for (ItemListener il : itemListeners) {
                addItemListener(il);
            }
            
            for (ActionListener al : actionListeners) {
                addActionListener(al);
            }
        }

    
        if (selectMe != null) {            
            setSelectedItem(selectMe);
        } else {
            // select last element
            int size = getItemCount();
            if (size > 0) {
                setSelectedIndex(size - 1);
            }
        }
        if (getItemCount() == 0) {
            StringBuffer error = new StringBuffer(
                    "No column in spec compatible to");
            if (m_filterClasses.length == 1) {
                error.append(" \"");
                error.append(m_filterClasses[0].getSimpleName());
                error.append('"');
            } else {
                for (int i = 0; i < m_filterClasses.length; i++) {
                    error.append(" \"");
                    error.append(m_filterClasses[0].getSimpleName());
                    error.append('"');
                    if (i == m_filterClasses.length - 2) { // second last
                        error.append(" or");
                    }
                }
            }
            error.append('.');
            throw new NotConfigurableException(error.toString());
        }
    }

    /**
     * Gets the selected column.
     * 
     * @return The cell that is currently being selected.
     */
    public final String getSelectedColumn() {
        DataColumnSpec selected = (DataColumnSpec)getSelectedItem();
        if (selected != null) {
            return selected.getName();
        }
        return null;
    }
    
    /** 
     * Selects the column with the name provided in the argument. Does nothing
     * if the argument is <code>null</code> or the name is invalid.
     * @param name The name of the column.
     */
    public final void setSelectedColumn(final String name) {
        if (name == null) {
            return;
        }
        final int size = getItemCount();
        for (int i = 0; i < size; i++) {
            DataColumnSpec colSpec = (DataColumnSpec)getItemAt(i);
            if (colSpec.getName().equals(name)) {
                setSelectedIndex(i);
                return;
            }
        }
    }
}
