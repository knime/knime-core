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
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodedialog;

import java.awt.Dimension;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Provides a standard component for a dialog that allows to edit a double
 * value. Provides label and {@link javax.swing.JFormattedTextField} that checks
 * ranges as well as functionality to load/store into config object.
 * 
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * @author Giuseppe Di Fatta, University of Konstanz
 * 
 */
public class DialogComponentDouble extends DialogComponent {

    private final double m_dvalue;

    private final JFormattedTextField m_dvalueField;

    private final String m_configName;

    /**
     * Constructor that puts label and JFormattedTextField into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of JFormattedTextField
     * @param defaultValue initial value if no value is stored in the config
     */
    public DialogComponentDouble(final String configName, final String label,
            final double defaultValue) {
        this.add(new JLabel(label));
        m_dvalue = defaultValue;
        m_dvalueField =
            new JFormattedTextField(NumberFormat.getNumberInstance());
        m_dvalueField.setValue(m_dvalue);
        m_dvalueField.setColumns(6);
        this.add(m_dvalueField);
        m_configName = configName;
    }

    /**
     * Reads value for this dialog component from configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     */        
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
         double value = settings.getDouble(m_configName, m_dvalue);
         m_dvalueField.setValue(new Double(value));
    }

    /**
     * Writes settings of this dialog component into the configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to write into
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        try {
            m_dvalueField.commitEdit();
        } catch (ParseException e) {
           throw new InvalidSettingsException("Only double values are allowed");
        }
        double amount = ((Number)m_dvalueField.getValue()).doubleValue();
        settings.addDouble(m_configName, amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_dvalueField.setEnabled(enabled);
    }
    
    /**
     * Sets the preferred size of the internal component.
     * @param width the width
     * @param height the height
     */
    public void setSizeComponents(final int width, final int height) {
        m_dvalueField.setPreferredSize(new Dimension(width, height));
    }

}
