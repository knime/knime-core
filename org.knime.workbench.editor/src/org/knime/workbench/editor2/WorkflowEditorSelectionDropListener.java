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
  *   May 4, 2011 (morent): created
  */

package org.knime.workbench.editor2;

import java.net.MalformedURLException;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.ContentObject;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class WorkflowEditorSelectionDropListener extends
        WorkflowEditorDropTargetListener {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowEditorSelectionDropListener.class);

    /**
     * @param viewer the edit part viewer this drop target listener is attached
     *      to
     */
    protected WorkflowEditorSelectionDropListener(final EditPartViewer viewer) {
        super(viewer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transfer getTransfer() {
        return LocalSelectionTransfer.getTransfer();
    }

    /** {@inheritDoc} */
    @Override
    protected void handleDrop() {
        ContentObject obj = getDragResources(getCurrentEvent());
        ExplorerFileStore store =  obj.getObject();
        Point dropLocation = getDropLocation();
        try {
            getFactory().setSourceFileStore(store);
            super.handleDrop();
            dropNode(store.toURI().toURL(), dropLocation);
        } catch (MalformedURLException e) {
            LOGGER.error(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        if (!super.isEnabled(event)) {
            return false;
        }
        ExplorerFileStore fileStore = getDragResources(event).getObject();
        boolean enabled = !(ExplorerFileStore.isMetaNode(fileStore)
                || ExplorerFileStore.isWorkflow(fileStore)
                || ExplorerFileStore.isWorkflowGroup(fileStore));
        if (enabled) {
            event.feedback = DND.FEEDBACK_SELECT;
            event.operations = DND.DROP_COPY;
            event.detail = DND.DROP_COPY;
        }
        return enabled;
    }
}
