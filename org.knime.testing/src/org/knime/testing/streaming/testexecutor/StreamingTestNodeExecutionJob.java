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
 *   Oct 14, 2015 (hornm): created
 */
package org.knime.testing.streaming.testexecutor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainerException;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.exec.SandboxedNodeCreator;
import org.knime.core.node.message.Message;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortObjectOutput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeExecutionJob;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.execresult.NativeNodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;
import org.knime.core.node.workflow.execresult.NodeExecutionResult;
import org.knime.core.util.ThreadPool;
import org.knime.testing.streaming.testexecutor.noopexecutor.NoopExecutionJobManager;

/**
 * NodeExecutionJob that tests all streaming and distributed execution related functionsRoles()}<br>
 * TODO: use of the {@link MergeOperator#isHierarchical()}-method<br>
 * * TODO: little TODO's in the code
 *
 * @author Martin Horn, University of Konstanz
 */
public class StreamingTestNodeExecutionJob extends NodeExecutionJob {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(StreamingTestNodeExecutionJobManager.class);

    private int m_numChunks;

    private Future<?> m_future;

    private final List<String> m_warningMessages = new ArrayList<String>(3);

    private final List<BufferedDataContainer> m_tableChunksToBeDisposed = new ArrayList<BufferedDataContainer>();

    /**
     * @param nc
     * @param data
     * @param numChunks
     */
    protected StreamingTestNodeExecutionJob(final NodeContainer nc, final PortObject[] data, final int numChunks) {
        super(nc, data);
        m_numChunks = numChunks;
    }

