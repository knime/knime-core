/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   21.09.2005 (mb): created
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * Provides a standard component for a dialog that allows to select a column in
 * a given {@link org.knime.core.data.DataTableSpec}. Provides label and list
 * (possibly filtered by a given {@link org.knime.core.data.DataCell} type) as
 * well as functionality to load/store into a settings model.
 * The column name selection list will provide a RowID option if the provided
 * settings model object is an instance of {@link SettingsModelColumnName} which
 * provides the additional method <code>useRowID</code> to check if the
 * RowID was selected.
 *
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentColumnNameSelection extends DialogComponent {

    /** Contains all column names matching the given given filter class. */
    private final ColumnSelectionPanel m_chooser;

    private final JLabel m_label;

    private final int m_specIndex;

    private final ColumnFilter m_columnFilter;

    private final boolean m_isRequired;

    /**
     * Constructor that puts label and combobox into the panel. The dialog will
     * not open until the incoming table spec contains a column compatible to
     * one of the specified {@link DataValue} classes.
     *
     * @param model the model holding the value of this component. If the model
     * is an instance of {@link SettingsModelColumnName} a RowID option is
     * added to the select list.
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param columnFilter {@link ColumnFilter}. The combo box
     *            will allow to select only columns compatible with the
     *            column filter. All other columns will be ignored.
     */
    public DialogComponentColumnNameSelection(final SettingsModelString model,
            final String label, final int specIndex,
            final ColumnFilter columnFilter) {
        this(model, label, specIndex, true, columnFilter);
    }

    /**
     * Constructor that puts label and combobox into the panel. The dialog will
     * not open until the incoming table spec contains a column compatible to
     * one of the specified {@link DataValue} classes.
     *
     * @param model the model holding the value of this component. If the model
     * is an instance of {@link SettingsModelColumnName} a RowID option is
     * added to the select list.
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param classFilter which classes are available for selection
     */
    public DialogComponentColumnNameSelection(final SettingsModelString model,
            final String label, final int specIndex,
            final Class<? extends DataValue>... classFilter) {
        this(model, label, specIndex, true, classFilter);
    }

    /**
     * Constructor that puts label and combobox into the panel.
     *
     * @param model the model holding the value of this component. If the model
     * is an instance of {@link SettingsModelColumnName} a RowID option is
     * added to the select list.
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param isRequired true, if the component should throw an exception in
     *            case of no available compatible column, false otherwise.
     * @param classFilter which classes are available for selection
     */
    public DialogComponentColumnNameSelection(final SettingsModelString model,
            final String label, final int specIndex, final boolean isRequired,
            final Class<? extends DataValue>... classFilter) {
        this(model, label, specIndex, isRequired,
                new DataValueColumnFilter(classFilter));
    }

    /**
     * Constructor that puts label and combobox into the panel.
     *
     * @param model the model holding the value of this component. If the model
     * is an instance of {@link SettingsModelColumnName} a RowID option is
     * added to the select list.
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param isRequired true, if the component should throw an exception in
     *            case of no available compatible column, false otherwise.
     * @param addNoneCol true, if a none option should be added to the column
     * list
     * @param classFilter which classes are available for selection
     */
    public DialogComponentColumnNameSelection(final SettingsModelString model,
            final String label, final int specIndex, final boolean isRequired,
            final boolean addNoneCol,
            final Class<? extends DataValue>... classFilter) {
        this(model, label, specIndex, isRequired, addNoneCol,
                new DataValueColumnFilter(classFilter));
    }

    /**
     * Constructor that puts label and combobox into the panel.
     *
     * @param model the model holding the value of this component. If the model
     * is an instance of {@link SettingsModelColumnName} a RowID option is
     * added to the select list.
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param isRequired true, if the component should throw an exception in
     *            case of no available compatible column, false otherwise.
     * @param columnFilter {@link ColumnFilter}. The combo box
     *            will allow to select only columns compatible with the
     *            column filter. All other columns will be ignored.
     */
    public DialogComponentColumnNameSelection(final SettingsModelString model,
            final String label, final int specIndex, final boolean isRequired,
            final ColumnFilter columnFilter) {
        this(model, label, specIndex, isRequired, false, columnFilter);
    }

    /**
     * Constructor that puts label and combobox into the panel.
     *
     * @param model the model holding the value of this component. If the model
     * is an instance of {@link SettingsModelColumnName} a RowID option is
     * added to the select list.
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param isRequired true, if the component should throw an exception in
     *            case of no available compatible column, false otherwise.
     * @param addNoneCol true, if a none option should be added to the column
     * list
     * @param columnFilter {@link ColumnFilter}. The combo box
     *            will allow to select only columns compatible with the
     *            column filter. All other columns will be ignored.
     */
    public DialogComponentColumnNameSelection(final SettingsModelString model,
            final String label, final int specIndex, final boolean isRequired,
            final boolean addNoneCol, final ColumnFilter columnFilter) {
        super(model);
        m_label = new JLabel(label);
        getComponentPanel().add(m_label);
        m_isRequired = isRequired;
        m_columnFilter = columnFilter;
        final boolean addRowID = (model instanceof SettingsModelColumnName);
        m_chooser = new ColumnSelectionPanel((Border)null, m_columnFilter,
                addNoneCol, addRowID);
        m_chooser.setRequired(m_isRequired);
        getComponentPanel().add(m_chooser);
        m_specIndex = specIndex;

        // we are not listening to the selection panel and not updating the
        // model on a selection change. We set the value in the model right
        // before save

        m_chooser.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // a new item got selected, update the model
                    updateModel();
                }
            }
        });

        // update the selection panel, when the model was changed
        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                // if only the value in the model changes we only set the
                // selected column in the component.
                if ((getModel() instanceof SettingsModelColumnName)
                        && ((SettingsModelColumnName)getModel()).useRowID()) {
                    m_chooser.setRowIDSelected();
                } else {
                    m_chooser.setSelectedColumn(
                            ((SettingsModelString)getModel()).getStringValue());
                }
