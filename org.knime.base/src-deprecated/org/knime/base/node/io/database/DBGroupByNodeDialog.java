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
package org.knime.base.node.io.database;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * The node dialog of the group by node.
 *
 * @author Tobias Koetter, University of Konstanz
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
final class DBGroupByNodeDialog extends NodeDialogPane {

    private static final int DEFAULT_WIDTH = 680;

    private static final int DEFAULT_HEIGHT = 350;

    private final JTabbedPane m_tabs;

    private final SettingsModelFilterString m_groupByCols = new SettingsModelFilterString(
        DBGroupByNodeModel.CFG_GROUP_BY_COLUMNS);

    private final SettingsModelString m_columnNamePolicy = new SettingsModelString(
        DBGroupByNodeModel.CFG_COLUMN_NAME_POLICY, ColumnNamePolicy.getDefault().getLabel());

    private final DialogComponentColumnFilter m_groupCol;

    private final DBGroupByAggregationPanel m_aggregationPanel = new DBGroupByAggregationPanel();

    /**
     * Constructor for class GroupByNodeDialog.
     */
    @SuppressWarnings("unchecked")
    DBGroupByNodeDialog() {
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
        panel.add(m_aggregationPanel);
        //The advanced settings box
        final JComponent advancedBox = createAdvancedOptionsBox();
        panel.add(advancedBox);
        m_tabs.addTab("Options", panel);
        //calculate the component size
        int width =
            (int)Math.max(m_groupCol.getComponentPanel().getMinimumSize().getWidth(), m_aggregationPanel
                .getMinimumSize().getWidth());
        width = (int)Math.max(width, advancedBox.getMinimumSize().getWidth());
        width = Math.max(width, DEFAULT_WIDTH);
        final Dimension dimension = new Dimension(width, DEFAULT_HEIGHT);
        m_tabs.setMinimumSize(dimension);
        m_tabs.setPreferredSize(dimension);
        super.addTab("Settings", m_tabs);
        //add description tab
        final Component descriptionTab = AggregationMethods.createDescriptionPane();
        descriptionTab.setMinimumSize(dimension);
        descriptionTab.setMaximumSize(dimension);
        descriptionTab.setPreferredSize(dimension);
        super.addTab("Description", descriptionTab);
    }

    private JComponent createAdvancedOptionsBox() {
        //general option box
        final Box generalBox = new Box(BoxLayout.X_AXIS);
        final DialogComponentStringSelection colNamePolicy =
            new DialogComponentStringSelection(m_columnNamePolicy, "Column naming:",
                ColumnNamePolicy.getPolicyLabels());
        generalBox.add(colNamePolicy.getComponentPanel());
        //Advanced settings box
        final Box box = new Box(BoxLayout.Y_AXIS);
        box.add(generalBox);
        box.setMaximumSize(box.getPreferredSize());
        final Box rootBox = new Box(BoxLayout.X_AXIS);
        rootBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Advanced settings "));
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(box);
        rootBox.add(Box.createHorizontalGlue());
        return rootBox;
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
            throw new NotConfigurableException("No input spec available");
        }
        final DatabasePortObjectSpec dbspec = (DatabasePortObjectSpec)specs[0];
        final DataTableSpec spec = dbspec.getDataTableSpec();
        try {
            m_columnNamePolicy.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }
        m_aggregationPanel.loadSettingsFrom(settings, dbspec, spec);
        m_groupCol.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        columnsChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        //check if the dialog contains invalid group columns
        final Set<String> invalidInclCols = m_groupCol.getInvalidIncludeColumns();
        if (invalidInclCols != null && !invalidInclCols.isEmpty()) {
            throw new InvalidSettingsException(invalidInclCols.size() + " invalid group columns found.");
        }
        m_groupCol.saveSettingsTo(settings);
        m_columnNamePolicy.saveSettingsTo(settings);
        m_aggregationPanel.saveSettingsTo(settings);
    }
}
