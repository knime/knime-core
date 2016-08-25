/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Aug 23, 2016 (oole): created
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * StandardDialogComponent allowing the input of a user specified duration in days, hours, minutes, seconds
 *
 * @author Ole Ostergaard, KNIME.com GmbH
 * @since 3.3
 */
public final class DialogComponentDuration extends DialogComponent {


    private final JFormattedTextField m_durationField;

    /**
     * Creates a panel including the field for the duration. Optionally a label can be provided to be included
     * in the then created border
     *
     * @param model model to store the input duration values
     * @param label to place on the dialog or <code>null</code> if no border and label is wanted
     */
    public DialogComponentDuration(final SettingsModelDuration model, final String label) {
        super(model);
        m_durationField = new JFormattedTextField(model.getMask());
        Dimension size = new Dimension(150, 25);
        m_durationField.setPreferredSize(size);
        m_durationField.setHorizontalAlignment(SwingConstants.RIGHT);
        m_durationField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                    updateModel();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                    updateModel();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                    updateModel();
            }
        });

        // set panel
        JPanel panel = new JPanel(new GridBagLayout());
        if (label != null) {
            panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), label));
        } else {
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Duration: "), gbc);
        gbc.gridx++;
        panel.add(m_durationField, gbc);

        getComponentPanel().add(panel);
    }

    /**
     * Update the model
     */
    private void updateModel() {
        // we transfer the value from the field into the model
        ((SettingsModelDuration)getModel())
                .setDurationString(m_durationField.getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final SettingsModelDuration model = (SettingsModelDuration)getModel();
        setEnabledComponents(model.isEnabled());
        m_durationField.setText(model.getDurationString());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_durationField.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_durationField.setToolTipText(text);
    }
}
