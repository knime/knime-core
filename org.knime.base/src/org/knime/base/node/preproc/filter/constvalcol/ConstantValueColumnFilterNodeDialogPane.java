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

/**
 * The dialog for the constant value column filter. The user can specify which columns should be checked for containing
 * only identical values.
 *
 * @author Marc Bux, KNIME AG, Zurich, Switzerland
 * @since 3.6
 */
final class ConstantValueColumnFilterNodeDialogPane extends DefaultNodeSettingsPane {
    /**
     * The title of the group of options that allow to limit the filtering to specific values.
     */
    private static final String INEXCLUDE_LIST_TITLE = "Columns to be included in / excluded from the filter";

    /**
     * The tooltip of the column selection panel.
     */
    private static final String INEXCLUDE_LIST_TOOLTIP =
        "Select which columns to consider for filtering and which columns to pass through.";

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
     * Creates a new {@link DefaultNodeSettingsPane} for the column filter in order to set the desired columns.
     */
    public ConstantValueColumnFilterNodeDialogPane() {
        addInExcludeListDialogComponent();
        addFilterOptionsDialogComponent();
        addMiscOptions();
    }

    /**
     * Creates dialog components for selecting which columns to include in respectively exclude from the filtering
     * process.
     */
    private void addInExcludeListDialogComponent() {
        createNewGroup(INEXCLUDE_LIST_TITLE);
        SettingsModelColumnFilter2 settings =
            new SettingsModelColumnFilter2(ConstantValueColumnFilterNodeModel.SELECTED_COLS);
        DialogComponentColumnFilter2 dialog = new DialogComponentColumnFilter2(settings, 0);
        dialog.setToolTipText(INEXCLUDE_LIST_TOOLTIP);
        addDialogComponent(dialog);
        closeCurrentGroup();
    }

    /**
     * Creates dialog components for specifying which constant value columns to filter.
     */
    private void addFilterOptionsDialogComponent() {
        SettingsModelBoolean filterNumeric = ConstantValueColumnFilterNodeModel.createFilterNumericModel();
        SettingsModelDouble filterNumericValue = ConstantValueColumnFilterNodeModel.createFilterNumericValueModel();
        SettingsModelBoolean filterString = ConstantValueColumnFilterNodeModel.createFilterStringModel();
        SettingsModelString filterStringValue = ConstantValueColumnFilterNodeModel.createFilterStringValueModel();
        SettingsModelBoolean filterMissing = ConstantValueColumnFilterNodeModel.createFilterMissingModel();
        SettingsModelBoolean filterAll = ConstantValueColumnFilterNodeModel.createFilterAllModel(filterNumeric,
            filterNumericValue, filterString, filterStringValue, filterMissing);

        createNewGroup(FILTER_OPTIONS_TITLE);

        registerNewDialogComponent(new DialogComponentBoolean(filterAll, FILTER_OPTIONS_ALL_LABEL),
            FILTER_OPTIONS_ALL_TOOLTIP);

        setHorizontalPlacement(true);

        registerNewDialogComponent(new DialogComponentBoolean(filterNumeric, FILTER_OPTIONS_NUMERIC_LABEL),
            FILTER_OPTIONS_NUMERIC_TOOLTIP);
        registerNewDialogComponent(new DialogComponentNumberEdit(filterNumericValue, "", 5),
            FILTER_OPTIONS_NUMERIC_TOOLTIP);

        setHorizontalPlacement(false);
        setHorizontalPlacement(true);

        registerNewDialogComponent(new DialogComponentBoolean(filterString, FILTER_OPTIONS_STRING_LABEL),
            FILTER_OPTIONS_STRING_TOOLTIP);
        registerNewDialogComponent(new DialogComponentString(filterStringValue, ""), FILTER_OPTIONS_STRING_TOOLTIP);

        setHorizontalPlacement(false);

        registerNewDialogComponent(new DialogComponentBoolean(filterMissing, FILTER_OPTIONS_MISSING_LABEL),
            FILTER_OPTIONS_MISSING_TOOLTIP);

        closeCurrentGroup();
    }

    private void addMiscOptions() {
        createNewGroup(MISC_OPTIONS_TITLE);

        registerNewDialogComponent(
            new DialogComponentNumber(ConstantValueColumnFilterNodeModel.createRowThresholdModel(),
                MISC_OPTIONS_ROW_THRESHOLD_LABEL, 1, 5),
            MISC_OPTIONS_ROW_THRESHOLD_TOOLTIP);

        closeCurrentGroup();
    }

    private void registerNewDialogComponent(final DialogComponent dc, final String tooltipText) {
        dc.setToolTipText(tooltipText);
        dc.getComponentPanel().setMaximumSize(dc.getComponentPanel().getPreferredSize());
        addDialogComponent(dc);
    }

}
