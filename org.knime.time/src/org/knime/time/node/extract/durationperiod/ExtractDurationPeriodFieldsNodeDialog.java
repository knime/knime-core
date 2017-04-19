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
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.time.Granularity;

/**
 * The node dialog of the node which extracts duration or period fields.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class ExtractDurationPeriodFieldsNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnNameSelection m_dialogCompColSelect;

    private final DialogComponentButtonGroup m_dialogCompModSelect;

    private final DialogComponentStringSelection m_dialogCompDurationFieldSelect;

    private final DialogComponentStringSelection m_dialogCompPeriodFieldSelect;

    private final DialogComponentBoolean[] m_dialogCompsDurationFields;

    private final DialogComponentBoolean[] m_dialogCompsPeriodFields;

    /**
     * Creates a new dialog.
     */
    @SuppressWarnings("unchecked")
    public ExtractDurationPeriodFieldsNodeDialog() {
        m_dialogCompColSelect =
            new DialogComponentColumnNameSelection(ExtractDurationPeriodFieldsNodeModel.createColSelectModel(),
                "Duration or Period Column", 0, true, DurationValue.class, PeriodValue.class);

        final SettingsModelString modusSelectModel = ExtractDurationPeriodFieldsNodeModel.createModSelectModel();
        m_dialogCompModSelect = new DialogComponentButtonGroup(modusSelectModel, true, null,
            ExtractDurationPeriodFieldsNodeModel.MODUS_SINGLE, ExtractDurationPeriodFieldsNodeModel.MODUS_SEVERAL);

        final String[] fieldsDuration = new String[]{Granularity.HOUR.toString(), Granularity.MINUTE.toString(),
            Granularity.SECOND.toString(), Granularity.MILLISECOND.toString(), Granularity.NANOSECOND.toString()};
        m_dialogCompDurationFieldSelect =
            new DialogComponentStringSelection(ExtractDurationPeriodFieldsNodeModel.createDurationFieldSelectionModel(),
                "Field:", fieldsDuration);

        m_dialogCompPeriodFieldSelect =
            new DialogComponentStringSelection(ExtractDurationPeriodFieldsNodeModel.createPeriodFieldSelectionModel(),
                "Field:", new String[]{Granularity.YEAR.toString(), Granularity.MONTH.toString()});

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
         * add modus selection
         */
        final JPanel panelFieldSelect = new JPanel(new GridBagLayout());
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(panelFieldSelect, gbc);
        panelFieldSelect.setBorder(BorderFactory.createTitledBorder("Field Selection"));
        final GridBagConstraints gbcFieldSelect = new GridBagConstraints();
        gbcFieldSelect.insets = new Insets(5, 5, 5, 5);
        gbcFieldSelect.fill = GridBagConstraints.VERTICAL;
        gbcFieldSelect.gridx = 0;
        gbcFieldSelect.gridy = 0;
        gbcFieldSelect.weighty = 1;
        gbcFieldSelect.anchor = GridBagConstraints.WEST;

        panelFieldSelect.add(m_dialogCompModSelect.getComponentPanel(), gbcFieldSelect);

        final JPanel switchingPanel = new JPanel(new CardLayout());
        gbcFieldSelect.gridx++;
        gbcFieldSelect.weightx = 1;
        gbcFieldSelect.insets = new Insets(5, 40, 5, 5);
        panelFieldSelect.add(switchingPanel, gbcFieldSelect);

        final GridBagConstraints gbcSwitchingPanels = new GridBagConstraints();
        gbcSwitchingPanels.fill = GridBagConstraints.VERTICAL;
        gbcSwitchingPanels.gridx = 0;
        gbcSwitchingPanels.gridy = 0;
        gbcSwitchingPanels.weightx = 1;
        gbcSwitchingPanels.anchor = GridBagConstraints.WEST;
        /*
         * panel containing combobox of duration fields
         */
        final JPanel panelDurationCombo = new JPanel(new GridBagLayout());
        gbcSwitchingPanels.weighty = 1;
        panelDurationCombo.add(m_dialogCompDurationFieldSelect.getComponentPanel(), gbcSwitchingPanels);
        switchingPanel.add(panelDurationCombo,
            DurationCellFactory.TYPE.toString() + ExtractDurationPeriodFieldsNodeModel.MODUS_SINGLE);
        /*
         * panel containing checkboxes of duration fields
         */
        final JPanel panelDurationCheck = new JPanel(new GridBagLayout());
        gbcSwitchingPanels.gridy = 0;
        gbcSwitchingPanels.weighty = 0;
        for (final DialogComponentBoolean dc : m_dialogCompsDurationFields) {
            if (dc == m_dialogCompsDurationFields[m_dialogCompsDurationFields.length - 1]) {
                gbcSwitchingPanels.weighty = 1;
            }
            panelDurationCheck.add(dc.getComponentPanel(), gbcSwitchingPanels);
            gbcSwitchingPanels.gridy++;
        }
        switchingPanel.add(panelDurationCheck,
            DurationCellFactory.TYPE.toString() + ExtractDurationPeriodFieldsNodeModel.MODUS_SEVERAL);
        /*
         * panel containing combobox of period fields
         */
        final JPanel panelPeriodCombo = new JPanel(new GridBagLayout());
        gbcSwitchingPanels.gridy = 0;
        gbcSwitchingPanels.weighty = 1;
        panelPeriodCombo.add(m_dialogCompPeriodFieldSelect.getComponentPanel(), gbcSwitchingPanels);
        switchingPanel.add(panelPeriodCombo,
            PeriodCellFactory.TYPE.toString() + ExtractDurationPeriodFieldsNodeModel.MODUS_SINGLE);
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
        switchingPanel.add(panelPeriodCheck,
            PeriodCellFactory.TYPE.toString() + ExtractDurationPeriodFieldsNodeModel.MODUS_SEVERAL);

        /*
         * add tab
         */
        addTab("Options", panel);

        m_dialogCompColSelect.getModel().addChangeListener(l -> {
            if (m_dialogCompColSelect.getSelectedAsSpec() != null) {
                ((CardLayout)switchingPanel.getLayout()).show(switchingPanel,
                    m_dialogCompColSelect.getSelectedAsSpec().getType().toString() + modusSelectModel.getStringValue());
            }
        });
        modusSelectModel.addChangeListener(l -> {
            if (m_dialogCompColSelect.getSelectedAsSpec() != null) {
                ((CardLayout)switchingPanel.getLayout()).show(switchingPanel,
                    m_dialogCompColSelect.getSelectedAsSpec().getType().toString() + modusSelectModel.getStringValue());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dialogCompColSelect.saveSettingsTo(settings);
        m_dialogCompModSelect.saveSettingsTo(settings);
        m_dialogCompDurationFieldSelect.saveSettingsTo(settings);
        m_dialogCompPeriodFieldSelect.saveSettingsTo(settings);
        for (final DialogComponentBoolean dc : m_dialogCompsDurationFields) {
            dc.saveSettingsTo(settings);
        }
        for (final DialogComponentBoolean dc : m_dialogCompsPeriodFields) {
            dc.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_dialogCompColSelect.loadSettingsFrom(settings, specs);
        m_dialogCompModSelect.loadSettingsFrom(settings, specs);
        m_dialogCompDurationFieldSelect.loadSettingsFrom(settings, specs);
        m_dialogCompPeriodFieldSelect.loadSettingsFrom(settings, specs);
        for (final DialogComponentBoolean dc : m_dialogCompsDurationFields) {
            dc.loadSettingsFrom(settings, specs);
        }
        for (final DialogComponentBoolean dc : m_dialogCompsPeriodFields) {
            dc.loadSettingsFrom(settings, specs);
        }
    }
}
