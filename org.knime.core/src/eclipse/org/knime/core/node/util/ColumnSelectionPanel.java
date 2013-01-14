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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node.util;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashSet;
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
import org.knime.core.data.DataColumnSpecCreator;
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

    private static final String NONE_OPTION_LABEL = "<none>";

    private static final String ROWID_OPTION_LABEL = "<RowID>";

    /** Contains all column names for the given given filter class. */
    private final JComboBox m_chooser;


    /**Show only columns which pass the given {@link ColumnFilter}.*/
    private final ColumnFilter m_columnFilter;

    private boolean m_isRequired;

    /**If set to true a no column item is added as first item of the list.*/
    private boolean m_addNoneColOption = false;

    /**If set to true a RowID item is added as first item of the list.*/
    private boolean m_addRowIDOption = false;

    private DataColumnSpec m_noneColSpec;

    private DataColumnSpec m_rowIDColSpec;

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
     * Creates new Panel that will filter columns using the given
     * {@link ColumnFilter}. The panel will have a border as given.
     * If null, no border is set.
     *
     *
     * @param columnFilter {@link ColumnFilter}. The combo box
     *            will allow to select only columns compatible with the
     *            column filter. All other columns will be ignored.
     * @param border Border for the panel or null to have no border.
     *
     * @see #update(DataTableSpec,String)
     */
    public ColumnSelectionPanel(final Border border,
            final ColumnFilter columnFilter) {
       this(border, columnFilter, false);
    }
    /**
     * Creates new Panel that will filter columns using the given
     * {@link ColumnFilter}. The panel will have a border as given.
     * If null, no border is set.
     *
     *
     * @param columnFilter {@link ColumnFilter}. The combo box
     *            will allow to select only columns compatible with the
     *            column filter. All other columns will be ignored.
     * @param border Border for the panel or null to have no border.
     * @param addNoneCol true, if a none option should be added to the column
     * list
     * @see #update(DataTableSpec,String)
     */
    public ColumnSelectionPanel(final Border border,
            final ColumnFilter columnFilter, final boolean addNoneCol) {
        this(border, columnFilter, addNoneCol, false);
    }
    /**
     * Creates new Panel that will filter columns using the given
     * {@link ColumnFilter}. The panel will have a border as given.
     * If null, no border is set.
     *
     *
     * @param columnFilter {@link ColumnFilter}. The combo box
     *            will allow to select only columns compatible with the
     *            column filter. All other columns will be ignored.
     * @param border Border for the panel or null to have no border.
     * @param addNoneCol true, if a none option should be added to the column
     * list
     * @param addRowID true, if a RowID option should be added to the column
     * list
     * @see #update(DataTableSpec,String)
     */
    public ColumnSelectionPanel(final Border border,
            final ColumnFilter columnFilter, final boolean addNoneCol,
            final boolean addRowID) {
        if (columnFilter == null) {
            throw new NullPointerException("ColumnFilter must not be null");
        }
        m_columnFilter = columnFilter;
        if (border != null) {
            setBorder(border);
        }
        m_chooser = new JComboBox();
        m_chooser.setRenderer(new DataColumnSpecListCellRenderer());
        m_chooser.setMinimumSize(new Dimension(100, 25));
        m_chooser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        m_isRequired = true;
        m_addNoneColOption = addNoneCol;
        m_addRowIDOption = addRowID;
        add(m_chooser);
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
        this(border, new DataValueColumnFilter(filterValueClasses));
    }

    /**
     * Creates a column selection panel with a label instead of a border which
     * preserves the minimum size to either the label width or the combo box
     * width.
     * @param label label of the combo box.
     * @param columnFilter {@link ColumnFilter}. The combo box
     *            will allow to select only columns compatible with the
     *            column filter. All other columns will be ignored.
     */
    public ColumnSelectionPanel(final JLabel label,
            final ColumnFilter columnFilter) {
        if (columnFilter == null) {
            throw new NullPointerException("ColumnFilter must not be null");
        }
        m_columnFilter = columnFilter;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        m_chooser = new JComboBox();
        m_chooser.setRenderer(new DataColumnSpecListCellRenderer());
        m_chooser.setMinimumSize(new Dimension(100, 25));
        m_chooser.setMaximumSize(new Dimension(200, 25));
        m_isRequired = true;
        final Box labelBox = Box.createHorizontalBox();
        labelBox.add(label);
        labelBox.add(Box.createHorizontalGlue());
        add(labelBox);
        add(Box.createVerticalGlue());
        final Box chooserBox = Box.createHorizontalBox();
        chooserBox.add(m_chooser);
        chooserBox.add(Box.createHorizontalGlue());
        add(chooserBox);
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
        this(label, new DataValueColumnFilter(filterValueClasses));
    }

    /**
     * True, if a compatible type is required, false otherwise.
     * If required an exception is thrown in the update method +
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
     * Use the {@link #update(DataTableSpec, String, boolean)} method instead
     * if the {@link ColumnSelectionPanel} contains the none and RowID option.
     *
     * @param spec To get the column names, types and the current index from.
     * @param selColName The column name to be set as chosen.
     * @throws NotConfigurableException If the spec does not contain at least
     * one compatible type.
     * @see #update(DataTableSpec, String, boolean)
     */
    public final void update(final DataTableSpec spec, final String selColName)
    throws NotConfigurableException {
        update(spec, selColName, false);
    }

    /**
     * Updates this filter panel by removing all current items and adding the
     * columns according to the content of the argument <code>spec</code>. If
     * a column name is provided and it is not filtered out the corresponding
     * item in the combo box will be selected.
     *
     * @param spec To get the column names, types and the current index from.
     * @param selColName The column name to be set as chosen.
     * @param useRowID set this parameter to <code>true</code> and the
     * selColName to <code>null</code> if the the row id option should be
     * selected
     * @throws NotConfigurableException If the spec does not contain at least
     * one compatible type.
     * @since 2.6
     */
    public final void update(final DataTableSpec spec, final String selColName,
            final boolean useRowID)
    throws NotConfigurableException {
        m_spec = spec;
        m_chooser.removeAllItems();
        if (m_addNoneColOption) {
            final String noneOption =
                DataTableSpec.getUniqueColumnName(spec, NONE_OPTION_LABEL);
            m_noneColSpec = new DataColumnSpecCreator(noneOption,
                    DataType.getMissingCell().getType()).createSpec();
            m_chooser.addItem(m_noneColSpec);
            m_chooser.setToolTipText("Select " + noneOption + " for no column");
        }
        if (m_addRowIDOption) {
            final String rowIDOption =
                DataTableSpec.getUniqueColumnName(spec, ROWID_OPTION_LABEL);
            m_rowIDColSpec = new DataColumnSpecCreator(rowIDOption,
                    DataType.getMissingCell().getType()).createSpec();
            m_chooser.addItem(m_rowIDColSpec);
            m_chooser.setToolTipText("Select " + rowIDOption + " for RowID");
        }
        if (spec != null) {
            DataColumnSpec selectMe = null;
            for (int c = 0; c < spec.getNumColumns(); c++) {
                final DataColumnSpec current = spec.getColumnSpec(c);
                if (m_columnFilter.includeColumn(current)) {
                    m_chooser.addItem(current);
                    if (current.getName().equals(selColName)) {
                        selectMe = current;
                    }
                }
            }
            if (selectMe != null) {
                m_chooser.setSelectedItem(selectMe);
            } else {
                if (m_addNoneColOption && !useRowID) {
                    m_chooser.setSelectedItem(m_noneColSpec);
                } else if (m_addRowIDOption && useRowID) {
                    m_chooser.setSelectedItem(m_rowIDColSpec);
                } else {
                    // select last element
                    final int size = m_chooser.getItemCount();
                    if (size > 0) {
                        m_chooser.setSelectedIndex(size - 1);
                    }
                }
            }
        }
        if (m_chooser.getItemCount() == 0 && m_isRequired) {
            throw new NotConfigurableException(m_columnFilter.allFilteredMsg());
        }
    }

    /**
     * Returns the {@link DataTableSpec} used to
     * {@link #update(DataTableSpec, String)} this component.
     * @return the underlying spec
     * @since 2.6
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @return <code>true</code> if the user has selected the RowID
     */
    public boolean rowIDSelected() {
        return (m_addRowIDOption && m_rowIDColSpec != null
                && m_rowIDColSpec.equals(m_chooser.getSelectedItem()));
    }

    /**
     * Gets the selected column as DataColumnSpec. If the
     * addNoneCol flag is true the method returns <code>null</code> if the user
     * has selected the no column item from the combo box.
     *
     * @return The spec of the column that is currently selected or
     * <code>null</code> if the addNoneCol flag is set to true and
     * the no column item is selected or the addRowID flag is set to true and
     * the RowID column item is selected. Check the useRowID column if the
     * RowID option was selected.
     */
    public final DataColumnSpec getSelectedColumnAsSpec() {
        final DataColumnSpec selected =
            (DataColumnSpec)m_chooser.getSelectedItem();
        if (selected == null
                || (m_addNoneColOption && selected.equals(m_noneColSpec))
                || (m_addRowIDOption && selected.equals(m_rowIDColSpec))) {
            return null;
        }
        return selected;
    }

    /**
     * Gets the selected column. If the addNoneCol flag
     * is true the method returns <code>null</code> if the user
     * has selected the no column item from the combo box.
     *
     * @return The cell that is currently being selected or <code>null</code>
     * if the addNoneCol flag is set to true and the no column item is selected
     * or the addRowID flag is set to true and the RowID column item is
     * selected. Check the useRowID column if the RowID option was selected.
     */
    public final String getSelectedColumn() {
        final DataColumnSpec selected = getSelectedColumnAsSpec();
        return selected != null ? selected.getName() : null;
    }


    /**
     * Attempts to set the argument as selected column and disables the combo
     * box if there is only one available.
     *
     * @param colName Name of the fixed x columns.
     */
    public void fixSelectablesTo(final String... colName) {
        final DataColumnSpec oldSelected = (DataColumnSpec)m_chooser
            .getSelectedItem();
        final HashSet<String> hash =
            new HashSet<String>(Arrays.asList(colName));
        final Vector<DataColumnSpec> survivers = new Vector<DataColumnSpec>();
        for (int item = 0; item < m_chooser.getItemCount(); item++) {
            final DataColumnSpec s = (DataColumnSpec)m_chooser.getItemAt(item);
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

    /** @return the number of selectable elements in the list. */
    public int getNrItemsInList() {
        return m_chooser.getModel().getSize();
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
        if (m_addNoneColOption
                && m_noneColSpec != null
                && columnName == null) {
            m_chooser.setSelectedItem(m_noneColSpec);
            return;
        }
        if (m_spec != null) {
            final DataColumnSpec colSpec = m_spec.getColumnSpec(columnName);
            m_chooser.setSelectedItem(colSpec);
        }
    }

    /**
     * Selects the RowID item in the item list if a RowID entry exists.
     */
    public final void setRowIDSelected() {
        if (m_addRowIDOption && m_rowIDColSpec != null) {
            m_chooser.setSelectedItem(m_rowIDColSpec);
        }
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