    /**
     * Set the future that represents the pending execution.
     *
     * @param future the future to set
     */
    void setFuture(final Future<?> future) {
        m_future = future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReConnecting() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeContainerExecutionStatus mainExecute() {
        NodeContainer nodeContainer = getNodeContainer();
        if (!(nodeContainer instanceof NativeNodeContainer)) {
            String message =
                "Streaming and distributed TEST execution only available for native nodes (i.e. no meta- or subnodes)";
            nodeContainer.setNodeMessage(new NodeMessage(Type.ERROR, message));
            LOGGER.error(message);
            return NodeContainerExecutionStatus.FAILURE;
        }

        //TODO should actually not be used for execution itself, but is currently!
        NativeNodeContainer localNodeContainer = (NativeNodeContainer)nodeContainer;

        if (localNodeContainer.getNodeModel() instanceof LoopStartNode
            || localNodeContainer.getNodeModel() instanceof LoopEndNode) {
            String message = "Streaming and distributed TEST execution doesn't work for Loop Start and End nodes.";
            nodeContainer.setNodeMessage(new NodeMessage(Type.ERROR, message));
            LOGGER.error(message);
            return NodeContainerExecutionStatus.FAILURE;
        }

        localNodeContainer.getNodeModel().addWarningListener(w -> {
            if (w != null) {
                m_warningMessages.add(w.getSummary());
            }
        });

        // get the input object specs
        PortObject[] inPortObjects = getPortObjects(); // includes the flow
                                                       // variable port object!
        PortObjectSpec[] inPortObjectSpecs = new PortObjectSpec[inPortObjects.length];
        for (int i = 1; i < inPortObjectSpecs.length; i++) { // without flow
                                                                 // variable port
                                                             //check if it's not an optional in-port
            if (inPortObjects[i] != null) {
                inPortObjectSpecs[i] = inPortObjects[i].getSpec();
            }
        }

        // get input port roles
        LOGGER.info("call local: NodeModel#getInputPortRoles");
        InputPortRole[] inputPortRoles = localNodeContainer.getNodeModel().getInputPortRoles();

        // get flow variables for all non-streamable ports
        // TODO: why only for non-streamable ports?
        // WorkflowManager wfm = localNodeContainer.getParent();
        // ArrayList<FlowObjectStack> flowObjectStacks = new
        // ArrayList<FlowObjectStack>(inPortObjects.length);
        // for (int i = 0; i < inPortObjects.length; i++) {
        // ConnectionContainer con =
        // wfm.getIncomingConnectionFor(localNodeContainer.getID(), i);
        // if ((con != null && i == 0) || (con != null && inputPortRoles[i -
        // 1].isStreamable())) {
        // flowObjectStacks.add(((SingleNodeContainer)wfm.getNodeContainer(con.getSource())).getFlowObjectStack());
        // }
        // }

        // check for distributable ports
        boolean isDistributable = false;
        for (int i = 0; i < inputPortRoles.length; i++) {
            if (inputPortRoles[i].isDistributable()) {
                isDistributable = true;
            }
        }

        /* ---- create node copies and configure ----*/

        //adjust the number of chunks if one of the distributable input table contains less rows than chunks
        final int numChunks = calcNumberOfChunks(inPortObjects, inputPortRoles, isDistributable);

        record Tuple( // NOSONAR (equals/hashcode in arrays)
            NativeNodeContainer[] remoteNodeContainers, //
            ExecutionContext[] remoteExec, //
            ExecutionContext localExec) {
        }

        Callable<Optional<Tuple>> callable = new Callable<>() {
            @Override
            public Optional<Tuple> call() {
                // locking node insertion and configuration as there might be concurrent workflow changes
                // (Component Output nodes will execute _all_ nodes as part of its own execution.)
                try (WorkflowLock lock = localNodeContainer.getParent().lock()) {
                    //create the 'remote' node containers used for the execution itself
                    NativeNodeContainer[] remoteNodeContainers = createNodeCopies(localNodeContainer, numChunks);

                    //exactly one execution context per 'remote' node
                    ExecutionContext[] remoteExec = createExecutionContexts(remoteNodeContainers);

                    //execution context for the original node
                    //- mainly for the creation of the input and output tables (to be fed into the 'remote' node copies)
                    //- created tables are tracked in m_tableChunksToBeDisposed to be disposed at the end
                    //- should actually not be used for the actual execution but is currently! (TODO)
                    ExecutionContext localExec = remoteExec[0];

                    // configure the node copies
                    for (int i = 0; i < remoteNodeContainers.length; i++) {
                        // wfm.createAndSetFlowObjectStackFor(localNodeContainer,
                        // flowObjectStacks.toArray(new
                        // FlowObjectStack[flowObjectStacks.size()]));
                        LOGGER.info("call remote: NodeModel#configure");
                        boolean isConfigureOK = remoteNodeContainers[i].callNodeConfigure(inPortObjectSpecs, true);
                        if (!isConfigureOK) {
                            String message = "Configuration failed";
                            nodeContainer.setNodeMessage(new NodeMessage(Type.ERROR, message));
                            LOGGER.error(message);
                            return Optional.empty();
                        }
                    }
                    return Optional.of(new Tuple(remoteNodeContainers, remoteExec, localExec));
                }
            }
        };


        final Tuple tuple;
        try {
            ThreadPool currentPool = ThreadPool.currentPool();
            Optional<Tuple> tupleOpt;
            if (ThreadPool.currentPool() != null) {
                tupleOpt = currentPool.runInvisible(callable);
            } else {
                tupleOpt = callable.call();
            }
            if (tupleOpt.isEmpty()) {
                // configuration failed
                return NodeContainerExecutionStatus.FAILURE;
            }
            tuple = tupleOpt.get();
        } catch (Exception e) { // NOSONAR (InterruptedException ... canceled in code)
            LOGGER.error(e.getMessage(), e);
            return NodeContainerExecutionStatus.FAILURE;
        }

        /* --- intermediate runs --- */

        //sanity check: if NodeModel#createInitialStreamableOperatorInternals" is overridden, the "NodeModel#iterate" should be overridden, too.
        //Otherwise it doesn't make sense.
        if (checkForOverriddenMethod(localNodeContainer, "createInitialStreamableOperatorInternals")
            && !checkForOverriddenMethod(localNodeContainer, "iterate", StreamableOperatorInternals.class)) {
            m_warningMessages.add(
                "Implementation warning: Overriding the 'createInitialStreamableOperatorInternals'-method without overriding the 'iterate'-method doesn't make sense.");
        }

        // create initial streamable operator internals for the first call of the iterate-method
        LOGGER.info("call local: NodeModel#createInitialStreamableOperatorInternals");
        StreamableOperatorInternals operatorInternals =
            localNodeContainer.getNodeModel().createInitialStreamableOperatorInternals();

        LOGGER.info("call local: NodeModel#createMergeOperator");
        // can be null
        MergeOperator localMergeOperator = localNodeContainer.getNodeModel().createMergeOperator();

        StreamableOperatorInternals[] newInternals = new StreamableOperatorInternals[numChunks];
        final PortObjectSpec[] inSpecsNoFlowPort = ArrayUtils.remove(inPortObjectSpecs, 0);
        LOGGER.info("call local: NodeModel#iterate");

        // Port types for determining whether a port must be copied or not in createPortInputs(...)
        PortType[] portTypes = new PortType[inPortObjects.length];
        // Skipping the variable port
        for (int i = 1; i < inPortObjects.length; i++) {
            portTypes[i - 1] = localNodeContainer.getInPort(i).getPortType();
        }

        try {
            // create port inputs for the streamable execution
            PortInput[][] portInputs =
                createPortInputs(inputPortRoles, inPortObjects, portTypes, numChunks, tuple.localExec);
            while (localNodeContainer.getNodeModel().iterate(operatorInternals)) {

                newInternals = performIntermediateIteration(tuple.remoteNodeContainers, tuple.remoteExec,
                    operatorInternals, inSpecsNoFlowPort, portInputs, numChunks, localMergeOperator != null);

                if (localMergeOperator != null) {
                    LOGGER.info("call local: MergeOperator#mergeIntermediate");
                    operatorInternals = localMergeOperator.mergeIntermediate(newInternals);
                } else {
                    CheckUtils.checkState(newInternals.length == 1, "Multiple internals without merge.");
                    operatorInternals = newInternals[0];
                }

                //re-create port inputs since they were already iterated above
                portInputs = createPortInputs(inputPortRoles, inPortObjects, portTypes, numChunks, tuple.localExec);

            }

            // create the out specs (after all intermediate iterations have been performed!)
            LOGGER.info("call local: NodeModel#computeFinalOutputSpecs");
            PortObjectSpec[] outSpecsNoFlowPort = null;
            outSpecsNoFlowPort =
                localNodeContainer.getNodeModel().computeFinalOutputSpecs(operatorInternals, inSpecsNoFlowPort);

            /* ---- take care about the output ---- */

            LOGGER.info("call local: NodeModel#getOutputPortRoles");
            OutputPortRole[] outputPortRoles = localNodeContainer.getNodeModel().getOutputPortRoles();
            // TODO: one single output table (for distributed ports) for all distributed nodes ... should be ok?
            //create the portOutputs for the StreamableOperator#runFinal-method
            //-> if node is run distributed, only distributed ports have to be set (i.e. RowOutputs), otherwise all
            PortOutput[] portOutputs = createPortOutputs(localNodeContainer.getNode(), outputPortRoles,
                outSpecsNoFlowPort, isDistributable, true, tuple.localExec);

            /* ---- the real work is done here ---- */

            for (int i = 0; i < numChunks; i++) {
                LOGGER.info("call remote: NodeModel#createStreamableOperator");
                StreamableOperator streamableOperator = null;
                streamableOperator = tuple.remoteNodeContainers[i].getNodeModel()
                    .createStreamableOperator(new PartitionInfo(i, numChunks), inSpecsNoFlowPort);

                //simulates transfer of the internals from the local node to the remote ones
                operatorInternals = saveAndLoadInternals(operatorInternals);

                LOGGER.info("call: StreamableOperator#loadInternals");
                streamableOperator.loadInternals(operatorInternals);

                LOGGER.info("call: StreamableOperator#runFinal");
                try {
                    PortOutput[] tmpPortOutputs = portOutputs.clone();
                    streamableOperator.runFinal(portInputs[i], portOutputs, tuple.remoteExec[i]);
                    //make sure that the portOutputs-object hasn't been manipulated directly (only it's containing objects)
                    if (IntStream.range(0, portOutputs.length).anyMatch(j -> {
                        return tmpPortOutputs[j] != portOutputs[j];
                    })) {
                        throw new IllegalStateException("Output array must not be manipulated.");
                    }
                } catch (ClassCastException e) {
                    throw new ClassCastException(e.getMessage()
                        + ". Likely reason: port-role is not set as streamable -> overwrite get[Input|Ouptut]PortRoles()-methods in NodeModel.");
                }
                checkClosedPortOutputs(portOutputs);
                if (localMergeOperator != null) {
                    LOGGER.info("call: StreamableOperator#saveInternals");
                    newInternals[i] = saveAndLoadInternals(streamableOperator.saveInternals());
                }
            }

            if (localMergeOperator != null) {
                LOGGER.info("call: MergeOperator#mergeFinals");
                operatorInternals = localMergeOperator.mergeFinal(newInternals);
            } else if (numChunks == 1) {
                operatorInternals = newInternals[0];
            }

            /* --- finish the execution -- */

            if (localMergeOperator != null) {
                LOGGER.info("call local: NodeModel#finishStreamableExecution");
                //create the port outputs for the NodeModel#finishStreamableExecution-method -> only non-distributed ports have to be provided here
                PortOutput[] nonDistrPortOutputs;
                if (isDistributable) {
                    nonDistrPortOutputs = createPortOutputs(localNodeContainer.getNode(), outputPortRoles,
                        outSpecsNoFlowPort, isDistributable, false, tuple.localExec);
                } else {
                    //if the node is not distributable we assume that all port-outputs have already been set in the runFinal-Method
                    //and don't pass any port outputs here -> the finishStreamableExecution method is than only be used
                    // to set warning messages etc.
                    nonDistrPortOutputs = new PortOutput[outputPortRoles.length];
                }

                PortOutput[] tmpPortOutputs = nonDistrPortOutputs.clone();
                localNodeContainer.getNodeModel().finishStreamableExecution(operatorInternals, tuple.localExec,
                    nonDistrPortOutputs);
                //make sure that the pArrays.equals(a, a2)ortOutputs-object hasn't been manipulated directly, only it's containing objects
                if (IntStream.range(0, portOutputs.length).anyMatch(j -> {
                    return tmpPortOutputs[j] != nonDistrPortOutputs[j];
                })) {
                    throw new IllegalStateException("Output array must not be manipulated.");
                }
                //merge the portOutputs and the nonDistrPortOutputs
                for (int i = 0; i < nonDistrPortOutputs.length; i++) {
                    if (nonDistrPortOutputs[i] != null) {
                        portOutputs[i] = nonDistrPortOutputs[i];
                    }
                }
            } else {
                //make sure that if the NodeModel#finishStreamableExecution-method is implemented, the NodeModel#createMergeOperator is implemented as well
                //check whether the current node model overrides the #finishStreamableExecution-method
                if (checkForOverriddenMethod(localNodeContainer, "finishStreamableExecution",
                    StreamableOperatorInternals.class, ExecutionContext.class, PortOutput[].class)) {
                    //method has been overridden -> createMergeOperator-method actually needs to be implemented as well!
                    throw new IllegalStateException(
                        "The 'NodeModel#finishStreamExecution'-method is overridden but no merge operator provided. Please override the 'NodeModel#createMergeOperator'-method as well.");
                }
            }

            PortObject[] outPortObjects = new PortObject[localNodeContainer.getNrOutPorts()];
            PortObjectSpec[] outPortObjectSpecs = new PortObjectSpec[localNodeContainer.getNrOutPorts()];
            outPortObjects[0] = FlowVariablePortObject.INSTANCE; // set variable out port
            outPortObjectSpecs[0] = FlowVariablePortObjectSpec.INSTANCE; // set variable out port
            for (int i = 1; i < outPortObjects.length; i++) { // without variable port output
                //retrieve the out port objects
                if (portOutputs[i - 1] instanceof BufferedDataContainerRowOutput) {
                    BufferedDataTable table = ((BufferedDataContainerRowOutput)portOutputs[i - 1]).getDataTable();
                    outPortObjects[i] = table;
                    //check if table is empty and set appropriate warning message
                    if (table.size() == 0) {
                        m_warningMessages.add("Node created an empty data table.");
                    }
                } else {
                    outPortObjects[i] = ((PortObjectOutput)portOutputs[i - 1]).getPortObject();
                }

                //retrieve the out port object specs
                if (outSpecsNoFlowPort != null && outSpecsNoFlowPort[i - 1] != null) {
                    //get out port specs as return by the configure-method (happen to be null in some cases, i.e. the Transpose-node)
                    outPortObjectSpecs[i] = outSpecsNoFlowPort[i - 1];
                } else if (outPortObjects[i] != null) { //port objects can be null (mainly in loop iterations)
                    //get outport specs as given by the result port objects
                    outPortObjectSpecs[i] = outPortObjects[i].getSpec();
                }
            }
            NativeNodeContainerExecutionResult execResult = localNodeContainer.createExecutionResult(tuple.localExec);
            NodeExecutionResult nodeExecResult = execResult.getNodeExecutionResult();
            nodeExecResult.setInternalHeldPortObjects(null);
            nodeExecResult.setNodeInternDir(null);
            nodeExecResult.setPortObjects(outPortObjects);
            nodeExecResult.setPortObjectSpecs(outPortObjectSpecs);
            WorkflowPersistor.LoadResult loadResult = new WorkflowPersistor.LoadResult("streaming test exec result");
            execResult.setSuccess(true);
            //TODO: since some port objects are null if in an iteration of a loop end node, the execution result cannot be loaded every time
            //possible workaround: check for all port objects to be non-null and only load execution result if that's the case
            //            if (Arrays.stream(outPortObjects).noneMatch(p -> p == null)) {
            localNodeContainer.loadExecutionResult(execResult, tuple.localExec, loadResult);
            //            }
            if (!m_warningMessages.isEmpty()) {
                String joinedMessages = m_warningMessages.stream().collect(Collectors.joining("\n"));
                NodeMessage nm = new NodeMessage(Type.WARNING, joinedMessages);
                localNodeContainer.setNodeMessage(nm);
                execResult.setMessage(nm);
            }
            return execResult;
        } catch (Exception e) {
            //copied from Node.java
            boolean isCanceled = e instanceof CanceledExecutionException;
            isCanceled = isCanceled || e instanceof InterruptedException;
            // TODO this can all be shortened to exec.isCanceled()?
            // isCanceled = isCanceled || localExec.isCanceled(); //not visible
            // writing to a buffer is done asynchronously -- if this thread
            // is interrupted while waiting for the IO thread to flush we take
            // it as a graceful exit
            isCanceled =
                isCanceled || (e instanceof DataContainerException && e.getCause() instanceof InterruptedException);
            if (isCanceled) {
                localNodeContainer.setNodeMessage(NodeMessage.newWarning("Execution canceled"));
                return NodeContainerExecutionStatus.FAILURE;
            }
            localNodeContainer.getNode()
                .createErrorMessageAndNotify(Message.fromSummary("Execute failed: " + e.getMessage()), e);
            return NodeContainerExecutionStatus.FAILURE;
        } finally {
            //remove virtual nodes from workflow
            removeNodeCopies(tuple.remoteNodeContainers);

            //other things to be done in post execution
            postExecution(tuple.remoteExec, tuple.remoteNodeContainers);

            // clear/dispose all newly created table chunks if there are any (created via creatTableChunks)
            m_tableChunksToBeDisposed.forEach(c -> c.dispose());
            m_tableChunksToBeDisposed.clear();
        }
    }

    private int calcNumberOfChunks(final PortObject[] inPortObjects, final InputPortRole[] inputPortRoles,
        final boolean isDistributable) {
        int numChunks = isDistributable ? m_numChunks : 1;
        for (int i = 1; i < inPortObjects.length; i++) { //without the flow variable port
            if (inputPortRoles[i - 1].isDistributable()) {
                int rowCount = (int)((BufferedDataTable)inPortObjects[i]).size();
                if (rowCount < numChunks) {
                    numChunks = Math.max(1, rowCount);
                }
            }
        }
        return numChunks;
    }

    /**
     * Does all the necessary things in order to create the execution context for a single node instance. Call
     * {@link #postExecution(NativeNodeContainer)} to finish-up the node's execution with the created execution context.
     */
    private ExecutionContext[] createExecutionContexts(final NativeNodeContainer... nc) {
        ExecutionContext[] exec = new ExecutionContext[nc.length];
        for (int i = 0; i < nc.length; i++) {
            exec[i] = nc[i].createExecutionContext();
            Node node = nc[i].getNode();
            node.openFileStoreHandler(exec[i]);
        }
        return exec;
    }

    /**
     * Performs necessary actions after the node's execution is finished. The entries of the passed arrays will also be
     * set to <code>null</code> in order to make sure that they aren't be used anymore afterwards.
     */
    private void postExecution(final ExecutionContext[] exec, final NativeNodeContainer[] nc) {
        for (int i = 0; i < nc.length; i++) {
            nc[i].putOutputTablesIntoGlobalRepository(exec[i]);
            nc[i] = null;
            exec[i] = null;
        }
    }

    /**
     * @param remoteNodeContainers
     * @param inSpecsNoFlowPort
     * @param portInputs port inputs for each chunk and port
     * @param numChunks
     * @param mergeOpAvailable true if a mergeOperator is available
     * @return the newly created operation internals of each remote node
     * @throws Exception
     *
     */
    private StreamableOperatorInternals[] performIntermediateIteration(final NativeNodeContainer[] remoteNodeContainers,
        final ExecutionContext[] exec, final StreamableOperatorInternals internals,
        final PortObjectSpec[] inSpecsNoFlowPort, final PortInput[][] portInputs, final int numChunks,
        final boolean mergeOpAvailable) throws Exception {

        final var newInternals = new StreamableOperatorInternals[numChunks];
        for (var i = 0; i < numChunks; i++) {
            var currentInternals = internals;
//            LOGGER.info("call remote: NodeModel#createInitialStreamableOperatorInternals");
//            StreamableOperatorInternals internals =
//                remoteNodeContainers[i].getNodeModel().createInitialStreamableOperatorInternals();

            LOGGER.info("call remote: NodeModel#createStreamableOperator");
            final var streamableOperator = remoteNodeContainers[i].getNodeModel()
                .createStreamableOperator(new PartitionInfo(i, numChunks), inSpecsNoFlowPort);

            LOGGER.info("call: StreamableOperator#loadInternals");
            if (mergeOpAvailable) {
                // transfer internals
                currentInternals = saveAndLoadInternals(currentInternals);
            }
            streamableOperator.loadInternals(currentInternals);

            LOGGER.info("call: StreamableOperator#runIntermediate");
            streamableOperator.runIntermediate(portInputs[i], exec[i]);

            LOGGER.info("call: StreamableOperator#saveInternals");
            currentInternals = streamableOperator.saveInternals();
            if (mergeOpAvailable) {
                // transfer internals
                currentInternals = saveAndLoadInternals(currentInternals);
            }
            newInternals[i] = currentInternals;
        }
        return newInternals;
    }

    /* multiple port inputs for each chunk -> return PortInput[chunks][ports]
     * PortInput[chunk][port] might be null if the number of chunks is smaller than the number of rows at the given port */
    private PortInput[][] createPortInputs(final InputPortRole[] inputPortRoles, final PortObject[] inPortObjects,
        final PortType[] inPortTypes, final int numChunks, final ExecutionContext exec)
        throws CanceledExecutionException, IOException {
        PortInput[][] portInputs = new PortInput[numChunks][inputPortRoles.length];
        for (int i = 0; i < inputPortRoles.length; i++) {
            if (inPortObjects[i + 1] == null) {
                //usually happens in case of unconnected optional inports
                //nothing to do here
                continue;
            }
            // if distributed: create chunks of the input table(s)
            if (inputPortRoles[i].isDistributable() && numChunks > 1) {
                // create a own DataTableRowInput for each chunk, but only
                // if input is distributable
                BufferedDataTable[] tables =
                    createTableChunks((BufferedDataTable)inPortObjects[i + 1], numChunks, exec);
                for (int j = 0; j < tables.length; j++) {
                    if (inputPortRoles[i].isStreamable()) {
                        portInputs[j][i] = new DataTableRowInput(tables[j]);
                    } else {
                        portInputs[j][i] = new PortObjectInput(tables[j]);
                    }
                }
            } else {
                // no distributed execution
                for (int j = 0; j < numChunks; j++) {
                    if (inputPortRoles[i].isStreamable()) {
                        portInputs[j][i] = new DataTableRowInput((BufferedDataTable)inPortObjects[i + 1]);
                    } else {
                        // i here and i+1 below because port type array does not contain flow variable port, but inPortObjects does
                        final PortType portType = inPortTypes[i];
                        final boolean isData =
                            BufferedDataTable.TYPE.equals(portType) || BufferedDataTable.TYPE_OPTIONAL.equals(portType);
                        if (isData) {
                            portInputs[j][i] = new PortObjectInput(inPortObjects[i + 1]);
                        } else {
                            portInputs[j][i] = new PortObjectInput(Node.copyPortObject(inPortObjects[i + 1], exec));
                        }
                    }
                }
            }
        }
        return portInputs;
    }

    /**
     * Creates the port output array depending on the given parameters. If the node is distributable (i.e. there is at
     * least one input port that is distributable) AND the outputs are to be created for the
     * StreamableOperator#runFinal-method, only the 'slots' that are distributed (according to the output role) are set,
     * the others are <code>null</code>. If the node is distributable AND the outputs are to be created for the
     * NodeModel#finishStreamableExecution-method, only the non-distributed ports are set.
     *
     * If the node is NOT distributable, all 'slots' of the port output-array are filled (either with RowOutputs, if its
     * a data table or PortObjectOutputs otherwise), not matter distributed or not (in this case the
     * 'createForRunFinalMethod' has no effect)
     *
     *
     * @param node needed to determine the out port type
     * @param outRoles the output-roles - distributed or not
     * @param outSpecsNoFlowPort the out specs needed to create the RowOutput's, if null (i.e. if the
     *            NodeModel#configure-method returns null, e.g. Transpose-node), a PortObjectOutput is created instead
     * @param isDistributable if the whole node can be run in distributed manner (i.e. there is at least one distributed
     *            in port)
     * @param createForRunFinalMethod if the port outputs are to be created to be used as parameters in the
     *            StreamableOperator#runFinal method (only distributed outputs are set) or not (only non-distributed
     *            outputs are set, since assumed to be used in the NodeModel#finishStreamableExecution-method)
     * @param exec the execution context to create the buffered data container
     * @return the port outputs with some 'slots' possibly set to null
     */
    private PortOutput[] createPortOutputs(final Node node, final OutputPortRole[] outRoles,
        final PortObjectSpec[] outSpecsNoFlowPort, final boolean isDistributable, final boolean createForRunFinalMethod,
        final ExecutionContext exec) {
        PortOutput[] portOutputs = new PortOutput[node.getNrOutPorts() - 1]; // without flow variables port
        for (int i = 0; i < portOutputs.length; i++) {
            //fill all port outputs if NOT distributed OR fill either the distributed outports or non-distributed outports ONLY (depending on the createForRunFinal-flag)
            if (!isDistributable || (isDistributable && outRoles[i].isDistributable() && createForRunFinalMethod)
                || (isDistributable && !outRoles[i].isDistributable() && !createForRunFinalMethod)) {
                if ((node.getOutputType(i + 1).equals(BufferedDataTable.TYPE) // (i+1)-> skip flow variable port
                    || node.getOutputType(i + 1).equals(BufferedDataTable.TYPE_OPTIONAL))) {
                    // output is a BufferedDataTable -> create a row output that wraps a BufferedDataTable
                    BufferedDataContainerRowOutput rowOutput;
                    if (outSpecsNoFlowPort == null || outSpecsNoFlowPort[i] == null) {
                        //outSpecsNoFlowPort might be null if the node models' configure-method return null (e.g. Transpose-node)
                        //use row output the only is allowed to be filled by 'setFully'
                        rowOutput = new BufferedDataContainerRowOutput();
                    } else {
                        //don't dispose (i.e. add it to m_tableChunksToBeDisposed) the output data containers
                        //since they are needed by successors and are persisted
                        rowOutput = new BufferedDataContainerRowOutput(
                            exec.createDataContainer((DataTableSpec)outSpecsNoFlowPort[i], true));
                    }
                    portOutputs[i] = rowOutput;

                } else {
                    //output is not a data table (or configure return null)

                    //only set ALL port outputs if node is NOT distributed!! (see javadoc of StreamableOperator#runFinal(...))
                    //if the node is distributed, only the RowOutput are set
                    portOutputs[i] = new PortObjectOutput();
                }
            }
            //else - port at position i remains null
        }
        return portOutputs;
    }

    /**
     * Checks whether all row outputs have been closed.
     */
    private void checkClosedPortOutputs(final PortOutput[] portOutputs) {
        for (PortOutput portOutput : portOutputs) {
            if (portOutput instanceof BufferedDataContainerRowOutput) {
                if (!((BufferedDataContainerRowOutput)portOutput).closeCalled()) {
                    throw new IllegalStateException("close() has NOT been called on at least one RowOutput.");
                }
            }
        }
    }

    /**
     * Creates the given number of copies of the given node TODO: use {@link SandboxedNodeCreator}!!
     */
    private NativeNodeContainer[] createNodeCopies(final NativeNodeContainer nodeContainer, final int numCopies) {
        WorkflowManager workflowManager = nodeContainer.getParent();
        WorkflowCopyContent.Builder sourceContent = WorkflowCopyContent.builder();
        sourceContent.setNodeIDs(nodeContainer.getID());
        sourceContent.setIncludeInOutConnections(false);
        WorkflowPersistor workflowPersistor = workflowManager.copy(sourceContent.build());
        NativeNodeContainer[] nodeContainers = new NativeNodeContainer[numCopies];
        //TODO: actually DON'T use the local node container for execution
        //however, currently the way the nodes are copied doesn't work with flow variables in some circumstance
        nodeContainers[0] = nodeContainer;
        NodeID[] nodeIDs = new NodeID[numCopies];
        NodeUIInformation uiInf = nodeContainer.getUIInformation();
        Set<ConnectionContainer> connections = workflowManager.getIncomingConnectionsFor(nodeContainer.getID());
        for (int i = 1; i < numCopies; i++) {
            WorkflowCopyContent sinkContent = workflowManager.paste(workflowPersistor);
            nodeContainers[i] = (NativeNodeContainer)workflowManager.getNodeContainer(sinkContent.getNodeIDs()[0]);
            nodeContainers[i].setUIInformation(
                NodeUIInformation.builder(uiInf).translate(new int[]{0, (i + 1) * uiInf.getBounds()[3]}).build());
            nodeIDs[i] = nodeContainers[i].getID();

            //set all incoming connections
            for (ConnectionContainer c : connections) {
                workflowManager.addConnection(c.getSource(), c.getSourcePort(), nodeContainers[i].getID(),
                    c.getDestPort());
            }
            nodeContainers[i].getParent().setJobManager(nodeIDs[i], NoopExecutionJobManager.INSTANCE);
            nodeContainers[i].getNode().setFileStoreHandler(nodeContainer.getNode().getFileStoreHandler());
        }

        //TODO: collapse the node copies into a meta node, unfortunately doesn't work -> flow variables are somehow not propagated
        //workflowManager.collapseIntoMetaNode(nodeIDs, new WorkflowAnnotation[0], "Distributed Execution");
        return nodeContainers;
    }

    private void removeNodeCopies(final NativeNodeContainer[] nodeContainers) {
        //NOTE: the for loop starts with _1_ since the original node is at position 0 that must not be deleted
        //TODO this should actually NOT be necessary since all nodes used for execution should be copies
        //(or better sandboxed)
        for (int i = 1; i < nodeContainers.length; i++) {
            final var nc = nodeContainers[i];
            nc.getNode().setFileStoreHandler(null);
            WorkflowManager workflowManager = waitWhileExecuting(nc);
            NodeID id = nc.getID();
            workflowManager.removeNode(id);
        }
    }

    /**
     * Added as part of AP-20402 - component output nodes execute all contained nodes, incl. the "remote" ones
     * added by this class. This code waits until these nodes have finished execution (before they get removed).
     *
     * Fixed test case failure in knime-base: test_AP-15123_File_Meta_Info_File_Permissions
     */
    private static WorkflowManager waitWhileExecuting(final NativeNodeContainer nc) {
        final var workflowManager = nc.getParent();
        final var reentrantLock = workflowManager.getReentrantLockInstance();
        final var condition = reentrantLock.newCondition();
        try (var lock = workflowManager.lock()) {
            NodeStateChangeListener listener = null;
            while (nc.getNodeContainerState().isExecutionInProgress()) {
                if (listener == null) {
                    listener = event -> {
                        reentrantLock.lock();
                        try { // NOSONAR (complexity)
                            condition.signalAll();
                        } finally {
                            reentrantLock.unlock();
                        }
                    };
                    nc.addNodeStateChangeListener(listener);
                }
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    LOGGER.error(String.format("Error cleaning up %s: %s",
                        StreamingTestNodeExecutionJob.class.getSimpleName(), e.getMessage()), e);
                    Thread.currentThread().interrupt(); // as suggested by sonar
                    break;
                }
            }
            if (listener != null) {
                nc.removeNodeStateChangeListener(listener);
            }
        }
        return workflowManager;
    }

