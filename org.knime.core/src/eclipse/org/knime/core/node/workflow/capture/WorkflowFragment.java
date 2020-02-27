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
 *   10 Dec 2019 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.workflow.capture;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.WorkflowSaveHelper;
import org.knime.core.util.FileUtil;
import org.knime.core.util.FileUtil.ZipFileFilter;
import org.knime.core.util.LockFailedException;
import org.knime.core.util.VMFileLocker;

/**
 * Represents a sub-workflow by keeping it's own {@link WorkflowManager}-instance - similar to {@link SubNodeContainer}. Unlike
 * the {@link SubNodeContainer}, a workflow fragment
 * <ul>
 * <li>is not part of the {@link NodeContainer} hierarchy</li>
 * <li>the input- and output ports a represented by {@link Port}-objects (instead of extra nodes within the same
 * workflow)</li>
 * <li>it has metadata that is accessible without loading the contained {@link WorkflowManager}, such as name, input-
 * and output ports (id, index, type), ids of so called 'object reference reader nodes'</li>
 * <li>it maintains a list of node id suffixes of 'object reference reader nodes' (as metadata) that represents
 * additional static input data (e.g. a decision tree model)</li>
 * </ul>
 *
 * Workflow fragment instances are returned by {@link WorkflowManager#createCaptureOperationFor(NodeID)}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 4.2
 */
public final class WorkflowFragment {

    //cached workflow manager
    private WorkflowManager m_wfm = null;

    private byte[] m_wfmStream = null;

    private final String m_name;

    private final Set<NodeIDSuffix> m_portObjectReferenceReaderNodes;

    private final List<Input> m_inputs;

    private final List<Output> m_outputs;

    /**
     * Creates a new instance.
     *
     * @param wfm the workflow manager representing the workflow fragment
     * @param inputs workflow fragment's inputs
     * @param outputs workflow fragment's outputs
     * @param portObjectReferenceReaderNodes relative node ids of nodes that reference port objects in another workflow
     */
    public WorkflowFragment(final WorkflowManager wfm, final List<Input> inputs, final List<Output> outputs,
        final Set<NodeIDSuffix> portObjectReferenceReaderNodes) {
        m_wfm = wfm;
        m_name = wfm.getName();
        m_inputs = CheckUtils.checkArgumentNotNull(inputs);
        m_outputs = CheckUtils.checkArgumentNotNull(outputs);
        m_portObjectReferenceReaderNodes = CheckUtils.checkArgumentNotNull(portObjectReferenceReaderNodes);
    }

    /**
     * Constructor for de-serialization. Initializes the workflow fragment exclusively with metadata. The actual
     * workflow data is subsequently loaded via {@link #loadWorkflowData(ZipInputStream)}.
     *
     * @param name
     * @param inputs
     * @param outputs
     * @param portObjectReferenceReaderNodes
     */
    WorkflowFragment(final String name, final List<Input> inputs, final List<Output> outputs,
        final Set<NodeIDSuffix> portObjectReferenceReaderNodes) {
        m_name = CheckUtils.checkArgumentNotNull(name);
        m_inputs = CheckUtils.checkArgumentNotNull(inputs);
        m_outputs = CheckUtils.checkArgumentNotNull(outputs);
        m_portObjectReferenceReaderNodes = CheckUtils.checkArgumentNotNull(portObjectReferenceReaderNodes);
    }

