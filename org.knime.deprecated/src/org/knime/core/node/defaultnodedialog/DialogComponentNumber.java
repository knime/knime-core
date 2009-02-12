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
 *   18.09.2005 (mb): created
 */
package org.knime.core.node.defaultnodedialog;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Provide a standard component for a dialog that allows to edit an integer
 * value. Provides label and spinner that checks ranges as well as functionality
 * to load/store into config object.
 * 
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentNumber extends DialogComponent {
    private JSpinner m_spinner;

    private String m_configName;
    
    /* final field to store the type, either int or double */
    private final Type m_type;
    
    /** possible types. */
    private enum Type {
        /** Int type. */
        INT,
        /** Double type. */
        DOUBLE
    }
    

    /**
     * Constructor put label and spinner into panel (int type).
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of spinner
     * @param minValue min value, and
     * @param maxValue max value for spinner
     * @param defaultValue initial value if no value is stored in the config
     */
    public DialogComponentNumber(final String configName, final String label,
            final int minValue, final int maxValue, final int defaultValue) {
        this.add(new JLabel(label));
        SpinnerNumberModel model = new SpinnerNumberModel(defaultValue,
                minValue, maxValue, 1);
        m_spinner = new JSpinner(model);
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)m_spinner
                .getEditor();
        editor.getTextField().setColumns(
                Integer.toString(maxValue).length() + 1);
        this.add(m_spinner);
        m_configName = configName;
        m_type = Type.INT;
    }
    
    
    
    /**
     * Constructor put label and spinner into panel (double type).
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of spinner
     * @param minValue min value, and
     * @param maxValue max value for spinner
     * @param defaultValue initial value if no value is stored in the config
     * @param stepSize the step size of the spinner 
     */
    public DialogComponentNumber(final String configName, final String label,
            final double minValue, final double maxValue, 
            final double defaultValue, final double stepSize) {
        this.add(new JLabel(label));
        SpinnerNumberModel model = new SpinnerNumberModel(defaultValue,
                minValue, maxValue, stepSize);
        m_spinner = new JSpinner(model);
        m_spinner.setEditor(new JSpinner.NumberEditor(m_spinner,
        "0.0################################################"));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)m_spinner
                .getEditor();
        editor.getTextField().setColumns(
                Double.toString(maxValue).length() + 1);

        this.add(m_spinner);
        m_configName = configName;
        m_type = Type.DOUBLE;
    }

    /**
     * Read value for this dialog component from configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws InvalidSettingsException If the settings could not be read.
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws InvalidSettingsException {
        assert (settings != null);
        if (m_type.equals(Type.INT)) {
            int newInt = settings.getInt(m_configName);
            m_spinner.setValue(newInt);
        } else if (m_type.equals(Type.DOUBLE)) {
            double newDouble = settings.getDouble(m_configName);
            m_spinner.setValue(newDouble);
        } else {
            assert false;
        }
    }

    /**
     * write settings of this dialog component into the configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to write into.
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_type.equals(Type.INT)) {
            settings.addInt(m_configName, ((Integer)m_spinner.getValue()));
        } else if (m_type.equals(Type.DOUBLE)) {
            settings.addDouble(m_configName, ((Double)m_spinner.getValue()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_spinner.setEnabled(enabled);
    }
}
