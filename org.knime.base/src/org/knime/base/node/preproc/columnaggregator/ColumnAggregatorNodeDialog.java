/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 */

package org.knime.base.node.preproc.columnaggregator;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
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
import org.knime.base.data.aggregation.dialogutil.ColumnAggregationPanel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;


/**
 * {@link NodeDialogPane} of the column aggregator node.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class ColumnAggregatorNodeDialog  extends NodeDialogPane {

    /**The width of the default component.*/
    public static final int DEFAULT_WIDTH = 650;

    /**The height of the default component.*/
    public static final int DEFAULT_HEIGHT = 350;

    private final JTabbedPane m_tabs;

    private final SettingsModelColumnFilter2 m_aggregationCols =
        ColumnAggregatorNodeModel.createAggregationColsModel();

    private final SettingsModelIntegerBounded m_maxUniqueValues =
        ColumnAggregatorNodeModel.createMaxUniqueValsModel();

    private final SettingsModelString m_valueDelimiter =
        ColumnAggregatorNodeModel.createValueDelimiterModel();

    private final SettingsModelBoolean m_removeRetainedCols =
        ColumnAggregatorNodeModel.createRemoveRetainedColsModel();

    private final SettingsModelBoolean m_removeAgregationCols =
        ColumnAggregatorNodeModel.createRemoveAggregationColsModel();

    private final DialogComponentColumnFilter2 m_aggrColumnsComponent;

    private final ColumnAggregationPanel m_aggrMethodsPanel =
        new ColumnAggregationPanel(" Aggregation settings ");

    private final Collection<DialogComponent> m_components =
        new LinkedList<DialogComponent>();

    private DataTableSpec m_spec = null;

    /**Constructor for class ColumnAggregatorNodeDialog. */
    public ColumnAggregatorNodeDialog() {
//create the root tab
        m_tabs = new JTabbedPane();
        m_tabs.setBorder(BorderFactory.createTitledBorder(""));
        m_tabs.setOpaque(true);

//The group column box
        m_aggrColumnsComponent =
            new DialogComponentColumnFilter2(m_aggregationCols, 0);
        m_components.add(m_aggrColumnsComponent);
        m_aggrColumnsComponent.setIncludeTitle(" Aggregation column(s) ");
        m_aggrColumnsComponent.setExcludeTitle(" Available column(s) ");
        m_aggregationCols.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                //remove all group columns from the aggregation column list
                columnsChanged();
            }
        });
        final JPanel aggrColumnsPanel =
            m_aggrColumnsComponent.getComponentPanel();
        aggrColumnsPanel.setLayout(new GridLayout(1, 1));
        m_tabs.addTab("Columns", aggrColumnsPanel);

//The last tab: aggregations and advance settings
        final JPanel aggrMethodPanel = new JPanel();
        aggrMethodPanel.setLayout(
                new BoxLayout(aggrMethodPanel, BoxLayout.Y_AXIS));

//The aggregation column box
        aggrMethodPanel.add(m_aggrMethodsPanel.getComponentPanel());

//The advanced settings box
        final JComponent advancedBox = createAdvancedOptionsBox();
        aggrMethodPanel.add(advancedBox);
        m_tabs.addTab("Options", aggrMethodPanel);

//calculate the component size
        int width = (int)Math.max(m_aggrColumnsComponent.getComponentPanel().
                getMinimumSize().getWidth(), m_aggrMethodsPanel.
                    getComponentPanel().getMinimumSize().getWidth());
        width = (int)Math.max(width, advancedBox.getMinimumSize().getWidth());
        width = Math.max(width, DEFAULT_WIDTH);
        final Dimension dimension =
            new Dimension(width, DEFAULT_HEIGHT);
        //ensure that the advanced box has the same size as the method panel
        final Dimension advancedDim = new Dimension(width,
                advancedBox.getPreferredSize().height);
        advancedBox.setMinimumSize(advancedDim);
        advancedBox.setPreferredSize(advancedDim);
        advancedBox.setMaximumSize(advancedDim);
        m_tabs.setMinimumSize(dimension);
        m_tabs.setPreferredSize(dimension);
        super.addTab("Settings", m_tabs);

