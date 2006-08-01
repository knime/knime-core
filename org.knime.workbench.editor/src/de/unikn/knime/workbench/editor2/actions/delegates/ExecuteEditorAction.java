/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2004
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
 *   10.11.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.actions.delegates;

import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.actions.AbstractNodeAction;
import de.unikn.knime.workbench.editor2.actions.ExecuteAction;

/**
 * Editor action for "execute".
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ExecuteEditorAction extends AbstractEditorAction {
    /**
     * @see 
     * de.unikn.knime.workbench.editor2.actions.delegates.AbstractEditorAction
     *      #createAction(de.unikn.knime.workbench.editor2.WorkflowEditor)
     */
    @Override
    protected AbstractNodeAction createAction(final WorkflowEditor editor) {
        return new ExecuteAction(editor);
    }
}
