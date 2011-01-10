/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
