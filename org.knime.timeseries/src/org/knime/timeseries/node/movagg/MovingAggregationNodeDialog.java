/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.04.2014 (koetter): created
 */
package org.knime.timeseries.node.movagg;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.dialogutil.AggregationColumnPanel;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.GroupByNodeDialog;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

/**
 * Node dialog of the Moving Aggregation node.
 *  @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 *  @since 2.10
 */
public class MovingAggregationNodeDialog extends NodeDialogPane {

    private final AggregationColumnPanel m_aggrColPanel = new AggregationColumnPanel(null);

    private SettingsModelBoolean m_handleMissingsModel = MovingAggregationNodeModel.createHandleMissingsModel();
    private final DialogComponent m_handleMissings = new DialogComponentBoolean(m_handleMissingsModel,
        "Resolve missing values in the beginning");

    private final SettingsModelBoolean m_cumulativeCompModel =
            MovingAggregationNodeModel.createCumulativeComputingModel();

    private final DialogComponent m_cumulativeComp =
            new DialogComponentBoolean(m_cumulativeCompModel, "Cumulative computation");

    private final SettingsModelInteger m_winLengthModel = MovingAggregationNodeModel.createWindowLengthModel();

    private final DialogComponent m_winLength = new DialogComponentNumberEdit(m_winLengthModel, "Window length", 8);

    private final DialogComponent m_removeRetainedCols = new DialogComponentBoolean(
        MovingAggregationNodeModel.createRemoveRetainedColsModel(), "Remove retained columns");

    private final DialogComponent m_removeAggrCols = new DialogComponentBoolean(
        MovingAggregationNodeModel.createRemoveAggregationColsModel(), "Remove aggregation columns");

    private final DialogComponent m_columnNamePolicy = new DialogComponentStringSelection(
        MovingAggregationNodeModel.createColNamePolicyModel(), "Column naming", ColumnNamePolicy.getPolicyLabels());

    private final DialogComponent m_maxNoneNumericVals =
            new DialogComponentNumber(MovingAggregationNodeModel.createMaxUniqueValModel(),
                "Maximum unique values per group", new Integer(1000), 5);

    private final DialogComponentString m_valueDelimiter = new DialogComponentString(
        MovingAggregationNodeModel.createValueDelimiterModel(), "Value delimiter", false, 3);

    /**
     * Constructor.
     */
    public MovingAggregationNodeDialog() {
        m_cumulativeCompModel.addChangeListener(new ChangeListener() {
            /**{@inheritDoc}*/
            @Override
            public void stateChanged(final ChangeEvent e) {
                final boolean enabled = !m_cumulativeCompModel.getBooleanValue();
                m_winLengthModel.setEnabled(enabled);
                m_handleMissingsModel.setEnabled(enabled);
            }
        });
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        final Box generalSettingsBox = new Box(BoxLayout.X_AXIS);
        generalSettingsBox.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " General settings "));
        generalSettingsBox.add(m_winLength.getComponentPanel());
        generalSettingsBox.add(m_handleMissings.getComponentPanel());
        generalSettingsBox.add(m_cumulativeComp.getComponentPanel());
        generalSettingsBox.add(m_removeAggrCols.getComponentPanel());
        generalSettingsBox.add(m_removeRetainedCols.getComponentPanel());

        final Box subAggregationBox = new Box(BoxLayout.X_AXIS);
        m_maxNoneNumericVals.setToolTipText("All groups with more unique values "
                    + "will be skipped and replaced by a missing value");
        subAggregationBox.add(m_maxNoneNumericVals.getComponentPanel());
        subAggregationBox.add(m_valueDelimiter.getComponentPanel());
        subAggregationBox.add(m_columnNamePolicy.getComponentPanel());

        final Box aggregationBox = new Box(BoxLayout.Y_AXIS);
        aggregationBox.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Aggregation settings "));
        aggregationBox.add(m_aggrColPanel.getComponentPanel());
        aggregationBox.add(subAggregationBox);

        rootPanel.add(generalSettingsBox);
        rootPanel.add(aggregationBox);
        addTab("Settings", rootPanel);
      //calculate the component size
        int width = (int)m_aggrColPanel.getComponentPanel().getMinimumSize().getWidth();
        width = Math.max(width, GroupByNodeDialog.DEFAULT_WIDTH);
        final Dimension dimension =
            new Dimension(width, GroupByNodeDialog.DEFAULT_HEIGHT);
      //add description tab
        final Component descriptionTab = AggregationMethods.createDescriptionPane();
        descriptionTab.setMinimumSize(dimension);
        descriptionTab.setMaximumSize(dimension);
        descriptionTab.setPreferredSize(dimension);
        super.addTab("Description", descriptionTab);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        if (specs == null || specs.length != 1) {
            return;
        }
        m_winLength.loadSettingsFrom(settings, specs);
        m_cumulativeComp.loadSettingsFrom(settings, specs);
        m_handleMissings.loadSettingsFrom(settings, specs);
        m_removeRetainedCols.loadSettingsFrom(settings, specs);
        m_removeAggrCols.loadSettingsFrom(settings, specs);
        m_columnNamePolicy.loadSettingsFrom(settings, specs);
        m_valueDelimiter.loadSettingsFrom(settings, specs);
        m_maxNoneNumericVals.loadSettingsFrom(settings, specs);
        try {
            m_aggrColPanel.loadSettingsFrom(settings, specs[0]);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_winLength.saveSettingsTo(settings);
        m_cumulativeComp.saveSettingsTo(settings);
        m_handleMissings.saveSettingsTo(settings);
        m_removeRetainedCols.saveSettingsTo(settings);
        m_removeAggrCols.saveSettingsTo(settings);
        m_columnNamePolicy.saveSettingsTo(settings);
        m_valueDelimiter.saveSettingsTo(settings);
        m_maxNoneNumericVals.saveSettingsTo(settings);
        m_aggrColPanel.saveSettingsTo(settings);
    }
}
