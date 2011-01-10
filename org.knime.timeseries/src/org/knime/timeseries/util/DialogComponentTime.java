/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 * 
 * History
 *   15.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.util;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
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
 * A dialog component to enter a time with text fields for hour, minute, seconds
 * and milliseconds. The milliseconds are optional and displayed with a checkbox
 * to activate them. 
 * @see DialogComponentCalendar
 * @see SettingsModelCalendar
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DialogComponentTime extends DialogComponent {

    private JTextField m_hourUI;

    private JTextField m_minuteUI;

    private JTextField m_secondUI;

    private JTextField m_milliUI;
    
    private JCheckBox m_useMillis; 

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
        
        getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
    }

    private JPanel createTimePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.X_AXIS));
        m_hourUI = new JTextField(2);
        m_hourUI.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                m_hourUI.selectAll();
            }
        });
        m_hourUI.getDocument().addDocumentListener(
                new AbstractValidateDocumentListener() {
            @Override
            protected void validate() {
                try {
                    updateHour();
                } catch (InvalidSettingsException e) {
                    showError(m_hourUI);
                }
            }
        });
        timePanel.add(new JLabel("Hour:"));
        timePanel.add(m_hourUI);
        timePanel.add(Box.createHorizontalStrut(5));
        m_minuteUI = new JTextField(2);
        m_minuteUI.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                m_minuteUI.selectAll();
            }
        });
        m_minuteUI.getDocument().addDocumentListener(
                new AbstractValidateDocumentListener() {
            @Override
            protected void validate() {
                try {
                    updateMinute();
                } catch (InvalidSettingsException e) {
                    showError(m_minuteUI);
                }
            }
        });
        timePanel.add(new JLabel("Minute:"));
        timePanel.add(m_minuteUI);
        timePanel.add(Box.createHorizontalStrut(5));
        m_secondUI = new JTextField(2);
        m_secondUI.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                m_secondUI.selectAll();
            }
        });
        m_secondUI.getDocument().addDocumentListener(
                new AbstractValidateDocumentListener() {
            @Override
            protected void validate() {
                try {
                    updateSecond();
                } catch (InvalidSettingsException e) {
                    showError(m_secondUI);
                }
            }
        });
        timePanel.add(new JLabel("Second:"));
        timePanel.add(m_secondUI);
        timePanel.add(Box.createHorizontalStrut(5));
        
        m_useMillis = new JCheckBox();
        m_useMillis.setSelected(false);
        m_milliUI = new JTextField(3);
        m_milliUI.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                m_milliUI.selectAll();
            }
        });
        m_milliUI.getDocument().addDocumentListener(
                new AbstractValidateDocumentListener() {
            @Override
            protected void validate() {
                try {
                    updateMillisecond();
                } catch (InvalidSettingsException e) {
                    showError(m_milliUI);
                }
            }
        });
        m_useMillis.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean enable = m_useMillis.isSelected();
                if (!enable) {
                    // if we are disabled -> clear all errors
                    // DialogComponent sets default (active!) colors
                    clearError(m_milliUI);
                }                
                // now enable or disable the text field (in order to get the 
                // inactive default colors
                m_milliUI.setEnabled(enable);
                // immediately update the model
                SettingsModelCalendar model = (SettingsModelCalendar)getModel();
                model.setUseMilliseconds(enable);
                if (enable) {
                    // this has do be called after the value has been written
                    // into the model: if !useMillis it is not validated
                    try {
                        // call update milliseconds in order to set error
                        updateMillisecond();
                    } catch (InvalidSettingsException ise) {
                        showError(m_milliUI);
                    }
                }
            }
            
        });
        timePanel.add(m_useMillis);
        timePanel.add(new JLabel("Milli:"));
        timePanel.add(m_milliUI);
        panel.add(timePanel);
        return panel;
    }
    
    private static boolean isValidHour(final String enteredValue) {
        try {
            int hour = Integer.parseInt(enteredValue);
            if (hour >= 0 &&  hour < 24) {
                return true;
            }
        } catch (Exception e) {
            // obviously not valid
        }
        return false;
    }
    
    private static boolean isValidMinuteOrSecond(final String enteredValue) {
        try {            
            int minute = Integer.parseInt(enteredValue);
            if (minute >= 0 && minute < 60) {
                return true;
            }
        } catch (Exception e) {
            // obviously not valid!
        }
        return false;        
    }
    
    private static boolean isValidMillisecond(final String enteredValue) {
        try {
            int milli = Integer.parseInt(enteredValue);
            if (milli >= 0 && milli < 1000) {
                return true;
            }
        } catch (Exception e) {
            // obviously not valid
        }
        return false;
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
        m_secondUI.setEnabled(enabled);
        m_useMillis.setEnabled(enabled);
        m_useMillis.setSelected(((SettingsModelCalendar)getModel())
                .useMilliseconds());
        m_milliUI.setEnabled(enabled && m_useMillis.isSelected());
        
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
        m_hourUI.setText(Integer.toString(calendar.get(
                Calendar.HOUR_OF_DAY)));
        m_minuteUI.setText(Integer.toString(calendar.get(
                Calendar.MINUTE)));
        m_secondUI.setText(Integer.toString(calendar.get(Calendar.SECOND)));
        m_milliUI.setText(Integer.toString(calendar.get(
                Calendar.MILLISECOND)));
        m_useMillis.setSelected(model.useMilliseconds());
        setEnabledComponents(model.useTime() && model.isEnabled());
    }
    
    /**
     * Writes the hour immediately into the model's calendar or 
     * throws an exception if entered value is not an integer.
     * 
     * @throws InvalidSettingsException if entered value is not an int
     */
    protected void updateHour() throws InvalidSettingsException {
        SettingsModelCalendar model = (SettingsModelCalendar)getModel();
        if (!model.useTime()) {
            return;
        }
        String hourText = m_hourUI.getText();
        if (!isValidHour(hourText)) {
            throw new InvalidSettingsException(
                    "Hour must be between 0 and 23 but is " 
                    + hourText + "!");
        }
        Calendar calendar = model.getCalendar();
        int hour = Integer.parseInt(hourText);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        model.setCalendar(calendar);
    }
    
    /**
     * Writes the entered minute immediately into the model's calendar
     * or throws an exception if the entered value is not valid.
     * 
     * @throws InvalidSettingsException if the entered value is not an int in 
     * the range of 0,59
     */
    protected void updateMinute() throws InvalidSettingsException {
        SettingsModelCalendar model = (SettingsModelCalendar)getModel();
        if (!model.useTime()) {
            // do not validate if time is not used by the model
            return;
        }
        String minuteText = m_minuteUI.getText();
        if (!isValidMinuteOrSecond(minuteText)) {
            throw new InvalidSettingsException(
                    "Minute must be between 0 and 59 but is "
                    + minuteText + "!");
        }
        Calendar calendar = model.getCalendar();
        int minute = Integer.parseInt(minuteText);
        calendar.set(Calendar.MINUTE, minute);
        model.setCalendar(calendar);
    }
    
    /**
     * Writes the second immediately into the model's calendar or throws an 
     * exception if the entered value is not a valid second.
     * 
     * @throws InvalidSettingsException if the entered value is not an int in
     * the range of 0-59 
     */
    protected void updateSecond() throws InvalidSettingsException {
        SettingsModelCalendar model = (SettingsModelCalendar)getModel();
        if (!model.useTime()) {
            // do not validate if time is not used by the model
            return;
        }
        String secondText = m_secondUI.getText();
        if (!isValidMinuteOrSecond(secondText)) {
            throw new InvalidSettingsException(
                    "Second must be between 0 and 59 but is "
                    + secondText + "!");
        }
        Calendar calendar = model.getCalendar();
        int second = Integer.parseInt(secondText);
        calendar.set(Calendar.SECOND, second);
        model.setCalendar(calendar);
    }
    
    /**
     * Writes the milliseconds immediately into the model's calendar or throws 
     * an exception if the entered value is not a valid millisecond.
     *  
     * @throws InvalidSettingsException if the entered value is not an int in 
     * the range of 0-999
     */
    protected void updateMillisecond() throws InvalidSettingsException {
        SettingsModelCalendar model = (SettingsModelCalendar)getModel();
        if (!model.useMilliseconds()) {
            // do not validate if milliseconds are not used by the model
            return;
        }
        String milliText = m_milliUI.getText();
        if (!isValidMillisecond(milliText)) {
            throw new InvalidSettingsException(
                    "Millisecond must be between 0 and 999 but is "
                    + milliText + "!");
        }
        Calendar calendar = model.getCalendar();
        int milli = Integer.parseInt(milliText);
        calendar.set(Calendar.MILLISECOND, milli);
        model.setCalendar(calendar);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() 
        throws InvalidSettingsException {
        updateHour();
        updateMinute();
        updateSecond();
        updateMillisecond();
    }
}
