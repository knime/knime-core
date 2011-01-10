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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   2010 10 29 (ohl): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

/**
 * Used by the annotations, annotation editor and figure to create or reuse a
 * font according to user set name and attributes or reusing a default font.
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class FontStore {

    private final HashMap<StoreKey, StoreValue> m_fontMap =
            new HashMap<StoreKey, StoreValue>();

    private final Font m_defaultFont;

    /**
     * Creates a new font store with a default font (which is used in case no
     * font name is specified).
     *
     * @param defaultFont
     */
    public FontStore(final Font defaultFont) {
        if (defaultFont == null) {
            throw new NullPointerException("Default font can't be null");
        }
        m_defaultFont = defaultFont;
        m_fontMap.put(new StoreKey(defaultFont), new StoreValue(defaultFont));
    }

    /**
     * Returns the font with the specified attributes (using attributes of the
     * default font, in case name is null or the size is not positive). Each
     * font must be released after it is not used anymore (@see #releaseFont).
     *
     * @param name of the font to return (could be null, in which case the name
     *            of the default font is used).
     * @param size of the font to return (if zero or negative the size of the
     *            default font is used).
     * @param style an SWT style (BOLD or ITALIC, etc.)
     * @return the specified font - keeping a reference count. Release the font
     *         after usage
     */
    public Font getFont(final String name, final int size, final int style) {
        StoreKey key = new StoreKey(name, size, style);
        StoreValue value = m_fontMap.get(key);
        if (value != null) {
            value.incrUseCount();
        } else {
            value = new StoreValue(name, size, style);
            m_fontMap.put(key, value);
        }
        return value.getFont();

    }

    /**
     * Returns the specified font with at least the specified style(s) set.
     *
     * @param font the font to add styles to
     * @param SWTstyle the styles the result font should have (in addition to
     *            the styles already set in the font).
     *
     * @return the specified font with the specified style(s) set
     */
    public Font addStyleToFont(final Font font, final int SWTstyle) {
        Font f = font;
        if (f == null) {
            f = m_defaultFont;
        }
        FontData fd = f.getFontData()[0];
        if ((fd.getStyle() & SWTstyle) == SWTstyle) {
            // already has all styles set
            return f;
        }
        return getFont(fd.getName(), fd.getHeight(), fd.getStyle() | SWTstyle);
    }

    /**
     * Returns a font that has NOT the specified styles set. All other styles in
     * the font are unaffected.
     *
     * @param font the font to clear the styles in
     * @param SWTstyle the style(s) to clear in the result font.
     * @return the specified font with the specified style(s) cleared.
     */
    public Font removeStyleFromFont(final Font font, final int SWTstyle) {
        Font f = font;
        if (f == null) {
            f = m_defaultFont;
        }
        FontData fd = f.getFontData()[0];
        if ((fd.getStyle() & SWTstyle) == 0) {
            // doesn't have the styles
            return f;
        }
        return getFont(fd.getName(), fd.getHeight(), fd.getStyle()
                & (~SWTstyle));
    }

    /**
     * Returns the font initially set as default font.
     *
     * @return the font initially set as default font
     */
    public Font getDefaultFont() {
        return m_defaultFont;
    }

    /**
     * Returns true if the argument is the default font specified at creation
     * time. (Does a pointer compare only.)
     *
     * @param font
     * @return true if the argument is the default font specified at creation
     *         time.
     */
    public boolean isDefaultFont(final Font font) {
        return font == m_defaultFont;
    }

    /**
     * @param font releases one reference count of the specified font. If the
     *            reference count is at zero, the font is disposed.
     * @return the reference count after this release
     */
    public int releaseFont(final Font font) {
        StoreKey key = new StoreKey(font);
        StoreValue val = m_fontMap.get(key);
        if (val == null) {
            // strange
            return 0;
        }
        int usage = val.decrUseCount();
        if (usage == 0) {
            m_fontMap.remove(key);
            Font f = val.getFont();
            if (f != m_defaultFont) {
                // default font SHOULD never have ref count zero...
                val.getFont().dispose();
            }
        }
        return usage;
    }

    private final class StoreKey {
        private final String m_name;

        private final int m_height;

        private final int m_style;

        private StoreKey(final Font font) {
            FontData fd = font.getFontData()[0];
            m_name = fd.getName();
            m_height = fd.getHeight();
            m_style = fd.getStyle();
        }

        private StoreKey(final String name, final int height, final int style) {
            if (name != null && !name.isEmpty()) {
                m_name = name;
            } else {
                m_name = m_defaultFont.getFontData()[0].getName();
            }
            if (height > 0) {
                m_height = height;
            } else {
                m_height = m_defaultFont.getFontData()[0].getHeight();
            }
            m_style = style;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return m_name.hashCode() ^ m_height ^ m_style;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof StoreKey) {
                StoreKey sk = (StoreKey)obj;
                return (m_height == sk.m_height && m_style == sk.m_style && m_name
                        .toLowerCase().equals(sk.m_name.toLowerCase()));
            }
            return false;
        }
    }

    private final class StoreValue {
        private final Font m_font;

        private AtomicInteger m_useCount = new AtomicInteger();

        /**
         * Initializes the usage count with one (meaning the font is tagged
         * being used once - no need to increment usage after creation).
         *
         * @param value
         */
        private StoreValue(final String name, final int height, final int style) {
            String fName = name;
            if (fName == null || fName.isEmpty()) {
                fName = m_defaultFont.getFontData()[0].getName();
            }
            int fHeight = height;
            if (fHeight <= 0) {
                fHeight = m_defaultFont.getFontData()[0].getHeight();
            }

            m_font = new Font(null, fName, fHeight, style);
            m_useCount.set(1);
        }

        private StoreValue(final Font value) {
            m_font = value;
            m_useCount.set(1);
        }

        private Font getFont() {
            return m_font;
        }

        /**
         * @return the new (incremented) usage count
         */
        private int incrUseCount() {
            return m_useCount.incrementAndGet();
        }

        private int decrUseCount() {
            return m_useCount.decrementAndGet();
        }
    }
}
