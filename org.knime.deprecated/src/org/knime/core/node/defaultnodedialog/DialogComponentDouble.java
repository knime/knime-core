/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
