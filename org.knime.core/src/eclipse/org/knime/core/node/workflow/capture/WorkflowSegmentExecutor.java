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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.exec.dataexchange.PortObjectRepository;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.capture.WorkflowSegment.Input;
import org.knime.core.node.workflow.capture.WorkflowSegment.Output;
import org.knime.core.node.workflow.capture.WorkflowSegment.PortID;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeFactory;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectInNodeModel;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectOutNodeFactory;
import org.knime.core.node.workflow.virtual.DefaultVirtualPortObjectOutNodeModel;
import org.knime.core.node.workflow.virtual.VirtualNodeInput;
import org.knime.core.node.workflow.virtual.parchunk.FlowVirtualScopeContext;
import org.knime.core.util.Pair;
import org.knime.core.util.ThreadPool;

/**
 * Represents an executable {@link WorkflowSegment}. The execution is done by embedding the workflow segment as a
 * metanode into the currently opened worflow.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class WorkflowSegmentExecutor {

    private final WorkflowManager m_wfm;

    private final NativeNodeContainer m_hostNode;

    private FlowVirtualScopeContext m_flowVirtualScopeContext;

    private NodeID m_virtualStartID;

    private NodeID m_virtualEndID;

    /**
     * @param ws the workflow segment to execute
     * @param workflowName the name of the metanode to be created (which will only be visible if 'debug' is
     *            <code>true</code>)
     * @param hostNode the node which is responsible for the execution of the workflow segment (which provides the
     *            input and receives the output data, supplies the file store, etc.)
     * @param debug if <code>true</code> the metanode the workflow segment is executed in, will be visible (for
     *            debugging purposes), if <code>false</code> it's hidden
     * @param warningConsumer callback for warning if there have while loading the workflow from the workflow segment
     * @throws KNIMEException If the workflow can't be instantiated from the segment.
     */
    public WorkflowSegmentExecutor(final WorkflowSegment ws, final String workflowName, final NodeContainer hostNode,
        final boolean debug, final Consumer<String> warningConsumer) throws KNIMEException {
        m_hostNode = (NativeNodeContainer)hostNode;
        m_wfm = hostNode.getParent().createAndAddSubWorkflow(new PortType[0], new PortType[0],
            (debug ? "Debug: " : "") + workflowName);
        m_flowVirtualScopeContext = new FlowVirtualScopeContext(hostNode.getID());
        m_wfm.setInitialScopeContext(m_flowVirtualScopeContext);
        if (!debug) {
            m_wfm.hideInUI();
        }

        // position
        NodeUIInformation startUIPlain = hostNode.getUIInformation();
        if (startUIPlain != null) {
            NodeUIInformation startUI =
                NodeUIInformation.builder(startUIPlain).translate(new int[]{60, -60, 0, 0}).build();
            m_wfm.setUIInformation(startUI);
        }

        // copy workflow segment into metanode
        WorkflowManager segmentWorkflow = BuildWorkflowsUtil.loadWorkflow(ws, warningConsumer);
        NodeID[] ids = segmentWorkflow.getNodeContainers().stream().map(NodeContainer::getID).toArray(NodeID[]::new);
        m_wfm.copyFromAndPasteHere(segmentWorkflow, WorkflowCopyContent.builder().setNodeIDs(ids).build());
        ws.disposeWorkflow();

        addVirtualIONodes(ws);
    }

    private void addVirtualIONodes(final WorkflowSegment wf) {

        //add virtual in node
        List<Input> inputs = wf.getConnectedInputs();
        PortType[] inTypes =
            inputs.stream().map(i -> getNonOptionalType(i.getType().get())).toArray(s -> new PortType[s]);
        int[] wfBounds = NodeUIInformation.getBoundingBoxOf(m_wfm.getNodeContainers());
        m_virtualStartID = m_wfm.createAndAddNode(new DefaultVirtualPortObjectInNodeFactory(inTypes));
        Pair<Integer, int[]> pos = BuildWorkflowsUtil.getInputOutputNodePositions(wfBounds, 1, true);
        m_wfm.getNodeContainer(m_virtualStartID).setUIInformation(
            NodeUIInformation.builder().setNodeLocation(pos.getFirst(), pos.getSecond()[0], -1, -1).build());

        //add virtual out node
        List<Output> outputs = wf.getConnectedOutputs();
        PortType[] outTypes =
            outputs.stream().map(o -> getNonOptionalType(o.getType().get())).toArray(s -> new PortType[s]);
        m_virtualEndID = m_wfm.createAndAddNode(new DefaultVirtualPortObjectOutNodeFactory(outTypes));
        pos = BuildWorkflowsUtil.getInputOutputNodePositions(wfBounds, 1, false);
        m_wfm.getNodeContainer(m_virtualEndID).setUIInformation(
            NodeUIInformation.builder().setNodeLocation(pos.getFirst(), pos.getSecond()[0], -1, -1).build());

        //connect virtual in
        for (int i = 0; i < inputs.size(); i++) {
            for (PortID p : inputs.get(i).getConnectedPorts()) {
                m_wfm.addConnection(m_virtualStartID, i + 1, p.getNodeIDSuffix().prependParent(m_wfm.getID()),
                    p.getIndex());
            }
        }

        //connect virtual out
        for (int i = 0; i < outputs.size(); i++) {
            PortID p = outputs.get(i).getConnectedPort().orElse(null);
            if (p != null) {
                m_wfm.addConnection(p.getNodeIDSuffix().prependParent(m_wfm.getID()), p.getIndex(), m_virtualEndID,
                    i + 1);
            }
        }
    }

    private static PortType getNonOptionalType(final PortType p) {
        return PortTypeRegistry.getInstance().getPortType(p.getPortObjectClass());
    }

    /**
     * Executes the workflow segment.
     *
     * @param inputData the input data to be used for execution
     * @param exec for cancellation
     * @return the resulting port objects and flow variables
     * @throws Exception if workflow execution fails
     */
    public Pair<PortObject[], List<FlowVariable>> executeWorkflow(final PortObject[] inputData, final ExecutionContext exec)
        throws Exception { // NOSONAR
        NativeNodeContainer virtualInNode = ((NativeNodeContainer)m_wfm.getNodeContainer(m_virtualStartID));
        DefaultVirtualPortObjectInNodeModel inNM =
                (DefaultVirtualPortObjectInNodeModel)virtualInNode.getNodeModel();

        m_flowVirtualScopeContext.registerHostNodeForPortObjectPersistence(m_hostNode, exec);

        inNM.setVirtualNodeInput(new VirtualNodeInput(inputData,
            collectOutputFlowVariablesFromUpstreamNodes(m_hostNode)));
        NativeNodeContainer nnc = (NativeNodeContainer)m_wfm.getNodeContainer(m_virtualEndID);
        DefaultVirtualPortObjectOutNodeModel outNM =
            (DefaultVirtualPortObjectOutNodeModel)nnc.getNodeModel();

        AtomicReference<Exception> exception = new AtomicReference<>();
        executeAndWait(exec, exception);

        if (exception.get() != null) {
            throw exception.get();
        }

        PortObject[] portObjectCopies = copyPortObjects(outNM.getOutObjects(), exec);
        // if (portObjectCopies != null) {
        //     removeSuperfluousFileStores(Stream.concat(stream(portObjectCopies), outputData.stream()));
        // }
        return Pair.create(portObjectCopies, getFlowVariablesFromNC(nnc));
    }

    private void executeAndWait(final ExecutionContext exec, final AtomicReference<Exception> exception) {
        // code copied from SubNodeContainer#executeWorkflowAndWait
        final Runnable inBackgroundRunner = () -> {
            m_wfm.executeUpToHere(m_virtualEndID);
            try {
                waitWhileInExecution(m_wfm, exec);
            } catch (InterruptedException | CanceledExecutionException e) { // NOSONAR
                m_wfm.cancelExecution(m_hostNode);
                Thread.currentThread().interrupt();
            }
        };
        final ThreadPool currentPool = ThreadPool.currentPool();
        if (currentPool != null) {
            // ordinary workflow execution
            try {
                currentPool.runInvisible(() -> {
                    inBackgroundRunner.run();
                    return null;
                });
            } catch (ExecutionException ee) {
                exception.compareAndSet(null, ee);
                NodeLogger.getLogger(this.getClass()).error(
                    ee.getCause().getClass().getSimpleName() + " while waiting for to-be-executed workflow to complete",
                    ee);
            }
        } else {
            // streaming execution
            inBackgroundRunner.run();
        }
    }

    private static void waitWhileInExecution(final WorkflowManager wfm, final ExecutionContext exec)
        throws InterruptedException, CanceledExecutionException {
        while (wfm.getNodeContainerState().isExecutionInProgress()) {
            wfm.waitWhileInExecution(1, TimeUnit.SECONDS);
            exec.checkCanceled();
        }
    }

    /**
     * Cancels the execution if it is running and removes the virtual node containing the workflow segment from
     * the hosting workflow.
     */
    public void dispose() {
        cancel();
        m_wfm.getParent().removeNode(m_wfm.getID());
    }

    /**
     * Cancels the execution of the workflow segment.
     */
    public void cancel() {
        if (m_wfm.getNodeContainerState().isExecutionInProgress()) {
            m_wfm.cancelExecution(m_wfm);
        }
    }

    private static List<FlowVariable> getFlowVariablesFromNC(final SingleNodeContainer nc) {
        Stream<FlowVariable> res;
        if (nc instanceof NativeNodeContainer) {
            res = ((NativeNodeContainer)nc).getNodeModel()
                .getAvailableFlowVariables(VariableTypeRegistry.getInstance().getAllTypes()).values().stream();
        } else {
            res = nc.createOutFlowObjectStack().getAllAvailableFlowVariables().values().stream();
        }
        return res.filter(fv -> fv.getScope() == Scope.Flow).collect(Collectors.toList());
    }

    private static PortObject[] copyPortObjects(final PortObject[] portObjects, final ExecutionContext exec)
        throws IOException, CanceledExecutionException {
        if (portObjects == null) {
            return null; // NOSONAR
        }
        PortObject[] portObjectCopies = new PortObject[portObjects.length];
        for (int i = 0; i < portObjects.length; i++) {
            if (portObjects[i] != null) {
                portObjectCopies[i] = PortObjectRepository.copy(portObjects[i], exec, exec);
            }
        }
        return portObjectCopies;
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
    private static List<FlowVariable> collectOutputFlowVariablesFromUpstreamNodes(final NodeContainer thisNode) {
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

}