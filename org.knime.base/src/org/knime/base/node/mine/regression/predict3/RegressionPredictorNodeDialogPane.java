/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.predict3;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * Dialog for the regression predictor.
 *
 * @author Heiko Hofer
 * @since 3.5
 */
public final class RegressionPredictorNodeDialogPane
        extends NodeDialogPane {
    private final boolean m_showProbOptions;
    private JCheckBox m_includeProbs;
    private JCheckBox m_hasCustomPredictionName;
    private JTextField m_customPredictionName;
    private JTextField m_probColumnSuffix;

    /**
     * Create new dialog for linear regression model.
     * @param showProbabilityOptions flag that indicates whether options for probability columns (only relevant for logistic regression)
     * should be shown
     *
     */
    public RegressionPredictorNodeDialogPane(final boolean showProbabilityOptions) {
        super();
        m_showProbOptions = showProbabilityOptions;
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(5, 5, 0, 0);

        Insets leftInsets = new Insets(3, 8, 3, 8);
        Insets rightInsets = new Insets(3, 0, 3, 8);
        Insets leftCategoryInsets = new Insets(11, 8, 3, 8);
        Insets rightCategoryInsets = new Insets(11, 0, 3, 8);

        c.insets = leftCategoryInsets;
        JPanel predictionColumnPanel = createPredictionColumnPanel();
        predictionColumnPanel.setBorder(BorderFactory.createTitledBorder("Prediction column"));
        p.add(predictionColumnPanel, c);

        if (m_showProbOptions) {
            c.gridy++;
            JPanel probabilitesPanel = createProbabilitiesPanel();
            probabilitesPanel.setBorder(BorderFactory.createTitledBorder(
                    "Probability columns (only for nominal prediction, e.g. Logistic Regression)"));
            p.add(probabilitesPanel, c);
        }


        c.gridx = 0;
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        c.weighty = 1;
        p.add(new JPanel(), c);

        addTab("Settings", p);
    }


    private JPanel createPredictionColumnPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(5, 5, 0, 0);

        Insets leftInsets = new Insets(3, 8, 3, 8);
        Insets rightInsets = new Insets(3, 0, 3, 8);
        Insets leftCategoryInsets = new Insets(11, 8, 3, 8);
        Insets rightCategoryInsets = new Insets(11, 0, 3, 8);

        c.gridy++;
        c.insets = leftInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        m_hasCustomPredictionName = new JCheckBox("Custom prediction column name:");
        m_hasCustomPredictionName.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                updateEnableState();
            }
        });
        p.add(m_hasCustomPredictionName, c);

        c.gridx++;
        c.insets = rightInsets;
        c.weightx = 1;
        m_customPredictionName = new JTextField();
        p.add(m_customPredictionName, c);

        p.add(new JPanel(), c);
        return p;
    }

    private JPanel createProbabilitiesPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(5, 5, 0, 0);

        Insets leftInsets = new Insets(3, 8, 3, 8);
        Insets rightInsets = new Insets(3, 0, 3, 8);
        Insets leftCategoryInsets = new Insets(11, 8, 3, 8);
        Insets rightCategoryInsets = new Insets(11, 0, 3, 8);

        c.insets = leftCategoryInsets;
        c.gridwidth = 2;

        c.gridy++;
        c.insets = leftInsets;
        m_includeProbs = new JCheckBox("Append columns with predicted probabilities");
        m_includeProbs.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                updateEnableState();
            }
        });
        p.add(m_includeProbs , c);


        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Suffix for probability columns:"), c);

        c.gridx++;
        c.insets = rightInsets;
        c.weightx = 1;
        m_probColumnSuffix = new JTextField();
        p.add(m_probColumnSuffix, c);

        return p;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        RegressionPredictorSettings s = new RegressionPredictorSettings();
        s.loadSettingsForDialog(settings);

        m_hasCustomPredictionName.setSelected(s.getHasCustomPredictionName());
        PMMLPortObjectSpec portSpec = (PMMLPortObjectSpec)specs[0];
        DataTableSpec tableSpec = (DataTableSpec)specs[1];

        // check if the model has the correct target type
        try {
            RegressionPredictorNodeModel.checkModelTargetType(portSpec, m_showProbOptions);
        } catch (InvalidSettingsException e1) {
            throw new NotConfigurableException(e1.getMessage());
        }

        if (s.getCustomPredictionName() != null) {
            m_customPredictionName.setText(s.getCustomPredictionName());
        } else {
            try {
                DataColumnSpec[] outSpec = RegressionPredictorCellFactory.createColumnSpec(
                    portSpec, tableSpec, new RegressionPredictorSettings());
                m_customPredictionName.setText(outSpec[outSpec.length - 1].getName());
            } catch (InvalidSettingsException e) {
                // Open dialog and give a chance define settings
            }
        }


        m_includeProbs.setSelected(s.getIncludeProbabilities());
        m_probColumnSuffix.setText(s.getPropColumnSuffix());

        updateEnableState();

    }

    private void updateEnableState() {
        m_customPredictionName.setEnabled(m_hasCustomPredictionName.isSelected());
        m_probColumnSuffix.setEnabled(m_includeProbs.isSelected());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        RegressionPredictorSettings s = new RegressionPredictorSettings();
        s.setHasCustomPredictionName(m_hasCustomPredictionName.isSelected());
        s.setCustomPredictionName(m_customPredictionName.getText());
        s.setIncludeProbabilities(m_includeProbs.isSelected());
        s.setPropColumnSuffix(m_probColumnSuffix.getText());

        s.saveSettings(settings);
    }
}
