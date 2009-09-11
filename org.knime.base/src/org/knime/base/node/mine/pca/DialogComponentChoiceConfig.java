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
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
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

/**
 * dialog component for dimension selection in PCA configuration, used for
 * {@link SettingsModelPCADimensions}
 * 
 * @author uwe, University of Konstanz
 */
public class DialogComponentChoiceConfig extends DialogComponent {
    /** spinner for setting dimensions. */
    private final JSpinner m_dimSpinner;

    /** spinner for setting preserved quality. */
    private final JSpinner m_qualitySlider;

    /** selection by dimension? */
    private final JRadioButton m_dimensionSelection;

    /** selection by preserved quality? */
    private final JRadioButton m_qualitySelection;

    private JLabel m_qualityLabel;

    private JLabel m_dimensionLabel;

    /**
     * @param model corresponding settings model
     * @param showAdditionalInfo if <code>true</code> additionally
     */
    public DialogComponentChoiceConfig(final SettingsModelPCADimensions model,
            final boolean showAdditionalInfo) {
        super(model);
        // TODO make label showing number of dimensions
        final JPanel panel = getComponentPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Target dimensions"));
        panel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        m_dimensionSelection = new JRadioButton("Dimensions to reduce to");
        panel.add(m_dimensionSelection, gbc);
        gbc.gridx++;
        m_dimSpinner =
                new JSpinner(new SpinnerNumberModel(2, 1, Integer.MAX_VALUE, 1));

        panel.add(m_dimSpinner, gbc);
        if (showAdditionalInfo) {
            m_qualityLabel =
                    new JLabel(" unknown fraction of information preserved");
            gbc.weightx = 1;
            gbc.gridx++;
            panel.add(m_qualityLabel, gbc);
            gbc.weightx = 0;
        }
        gbc.gridy++;
        gbc.gridx = 0;
        m_qualitySelection =
                new JRadioButton("Minimum information fraction "
                        + "to preserve (%)");
        panel.add(m_qualitySelection, gbc);
        gbc.gridx++;
        m_qualitySlider =
                new JSpinner(new SpinnerNumberModel(100d, 1d, 100d, 1d));

        setSliderLabels();
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(m_qualitySlider, gbc);
        if (showAdditionalInfo) {
            m_dimensionLabel = new JLabel(" all dimensions");
            gbc.gridx++;
            gbc.weightx = 1;
            panel.add(m_dimensionLabel, gbc);
            gbc.weightx = 0;
        }

        final ButtonGroup bg = new ButtonGroup();
        bg.add(m_dimensionSelection);
        bg.add(m_qualitySelection);
        m_dimensionSelection.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                m_dimSpinner.setEnabled(m_dimensionSelection.isSelected());
                if (showAdditionalInfo) {
                    m_qualityLabel
                            .setEnabled(m_dimensionSelection.isSelected());
                    final int dim =
                            ((SettingsModelPCADimensions)getModel())
                                    .getNeededDimensions(-1);
                    m_qualityLabel.setText(" ("
                            + ((SettingsModelPCADimensions)getModel())
                                    .getInformationPreservation(dim) + ")");
                }
            }

        });
        m_qualitySelection.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                m_qualitySlider.setEnabled(m_qualitySelection.isSelected());
                if (showAdditionalInfo) {
                    m_dimensionLabel
                            .setEnabled(m_qualitySelection.isSelected());

                }
            }

        });
        m_qualitySelection.setSelected(true);
        m_dimSpinner.setEnabled(false);
        final ChangeListener cl = new ChangeListener() {

            public void stateChanged(final ChangeEvent arg0) {
                final SettingsModelPCADimensions smodel =
                        (SettingsModelPCADimensions)getModel();
                smodel.setValues((Double)m_qualitySlider.getValue(),
                        (Integer)m_dimSpinner.getValue(), m_dimensionSelection
                                .isSelected());
                if (showAdditionalInfo) {
                    final int dim = smodel.getNeededDimensions(-1);
                    // if(dim<0 && model.getEigenval)
                    if (m_qualitySelection.isSelected()) {
                        if (smodel.getEigenvalues() != null
                                || m_qualitySlider.getValue().equals(100)) {
                            m_dimensionLabel.setText(" ("
                                    + (dim > 0 ? dim + "" : "all")
                                    + " dimensions)");
                        } else {
                            m_dimensionLabel.setText("");
                        }
                    }
                    if (m_dimensionSelection.isSelected()) {
                        m_qualityLabel.setText(" ("
                                + smodel.getInformationPreservation(dim) + ")");
                    }

                }
            }

        };
        m_qualitySelection.addChangeListener(cl);
        m_dimSpinner.addChangeListener(cl);
        m_qualitySlider.addChangeListener(cl);
        cl.stateChanged(null);
    }

    /**
     * 
     */
    private void setSliderLabels() {

        final SettingsModelPCADimensions model =
                (SettingsModelPCADimensions)getModel();
        // model.configureQualitySlider(m_qualitySlider);

        m_qualitySlider.repaint();
        if (model.getEigenvalues() != null) {
            model.setDimensions(Math.min(model.getEigenvalues().length, model
                    .getDimensions()));
            m_dimSpinner.setModel(new SpinnerNumberModel(model.getDimensions(),
                    1, model.getEigenvalues().length, 1));

        } else {
            m_dimSpinner.setModel(new SpinnerNumberModel(model.getDimensions(),
                    1, Integer.MAX_VALUE, 1));

        }
        ((JSpinner.NumberEditor)m_dimSpinner.getEditor()).getTextField()
                .setText("" + model.getDimensions());

        try {
            m_dimSpinner.commitEdit();
            // m_dimSpinner.repaint();
        } catch (final ParseException e) {
            // TODO Auto-generated catch block
        }
        if (m_qualityLabel != null) {
            m_qualityLabel.setText(" ("
                    + ((SettingsModelPCADimensions)getModel())
                            .getInformationPreservation(model.getDimensions())
                    + ")");
        }
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