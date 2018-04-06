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
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
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
     * Additional {@link org.knime.core.node.defaultnodesettings.DialogComponent}s that, due to having an unconventional
     * layout, have to be saved to the settings file manually via {@link #saveAdditionalSettingsTo(NodeSettingsWO)}.
     */
    private final List<DialogComponent> m_additionalComponents;

    /**
     * The title of the list of columns that is selected to be considered for filtering.
     */
    private static final String INCLUDE_LIST_TITLE = "Filter";

    /**
     * The title of the list of columns that is selected to be passed through.
     */
    private static final String EXCLUDE_LIST_TITLE = "Pass Through";

    /**
     * The tooltip of the column selection panel.
     */
    private static final String INEXCLUDE_LIST_TOOLTIP =
        "Select which columns to consider for filtering and which columns to pass through.";

    private static final String FILTER_OPTIONS_TAG = "Filter Settings";

    private static final String FILTER_OPTIONS_TITLE = "Filter constant value columns";

    private static final String FILTER_OPTIONS_ALL_LABEL = "all";

    private static final String FILTER_OPTIONS_ALL_TOOLTIP =
        "Filter columns with any constant value, i.e., all columns containing only duplicates of the same value.";

    private static final String FILTER_OPTIONS_NUMERIC_LABEL = "with numeric value";

    private static final String FILTER_OPTIONS_NUMERIC_TOOLTIP =
        "Filter columns containing only a specific numeric value.";

    private static final String FILTER_OPTIONS_STRING_LABEL = "with String value";

    private static final String FILTER_OPTIONS_STRING_TOOLTIP =
        "Filter columns containing only a specific String value.";

    private static final String FILTER_OPTIONS_MISSING_LABEL = "with missing value";

    private static final String FILTER_OPTIONS_MISSING_TOOLTIP = "Filter columns containing only missing values.";

    /**
     * Creates a new {@link DefaultNodeSettingsPane} for the column filter in order to set the desired columns.
     */
    public ConstantValueColumnFilterNodeDialogPane() {
        m_additionalComponents = new LinkedList<>();

        SettingsModelColumnFilter2 settings = new SettingsModelColumnFilter2(ConstantValueColumnFilterNodeModel.SELECTED_COLS);
        DialogComponentColumnFilter2 dialog = new DialogComponentColumnFilter2(settings, 0);
        dialog.setIncludeTitle(INCLUDE_LIST_TITLE);
        dialog.setExcludeTitle(EXCLUDE_LIST_TITLE);
        dialog.setToolTipText(INEXCLUDE_LIST_TOOLTIP);
        addDialogComponent(dialog);

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
        DialogComponentBoolean dialogAll = new DialogComponentBoolean(settingsAll, FILTER_OPTIONS_ALL_LABEL);
        dialogAll.setToolTipText(FILTER_OPTIONS_ALL_TOOLTIP);
        innerPanel.add(dialogAll.getComponentPanel(), c);
        m_additionalComponents.add(dialogAll);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        SettingsModelBoolean settingsNumeric =
            new SettingsModelBoolean(ConstantValueColumnFilterNodeModel.FILTER_NUMERIC, false);
        DialogComponentBoolean dialogNumeric =
            new DialogComponentBoolean(settingsNumeric, FILTER_OPTIONS_NUMERIC_LABEL);
        dialogNumeric.setToolTipText(FILTER_OPTIONS_NUMERIC_TOOLTIP);
        innerPanel.add(dialogNumeric.getComponentPanel(), c);
        m_additionalComponents.add(dialogNumeric);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        SettingsModelDouble settingsNumericValue =
            new SettingsModelDouble(ConstantValueColumnFilterNodeModel.FILTER_NUMERIC_VALUE, 0);
        DialogComponentNumberEdit dialogNumericValue = new DialogComponentNumberEdit(settingsNumericValue, "");
        dialogNumericValue.setToolTipText(FILTER_OPTIONS_NUMERIC_TOOLTIP);
        innerPanel.add(dialogNumericValue.getComponentPanel(), c);
        m_additionalComponents.add(dialogNumericValue);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        SettingsModelBoolean settingsString =
            new SettingsModelBoolean(ConstantValueColumnFilterNodeModel.FILTER_STRING, false);
        DialogComponentBoolean dialogString = new DialogComponentBoolean(settingsString, FILTER_OPTIONS_STRING_LABEL);
        dialogString.setToolTipText(FILTER_OPTIONS_STRING_TOOLTIP);
        innerPanel.add(dialogString.getComponentPanel(), c);
        m_additionalComponents.add(dialogString);

        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        SettingsModelString settingsStringValue =
            new SettingsModelString(ConstantValueColumnFilterNodeModel.FILTER_STRING_VALUE, "");
        DialogComponentString dialogStringValue = new DialogComponentString(settingsStringValue, "");
        dialogStringValue.setToolTipText(FILTER_OPTIONS_STRING_TOOLTIP);
        innerPanel.add(dialogStringValue.getComponentPanel(), c);
        m_additionalComponents.add(dialogStringValue);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        SettingsModelBoolean settingsMissing =
            new SettingsModelBoolean(ConstantValueColumnFilterNodeModel.FILTER_MISSING, false);
        DialogComponentBoolean dialogMissing =
            new DialogComponentBoolean(settingsMissing, FILTER_OPTIONS_MISSING_LABEL);
        dialogMissing.setToolTipText(FILTER_OPTIONS_MISSING_TOOLTIP);
        innerPanel.add(dialogMissing.getComponentPanel(), c);
        m_additionalComponents.add(dialogMissing);

        settingsAll.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (settingsAll.getBooleanValue()) {
                    settingsNumeric.setEnabled(false);
                    settingsNumericValue.setEnabled(false);
                    settingsString.setEnabled(false);
                    settingsStringValue.setEnabled(false);
                    settingsMissing.setEnabled(false);
                } else {
                    settingsNumeric.setEnabled(true);
                    settingsNumericValue.setEnabled(true);
                    settingsString.setEnabled(true);
                    settingsStringValue.setEnabled(true);
                    settingsMissing.setEnabled(true);
                }

            }
        });

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

        addTabAt(0, FILTER_OPTIONS_TAG, outerPanel);
        selectTab(FILTER_OPTIONS_TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (DialogComponent comp : m_additionalComponents) {
            comp.saveSettingsTo(settings);
        }
    }

}
