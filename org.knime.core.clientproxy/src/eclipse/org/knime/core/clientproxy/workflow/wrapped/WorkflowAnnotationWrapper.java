/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 17, 2016 (hornm): created
 */
package org.knime.core.clientproxy.workflow.wrapped;

import java.util.Optional;

import org.knime.core.api.node.workflow.AnnotationData;
import org.knime.core.api.node.workflow.AnnotationData.StyleRange;
import org.knime.core.api.node.workflow.AnnotationData.TextAlignment;
import org.knime.core.api.node.workflow.IWorkflowAnnotation;
import org.knime.core.api.node.workflow.NodeUIInformationListener;
import org.knime.core.api.node.workflow.WorkflowAnnotationID;
import org.knime.core.util.WrapperMapUtil;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class WorkflowAnnotationWrapper implements IWorkflowAnnotation {

    private final IWorkflowAnnotation m_delegate;

    /**
     *
     */
    private WorkflowAnnotationWrapper(final IWorkflowAnnotation delegate) {
        m_delegate = delegate;
    }

    public static final WorkflowAnnotationWrapper wrap(final IWorkflowAnnotation wa) {
        return WrapperMapUtil.getOrCreate(wa, o -> new WorkflowAnnotationWrapper(o), WorkflowAnnotationWrapper.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_delegate.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        return m_delegate.equals(obj);
    }

    @Override
    public AnnotationData getData() {
        return m_delegate.getData();
    }

    @Override
    public Optional<WorkflowAnnotationID> getID() {
        return m_delegate.getID();
    }

    @Override
    public void setData(final AnnotationData data) {
        m_delegate.setData(data);
    }

    @Override
    public void setID(final WorkflowAnnotationID wfaID) {
        m_delegate.setID(wfaID);
    }

    @Override
    public String getText() {
        return m_delegate.getText();
    }

    @Override
    public IWorkflowAnnotation clone() {
        return m_delegate.clone();
    }

    @Override
    public StyleRange[] getStyleRanges() {
        return m_delegate.getStyleRanges();
    }

    @Override
    public int getBgColor() {
        return m_delegate.getBgColor();
    }

    @Override
    public int getX() {
        return m_delegate.getX();
    }

    @Override
    public int getY() {
        return m_delegate.getY();
    }

    @Override
    public int getWidth() {
        return m_delegate.getWidth();
    }

    @Override
    public int getHeight() {
        return m_delegate.getHeight();
    }

    @Override
    public TextAlignment getAlignment() {
        return m_delegate.getAlignment();
    }

    @Override
    public int getBorderSize() {
        return m_delegate.getBorderSize();
    }

    @Override
    public int getBorderColor() {
        return m_delegate.getBorderColor();
    }

    @Override
    public int getDefaultFontSize() {
        return m_delegate.getDefaultFontSize();
    }

    @Override
    public int getVersion() {
        return m_delegate.getVersion();
    }

    @Override
    public void shiftPosition(final int xOff, final int yOff) {
        m_delegate.shiftPosition(xOff, yOff);
    }

    @Override
    public void setDimension(final int x, final int y, final int width, final int height) {
        m_delegate.setDimension(x, y, width, height);
    }

    @Override
    public void setDimensionNoNotify(final int x, final int y, final int width, final int height) {
        m_delegate.setDimensionNoNotify(x, y, width, height);
    }

    @Override
    public String toString() {
        return m_delegate.toString();
    }

    @Override
    public void copyFrom(final AnnotationData annotationData, final boolean includeBounds) {
        m_delegate.copyFrom(annotationData, includeBounds);
    }

    @Override
    public void addUIInformationListener(final NodeUIInformationListener l) {
        m_delegate.addUIInformationListener(l);
    }

    @Override
    public void removeUIInformationListener(final NodeUIInformationListener l) {
        m_delegate.removeUIInformationListener(l);
    }

    @Override
    public void fireChangeEvent() {
        m_delegate.fireChangeEvent();
    }

}
