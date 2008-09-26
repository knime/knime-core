/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *    27.08.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObjectSpec;

import org.knime.base.node.preproc.groupby.aggregation.AggregationMethod;
import org.knime.base.node.preproc.groupby.aggregation.ColumnAggregator;
import org.knime.base.node.preproc.groupby.dialogutil.AggregationColumnPanel;

import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * The node dialog of the group by node.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class GroupByNodeDialog extends NodeDialogPane {

    /**The width of the default component.*/
    public static final int COMPONENT_WIDTH = 680;

    /**The height of the default component.*/
    public static final int COMPONENT_HEIGHT = 550;

    private final JPanel m_panel;

    private final SettingsModelFilterString m_groupByCols =
        new SettingsModelFilterString(GroupByNodeModel.CFG_GROUP_BY_COLUMNS);

    private final SettingsModelIntegerBounded m_maxUniqueValues =
        new SettingsModelIntegerBounded(
                GroupByNodeModel.CFG_MAX_UNIQUE_VALUES, 10000, 1,
                Integer.MAX_VALUE);

    private final SettingsModelBoolean m_enableHilite =
        new SettingsModelBoolean(GroupByNodeModel.CFG_ENABLE_HILITE, false);

    private final SettingsModelBoolean m_sortInMemory =
        new SettingsModelBoolean(GroupByNodeModel.CFG_SORT_IN_MEMORY, false);

    private final SettingsModelBoolean m_keepColumnName =
        new SettingsModelBoolean(GroupByNodeModel.CFG_KEEP_COLUMN_NAME, false);

    private final DialogComponentColumnFilter m_groupColPanel;

    private final AggregationColumnPanel m_aggrColPanel =
        new AggregationColumnPanel();

    /**Constructor for class NewGroupByNodeDialogPane.
     *
     */
    public GroupByNodeDialog() {
        //create the root tab
        m_panel = new JPanel();
        final Dimension dimension =
            new Dimension(COMPONENT_WIDTH, COMPONENT_HEIGHT);
        m_panel.setMinimumSize(dimension);
        m_panel.setMaximumSize(dimension);
        m_panel.setPreferredSize(dimension);
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.Y_AXIS));
        addTab("Options", m_panel);

//The group column box
        m_groupColPanel = new DialogComponentColumnFilter(m_groupByCols, 0);
        m_groupColPanel.setIncludeTitle(" Group column(s) ");
        m_groupColPanel.setExcludeTitle(" Available column(s) ");
        m_groupByCols.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                //remove all group columns from the aggregation column list
                groupByColsChanged();
            }
        });
        final JPanel groupPanel =
            GroupByNodeDialog.createGroup(" Group settings ",
                m_groupColPanel.getComponentPanel());
        m_panel.add(groupPanel);

//The aggregation column box
        m_panel.add(m_aggrColPanel.getComponentPanel());

//The advanced settings box
        m_panel.add(createAdvancedOptionsBox());
    }

    private JComponent createAdvancedOptionsBox() {
        final Box box = new Box(BoxLayout.X_AXIS);
        box.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), " Advanced settings "));
        box.add(Box.createVerticalGlue());
        final DialogComponent maxNoneNumericVals =
            new DialogComponentNumber(m_maxUniqueValues,
                    "Maximum unique values per group", new Integer(1), 5);
        maxNoneNumericVals.setToolTipText("All groups with more unique values "
                + "will be skipped and replaced by a missing value");
        box.add(maxNoneNumericVals.getComponentPanel());
        box.add(Box.createVerticalGlue());

        final DialogComponent enableHilite = new DialogComponentBoolean(
                m_enableHilite, "Enable hiliting");
        box.add(enableHilite.getComponentPanel());
        box.add(Box.createVerticalGlue());

        final DialogComponent sortInMemory = new DialogComponentBoolean(
                m_sortInMemory, "Sort in memory");
        box.add(sortInMemory.getComponentPanel());
        box.add(Box.createVerticalGlue());

        final DialogComponent keepColName = new DialogComponentBoolean(
                m_keepColumnName, "Keep original column name(s)");
        box.add(keepColName.getComponentPanel());
        box.add(Box.createVerticalGlue());
        return box;
    }

    /**
     * Synchronizes the available aggregation column list and the
     * selected group columns.
     */
    void groupByColsChanged() {
        m_aggrColPanel.excludeColsChange(m_groupByCols.getIncludeList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        assert (specs.length == 1);
        final DataTableSpec spec = (DataTableSpec)specs[0];
        try {
            m_maxUniqueValues.loadSettingsFrom(settings);
            m_enableHilite.loadSettingsFrom(settings);
            m_sortInMemory.loadSettingsFrom(settings);
            m_keepColumnName.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }
        m_groupColPanel.loadSettingsFrom(settings, new DataTableSpec[] {spec});
        try {
            m_aggrColPanel.loadSettingsFrom(settings, spec);
        } catch (final InvalidSettingsException e) {
            m_aggrColPanel.initialize(spec, getColumnMethods(spec,
                    m_groupByCols.getIncludeList(), settings));
        }
        groupByColsChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_groupColPanel.saveSettingsTo(settings);
        m_maxUniqueValues.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
        m_sortInMemory.saveSettingsTo(settings);
        m_keepColumnName.saveSettingsTo(settings);
        m_aggrColPanel.saveSettingsTo(settings);
    }

    /**
     * @param title the title of the group
     * @param component the component to add to the group
     * @return the {@link JPanel} of the group
     */
    public static JPanel createGroup(final String title,
            final JComponent component) {
        final JPanel subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
        subPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), title));
        subPanel.add(component);
        return subPanel;
    }

    /**
     * Helper method to get the aggregation methods for the old node settings.
     * @param spec the input {@link DataTableSpec}
     * @param excludeCols the columns that should be excluded from the
     * aggregation columns
     * @param config the config object to read from
     * @return the {@link ColumnAggregator}s
     */
    public static List<ColumnAggregator> getColumnMethods(
            final DataTableSpec spec, final List<String> excludeCols,
            final ConfigRO config) {
        String numeric = null;
        String nominal = null;
        try {
            numeric =
                config.getString(GroupByNodeModel.OLD_CFG_NUMERIC_COL_METHOD);
            nominal =
                config.getString(GroupByNodeModel.OLD_CFG_NOMINAL_COL_METHOD);
        } catch (final InvalidSettingsException e) {
            numeric = AggregationMethod.getDefaultNumericMethod().getLabel();
            nominal = AggregationMethod.getDefaultNominalMethod().getLabel();
        }
        return GroupByNodeModel.createColumnAggregators(spec, excludeCols,
                numeric, nominal);
    }
}
