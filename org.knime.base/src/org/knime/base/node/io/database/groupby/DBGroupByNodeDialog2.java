/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.base.node.io.database.groupby;

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

import org.knime.base.data.aggregation.dialogutil.column.AggregationColumnPanel;
import org.knime.base.data.aggregation.dialogutil.pattern.PatternAggregationPanel;
import org.knime.base.data.aggregation.dialogutil.type.DataTypeAggregationPanel;
import org.knime.base.node.io.database.groupby.dialog.DBAggregationFunctionProvider;
import org.knime.base.node.io.database.groupby.dialog.column.DBColumnAggregationFunctionPanel;
import org.knime.base.node.io.database.groupby.dialog.pattern.DBPatternAggregationFunctionPanel;
import org.knime.base.node.io.database.groupby.dialog.type.DBDataTypeAggregationFunctionPanel;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * The node dialog of the group by node.
 *
 * @author Tobias Koetter, University of Konstanz
 */
final class DBGroupByNodeDialog2 extends NodeDialogPane {

    private static final int DEFAULT_WIDTH = 680;

    private static final int DEFAULT_HEIGHT = 350;

    private final JTabbedPane m_tabs;

    private final SettingsModelFilterString m_groupByCols = new SettingsModelFilterString(
        DBGroupByNodeModel2.CFG_GROUP_BY_COLUMNS);

    private final SettingsModelString m_columnNamePolicy = new SettingsModelString(
        DBGroupByNodeModel2.CFG_COLUMN_NAME_POLICY, ColumnNamePolicy.getDefault().getLabel());

    private final DialogComponentColumnFilter m_groupCol;

    private final DBColumnAggregationFunctionPanel m_aggregationPanel =
            new DBColumnAggregationFunctionPanel(DBGroupByNodeModel2.CFG_AGGREGATION_FUNCTIONS);

    private final DBPatternAggregationFunctionPanel m_patternPanel =
            new DBPatternAggregationFunctionPanel(DBGroupByNodeModel2.CFG_PATTERN_AGGREGATION_FUNCTIONS);

    private final DBDataTypeAggregationFunctionPanel m_typePanel =
            new DBDataTypeAggregationFunctionPanel(DBGroupByNodeModel2.CFG_TYPE_AGGREGATION_FUNCTIONS);

    private final SettingsModelBoolean m_addCountStar =
            new SettingsModelBoolean(DBGroupByNodeModel2.CFG_ADD_COUNT_STAR, false);

    private final SettingsModelString m_countStarColName = DBGroupByNodeModel2.createCountStarColNameModel();

    private final JPanel m_descriptionTab = new JPanel(new GridBagLayout());

