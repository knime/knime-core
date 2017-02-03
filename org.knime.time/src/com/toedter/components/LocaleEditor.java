/*
 * LocaleEditor.java - a locale editor for JavaBeans
 * Copyright (C) 2004 Kai Toedter
 * kai@toedter.com
 * www.toedter.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.toedter.components;

import java.util.Calendar;
import java.util.Locale;

/**
 * Property editor for locales.
 * 
 * @author Kai Toedter
 * @version $LastChangedRevision$
 * @version $LastChangedDate$
 */
public class LocaleEditor extends java.beans.PropertyEditorSupport {
	private Locale[] locales;
	private String[] localeStrings;
	private Locale locale;
	private int length;

	/**
	 * Default LocaleEditor constructor.
	 */
	public LocaleEditor() {
		locale = Locale.getDefault();
		locales = Calendar.getAvailableLocales();
		length = locales.length;
		localeStrings = new String[length];
	}

	/**
	 * Returns the locale strings.
	 * 
	 * @return the locale strings
	 */
	public String[] getTags() {
		for (int i = 0; i < length; i++)
			localeStrings[i] = locales[i].getDisplayName();

		return localeStrings;
	}

	/**
	 * Sets the locale strings as text and invokes setValue( locale ).
	 * 
	 * @param text
	 *            the locale string text
	 * 
	 * @throws IllegalArgumentException
	 *             not used
	 */
	public void setAsText(String text) throws IllegalArgumentException {
		for (int i = 0; i < length; i++)
			if (text.equals(locales[i].getDisplayName())) {
				locale = locales[i];
				setValue(locale);

				break;
			}
	}

	/**
	 * Returns the locale string as text.
	 * 
	 * @return the locale string
	 */
	public String getAsText() {
		return locale.getDisplayName();
	}
}