//add description tab
        final Component descriptionTab =
            AggregationMethods.createDescriptionPane();
        descriptionTab.setMinimumSize(dimension);
        descriptionTab.setMaximumSize(dimension);
        descriptionTab.setPreferredSize(dimension);
        super.addTab("Description", descriptionTab);
    }

    /**
     * Add additional panel (i.e. for pivoting) to this dialog.
     * @param p the panel to add to tabs
     * @param title the title for the new tab
     */
    protected final void addPanel(final JPanel p, final String title) {
        final int tabSize = m_tabs.getComponentCount();
        m_tabs.insertTab(title, null, p, null, tabSize - 1);
    }

    private JComponent createAdvancedOptionsBox() {
//general option box
        final DialogComponentBoolean removeAggregationCols =
            new DialogComponentBoolean(m_removeAgregationCols,
                    "Remove aggregation columns");
        m_components.add(removeAggregationCols);
        final DialogComponentBoolean removeRetainedCols =
            new DialogComponentBoolean(m_removeRetainedCols,
            "Remove retained columns");
        m_components.add(removeRetainedCols);
        final DialogComponent maxNoneNumericVals =
            new DialogComponentNumber(m_maxUniqueValues,
                    "Maximum unique values per row", new Integer(1000), 5);
        m_components.add(maxNoneNumericVals);
        maxNoneNumericVals.setToolTipText("All rows with more unique values "
                + "will be skipped and replaced by a missing value");
        final DialogComponentString valueDelimiter = new DialogComponentString(
                m_valueDelimiter, "Value delimiter", false, 5);
        m_components.add(valueDelimiter);


        final Box upperBox = new Box(BoxLayout.X_AXIS);
        upperBox.add(Box.createGlue());
        upperBox.add(removeAggregationCols.getComponentPanel());
        upperBox.add(removeRetainedCols.getComponentPanel());
        upperBox.add(Box.createGlue());

        final Box lowerBox = new Box(BoxLayout.X_AXIS);
        lowerBox.add(Box.createGlue());
        lowerBox.add(maxNoneNumericVals.getComponentPanel());
        lowerBox.add(valueDelimiter.getComponentPanel());
        lowerBox.add(Box.createGlue());

        final Box generalBox = new Box(BoxLayout.Y_AXIS);
        generalBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), " Advanced settings "));
        generalBox.add(upperBox);
        generalBox.add(lowerBox);
        return generalBox;
    }

    /**
     * Synchronizes the available aggregation column list and the
     * selected group columns.
     */
    protected final void columnsChanged() {
        m_aggrMethodsPanel.updateType(getSuperType());
    }

    /**
     * @return the {@link DataType} the selected columns are compatible with
     * or <code>null</code> if none is available or no table exists
     */
    private DataType getSuperType() {
        final FilterResult filterResult = m_aggregationCols.applyTo(m_spec);
        final List<String> includeList =
            Arrays.asList(filterResult.getIncludes());
        if (includeList == null || includeList.isEmpty()
                || m_spec == null) {
            return null;
        }
        final Set<DataType> types =
            new HashSet<DataType>(includeList.size());
        final HashSet<String> inclCols = new HashSet<String>(includeList);
        for (final DataColumnSpec colSpec : m_spec) {
            if (inclCols.contains(colSpec.getName())) {
                types.add(colSpec.getType());
            }
        }
        final DataType superType = CollectionCellFactory.getElementType(
                types.toArray(new DataType[0]));
        return superType;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        assert (specs.length == 1);
        m_spec = (DataTableSpec)specs[0];
        for (final DialogComponent component : m_components) {
            component.loadSettingsFrom(settings, specs);
        }
        try {
            m_aggrMethodsPanel.loadSettingsFrom(settings.getNodeSettings(
                    ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS),
                    getSuperType(), m_spec);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        for (final DialogComponent component : m_components) {
            component.saveSettingsTo(settings);
        }
        m_aggrMethodsPanel.saveSettingsTo(settings.addNodeSettings(
                ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS));
    }
}
