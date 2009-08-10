/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   05.06.2009 (Fabian Dill): created
 */
package org.knime.timeseries.util;

import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog component to configure a date, a time or both.
 * 
 * @author Fabian Dill, KNIME.com GmbH
 * 
 */
public class DialogComponentCalendar extends DialogComponent {

    private JCheckBox m_useDateUI;

    private JTextField m_yearUI;

    private JComboBox m_monthUI;

    private JComboBox m_dayUI;

    private JCheckBox m_useTimeUI;

    private JTextField m_hourUI;

    private JTextField m_minuteUI;

    private JTextField m_secondUI;

    private JTextField m_milliUI;


    /**
     * 
     * @param model SettingsModel to represent the selected date
     * @param label the label to display
     */
    public DialogComponentCalendar(final SettingsModelCalendar model,
            final String label) {
        super(model);
        // create a panel with a titled border (label)
        JPanel overall = new JPanel();
        overall.setLayout(new BoxLayout(overall, BoxLayout.Y_AXIS));
        overall.setBorder(BorderFactory.createTitledBorder(label));
        overall.add(createDatePanel());
        overall.add(createTimePanel());
        getComponentPanel().add(overall);
    }

    private JPanel createDatePanel() {
        // add the check box on top of the horizontal date panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Box useDateBox = Box.createHorizontalBox();
        m_useDateUI = new JCheckBox("Use date", true);
        m_useDateUI.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent arg0) {
                boolean enable = m_useDateUI.isSelected();
                m_yearUI.setEnabled(enable);
                m_monthUI.setEnabled(enable);
                m_dayUI.setEnabled(enable);
            }
        });
        useDateBox.add(m_useDateUI);
        useDateBox.add(Box.createHorizontalGlue());
        panel.add(useDateBox);

        JPanel datePanel = new JPanel();
        // one text input field (year)
        m_yearUI = new JTextField(4);
        datePanel.add(new JLabel("Year:"));
        datePanel.add(m_yearUI);
        // select boxes month
        m_monthUI = new JComboBox(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12});
        datePanel.add(new JLabel("Month:"));
        datePanel.add(m_monthUI);
        // select box day
        m_dayUI = new JComboBox(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
                27, 28, 29, 30, 31});
        datePanel.add(new JLabel("Day:"));
        datePanel.add(m_dayUI);

        panel.add(datePanel);
        return panel;
    }

    private JPanel createTimePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Box useTimeBox = Box.createHorizontalBox();
        m_useTimeUI = new JCheckBox("Use Time", false);
        m_useTimeUI.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent arg0) {
                boolean enable = m_useTimeUI.isSelected();
                m_hourUI.setEnabled(enable);
                m_minuteUI.setEnabled(enable);
                m_secondUI.setEnabled(enable);
                m_milliUI.setEnabled(enable);
            }
        });
        useTimeBox.add(m_useTimeUI);
        useTimeBox.add(Box.createHorizontalGlue());
        panel.add(useTimeBox);

        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.X_AXIS));

        m_hourUI = new JTextField(2);
        m_hourUI.setEnabled(false);
        timePanel.add(new JLabel("Hour:"));
        timePanel.add(m_hourUI);

        m_minuteUI = new JTextField(2);
        m_minuteUI.setEnabled(false);
        timePanel.add(new JLabel("Minute:"));
        timePanel.add(m_minuteUI);

        m_secondUI = new JTextField(2);
        m_secondUI.setEnabled(false);
        timePanel.add(new JLabel("Second:"));
        timePanel.add(m_secondUI);

        m_milliUI = new JTextField(3);
        m_milliUI.setEnabled(false);
        timePanel.add(new JLabel("Milli:"));
        timePanel.add(m_milliUI);

        panel.add(timePanel);

        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // should be at least one date column?
        // not should check that in configure method
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_yearUI.setEnabled(enabled);
        m_monthUI.setEnabled(enabled);
        m_dayUI.setEnabled(enabled);
        m_hourUI.setEnabled(enabled);
        m_minuteUI.setEnabled(enabled);
        m_secondUI.setEditable(enabled);
        m_milliUI.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        getComponentPanel().setToolTipText(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        SettingsModelCalendar model = (SettingsModelCalendar)getModel();
        Calendar calendar = model.getCalendar();
        m_useDateUI.setSelected(model.useDate());
        m_useTimeUI.setSelected(model.useTime());

        if (model.useDate()) {
            m_yearUI.setText("" + calendar.get(Calendar.YEAR));
            // setting the index is perfectly fine, since Calendar represents
            // months as zero-based indices (but not day of month)
            m_monthUI.setSelectedIndex(calendar.get(Calendar.MONTH));
            m_dayUI.setSelectedIndex(calendar.get(Calendar.DAY_OF_MONTH) - 1);
        }
        if (model.useTime()) {
            m_hourUI.setText(Integer.toString(calendar.get(
                    Calendar.HOUR_OF_DAY)));
            m_minuteUI.setText(Integer.toString(calendar.get(
                    Calendar.MINUTE)));
            m_secondUI.setText(Integer.toString(calendar.get(Calendar.SECOND)));
            m_milliUI.setText(Integer.toString(calendar.get(
                    Calendar.MILLISECOND)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() 
        throws InvalidSettingsException {
            SettingsModelCalendar model = (SettingsModelCalendar)getModel();
            Calendar calendar = model.getCalendar();
            model.setUseDate(m_useDateUI.isSelected());
            model.setUseTime(m_useTimeUI.isSelected());
            if (m_useDateUI.isSelected()) {
                try {
                    // check whether the year contains only numbers
                    int year = Integer.parseInt(m_yearUI.getText());
                    // taking the index is perfectly fine, since Calendar 
                    // represents months as zero-based indices 
                    // (but not day of month)
                    int month = m_monthUI.getSelectedIndex();
                    int day = m_dayUI.getSelectedIndex() + 1;
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                } catch (NumberFormatException nfe) {
                    throw new InvalidSettingsException("Not a valid year: "
                            + m_yearUI.getText() + "! "
                            + "Please use only integer numbers");
                }
            }
            if (m_useTimeUI.isSelected()) {
                try {
                // hour
                    int hour = Integer.parseInt(m_hourUI.getText());
                    if (hour < 0 || hour > 24) {
                        throw new InvalidSettingsException(
                                "Hour must be between 0 and 24 but is " 
                                + hour + "!");
                    }
                    int minute = Integer.parseInt(m_minuteUI.getText());
                    if (minute < 0 || minute > 60) {
                        throw new InvalidSettingsException(
                                "Minute must be between 0 and 60 but is "
                                + minute + "!");
                    }
                    int second = Integer.parseInt(m_secondUI.getText());
                    if (second < 0 || second > 60) {
                        throw new InvalidSettingsException(
                                "Second must be between 0 and 60 but is "
                                + second + "!");
                    }
                    int milli = Integer.parseInt(m_milliUI.getText());
                    if (milli < 0 || milli > 999) {
                        throw new InvalidSettingsException(
                                "Millisecond must be between 0 and 9 but is "
                                + milli + "!");
                    }
                    // TODO: this has to be documented somewhere: that hour of 
                    // day is used (0-24 and not am/pm)
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, second);
                    calendar.set(Calendar.MILLISECOND, milli);
                } catch (NumberFormatException nfe) {
                    throw new InvalidSettingsException(
                            "Not a valid time! "
                            + m_hourUI.getText() + ":" + m_minuteUI.getText() 
                            + ":" + m_secondUI.getText() + ":" 
                            + m_milliUI.getText());
                }
            }
            model.setCalendar(calendar);
    }

}
