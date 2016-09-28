/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 29, 2015 (wiswedel): created
 */
package org.knime.core.node.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.knime.core.api.node.workflow.ConnectionID;
import org.knime.core.api.node.workflow.IConnectionContainer;
import org.knime.core.api.node.workflow.WorkflowCopyContent;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.exec.dataexchange.PortObjectRepository;
import org.knime.core.node.exec.dataexchange.in.BDTInNodeFactory;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.CredentialsStore;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.execresult.NativeNodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeExecutionResult;
import org.knime.core.node.workflow.execresult.SubnodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.WorkflowExecutionResult;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LockFailedException;

/**
 * Helper to create a small workflow that runs a copy of a node in isolation. Needed for (remote) node execution jobs,
 * whereby the target node is first copied into a sandbox before it's run. Input data is populated by fake nodes of type
 * {@link PortObjectInNodeFactory}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 * @since 3.1
 */
public final class SandboxedNodeCreator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SandboxedNodeCreator.class);

    private static final NodeFactory<?> OBJECT_READ_NODE_FACTORY = new PortObjectInNodeFactory();

    private static final NodeFactory<?> TABLE_READ_NODE_FACTORY = new BDTInNodeFactory();

    private final WorkflowManager m_rootWFM;
    private final NodeContainer m_nc;

    private File m_localWorkflowDir;
    private PortObject[] m_inData;
    private boolean m_copyDataIntoNewContext;
    private boolean m_forwardConnectionProgressEvents;

    /** New creator with base information that can be further customized using the setter methods. None
     * of the arguments must be null.
     * @param ncToClone The node to clone.
     * @param inData The input data the node is provided with. Length must correspond to number of inputs.
     * @param rootWFM the parent workflow into which the new temporary project is created. The cluster
     * execution and streamer executor have their own private pool to not pollute WFM#ROOT.
     */
    public SandboxedNodeCreator(final NodeContainer ncToClone, final PortObject[] inData,
        final WorkflowManager rootWFM) {
        m_nc = CheckUtils.checkArgumentNotNull(ncToClone);
        m_rootWFM = CheckUtils.checkArgumentNotNull(rootWFM);
        m_inData = CheckUtils.checkArgumentNotNull(inData);
        CheckUtils.checkArgument(inData.length == m_nc.getNrInPorts(),
            "Invalid array port object array length, expected %d but got %d", m_nc.getNrInPorts(), inData.length);
    }

    /** Where to save the sandboxed workflow after it's created. For the cluster execution this is a 'real' folder.
     * Default is <code>null</code> -- so a temporary workflow folder is assigned and the workflow is not saved
     * to disc after it's been created.
     * @param localWorkflowDir The folder or null (which is the default).
     * @return this (method chaining)
     */
    public SandboxedNodeCreator setLocalWorkflowDir(final File localWorkflowDir) {
        m_localWorkflowDir = localWorkflowDir;
        return this;
    }

    /** Set whether to copy the data from the original workflow into the sandboxed workflow. This is true if the
     * workflow is run some place else (in the cluster) so that data including filestores and blobs make a context
     * switch. This is expensive (I/O) but needed to guarantee isolation. Default is <code>false</code> -- so the
     * temporary workflow is using the data directly associated with the original workflow.
     * @param copyDataIntoNewContext that property (default is <code>false</code>).
     * @return this (method chaining).
     */
    public SandboxedNodeCreator setCopyData(final boolean copyDataIntoNewContext) {
        m_copyDataIntoNewContext = copyDataIntoNewContext;
        return this;
    }

    /** Whether to forward progress events on {@link ConnectionContainer}. This is true for the streaming executor
     * but false otherwise.
     * @param forwardConnectionProgressEvents that property (default is <code>false</code>).
     * @return this (method chaining).
     */
    public SandboxedNodeCreator setForwardConnectionProgressEvents(final boolean forwardConnectionProgressEvents) {
        m_forwardConnectionProgressEvents = forwardConnectionProgressEvents;
        return this;
    }

    /** Callback from {@link SandboxedNode} instance whether to delete the folder after the workflow is disposed. */
    boolean isDeleteOnDiscard() {
        return m_localWorkflowDir == null;
    }

    /**
     * Creates that temporary mini workflow that is executed remotely on the cluster/stream executor.
     * The returned value should be {@link SandboxedNode#close()} when done (using try-with-resources). After this
     * method is called no other set-method should be called.
     *
     * @param exec for progress/cancelation
     * @return the index of the node that represents this node (the node to execute) in the temporary mini workflow
     * @throws InvalidSettingsException
     * @throws IOException
     * @throws CanceledExecutionException
     * @throws LockFailedException
     * @throws InterruptedException
     */
    public SandboxedNode createSandbox(final ExecutionMonitor exec) throws InvalidSettingsException, IOException,
    CanceledExecutionException, LockFailedException, InterruptedException {
        exec.setMessage("Creating virtual workflow");

        final WorkflowManager parent = m_nc.getParent();
        // derive workflow context via NodeContext as the parent could only a be a metanode in a metanode...
        final WorkflowContext origContext = NodeContext.getContext().getWorkflowManager().getContext();
        WorkflowContext.Factory ctxFactory;
        // this if-elseif-etc is OK for both streaming in cluster but not 100% certain for other cases
        // (specifically reading knime://knime.workflow files)
        if (!m_copyDataIntoNewContext) {
            ctxFactory = new WorkflowContext.Factory(origContext);
            if (m_localWorkflowDir != null) {
                ctxFactory.setOriginalLocation(origContext.getCurrentLocation())
                    .setCurrentLocation(m_localWorkflowDir);
            }
        } else if (m_localWorkflowDir != null) {
            ctxFactory = new WorkflowContext.Factory(m_localWorkflowDir);
        } else {
            ctxFactory = new WorkflowContext.Factory(FileUtil.createTempDir("sandbox-" + m_nc.getNameWithID()));
        }
        // We have to use the same location for the temporary files
        ctxFactory.setTempLocation(origContext.getTempLocation());
        origContext.getMountpointURI().ifPresent(u -> ctxFactory.setMountpointURI(u));

        WorkflowCreationHelper creationHelper = new WorkflowCreationHelper();
        creationHelper.setWorkflowContext(ctxFactory.createContext());
        if (!m_copyDataIntoNewContext) {
            creationHelper.setDataHandlers(parent.getGlobalTableRepository(), parent.getFileStoreHandlerRepository());
        }

        WorkflowManager tempWFM =
            m_rootWFM.createAndAddProject("Sandbox Exec on " + m_nc.getNameWithID(), creationHelper);

        // Add the workflow variables
        List<FlowVariable> workflowVariables = parent.getProjectWFM().getWorkflowVariables();
        tempWFM.addWorkflowVariables(true, workflowVariables.toArray(new FlowVariable[workflowVariables.size()]));

        //update credentials store of the workflow
        CredentialsStore cs  = tempWFM.getCredentialsStore();
        workflowVariables.stream()
            .filter(f -> f.getType().equals(FlowVariable.Type.CREDENTIALS))
            .filter(f -> !cs.contains(f.getName()))
            .forEach(cs::addFromFlowVariable);

        final int inCnt = m_inData.length;
        // port object IDs in static port object map, one entry for
        // each connected input (no value for unconnected optional inputs)
        List<Integer> portObjectRepositoryIDs = new ArrayList<Integer>(inCnt);
        try {
            NodeID[] ins = new NodeID[inCnt];
            for (int i = 0; i < inCnt; i++) {
                final PortObject in = m_inData[i];
                final NodeInPort inPort = m_nc.getInPort(i);
                final PortType portType = inPort.getPortType();
                if (in == null) { // unconnected optional input
                    CheckUtils.checkState(portType.isOptional(),
                        "No data at port %d, although port is mandatory (port type %s)", i, portType.getName());
                    continue;
                }
                int portObjectRepositoryID = PortObjectRepository.add(in);
                portObjectRepositoryIDs.add(portObjectRepositoryID);
                boolean isTable = BufferedDataTable.TYPE.equals(portType);
                NodeID inID = tempWFM.createAndAddNode(isTable ? TABLE_READ_NODE_FACTORY : OBJECT_READ_NODE_FACTORY);
                NodeSettings s = new NodeSettings("temp_data_in");
                tempWFM.saveNodeSettings(inID, s);
                List<FlowVariable> flowVars = getFlowVariablesOnPort(i);
                PortObjectInNodeModel.setInputNodeSettings(s,
                    portObjectRepositoryID, flowVars, m_copyDataIntoNewContext);

                //update credentials store of the workflow
                flowVars.stream()
                    .filter(f -> f.getType().equals(FlowVariable.Type.CREDENTIALS))
                    .filter(f -> !cs.contains(f.getName()))
                    .forEach(cs::addFromFlowVariable);

                tempWFM.loadNodeSettings(inID, s);
                ins[i] = inID;
            }
            // execute inPort object nodes to store the input data in them
            if (ins.length > 0 && !tempWFM.executeAllAndWaitUntilDoneInterruptibly()) {
                String error = "Unable to execute virtual workflow, status sent to log facilities";
                LOGGER.debug(error + ":");
                LOGGER.debug(tempWFM.toString());
                throw new RuntimeException(error);
            }
            // add the target node to the workflow
            WorkflowCopyContent.Builder content = WorkflowCopyContent.builder();
            content.setNodeIDs(m_nc.getID());
            final NodeID targetNodeID = tempWFM.copyFromAndPasteHere(parent, content.build()).getNodeIDs()[0];
            NodeContainer targetNode = tempWFM.getNodeContainer(targetNodeID);
            // connect target node to inPort object nodes, skipping unconnected (optional) inputs
            IntStream.range(0, inCnt).filter(i -> ins[i] != null)
            .forEach(i -> tempWFM.addConnection(ins[i], 1, targetNodeID, i));
            if (m_forwardConnectionProgressEvents) {
                setupConnectionProgressEventListeners(m_nc, targetNode);
            }

            // copy the existing tables into the (meta) node (e.g. an executed file reader that's necessary
            // for other nodes to execute)
            exec.setMessage("Copying tables into temp flow");
            NodeContainerExecutionResult origResult = m_nc.createExecutionResult(exec);
            ExecutionMonitor copyExec = exec.createSubProgress(0.0);
            copyExistingTablesIntoSandboxContainer(origResult, m_nc, targetNode, copyExec, m_copyDataIntoNewContext);
            CopyContentIntoTempFlowNodeExecutionJobManager copyDataIntoTmpFlow =
                new CopyContentIntoTempFlowNodeExecutionJobManager(origResult);
            NodeExecutionJobManager oldJobManager = targetNode.getJobManager();
            tempWFM.setJobManager(targetNodeID, copyDataIntoTmpFlow);
            tempWFM.executeAllAndWaitUntilDoneInterruptibly();
            tempWFM.setJobManager(targetNodeID, oldJobManager);

            // do not use the cluster executor on the cluster...
            tempWFM.setJobManager(targetNodeID,
                NodeExecutionJobManagerPool.getDefaultJobManagerFactory().getInstance());

            if (!m_copyDataIntoNewContext) {
                copyFileStoreHandlerReference(targetNode, parent, false);
            }

            // save workflow in the local job dir
            if (m_localWorkflowDir != null) {
                tempWFM.save(m_localWorkflowDir, exec, true);
                deepCopyFilesInWorkflowDir(m_nc, tempWFM);
            }
            return new SandboxedNode(tempWFM, targetNodeID);
        } finally {
            portObjectRepositoryIDs.stream().forEach(PortObjectRepository::remove);
        }
    }

    private static final String[] MAGIC_DATA_FOLDERS = {"data", "drop"};

    /**
     * Deep copies data and drop folders contained in the source directory to the target directory.
     * @param source Source node
     * @param targetParent Target node's parent
     */
    private static void deepCopyFilesInWorkflowDir(final NodeContainer source, final WorkflowManager targetParent) {
        NodeContainer target = targetParent.getNodeContainer(
            targetParent.getID().createChild(source.getID().getIndex()));
        ReferencedFile sourceDirRef = source.getNodeContainerDirectory();
        ReferencedFile targetDirRef = target.getNodeContainerDirectory();
        if (sourceDirRef == null) {
            // The source node has never been saved, there are no files to copy
            return;
        }
        File sourceDir = sourceDirRef.getFile();
        File targetDir = targetDirRef.getFile();

        for (String magicFolderName : MAGIC_DATA_FOLDERS) {
            File dataSourceDir = new File(sourceDir, magicFolderName);
            if (dataSourceDir.isDirectory()) {
                File dataTargetDir = new File(targetDir, magicFolderName);
                try {
                    FileUtils.copyDirectory(dataSourceDir, dataTargetDir);
                    LOGGER.debugWithFormat("Copied directory \"%s\" to \"%s\"",
                        dataSourceDir.getAbsolutePath(), dataTargetDir.getAbsolutePath());
                } catch (IOException ex) {
                    LOGGER.error(String.format("Could not copy directory \"%s\" to \"%s\": %s",
                        dataSourceDir.getAbsolutePath(), dataTargetDir.getAbsolutePath(), ex.getMessage()), ex);
                }
            }
        }
        Collection<NodeContainer> childrenList = Collections.emptyList();
        WorkflowManager childTargetParent = null;
        if (source instanceof WorkflowManager) {
            childrenList = ((WorkflowManager)source).getNodeContainers();
            childTargetParent = (WorkflowManager)target;
        } else if (source instanceof SubNodeContainer) {
            childrenList = ((SubNodeContainer)source).getWorkflowManager().getNodeContainers();
            childTargetParent = ((SubNodeContainer)target).getWorkflowManager();
        }
        for (NodeContainer child : childrenList) {
            deepCopyFilesInWorkflowDir(child, childTargetParent);
        }
    }

    /** Sets the file store handlers set on the original node recursively into the sandboxed node. This is
     * only done when the data is _not_ to be copied as the sandboxed node should use the data (includes file stores)
     * from the original node.
     * @param runNC The sandbox node container
     * @param origNCParent the parent of the original workflow
     * @param nullIt <code>true</code> to set a <code>null</code> file store handler - used in
     * {@link SandboxedNode#close()} (otherwise the file store handler is cleared when the temp flow is disposed).
     */
    private void copyFileStoreHandlerReference(final NodeContainer runNC, final WorkflowManager origNCParent,
        final boolean nullIt) {
        final NodeID origParentID = origNCParent.getID();
        final int runNCIndex = runNC.getID().getIndex();
        if (runNC instanceof NativeNodeContainer) {
            NativeNodeContainer runNNC = (NativeNodeContainer)runNC;
            NativeNodeContainer origNNC = origNCParent.getNodeContainer(
                origParentID.createChild(runNCIndex), NativeNodeContainer.class, true);
            if (origNNC.getNodeContainerState().isExecutionInProgress()) {
                final IFileStoreHandler fsHdl = nullIt ? null : origNNC.getNode().getFileStoreHandler();
                if (!nullIt) {
                    runNNC.clearFileStoreHandler();
                }
                runNNC.getNode().setFileStoreHandler(fsHdl);
            }
        } else if (runNC instanceof WorkflowManager) {
            WorkflowManager runWFM = (WorkflowManager)runNC;
            WorkflowManager origWFM = origNCParent.getNodeContainer(
                origParentID.createChild(runNCIndex), WorkflowManager.class, true);
            runWFM.getNodeContainers().stream().forEach(n -> copyFileStoreHandlerReference(n, origWFM, nullIt));
        } else {
            WorkflowManager runSubWFM = ((SubNodeContainer)runNC).getWorkflowManager();
            WorkflowManager origSubWFM = origNCParent.getNodeContainer(
                origParentID.createChild(runNCIndex), SubNodeContainer.class, true).getWorkflowManager();
            runSubWFM.getNodeContainers().stream().forEach(n -> copyFileStoreHandlerReference(n, origSubWFM, nullIt));
        }
    }

    /** For each connection in the sandbox add a progress listener that fires an event on the original connection. */
    private void setupConnectionProgressEventListeners(final NodeContainer origNC, final NodeContainer sandboxNC) {
        WorkflowManager origWFM;
        WorkflowManager sandboxWFM;
        if (origNC instanceof WorkflowManager) {
            origWFM = (WorkflowManager)origNC;
            sandboxWFM = (WorkflowManager)sandboxNC;
        } else if (origNC instanceof SubNodeContainer) {
            origWFM = ((SubNodeContainer)origNC).getWorkflowManager();
            sandboxWFM = ((SubNodeContainer)sandboxNC).getWorkflowManager();
        } else {
            return;
        }
        for (IConnectionContainer cc : origWFM.getConnectionContainers()) {
            if (cc instanceof ConnectionContainer) {
                NodeID sandboxTargetID = sandboxWFM.getID();
                if (!cc.getDest().equals(origWFM.getID())) {
                    // real connection, not a wfm out or through connection
                    sandboxTargetID = new NodeID(sandboxTargetID, cc.getDest().getIndex());
                }
                IConnectionContainer sbCC =
                    sandboxWFM.getConnection(new ConnectionID(sandboxTargetID, cc.getDestPort()));
                sbCC.addProgressListener(pe -> ((ConnectionContainer)cc).progressChanged(pe));
            } else {
                throw new IllegalArgumentException(
                    "Connection container of type " + cc.getClass().getName() + " not supported, yet.");
            }
        }
    }

    /**
     * Checks which flow variables are available on a port by looking on the output port connected to this input port.
     *
     * @param portIdx input port of the {@link NodeContainer} {@link #m_nc}
     * @return the flow variables available at this port
     */
    private List<FlowVariable> getFlowVariablesOnPort(final int portIdx) {
        WorkflowManager wfm = m_nc.getParent();
        Optional<Stream<FlowVariable>> nodeInputFlowVariables = wfm.getNodeInputFlowVariables(m_nc.getID(), portIdx);
        if (nodeInputFlowVariables.isPresent()) {
            List<FlowVariable> result = nodeInputFlowVariables.get()
                    .filter(fv -> !fv.isGlobalConstant()).collect(Collectors.toList());
            // getNodeInputFlowVariables returns top down, make sure iterations on list return oldest entry first
            // (will be pushed onto node stack using an iterator)
            Collections.reverse(result);
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Copies the tables (port and internal) into the context of the corresponding node in the targetWFM. The execution
     * result must fit to the passed node container.
     *
     * @param execResult the object holding the result of the sourceNC. If the sourceNC is a workflow, this must hold
     *            all results of all contained nodes.
     * @param sourceNC the node that produced the execution result.
     * @param targetNC the context into which the tables are copied into
     * @param progressMon For progress information
     * @param copyDataIntoNewContext as per {@link #setCopyData(boolean)}
     * @throws CanceledExecutionException
     * @throws IOException
     */
    public static void copyExistingTablesIntoSandboxContainer(final NodeContainerExecutionResult execResult,
        final NodeContainer sourceNC, final NodeContainer targetNC, final ExecutionMonitor progressMon,
        final boolean copyDataIntoNewContext) throws CanceledExecutionException, IOException {

        assert targetNC.getNrOutPorts() == sourceNC.getNrOutPorts();

        if (execResult instanceof NativeNodeContainerExecutionResult) {
            NativeNodeContainerExecutionResult sncResult = (NativeNodeContainerExecutionResult)execResult;
            // execResult and node types must match
            assert sourceNC instanceof NativeNodeContainer;
            assert targetNC instanceof NativeNodeContainer;

            // data is to copy ... get the correct execution context
            ExecutionContext targetExec = copyDataIntoNewContext
                    ? ((SingleNodeContainer)targetNC).createExecutionContext() : null;

            NodeExecutionResult ner = sncResult.getNodeExecutionResult();
            // TODO this copy process has to take place in a different place
            // though it needs the final execution context for correct copy
            // of BDT objects

            PortObject[] resultTables = new PortObject[targetNC.getNrOutPorts()];
            int copyCount = resultTables.length;

            // copy also the internally held tables (such as for instance
            // the table in the table view) -- use the copy of the outports
            // if they match (likely they don't)
            PortObject[] oldInternTables = ner.getInternalHeldPortObjects();
            PortObject[] newInternTables = null;
            if (oldInternTables != null) {
                newInternTables = new PortObject[oldInternTables.length];
                copyCount += newInternTables.length;
            }
            // skip flow variable output
            for (int i = 0; i < resultTables.length; i++) {
                ExecutionMonitor sub = progressMon.createSubProgress(1.0 / copyCount);
                progressMon.setMessage("Port " + i);
                PortObject o = ner.getPortObject(i);
                PortObject newPO = copyPortObject(o, sub, targetExec);
                if (newInternTables != null) {
                    for (int j = 0; j < oldInternTables.length; j++) {
                        if (oldInternTables[j] == o) {
                            newInternTables[j] = newPO;
                        }
                    }
                }
                sub.setProgress(1.0);
                resultTables[i] = newPO;
            }
            if (newInternTables != null) {
                for (int i = 0; i < newInternTables.length; i++) {
                    ExecutionMonitor sub = progressMon.createSubProgress(1.0 / copyCount);
                    progressMon.setMessage("Internal Table " + i);
                    if (newInternTables[i] == null) {
                        PortObject oldT = oldInternTables[i];
                        PortObject newT = copyPortObject(oldT, sub, targetExec);
                        newInternTables[i] = newT;
                    }
                    sub.setProgress(1.0);
                }
            }
            if (oldInternTables != null) {
                ner.setInternalHeldPortObjects(newInternTables);
            }
            ner.setPortObjects(resultTables);
        } else if (execResult instanceof WorkflowExecutionResult) {
            WorkflowExecutionResult wfmResult = (WorkflowExecutionResult)execResult;
            // exec result and node types must match
            WorkflowManager targetWFM = (WorkflowManager)targetNC;
            WorkflowManager sourceWFM = (WorkflowManager)sourceNC;
            copyIntoSandboxContainerRecursive(sourceWFM, targetWFM, wfmResult, progressMon, copyDataIntoNewContext);
        } else if (execResult instanceof SubnodeContainerExecutionResult) {
            SubnodeContainerExecutionResult subResult = (SubnodeContainerExecutionResult)execResult;

            WorkflowExecutionResult wfmResult = subResult.getWorkflowExecutionResult();
            WorkflowManager targetWFM = ((SubNodeContainer)targetNC).getWorkflowManager();
            WorkflowManager sourceWFM = ((SubNodeContainer)sourceNC).getWorkflowManager();
            copyIntoSandboxContainerRecursive(sourceWFM, targetWFM, wfmResult, progressMon, copyDataIntoNewContext);
        } else {
            throw new IllegalStateException("Unsupported node result type: " + execResult.getClass().getSimpleName());
        }
    }

    /**
     * @param sourceWFM
     * @param targetWFM
     * @param wfmResult
     * @param progressMon
     * @param copyDataIntoNewContext
     * @throws CanceledExecutionException
     * @throws IOException
     */
    private static void copyIntoSandboxContainerRecursive(final WorkflowManager sourceWFM,
        final WorkflowManager targetWFM, final WorkflowExecutionResult wfmResult, final ExecutionMonitor progressMon,
        final boolean copyDataIntoNewContext) throws CanceledExecutionException, IOException {
        assert wfmResult.getBaseID().equals(sourceWFM.getID());

        Map<NodeID, NodeContainerExecutionResult> resultMap = wfmResult.getExecutionResultMap();

        for (Map.Entry<NodeID, NodeContainerExecutionResult> e : resultMap.entrySet()) {
            ExecutionMonitor sub = progressMon.createSubProgress(1.0 / resultMap.size());
            NodeID sourceID = e.getKey();
            NodeContainerExecutionResult r = e.getValue();
            NodeID targetID = new NodeID(targetWFM.getID(), sourceID.getIndex());
            NodeContainer nextTarget = targetWFM.getNodeContainer(targetID);
            NodeContainer nextSource = sourceWFM.getNodeContainer(sourceID);
            progressMon.setMessage(nextSource.getNameWithID());
            copyExistingTablesIntoSandboxContainer(r, nextSource, nextTarget, sub, copyDataIntoNewContext);
            sub.setProgress(1.0);
        }
    }

    /** Deep clone of port object. */
    private static PortObject copyPortObject(final PortObject oldT,
        final ExecutionMonitor sub, final ExecutionContext targetExec) throws IOException, CanceledExecutionException {
        if (targetExec == null || oldT == null) {
            return oldT;
        }
        return PortObjectRepository.copy(oldT, targetExec, sub);
    }

    /** An instance wrapping a sandboxed node container. It should be {@link #close()}'d when done (e.g. using
     * try-with-resources). */
    public final class SandboxedNode implements AutoCloseable {

        private final WorkflowManager m_wfm;
        private final NodeID m_sandboxNodeID;

        private SandboxedNode(final WorkflowManager wfm, final NodeID sandboxNodeID) {
            m_wfm = CheckUtils.checkArgumentNotNull(wfm);
            m_sandboxNodeID = sandboxNodeID;
        }

        /**
         * @param expectedClass The expected class of the node container (same as original).
         * @return the sandboxed node container.
         */
        public <T extends NodeContainer> T getSandboxNode(final Class<T> expectedClass) {
            return m_wfm.getNodeContainer(m_sandboxNodeID, expectedClass, true);
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
            File location = m_wfm.getContext() != null ? m_wfm.getContext().getCurrentLocation() : null;
            if (!m_copyDataIntoNewContext) {
                copyFileStoreHandlerReference(getSandboxNode(NodeContainer.class), m_nc.getParent(), true);
            }
            m_wfm.getParent().removeNode(m_wfm.getID());
            if (isDeleteOnDiscard()) {
                if (!location.exists() && !FileUtil.deleteRecursively(location)) {
                    LOGGER.warnWithFormat("Could not delete location of temporary sandbox flow (\"%s\")",
                        location.getAbsolutePath());
                }
            }
        }
    }

}