//              update the enable status
                setEnabledComponents(getModel().isEnabled());
            }
        });
    }

    /** Returns the {@link DataColumnSpec} of the currently selected item.
     * This method delegates to
     *{@link ColumnSelectionPanel#getSelectedColumnAsSpec()}.
     * @return The currently selected item as {@link DataColumnSpec} or null
     * if none is selected (the list is empty) or the RowID should be used
     * (check return value of the useRowID method).
     * @see #useRowID
     */
    public final DataColumnSpec getSelectedAsSpec() {
        return m_chooser.getSelectedColumnAsSpec();
    }

    /** Returns the name of the currently selected item. This method delegates
     * to {@link ColumnSelectionPanel#getSelectedColumn()}.
     * @return The name of the currently selected item or null if none is
     * selected (the list is empty) or the RowID should be used
     * (check return value of the useRowID method).
     * @see #useRowID
     */
    public final String getSelected() {
        return m_chooser.getSelectedColumn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        String classCol = ((SettingsModelString)getModel()).getStringValue();
        if ((classCol == null) || (classCol.length() == 0)) {
            classCol = "** Unknown column **";
        }
        try {
            m_chooser.update((DataTableSpec)getLastTableSpec(m_specIndex), classCol);
            if (getModel() instanceof SettingsModelColumnName
                    && ((SettingsModelColumnName)getModel()).useRowID()) {
                m_chooser.setRowIDSelected();
            }
        } catch (final NotConfigurableException e1) {
            // we check the correctness of the table spec before, so
            // this exception shouldn't fly.
            assert false;
        }

        // update the enable status
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * Transfers the selected value from the component into the settings model.
     */
    private void updateModel() {
        if (getModel() instanceof SettingsModelColumnName) {
            ((SettingsModelColumnName)getModel()).setSelection(
                    m_chooser.getSelectedColumn(), m_chooser.rowIDSelected());
        } else {
	        ((SettingsModelString)getModel()).setStringValue(m_chooser
                .getSelectedColumn());
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        /*
         * this is a bit of code duplication: if the selection panel is set to
         * require at least one selectable column in the specs, it will fail
         * during update if no such column is present. We check this here, to
         * avoid loading if no column is selectable, so that the update with a
         * new value (following this method call) will not fail.
         */
        if ((specs == null) || (specs.length <= m_specIndex)) {
            throw new NotConfigurableException("Need input table spec to "
                    + "configure dialog. Configure or execute predecessor "
                    + "nodes.");
        }
        DataTableSpec spec;
        try {
            spec = (DataTableSpec)specs[m_specIndex];
        } catch (ClassCastException cce) {
            throw new NotConfigurableException("Wrong type of PortObject for"
                    + " ColumnNameSelectio, expecting DataTableSpec!");
        }
        if (spec == null) {
            throw new NotConfigurableException("Need input table spec to "
                    + "configure dialog. Configure or execute predecessor "
                    + "nodes.");
        }
        //if it's not required we don't need to check if at least one column
        //matches the criteria
        if (!m_isRequired) {
            return;
        }
        // now check if at least one column is compatible to the column filter
        for (final DataColumnSpec col : spec) {
            if (m_columnFilter.includeColumn(col)) {
                // we found one column we are compatible to - cool!
                return;
            }
        }
        //no column compatible to the current filter
        throw new NotConfigurableException(m_columnFilter.allFilteredMsg());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        // just in case we didn't get notified about the last selection ...
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_chooser.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_chooser.setToolTipText(text);
        m_label.setToolTipText(text);
    }

}
