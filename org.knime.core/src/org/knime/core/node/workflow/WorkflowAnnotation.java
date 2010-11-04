/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 */
package org.knime.core.node.workflow;

import java.util.concurrent.CopyOnWriteArraySet;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;

/**
 * An annotation on the workflow. It keeps all relevant information, such as
 * text, bg color and individual formatting. The fields in this class follow the
 * content of an SWT StyledText widget.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class WorkflowAnnotation implements UIInformation {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowAnnotation.class);

    private String m_text;

    private StyleRange[] m_styleRanges;

    private int m_bgColor;

    private int m_x;

    private int m_y;

    private int m_width;

    private int m_height;

    private CopyOnWriteArraySet<NodeUIInformationListener> m_uiListeners =
            new CopyOnWriteArraySet<NodeUIInformationListener>();

    /**
     * Creates a new annotation.
     */
    public WorkflowAnnotation() {
        // no op
    }

    /** @return the text */
    public String getText() {
        return m_text;
    }

    /** @param text the text to set */
    public void setText(final String text) {
        m_text = text;
        fireChangeEvent();
    }

    /** @return the styleRanges */
    public StyleRange[] getStyleRanges() {
        return m_styleRanges;
    }

    /** @param styleRanges the styleRanges to set */
    public void setStyleRanges(final StyleRange... styleRanges) {
        m_styleRanges = styleRanges;
        fireChangeEvent();
    }

    /** @return the bgColor */
    public int getBgColor() {
        return m_bgColor;
    }

    /** @param bgColor the bgColor to set */
    public void setBgColor(final int bgColor) {
        m_bgColor = bgColor;
        fireChangeEvent();
    }

    /** @return the x */
    public int getX() {
        return m_x;
    }

    /** @return the y */
    public int getY() {
        return m_y;
    }

    /** @return the width */
    public int getWidth() {
        return m_width;
    }

    /** @return the height */
    public int getHeight() {
        return m_height;
    }

    /** Shift annotation after copy&paste.
     * @param xOff x offset
     * @param yOff y offset
     */
    public void shiftPosition(final int xOff, final int yOff) {
        m_x += xOff;
        m_y += yOff;
        fireChangeEvent();
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
        m_x = x;
        m_y = y;
        m_width = width;
        m_height = height;
        fireChangeEvent();
    }

    /** {@inheritDoc} */
    @Override
    public void save(final NodeSettingsWO config) {
        config.addString("text", getText());
        config.addInt("bgcolor", getBgColor());
        config.addInt("x-coordinate", getX());
        config.addInt("y-coordinate", getY());
        config.addInt("width", getWidth());
        config.addInt("height", getHeight());
        NodeSettingsWO styleConfigs = config.addNodeSettings("styles");
        int i = 0;
        for (StyleRange sr : getStyleRanges()) {
            NodeSettingsWO cur = styleConfigs.addNodeSettings("style_" + (i++));
            sr.save(cur);
        }
    }

    /** {@inheritDoc}
    * loads new values and fires change event. */
    @Override
    public void load(final NodeSettingsRO config, final LoadVersion loadVersion)
            throws InvalidSettingsException {
        setText(config.getString("text"));
        setBgColor(config.getInt("bgcolor"));
        int x = config.getInt("x-coordinate");
        int y = config.getInt("y-coordinate");
        int width = config.getInt("width");
        int height = config.getInt("height");
        setDimension(x, y, width, height);
        NodeSettingsRO styleConfigs = config.getNodeSettings("styles");
        StyleRange[] styles = new StyleRange[styleConfigs.getChildCount()];
        int i = 0;
        for (String key : styleConfigs.keySet()) {
            NodeSettingsRO cur = styleConfigs.getNodeSettings(key);
            styles[i++] = StyleRange.load(cur);
        }
        setStyleRanges(styles);
        fireChangeEvent();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (m_text.length() < 60) {
            return m_text;
        } else {
            return m_text.substring(0, 60).concat("...");
        }
    }

    /**
     * Copy content, styles, position from the argument and notify listeners.
     *
     * @param annotation To copy from.
     * @param includeBounds Whether to also update x, y, width, height. If
     * false, it will only a copy the text with its styles
     */
    public void copyFrom(final WorkflowAnnotation annotation,
            final boolean includeBounds) {
        if (includeBounds) {
            m_x = annotation.m_x;
            m_y = annotation.m_y;
            m_width = annotation.m_width;
            m_height = annotation.m_height;
        }
        m_text = annotation.m_text;
        m_bgColor = annotation.m_bgColor;
        m_styleRanges = new StyleRange[annotation.m_styleRanges.length];
        for (int i = 0; i < m_styleRanges.length; i++) {
            m_styleRanges[i] = annotation.m_styleRanges[i].clone();
        }
        fireChangeEvent();
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowAnnotation clone() {
        try {
            WorkflowAnnotation result = (WorkflowAnnotation)super.clone();
            result.m_uiListeners =
                    new CopyOnWriteArraySet<NodeUIInformationListener>();
            return result;
        } catch (CloneNotSupportedException e) {
            LOGGER.coding("Unable to clone annotation: " + e.getMessage(), e);
            WorkflowAnnotation a = new WorkflowAnnotation();
            a.setText("Unable to clone annotation: " + e.getMessage());
            return a;
        }
    }

    public void addUIInformationListener(final NodeUIInformationListener l) {
        if (l == null) {
            throw new NullPointerException(
                    "NodeUIInformationListener must not be null!");
        }
        m_uiListeners.add(l);
    }

    public void removeUIInformationListener(final NodeUIInformationListener l) {
        m_uiListeners.remove(l);
    }

    private void fireChangeEvent() {
        for (NodeUIInformationListener l : m_uiListeners) {
            l.nodeUIInformationChanged(new NodeUIInformationEvent(
                    new NodeID(0), null, "NoName", "NoDescr"));
        }
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

    }

}
