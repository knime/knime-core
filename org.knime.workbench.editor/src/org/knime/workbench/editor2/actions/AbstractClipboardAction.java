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
     * {@inheritDoc}
     */
    @Override
    public abstract String getId();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void runOnNodes(NodeContainerEditPart[] nodeParts);

    /**
     * @return The selected <code>ConnectionContainerEditParts</code>, may be
     *         empty
     */
    protected ConnectionContainerEditPart[] getSelectedConnectionParts() {

        ArrayList<ConnectionContainerEditPart> objects 
            = new ArrayList<ConnectionContainerEditPart>(getSelectedObjects());

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
