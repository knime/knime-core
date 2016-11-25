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
 *   Aug 23, 2016 (oole): created
 */
package org.knime.core.node.defaultnodesettings;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * StandardDialogComponent allowing the input of a user specified time values for days, hours, minutes, seconds.
 *
 * @author Ole Ostergaard, KNIME.com GmbH
 * @since 3.3
 */
public class DialogComponentDuration extends DialogComponent {

    private static final int SPINNER_WIDTH = 10;

    private final JSpinner m_days;
    private final JSpinner m_hours;
    private final JSpinner m_minutes;
    private final JSpinner m_seconds;

    /**
     * @param model model to store the input duration
     * @param label to place on the dialog or <code>null</code> if no border and label is wanted
     * @param displaySeconds whether or not the seconds spinner should be displayed. <code>true</code> to display,
     *            <code>false</code> to hide
     */
    public DialogComponentDuration(final SettingsModelDuration model, final String label,
        final boolean displaySeconds) {
        this(model, label, displaySeconds, 364);
    }

    /**
     * @param model model to store the input time values
     * @param label to place on the dialog or <code>null</code> if no border and label is wanted
     * @param displaySeconds <code>true</code> seconds are displayed, <code>false</code> seconds can only be set via
     *            flow variables
     * @param maxDays the maximum amount of days that the spinner should allow
     */
    public DialogComponentDuration(final SettingsModelDuration model, final String label, final boolean displaySeconds,
        final int maxDays) {
        super(model);

        // set spinners
        SpinnerModel spinnerModelDays = new SpinnerNumberModel(0,0,maxDays, 1);
        SpinnerModel spinnerModelHours = new SpinnerNumberModel(0, 0, 23, 1);
        SpinnerModel spinnerModelMinutes = new SpinnerNumberModel(0, 0, 59, 1);
        SpinnerModel spinnerModelSeconds = new SpinnerNumberModel(0, 0, 59, 1);

        Dimension spinnerSize = new Dimension(SPINNER_WIDTH,25);
        m_days = new JSpinner(spinnerModelDays);
        m_days.setMaximumSize(spinnerSize);
        m_hours = new JSpinner(spinnerModelHours);
        m_hours.setMaximumSize(spinnerSize);
        m_minutes = new JSpinner(spinnerModelMinutes);
        m_minutes.setMaximumSize(spinnerSize);

        m_seconds = new JSpinner(spinnerModelSeconds);
        m_seconds.setMaximumSize(spinnerSize);

        // set panels
        JPanel panel = new JPanel(new GridBagLayout());
        if (label != null) {
            panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), label));
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0;
        gbc.gridy = 0;

        panel.add(new JLabel("Days: "), gbc);
        gbc.gridx++;
        panel.add(m_days,gbc);
        gbc.gridx++;

        panel.add(new JLabel("Hours: "), gbc);
        gbc.gridx++;
        panel.add(m_hours, gbc);
        gbc.gridx++;

        panel.add(new JLabel("Minutes: "), gbc);
        gbc.gridx++;
        panel.add(m_minutes, gbc);
        if (displaySeconds) {
            gbc.gridx++;

            panel.add(new JLabel("Seconds: "), gbc);
            gbc.gridx++;
            panel.add(m_seconds,gbc);
        }
        getComponentPanel().add(panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final SettingsModelDuration model = (SettingsModelDuration)getModel();
        setEnabledComponents(model.isEnabled());
        loadUnits(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
            ((SettingsModelDuration)getModel())
                .setDuration(Duration.ZERO.plusDays(Long.parseLong(m_days.getValue().toString()))
                    .plusHours(Long.parseLong(m_hours.getValue().toString()))
                    .plusMinutes(Long.parseLong(m_minutes.getValue().toString()))
                    .plusSeconds(Long.parseLong(m_seconds.getValue().toString())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_days.setEnabled(enabled);
        m_hours.setEnabled(enabled);
        m_minutes.setEnabled(enabled);
        m_seconds.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_days.setToolTipText(text);
        m_hours.setToolTipText(text);
        m_minutes.setToolTipText(text);
        m_seconds.setToolTipText(text);
    }

    /**
     * Update the duration according to the units stored in the model
     */
    private void loadUnits(final SettingsModelDuration model) {
            Duration duration =  ((SettingsModelDuration)getModel()).getDuration();
            m_days.setValue(duration.toDays());
            Duration durationMinusDays = duration.minusDays(duration.toDays());
            m_hours.setValue(durationMinusDays.toHours());
            Duration durationMinusHours = durationMinusDays.minusHours(durationMinusDays.toHours());
            m_minutes.setValue(durationMinusHours.toMinutes());
            Duration durationMinusSeconds = durationMinusHours.minusMinutes(durationMinusHours.toMinutes());
            m_seconds.setValue(durationMinusSeconds.getSeconds());
    }
}
