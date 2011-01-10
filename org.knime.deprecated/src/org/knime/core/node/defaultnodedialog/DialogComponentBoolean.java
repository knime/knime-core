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
 *   21.09.2005 (mb): created
 *   2006-05-24 (tm): reviewed
 */
package org.knime.core.node.defaultnodedialog;

import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Provide a standard component for a dialog that allows to edit a boolean
 * value. Provides label and checkbox as well as functionality to load/store
 * into config object.
 * 
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 * @author M. Berthold, University of Konstanz
 */
public final class DialogComponentBoolean extends DialogComponent {
    private final JCheckBox m_checkbox;
    private final String m_configName;
    private final boolean m_dftValue;

    /**
     * Constructor puts label and checkbox into panel.
     * 
     * @param configName name used in configuration file
     * @param label label for dialog in front of checkbox
     * @param isSelected default value for checkbox
     */
    public DialogComponentBoolean(final String configName, final String label,
            final boolean isSelected) {
        this.add(new JLabel(label));
        m_checkbox = new JCheckBox();
        m_checkbox.setSelected(isSelected);
        this.add(m_checkbox);
        m_configName = configName;
        m_dftValue = isSelected;
    }
    
    /**
     * Read value for this dialog component from configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) {
        boolean newBool = settings.getBoolean(m_configName, m_dftValue);
        m_checkbox.setSelected(newBool);
    }

    /**
     * Write settings of this dialog component into the configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to write into
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(m_configName, m_checkbox.getModel().isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabledComponents(final boolean enabled) {
        m_checkbox.setEnabled(enabled);
    }
    
    /**
     * Adds the listener to the underlying checkbox component.
     * 
     * @param l the listener to add
     */
    public void addItemListener(final ItemListener l) {
        m_checkbox.addItemListener(l);
    }
    
    /**
     * Removes the listener from the underlying checkbox component.
     * @param l the listener to remove
     */
    public void removeItemListener(final ItemListener l) {
        m_checkbox.removeItemListener(l);
    }
    
    /**
     * Returns if the checkbox is selected.
     * 
     * @return <code>true</code> if the checkbox is selected, <code>false</code>
     * otherwise
     */
    public boolean isSelected() {
        return m_checkbox.isSelected();
    }

    /**
     * Set the selection state of the checkbox.
     * 
     * @param select <code>true</code> or <code>false</code>
     */
    public void setSelected(final boolean select) {
        m_checkbox.setSelected(select);
    }
}
