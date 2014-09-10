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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.dialogutil.column.AggregationColumnPanel;
import org.knime.base.data.aggregation.dialogutil.pattern.PatternAggregationPanel;
import org.knime.base.data.aggregation.dialogutil.type.DataTypeAggregationPanel;
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
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node dialog of the Moving Aggregation node.
 *  @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 *  @since 2.10
 */
public class MovingAggregationNodeDialog extends NodeDialogPane {

    private final AggregationColumnPanel m_aggrColPanel = new AggregationColumnPanel(null);

    private final DataTypeAggregationPanel m_dataTypeAggrPanel =
            new DataTypeAggregationPanel(MovingAggregationNodeModel.CFG_DATA_TYPE_AGGREGATORS);

    private final PatternAggregationPanel m_patternAggrPanel =
            new PatternAggregationPanel(MovingAggregationNodeModel.CFG_PATTERN_AGGREGATORS);

    private SettingsModelBoolean m_handleMissingsModel = MovingAggregationNodeModel.createHandleMissingsModel();
    private final DialogComponent m_handleMissings = new DialogComponentBoolean(m_handleMissingsModel,
        "Resolve missing values for incomplete windows");

    private final SettingsModelBoolean m_cumulativeCompModel =
            MovingAggregationNodeModel.createCumulativeComputingModel();

    private final DialogComponent m_cumulativeComp =
            new DialogComponentBoolean(m_cumulativeCompModel, "Cumulative computation");

    private final SettingsModelInteger m_winLengthModel = MovingAggregationNodeModel.createWindowLengthModel();

    private final SettingsModelString m_windowTypeModel = MovingAggregationNodeModel.createWindowTypeModel();
    private final DialogComponent m_windowType = new DialogComponentButtonGroup(m_windowTypeModel, "Window type",
        false, WindowType.values());

    private final DialogComponent m_winLength = new DialogComponentNumberEdit(m_winLengthModel, "Window length", 15);

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
                m_windowTypeModel.setEnabled(enabled);
            }
        });
        final JPanel generalSettingsBox = createGeneralSettingsPanel();
        final JPanel aggregationBox = createAgggregationPanel();
        JPanel rootPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        rootPanel.add(generalSettingsBox, c);
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 1;
        rootPanel.add(aggregationBox, c);
        addTab("Settings", rootPanel);
      //calculate the component size
        final int width = (int)m_aggrColPanel.getComponentPanel().getMinimumSize().getWidth();
        final Dimension dimension = new Dimension(width, GroupByNodeDialog.DEFAULT_HEIGHT);
      //add description tab
        final Component descriptionTab = AggregationMethods.createDescriptionPane();
        descriptionTab.setMinimumSize(dimension);
        descriptionTab.setMaximumSize(dimension);
        descriptionTab.setPreferredSize(dimension);
        super.addTab("Description", descriptionTab);
    }

    private JPanel createGeneralSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " General settings "));
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridy = 0;
        c.gridx = 0;
        panel.add(m_windowType.getComponentPanel(), c);
        c.gridx++;
        panel.add(m_winLength.getComponentPanel(), c);
        c.gridx++;
        panel.add(m_handleMissings.getComponentPanel(), c);
        c.gridx++;
        c.weightx = 1;
        panel.add(new JLabel(), c);
        c.weightx = 0;
        c.gridy++;
        c.gridx = 0;
        panel.add(m_cumulativeComp.getComponentPanel(), c);
        c.gridx++;
        panel.add(m_removeAggrCols.getComponentPanel(), c);
        c.gridx++;
        panel.add(m_removeRetainedCols.getComponentPanel(), c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 1;
        panel.add(new JLabel(), c);
        return panel;
    }

    private JPanel createAgggregationPanel() {
        final JTabbedPane aggregationPanel = new JTabbedPane();
        aggregationPanel.add(AggregationColumnPanel.DEFAULT_TITLE, m_aggrColPanel.getComponentPanel());
        aggregationPanel.add(PatternAggregationPanel.DEFAULT_TITLE, m_patternAggrPanel.getComponentPanel());
        aggregationPanel.add(DataTypeAggregationPanel.DEFAULT_TITLE, m_dataTypeAggrPanel.getComponentPanel());
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Aggregation settings "));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = 0;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 4;
        panel.add(aggregationPanel, c);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridy++;
        m_maxNoneNumericVals.setToolTipText("All groups with more unique values "
                + "will be skipped and replaced by a missing value");
        panel.add(m_maxNoneNumericVals.getComponentPanel(), c);
        c.gridx++;
        panel.add(m_valueDelimiter.getComponentPanel(), c);
        c.gridx++;
        panel.add(m_columnNamePolicy.getComponentPanel(), c);
        c.gridx++;
        c.weightx = 1;
        panel.add(new JLabel(), c);
        return panel;
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
        m_windowType.loadSettingsFrom(settings, specs);
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
        try {
            m_patternAggrPanel.loadSettingsFrom(settings, specs[0]);
            m_dataTypeAggrPanel.loadSettingsFrom(settings, specs[0]);
        } catch (InvalidSettingsException e) {
            //introduced in 2.11
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        validateSettings(settings);
        m_winLength.saveSettingsTo(settings);
        m_windowType.saveSettingsTo(settings);
        m_cumulativeComp.saveSettingsTo(settings);
        m_handleMissings.saveSettingsTo(settings);
        m_removeRetainedCols.saveSettingsTo(settings);
        m_removeAggrCols.saveSettingsTo(settings);
        m_columnNamePolicy.saveSettingsTo(settings);
        m_valueDelimiter.saveSettingsTo(settings);
        m_maxNoneNumericVals.saveSettingsTo(settings);
        m_aggrColPanel.saveSettingsTo(settings);
        m_patternAggrPanel.saveSettingsTo(settings);
        m_dataTypeAggrPanel.saveSettingsTo(settings);
    }

    private void validateSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_aggrColPanel.validate();
        m_patternAggrPanel.validate();
        m_dataTypeAggrPanel.validate();
    }
}
