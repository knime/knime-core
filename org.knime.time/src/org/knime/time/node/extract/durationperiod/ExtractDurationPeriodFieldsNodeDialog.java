/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 9, 2017 (simon): created
 */
package org.knime.time.node.extract.durationperiod;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.time.util.Granularity;

/**
 * The node dialog of the node which extracts duration or period fields.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class ExtractDurationPeriodFieldsNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnNameSelection m_dialogCompColSelect;

    private final DialogComponentBoolean[] m_dialogCompsDurationFields;

    private final DialogComponentBoolean[] m_dialogCompsPeriodFields;

    private final DialogComponentBoolean m_dialogCompSubSecond;

    private final DialogComponentStringSelection m_dialogCompSubsecondUnits;

    /**
     * Creates a new dialog.
     */
    @SuppressWarnings("unchecked")
    public ExtractDurationPeriodFieldsNodeDialog() {
        m_dialogCompColSelect =
            new DialogComponentColumnNameSelection(ExtractDurationPeriodFieldsNodeModel.createColSelectModel(),
                "Duration column", 0, true, DurationValue.class, PeriodValue.class);

        final String[] fieldsDuration =
            new String[]{Granularity.HOUR.toString(), Granularity.MINUTE.toString(), Granularity.SECOND.toString()};
        m_dialogCompsDurationFields = new DialogComponentBoolean[fieldsDuration.length];
        for (int i = 0; i < fieldsDuration.length; i++) {
            final String gran = fieldsDuration[i];
            m_dialogCompsDurationFields[i] =
                new DialogComponentBoolean(ExtractDurationPeriodFieldsNodeModel.createFieldBooleanModel(gran), gran);
        }

        final String[] fieldsPeriod =
            new String[]{Granularity.YEAR.toString(), Granularity.MONTH.toString(), Granularity.DAY.toString()};
        m_dialogCompsPeriodFields = new DialogComponentBoolean[fieldsPeriod.length];
        for (int i = 0; i < fieldsPeriod.length; i++) {
            final String gran = fieldsPeriod[i];
            m_dialogCompsPeriodFields[i] =
                new DialogComponentBoolean(ExtractDurationPeriodFieldsNodeModel.createFieldBooleanModel(gran), gran);
        }

        final String[] subsecondUnits = new String[]{Granularity.MILLISECOND.toString(),
            Granularity.MICROSECOND.toString(), Granularity.NANOSECOND.toString()};

        final SettingsModelBoolean subSecondModel =
            ExtractDurationPeriodFieldsNodeModel.createFieldBooleanModel("subsecond");
        m_dialogCompSubSecond = new DialogComponentBoolean(subSecondModel, "Subseconds in");

        m_dialogCompSubsecondUnits = new DialogComponentStringSelection(
            ExtractDurationPeriodFieldsNodeModel.createSubsecondUnitsModel(subSecondModel), null, subsecondUnits);

        /*
         * create panel with gbc
         */
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;

        /*
         * add column selection
         */
        final JPanel panelColSelect = new JPanel(new GridBagLayout());
        panelColSelect.setBorder(BorderFactory.createTitledBorder("Column Selection"));
        final GridBagConstraints gbcColSelect = new GridBagConstraints();
        gbcColSelect.insets = new Insets(5, 5, 5, 5);
        gbcColSelect.fill = GridBagConstraints.VERTICAL;
        gbcColSelect.gridx = 0;
        gbcColSelect.gridy = 0;
        gbcColSelect.anchor = GridBagConstraints.WEST;
        gbcColSelect.weightx = 1;
        panelColSelect.add(m_dialogCompColSelect.getComponentPanel(), gbcColSelect);
        panel.add(panelColSelect, gbc);

        /*
         * add field selection
         */
        gbc.gridy++;
        gbc.weighty = 1;
        final JPanel switchingPanel = new JPanel(new CardLayout());
        panel.add(switchingPanel, gbc);
        switchingPanel.setBorder(BorderFactory.createTitledBorder("Field Selection"));
        final GridBagConstraints gbcSwitchingPanels = new GridBagConstraints();
        gbcSwitchingPanels.fill = GridBagConstraints.VERTICAL;
        gbcSwitchingPanels.gridx = 0;
        gbcSwitchingPanels.gridy = 0;
        gbcSwitchingPanels.weightx = 0;
        gbcSwitchingPanels.anchor = GridBagConstraints.WEST;
        /*
         * panel containing checkboxes of duration fields
         */
        final JPanel panelDurationCheck = new JPanel(new GridBagLayout());
        gbcSwitchingPanels.gridy = 0;
        gbcSwitchingPanels.weighty = 0;
        for (final DialogComponentBoolean dc : m_dialogCompsDurationFields) {
            panelDurationCheck.add(dc.getComponentPanel(), gbcSwitchingPanels);
            gbcSwitchingPanels.gridy++;
        }
        panelDurationCheck.add(m_dialogCompSubSecond.getComponentPanel(), gbcSwitchingPanels);
        gbcSwitchingPanels.gridx++;
        gbcSwitchingPanels.weightx = 1;
        gbcSwitchingPanels.weighty = 1;
        panelDurationCheck.add(m_dialogCompSubsecondUnits.getComponentPanel(), gbcSwitchingPanels);
        switchingPanel.add(panelDurationCheck, DurationCellFactory.TYPE.toString());
        /*
         * panel containing checkboxes of period fields
         */
        final JPanel panelPeriodCheck = new JPanel(new GridBagLayout());
        gbcSwitchingPanels.gridy = 0;
        gbcSwitchingPanels.weighty = 0;
        for (final DialogComponentBoolean dc : m_dialogCompsPeriodFields) {
            if (dc == m_dialogCompsPeriodFields[m_dialogCompsPeriodFields.length - 1]) {
                gbcSwitchingPanels.weighty = 1;
            }
            panelPeriodCheck.add(dc.getComponentPanel(), gbcSwitchingPanels);
            gbcSwitchingPanels.gridy++;
        }
        switchingPanel.add(panelPeriodCheck, PeriodCellFactory.TYPE.toString());

        /*
         * add tab
         */
        addTab("Options", panel);

        m_dialogCompColSelect.getModel().addChangeListener(l -> {
            if (m_dialogCompColSelect.getSelectedAsSpec() != null) {
                ((CardLayout)switchingPanel.getLayout()).show(switchingPanel,
                    m_dialogCompColSelect.getSelectedAsSpec().getType().toString());
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dialogCompColSelect.saveSettingsTo(settings);
        for (final DialogComponentBoolean dc : m_dialogCompsDurationFields) {
            dc.saveSettingsTo(settings);
        }
        for (final DialogComponentBoolean dc : m_dialogCompsPeriodFields) {
            dc.saveSettingsTo(settings);
        }
        m_dialogCompSubSecond.saveSettingsTo(settings);
        m_dialogCompSubsecondUnits.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_dialogCompColSelect.loadSettingsFrom(settings, specs);
        for (final DialogComponentBoolean dc : m_dialogCompsDurationFields) {
            dc.loadSettingsFrom(settings, specs);
        }
        for (final DialogComponentBoolean dc : m_dialogCompsPeriodFields) {
            dc.loadSettingsFrom(settings, specs);
        }
        m_dialogCompSubSecond.loadSettingsFrom(settings, specs);
        m_dialogCompSubsecondUnits.loadSettingsFrom(settings, specs);
    }
}
