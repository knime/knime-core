/* @(#)$$RCSfile$$ 
 * $$Revision$$ $$Date$$ $$Author$$
 * 
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
 *   ${date} (${user}): created
 */
package de.unikn.knime.workbench.editor2;

import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.DeleteRetargetAction;
import org.eclipse.gef.ui.actions.RedoRetargetAction;
import org.eclipse.gef.ui.actions.UndoRetargetAction;
import org.eclipse.gef.ui.actions.ZoomComboContributionItem;
import org.eclipse.gef.ui.actions.ZoomInRetargetAction;
import org.eclipse.gef.ui.actions.ZoomOutRetargetAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Contributes action to the toolbar / menu bar.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowEditorActionBarContributor extends ActionBarContributor {

    /**
     * @see org.eclipse.gef.ui.actions.ActionBarContributor#buildActions()
     */
    protected void buildActions() {
        addRetargetAction(new UndoRetargetAction());
        addRetargetAction(new RedoRetargetAction());
        addRetargetAction(new DeleteRetargetAction());

        addRetargetAction(new ZoomInRetargetAction());
        addRetargetAction(new ZoomOutRetargetAction());
    }

    /**
     * @see org.eclipse.gef.ui.actions.ActionBarContributor
     *      #declareGlobalActionKeys()
     */
    protected void declareGlobalActionKeys() {
        addGlobalActionKey(ActionFactory.PRINT.getId());
        addGlobalActionKey(ActionFactory.SELECT_ALL.getId());
        addGlobalActionKey(ActionFactory.PASTE.getId());
        addGlobalActionKey(ActionFactory.COPY.getId());
        addGlobalActionKey(ActionFactory.CUT.getId());
    }

    /**
     * @see org.eclipse.ui.part.EditorActionBarContributor
     *      #contributeToToolBar(IToolBarManager)
     */
    public void contributeToToolBar(final IToolBarManager tbm) {
        tbm.add(getAction(ActionFactory.UNDO.getId()));
        tbm.add(getAction(ActionFactory.REDO.getId()));

        tbm.add(new Separator());
        String[] zoomStrings = new String[] {ZoomManager.FIT_ALL,
                ZoomManager.FIT_HEIGHT, ZoomManager.FIT_WIDTH};
        tbm.add(new ZoomComboContributionItem(getPage(), zoomStrings));
    }

    /**
     * @see org.eclipse.ui.part.EditorActionBarContributor
     *      #contributeToMenu(IMenuManager)
     */
    public void contributeToMenu(final IMenuManager menubar) {
        super.contributeToMenu(menubar);

        // MenuManager viewMenu = new MenuManager("Zoom");
        // viewMenu.add(getAction(GEFActionConstants.ZOOM_IN));
        // viewMenu.add(getAction(GEFActionConstants.ZOOM_OUT));
        // viewMenu.add(new Separator());
        // viewMenu.add(getAction(GEFActionConstants.TOGGLE_RULER_VISIBILITY));
        // viewMenu.add(getAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY));
        // viewMenu.add(getAction(GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY));
        // viewMenu.add(new Separator());
        // viewMenu.add(getAction(GEFActionConstants.MATCH_WIDTH));
        // viewMenu.add(getAction(GEFActionConstants.MATCH_HEIGHT));
        // menubar.insertAfter(IWorkbenchActionConstants.M_EDIT, viewMenu);
    }
}
