/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Jan 31, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.stats.outlier.handler;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.knime.base.algorithms.outlier.options.NumericOutliersDetectionOption;
import org.knime.base.algorithms.outlier.options.NumericOutliersReplacementStrategy;
import org.knime.base.algorithms.outlier.options.NumericOutliersTreatmentOption;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * The node dialog of the numeric outliers node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class NumericOutliersNodeDialogPane extends NodeDialogPane {

    /** Default insets to create space between panels. */
    private static final Insets INSET = new Insets(15, 0, 0, 0);

    /** The restrict outlier handling border label. */
    private static final String RESTRICTION_BORDER_TITLE = "Apply to";

    /** The algorithm selection border panel title. */
    private static final String COMPUTATION_BORDER_TITLE = "Quartile calculation";

    /** The estimation algorithm label. */
    private static final String ESTIMATION_LABEL = "Full data estimate using";

    /** The heuristic algorithm label. */
    private static final String HEURISTIC_LABEL = "Use heuristic (memory friendly)";

    /** The outlier selection border label. */
    private static final String OUTLIER_SELECTION_BORDER_TITLE = "Outlier Selection";

    /** The group selection border label. */
    private static final String GROUP_SELECTION_BORDER_TITLE = "Group Selection";

    /** The title of the outlier treatment panel. */
    private static final String TREATMENT_BORDER_TITLE = "Outlier Treatment";

    /** The memory policy border label. */
    private static final String MEMORY_BORDER_TITLE = "Memory Policy";

    /** The title of the general settings panel. */
    private static final String SETTINGS_BORDER_TITLE = "General Settings";

    /** The apply to groups checkbox name. */
    private static final String APPLY_TO_GROUPS = "Compute outlier statistics on groups";

    /** The groups tab name. */
    private static final String GROUPS_TAB = "Group Settings";

    /** The outlier tab name. */
    private static final String OUTLIER_TAB = "Outlier Settings";

    /** The IQR scalar label name. */
    private static final String QUARTILE_RANGE_MULT = "Interquartile range multiplier (k)";

    /** The treatment option label name . */
    private static final String TREATMENT_OPTION = "Treatment option";

    /** The outlier replacement strategy label name. */
    private static final String REPLACEMENT_STRATEGY = "Replacement strategy";

    /** The memory policy label name. */
    private static final String MEMORY_POLICY = "Process groups in memory";

    /** The domain policy label name. */
    private static final String DOMAIN_POLICY = "Update domain";

    /** Dialog indicating whether the algorithm should be executed in or out of memory. */
    private DialogComponentBoolean m_memoryDialog;

    /** Dialog holding information on how the quartiles are calculated if the algorithm is running in-memory. */
    private DialogComponentStringSelection m_estimationDialog;

    /** Dialog holding the factor to scale the IQR. */
    private DialogComponentNumberEdit m_scalarDialog;

    /** Dialog of the selected groups. */
    private DialogComponentColumnFilter2 m_groupsDialog;

    /** Dialog of the columns to check for outliers. */
    private DialogComponentColumnFilter2 m_outlierDialog;

    /** Dialog indicating whether the algorithm should create groups. */
    private DialogComponentBoolean m_useGroupsDialog;

    /** Dialog informing about the outlier replacement strategy. */
    private DialogComponentStringSelection m_outlierReplacementDialog;

    /** Dialog informing about the accuracy of the quartiles computation. */
    private DialogComponentButtonGroup m_heuristicDialog;

    /** Dialog indicating whether the algorithm should create groups. */
    private DialogComponentBoolean m_updateDomainDialog;

    /** Dialog informing about the treatment of the outliers. */
    private DialogComponentStringSelection m_outlierTreatmentDialog;

    /** Dialog informing about the outlier detection restrictions. */
    private DialogComponentStringSelection m_restrictionDialog;

    /** Array holding all dialog components (convenience). */
    private final DialogComponent[] m_diaComp;

    /** Inits the dialog. */
    NumericOutliersNodeDialogPane() {
        addTab(OUTLIER_TAB, createOutlierDialog());
        addTab(GROUPS_TAB, createGroupDialog());
        addListeners();

        m_diaComp = new DialogComponent[]{m_groupsDialog, m_outlierDialog, m_scalarDialog, m_estimationDialog,
            m_memoryDialog, m_useGroupsDialog, m_outlierTreatmentDialog, m_restrictionDialog,
            m_outlierReplacementDialog, m_heuristicDialog, m_updateDomainDialog};
    }

    /**
     * Create the outlier dialog.
     *
     * @return the outerlier dialog
     */
    private Component createOutlierDialog() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 2;

        // add panel border
        panel.setBorder(BorderFactory.createTitledBorder(OUTLIER_SELECTION_BORDER_TITLE));

        // add outlier selection panel
        panel.add(createOutlierSelectionDialog(), gbc);

        gbc.insets = INSET;
        ++gbc.gridy;
        gbc.weighty = 0;
        gbc.gridwidth = 1;

        // add settings panel
        panel.add(createSettingsDialog(), gbc);
        ++gbc.gridx;

        // add treatment panel
        panel.add(createTreatmentDialog(), gbc);

        return panel;
    }

    /**
     * Creates the outlier selection dialog.
     *
     * @return the outlier selection dialog
     */
    private JPanel createOutlierSelectionDialog() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        // add the outlier selection dialog
        m_outlierDialog = new DialogComponentColumnFilter2(NumericOutliersNodeModel.createOutlierFilterModel(), 0);
        panel.add(m_outlierDialog.getComponentPanel(), gbc);
        return panel;
    }

    /**
     * Creates the groups dialog.
     *
     * @return the group dialog
     */
    private JPanel createGroupDialog() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // add the group selection panel
        panel.add(getGroupSelectionPanel(), gbc);

        gbc.insets = INSET;
        gbc.weightx = 0;
        gbc.weighty = 0;
        ++gbc.gridy;

        // add the memory policy panel
        panel.add(getMemoryPanel(), gbc);

        // return the panel
        return panel;
    }

    /**
     * Creates the groups selection panel.
     *
     * @return the group selection dialog
     */
    private JPanel getGroupSelectionPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        // add panel border
        panel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), GROUP_SELECTION_BORDER_TITLE));

        // add apply to groups selection dialog
        m_useGroupsDialog =
            new DialogComponentBoolean(NumericOutliersNodeModel.createUseGroupsModel(), APPLY_TO_GROUPS);
        panel.add(m_useGroupsDialog.getComponentPanel(), gbc);

        gbc.insets = INSET;
        ++gbc.gridy;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        m_groupsDialog = new DialogComponentColumnFilter2(NumericOutliersNodeModel.createGroupFilterModel(), 0);
        panel.add(m_groupsDialog.getComponentPanel(), gbc);
        return panel;
    }

    private JPanel getMemoryPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;

        gbc.weightx = 1;

        // add panel border
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), MEMORY_BORDER_TITLE));

        // add memory policy dialog
        m_memoryDialog = new DialogComponentBoolean(NumericOutliersNodeModel.createMemoryModel(), MEMORY_POLICY);
        panel.add(m_memoryDialog.getComponentPanel(), gbc);

        return panel;
    }

    /**
     * Creates the settings panel.
     *
     * @return the settings panel
     */
    private JPanel createSettingsDialog() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;

        // add panel border
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), SETTINGS_BORDER_TITLE));

        // add IQR scale dialog
        m_scalarDialog =
            new DialogComponentNumberEdit(NumericOutliersNodeModel.createScalarModel(), QUARTILE_RANGE_MULT);
        panel.add(m_scalarDialog.getComponentPanel(), gbc);

        // add same space to the boarder title
        gbc.insets = new Insets(5, 0, 0, 0);
        ++gbc.gridy;

        // add the heuristic dialog
        panel.add(createComputationPanel(), gbc);

        // reduce space to the boarder title
        gbc.insets = new Insets(0, 0, 0, 0);
        ++gbc.gridy;

        // add the update domain dialog
        m_updateDomainDialog = new DialogComponentBoolean(NumericOutliersNodeModel.createDomainModel(), DOMAIN_POLICY);
        panel.add(m_updateDomainDialog.getComponentPanel(), gbc);

        return panel;
    }

    /**
     * Creates the computational panel holding the selection buttons for the computation and the estimation type panel.
     *
     * @return the computational panel
     */
    private Component createComputationPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;

        // add panel border
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), COMPUTATION_BORDER_TITLE));

        final SettingsModelString heuristicModel = NumericOutliersNodeModel.createHeuristicModel();
        m_heuristicDialog = new DialogComponentButtonGroup(heuristicModel, null, true,
            new ButtonGroupEnumInterface[]{getEnumInterface(heuristicModel, HEURISTIC_LABEL, null, true),
                getEnumInterface(heuristicModel, ESTIMATION_LABEL, null, false)});

        panel.add(m_heuristicDialog.getButton(String.valueOf(true)), gbc);

        ++gbc.gridy;

        panel.add(createEstimationPanel(m_heuristicDialog.getButton(String.valueOf(false))), gbc);

        return panel;

    }

    /**
     * Creates the estimation panel.
     *
     * @param button the button to the left
     * @return the estimation panel
     */
    private Component createEstimationPanel(final AbstractButton button) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;

        panel.add(button, gbc);

        ++gbc.gridx;

        // add a component to select the percentile estimation type (filter legacy option)
        List<String> eTypes = Arrays.stream(EstimationType.values())//
            .filter(t -> t != EstimationType.LEGACY)//
            .map(t -> t.name())//
            .collect(Collectors.toList());
        m_estimationDialog =
            new DialogComponentStringSelection(NumericOutliersNodeModel.createEstimationModel(), null, eTypes);

        panel.add(m_estimationDialog.getComponentPanel(), gbc);

        return panel;
    }

    /**
     * Creates an instance of the ButtonGroupEnumInterface from the provided parameters.
     *
     * @param model the settings model
     * @param text the label text
     * @param toolTip the tool tip
     * @param action the action (name)
     * @return a properly intialized instance of ButtonGroupEnumInterface
     */
    private ButtonGroupEnumInterface getEnumInterface(final SettingsModelString model, final String text,
        final String toolTip, final boolean action) {
        return new ButtonGroupEnumInterface() {

            @Override
            public boolean isDefault() {
                return model.getStringValue().equals(String.valueOf(action));
            }

            @Override
            public String getToolTip() {
                return toolTip;
            }

            @Override
            public String getText() {
                return text;
            }

            @Override
            public String getActionCommand() {
                return String.valueOf(action);
            }
        };
    }

    /**
     * Creates the treatment dialog.
     *
     * @return the treatment dialog
     */
    private JPanel createTreatmentDialog() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;

        // add panel border
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), TREATMENT_BORDER_TITLE));

        // create component to select the outlier treatment option
        m_outlierTreatmentDialog = new DialogComponentStringSelection(
            NumericOutliersNodeModel.createOutlierTreatmentModel(), TREATMENT_OPTION,
            Arrays.stream(NumericOutliersTreatmentOption.values()).map(val -> val.toString()).toArray(String[]::new));

        // create component to restrict the outlier treatment
        m_restrictionDialog = new DialogComponentStringSelection(NumericOutliersNodeModel.createOutlierDetectionModel(),
            RESTRICTION_BORDER_TITLE,
            Arrays.stream(NumericOutliersDetectionOption.values()).map(val -> val.toString()).toArray(String[]::new));

        // add component to select the outlier replacement strategy
        m_outlierReplacementDialog = new DialogComponentStringSelection(
            NumericOutliersNodeModel.createOutlierReplacementModel(), REPLACEMENT_STRATEGY, Arrays
                .stream(NumericOutliersReplacementStrategy.values()).map(val -> val.toString()).toArray(String[]::new));

        updatePrefSize(0, 10, m_outlierTreatmentDialog.getComponentPanel(), m_restrictionDialog.getComponentPanel(),
            m_outlierReplacementDialog.getComponentPanel());

        updatePrefSize(1, 0, m_outlierTreatmentDialog.getComponentPanel(), m_restrictionDialog.getComponentPanel(),
            m_outlierReplacementDialog.getComponentPanel());

        // add all dialogs
        panel.add(m_restrictionDialog.getComponentPanel(), gbc);

        ++gbc.gridy;

        panel.add(m_outlierTreatmentDialog.getComponentPanel(), gbc);

        ++gbc.gridy;

        panel.add(m_outlierReplacementDialog.getComponentPanel(), gbc);

        ++gbc.gridy;
        gbc.weightx = 1;
        gbc.weighty = 1;

        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

    /**
     * Ensures that all provided components have the same preferred dimensionality at the given index
     *
     * @param cInd the component index
     * @param offset the offset
     * @param components the components whose preferred dimensionaility needs to be updated
     */
    private void updatePrefSize(final int cInd, final int offset, final JPanel... components) {
        final Dimension prefDim = new Dimension(offset, 0);
        for (final JPanel comp : components) {
            prefDim.setSize(Math.max(prefDim.getWidth(), comp.getComponent(cInd).getMinimumSize().getWidth() + offset),
                0);
        }
        for (final JPanel comp : components) {
            Component compToRes = comp.getComponent(cInd);
            prefDim.setSize(prefDim.getWidth(), compToRes.getMinimumSize().getHeight());
            compToRes.setPreferredSize(prefDim);
        }
    }

    /**
     * Adds listeners to the different dialog components/ model settings.
     */
    private void addListeners() {
        // only enable estimation types if in-memory calculation is selected
        m_heuristicDialog.getModel().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                toggleEstimationDialog();
            }
        });

        // only enable groups dialog if apply to groups is selected
        m_useGroupsDialog.getModel().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                toggleGroupsDialog();
            }
        });

        m_outlierTreatmentDialog.getModel().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                toggleReplacement();
            }
        });
    }

    /**
     * Enables and disables the estimation dialog depending on the memory selection.
     */
    private void toggleEstimationDialog() {
        m_estimationDialog.getModel()
            .setEnabled(!Boolean.parseBoolean(((SettingsModelString)m_heuristicDialog.getModel()).getStringValue()));
    }

    /**
     * Enables and disables the groups dialog depending on the use groups selection.
     */
    private void toggleGroupsDialog() {
        m_groupsDialog.getModel().setEnabled(m_useGroupsDialog.isSelected());
        m_memoryDialog.getModel().setEnabled(m_useGroupsDialog.isSelected());
    }

    /**
     * Enables and disables the replacement dialog depending on the treatment choice.
     */
    private void toggleReplacement() {
        m_outlierReplacementDialog.getModel().setEnabled(((SettingsModelString)m_outlierTreatmentDialog.getModel())
            .getStringValue().equals(NumericOutliersTreatmentOption.REPLACE.toString()));
    }

    /**
     * Initially enables and disables dialogs.
     */
    private void toggleDialogs() {
        toggleEstimationDialog();
        toggleGroupsDialog();
        toggleReplacement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (final DialogComponent dia : m_diaComp) {
            dia.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        assert (settings != null && specs != null);
        for (final DialogComponent dia : m_diaComp) {
            dia.loadSettingsFrom(settings, specs);
        }
        toggleDialogs();

    }

}