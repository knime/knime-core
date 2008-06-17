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
 *   16.11.2005 (gdf): created
 */

package org.knime.core.node.defaultnodedialog;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Provide a standard component for a dialog that allows to edit a text field.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * 
 */
public final class DialogComponentTextField extends DialogComponent {

    private final JTextField m_textField;

    private final String m_configName;
    
    private final String m_dftText;

    /**
     * Constructor put label and JTextField into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of JTextField
     * @param dftText initial value if no value is stored in the config
     */
    public DialogComponentTextField(
            final String configName, final String label, final String dftText) {
        this.add(new JLabel(label));
        m_dftText = dftText;
        m_textField = new JTextField(m_dftText);
        this.add(m_textField);
        m_configName = configName;
    }
    
    /**
     * Read value for this dialog component from configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        m_textField.setText(settings.getString(m_configName, m_dftText));
    }

    /**
     * write settings of this dialog component into the configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to write into.
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(m_configName, m_textField.getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabledComponents(final boolean enabled) {
        m_textField.setEnabled(enabled);
    }
    
    /**
     * Sets the preferred size of the internal component.
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_textField.setPreferredSize(new Dimension(width, height));
    }
    
}
