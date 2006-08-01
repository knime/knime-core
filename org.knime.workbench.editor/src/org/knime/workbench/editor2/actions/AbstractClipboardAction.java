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
 *   21.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * The abstract action for the clipboard action.
 * Provides essentially a method to gather all connection parts.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class AbstractClipboardAction extends AbstractNodeAction {

    /**
     * Constructs a new abstract clipboard action.
     * 
     * @param editor the workflow editor this action is intended for
     */
    public AbstractClipboardAction(final WorkflowEditor editor) {

        super(editor);
    }

    /**
     * @see org.eclipse.jface.action.IAction#getId()
     */
    @Override
    public abstract String getId();

    /**
     * @see de.unikn.knime.workbench.editor2.actions.AbstractNodeAction#runOnNodes(de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart[])
     */
    @Override
    public abstract void runOnNodes(NodeContainerEditPart[] nodeParts);

    /**
     * @return The selected <code>ConnectionContainerEditParts</code>, may be
     *         empty
     */
    protected ConnectionContainerEditPart[] getSelectedConnectionParts() {

        ArrayList<ConnectionContainerEditPart> objects = new ArrayList<ConnectionContainerEditPart>(
                getSelectedObjects());

        // clean list, that is, remove all objects that are not edit
        // parts for a ConnectionContainer
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            Object element = iter.next();
            if (!(element instanceof ConnectionContainerEditPart)) {
                iter.remove();
                continue;
            }
        }

        final ConnectionContainerEditPart[] parts = objects
                .toArray(new ConnectionContainerEditPart[objects.size()]);

        return parts;
    }
}
