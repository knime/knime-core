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
 *   Apr 5, 2017 (simon): created
 */
package org.knime.time.node.calculate.datetimedifference;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.DataValueColumnFilter;
import org.knime.time.util.DialogComponentDateTimeSelection;
import org.knime.time.util.Granularity;
import org.knime.time.util.SettingsModelDateTime;
import org.knime.time.util.DialogComponentDateTimeSelection.DisplayOption;

/**
 * The node dialog of the node which calculates differences between two date&time values.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class DateTimeDifferenceNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnNameSelection m_dialogComp1stColSelect;

    private final DialogComponentColumnNameSelection m_dialogComp2ndColSelect;

    private final DialogComponentButtonGroup m_dialogCompModusSelect;

    private final DialogComponentDateTimeSelection m_dialogCompDateTimeSelect;

    private final DialogComponentButtonGroup m_dialogCompCalculationSelect;

    private final DialogComponentStringSelection m_dialogCompGranularity;

    private final DialogComponentString m_dialogCompNewColName;

    private final JLabel m_warningLabel;

    @SuppressWarnings("unchecked")
    public DateTimeDifferenceNodeDialog() {
        m_dialogComp1stColSelect = new DialogComponentColumnNameSelection(
            DateTimeDifferenceNodeModel.createColSelectModel(1), "Date&Time column ", 0, true, LocalDateValue.class,
            LocalTimeValue.class, LocalDateTimeValue.class, ZonedDateTimeValue.class);

        final SettingsModelString modusModel = DateTimeDifferenceNodeModel.createModusSelection();
        m_dialogCompModusSelect = new DialogComponentButtonGroup(modusModel, null, true, ModusOptions.values());

        m_dialogComp2ndColSelect = new DialogComponentColumnNameSelection(
            DateTimeDifferenceNodeModel.createColSelectModel(2), "", 0, true, LocalDateValue.class,
            LocalTimeValue.class, LocalDateTimeValue.class, ZonedDateTimeValue.class);

        final SettingsModelDateTime dateTimeModel = DateTimeDifferenceNodeModel.createDateTimeModel();
        m_dialogCompDateTimeSelect =
            new DialogComponentDateTimeSelection(dateTimeModel, null, DisplayOption.SHOW_DATE_AND_TIME_AND_TIMEZONE);

        final SettingsModelString calculationModel = DateTimeDifferenceNodeModel.createCalculationSelection();
        m_dialogCompCalculationSelect =
            new DialogComponentButtonGroup(calculationModel, null, true, OutputMode.values());

        m_dialogCompGranularity = new DialogComponentStringSelection(
            DateTimeDifferenceNodeModel.createGranularityModel(calculationModel), null, Granularity.strings());

        m_dialogCompNewColName =
            new DialogComponentString(DateTimeDifferenceNodeModel.createNewColNameModel(), "New column name: ");

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
        panelColSelect.setBorder(BorderFactory.createTitledBorder("Base column"));
        final GridBagConstraints gbcColSelect = new GridBagConstraints();
        gbcColSelect.insets = new Insets(5, 5, 5, 5);
        gbcColSelect.fill = GridBagConstraints.VERTICAL;
        gbcColSelect.gridx = 0;
        gbcColSelect.gridy = 0;
        gbcColSelect.anchor = GridBagConstraints.WEST;
        gbcColSelect.weightx = 1;
        panelColSelect.add(m_dialogComp1stColSelect.getComponentPanel(), gbcColSelect);
        panel.add(panelColSelect, gbc);

        /*
         * add modus selection
         */
        final JPanel panelModusSelect = new JPanel(new GridBagLayout());
        gbc.gridy++;
        panel.add(panelModusSelect, gbc);
        panelModusSelect.setBorder(BorderFactory.createTitledBorder("Calculate difference to"));
        final GridBagConstraints gbcModusSelect = new GridBagConstraints();
        gbcModusSelect.insets = new Insets(5, 5, 5, 5);
        gbcModusSelect.fill = GridBagConstraints.VERTICAL;
        gbcModusSelect.gridx = 0;
        gbcModusSelect.gridy = 0;
        gbcModusSelect.anchor = GridBagConstraints.WEST;
        // add button group
        panelModusSelect.add(m_dialogCompModusSelect.getComponentPanel(), gbcModusSelect);
        // add card layout
        final JPanel cardPanelModus = new JPanel(new CardLayout());
        gbcModusSelect.gridx++;
        gbcModusSelect.weightx = 1;
        panelModusSelect.add(cardPanelModus, gbcModusSelect);
        // add card with column selection
        cardPanelModus.add(m_dialogComp2ndColSelect.getComponentPanel(), ModusOptions.Use2ndColumn.name());
        cardPanelModus.add(m_dialogCompDateTimeSelect.getComponentPanel(), ModusOptions.UseFixedTime.name());
        cardPanelModus.add(new JPanel(), ModusOptions.UseExecutionTime.name());
        cardPanelModus.add(new JPanel(), ModusOptions.UsePreviousRow.name());

        /*
         * add calculation modus selection
         */
        final JPanel panelCalculationSelect = new JPanel(new GridBagLayout());
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(panelCalculationSelect, gbc);
        panelCalculationSelect.setBorder(BorderFactory.createTitledBorder("Output options"));
        final GridBagConstraints gbcCalculationSelect = new GridBagConstraints();
        gbcCalculationSelect.insets = new Insets(5, 5, 5, 5);
        gbcCalculationSelect.fill = GridBagConstraints.VERTICAL;
        gbcCalculationSelect.gridx = 0;
        gbcCalculationSelect.gridy = 0;
        gbcCalculationSelect.anchor = GridBagConstraints.WEST;
        // add button group
        panelCalculationSelect.add(m_dialogCompCalculationSelect.getComponentPanel(), gbcCalculationSelect);
        // add card panel
        gbcCalculationSelect.gridx++;
        gbcCalculationSelect.weightx = 1;
        gbcCalculationSelect.insets = new Insets(5, 20, 0, 0);
        // add granularity selection
        panelCalculationSelect.add(m_dialogCompGranularity.getComponentPanel(), gbcCalculationSelect);
        // add column name edit
        gbcCalculationSelect.gridx = 0;
        gbcCalculationSelect.weightx = 0;
        gbcCalculationSelect.weighty = 1;
        gbcCalculationSelect.gridwidth = 2;
        gbcCalculationSelect.insets = new Insets(0, 10, 0, 0);
        gbcCalculationSelect.gridy++;
        panelCalculationSelect.add(m_dialogCompNewColName.getComponentPanel(), gbcCalculationSelect);

        // add warning label
        gbcCalculationSelect.gridx = 0;
        gbcCalculationSelect.gridy++;
        m_warningLabel = new JLabel();
        m_warningLabel.setPreferredSize(new Dimension(600, new JLabel(" ").getPreferredSize().height));
        m_warningLabel.setForeground(Color.RED);
        panelCalculationSelect.add(m_warningLabel, gbcCalculationSelect);

        /*
         * add tab
         */
        addTab("Options", panel);
        /*
         * Change listeners
         */
        m_dialogComp1stColSelect.getModel().addChangeListener(l -> {
            updateDateTimeComponents(dateTimeModel);
        });

        modusModel.addChangeListener(l -> {
            ((CardLayout)(cardPanelModus.getLayout())).show(cardPanelModus, modusModel.getStringValue());
            updateWarningLabel();
        });

        m_dialogComp2ndColSelect.getModel().addChangeListener(l -> updateWarningLabel());

        m_dialogCompGranularity.getModel().addChangeListener(l -> updateWarningLabel());
    }

    @SuppressWarnings("unchecked")
    private void updateDateTimeComponents(final SettingsModelDateTime dateTimeModel) {
        if (m_dialogComp1stColSelect.getSelectedAsSpec() != null) {
            final DataType type = m_dialogComp1stColSelect.getSelectedAsSpec().getType();
            // adapt column filter of column selection for 2nd column
            try {
                final List<Class<? extends DataValue>> valueClasses = type.getValueClasses();
                if (valueClasses.contains(ZonedDateTimeValue.class)) {
                    m_dialogComp2ndColSelect.setColumnFilter(new DataValueColumnFilter(ZonedDateTimeValue.class));
                } else if (valueClasses.contains(LocalDateTimeValue.class)) {
                    m_dialogComp2ndColSelect.setColumnFilter(new DataValueColumnFilter(LocalDateTimeValue.class));
                } else if (valueClasses.contains(LocalDateValue.class)) {
                    m_dialogComp2ndColSelect.setColumnFilter(new DataValueColumnFilter(LocalDateValue.class));
                } else if (valueClasses.contains(LocalTimeValue.class)) {
                    m_dialogComp2ndColSelect.setColumnFilter(new DataValueColumnFilter(LocalTimeValue.class));
                }
            } catch (NotConfigurableException ex) {
                // will never happen, because there is always at least the selected 1st column available
            }

            // adapt date&time component
            dateTimeModel.setUseDate(!type.isCompatible(LocalTimeValue.class));
            dateTimeModel.setUseTime(!type.isCompatible(LocalDateValue.class));
            dateTimeModel.setUseZone(type.isCompatible(ZonedDateTimeValue.class));
        }
        updateWarningLabel();
    }

    private void updateWarningLabel() {
        m_warningLabel.setText("");
        if (((SettingsModelString)m_dialogCompModusSelect.getModel()).getStringValue()
            .equals(ModusOptions.Use2ndColumn.name()) && m_dialogComp1stColSelect.getSelectedAsSpec() != null
            && m_dialogComp2ndColSelect.getSelectedAsSpec() != null) {
            if (!m_dialogComp2ndColSelect.getSelectedAsSpec().getType()
                .isCompatible(m_dialogComp1stColSelect.getSelectedAsSpec().getType().getPreferredValueClass())) {
                m_warningLabel.setText("The both selected columns do not have the same type and are not compatible.");
                return;
            }
        }
        if (((SettingsModelString)m_dialogCompCalculationSelect.getModel()).getStringValue()
            .equals(OutputMode.Granularity.name())) {
            final Granularity granularity =
                Granularity.fromString(((SettingsModelString)m_dialogCompGranularity.getModel()).getStringValue());
            if (m_dialogComp1stColSelect.getSelectedAsSpec() != null) {
                final DataType type = m_dialogComp1stColSelect.getSelectedAsSpec().getType();
                if ((type.isCompatible(LocalTimeValue.class) && granularity.getChronoUnit().isDateBased())) {
                    m_warningLabel.setText("The selected column is not compatible with the selected granularity: "
                        + granularity.toString());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        updateWarningLabel();
        if (m_warningLabel.getText().length() > 0) {
            throw new InvalidSettingsException(m_warningLabel.getText());
        }
        m_dialogComp1stColSelect.saveSettingsTo(settings);
        m_dialogComp2ndColSelect.saveSettingsTo(settings);
        m_dialogCompModusSelect.saveSettingsTo(settings);
        m_dialogCompDateTimeSelect.saveSettingsTo(settings);
        m_dialogCompCalculationSelect.saveSettingsTo(settings);
        m_dialogCompGranularity.saveSettingsTo(settings);
        m_dialogCompNewColName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_dialogComp1stColSelect.loadSettingsFrom(settings, specs);
        m_dialogComp2ndColSelect.loadSettingsFrom(settings, specs);
        m_dialogCompModusSelect.loadSettingsFrom(settings, specs);
        m_dialogCompDateTimeSelect.loadSettingsFrom(settings, specs);
        m_dialogCompCalculationSelect.loadSettingsFrom(settings, specs);
        m_dialogCompGranularity.loadSettingsFrom(settings, specs);
        m_dialogCompNewColName.loadSettingsFrom(settings, specs);
        updateDateTimeComponents((SettingsModelDateTime)m_dialogCompDateTimeSelect.getModel());
    }

}
