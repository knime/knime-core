/*
 *  Copyright (C) 2011 Kai Toedter 
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

import java.awt.Color;
import java.util.Date;

/**
 * Implementations of this interface can be added to various JCalendar
 * components to check if certain dates are valid for selection.
 * 
 * @author Kai Toedter
 * @version $LastChangedRevision: 142 $
 * @version $LastChangedDate: 2011-06-05 07:06:03 +0200 (So, 05 Jun 2011) $
 * 
 */
public interface IDateEvaluator {
	/**
	 * Checks if a date is a special date (might have different colors and tooltips)
	 * 
	 * @param date
	 *            the date to check
	 * @return true, if the date can be selected
	 */
	boolean isSpecial(Date date);

	/**
	 * @return the foreground color (used by JDayChooser)
	 */
	Color getSpecialForegroundColor();

	/**
	 * @return the background color (used by JDayChooser)
	 */
	Color getSpecialBackroundColor();
	
	/**
	 * @return the tooltip (used by JDayChooser)
	 */
	String getSpecialTooltip();
	
	/**
	 * Checks if a date is invalid for selection
	 * 
	 * @param date
	 *            the date to check
	 * @return true, if the date is invalid and cannot be selected
	 */
	boolean isInvalid(Date date);

	/**
	 * @return the foreground color (used by JDayChooser)
	 */
	Color getInvalidForegroundColor();

	/**
	 * @return the background color (used by JDayChooser)
	 */
	Color getInvalidBackroundColor();
	
	/**
	 * @return the tooltip (used by JDayChooser)
	 */
	String getInvalidTooltip();

}
