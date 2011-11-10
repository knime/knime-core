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
 * -------------------------------------------------------------------
 */
package org.knime.workbench.ui.navigator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.ui.part.ResourceTransfer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowPersistor;

/**
 *
 * @author Fabian Dill, KNIME.com AG
 */
public class WorkflowMoveDragListener implements DragSourceListener {

    private final TreeViewer m_viewer;

    /**
     * @param viewer
     */
    public WorkflowMoveDragListener(final TreeViewer viewer) {
        m_viewer = viewer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragFinished(final DragSourceEvent event) {
        // the move action will do our job
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void dragSetData(final DragSourceEvent event) {
        IStructuredSelection selection =
                (IStructuredSelection)m_viewer.getSelection();
        if (ResourceTransfer.getInstance().isSupportedType(event.dataType)) {
            // in the navigator almost every tree item is an IResource -
            // except for nodes of an open workflow (these are SingleNodeCont.)
            List<IResource> drags = new ArrayList<IResource>();
            Iterator<Object> s = selection.iterator();
            while (s.hasNext()) {
                Object o = s.next();
                if (!isNode(o)) {
                    drags.add((IResource)o);
                }
            }
            event.data = drags.toArray(new IResource[drags.size()]);
        }
    }

    /*
     * Nodes are either SingleNodeContainers (in case the flow is opened in
     * an editor) or IFolders.
     */
    private boolean isNode(final Object source) {
        if (source instanceof IFolder) {
            IFolder dir = (IFolder)source;
            IContainer p = dir.getParent();
            if (p == null) {
                return false;
            }
            return p.exists(new Path(WorkflowPersistor.WORKFLOW_FILE));
        }
        return (source instanceof SingleNodeContainer);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void dragStart(final DragSourceEvent event) {
        // don't start dragging if the selection contains only nodes
        IStructuredSelection selection =
                (IStructuredSelection)m_viewer.getSelection();
        Iterator<Object> s = selection.iterator();
        while (s.hasNext()) {
            Object o = s.next();
            if (!isNode(o)) {
                return;
            }
        }
        event.doit = false;
    }

}
