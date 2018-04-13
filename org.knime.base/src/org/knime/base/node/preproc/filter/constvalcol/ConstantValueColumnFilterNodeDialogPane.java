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

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The dialog for the constant value column filter. The user can specify which columns should be checked for containing
 * only identical values.
 *
 * @author Marc Bux, KNIME AG, Zurich, Switzerland
 * @since 3.6
 */
final class ConstantValueColumnFilterNodeDialogPane extends NodeDialogPane {
    /**
     * The title of the tab of the column selection panel.
     */
    private static final String INEXCLUDE_LIST_TAB = "Include / Exclude Columns";

    /**
     * The title of the column selection panel.
     */
    private static final String INEXCLUDE_LIST_TITLE = "Select columns to be included in / excluded from the filter";

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
     * The label of the option for specifying the minimum number of rows a table must have to be considered for
     * filtering.
     */
    private static final String MISC_OPTIONS_ROW_THRESHOLD_LABEL = "Minimum number of rows:";

    /**
     * The title of the group of options that allow to limit the filtering to specific values.
     */
    private static final String MISC_OPTIONS_TITLE = "Miscellaneous options";

    /**
     * the tooltip of the option for specifying the minimum number of rows a table must have to be considered for
     * filtering.
     */
    private static final String MISC_OPTIONS_ROW_THRESHOLD_TOOLTIP =
        "The minimum number of rows a table must have to be considered for filtering. If the table size is below the specified value, the table will not be filtered / altered.";

    /**
     * {@link org.knime.core.node.defaultnodesettings.DialogComponent} objects that have to be saved to the settings
     * file manually via {@link #saveAdditionalSettingsTo(NodeSettingsWO)}.
     */
    private final List<DialogComponent> m_components;

    /**
     * Creates a new {@link DefaultNodeSettingsPane} for the column filter in order to set the desired columns.
     */
    public ConstantValueColumnFilterNodeDialogPane() {
        m_components = new LinkedList<>();
        addInExcludeListTab();
        addFilterOptionsTab();
    }

    /**
     * Creates dialog components for selecting which columns to include in respectively exclude from the filtering
     * process.
     */
    private void addInExcludeListTab() {
        SettingsModelColumnFilter2 columnFilter = ConstantValueColumnFilterNodeModel.createColumnFilterModel();
        DialogComponentColumnFilter2 dialog = new DialogComponentColumnFilter2(columnFilter, 0);
        dialog.setToolTipText(INEXCLUDE_LIST_TOOLTIP);
        m_components.add(dialog);
        dialog.getComponentPanel().setBorder(new TitledBorder(INEXCLUDE_LIST_TITLE));
        addTab(INEXCLUDE_LIST_TAB, dialog.getComponentPanel());
    }

    /**
     * Creates dialog components for specifying which constant value columns to filter.
     */
    private void addFilterOptionsTab() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;

        SettingsModelBoolean filterNumeric = ConstantValueColumnFilterNodeModel.createFilterNumericModel();
        SettingsModelDouble filterNumericValue = ConstantValueColumnFilterNodeModel.createFilterNumericValueModel();
        SettingsModelBoolean filterString = ConstantValueColumnFilterNodeModel.createFilterStringModel();
        SettingsModelString filterStringValue = ConstantValueColumnFilterNodeModel.createFilterStringValueModel();
        SettingsModelBoolean filterMissing = ConstantValueColumnFilterNodeModel.createFilterMissingModel();
        SettingsModelBoolean filterAll = ConstantValueColumnFilterNodeModel.createFilterAllModel(filterNumeric,
            filterNumericValue, filterString, filterStringValue, filterMissing);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(new TitledBorder(FILTER_OPTIONS_TITLE));
        addComponent(new DialogComponentBoolean(filterAll, FILTER_OPTIONS_ALL_LABEL), FILTER_OPTIONS_ALL_TOOLTIP,
            filterPanel, c, 0, 0, 2, 0, 0);
        addComponent(new DialogComponentBoolean(filterNumeric, FILTER_OPTIONS_NUMERIC_LABEL),
            FILTER_OPTIONS_NUMERIC_TOOLTIP, filterPanel, c, 0, 1, 1, 0, 0);
        addComponent(new DialogComponentNumberEdit(filterNumericValue, "", 5), FILTER_OPTIONS_NUMERIC_TOOLTIP,
            filterPanel, c, 1, 1, 1, 0, 0);
        addComponent(new DialogComponentBoolean(filterString, FILTER_OPTIONS_STRING_LABEL),
            FILTER_OPTIONS_STRING_TOOLTIP, filterPanel, c, 0, 2, 1, 0, 0);
        addComponent(new DialogComponentString(filterStringValue, ""), FILTER_OPTIONS_STRING_TOOLTIP, filterPanel, c, 1,
            2, 1, 0, 0);
        addComponent(new DialogComponentBoolean(filterMissing, FILTER_OPTIONS_MISSING_LABEL),
            FILTER_OPTIONS_MISSING_TOOLTIP, filterPanel, c, 0, 3, 2, 0, 0);

        JPanel miscPanel = new JPanel(new GridBagLayout());
        miscPanel.setBorder(new TitledBorder(MISC_OPTIONS_TITLE));
        addComponent(
            new DialogComponentNumber(ConstantValueColumnFilterNodeModel.createRowThresholdModel(),
                MISC_OPTIONS_ROW_THRESHOLD_LABEL, 1, 5),
            MISC_OPTIONS_ROW_THRESHOLD_TOOLTIP, miscPanel, c, 0, 0, 1, 0, 0);

        JPanel outerPanel = new JPanel(new GridBagLayout());
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        outerPanel.add(filterPanel);
        outerPanel.add(miscPanel);

        addTab(FILTER_OPTIONS_TAB, outerPanel);
    }

    private void addComponent(final DialogComponent dc, final String tooltipText, final JPanel panel,
        final GridBagConstraints c, final int x, final int y, final int width, final int weightx, final int weighty) {
        dc.setToolTipText(tooltipText);
        m_components.add(dc);

        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.weightx = weightx;
        c.weighty = weighty;

        panel.add(dc.getComponentPanel(), c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (DialogComponent comp : m_components) {
            comp.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        for (DialogComponent comp : m_components) {
            comp.loadSettingsFrom(settings, specs);
        }
    }

}
