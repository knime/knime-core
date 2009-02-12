/* 
 * -------------------------------------------------------------------
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
     * {@inheritDoc}
     */
    public Object[] getElements(final Object inputElement) {
        return ((IContainerObject)inputElement).getChildren();
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof IContainerObject) {
            return ((IContainerObject)parentElement).getChildren();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Object getParent(final Object element) {
        return ((IRepositoryObject)element).getParent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasChildren(final Object element) {
        // If we have a simple object, this contains no children
        if (element instanceof ISimpleObject) {
            return false;
        }

        return ((IContainerObject)element).hasChildren();

    }

    /**
     * {@inheritDoc}
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
