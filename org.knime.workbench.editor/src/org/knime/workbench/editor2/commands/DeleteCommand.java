/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

import static org.knime.workbench.ui.async.AsyncUtil.wfmAsyncSwitch;
import static org.knime.workbench.ui.async.AsyncUtil.wfmAsyncSwitchRethrow;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.eclipse.gef.EditPartViewer;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.node.workflow.WorkflowCopyUI;
import org.knime.core.ui.node.workflow.WorkflowCopyWithOffsetUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.OperationNotAllowedException;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * This is the command to delete nodes (and connections) from a workflow.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DeleteCommand extends AbstractKNIMECommand {

    /** Ids of nodes being deleted. */
    private final NodeID[] m_nodeIDs;
    /** References to annotations being deleted. */
    private final WorkflowAnnotationID[] m_annotationIDs;

    /** Array containing connections that are to be deleted and which are not
     * part of the persistor (perisistor only covers connections whose source
     * and destination is part of the persistor as well). */
    private final ConnectionContainerUI[] m_connections;

    /** Number of connections that will be deleted upon execute(). This includes
     * m_connections and all connections covered by the persistor. This number
     * is at least m_connections.length.
     */
    private final int m_connectionCount;

    /** Copy of deleted sub flow for undo. */
    private WorkflowCopyUI m_undoCopy;

    /** A viewer in which to update the selection upon undo or null if none
     * could be determined. */
    private final EditPartViewer m_viewer;

    /**
     * To work backwards-compatible with {@link WorkflowManager}
     *
     * @param editParts
     * @param manager
     */
    public DeleteCommand(final Collection<?> editParts, final WorkflowManager manager) {
        this(editParts, WorkflowManagerWrapper.wrap(manager));
    }

    /**
     * Creates a new delete command for a set of nodes. Undo will also restore
     * all connections that were removed as part of this command's execute.
     * @param editParts Selected nodes and connections and annotations
     * @param manager wfm hosting the nodes.
     */
    public DeleteCommand(final Collection<?> editParts,
            final WorkflowManagerUI manager) {
        super(manager);
        Set<NodeID> idSet = new LinkedHashSet<NodeID>();
        Set<WorkflowAnnotationID> annotationSet =
            new LinkedHashSet<WorkflowAnnotationID>();
        Set<ConnectionContainerUI> conSet =
            new LinkedHashSet<ConnectionContainerUI>();
        EditPartViewer viewer = null;
        for (Object p : editParts) {
            if (p instanceof NodeContainerEditPart) {
                NodeContainerEditPart ncep = (NodeContainerEditPart)p;
                if (viewer == null && ncep.getParent() != null) {
                    viewer = ncep.getViewer();
                }
                NodeID id = ncep.getNodeContainer().getID();
                idSet.add(id);
                // the selection may correspond to the outer workflow, this
                // happens a metanode is double-clicked (opened) and the
                // action buttons are enabled/disabled -- a new DeleteCommand
                // is created with the correct WorkbenchPart but the wrong
                // selection (seen in debugger)
                if (!manager.containsNodeContainer(id)) {
                    // render the command invalid (canExecute() returns false)
                    conSet.clear();
                    idSet.clear();
                    annotationSet.clear();
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
            } else if (p instanceof AnnotationEditPart) {
                // this gets the node annotations and the workflow annotations
                AnnotationEditPart anno = (AnnotationEditPart)p;
                Annotation annotationModel = anno.getModel();
                if (annotationModel instanceof WorkflowAnnotation) {
                    annotationSet.add(((WorkflowAnnotation)annotationModel).getID());
                }
                if (viewer == null && anno.getParent() != null) {
                    viewer = anno.getViewer();
                }
            }
        }
        m_viewer = viewer;
        m_nodeIDs = idSet.toArray(new NodeID[idSet.size()]);
        m_annotationIDs = annotationSet.toArray(
                new WorkflowAnnotationID[annotationSet.size()]);

        m_connectionCount = conSet.size();
        // remove all connections that will be contained in the persistor
        for (Iterator<ConnectionContainerUI> it = conSet.iterator();
        it.hasNext();) {
            ConnectionContainerUI c = it.next();
            if (idSet.contains(c.getSource()) && idSet.contains(c.getDest())) {
                it.remove();
            }
        }

        m_connections = conSet.toArray(new ConnectionContainerUI[conSet.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }

        final WorkflowManagerUI hostWFM = getHostWFMUI();
        for (NodeID id : m_nodeIDs) {
            if (!hostWFM.canRemoveNode(id)) {
                return false;
            }
        }

        final WorkflowManager wm = getHostWFM();
        for (ConnectionContainerUI cc : m_connections) {
            final ConnectionID ccId = cc.getID();

            // hostWFM.canRemoveConnection(ccId) will throw an exception instead of returning false due to
            //      it eventually calling WorkflowManager.getIncomingConnectionFor(NodeID, int); we pre-emptively
            //      do the check it would do for containment
            if (((wm != null) && (!wm.containsNodeContainer(ccId.getDestinationNode())))
                || (!hostWFM.canRemoveConnection(ccId))) {
                return false;
            }
        }

        return (m_nodeIDs.length > 0) || (m_connections.length > 0) || (m_annotationIDs.length > 0);
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        WorkflowManagerUI hostWFM = getHostWFMUI();
        // The WFM removes all connections for us, before the node is
        // removed.
        if (m_nodeIDs.length > 0 || m_annotationIDs.length > 0) {
            WorkflowCopyContent.Builder content = WorkflowCopyContent.builder();
            content.setNodeIDs(m_nodeIDs);
            content.setAnnotationIDs(m_annotationIDs);
            try {
                m_undoCopy = wfmAsyncSwitchRethrow(wfm -> wfm.cut(content.build()), wfm -> wfm.cutAsync(content.build()),
                    hostWFM, "Deleting content ...");
            } catch (OperationNotAllowedException e) {
                openDialog("Problem while deleting parts", e.getMessage());
                return;
            }
        }

        //remove dangling connections that haven't been removed by the cut
        ConnectionID[] connectionIDs =
            Arrays.stream(m_connections).map(cc -> cc.getID()).toArray(size -> new ConnectionID[size]);
        try {
            wfmAsyncSwitchRethrow(wfm -> {
                wfm.remove(new NodeID[0], connectionIDs, new WorkflowAnnotationID[0]);
                return null;
            }, wfm -> wfm.removeAsync(new NodeID[0], connectionIDs, new WorkflowAnnotationID[0]), hostWFM,
                "Deleting connections ...");
        } catch (OperationNotAllowedException e) {
            openDialog("Problem while deleting parts", e.getMessage());
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

        WorkflowManagerUI hostWFM = getHostWFMUI();
        wfmAsyncSwitch(wfm -> {
            //paste copied content
            if (m_undoCopy != null) {
                wfm.paste(m_undoCopy);
            }

            //add dangling connections
            for (ConnectionContainerUI cc : m_connections) {
                wfm.addConnection(cc.getSource(), cc.getSourcePort(), cc.getDest(), cc.getDestPort(),
                    cc.getUIInfo() != null ? cc.getUIInfo().getAllBendpoints() : null);
            }
            return null;
        }, wfm -> {
            //paste copied content
            CompletableFuture<WorkflowCopyContent> pasteFuture = null;
            if (m_undoCopy != null) {
                assert m_undoCopy instanceof WorkflowCopyWithOffsetUI;
                pasteFuture = wfm.pasteAsync((WorkflowCopyWithOffsetUI)m_undoCopy);
            }

            //add dangling connections
            Supplier<CompletableFuture<Void>> addConnections = () -> {
                CompletableFuture<?>[] futures = new CompletableFuture[m_connections.length];
                for (int i = 0; i < m_connections.length; i++) {
                    //TODO reduce the number of async requests and or parallize!!
                    ConnectionContainerUI cc = m_connections[i];
                    CompletableFuture<ConnectionContainerUI> future =
                        wfm.addConnectionAsync(cc.getSource(), cc.getSourcePort(), cc.getDest(), cc.getDestPort(),
                            cc.getUIInfo() != null ? cc.getUIInfo().getAllBendpoints() : null).getUnderlyingFuture();
                    futures[i] = future;
                }
                //combine futures and refresh workflow when all are completed
                return CompletableFuture.allOf(futures).thenCompose(f -> wfm.refresh(false));
            };
            if (pasteFuture != null) {
                return pasteFuture.thenCompose(c -> addConnections.get());
            } else {
                return addConnections.get();
            }
        }, hostWFM, "Pasting workflow content ...");
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        m_undoCopy = null;
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

    /** @return the number of workflow annotations to be deleted. */
    public int getAnnotationCount() {
        return m_annotationIDs.length;
    }
}
