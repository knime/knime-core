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
 *   20.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.editparts.ZoomManager;
import org.knime.core.node.workflow.ModellingNodeExtraInfo;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.workbench.editor2.ClipboardWorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Implements the clipboard paste action to paste nodes and connections from the
 * clipboard into the editor. This sub class is used for context invoked pastes
 * only and pastes the nodes to the location of the current cursor.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class PasteActionContextMenu extends PasteAction {
//     private static final NodeLogger LOGGER =
//         NodeLogger.getLogger(PasteActionContextMenu.class);

    /** ID for this action. */
    public static final String ID = "PasteActionContext";

    /**
     * Constructs a new clipboard paste action.
     * 
     * @param editor the workflow editor this action is intended for
     */
    public PasteActionContextMenu(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        super.runOnNodes(nodeParts);
        ClipboardWorkflowManager.resetRetrievalCounter();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected int[] calculateShift(final NodeID[] ids) {
        int x = getEditor().getSelectionTool().getXLocation();
        int y = getEditor().getSelectionTool().getYLocation();
        int smallestX = Integer.MAX_VALUE;
        int smallestY = Integer.MAX_VALUE;
        for (int i = 0; i < ids.length; i++) {
            NodeContainer nc = getManager().getNodeContainer(ids[i]);
            // finaly change the extra info so that the copies are
            // located differently (if not null)
            ModellingNodeExtraInfo extraInfo =
                    (ModellingNodeExtraInfo)nc.getUIInformation();
            int currentX = extraInfo.getBounds()[0];
            int currentY = extraInfo.getBounds()[1];
            if (currentX < smallestX) {
                smallestX = currentX;
            }
            if (currentY < smallestY) {
                smallestY = currentY;
            }
        }
        ZoomManager zoomManager =
                (ZoomManager)getEditor().getViewer().getProperty(
                        ZoomManager.class.toString());

        Point viewPortLocation = zoomManager.getViewport().getViewLocation();
        x += viewPortLocation.x;
        y += viewPortLocation.y;
        double zoom = zoomManager.getZoom();
        x /= zoom;
        y /= zoom;

        int shiftx = x - smallestX;
        int shifty = y - smallestY;

        return new int[]{shiftx, shifty};
    }
}
