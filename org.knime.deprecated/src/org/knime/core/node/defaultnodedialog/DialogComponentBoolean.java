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
 *   2006-05-24 (tm): reviewed
 */
package org.knime.core.node.defaultnodedialog;

import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Provide a standard component for a dialog that allows to edit a boolean
 * value. Provides label and checkbox as well as functionality to load/store
 * into config object.
 * 
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * @author M. Berthold, University of Konstanz
 */
public final class DialogComponentBoolean extends DialogComponent {
    private final JCheckBox m_checkbox;
    private final String m_configName;
    private final boolean m_dftValue;

    /**
     * Constructor puts label and checkbox into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of checkbox
     * @param isSelected default value for checkbox
     */
    public DialogComponentBoolean(final String configName, final String label,
            final boolean isSelected) {
        this.add(new JLabel(label));
        m_checkbox = new JCheckBox();
        m_checkbox.setSelected(isSelected);
        this.add(m_checkbox);
        m_configName = configName;
        m_dftValue = isSelected;
    }
    
    /**
     * Read value for this dialog component from configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        boolean newBool = settings.getBoolean(m_configName, m_dftValue);
        m_checkbox.setSelected(newBool);
    }

    /**
     * Write settings of this dialog component into the configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to write into
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(m_configName, m_checkbox.getModel().isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabledComponents(final boolean enabled) {
        m_checkbox.setEnabled(enabled);
    }
    
    /**
     * Adds the listener to the underlying checkbox component.
     * 
     * @param l the listener to add
     */
    public void addItemListener(final ItemListener l) {
        m_checkbox.addItemListener(l);
    }
    
    /**
     * Removes the listener from the underlying checkbox component.
     * @param l the listener to remove
     */
    public void removeItemListener(final ItemListener l) {
        m_checkbox.removeItemListener(l);
    }
    
    /**
     * Returns if the checkbox is selected.
     * 
     * @return <code>true</code> if the checkbox is selected, <code>false</code>
     * otherwise
     */
    public boolean isSelected() {
        return m_checkbox.isSelected();
    }

    /**
     * Set the selection state of the checkbox.
     * 
     * @param select <code>true</code> or <code>false</code>
     */
    public void setSelected(final boolean select) {
        m_checkbox.setSelected(select);
    }
}
