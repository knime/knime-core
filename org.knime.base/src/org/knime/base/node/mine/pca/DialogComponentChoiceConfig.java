/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 *   Sep 8, 2009 (uwe): created
 */
package org.knime.base.node.mine.pca;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

public class DialogComponentChoiceConfig extends DialogComponent {

    private final JSpinner m_dimSpinner;

    private final JSpinner m_qualitySlider;

    private final JRadioButton m_dimensionSelection;

    private final JRadioButton m_qualitySelection;

    /**
     * @param nodeDialog TODO
     * @param model
     */
    public DialogComponentChoiceConfig(final SettingsModelPCADimensions model) {
        super(model);

        final JPanel panel = getComponentPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Target dimensions"));
        panel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        m_dimensionSelection = new JRadioButton("dimensions to reduce to");
        panel.add(m_dimensionSelection, gbc);
        gbc.gridx++;
        m_dimSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));

        m_dimSpinner.setEditor(new JSpinner.NumberEditor(m_dimSpinner));
        panel.add(m_dimSpinner, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        m_qualitySelection =
                new JRadioButton("minimum information fraction to preserve (%)");
        panel.add(m_qualitySelection, gbc);
        gbc.gridx++;
        m_qualitySlider = new JSpinner(new SpinnerNumberModel(100, 1, 100, 1));

        setSliderLabels();
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(m_qualitySlider, gbc);
        gbc.anchor = GridBagConstraints.EAST;
        final ButtonGroup bg = new ButtonGroup();
        bg.add(m_dimensionSelection);
        bg.add(m_qualitySelection);
        m_dimensionSelection.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                m_dimSpinner.setEnabled(m_dimensionSelection.isSelected());
            }

        });
        m_qualitySelection.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                m_qualitySlider.setEnabled(m_qualitySelection.isSelected());
            }

        });
        m_qualitySelection.setSelected(true);
        m_dimSpinner.setEnabled(false);
        final ChangeListener cl = new ChangeListener() {

            public void stateChanged(final ChangeEvent arg0) {
                ((SettingsModelPCADimensions)getModel()).setValues(
                        (Integer)m_qualitySlider.getValue(),
                        (Integer)m_dimSpinner.getValue(), m_dimensionSelection
                                .isSelected());
            }

        };
        m_qualitySelection.addChangeListener(cl);
        m_dimSpinner.addChangeListener(cl);
        m_qualitySlider.addChangeListener(cl);
    }

    /**
     * 
     */
    private void setSliderLabels() {

        ((SettingsModelPCADimensions)getModel())
                .configureQualitySlider(m_qualitySlider);
        m_qualitySlider.repaint();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // always
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_dimSpinner.setEnabled(enabled && m_dimensionSelection.isSelected());
        m_qualitySlider.setEnabled(enabled && m_qualitySelection.isSelected());
        m_dimensionSelection.setEnabled(enabled);
        m_qualitySelection.setEnabled(enabled);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_dimSpinner.setToolTipText(text);
        m_qualitySlider.setToolTipText(text);
        // getPanel().setToolTipText(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        setSliderLabels();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        // validated

    }

}