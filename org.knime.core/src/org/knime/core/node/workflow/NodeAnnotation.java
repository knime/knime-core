/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: Oct 20, 2011
 * Author: Peter Ohl
 */
package org.knime.core.node.workflow;



/**
 * Annotation associated with a node. Moves with the node. Can't be moved
 * separately.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class NodeAnnotation extends Annotation
    implements NodeUIInformationListener {

    private NodeContainer m_nodeContainer;

    /** Create default annotation, to be activated later. */
    public NodeAnnotation() {
        this(new NodeAnnotationData(true));
    }

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

    public NodeContainer getNodeContainer() {
        return m_nodeContainer;
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
        m_nodeContainer.setDirty();
        super.fireChangeEvent();
    }

}
