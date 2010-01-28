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
 *   Sep 8, 2009 (uwe): created
 */
package org.knime.base.node.mine.pca;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        final ActionListener al = new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
            	model.setDimensionsSelected(m_dimensionSelection.isSelected());
                m_qualitySlider.setEnabled(m_qualitySelection.isSelected());
                if (showAdditionalInfo) {
                    m_dimensionLabel
                            .setEnabled(m_qualitySelection.isSelected());

                }
                m_dimSpinner.setEnabled(m_dimensionSelection.isSelected());
                if (showAdditionalInfo) {
                    m_qualityLabel
                            .setEnabled(m_dimensionSelection.isSelected());
                    final int dim =
                            m_dimensionSelection.isSelected() ? (Integer)m_dimSpinner
                                    .getValue()
                                    : ((SettingsModelPCADimensions)getModel())
                                            .getNeededDimensions(-1);

                    final String currentDimensions =
                            ((SettingsModelPCADimensions)getModel())
                                    .getInformationPreservation(dim);

                    m_qualityLabel.setText(" (" + currentDimensions + ")");
                }
            }

        };
        m_dimensionSelection.addActionListener(al);
        m_qualitySelection.addActionListener(al);
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
        // m_qualitySelection.addActionListener(al)(cl);
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
