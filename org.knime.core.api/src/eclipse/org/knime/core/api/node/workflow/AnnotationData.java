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
 *   Oct 20, 2011 (wiswedel): created
 */
package org.knime.core.api.node.workflow;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author  Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class AnnotationData {

    /** Text alignment in annotation. */
    public enum TextAlignment {
        /** Left alignment. */
        LEFT,
        /** Center alignment. */
        CENTER,
        /** Right alignment. */
        RIGHT;
    }

    /** Old type annotation - font is system font (inconsistent layout).
     * @since 3.0 */
    public static final int VERSION_OLD = -1;
    /** Released with 3.0 - uses (almost) same fonts on all systems.
     * @since 3.0 */
    public static final int VERSION_20151012 = 20151012;

    /** Released with 3.1: store default font size in annotation.
     * @since 3.1 */
    public static final int VERSION_20151123 = 20151123;

    /**  */
    private String m_text = "";

    /**  */
    private StyleRange[] m_styleRanges = new StyleRange[0];

    /**  */
    private final int m_bgColor;

    /**  */
    private final int m_x;

    /**  */
    private final int m_y;

    /**  */
    private final int m_width;

    /**  */
    private final int m_height;

    private final TextAlignment m_alignment;

    private final int m_borderSize;

    private final int m_borderColor;

    private final int m_defaultFontSize;

    private final int m_version;

    /**
     * Creates a new annotation data object from the passed builder.
     *
     * @param builder
     */
    AnnotationData(@SuppressWarnings("rawtypes") final Builder builder) {
        m_text = builder.m_text;
        m_styleRanges = builder.m_styleRanges;
        m_bgColor = builder.m_bgColor;
        m_x = builder.m_x;
        m_y = builder.m_y;
        m_width = builder.m_width;
        m_height = builder.m_height;
        m_alignment = builder.m_alignment;
        m_borderSize = builder.m_borderSize;
        m_borderColor = builder.m_borderColor;
        m_defaultFontSize = builder.m_defaultFontSize;
        m_version = builder.m_version;
    }

    /** @return the text */
    public final String getText() {
        return m_text;
    }

    /** @return the styleRanges */
    public final StyleRange[] getStyleRanges() {
        return m_styleRanges;
    }

    /** @return the bgColor */
    public final int getBgColor() {
        return m_bgColor;
    }

    /** @return the x */
    public final int getX() {
        return m_x;
    }

    /** @return the y */
    public final int getY() {
        return m_y;
    }

    /** @return the width */
    public final int getWidth() {
        return m_width;
    }

    /** @return the height */
    public final int getHeight() {
        return m_height;
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

    /** @return the color of the border. See also {@link #getBorderSize()}.
     * @since 3.0*/
    public final int getBorderColor() {
        return m_borderColor;
    }

    /** Save-Version of this annotation. "Old" annotations used the system default font, newer ones will use
     * hardcoded font families.
     * @return the version, usually one of AnnotationData#VERSION_XYZ.
     * @since 3.0 */
    public final int getVersion() {
        return m_version;
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
        if (!Objects.equals(m_text, other.m_text)) {
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
     * Helper method to clone an array of {@link StyleRange}s.
     *
     * @param otherStyles
     * @return */
    private static StyleRange[] cloneStyleRanges(final StyleRange[] otherStyles) {
        StyleRange[] myStyles = new StyleRange[otherStyles.length];
        for (int i = 0; i < otherStyles.length; i++) {
            myStyles[i] = StyleRange.builder(otherStyles[i]).build();
        }
        return myStyles;
    }

    /** {@inheritDoc} */
    @Override
    public AnnotationData clone() {
        //we should not provide a clone method
        //e.g. conflicts with the final-fields
        //see also https://stackoverflow.com/questions/2427883/clone-vs-copy-constructor-which-is-recommended-in-java
        throw new UnsupportedOperationException();
    }

    /** @return new Builder with defaults. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param annoData object to copy the values from
     * @param includeBounds Whether to also update x, y, width, height. If false, it will only a copy the text with
         *            its styles
     * @return new Builder with the values copied from the passed argument
     */
    public static final Builder builder(final AnnotationData annoData, final boolean includeBounds) {
        return new Builder().copyFrom(annoData, includeBounds);
    }

    /** Builder pattern for {@link AnnotationData}.
     * @param <B> allows to return the right builder when sub-classed
     * @param <D>
     * */
    public static class Builder<B extends Builder<B, D>, D extends AnnotationData> {

        /**  */
        private String m_text = "";

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

        private int m_version = VERSION_20151123;

        /**
         * Copy content, styles, position from the argument.
         *
         * @param otherData To copy from.
         * @param includeBounds Whether to also update x, y, width, height. If false, it will only a copy the text with
         *            its styles
         * @return this
         */
        public B copyFrom(final AnnotationData otherData, final boolean includeBounds) {
            if (includeBounds) {
                setDimension(otherData.getX(), otherData.getY(), otherData.getWidth(), otherData.getHeight());
            }
            setText(otherData.getText());
            setBgColor(otherData.getBgColor());
            setAlignment(otherData.getAlignment());
            setBorderSize(otherData.getBorderSize());
            setBorderColor(otherData.getBorderColor());
            setDefaultFontSize(otherData.getDefaultFontSize());
            m_version = otherData.getVersion();
            StyleRange[] otherStyles = otherData.getStyleRanges();
            StyleRange[] myStyles = cloneStyleRanges(otherStyles);
            setStyleRanges(myStyles);
            return (B)this;
        }

        /** @param text the text to set
         * @return this*/
        public final B setText(final String text) {
            m_text = text;
            return (B)this;
        }

        /** @param styleRanges the styleRanges to set
         * @return this*/
        public final B setStyleRanges(final StyleRange[] styleRanges) {
            if (styleRanges == null || Arrays.asList(styleRanges).contains(null)) {
                throw new NullPointerException("Argument must not be null.");
            }
            m_styleRanges = styleRanges;
            return (B)this;
        }

        /** @param bgColor the bgColor to set
         * @return this*/
        public final B setBgColor(final int bgColor) {
            m_bgColor = bgColor;
            return (B)this;
        }

        /** @param x the x to set
         * @return this*/
        public final B setX(final int x) {
            m_x = x;
            return (B)this;
        }

        /** @param y the y to set
         * @return this*/
        public final B setY(final int y) {
            m_y = y;
            return (B)this;
        }

        /** @param width the width to set
         * @return this*/
        public final B setWidth(final int width) {
            m_width = width;
            return (B)this;
        }

        /** @param height the height to set
         * @return this*/
        public final B setHeight(final int height) {
            m_height = height;
            return (B)this;
        }

        /** @param alignment the alignment to set
         * @return this*/
        public final B setAlignment(final TextAlignment alignment) {
            m_alignment = alignment == null ? TextAlignment.LEFT : alignment;
            return (B)this;
        }

        /** @param size border size in pixel, 0 or neg. for no border.
         * @return this
         * @since 3.0*/
        public final B setBorderSize(final int size) {
            m_borderSize = size;
            return (B)this;
        }

        /** @param color the new border color to set.
         * @return this
         * @since 3.0*/
        public final B setBorderColor(final int color) {
            m_borderColor = color;
            return (B)this;
        }

        /** Shift annotation after copy&amp;paste.
         * @param xOff x offset
         * @param yOff y offset
         * @return this
         */
        public final B shiftPosition(final int xOff, final int yOff) {
            setX(m_x + xOff);
            setY(m_y + yOff);
            return (B)this;
        }

        /**
         * Set dimensionality.
         *
         * @param x x-coordinate
         * @param y y-coordinate
         * @param width width of component
         * @param height height of component
         * @return this
         */
        public final B setDimension(final int x, final int y, final int width,
                final int height) {
            setX(x);
            setY(y);
            setWidth(width);
            setHeight(height);
            return (B)this;
        }

        /**
         * Set the default font size for this annotation.
         * @param size new default size
         * @return this
         * @since 3.1
         */
        public final B setDefaultFontSize(final int size) {
            m_defaultFontSize = size;
            return (B)this;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

        /** @return {@link AnnotationData} with current values. */
        public D build() {
            return (D)new AnnotationData(this);
        }

    }

    /** Formatting rule on the text; similar to SWT style range. */
    public static final class StyleRange {

        private final int m_start;

        private final int m_length;

        private final String m_fontName;

        private final int m_fontStyle;

        private final int m_fontSize;

        private final int m_fgColor;

        private StyleRange(final Builder builder) {
            m_start = builder.m_start;
            m_length = builder.m_length;
            m_fontName = builder.m_fontName;
            m_fontStyle = builder.m_fontStyle;
            m_fontSize = builder.m_fontSize;
            m_fgColor = builder.m_fgColor;
        }

        /** @return the start */
        public int getStart() {
            return m_start;
        }

        /** @return the length */
        public int getLength() {
            return m_length;
        }

        /** @return the fontName */
        public String getFontName() {
            return m_fontName;
        }

        /** @return the fontStyle */
        public int getFontStyle() {
            return m_fontStyle;
        }

        /** @return the fontSize */
        public int getFontSize() {
            return m_fontSize;
        }

        /** @return the fgColor */
        public int getFgColor() {
            return m_fgColor;
        }

        /** {@inheritDoc} */
        @Override
        protected StyleRange clone() {
            //we should not provide a clone method
            //e.g. conflicts with the final-fields
            //see also https://stackoverflow.com/questions/2427883/clone-vs-copy-constructor-which-is-recommended-in-java
            throw new UnsupportedOperationException();
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
            if (!Objects.equals(m_fontName, other.m_fontName) ) {
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

        /** @return new Builder with defaults. */
        public static final Builder builder() {
            return new Builder();
        }

        /**
         * @param styleRange
         * @return new Builder with values copied from the passed {@link StyleRange} object
         */
        public static final Builder builder(final StyleRange styleRange) {
            return new Builder().copyFrom(styleRange);
        }

        /** Builder pattern for {@link StyleRange}. */
        public static final class Builder {

            private int m_start;

            private int m_length;

            private String m_fontName;

            private int m_fontStyle;

            private int m_fontSize;

            private int m_fgColor;

            public Builder() {
                //
            }

            /**
             * Copies all fields from another {@link StyleRange} object.
             *
             * @param styleRange To copy from.
             * @return this
             */
            public Builder copyFrom(final StyleRange styleRange) {
                m_start = styleRange.m_start;
                m_length = styleRange.m_length;
                m_fontName = styleRange.m_fontName;
                m_fontStyle = styleRange.m_fontStyle;
                m_fontSize = styleRange.m_fontSize;
                m_fgColor = styleRange.m_fgColor;
                return this;
            }


            /** @param start the start to set
             * @return this*/
            public Builder setStart(final int start) {
                m_start = start;
                return this;
            }

            /** @param length the length to set
             * @return this*/
            public Builder setLength(final int length) {
                m_length = length;
                return this;
            }

            /** @param fontName the fontName to set
             * @return this*/
            public Builder setFontName(final String fontName) {
                m_fontName = fontName;
                return this;
            }

            /** @param fontStyle the fontStyle to set
             * @return this*/
            public Builder setFontStyle(final int fontStyle) {
                m_fontStyle = fontStyle;
                return this;
            }

            /** @param fontSize the fontSize to set
             * @return this*/
            public Builder setFontSize(final int fontSize) {
                m_fontSize = fontSize;
                return this;
            }

            /** @param fgColor the fgColor to set
             * @return this*/
            public Builder setFgColor(final int fgColor) {
                m_fgColor = fgColor;
                return this;
            }

            /** @return {@link StyleRange} with current values. */
            public StyleRange build() {
                return new StyleRange(this);
            }

        }

    }

}
