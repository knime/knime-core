/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Oct 20, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.util.LoadVersion;
import org.knime.shared.workflow.def.AnnotationDataDef;
import org.knime.shared.workflow.def.AnnotationDataDef.ContentTypeEnum;
import org.knime.shared.workflow.def.StyleRangeDef;

/**
 * @author  Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Kai Franze, KNIME GmbH
 */
public class AnnotationData implements Cloneable {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AnnotationData.class);

    /** Text alignment in annotation. */
    public enum TextAlignment {
        /** Left alignment. */
        LEFT,
        /** Center alignment. */
        CENTER,
        /** Right alignment. */
        RIGHT;
    }

    /**
     * @param swtAlignment a value which should be one of <code>SWT.RIGHT</code>, <code>SWT.CENTER</code>,
     *            <code>SWT.LEFT</code>
     * @return the semantically related <code>TextAlignment</code> enum
     * @since 4.0
     */
    public static TextAlignment getTextAlignmentForSWTAlignment(final int swtAlignment) {
        return switch (swtAlignment) {
            case SWT.RIGHT -> TextAlignment.RIGHT;
            case SWT.CENTER -> TextAlignment.CENTER;
            default -> TextAlignment.LEFT;
        };
    }

    /**
     * Old type annotation - font is system font (inconsistent layout).
     *
     * @since 3.0
     */
    public static final int VERSION_OLD = -1;

    /**
     * Released with 3.0 - uses (almost) same fonts on all systems.
     *
     * @since 3.0
     */
    public static final int VERSION_20151012 = 20151012;

    /**
     * Released with 3.1 - store default font size in annotation.
     *
     * @since 3.1
     */
    public static final int VERSION_20151123 = 20151123;

    /**
     * Released with 5.1 - Stores different content types in the text field.
     *
     * @since 5.1
     */
    public static final int VERSION_20230412 = 20230412;

    /**  */
    private String m_text = "";

    private ContentTypeEnum m_contentType = ContentTypeEnum.PLAIN;

    /**  */
    private StyleRange[] m_styleRanges = new StyleRange[0];

    /**  */
    private int m_bgColor;

    /**  */
    private int m_x;

    /**  */
    private int m_y;

    /**  */
    private int m_width;

    /**  */
    private int m_height;

    private TextAlignment m_alignment = TextAlignment.LEFT;

    private int m_borderSize = 0;

    private int m_borderColor = 0;

    private int m_defaultFontSize = -1;

    private int m_version = VERSION_20230412;

    /** @return the text */
    public final String getText() {
        return m_text;
    }

    /** @param text the text to set */
    public final void setText(final String text) {
        m_text = text;
    }

    /**
     * @return The content type in annotation
     *
     * @since 5.1
     */
    public final ContentTypeEnum getContentType() {
        return m_contentType;
    }

    /**
     * @param contentType The content type in annotation
     *
     * @since 5.1
     */
    public final void setContentType(final ContentTypeEnum contentType) {
        m_contentType = contentType;
    }

    /**
     * @param version of this annotation, e.g. {@link #VERSION_20230412}
     *
     * @since 5.2
     */
    public final void setVersion(final int version) {
        m_version = version;
    }

    /** @return the styleRanges */
    public final StyleRange[] getStyleRanges() {
        return m_styleRanges;
    }

    /** @param styleRanges the styleRanges to set */
    public final void setStyleRanges(final StyleRange[] styleRanges) {
        if (styleRanges == null || Arrays.asList(styleRanges).contains(null)) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_styleRanges = styleRanges;
    }

    /** @param styleRanges the styleRanges to set */
    public final void setStyleRanges(final List<StyleRangeDef> styleRanges) {
        if (styleRanges == null || styleRanges.contains(null)) {
            throw new RuntimeException("Argument must not be null.");
        }
        m_styleRanges = styleRanges.stream().map(s -> {
            var styleRange = new StyleRange();
            styleRange.setFgColor(s.getColor());
            styleRange.setFontName(s.getFontName());
            styleRange.setFontSize(s.getFontSize());
            styleRange.setFontStyle(s.getFontStyle());
            styleRange.setLength(s.getLength());
            styleRange.setStart(s.getStart());
            return styleRange;
        }).toArray(StyleRange[]::new);
    }

    /** @return the bgColor */
    public final int getBgColor() {
        return m_bgColor;
    }

    /** @param bgColor the bgColor to set */
    public final void setBgColor(final int bgColor) {
        m_bgColor = bgColor;
    }

    /** @return the x */
    public final int getX() {
        return m_x;
    }

    /** @param x the x to set */
    public final void setX(final int x) {
        m_x = x;
    }

    /** @return the y */
    public final int getY() {
        return m_y;
    }

    /** @param y the y to set */
    public final void setY(final int y) {
        m_y = y;
    }

    /** @return the width */
    public final int getWidth() {
        return m_width;
    }

    /** @param width the width to set */
    public final void setWidth(final int width) {
        m_width = width;
    }

    /** @return the height */
    public final int getHeight() {
        return m_height;
    }

    /** @param height the height to set */
    public final void setHeight(final int height) {
        m_height = height;
    }

    /** @param alignment the alignment to set */
    public final void setAlignment(final TextAlignment alignment) {
        m_alignment = alignment == null ? TextAlignment.LEFT : alignment;
    }

    /** @return the alignment */
    public final TextAlignment getAlignment() {
        return m_alignment;
    }

    /** @return the border width. 0 or neg. for no border.
     * @since 3.0*/
    public final int getBorderSize() {
        return m_borderSize;
    }

    /** @param size border size in pixel, 0 or neg. for no border.
     * @since 3.0*/
    public final void setBorderSize(final int size) {
        m_borderSize = size;
    }

    /** @return the color of the border. See also {@link #getBorderSize()}.
     * @since 3.0*/
    public final int getBorderColor() {
        return m_borderColor;
    }

    /** @param color the new border color to set.
     * @since 3.0*/
    public final void setBorderColor(final int color) {
        m_borderColor = color;
    }

    /** Shift annotation after copy&amp;paste.
     * @param xOff x offset
     * @param yOff y offset
     */
    public final void shiftPosition(final int xOff, final int yOff) {
        setX(getX() + xOff);
        setY(getY() + yOff);
    }

    /** Save-Version of this annotation. "Old" annotations used the system default font, newer ones will use
     * hardcoded font families.
     * @return the version, usually one of AnnotationData#VERSION_XYZ.
     * @since 3.0 */
    public final int getVersion() {
        return m_version;
    }

    /**
     * Set dimensionality.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param width width of component
     * @param height height of component
     */
    public final void setDimension(final int x, final int y, final int width,
            final int height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }

    /**
     * Set the default font size for this annotation.
     * @param size new default size
     * @since 3.1
     */
    public final void setDefaultFontSize(final int size) {
        m_defaultFontSize = size;
    }

    /**
     * Return the default font size for this annotation, or -1 if size from pref page should be used. (For old versions
     * only.)
     * @return the default font size for this annotation, or -1 if size from pref page should be used
     * @since 3.1
     */
    public final int getDefaultFontSize() {
        return m_defaultFontSize;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String text = getText();
        if (text == null) {
            return "";
        }
        String result = text.replaceAll("[\r\n]+", " ");
        if (result.length() > 60) {
            result = result.substring(0, 60).concat("...");
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AnnotationData)) {
            return false;
        }
        AnnotationData other = (AnnotationData)obj;
        if (!m_alignment.equals(other.m_alignment)) {
            return false;
        }
        if (m_bgColor != other.m_bgColor) {
            return false;
        }
        if (m_height != other.m_height) {
            return false;
        }
        if (m_width != other.m_width) {
            return false;
        }
        if (m_x != other.m_x) {
            return false;
        }
        if (m_y != other.m_y) {
            return false;
        }
        if (m_borderSize != other.m_borderSize) {
            return false;
        }
        if (m_borderColor != other.m_borderColor) {
            return false;
        }
        if (!Arrays.equals(m_styleRanges, other.m_styleRanges)) {
            return false;
        }
        if (!ConvenienceMethods.areEqual(m_text, other.m_text)) {
            return false;
        }
        if (m_defaultFontSize != other.m_defaultFontSize) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = m_alignment.hashCode();
        result ^= m_bgColor;
        result ^= m_height;
        result ^= m_width;
        result ^= m_x;
        result ^= m_y;
        result ^= m_borderColor;
        result ^= m_borderSize;
        result ^= m_defaultFontSize;
        result ^= Arrays.hashCode(m_styleRanges);
        if (m_text != null) {
            result ^= m_text.hashCode();
        }
        return result;
    }

    /**
     * Copy content, styles, position from the argument.
     *
     * @param otherData To copy from.
     * @param includeBounds Whether to also update x, y, width, height. If
     * false, it will only a copy the text with its styles
     */
    public void copyFrom(final AnnotationData otherData, final boolean includeBounds) {
        if (includeBounds) {
            setDimension(otherData.getX(), otherData.getY(),
                    otherData.getWidth(), otherData.getHeight());
        }
        setText(otherData.getText());
        setContentType(otherData.getContentType());
        setBgColor(otherData.getBgColor());
        setAlignment(otherData.getAlignment());
        setBorderSize(otherData.getBorderSize());
        setBorderColor(otherData.getBorderColor());
        setDefaultFontSize(otherData.getDefaultFontSize());
        m_version = otherData.getVersion();
        StyleRange[] otherStyles = otherData.getStyleRanges();
        StyleRange[] myStyles = cloneStyleRanges(otherStyles);
        setStyleRanges(myStyles);
    }

    /**
     * @param otherStyles
     * @return */
    private StyleRange[] cloneStyleRanges(final StyleRange[] otherStyles) {
        StyleRange[] myStyles = new StyleRange[otherStyles.length];
        for (int i = 0; i < otherStyles.length; i++) {
            myStyles[i] = otherStyles[i].clone();
        }
        return myStyles;
    }

    /** {@inheritDoc} */
    @Override
    public AnnotationData clone() {
        try {
            AnnotationData clone = (AnnotationData)super.clone();
            StyleRange[] clonedStyles = cloneStyleRanges(getStyleRanges());
            clone.setStyleRanges(clonedStyles);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Cant't clone", e);
        }
    }

    /**
     * Save all data.
     *
     * @param config To save to.
     */
    public void save(final NodeSettingsWO config) {
        config.addString("text", getText());
        config.addString("contentType", getContentType().toString());
        config.addInt("bgcolor", getBgColor());
        config.addInt("x-coordinate", getX());
        config.addInt("y-coordinate", getY());
        config.addInt("width", getWidth());
        config.addInt("height", getHeight());
        config.addString("alignment", getAlignment().toString());
        config.addInt("borderSize", m_borderSize);
        config.addInt("borderColor", m_borderColor);
        config.addInt("defFontSize", m_defaultFontSize);
        config.addInt("annotation-version", m_version);
        NodeSettingsWO styleConfigs = config.addNodeSettings("styles");
        var i = 0;
        for (StyleRange sr : getStyleRanges()) {
            NodeSettingsWO cur = styleConfigs.addNodeSettings("style_" + i);
            sr.save(cur);
            i++;
        }
    }

    /**
     * Helper method to get the {@link ContentTypeEnum}. It cannot be part of {@link AnnotationDataDef}, since it is
     * auto-generated.
     */
    private static ContentTypeEnum getContentTypeEnumFromContentTypeValue(final String contentTypeValue) {
        var values = ContentTypeEnum.values();
        for (var value : values) {
            if (contentTypeValue.equals(value.toString())) {
                return value;
            }
        }
        LOGGER.warn("Could not restore content type, setting it to 'text/plain' by default");
        return ContentTypeEnum.PLAIN;
    }

    /**
     * Loads new values.
     *
     * @param config To load from
     * @param loadVersion Version to load
     * @throws InvalidSettingsException If fails
     * @since 3.7
     */
    public void load(final NodeSettingsRO config, final LoadVersion loadVersion) throws InvalidSettingsException {
        setText(config.getString("text"));

        // Default to 'text/plain' for backward compatibility
        var contentTypeValue = config.getString("contentType", ContentTypeEnum.PLAIN.toString());
        setContentType(getContentTypeEnumFromContentTypeValue(contentTypeValue));

        setBgColor(config.getInt("bgcolor"));
        int x = config.getInt("x-coordinate");
        int y = config.getInt("y-coordinate");
        int width = config.getInt("width");
        int height = config.getInt("height");
        int borderSize = config.getInt("borderSize", 0); // default to 0 for backward compatibility
        int borderColor = config.getInt("borderColor", 0); // default for backward compatibility
        int defFontSize = config.getInt("defFontSize", -1); // default for backward compatibility
        m_version = config.getInt("annotation-version", VERSION_OLD); // added in 3.0

        TextAlignment alignment = TextAlignment.LEFT;
        if (loadVersion.ordinal() >= LoadVersion.V250.ordinal()) {
            String alignmentS = config.getString("alignment");
            try {
                alignment = TextAlignment.valueOf(alignmentS);
            } catch (Exception e) {
                throw new InvalidSettingsException("Invalid alignment: " + alignmentS, e);
            }
        }

        setDimension(x, y, width, height);
        setAlignment(alignment);
        setBorderSize(borderSize);
        setBorderColor(borderColor);
        setDefaultFontSize(defFontSize);

        NodeSettingsRO styleConfigs = config.getNodeSettings("styles");
        StyleRange[] styles = new StyleRange[styleConfigs.getChildCount()];
        var i = 0;
        for (String key : styleConfigs.keySet()) {
            NodeSettingsRO cur = styleConfigs.getNodeSettings(key);
            styles[i] = StyleRange.load(cur);
            i++;
        }
        setStyleRanges(styles);
    }

    /** Formatting rule on the text; similar to SWT style range. */
    public static final class StyleRange implements Cloneable {

        /**
         * The font style constant indicating a normal weight, non-italic font (value is 0).
         *
         * @since 4.3
         */
        public static final int NORMAL = 0;

        /**
         * The font style constant indicating a bold weight font (value is 1&lt;&lt;0).
         *
         * @since 4.3
         */
        public static final int BOLD = 1 << 0;

        /**
         * The font style constant indicating an italic font (value is 1&lt;&lt;1).
         *
         * @since 4.3
         */
        public static final int ITALIC = 1 << 1;

        private int m_start;

        private int m_length;

        private String m_fontName;

        private int m_fontStyle;

        private int m_fontSize;

        private int m_fgColor;

        /** @return the start */
        public int getStart() {
            return m_start;
        }

        /** @param start the start to set */
        public void setStart(final int start) {
            m_start = start;
        }

        /** @return the length */
        public int getLength() {
            return m_length;
        }

        /** @param length the length to set */
        public void setLength(final int length) {
            m_length = length;
        }

        /** @return the fontName */
        public String getFontName() {
            return m_fontName;
        }

        /** @param fontName the fontName to set */
        public void setFontName(final String fontName) {
            m_fontName = fontName;
        }

        /** @return the fontStyle */
        public int getFontStyle() {
            return m_fontStyle;
        }

        /** @param fontStyle the fontStyle to set */
        public void setFontStyle(final int fontStyle) {
            m_fontStyle = fontStyle;
        }

        /** @return the fontSize */
        public int getFontSize() {
            return m_fontSize;
        }

        /** @param fontSize the fontSize to set */
        public void setFontSize(final int fontSize) {
            m_fontSize = fontSize;
        }

        /** @return the fgColor */
        public int getFgColor() {
            return m_fgColor;
        }

        /** @param fgColor the fgColor to set */
        public void setFgColor(final int fgColor) {
            m_fgColor = fgColor;
        }

        /**
         * Save to settings.
         *
         * @param settings To save to.
         */
        public void save(final NodeSettingsWO settings) {
            settings.addInt("start", getStart());
            settings.addInt("length", getLength());
            settings.addString("fontname", getFontName());
            settings.addInt("fontstyle", getFontStyle());
            settings.addInt("fontsize", getFontSize());
            settings.addInt("fgcolor", getFgColor());
        }

        /**
         * Load from settings.
         *
         * @param settings To load from.
         * @return The load style.
         * @throws InvalidSettingsException If that fails.
         */
        public static StyleRange load(final NodeSettingsRO settings)
                throws InvalidSettingsException {
            StyleRange result = new StyleRange();
            result.setStart(settings.getInt("start"));
            result.setLength(settings.getInt("length"));
            result.setFontName(settings.getString("fontname"));
            result.setFontStyle(settings.getInt("fontstyle"));
            result.setFontSize(settings.getInt("fontsize"));
            result.setFgColor(settings.getInt("fgcolor"));
            return result;
        }

        /** {@inheritDoc} */
        @Override
        protected StyleRange clone() {
            try {
                return (StyleRange)super.clone();
            } catch (CloneNotSupportedException e) {
                LOGGER.error("Error cloning style", e);
            }
            return new StyleRange();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof StyleRange)) {
                return false;
            }
            StyleRange other = (StyleRange)obj;
            if (other.m_fgColor != m_fgColor) {
                return false;
            }
            if (other.m_fontSize != m_fontSize) {
                return false;
            }
            if (other.m_fontStyle != m_fontStyle) {
                return false;
            }
            if (!ConvenienceMethods.areEqual(m_fontName, other.m_fontName) ) {
                return false;
            }
            if (other.m_length != m_length) {
                return false;
            }
            if (other.m_start!= m_start) {
                return false;
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int result = m_fgColor;
            result ^= m_fontSize;
            result ^= m_fontStyle;
            result ^= m_length;
            result ^= m_start;
            if (m_fontName != null) {
                result ^= m_fontName.hashCode();
            }
            return result;
        }

    }

}
