/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   28.02.2006 (sieb): created
 */
package org.knime.workbench.editor2;

import org.eclipse.gef.DragTracker;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.tools.MarqueeDragTracker;
import org.eclipse.gef.tools.MarqueeSelectionTool;

/**
 * Overrides the default <code>ScalableFreeformRootEditPart</code> to return
 * <code>MarqueeSelectionTool</code>s selecting also connections.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ConnectionSelectingScalableFreeformRootEditPart extends
        ScalableFreeformRootEditPart {

    /**
     * Creates a MarqueeDragTracker selecting also connections.
     * 
     * @see org.eclipse.gef.EditPart#getDragTracker(org.eclipse.gef.Request)
     */
    @Override
    public DragTracker getDragTracker(final Request req) {
        /*
         * The root will only be asked for a drag tracker if for some reason the
         * contents editpart says it is neither selector nor opaque.
         */
        MarqueeDragTracker tracker = new MarqueeDragTracker();
        tracker
                .setMarqueeBehavior(MarqueeSelectionTool.BEHAVIOR_NODES_AND_CONNECTIONS);
        
        return tracker;
    }
}
