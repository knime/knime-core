/* 
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
 *   2006-05-26 (tm): reviewed
 */
package de.unikn.knime.core.node.defaultnodedialog;

import javax.swing.JLabel;
import javax.swing.border.Border;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataValue;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NotConfigurableException;
import de.unikn.knime.core.node.util.ColumnSelectionPanel;

/**
 * Provides a standard component for a dialog that allows to select a column in
 * a given {@link de.unikn.knime.core.data.DataTableSpec}. Provides label and
 * list (possibly filtered by a given {@link de.unikn.knime.core.data.DataCell}
 * type) as well as functionality to load/store into config object.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentColumnSelection extends DialogComponent {
    /** Contains all column names matching the given given filter class. */
    private final ColumnSelectionPanel m_chooser;

    private final String m_configName;

    private final int m_specIndex;
    
    private final boolean m_isRequired;

    /**
     * Constructor that puts label and checkbox into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param classFilter which classes are available for selection
     */
    public DialogComponentColumnSelection(final String configName,
            final String label, final int specIndex,
            final Class<? extends DataValue>... classFilter) {
        this.add(new JLabel(label));
        m_chooser = new ColumnSelectionPanel((Border)null, classFilter);
        this.add(m_chooser);
        m_configName = configName;
        m_specIndex = specIndex;
        m_isRequired = true;
    }
    
    
    /**
     * Constructor that puts label and checkbox into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of checkbox
     * @param specIndex index of (input) port listing available columns
     * @param isRequired True, if the component should throw an exception in 
     * case of no available compatible type, false otherwise.
     * @param classFilter which classes are available for selection
     */
    public DialogComponentColumnSelection(final String configName,
            final String label, final int specIndex, final boolean isRequired,
            final Class<? extends DataValue>... classFilter) {
        this.add(new JLabel(label));
        m_chooser = new ColumnSelectionPanel((Border)null, classFilter);
        this.add(m_chooser);
        m_configName = configName;
        m_specIndex = specIndex;
        m_isRequired = isRequired;
    }

    /**
     * Reads values for this dialog component from configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     * @throws InvalidSettingsException if the settings could not be read
     * @throws NotConfigurableException If the spec does not contain at least
     * one column which is compatible to the value list as given in the 
     * constructor.
     */
    @Override
    public void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) 
        throws InvalidSettingsException, NotConfigurableException {
        String classCol = "** Unknown column **";
        try {
            classCol = settings.getString(m_configName);
        } finally {
            // update JComboBox with list of column names
            DataTableSpec spec = specs[m_specIndex];
            m_chooser.update(spec, classCol, m_isRequired);
        }
    }

    /**
     * Writes settings of this dialog component into the configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to write into
     */
    @Override
    public void saveSettingsTo(final NodeSettings settings) {
        String classCol = m_chooser.getSelectedColumn();
        settings.addString(m_configName, classCol);
    }

    /**
     * @see de.unikn.knime.core.node.defaultnodedialog.DialogComponent
     *      #setEnabledComponents(boolean)
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_chooser.setEnabled(enabled);
    }
}
