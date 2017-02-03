/*
 *  JDateChooser.java  - A bean for choosing a date
 *  Copyright (C) 2004 Kai Toedter
 *  kai@toedter.com
 *  www.toedter.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.toedter.calendar;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A date chooser containing a date editor and a button, that makes a JCalendar visible for choosing a date. If no date
 * editor is specified, a JTextFieldDateEditor is used as default.
 *
 * @author Kai Toedter
 * @version $LastChangedRevision$
 * @version $LastChangedDate$
 */
public class JDateChooser extends JPanel implements ActionListener, PropertyChangeListener {

    private static final long serialVersionUID = -4306412745720670722L;

    protected IDateEditor dateEditor;

    protected JButton calendarButton;

    protected JCalendar jcalendar;

    protected JPopupMenu popup;

    protected boolean isInitialized;

    protected boolean dateSelected;

    protected Date lastSelectedDate;

    private ChangeListener changeListener;

    /**
     * Creates a new JDateChooser. By default, no date is set and the textfield is empty.
     */
    public JDateChooser() {
        this(null, null, null, null);
    }

    /**
     * Creates a new JDateChooser with given IDateEditor.
     *
     * @param dateEditor the dateEditor to be used used to display the date. if null, a JTextFieldDateEditor is used.
     */
    public JDateChooser(final IDateEditor dateEditor) {
        this(null, null, null, dateEditor);
    }

    /**
     * Creates a new JDateChooser.
     *
     * @param date the date or null
     */
    public JDateChooser(final Date date) {
        this(date, null);
    }

    /**
     * Creates a new JDateChooser.
     *
     * @param date the date or null
     * @param dateFormatString the date format string or null (then MEDIUM SimpleDateFormat format is used)
     */
    public JDateChooser(final Date date, final String dateFormatString) {
        this(date, dateFormatString, null);
    }

    /**
     * Creates a new JDateChooser.
     *
     * @param date the date or null
     * @param dateFormatString the date format string or null (then MEDIUM SimpleDateFormat format is used)
     * @param dateEditor the dateEditor to be used used to display the date. if null, a JTextFieldDateEditor is used.
     */
    public JDateChooser(final Date date, final String dateFormatString, final IDateEditor dateEditor) {
        this(null, date, dateFormatString, dateEditor);
    }

    /**
     * Creates a new JDateChooser. If the JDateChooser is created with this constructor, the mask will be always visible
     * in the date editor. Please note that the date pattern and the mask will not be changed if the locale of the
     * JDateChooser is changed.
     *
     * @param datePattern the date pattern, e.g. "MM/dd/yy"
     * @param maskPattern the mask pattern, e.g. "##/##/##"
     * @param placeholder the place holder character, e.g. '_'
     */
    public JDateChooser(final String datePattern, final String maskPattern, final char placeholder) {
        this(null, null, datePattern, new JTextFieldDateEditor(datePattern, maskPattern, placeholder));
    }

