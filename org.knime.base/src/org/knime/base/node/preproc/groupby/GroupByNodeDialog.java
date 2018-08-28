/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.groupby;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.dialogutil.column.AggregationColumnPanel;
import org.knime.base.data.aggregation.dialogutil.pattern.PatternAggregationPanel;
import org.knime.base.data.aggregation.dialogutil.type.DataTypeAggregationPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * The node dialog of the group by node.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class GroupByNodeDialog extends NodeDialogPane {

    /** The height of the default component. */
    public static final int DEFAULT_HEIGHT = 370;

    private final JTabbedPane m_tabs;

    private final SettingsModelFilterString m_groupByCols =
        new SettingsModelFilterString(GroupByNodeModel.CFG_GROUP_BY_COLUMNS);

    private final SettingsModelIntegerBounded m_maxUniqueValues =
        new SettingsModelIntegerBounded(GroupByNodeModel.CFG_MAX_UNIQUE_VALUES, 10000, 1, Integer.MAX_VALUE);

    private final SettingsModelBoolean m_enableHilite =
        new SettingsModelBoolean(GroupByNodeModel.CFG_ENABLE_HILITE, false);

    private final SettingsModelString m_valueDelimiter =
        new SettingsModelString(GroupByNodeModel.CFG_VALUE_DELIMITER, GlobalSettings.STANDARD_DELIMITER);

    /**
     * This setting was used prior KNIME 2.6.
     *
     * @deprecated
     */
    @Deprecated
    private final SettingsModelBoolean m_sortInMemory =
        new SettingsModelBoolean(GroupByNodeModel.CFG_SORT_IN_MEMORY, false);

    private final SettingsModelBoolean m_retainOrder =
        new SettingsModelBoolean(GroupByNodeModel.CFG_RETAIN_ORDER, false);

    private final SettingsModelBoolean m_inMemory = new SettingsModelBoolean(GroupByNodeModel.CFG_IN_MEMORY, false);

    private final SettingsModelString m_columnNamePolicy =
        new SettingsModelString(GroupByNodeModel.CFG_COLUMN_NAME_POLICY, ColumnNamePolicy.getDefault().getLabel());

    private final DialogComponentColumnFilter m_groupCol;

    private final AggregationColumnPanel m_aggrColPanel = new AggregationColumnPanel();

    private final DataTypeAggregationPanel m_dataTypeAggrPanel =
        new DataTypeAggregationPanel(GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS);

    private final PatternAggregationPanel m_patternAggrPanel =
        new PatternAggregationPanel(GroupByNodeModel.CFG_PATTERN_AGGREGATORS);

    //used to now the implementation version of the node
    private final SettingsModelInteger m_version = GroupByNodeModel.createVersionModel();

    /** Constructor for class GroupByNodeDialog. */
    public GroupByNodeDialog() {
        this(false, false);
    }

    /**
     * Constructor for class GroupByNodeDialog.
     *
     * @param showPattern <code>true</code> if the pattern based aggregation selection should be displayed
     * @param showType <code>true</code> if the type based aggregation selection should be displayed
     * @since 2.11
     */
    @SuppressWarnings("unchecked")
    public GroupByNodeDialog(final boolean showPattern, final boolean showType) {
        //create the root tab
        m_tabs = new JTabbedPane();
        m_tabs.setBorder(BorderFactory.createTitledBorder(""));
        m_tabs.setOpaque(true);

        //The group column box
        m_groupCol = new DialogComponentColumnFilter(m_groupByCols, 0, false,
            new ColumnFilterPanel.ValueClassFilter(DataValue.class), false);
        m_groupCol.setIncludeTitle(" Group column(s) ");
        m_groupCol.setExcludeTitle(" Available column(s) ");
        //we are only interested in showing the invalid include columns
        m_groupCol.setShowInvalidIncludeColumns(true);
        m_groupByCols.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                //remove all group columns from the aggregation column list
                columnsChanged();
            }
        });

        final JPanel groupColPanel = new JPanel();
        groupColPanel.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        final JPanel groupColFilterPanel = m_groupCol.getComponentPanel();
        groupColFilterPanel.setLayout(new GridLayout(1, 1));
        groupColFilterPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Group settings "));
        groupColPanel.add(groupColFilterPanel, c);
        m_tabs.addTab("Groups", groupColPanel);

        final JPanel columnBasedPanel = new JPanel();
        columnBasedPanel.setLayout(new BoxLayout(columnBasedPanel, BoxLayout.Y_AXIS));
        columnBasedPanel.add(m_aggrColPanel.getComponentPanel());
        m_tabs.addTab(AggregationColumnPanel.DEFAULT_TITLE, columnBasedPanel);

        if (showPattern) {
            final JPanel patternPanel = new JPanel();
            patternPanel.setLayout(new BoxLayout(patternPanel, BoxLayout.Y_AXIS));
            patternPanel.add(m_patternAggrPanel.getComponentPanel());
            m_tabs.addTab(PatternAggregationPanel.DEFAULT_TITLE, patternPanel);
        }
        if (showType) {
            final JPanel typeBasedPanel = new JPanel();
            typeBasedPanel.setLayout(new BoxLayout(typeBasedPanel, BoxLayout.Y_AXIS));
            typeBasedPanel.add(m_dataTypeAggrPanel.getComponentPanel());
            m_tabs.addTab(DataTypeAggregationPanel.DEFAULT_TITLE, typeBasedPanel);
        }

        //calculate the component size
        final int width = (int)m_tabs.getMinimumSize().getWidth();
        final Dimension dimension = new Dimension(width, DEFAULT_HEIGHT);
        m_tabs.setMinimumSize(dimension);
        m_tabs.setPreferredSize(dimension);
        final JPanel topBottomPanel = new JPanel(new GridBagLayout());
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        topBottomPanel.add(m_tabs, c);
        c.gridy++;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 0;
        topBottomPanel.add(createAdvancedOptionsBox(), c);
        super.addTab("Settings", topBottomPanel);

        //add the  process in memory change listener
        m_inMemory.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                inMemoryChanged();
            }
        });

        //add description tab
        final Component descriptionTab = AggregationMethods.createDescriptionPane();
        descriptionTab.setMinimumSize(dimension);
        descriptionTab.setMaximumSize(dimension);
        descriptionTab.setPreferredSize(dimension);
        super.addTab("Description", descriptionTab);
    }

    /**
     * Add additional panel (for example for pivoting) to this dialog.
     *
     * @param p the panel to add to tabs
     * @param title the title for the new tab
     */
    protected final void addPanel(final JPanel p, final String title) {
        final int tabSize = m_tabs.getComponentCount();
        m_tabs.insertTab(title, null, p, null, tabSize - 1);
    }

    /**
     * Add additional panel (for example for pivoting) to this dialog.
     *
     * @param p the panel to add to tabs
     * @param title the title for the new tab
     * @param index the index the {@link JPanel} should be added
     * @since 2.11
     */
    protected final void addPanel(final JPanel p, final String title, final int index) {
        m_tabs.insertTab(title, null, p, null, index);
    }

    /** Call this method if the process in memory flag has changed. */
    protected void inMemoryChanged() {
        final boolean inMem = m_inMemory.getBooleanValue();
        m_retainOrder.setBooleanValue(inMem);
        m_retainOrder.setEnabled(!inMem);
    }

    private JComponent createAdvancedOptionsBox() {
        final DialogComponent maxNoneNumericVals =
            new DialogComponentNumber(m_maxUniqueValues, "Maximum unique values per group", new Integer(1000), 5);
        maxNoneNumericVals
            .setToolTipText("All groups with more unique values " + "will be skipped and replaced by a missing value");
        final DialogComponentStringSelection colNamePolicy = new DialogComponentStringSelection(m_columnNamePolicy,
            "Column naming:", ColumnNamePolicy.getPolicyLabels());
        final DialogComponent enableHilite = new DialogComponentBoolean(m_enableHilite, "Enable hiliting");
        final DialogComponentString valueDelimiter =
            new DialogComponentString(m_valueDelimiter, "Value delimiter", false, 2);
        final DialogComponent inMemory = new DialogComponentBoolean(m_inMemory, "Process in memory");
        inMemory.setToolTipText("Processes all data in memory.");
        final DialogComponent retainOrder = new DialogComponentBoolean(m_retainOrder, "Retain row order");
        retainOrder.setToolTipText("Retains the original row order of the input table.");

        final JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Advanced settings "));
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;

        rootPanel.add(colNamePolicy.getComponentPanel(), c);
        c.gridx++;
        rootPanel.add(enableHilite.getComponentPanel(), c);
        c.gridx++;
        rootPanel.add(inMemory.getComponentPanel(), c);
        c.gridx++;
        rootPanel.add(retainOrder.getComponentPanel(), c);

        c.gridy++;
        c.gridx = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        rootPanel.add(maxNoneNumericVals.getComponentPanel(), c);
        c.gridx++;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel fakePanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.LINE_START;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.NONE;
        fakePanel.add(valueDelimiter.getComponentPanel(), gc);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridy++;
        fakePanel.add(new JPanel(), gc);
        rootPanel.add(fakePanel, c);
        return rootPanel;
    }

    /**
     * Synchronizes the available aggregation column list and the selected group columns.
     */
    protected final void columnsChanged() {
        excludeColumns(m_groupByCols.getIncludeList());
    }

    /**
     * Synchronizes the available aggregation column list and the selected columns.
     *
     * @param columns the column that are changed and need to be excluded from the aggregation list
     */
    protected void excludeColumns(final List<String> columns) {
        m_aggrColPanel.excludeColsChange(columns);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        assert (specs.length == 1);
        final DataTableSpec spec = (DataTableSpec)specs[0];
        try {
            m_maxUniqueValues.loadSettingsFrom(settings);
            m_enableHilite.loadSettingsFrom(settings);
            m_columnNamePolicy.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }

        try {
            //this option was introduced in Knime 2.0
            m_aggrColPanel.loadSettingsFrom(settings, spec);
        } catch (final InvalidSettingsException e) {
            final List<ColumnAggregator> columnMethods =
                GroupByNodeModel.compGetColumnMethods(spec, m_groupByCols.getIncludeList(), settings);
            m_aggrColPanel.initialize(spec, columnMethods);
        }
        try {
            m_patternAggrPanel.loadSettingsFrom(settings, spec);
            m_dataTypeAggrPanel.loadSettingsFrom(settings, spec);
        } catch (InvalidSettingsException e) {
            //introduced in 2.11
        }
        try {
            //this option was introduced in Knime 2.0.3+
            m_retainOrder.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_retainOrder.setBooleanValue(false);
        }
        try {
            //this option was introduced in Knime 2.1.2+
            m_inMemory.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_inMemory.setBooleanValue(false);
        }
        // this option was introduced in Knime 2.4+
        try {
            m_valueDelimiter.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_valueDelimiter.setStringValue(GlobalSettings.STANDARD_DELIMITER);
        }
        m_groupCol.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        columnsChanged();
        try {
            m_version.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        validateSettings(settings);
        m_groupCol.saveSettingsTo(settings);
        m_maxUniqueValues.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
        m_valueDelimiter.saveSettingsTo(settings);
        // This setting was used prior KNIME 2.6
        m_sortInMemory.saveSettingsTo(settings);
        m_columnNamePolicy.saveSettingsTo(settings);
        m_aggrColPanel.saveSettingsTo(settings);
        m_patternAggrPanel.saveSettingsTo(settings);
        m_dataTypeAggrPanel.saveSettingsTo(settings);
        m_retainOrder.saveSettingsTo(settings);
        m_inMemory.saveSettingsTo(settings);

        m_version.saveSettingsTo(settings);
    }

    private void validateSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        //check if the dialog contains invalid group columns
        final Set<String> invalidInclCols = m_groupCol.getInvalidIncludeColumns();
        if (invalidInclCols != null && !invalidInclCols.isEmpty()) {
            throw new InvalidSettingsException(invalidInclCols.size() + " invalid group columns found.");
        }
        final ColumnNamePolicy columnNamePolicy = ColumnNamePolicy.getPolicy4Label(m_columnNamePolicy.getStringValue());
        if (columnNamePolicy == null) {
            throw new InvalidSettingsException("Invalid column name policy");
        }
        m_aggrColPanel.validate();
        m_patternAggrPanel.validate();
        m_dataTypeAggrPanel.validate();
    }
}
