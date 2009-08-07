/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   02.03.2006 (Christoph Sieb): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.ZoomManager;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * GEF Command for changing the location of a <code>ConnectionContainer</code>
 * in the workflow. The bounds are stored into the <code>UIInformation</code>
 * object of the <code>ConnectionContainer</code>
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ChangeBendPointLocationCommand extends Command {
    
    private final Point m_locationShift;
    
    /* We keep destination node and port instead of the ConnectionContainer
     * as field. This allows redo/undo to be performed even if the connection 
     * was (temporarily) removed. 
     */
    /** ID of the connection's destination node. */
    private final NodeID m_destNodeID;
    /** Port the connection leads to. */
    private final int m_destPort;
    /** The associated workflow manager. */
    private final WorkflowManager m_manager;
    private final ZoomManager m_zoomManager;

    /**
     * @param container The connection container to change
     * @param locationShift the values (x,y) to change the location of all
     *            bendpoints
     * @param zoomManager The zoom manager
     */
    public ChangeBendPointLocationCommand(
            final ConnectionContainerEditPart container,
            final Point locationShift, final ZoomManager zoomManager) {
        m_zoomManager = zoomManager;
        m_locationShift = locationShift;
        if (container != null) {
            m_destNodeID = container.getModel().getDest();
            m_destPort = container.getModel().getDestPort();
            m_manager = container.getWorkflowManager();
        } else {
            m_destNodeID = null;
            m_destPort = -1;
            m_manager = null;
        }
    }

    private ConnectionContainer getConnectionContainer() {
        if (m_destNodeID == null || m_manager == null) {
            return null;
        }
        return m_manager.getIncomingConnectionFor(m_destNodeID, m_destPort); 
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        return getConnectionContainer() != null;
    }
    
    /**
     * Shift all bendpoints in positive shift direction.
     * 
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        NodeLogger.getLogger(ChangeBendPointLocationCommand.class).debug(
                " execute change bendpoint location command...");
        changeBendpointsUIInfo(false);
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return canExecute();
    }
    
    /**
     * Shift all bendpoints in negative shift direction.
     * 
     * @see org.eclipse.gef.commands.Command#undo()
     */
    @Override
    public void undo() {
        changeBendpointsUIInfo(true);
    }

    private void changeBendpointsUIInfo(final boolean shiftBack) {
        
        ConnectionContainer cc = getConnectionContainer();
        ConnectionUIInformation ui = (ConnectionUIInformation)cc.getUIInfo();
        if (ui == null) {
            return;
        }

        int[][] bendpoints = ui.getAllBendpoints();

        Point locationShift = m_locationShift.getCopy();

        WorkflowEditor.adaptZoom(m_zoomManager, locationShift, false);

        int length = bendpoints.length;
        int shiftX = shiftBack ? locationShift.x * -1 : locationShift.x;
        int shiftY = shiftBack ? locationShift.y * -1 : locationShift.y;
        
        ConnectionUIInformation newUI = new ConnectionUIInformation();
        for (int i = 0; i < length; i++) {

            // get old
            int x = ui.getBendpoint(i)[0];
            int y = ui.getBendpoint(i)[1];

            // set the new point
            newUI.addBendpoint(x + shiftX, y + shiftY, i);
        }

        // must set explicitly so that event is fired by container
        cc.setUIInfo(newUI);
    }
}
