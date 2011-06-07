/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
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
 *   May 4, 2011 (morent): created
 */

package org.knime.workbench.editor2;

import java.net.URL;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.knime.core.node.ContextAwareNodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.repository.util.ContextAwareNodeFactoryMapper;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public abstract class WorkflowEditorDropTargetListener
        <T extends CreationFactory> extends AbstractTransferDropTargetListener {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowEditorFileDropTargetListener.class);


    private final T m_factory;

    /**
     * @param viewer the edit part viewer this drop target listener is attached
     *            to
     */
    protected WorkflowEditorDropTargetListener(final EditPartViewer viewer,
            final T factory) {
        super(viewer);
        m_factory = factory;
    }

    /**
     * @param url the URL of the file
     * @return a node factory creating a node that is registered for handling
     *      this type of file
     */
    protected ContextAwareNodeFactory<NodeModel> getNodeFactory(final URL url) {
        String path = url.getPath();
        Class<? extends ContextAwareNodeFactory<NodeModel>> clazz
                = ContextAwareNodeFactoryMapper.getNodeFactory(path);
        if (clazz == null) {
            LOGGER.warn("No node factory is registered for handling "
                    + " \"" + path + "\"");
            return null;
        }
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            LOGGER.error("Can't create node " + clazz.getName() + ".", e);
        } catch (IllegalAccessException e) {
            LOGGER.error(e);
        }
        return null;
    }

    /**
     * @param event the drop target event
     * @return the first dragged resource or null if the event contains a
     *      resource that is not of type {@link ContentObject}
     */
    protected ContentObject getDragResources(final DropTargetEvent event) {
        LocalSelectionTransfer transfer = (LocalSelectionTransfer)getTransfer();
        ISelection selection = transfer.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            Object firstElement = ss.getFirstElement();
            if (firstElement instanceof ContentObject) {
                return (ContentObject)firstElement;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected Request createTargetRequest() {
        CreateRequest request = new CreateRequest();
        request.setFactory(m_factory);
        return request;
    }

    /** {@inheritDoc} */
    @Override
    protected void updateTargetRequest() {
        ((CreateRequest)getTargetRequest()).setLocation(getDropLocation());
    }

    /**
     * @return the creation factory
     */
    protected T getFactory() {
        return m_factory;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        return getDragResources(event) != null;
    }


}
