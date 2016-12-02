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
 *   Dec 2, 2016 (hornm): created
 */
package org.knime.core.clientproxy.workflow;

import org.knime.core.api.node.workflow.AnnotationData.StyleRange;
import org.knime.core.api.node.workflow.AnnotationData.TextAlignment;
import org.knime.core.api.node.workflow.IAnnotation;
import org.knime.core.api.node.workflow.INodeAnnotation;
import org.knime.core.api.node.workflow.INodeContainer;
import org.knime.core.api.node.workflow.NodeAnnotationData;
import org.knime.core.api.node.workflow.NodeUIInformationListener;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class NodeAnnotation implements INodeAnnotation {


    private NodeAnnotationEnt m_nodeAnnotation;
    private NodeContainer m_nc;

    /**
     * @param nodeAnnotation
     *
     */
    public NodeAnnotation(final NodeAnnotationEnt nodeAnnotation, final NodeContainer nc) {
        m_nodeAnnotation = nodeAnnotation;
        m_nc = nc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeAnnotationData getData() {
        return NodeAnnotationData.builder()
        .setText(m_nodeAnnotation.getText())
        .setX(m_nodeAnnotation.getX())
        .setY(m_nodeAnnotation.getY())
        .setBgColor(m_nodeAnnotation.getBackgroundColor())
        .setBorderColor(m_nodeAnnotation.getBorderColor())
        .setBorderSize(m_nodeAnnotation.getBorderSize())
        .setDefaultFontSize(m_nodeAnnotation.getDefaultFontSize())
        .setHeight(m_nodeAnnotation.getHeight())
        .setWidth(m_nodeAnnotation.getWidth())
        .setAlignment(TextAlignment.valueOf(m_nodeAnnotation.getTextAlignment())).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setData(final NodeAnnotationData data) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return m_nodeAnnotation.getText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StyleRange[] getStyleRanges() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBgColor() {
        return m_nodeAnnotation.getBackgroundColor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getX() {
        return m_nodeAnnotation.getX();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getY() {
        return m_nodeAnnotation.getY();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return m_nodeAnnotation.getWidth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        return m_nodeAnnotation.getHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TextAlignment getAlignment() {
        return TextAlignment.valueOf(m_nodeAnnotation.getTextAlignment());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBorderSize() {
        return m_nodeAnnotation.getBorderSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBorderColor() {
        return m_nodeAnnotation.getBorderColor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDefaultFontSize() {
        return m_nodeAnnotation.getDefaultFontSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVersion() {
        return m_nodeAnnotation.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shiftPosition(final int xOff, final int yOff) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDimension(final int x, final int y, final int width, final int height) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDimensionNoNotify(final int x, final int y, final int width, final int height) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAnnotation<NodeAnnotationData> clone() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFrom(final NodeAnnotationData annotationData, final boolean includeBounds) {
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
    public INodeContainer getNodeContainer() {
        return m_nc;
    }

}
