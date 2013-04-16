/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   Oct 20, 2011 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.util.Arrays;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;

/**
 * @author  Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class AnnotationData implements Cloneable {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(AnnotationData.class);

    /** Text alignment in annotation. */
    public enum TextAlignment {
        /** Left alignment. */
        LEFT,
        /** Center alignment. */
        CENTER,
        /** Right alignment. */
        RIGHT;
    }

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

    /** @return the text */
    public String getText() {
        return m_text;
    }

    /** @param text the text to set */
    public void setText(final String text) {
        m_text = text;
    }

    /** @return the styleRanges */
    public StyleRange[] getStyleRanges() {
        return m_styleRanges;
    }

    /** @param styleRanges the styleRanges to set */
    public void setStyleRanges(final StyleRange[] styleRanges) {
        if (styleRanges == null || Arrays.asList(styleRanges).contains(null)) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_styleRanges = styleRanges;
    }

    /** @return the bgColor */
    public int getBgColor() {
        return m_bgColor;
    }

    /** @param bgColor the bgColor to set */
    public void setBgColor(final int bgColor) {
        m_bgColor = bgColor;
    }

    /** @return the x */
    public int getX() {
        return m_x;
    }

    /** @param x the x to set */
    public void setX(final int x) {
        m_x = x;
    }

    /** @return the y */
    public int getY() {
        return m_y;
    }

    /** @param y the y to set */
    public void setY(final int y) {
        m_y = y;
    }

    /** @return the width */
    public int getWidth() {
        return m_width;
    }

    /** @param width the width to set */
    public void setWidth(final int width) {
        m_width = width;
    }

    /** @return the height */
    public int getHeight() {
        return m_height;
    }

    /** @param height the height to set */
    public void setHeight(final int height) {
        m_height = height;
    }

    /** @param alignment the alignment to set */
    public void setAlignment(final TextAlignment alignment) {
        m_alignment = alignment == null ? TextAlignment.LEFT : alignment;
    }

    /** @return the alignment */
    public TextAlignment getAlignment() {
        return m_alignment;
    }

    /** Shift annotation after copy&paste.
     * @param xOff x offset
     * @param yOff y offset
     */
    public void shiftPosition(final int xOff, final int yOff) {
        setX(getX() + xOff);
        setY(getY() + yOff);
    }

    /**
     * Set dimensionality.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param width width of component
     * @param height height of component
     */
    public void setDimension(final int x, final int y, final int width,
            final int height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
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
        if (!Arrays.equals(m_styleRanges, other.m_styleRanges)) {
            return false;
        }
        if (!ConvenienceMethods.areEqual(m_text, other.m_text)) {
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
    public void copyFrom(final AnnotationData otherData,
            final boolean includeBounds) {
        if (includeBounds) {
            setDimension(otherData.getX(), otherData.getY(),
                    otherData.getWidth(), otherData.getHeight());
        }
        setText(otherData.getText());
        setBgColor(otherData.getBgColor());
        setAlignment(otherData.getAlignment());
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
    /** Save all data.
     * @param config To save to.
     */
    public void save(final NodeSettingsWO config) {
        config.addString("text", getText());
        config.addInt("bgcolor", getBgColor());
        config.addInt("x-coordinate", getX());
        config.addInt("y-coordinate", getY());
        config.addInt("width", getWidth());
        config.addInt("height", getHeight());
        config.addString("alignment", getAlignment().toString());
        NodeSettingsWO styleConfigs = config.addNodeSettings("styles");
        int i = 0;
        for (StyleRange sr : getStyleRanges()) {
            NodeSettingsWO cur = styleConfigs.addNodeSettings("style_" + (i++));
            sr.save(cur);
        }
    }

    /** loads new values.
     * @param config To load from
     * @param loadVersion Version to load
     * @throws InvalidSettingsException If fails*/
    public void load(final NodeSettingsRO config, final LoadVersion loadVersion)
            throws InvalidSettingsException {
        setText(config.getString("text"));
        setBgColor(config.getInt("bgcolor"));
        int x = config.getInt("x-coordinate");
        int y = config.getInt("y-coordinate");
        int width = config.getInt("width");
        int height = config.getInt("height");
        TextAlignment alignment = TextAlignment.LEFT;
        if (loadVersion.ordinal() >= LoadVersion.V250.ordinal()) {
            String alignmentS = config.getString("alignment");
            try {
                alignment = TextAlignment.valueOf(alignmentS);
            } catch (Exception e) {
                throw new InvalidSettingsException(
                        "Invalid alignment: " + alignmentS, e);
            }
        }
        setDimension(x, y, width, height);
        setAlignment(alignment);
        NodeSettingsRO styleConfigs = config.getNodeSettings("styles");
        StyleRange[] styles = new StyleRange[styleConfigs.getChildCount()];
        int i = 0;
        for (String key : styleConfigs.keySet()) {
            NodeSettingsRO cur = styleConfigs.getNodeSettings(key);
            styles[i++] = StyleRange.load(cur);
        }
        setStyleRanges(styles);
    }

    /** Formatting rule on the text; similar to SWT style range. */
    public static final class StyleRange implements Cloneable {

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