    /**
     * Creates a new JDateChooser.
     *
     * @param jcal the JCalendar to be used
     * @param date the date or null
     * @param dateFormatString the date format string or null (then MEDIUM Date format is used)
     * @param dateEditor the dateEditor to be used used to display the date. if null, a JTextFieldDateEditor is used.
     */
    public JDateChooser(final JCalendar jcal, final Date date, final String dateFormatString,
        final IDateEditor dateEditor) {
        setName("JDateChooser");

        this.dateEditor = dateEditor;
        if (this.dateEditor == null) {
            this.dateEditor = new JTextFieldDateEditor();
        }
        this.dateEditor.addPropertyChangeListener("date", this);

        if (jcal == null) {
            jcalendar = new JCalendar(date);
        } else {
            jcalendar = jcal;
            if (date != null) {
                jcalendar.setDate(date);
            }
        }

        setLayout(new BorderLayout());

        jcalendar.getDayChooser().addPropertyChangeListener("day", this);
        // always fire"day" property even if the user selects
        // the already selected day again
        jcalendar.getDayChooser().setAlwaysFireDayProperty(true);

        setDateFormatString(dateFormatString);
        setDate(date);

        // Display a calendar button with an icon
        URL iconURL = getClass().getResource("/com/toedter/calendar/images/JDateChooserIcon.gif");
        ImageIcon icon = new ImageIcon(iconURL);

        calendarButton = new JButton(icon) {
            private static final long serialVersionUID = -1913767779079949668L;

            @Override
            public boolean isFocusable() {
                return false;
            }
        };
        calendarButton.setMargin(new Insets(0, 0, 0, 0));
        calendarButton.addActionListener(this);

        // Alt + 'C' selects the calendar.
        calendarButton.setMnemonic(KeyEvent.VK_C);

        add(calendarButton, BorderLayout.EAST);
        add(this.dateEditor.getUiComponent(), BorderLayout.CENTER);

        calendarButton.setMargin(new Insets(0, 0, 0, 0));
        // calendarButton.addFocusListener(this);

        popup = new JPopupMenu() {
            private static final long serialVersionUID = -6078272560337577761L;

            @Override
            public void setVisible(final boolean b) {
                Boolean isCanceled = (Boolean)getClientProperty("JPopupMenu.firePopupMenuCanceled");
                if (b || (!b && dateSelected) || ((isCanceled != null) && !b && isCanceled.booleanValue())) {
                    super.setVisible(b);
                }
            }
        };

        popup.setLightWeightPopupEnabled(true);

        popup.add(jcalendar);

        lastSelectedDate = date;

        // Corrects a problem that occurred when the JMonthChooser's combobox is
        // displayed, and a click outside the popup does not close it.

        // The following idea was originally provided by forum user
        // podiatanapraia:
        changeListener = new ChangeListener() {
            boolean hasListened = false;

            @Override
            public void stateChanged(final ChangeEvent e) {
                if (hasListened) {
                    hasListened = false;
                    return;
                }
                if (popup.isVisible() && JDateChooser.this.jcalendar.monthChooser.getComboBox().hasFocus()) {
                    MenuElement[] me = MenuSelectionManager.defaultManager().getSelectedPath();
                    MenuElement[] newMe = new MenuElement[me.length + 1];
                    newMe[0] = popup;
                    for (int i = 0; i < me.length; i++) {
                        newMe[i + 1] = me[i];
                    }
                    hasListened = true;
                    MenuSelectionManager.defaultManager().setSelectedPath(newMe);
                }
            }
        };
        MenuSelectionManager.defaultManager().addChangeListener(changeListener);
        // end of code provided by forum user podiatanapraia

        isInitialized = true;
    }

    /**
     * Called when the calendar button was pressed.
     *
     * @param e the action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        int x = calendarButton.getWidth() - (int)popup.getPreferredSize().getWidth();
        int y = calendarButton.getY() + calendarButton.getHeight();

        Calendar calendar = Calendar.getInstance();
        Date date = dateEditor.getDate();
        if (date != null) {
            calendar.setTime(date);
        }
        jcalendar.setCalendar(calendar);
        popup.show(calendarButton, x, y);
        dateSelected = false;
    }

    /**
     * Listens for a "date" property change or a "day" property change event from the JCalendar. Updates the date editor
     * and closes the popup.
     *
     * @param evt the event
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("day")) {
            if (popup.isVisible()) {
                dateSelected = true;
                popup.setVisible(false);
                if (((Integer)evt.getNewValue()).intValue() > 0) {
                    setDate(jcalendar.getCalendar().getTime());
                } else {
                    setDate(null);
                }
            }
        } else if (evt.getPropertyName().equals("date")) {
            if (evt.getSource() == dateEditor) {
                firePropertyChange("date", evt.getOldValue(), evt.getNewValue());
            } else {
                setDate((Date)evt.getNewValue());
            }
        }
    }

    /**
     * Updates the UI of itself and the popup.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        setEnabled(isEnabled());

        if (jcalendar != null) {
            SwingUtilities.updateComponentTreeUI(popup);
        }
    }

    /**
     * Sets the locale.
     *
     * @param l The new locale value
     */
    @Override
    public void setLocale(final Locale l) {
        super.setLocale(l);
        dateEditor.setLocale(l);
        jcalendar.setLocale(l);
    }

    /**
     * Gets the date format string.
     *
     * @return Returns the dateFormatString.
     */
    public String getDateFormatString() {
        return dateEditor.getDateFormatString();
    }

    /**
     * Sets the date format string. E.g "MMMMM d, yyyy" will result in "July 21, 2004" if this is the selected date and
     * locale is English.
     *
     * @param dfString The dateFormatString to set.
     */
    public void setDateFormatString(final String dfString) {
        dateEditor.setDateFormatString(dfString);
        invalidate();
    }

    /**
     * Returns the date. If the JDateChooser is started with a null date and no date was set by the user, null is
     * returned.
     *
     * @return the current date
     */
    public Date getDate() {
        return dateEditor.getDate();
    }

    /**
     * Sets the date. Fires the property change "date" if date != null.
     *
     * @param date the new date.
     */
    public void setDate(final Date date) {
        dateEditor.setDate(date);
        if (getParent() != null) {
            getParent().invalidate();
        }
    }