    /**
     * Constructor for class GroupByNodeDialog.
     */
    @SuppressWarnings("unchecked")
    DBGroupByNodeDialog2() {
        m_addCountStar.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_countStarColName.setEnabled(m_addCountStar.getBooleanValue());
            }
        });
        m_countStarColName.setEnabled(m_addCountStar.getBooleanValue());
        //create the root tab
        m_tabs = new JTabbedPane();
        m_tabs.setBorder(BorderFactory.createTitledBorder(""));
        m_tabs.setOpaque(true);
        //The group column box
        m_groupCol =
            new DialogComponentColumnFilter(m_groupByCols, 0, false, new ColumnFilterPanel.ValueClassFilter(
                DataValue.class), false);
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
        final JPanel groupColPanel = m_groupCol.getComponentPanel();
        groupColPanel.setLayout(new GridLayout(1, 1));
        groupColPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Group settings "));
        m_tabs.addTab("Groups", groupColPanel);
        //The last tab: aggregations and advance settings
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        //The aggregation column box
        m_tabs.addTab(AggregationColumnPanel.DEFAULT_TITLE, m_aggregationPanel.getComponentPanel());
        m_tabs.addTab(PatternAggregationPanel.DEFAULT_TITLE, m_patternPanel.getComponentPanel());
        m_tabs.addTab(DataTypeAggregationPanel.DEFAULT_TITLE, m_typePanel.getComponentPanel());
        //calculate the component size
        int width =
            (int)Math.max(m_groupCol.getComponentPanel().getMinimumSize().getWidth(),
                m_aggregationPanel.getComponentPanel().getMinimumSize().getWidth());
        width = (int)Math.max(width, m_patternPanel.getComponentPanel().getMinimumSize().getWidth());
        width = Math.max(width, DEFAULT_WIDTH);
        final Dimension dimension = new Dimension(width, DEFAULT_HEIGHT);
        m_tabs.setMinimumSize(dimension);
        m_tabs.setPreferredSize(dimension);
        final JPanel topBottomPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        topBottomPanel.add(m_tabs, c);
        c.gridy++;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 0;
        topBottomPanel.add(createAdvancedOptionsPanel(), c);
        super.addTab("Settings", topBottomPanel);

        //add description tab
        m_descriptionTab.setMinimumSize(dimension);
        m_descriptionTab.setMaximumSize(dimension);
        m_descriptionTab.setPreferredSize(dimension);
        super.addTab("Description", m_descriptionTab);
    }

    private JComponent createAdvancedOptionsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Advanced settings "));
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.NONE;
        final DialogComponentStringSelection colNamePolicy =
            new DialogComponentStringSelection(m_columnNamePolicy, "Column naming:",
                ColumnNamePolicy.getPolicyLabels());
        panel.add(colNamePolicy.getComponentPanel(), c);
        c.gridx++;
        panel.add(new DialogComponentBoolean(m_addCountStar, "Add COUNT(*)").getComponentPanel(), c);
        c.gridx++;
        panel.add(
            new DialogComponentString(m_countStarColName, "column name: ", true, 15).getComponentPanel(), c);
        return panel;
    }

    /**
     * Synchronizes the available aggregation column list and the selected group columns.
     */
    private final void columnsChanged() {
        excludeColumns(m_groupByCols.getIncludeList());
    }

    /**
     * Synchronizes the available aggregation column list and the selected columns.
     *
     * @param columns the column that are changed and need to be excluded from the aggregation list
     */
    private void excludeColumns(final List<String> columns) {
        m_aggregationPanel.excludeColsChange(columns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        if (specs == null || specs.length < 1 || specs[0] == null) {
            throw new NotConfigurableException("No input connection found.");
        }
        final DatabasePortObjectSpec dbspec = (DatabasePortObjectSpec)specs[0];
        final DataTableSpec spec = dbspec.getDataTableSpec();
        try {
            final DatabaseQueryConnectionSettings connectionSettings = dbspec.getConnectionSettings(null);
            m_columnNamePolicy.loadSettingsFrom(settings);
            final String dbIdentifier = connectionSettings.getDatabaseIdentifier();
            m_aggregationPanel.loadSettingsFrom(settings, dbIdentifier, spec);
            m_patternPanel.loadSettingsFrom(settings, dbIdentifier, spec);
            m_typePanel.loadSettingsFrom(settings, dbIdentifier, spec);
            m_addCountStar.loadSettingsFrom(settings);
            m_countStarColName.loadSettingsFrom(settings);
            final DBAggregationFunctionProvider functionProvider =
                    new DBAggregationFunctionProvider(connectionSettings.getUtility());
            m_descriptionTab.removeAll();
            final GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;
            m_descriptionTab.add(functionProvider.getDescriptionPane(), c);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }
        m_groupCol.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        columnsChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        validateSettings(settings);
        m_groupCol.saveSettingsTo(settings);
        m_columnNamePolicy.saveSettingsTo(settings);
        m_aggregationPanel.saveSettingsTo(settings);
        m_patternPanel.saveSettingsTo(settings);
        m_typePanel.saveSettingsTo(settings);
        m_addCountStar.saveSettingsTo(settings);
        m_countStarColName.saveSettingsTo(settings);
    }

    private void validateSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
      //check if the dialog contains invalid group columns
        final Set<String> invalidInclCols = m_groupCol.getInvalidIncludeColumns();
        if (invalidInclCols != null && !invalidInclCols.isEmpty()) {
            throw new InvalidSettingsException(invalidInclCols.size() + " invalid group columns found.");
        }
        final ColumnNamePolicy columnNamePolicy =
                ColumnNamePolicy.getPolicy4Label(m_columnNamePolicy.getStringValue());
        if (columnNamePolicy == null) {
            throw new InvalidSettingsException("Invalid column name policy");
        }
        m_aggregationPanel.validate();
        m_patternPanel.validate();
        m_typePanel.validate();
    }
}
