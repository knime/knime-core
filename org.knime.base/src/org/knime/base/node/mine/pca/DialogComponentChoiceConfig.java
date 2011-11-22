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
 * Implementation of a dialog component for dimension selection in PCA
 * configuration, used for {@link SettingsModelPCADimensions}.
 *
 * Selection of either a number of dimensions to reduce to or a minimal amount
 * of information to be preserved by the dimension reduction.
 *
 * @author Uwe Nagel, University of Konstanz
 */
public class DialogComponentChoiceConfig extends DialogComponent {

    /** spinner for setting dimensions. */
    private final JSpinner m_dimSpinner;

    /** spinner for setting preserved quality. */
    private final JSpinner m_qualitySpinner;

    /** selection by dimension? */
    private final JRadioButton m_dimensionSelection;

    /** selection by preserved quality? */
    private final JRadioButton m_qualitySelection;
    /**
     * show fraction of total information to be preserved after reduction,
     * corresponds to dimension selection.
     */
    private JLabel m_qualityLabel;
    /**
     * Show the number of dimensions needed to preserve chosen information
     * fraction, corresponds to minimum quality selection.
     */
    private JLabel m_dimensionLabel;

    /**
     * @param model
     *            corresponding settings model
     * @param showAdditionalInfo
     *            if <code>true</code> additional information about number of
     *            output dimensions and the fraction of information preserved is
     *            shown.
     */
    public DialogComponentChoiceConfig(final SettingsModelPCADimensions model,
            final boolean showAdditionalInfo) {
        super(model);

        final JPanel panel = getComponentPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Target dimensions"));
        panel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        // either choose number of dimensions to reduce to
        m_dimensionSelection = new JRadioButton("Dimensions to reduce to");
        m_dimensionSelection.setSelected(model.getDimensionsSelected());
        panel.add(m_dimensionSelection, gbc);
        gbc.gridx++;
        // dimension selection
        m_dimSpinner = new JSpinner(new SpinnerNumberModel(
                model.getDimensions(), 1, Integer.MAX_VALUE, 1));

        panel.add(m_dimSpinner, gbc);
        // label with additional information
        if (showAdditionalInfo) {
            m_qualityLabel = new JLabel(
            " unknown fraction of information preserved");
            gbc.weightx = 1;
            gbc.gridx++;
            panel.add(m_qualityLabel, gbc);
            gbc.weightx = 0;
        }
        gbc.gridy++;
        gbc.gridx = 0;
        // the part for minimum information fraction to be preserved
        m_qualitySelection = new JRadioButton("Minimum information fraction "
                + "to preserve (%)");
        m_qualitySelection.setSelected(!model.getDimensionsSelected());
        panel.add(m_qualitySelection, gbc);
        gbc.gridx++;
        m_qualitySpinner = new JSpinner(new SpinnerNumberModel(
                model.getMinQuality(), 1d, 100d, 1d));


        gbc.anchor = GridBagConstraints.WEST;
        panel.add(m_qualitySpinner, gbc);
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

            @Override
            public void actionPerformed(final ActionEvent e) {
                model.setDimensionsSelected(m_dimensionSelection.isSelected());
                updateComponent();
            }

        };
        m_dimensionSelection.addActionListener(al);
        m_qualitySelection.addActionListener(al);
        // m_qualitySelection.setSelected(true);
        // m_dimSpinner.setEnabled(false);
        final ChangeListener cl = new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent arg0) {
                final SettingsModelPCADimensions smodel =
                    (SettingsModelPCADimensions) getModel();
                smodel.setValues((Double) m_qualitySpinner.getValue(),
                        (Integer) m_dimSpinner.getValue(),
                        m_dimensionSelection.isSelected());
                updateComponent();
            }

        };
        m_dimSpinner.addChangeListener(cl);
        m_qualitySpinner.addChangeListener(cl);
        updateComponent();
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
        m_dimSpinner.setEnabled(enabled);
        m_qualitySpinner.setEnabled(enabled);
        m_dimensionSelection.setEnabled(enabled);
        m_qualitySelection.setEnabled(enabled);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_dimSpinner.setToolTipText(text);
        m_qualitySpinner.setToolTipText(text);
        // getPanel().setToolTipText(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final SettingsModelPCADimensions model =
            (SettingsModelPCADimensions) getModel();

        // model.configureQualitySlider(m_qualitySlider);
        final int dimensions = model.getDimensions();
        final boolean dimensionsSelected = model.getDimensionsSelected();
        final double quality = model.getMinQuality();
        // dimension parts
        m_dimensionSelection.setSelected(dimensionsSelected);
        m_dimSpinner.setEnabled(dimensionsSelected);
        m_dimSpinner.setValue(dimensions);

        if (m_qualityLabel != null) {
            m_qualityLabel.setEnabled(dimensionsSelected);
            m_qualityLabel.setText(" "
                    + model.getInformationPreservation(dimensions));
        }

        // min quality parts

        m_qualitySpinner.setEnabled(!dimensionsSelected);

        m_qualitySpinner.setValue(quality);
        if (m_dimensionLabel != null) {
            m_dimensionLabel.setText(" "
                    + model.getNeededDimensionDescription());
            m_dimensionLabel.setEnabled(!dimensionsSelected);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        // validated

    }

}
