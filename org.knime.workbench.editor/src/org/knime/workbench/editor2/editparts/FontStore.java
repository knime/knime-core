/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   2010 10 29 (ohl): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.knime.core.api.node.workflow.AnnotationData;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ViewUtils;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Used by the workflow editor, annotations, annotation editor and figure to create or reuse a font according to user
 * set name and attributes or reusing a default font. This class scales the font according to the system zoom level
 * (for high dpi displays) - that is, it downscales (!) the size by the corresponding factor!
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class FontStore {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FontStore.class);

    private final Map<StoreKey, StoreValue> m_fontMap =
            new HashMap<StoreKey, StoreValue>();

    private final String m_defFontName;

    private final int m_defFontStyle;

    public static final FontStore INSTANCE = new FontStore();

    /**
     * Creates a new font store with a default font (which is used in case no
     * font name is specified).
     *
     * @param defaultFont
     */
    private FontStore() {
        Font defaultFont = defaultFont();
        FontData defaultFontData = defaultFont.getFontData()[0];
        m_defFontName = defaultFontData.getName();
        m_defFontStyle = defaultFontData.getStyle();
    }

    private static Font defaultFont() {
        /* We want to use "Arial" as THE font.
         * Fallback on Windows is:
         *     Microsoft Sans Serif, Trebuchet MS
         * Fallback on Mac:
         *     Trebuchet MS
         * Fallback on Linux:
         *     Nimbus Sans L
         */
        // the "new Font(..." constructor does not verify the validity of a font name so we need to check first
        // if the font is available.
        Set<String> installedFontFamilyNames = Stream.of(Display.getCurrent().getFontList(null, true))
                .map(f -> f.getName())
                .collect(Collectors.toSet());
        int defFontSize = getFontSizeFromKNIMEPrefPage();
        Font defFont = null;
        String name = "Arial";
        try {
            if (installedFontFamilyNames.contains(name)) {
                defFont = new Font(Display.getDefault(), name, defFontSize, SWT.NORMAL);
            }
        } catch (SWTError e) {
            LOGGER.warn("Font '" + name + "' is not available on your system. "
                    + "Try to install it, if you want workflows to look the same on different computers.", e);
        }
        if (defFont == null) {
            // Fall back to "common" fonts similar to Arial
            if (Platform.OS_MACOSX.equals(Platform.getOS())) {
                defFont = tryLoadFallbackFont(defFontSize, "Trebuchet MS", installedFontFamilyNames);
            } else if (Platform.OS_LINUX.equals(Platform.getOS())) {
                defFont = tryLoadFallbackFont(defFontSize, "Nimbus Sans L", installedFontFamilyNames);
            } else {
                defFont = tryLoadFallbackFont(defFontSize, "Microsoft Sans Serif", installedFontFamilyNames);
                if (defFont == null) {
                    defFont = tryLoadFallbackFont(defFontSize, "Trebuchet MS", installedFontFamilyNames);
                }
            }
        }
        if (defFont == null) {
            // last resort: use system default font. May look totally different on different systems.
            defFont = Display.getDefault().getSystemFont();
            LOGGER.warn("Using the system default font for annotations: " + defFont);
        }
        return defFont;
    }

    /** Used to instantiate SWT Font objects given a name. It will check the (AWT) list of available font names first.
     * @return the font object (which very likely is valid) or null if font not available. */
    private static Font tryLoadFallbackFont(final int defFontSize, final String name,
        final Set<String> installedFontFamilyNames) {
        if (installedFontFamilyNames.contains(name)) {
            try {
                return new Font(Display.getDefault(), name, defFontSize, SWT.NORMAL);
            } catch (SWTError e) {
                // this is not actually thrown by an invalid name - kept here because Font constructor may throw it
                LOGGER.warn("Unable to use fallback font '" + name + "'. ", e);
            }
        }
        return null;
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
        int pt = size;
        if (Boolean.getBoolean(KNIMEConstants.PROPERTY_HIGH_DPI_SUPPORT)) {
            int z = ViewUtils.getDisplayZoom();
            if (z != 100) {
                // the editor scales fonts with the display zoom. But the rest is unscaled!
                // Thus we scale down! So the editor can scale it up again.
                pt = pt * 100 / z;
            }
        }
        StoreKey key = new StoreKey(name, pt, style);
        StoreValue value = m_fontMap.get(key);
        if (value != null) {
            value.incrUseCount();
        } else {
            value = new StoreValue(name, pt, style);
            m_fontMap.put(key, value);
        }
        return value.getFont();

    }

    /** Get font according to KNIME style range. If font name and/or font
     * size are unspecified it will use the font data from the 2nd argument.
     * (Default font for node annotations grows and shrinks with global
     * preference setting).
     * @param knimeSR style range
     * @param defaultFont the default font (different for workflow and node
     * annotations).
     * @return A font object. */
    public Font getAnnotationFont(final AnnotationData.StyleRange knimeSR,
            final Font defaultFont) {
        String knFontName = knimeSR.getFontName();
        int knFontSize = knimeSR.getFontSize();
        final int knFontStyle = knimeSR.getFontStyle();
        if (knFontName == null || knFontSize <= 0) {
            knFontName = defaultFont.getFontData()[0].getName();
            knFontSize = defaultFont.getFontData()[0].getHeight();
        }
        return getFont(knFontName, knFontSize, knFontStyle);
    }

    /** Persists the font data in the argument style range. It only saves
     * font name and size if it's different from the default (pref page) font
     * as otherwise the font should change with changing the pref page values.
     * @param toSaveTo The style range to save to.
     * @param f The used font
     */
    public static void saveAnnotationFontToStyleRange(
            final AnnotationData.StyleRange.Builder toSaveTo,
            final Font f) {
        if (f != null) {
            final FontData fontData = f.getFontData()[0];
            String fontName = fontData.getName();
            int fontSize = fontData.getHeight();
            toSaveTo.setFontName(fontName);
            toSaveTo.setFontSize(fontSize);
            toSaveTo.setFontStyle(fontData.getStyle());
        }
    }

    /**
     * Returns the specified font with at least the specified style(s) set.
     *
     * @param font the font to add styles to
     * @param swtStyle the styles the result font should have (in addition to
     *            the styles already set in the font).
     *
     * @return the specified font with the specified style(s) set
     */
    public Font addStyleToFont(final Font font, final int swtStyle) {
        FontData fd = font.getFontData()[0];
        if ((fd.getStyle() & swtStyle) == swtStyle) {
            // already has all styles set
            return font;
        }
        return getFont(fd.getName(), fd.getHeight(), fd.getStyle() | swtStyle);
    }

    /**
     * Returns a font that has NOT the specified styles set. All other styles in
     * the font are unaffected.
     *
     * @param font the font to clear the styles in
     * @param swtStyle the style(s) to clear in the result font.
     * @return the specified font with the specified style(s) cleared.
     */
    public Font removeStyleFromFont(final Font font, final int swtStyle) {
        FontData fd = font.getFontData()[0];
        if ((fd.getStyle() & swtStyle) == 0) {
            // doesn't have the styles
            return font;
        }
        return getFont(fd.getName(), fd.getHeight(), fd.getStyle() & (~swtStyle));
    }

    /**
     * Returns the font. Which is Arial or a fallback font if Arial is not available. Font must be released if not
     * needed anymore.
     * @param size desired size (@see {@link #getFontSizeFromKNIMEPrefPage()}).
     *  @see #releaseFont(Font)
     * @return the font.
     */
    public Font getDefaultFont(final int size) {
        int pt = size;
        if (Boolean.getBoolean(KNIMEConstants.PROPERTY_HIGH_DPI_SUPPORT)) {
            int z = ViewUtils.getDisplayZoom();
            if (z != 100) {
                // the editor scales fonts with the display zoom. But the rest is unscaled! Thus we scale down!
                pt = pt * 100 / z;
                if (pt <= 0) {
                    pt = 1;
                }
            }
        }
        return getFont(m_defFontName, pt, m_defFontStyle);
    }

    /**
     * Returns the font in bold. Font must be released if not needed anymore.
     * @param size desired size (@see {@link #getFontSizeFromKNIMEPrefPage()}).
     *  @see #releaseFont(Font)
     * @return the font in bold
     */
    public Font getDefaultFontBold(final int size) {
        int pt = size;
        if (Boolean.getBoolean(KNIMEConstants.PROPERTY_HIGH_DPI_SUPPORT)) {
            int z = ViewUtils.getDisplayZoom();
            if (z != 100) {
                // the editor scales fonts with the display zoom. But the rest is unscaled! Thus we scale down!
                pt = pt * 100 / z;
            }
        }
        return getFont(m_defFontName, pt, m_defFontStyle | SWT.BOLD);
    }

    /**
     * Returns the default font in the desired size and style
     * @param size
     * @param bold
     * @param italic
     * @return the font.
     */
    public Font getDefaultFont(final int size, final boolean bold, final boolean italic) {
        int pt = size;
        if (Boolean.getBoolean(KNIMEConstants.PROPERTY_HIGH_DPI_SUPPORT)) {
            int z = ViewUtils.getDisplayZoom();
            if (z != 100) {
                // the editor scales fonts with the display zoom. But the rest is unscaled! Thus we scale down!
                pt = pt * 100 / z;
                if (pt <= 0) {
                    pt = 1;
                }
            }
        }
        int style = SWT.NONE;
        if (bold) {
            style |= SWT.BOLD;
        }
        if (italic) {
            style |= SWT.ITALIC;
        }
        return getFont(m_defFontName, pt, style);
    }

    /**
     * Returns true if the argument is the default font.
     *
     * @param font
     * @return true if the argument is the default font.
     */
    public boolean isDefaultFont(final Font font) {
        Font defaultFont = getFont(m_defFontName, getFontSizeFromKNIMEPrefPage(), m_defFontStyle);
        boolean result = font.equals(defaultFont);
        releaseFont(defaultFont);
        return result;
    }

    /**
     * Method is provided to support backward compatibility only. You should not use this method. Rather call
     * {@link #getDefaultFont(int)}. Avoid calling this method to maintain the same look on all systems.
     *
     * @return the default font of the system. May return different fonts on different systems!
     */
    public Font getSystemDefaultFont() {
        FontData sysFont = Display.getDefault().getSystemFont().getFontData()[0];
        // go through #getFont to account for highDPI scaling
        return getFont(sysFont.getName(), sysFont.getHeight(), sysFont.getStyle());
    }

    /**
     * Method is provided to support backward compatibility only. You should not use this method. Rather call
     * {@link #getDefaultFont(int)}. Avoid calling this method to maintain the same look on all systems.
     *
     * @return the default font of the system in bold and in the size set for node labels. May return different fonts on
     *         different systems!
     */
    public Font getSystemDefFontNodeAnnotations() {
        FontData sysFont = getSystemDefaultFont().getFontData()[0];
        return getFont(sysFont.getName(), getFontSizeFromKNIMEPrefPage(), SWT.NORMAL);
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
            val.getFont().dispose();
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
                m_name = m_defFontName;
            }
            if (height > 0) {
                m_height = height;
            } else {
                m_height = getFontSizeFromKNIMEPrefPage();
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
                fName = m_defFontName;
            }
            int fHeight = height;
            if (fHeight <= 0) {
                fHeight = getFontSizeFromKNIMEPrefPage();
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

    /**
     * Returns the font size value entered by the user in the pref page. Fonts usually size with the system zoom
     * factor (high dpi display). Depending on where you use this, you may need to scale the returned value.</br>
     * @return currently set preference value for the font size in the workflow editor (at the node)
     */
    public static int getFontSizeFromKNIMEPrefPage() {
        IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        int fontSize = store.getInt(PreferenceConstants.P_NODE_LABEL_FONT_SIZE);
        return fontSize;
    }

}
