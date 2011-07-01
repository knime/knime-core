/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: Jun 29, 2011
 * Author: ohl
 */
package org.knime.workbench.ui.navigator;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.DeleteResourceAction;
import org.eclipse.ui.views.navigator.IResourceNavigator;
import org.eclipse.ui.views.navigator.MainActionGroup;
import org.eclipse.ui.views.navigator.ResourceNavigatorRenameAction;
import org.knime.workbench.ui.navigator.actions.CopyToClipboard;
import org.knime.workbench.ui.navigator.actions.DeleteAction;
import org.knime.workbench.ui.navigator.actions.PasteAction;
import org.knime.workbench.ui.navigator.actions.RenameAction;

/**
 *
 * @author ohl, University of Konstanz
 */
@SuppressWarnings("deprecation")
public class KNIMEResourceNavigatorActionGroup extends MainActionGroup {

    private final Clipboard m_clipboard;

    private DeleteAction m_delAction;

    private RenameAction m_renAction;

    private CopyToClipboard m_copyAction;

    private PasteAction m_pasteAction;

    /**
     * @param navigator
     */
    public KNIMEResourceNavigatorActionGroup(
            final IResourceNavigator navigator, final TreeViewer viewer,
            final Clipboard clipboard) {
        super(navigator);
        m_clipboard = clipboard;
        m_delAction = new DeleteAction(viewer.getControl().getShell(), viewer);
        m_renAction = new RenameAction(viewer);
        m_copyAction = new CopyToClipboard(viewer, m_clipboard);
        m_pasteAction = new PasteAction(viewer, m_clipboard);
        // the copy action must notify the paste action of new clipboard content
        m_copyAction.setPasteAction(m_pasteAction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillActionBars(final IActionBars actionBars) {
        super.fillActionBars(actionBars);
        actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
                m_delAction);
        actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(),
                m_renAction);
        actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
                m_copyAction);
        actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
                m_pasteAction);
        updateMyActions();
        actionBars.updateActionBars();
        actionBars.getMenuManager().updateAll(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillContextMenu(final IMenuManager menu) {
        super.fillContextMenu(menu);
        IContributionItem copyItem = menu.find("org.eclipse.ui.CopyAction");
        if (copyItem != null) {
            // move must be our own action (due to workflow locks)
            menu.insertBefore("org.eclipse.ui.CopyAction", m_copyAction);
            menu.remove("org.eclipse.ui.CopyAction");
        }
        if (menu.find("org.eclipse.ui.PasteAction") != null) {
            // move must be our own action (due to workflow locks)
            menu.insertBefore("org.eclipse.ui.PasteAction", m_pasteAction);
            menu.remove("org.eclipse.ui.PasteAction");
        }
        // delete must be our own action (due to workflow locks)
        if (menu.find(DeleteResourceAction.ID) != null) {
            menu.insertBefore(DeleteResourceAction.ID, new Separator());
            menu.insertBefore(DeleteResourceAction.ID, m_delAction);
            menu.remove(DeleteResourceAction.ID);
        }

        // Rename must be our own action (due to workflow locks). Hence
        // replace the default rename action if it is there. */
        if (menu.find(ResourceNavigatorRenameAction.ID) != null) {
            menu.insertBefore(ResourceNavigatorRenameAction.ID, m_renAction);
            menu.remove(ResourceNavigatorRenameAction.ID);
        }
        updateMyActions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateActionBars() {
        updateMyActions();
    }

    private void updateMyActions() {
        IStructuredSelection selection =
                (IStructuredSelection)getContext().getSelection();
        m_copyAction.selectionChanged(selection);
        m_pasteAction.selectionChanged(selection);
        m_renAction.selectionChanged(selection);
        m_delAction.selectionChanged(selection);

    }
}
