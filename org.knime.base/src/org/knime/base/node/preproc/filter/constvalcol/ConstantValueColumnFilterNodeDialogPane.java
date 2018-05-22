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

import java.awt.Color;
import java.awt.Component;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
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
    /*
     * The title of the tab of the column selection panel.
     */
    private static final String INEXCLUDE_LIST_TAB = "Column Options";

    /*
     * The tooltip of the column selection panel.
     */
    private static final String INEXCLUDE_LIST_TOOLTIP =
        "Select which columns to apply the filter to and which columns to retain.";

    /*
     * The title of the list of columns for which to apply the filter.
     */
    private static final String INEXCLUDE_LIST_INCLUDE_TITLE = "Apply filter";

    /*
     * The title of the list of columns which to retain (i.e., which to be passed through the filter).
     */
    private static final String INEXCLUDE_LIST_EXCLUDE_TITLE = "Do not apply filter (retain column)";

    /*
     * The title of the tab in which filter options can be selected.
     */
    private static final String FILTER_OPTIONS_TAB = "Filter Options";

    /*
     * The title of the group of options that allow to limit the filtering to specific values.
     */
    private static final String FILTER_OPTIONS_TITLE = "Remove constant value columns";

    /*
     * The label of the option to filter all constant value columns.
     */
    static final String FILTER_OPTIONS_ALL_LABEL = "All";

    /*
     * The label of the option to filter .
     */
    static final String FILTER_OPTIONS_PARTIAL_LABEL = "Partial";

    /*
     * The tooltip of the option to filter all or only specific constant value columns.
     */
    private static final String FILTER_OPTIONS_SPECIFICITY_TOOLTIP =
        "Filter all columns with any constant value or only filter columns with specific constant values.";

    /*
     * The label of the option to filter columns with a specific constant numeric value.
     */
    private static final String FILTER_OPTIONS_NUMERIC_LABEL = "containing only numeric values of";

    /*
     * The tooltip of the option to filter columns with a specific constant numeric value.
     */
    private static final String FILTER_OPTIONS_NUMERIC_TOOLTIP =
        "Filter columns containing only a specific numeric value.";

    /*
     * The label of the option to filter columns with a specific constant String value.
     */
    private static final String FILTER_OPTIONS_STRING_LABEL = "containing only String values of";

    /*
     * The tooltip of the option to filter columns with a specific constant String value.
     */
    private static final String FILTER_OPTIONS_STRING_TOOLTIP =
        "Filter columns containing only a specific String value.";

    /*
     * The label of the option to filter columns containing only missing values.
     */
    private static final String FILTER_OPTIONS_MISSING_LABEL = "containing only missing values";

    /*
     * The tooltip of the option to filter columns containing only missing values.
     */
    private static final String FILTER_OPTIONS_MISSING_TOOLTIP = "Filter columns containing only missing values.";

    /*
     * The warning message to be displayed (in the dialog pane) when partial filtering is selected, yet no type of
     * column (numeric, String, missing) is specified.
     */
    private static final String FILTER_OPTIONS_UNSELETCTED_WARNING =
        "At least one partial filtering option should be selected.";

    /*
     * The label of the option for specifying the minimum number of rows a table must have to be considered for
     * filtering.
     */
    private static final String MISC_OPTIONS_ROW_THRESHOLD_LABEL = "Minimum number of rows:";

    /*
     * The title of the group of options that allow to limit the filtering to specific values.
     */
    private static final String MISC_OPTIONS_TITLE = "Miscellaneous options";

    /*
     * The tooltip of the option for specifying the minimum number of rows a table must have to be considered for
     * filtering.
     */
    private static final String MISC_OPTIONS_ROW_THRESHOLD_TOOLTIP =
        "The minimum number of rows a table must have to be considered for filtering. If the table size is below the specified value, the table will not be filtered / altered.";

    /*
     * Dialog components that have to be saved to the settings file manually.
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

    /*
     * Creates dialog components for selecting which columns to include in respectively exclude from the filtering
     * process.
     */
    private void addInExcludeListTab() {
        SettingsModelColumnFilter2 columnFilter = ConstantValueColumnFilterNodeModel.createColumnFilterModel();
        DialogComponentColumnFilter2 dialog = new DialogComponentColumnFilter2(columnFilter, 0);
        dialog.setToolTipText(INEXCLUDE_LIST_TOOLTIP);
        dialog.setIncludeTitle(INEXCLUDE_LIST_INCLUDE_TITLE);
        dialog.setExcludeTitle(INEXCLUDE_LIST_EXCLUDE_TITLE);
        m_components.add(dialog);

        addTab(INEXCLUDE_LIST_TAB, dialog.getComponentPanel());
    }

    /*
     * Creates dialog components for specifying which constant value columns to filter.
     */
    private void addFilterOptionsTab() {
        SettingsModelBoolean filterNumeric = ConstantValueColumnFilterNodeModel.createFilterNumericModel();
        SettingsModelDouble filterNumericValue = ConstantValueColumnFilterNodeModel.createFilterNumericValueModel();
        SettingsModelBoolean filterString = ConstantValueColumnFilterNodeModel.createFilterStringModel();
        SettingsModelString filterStringValue = ConstantValueColumnFilterNodeModel.createFilterStringValueModel();
        SettingsModelBoolean filterMissing = ConstantValueColumnFilterNodeModel.createFilterMissingModel();
        SettingsModelString filterAll = ConstantValueColumnFilterNodeModel.createFilterAllModel();

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(new TitledBorder(FILTER_OPTIONS_TITLE));
        addDialogComponentsWithGlue(false, filterPanel, FILTER_OPTIONS_SPECIFICITY_TOOLTIP,
            new DialogComponentButtonGroup(filterAll, true, null, FILTER_OPTIONS_ALL_LABEL,
                FILTER_OPTIONS_PARTIAL_LABEL));
        addDialogComponentsWithGlue(true, filterPanel, FILTER_OPTIONS_NUMERIC_TOOLTIP,
            new DialogComponentBoolean(filterNumeric, FILTER_OPTIONS_NUMERIC_LABEL),
            new DialogComponentNumberEdit(filterNumericValue, "", 5));
        addDialogComponentsWithGlue(true, filterPanel, FILTER_OPTIONS_STRING_TOOLTIP,
            new DialogComponentBoolean(filterString, FILTER_OPTIONS_STRING_LABEL),
            new DialogComponentString(filterStringValue, ""));
        addDialogComponentsWithGlue(true, filterPanel, FILTER_OPTIONS_MISSING_TOOLTIP,
            new DialogComponentBoolean(filterMissing, FILTER_OPTIONS_MISSING_LABEL));

        JLabel warningLabel =
            new JLabel(FILTER_OPTIONS_UNSELETCTED_WARNING, NodeView.WARNING_ICON, SwingConstants.LEFT);
        warningLabel.setVisible(false);
        warningLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        warningLabel.setBackground(Color.WHITE);
        warningLabel.setOpaque(true);
        JPanel warningPanel = new JPanel();
        warningPanel.setLayout(new BoxLayout(warningPanel, BoxLayout.X_AXIS));
        warningPanel.add(Box.createHorizontalGlue());
        fixWidthAndAddComponent(warningLabel, warningPanel);
        warningPanel.add(Box.createHorizontalGlue());
        filterPanel.add(warningPanel);

        JPanel miscPanel = new JPanel();
        miscPanel.setLayout(new BoxLayout(miscPanel, BoxLayout.Y_AXIS));
        miscPanel.setBorder(new TitledBorder(MISC_OPTIONS_TITLE));
        addDialogComponentsWithGlue(false, miscPanel, MISC_OPTIONS_ROW_THRESHOLD_TOOLTIP, new DialogComponentNumber(
            ConstantValueColumnFilterNodeModel.createRowThresholdModel(), MISC_OPTIONS_ROW_THRESHOLD_LABEL, 1, 5));

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        outerPanel.add(filterPanel);
        outerPanel.add(miscPanel);
        outerPanel.add(Box.createVerticalGlue());

        addTabAt(0, FILTER_OPTIONS_TAB, outerPanel);
        setSelected(FILTER_OPTIONS_TAB);

        ChangeListener cl = new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (filterAll.getStringValue()
                    .equals(ConstantValueColumnFilterNodeDialogPane.FILTER_OPTIONS_ALL_LABEL)) {
                    filterNumeric.setEnabled(false);
                    filterNumericValue.setEnabled(false);
                    filterString.setEnabled(false);
                    filterStringValue.setEnabled(false);
                    filterMissing.setEnabled(false);
                    warningLabel.setVisible(false);
                } else {
                    filterNumeric.setEnabled(true);
                    filterNumericValue.setEnabled(true);
                    filterString.setEnabled(true);
                    filterStringValue.setEnabled(true);
                    filterMissing.setEnabled(true);
                    if (!filterNumeric.getBooleanValue() && !filterString.getBooleanValue()
                        && !filterMissing.getBooleanValue()) {
                        warningLabel.setVisible(true);
                    } else {
                        warningLabel.setVisible(false);
                    }
                }
            }
        };

        filterNumeric.addChangeListener(cl);
        filterString.addChangeListener(cl);
        filterMissing.addChangeListener(cl);
        filterAll.addChangeListener(cl);
    }

    /*
     * A function that registers new {@link DialogComponent}, which it then adds to a newly created (horizontal) {@link JPanel}.
     */
    private void addDialogComponentsWithGlue(final boolean createStrut, final JPanel outerPanel,
        final String tooltipText, final DialogComponent... dcs) {
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));
        if (createStrut) {
            fixWidthAndAddComponent(Box.createHorizontalStrut(20), innerPanel);
        }
        for (DialogComponent dc : dcs) {
            dc.setToolTipText(tooltipText);
            m_components.add(dc);
            fixWidthAndAddComponent(dc.getComponentPanel(), innerPanel);
        }
        innerPanel.add(Box.createHorizontalGlue());
        outerPanel.add(innerPanel);
    }

    /*
     * A function that fixes the width of a {@link Component} and adds it to a {@link JPanel}.
     */
    private static void fixWidthAndAddComponent(final Component c, final JPanel panel) {
        c.setMaximumSize(c.getPreferredSize());
        panel.add(c);
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
