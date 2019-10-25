/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.ui.node.workflow.async;

import java.util.concurrent.CompletableFuture;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.node.workflow.WorkflowCopyUI;
import org.knime.core.ui.node.workflow.WorkflowCopyWithOffsetUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;

/**
 * UI-interface that provides asynchronous versions of some methods of {@link WorkflowManagerUI} - see {@link AsyncUI}.
 *
 * The asynchronous workflow manager also adds methods to refresh the workflow, set a workflow disconnected, etc.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface AsyncWorkflowManagerUI extends WorkflowManagerUI, AsyncNodeContainerUI {

    /**
     * {@inheritDoc}
     */
    @Override
    default void remove(final NodeID[] nodeIDs, final ConnectionID[] connectionIDs,
        final WorkflowAnnotationID[] annotationIDs) {
        throw new UnsupportedOperationException("Please use the async method instead.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default NodeID createAndAddNode(final NodeFactory<?> factory, final NodeUIInformation uiInfo) {
        throw new UnsupportedOperationException("Please use the async method instead.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default NodeID createAndAddNode(final NodeFactory<?> factory, final NodeUIInformation uiInfo,
        final ModifiableNodeCreationConfiguration creationConfig) {
        throw new UnsupportedOperationException("Please use the async method instead.");
    }

    /**
     * Async version of {@link #createAndAddNode(NodeFactory, NodeUIInformation)}.
     *
     * @param factory
     * @param uiInfo
     * @return the result as a future
     */
    CompletableFuture<NodeID> createAndAddNodeAsync(NodeFactory<?> factory, final NodeUIInformation uiInfo);

    /**
     * Async version of {@link #remove(NodeID[], ConnectionContainerUI[], WorkflowAnnotationID[])}.
     *
     * @param nodeIDs
     * @param connectionIDs
     * @param annotationIDs
     * @return void as future - throws a {@link OperationNotAllowedException} if operation couldn't be performed (e.g.
     *         because there are executing node's successors) on {@link CompletableFutureEx#getOrThrow()}
     */
    CompletableFutureEx<Void, OperationNotAllowedException> removeAsync(final NodeID[] nodeIDs,
        final ConnectionID[] connectionIDs, WorkflowAnnotationID[] annotationIDs);

    /**
     * {@inheritDoc}
     */
    @Override
    default ConnectionContainerUI addConnection(final NodeID source, final int sourcePort, final NodeID dest,
        final int destPort, final int[]... bendpoints) {
        throw new UnsupportedOperationException("Please use the async method instead.");
    }


    /**
     * Async version of {@link #addConnection(NodeID, int, NodeID, int)}.
     *
     * @param source
     * @param sourcePort
     * @param dest
     * @param destPort
     * @param bendpoints
     * @return result as future - throws a {@link OperationNotAllowedException} if operation couldn't be performed on
     *         {@link CompletableFutureEx#getOrThrow()}
     */
    CompletableFutureEx<ConnectionContainerUI, OperationNotAllowedException> addConnectionAsync(final NodeID source,
        final int sourcePort, final NodeID dest, final int destPort, int[]... bendpoints);

    /**
     * {@inheritDoc}
     */
    @Override
    default WorkflowCopyUI copy(final WorkflowCopyContent content) {
        throw new UnsupportedOperationException("Please use the async method instead.");
    }

    /**
     * Async version of {@link #copy(boolean, WorkflowCopyContent)}.
     *
     * @param isUndoableDeleteCommand
     * @param content
     * @return result as future
     */
    CompletableFuture<WorkflowCopyWithOffsetUI> copyAsync(WorkflowCopyContent content);

    /**
     * {@inheritDoc}
     */
    @Override
    default WorkflowCopyUI cut(final WorkflowCopyContent content) {
        throw new UnsupportedOperationException("Please use the async method instead.");
    }

    /**
     * Async version of {@link #cut(WorkflowCopyContent)}.
     *
     * @param content
     * @return result as future - throws a {@link OperationNotAllowedException} if operation couldn't be performed (e.g.
     *         because there are executing node's successors) on {@link CompletableFutureEx#getOrThrow()}
     */
    CompletableFutureEx<WorkflowCopyWithOffsetUI, OperationNotAllowedException>
        cutAsync(final WorkflowCopyContent content);

    /**
     * {@inheritDoc}
     */
    @Override
    default WorkflowCopyContent paste(final WorkflowCopyUI workflowCopy) {
        throw new UnsupportedOperationException("Please use the async method instead.");
    }


    /**
     * Async version of {@link #paste(WorkflowCopyUI)}.
     *
     * @param workflowCopy
     * @return result as future
     */
    CompletableFuture<WorkflowCopyContent> pasteAsync(WorkflowCopyWithOffsetUI workflowCopy);


    /**
     * {@inheritDoc}
     *
     * Narrow down return type to {@link AsyncNodeContainerUI}.
     */
    @Override
    AsyncNodeContainerUI getNodeContainer(NodeID id);


    /* --------
     * New methods that don't replace an existing one in WorkflowManagerUI
     * but are specific to AsynchWorkflowManager
     * -------- */

    /**
     * Refreshes the workflow (e.g. downloads the new state) asynchronously.
     *
     * If a job is swapped it will be copied back to memory.
     *
     * @param deepRefresh if <code>true</code> the workflow itself and all contained sub-workflows (i.e. metanodes) will
     *            be refreshed, if <code>false</code> only the top level workflow will be refreshed.
     * @return a future for async use
     */
    CompletableFuture<Void> refreshAsync(final boolean deepRefresh);

    /**
     * Refreshes the workflow (e.g. downloads the new state) and returns when done or fails with an
     * {@link SnapshotNotFoundException}.
     *
     * @param deepRefresh if <code>true</code> the workflow itself and all contained sub-workflows (i.e. metanodes) will
     *            be refreshed, if <code>false</code> only the top level workflow will be refreshed
     * @throws SnapshotNotFoundException if refresh is not possible because the locally hold snapshot is not known to the
     *             server (anymore), see {@link SnapshotNotFoundException} for possible reasons
     */
    void refreshOrFail(final boolean deepRefresh) throws SnapshotNotFoundException;

    /**
     * Sets the disconnected-status of the workflow manager. I.e. in case the workflow manager implementation is just a
     * client and has lost the connection to the server.
     *
     * @param disconnected <code>true</code> if disconnected, otherwise <code>false</code>
     */
    void setDisconnected(final boolean disconnected);

    /**
     * @param listener listener that gets informed when the write protection status has changed. The actual status can
     *            be accessed via {@link #isWriteProtected()}.
     */
    void addWriteProtectionChangedListener(final Runnable listener);

    /**
     * @param listener the listener to be removed and to not be called anymore
     */
    void removeWriteProtectionChangedListener(final Runnable listener);
}