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
 *   21.09.2005 (mb): created
 *   2006-05-24 (tm): reviewed
 *   25.09.2006 (ohl): using SettingsModel
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Provides a standard component for a dialog that allows to edit a boolean
 * value. Provides a checkbox as well as functionality to load/store the value
 * into a config object.
 *
 * @author M. Berthold, University of Konstanz
 */
public final class DialogComponentBoolean extends DialogComponent {
    private final JCheckBox m_checkbox;

    /**
     * Constructor puts a checkbox with the specified label into the panel.
     *
     * @param booleanModel an already created settings model
     * @param label the label for checkbox.
     */
    public DialogComponentBoolean(final SettingsModelBoolean booleanModel,
            final String label) {
        super(booleanModel);

        m_checkbox = new JCheckBox(label);
        m_checkbox.setSelected(booleanModel.getBooleanValue());
        // update the model, if the user changes the component
        m_checkbox.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final SettingsModelBoolean model =
                    (SettingsModelBoolean)getModel();
                model.setBooleanValue(m_checkbox.isSelected());
            }
        });

        // update the checkbox, whenever the model changes - make sure we get
        // notified first.
        getModel().prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
        getComponentPanel().add(m_checkbox);
        //call this method to be in sync with the settings model
        updateComponent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        // only update component if values are off
        final SettingsModelBoolean model = (SettingsModelBoolean)getModel();
        setEnabledComponents(model.isEnabled());
        if (model.getBooleanValue() != m_checkbox.isSelected()) {
            m_checkbox.setSelected(model.getBooleanValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        // nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // we're always good.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_checkbox.setEnabled(enabled);
    }

    /**
     * @return <code>true</code> if the checkbox is selected,
     *         <code>false</code> otherwise
     */
    public boolean isSelected() {
        return m_checkbox.isSelected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_checkbox.setToolTipText(text);
    }
}
