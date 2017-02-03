/*
 *  UTF8ResourceBundle.java  - A hack for reading UTF-8 encoded property files
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

package com.toedter.components;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * This class is a hack to read UTF-8 encoded property files. The implementation
 * is based on http://www.thoughtsabout.net/blog/archives/000044.html
 * 
 * @author Kai Toedter
 * 
 */
public abstract class UTF8ResourceBundle {

	public static final ResourceBundle getBundle(String baseName, Locale locale) {
		ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
		if (!(bundle instanceof PropertyResourceBundle))
			return bundle;

		return new UTF8PropertyResourceBundle((PropertyResourceBundle) bundle);
	}

	private static class UTF8PropertyResourceBundle extends ResourceBundle {
		PropertyResourceBundle propertyResourceBundle;

		private UTF8PropertyResourceBundle(PropertyResourceBundle bundle) {
			this.propertyResourceBundle = bundle;
		}

		public Enumeration getKeys() {
			return propertyResourceBundle.getKeys();
		}

		protected Object handleGetObject(String key) {
			String value = (String) propertyResourceBundle.handleGetObject(key);
			if (value != null) {
				try {
					return new String(value.getBytes("ISO-8859-1"), "UTF-8");
				} catch (UnsupportedEncodingException exception) {
					throw new RuntimeException(
							"UTF-8 encoding is not supported.", exception);
				}
			}
			return null;
		}
	}
}
