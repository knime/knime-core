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
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.ContextAwareNodeFactory;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 *
 * @author ohl, University of Konstanz
 */
public class DropNodeCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DropNodeCommand.class);

    private final ContextAwareNodeFactory<NodeModel> m_factory;

    private final Point m_location;

    private NodeContainer m_container;

    private final NodeCreationContext m_dropContext;

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager that should host the new node
     * @param factory The factory of the Node that should be added
     * @param context the file to be set as source for the new node.
     * @param location Initial visual location in the
     */
    public DropNodeCommand(final WorkflowManager manager,
            final ContextAwareNodeFactory<NodeModel> factory,
            final NodeCreationContext context, final Point location) {
        super(manager);
        m_factory = factory;
        m_location = location;
        m_dropContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return m_factory != null && (m_location != null)
            && (m_dropContext != null) && super.canExecute();
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        // Add node to workflow and get the container
        WorkflowManager hostWFM = getHostWFM();
        try {
            NodeID id =
                    hostWFM.addNodeAndApplyContext(m_factory, m_dropContext);
            m_container = hostWFM.getNodeContainer(id);
            // create extra info and set it
            NodeUIInformation info =
                new NodeUIInformation(m_location.x, m_location.y, -1, -1, true);
            m_container.setUIInformation(info);

            // Open the dialog. Some times.
            if (m_container != null
                    && m_container instanceof SingleNodeContainer
                    && m_container.getState().equals(State.IDLE)
                    && m_container.hasDialog()
                    // and has only a variable in port
                    && m_container.getNrInPorts() == 1) {
                // if not executable and has a dialog and is fully connected
//                m_container.openDialogInJFrame();

                // This is embedded in a special JFace wrapper dialog
                //
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            WrappedNodeDialog dlg =
                                new WrappedNodeDialog(
                                        Display.getCurrent().getActiveShell(),
                                        m_container);
                            dlg.open();
                        } catch (Throwable t) {
                            // they need to open it manually then
                        }
                    }
                });
            }

        } catch (Throwable t) {
            // if fails notify the user
            LOGGER.debug("Node cannot be created.", t);
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_WARNING | SWT.OK);
            mb.setText("Node cannot be created.");
            mb.setMessage("The selected node could not be created "
                    + "due to the following reason:\n" + t.getMessage());
            mb.open();
            return;
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_container != null
                && getHostWFM().canRemoveNode(m_container.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Removing node #" + m_container.getID());
        if (canUndo()) {
            getHostWFM().removeNode(m_container.getID());

            // TODO: save the nodes settings for a re-do. In case the dialog
            // was opened and settings adjusted.
        } else {
            MessageDialog.openInformation(
                    Display.getDefault().getActiveShell(),
                    "Operation no allowed",
                    "The node " + m_container.getNameWithID()
                            + " can currently not be removed");
        }
    }

}
