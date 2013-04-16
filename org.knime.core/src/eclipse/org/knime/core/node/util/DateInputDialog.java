/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * Created on 20.03.2013 by peter
 */
package org.knime.core.node.util;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.toedter.calendar.JDateChooser;

/**
 * This class supplies a GUI-Component (JPanel) which allows the input of an Date. The dialog itself is quite flexible
 * as it allows the date to be optional as well as the time-fields hour, minute and second. To enable easy saving and
 * loading of the options, the selected date as well as the current status ( status code (getIntForStatus) has to be
 * saved only. The date will be returned in UTC-Time.
 **
 *
 * @author Sebastian Peter, University of Konstanz
 * @since 2.8
 */
public class DateInputDialog extends JPanel {
    public static enum Mode {
        NODATE, NOTIME, HOURS, MINUTES, SECONDS
    };

    /**
     *
     */
    private static final long serialVersionUID = 1064690706141780973L;

    /**
     * Constant used for Horizontal gaps.
     */
    private static final int HORIZ_SPACE = 10;

    /**
     * Constant used for vertical gaps.
     */
    private static final int VERT_SPACE = 10;

    /**
     * Constant used for the dialog width.
     */
    private static final int PANEL_WIDTH = 400;

    /**
     * Constant used for the spinner fields width.
     */
    private static final int SPINNER_WIDTH = 10;

    private JDateChooser m_startDate;

    private JCheckBox m_useHours;

    private JCheckBox m_useMinutes;

    private JCheckBox m_useSeconds;

    private JSpinner m_hours;

    private JSpinner m_minutes;

    private JSpinner m_seconds;

    private boolean m_displayHours = false;

    private boolean m_displayMinutes = false;

    private boolean m_displaySeconds = false;

    private boolean m_isOptional = true;

    private Mode m_usedMode;

    private JCheckBox m_useDate;

    /**
     *
     * Returns how the dialog is configured, meaning which fields are displayed in it.
     *
     *
     * @return Mode Enum, describing the visible fields of the dialog.
     */
    public Mode getIntitalMode() {
        return m_usedMode;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        if (enabled) {
            m_startDate.setEnabled(true);
            m_useMinutes.setEnabled(true);
        } else {
            m_startDate.setEnabled(false);
            m_useHours.setEnabled(false);
            m_useMinutes.setEnabled(false);
            m_useSeconds.setEnabled(false);
            m_hours.setEnabled(false);
            m_minutes.setEnabled(false);
            m_seconds.setEnabled(false);
        }
    }

