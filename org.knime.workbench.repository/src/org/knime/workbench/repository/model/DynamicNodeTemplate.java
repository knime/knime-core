/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2012
  * KNIME.com, Zurich, Switzerland
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
  * History
  *   Mar 20, 2012 (morent): created
  */

package org.knime.workbench.repository.model;

import org.knime.core.node.NodeSetFactory;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class DynamicNodeTemplate extends NodeTemplate {
    private NodeSetFactory m_nodeSetFactory;

    /**
     * Constructs a new DynamicNodeTemplate.
     *
     * @param nodeID The id of the dynamic node.
     * @param nodeSetFactory the NodeSetFactory that created this
     *      DynamicNodeTemplate
     */
    public DynamicNodeTemplate(final String nodeID,
            final NodeSetFactory nodeSetFactory) {
        super(nodeID);
        m_nodeSetFactory = nodeSetFactory;
    }

    /**
     * @param factory the nodeSetFactory that created this DynamicNodeTemplate
     */
    public void setNodeSetFactory(final NodeSetFactory factory) {
        m_nodeSetFactory = factory;
    }

    /**
     * @return the class of the NodeSetFactory that created this
     *      DynamicNodeTemplate
     */
    public Class <? extends NodeSetFactory> getNodeSetFactoryClass() {
        return m_nodeSetFactory.getClass();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DynamicNodeTemplate)) {
            return false;
        }
        return getID().equals(((DynamicNodeTemplate)obj).getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getID().hashCode();
    }
}
