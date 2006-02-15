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
 * History
 *   21.09.2005 (mb): created
 */
package de.unikn.knime.core.node.defaultnodedialog;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/**
 * Provide a standard component for a dialog that allows to edit a boolean
 * value. Provides label and checkbox as well as functionality to load/store
 * into config object.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentBoolean extends DialogComponent {
    private JCheckBox m_checkbox;

    private String m_configName;

    /**
     * Constructor put label and checkbox into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of checkbox
     * @param isSelected default value for CheckBox.
     */
    public DialogComponentBoolean(final String configName, final String label,
            final boolean isSelected) {
        this.add(new JLabel(label));
        m_checkbox = new JCheckBox();
        m_checkbox.setSelected(isSelected);
        this.add(m_checkbox);
        m_configName = configName;
    }

    /**
     * Read value for this dialog component from configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws InvalidSettingsException if load fails.
     */
    public void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) throws InvalidSettingsException {
        assert (settings != null);
        try {
            boolean newBool = settings.getBoolean(m_configName);
            m_checkbox.setSelected(newBool);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException(ise + " Failed to read '"
                    + m_configName + "' value.");
        }
    }

    /**
     * write settings of this dialog component into the configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to write into.
     */
    public void saveSettingsTo(final NodeSettings settings) {
        settings.addBoolean(m_configName, m_checkbox.getModel().isSelected());
    }

    /**
     * @see de.unikn.knime.core.node.defaultnodedialog.DialogComponent
     *      #setEnabledComponents(boolean)
     */
    @Override
    public void setEnabledComponents(final boolean enabled) {
        m_checkbox.setEnabled(enabled);
    }
}