    /**
     * This method should be called during the loading of the Dialog, if the last used date should be displayed again.
     * Therefore the date is needed in the timesInMillies format additionally the Mode (the status of the selected time
     * fields is needed).
     *
     * @param date Time in milliseconds
     * @param mode Enum to specify which fields of the dialog are enabled and selected
     */
    public void setDateAndMode(final long date, final Mode mode) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(date);
        setDateAndMode(cal.getTime(), mode);
    }

    /**
     *
     * @param date Date to display in the Selection
     * @param mode Depending on the Mode during instantiation, sets the possible fields
     */
    public void setDateAndMode(final Date date, final Mode mode) {

        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);

        int hours = cal.get(Calendar.HOUR_OF_DAY);
        int minutes = cal.get(Calendar.MINUTE);
        int seconds = cal.get(Calendar.SECOND);

        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        m_startDate.setDate(cal.getTime());

        switch (mode) {
            case NODATE:
                m_useDate.setSelected(false);
                m_useDate.setEnabled(true);
                if (m_isOptional) {
                    m_startDate.setEnabled(false);
                    m_useHours.setEnabled(false);
                } else {
                    m_startDate.setEnabled(true);
                    m_useHours.setEnabled(true);
                }

                m_useMinutes.setEnabled(false);
                m_useSeconds.setEnabled(false);
                m_hours.setEnabled(false);
                m_hours.setValue(0);
                m_minutes.setEnabled(false);
                m_minutes.setValue(0);
                m_seconds.setEnabled(false);
                m_seconds.setValue(0);
                break;

            case HOURS:
                m_useDate.setSelected(true);
                m_useDate.setEnabled(true);
                m_startDate.setEnabled(true);
                m_displayHours = true;
                m_hours.setValue(hours);
                m_seconds.setEnabled(false);
                m_useSeconds.setEnabled(false);
                m_minutes.setEnabled(false);
                m_useMinutes.setEnabled(true);
                m_hours.setEnabled(true);
                m_useHours.setEnabled(true);
                m_useHours.setSelected(true);
                break;

            case MINUTES:
                m_useDate.setSelected(true);
                m_useDate.setEnabled(true);
                m_startDate.setEnabled(true);
                m_hours.setValue(hours);
                m_minutes.setValue(minutes);
                m_seconds.setEnabled(false);
                m_useSeconds.setEnabled(true);
                m_minutes.setEnabled(true);
                m_useMinutes.setEnabled(true);
                m_useMinutes.setSelected(true);
                m_hours.setEnabled(true);
                m_useHours.setEnabled(true);
                m_useHours.setSelected(true);
                break;
            case SECONDS:
                m_useDate.setSelected(true);
                m_useDate.setEnabled(true);
                m_startDate.setEnabled(true);
                m_hours.setValue(hours);
                m_minutes.setValue(minutes);
                m_seconds.setValue(seconds);
                m_seconds.setEnabled(true);
                m_useSeconds.setEnabled(true);
                m_useSeconds.setSelected(true);
                m_minutes.setEnabled(true);
                m_useMinutes.setEnabled(true);
                m_useMinutes.setSelected(true);
                m_hours.setEnabled(true);
                m_useHours.setEnabled(true);
                m_useHours.setSelected(true);
                break;
            case NOTIME:
            default:
                m_startDate.setEnabled(true);
                m_useDate.setEnabled(true);
                m_useDate.setSelected(true);
                m_seconds.setEnabled(false);
                m_useSeconds.setEnabled(false);
                m_minutes.setEnabled(false);
                m_useMinutes.setEnabled(false);
                m_hours.setEnabled(false);
                m_useHours.setEnabled(true);
                m_useDate.setEnabled(true);
                m_useDate.setSelected(true);
                break;
        }
    }

    /**
     * Constructs a new optional DateInputDialog, displaying the fields according to the mode.
     *
     * @param mode Specifys the visible time fields in the dialog
     */
    public DateInputDialog(final Mode mode) {
        this(mode, true);
    }

    /**
     * Constructs a new DateInputDialog, displaying the fields according to the mode. Furthermore the date can be
     * mandatory (optional = false) or or optional (optional = true) in this case the user can skip specify a Date.
     *
     * @param mode Enum specifying the visible time fields in the dialog
     * @param optional true, if no date has to be specified by the user.
     */
    public DateInputDialog(final Mode mode, final boolean optional) {
        switch (mode) {
            case HOURS:
                m_displayHours = true;
                break;
            case MINUTES:
                m_displayHours = true;
                m_displayMinutes = true;
                break;
            case SECONDS:
                m_displayHours = true;
                m_displayMinutes = true;
                m_displaySeconds = true;
                break;
            default:
                break;
        }
        m_usedMode = mode;
        m_isOptional = optional;
        initialize();

    }

    /**
     * Creates a new DateInpitDialog, displaying all Fields (date, hour, minute, second) and the date is optional.
     *
     */
    public DateInputDialog() {
        m_usedMode = Mode.SECONDS;
        this.m_displayHours = true;
        this.m_displayMinutes = true;
        this.m_displaySeconds = true;

        initialize();
    }

    /**
     * This Method returns false if no date is specified, in case the component is optional.
     *
     *
     * @return true if an date is selected, else false
     */
    public boolean isUsed() {
        if (m_isOptional) {
            return m_useDate.isSelected();
        }
        return true;
    }

    /**
     * This method returns the selected Date. Not active Fields (hour, minute, seconds) are ignored. Therefore the
     * fields in the Date object are set to .
     *
     *
     * @return the specified date, or null iff optional & no date selected.
     */
    public Date getSelectedDate() {
        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        if (m_useHours.isSelected()) {
            hours = (Integer)m_hours.getValue();
        }
        if (m_useMinutes.isSelected()) {
            minutes = (Integer)m_minutes.getValue();
        }
        if (m_useSeconds.isSelected()) {
            seconds = (Integer)m_seconds.getValue();
        }
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

        cal.set(m_startDate.getCalendar().get(Calendar.YEAR), m_startDate.getCalendar().get(Calendar.MONTH),
                m_startDate.getCalendar().get(Calendar.DAY_OF_MONTH), hours, minutes, seconds);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    /**
     *
     * This method converts the status code to the according Enum representation.
     *
     * @param status - Status to convert
     * @return Enum representation for the given status code.
     */
    public static Mode getModeForStatus(final int status) {
        if (status == 0) {
            return Mode.NODATE;
        }
        if (status == 1) {
            return Mode.NOTIME;
        }
        if (status == 2) {
            return Mode.HOURS;
        }
        if (status == 3) {
            return Mode.MINUTES;
        }
        return Mode.SECONDS;

    }

    /**
     * This method return an integer Status code, which reflects the currently active fields in the dialog.
     *
     * 0: no Date selected 1: Date but not hours 2: Date and Hours, no Minutes... 3: Date, hours and Minutes 4:
     * Everything is selected
     *
     * The status code can then be converted in the Mode Enum via getModeForStatus-Method.
     *
     * @return status code for the currently selected fields
     */
    public int getIntForStatus() {
        if (!m_useDate.isSelected()) {
            return 0;
        }
        if (!m_useHours.isSelected()) {
            return 1;
        }
        if (!m_useMinutes.isSelected()) {
            return 2;
        }
        if (!m_useSeconds.isSelected()) {
            return 3;
        }

        return 4;
    }

    /**
     * This method initializes the dialog Component, with standard values. Which are later on overwritten by the load
     * method. All possible JComponets are created and initialized nevertheless they might not be used due to different
     * configurations.
     */
    private void initialize() {
        m_useSeconds = new JCheckBox();
        m_useMinutes = new JCheckBox();
        m_useHours = new JCheckBox();
        m_useDate = new JCheckBox();
        m_hours = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        m_hours.setMaximumSize(new Dimension(SPINNER_WIDTH, 25));
        m_hours.setMinimumSize(new Dimension(SPINNER_WIDTH, 25));
        m_minutes = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        m_minutes.setMaximumSize(new Dimension(SPINNER_WIDTH, 25));
        m_minutes.setMinimumSize(new Dimension(SPINNER_WIDTH, 25));
        m_seconds = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        m_seconds.setMaximumSize(new Dimension(SPINNER_WIDTH, 25));
        m_seconds.setMinimumSize(new Dimension(SPINNER_WIDTH, 25));
        m_startDate = new JDateChooser();
        m_startDate.setLocale(Locale.US);
        m_startDate.getJCalendar().getCalendar().setTimeZone(TimeZone.getTimeZone("UTC"));

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setMinimumSize(new Dimension(PANEL_WIDTH, 60));
        Box outerBox = Box.createVerticalBox();
        Box dateBox = Box.createHorizontalBox();
        if (m_isOptional) {
            dateBox.add(m_useDate);
            dateBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        }
        dateBox.add(m_startDate);
        dateBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
        dateBox.setPreferredSize(new Dimension(PANEL_WIDTH, 25));
        dateBox.setMaximumSize(new Dimension(PANEL_WIDTH, 25));
        dateBox.setMinimumSize(new Dimension(PANEL_WIDTH, 25));

        outerBox.add(dateBox);
        Box timeBox = Box.createHorizontalBox();
        if (m_displayHours) {
            timeBox.add(m_useHours);
            timeBox.add(new JLabel("Hour: "));
            timeBox.add(m_hours);
            timeBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
            timeBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
            if (m_displayMinutes) {
                timeBox.add(m_useMinutes);
                timeBox.add(new JLabel("Minute: "));
                timeBox.add(m_minutes);
                timeBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
                timeBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
                if (m_displaySeconds) {
                    timeBox.add(m_useSeconds);
                    timeBox.add(new JLabel("Second: "));
                    timeBox.add(m_seconds);
                    timeBox.add(Box.createHorizontalStrut(HORIZ_SPACE));
                }
            }
        }
        timeBox.add(Box.createHorizontalGlue());
        outerBox.setMaximumSize(new Dimension(PANEL_WIDTH, 60));
        outerBox.setMinimumSize(new Dimension(PANEL_WIDTH, 60));
        outerBox.add(Box.createVerticalStrut(VERT_SPACE));
        outerBox.add(timeBox);

        if (m_isOptional) {
            m_startDate.setEnabled(false);
            m_useHours.setEnabled(false);
        } else {
            m_startDate.setEnabled(true);
            m_useHours.setEnabled(true);
        }
        m_useHours.setSelected(false);
        m_hours.setEnabled(false);
        m_useMinutes.setEnabled(false);
        m_minutes.setEnabled(false);
        m_useSeconds.setEnabled(false);
        m_seconds.setEnabled(false);

        m_useDate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_useDate.isSelected()) {
                    m_startDate.setEnabled(true);
                    m_useHours.setEnabled(true);
                } else {
                    m_startDate.setEnabled(false);
                    m_startDate.cleanup();
                    m_useHours.setSelected(false);
                    m_useHours.setEnabled(false);
                    m_hours.setEnabled(false);
                    m_useMinutes.setEnabled(false);
                    m_useMinutes.setSelected(false);
                    m_minutes.setEnabled(false);
                    m_useSeconds.setEnabled(false);
                    m_useSeconds.setSelected(false);
                    m_seconds.setEnabled(false);
                }
            }
        });

        m_useHours.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_useHours.isSelected()) {
                    m_hours.setEnabled(true);
                    m_useMinutes.setEnabled(true);
                    m_minutes.setEnabled(false);
                    m_useSeconds.setEnabled(false);
                    m_seconds.setEnabled(false);
                } else {
                    m_hours.setEnabled(false);
                    m_useMinutes.setEnabled(false);
                    m_useMinutes.setSelected(false);
                    m_minutes.setEnabled(false);
                    m_useSeconds.setEnabled(false);
                    m_useSeconds.setSelected(false);
                    m_seconds.setEnabled(false);
                }
            }
        });

        m_useMinutes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_useMinutes.isSelected()) {
                    m_minutes.setEnabled(true);
                    m_useSeconds.setEnabled(true);
                    m_seconds.setEnabled(false);
                } else {
                    m_useMinutes.setSelected(false);
                    m_minutes.setEnabled(false);
                    m_useSeconds.setEnabled(false);
                    m_useSeconds.setSelected(false);
                    m_seconds.setEnabled(false);
                    m_seconds.setEnabled(false);
                }
            }
        });

        m_useSeconds.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_useSeconds.isSelected()) {
                    m_seconds.setEnabled(true);
                } else {
                    m_seconds.setEnabled(false);
                }
            }
        });
        add(outerBox);
        add(Box.createVerticalStrut(VERT_SPACE));
    }
}
