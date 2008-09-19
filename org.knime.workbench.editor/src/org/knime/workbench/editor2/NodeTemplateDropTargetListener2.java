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
 * -------------------------------------------------------------------
 *
 * History
 *   04.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.editor2.commands.CreateNodeCommand;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.repository.model.NodeTemplate;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class NodeTemplateDropTargetListener2 implements
        TransferDropTargetListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            NodeTemplateDropTargetListener2.class);


    private final EditPartViewer m_viewer;

    public NodeTemplateDropTargetListener2(final EditPartViewer viewer) {
        m_viewer = viewer;
    }

    /**
     * {@inheritDoc}
     */
    public Transfer getTransfer() {
        return LocalSelectionTransfer.getTransfer();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnabled(final DropTargetEvent event) {
        // TODO: calculate enabled
        // if getSelection is instanceof NodeTemplate
        // If we aren't a NodeTemplate, perhaps we can get an adapter ?
        if (isNodeTemplate()) {
            event.feedback = DND.FEEDBACK_SELECT;
            event.operations = DND.DROP_COPY;
            event.detail = DND.DROP_COPY;
        }
        return isNodeTemplate();
    }

    /**
     * {@inheritDoc}
     */
    public void dragEnter(final DropTargetEvent event) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void dragLeave(final DropTargetEvent event) {
        // do nothing
    }

    protected Point getDropLocation(final DropTargetEvent event) {
        event.x = event.display.getCursorLocation().x;
        event.y = event.display.getCursorLocation().y;
        Point p = new Point(m_viewer.getControl()
                    .toControl(event.x, event.y).x,
                    m_viewer.getControl()
                    .toControl(event.x, event.y).y);
        LOGGER.debug("to control: " + p);
        return p;
    }

    /**
     * {@inheritDoc}
     */
    public void dragOperationChanged(final DropTargetEvent event) {
        // do nothing -> all is handled during "drop"
    }

    /**
     * {@inheritDoc}
     */
    public void dragOver(final DropTargetEvent event) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void drop(final DropTargetEvent event) {
        // TODO: get the Selection from the LocalSelectionTransfer
        // check instanceof NodeTemplate and fire a CreateRequest
        LOGGER.debug("drop: " + event);
        if (isNodeTemplate()) {
            NodeTemplate template = (NodeTemplate)((IStructuredSelection)
                        LocalSelectionTransfer.getTransfer().getSelection())
                        .getFirstElement();
            CreateRequest request = new CreateRequest();
            // TODO for some reason sometimes the event contains no object - but
            // this doesn't seem to matter - dragging continues as expected
            if (template != null) {
                // Set the factory on the current request
                NodeFromNodeTemplateCreationFactory factory
                    = new NodeFromNodeTemplateCreationFactory(template);
                request.setFactory(factory);
                WorkflowRootEditPart root = (WorkflowRootEditPart)m_viewer
                    .getRootEditPart().getContents();
                m_viewer.getEditDomain().getCommandStack().execute(
                        new CreateNodeCommand(root.getWorkflowManager(),
                                (NodeFactory<? extends NodeModel>)
                                    factory.getNewObject(),
                                getDropLocation(event)));
                NodeUsageRegistry.addNode(template);
            }
        }
    }


    private boolean isNodeTemplate() {
        if (LocalSelectionTransfer.getTransfer().getSelection() == null) {
            return false;
        }
        Object template = ((IStructuredSelection)LocalSelectionTransfer
                .getTransfer().getSelection()).getFirstElement();
        if (!(template instanceof NodeTemplate)) {
            // Last change: Ask adaptables for an adapter object
            if (template instanceof IAdaptable) {
                template = ((IAdaptable) template).getAdapter(
                        NodeTemplate.class);
            }
        }
        return template instanceof NodeTemplate;
    } 

    /**
     * {@inheritDoc}
     */
    public void dropAccept(final DropTargetEvent event) {
    }
}
