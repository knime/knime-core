/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Set;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.WorkflowManagerInput;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * This is the command to delete <code>NodeContainer</code>s from the
 * workflow.
 *
 * @author Florian Georg, University of Konstanz
 */
public class DeleteNodeContainerCommand extends Command {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DeleteNodeContainerCommand.class);

    private final NodeContainerEditPart m_part;

    private final WorkflowManager m_manager;
    
    private WorkflowPersistor m_undoPersitor;
    
    private Set<ConnectionContainer> m_inConnections;
    private Set<ConnectionContainer> m_outConnections;

    /**
     * Creates a new delete command for a <code>NodeContainer</code>.
     *
     * @param nodePart The container edit part to delete
     * @param manager The manager hosting the container
     */
    public DeleteNodeContainerCommand(final NodeContainerEditPart nodePart,
            final WorkflowManager manager) {
        m_part = nodePart;
        m_manager = manager;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        return m_manager.canRemoveNode(m_part.getNodeContainer().getID());
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        LOGGER.debug("Deleting node #" + m_part.getNodeContainer().getID()
                + " from Workflow");
        // The WFM removes all connections for us, before the node is
        // removed.
        try {
            NodeContainer nc = m_part.getNodeContainer();
            m_undoPersitor = m_manager.copy(nc.getID());
            m_outConnections = m_manager.getOutgoingConnectionsFor(nc.getID());
            m_inConnections = m_manager.getIncomingConnectionsFor(nc.getID());
            m_manager.removeNode(nc.getID());
            if (nc instanceof WorkflowManager) {
                WorkflowManagerInput in = 
                    new WorkflowManagerInput((WorkflowManager)nc,
                        // since the equals method of the WorkflowManagerInput
                        // only looks for the WorkflowManager, we can pass null
                        // as the editor argument 
                        null);
                IEditorPart editor = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage().findEditor(in);
                if (editor != null) {
                    editor.getEditorSite().getPage().closeEditor(editor, false);
                }
            }
            
        } catch (Exception ex) {
            LOGGER.warn("Operation not allowed.", ex);
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {                    
                    MessageBox mb =
                        new MessageBox(Display.getDefault().getActiveShell(),
                                SWT.ICON_INFORMATION | SWT.OK);
                    mb.setText("Operation not allowed");
                    mb.setMessage("You cannot remove this node");
                    mb.open();
                    if (m_part.getFigure() instanceof NodeContainerFigure) {
                        ((NodeContainerFigure)m_part.getFigure()).unmark();
                    }
                }
            });
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void undo() {
        NodeID newNode = m_manager.paste(m_undoPersitor)[0];
        boolean hasFailure = false;
        for (ConnectionContainer in : m_inConnections) {
            if (m_manager.canAddConnection(in.getSource(), in.getSourcePort(),
                    newNode, in.getDestPort())) {
                ConnectionContainer c = m_manager.addConnection(in.getSource(),
                        in.getSourcePort(), newNode, in.getDestPort());
                c.setUIInfo(in.getUIInfo());
            } else {
                hasFailure = true;
            }
        }
        for (ConnectionContainer out : m_outConnections) {
            if (m_manager.canAddConnection(newNode, out.getSourcePort(),
                    out.getDest(), out.getDestPort())) {
                ConnectionContainer c = m_manager.addConnection(newNode, 
                        out.getSourcePort(), out.getDest(), out.getDestPort());
                c.setUIInfo(out.getUIInfo());
            } else {
                hasFailure = true;
            }
        }
        if (hasFailure) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    MessageDialog.openInformation(
                            Display.getDefault().getActiveShell(),
                            "Operation not allowed",
                            "Not all connections could be restored");
                }
            });
        }
    }
    
}
