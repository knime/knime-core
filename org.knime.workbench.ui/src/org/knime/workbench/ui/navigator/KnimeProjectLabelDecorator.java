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
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class KnimeProjectLabelDecorator implements ILabelDecorator {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            KnimeProjectLabelDecorator.class);
    
    private static final Image m_projectExecuting = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_executing2.png");
    private static final Image m_projectExecuted = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_executed2.png");
    private static final Image m_projectConfigured = KNIMEUIPlugin.getDefault()
        .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_configured.png");
    private static final Image m_node = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/node.png"); 

    private static final Map<String, NodeContainer>m_projects 
        = new HashMap<String, NodeContainer>();
    
    public void addListener(ILabelProviderListener listener) {
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    }

    public Image decorateImage(Image image, Object element) {
        Image img = image;
        if (element instanceof IProject) {
            IProject project = (IProject)element;
            NodeContainer projectNode = m_projects.get(project.getName());
            if (projectNode == null) {
                for (NodeContainer nc : WorkflowManager.ROOT
                            .getNodeContainerBreadthFirstSearch()) {
                    if (nc.getName().equals(project.getName())) {
                        projectNode = nc;
                        m_projects.put(project.getName(), projectNode); 
                        break;
                    }
                }
            }
            if (projectNode == null) {
                return img;
            }
            if (projectNode.getState().equals(NodeContainer.State.EXECUTED)) {
                img = m_projectExecuted;
            } else if (projectNode.getState().equals(
                    NodeContainer.State.EXECUTING)) {
                img = m_projectExecuting;                        
            } else if (projectNode.getState().equals(
                    NodeContainer.State.CONFIGURED)) {
                img = m_projectConfigured;
            }
        } else if (element instanceof IFolder) {
            // then its a node
            // TODO: also show status of the node?
            img = m_node;
        }
        return img;
    }

    public String decorateText(String text, Object element) {
        return text;
    }
    

}
