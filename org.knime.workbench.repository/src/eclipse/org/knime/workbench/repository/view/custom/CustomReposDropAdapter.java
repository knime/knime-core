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
 *   04.06.2012 (meinl): created
 */
package org.knime.workbench.repository.view.custom;

import java.util.Iterator;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.knime.workbench.repository.model.AbstractRepositoryObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.CustomRepositoryManager;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.Root;

/**
 * Drop listener for the custom repository view.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
class CustomReposDropAdapter extends ViewerDropAdapter {
    private final CustomRepositoryManager m_manager;

    private final CustomRepositoryView m_reposView;

    CustomReposDropAdapter(final TreeViewer viewer,
            final CustomRepositoryManager manager,
            final CustomRepositoryView reposView) {
        super(viewer);
        m_manager = manager;
        m_reposView = reposView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performDrop(final Object data) {
        AbstractRepositoryObject target =
                (AbstractRepositoryObject)getCurrentTarget();

        if (!isValidTarget(target)) {
            return false;
        }
        if (!(data instanceof IStructuredSelection)) {
            return false;
        }
        IStructuredSelection selection = (IStructuredSelection)data;
        if (selection.isEmpty()) {
            return false;
        }
        Iterator<?> it = selection.iterator();
        while (it.hasNext()) {
            if (!(it.next() instanceof AbstractRepositoryObject)) {
                return false;
            }
        }

        if (target == null) {
            target = m_manager.getRoot();
        }

        it = selection.iterator();
        boolean ok = true;
        while (it.hasNext()) {
            ok &= addOneObject(target, (AbstractRepositoryObject)it.next());
        }

        getViewer().refresh();
        return ok;
    }

    private boolean addOneObject(final AbstractRepositoryObject target,
            final AbstractRepositoryObject o) {
        final AbstractRepositoryObject copy;
        if (sourceAndTargetEqual(getCurrentEvent())) {
            copy = o;
        } else {
            copy = (AbstractRepositoryObject)o.deepCopy();

        }
        if (target instanceof Root) {
            Root root = (Root)target;
            if (root.contains(copy)) {
                return false;
            }

            root.addChild(copy);
            copy.setParent(root);
        } else {
            int location = getCurrentLocation();
            IContainerObject parent = target.getParent();
            assert ((parent instanceof Root) || ((parent instanceof Category) && CustomRepositoryManager
                    .isCustomCategory((Category)parent)));

            if (location == LOCATION_AFTER) {
                if (!parent.addChildAfter(copy, target)) {
                    return false;
                }
                copy.setParent(parent);
            } else if (location == LOCATION_BEFORE) {
                if (!parent.addChildBefore(copy, target)) {
                    return false;
                }
                copy.setParent(parent);
            } else if (location == LOCATION_ON) {
                assert (target instanceof Category);
                Category cat = (Category)target;
                if (cat.contains(copy)) {
                    return false;
                }

                assert CustomRepositoryManager.isCustomCategory(cat);
                cat.addChild(copy);
                copy.setParent(cat);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateDrop(final Object target, final int operation,
            final TransferData transferType) {
        // if (sourceAndTargetEqual(getCurrentEvent())) {
        // overrideOperation(DND.DROP_MOVE);
        // } else {
        // overrideOperation(DND.DROP_COPY);
        // }

        return !m_reposView.isLocked()
                && isValidTarget(target)
                && LocalSelectionTransfer.getTransfer().isSupportedType(
                        transferType);
    }

    private boolean isValidTarget(final Object target) {
        if (target instanceof Root) {
            return true;
        } else if (target instanceof IRepositoryObject) {
            IContainerObject parent = ((IRepositoryObject)target).getParent();
            if (!((parent instanceof Root) || ((parent instanceof Category) && CustomRepositoryManager
                    .isCustomCategory((Category)parent)))) {
                return false;
            }

            if (getCurrentLocation() == LOCATION_ON) {
                return (target instanceof Category);
            } else {
                return true;
            }
        }

        return (target == null);
    }

    private boolean sourceAndTargetEqual(final DropTargetEvent event) {
        return ((DropTarget)event.widget).getControl().getData("isDragSource") != null;
    }
}
