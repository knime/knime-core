/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   07.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.navigator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class KnimeProjectLabelDecorator implements ILabelDecorator {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            KnimeProjectLabelDecorator.class);
    
    private static final Image EXECUTING = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_executing2.png");
    private static final Image EXECUTED = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_executed2.png");
    private static final Image CONFIGURED = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_configured.png");
    private static final Image NODE = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/node.png"); 

    private static final Map<String, NodeContainer>PROJECTS 
        = new HashMap<String, NodeContainer>();
    
    private final Set<ILabelProviderListener>m_listeners 
        = new HashSet<ILabelProviderListener>();
    
    /**
     * Adds a listener to the {@link WorkflowManager#ROOT} and to all already 
     * opened projects.
     */
    public KnimeProjectLabelDecorator() {
        WorkflowManager.ROOT.addListener(new WorkflowListener() {

            public void workflowChanged(final WorkflowEvent event) {
                switch (event.getType()) {
                case NODE_ADDED:
                    NodeContainer nc = ((NodeContainer)event.getNewValue());
                    PROJECTS.put(nc.getName(), nc);
                    break;
                case NODE_REMOVED:
                    PROJECTS.remove(event.getOldValue());
                    break;
                default: // no interest in other events here
                }
            }
            
        });
        for (NodeContainer nc 
                : WorkflowManager.ROOT.getNodeContainerBreadthFirstSearch()) {
            // TODO: bad hack to determine projects...
            if (nc.getID().toString().lastIndexOf(":") < 2) {
                PROJECTS.put(nc.getName(), nc);
            } else {
                // if we have really a breadth first search then we are finished
                break;
            }
        }
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    public void addListener(final ILabelProviderListener listener) {
        NodeLogger.getLogger(KnimeProjectLabelDecorator.class)
            .info("listener added " + listener);
        m_listeners.add(listener);
    }

    /**
     * 
     * {@inheritDoc}
     */
    public void dispose() {
        EXECUTED.dispose();
        EXECUTING.dispose();
        CONFIGURED.dispose();
        NODE.dispose();
        PROJECTS.clear();
    }

    /**
     * 
     * {@inheritDoc}
     */
    public boolean isLabelProperty(final Object element, 
            final String property) {
        return false;
    }

    /**
     * 
     * {@inheritDoc}
     */
    public void removeListener(final ILabelProviderListener listener) {
        m_listeners.remove(listener);
    }

    /**
     * 
     * {@inheritDoc}
     */
    public Image decorateImage(final Image image, final Object element) {
        Image img = image;
        if (element instanceof IProject) {
            IProject project = (IProject)element;
            NodeContainer projectNode = PROJECTS.get(project.getName());
            if (projectNode == null) {
                return img;
            }
            if (projectNode.getState().equals(NodeContainer.State.EXECUTED)) {
                img = EXECUTED;
            } else if (projectNode.getState().equals(
                    NodeContainer.State.EXECUTING)) {
                img = EXECUTING;                        
            } else if (projectNode.getState().equals(
                    NodeContainer.State.CONFIGURED)) {
                img = CONFIGURED;
            }
        } else if (element instanceof IFolder) {
            // then its a node
            img = NODE;
        }
        return img;
    }

    /**
     * 
     * {@inheritDoc}
     */
    public String decorateText(final String text, 
            final Object element) {
        return text;
    }
    

}
