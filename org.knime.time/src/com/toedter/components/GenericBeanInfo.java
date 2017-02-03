/*
 *  GenericBeanInfo.java  - Bean Info base for all  toedter.com beans
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
package com.toedter.components;

import java.awt.Image;
import java.beans.PropertyEditorManager;
import java.beans.SimpleBeanInfo;
import java.util.Locale;

/**
 * A generic bean info class use by all bean infos of the JCalendar bean suite.
 * 
 * @author Kai Toedter
 * @version $LastChangedRevision$
 * @version $LastChangedDate$
 */
public class GenericBeanInfo extends SimpleBeanInfo {
	/** 16x16 color icon. */
	protected Image iconColor16;

	/** 32x32 color icon. */
	protected Image iconColor32;

	/** 16x16 mono icon. */
	protected Image iconMono16;

	/** 32x32 mono icon. */
	protected Image iconMono32;

	/**
	 * Constructs a new BeanInfo.
	 */
	public GenericBeanInfo(String bean, boolean registerLocaleEditor) {
		try {
			iconColor16 = loadImage("images/" + bean + "Color16.gif");
			iconColor32 = loadImage("images/" + bean + "Color32.gif");
			iconMono16 = loadImage("images/" + bean + "Mono16.gif");
			iconMono32 = loadImage("images/" + bean + "Mono32.gif");
		} catch (RuntimeException e) {
			System.out.println("GenericBeanInfo.GenericBeanInfo(): " + e);
		}
		
		if(registerLocaleEditor) {
			PropertyEditorManager.registerEditor(Locale.class,LocaleEditor.class);
		}
	}

	/**
	 * This method returns an image object that can be used to represent the
	 * bean in toolboxes, toolbars, etc.
	 * 
	 * @param iconKind
	 *            the kind of requested icon
	 * 
	 * @return the icon image
	 */
	public Image getIcon(int iconKind) {
		switch (iconKind) {
			case ICON_COLOR_16x16 :
				return iconColor16;

			case ICON_COLOR_32x32 :
				return iconColor32;

			case ICON_MONO_16x16 :
				return iconMono16;

			case ICON_MONO_32x32 :
				return iconMono32;
		}

		return null;
	}
}