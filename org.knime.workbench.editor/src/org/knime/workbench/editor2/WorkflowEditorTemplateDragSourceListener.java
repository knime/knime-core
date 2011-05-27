/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2011
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
  *   May 20, 2011 (morent): created
  */

package org.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.dnd.AbstractTransferDragSourceListener;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.core.WorkflowManagerTransfer;
import org.knime.workbench.editor2.editparts.SubworkflowEditPart;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class WorkflowEditorTemplateDragSourceListener extends
        AbstractTransferDragSourceListener {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowEditorTemplateDragSourceListener.class);

    /**
     * Constructs an WorkflowEditorTemplateDragSourceListener with the specified
     * EditPartViewer.
     *
     * @param viewer the edit part viewer
     */
    public WorkflowEditorTemplateDragSourceListener(
            final WorkflowEditorViewer viewer) {
        super(viewer, WorkflowManagerTransfer.getTransfer());
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void dragSetData(final DragSourceEvent event) {
        /* In the dragStart method is already verified that we have a list
         * containing only SubworkflowEditPart items. */
        List<WorkflowManager> wfm = new ArrayList<WorkflowManager>();
        for (Object item : getViewer().getSelectedEditParts()) {
            wfm.add(((SubworkflowEditPart)item).getNodeContainer());
        }
        event.data = wfm;
        LOGGER.debug("dragSetData with event: " + event
                + "(datatype = " + event.dataType
                + "; data = " + event.data
                + ")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragStart(final DragSourceEvent event) {
        LOGGER.debug("dragStart with event: " + event);
        /* Cancel the drag and move the node using the tracker if SHIFT key is
         * not pressed. */
        if (!getViewer().isShiftPressed()) {
            event.doit = false;
            return;
        }
        @SuppressWarnings("rawtypes")
        List selectedEditParts = getViewer().getSelectedEditParts();
        StringBuffer sb = new StringBuffer("Dragging meta nodes: ");
        /* Check if every selected item is a meta node. If not the drag is
         * canceled and the items are moved around by the responsible tracker
         * within the editor.*/
        for (Object selection : selectedEditParts) {
            if (selection instanceof SubworkflowEditPart) {
                SubworkflowEditPart meta = (SubworkflowEditPart)selection;
                sb.append(meta);
                sb.append(", ");
            } else {
                LOGGER.debug("Found " + selection + " which cannot be dragged. "
                        + "Only meta nodes are allowed for dragging.");
                event.doit = false;
                return;
            }
        }
        LOGGER.debug(sb.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragFinished(final DragSourceEvent event) {
        LOGGER.debug("dragFinished with event: " + event);
        /* This should already be the case but does not hurt either.
         * Nothing else to do here as the data is always "copied".*/
        getViewer().setShiftPressed(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WorkflowEditorViewer getViewer() {
        return (WorkflowEditorViewer)super.getViewer();
    }

}
