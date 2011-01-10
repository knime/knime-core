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
 *   16.11.2005 (gdf): created
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
 * Provide a standard component for a dialog that allows to edit a int value.
 * Provides label and JFormattedTextField that checks ranges as well as
 * functionality to load/store into config object.
 * 
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * @author Thomas Gabriel, University of Konstanz
 */
public class DialogComponentInteger extends DialogComponent {

    private final int m_dvalue;

    private final JFormattedTextField m_dvalueField;

    private final NumberFormat m_dvalueFormat;

    private final String m_configName;

    /**
     * Constructor put label and JFormattedTextField into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of JFormattedTextField
     * @param defaultValue initial value if no value is stored in the config
     */
    public DialogComponentInteger(final String configName, final String label,
            final int defaultValue) {
        this.add(new JLabel(label));
        m_dvalue = defaultValue;
        m_dvalueFormat = NumberFormat.getIntegerInstance();
        m_dvalueField = new JFormattedTextField(m_dvalueFormat);
        m_dvalueField.setValue(new Integer(m_dvalue));
        m_dvalueField.setColumns(6);
        this.add(m_dvalueField);
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
        int value = settings.getInt(m_configName, m_dvalue);
        m_dvalueField.setValue(new Integer(value));
    }

    /**
     * write settings of this dialog component into the configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to write into.
     * @throws InvalidSettingsException if the user has entered wrong values.
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        try {
            m_dvalueField.commitEdit();
        } catch (ParseException e) {
            throw new InvalidSettingsException("Only ints please");
        }
        int amount = ((Number)m_dvalueField.getValue()).intValue();
        settings.addInt(m_configName, amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabledComponents(final boolean enabled) {
        m_dvalueField.setEnabled(enabled);
    }
    
    /**
     * Sets the preferred size of the internal component.
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_dvalueField.setPreferredSize(new Dimension(width, height));
    }
}

