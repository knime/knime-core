/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.ISimpleObject;

/**
 * ContentProvider for the object repository.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class RepositoryContentProvider implements IStructuredContentProvider,
        ITreeContentProvider {
    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#
     *      getElements(java.lang.Object)
     */
    public Object[] getElements(final Object inputElement) {
        return ((IContainerObject)inputElement).getChildren();
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#
     *      getChildren(java.lang.Object)
     */
    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof IContainerObject) {
            return ((IContainerObject)parentElement).getChildren();
        }
        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#
     *      getParent(java.lang.Object)
     */
    public Object getParent(final Object element) {
        return ((IRepositoryObject)element).getParent();
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#
     *      hasChildren(java.lang.Object)
     */
    public boolean hasChildren(final Object element) {
        // If we have a simple object, this contains no children
        if (element instanceof ISimpleObject) {
            return false;
        }

        return ((IContainerObject)element).hasChildren();

    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {

    }

    /**
     * Changes the input.
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#
     *      inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object,
     *      java.lang.Object)
     */
    public void inputChanged(final Viewer viewer, final Object oldInput,
            final Object newInput) {
        if (!((newInput instanceof IContainerObject) || (newInput == null))) {
            throw new IllegalArgumentException(
                    "ContentProvider needs an 'IContainerObject' as input");
        }
    }
}
