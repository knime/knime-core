/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2009
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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

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

    private DialogComponentDate m_date;
    
    private DialogComponentTime m_time; 

    private JCheckBox m_useDateUI;

    private JCheckBox m_useTimeUI;



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
        m_date = new DialogComponentDate((SettingsModelCalendar)getModel(), 
                "Date:");
        // add the check box on top of the horizontal date panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Box useDateBox = Box.createHorizontalBox();
        m_useDateUI = new JCheckBox("Use date", true);
        m_useDateUI.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean enable = m_useDateUI.isSelected();
                // only dis-/enable UI since they all share one model 
                m_date.setEnabledComponents(enable);
                ((SettingsModelCalendar)getModel()).setUseDate(enable);
                m_date.getModel().setEnabled(enable);
            }
        });
        useDateBox.add(m_useDateUI);
        useDateBox.add(Box.createHorizontalGlue());
        panel.add(useDateBox);
        panel.add(m_date.getComponentPanel());
        return panel;
    }

    private JPanel createTimePanel() {
        m_time = new DialogComponentTime((SettingsModelCalendar)getModel(), 
                "Time:");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Box useTimeBox = Box.createHorizontalBox();
        m_useTimeUI = new JCheckBox("Use Time", true);
        m_useTimeUI.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                boolean enable = m_useTimeUI.isSelected();
                // only dis-/enable UI since they all share one model
                m_time.setEnabledComponents(enable);
                ((SettingsModelCalendar)getModel()).setUseTime(enable);
                m_time.getModel().setEnabled(enable);
            }
        });
        useTimeBox.add(m_useTimeUI);
        useTimeBox.add(Box.createHorizontalGlue());
        panel.add(useTimeBox);
        panel.add(m_time.getComponentPanel());
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
        m_date.setEnabledComponents(enabled);
        m_time.setEnabledComponents(enabled);
        m_useDateUI.setEnabled(enabled);
        m_useTimeUI.setEnabled(enabled);
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
        m_useDateUI.setSelected(model.useDate());
        m_useTimeUI.setSelected(model.useTime());
        m_date.updateComponent();
        m_time.updateComponent();
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
            if (!m_useDateUI.isSelected() && !m_useTimeUI.isSelected()) {
                throw new InvalidSettingsException(
                        "Either date or time or both must be selected!");
            }
            if (m_useDateUI.isSelected()) {                
                m_date.validateSettingsBeforeSave();
            }
            if (m_useTimeUI.isSelected()) {
                m_time.validateSettingsBeforeSave();
            }
            model.setCalendar(calendar);
    }

}
