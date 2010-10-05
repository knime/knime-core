/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;

/**
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME.com GmbH
 */
public class KnimeResourceContentProvider implements ITreeContentProvider {
    
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
                    project.getFullPath());
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
                    .getFullPath());
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


    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParent(final Object element) {
        if (element instanceof IResource) {
            return ((IResource)element).getParent();
        }
        if (element instanceof NodeContainer) {
            return ((NodeContainer)element).getParent();
        }
        return ResourcesPlugin.getWorkspace().getRoot();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getElements(final Object inputElement) {
        return getChildren(inputElement);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        // nothing to do
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, 
            final Object newInput) {
        // nothing to do
    }
}
