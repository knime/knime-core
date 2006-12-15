/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   21.09.2005 (mb): created
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * Provides a standard component for a dialog that allows to select a column in
 * a given {@link org.knime.core.data.DataTableSpec}. Provides label and list
 * (possibly filtered by a given {@link org.knime.core.data.DataCell} type) as
 * well as functionality to load/store into config object.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentColumnNameSelection extends DialogComponent {

    /** Contains all column names matching the given given filter class. */
    private final ColumnSelectionPanel m_chooser;

    private final JLabel m_label;
    
    private final int m_specIndex;

    private final List<Class<? extends DataValue>> m_typeList;

    /**
     * Constructor that puts label and combobox into the panel. The dialog will
     * not open until the incoming table spec contains a column compatible to
     * one of the specified {@link DataValue} classes.
     * 
     * @param model the model holding the value of this component
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
     * @param model the model holding the value of this component
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param isRequired true, if the component should throw an exception in
     *            case of no available compatible column, false otherwise.
     * @param classFilter which classes are available for selection
     */
    public DialogComponentColumnNameSelection(final SettingsModelString model,
            final String label, final int specIndex, final boolean isRequired,
            final Class<? extends DataValue>... classFilter) {
        super(model);
        m_label = new JLabel(label);
        getComponentPanel().add(m_label);
        m_chooser = new ColumnSelectionPanel((Border)null, classFilter);
        m_chooser.setRequired(isRequired);
        getComponentPanel().add(m_chooser);

        m_specIndex = specIndex;
        m_typeList = Arrays.asList(classFilter);

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
                m_chooser.setSelectedColumn(((SettingsModelString)getModel())
                        .getStringValue());
            }
        });
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #updateComponent()
     */
    @Override
    void updateComponent() {
        String classCol = ((SettingsModelString)getModel()).getStringValue();
        if ((classCol == null) || (classCol.length() == 0)) {
            classCol = "** Unknown column **";
        }
        try {
            m_chooser.update(getLastTableSpec(m_specIndex), classCol);
        } catch (NotConfigurableException e1) {
            // we check the correctness of the table spec before, so
            // this exception shouldn't fly.
            assert false;
        }
    }

    /**
     * Transfers the selected value from the component into the settings model.
     */
    private void updateModel() {
        ((SettingsModelString)getModel()).setStringValue(m_chooser
                .getSelectedColumn());
    }

    /**
     * @see DialogComponent
     *      #checkConfigurabilityBeforeLoad(org.knime.core.data.DataTableSpec[])
     */
    @Override
    void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs)
            throws NotConfigurableException {
        /*
         * this is a bit of code duplication: if the selection panel is set to
         * require at least one selectable column in the specs, it will fail
         * during update if no such column is present. We check this here, to
         * avoid loading if no column is selectable, so that the update with a
         * new value (following this method call) will not fail.
         */
        if ((specs == null) || (specs.length < m_specIndex)) {
            throw new NotConfigurableException("Need input table spec to "
                    + "configure dialog. Configure or execute predecessor "
                    + "nodes.");
        }
        DataTableSpec spec = specs[m_specIndex];
        if (spec == null) {
            throw new NotConfigurableException("Need input table spec to "
                    + "configure dialog. Configure or execute predecessor "
                    + "nodes.");
        }
        // now check if at least one column is compatible to at least
        // one of the types we accept
        for (DataColumnSpec col : spec) {
            for (Class<? extends DataValue> t : m_typeList) {
                if (col.getType().isCompatible(t)) {
                    // we found one acceptable type we are compatible to - cool!
                    return;
                }
            }
        }

        String typeList = "";
        int count = 0;
        for (Class<? extends DataValue> t : m_typeList) {
            if (count == 3) {
                typeList += "...";
                break;
            }
            if (count > 0) {
                typeList += ", ";
            }
            typeList += t.getSimpleName();
            count++;
        }

        // here none of the columns are compatible to the acceptable types.
        throw new NotConfigurableException("The input table doesn't contain"
                + " a column with the expected type(s): " + typeList + ".");
    }

    /**
     * @see DialogComponent#validateStettingsBeforeSave()
     */
    @Override
    void validateStettingsBeforeSave() throws InvalidSettingsException {
        // just in case we didn't get notified about the last selection ...
        updateModel();
    }

    /**
     * @see DialogComponent#setEnabledComponents(boolean)
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_chooser.setEnabled(enabled);
    }

    /**
     * @see org.knime.core.node.defaultnodesettings.DialogComponent
     *      #setToolTipText(java.lang.String)
     */
    @Override
    public void setToolTipText(final String text) {
        m_chooser.setToolTipText(text);
        m_label.setToolTipText(text);
    }

}
