/* 
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
 *   ${date} (${user}): created
 */
package org.knime.workbench.editor2;

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
     * {@inheritDoc}
     */
    @Override
    protected void buildActions() {
        addRetargetAction(new UndoRetargetAction());
        addRetargetAction(new RedoRetargetAction());
        addRetargetAction(new DeleteRetargetAction());
        addRetargetAction(new ZoomInRetargetAction());
        addRetargetAction(new ZoomOutRetargetAction());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void declareGlobalActionKeys() {
        addGlobalActionKey(ActionFactory.PRINT.getId());
        addGlobalActionKey(ActionFactory.SELECT_ALL.getId());
        addGlobalActionKey(ActionFactory.PASTE.getId());
        addGlobalActionKey(ActionFactory.COPY.getId());
        addGlobalActionKey(ActionFactory.CUT.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributeToToolBar(final IToolBarManager tbm) {
//        tbm.add(getAction(ActionFactory.UNDO.getId()));
//        tbm.add(getAction(ActionFactory.REDO.getId()));

        tbm.add(new Separator());
        String[] zoomStrings = new String[] {ZoomManager.FIT_ALL,
                ZoomManager.FIT_HEIGHT, ZoomManager.FIT_WIDTH};
        tbm.add(new ZoomComboContributionItem(getPage(), zoomStrings));
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
