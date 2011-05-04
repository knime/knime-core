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
import java.util.Iterator;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(final DropTargetEvent event) {
        IStructuredSelection selection = (IStructuredSelection)
                LocalSelectionTransfer.getTransfer().getSelection();
        @SuppressWarnings("rawtypes")
        Iterator iterator = selection.iterator();
        ExplorerFileStore file = null;
        // get the one and only selected file
        if (iterator.hasNext()) {
            Object next = iterator.next();
            if (!(next instanceof ContentObject)) {
                return;
            }
            file = ((ContentObject)next).getObject();
        } else {
            return;
        }
        if (iterator.hasNext()) {
            LOGGER.warn("Only a single file can be dropped. Multiple "
                    + "selections are not allowed.");
            return; // on multiple selections nothing is dropped.
        }
        Point dropLocation = getDropLocation(event);
        try {
            dropNode(file.toURI().toURL(), dropLocation);
        } catch (MalformedURLException e) {
            LOGGER.error(e);
        }

    }
}
