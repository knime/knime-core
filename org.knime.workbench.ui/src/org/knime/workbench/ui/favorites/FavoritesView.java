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
 * History
 *   13.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.favorites;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.workbench.core.nodeprovider.NodeProvider;
import org.knime.workbench.repository.NodeUsageListener;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.repository.view.RepositoryContentProvider;
import org.knime.workbench.repository.view.RepositoryLabelProvider;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class FavoritesView extends ViewPart implements NodeUsageListener {

    private TreeViewer m_viewer;


    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        m_viewer = new TreeViewer(parent,
                SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        this.getSite().setSelectionProvider(m_viewer);
        Transfer[] transfers = new Transfer[]{
                LocalSelectionTransfer.getTransfer()};
        m_viewer.addDragSupport(DND.DROP_COPY, transfers,
                new FavoriteNodesDragSource(this));
        m_viewer.addDropSupport(DND.DROP_COPY, transfers,
                new FavoriteNodesDropTarget(this));
        m_viewer.setLabelProvider(new RepositoryLabelProvider());
        m_viewer.setContentProvider(new RepositoryContentProvider());

        // no sorting
        m_viewer.setComparator(null);
        m_viewer.setInput("Loading favorite nodes...");

        Job treeUpdater = new Job("Favorite Nodes Loader") {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                final Root root = FavoriteNodesManager.getInstance().getRoot();
                final Object category = root.getChildByID(
                        FavoriteNodesManager.FAV_CAT_ID, false);

                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                } else {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!parent.isDisposed()) {
                                m_viewer.setInput(root);
                                m_viewer.expandToLevel(category, 1);
                            }
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        };
        treeUpdater.setSystem(true);
        treeUpdater.schedule();

        NodeUsageRegistry.addNodeUsageListener(FavoritesView.this);
        hookDoubleClickAction();
    }

    private void hookDoubleClickAction() {
        m_viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                Object o =
                        ((IStructuredSelection)event.getSelection())
                                .getFirstElement();
                if (o instanceof NodeTemplate) {
                    NodeTemplate tmplt = (NodeTemplate)o;
                    NodeFactory<? extends NodeModel> nodeFact;
                    try {
                        nodeFact = tmplt.getFactory().newInstance();
                    } catch (Exception e) {
                        NodeLogger.getLogger(FavoritesView.class).error(
                                "Unable to instantiate the selected node "
                                + tmplt.getFactory().getName(), e);
                        return;
                    }
                    boolean added = NodeProvider.INSTANCE.addNode(nodeFact);
                    if (added) {
                        NodeUsageRegistry.addNode(tmplt);
                    }
                }
            }
        });
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
    }

    /**
     *
     * @return the selection of the underlying list
     */
    public ISelection getSelection() {
        return m_viewer.getSelection();
    }

    /**
     *
     * @param template adds this node template to the favorites
     */
    void addNodeTemplate(final NodeTemplate template) {
        FavoriteNodesManager.getInstance().addFavoriteNode(template);
        m_viewer.refresh();
        m_viewer.expandToLevel(template, AbstractTreeViewer.ALL_LEVELS);
    }



    /**
     * Removes the given node from the personal favorites and refreshes the
     * tree.
     * @param node node to be removed from the personal favorite nodes
     */
    public void removeFavorite(final NodeTemplate node) {
        FavoriteNodesManager.getInstance().removeFavoriteNode(node);
        refreshView();
    }


    /**
     * Expands all tree items (the categories).
     */
    public void expandAll() {
        m_viewer.expandAll();
    }

    /**
     * Collapses the whole tree.
     */
    public void collapseAll() {
        m_viewer.collapseAll();
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void nodeAdded() {
        FavoriteNodesManager.getInstance().updateNodes();
        refreshView();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void frequentHistoryChanged() {
        FavoriteNodesManager.getInstance().updateFrequentUsedNodes();
        refreshView();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void usedHistoryChanged() {
        FavoriteNodesManager.getInstance().updateLastUsedNodes();
        // TODO: if the manager would know the view,
        // or the view would have access to the categories
        // we could refresh more specifically this categroy
        refreshView();
    }

    private void refreshView() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                if (!m_viewer.getControl().isDisposed()) {
                    m_viewer.refresh();
                }
            }
        });
    }
}
