/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
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
 * History
 *   09.09.2009 (Fabian Dill): created
 */
package org.knime.workbench.ui.wizards.workflowgroup;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.knime.workbench.ui.navigator.KnimeResourceContentProvider;

/**
 * Provides the {@link IWorkspaceRoot} as a leaf in the tree.
 * 
 * @see KnimeResourceLabelProviderWithRoot
 *  
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class KnimeResourceContentProviderWithRoot extends
        KnimeResourceContentProvider {
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Object[] getElements(final Object inputElement) {
        if (inputElement instanceof IWorkspaceRoot) {
            Object[] elements = super.getElements(inputElement);
            Object[] rootAdded = new Object[elements.length + 1];
            rootAdded[0] = inputElement;
            System.arraycopy(elements, 0, rootAdded, 1, 
                    elements.length);
            return rootAdded;
        }
        return super.getElements(inputElement);
    }
    
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren(final Object element) {
        if (element instanceof IWorkspaceRoot) {
            return false;
        }
        return super.hasChildren(element);
    }

}
