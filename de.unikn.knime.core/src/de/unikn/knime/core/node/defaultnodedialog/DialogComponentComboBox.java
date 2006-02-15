/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2004
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
 *   17.11.2005 (gdf): created
 */
package de.unikn.knime.core.node.defaultnodedialog;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/**
 * Provide a standard component for a dialog that allows to select among a list
 * of option provided as an ArrayList of String elements.
 * 
 * @author Giuseppe Di Fatta, University of Konstanz and ICAR-CNR
 * 
 */
public class DialogComponentComboBox extends DialogComponent {
    // private final static NodeLogger LOGGER =
    // NodeLogger.getLogger(DialogComponentComboBox.class);

    private JComboBox m_combobox;

    private String m_configName;

    /**
     * Constructor put label and combobox into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of combobox
     * @param list list of items (String) for the combobox
     */
    public DialogComponentComboBox(final String configName, final String label,
            final List<String> list) {
        this.add(new JLabel(label));
        m_combobox = new JComboBox();

        Iterator it = list.iterator();
        while (it.hasNext()) {
            String s = (String)it.next();
            m_combobox.addItem(s);
        }

        this.add(m_combobox);
        m_configName = configName;
    }

    /**
     * Constructor put label and combobox into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of combobox
     * @param list list of items (String) for the combobox
     */
    public DialogComponentComboBox(final String configName, final String label,
            final String... list) {
        this(configName, label, Arrays.asList(list));
    }

    /**
     * Read value for this dialog component from configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The items of the combobox
     * @throws InvalidSettingsException if load fails.
     */
    public void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) throws InvalidSettingsException {
        assert (settings != null);
        String selection = null;
        selection = settings.getString(m_configName);

        if (selection != null) {
            // LOGGER.debug("<loadSettingsFrom>" + m_configName
            // + ": " + selection);
            m_combobox.setSelectedItem(selection);
            // LOGGER.debug("<loadSettingsFrom>"
            // + "now the selected item is: "
            // + m_combobox.getSelectedItem().toString());
        }
    }

    /**
     * write settings of this dialog component into the configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to write into.
     */
    public void saveSettingsTo(final NodeSettings settings) {
        settings.addString(m_configName, m_combobox.getSelectedItem()
                .toString());
        // LOGGER.debug("<saveSettingsTo>" + m_configName
        // + ": " + m_combobox.getSelectedItem().toString());
    }

    /**
     * @see de.unikn.knime.core.node.defaultnodedialog.DialogComponent
     *      #setEnabledComponents(boolean)
     */
    @Override
    public void setEnabledComponents(final boolean enabled) {
        m_combobox.setEnabled(enabled);
    }
}
