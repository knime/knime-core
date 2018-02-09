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
 *   Jan 31, 2018 (ortmann): created
 */
package org.knime.base.node.stats.outlier;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
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
import org.knime.core.node.port.PortObjectSpec;

/**
 * The node dialog of the outlier detector node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class OutlierDetectorNodeDialogPane extends NodeDialogPane {

    /** The tab label. */
    private static final String TAB_NAME = "Settings";

    /** The outlier settings border label. */
    private static final String OUTLIER_SETTINGS_BORDER = "Outlier settings";

    /** The group settings border label. */
    private static final String GROUP_SETTINGS_BORDER = "Group settings";

    /** The title of the outlier treatment panel. */
    private static final String TREATMENT_TITLE = "Outlier treatment";

    /** The title of the general settings panel. */
    private static final String SETTINGS_TITLE = "General settings";

    /** The apply to groups checkbox name. */
    private static final String APPLY_TO_GROUPS = "Compute outlier statistics on groups";

    /** The groups tab name. */
    private static final String GROUPS_TAB = "Groups";

    /** The columns tab name. */
    private static final String OUTLIER_TAB = "Outliers";

    /** The estimation type name. */
    private static final String ESTIMATION_TYPE = "Estimation type: ";

    /** The IQR scalar label name. */
    private static final String QUARTILE_RANGE_MULT = "IQR scalar";

    /** The memory policy label name. */
    private static final String MEMORY_POLICY = "Process in memory";

    /** The outlier replacement strategy label name. */
    private static final String REPLACEMENT_STRATEGY = "Replacement strategy";

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

    /** Dialog informing about the treatment of the outliers. */
    private DialogComponentButtonGroup m_outlierTreatmentDialog;

    /** Dialog informing about the outlier replacement strategy. */
    private DialogComponentStringSelection m_outlierReplacementDialog;

    /** Array holding all dialog components (convenience). */
    private final DialogComponent[] m_diaComp;

    /** Inits the dialog. */
    public OutlierDetectorNodeDialogPane() {
        addTab(TAB_NAME, getSettingsPanel());
        addListeners();

        m_diaComp = new DialogComponent[]{m_groupsDialog, m_outlierDialog, m_scalarDialog, m_estimationDialog,
            m_memoryDialog, m_useGroupsDialog, m_outlierTreatmentDialog, m_outlierReplacementDialog};
    }

    /**
     * Creates the settings tab.
     *
     * @return the settings tab
     */
    private Component getSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 2;

        // add tabbed pane for the outlier and group column selection
        final JTabbedPane tPane = new JTabbedPane();
        tPane.setBorder(BorderFactory.createTitledBorder(""));
        tPane.addTab(OUTLIER_TAB, createOutlierPanel());
        tPane.addTab(GROUPS_TAB, createGroupsPanel());
        panel.add(tPane, gbc);

        ++gbc.gridy;
        gbc.weighty = 0;
        gbc.gridwidth = 1;

        // add settings panel
        panel.add(createSettingsPanel(), gbc);
        ++gbc.gridx;

        // add treatment panel
        panel.add(createTreatmentPanel(), gbc);

        return panel;
    }

    /**
     * Creates the outlier selection dialog.
     *
     * @return the outlier selection dialog
     */
    private JPanel createOutlierPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        panel.setBorder(BorderFactory.createTitledBorder(OUTLIER_SETTINGS_BORDER));
        m_outlierDialog = new DialogComponentColumnFilter2(OutlierDetectorNodeModel.createOutlierFilterModel(), 0);
        panel.add(m_outlierDialog.getComponentPanel(), gbc);
        return panel;
    }

    /**
     * Creates the groups selection dialog.
     *
     * @return the group selection dialog
     */
    private JPanel createGroupsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        panel.setBorder(BorderFactory.createTitledBorder(GROUP_SETTINGS_BORDER));
        m_groupsDialog = new DialogComponentColumnFilter2(OutlierDetectorNodeModel.createGroupFilterModel(), 0);
        panel.add(m_groupsDialog.getComponentPanel(), gbc);
        return panel;
    }

    /**
     * Creates the settings panel.
     *
     * @return the settings panel
     */
    private JPanel createSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;

        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), SETTINGS_TITLE));

        // add apply to groups selection dialog
        m_useGroupsDialog =
            new DialogComponentBoolean(OutlierDetectorNodeModel.createUseGroupsModel(), APPLY_TO_GROUPS);
        panel.add(m_useGroupsDialog.getComponentPanel(), gbc);

        ++gbc.gridy;

        // add IQR scale dialog
        m_scalarDialog =
            new DialogComponentNumberEdit(OutlierDetectorNodeModel.createScalarModel(), QUARTILE_RANGE_MULT);
        panel.add(m_scalarDialog.getComponentPanel(), gbc);

        ++gbc.gridy;

        // add memory policy dialog
        m_memoryDialog = new DialogComponentBoolean(OutlierDetectorNodeModel.createMemoryModel(), MEMORY_POLICY);
        panel.add(m_memoryDialog.getComponentPanel(), gbc);

        ++gbc.gridy;

        // add a component to select the percentile estimation type
        List<String> eTypes =
            Arrays.stream(EstimationType.values()).map(val -> val.toString()).collect(Collectors.toList());
        m_estimationDialog = new DialogComponentStringSelection(OutlierDetectorNodeModel.createEstimationModel(),
            ESTIMATION_TYPE, eTypes);
        panel.add(m_estimationDialog.getComponentPanel(), gbc);

        return panel;
    }

    /**
     * @param panel
     * @param gbc
     */
    private JPanel createTreatmentPanel() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), TREATMENT_TITLE));

        // add component to select the outlier treatment option
        m_outlierTreatmentDialog =
            new DialogComponentButtonGroup(OutlierDetectorNodeModel.createOutlierTreatmentModel(), false, null,
                Arrays.stream(OutlierDetectorNodeModel.TREATMENT_OPTIONS.values()).map(val -> val.toString())
                    .toArray(String[]::new));
        panel.add(m_outlierTreatmentDialog.getComponentPanel(), gbc);

        ++gbc.gridy;

        // add component to select the outlier replacement strategy
        m_outlierReplacementDialog =
            new DialogComponentStringSelection(OutlierDetectorNodeModel.createOutlierReplacementModel(),
                REPLACEMENT_STRATEGY, Arrays.stream(OutlierDetectorNodeModel.REPLACEMENT_STRATEGY.values())
                    .map(val -> val.toString()).toArray(String[]::new));
        panel.add(m_outlierReplacementDialog.getComponentPanel(), gbc);
        gbc.weighty = 1;
        ++gbc.gridy;
        panel.add(Box.createGlue(), gbc);
        return panel;
    }

    /**
     * Adds listeners to the different dialog components/ model settings.
     */
    private void addListeners() {
        // only enable estimation types if in-memory calculation is selected
        m_memoryDialog.getModel().addChangeListener(new ChangeListener() {

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
     *
     */
    private void toggleReplacement() {
        m_outlierReplacementDialog.getModel().setEnabled(m_outlierTreatmentDialog
            .getButton(OutlierDetectorNodeModel.TREATMENT_OPTIONS.REPLACE.toString()).isSelected());
    }

    /**
     * Initially enables and disables.
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

    /**
     *
     */
    private void toggleEstimationDialog() {
        m_estimationDialog.getModel().setEnabled(m_memoryDialog.isSelected());
    }

    /**
     *
     */
    private void toggleGroupsDialog() {
        m_groupsDialog.getModel().setEnabled(m_useGroupsDialog.isSelected());
    }

}
