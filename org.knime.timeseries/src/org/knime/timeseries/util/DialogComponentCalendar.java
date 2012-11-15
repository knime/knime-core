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
 *   05.06.2009 (Fabian Dill): created
 */
package org.knime.timeseries.util;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog component to configure a date, a time or both. This is a convenience
 * class consisting of  {@link DialogComponentDate} and
 * {@link DialogComponentTime} handling the loading and saving of the
 * settings for both of them with one single model.
 *
 * @see DialogComponentDate
 * @see DialogComponentTime
 * @see SettingsModelCalendar
 *
 * @author Fabian Dill, KNIME.com AG
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

        getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
    }

    private JPanel createDatePanel() {
        m_date = new DialogComponentDate((SettingsModelCalendar)getModel(),
                "Date:");
        // add the check box on top of the horizontal date panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Box useDateBox = Box.createHorizontalBox();
        m_useDateUI = new JCheckBox("Use date", false);
        m_useDateUI.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean enable = m_useDateUI.isSelected();
                // only dis-/enable UI since they all share one model
                m_date.setEnabledComponents(enable);
                ((SettingsModelCalendar)getModel()).setUseDate(enable);
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
            @Override
            public void itemStateChanged(final ItemEvent e) {
                boolean enable = m_useTimeUI.isSelected();
                // only dis-/enable UI since they all share one model
                m_time.setEnabledComponents(enable);
                ((SettingsModelCalendar)getModel()).setUseTime(enable);
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
     * @param disabled If true, the checkbox for using the date is disabled.
     */
    public void setDateDisabled(final boolean disabled) {
        m_useDateUI.setEnabled(!disabled);
    }

    /**
     * @param disabled If true, the checkbox for using the time is disabled.
     */
    public void setTimeDisabled(final boolean disabled) {
        m_useTimeUI.setEnabled(!disabled);
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
        m_useDateUI.setEnabled(model.isEnabled());

        m_useTimeUI.setSelected(model.useTime());
        m_useTimeUI.setEnabled(model.isEnabled());

        m_date.updateComponent();
        m_time.updateComponent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
        throws InvalidSettingsException {
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
    }

}
