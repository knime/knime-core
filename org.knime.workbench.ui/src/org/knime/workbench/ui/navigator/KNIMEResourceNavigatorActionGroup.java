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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.views.navigator.IResourceNavigator;
import org.eclipse.ui.views.navigator.MainActionGroup;
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

    private final TreeViewer m_viewer;

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
        m_viewer = viewer;
        m_clipboard = clipboard;
        m_delAction =
            new DeleteAction(m_viewer.getControl().getShell(), m_viewer);
        m_renAction = new RenameAction(m_viewer);
        m_copyAction = new CopyToClipboard(m_viewer, m_clipboard);
        m_pasteAction = new PasteAction(m_viewer, m_clipboard);
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
        actionBars.updateActionBars();
        actionBars.getMenuManager().updateAll(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateActionBars() {
        super.updateActionBars();
        IStructuredSelection selection =
                (IStructuredSelection)getContext().getSelection();
        ((SelectionListenerAction)m_pasteAction).selectionChanged(selection);
    }

}
