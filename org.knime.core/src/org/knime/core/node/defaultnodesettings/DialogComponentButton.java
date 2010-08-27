/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   23.07.2007 (thiel): created
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Provides a component containing a button. The button's text can be set 
 * individually and an <code>ActionListener</code> can be added, to respond to 
 * action events. A model is not needed, since no input component like a 
 * text field etc. is provided here. Thus no setting values will be saved.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class DialogComponentButton extends DialogComponent {

    private JButton m_button;
    
    /**
     * Creates new instance of <code>DialogComponentButton</code> with given
     * text to set as button label.
     * 
     * @param label The button label.
     */
    public DialogComponentButton(final String label) { 
        super(new EmptySettingsModel());
        
        getComponentPanel().setLayout(new FlowLayout());
        m_button = new JButton(label);
        getComponentPanel().add(m_button);
        
    }
    
    /**
     * Sets the given text.
     * 
     * @param text The text to set.
     */
    public void setText(final String text) {
        m_button.setText(text);
    }
    
    /**
     * Adds the given listener to the button.
     * 
     * @param listener The listener to add.
     */
    public void addActionListener(final ActionListener listener) {
        m_button.addActionListener(listener);
    }
    
    /**
     * @param listener The listener to remove.
     */
    public void removeActionListener(final ActionListener listener) {
        m_button.removeActionListener(listener);
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_button.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_button.setToolTipText(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        // Nothing to do ...
    }
}
