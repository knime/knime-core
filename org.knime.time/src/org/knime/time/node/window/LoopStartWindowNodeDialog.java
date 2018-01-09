/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 3, 2010 (wiswedel): created
 */
package org.knime.time.node.window;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.time.node.window.LoopStartWindowConfiguration.Trigger;
import org.knime.time.node.window.LoopStartWindowConfiguration.Unit;
import org.knime.time.node.window.LoopStartWindowConfiguration.WindowDefinition;
import org.knime.time.util.DialogComponentDateTimeSelection;
import org.knime.time.util.DialogComponentDateTimeSelection.DisplayOption;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.SettingsModelDateTime;

/**
 * Dialog pane for the Window Loop Start node.
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
final class LoopStartWindowNodeDialog extends NodeDialogPane {

    /* Windowing definition */
    private final JRadioButton m_forwardRButton;

    private final JRadioButton m_centralRButton;

    private final JRadioButton m_backwardRButton;

    /* Triggering */
    private final JRadioButton m_rowTrigRButton;

    private final JRadioButton m_timeTrigRButton;

    /* Event Triggered */
    private final JSpinner m_stepSizeSpinner;

    private final JSpinner m_windowSizeSpinner;

    private final JLabel m_stepSizeLabel;

    private final JLabel m_windowSizeLabel;

    private final JCheckBox m_limitWindowCheckBox;

    /* Time Triggered*/
    private final JLabel m_windowTimeLabel;

    private final JLabel m_startTimeLabel;

    private final JTextField m_windowSizeTime;

    private final JTextField m_stepSizeTime;

    private final JCheckBox m_useSpecifiedStartTimeCheckBox;

    private final DialogComponentDateTimeSelection m_specifiedStartTime;

    private final DialogComponentColumnNameSelection m_columnSelector;

    private final JComboBox<Unit> m_timeWindowUnit;

    private final JComboBox<Unit> m_startTimeUnit;

    private final JLabel m_inLabel;

    private final JLabel m_inLabel2;

    final SettingsModelDateTime m_modelStart = LoopStartWindowNodeModel.createStartModel();

    /**
     *
     */
    public LoopStartWindowNodeDialog() {
        ButtonGroup bg = new ButtonGroup();

        m_forwardRButton = new JRadioButton("Forward");
        m_backwardRButton = new JRadioButton("Backward");
        m_centralRButton = new JRadioButton("Central");

        bg.add(m_forwardRButton);
        bg.add(m_centralRButton);
        bg.add(m_backwardRButton);

        bg = new ButtonGroup();
        m_rowTrigRButton = new JRadioButton("Row based");
        m_timeTrigRButton = new JRadioButton("Time based");

        bg.add(m_rowTrigRButton);
        bg.add(m_timeTrigRButton);

        m_windowSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, Integer.MAX_VALUE, 2));
        m_stepSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, Integer.MAX_VALUE, 2));

        m_windowSizeTime = new JTextField(14);
        m_stepSizeTime = new JTextField(14);

        m_stepSizeLabel = new JLabel("Step size");
        m_windowSizeLabel = new JLabel("Window size");

        m_windowTimeLabel = new JLabel("Window size");
        m_startTimeLabel = new JLabel("Step size");

        m_limitWindowCheckBox = new JCheckBox("Limit window to table");
        m_limitWindowCheckBox.setSelected(true);

        m_useSpecifiedStartTimeCheckBox = new JCheckBox("Start at:");
        m_useSpecifiedStartTimeCheckBox.setSelected(false);

        m_specifiedStartTime =
            new DialogComponentDateTimeSelection(m_modelStart, null, DisplayOption.SHOW_DATE_AND_TIME_AND_TIMEZONE);

        m_startTimeUnit = new JComboBox<>(Unit.values());
        m_timeWindowUnit = new JComboBox<>(Unit.values());

        m_inLabel = new JLabel("in");
        m_inLabel2 = new JLabel("in");

        m_columnSelector =
            new DialogComponentColumnNameSelection(LoopStartWindowNodeModel.createColumnModel(), "Time column", 0,
                false, LocalTimeValue.class, LocalDateTimeValue.class, LocalDateValue.class, ZonedDateTimeValue.class);

        ActionListener triggerListener = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                /* Time triggered */
                m_windowSizeTime.setEnabled(!m_rowTrigRButton.isSelected());
                m_columnSelector.getModel().setEnabled(!m_rowTrigRButton.isSelected());
                m_stepSizeTime.setEnabled(!m_rowTrigRButton.isSelected());
                m_useSpecifiedStartTimeCheckBox.setEnabled(!m_rowTrigRButton.isSelected());
                m_modelStart.setEnabled(!m_rowTrigRButton.isSelected() && m_useSpecifiedStartTimeCheckBox.isSelected());
                m_startTimeUnit.setEnabled(!m_rowTrigRButton.isSelected());
                m_timeWindowUnit.setEnabled(!m_rowTrigRButton.isSelected());

                /* Event triggered */
                m_windowSizeSpinner.setEnabled(m_rowTrigRButton.isSelected());
                m_stepSizeSpinner.setEnabled(m_rowTrigRButton.isSelected());
                m_limitWindowCheckBox.setEnabled(m_rowTrigRButton.isSelected());
            }
        };

        /* Listener for start time. */
        ActionListener selectorListener = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_columnSelector.getSelectedAsSpec() != null) {

                    if (m_columnSelector.getSelectedAsSpec().getType().equals(DataType.getType(LocalDateCell.class))) {
                        m_modelStart.setUseDate(true);
                        m_modelStart.setUseTime(false);
                        m_modelStart.setUseZone(false);
                    } else if (m_columnSelector.getSelectedAsSpec().getType()
                        .equals(DataType.getType(LocalDateTimeCell.class))) {
                        m_modelStart.setUseDate(true);
                        m_modelStart.setUseTime(true);
                        m_modelStart.setUseZone(false);
                    } else if (m_columnSelector.getSelectedAsSpec().getType()
                        .equals(DataType.getType(LocalTimeCell.class))) {
                        m_modelStart.setUseDate(false);
                        m_modelStart.setUseTime(true);
                        m_modelStart.setUseZone(false);
                    } else if (m_columnSelector.getSelectedAsSpec().getType()
                        .equals(DataType.getType(ZonedDateTimeCell.class))) {
                        m_modelStart.setUseDate(true);
                        m_modelStart.setUseTime(true);
                        m_modelStart.setUseZone(true);
                    }

                } else {
                    m_specifiedStartTime.getComponentPanel().setEnabled(false);
                }

            }
        };

        /* TODO: hardcoded */
        ((ColumnSelectionPanel)m_columnSelector.getComponentPanel().getComponent(1))
            .addActionListener(selectorListener);

        m_rowTrigRButton.addActionListener(triggerListener);
        m_timeTrigRButton.addActionListener(triggerListener);
        m_limitWindowCheckBox.addActionListener(triggerListener);
        m_useSpecifiedStartTimeCheckBox.addActionListener(triggerListener);

        m_forwardRButton.doClick();
        m_rowTrigRButton.doClick();

        initLayout();
    }

    /**
     * Initiates the layout.
     */
    private void initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints constraint = new GridBagConstraints();

        constraint.anchor = GridBagConstraints.LINE_START;
        constraint.gridx = 1;
        constraint.gridy = 1;
        constraint.fill = GridBagConstraints.HORIZONTAL;

        /* Trigger sub-panel*/
        JPanel triggerPanel = new JPanel(new GridBagLayout());

        GridBagConstraints subConstraint = new GridBagConstraints();
        subConstraint.ipadx = 2;
        subConstraint.ipady = 5;
        subConstraint.insets = new Insets(2, 2, 2, 2);
        subConstraint.weightx = 1;
        subConstraint.weighty = 1;
        subConstraint.gridx = 1;
        subConstraint.gridy = 1;

        triggerPanel.add(m_rowTrigRButton, subConstraint);

        subConstraint.gridx++;
        triggerPanel.add(m_timeTrigRButton, subConstraint);

        panel.add(triggerPanel, constraint);

        /* Event sub-panel */
        JPanel rowPanel = new JPanel(new GridBagLayout());

        subConstraint.gridx = 1;
        subConstraint.gridy = 1;

        rowPanel.add(m_windowSizeLabel, subConstraint);

        subConstraint.gridx++;
        rowPanel.add(m_windowSizeSpinner, subConstraint);

        subConstraint.gridx--;
        subConstraint.gridy++;
        rowPanel.add(m_stepSizeLabel, subConstraint);

        subConstraint.gridx++;
        rowPanel.add(m_stepSizeSpinner, subConstraint);

        rowPanel.setBorder(BorderFactory.createTitledBorder("Row based"));

        constraint.gridy++;
        panel.add(rowPanel, constraint);

        /* Time sub-panel */
        JPanel timePanel = new JPanel(new GridBagLayout());

        subConstraint.gridx = 1;
        subConstraint.gridy = 1;

        Component[] comp = m_columnSelector.getComponentPanel().getComponents();
        timePanel.add(comp[0], subConstraint);

        subConstraint.gridx++;
        subConstraint.fill = GridBagConstraints.HORIZONTAL;
        timePanel.add(comp[1], subConstraint);

        subConstraint.gridx--;
        subConstraint.gridy++;
        subConstraint.fill = GridBagConstraints.NONE;
        timePanel.add(m_windowTimeLabel, subConstraint);

        subConstraint.gridx++;
        subConstraint.fill = GridBagConstraints.HORIZONTAL;
        timePanel.add(m_windowSizeTime, subConstraint);

        subConstraint.gridx++;
        timePanel.add(m_inLabel, subConstraint);

        subConstraint.gridx++;
        timePanel.add(m_timeWindowUnit, subConstraint);

        subConstraint.gridx -= 3;
        subConstraint.gridy++;
        subConstraint.fill = GridBagConstraints.NONE;
        timePanel.add(m_startTimeLabel, subConstraint);

        subConstraint.gridx++;
        subConstraint.fill = GridBagConstraints.HORIZONTAL;
        timePanel.add(m_stepSizeTime, subConstraint);

        subConstraint.gridx++;
        timePanel.add(m_inLabel2, subConstraint);

        subConstraint.gridx++;
        timePanel.add(m_startTimeUnit, subConstraint);

        subConstraint.gridx -= 3;
        subConstraint.gridy++;
        subConstraint.fill = GridBagConstraints.NONE;
        timePanel.add(m_useSpecifiedStartTimeCheckBox, subConstraint);

        subConstraint.gridx++;
        subConstraint.fill = GridBagConstraints.HORIZONTAL;
        timePanel.add(m_specifiedStartTime.getComponentPanel(), subConstraint);

        timePanel.setBorder(BorderFactory.createTitledBorder("Time based"));

        constraint.gridy++;
        panel.add(timePanel, constraint);

        addTab("Options", panel);

        /* Advanced Panel */
        JPanel advancedPanel = new JPanel(new GridBagLayout());

        constraint = new GridBagConstraints();
        constraint.gridx = 1;
        constraint.gridy = 1;
        constraint.fill = GridBagConstraints.BOTH;
        constraint.weightx = 1;
        constraint.weighty = 0.5;

        rowPanel = new JPanel();

        subConstraint.gridx = 1;
        subConstraint.gridy = 1;
        subConstraint.fill = GridBagConstraints.VERTICAL;
        rowPanel.add(m_limitWindowCheckBox, subConstraint);

        rowPanel.setBorder(BorderFactory.createTitledBorder("Row based"));
        advancedPanel.add(rowPanel, constraint);

        /* Window definition sub-panel */
        JPanel windowDefinitionPanel = new JPanel(new GridBagLayout());

        subConstraint.gridx = 1;
        subConstraint.gridy = 1;
        windowDefinitionPanel.add(m_forwardRButton, subConstraint);

        subConstraint.gridx++;
        windowDefinitionPanel.add(m_centralRButton, subConstraint);

        subConstraint.gridx++;
        windowDefinitionPanel.add(m_backwardRButton, subConstraint);

        windowDefinitionPanel.setBorder(BorderFactory.createTitledBorder("Windowing"));

        constraint.gridy++;
        advancedPanel.add(windowDefinitionPanel, constraint);

        addTab("Advanced", advancedPanel);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        LoopStartWindowConfiguration config = new LoopStartWindowConfiguration();
        config.loadSettingsInDialog(settings);

        m_windowSizeSpinner.setValue(config.getEventWindowSize());
        m_stepSizeSpinner.setValue(config.getEventStepSize());
        m_limitWindowCheckBox.setSelected(config.getLimitWindow());
        m_useSpecifiedStartTimeCheckBox.setSelected(config.useSpecifiedStartTime());
        m_startTimeUnit.setSelectedItem(config.getTimeStepUnit());
        m_timeWindowUnit.setSelectedItem(config.getTimeWindowUnit());

        switch (config.getWindowDefinition()) {
            case FORWARD:
                m_forwardRButton.doClick();
                break;
            case BACKWARD:
                m_backwardRButton.doClick();
                break;
            default:
                m_centralRButton.doClick();
        }

        switch (config.getTrigger()) {
            case ROW:
                m_rowTrigRButton.doClick();
                break;
            default:
                m_timeTrigRButton.doClick();
                m_stepSizeTime.setText(config.getTimeStepSize());
                m_windowSizeTime.setText(config.getTimeWindowSize());
        }

        m_columnSelector.loadSettingsFrom(settings, specs);

        if (m_columnSelector.getSelectedAsSpec() != null) {
            m_specifiedStartTime.loadSettingsFrom(settings, specs);

            if (m_columnSelector.getSelectedAsSpec().getType().equals(DataType.getType(LocalDateCell.class))) {
                m_modelStart.setUseDate(true);
                m_modelStart.setUseTime(false);
                m_modelStart.setUseZone(false);
            } else if (m_columnSelector.getSelectedAsSpec().getType()
                .equals(DataType.getType(LocalDateTimeCell.class))) {
                m_modelStart.setUseDate(true);
                m_modelStart.setUseTime(true);
                m_modelStart.setUseZone(false);
            } else if (m_columnSelector.getSelectedAsSpec().getType().equals(DataType.getType(LocalTimeCell.class))) {
                m_modelStart.setUseDate(false);
                m_modelStart.setUseTime(true);
                m_modelStart.setUseZone(false);
            } else if (m_columnSelector.getSelectedAsSpec().getType()
                .equals(DataType.getType(ZonedDateTimeCell.class))) {
                m_modelStart.setUseDate(true);
                m_modelStart.setUseTime(true);
                m_modelStart.setUseZone(true);
            }
        } else {
            m_modelStart.setEnabled(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        LoopStartWindowConfiguration config = new LoopStartWindowConfiguration();
        config.setEventWindowSize((Integer)m_windowSizeSpinner.getValue());
        config.setEventStepSize((Integer)m_stepSizeSpinner.getValue());
        config.setLimitWindow(m_limitWindowCheckBox.isSelected());

        if (m_forwardRButton.isSelected()) {
            config.setWindowDefinition(WindowDefinition.FORWARD);
        } else if (m_backwardRButton.isSelected()) {
            config.setWindowDefinition(WindowDefinition.BACKWARD);
        } else {
            config.setWindowDefinition(WindowDefinition.CENTRAL);
        }

        if (m_rowTrigRButton.isSelected()) {
            config.setTrigger(Trigger.ROW);
        } else {
            config.setTrigger(Trigger.TIME);

            if (m_columnSelector == null || m_columnSelector.getSelectedAsSpec() == null) {
                throw new InvalidSettingsException("No valid time column has been chosen");
            }

            /* Check if step size is smaller than 0. */
            try {
                if (m_stepSizeTime.getText() != null && Double.parseDouble(m_stepSizeTime.getText()) < 0) {
                   throw new InvalidSettingsException("Step size '"+m_stepSizeTime.getText()+"' invalid. Step size must be greater than 0.");
                }
            } catch (NumberFormatException e) {

            }
            try {
                if (m_windowSizeTime.getText() != null && Double.parseDouble(m_windowSizeTime.getText()) < 0) {
                    throw new InvalidSettingsException("Window size '"+m_windowSizeTime.getText()+"' invalid. Window size must be greater than 0.");
                }
            } catch (NumberFormatException e) {

            }

            /* Check that either no unit is selected or that the given input does not contain any letters. */
            if (m_startTimeUnit.getSelectedItem() != Unit.NO_UNIT && !m_stepSizeTime.getText().matches("^[0-9]+$")) {
                throw new InvalidSettingsException("Step size: input '"+m_stepSizeTime.getText()+"' invalid. Only integers are allowed when unit '"+m_startTimeUnit.getSelectedItem()+"' is chosen");

            }
            if(m_timeWindowUnit.getSelectedItem() != Unit.NO_UNIT && !m_windowSizeTime.getText().matches("^[0-9]+$")) {
                throw new InvalidSettingsException("Window size: input '"+m_windowSizeTime.getText()+"' invalid. Only integers are allowed when unit '"+m_timeWindowUnit.getSelectedItem()+"' is chosen");
            }

            try {
                Duration startDur = null;

                /* If unit is milliseconds we have it to change to seconds for parsing. */
                if (((Unit)m_startTimeUnit.getSelectedItem()) == Unit.MILLISECONDS) {
                    double tempStart = Double.parseDouble(m_stepSizeTime.getText());
                    tempStart /= 1000;

                    startDur = DurationPeriodFormatUtils.parseDuration(tempStart + Unit.SECONDS.getUnitLetter());
                } else {
                    startDur = DurationPeriodFormatUtils.parseDuration(
                        m_stepSizeTime.getText() + ((Unit)m_startTimeUnit.getSelectedItem()).getUnitLetter());
                }

                /* Limit step size to 24h */
                if (m_columnSelector.getSelectedAsSpec().getType().equals(DataType.getType(LocalTimeCell.class))) {
                    Duration temp = Duration.ofHours(24);

                    if (startDur.compareTo(temp) > 0) {
                        throw new InvalidSettingsException(
                            "Step size must not be greater than 24h when LocalTime is selected");
                    } else if (startDur.compareTo(Duration.ZERO) == 0 || startDur.isNegative()) {
                        throw new InvalidSettingsException("Step size '"+m_stepSizeTime.getText() + ((Unit)m_startTimeUnit.getSelectedItem()).getUnitLetter()+"' invalid. Step size must be greater than 0.");
                    }
                }

                if (m_columnSelector.getSelectedAsSpec().getType().equals(DataType.getType(LocalDateCell.class))) {
                    throw new InvalidSettingsException(
                        "Step size: Duration based step size '"+m_stepSizeTime.getText()+"' is not allowed for type LocalDate. Note that 'm' is reserved for minutes, use 'M' for months.");
                }

                config.setTimeStepSize(m_stepSizeTime.getText());
            } catch (DateTimeParseException e) {
                try {
                    Period startPer = DurationPeriodFormatUtils
                        .parsePeriod(m_stepSizeTime.getText() + ((Unit)m_startTimeUnit.getSelectedItem()).getUnitLetter());

                    /* Period is not allowed. */
                    if (m_columnSelector.getSelectedAsSpec().getType().equals(DataType.getType(LocalTimeCell.class))) {
                        throw new InvalidSettingsException(
                            "Step size: Date based step size '"+m_stepSizeTime.getText()+"' is not allowed for type LocalTime. Note that 'M' is reserved for months, use 'm' for minutes.");
                    } else if (m_centralRButton.isSelected()) {
                        throw new InvalidSettingsException(
                            "Step size: Date based step size '"+m_stepSizeTime.getText()+"' is not allowed for central windowing. Note that 'M' is reserved for months, use 'm' for minutes.");
                    }

                    if(startPer.isZero() || startPer.isNegative()) {
                        throw new InvalidSettingsException("Step size '"+m_stepSizeTime.getText() + ((Unit)m_startTimeUnit.getSelectedItem()).getUnitLetter()+"' invalid. Step Size must be greater than 0");
                    }

                    config.setTimeStepSize(m_stepSizeTime.getText());
                } catch (DateTimeParseException e2) {
                    throw new InvalidSettingsException("Step size: '" + m_stepSizeTime.getText()
                        + "' is not a valid duration. Note that 'M' is reserved for months, use 'm' for minutes.");
                }
            }

            try {
                Duration windowDur = null;

                /* If unit is milliseconds we have it to change to seconds for parsing. */
                if (((Unit)m_timeWindowUnit.getSelectedItem()) == Unit.MILLISECONDS) {
                    double tempWindow = Double.parseDouble(m_windowSizeTime.getText());
                    tempWindow /= 1000;

                    windowDur = DurationPeriodFormatUtils.parseDuration(tempWindow + Unit.SECONDS.getUnitLetter());
                } else {
                    windowDur = DurationPeriodFormatUtils.parseDuration(
                        m_windowSizeTime.getText() + ((Unit)m_timeWindowUnit.getSelectedItem()).getUnitLetter());
                }

                /* Limit window to 24h */
                if (m_columnSelector.getSelectedAsSpec().getType().equals(DataType.getType(LocalTimeCell.class))) {
                    Duration temp = Duration.ofHours(24);

                    if (windowDur.compareTo(temp) > 0) {
                        throw new InvalidSettingsException(
                            "Window size must not be greater than 24h when LocalTime is selected");
                    } else if (windowDur.isZero() || windowDur.isNegative()) {
                        throw new InvalidSettingsException("Window size '"+m_windowSizeTime.getText() + ((Unit)m_timeWindowUnit.getSelectedItem()).getUnitLetter()+"' invalid. Window size must be greater than 0");
                    }
                }

                if (m_columnSelector.getSelectedAsSpec().getType().equals(DataType.getType(LocalDateCell.class))) {
                    throw new InvalidSettingsException(
                        "Window size: Time based window size '"+m_windowSizeTime.getText()+"' is not allowed for type LocalDate. Note that 'm' is reserved for minutes, use 'M' for months.");
                }

                config.setTimeWindowSize(m_windowSizeTime.getText());
            } catch (DateTimeParseException e) {
                try {
                    Period windowPer = DurationPeriodFormatUtils.parsePeriod(
                        m_windowSizeTime.getText() + ((Unit)m_timeWindowUnit.getSelectedItem()).getUnitLetter());

                    /* Period is not allowed. */
                    if (m_columnSelector.getSelectedAsSpec().getType().equals(DataType.getType(LocalTimeCell.class))) {
                        throw new InvalidSettingsException(
                            "Window size: Date based window size '"+m_windowSizeTime.getText()+"' is not allowed for type LocalTime. Note that 'M' is reserved for months, use 'm' for minutes.");
                    } else if (m_centralRButton.isSelected()) {
                        throw new InvalidSettingsException(
                            "Window size: Date based window size '"+m_windowSizeTime.getText()+"' is not allowed for central windowing. Note that 'M' is reserved for months, use 'm' for minutes.");
                    }

                    if(windowPer.isZero() || windowPer.isNegative()) {
                        throw new InvalidSettingsException("Window size '"+m_windowSizeTime.getText() + ((Unit)m_timeWindowUnit.getSelectedItem()).getUnitLetter()+"' invalid. Window size must be greater than 0");
                    }

                    config.setTimeWindowSize(m_windowSizeTime.getText());
                } catch (DateTimeParseException e2) {
                    throw new InvalidSettingsException("Window size: '" + m_windowSizeTime.getText()
                        + "' is not a valid duration. Note that 'M' is reserved for months, use 'm' for minutes.");
                }
            }

            config.setTimeWindowUnit((Unit)m_timeWindowUnit.getSelectedItem());
            config.setTimeStepUnit((Unit)m_startTimeUnit.getSelectedItem());
        }

        config.setUseSpecifiedStartTime(m_useSpecifiedStartTimeCheckBox.isSelected());

        config.saveSettingsTo(settings);
        m_specifiedStartTime.saveSettingsTo(settings);
        m_columnSelector.saveSettingsTo(settings);
    }

}
