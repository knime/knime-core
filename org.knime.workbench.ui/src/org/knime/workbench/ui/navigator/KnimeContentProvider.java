/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   18.07.2007 (sieb): created
 */
package org.knime.workbench.ui.navigator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.knime.core.node.workflow.WorkflowPersistor;

/**
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class KnimeContentProvider extends WorkbenchContentProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren(final Object element) {
        // the knime navigator does not has any children except the first root
        // level
        if (element instanceof IProject) {
            boolean isKnime = ((IProject)element).exists(new Path("/" 
                    + WorkflowPersistor.WORKFLOW_FILE));
            try {
                return isKnime && (((IProject)element).members().length > 2);
            } catch (CoreException ce) {
                return false;
            }
        } else if (element instanceof IFolder) {
            return ((IFolder)element).exists(new Path("/" 
                     + WorkflowPersistor.WORKFLOW_FILE));
        }
        return false;
    }
    
    @Override
    public Object[] getChildren(Object element) {
        // TODO: return the nodes and meta nodes etc.
        return super.getChildren(element);
    }
}
