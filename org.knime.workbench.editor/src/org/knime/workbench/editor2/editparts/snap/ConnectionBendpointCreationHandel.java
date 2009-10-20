/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
import org.eclipse.gef.handles.BendpointCreationHandle;
import org.eclipse.gef.tools.ConnectionBendpointTracker;

/**
 * Returns a different ConnectionBendpointTracker to enable snapping of
 * bendpoints.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ConnectionBendpointCreationHandel extends BendpointCreationHandle {

    /**
     * Creates a new BendpointCreationHandle.
     */
    public ConnectionBendpointCreationHandel() {
    }

    /**
     * Creates a new BendpointCreationHandle, sets its owner to
     * <code>owner</code> and its index to <code>index</code>, and sets its
     * locator to a new {@link org.eclipse.draw2d.MidpointLocator}.
     * 
     * @param owner the ConnectionEditPart owner
     * @param index the index
     */
    public ConnectionBendpointCreationHandel(final ConnectionEditPart owner,
            final int index) {
        super(owner, index);
    }

    /**
     * Creates a new BendpointCreationHandle, sets its owner to
     * <code>owner</code> and its index to <code>index</code>, and sets its
     * locator to a new {@link org.eclipse.draw2d.MidpointLocator} with the
     * given <code>locatorIndex</code>.
     * 
     * @param owner the ConnectionEditPart owner
     * @param index the index
     * @param locatorIndex the locator index
     */
    public ConnectionBendpointCreationHandel(final ConnectionEditPart owner,
            final int index, final int locatorIndex) {
        super(owner, index, locatorIndex);
    }

    /**
     * Creates a new BendpointCreationHandle and sets its owner to
     * <code>owner</code>, sets its index to <code>index</code>, and sets
     * its locator to <code>locator</code>.
     * 
     * @param owner the ConnectionEditPart owner
     * @param index the index
     * @param locator the Locator
     */
    public ConnectionBendpointCreationHandel(final ConnectionEditPart owner,
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
        tracker.setType(RequestConstants.REQ_CREATE_BENDPOINT);
        tracker.setDefaultCursor(getCursor());
        return tracker;
    }
}
