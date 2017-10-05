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
 *   Jan 20, 2017 (simon): created
 */
package org.knime.time.node.filter.datetimebasedrowfilter;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.time.util.DialogComponentDateTimeSelection;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.Granularity;
import org.knime.time.util.SettingsModelDateTime;
import org.knime.time.util.DialogComponentDateTimeSelection.DisplayOption;

/**
 * The node dialog of the node which filters rows based on a time window on one of the new date&time columns.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class DateTimeBasedRowFilterNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnNameSelection m_dialogCompColSelection;

    private final DialogComponentBoolean m_dialogCompStartBoolean;

    private final DialogComponentBoolean m_dialogCompEndBoolean;

    private final DialogComponentDateTimeSelection m_dialogCompStartDateTime;

    private final DialogComponentDateTimeSelection m_dialogCompEndDateTime;

    private final DialogComponentButtonGroup m_dialogCompEndSelection;

    private final DialogComponentString m_dialogCompPeriodOrDurationValue;

    private final DialogComponentNumber m_dialogCompNumericalValue;

    private final DialogComponentStringSelection m_dialogCompNumericalGranularity;

    private final DialogComponentBoolean m_dialogCompStartInclusive;

    private final DialogComponentBoolean m_dialogCompEndInclusive;

    private final DialogComponentBoolean m_dialogCompStartAlwaysNow;

    private final DialogComponentBoolean m_dialogCompEndAlwaysNow;

    private final JLabel m_typeWarningLabel;

    /** Setting up all DialogComponents. */
    @SuppressWarnings("unchecked")
    public DateTimeBasedRowFilterNodeDialog() {

        /*
         * DialogComponents
         */
        final SettingsModelString colSelectModel = DateTimeBasedRowFilterNodeModel.createColSelectModel();
        m_dialogCompColSelection = new DialogComponentColumnNameSelection(colSelectModel, "Date&Time Column: ", 0,
            LocalDateTimeValue.class, ZonedDateTimeValue.class, LocalDateValue.class, LocalTimeValue.class);

        final SettingsModelBoolean startBooleanModel = DateTimeBasedRowFilterNodeModel.createStartBooleanModel();
        m_dialogCompStartBoolean = new DialogComponentBoolean(startBooleanModel, null);

        final SettingsModelBoolean endBooleanModel = DateTimeBasedRowFilterNodeModel.createEndBooleanModel();
        m_dialogCompEndBoolean = new DialogComponentBoolean(endBooleanModel, null);

        final SettingsModelDateTime startDateTimeModel = DateTimeBasedRowFilterNodeModel.createStartDateTimeModel();
        m_dialogCompStartDateTime = new DialogComponentDateTimeSelection(startDateTimeModel, null,
            DisplayOption.SHOW_DATE_AND_TIME_AND_TIMEZONE);

        final SettingsModelDateTime endDateTimeModel = DateTimeBasedRowFilterNodeModel.createEndDateTimeModel();
        m_dialogCompEndDateTime =
            new DialogComponentDateTimeSelection(endDateTimeModel, null, DisplayOption.SHOW_DATE_AND_TIME_AND_TIMEZONE);

        m_dialogCompPeriodOrDurationValue =
            new DialogComponentString(DateTimeBasedRowFilterNodeModel.createPeriodValueModel(), "", false, 30);

        m_dialogCompNumericalValue =
            new DialogComponentNumber(DateTimeBasedRowFilterNodeModel.createNumericalValueModel(), "", 1, 17);

        final SettingsModelString granularityModel = DateTimeBasedRowFilterNodeModel.createNumericalGranularityModel();
        m_dialogCompNumericalGranularity =
            new DialogComponentStringSelection(granularityModel, "Granularity:", Granularity.strings());

        final SettingsModelBoolean startInclusiveModel = DateTimeBasedRowFilterNodeModel.createStartInclusiveModel();
        m_dialogCompStartInclusive = new DialogComponentBoolean(startInclusiveModel, "Inclusive");

        final SettingsModelBoolean startAlwaysNowModel = DateTimeBasedRowFilterNodeModel.createStartAlwaysNowModel();
        m_dialogCompStartAlwaysNow = new DialogComponentBoolean(startAlwaysNowModel, "Use execution date&ime");

        final SettingsModelString endSelectionModel = DateTimeBasedRowFilterNodeModel.createEndSelectionModel();
        m_dialogCompEndSelection = new DialogComponentButtonGroup(endSelectionModel, null, true,
            EndMode.values());

        final SettingsModelBoolean endInclusiveModel = DateTimeBasedRowFilterNodeModel.createEndInclusiveModel();
        m_dialogCompEndInclusive = new DialogComponentBoolean(endInclusiveModel, "Inclusive");

        final SettingsModelBoolean endAlwaysNowModel = DateTimeBasedRowFilterNodeModel.createEndAlwaysNowModel();
        m_dialogCompEndAlwaysNow = new DialogComponentBoolean(endAlwaysNowModel, "Use execution date&time");

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
        panelColSelect.add(m_dialogCompColSelection.getComponentPanel(), gbcColSelect);
        panel.add(panelColSelect, gbc);

        /*
         * add date&time/format selection
         */
        gbc.gridy++;
        gbc.weighty = 1;
        final JPanel panelDateTimeSelection = new JPanel(new GridBagLayout());
        panelDateTimeSelection.setBorder(BorderFactory.createTitledBorder("Date&Time Selection"));
        final GridBagConstraints gbcDateTimeSelection = new GridBagConstraints();
        // add label and combo box for type selection
        gbcDateTimeSelection.fill = GridBagConstraints.VERTICAL;
        gbcDateTimeSelection.gridx = 0;
        gbcDateTimeSelection.gridy = 0;
        gbcDateTimeSelection.weightx = 1;
        gbcDateTimeSelection.anchor = GridBagConstraints.WEST;
        final JPanel panelStartEnd = new JPanel(new GridBagLayout());
        gbcDateTimeSelection.fill = GridBagConstraints.BOTH;
        gbcDateTimeSelection.insets = new Insets(5, 0, 0, 0);
        panelDateTimeSelection.add(panelStartEnd, gbcDateTimeSelection);
        final GridBagConstraints gbcStartEnd = new GridBagConstraints();
        /*
         *  add start
         */
        gbcStartEnd.fill = GridBagConstraints.VERTICAL;
        gbcStartEnd.gridx = 0;
        gbcStartEnd.gridy = 0;
        gbcStartEnd.anchor = GridBagConstraints.WEST;
        final JPanel panelStartBoolean = new JPanel();
        panelStartBoolean.add(m_dialogCompStartBoolean.getComponentPanel());
        panelStartBoolean.add(new JLabel("Start:"));
        panelStartEnd.add(panelStartBoolean, gbcStartEnd);
        final JPanel panelStartSelection = new JPanel(new GridBagLayout());
        panelStartSelection.setBorder(BorderFactory.createTitledBorder(""));
        gbcStartEnd.fill = GridBagConstraints.BOTH;
        gbcStartEnd.gridx++;
        gbcStartEnd.weightx = 1;
        gbcStartEnd.insets = new Insets(7, 0, 0, 5);
        panelStartEnd.add(panelStartSelection, gbcStartEnd);
        final GridBagConstraints gbcStartSelection = new GridBagConstraints();
        gbcStartSelection.fill = GridBagConstraints.VERTICAL;
        gbcStartSelection.gridx = 0;
        gbcStartSelection.gridy = 0;
        gbcStartSelection.anchor = GridBagConstraints.WEST;
        gbcStartSelection.insets = new Insets(5, 0, 0, 5);
        gbcStartSelection.gridheight = 2;
        panelStartSelection.add(m_dialogCompStartDateTime.getComponentPanel(), gbcStartSelection);
        final JButton startButton = new JButton("Now");
        gbcStartSelection.gridx++;
        gbcStartSelection.gridheight = 1;
        gbcStartSelection.insets = new Insets(10, 10, 0, 5);
        gbcStartSelection.weightx = 1;
        panelStartSelection.add(startButton, gbcStartSelection);
        // add inclusive and always now checkboxes
        final JPanel panelStartInclusiveAlwaysNow = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcInclusiveAlwaysNow = new GridBagConstraints();
        gbcInclusiveAlwaysNow.fill = GridBagConstraints.VERTICAL;
        gbcInclusiveAlwaysNow.gridx = 0;
        gbcInclusiveAlwaysNow.gridy = 0;
        gbcInclusiveAlwaysNow.anchor = GridBagConstraints.WEST;
        gbcInclusiveAlwaysNow.insets = new Insets(0, 5, 0, 0);
        panelStartInclusiveAlwaysNow.add(m_dialogCompStartInclusive.getComponentPanel(), gbcInclusiveAlwaysNow);
        gbcInclusiveAlwaysNow.gridx++;
        panelStartInclusiveAlwaysNow.add(m_dialogCompStartAlwaysNow.getComponentPanel(), gbcInclusiveAlwaysNow);
        gbcStartSelection.gridy += 2;
        gbcStartSelection.gridwidth = 2;
        gbcStartSelection.gridx = 0;
        gbcStartSelection.insets = new Insets(0, 0, 0, 0);
        panelStartSelection.add(panelStartInclusiveAlwaysNow, gbcStartSelection);
        /*
         * add end
         */
        gbcStartEnd.insets = new Insets(15, 0, 0, 0);
        gbcStartEnd.fill = GridBagConstraints.VERTICAL;
        gbcStartEnd.gridy++;
        gbcStartEnd.gridx = 0;
        gbcStartEnd.weightx = 0;
        final JPanel panelEndBoolean = new JPanel();
        panelEndBoolean.add(m_dialogCompEndBoolean.getComponentPanel());
        panelEndBoolean.add(new JLabel("End:"));
        panelStartEnd.add(panelEndBoolean, gbcStartEnd);

        final JPanel panelEndSelection = new JPanel(new GridBagLayout());
        panelEndSelection.setBorder(BorderFactory.createTitledBorder(""));
        gbcStartEnd.fill = GridBagConstraints.BOTH;
        gbcStartEnd.gridx++;
        gbcStartEnd.insets = new Insets(20, 0, 0, 5);
        panelStartEnd.add(panelEndSelection, gbcStartEnd);
        final GridBagConstraints gbcEndSelection = new GridBagConstraints();
        gbcEndSelection.fill = GridBagConstraints.VERTICAL;
        gbcEndSelection.gridx = 0;
        gbcEndSelection.gridy = 0;
        gbcEndSelection.anchor = GridBagConstraints.WEST;
        panelEndSelection.add(m_dialogCompEndSelection.getComponentPanel(), gbcEndSelection);

        // create switching panel with CardLayout
        final JPanel panelSwitchEndSettings = new JPanel(new CardLayout());
        gbcEndSelection.gridx++;
        gbcEndSelection.weightx = 1;
        panelEndSelection.add(panelSwitchEndSettings, gbcEndSelection);

        final GridBagConstraints gbcEndSwitch = new GridBagConstraints();
        gbcEndSwitch.fill = GridBagConstraints.VERTICAL;
        gbcEndSwitch.anchor = GridBagConstraints.WEST;
        // create panel for end date&time option
        final JPanel panelEndDateTimeOption = new JPanel(new GridBagLayout());
        panelSwitchEndSettings.add(panelEndDateTimeOption, EndMode.DateTime.name());
        gbcEndSwitch.gridx = 0;
        gbcEndSwitch.gridy = 0;
        gbcEndSwitch.gridheight = 2;
        panelEndDateTimeOption.add(m_dialogCompEndDateTime.getComponentPanel(), gbcEndSwitch);
        final JButton endButton = new JButton("Now");
        gbcEndSwitch.gridx++;
        gbcEndSwitch.gridheight = 1;
        gbcEndSwitch.insets = new Insets(5, 10, 0, 5);
        panelEndDateTimeOption.add(endButton, gbcEndSwitch);
        // add inclusive and always now checkboxes
        final JPanel panelEndInclusiveAlwaysNow = new JPanel(new GridBagLayout());
        gbcInclusiveAlwaysNow.gridx = 0;
        panelEndInclusiveAlwaysNow.add(m_dialogCompEndAlwaysNow.getComponentPanel(), gbcInclusiveAlwaysNow);
        gbcEndSwitch.insets = new Insets(0, 0, 0, 5);
        gbcEndSwitch.gridx = 0;
        gbcEndSwitch.gridy += 2;
        panelEndDateTimeOption.add(panelEndInclusiveAlwaysNow, gbcEndSwitch);

        // create panel for end period/duration option
        gbcEndSwitch.gridx = 0;
        gbcEndSwitch.gridy = 0;
        final JPanel panelEndPeriodDurationOption = new JPanel(new GridBagLayout());
        panelSwitchEndSettings.add(panelEndPeriodDurationOption,
            EndMode.Duration.name());
        panelEndPeriodDurationOption.add(m_dialogCompPeriodOrDurationValue.getComponentPanel(), gbcEndSwitch);

        // create panel for numerical and granularity option
        gbcEndSwitch.gridx = 0;
        gbcEndSwitch.gridy = 0;
        final JPanel panelEndNumericalOption = new JPanel(new GridBagLayout());
        panelSwitchEndSettings.add(panelEndNumericalOption, EndMode.Numerical.name());
        panelEndNumericalOption.add(m_dialogCompNumericalValue.getComponentPanel(), gbcEndSwitch);
        gbcEndSwitch.gridy++;
        gbcEndSwitch.insets = new Insets(0, 7, 0, 0);
        gbcEndSwitch.weightx = 1;
        panelEndNumericalOption.add(m_dialogCompNumericalGranularity.getComponentPanel(), gbcEndSwitch);

        gbcEndSelection.gridx = 0;
        gbcEndSelection.gridy++;
        gbcEndSelection.gridwidth = 2;
        panelEndSelection.add(m_dialogCompEndInclusive.getComponentPanel(), gbcEndSelection);
        /*
         *  add label for warning
         */
        m_typeWarningLabel = new JLabel();
        m_typeWarningLabel.setPreferredSize(new Dimension(410, new JLabel(" ").getPreferredSize().height));
        gbcDateTimeSelection.gridy++;
        gbcDateTimeSelection.weighty = 1;
        gbcDateTimeSelection.insets = new Insets(5, 5, 5, 5);
        m_typeWarningLabel.setForeground(Color.RED);
        panelDateTimeSelection.add(m_typeWarningLabel, gbcDateTimeSelection);
        panel.add(panelDateTimeSelection, gbc);

        /*
         * add tab
         */
        addTab("Options", panel);

        /*
         * change listeners
         */
        colSelectModel.addChangeListener(e -> {
            updateEnableStatusOfDateTimeComponents(startDateTimeModel, endDateTimeModel);
        });
        startDateTimeModel.addChangeListener(e -> updateWarningLabel());
        endDateTimeModel.addChangeListener(e -> updateWarningLabel());
        m_dialogCompPeriodOrDurationValue.getModel().addChangeListener(e -> updateWarningLabel());
        m_dialogCompNumericalGranularity.getModel().addChangeListener(e -> updateWarningLabel());
        startButton.addActionListener(l -> startDateTimeModel
            .setZonedDateTime(startDateTimeModel.useMillis() ? ZonedDateTime.now() : ZonedDateTime.now().withNano(0)));
        endButton.addActionListener(l -> endDateTimeModel
            .setZonedDateTime(endDateTimeModel.useMillis() ? ZonedDateTime.now() : ZonedDateTime.now().withNano(0)));
        startAlwaysNowModel.addChangeListener(l -> {
            updateEnableStatusOfDateTimeComponents(startDateTimeModel, endDateTimeModel);
            startButton.setEnabled(!startAlwaysNowModel.getBooleanValue());
        });
        endAlwaysNowModel.addChangeListener(l -> {
            updateEnableStatusOfDateTimeComponents(startDateTimeModel, endDateTimeModel);
            endButton.setEnabled(!endAlwaysNowModel.getBooleanValue());
        });

        endSelectionModel.addChangeListener(l -> {
            final CardLayout cl = (CardLayout)panelSwitchEndSettings.getLayout();
            cl.show(panelSwitchEndSettings, endSelectionModel.getStringValue());
            updateWarningLabel();
        });

        startBooleanModel.addChangeListener(l -> {
            endBooleanModel.setEnabled(startBooleanModel.getBooleanValue());
            startInclusiveModel.setEnabled(startBooleanModel.getBooleanValue());
            startAlwaysNowModel.setEnabled(startBooleanModel.getBooleanValue());
            startButton.setEnabled(startBooleanModel.getBooleanValue() && !startAlwaysNowModel.getBooleanValue());
            updateEnableStatusOfDateTimeComponents(startDateTimeModel, endDateTimeModel);

            if (!startBooleanModel.getBooleanValue()) {
                endSelectionModel.setStringValue(EndMode.DateTime.name());
            }
            m_dialogCompEndSelection.getButton(EndMode.DateTime.name())
                .setSelected(endBooleanModel.getBooleanValue());
            m_dialogCompEndSelection.getButton(EndMode.Duration.name())
                .setEnabled(startBooleanModel.getBooleanValue() && endBooleanModel.getBooleanValue());
            m_dialogCompEndSelection.getButton(EndMode.Numerical.name())
                .setEnabled(startBooleanModel.getBooleanValue() && endBooleanModel.getBooleanValue());
            updateWarningLabel();
        });

        endBooleanModel.addChangeListener(l -> {
            startBooleanModel.setEnabled(endBooleanModel.getBooleanValue());
            endSelectionModel.setEnabled(endBooleanModel.getBooleanValue());
            endInclusiveModel.setEnabled(endBooleanModel.getBooleanValue());
            endAlwaysNowModel.setEnabled(endBooleanModel.getBooleanValue());
            endButton.setEnabled(endBooleanModel.getBooleanValue() && !endAlwaysNowModel.getBooleanValue());
            updateEnableStatusOfDateTimeComponents(startDateTimeModel, endDateTimeModel);
            if (!startBooleanModel.getBooleanValue()) {

            }
            updateWarningLabel();
        });

    }

    private void updateEnableStatusOfDateTimeComponents(final SettingsModelDateTime startDateTimeModel,
        final SettingsModelDateTime endDateTimeModel) {
        try {
            final DataType type = m_dialogCompColSelection.getSelectedAsSpec().getType();
            startDateTimeModel
                .setEnabled(!((SettingsModelBoolean)m_dialogCompStartAlwaysNow.getModel()).getBooleanValue()
                    && ((SettingsModelBoolean)m_dialogCompStartBoolean.getModel()).getBooleanValue());
            startDateTimeModel.setUseDate(!type.isCompatible(LocalTimeValue.class));
            startDateTimeModel.setUseTime(!type.isCompatible(LocalDateValue.class));
            startDateTimeModel.setUseZone(type.isCompatible(ZonedDateTimeValue.class));

            endDateTimeModel.setEnabled(!((SettingsModelBoolean)m_dialogCompEndAlwaysNow.getModel()).getBooleanValue()
                && ((SettingsModelBoolean)m_dialogCompEndBoolean.getModel()).getBooleanValue());
            endDateTimeModel.setUseDate(!type.isCompatible(LocalTimeValue.class));
            endDateTimeModel.setUseTime(!type.isCompatible(LocalDateValue.class));
            endDateTimeModel.setUseZone(type.isCompatible(ZonedDateTimeValue.class));
        } catch (NullPointerException e) {
        }
    }

    /**
     * Checks format and time settings and updates the warning label accordingly
     */
    private void updateWarningLabel() {
        m_typeWarningLabel.setText("");
        try {
            if (((SettingsModelBoolean)m_dialogCompEndBoolean.getModel()).getBooleanValue()
                && ((SettingsModelString)m_dialogCompEndSelection.getModel()).getStringValue()
                    .equals(EndMode.Duration.name())) {
                checkPeriodOrDuration();
            } else {
                ((JComponent)m_dialogCompPeriodOrDurationValue.getComponentPanel().getComponent(1))
                    .setBorder(UIManager.getBorder("TextField.border"));
            }
            if (((SettingsModelBoolean)m_dialogCompEndBoolean.getModel()).getBooleanValue()
                && ((SettingsModelString)m_dialogCompEndSelection.getModel()).getStringValue()
                    .equals(EndMode.Numerical.name())) {
                checkGranularity();
            }
            checkStartAndEnd();
        } catch (Exception e) {
            m_typeWarningLabel.setText(e.getMessage());
        }
    }

    /**
     * Checks if period or duration can be parsed
     */
    private void checkPeriodOrDuration() {
        String warning = "";
        try {
            DurationPeriodFormatUtils
                .parsePeriod(((SettingsModelString)m_dialogCompPeriodOrDurationValue.getModel()).getStringValue());
            if (m_dialogCompColSelection.getSelectedAsSpec().getType().isCompatible(LocalTimeValue.class)) {
                warning = "A date-based duration cannot be applied on a time!";
            }
        } catch (DateTimeParseException e) {
            try {
                DurationPeriodFormatUtils.parseDuration(
                    ((SettingsModelString)m_dialogCompPeriodOrDurationValue.getModel()).getStringValue());
                if (m_dialogCompColSelection.getSelectedAsSpec().getType().isCompatible(LocalDateValue.class)) {
                    warning = "A time-based duration cannot be applied on a date!";
                }
            } catch (DateTimeParseException e2) {
                warning = "Value does not represent a duration!";
            }
        }
        if (!warning.equals("")) {
            ((JComponent)m_dialogCompPeriodOrDurationValue.getComponentPanel().getComponent(1))
                .setBorder(BorderFactory.createLineBorder(Color.RED));
            m_typeWarningLabel.setText(warning);
        } else {
            ((JComponent)m_dialogCompPeriodOrDurationValue.getComponentPanel().getComponent(1))
                .setBorder(UIManager.getBorder("TextField.border"));
        }
    }

    /**
     * Checks if granularity can be applied on column
     */
    private void checkGranularity() {
        String warning = "";
        final TemporalAmount periodOrDuration =
            Granularity.fromString(((SettingsModelString)m_dialogCompNumericalGranularity.getModel()).getStringValue())
                .getPeriodOrDuration(1);
        if (periodOrDuration instanceof Period
            && m_dialogCompColSelection.getSelectedAsSpec().getType().isCompatible(LocalTimeValue.class)) {
            warning = ((SettingsModelString)m_dialogCompNumericalGranularity.getModel()).getStringValue()
                + " cannot be applied on a time!";
        }
        if (periodOrDuration instanceof Duration
            && m_dialogCompColSelection.getSelectedAsSpec().getType().isCompatible(LocalDateValue.class)) {
            warning = ((SettingsModelString)m_dialogCompNumericalGranularity.getModel()).getStringValue()
                + " cannot be applied on a date!";
        }

        if (!warning.equals("")) {
            ((JComponent)m_dialogCompNumericalGranularity.getComponentPanel().getComponent(1))
                .setBorder(BorderFactory.createLineBorder(Color.RED));
            m_typeWarningLabel.setText(warning);
        } else {
            ((JComponent)m_dialogCompNumericalGranularity.getComponentPanel().getComponent(1)).setBorder(null);
        }
    }

    /**
     * Checks if start and end string can be parsed with the set format and locale
     */
    private void checkStartAndEnd() {
        if (m_dialogCompStartDateTime.getModel().isEnabled() && m_dialogCompEndDateTime.getModel().isEnabled()
            && ((SettingsModelString)m_dialogCompEndSelection.getModel()).getStringValue()
                .equals(EndMode.DateTime.name())) {
            boolean isStartAfterEnd = false;
            final DataType dataType = m_dialogCompColSelection.getSelectedAsSpec().getType();
            if (dataType.isCompatible(LocalDateValue.class)) {
                isStartAfterEnd = ((SettingsModelDateTime)m_dialogCompStartDateTime.getModel()).getLocalDate()
                    .isAfter(((SettingsModelDateTime)m_dialogCompEndDateTime.getModel()).getLocalDate());
            }
            if (dataType.isCompatible(LocalTimeValue.class)) {
                isStartAfterEnd = ((SettingsModelDateTime)m_dialogCompStartDateTime.getModel()).getLocalTime()
                    .isAfter(((SettingsModelDateTime)m_dialogCompEndDateTime.getModel()).getLocalTime());
            }
            if (dataType.isCompatible(LocalDateTimeValue.class)) {
                isStartAfterEnd = ((SettingsModelDateTime)m_dialogCompStartDateTime.getModel()).getLocalDateTime()
                    .isAfter(((SettingsModelDateTime)m_dialogCompEndDateTime.getModel()).getLocalDateTime());
            }
            if (dataType.isCompatible(ZonedDateTimeValue.class)) {
                isStartAfterEnd = ((SettingsModelDateTime)m_dialogCompStartDateTime.getModel()).getZonedDateTime()
                    .isAfter(((SettingsModelDateTime)m_dialogCompEndDateTime.getModel()).getZonedDateTime());
            }
            if (isStartAfterEnd) {
                m_typeWarningLabel.setText("End date must not be before start date!");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        updateWarningLabel();
        if (!m_typeWarningLabel.getText().equals("")) {
            throw new InvalidSettingsException(m_typeWarningLabel.getText());
        }
        m_dialogCompColSelection.saveSettingsTo(settings);
        m_dialogCompStartBoolean.saveSettingsTo(settings);
        m_dialogCompEndBoolean.saveSettingsTo(settings);
        m_dialogCompStartDateTime.saveSettingsTo(settings);
        m_dialogCompEndDateTime.saveSettingsTo(settings);
        m_dialogCompStartInclusive.saveSettingsTo(settings);
        m_dialogCompEndInclusive.saveSettingsTo(settings);
        m_dialogCompStartAlwaysNow.saveSettingsTo(settings);
        m_dialogCompEndAlwaysNow.saveSettingsTo(settings);
        m_dialogCompEndSelection.saveSettingsTo(settings);
        m_dialogCompPeriodOrDurationValue.saveSettingsTo(settings);
        m_dialogCompNumericalValue.saveSettingsTo(settings);
        m_dialogCompNumericalGranularity.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_dialogCompStartBoolean.loadSettingsFrom(settings, specs);
        m_dialogCompEndBoolean.loadSettingsFrom(settings, specs);
        m_dialogCompStartDateTime.loadSettingsFrom(settings, specs);
        m_dialogCompEndDateTime.loadSettingsFrom(settings, specs);
        m_dialogCompStartInclusive.loadSettingsFrom(settings, specs);
        m_dialogCompEndInclusive.loadSettingsFrom(settings, specs);
        m_dialogCompStartAlwaysNow.loadSettingsFrom(settings, specs);
        m_dialogCompEndAlwaysNow.loadSettingsFrom(settings, specs);
        m_dialogCompEndSelection.loadSettingsFrom(settings, specs);
        m_dialogCompPeriodOrDurationValue.loadSettingsFrom(settings, specs);
        m_dialogCompNumericalValue.loadSettingsFrom(settings, specs);
        m_dialogCompNumericalGranularity.loadSettingsFrom(settings, specs);
        m_dialogCompColSelection.loadSettingsFrom(settings, specs);
        if (((SettingsModelString)m_dialogCompEndSelection.getModel()).getStringValue() == null) {
            ((SettingsModelString)m_dialogCompEndSelection.getModel())
                .setStringValue(EndMode.DateTime.name());
        }
        updateWarningLabel();
    }
}