    /**
     * Returns the calendar. If the JDateChooser is started with a null date (or null calendar) and no date was set by
     * the user, null is returned.
     *
     * @return the current calendar
     */
    public Calendar getCalendar() {
        Date date = getDate();
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    /**
     * Sets the calendar. Value null will set the null date on the date editor.
     *
     * @param calendar the calendar.
     */
    public void setCalendar(final Calendar calendar) {
        if (calendar == null) {
            dateEditor.setDate(null);
        } else {
            dateEditor.setDate(calendar.getTime());
        }
    }

    /**
     * Enable or disable the JDateChooser.
     *
     * @param enabled the new enabled value
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        if (dateEditor != null) {
            dateEditor.setEnabled(enabled);
            calendarButton.setEnabled(enabled);
        }
    }

    /**
     * Returns true, if enabled.
     *
     * @return true, if enabled.
     */
    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    /**
     * Sets the icon of the buuton.
     *
     * @param icon The new icon
     */
    public void setIcon(final ImageIcon icon) {
        calendarButton.setIcon(icon);
    }

    /**
     * Sets the font of all subcomponents.
     *
     * @param font the new font
     */
    @Override
    public void setFont(final Font font) {
        if (isInitialized) {
            dateEditor.getUiComponent().setFont(font);
            jcalendar.setFont(font);
        }
        super.setFont(font);
    }

    /**
     * Returns the JCalendar component. THis is usefull if you want to set some properties.
     *
     * @return the JCalendar
     */
    public JCalendar getJCalendar() {
        return jcalendar;
    }

    /**
     * Returns the calendar button.
     *
     * @return the calendar button
     */
    public JButton getCalendarButton() {
        return calendarButton;
    }

    /**
     * Returns the date editor.
     *
     * @return the date editor
     */
    public IDateEditor getDateEditor() {
        return dateEditor;
    }

    /**
     * Sets a valid date range for selectable dates. If max is before min, the default range with no limitation is set.
     *
     * @param min the minimum selectable date or null (then the minimum date is set to 01\01\0001)
     * @param max the maximum selectable date or null (then the maximum date is set to 01\01\9999)
     */
    public void setSelectableDateRange(final Date min, final Date max) {
        jcalendar.setSelectableDateRange(min, max);
        dateEditor.setSelectableDateRange(jcalendar.getMinSelectableDate(), jcalendar.getMaxSelectableDate());
    }

    public void setMaxSelectableDate(final Date max) {
        jcalendar.setMaxSelectableDate(max);
        dateEditor.setMaxSelectableDate(max);
    }

    public void setMinSelectableDate(final Date min) {
        jcalendar.setMinSelectableDate(min);
        dateEditor.setMinSelectableDate(min);
    }

    /**
     * Gets the maximum selectable date.
     *
     * @return the maximum selectable date
     */
    public Date getMaxSelectableDate() {
        return jcalendar.getMaxSelectableDate();
    }

    /**
     * Gets the minimum selectable date.
     *
     * @return the minimum selectable date
     */
    public Date getMinSelectableDate() {
        return jcalendar.getMinSelectableDate();
    }

    /**
     * Should only be invoked if the JDateChooser is not used anymore. Due to popup handling it had to register a change
     * listener to the default menu selection manager which will be unregistered here. Use this method to cleanup
     * possible memory leaks.
     */
    public void cleanup() {
        MenuSelectionManager.defaultManager().removeChangeListener(changeListener);
        changeListener = null;
    }

    @Override
    public boolean requestFocusInWindow() {
        if (dateEditor instanceof JComponent) {
            return ((JComponent)dateEditor).requestFocusInWindow();
        }
        return super.requestFocusInWindow();
    }

    /**
     * Creates a JFrame with a JDateChooser inside and can be used for testing.
     *
     * @param s The command line arguments
     */
    public static void main(final String[] s) {
        JFrame frame = new JFrame("JDateChooser");
        JDateChooser dateChooser = new JDateChooser();
        // JDateChooser dateChooser = new JDateChooser(null, new Date(), null,
        // null);
        // dateChooser.setLocale(new Locale("de"));
        // dateChooser.setDateFormatString("dd. MMMM yyyy");

        // dateChooser.setPreferredSize(new Dimension(130, 20));
        // dateChooser.setFont(new Font("Verdana", Font.PLAIN, 10));
        // dateChooser.setDateFormatString("yyyy-MM-dd HH:mm");

        // URL iconURL = dateChooser.getClass().getResource(
        // "/com/toedter/calendar/images/JMonthChooserColor32.gif");
        // ImageIcon icon = new ImageIcon(iconURL);
        // dateChooser.setIcon(icon);

        frame.getContentPane().add(dateChooser);
        frame.pack();
        frame.setVisible(true);

        dateChooser.requestFocusInWindow();
    }

}
