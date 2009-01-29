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
 *   21.09.2005 (mb): created
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodedialog;

import javax.swing.JLabel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;


/**
 * Provides a standard component for a dialog that allows to select a column in
 * a given {@link org.knime.core.data.DataTableSpec}. Provides label and
 * list (possibly filtered by a given {@link org.knime.core.data.DataCell}
 * type) as well as functionality to load/store into config object.
 * 
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentColumnSelection extends DialogComponent {
    /** Contains all column names matching the given given filter class. */
    private final ColumnSelectionPanel m_chooser;

    private final String m_configName;

    private final int m_specIndex;
    

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
        this(configName, label, specIndex, true, classFilter);
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
        m_chooser.setRequired(isRequired);
        this.add(m_chooser);
        m_configName = configName;
        m_specIndex = specIndex;
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
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) 
        throws InvalidSettingsException, NotConfigurableException {
        String classCol = "** Unknown column **";
        try {
            classCol = settings.getString(m_configName);
        } catch (Exception e) {
            // do nothing here, since its the dialog
            // catch it that the DefaultNodeDialogPane doesn't 
            // interrupt the for loop (loadSettings)
        } finally {
            // update JComboBox with list of column names
            DataTableSpec spec = specs[m_specIndex];
            m_chooser.update(spec, classCol);
        }
    }

    /**
     * Writes settings of this dialog component into the configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to write into
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        String classCol = m_chooser.getSelectedColumn();
        settings.addString(m_configName, classCol);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_chooser.setEnabled(enabled);
    }
}
