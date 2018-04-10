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
 *   4 Apr 2018 (Marc): created
 */
package org.knime.base.node.preproc.filter.constvalcol;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog for the constant value column filter. The user can specify which columns should be checked for containing
 * only identical values.
 *
 * @author Marc Bux, KNIME AG, Zurich, Switzerland
 * @since 3.6
 */
public class ConstantValueColumnFilterNodeDialogPane extends DefaultNodeSettingsPane {
    /**
     * The title of the list of columns that is selected to be considered for filtering.
     */
    private static final String INCLUDE_LIST_TITLE = "Apply filter to these columns (include)";

    /**
     * The title of the list of columns that is selected to be passed through.
     */
    private static final String EXCLUDE_LIST_TITLE = "Pass these columns through filter (exclude)";

    /**
     * The tooltip of the column selection panel.
     */
    private static final String INEXCLUDE_LIST_TOOLTIP =
        "Select which columns to consider for filtering and which columns to pass through.";

    /**
     * The title of the tab in which filter options can be selected.
     */
    private static final String FILTER_OPTIONS_TAB = "Filter Settings";

    /**
     * The title of the group of options that allow to limit the filtering to specific values.
     */
    private static final String FILTER_OPTIONS_TITLE = "Filter constant value columns";

    /**
     * The label of the option to filter all constant value columns.
     */
    private static final String FILTER_OPTIONS_ALL_LABEL = "all";

    /**
     * The tooltip of the option to filter all constant value columns.
     */
    private static final String FILTER_OPTIONS_ALL_TOOLTIP =
        "Filter columns with any constant value, i.e., all columns containing only duplicates of the same value.";

    /**
     * The label of the option to filter columns with a specific constant numeric value.
     */
    private static final String FILTER_OPTIONS_NUMERIC_LABEL = "with numeric value";

    /**
     * The tooltip of the option to filter columns with a specific constant numeric value.
     */
    private static final String FILTER_OPTIONS_NUMERIC_TOOLTIP =
        "Filter columns containing only a specific numeric value.";

    /**
     * The label of the option to filter columns with a specific constant String value.
     */
    private static final String FILTER_OPTIONS_STRING_LABEL = "with String value";

    /**
     * The tooltip of the option to filter columns with a specific constant String value.
     */
    private static final String FILTER_OPTIONS_STRING_TOOLTIP =
        "Filter columns containing only a specific String value.";

    /**
     * The label of the option to filter columns containing only missing values.
     */
    private static final String FILTER_OPTIONS_MISSING_LABEL = "with missing value";

    /**
     * The tooltip of the option to filter columns containing only missing values.
     */
    private static final String FILTER_OPTIONS_MISSING_TOOLTIP = "Filter columns containing only missing values.";

    /**
     * The dialog component for the option to filter all constant value columns.
     */
    private DialogComponentBoolean m_dialogAll;

    /**
     * The dialog component for the option to filter columns with a specific constant numeric value.
     */
    private DialogComponentBoolean m_dialogNumeric;

    /**
     * The dialog component for the specific numeric value that is to be looked for in filtering.
     */
    private DialogComponentNumberEdit m_dialogNumericValue;

    /**
     * The dialog component for the option to filter columns with a specific constant String value.
     */
    private DialogComponentBoolean m_dialogString;

    /**
     * The dialog component for the specific String value that is to be looked for in filtering.
     */
    private DialogComponentString m_dialogStringValue;

    /**
     * The dialog component for the option to filter columns containing only missing values.
     */
    private DialogComponentBoolean m_dialogMissing;

    /**
     * Creates a new {@link DefaultNodeSettingsPane} for the column filter in order to set the desired columns.
     */
    public ConstantValueColumnFilterNodeDialogPane() {
        addInExcludeListDialogComponent();
        addFilterOptionsDialogComponent();
    }

    /**
     * Creates dialog components for selecting which columns to include in respectively exclude from the filtering
     * process.
     */
    private void addInExcludeListDialogComponent() {
        SettingsModelColumnFilter2 settings =
            new SettingsModelColumnFilter2(ConstantValueColumnFilterNodeModel.SELECTED_COLS);
        DialogComponentColumnFilter2 dialog = new DialogComponentColumnFilter2(settings, 0);
        dialog.setIncludeTitle(INCLUDE_LIST_TITLE);
        dialog.setExcludeTitle(EXCLUDE_LIST_TITLE);
        dialog.setToolTipText(INEXCLUDE_LIST_TOOLTIP);
        addDialogComponent(dialog);
    }

