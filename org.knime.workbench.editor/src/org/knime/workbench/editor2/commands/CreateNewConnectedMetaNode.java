/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: Mar 31, 2011
 * Author: ohl
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author ohl, University of Konstanz
 */
public class CreateNewConnectedMetaNode extends CreateNewConnectedNode {

    private final WorkflowManager m_destination;

    private final WorkflowManager m_source;

    private final NodeID m_sourceID;

    private final Point m_location;

    /**
     * @param viewer
     * @param manager
     * @param factory
     * @param location absolute coordinates of the new node
     * @param connectTo
     */
    public CreateNewConnectedMetaNode(final EditPartViewer viewer,
            final WorkflowManager destination, final WorkflowManager source,
            final NodeID sourceID, final Point location, final NodeID connectTo) {
        super(viewer, destination, null, location, connectTo);
        m_destination = destination;
        m_source = source;
        m_sourceID = sourceID;
        m_location = location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return m_destination != null && m_source != null && m_sourceID != null
                && m_location != null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeID createNewNode() {
        WorkflowCopyContent content = new WorkflowCopyContent();
        content.setNodeIDs(m_sourceID);
        NodeID[] copied =
                m_destination.copyFromAndPasteHere(m_source, content)
                        .getNodeIDs();
        assert copied.length == 1;
        // create UI info
        NodeContainer newNode = m_destination.getNodeContainer(copied[0]);
        NodeUIInformation uiInfo =
                (NodeUIInformation)newNode.getUIInformation();
        // create extra info and set it
        if (uiInfo == null) {
            uiInfo =
                    new NodeUIInformation(m_location.x, m_location.y, -1, -1,
                            true);
        } else {
            uiInfo.setNodeLocation(m_location.x, m_location.y,
                    uiInfo.getBounds()[2], uiInfo.getBounds()[3]);
        }
        newNode.setUIInformation(uiInfo);
        return copied[0];

    }

}
