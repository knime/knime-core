/* Created on Jun 12, 2006 11:03:30 AM by thor
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
 */
package org.knime.base.node.meta.xvalidation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This is the simple dialog for the cross validation node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidateDialog extends NodeDialogPane {
    private final XValidateSettings m_settings = new XValidateSettings();

    private final JSpinner m_validations =
            new JSpinner(new SpinnerNumberModel(10, 2, 100, 1));

    private final JRadioButton m_linearSampling = new JRadioButton();

    private final JRadioButton m_randomSampling = new JRadioButton();

    private final JRadioButton m_leaveOneOut = new JRadioButton();

    private final JRadioButton m_stratifiedSampling = new JRadioButton();

    private final ColumnSelectionComboxBox m_classColumn =
            new ColumnSelectionComboxBox((Border)null, DataValue.class);

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
        p.add(new JLabel("Linear sampling   "), c);
        c.gridx = 1;
        p.add(m_linearSampling, c);

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Random sampling   "), c);
        c.gridx = 1;
        p.add(m_randomSampling, c);

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Stratified sampling   "), c);
        c.gridx = 1;
        p.add(m_stratifiedSampling, c);
        m_stratifiedSampling.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_classColumn.setEnabled(m_stratifiedSampling.isSelected());
            }
        });

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("   Class column   "), c);
        c.gridx = 1;
        p.add(m_classColumn, c);

        c.gridy++;
        c.gridx = 0;
        p.add(new JLabel("Leave-one-out   "), c);
        c.gridx = 1;
        p.add(m_leaveOneOut, c);
        m_leaveOneOut.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_validations.setEnabled(!m_leaveOneOut.isSelected());
            }
        });

        ButtonGroup bg = new ButtonGroup();
        bg.add(m_linearSampling);
        bg.add(m_randomSampling);
        bg.add(m_stratifiedSampling);
        bg.add(m_leaveOneOut);

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
        if (m_settings.randomSampling()) {
            m_randomSampling.setSelected(true);
        } else if (m_settings.stratifiedSampling()) {
            m_stratifiedSampling.setSelected(true);
        } else if (m_settings.leaveOneOut()) {
            m_leaveOneOut.setSelected(true);
            m_validations.setEnabled(false);
        } else {
            m_linearSampling.setSelected(true);
        }

        m_classColumn.update(specs[0], m_settings.classColumn());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.validations((byte)Math.min(100, ((Number)m_validations
                .getValue()).intValue()));
        m_settings.randomSampling(m_randomSampling.isSelected());
        m_settings.stratifiedSampling(m_stratifiedSampling.isSelected());
        m_settings.leaveOneOut(m_leaveOneOut.isSelected());
        m_settings.classColumn(m_classColumn.getSelectedColumn());
        m_settings.saveSettingsTo(settings);
    }
}
