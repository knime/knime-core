/* Created on Jun 12, 2006 11:03:30 AM by thor
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
 */
package org.knime.base.node.meta.xvalidation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * This is the simple dialog for the cross validation node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidateDialog extends NodeDialogPane {
    private final XValidateSettings m_settings = new XValidateSettings();
    private final JSpinner m_validations = new JSpinner(
            new SpinnerNumberModel(10, 2, 100, 1));
    private final JCheckBox m_randomSampling = new JCheckBox();
    @SuppressWarnings("unchecked")
    private final JCheckBox m_leaveOneOut = new JCheckBox();

    /**
     * Creates a new dialog for the cross validation settings.
     */
    public XValidateDialog() {
        super();

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 1, 2, 1);
        p.add(new JLabel("Number of validations   "), c);
        c.gridx = 1;
        p.add(m_validations, c);

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Random sampling   "), c);
        c.gridx = 1;
        p.add(m_randomSampling, c);


        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Leave-one-out   "), c);
        c.gridx = 1;
        p.add(m_leaveOneOut, c);
        m_leaveOneOut.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_validations.setEnabled(!m_leaveOneOut.isSelected());
                m_randomSampling.setEnabled(!m_leaveOneOut.isSelected());
            }
        });

        p.setSize(400, 90);
        addTab("Standard settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            m_settings.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // ignore it and use default values
        }

        m_validations.setValue(m_settings.validations());
        m_randomSampling.setSelected(m_settings.randomSampling());
        m_leaveOneOut.setSelected(m_settings.leaveOneOut());
        m_validations.setEnabled(!m_settings.leaveOneOut());
        m_randomSampling.setEnabled(!m_settings.leaveOneOut());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.validations((byte) Math.min(100,
                ((Number) m_validations.getValue()).intValue()));
        m_settings.randomSampling(m_randomSampling.isSelected());
        m_settings.leaveOneOut(m_leaveOneOut.isSelected());
        m_settings.saveSettingsTo(settings);
    }
}
