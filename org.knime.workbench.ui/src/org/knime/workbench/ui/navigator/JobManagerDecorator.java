/* This source code, its documentation and all appendant files
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
 */
package org.knime.workbench.ui.navigator;

import java.net.URL;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.knime.core.node.workflow.NodeContainer;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public class JobManagerDecorator implements
        ILabelDecorator {

    private final CopyOnWriteArraySet<ILabelProviderListener>m_listeners =
            new CopyOnWriteArraySet<ILabelProviderListener>();
    

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(ILabelProviderListener listener) {
        m_listeners.add(listener);
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
    public boolean isLabelProperty(Object element, String property) {
        // TODO Auto-generated method stub
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(ILabelProviderListener listener) {
        m_listeners.remove(listener);
    }

    @Override
    public Image decorateImage(Image image, Object element) {
        if (element instanceof IContainer) {
            NodeContainer cont = ProjectWorkflowMap.getWorkflow(
                    ((IContainer)element).getFullPath().toString());
            if (cont != null) {
                URL iconURL = cont.findJobManager().getIcon();
                if (iconURL != null) {
                    ImageDescriptor descr = ImageDescriptor.createFromURL(
                            iconURL);
                    return new DecorationOverlayIcon(image, 
                            descr, IDecoration.TOP_RIGHT).createImage();
                }
            }
//            image.addOverlay(descr, IDecoration.TOP_RIGHT);
            
        }
        return image;
    }

    @Override
    public String decorateText(String text, Object element) {
        // TODO Auto-generated method stub
        return null;
    }



}