    /**
     * Creates dialog components for specifying which constant value columns to filter.
     */
    private void addFilterOptionsDialogComponent() {
        // The panel has to be created manually, since there are no default composite dialog components that have a
        // check box, a text label, and a text field in the same line. However, such components are required here
        // for the options to filter columns containing a specific constant numeric or String value.
        JPanel innerPanel = new JPanel(new GridBagLayout());
        innerPanel.setBorder(new TitledBorder(FILTER_OPTIONS_TITLE));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        SettingsModelBoolean settingsAll =
            new SettingsModelBoolean(ConstantValueColumnFilterNodeModel.FILTER_ALL, false);
        m_dialogAll = new DialogComponentBoolean(settingsAll, FILTER_OPTIONS_ALL_LABEL);
        m_dialogAll.setToolTipText(FILTER_OPTIONS_ALL_TOOLTIP);
        innerPanel.add(m_dialogAll.getComponentPanel(), c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        SettingsModelBoolean settingsNumeric =
            new SettingsModelBoolean(ConstantValueColumnFilterNodeModel.FILTER_NUMERIC, false);
        m_dialogNumeric = new DialogComponentBoolean(settingsNumeric, FILTER_OPTIONS_NUMERIC_LABEL);
        m_dialogNumeric.setToolTipText(FILTER_OPTIONS_NUMERIC_TOOLTIP);
        innerPanel.add(m_dialogNumeric.getComponentPanel(), c);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        SettingsModelDouble settingsNumericValue =
            new SettingsModelDouble(ConstantValueColumnFilterNodeModel.FILTER_NUMERIC_VALUE, 0);
        m_dialogNumericValue = new DialogComponentNumberEdit(settingsNumericValue, "");
        m_dialogNumericValue.setToolTipText(FILTER_OPTIONS_NUMERIC_TOOLTIP);
        innerPanel.add(m_dialogNumericValue.getComponentPanel(), c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        SettingsModelBoolean settingsString =
            new SettingsModelBoolean(ConstantValueColumnFilterNodeModel.FILTER_STRING, false);
        m_dialogString = new DialogComponentBoolean(settingsString, FILTER_OPTIONS_STRING_LABEL);
        m_dialogString.setToolTipText(FILTER_OPTIONS_STRING_TOOLTIP);
        innerPanel.add(m_dialogString.getComponentPanel(), c);

        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        SettingsModelString settingsStringValue =
            new SettingsModelString(ConstantValueColumnFilterNodeModel.FILTER_STRING_VALUE, "");
        m_dialogStringValue = new DialogComponentString(settingsStringValue, "");
        m_dialogStringValue.setToolTipText(FILTER_OPTIONS_STRING_TOOLTIP);
        innerPanel.add(m_dialogStringValue.getComponentPanel(), c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        SettingsModelBoolean settingsMissing =
            new SettingsModelBoolean(ConstantValueColumnFilterNodeModel.FILTER_MISSING, false);
        m_dialogMissing = new DialogComponentBoolean(settingsMissing, FILTER_OPTIONS_MISSING_LABEL);
        m_dialogMissing.setToolTipText(FILTER_OPTIONS_MISSING_TOOLTIP);
        innerPanel.add(m_dialogMissing.getComponentPanel(), c);

        // If all columns are to be filtered, specific column filtering is disabled.
        settingsAll.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (settingsAll.getBooleanValue()) {
                    settingsNumeric.setEnabled(false);
                    settingsNumericValue.setEnabled(false);
                    settingsString.setEnabled(false);
                    settingsStringValue.setEnabled(false);
                    settingsMissing.setEnabled(false);
                    settingsNumeric.setBooleanValue(false);
                    settingsString.setBooleanValue(false);
                    settingsMissing.setBooleanValue(false);
                } else {
                    settingsNumeric.setEnabled(true);
                    settingsNumericValue.setEnabled(true);
                    settingsString.setEnabled(true);
                    settingsStringValue.setEnabled(true);
                    settingsMissing.setEnabled(true);
                }

            }
        });

        settingsAll.setBooleanValue(true);

        // Weights in the outer panel assure that the inner panel does not request more space than required.
        JPanel outerPanel = new JPanel(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        outerPanel.add(innerPanel, c);

        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        outerPanel.add(new JLabel(), c);

        addTabAt(0, FILTER_OPTIONS_TAB, outerPanel);
        selectTab(FILTER_OPTIONS_TAB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        // Additional (custom) components that have to be saved to the settings file manually.
        m_dialogAll.saveSettingsTo(settings);
        m_dialogNumeric.saveSettingsTo(settings);
        m_dialogNumericValue.saveSettingsTo(settings);
        m_dialogString.saveSettingsTo(settings);
        m_dialogStringValue.saveSettingsTo(settings);
        m_dialogMissing.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        try {
            m_dialogAll.getModel().loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            System.err
                .println("Settings for option" + FILTER_OPTIONS_TITLE + " " + FILTER_OPTIONS_ALL_LABEL + "invalid");
        }

        try {
            m_dialogNumeric.getModel().loadSettingsFrom(settings);
            m_dialogNumericValue.getModel().loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            System.err
                .println("Settings for option" + FILTER_OPTIONS_TITLE + " " + FILTER_OPTIONS_NUMERIC_LABEL + "invalid");
        }

        try {
            m_dialogString.getModel().loadSettingsFrom(settings);
            m_dialogStringValue.getModel().loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            System.err
                .println("Settings for option" + FILTER_OPTIONS_TITLE + " " + FILTER_OPTIONS_STRING_LABEL + "invalid");
        }

        try {
            m_dialogMissing.getModel().loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            System.err
                .println("Settings for option" + FILTER_OPTIONS_TITLE + " " + FILTER_OPTIONS_MISSING_LABEL + "invalid");
        }
    }

}
