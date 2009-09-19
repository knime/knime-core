/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   15.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.util;

import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DialogComponentTime extends DialogComponent {
    
    private JTextField m_hourUI;

    private JTextField m_minuteUI;

    private JTextField m_secondUI;

    private JTextField m_milliUI;

    /**
     * 
     * @param model SettingsModel to represent the selected date
     * @param label the label to display
     */
    public DialogComponentTime(final SettingsModelCalendar model,
            final String label) {
        super(model);
        // create a panel with a titled border (label)
        JPanel overall = new JPanel();
        overall.setLayout(new BoxLayout(overall, BoxLayout.Y_AXIS));
        overall.setBorder(BorderFactory.createTitledBorder(label));
        overall.add(createTimePanel());
        getComponentPanel().add(overall);
    }

    private JPanel createTimePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.X_AXIS));

        m_hourUI = new JTextField(2);
        timePanel.add(new JLabel("Hour:"));
        timePanel.add(m_hourUI);

        m_minuteUI = new JTextField(2);
        timePanel.add(new JLabel("Minute:"));
        timePanel.add(m_minuteUI);

        m_secondUI = new JTextField(2);
        timePanel.add(new JLabel("Second:"));
        timePanel.add(m_secondUI);

        m_milliUI = new JTextField(3);
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
            model.setUseTime(true);
            try {
            // hour
                int hour = Integer.parseInt(m_hourUI.getText());
                if (hour < 0 || hour > 24) {
                    throw new InvalidSettingsException(
                            "Hour must be between 0 and 24 but is " 
                            + hour + "!");
                }
                int minute = Integer.parseInt(m_minuteUI.getText());
                if (minute < 0 || minute >= 60) {
                    throw new InvalidSettingsException(
                            "Minute must be between 0 and 59 but is "
                            + minute + "!");
                }
                int second = Integer.parseInt(m_secondUI.getText());
                if (second < 0 || second >= 60) {
                    throw new InvalidSettingsException(
                            "Second must be between 0 and 59 but is "
                            + second + "!");
                }
                int milli = Integer.parseInt(m_milliUI.getText());
                if (milli < 0 || milli > 999) {
                    throw new InvalidSettingsException(
                            "Millisecond must be between 0 and 999 but is "
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
            model.setCalendar(calendar);
    }
}
