/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node.util;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.NotConfigurableException;


/**
 * Class implements a panel to choose a column of a certain type retrieved from
 * the <code>DataTableSpec</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 *  
 */
public class ColumnSelectionPanel extends JPanel {
    private static final long serialVersionUID = 9095144868702823700L;

    /** Contains all column names for the given given filter class. */
    private final JComboBox m_chooser;

    /** Show only columns of types that are compatible 
     * to one of theses classes. */
    private final Class<? extends DataValue>[] m_filterClasses;
    
    private boolean m_isRequired;
    
    private DataTableSpec m_spec;
    
    /**
     * Creates new Panel that will filter columns for particular value classes.
     * The panel will have a titled border with name "Column Selection".
     * 
     * @param filterValueClasses classes derived from DataValue. The combo box
     *            will allow to select only columns compatible with one of 
     *            these classes. All other columns will be ignored.
     * 
     * @see #update(DataTableSpec,String)
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
    @SuppressWarnings("unchecked")
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
     * @see #update(DataTableSpec,String)
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
     * @see #update(DataTableSpec,String)
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
        m_chooser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        m_isRequired = true;
        add(m_chooser);
    }
    
    /**
     * Creates a column selection panel with a label instead of a border which 
     * preserves the minimum size to either the label width or the combo box 
     * width.
     * @param label label of the combo box.
     * @param filterValueClasses allowed classes.
     */
    public ColumnSelectionPanel(final JLabel label, 
            final Class<? extends DataValue>...filterValueClasses) {
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
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        m_chooser = new JComboBox();
        m_chooser.setRenderer(new DataColumnSpecListCellRenderer());
        m_chooser.setMinimumSize(new Dimension(100, 25));
        m_chooser.setMaximumSize(new Dimension(200, 25));
        m_isRequired = true;
        Box labelBox = Box.createHorizontalBox();
        labelBox.add(label);
        labelBox.add(Box.createHorizontalGlue());
        add(labelBox);
        add(Box.createVerticalGlue());
        Box chooserBox = Box.createHorizontalBox();
        chooserBox.add(m_chooser);
        chooserBox.add(Box.createHorizontalGlue());
        add(chooserBox);        
    }
    
    /**
     * True, if a compatible type is required, false otherwise.
     * If required an excpetion is thrown in the update method +
     * if no compatible type was found in the input spec. If it is not required
     * this exception is suppressed.
     * @param isRequired True, if at least one compatible type is required, 
     *  false otherwise.
     */
    public final void setRequired(final boolean isRequired) {
        m_isRequired = isRequired;
    }
    
    /**
     * Indicates whether in the current configuration at least one compatible 
     * type is required or not.
     * @return True, if at least one compatible type is required, false 
     * otherwise.
     */
    public final boolean isRequired() {
        return m_isRequired;
    }
    
    
    /**
     * Updates this filter panel by removing all current items and adding the
     * columns according to the content of the argument <code>spec</code>. If
     * a column name is provided and it is not filtered out the corresponding
     * item in the combo box will be selected.
     * 
     * @param spec To get the column names, types and the current index from.
     * @param selColName The column name to be set as chosen.
     * @throws NotConfigurableException If the spec does not contain at least
     * one compatible type.
     */
    public final void update(final DataTableSpec spec, final String selColName) 
    throws NotConfigurableException {
        m_spec = spec;
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
        if (m_chooser.getItemCount() == 0 && m_isRequired) {
            StringBuffer error = new StringBuffer(
                    "No column in spec compatible to");
            if (m_filterClasses.length == 1) {
                error.append(" \"");
                error.append(m_filterClasses[0].getSimpleName());
                error.append('"');
            } else {
                for (int i = 0; i < m_filterClasses.length; i++) {
                    error.append(" \"");
                    error.append(m_filterClasses[i].getSimpleName());
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
        DataColumnSpec selected = (DataColumnSpec)m_chooser.getSelectedItem();
        if (selected != null) {
            return selected.getName();
        }
        return null;
    }
    
    
    /**
     * Attempts to set the argument as selected column and disables the combo
     * box if there is only one available.
     * 
     * @param colName Name of the fixed x columns.
     */
    public void fixSelectablesTo(final String... colName) {
        DataColumnSpec oldSelected = (DataColumnSpec)m_chooser
            .getSelectedItem();
        HashSet<String> hash = new HashSet<String>(Arrays.asList(colName));
        Vector<DataColumnSpec> survivers = new Vector<DataColumnSpec>();
        for (int item = 0; item < m_chooser.getItemCount(); item++) {
            DataColumnSpec s = (DataColumnSpec)m_chooser.getItemAt(item);
            if (hash.contains(s.getName())) {
                survivers.add(s);
            }
        }
        m_chooser.setModel(new DefaultComboBoxModel(survivers));
        if (survivers.contains(oldSelected)) {
            m_chooser.setSelectedItem(oldSelected);
        } else {
            // may be -1 ... but that is ok
            m_chooser.setSelectedIndex(survivers.size() - 1);
        }
        m_chooser.setEnabled(survivers.size() > 1);
    }
    
    
    /**
     * 
     * @return the selected index. 
     */
    public final int getSelectedIndex() {
        return m_chooser.getSelectedIndex();
    }
    
    /**
     * Selects the given index in the combo box.
     * @param index Select this item.
     */
    public final void setSelectedIndex(final int index) {
        m_chooser.setSelectedIndex(index);
    }
    
    /**
     * 
     * @param columnName - the name of the column to select.
     */
    public final void setSelectedColumn(final String columnName) {
        DataColumnSpec colSpec = m_spec.getColumnSpec(columnName);
        m_chooser.setSelectedItem(colSpec);
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
