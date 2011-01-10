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
 *   09.06.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.navigator.ResourceNavigator;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class KnimeResourceChangeListener implements IResourceChangeListener {

    private final IResourceDeltaVisitor m_visitor;

    /**
     *
     * @param navigator usually the KNIME resource navigator
     */
    public KnimeResourceChangeListener(final ResourceNavigator navigator) {
        m_visitor = new IResourceDeltaVisitor() {

             private void doRefresh(final Object node) {
                 Display.getDefault().asyncExec(new Runnable() {

                    public void run() {
                        if (navigator.getViewer().getControl().isDisposed()) {
                            return;
                        }
                        if (node == null) {
                            navigator.getViewer().refresh();
                        }
                        if (node instanceof IWorkspaceRoot) {
                            navigator.getViewer().refresh();
                        } else {
                            navigator.getViewer().refresh(node);
                        }
                    }

                 });
             }
             private void doRemove(final IResource node) {

                Display.getDefault().asyncExec(new Runnable() {

                    public void run() {
                        if (navigator != null && navigator.getViewSite() != null
                                && navigator.getViewSite().getShell() != null
                                && !navigator.getViewSite().getShell()
                                .isDisposed()) {
                            navigator.getViewer().remove(node);
                        }
                    }

                 });
             }

              public boolean visit(final IResourceDelta delta) {
                 IResource res = delta.getResource();
                 IResource parent = res.getParent();

                 switch (delta.getKind()) {
                    case IResourceDelta.ADDED:
                        doRefresh(parent);
                        break;
                    case IResourceDelta.REMOVED:
                        doRemove(res);
                        break;
                    case IResourceDelta.CHANGED:
                        // do not refresh the KnimeResourceNavigator if a file
                        // has changed
                        if (res instanceof IFile) {
                            return false;
                        }
                        // might be the workspace root that has changed
                        // new projects added!
                        doRefresh(parent);
                        break;
                    }
                 return true; // visit the children
              }
           };
    }

    /**
     * {@inheritDoc}
     */
    public void resourceChanged(final IResourceChangeEvent event) {
        switch (event.getType()) {
        case IResourceChangeEvent.PRE_CLOSE:

            break;
        case IResourceChangeEvent.PRE_DELETE:

            break;
        case IResourceChangeEvent.POST_CHANGE:

                try {
                        event.getDelta().accept(m_visitor);

                    } catch (CoreException e) {
                        // do nothing
                        // Only used to keep the tree in sync with the
                        // resources.
                    }

            break;
        }
    }

}
