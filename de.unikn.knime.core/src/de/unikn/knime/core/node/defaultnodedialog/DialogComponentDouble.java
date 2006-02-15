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
 *   16.11.2005 (gdf): created
 */

package de.unikn.knime.core.node.defaultnodedialog;

import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/**
 * Provide a standard component for a dialog that allows to edit a double value.
 * Provides label and JFormattedTextField that checks ranges as well as
 * functionality to load/store into config object.
 * 
 * @author Giuseppe Di Fatta, University of Konstanz and ICAR-CNR
 * 
 */
public class DialogComponentDouble extends DialogComponent {

    private double m_dvalue = 0.0;

    private JFormattedTextField m_dvalueField;

    private NumberFormat m_dvalueFormat;

    private String m_configName;

    /**
     * Constructor put label and JFormattedTextField into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of JFormattedTextField
     * @param defaultValue initial value if no value is stored in the config
     */
    public DialogComponentDouble(final String configName, final String label,
            final double defaultValue) {

        this.add(new JLabel(label));
        m_dvalue = defaultValue;
        m_dvalueFormat = NumberFormat.getNumberInstance();
        m_dvalueField = new JFormattedTextField(m_dvalueFormat);
        m_dvalueField.setValue(new Double(m_dvalue));
        m_dvalueField.setColumns(6);
        // dvalueField.addPropertyChangeListener("value", this);

        this.add(m_dvalueField);
        m_configName = configName;
    }

    /**
     * Read value for this dialog component from configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     */
    public void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) {
        assert (settings != null);
        try {
            m_dvalue = settings.getDouble(m_configName);

        } catch (InvalidSettingsException ise) {
            // throw new InvalidSettingsException(ise + " Failed to read '"
            // + m_configName + "' value.");
        }
        m_dvalueField.setValue(new Double(m_dvalue));
    }

    /**
     * write settings of this dialog component into the configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to write into.
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    public void saveSettingsTo(final NodeSettings settings)
            throws InvalidSettingsException {
        try {
            m_dvalueField.commitEdit();
        } catch (ParseException e) {
            throw new InvalidSettingsException("Only doubles please");
        }
        double amount = ((Number)m_dvalueField.getValue()).doubleValue();
        m_dvalue = amount;
        settings.addDouble(m_configName, m_dvalue);
        // ((Double)dvalueField.getValue()).doubleValue());
    }

    /**
     * @see de.unikn.knime.core.node.defaultnodedialog.DialogComponent
     *      #setEnabledComponents(boolean)
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_dvalueField.setEnabled(enabled);
    }
}
