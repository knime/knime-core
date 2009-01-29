/*
 * ------------------------------------------------------------------
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
    
    private IResourceDeltaVisitor m_visitor;
    
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
                        // TODO: look for .knime file name
                        if (res instanceof IFile) {
                            doRefresh(res);
                        }
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
