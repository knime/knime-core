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
 * History
 *   May 15, 2020 (hornm): created
 */
package org.knime.core.node.workflow.virtual.parchunk;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.exec.dataexchange.PortObjectRepository;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.FlowScopeContext;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowCaptureOperation;
import org.knime.core.node.workflow.virtual.AbstractPortObjectRepositoryNodeModel;
import org.knime.core.node.workflow.virtual.VirtualNodeContext;

/**
 * Marks a virtual scope, i.e. a scope (a set of nodes) that is not permanently present and deleted after the execution
 * of all contained nodes.
 *
 * A virtual scope is marked by the {@link VirtualParallelizedChunkPortObjectInNodeModel} and
 * {@link VirtualParallelizedChunkPortObjectOutNodeModel}.
 *
 * With the disposal of the 'virtual' workflow/scope, all port objects created in that workflow (except those that are
 * passed to the virtual output node, {@link VirtualParallelizedChunkPortObjectOutNodeModel}), are disposed, too and not
 * accessible anymore after the finished execution. This, however, is not always desired (see the example use case
 * description below) and there is a mechanism that allows one to keep and store selected port objects with the node
 * model that controls the virtual workflow execution (called 'host node'): Right before the virtual workflow is
 * executed,
 * {@link FlowVirtualScopeContext#registerHostNode(org.knime.core.node.workflow.NativeNodeContainer, org.knime.core.node.ExecutionContext)}
 * needs to be called with 'host node' as parameter. As a result, some port objects (which port objects exactly is not
 * controlled by the 'host node') are automatically added to the list of port objects of the host node (via
 * {@link AbstractPortObjectRepositoryNodeModel#addPortObject(UUID, PortObject)}) and subsequently managed (i.e. saved
 * and loaded) by the host node. The port objects are made available to other downstream nodes by registering them at
 * the {@link PortObjectRepository}.
 *
 * An example use case is Integrated Deployment and the Workflow Executor (or the Parallel Chunk Loop): In case the
 * Workflow Executor is supposed to execute a workflow that in turn captures another workflow (i.e. uses the 'Capture
 * Workflow Start' and 'Capture Workflow End' nodes). If this to be captured workflow now has a 'static' input directly
 * connected into the scope, they are usually referenced from the captured workflow segment by their node-id and port
 * index. This, however, is not possible here since the referenced node won't exist anymore after the successful
 * execution of the virtual workflow. Thus, the capture-logic (see {@link WorkflowCaptureOperation}) makes sure that, if
 * a workflow is captured within a virtual (i.e. temporary) workflow, the port objects, which are referenced from the
 * captured workflow segment, are registered in the {@link PortObjectRepository} and passed to the host node of the
 * virtual workflow (i.e. this node model) for persistence. By that, those 'static' inputs are still available to
 * downstream nodes operating on the (in a virtual workflow) captured workflow segment, such as the Workflow Executor or
 * Workflow Writer nodes.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class FlowVirtualScopeContext extends FlowScopeContext implements VirtualNodeContext {

    private NativeNodeContainer m_nc;

    private ExecutionContext m_exec;

    private final Path m_dataAreaPath;

    private final Set<Restriction>  m_restrictions;

    /**
     * New instance without imposing any {@link Restriction}s on the nodes being executed in the scope.
     */
    public FlowVirtualScopeContext() {
        m_restrictions = Set.of();
        m_dataAreaPath = null;
    }

    /**
     * New instance with custom restrictions and a data area path supplier.
     *
     * @param owner see {@link #setOwner(NodeID)}
     * @param dataAreaPath the virtual data area path - can be {@code null}
     * @param restrictions set of restrictions that should apply for the scope
     */
    public FlowVirtualScopeContext(final NodeID owner, final Path dataAreaPath, final Restriction... restrictions) {
        setOwner(owner);
        m_dataAreaPath = dataAreaPath;
        m_restrictions = Set.of(restrictions);
    }

    /**
     * Allows one to register a node with this virtual scope context. This registered node is, among other things, used
     * to persist (and thus keep) port objects which would have otherwise be gone once the virtual execution of the
     * underlying workflow is finished successfully.
     *
     * This method needs to be called right before the nodes of this virtual scope are executed.
     *
     * This node container does not need to be provided if the start node of this virtual scope is connected (upstream)
     * to another loop start (as it is the case with the parallel chunk loop start node). In all other case it must be
     * set.
     *
     * @param hostNode the node the virtual scope is associated with; the node is, e.g., used for persistence of
     *            selected port objects and to provide a file store handler (its node model needs to be of type
     *            {@link AbstractPortObjectRepositoryNodeModel} in that case)
     * @param exec the host node's execution context, mainly used to copy port objects (which are then made available
     *            via the {@link PortObjectRepository})
     */
    public void registerHostNode(final NativeNodeContainer hostNode, final ExecutionContext exec) {
        m_nc = hostNode;
        m_exec = exec;
    }

    /**
     * @return whether a host node has been registered via
     *         {@link #registerHostNode(NativeNodeContainer, ExecutionContext)} or not
     *
     * @since 5.5
     */
    public boolean hasHostNode() {
        return m_nc != null;
    }

    /**
     * @return the file store handler to be used within the virtual scope, or an empty optional if no host node has been
     *         registered via {@link #registerHostNode(NativeNodeContainer, ExecutionContext)}
     * @throws IllegalStateException if the host node is there but has been reset
     *
     * @since 5.5
     */
    public Optional<IFileStoreHandler> createFileStoreHandler() {
        if (m_nc == null) {
            return Optional.empty();
        }
        if (m_nc.getNodeContainerState().isExecuted()) {
            // we don't want to permanently keep the file stores for an already executed node
            // because those are guaranteed to be not needed anymore downstream
            var fsh = NotInWorkflowWriteFileStoreHandler.create();
            fsh.open();
            return Optional.of(fsh);
        }

        var fsh = m_nc.getNode().getFileStoreHandler();
        if (fsh == null) {
            // can happen if the node associated with the virtual scope is reset
            throw new IllegalStateException(
                "No file store handler given. Try to re-execute '" + m_nc.getNameWithID() + "'");
        }
        return Optional.of(fsh);
    }

    /**
     * Adds a port object to the {@link PortObjectRepository} (to be available to downstream nodes) and the host node
     * (assuming its node model is of type {@link AbstractPortObjectRepositoryNodeModel}) for persistence.
     *
     * The host node is registered via {@link #registerHostNode(NativeNodeContainer, ExecutionContext)}.
     *
     * @param po the port object to be added to the {@link PortObjectRepository} and published to the host node
     * @return the id of the port object in the {@link PortObjectRepository}
     * @throws CanceledExecutionException
     * @throws IOException
     *
     * @throws IllegalStateException if there is no host node associated with the virtual scope or host node's node
     *             model is not a {@link AbstractPortObjectRepositoryNodeModel}
     */
    public UUID addPortObjectToRepositoryAndHostNode(final PortObject po)
        throws IOException, CanceledExecutionException {
        if (m_nc == null) {
            throw new IllegalStateException("No host node to forward the port objects to set");
        }

        if (m_nc.getNodeModel() instanceof AbstractPortObjectRepositoryNodeModel poRepoNodeModel) {
            UUID id = PortObjectRepository.addCopy(po, m_exec);
            poRepoNodeModel.addPortObject(id, PortObjectRepository.get(id).get()); // NOSONAR
            return id;
        } else {
            throw new IllegalStateException(
                "Host node's node model is not a " + AbstractPortObjectRepositoryNodeModel.class.getSimpleName());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRestriction(final Restriction restriction) {
        return m_restrictions.contains(restriction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Path> getVirtualDataAreaPath() {
        return Optional.ofNullable(m_dataAreaPath);
    }

}
