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
 *   18.07.2007 (sieb): created
 */
package org.knime.workbench.ui.navigator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;

/**
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class KnimeResourceContentProvider extends WorkbenchContentProvider {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            KnimeResourceContentProvider.class);
    
    private static final Object[] EMPTY_ARRAY = new Object[0];
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren(final Object element) {
        if (element instanceof IFile) {
            return false;
        }
        if (isKNIMEWorkflow(element)) {
            IContainer project = (IContainer)element;
            NodeContainer workflow = ProjectWorkflowMap.getWorkflow(
                    project.getFullPath().toString());
            if (workflow != null) {
                // if the workflow is open then it is regsitered and 
                // the number of contained nodes is returned
                return ((WorkflowManager)workflow).getNodeContainers()
                    .size() > 0;
            } 
        } else if (element instanceof WorkflowManager) {
            return ((WorkflowManager)element).getNodeContainers().size() > 0;
        } 
        // check if parent is a KNIME workflow
        // then it is a node and has no children
        if (element instanceof IContainer) {
            IContainer container = (IContainer)element;
            if (isKNIMEWorkflow(container.getParent())) {
                return false;
            }
        }
        return getFolders(element).length > 0;
    }
    
    
    private boolean isKNIMEWorkflow(final Object element) {
        if (element instanceof IContainer) {
            IContainer container = (IContainer)element;
            return container.exists(new Path(WorkflowPersistor.WORKFLOW_FILE));
        } 
        return false;
    }

    
    private Object[] getFolders(final Object element) {
        if (element instanceof IContainer) {
            IContainer container = (IContainer)element;
            List<IResource> children = new ArrayList<IResource>();
            try {
                for (IResource r : container.members()) {
                    if (r instanceof IContainer) {
                        children.add(r);
                    }
                }
                return children.toArray();
            } catch (CoreException e) {
                LOGGER.debug("Error while retrieving information for element "
                        + container.getName()); 
                return EMPTY_ARRAY;
            }
        }
        return EMPTY_ARRAY;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Object[] getChildren(final Object element) {
        if (element instanceof IFile) {
            return EMPTY_ARRAY;
        }
        if (isKNIMEWorkflow(element)) {
            IContainer project = (IContainer)element;
            NodeContainer workflow = ProjectWorkflowMap.getWorkflow(project
                    .getFullPath().toString());
            if (workflow != null) {
                // if the workflow is open then it is regsitered and
                // the number of contained nodes is returned
                return getSortedNodeContainers(
                        ((WorkflowManager)workflow).getNodeContainers());
            }
        } else if (element instanceof WorkflowManager) {
            return getSortedNodeContainers(((WorkflowManager)element)
                    .getNodeContainers());
        }
        return getFolders(element);
    }

    // bugfix: 1474 (now nodes are always sorted lexicographically)
    private Object[] getSortedNodeContainers(
            final Collection<NodeContainer> nodes) {
        Set<NodeContainer>copy = new TreeSet<NodeContainer>(
                new Comparator<NodeContainer>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public int compare(final NodeContainer o1, 
                    final NodeContainer o2) {
                return o1.getNameWithID().compareTo(o2.getNameWithID());
            }
            
        });
        copy.addAll(nodes);
        return copy.toArray();
    }
}
