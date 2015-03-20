/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.workbench.editor2.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * GEF command for adding a meta node from the repository to the workflow.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ReplaceMetaNodeCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ReplaceMetaNodeCommand.class);

    private final WorkflowPersistor m_persistor;

    private final Point m_location;

    private final boolean m_snapToGrid;

    // for undo
    private WorkflowCopyContent m_copyContent;

    private DeleteCommand m_deleteCommand;

    private NodeContainerEditPart m_oldMetaNode;

    private NodeContainer m_container;

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager that should host the new node
     * @param persistor the paste content
     * @param location Initial visual location in the
     * @param snapToGrid if node location should be rounded to closest grid location.
     */
    public ReplaceMetaNodeCommand(final WorkflowManager manager,
            final WorkflowPersistor persistor, final NodeContainerEditPart node, final boolean snapToGrid) {
        super(manager);
        m_persistor = persistor;
        int[] bounds = node.getNodeContainer().getUIInformation().getBounds();
        m_location = new Point(bounds[0], bounds[1]);
        m_snapToGrid = snapToGrid;
        m_oldMetaNode = node;
        m_deleteCommand = new DeleteCommand(Collections.singleton(m_oldMetaNode), manager);
    }

    /** We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc} */
    @Override
    public boolean canExecute() {
        return super.canExecute() && m_persistor != null && m_location != null && m_deleteCommand.canExecute();
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {

     // the following code has mainly been copied from
        // IDEWorkbenchWindowAdvisor#preWindowShellClose
        IPreferenceStore store =
            KNIMEUIPlugin.getDefault().getPreferenceStore();
        if (!store.contains(PreferenceConstants.P_CONFIRM_RESET)
                || store.getBoolean(PreferenceConstants.P_CONFIRM_RESET)) {
            MessageDialogWithToggle dialog =
                MessageDialogWithToggle.openOkCancelConfirm(
                    Display.getDefault().getActiveShell(),
                    "Confirm reset...",
                    "Do you really want to reset all downstream node(s) ?",
                    "Do not ask again", false, null, null);
            if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
                return;
            }
            if (dialog.getToggleState()) {
                store.setValue(PreferenceConstants.P_CONFIRM_RESET, false);
                KNIMEUIPlugin.getDefault().savePluginPreferences();
            }
        }

        WorkflowManager hostWFM = getHostWFM();
        Set<ConnectionContainer> incomingConnectionsForOldNode = hostWFM.getIncomingConnectionsFor(m_oldMetaNode.getNodeContainer().getID());
        Set<ConnectionContainer> outgoingConnectionsForOldNode = hostWFM.getOutgoingConnectionsFor(m_oldMetaNode.getNodeContainer().getID());

        m_deleteCommand.execute();

        // Add node to workflow and get the container
        try {
            WorkflowManager wfm = getHostWFM();
            m_copyContent = wfm.paste(m_persistor);
            NodeID[] nodeIDs = m_copyContent.getNodeIDs();
            if (nodeIDs.length > 0) {
                NodeID first = nodeIDs[0];
                m_container = wfm.getNodeContainer(first);
                // create extra info and set it
                NodeUIInformation info = new NodeUIInformation(
                        m_location.x, m_location.y, -1, -1, false);
                info.setSnapToGrid(m_snapToGrid);
                info.setIsDropLocation(true);
                m_container.setUIInformation(info);
            }
        } catch (Throwable t) {
            // if fails notify the user
            String error = "Meta node cannot be created";
            LOGGER.debug(error + ": " + t.getMessage(), t);
            MessageBox mb = new MessageBox(Display.getDefault().
                    getActiveShell(), SWT.ICON_WARNING | SWT.OK);
            mb.setText(error);
            mb.setMessage("The meta node could not be created "
                    + "due to the following reason:\n" + t.getMessage());
            mb.open();
            return;
        }

        // set incoming connections
        Iterator<ConnectionContainer> itr = incomingConnectionsForOldNode.iterator();
        int p = 0;
        while (itr.hasNext()) {
            ConnectionContainer cc = itr.next();

            while (!hostWFM.canAddConnection(cc.getSource(), cc.getSourcePort(), m_container.getID(), p)) {
                p++;
                if (p > m_container.getNrInPorts()) {
                    break;
                }
            }

            if (p > m_container.getNrInPorts()) {
                break;
            } else {
                hostWFM.addConnection(cc.getSource(), cc.getSourcePort(), m_container.getID(), p);
            }
        }

        // set outgoing connections
        itr = outgoingConnectionsForOldNode.iterator();
        p = 0;
        while (itr.hasNext()) {
            ConnectionContainer cc = itr.next();

            while (!hostWFM.canAddConnection(m_container.getID(), p, cc.getDest(), cc.getDestPort())) {
                p++;
                if (p > m_container.getNrOutPorts()) {
                    break;
                }
            }

            if (p > m_container.getNrOutPorts()) {
                break;
            } else {
                hostWFM.addConnection(m_container.getID(), p, cc.getDest(), cc.getDestPort());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_deleteCommand.canUndo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        NodeID[] ids = m_copyContent.getNodeIDs();
        if (LOGGER.isDebugEnabled()) {
            String debug = "Removing node";
            if (ids.length == 1) {
                debug = debug + " " + ids[0];
            } else {
                debug = debug + "s " + Arrays.asList(ids);
            }
            LOGGER.debug(debug);
        }
        WorkflowManager wm = getHostWFM();
        if (canUndo()) {
            for (NodeID id : ids) {
                wm.removeNode(id);
            }
            for (WorkflowAnnotation anno : m_copyContent.getAnnotations()) {
                wm.removeAnnotation(anno);
            }
            m_deleteCommand.undo();
        } else {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(),
                    "Operation no allowed", "The node(s) "
                    + Arrays.asList(ids) + " can currently not be removed");
        }
    }

}
