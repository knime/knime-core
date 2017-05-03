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
 *   Feb 23, 2017 (simon): created
 */
package org.knime.time.node.create.createdatetime;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.time.node.convert.DateTimeTypes;
import org.knime.time.util.DialogComponentDateTimeSelection;
import org.knime.time.util.DialogComponentDateTimeSelection.DisplayOption;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.SettingsModelDateTime;

/**
 * The node dialog of the node which creates date and time cells.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class CreateDateTimeNodeDialog extends NodeDialogPane {
    final static String BUTTONS = "buttons";

    final static String LABELS = "labels";

    private final JComboBox<DateTimeTypes> m_typeCombobox;

    private final DialogComponentString m_dialogCompColumnName;

    private final DialogComponentButtonGroup m_dialogCompRowNrOptionSelection;

    private final DialogComponentNumber m_dialogCompRowNrFixed;

    private final DialogComponentDateTimeSelection m_dialogCompStart;

    private final DialogComponentButtonGroup m_dialogCompDurationOrEnd;

    private final DialogComponentString m_dialogCompDuration;

    private final DialogComponentDateTimeSelection m_dialogCompEnd;

    private final DialogComponentBoolean m_dialogCompStartUseExecTime;

    private final DialogComponentBoolean m_dialogCompEndUseExecTime;

    private final JLabel m_warningLabel;

    private boolean m_updateWarningLabel = true;

    CreateDateTimeNodeDialog() {
        m_typeCombobox = new JComboBox<DateTimeTypes>(DateTimeTypes.values());

        m_dialogCompColumnName =
            new DialogComponentString(CreateDateTimeNodeModel.createColumnNameModel(), null, true, 15);

        final SettingsModelString rowNrOptionSelectionModel = CreateDateTimeNodeModel.createRowNrOptionSelectionModel();
        m_dialogCompRowNrOptionSelection =
            new DialogComponentButtonGroup(rowNrOptionSelectionModel, null, true, RowNrMode.values());

        m_dialogCompRowNrFixed = new DialogComponentNumber(
            CreateDateTimeNodeModel.createRowNrFixedModel(rowNrOptionSelectionModel), null, 10, 10);

        final SettingsModelBoolean modelStartUseExecTime = CreateDateTimeNodeModel.createStartUseExecTimeModel();
        m_dialogCompStartUseExecTime = new DialogComponentBoolean(modelStartUseExecTime, "Use execution date&time");

        final SettingsModelDateTime modelStart = CreateDateTimeNodeModel.createStartModel(modelStartUseExecTime);
        m_dialogCompStart =
            new DialogComponentDateTimeSelection(modelStart, null, DisplayOption.SHOW_DATE_AND_TIME_AND_TIMEZONE);

        final SettingsModelString durationOrEndSelectionModel =
            CreateDateTimeNodeModel.createDurationOrEndSelectionModel();
        m_dialogCompDurationOrEnd =
            new DialogComponentButtonGroup(durationOrEndSelectionModel, null, true, EndMode.values());
        m_dialogCompDuration = new DialogComponentString(
            CreateDateTimeNodeModel.createDurationModel(rowNrOptionSelectionModel, durationOrEndSelectionModel), null,
            true, 20);

        final SettingsModelBoolean modelEndUseExecTime =
            CreateDateTimeNodeModel.createEndUseExecTimeModel(rowNrOptionSelectionModel, durationOrEndSelectionModel);
        m_dialogCompEndUseExecTime = new DialogComponentBoolean(modelEndUseExecTime, "Use execution date&time");

        final SettingsModelDateTime modelEnd = CreateDateTimeNodeModel.createEndModel(rowNrOptionSelectionModel,
            durationOrEndSelectionModel, modelEndUseExecTime);
        m_dialogCompEnd = new DialogComponentDateTimeSelection(modelEnd, null, DisplayOption.SHOW_DATE_AND_TIME);

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
        gbc.anchor = GridBagConstraints.WEST;

        /*
         * add type and column name selection
         */
        final JPanel panelOutput = new JPanel(new GridBagLayout());
        panel.add(panelOutput, gbc);
        final GridBagConstraints gbcOutput = new GridBagConstraints();
        gbcOutput.fill = GridBagConstraints.VERTICAL;
        gbcOutput.gridx = 0;
        gbcOutput.gridy = 0;
        gbcOutput.anchor = GridBagConstraints.WEST;
        gbcOutput.insets = new Insets(5, 5, 5, 5);
        panelOutput.setBorder(BorderFactory.createTitledBorder("Output Settings"));
        panelOutput.add(new JLabel("Output type: "), gbcOutput);
        gbcOutput.gridx++;
        gbcOutput.weightx = 1;
        gbcOutput.insets = new Insets(5, 17, 5, 5);
        gbcOutput.ipadx = 15;
        panelOutput.add(m_typeCombobox, gbcOutput);
        gbcOutput.ipadx = 0;
        gbcOutput.insets = new Insets(5, 5, 5, 5);
        gbcOutput.weightx = 0;
        gbcOutput.gridy++;
        gbcOutput.gridx = 0;
        panelOutput.add(new JLabel("New column name: "), gbcOutput);
        gbcOutput.weightx = 1;
        gbcOutput.gridx++;
        panelOutput.add(m_dialogCompColumnName.getComponentPanel(), gbcOutput);

        /*
         * add mode of row numbers selection
         */
        final JPanel panelModusRowNr = new JPanel(new GridBagLayout());
        gbc.gridy++;
        panel.add(panelModusRowNr, gbc);
        panelModusRowNr.setBorder(BorderFactory.createTitledBorder("Mode Selection"));
        final GridBagConstraints gbcModusRowNr = new GridBagConstraints();
        gbcModusRowNr.fill = GridBagConstraints.VERTICAL;
        gbcModusRowNr.gridx = 0;
        gbcModusRowNr.gridy = 0;
        gbcModusRowNr.anchor = GridBagConstraints.WEST;
        gbcModusRowNr.insets = new Insets(14, 5, 5, 5);

        panelModusRowNr.add(new JLabel("Number of rows:"), gbcModusRowNr);
        gbcModusRowNr.insets = new Insets(5, 5, 5, 5);
        gbcModusRowNr.gridheight = 2;
        gbcModusRowNr.gridx++;
        panelModusRowNr.add(m_dialogCompRowNrOptionSelection.getComponentPanel(), gbcModusRowNr);
        gbcModusRowNr.gridx++;
        gbcModusRowNr.weightx = 1;
        panelModusRowNr.add(m_dialogCompRowNrFixed.getComponentPanel(), gbcModusRowNr);

        /*
         * add start selection
         */
        final JPanel panelStartTime = new JPanel(new GridBagLayout());
        gbc.gridy++;
        panel.add(panelStartTime, gbc);
        panelStartTime.setBorder(BorderFactory.createTitledBorder("Starting Point"));
        final GridBagConstraints gbcStartTime = new GridBagConstraints();
        gbcStartTime.fill = GridBagConstraints.VERTICAL;
        gbcStartTime.gridx = 0;
        gbcStartTime.gridy = 0;
        gbcStartTime.anchor = GridBagConstraints.WEST;
        gbcStartTime.insets = new Insets(18, 28, 5, 5);

        panelStartTime.add(new JLabel("Start:"), gbcStartTime);
        gbcStartTime.gridheight = 2;
        gbcStartTime.insets = new Insets(5, 39, 0, 5);
        gbcStartTime.gridx++;
        panelStartTime.add(m_dialogCompStart.getComponentPanel(), gbcStartTime);

        gbcStartTime.gridy += 2;
        gbcStartTime.insets = new Insets(0, 42, 5, 5);
        gbcStartTime.gridheight = 1;
        gbcStartTime.weightx = 1;
        panelStartTime.add(m_dialogCompStartUseExecTime.getComponentPanel(), gbcStartTime);

        /*
         * add interval and end selection
         */
        final JPanel panelEndTime = new JPanel(new GridBagLayout());
        gbc.gridy++;
        panel.add(panelEndTime, gbc);
        panelEndTime.setBorder(BorderFactory.createTitledBorder("Ending Point"));
        final GridBagConstraints gbcEndTime = new GridBagConstraints();
        gbcEndTime.fill = GridBagConstraints.VERTICAL;
        gbcEndTime.gridx = 0;
        gbcEndTime.gridy = 0;
        gbcEndTime.anchor = GridBagConstraints.WEST;
        gbcEndTime.insets = new Insets(0, 13, 5, 5);

        final JPanel panelSwitchModus = new JPanel(new CardLayout());
        gbcEndTime.insets = new Insets(5, 5, 5, 5);
        gbcEndTime.gridx = 0;
        gbcEndTime.gridy += 1;
        panelEndTime.add(panelSwitchModus, gbcEndTime);
        final GridBagConstraints gbcDurationEnd = new GridBagConstraints();
        gbcDurationEnd.fill = GridBagConstraints.VERTICAL;
        gbcDurationEnd.gridx = 0;
        gbcDurationEnd.gridy = 0;
        gbcDurationEnd.anchor = GridBagConstraints.WEST;
        gbcDurationEnd.insets = new Insets(7, 3, 5, 5);
        // add buttons
        final JPanel panelDurationEndButtons = new JPanel(new GridBagLayout());
        panelDurationEndButtons.add(m_dialogCompDurationOrEnd.getButton(EndMode.Duration.name()), gbcDurationEnd);
        gbcDurationEnd.gridy++;
        panelDurationEndButtons.add(m_dialogCompDurationOrEnd.getButton(EndMode.End.name()), gbcDurationEnd);
        panelSwitchModus.add(panelDurationEndButtons, BUTTONS);
        // add labels
        final JPanel panelDurationEnd = new JPanel(new GridBagLayout());
        gbcDurationEnd.insets = new Insets(10, 20, 9, 5);
        panelDurationEnd.add(new JLabel(EndMode.Duration.getText()), gbcDurationEnd);
        gbcDurationEnd.gridy++;
        gbcDurationEnd.insets = new Insets(11, 20, 7, 5);
        panelDurationEnd.add(new JLabel(EndMode.End.getText()), gbcDurationEnd);
        panelSwitchModus.add(panelDurationEnd, LABELS);

        final JPanel panelDurationEndSelection = new JPanel(new GridBagLayout());
        gbcEndTime.gridx++;
        gbcEndTime.weightx = 1;
        gbcEndTime.weighty = 1;
        gbcEndTime.gridheight = 2;
        panelEndTime.add(panelDurationEndSelection, gbcEndTime);
        final GridBagConstraints gbcDurationEndSelection = new GridBagConstraints();
        gbcDurationEndSelection.fill = GridBagConstraints.VERTICAL;
        gbcDurationEndSelection.gridx = 0;
        gbcDurationEndSelection.gridy = 0;
        gbcDurationEndSelection.anchor = GridBagConstraints.WEST;
        gbcDurationEndSelection.insets = new Insets(5, 5, 0, 5);
        panelDurationEndSelection.add(m_dialogCompDuration.getComponentPanel(), gbcDurationEndSelection);
        gbcDurationEndSelection.gridy++;
        gbcDurationEndSelection.insets = new Insets(0, 7, 0, 5);
        panelDurationEndSelection.add(m_dialogCompEnd.getComponentPanel(), gbcDurationEndSelection);
        gbcDurationEndSelection.gridy++;
        gbcDurationEndSelection.insets = new Insets(0, 9, 5, 5);
        gbcDurationEndSelection.weighty = 1;
        panelDurationEndSelection.add(m_dialogCompEndUseExecTime.getComponentPanel(), gbcDurationEndSelection);

        m_warningLabel = new JLabel("");
        m_warningLabel.setPreferredSize(new Dimension(500, new JLabel(" ").getPreferredSize().height));
        m_warningLabel.setForeground(Color.RED);
        gbc.gridy++;
        gbc.insets = new Insets(5, 10, 5, 5);
        gbc.weighty = 1;
        panel.add(m_warningLabel, gbc);

        /*
         * add tab
         */
        addTab("Options", panel);

        rowNrOptionSelectionModel.addChangeListener(l -> {
            final CardLayout cardLayout = (CardLayout)panelSwitchModus.getLayout();
            if (rowNrOptionSelectionModel.getStringValue().equals(RowNrMode.Fixed.name())) {
                cardLayout.show(panelSwitchModus, BUTTONS);
            } else {
                cardLayout.show(panelSwitchModus, LABELS);
            }
            updateWarningLabel();
        });

        m_typeCombobox.addActionListener(l -> {
            m_updateWarningLabel = false;
            // update date&time models
            final boolean useDate = !m_typeCombobox.getSelectedItem().equals(DateTimeTypes.LOCAL_TIME);
            final boolean useTime = !m_typeCombobox.getSelectedItem().equals(DateTimeTypes.LOCAL_DATE);
            final boolean useZone = m_typeCombobox.getSelectedItem().equals(DateTimeTypes.ZONED_DATE_TIME);
            modelStart.setUseDate(useDate);
            modelStart.setUseTime(useTime);
            modelStart.setUseZone(useZone);
            modelEnd.setUseDate(useDate);
            modelEnd.setUseTime(useTime);
            m_updateWarningLabel = true;
            updateWarningLabel();
        });

        modelStart.addChangeListener(l -> {
            updateWarningLabel();
        });
        modelEnd.addChangeListener(l -> updateWarningLabel());
        modelStartUseExecTime.addChangeListener(l -> updateWarningLabel());
        modelEndUseExecTime.addChangeListener(l -> updateWarningLabel());
        m_dialogCompDuration.getModel().addChangeListener(l -> updateWarningLabel());
    }

    private void updateWarningLabel() {
        m_warningLabel.setText("");
        // === check period duration field ===
        if (m_dialogCompDuration.getModel().isEnabled() && m_updateWarningLabel) {
            final DateTimeTypes selectedItem = (DateTimeTypes)m_typeCombobox.getSelectedItem();
            Duration duration = null;
            Period period = null;
            if (((SettingsModelString)m_dialogCompDuration.getModel()).getStringValue() == null) {
                m_warningLabel.setText("Please enter an interval.");
                return;
            }
            try {
                duration = DurationPeriodFormatUtils
                    .parseDuration(((SettingsModelString)m_dialogCompDuration.getModel()).getStringValue());
                if (selectedItem.equals(DateTimeTypes.LOCAL_DATE)) {
                    m_warningLabel.setText("A duration cannot be applied to a local date.");
                    return;
                }
            } catch (DateTimeParseException e1) {
                try {
                    period = DurationPeriodFormatUtils
                        .parsePeriod(((SettingsModelString)m_dialogCompDuration.getModel()).getStringValue());
                    if (selectedItem.equals(DateTimeTypes.LOCAL_TIME)) {
                        m_warningLabel.setText("A period cannot be applied to a local time.");
                        return;
                    }
                } catch (DateTimeParseException e2) {
                    m_warningLabel.setText("Value does not represent a period or duration.");
                    return;
                }
            }
            if (((SettingsModelString)m_dialogCompRowNrOptionSelection.getModel()).getStringValue()
                .equals(RowNrMode.Variable.name())) {
                // === check that duration is not zero and row number variable ===
                if ((duration != null && duration.isZero()) || (period != null && period.isZero())) {
                    m_warningLabel.setText("Interval must not be zero.");
                }

                // === check that start is before end and duration positive and vice versa ===
                final Temporal start = ((SettingsModelBoolean)m_dialogCompStartUseExecTime.getModel()).getBooleanValue()
                    ? CreateDateTimeNodeModel.getTemporalExecTimeWithFormat(
                        ((SettingsModelDateTime)m_dialogCompEnd.getModel()).getSelectedDateTime())
                    : ((SettingsModelDateTime)m_dialogCompStart.getModel()).getSelectedDateTime();

                final Temporal end = ((SettingsModelBoolean)m_dialogCompEndUseExecTime.getModel()).getBooleanValue()
                    ? CreateDateTimeNodeModel.getTemporalExecTimeWithFormat(
                        ((SettingsModelDateTime)m_dialogCompStart.getModel()).getSelectedDateTime())
                    : ((SettingsModelDateTime)m_dialogCompEnd.getModel()).getSelectedDateTime();

                if (!selectedItem.equals(DateTimeTypes.LOCAL_TIME)) {
                    final boolean isStartBeforeEnd;
                    final boolean isEqual;
                    if (selectedItem.equals(DateTimeTypes.LOCAL_DATE)) {
                        isStartBeforeEnd = ((LocalDate)start).isBefore((LocalDate)end);
                        isEqual = ((LocalDate)start).isEqual((LocalDate)end);
                    } else if (selectedItem.equals(DateTimeTypes.LOCAL_DATE_TIME)) {
                        isStartBeforeEnd = ((LocalDateTime)start).isBefore((LocalDateTime)end);
                        isEqual = ((LocalDateTime)start).isEqual((LocalDateTime)end);
                    } else {
                        isStartBeforeEnd = (((ZonedDateTime)start)).toLocalDateTime().isBefore(((LocalDateTime)end));
                        isEqual = (((ZonedDateTime)start)).toLocalDateTime().isEqual(((LocalDateTime)end));
                    }

                    final boolean isDurationNegative;
                    if (duration != null) {
                        isDurationNegative = duration.isNegative();
                    } else if (period != null) {
                        isDurationNegative = period.isNegative();
                    } else {
                        throw new IllegalStateException("Either duration or period must not be null!");
                    }

                    if (isStartBeforeEnd && isDurationNegative) {
                        m_warningLabel.setText("Start is before end, but the interval is negative.");
                        return;
                    }
                    if (!isStartBeforeEnd && !isEqual && !isDurationNegative) {
                        m_warningLabel.setText("Start is after end, but the interval is positive.");
                        return;
                    }
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
        final String text = m_warningLabel.getText();
        if (!StringUtils.isEmpty(text)) {
            throw new InvalidSettingsException(text);
        }
        settings.addString("type", ((DateTimeTypes)m_typeCombobox.getModel().getSelectedItem()).name());
        m_dialogCompColumnName.saveSettingsTo(settings);
        m_dialogCompRowNrOptionSelection.saveSettingsTo(settings);
        m_dialogCompRowNrFixed.saveSettingsTo(settings);
        m_dialogCompStart.saveSettingsTo(settings);
        m_dialogCompDurationOrEnd.saveSettingsTo(settings);
        m_dialogCompDuration.saveSettingsTo(settings);
        m_dialogCompEnd.saveSettingsTo(settings);
        m_dialogCompStartUseExecTime.saveSettingsTo(settings);
        m_dialogCompEndUseExecTime.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_typeCombobox
            .setSelectedItem(DateTimeTypes.valueOf(settings.getString("type", DateTimeTypes.LOCAL_DATE_TIME.name())));
        m_dialogCompColumnName.loadSettingsFrom(settings, specs);
        m_dialogCompStart.loadSettingsFrom(settings, specs);
        m_dialogCompDuration.loadSettingsFrom(settings, specs);
        m_dialogCompRowNrOptionSelection.loadSettingsFrom(settings, specs);
        m_dialogCompDurationOrEnd.loadSettingsFrom(settings, specs);
        m_dialogCompRowNrFixed.loadSettingsFrom(settings, specs);
        m_dialogCompEnd.loadSettingsFrom(settings, specs);
        m_dialogCompStartUseExecTime.loadSettingsFrom(settings, specs);
        m_dialogCompEndUseExecTime.loadSettingsFrom(settings, specs);
    }
}
