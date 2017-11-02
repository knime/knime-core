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

/**
 * Annotation associated with a node. Moves with the node. Can't be moved
 * separately.
 *
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 */
public final class NodeAnnotation extends Annotation implements NodeUIInformationListener {

    private NodeID m_nodeID = null;
    private Runnable m_changeListener;

    /**
     * @param data */
    public NodeAnnotation(final NodeAnnotationData data) {
        super(data);
    }

    /**
     *
     * @param nodeID the node id this node annotation is registered for
     * @param changeListener called when something has changed, .e.g. in order to set the workflow dirty in consequence
     * @since 3.5
     */
    public void registerOnNodeContainer(final NodeID nodeID, final Runnable changeListener) {
        m_changeListener = changeListener;
        assert m_nodeID == null;
        if (nodeID == null) {
            throw new NullPointerException("Can't hook annotation to null");
        }
        m_nodeID = nodeID;
    }

    /**
     * @return the id of the node associated with this annotation
     * @since 3.5
     */
    public NodeID getNodeID() {
        return m_nodeID;
    }

    /** {@inheritDoc} */
    @Override
    public NodeAnnotationData getData() {
        return (NodeAnnotationData)super.getData();
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
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        // don't set dirty - event was fired by corresponding node
        super.fireChangeEvent();
    }

    /** {@inheritDoc} */
    @Override
    protected void fireChangeEvent() {
        m_changeListener.run();
        super.fireChangeEvent();
    }

}
