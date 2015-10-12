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
 */
package org.knime.core.node.workflow;

import java.util.concurrent.CopyOnWriteArraySet;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.AnnotationData.StyleRange;
import org.knime.core.node.workflow.AnnotationData.TextAlignment;

/**
 * An annotation on the workflow. It keeps all relevant information, such as
 * text, bg color and individual formatting. The fields in this class follow the
 * content of an SWT StyledText widget.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class Annotation implements UIInformation {

    private AnnotationData m_data;

    private CopyOnWriteArraySet<NodeUIInformationListener> m_uiListeners =
            new CopyOnWriteArraySet<NodeUIInformationListener>();

    /** Create new annotation with arg data (not null).
     * @param data The data
     */
    Annotation(final AnnotationData data) {
        m_data = CheckUtils.checkArgumentNotNull(data);
    }

    /** @return the data */
    public AnnotationData getData() {
        return m_data;
    }

    /** @return the text */
    public final String getText() {
        return m_data.getText();
    }

    /** @return the styleRanges */
    public final StyleRange[] getStyleRanges() {
        return m_data.getStyleRanges();
    }

    /** @return the bgColor */
    public final int getBgColor() {
        return m_data.getBgColor();
    }

    /** @return the x */
    public final int getX() {
        return m_data.getX();
    }

    /** @return the y */
    public final int getY() {
        return m_data.getY();
    }

    /** @return the width */
    public final int getWidth() {
        return m_data.getWidth();
    }

    /** @return the height */
    public final int getHeight() {
        return m_data.getHeight();
    }

    /** @return the alignment */
    public final TextAlignment getAlignment() {
        return m_data.getAlignment();
    }

    /** @return the border size, 0 or neg. for none.
     * @since 3.0*/
    public final int getBorderSize() {
        return m_data.getBorderSize();
    }

    /** @return the border color.
     * @since 3.0*/
    public final int getBorderColor()  {
        return m_data.getBorderColor();
    }

    /**
     * @return The version to guarantee backward compatible look.
     * @see org.knime.core.node.workflow.AnnotationData#getVersion()
     * @since 3.0
     */
    public final int getVersion() {
        return m_data.getVersion();
    }

    /** Shift annotation after copy&paste.
     * @param xOff x offset
     * @param yOff y offset
     */
    public final void shiftPosition(final int xOff, final int yOff) {
        m_data.shiftPosition(xOff, yOff);
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
    public final void setDimension(final int x, final int y, final int width,
            final int height) {
        m_data.setDimension(x, y, width, height);
        fireChangeEvent();
    }

    /**
     * Set dimensions, but don't notify any listener. (Used only by the GUI for
     * node annotations for legacy support. Sets dimensions in old workflows
     * without making the flow dirty.)
     * @param x coordinate
     * @param y coordinate
     * @param width of the annotation
     * @param height of the component
     */
    protected void setDimensionNoNotify(final int x, final int y, final int width,
            final int height) {
        m_data.setDimension(x, y, width, height);
    }

    /** {@inheritDoc} */
    @Override
    public void save(final NodeSettingsWO config) {
        m_data.save(config);
    }

    /** {@inheritDoc}
    * loads new values and fires change event. */
    @Override
    public void load(final NodeSettingsRO config, final FileWorkflowPersistor.LoadVersion loadVersion)
            throws InvalidSettingsException {
        m_data.load(config, loadVersion);
        fireChangeEvent();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_data.toString();
    }

    /**
     * Copy content, styles, position from the argument and notify listeners.
     *
     * @param annotationData To copy from.
     * @param includeBounds Whether to also update x, y, width, height. If
     * false, it will only a copy the text with its styles
     */
    public void copyFrom(final AnnotationData annotationData,
            final boolean includeBounds) {
        m_data.copyFrom(annotationData, includeBounds);
        fireChangeEvent();
    }

    /** {@inheritDoc} */
    @Override
    public Annotation clone() {
        try {
            Annotation result = (Annotation)super.clone();
            AnnotationData clonedData = m_data.clone();
            result.m_data = clonedData;
            result.m_uiListeners =
                new CopyOnWriteArraySet<NodeUIInformationListener>();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone failed", e);
        }
    }

    public final void addUIInformationListener(final NodeUIInformationListener l) {
        if (l == null) {
            throw new NullPointerException(
                    "NodeUIInformationListener must not be null!");
        }
        m_uiListeners.add(l);
    }

    public final void removeUIInformationListener(final NodeUIInformationListener l) {
        m_uiListeners.remove(l);
    }

    protected void fireChangeEvent() {
        for (NodeUIInformationListener l : m_uiListeners) {
            l.nodeUIInformationChanged(new NodeUIInformationEvent(
                    new NodeID(0), null, null));
        }
    }

}

