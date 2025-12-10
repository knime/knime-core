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
 *   Feb 15, 2021 (hornm): created
 */
package org.knime.core.node.workflow.capture;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowDataRepository;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.LocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.Pair;
import org.knime.core.util.ThreadPool;

/**
 * Entry point to create workflow segment executor instances - see
 * {@link #builder(NativeNodeContainer, ExecutionMode, String, Consumer, ExecutionContext, boolean)}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class WorkflowSegmentExecutor {

    private WorkflowSegmentExecutor() {
        // utility class
    }

    /**
     * Entry method to create workflow segment executor instances via a builder.
     *
     * It allows on to either create an 'isolated' executor (see {@link Builder#isolated(boolean)}) or 'combined'
     * executor (see {@link Builder#combined(PortObject[])} - defining different execution strategies when multiple
     * workflow segments are to be executed in succession.
     *
     * @param hostNode the node which is responsible for the execution of the workflow segment (which provides the input
     *            and receives the output data, supplies the file store, etc.)
     * @param mode the workflow segment {@link ExecutionMode}
     * @param workflowName the name of the metanode to be created (which will only be visible if 'debug' is
     *            <code>true</code>)
     * @param loadWarningConsumer callback for warning if there have while loading the workflow from the workflow
     *            segment
     * @param exec for cancellation
     * @param collectMessages whether to collect node messages after successful execution or not (will be part of the
     *            execution result) - if the execution fails, messages are always collected
     * @return the builder to further configure the workflow segment executor
     * @since 5.9
     */
    public static Builder builder(final NativeNodeContainer hostNode, final ExecutionMode mode,
        final String workflowName, final Consumer<String> loadWarningConsumer, final ExecutionContext exec,
        final boolean collectMessages) {
        return new Builder(new BuilderParams(hostNode, mode, workflowName, loadWarningConsumer, exec, collectMessages));
    }

    /**
     *  The builder for creating different types of workflow segment executors.
     */
    public static final class Builder {

        private final BuilderParams m_params;

        private Builder(final BuilderParams params) {
            m_params = params;
        }

        /**
         * Creates an isolated executor which executes each the workflow segment in isolated temporary workflow project.
         *
         * @param executeAll if <code>true</code> all nodes in the segment are executed (which is new behavior),
         *            previously and if <code>false</code> only the output nodes would be executed
         *
         * @return the builder for the isolated executor
         */
        public IsolatedExecutor.Builder isolated(final boolean executeAll) {
            return new IsolatedExecutor.Builder(m_params, executeAll);
        }

        /**
         * Creates a combined executor which executes the workflow segment as part of the same workflow by connecting
         * them together.
         *
         * This executor will create the combined workflow to be used for workflow segment execution on
         * {@link org.knime.core.node.workflow.capture.CombinedExecutor.Builder#build()}.
         *
         * @param initialInputs the initial input port objects as source for the combined workflow used for execution
         * @return the builder for the combined executor
         */
        public CombinedExecutor.Builder combined(final PortObject[] initialInputs) {
            return new CombinedExecutor.Builder(m_params, initialInputs);
        }

        /**
         * Creates a combined executor which executes the workflow segment as part of the same workflow by connecting
         * them together.
         *
         * This executor won't create the workflow used for execution anew, but instead uses the provided one.
         *
         * @param combinedWorkflow an already existing workflow to be used for workflow segment execution
         * @return the builder for the combined executor
         */
        public CombinedExecutor.Builder combined(final WorkflowManager combinedWorkflow) {
            return new CombinedExecutor.Builder(m_params, combinedWorkflow);
        }

    }

    record BuilderParams(NativeNodeContainer hostNode, ExecutionMode mode, String workflowName,
        Consumer<String> loadWarningConsumer, ExecutionContext exec, boolean collectMessages) {

    }

    /**
     * Controls how the workflow segment is executed.
     */
    public enum ExecutionMode {
            /**
             * Executed as a visible metanode next to the host node. Usually used for debugging purposes.
             */
            DEBUG,
            /**
             * Executed as invisible metanode next to the host node.
             */
            DEFAULT,
            /**
             * Executed as a metanode in a separate, temporary, workflow project. This execution mode doesn't have any
             * impact on the execution state of the parent workflow (e.g. component workflow) of the host node.
             */
            DETACHED;

    }

    static WorkflowManager createTemporaryWorkflowProject(final WorkflowDataRepository workflowDataRepository,
        final WorkflowContextV2 orgContext) {
        var mountpoint = ClassUtils.castOptional(AnalyticsPlatformExecutorInfo.class, orgContext.getExecutorInfo())//
            .flatMap(info -> info.getMountpoint()) //
            .map(mp -> Pair.create(mp.getFirst().getAuthority(), mp.getSecond())).orElse(Pair.create(null, null));
        var creationHelper =
            new WorkflowCreationHelper(createContext(orgContext.getExecutorInfo().getLocalWorkflowPath(),
                mountpoint.getFirst(), mountpoint.getSecond(), orgContext.getLocationInfo()));
        creationHelper.setWorkflowDataRepository(workflowDataRepository);
        return WorkflowManager.ROOT.createAndAddProject("workflow_segment_executor", creationHelper);
    }

    private static WorkflowContextV2 createContext(final Path workflowFolderPath, final String mountId,
        final Path mountpointRoot, final LocationInfo locationInfo) {
        return WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> {
                var res = exec //
                    .withCurrentUserAsUserId() //
                    .withLocalWorkflowPath(workflowFolderPath);
                if (mountId != null) {
                    res.withMountpoint(mountId, mountpointRoot);
                }
                return res;
            }) //
            .withLocation(locationInfo) //
            .build();
    }

    static void executeAndWait(final NativeNodeContainer hostNode, final WorkflowManager wfm,
        final ExecutionContext exec, final NodeID virtualEndID, final AtomicReference<Exception> exception) {
        // code copied from SubNodeContainer#executeWorkflowAndWait
        final Runnable inBackgroundRunner = () -> {
            executeThisSegmentWithoutWaiting(wfm, virtualEndID);
            try {
                waitWhileInExecution(wfm, exec);
            } catch (InterruptedException | CanceledExecutionException e) { // NOSONAR
                wfm.cancelExecution(hostNode);
                Thread.currentThread().interrupt();
            }
        };
        final var currentPool = ThreadPool.currentPool();
        if (currentPool != null) {
            // ordinary workflow execution
            try {
                currentPool.runInvisible(Executors.callable(inBackgroundRunner::run));
            } catch (ExecutionException ee) {
                exception.compareAndSet(null, ee);
                NodeLogger.getLogger(WorkflowSegmentExecutor.class).error(
                    ee.getCause().getClass().getSimpleName() + " while waiting for to-be-executed workflow to complete",
                    ee);
            } catch (final InterruptedException e) { // NOSONAR interrupt is handled by WFM cancellation
                wfm.cancelExecution();
            }
        } else {
            // streaming execution
            inBackgroundRunner.run();
        }
    }

    private static void executeThisSegmentWithoutWaiting(final WorkflowManager wfm, final NodeID virtualEndID) {
        if (virtualEndID != null) {
            wfm.executeUpToHere(virtualEndID);
        } else {
            wfm.executeAll();
        }
    }

    private static void waitWhileInExecution(final WorkflowManager wfm, final ExecutionContext exec)
        throws InterruptedException, CanceledExecutionException {
        while (wfm.getNodeContainerState().isExecutionInProgress()) {
            wfm.waitWhileInExecution(1, TimeUnit.SECONDS);
            exec.checkCanceled();
        }
    }

    static List<FlowVariable> getFlowVariablesFromNC(final SingleNodeContainer nc) {
        Stream<FlowVariable> res;
        if (nc instanceof NativeNodeContainer) {
            res = ((NativeNodeContainer)nc).getNodeModel()
                .getAvailableFlowVariables(VariableTypeRegistry.getInstance().getAllTypes()).values().stream();
        } else {
            res = nc.createOutFlowObjectStack().getAllAvailableFlowVariables().values().stream();
        }
        return res.filter(fv -> fv.getScope() == Scope.Flow).collect(Collectors.toList());
    }

    /*
     * Remove file stores that aren't needed anymore because they aren't part of any of the port objects
     * (either as file store cell or file store port object).
     */
    //private static void removeSuperfluousFileStores(final Stream<PortObject> portObjects) {
    // TODO
    // see ticket https://knime-com.atlassian.net/browse/AP-14414
    // m_thisNode.getNode().getFileStoreHandler();
    // ...
    //}

    /*
     * Essentially only take the flow variables coming in via the 2nd to nth input port (and ignore flow var (0th)
     * and workflow (1st) port). Otherwise those will always take precedence and can possibly
     * interfere with the workflow being executed.
     */
    static List<FlowVariable> collectOutputFlowVariablesFromUpstreamNodes(final NodeContainer thisNode) {
        // skip flow var (0th) and workflow (1st) input port
        WorkflowManager wfm = thisNode.getParent();
        List<FlowVariable> res = new ArrayList<>();
        for (int i = 2; i < thisNode.getNrInPorts(); i++) {
            ConnectionContainer cc = wfm.getIncomingConnectionFor(thisNode.getID(), i);
            NodeID sourceId = cc.getSource();
            SingleNodeContainer snc;
            if (sourceId.equals(wfm.getID())) {
                // if upstream port is the 'inner' output port of a metanode
                snc = wfm.getWorkflowIncomingPort(cc.getSourcePort()).getConnectedNodeContainer();
            } else {
                NodeContainer nc = wfm.getNodeContainer(sourceId);
                if (nc instanceof WorkflowManager) {
                    // if the upstream node is a metanode
                    snc = ((WorkflowManager)nc).getOutPort(cc.getSourcePort()).getConnectedNodeContainer();
                } else {
                    snc = (SingleNodeContainer)nc;
                }
            }
            List<FlowVariable> vars = getFlowVariablesFromNC(snc);
            // reverse the order of the flow variables in order to preserve the original order
            ListIterator<FlowVariable> reverseIter = vars.listIterator(vars.size());
            while (reverseIter.hasPrevious()) {
                res.add(reverseIter.previous());
            }
        }
        return res;
    }

    static void cancel(final WorkflowManager wfm) {
        if (wfm.getNodeContainerState().isExecutionInProgress()) {
            wfm.cancelExecution(wfm);
        }
        try {
            wfm.waitWhileInExecution(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            // should never happen
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Represents messages of nodes containing name, node ID, error or warning message and WorkflowSegmentNodeMessages
     * of nested nodes.
     *
     * @param nodeName the node name
     * @param nodeID the node ID
     * @param message the error message of the node
     * @param recursiveMessages a list of node messages for nested nodes if the node is a container, otherwise an empty
     *            list
     *
     * @since 5.4
     */
    public record WorkflowSegmentNodeMessage(String nodeName, NodeID nodeID, NodeMessage message,
        List<WorkflowSegmentNodeMessage> recursiveMessages) {

        /**
         * Helper to aggregate a single error message based on the node error message in execution result.
         *
         * @param nodeMessages
         *
         * @return the compiled error message
         *
         * @since 5.9
         */
        public static String compileSingleErrorMessage(final List<WorkflowSegmentNodeMessage> nodeMessages) {
            var errorMessages = nodeMessages.stream().toList();
            // determine the number of failed nodes that are not containers
            List<WorkflowSegmentNodeMessage> leafErrorMessages = new ArrayList<>();
            for (WorkflowSegmentNodeMessage message : errorMessages) {
                recursivelyExtractLeafNodeErrorMessages(message, leafErrorMessages);
            }
            return constructErrorMessage(leafErrorMessages.size(), errorMessages);
        }

        private static void recursivelyExtractLeafNodeErrorMessages(final WorkflowSegmentNodeMessage message,
            final List<WorkflowSegmentNodeMessage> result) {
            if (message.message().getMessageType() == Type.RESET) {
                return;
            }
            if (message.recursiveMessages().isEmpty()) {
                result.add(message);
            } else {
                for (WorkflowSegmentNodeMessage nestedMessage : message.recursiveMessages()) {
                    recursivelyExtractLeafNodeErrorMessages(nestedMessage, result);
                }
            }
        }

        private static String constructErrorMessage(final int numberOfFailedNodes,
            final List<WorkflowSegmentNodeMessage> errorMessages) {
            String iNodes;
            if (numberOfFailedNodes == 1) {
                iNodes = "one node";
            } else {
                iNodes = String.valueOf(numberOfFailedNodes) + " nodes";
            }
            return String.format("Workflow contains %s with execution failure:%n%s", iNodes, errorMessages.stream()//
                .map(msg -> recursivelyConstructErrorMessage(msg, ""))//
                .collect(Collectors.joining(",\n")));
        }

        private static String recursivelyConstructErrorMessage(final WorkflowSegmentNodeMessage message,
            final String prefix) {
            if (message.recursiveMessages().isEmpty()) {
                return prefix + message.nodeName() + " #" + message.nodeID().getIndex() + ": "
                    + removeErrorPrefix(message.message().getMessage());
            } else {
                String newPrefix = prefix + message.nodeName() + " #" + message.nodeID().getIndex() + " > ";
                return message.recursiveMessages().stream().filter(msg -> msg.message().getMessageType() == Type.ERROR)
                    .map(ms -> recursivelyConstructErrorMessage(ms, newPrefix)).collect(Collectors.joining(",\n"));
                // WorkflowSegmentNodeMessage of type ERROR can contain messages of type WARNING
            }
        }

        private static String removeErrorPrefix(final String msg) {
            if (msg.startsWith(Node.EXECUTE_FAILED_PREFIX)) {
                return StringUtils.removeStart(msg, Node.EXECUTE_FAILED_PREFIX);
            }
            return msg;
        }
    }

    static List<WorkflowSegmentNodeMessage> recursivelyExtractNodeMessages(final NodeContainer nc) {
        if (nc instanceof NativeNodeContainer) {
            return Collections.emptyList();
        }
        WorkflowManager wfm = null;
        if (nc instanceof WorkflowManager w) {
            wfm = w;
        } else if (nc instanceof SubNodeContainer snc) {
            wfm = snc.getWorkflowManager();
        } else {
            throw new IllegalStateException("Received unexpected NodeContainer.");
        }
        return wfm.getNodeContainers().stream()
            .map(n -> new WorkflowSegmentNodeMessage(n.getName(), n.getID(), n.getNodeMessage(),
                recursivelyExtractNodeMessages(n)))
            .filter(msg -> (!msg.recursiveMessages().isEmpty()) || (msg.message().getMessageType() == Type.ERROR)
                || (msg.message().getMessageType() == Type.WARNING))
            .toList();
    }

}
