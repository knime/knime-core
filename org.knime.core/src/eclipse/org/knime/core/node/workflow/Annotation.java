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

import org.knime.core.api.node.workflow.AnnotationData;
import org.knime.core.api.node.workflow.AnnotationData.Builder;
import org.knime.core.api.node.workflow.AnnotationData.StyleRange;
import org.knime.core.api.node.workflow.AnnotationData.TextAlignment;
import org.knime.core.api.node.workflow.IAnnotation;
import org.knime.core.api.node.workflow.NodeUIInformationEvent;
import org.knime.core.api.node.workflow.NodeUIInformationListener;
import org.knime.core.node.util.CheckUtils;

/**
 * An annotation on the workflow. It keeps all relevant information, such as
 * text, bg color and individual formatting. The fields in this class follow the
 * content of an SWT StyledText widget.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class Annotation<D extends AnnotationData> implements IAnnotation<D> {

    private D m_data;

    private CopyOnWriteArraySet<NodeUIInformationListener> m_uiListeners =
            new CopyOnWriteArraySet<NodeUIInformationListener>();

    /** Create new annotation with arg data (not null).
     * @param data The data
     */
    Annotation(final D data) {
        m_data = CheckUtils.checkArgumentNotNull(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public D getData() {
        return m_data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setData(final D data) {
        m_data = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getText() {
        return m_data.getText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final StyleRange[] getStyleRanges() {
        return m_data.getStyleRanges();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getBgColor() {
        return m_data.getBgColor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getX() {
        return m_data.getX();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getY() {
        return m_data.getY();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getWidth() {
        return m_data.getWidth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getHeight() {
        return m_data.getHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final TextAlignment getAlignment() {
        return m_data.getAlignment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getBorderSize() {
        return m_data.getBorderSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getBorderColor()  {
        return m_data.getBorderColor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getDefaultFontSize() {
        return m_data.getDefaultFontSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getVersion() {
        return m_data.getVersion();
    }

    /**
     * {@inheritDoc}
     *
     * Note: with every call a new {@link AnnotationData} object is copied and created.
     */
    @Override
    public final void shiftPosition(final int xOff, final int yOff) {
        m_data = createAnnotationDataBuilder(m_data, true).shiftPosition(xOff, yOff).build();
        fireChangeEvent();
    }

    /**
     * {@inheritDoc}
     *
     * Note: with every call a new {@link AnnotationData} object is copied and created.
     */
    @Override
    public final void setDimension(final int x, final int y, final int width,
            final int height) {
        m_data = createAnnotationDataBuilder(m_data, true).setDimension(x, y, width, height).build();
        fireChangeEvent();
    }

    /**
     * Set dimensions, but don't notify any listener. (Used only by the GUI for
     * node annotations for legacy support. Sets dimensions in old workflows
     * without making the flow dirty.)
     *
     * Note: with every call a new {@link AnnotationData} object is copied and created.
     *
     * @param x coordinate
     * @param y coordinate
     * @param width of the annotation
     * @param height of the component
     */
    protected void setDimensionNoNotify(final int x, final int y, final int width,
            final int height) {
        m_data = createAnnotationDataBuilder(m_data, true).setDimension(x, y, width, height).build();
    }


    /**
     * Creates the annotation data builder initialized with the values of the passed annotation data object.
     *
     * @param annoData
     * @param includeBounds
     * @return the new builder
     */
    protected abstract <B extends Builder<B, D>> B createAnnotationDataBuilder(D annoData, boolean includeBounds);

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_data.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(final D annotationData,
            final boolean includeBounds) {
        m_data = createAnnotationDataBuilder(annotationData, includeBounds).build();
        fireChangeEvent();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Annotation clone() {
        try {
            Annotation result = (Annotation)super.clone();
            AnnotationData clonedData = AnnotationData.builder(m_data, true).build();
            result.m_data = clonedData;
            result.m_uiListeners =
                new CopyOnWriteArraySet<NodeUIInformationListener>();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addUIInformationListener(final NodeUIInformationListener l) {
        if (l == null) {
            throw new NullPointerException(
                    "NodeUIInformationListener must not be null!");
        }
        m_uiListeners.add(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void removeUIInformationListener(final NodeUIInformationListener l) {
        m_uiListeners.remove(l);
    }

    @Override
    public void fireChangeEvent() {
        for (NodeUIInformationListener l : m_uiListeners) {
            l.nodeUIInformationChanged(new NodeUIInformationEvent(
                    new NodeID(0), null, null));
        }
    }

}

