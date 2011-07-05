/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
import org.eclipse.ui.actions.RenameResourceAction;
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
        if (menu.find(RenameResourceAction.ID) != null) {
            menu.insertBefore(RenameResourceAction.ID, m_renAction);
            menu.remove(RenameResourceAction.ID);
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
