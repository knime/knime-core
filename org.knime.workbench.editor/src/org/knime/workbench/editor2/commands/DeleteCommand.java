/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.WorkflowManagerInput;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * This is the command to delete nodes (and connections) from a workflow.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DeleteCommand extends Command {

    /** WFM, in which nodes/connections are deleted. */
    private final WorkflowManager m_manager;
    
    /** Ids of nodes being deleted. */
    private final NodeID[] m_nodeIDs;
    
    /** Array containing connections that are to be deleted and which are not 
     * part of the persistor (perisistor only covers connections whose source
     * and destination is part of the persistor as well). */
    private final ConnectionContainer[] m_connections;
    
    /** Number of connections that will be deleted upon execute(). This includes
     * m_connections and all connections covered by the persistor. This number
     * is at least m_connections.length.
     */
    private final int m_connectionCount;

    /** Persistor of deleted sub flow for undo. */
    private WorkflowPersistor m_undoPersitor;

    /** A viewer in which to update the selection upon undo or null if none
     * could be determined. */
    private final EditPartViewer m_viewer;

    /**
     * Creates a new delete command for a set of nodes. Undo will also restore
     * all connections that were removed as part of this command's execute.
     * @param nodesAndConnectionParts Selected nodes and connections
     * @param manager wfm hosting the nodes.
     */
    public DeleteCommand(final Collection<?> nodesAndConnectionParts,
            final WorkflowManager manager) {
        m_manager = manager;
        Set<NodeID> idSet = new LinkedHashSet<NodeID>();
        Set<ConnectionContainer> conSet = 
            new LinkedHashSet<ConnectionContainer>();
        EditPartViewer viewer = null;
        for (Object p : nodesAndConnectionParts) {
            if (p instanceof NodeContainerEditPart) {
                NodeContainerEditPart ncep = (NodeContainerEditPart)p;
                if (viewer == null && ncep.getParent() != null) {
                    viewer = ncep.getViewer();
                }
                NodeID id = ncep.getNodeContainer().getID();
                idSet.add(id);
                // the selection may correspond to the outer workflow, this
                // happens a meta node is double-cliked (opened) and the 
                // action buttons are enabled/disabled -- a new DeleteCommand
                // is created with the correct WorkbenchPart but the wrong 
                // selection (seen in debugger)
                if (!manager.containsNodeContainer(id)) {
                    // render the command invalid (canExecute() returns false)
                    conSet.clear();
                    idSet.clear();
                    break;
                }
                conSet.addAll(manager.getIncomingConnectionsFor(id));
                conSet.addAll(manager.getOutgoingConnectionsFor(id));
            } else if (p instanceof ConnectionContainerEditPart) {
                ConnectionContainerEditPart ccep = 
                    (ConnectionContainerEditPart)p;
                conSet.add(ccep.getModel());
                if (viewer == null && ccep.getParent() != null) {
                    viewer = ccep.getViewer();
                }
            }
        }
        m_viewer = viewer;
        m_nodeIDs = idSet.toArray(new NodeID[idSet.size()]);
        
        m_connectionCount = conSet.size(); 
        // remove all connections that will be contained in the persistor 
        for (Iterator<ConnectionContainer> it = conSet.iterator(); 
        it.hasNext();) {
            ConnectionContainer c = it.next();
            if (idSet.contains(c.getSource()) && idSet.contains(c.getDest())) {
                it.remove();
            }
        }
        m_connections = conSet.toArray(new ConnectionContainer[conSet.size()]);
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        boolean foundValid = false;
        for (NodeID id : m_nodeIDs) {
            foundValid = true;
            if (!m_manager.canRemoveNode(id)) {
                return false;
            }
        }
        for (ConnectionContainer cc : m_connections) {
            foundValid = true;
            if (!m_manager.canRemoveConnection(cc)) {
                return false;
            }
        }
        return foundValid;
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        // The WFM removes all connections for us, before the node is
        // removed.
        if (m_nodeIDs.length > 0) {
            m_undoPersitor = m_manager.copy(m_nodeIDs);
        }
        for (NodeID id : m_nodeIDs) {
            NodeContainer nc = m_manager.getNodeContainer(id);
            if (nc instanceof WorkflowManager) {
                // since the equals method of the WorkflowManagerInput
                // only looks for the WorkflowManager, we can pass
                // null as the editor argument
                WorkflowManagerInput in =
                    new WorkflowManagerInput((WorkflowManager)nc, null);
                IEditorPart editor =
                    PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage()
                    .findEditor(in);
                if (editor != null) {
                    editor.getEditorSite().getPage().closeEditor(editor,
                            false);
                }
            }
            m_manager.removeNode(id);
        }
        for (ConnectionContainer cc : m_connections) {
            m_manager.removeConnection(cc);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void undo() {
        // select the new ones....
        if (m_viewer != null  && m_viewer.getRootEditPart().getContents() 
                instanceof WorkflowRootEditPart) {
            ((WorkflowRootEditPart)m_viewer.getRootEditPart().getContents())
                .setFutureSelection(m_nodeIDs);
            m_viewer.deselectAll();
        }

        if (m_undoPersitor != null) {
            m_manager.paste(m_undoPersitor);
        }
        for (ConnectionContainer cc : m_connections) {
            ConnectionContainer newCC = m_manager.addConnection(cc.getSource(), 
                    cc.getSourcePort(), cc.getDest(), cc.getDestPort());
            newCC.setUIInfo(cc.getUIInfo());
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void dispose() {
        m_undoPersitor = null;
        super.dispose();
    }

    /** @return the number of nodes to be deleted. */
    public int getNodeCount() {
        return m_nodeIDs.length;
    }
    
    /** @return the number of connections to be deleted. */
    public int getConnectionCount() {
        return m_connectionCount;
    }

}
