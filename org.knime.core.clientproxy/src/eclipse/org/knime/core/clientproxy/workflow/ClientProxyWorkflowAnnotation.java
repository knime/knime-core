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
 *   Nov 9, 2016 (hornm): created
 */
package org.knime.core.clientproxy.workflow;

import java.util.Optional;

import org.knime.core.api.node.workflow.AnnotationData;
import org.knime.core.api.node.workflow.AnnotationData.StyleRange;
import org.knime.core.api.node.workflow.AnnotationData.TextAlignment;
import org.knime.core.api.node.workflow.IWorkflowAnnotation;
import org.knime.core.api.node.workflow.NodeUIInformationListener;
import org.knime.core.api.node.workflow.WorkflowAnnotationID;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowAnnotationEnt;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class ClientProxyWorkflowAnnotation implements IWorkflowAnnotation {

    private WorkflowAnnotationEnt m_anno;

    /**
     *
     */
    public ClientProxyWorkflowAnnotation(final WorkflowAnnotationEnt anno) {
        m_anno = anno;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotationData getData() {
        BoundsEnt bounds = m_anno.getBounds();
        return AnnotationData.builder().setAlignment(TextAlignment.valueOf(m_anno.getAlignment()))
            .setBgColor(m_anno.getBgColor()).setBorderColor(m_anno.getBorderColor())
            .setBorderSize(m_anno.getBorderSize()).setDefaultFontSize(m_anno.getFontSize())
            .setDimension(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()).setText(m_anno.getText())
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setData(final AnnotationData data) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return m_anno.getText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StyleRange[] getStyleRanges() {
        return m_anno.getStyleRanges().stream().map(sr -> {
                    return StyleRange.builder()
                            .setStart(sr.getStart())
                            .setLength(sr.getLength())
                            .setFontName(sr.getFontName())
                            .setFontSize(sr.getFontSize())
                            .setFgColor(sr.getForegroundColor())
                            .setFontStyle(getFontStyleIdx(sr.getFontStyle()))
                            .build();
                }).toArray(StyleRange[]::new);
    }

    private int getFontStyleIdx(final String fontStyle) {
        //indices from SWT-class
        if (fontStyle.equals("normal")) {
            return 0;
        } else if (fontStyle.equals("bold")) {
            return 1;
        } else if (fontStyle.equals("italic")) {
            return 2;
        } else {
            //return normal style by default
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBgColor() {
        return m_anno.getBgColor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getX() {
        return m_anno.getBounds().getX();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getY() {
        return m_anno.getBounds().getY();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return m_anno.getBounds().getWidth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        return m_anno.getBounds().getHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TextAlignment getAlignment() {
        return TextAlignment.valueOf(m_anno.getAlignment());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBorderSize() {
        return m_anno.getBorderSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBorderColor() {
        return m_anno.getBorderColor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDefaultFontSize() {
        return m_anno.getFontSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shiftPosition(final int xOff, final int yOff) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDimension(final int x, final int y, final int width, final int height) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDimensionNoNotify(final int x, final int y, final int width, final int height) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(final AnnotationData annotationData, final boolean includeBounds) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addUIInformationListener(final NodeUIInformationListener l) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeUIInformationListener(final NodeUIInformationListener l) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireChangeEvent() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<WorkflowAnnotationID> getID() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setID(final WorkflowAnnotationID wfaID) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWorkflowAnnotation clone() {
        // TODO Auto-generated method stub
        return null;
    }

}