    /** Divides the given table into the given number of chunks. */
    private BufferedDataTable[] createTableChunks(final BufferedDataTable table, final int numChunks,
        final ExecutionContext exec) {
        int chunkSize = (int)(table.size() / (double)numChunks);
        int chunkCount = 0;
        int count = 0;
        RowIterator it = table.iterator();
        BufferedDataContainer container = null;
        BufferedDataTable[] tableChunks = new BufferedDataTable[numChunks];
        while (true) {
            if ((chunkCount < numChunks - 1 && (count++ % chunkSize == 0)) || !it.hasNext()) { //chunkCount < numChunks-1 -> put all remaining rows into the last chunk
                if (container != null) {
                    container.close();
                    tableChunks[chunkCount] = container.getTable();
                    chunkCount++;
                }
                if (!it.hasNext()) {
                    break;
                }
                container = exec.createDataContainer(table.getDataTableSpec());

                //keep track of the table to be disposed when the job is finished
                m_tableChunksToBeDisposed.add(container);
            }
            container.addRowToTable(it.next());
        }
        return tableChunks;
    }

    /**
     * Helper function to first save the internals to a data output stream and immediately restores them afterwards.
     * This should simulate the transfer of the internals between the local and remote nodes.
     */
    private StreamableOperatorInternals saveAndLoadInternals(final StreamableOperatorInternals internals)
        throws IOException, InstantiationException, IllegalAccessException {

        if (internals == null) {
            return null;
        }

        final byte[] bytes;
        try (final var byteArrayOutput = new ByteArrayOutputStream()) {
            try (final var out = new DataOutputStream(byteArrayOutput)) {
                internals.save(out);
            }
            bytes = byteArrayOutput.toByteArray();
        }

        final var res = internals.getClass().newInstance();
        try (final var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            res.load(in);
        }
        return res;
    }

    /**
     * Helper method that checks whether the given method is overridden by the given NodeModel-implementation.
     *
     * @param the node container to be checked
     * @param name the name of the method
     * @param parameterTypes the list of parameters
     * @return <code>true</code> if overridden
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    private boolean checkForOverriddenMethod(final NativeNodeContainer nc, final String name,
        final Class<?>... parameterTypes) {
        try {
            Method m = nc.getNodeModel().getClass().getMethod(name, parameterTypes);
            return m.getDeclaringClass() != NodeModel.class;
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean cancel() {
        if (m_future == null) {
            throw new IllegalStateException("Future that represents the execution has not been set.");
        }
        return m_future.cancel(true);
    }

}
