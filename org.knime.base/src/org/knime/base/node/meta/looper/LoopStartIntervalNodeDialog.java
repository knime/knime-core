/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   24.02.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * This is the dialog for the interval looper node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopStartIntervalNodeDialog extends NodeDialogPane {
    private final JTextField m_from = new JTextField(10);

    private final JTextField m_to = new JTextField(10);

    private final JTextField m_step = new JTextField(10);

    private final LoopStartIntervalSettings m_settings =
            new LoopStartIntervalSettings();

    /**
     * Creates a new dialog for the looper node.
     */
    public LoopStartIntervalNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHEAST;
        p.add(new JLabel("From   "), c);
        c.gridx = 1;
        p.add(m_from, c);

        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.NORTHEAST;
        p.add(new JLabel("To   "), c);
        c.gridx = 1;
        p.add(m_to, c);

        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.NORTHEAST;
        p.add(new JLabel("Step   "), c);
        c.gridx = 1;
        p.add(m_step, c);

        addTab("Standard settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsFrom(settings);
        m_from.setText(Double.toString(m_settings.from()));
        m_to.setText(Double.toString(m_settings.to()));
        m_step.setText(Double.toString(m_settings.step()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.from(Double.parseDouble(m_from.getText()));
        m_settings.to(Double.parseDouble(m_to.getText()));
        m_settings.step(Double.parseDouble(m_step.getText()));
        m_settings.saveSettingsTo(settings);
    }
}
