/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.extrainfo.ModellingConnectionExtraInfo;

/**
 * GEF Command for changing the location of a <code>ConnectionContainer</code>
 * in the workflow. The bounds are stored into the <code>ExtraInfo</code>
 * object of the <code>ConnectionContainer</code>
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ChangeBendPointLocationCommand extends Command {
    private Point m_locationShift;

    private ConnectionContainerEditPart m_container;

    private ModellingConnectionExtraInfo m_extraInfo;

    private ZoomManager m_zoomManager;

    /**
     * @param container The node container to change
     * @param locationShift the values (x,y) to change the location of all
     *            bendpoints
     */
    public ChangeBendPointLocationCommand(
            final ConnectionContainerEditPart container,
            final Point locationShift, final ZoomManager zoomManager) {
        if (container == null
                || container.getUIInformation() == null
                || !(container.getUIInformation() 
                        instanceof ModellingConnectionExtraInfo)) {
            return;
        }

        m_extraInfo = (ModellingConnectionExtraInfo)container
            .getUIInformation();
        m_locationShift = locationShift;
        m_container = container;

        m_zoomManager = zoomManager;
    }

    /**
     * Shift all bendpoints in positive shift direction.
     * 
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        NodeLogger.getLogger(ChangeBendPointLocationCommand.class).debug(
                " execute chenge bendpoint location command...");
        changeBendpointsExtraInfo(false);
    }

    /**
     * Shift all bendpoints in negative shift direction.
     * 
     * @see org.eclipse.gef.commands.Command#undo()
     */
    @Override
    public void undo() {
        changeBendpointsExtraInfo(true);
    }

    private void changeBendpointsExtraInfo(final boolean shiftBack) {
        if (m_extraInfo == null) {
            return;
        }

        int[][] bendpoints = m_extraInfo.getAllBendpoints();

        Point locationShift = m_locationShift.getCopy();

        WorkflowEditor.adaptZoom(m_zoomManager, locationShift, false);

        int length = bendpoints.length;
        int shiftX = shiftBack ? locationShift.x * -1 : locationShift.x;
        int shiftY = shiftBack ? locationShift.y * -1 : locationShift.y;

        for (int i = 0; i < length; i++) {

            // get old
            int x = m_extraInfo.getBendpoint(i)[0];
            int y = m_extraInfo.getBendpoint(i)[1];

            // remove the old point
            m_extraInfo.removeBendpoint(i);

            // set the new point
            m_extraInfo.addBendpoint(x + shiftX, y + shiftY, i);
        }

        // must set explicitly so that event is fired by container
        m_container.setUIInformation(m_extraInfo);
    }
}
