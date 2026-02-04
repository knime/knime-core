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
 *   Mar 28, 2014 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Interface used by embedded {@link WorkflowManager} instances to invoke actions on the parent item. The parent
 * item is a {@link WorkflowManager} for metanodes or projects. It is a {@link SubNodeContainer} for the instance
 * that is used inside a sub node.
 *
 * <p>None of the methods are meant be public API.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.10
 */
public interface NodeContainerParent {

    /** See {@link NodeContainer#getDirectNCParent()}.
     * @return the direct node container parent.
     */
    public NodeContainerParent getDirectNCParent();

    /**
     * Pulls {@link NodeContainer#findJobManager()} into parent interface.
     * @since 2.12
     */
    public NodeExecutionJobManager findJobManager();

    /** If a contained node contained (independent of its connections). Generally true but subnodes disallow
     * node reset if there are downstream executing nodes.
     * @return that property.
     */
    public boolean canResetContainedNodes();

    /**
     * For the special case this parent is a {@link SubNodeContainer}, or this parent is contained in one,
     * and that container has downstream nodes in execution, return <code>true</code>. Used to determine if
     * content in a SNC can be modified, whereby "downstream" might not be directly connected to the node in question.
     *
     * @return this property.
     * @since 5.3
     */
    boolean canModifyStructure();

    /** May return false for nodes contained in subnodes, which cannot be configured until all data is available
     * to the subnode. Used to ensure the user doesn't execute nodes in a subnode unless all data is available. For
     * ordinary workflows/metanodes it always returns true.
     * @return that property */
    public boolean canConfigureNodes();

    /** @return the ReentrantLock underlying the {@link WorkflowLock} instance used by this NC parent. */
    public ReentrantLock getReentrantLockInstance();

    /** Locks and returns the {@link WorkflowLock} associated with this workflow. This should always be used in a
     * try-with-resources statement:
     * <pre>
     * try (WorkflowLock lock = lock()) {
     *    ...
     * }
     * </pre>
     * @return The workflow lock instance, freshly locked (see {@link ReentrantLock#lock()}.
     */
    public WorkflowLock lock();

    /** @return true if the calling thread has acquired the workflow lock either via {@link #lock()} or by
     * locking the {@link #getReentrantLockInstance() ReentrantLock}. Mainly used for assertions.
     */
    public boolean isLockedByCurrentThread();

    /**
     * This flag is only a hint for the UI -- none of the add/remove operations will read this flag.
     *
     * @return Whether edit operations are not permitted.
     */
    public boolean isWriteProtected();

    /** Private routine which assembles a stack of workflow variables all
     * the way to the top of the workflow hierarchy.
     */
    public void pushWorkflowVariablesOnStack(final FlowObjectStack sos);

    /** @return the cipher (used in persistor). */
    public WorkflowCipher getWorkflowCipher();

    /** Returns the argument string unless this workflow or any of the parent wfms is encrypted.
     * Then it appends ".encrypted" to the argument.
     * @param fileName Suggest file name
     * @return fileName, possibly appended with ".encrypted".
     * @noreference This method is not intended to be referenced by clients.
     */
    public abstract String getCipherFileName(final String fileName);

    /** @param out The output
     * @return see {@link WorkflowCipher#cipherOutput(WorkflowManager, OutputStream)}.
     * @throws IOException If fails
     * @noreference This method is not intended to be referenced by clients. */
    @SuppressWarnings("javadoc")
    public abstract OutputStream cipherOutput(final OutputStream out) throws IOException;

    /**
     * @return The root workflow manager of the project containing this workflow manager
     */
    public abstract WorkflowManager getProjectWFM();

    /** Called by children when they are set dirty. */
    public void setDirty();

    /**
     * Allows a subnode container to remove irrelevant errors from the collected list of node errors, specifically the
     * virtual output node will only mirror errors. The default implementation does nothing.
     *
     * @param messageMap The map of contained nodes to their collected messages.
     * @since 5.1
     */
    default void postProcessNodeErrors(final Map<NodeContainer, String> messageMap) {
        // empty
    }

    /**
     * Checks whether this object (i.e. a workflow or subnode) or another parent up the hierarchy is in wizard execution
     * mode.
     *
     * @return <code>true</code> if the workflow or subnode (or one of its parents) is executed in wizard execution
     *         (i.e. step by step), otherwise <code>false</code>
     * @since 3.6
     */
    default public boolean isInWizardExecution() {
        return false;
    }

    /**
     * Determines whether this container parent is a project (i.e. the node container at the 'upper-most' level).
     *
     * @return <code>true</code> if this node container is a project, otherwise <code>false</code>
     * @since 4.2
     */
    public default boolean isProject() {
        return false;
    }

    /**
     * Determines the 'upper-most' parent workflow manager of the given node container. I.e. the workflow manager
     * returns {@code true} for {@link WorkflowManager#isProject()} or {@link WorkflowManager#isComponentProjectWFM()}.
     *
     * @param nc the node container to find the parent for
     *
     * @return the project workflow manager of the give node container
     * @since 4.2
     */
    public static WorkflowManager getProjectWFM(final NodeContainer nc) {
        // find the actual workflow and not the metanode the container may be in
        NodeContainerParent parent = nc instanceof WorkflowManager ? (WorkflowManager)nc : nc.getDirectNCParent();

        while (!parent.isProject()) {
            assert parent != null : "Parent item can't be null as a project parent is expected";
            parent = parent.getDirectNCParent();
        }
        return (parent instanceof WorkflowManager) ? (WorkflowManager)parent
            : ((SubNodeContainer)parent).getWorkflowManager();
    }

    /**
     * @return metadata, may be {@code null}
     * @since 5.1
     */
    default NodeContainerMetadata getMetadata() {
        return null;
    }

    /**
     * Sets the metadata of this workflow or component.
     *
     * @param updatedMetadata updated metadata, {@code null} fur deletion
     * @since 5.1
     */
    default void setContainerMetadata(final NodeContainerMetadata updatedMetadata) {
        throw new IllegalStateException("Node container does not support metadata: " + getClass().getSimpleName());
    }
}
