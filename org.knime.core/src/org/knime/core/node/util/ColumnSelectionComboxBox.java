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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node.util;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.NotConfigurableException;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.border.Border;

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
     * Show only columns that are compatible to the ColumnFilter.
     */
    private ColumnFilter m_columnFilter;

    private DataTableSpec m_spec;

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
    @SuppressWarnings("unchecked")
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
        this(border, new DataValueColumnFilter(filterValueClasses));
    }

    /**
     * Creates new Panel that will filter columns for particular value classes.
     * The panel will have a border as given. If null, no border is set.
     *
     * @param columnFilter The combo box will allow to select only columns which
     *            are not filtered by this {@link ColumnFilter}
     * @param border Border for the panel or null to have no border.
     *
     * @see #update(DataTableSpec, String)
     */
    public ColumnSelectionComboxBox(final Border border,
            final ColumnFilter columnFilter) {
        if (columnFilter == null) {
            throw new NullPointerException("Column filter must not be null");
        }
        m_columnFilter = columnFilter;
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
     *             compatible to the target value class(es) as given in
     *             constructor.
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
     * @param suppressEvents <code>true</code> if events caused by adding
     *            items to the combo box should be suppressed,
     *            <code>false</code> otherwise.
     * @throws NotConfigurableException If the spec does not contain any column
     *             compatible to the target value class(es) as given in
     *             constructor.
     */
    public final void update(final DataTableSpec spec, final String selColName,
            final boolean suppressEvents) throws NotConfigurableException {
        update(spec, selColName, suppressEvents, m_columnFilter);
    }

    /**
     * Updates this filter panel by removing all current items and adding the
     * columns according to the content of the argument <code>spec</code>. If
     * a column name is provided and it is not filtered out the corresponding
     * item in the combo box will be selected.
     *
     * @param spec To get the column names, types and the current index from.
     * @param selColName The column name to be set as chosen.
     * @param suppressEvents <code>true</code> if events caused by adding
     *            items to the combo box should be suppressed,
     *            <code>false</code> otherwise.
     * @param filter a filter that filters the columns that should be shown in
     *            the combo box; this overrides the value classes given in the
     *            constructor
     * @throws NotConfigurableException If the spec does not contain any column
     *             compatible to the target value class(es) as given in
     *             constructor.
     */
    public final void update(final DataTableSpec spec, final String selColName,
            final boolean suppressEvents, final ColumnFilter filter)
            throws NotConfigurableException {
        m_spec = spec;
        m_columnFilter = filter;
        ItemListener[] itemListeners = null;
        ActionListener[] actionListeners = null;

        if (suppressEvents) {
            itemListeners = getListeners(ItemListener.class);
            for (final ItemListener il : itemListeners) {
                removeItemListener(il);
            }

            actionListeners = getListeners(ActionListener.class);
            for (final ActionListener al : actionListeners) {
                removeActionListener(al);
            }
        }

        removeAllItems();
        DataColumnSpec selectMe = null;
        if (m_spec != null) {
            for (int c = 0; c < m_spec.getNumColumns(); c++) {
                final DataColumnSpec current = m_spec.getColumnSpec(c);
                if (m_columnFilter.includeColumn(current)) {
                    addItem(current);
                    if (current.getName().equals(selColName)) {
                        selectMe = current;
                    }
                }
            }
            setSelectedItem(null);
        }

        if (suppressEvents) {
            for (final ItemListener il : itemListeners) {
                addItemListener(il);
            }

            for (final ActionListener al : actionListeners) {
                addActionListener(al);
            }
        }

        if (selectMe != null) {
            setSelectedItem(selectMe);
        } else {
            // select last element
            final int size = getItemCount();
            if (size > 0) {
                setSelectedIndex(size - 1);
            }
        }
        if (getItemCount() == 0) {
            throw new NotConfigurableException(m_columnFilter.allFilteredMsg());
        }
    }

    /**
     * @param allowedTypes filter for the columns all column not compatible with
     *            any of the allowed types are not displayed.
     *
     * @throws NotConfigurableException If the spec does not contain any column
     *             compatible to the given filter.
     */
    public void setAllowedTypes(
            final Class<? extends DataValue>... allowedTypes)
    throws NotConfigurableException {
        setColumnFilter(new DataValueColumnFilter(allowedTypes));
    }

    /**
     * Sets the internal used {@link ColumnFilter} to the given one and calls
     * the {@link #update(DataTableSpec, String, boolean, ColumnFilter)}
     * method to update the column panel.
     *
     * @param filter the new {@link ColumnFilter} to use
     * @throws NotConfigurableException If the spec does not contain any column
     *             compatible to the given filter.
     */
    public void setColumnFilter(final ColumnFilter filter)
    throws NotConfigurableException {
        m_columnFilter = filter;
        if (m_spec == null) {
            //the spec is not available that's why we do not need to call
            //the update method
            return;
        }
        update(m_spec, getSelectedColumn(), false, filter);
    }

    /**
     * Gets the selected column.
     *
     * @return The cell that is currently being selected.
     */
    public final String getSelectedColumn() {
        final DataColumnSpec selected = (DataColumnSpec)getSelectedItem();
        if (selected != null) {
            return selected.getName();
        }
        return null;
    }

    /**
     * Selects the column with the name provided in the argument. Does nothing
     * if the argument is <code>null</code> or the name is invalid.
     *
     * @param name The name of the column.
     */
    public final void setSelectedColumn(final String name) {
        if (name == null) {
            return;
        }
        final int size = getItemCount();
        for (int i = 0; i < size; i++) {
            final DataColumnSpec colSpec = (DataColumnSpec)getItemAt(i);
            if (colSpec.getName().equals(name)) {
                setSelectedIndex(i);
                return;
            }
        }
    }
}
