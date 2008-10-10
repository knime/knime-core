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
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
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
        // check whether it is a IProject (closed project)
        if (element instanceof IProject) {
            // try to find the registered workflow manager
            NodeContainer workflow = ProjectWorkflowMap.getWorkflow(
                    ((IProject)element).getName());
            if (workflow != null) {
                // if the workflow is open then it is regsitered and 
                // the number of contained nodes is returned
                return ((WorkflowManager)workflow).getNodeContainers()
                    .size() > 0;
            }
            // if the project is closed check for existence of workflow.knime 
            // file
            boolean isKnime = ((IProject)element).exists(new Path("/" 
                    + WorkflowPersistor.WORKFLOW_FILE));
            try {
                // if workflow.knime file is contained and there are other 
                // elements -> except the workflow.knime file 
                // hence > 2  (workflow.knime and .lock)
                return isKnime && (((IProject)element).members().length > 2);
            } catch (CoreException ce) {
                return false;
            }
        } else if (element instanceof IFolder) {
            // also closed meta nodes are detected and can be expanded
            return ((IFolder)element).exists(new Path("/" 
                     + WorkflowPersistor.WORKFLOW_FILE));
            // process meta nodes (no project but workflow manager = MetaNode)
        } else if (element instanceof WorkflowManager) {
            return ((WorkflowManager)element).getNodeContainers().size() > 0;
        }
        return false;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Object[] getChildren(final Object element) {
        // TODO: return the nodes and meta nodes etc.
        if (element instanceof IProject) {
            NodeContainer workflow = ProjectWorkflowMap.getWorkflow(
                    ((IProject)element).getName());
            if (workflow != null) {
                return ((WorkflowManager)workflow).getNodeContainers()
                    .toArray();
            }
            // process meta nodes
        } else if (element instanceof WorkflowManager) {
            return ((WorkflowManager)element).getNodeContainers().toArray();
        }
        return super.getChildren(element);
    }
}
