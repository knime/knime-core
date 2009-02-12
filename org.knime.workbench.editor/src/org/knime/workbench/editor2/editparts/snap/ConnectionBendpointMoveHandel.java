/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   14.07.2006 (sieb): created
 */
package org.knime.workbench.editor2.editparts.snap;

import org.eclipse.draw2d.Locator;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.handles.BendpointMoveHandle;
import org.eclipse.gef.tools.ConnectionBendpointTracker;

/**
 * Returns a different ConnectionBendpointTracker to enable snapping of
 * bendpoints.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ConnectionBendpointMoveHandel extends BendpointMoveHandle {
    /**
     * Creates a new BendpointMoveHandle.
     */
    public ConnectionBendpointMoveHandel() {
    }

    /**
     * Creates a new BendpointMoveHandle, sets its owner to <code>owner</code>
     * and its index to <code>index</code>, and sets its locator to a new
     * {@link org.eclipse.draw2d.BendpointLocator}.
     * 
     * @param owner the ConnectionEditPart owner
     * @param index the index
     */
    public ConnectionBendpointMoveHandel(final ConnectionEditPart owner,
            final int index) {
        super(owner, index);
    }

    /**
     * Creates a new BendpointMoveHandle, sets its owner to <code>owner</code>
     * and its index to <code>index</code>, and sets its locator to a new
     * {@link org.eclipse.draw2d.BendpointLocator} with the given
     * <code>locatorIndex</code>.
     * 
     * @param owner the ConnectionEditPart owner
     * @param index the index
     * @param locatorIndex the index to use for the locator
     */
    public ConnectionBendpointMoveHandel(final ConnectionEditPart owner,
            final int index, final int locatorIndex) {
        super(owner, index, locatorIndex);
    }

    /**
     * Creates a new BendpointMoveHandle and sets its owner to
     * <code>owner</code>, sets its index to <code>index</code>, and sets
     * its locator to <code>locator</code>.
     * 
     * @param owner the ConnectionEditPart owner
     * @param index the index
     * @param locator the Locator
     */
    public ConnectionBendpointMoveHandel(final ConnectionEditPart owner,
            final int index, final Locator locator) {
        super(owner, index, locator);
    }

    /**
     * Creates and returns a new {@link ConnectionBendpointTracker}.
     * 
     * @return the new ConnectionBendpointTracker
     */
    @Override
    protected DragTracker createDragTracker() {
        ConnectionBendpointTracker tracker;
        tracker = new WorkflowConnectionBendpointTracker(
                (ConnectionEditPart)getOwner(), getIndex());
        tracker.setType(RequestConstants.REQ_MOVE_BENDPOINT);
        tracker.setDefaultCursor(getCursor());
        return tracker;
    }
}
