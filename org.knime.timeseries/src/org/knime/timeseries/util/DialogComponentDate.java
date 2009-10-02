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

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog component to enter a date with year, month and day. 
 * @see DialogComponentCalendar
 * @see SettingsModelCalendar
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DialogComponentDate extends DialogComponent {
    
    private JTextField m_yearUI;

    private JComboBox m_monthUI;

    private JComboBox m_dayUI;


    /**
     * 
     * @param model SettingsModel to represent the selected date
     * @param label the label to display
     */
    public DialogComponentDate(final SettingsModelCalendar model,
            final String label) {
        super(model);
        // create a panel with a titled border (label)
        JPanel overall = new JPanel();
        overall.setLayout(new BoxLayout(overall, BoxLayout.Y_AXIS));
        overall.setBorder(BorderFactory.createTitledBorder(label));
        overall.add(createDatePanel());
        getComponentPanel().add(overall);
    }

    private JPanel createDatePanel() {
        // add the check box on top of the horizontal date panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel datePanel = new JPanel();
        // one text input field (year)
        m_yearUI = new JTextField(4);
        m_yearUI.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                m_yearUI.selectAll();
            }
        });
        m_yearUI.getDocument().addDocumentListener(
                new AbstractValidateDocumentListener() {
            @Override
            protected void validate() {
                try {
                    // if it is an integer it is a valid year
                    updateModel();
                } catch (Exception e) {
                    // else show error
                    showError(m_yearUI);
                }
            }
        });
        datePanel.add(new JLabel("Year:"));
        datePanel.add(m_yearUI);
        // select boxes month
        m_monthUI = new JComboBox(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12});
        m_monthUI.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                try {
                    updateModel();
                } catch (Exception ex) {
                    // year may be invalid -> month is anyway a select box
                }
            }
            
        });
        datePanel.add(new JLabel("Month:"));
        datePanel.add(m_monthUI);
        // select box day
        m_dayUI = new JComboBox(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
                27, 28, 29, 30, 31});
        m_dayUI.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                try {
                    updateModel();
                } catch (Exception ex) {
                    // year may be invalid -> day is anyway a select box
                }
            }
        });
        datePanel.add(new JLabel("Day:"));
        datePanel.add(m_dayUI);

        panel.add(datePanel);
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
        Calendar calendar = null;
        if (model.useDate()) {
            // set the fields from the model's calendar
            calendar = model.getCalendar();
        } else {
            // do not set the fields of the model's calendar: they are not set, 
            // i.e. 1.1.1970.
            // for user friendliness use current date
            calendar = Calendar.getInstance(DateAndTimeCell.UTC_TIMEZONE);
        }
        m_yearUI.setText("" + calendar.get(Calendar.YEAR));
        // setting the index is perfectly fine, since Calendar represents
        // months as zero-based indices (but not day of month)
        m_monthUI.setSelectedIndex(calendar.get(Calendar.MONTH));
        m_dayUI.setSelectedIndex(calendar.get(Calendar.DAY_OF_MONTH) - 1);
        setEnabledComponents(model.useDate());
    }
    
    /**
     * Writes the values immediately into the model. 
     *  
     * @throws InvalidSettingsException if the year is not an integer
     */
    protected void updateModel() throws InvalidSettingsException {
        SettingsModelCalendar model = (SettingsModelCalendar)getModel();
        if (!model.useDate()) {
            // do not update/validate if date is not used by the model
            return;
        }
        Calendar calendar = model.getCalendar();
        // taking the index is perfectly fine, since Calendar 
        // represents months as zero-based indices 
        // (but not day of month)
        int month = m_monthUI.getSelectedIndex();
        int day = m_dayUI.getSelectedIndex() + 1;
        // First set month and day -> unlikely that an error occurs
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        model.setCalendar(calendar);
        try {
            // check whether the year contains only numbers
            int year = Integer.parseInt(m_yearUI.getText());
            calendar.set(Calendar.YEAR, year);
            model.setCalendar(calendar);
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException("Not a valid year: "
                    + m_yearUI.getText() + "! "
                    + "Please use only integer numbers");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() 
        throws InvalidSettingsException {
        updateModel();
    }

}
