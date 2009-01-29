/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   17.11.2005 (gdf): created
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodedialog;

import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Provide a standard component for a dialog that allows to select among a list
 * of options provided as a list of String elements.
 * 
 * @author Giuseppe Di Fatta, University of Konstanz
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * 
 */
public class DialogComponentComboBox extends DialogComponent {
    // private final static NodeLogger LOGGER =
    // NodeLogger.getLogger(DialogComponentComboBox.class);

    private JComboBox m_combobox;

    private String m_configName;

    /**
     * Constructor that puts label and combobox into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of combobox
     * @param list list of items for the combobox
     */
    public DialogComponentComboBox(final String configName, final String label,
            final List<String> list) {
        this.add(new JLabel(label));
        m_combobox = new JComboBox();

        for (String s : list) {
            m_combobox.addItem(s);
        }

        this.add(m_combobox);
        m_configName = configName;
    }

    /**
     * Constructor that puts label and combobox into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of combobox
     * @param list list of items for the combobox
     */
    public DialogComponentComboBox(final String configName, final String label,
            final String... list) {
        this(configName, label, Arrays.asList(list));
    }

    /**
     * Read value for this dialog component from configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the items of the combobox
     * @throws InvalidSettingsException if the settings could not be read
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws InvalidSettingsException {
        String selection = null;
        selection = settings.getString(m_configName);
        if (selection != null) {
            m_combobox.setSelectedItem(selection);
        }
    }

    /**
     * Writes settings of this dialog component into the configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to write into
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(m_configName, m_combobox.getSelectedItem()
                .toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabledComponents(final boolean enabled) {
        m_combobox.setEnabled(enabled);
    }
}
