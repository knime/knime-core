/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
