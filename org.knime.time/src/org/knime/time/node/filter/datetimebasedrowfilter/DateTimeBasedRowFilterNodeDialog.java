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
 *   Jan 20, 2017 (simon): created
 */
package org.knime.time.node.filter.datetimebasedrowfilter;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
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
import org.knime.time.Granularity;

/**
 * The node dialog of the node which filters rows based on a time window on one of the new date&time columns.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class DateTimeBasedRowFilterNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnNameSelection m_dialogCompColSelection;

    private final DialogComponentStringSelection m_dialogCompFormatSelect;

    private final DialogComponentStringSelection m_dialogCompLocale;

    private final DialogComponentBoolean m_dialogCompStartBoolean;

    private final DialogComponentBoolean m_dialogCompEndBoolean;

    private final DialogComponentString m_dialogCompDateTimeStart;

    private final DialogComponentString m_dialogCompDateTimeEnd;

    private final DialogComponentButtonGroup m_dialogCompEndSelection;

    private final DialogComponentString m_dialogCompPeriodOrDurationValue;

    private final DialogComponentNumber m_dialogCompNumericalValue;

    private final DialogComponentStringSelection m_dialogCompNumericalGranularity;

    private final DialogComponentBoolean m_dialogCompStartInclusive;

    private final DialogComponentBoolean m_dialogCompEndInclusive;

    private final DialogComponentBoolean m_dialogCompStartAlwaysNow;

    private final DialogComponentBoolean m_dialogCompEndAlwaysNow;

    private final JLabel m_typeWarningLabel;

    private final SettingsModelString m_formatModel;

    private final SettingsModelString m_localeModel;

    /** Setting up all DialogComponents. */
    @SuppressWarnings("unchecked")
    public DateTimeBasedRowFilterNodeDialog() {

        /*
         * DialogComponents
         */
        final SettingsModelString colSelectModel = DateTimeBasedRowFilterNodeModel.createColSelectModel();
        m_dialogCompColSelection = new DialogComponentColumnNameSelection(colSelectModel, "Date&Time Column: ", 0,
            LocalDateTimeValue.class, ZonedDateTimeValue.class, LocalDateValue.class, LocalTimeValue.class);

        m_formatModel = DateTimeBasedRowFilterNodeModel.createFormatModel();
        m_dialogCompFormatSelect = new DialogComponentStringSelection(m_formatModel, "Format: ",
            DateTimeBasedRowFilterNodeModel.createPredefinedFormats(), true);

        final Locale[] availableLocales = Locale.getAvailableLocales();
        final String[] availableLocalesString = new String[availableLocales.length];
        for (int i = 0; i < availableLocales.length; i++) {
            availableLocalesString[i] = availableLocales[i].toString();
        }
        Arrays.sort(availableLocalesString);
        m_localeModel = DateTimeBasedRowFilterNodeModel.createLocaleModel();
        m_dialogCompLocale = new DialogComponentStringSelection(m_localeModel, "Locale:  ", availableLocalesString);

        final SettingsModelBoolean startBooleanModel = DateTimeBasedRowFilterNodeModel.createStartBooleanModel();
        m_dialogCompStartBoolean = new DialogComponentBoolean(startBooleanModel, null);

        final SettingsModelBoolean endBooleanModel = DateTimeBasedRowFilterNodeModel.createEndBooleanModel();
        m_dialogCompEndBoolean = new DialogComponentBoolean(endBooleanModel, null);

        final SettingsModelString startDateTimeModel = DateTimeBasedRowFilterNodeModel.createDateTimeStartModel();
        m_dialogCompDateTimeStart = new DialogComponentString(startDateTimeModel, "", true, 25);

        final SettingsModelString endDateTimeModel = DateTimeBasedRowFilterNodeModel.createDateTimeEndModel();
        m_dialogCompDateTimeEnd = new DialogComponentString(endDateTimeModel, "", true, 25);

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
        m_dialogCompStartAlwaysNow = new DialogComponentBoolean(startAlwaysNowModel, "Use Execution Time");

        final SettingsModelString endSelectionModel = DateTimeBasedRowFilterNodeModel.createEndSelectionModel();
        m_dialogCompEndSelection = new DialogComponentButtonGroup(endSelectionModel, true, null,
            DateTimeBasedRowFilterNodeModel.END_OPTION_DATE_TIME,
            DateTimeBasedRowFilterNodeModel.END_OPTION_PERIOD_DURATION,
            DateTimeBasedRowFilterNodeModel.END_OPTION_NUMERICAL);

        final SettingsModelBoolean endInclusiveModel = DateTimeBasedRowFilterNodeModel.createEndInclusiveModel();
        m_dialogCompEndInclusive = new DialogComponentBoolean(endInclusiveModel, "Inclusive");

        final SettingsModelBoolean endAlwaysNowModel = DateTimeBasedRowFilterNodeModel.createEndAlwaysNowModel();
        m_dialogCompEndAlwaysNow = new DialogComponentBoolean(endAlwaysNowModel, "Use Execution Time");

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
        gbc.weighty = 1;

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
        final JPanel panelDateTimeSelection = new JPanel(new GridBagLayout());
        panelDateTimeSelection.setBorder(BorderFactory.createTitledBorder("Date&Time Selection"));
        final GridBagConstraints gbcDateTimeSelection = new GridBagConstraints();
        // add label and combo box for type selection
        gbcDateTimeSelection.fill = GridBagConstraints.VERTICAL;
        gbcDateTimeSelection.gridx = 0;
        gbcDateTimeSelection.gridy = 0;
        gbcDateTimeSelection.weightx = 1;
        gbcDateTimeSelection.anchor = GridBagConstraints.WEST;
        // add format selection
        panelDateTimeSelection.add(m_dialogCompFormatSelect.getComponentPanel(), gbcDateTimeSelection);
        // add label and combo box for locale selection
        gbcDateTimeSelection.gridy++;
        panelDateTimeSelection.add(m_dialogCompLocale.getComponentPanel(), gbcDateTimeSelection);
        // add date&time textfields
        final JPanel panelStartEnd = new JPanel(new GridBagLayout());
        gbcDateTimeSelection.fill = GridBagConstraints.BOTH;
        gbcDateTimeSelection.gridy++;
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
        panelStartSelection.add(m_dialogCompDateTimeStart.getComponentPanel(), gbcStartSelection);
        final JButton startButton = new JButton("Now");
        gbcStartSelection.gridx++;
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
        gbcStartSelection.gridy++;
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
        panelSwitchEndSettings.add(panelEndDateTimeOption, DateTimeBasedRowFilterNodeModel.END_OPTION_DATE_TIME);
        gbcEndSwitch.gridx = 0;
        gbcEndSwitch.gridy = 0;
        panelEndDateTimeOption.add(m_dialogCompDateTimeEnd.getComponentPanel(), gbcEndSwitch);
        final JButton endButton = new JButton("Now");
        gbcEndSwitch.gridx++;
        gbcEndSwitch.insets = new Insets(0, 0, 0, 5);
        panelEndDateTimeOption.add(endButton, gbcEndSwitch);
        // add inclusive and always now checkboxes
        final JPanel panelEndInclusiveAlwaysNow = new JPanel(new GridBagLayout());
        gbcInclusiveAlwaysNow.gridx = 0;
        panelEndInclusiveAlwaysNow.add(m_dialogCompEndAlwaysNow.getComponentPanel(), gbcInclusiveAlwaysNow);
        gbcEndSwitch.gridx = 0;
        gbcEndSwitch.gridy++;
        panelEndDateTimeOption.add(panelEndInclusiveAlwaysNow, gbcEndSwitch);

        // create panel for end period/duration option
        gbcEndSwitch.gridx = 0;
        gbcEndSwitch.gridy = 0;
        final JPanel panelEndPeriodDurationOption = new JPanel(new GridBagLayout());
        panelSwitchEndSettings.add(panelEndPeriodDurationOption,
            DateTimeBasedRowFilterNodeModel.END_OPTION_PERIOD_DURATION);
        panelEndPeriodDurationOption.add(m_dialogCompPeriodOrDurationValue.getComponentPanel(), gbcEndSwitch);

        // create panel for numerical and granularity option
        gbcEndSwitch.gridx = 0;
        gbcEndSwitch.gridy = 0;
        final JPanel panelEndNumericalOption = new JPanel(new GridBagLayout());
        panelSwitchEndSettings.add(panelEndNumericalOption, DateTimeBasedRowFilterNodeModel.END_OPTION_NUMERICAL);
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
            if (!checkFormat()) {
                m_formatModel.setStringValue(guessFormat());
            } else {
                updateWarningLabel();
            }
        });
        m_formatModel.addChangeListener(e -> updateWarningLabel());
        m_localeModel.addChangeListener(e -> updateWarningLabel());
        startDateTimeModel.addChangeListener(e -> updateWarningLabel());
        endDateTimeModel.addChangeListener(e -> updateWarningLabel());
        m_dialogCompPeriodOrDurationValue.getModel().addChangeListener(e -> updateWarningLabel());
        m_dialogCompNumericalGranularity.getModel().addChangeListener(e -> updateWarningLabel());
        startButton.addActionListener(l -> startDateTimeModel.setStringValue(getTimeNow()));
        endButton.addActionListener(l -> endDateTimeModel.setStringValue(getTimeNow()));
        startAlwaysNowModel.addChangeListener(l -> {
            startDateTimeModel.setEnabled(!startAlwaysNowModel.getBooleanValue());
            startButton.setEnabled(!startAlwaysNowModel.getBooleanValue());
        });
        endAlwaysNowModel.addChangeListener(l -> {
            endDateTimeModel.setEnabled(!endAlwaysNowModel.getBooleanValue());
            endButton.setEnabled(!endAlwaysNowModel.getBooleanValue());
        });

        endSelectionModel.addChangeListener(l -> {
            final CardLayout cl = (CardLayout)panelSwitchEndSettings.getLayout();
            cl.show(panelSwitchEndSettings, endSelectionModel.getStringValue());
            updateWarningLabel();
        });

        startBooleanModel.addChangeListener(l -> {
            endBooleanModel.setEnabled(startBooleanModel.getBooleanValue());
            enableComponents(panelStartSelection, startBooleanModel.getBooleanValue());
            if (startBooleanModel.getBooleanValue()) {
                enableComponents(m_dialogCompDateTimeStart.getComponentPanel(), !startAlwaysNowModel.getBooleanValue());
                startDateTimeModel.setEnabled(!startAlwaysNowModel.getBooleanValue());
                startButton.setEnabled(!startAlwaysNowModel.getBooleanValue());
            } else {
                m_dialogCompEndSelection.getButton(DateTimeBasedRowFilterNodeModel.END_OPTION_DATE_TIME)
                    .setSelected(true);
                endSelectionModel.setStringValue(DateTimeBasedRowFilterNodeModel.END_OPTION_DATE_TIME);
            }
            m_dialogCompEndSelection.getButton(DateTimeBasedRowFilterNodeModel.END_OPTION_PERIOD_DURATION)
                .setEnabled(startBooleanModel.getBooleanValue() && endBooleanModel.getBooleanValue());
            m_dialogCompEndSelection.getButton(DateTimeBasedRowFilterNodeModel.END_OPTION_NUMERICAL)
                .setEnabled(startBooleanModel.getBooleanValue() && endBooleanModel.getBooleanValue());
            updateWarningLabel();
        });

        endBooleanModel.addChangeListener(l -> {
            startBooleanModel.setEnabled(endBooleanModel.getBooleanValue());
            enableComponents(panelEndSelection, endBooleanModel.getBooleanValue());
            if (endBooleanModel.getBooleanValue()) {
                enableComponents(m_dialogCompDateTimeEnd.getComponentPanel(), !endAlwaysNowModel.getBooleanValue());
                endDateTimeModel.setEnabled(!endAlwaysNowModel.getBooleanValue());
                endButton.setEnabled(!endAlwaysNowModel.getBooleanValue());
            }
            updateWarningLabel();
        });

    }

    private void enableComponents(final Container container, final boolean enable) {
        final Component[] components = container.getComponents();
        for (Component component : components) {
            component.setEnabled(enable);
            if (component instanceof Container) {
                enableComponents((Container)component, enable);
            }
        }
    }

    /**
     * Guesses an appropriate format according to the selected data type
     *
     * @return guessed format
     */
    private String guessFormat() {
        if (m_dialogCompColSelection.getSelectedAsSpec() != null) {
            final DataType dataType = m_dialogCompColSelection.getSelectedAsSpec().getType();
            final Collection<String> formats = DateTimeBasedRowFilterNodeModel.createPredefinedFormats();
            for (final String format : formats) {
                final DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(format, new Locale(m_localeModel.getStringValue()));
                if (dataType.equals(ZonedDateTimeCellFactory.TYPE)) {
                    try {
                        final String string = ZonedDateTime.now().format(formatter);
                        ZonedDateTime.parse(string, formatter);
                        return format;
                    } catch (DateTimeException e) {
                    }
                }
                if (dataType.equals(LocalDateTimeCellFactory.TYPE)) {
                    try {
                        final String string = LocalDateTime.now().format(formatter);
                        LocalDateTime.parse(string, formatter);
                        return format;
                    } catch (DateTimeException e) {
                    }
                }
                if (dataType.equals(LocalDateCellFactory.TYPE)) {
                    try {
                        final String string = LocalDate.now().format(formatter);
                        LocalDate.parse(string, formatter);
                        return format;
                    } catch (DateTimeException e) {
                    }
                }
                if (dataType.equals(LocalTimeCellFactory.TYPE)) {
                    try {
                        final String string = LocalTime.now().format(formatter);
                        LocalTime.parse(string, formatter);
                        return format;
                    } catch (DateTimeException e) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks format and time settings and updates the warning label accordingly
     */
    private void updateWarningLabel() {
        m_typeWarningLabel.setText("");
        try {
            DateTimeFormatter.ofPattern(m_formatModel.getStringValue(), new Locale(m_localeModel.getStringValue()));
            if (((SettingsModelBoolean)m_dialogCompEndBoolean.getModel()).getBooleanValue()
                && ((SettingsModelString)m_dialogCompEndSelection.getModel()).getStringValue()
                    .equals(DateTimeBasedRowFilterNodeModel.END_OPTION_PERIOD_DURATION)) {
                checkPeriodOrDuration();
            } else {
                ((JComponent)m_dialogCompPeriodOrDurationValue.getComponentPanel().getComponent(1))
                    .setBorder(UIManager.getBorder("TextField.border"));
            }
            if (((SettingsModelBoolean)m_dialogCompEndBoolean.getModel()).getBooleanValue()
                && ((SettingsModelString)m_dialogCompEndSelection.getModel()).getStringValue()
                    .equals(DateTimeBasedRowFilterNodeModel.END_OPTION_NUMERICAL)) {
                checkGranularity();
            }
            checkStartAndEnd(((SettingsModelString)m_dialogCompDateTimeStart.getModel()).getStringValue(),
                ((SettingsModelString)m_dialogCompDateTimeEnd.getModel()).getStringValue());
            checkFormat();
        } catch (IllegalArgumentException e) {
            m_typeWarningLabel.setText(e.getMessage());
        } catch (NullPointerException e) {
            ((JTextField)m_dialogCompDateTimeStart.getComponentPanel().getComponent(1))
                .setBorder(UIManager.getBorder("TextField.border"));
            ((JTextField)m_dialogCompDateTimeEnd.getComponentPanel().getComponent(1))
                .setBorder(UIManager.getBorder("TextField.border"));
        }
    }

    /**
     * Checks if period or duration can be parsed
     */
    private void checkPeriodOrDuration() {
        String warning = "";
        try {
            Period.parse(((SettingsModelString)m_dialogCompPeriodOrDurationValue.getModel()).getStringValue());
            if (m_dialogCompColSelection.getSelectedAsSpec().getType().equals(LocalTimeCellFactory.TYPE)) {
                warning = "A period cannot be applied on a LocalTime column!";
            }
        } catch (DateTimeParseException e) {
            try {
                Duration.parse(((SettingsModelString)m_dialogCompPeriodOrDurationValue.getModel()).getStringValue());
                if (m_dialogCompColSelection.getSelectedAsSpec().getType().equals(LocalDateCellFactory.TYPE)) {
                    warning = "A duration cannot be applied on a LocalDate column!";
                }
            } catch (DateTimeParseException e2) {
                warning = "Value does not represent a period or duration!";
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
        final Object periodOrDuration =
            Granularity.fromString(((SettingsModelString)m_dialogCompNumericalGranularity.getModel()).getStringValue())
                .getPeriodOrDuration(1);
        if (periodOrDuration instanceof Period
            && m_dialogCompColSelection.getSelectedAsSpec().getType().equals(LocalTimeCellFactory.TYPE)) {
            warning = ((SettingsModelString)m_dialogCompNumericalGranularity.getModel()).getStringValue()
                + " cannot be applied on a LocalTime column!";
        }
        if (periodOrDuration instanceof Duration
            && m_dialogCompColSelection.getSelectedAsSpec().getType().equals(LocalDateCellFactory.TYPE)) {
            warning = ((SettingsModelString)m_dialogCompNumericalGranularity.getModel()).getStringValue()
                + " cannot be applied on a LocalDate column!";
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
     * Checks the format
     *
     * @return if format goes with data type
     */
    private boolean checkFormat() {
        try {
            if (m_dialogCompColSelection.getSelectedAsSpec() != null) {
                final DataType dataType = m_dialogCompColSelection.getSelectedAsSpec().getType();
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(m_formatModel.getStringValue(),
                    new Locale(m_localeModel.getStringValue()));
                if (dataType.equals(ZonedDateTimeCellFactory.TYPE)) {
                    try {
                        final String string = ZonedDateTime.now().format(formatter);
                        ZonedDateTime.parse(string, formatter);
                        ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1)).setBorder(null);
                        return true;
                    } catch (DateTimeException e) {
                    }
                }
                if (dataType.equals(LocalDateTimeCellFactory.TYPE)) {
                    try {
                        final String string = LocalDateTime.now().format(formatter);
                        LocalDateTime.parse(string, formatter);
                        ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1)).setBorder(null);
                        return true;
                    } catch (DateTimeException e) {
                    }
                }
                if (dataType.equals(LocalDateCellFactory.TYPE)) {
                    try {
                        final String string = LocalDate.now().format(formatter);
                        LocalDate.parse(string, formatter);
                        ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1)).setBorder(null);
                        return true;
                    } catch (DateTimeException e) {
                    }
                }
                if (dataType.equals(LocalTimeCellFactory.TYPE)) {
                    try {
                        final String string = LocalTime.now().format(formatter);
                        LocalTime.parse(string, formatter);
                        ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1)).setBorder(null);
                        return true;
                    } catch (DateTimeException e) {
                    }
                }
            }
            ((JComponent)m_dialogCompFormatSelect.getComponentPanel().getComponent(1))
                .setBorder(BorderFactory.createLineBorder(Color.RED));
            m_typeWarningLabel.setText("Format does not fit the data type!");
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Formats the current time according to the set format and locale
     *
     * @return formatted current time
     */
    private String getTimeNow() {
        final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern(m_formatModel.getStringValue(), new Locale(m_localeModel.getStringValue()));
        return formatter.format(ZonedDateTime.now());
    }

    /**
     * Checks if start and end string can be parsed with the set format and locale
     */
    private void checkStartAndEnd(final String start, final String end) {
        boolean startOk = false;
        boolean needToCheckIfStartIsBefore = true;
        if (!((SettingsModelBoolean)m_dialogCompStartBoolean.getModel()).getBooleanValue()
            || ((SettingsModelBoolean)m_dialogCompStartAlwaysNow.getModel()).getBooleanValue()) {
            ((JTextField)m_dialogCompDateTimeStart.getComponentPanel().getComponent(1))
                .setBorder(UIManager.getBorder("TextField.border"));
            startOk = true;
            needToCheckIfStartIsBefore = false;
        } else {
            if (start != null && checkDateTimeString(start)) {
                ((JTextField)m_dialogCompDateTimeStart.getComponentPanel().getComponent(1))
                    .setBorder(UIManager.getBorder("TextField.border"));
                startOk = true;
            } else {
                ((JTextField)m_dialogCompDateTimeStart.getComponentPanel().getComponent(1))
                    .setBorder(BorderFactory.createLineBorder(Color.RED));
            }
        }

        boolean endOk = false;
        if (!((SettingsModelString)m_dialogCompEndSelection.getModel()).getStringValue()
            .equals(DateTimeBasedRowFilterNodeModel.END_OPTION_DATE_TIME)
            || !((SettingsModelBoolean)m_dialogCompEndBoolean.getModel()).getBooleanValue()
            || ((SettingsModelBoolean)m_dialogCompEndAlwaysNow.getModel()).getBooleanValue()) {
            ((JTextField)m_dialogCompDateTimeEnd.getComponentPanel().getComponent(1))
                .setBorder(UIManager.getBorder("TextField.border"));
            endOk = true;
            needToCheckIfStartIsBefore = false;
        } else {
            if (end != null && checkDateTimeString(end)) {
                ((JTextField)m_dialogCompDateTimeEnd.getComponentPanel().getComponent(1))
                    .setBorder(UIManager.getBorder("TextField.border"));
                endOk = true;
            } else {
                ((JTextField)m_dialogCompDateTimeEnd.getComponentPanel().getComponent(1))
                    .setBorder(BorderFactory.createLineBorder(Color.RED));
            }
        }

        if (!startOk || !endOk) {
            if (!startOk) {
                if (!endOk) {
                    m_typeWarningLabel.setText("Start and end date do not fit the format and locale!");
                } else {
                    m_typeWarningLabel.setText("Start date does not fit the format and locale!");
                }
            } else {
                m_typeWarningLabel.setText("End date does not fit the format and locale!");
            }
        } else {
            if (needToCheckIfStartIsBefore) {
                if (((SettingsModelBoolean)m_dialogCompStartBoolean.getModel()).getBooleanValue()
                    && ((SettingsModelBoolean)m_dialogCompEndBoolean.getModel()).getBooleanValue()) {
                    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(m_formatModel.getStringValue(),
                        new Locale(m_localeModel.getStringValue()));
                    boolean isAfter = false;
                    final DataType dataType = m_dialogCompColSelection.getSelectedAsSpec().getType();
                    if (dataType.equals(LocalDateCellFactory.TYPE)) {
                        isAfter = LocalDate.parse(start, formatter).isAfter(LocalDate.parse(end, formatter));
                    }
                    if (dataType.equals(LocalTimeCellFactory.TYPE)) {
                        isAfter = LocalTime.parse(start, formatter).isAfter(LocalTime.parse(end, formatter));
                    }
                    if (dataType.equals(LocalDateTimeCellFactory.TYPE)) {
                        isAfter = LocalDateTime.parse(start, formatter).isAfter(LocalDateTime.parse(end, formatter));
                    }
                    if (dataType.equals(ZonedDateTimeCellFactory.TYPE)) {
                        isAfter = ZonedDateTime.parse(start, formatter).isAfter(ZonedDateTime.parse(end, formatter));
                    }
                    if (isAfter) {
                        m_typeWarningLabel.setText("End date must not be before start date!");
                    }
                }
            }
        }
    }

    /**
     * Checks if a string can be parsed with the set format and locale
     */
    private boolean checkDateTimeString(final String string) {
        final DataType dataType;
        try {
            dataType = m_dialogCompColSelection.getSelectedAsSpec().getType();
        } catch (NullPointerException e) {
            return false;
        }
        final DateTimeFormatter formatter;
        try {
            formatter =
                DateTimeFormatter.ofPattern(m_formatModel.getStringValue(), new Locale(m_localeModel.getStringValue()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (dataType.equals(ZonedDateTimeCellFactory.TYPE)) {
            try {
                ZonedDateTime.parse(string, formatter);
                return true;
            } catch (DateTimeException e) {
            }
        }
        if (dataType.equals(LocalDateTimeCellFactory.TYPE)) {
            try {
                LocalDateTime.parse(string, formatter);
                return true;
            } catch (DateTimeException e) {
            }
        }
        if (dataType.equals(LocalDateCellFactory.TYPE)) {
            try {
                LocalDate.parse(string, formatter);
                return true;
            } catch (DateTimeException e) {
            }
        }
        if (dataType.equals(LocalTimeCellFactory.TYPE)) {
            try {
                LocalTime.parse(string, formatter);
                return true;
            } catch (DateTimeException e) {
            }
        }
        return false;
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
        m_dialogCompFormatSelect.saveSettingsTo(settings);
        m_dialogCompLocale.saveSettingsTo(settings);
        m_dialogCompStartBoolean.saveSettingsTo(settings);
        m_dialogCompEndBoolean.saveSettingsTo(settings);
        m_dialogCompDateTimeStart.saveSettingsTo(settings);
        m_dialogCompDateTimeEnd.saveSettingsTo(settings);
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
        m_dialogCompColSelection.loadSettingsFrom(settings, specs);
        m_dialogCompFormatSelect.loadSettingsFrom(settings, specs);
        m_dialogCompLocale.loadSettingsFrom(settings, specs);
        m_dialogCompStartBoolean.loadSettingsFrom(settings, specs);
        m_dialogCompEndBoolean.loadSettingsFrom(settings, specs);
        m_dialogCompDateTimeStart.loadSettingsFrom(settings, specs);
        m_dialogCompDateTimeEnd.loadSettingsFrom(settings, specs);
        m_dialogCompStartInclusive.loadSettingsFrom(settings, specs);
        m_dialogCompEndInclusive.loadSettingsFrom(settings, specs);
        m_dialogCompStartAlwaysNow.loadSettingsFrom(settings, specs);
        m_dialogCompEndAlwaysNow.loadSettingsFrom(settings, specs);
        m_dialogCompEndSelection.loadSettingsFrom(settings, specs);
        m_dialogCompPeriodOrDurationValue.loadSettingsFrom(settings, specs);
        m_dialogCompNumericalValue.loadSettingsFrom(settings, specs);
        m_dialogCompNumericalGranularity.loadSettingsFrom(settings, specs);
        if (((SettingsModelString)m_dialogCompEndSelection.getModel()).getStringValue() == null) {
            ((SettingsModelString)m_dialogCompEndSelection.getModel()).setStringValue(DateTimeBasedRowFilterNodeModel.END_OPTION_DATE_TIME);
        }
        // kind of a hack: change values of the start and end bool to notify change listeners
        final String selected = ((SettingsModelString)m_dialogCompEndSelection.getModel()).getStringValue();
        ((SettingsModelBoolean)m_dialogCompEndBoolean.getModel())
            .setBooleanValue(!((SettingsModelBoolean)m_dialogCompEndBoolean.getModel()).getBooleanValue());
        ((SettingsModelBoolean)m_dialogCompEndBoolean.getModel())
            .setBooleanValue(!((SettingsModelBoolean)m_dialogCompEndBoolean.getModel()).getBooleanValue());
        ((SettingsModelBoolean)m_dialogCompStartBoolean.getModel())
            .setBooleanValue(!((SettingsModelBoolean)m_dialogCompStartBoolean.getModel()).getBooleanValue());
        ((SettingsModelBoolean)m_dialogCompStartBoolean.getModel())
            .setBooleanValue(!((SettingsModelBoolean)m_dialogCompStartBoolean.getModel()).getBooleanValue());
        ((SettingsModelString)m_dialogCompEndSelection.getModel()).setStringValue(selected);

        if (((SettingsModelString)m_dialogCompFormatSelect.getModel()).getStringValue().equals("")) {
            ((SettingsModelString)m_dialogCompFormatSelect.getModel()).setStringValue(guessFormat());
            ((SettingsModelString)m_dialogCompDateTimeStart.getModel()).setStringValue(getTimeNow());
            ((SettingsModelString)m_dialogCompDateTimeEnd.getModel()).setStringValue(getTimeNow());
        }
        if (!checkFormat()) {
            ((SettingsModelString)m_dialogCompFormatSelect.getModel()).setStringValue(guessFormat());
        }
        updateWarningLabel();
    }
}
