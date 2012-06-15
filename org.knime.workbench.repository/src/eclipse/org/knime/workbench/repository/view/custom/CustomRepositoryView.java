/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 * History
 *   29.05.2012 (meinl): created
 */
package org.knime.workbench.repository.view.custom;

import org.apache.xmlbeans.XmlException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.knime.workbench.repository.model.CustomRepositoryManager;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.repository.view.AbstractRepositoryView;

/**
 * This is the custom node repository in which the user can arrange existing
 * categories and node and can add new categories.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class CustomRepositoryView extends AbstractRepositoryView {
    private CustomRepositoryManager m_manager;

    private boolean m_isLocked = false;

    private CustomTreeEditor m_treeEditor;

    @Override
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent);

        m_viewer.setLabelProvider(new CustomRepositoryLabelProvider(m_viewer
                .getControl().getFont()));

        Transfer[] transfers =
                new Transfer[]{LocalSelectionTransfer.getTransfer()};
        m_viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, transfers,
                new CustomReposDropAdapter(m_viewer, m_manager, this));

        final Tree tree = m_viewer.getTree();
        tree.addListener(SWT.MouseUp, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                TreeItem item = tree.getItem(new Point(event.x, event.y));
                if (item == null) {
                    tree.setSelection(new TreeItem[]{});
                }
            }
        });

        m_removeEntryAction = new RemoveEntryAction(m_viewer);
        m_newCategoryAction = new NewCategoryAction(m_viewer);

        m_treeEditor = new CustomTreeEditor(m_viewer.getTree());

        if (!m_isLocked) {
            m_viewer.getTree().addListener(SWT.Selection, m_treeEditor);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Root transformRepository(final Root originalRoot) {
        return m_manager.transformRepository(originalRoot);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IViewSite site, final IMemento memento)
            throws PartInitException {
        super.init(site, memento);

        Boolean locked =
                (memento != null) ? memento.getBoolean("locked") : null;
        try {
            if ((memento != null) && (memento.getString("tree") != null)) {
                String xml = memento.getString("tree");
                m_manager = new CustomRepositoryManager(xml);
                m_isLocked = (locked != null) ? locked.booleanValue() : true;
            } else {
                m_manager = new CustomRepositoryManager();
                m_isLocked = false;
            }
        } catch (XmlException ex) {
            throw new PartInitException(
                    "Could not load custom repository content", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveState(final IMemento memento) {
        memento.putString("tree", m_manager.serializeRepository());
        memento.putBoolean("locked", m_isLocked);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fillLocalToolBar(final IToolBarManager manager) {
        super.fillLocalToolBar(manager);
        m_removeAllAction = new RemoveAllAction(m_viewer);
        m_removeAllAction.setEnabled(!m_isLocked);
        manager.add(m_removeAllAction);

        m_toggleLockAction = new ToggleLockAction(this);
        manager.add(m_toggleLockAction);

        m_loadDefinitionAction = new LoadDefinitionAction(m_manager, m_viewer);
        m_loadDefinitionAction.setEnabled(!m_isLocked);
        manager.add(m_loadDefinitionAction);
        manager.add(new SaveDefinitionAction(m_viewer.getControl().getShell(),
                m_manager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fillContextMenu(final IMenuManager manager) {
        super.fillContextMenu(manager);

        if (!m_isLocked) {
            manager.add(m_newCategoryAction);
            m_newCategoryAction.setEnabled(m_newCategoryAction.canBeEnabled());
            manager.add(m_removeEntryAction);
            m_removeEntryAction.setEnabled(m_removeEntryAction.canBeEnabled());
        }
    }

    private RemoveEntryAction m_removeEntryAction;

    private NewCategoryAction m_newCategoryAction;

    private RemoveAllAction m_removeAllAction;

    private ToggleLockAction m_toggleLockAction;

    private LoadDefinitionAction m_loadDefinitionAction;

    boolean toggleLock() {
        m_isLocked = !m_isLocked;
        m_removeAllAction.setEnabled(!m_isLocked);
        m_loadDefinitionAction.setEnabled(!m_isLocked);

        if (m_isLocked) {
            m_viewer.getTree().removeListener(SWT.Selection, m_treeEditor);
        } else {
            m_viewer.getTree().addListener(SWT.Selection, m_treeEditor);
        }

        return m_isLocked;
    }

    boolean isLocked() {
        return m_isLocked;
    }
}
