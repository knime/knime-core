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
 */
package org.knime.core.node.workflow;

import org.knime.core.def.node.workflow.INodeAnnotation;
import org.knime.core.def.node.workflow.NodeAnnotationData;
import org.knime.core.def.node.workflow.NodeUIInformationEvent;
import org.knime.core.def.node.workflow.NodeUIInformationListener;

/**
 * Annotation associated with a node. Moves with the node. Can't be moved
 * separately.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public final class NodeAnnotation extends Annotation<NodeAnnotationData> implements INodeAnnotation, NodeUIInformationListener {

    private NodeContainer m_nodeContainer;

    /**
     * @param data */
    public NodeAnnotation(final NodeAnnotationData data) {
        super(data);
    }

    void registerOnNodeContainer(final NodeContainer node) {
        assert m_nodeContainer == null;
        if (node == null) {
            throw new NullPointerException("Can't hook annotation to null");
        }
        m_nodeContainer = node;
        m_nodeContainer.addUIInformationListener(this);
    }

    void unregisterFromNodeContainer() {
        assert m_nodeContainer != null;
        m_nodeContainer.removeUIInformationListener(this);
        m_nodeContainer = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainer getNodeContainer() {
        return m_nodeContainer;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setDimensionNoNotify(final int x, final int y, final int width,
            final int height) {
        super.setDimensionNoNotify(x, y, width, height);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected NodeAnnotationData.Builder createAnnotationDataBuilder(final NodeAnnotationData annoData,
        final boolean includeBounds) {
        return NodeAnnotationData.builder(annoData, includeBounds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        // don't set dirty - event was fired by corresponding node
        super.fireChangeEvent();
    }

    /** {@inheritDoc} */
    @Override
    public void fireChangeEvent() {
        m_nodeContainer.setDirty();
        super.fireChangeEvent();
    }

}
