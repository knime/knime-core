/*
 *  JDayChooser.java  - A bean for choosing a day
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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * JDayChooser is a bean for choosing a day.
 * 
 * @author Kai Toedter
 * @version $LastChangedRevision$
 * @version $LastChangedDate$
 */
public class JDayChooser extends JPanel implements ActionListener, KeyListener,
		FocusListener {
	private static final long serialVersionUID = 5876398337018781820L;

	protected JButton[] days;

	protected JButton[] weeks;

	protected JButton selectedDay;

	protected JPanel weekPanel;

	protected JPanel dayPanel;

	protected int day;

	protected Color oldDayBackgroundColor;

	protected Color selectedColor;

	protected Color sundayForeground;

	protected Color weekdayForeground;

	protected Color decorationBackgroundColor;

	protected String[] dayNames;

	protected Calendar calendar;

	protected Calendar today;

	protected Locale locale;

	protected boolean initialized;

	protected boolean weekOfYearVisible;

	protected boolean decorationBackgroundVisible = true;

	protected boolean decorationBordersVisible;

	protected boolean dayBordersVisible;

	private boolean alwaysFireDayProperty;

	protected int maxDayCharacters;

	protected List dateEvaluators;
	
	protected MinMaxDateEvaluator minMaxDateEvaluator;

	/**
	 * Default JDayChooser constructor.
	 */
	public JDayChooser() {
		this(false);
	}

	/**
	 * JDayChooser constructor.
	 * 
	 * @param weekOfYearVisible
	 *            true, if the weeks of a year shall be shown
	 */
	public JDayChooser(boolean weekOfYearVisible) {
		setName("JDayChooser");
		setBackground(Color.blue);

		dateEvaluators = new ArrayList(1);
		minMaxDateEvaluator = new MinMaxDateEvaluator();
		addDateEvaluator(minMaxDateEvaluator);

		this.weekOfYearVisible = weekOfYearVisible;
		locale = Locale.getDefault();
		days = new JButton[49];
		selectedDay = null;
		calendar = Calendar.getInstance(locale);
		today = (Calendar) calendar.clone();

		setLayout(new BorderLayout());

		dayPanel = new JPanel();
		dayPanel.setLayout(new GridLayout(7, 7));

		sundayForeground = new Color(164, 0, 0);
		weekdayForeground = new Color(0, 90, 164);

		// decorationBackgroundColor = new Color(194, 211, 252);
		// decorationBackgroundColor = new Color(206, 219, 246);
		decorationBackgroundColor = new Color(210, 228, 238);

		for (int y = 0; y < 7; y++) {
			for (int x = 0; x < 7; x++) {
				int index = x + (7 * y);

				if (y == 0) {
					// Create a button that doesn't react on clicks or focus
					// changes.
					// Thanks to Thomas Schaefer for the focus hint :)
					days[index] = new DecoratorButton();
				} else {
					days[index] = new JButton("x") {
						private static final long serialVersionUID = -7433645992591669725L;

						public void paint(Graphics g) {
							if ("Windows".equals(UIManager.getLookAndFeel()
									.getID())) {
								// this is a hack to get the background painted
								// when using Windows Look & Feel
								if (selectedDay == this) {
									g.setColor(selectedColor);
									g.fillRect(0, 0, getWidth(), getHeight());
								}
							}
							super.paint(g);
						}

					};
					days[index].addActionListener(this);
					days[index].addKeyListener(this);
					days[index].addFocusListener(this);
				}

				days[index].setMargin(new Insets(0, 0, 0, 0));
				days[index].setFocusPainted(false);
				dayPanel.add(days[index]);
			}
		}

		weekPanel = new JPanel();
		weekPanel.setLayout(new GridLayout(7, 1));
		weeks = new JButton[7];

		for (int i = 0; i < 7; i++) {
			weeks[i] = new DecoratorButton();
			weeks[i].setMargin(new Insets(0, 0, 0, 0));
			weeks[i].setFocusPainted(false);
			weeks[i].setForeground(new Color(100, 100, 100));

			if (i != 0) {
				weeks[i].setText("0" + (i + 1));
			}

			weekPanel.add(weeks[i]);
		}

		init();

		setDay(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
		add(dayPanel, BorderLayout.CENTER);

		if (weekOfYearVisible) {
			add(weekPanel, BorderLayout.WEST);
		}

		initialized = true;
		updateUI();
	}

	/**
	 * Initializes the locale specific names for the days of the week.
	 */
	protected void init() {
		JButton testButton = new JButton();
		oldDayBackgroundColor = testButton.getBackground();
		selectedColor = new Color(160, 160, 160);

		Date date = calendar.getTime();
		calendar = Calendar.getInstance(locale);
		calendar.setTime(date);

		drawDayNames();
		drawDays();
	}

	/**
	 * Draws the day names of the day columnes.
	 */
	private void drawDayNames() {
		int firstDayOfWeek = calendar.getFirstDayOfWeek();
		DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(locale);
		dayNames = dateFormatSymbols.getShortWeekdays();

		int day = firstDayOfWeek;

		for (int i = 0; i < 7; i++) {
			if (maxDayCharacters > 0 && maxDayCharacters < 5) {
				if (dayNames[day].length() >= maxDayCharacters) {
					dayNames[day] = dayNames[day]
							.substring(0, maxDayCharacters);
				}
			}

			days[i].setText(dayNames[day]);

			if (day == 1) {
				days[i].setForeground(sundayForeground);
			} else {
				days[i].setForeground(weekdayForeground);
			}

			if (day < 7) {
				day++;
			} else {
				day -= 6;
			}
		}
	}

	/**
	 * Initializes both day names and weeks of the year.
	 */
	protected void initDecorations() {
		for (int x = 0; x < 7; x++) {
			days[x].setContentAreaFilled(decorationBackgroundVisible);
			days[x].setBorderPainted(decorationBordersVisible);
			days[x].invalidate();
			days[x].repaint();
			weeks[x].setContentAreaFilled(decorationBackgroundVisible);
			weeks[x].setBorderPainted(decorationBordersVisible);
			weeks[x].invalidate();
			weeks[x].repaint();
		}
	}

	/**
	 * Hides and shows the week buttons.
	 */
	protected void drawWeeks() {
		Calendar tmpCalendar = (Calendar) calendar.clone();

		for (int i = 1; i < 7; i++) {
			tmpCalendar.set(Calendar.DAY_OF_MONTH, (i * 7) - 6);

			int week = tmpCalendar.get(Calendar.WEEK_OF_YEAR);
			String buttonText = Integer.toString(week);

			if (week < 10) {
				buttonText = "0" + buttonText;
			}

			weeks[i].setText(buttonText);

			if ((i == 5) || (i == 6)) {
				weeks[i].setVisible(days[i * 7].isVisible());
			}
		}
	}

	/**
	 * Hides and shows the day buttons.
	 */
	protected void drawDays() {
		Calendar tmpCalendar = (Calendar) calendar.clone();
		tmpCalendar.set(Calendar.HOUR_OF_DAY, 0);
		tmpCalendar.set(Calendar.MINUTE, 0);
		tmpCalendar.set(Calendar.SECOND, 0);
		tmpCalendar.set(Calendar.MILLISECOND, 0);

		int firstDayOfWeek = tmpCalendar.getFirstDayOfWeek();
		tmpCalendar.set(Calendar.DAY_OF_MONTH, 1);

		int firstDay = tmpCalendar.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek;

		if (firstDay < 0) {
			firstDay += 7;
		}

		int i;

		for (i = 0; i < firstDay; i++) {
			days[i + 7].setVisible(false);
			days[i + 7].setText("");
		}

		tmpCalendar.add(Calendar.MONTH, 1);

		Date firstDayInNextMonth = tmpCalendar.getTime();
		tmpCalendar.add(Calendar.MONTH, -1);

		Date day = tmpCalendar.getTime();
		int n = 0;
		Color foregroundColor = getForeground();

		while (day.before(firstDayInNextMonth)) {
			days[i + n + 7].setText(Integer.toString(n + 1));
			days[i + n + 7].setVisible(true);

			if ((tmpCalendar.get(Calendar.DAY_OF_YEAR) == today
					.get(Calendar.DAY_OF_YEAR))
					&& (tmpCalendar.get(Calendar.YEAR) == today
							.get(Calendar.YEAR))) {
				days[i + n + 7].setForeground(sundayForeground);
			} else {
				days[i + n + 7].setForeground(foregroundColor);
			}

			if ((n + 1) == this.day) {
				days[i + n + 7].setBackground(selectedColor);
				selectedDay = days[i + n + 7];
			} else {
				days[i + n + 7].setBackground(oldDayBackgroundColor);
			}

			Iterator iterator = dateEvaluators.iterator(); 
			days[i + n + 7].setEnabled(true);
			while (iterator.hasNext()) {
				IDateEvaluator dateEvaluator = (IDateEvaluator) iterator.next();
				if (dateEvaluator.isSpecial(day)) {
					days[i + n + 7].setForeground(dateEvaluator
							.getSpecialForegroundColor());
					days[i + n + 7].setBackground(dateEvaluator
							.getSpecialBackroundColor());
					days[i + n + 7].setToolTipText(dateEvaluator.getSpecialTooltip());
					days[i + n + 7].setEnabled(true);
				} 
				if (dateEvaluator.isInvalid(day)){
					days[i + n + 7].setForeground(dateEvaluator
							.getInvalidForegroundColor());
					days[i + n + 7].setBackground(dateEvaluator
							.getInvalidBackroundColor());
					days[i + n + 7].setToolTipText(dateEvaluator.getInvalidTooltip());
					days[i + n + 7].setEnabled(false);
				}
			}

			n++;
			tmpCalendar.add(Calendar.DATE, 1);
			day = tmpCalendar.getTime();
		}

		for (int k = n + i + 7; k < 49; k++) {
			days[k].setVisible(false);
			days[k].setText("");
		}

		drawWeeks();
	}

	/**
	 * Returns the locale.
	 * 
	 * @return the locale value
	 * 
	 * @see #setLocale
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Sets the locale.
	 * 
	 * @param locale
	 *            the new locale value
	 * 
	 * @see #getLocale
	 */
	public void setLocale(Locale locale) {
		if (!initialized) {
			super.setLocale(locale);
		} else {
			this.locale = locale;
			super.setLocale(locale);
			init();
		}
	}

	/**
	 * Sets the day. This is a bound property.
	 * 
	 * @param d
	 *            the day
	 * 
	 * @see #getDay
	 */
	public void setDay(int d) {
		if (d < 1) {
			d = 1;
		}
		Calendar tmpCalendar = (Calendar) calendar.clone();
		tmpCalendar.set(Calendar.DAY_OF_MONTH, 1);
		tmpCalendar.add(Calendar.MONTH, 1);
		tmpCalendar.add(Calendar.DATE, -1);

		int maxDaysInMonth = tmpCalendar.get(Calendar.DATE);

		if (d > maxDaysInMonth) {
			d = maxDaysInMonth;
		}

		int oldDay = day;
		day = d;

		if (selectedDay != null) {
			selectedDay.setBackground(oldDayBackgroundColor);
			selectedDay.repaint();
		}

		for (int i = 7; i < 49; i++) {
			if (days[i].getText().equals(Integer.toString(day))) {
				selectedDay = days[i];
				selectedDay.setBackground(selectedColor);
				break;
			}
		}

		if (alwaysFireDayProperty) {
			firePropertyChange("day", 0, day);
		} else {
			firePropertyChange("day", oldDay, day);
		}
	}

	/**
	 * this is needed for JDateChooser.
	 * 
	 * @param alwaysFire
	 *            true, if day property shall be fired every time a day is
	 *            chosen.
	 */
	public void setAlwaysFireDayProperty(boolean alwaysFire) {
		alwaysFireDayProperty = alwaysFire;
	}

	/**
	 * Returns the selected day.
	 * 
	 * @return the day value
	 * 
	 * @see #setDay
	 */
	public int getDay() {
		return day;
	}

	/**
	 * Sets a specific month. This is needed for correct graphical
	 * representation of the days.
	 * 
	 * @param month
	 *            the new month
	 */
	public void setMonth(int month) {
		calendar.set(Calendar.MONTH, month);
		int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

		if (day > maxDays) {
			day = maxDays;
		}

		drawDays();
	}

	/**
	 * Sets a specific year. This is needed for correct graphical representation
	 * of the days.
	 * 
	 * @param year
	 *            the new year
	 */
	public void setYear(int year) {
		calendar.set(Calendar.YEAR, year);
		drawDays();
	}

	/**
	 * Sets a specific calendar. This is needed for correct graphical
	 * representation of the days.
	 * 
	 * @param calendar
	 *            the new calendar
	 */
	public void setCalendar(Calendar calendar) {
		this.calendar = calendar;
		drawDays();
	}

	/**
	 * Sets the font property.
	 * 
	 * @param font
	 *            the new font
	 */
	public void setFont(Font font) {
		if (days != null) {
			for (int i = 0; i < 49; i++) {
				days[i].setFont(font);
			}
		}
		if (weeks != null) {
			for (int i = 0; i < 7; i++) {
				weeks[i].setFont(font);
			}
		}
	}

	/**
	 * Sets the foregroundColor color.
	 * 
	 * @param foreground
	 *            the new foregroundColor
	 */
	public void setForeground(Color foreground) {
		super.setForeground(foreground);

		if (days != null) {
			for (int i = 7; i < 49; i++) {
				days[i].setForeground(foreground);
			}

			drawDays();
		}
	}

	/**
	 * JDayChooser is the ActionListener for all day buttons.
	 * 
	 * @param e
	 *            the ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		JButton button = (JButton) e.getSource();
		String buttonText = button.getText();
		int day = new Integer(buttonText).intValue();
		setDay(day);
	}

	/**
	 * JDayChooser is the FocusListener for all day buttons. (Added by Thomas
	 * Schaefer)
	 * 
	 * @param e
	 *            the FocusEvent
	 */
	/*
	 * Code below commented out by Mark Brown on 24 Aug 2004. This code breaks
	 * the JDateChooser code by triggering the actionPerformed method on the
	 * next day button. This causes the date chosen to always be incremented by
	 * one day.
	 */
	public void focusGained(FocusEvent e) {
		// JButton button = (JButton) e.getSource();
		// String buttonText = button.getText();
		//
		// if ((buttonText != null) && !buttonText.equals("") &&
		// !e.isTemporary()) {
		// actionPerformed(new ActionEvent(e.getSource(), 0, null));
		// }
	}

	/**
	 * Does nothing.
	 * 
	 * @param e
	 *            the FocusEvent
	 */
	public void focusLost(FocusEvent e) {
	}

	/**
	 * JDayChooser is the KeyListener for all day buttons. (Added by Thomas
	 * Schaefer and modified by Austin Moore)
	 * 
	 * @param e
	 *            the KeyEvent
	 */
	public void keyPressed(KeyEvent e) {
		int offset = (e.getKeyCode() == KeyEvent.VK_UP) ? (-7) : ((e
				.getKeyCode() == KeyEvent.VK_DOWN) ? (+7)
				: ((e.getKeyCode() == KeyEvent.VK_LEFT) ? (-1) : ((e
						.getKeyCode() == KeyEvent.VK_RIGHT) ? (+1) : 0)));

		int newDay = getDay() + offset;

		if ((newDay >= 1)
				&& (newDay <= calendar.getMaximum(Calendar.DAY_OF_MONTH))) {
			setDay(newDay);
		}
	}

	/**
	 * Does nothing.
	 * 
	 * @param e
	 *            the KeyEvent
	 */
	public void keyTyped(KeyEvent e) {
	}

	/**
	 * Does nothing.
	 * 
	 * @param e
	 *            the KeyEvent
	 */
	public void keyReleased(KeyEvent e) {
	}

	/**
	 * Enable or disable the JDayChooser.
	 * 
	 * @param enabled
	 *            The new enabled value
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		for (short i = 0; i < days.length; i++) {
			if (days[i] != null) {
				days[i].setEnabled(enabled);
			}
		}

		for (short i = 0; i < weeks.length; i++) {
			if (weeks[i] != null) {
				weeks[i].setEnabled(enabled);
			}
		}
	}

	/**
	 * In some Countries it is often usefull to know in which week of the year a
	 * date is.
	 * 
	 * @return boolean true, if the weeks of the year is shown
	 */
	public boolean isWeekOfYearVisible() {
		return weekOfYearVisible;
	}

	/**
	 * In some Countries it is often usefull to know in which week of the year a
	 * date is.
	 * 
	 * @param weekOfYearVisible
	 *            true, if the weeks of the year shall be shown
	 */
	public void setWeekOfYearVisible(boolean weekOfYearVisible) {
		if (weekOfYearVisible == this.weekOfYearVisible) {
			return;
		} else if (weekOfYearVisible) {
			add(weekPanel, BorderLayout.WEST);
		} else {
			remove(weekPanel);
		}

		this.weekOfYearVisible = weekOfYearVisible;
		validate();
		dayPanel.validate();
	}

	/**
	 * Returns the day panel.
	 * 
	 * @return the day panel
	 */
	public JPanel getDayPanel() {
		return dayPanel;
	}

	/**
	 * Returns the color of the decoration (day names and weeks).
	 * 
	 * @return the color of the decoration (day names and weeks).
	 */
	public Color getDecorationBackgroundColor() {
		return decorationBackgroundColor;
	}

	/**
	 * Sets the background of days and weeks of year buttons.
	 * 
	 * @param decorationBackgroundColor
	 *            The background to set
	 */
	public void setDecorationBackgroundColor(Color decorationBackgroundColor) {
		this.decorationBackgroundColor = decorationBackgroundColor;

		if (days != null) {
			for (int i = 0; i < 7; i++) {
				days[i].setBackground(decorationBackgroundColor);
			}
		}

		if (weeks != null) {
			for (int i = 0; i < 7; i++) {
				weeks[i].setBackground(decorationBackgroundColor);
			}
		}
	}

	/**
	 * Returns the Sunday foreground.
	 * 
	 * @return Color the Sunday foreground.
	 */
	public Color getSundayForeground() {
		return sundayForeground;
	}

	/**
	 * Returns the weekday foreground.
	 * 
	 * @return Color the weekday foreground.
	 */
	public Color getWeekdayForeground() {
		return weekdayForeground;
	}

	/**
	 * Sets the Sunday foreground.
	 * 
	 * @param sundayForeground
	 *            The sundayForeground to set
	 */
	public void setSundayForeground(Color sundayForeground) {
		this.sundayForeground = sundayForeground;
		drawDayNames();
		drawDays();
	}

	/**
	 * Sets the weekday foreground.
	 * 
	 * @param weekdayForeground
	 *            The weekdayForeground to set
	 */
	public void setWeekdayForeground(Color weekdayForeground) {
		this.weekdayForeground = weekdayForeground;
		drawDayNames();
		drawDays();
	}

	/**
	 * Requests that the selected day also have the focus.
	 */
	public void setFocus() {
		if (selectedDay != null) {
			this.selectedDay.requestFocus();
		}
	}

	/**
	 * The decoration background is the background color of the day titles and
	 * the weeks of the year.
	 * 
	 * @return Returns true, if the decoration background is painted.
	 */
	public boolean isDecorationBackgroundVisible() {
		return decorationBackgroundVisible;
	}

	/**
	 * The decoration background is the background color of the day titles and
	 * the weeks of the year.
	 * 
	 * @param decorationBackgroundVisible
	 *            true, if the decoration background shall be painted.
	 */
	public void setDecorationBackgroundVisible(
			boolean decorationBackgroundVisible) {
		this.decorationBackgroundVisible = decorationBackgroundVisible;
		initDecorations();
	}

	/**
	 * The decoration border is the button border of the day titles and the
	 * weeks of the year.
	 * 
	 * @return Returns true, if the decoration border is painted.
	 */
	public boolean isDecorationBordersVisible() {
		return decorationBordersVisible;
	}

	public boolean isDayBordersVisible() {
		return dayBordersVisible;
	}

	/**
	 * The decoration border is the button border of the day titles and the
	 * weeks of the year.
	 * 
	 * @param decorationBordersVisible
	 *            true, if the decoration border shall be painted.
	 */
	public void setDecorationBordersVisible(boolean decorationBordersVisible) {
		this.decorationBordersVisible = decorationBordersVisible;
		initDecorations();
	}

	public void setDayBordersVisible(boolean dayBordersVisible) {
		this.dayBordersVisible = dayBordersVisible;
		if (initialized) {
			for (int x = 7; x < 49; x++) {
				if ("Windows".equals(UIManager.getLookAndFeel().getID())) {
					days[x].setContentAreaFilled(dayBordersVisible);
				} else {
					days[x].setContentAreaFilled(true);
				}
				days[x].setBorderPainted(dayBordersVisible);
			}
		}
	}

	/**
	 * Updates the UI and sets the day button preferences.
	 */
	public void updateUI() {
		super.updateUI();
		setFont(Font.decode("Dialog Plain 11"));

		if (weekPanel != null) {
			weekPanel.updateUI();
		}
		if (initialized) {
			if ("Windows".equals(UIManager.getLookAndFeel().getID())) {
				setDayBordersVisible(false);
				setDecorationBackgroundVisible(true);
				setDecorationBordersVisible(false);
			} else {
				setDayBordersVisible(true);
				setDecorationBackgroundVisible(decorationBackgroundVisible);
				setDecorationBordersVisible(decorationBordersVisible);
			}
		}
	}

	/**
	 * Sets a valid date range for selectable dates. If max is before min, the
	 * default range with no limitation is set.
	 * 
	 * @param min
	 *            the minimum selectable date or null (then the minimum date is
	 *            set to 01\01\0001)
	 * @param max
	 *            the maximum selectable date or null (then the maximum date is
	 *            set to 01\01\9999)
	 */
	public void setSelectableDateRange(Date min, Date max) {
		minMaxDateEvaluator.setMaxSelectableDate(max);
		minMaxDateEvaluator.setMinSelectableDate(min);
		drawDays();
	}

	/**
	 * Sets the maximum selectable date. If null, the date 01\01\9999 will be
	 * set instead.
	 * 
	 * @param max
	 *            the maximum selectable date
	 * 
	 * @return the maximum selectable date
	 */
	public Date setMaxSelectableDate(Date max) {
		Date maxSelectableDate = minMaxDateEvaluator.setMaxSelectableDate(max);
		drawDays();
		return maxSelectableDate;
	}

	/**
	 * Sets the minimum selectable date. If null, the date 01\01\0001 will be
	 * set instead.
	 * 
	 * @param min
	 *            the minimum selectable date
	 * 
	 * @return the minimum selectable date
	 */
	public Date setMinSelectableDate(Date min) {
		Date minSelectableDate = minMaxDateEvaluator.setMinSelectableDate(min);
		drawDays();
		return minSelectableDate;
	}

	/**
	 * Gets the maximum selectable date.
	 * 
	 * @return the maximum selectable date
	 */
	public Date getMaxSelectableDate() {
		return minMaxDateEvaluator.getMaxSelectableDate();
	}

	/**
	 * Gets the minimum selectable date.
	 * 
	 * @return the minimum selectable date
	 */
	public Date getMinSelectableDate() {
		return minMaxDateEvaluator.getMinSelectableDate();
	}

	/**
	 * Gets the maximum number of characters of a day name or 0. If 0 is
	 * returned, dateFormatSymbols.getShortWeekdays() will be used.
	 * 
	 * @return the maximum number of characters of a day name or 0.
	 */
	public int getMaxDayCharacters() {
		return maxDayCharacters;
	}

	/**
	 * Sets the maximum number of characters per day in the day bar. Valid
	 * values are 0-4. If set to 0, dateFormatSymbols.getShortWeekdays() will be
	 * used, otherwise theses strings will be reduced to the maximum number of
	 * characters.
	 * 
	 * @param maxDayCharacters
	 *            the maximum number of characters of a day name.
	 */
	public void setMaxDayCharacters(int maxDayCharacters) {
		if (maxDayCharacters == this.maxDayCharacters) {
			return;
		}

		if (maxDayCharacters < 0 || maxDayCharacters > 4) {
			this.maxDayCharacters = 0;
		} else {
			this.maxDayCharacters = maxDayCharacters;
		}
		drawDayNames();
		drawDays();
		invalidate();
	}

	/**
	 * Creates a JFrame with a JDayChooser inside and can be used for testing.
	 * 
	 * @param s
	 *            The command line arguments
	 */
	public static void main(String[] s) {
		JFrame frame = new JFrame("JDayChooser");
		frame.getContentPane().add(new JDayChooser());
		frame.pack();
		frame.setVisible(true);
	}

	class DecoratorButton extends JButton {
		private static final long serialVersionUID = -5306477668406547496L;

		public DecoratorButton() {
			setBackground(decorationBackgroundColor);
			setContentAreaFilled(decorationBackgroundVisible);
			setBorderPainted(decorationBordersVisible);
		}

		public void addMouseListener(MouseListener l) {
		}

		public boolean isFocusable() {
			return false;
		}

		public void paint(Graphics g) {
			if ("Windows".equals(UIManager.getLookAndFeel().getID())) {
				// this is a hack to get the background painted
				// when using Windows Look & Feel
				if (decorationBackgroundVisible) {
					g.setColor(decorationBackgroundColor);
				} else {
					g.setColor(days[7].getBackground());
				}
				g.fillRect(0, 0, getWidth(), getHeight());
				if (isBorderPainted()) {
					setContentAreaFilled(true);
				} else {
					setContentAreaFilled(false);
				}
			}
			super.paint(g);
		}
	};

	public void addDateEvaluator(IDateEvaluator dateEvaluator) {
		dateEvaluators.add(dateEvaluator);
	}

	public void removeDateEvaluator(IDateEvaluator dateEvaluator) {
		dateEvaluators.remove(dateEvaluator);
	}
}