    /**
     * Loads the workflow representing the fragment.
     *
     * Always call {@link #disposeWorkflow()} if the returned workflow manager is not needed anymore!
     *
     * This method (i.e. lazily loading the workflow) might become unnecessary in the future once the workflow manager
     * can be de-/serialized directly to/from a stream.
     *
     * @return the workflow manager representing the fragment
     */
    public WorkflowManager loadWorkflow() {
        if (m_wfm == null) {
            File tmpDir = null;
            try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(m_wfmStream))) {
                tmpDir = FileUtil.createTempDir("workflow_fragment");
                FileUtil.unzip(in, tmpDir, 1);
                WorkflowLoadHelper loadHelper =
                    new WorkflowLoadHelper(new WorkflowContext.Factory(tmpDir).createContext());
                WorkflowLoadResult loadResult;
                loadResult =
                    WorkflowManager.EXTRACTED_WORKFLOW_ROOT.load(tmpDir, new ExecutionMonitor(), loadHelper, false);
                m_wfm = loadResult.getWorkflowManager();
            } catch (InvalidSettingsException | CanceledExecutionException | UnsupportedWorkflowVersionException
                    | LockFailedException | IOException ex) {
                // should never happen
                throw new IllegalStateException("Failed loading workflow port object", ex);
            }
        }
        return m_wfm;
    }

    /**
     * Disposes the workflow manager cached by this fragment (either loaded via {@link #loadWorkflow()} or passed to the
     * constructor). Removes it from the workflow hierarchy and the local reference.
     */
    public void disposeWorkflow() {
        if (m_wfm != null) {
            WorkflowManager.EXTRACTED_WORKFLOW_ROOT.removeNode(m_wfm.getID());
            m_wfm = null;
        }
    }

    /**
     * Disposes the workflow manager cached by this fragment (either loaded via {@link #loadWorkflow()} or passed to the
     * constructor). Removes it from the workflow hierarchy and the local reference.
     *
     * If not already done, also serializes the workflow to the internally kept byte stream for later retrieval.
     *
     * This method only needs to be called if the {@link WorkflowFragment} has been initialized with a new
     * {@link WorkflowManager}. In all other cases {@link #disposeWorkflow()} is sufficient.
     *
     * @throws IOException thrown if persisting the workflow to the internal in-memory byte stream failed
     */
    public void serializeAndDisposeWorkflow() throws IOException {
        if (m_wfm != null && m_wfmStream == null) {
            m_wfmStream = wfmToStream(m_wfm);
        }
        disposeWorkflow();
    }

    private static byte[] wfmToStream(final WorkflowManager wfm) throws IOException {
        File tmpDir = FileUtil.createTempDir("workflow_fragment");
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ZipOutputStream out = new ZipOutputStream(bos);) {
            WorkflowSaveHelper saveHelper = new WorkflowSaveHelper(false, false);
            wfm.save(tmpDir, saveHelper, new ExecutionMonitor());
            FileUtil.zipDir(out, Collections.singleton(tmpDir), new ZipFileFilter() {
                @Override
                public boolean include(final File f) {
                    return !f.getName().equals(VMFileLocker.LOCK_FILE);
                }
            }, null);
            bos.flush();
            return bos.toByteArray();
        } catch (LockFailedException | CanceledExecutionException | IOException e) {
            throw new IOException("Failed saving workflow port object", e);
        } finally {
            FileUtil.deleteRecursively(tmpDir.getAbsoluteFile());
        }
    }

    void loadWorkflowData(final ZipInputStream in) throws IOException {
        ZipEntry entry = in.getNextEntry();
        if (!entry.getName().equals("workflow.bin")) {
            throw new IOException("Expected workflow.bin file in stream, got " + entry.getName());
        }
        m_wfmStream = IOUtils.toByteArray(in);
    }

    /**
     * @throws IOException
     *
     */
    void saveWorkflowData(final ZipOutputStream out) throws IOException {
        if (m_wfmStream == null) {
            if (m_wfm == null) {
                //only happens if WorkflowFragment is instantiated with a WorkflowManager
                //and #disposeWorkflow() is called before #save(...)
                throw new IllegalStateException("Can't save workflow fragment. Workflow has been disposed already.");
            }
            m_wfmStream = wfmToStream(m_wfm);
        }
        out.putNextEntry(new ZipEntry("workflow.bin"));
        out.write(m_wfmStream);
        out.closeEntry();
    }

    /**
     * @return the workflow name as stored with the fragment's metadata
     */
    String getName() {
        return m_name;
    }

    /**
     * @return relative node ids of nodes that reference port objects in another workflow (TODO revisit)
     */
    public Set<NodeIDSuffix> getPortObjectReferenceReaderNodes() {
        return m_portObjectReferenceReaderNodes;
    }

    /**
     * @return workflow fragment's inputs that are connected to at least one node (in the order of the capture node
     *         start ports)
     */
    public List<Input> getConnectedInputs() {
        return m_inputs.stream().filter(Input::isConnected).collect(Collectors.toList());
    }

    /**
     * @return workflow fragment's output ports that are connected to a node (in the order of the capture node end
     *         ports)
     */
    public List<Output> getConnectedOutputs() {
        return m_outputs.stream().filter(Output::isConnected).collect(Collectors.toList());
    }

    /**
     * Represents a input/output in a workflow fragment enriched with some additional information.
     */
    public abstract static class IOInfo {

        private final PortType m_type;

        private final DataTableSpec m_spec;

        /**
         * Creates an new input marker.
         *
         * @param type can be <code>null</code> if type couldn't be determined (final because the respective plugin is not
         *            installed)
         * @param spec can be <code>null</code>
         */
        public IOInfo(final PortType type, final DataTableSpec spec) {
            m_spec = spec;
            m_type = type;
        }

        /**
         * @return port type or an empty optional if port type couldn't be determined (e.g. because the respective
         *         plugin is not installed)
         */
        public Optional<PortType> getType() {
            return Optional.ofNullable(m_type);
        }

        /**
         * @return the data table spec or an empty optional (if not persisted with the port)
         */
        public Optional<DataTableSpec> getSpec() {
            return Optional.ofNullable(m_spec);
        }

    }

    /**
     * Represents an input of a workflow fragment.
     */
    public static final class Input extends IOInfo {

        private Set<PortID> m_connectedPorts;

        /**
         * @param type
         * @param spec
         * @param connectedPorts set of ports the input is connected to
         */
        public Input(final PortType type, final DataTableSpec spec, final Set<PortID> connectedPorts) {
            super(type, spec);
            m_connectedPorts = connectedPorts;
        }

        /**
         * @return set of ports the input is connected to
         *
         */
        public Set<PortID> getConnectedPorts() {
            return m_connectedPorts;
        }

        /**
         * @return whether the fragment input is connected to at least one node port
         */
        public boolean isConnected() {
            return !m_connectedPorts.isEmpty();
        }
    }

    /**
     * Represents an output of a workflow fragment.
     */
    public static final class Output extends IOInfo {

        private PortID m_connectedPort;

        /**
         * @param type
         * @param spec
         * @param connectedPort the port that is connected to this output, can be <code>null</code> if nothing is
         *            connected
         */
        public Output(final PortType type, final DataTableSpec spec, final PortID connectedPort) {
            super(type, spec);
            m_connectedPort = connectedPort;
        }

        /**
         * @return the port which is connected to this output, or an empty optional if none connected
         */
        public Optional<PortID> getConnectedPort() {
            return Optional.ofNullable(m_connectedPort);
        }

        /**
         * @return whether a node port is connected to the fragment output
         */
        public boolean isConnected() {
            return m_connectedPort != null;
        }
    }

    /**
     * References/marks ports in the workflow fragment by node id suffix and port index.
     */
    public static final class PortID {

        private NodeIDSuffix m_nodeIDSuffix;

        private int m_index;

        /**
         * Creates a new port id represented by node id suffix and port index.
         *
         * @param nodeIDSuffix
         * @param index
         */
        public PortID(final NodeIDSuffix nodeIDSuffix, final int index) {
            m_nodeIDSuffix = CheckUtils.checkArgumentNotNull(nodeIDSuffix);
            if (index < 0) {
                throw new IndexOutOfBoundsException(String.format("Port index %d out of bounds.", index));
            }
            m_nodeIDSuffix = nodeIDSuffix;
            m_index = index;
        }

        /**
         * @return node id suffix relative to workflow
         */
        public NodeIDSuffix getNodeIDSuffix() {
            return m_nodeIDSuffix;
        }

        /**
         * @return port index
         */
        public int getIndex() {
            return m_index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Node #" + m_nodeIDSuffix + " | Port #" + m_index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            HashCodeBuilder hash = new HashCodeBuilder().append(m_nodeIDSuffix).append(m_index);
            return hash.build();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof PortID)) {
                return false;
            }
            PortID other = (PortID)obj;
            return new EqualsBuilder().append(m_nodeIDSuffix, other.m_nodeIDSuffix).append(m_index, other.m_index)
                .build();
        }
    }
}
