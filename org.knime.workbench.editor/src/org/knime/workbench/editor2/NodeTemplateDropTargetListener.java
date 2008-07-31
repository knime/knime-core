/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   ${date} (Florian Georg): created
 */
package org.knime.workbench.editor2;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.swt.dnd.DND;

import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.view.NodeTemplateTransfer;

/**
 * Provides a listener for dropping templates onto the editor. Templates are
 * required to be of Type <code>NodeTemplate</code> and will be turned into
 * Workflow model objects via the
 * <code>NodeFromNodeTemplateCreationFactory</code> A special Transfer type is
 * used (NodeTemplateTransfer)
 * 
 * @see org.knime.workbench.repository.model.NodeTemplate
 * @see org.knime.workbench.editor2.NodeFromNodeTemplateCreationFactory
 * @see org.knime.workbench.repository.view.NodeTemplateTransfer
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeTemplateDropTargetListener extends
        AbstractTransferDropTargetListener {
    /**
     * Create a new drop listener for that viewer, using
     * <code>NodeTemplateTransfer</code>.
     * 
     * @param viewer The viewer
     */
    public NodeTemplateDropTargetListener(final EditPartViewer viewer) {
        super(viewer, NodeTemplateTransfer.getInstance());
    }

    /**
     * This creates and returns the appropriate CreationFactory for
     * <code>NodeTemplate</code> objects that are to be dropped into the
     * editor. If the template is no such object, it is asked for an adapter -
     * if it implements <code>IAdaptable</code>. So you can drop arbitrary
     * objects into the editor, as long as
     * <code>getAdapter(NodeTemplate.class)</code> returns a valid
     * <code>NodeTemplate</code> object
     * 
     * @param template The NodeTemplate that should be used in the
     *            CreationFactory
     * @return The CreationFactory that is able to create model objects from the
     *         template
     * 
     * @see IAdaptable#getAdapter(java.lang.Class)
     */
    protected CreationFactory getFactory(final Object template) {

        Object t = template;
        // If we aren't a NodeTemplate, perhaps we can get an adapter ?
        if (!(template instanceof NodeTemplate)) {
            // Last change: Ask adaptables for an adapter object
            if (template instanceof IAdaptable) {
                t = ((IAdaptable) template).getAdapter(NodeTemplate.class);
            }
        }
        // up to this point, we *must* have a valid NodeTemplate object.
        // null is not allowed, and treated as a logical error
        assert (t instanceof NodeTemplate) : "Can only drop "
                + "'NodeTemplate' instances on the editor, but was:" + t;

        // Create and return the factory
        NodeFromNodeTemplateCreationFactory factory = null;
        factory = new NodeFromNodeTemplateCreationFactory((NodeTemplate) t);

        return factory;
    }

    /**
     * Creates a "Create" request if something is dropped in the editor.
     * 
     * @return The CreateRequest
     */
    @Override
    protected Request createTargetRequest() {
        CreateRequest request = new CreateRequest();

        // take the dragged object out of the current event
        NodeTemplate template = (NodeTemplate) getCurrentEvent().data;

        // TODO for some reason sometimes the event contains no object - but
        // this doesn't seem to matter - dragging continues as expected
        if (template != null) {
            // Set the factory on the current request
            request.setFactory(this.getFactory(template));
        }
        return request;
    }

    /**
     * Update the location of the current CreateRequest.
     */
    @Override
    protected void updateTargetRequest() {
        ((CreateRequest) getTargetRequest()).setLocation(getDropLocation());
    }

    /**
     * Since we don't want to MOVE something, make sure this is always a
     * DND.DROP_COPY operation.
     * 
     * @see org.eclipse.gef.dnd.AbstractTransferDropTargetListener
     *      #handleDragOver()
     */
    @Override
    protected void handleDragOver() {
        getCurrentEvent().detail = DND.DROP_COPY;
        super.handleDragOver();
    }
}
