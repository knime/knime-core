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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
            for (NodeContainer nc : WorkflowManager.ROOT.getNodeContainers()) {
                if (nc.getName().equals(project.getName())) {
                    if (nc.getState().equals(NodeContainer.State.EXECUTED)) {
                        img = KNIMEUIPlugin.getDefault().getImage(
                                KNIMEUIPlugin.PLUGIN_ID, 
                        "icons/project_executed2.png");
                    } else if (nc.getState().equals(
                            NodeContainer.State.EXECUTING)) {
                        img = KNIMEUIPlugin.getDefault().getImage(
                                KNIMEUIPlugin.PLUGIN_ID, 
                        "icons/project_executing2.png");                        
                    } else if (nc.getState().equals(
                            NodeContainer.State.CONFIGURED)) {
                        img = KNIMEUIPlugin.getDefault().getImage(
                                KNIMEUIPlugin.PLUGIN_ID, 
                        "icons/project_configured.png"); 
                    }
                }
            }
        }
        return img;
    }

    public String decorateText(String text, Object element) {
        if (element instanceof IResource && element instanceof IProject) {
            // TODO: mark dirty -> check editor?
        }
        return text;
    }
    

}
