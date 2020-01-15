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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
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
 * Workflow fragment instances are returned by {@link WorkflowManager#capturePartOf(NodeID)}.
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

    private final List<Port> m_inputPorts;

    private final List<Port> m_outputPorts;

    /**
     * Creates a new instance.
     *
     * @param wfm the workflow manager representing the workflow fragment
     * @param inputPorts workflow fragment's input ports
     * @param outputPorts workflow fragment's output ports
     * @param portObjectReferenceReaderNodes relative node ids of nodes that reference port objects in another workflow
     */
    public WorkflowFragment(final WorkflowManager wfm, final List<Port> inputPorts, final List<Port> outputPorts,
        final Set<NodeIDSuffix> portObjectReferenceReaderNodes) {
        m_wfm = wfm;
        m_name = wfm.getName();
        m_inputPorts = CheckUtils.checkArgumentNotNull(inputPorts);
        m_outputPorts = CheckUtils.checkArgumentNotNull(outputPorts);
        m_portObjectReferenceReaderNodes = CheckUtils.checkArgumentNotNull(portObjectReferenceReaderNodes);
    }

    private WorkflowFragment(final byte[] wfmStream, final String name, final List<Port> inputPorts,
        final List<Port> outputPorts, final Set<NodeIDSuffix> portObjectReferenceReaderNodes) {
        m_wfmStream = wfmStream;
        m_name = CheckUtils.checkArgumentNotNull(name);
        m_inputPorts = CheckUtils.checkArgumentNotNull(inputPorts);
        m_outputPorts = CheckUtils.checkArgumentNotNull(outputPorts);
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
            } finally {
                if (tmpDir != null) {
                    FileUtil.deleteRecursively(tmpDir.getAbsoluteFile());
                }
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
     * @return the workflow name as stored with the fragment's metadata
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return relative node ids of nodes that reference port objects in another workflow (TODO revisit)
     */
    public Set<NodeIDSuffix> getPortObjectReferenceReaderNodes() {
        return m_portObjectReferenceReaderNodes;
    }

    /**
     * @return workflow fragment's input ports
     */
    public List<Port> getInputPorts() {
        return m_inputPorts;
    }

    /**
     * @return workflow fragment's output ports
     */
    public List<Port> getOutputPorts() {
        return m_outputPorts;
    }

    /**
     * Saves the workflow fragment to a zip output stream.
     *
     * @param out the stream to write to
     * @throws IOException if serializing of the workflow fails (output stream will not be closed)
     */
    public void save(final ZipOutputStream out) throws IOException {
        out.putNextEntry(new ZipEntry("metadata.xml"));
        ModelContent metadata = new ModelContent("metadata.xml");
        metadata.addString("name", m_name);

        ModelContentWO refNodeIds = metadata.addModelContent("ref_node_ids");
        refNodeIds.addInt("num_ids", m_portObjectReferenceReaderNodes.size());
        int i = 0;
        for (NodeIDSuffix id : m_portObjectReferenceReaderNodes) {
            refNodeIds.addIntArray("ref_node_id_" + i, id.getSuffixArray());
            i++;
        }

        ModelContentWO inputPorts = metadata.addModelContent("input_ports");
        savePorts(inputPorts, m_inputPorts);

        ModelContentWO outputPorts = metadata.addModelContent("output_ports");
        savePorts(outputPorts, m_outputPorts);

        try (final NonClosableOutputStream.Zip zout = new NonClosableOutputStream.Zip(out)) {
            metadata.saveToXML(zout);
        }

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

    /**
     * Loads a workflow fragment from an zip input stream.
     *
     * @param in the stream to read from
     * @return a new {@link WorkflowFragment} (input stream is not closed)
     * @throws IOException if a problem occurred while reading (input stream will not be closed)
     */
    public static WorkflowFragment load(final ZipInputStream in) throws IOException {
        ZipEntry entry = in.getNextEntry();
        if (!entry.getName().equals("metadata.xml")) {
            throw new IOException("Expected metadata.xml file in stream, got " + entry.getName());
        }

        try (InputStream noneCloseIn = new NonClosableInputStream.Zip(in)) {
            ModelContentRO metadata = ModelContent.loadFromXML(noneCloseIn);
            ModelContentRO refNodeIds = metadata.getModelContent("ref_node_ids");
            Set<NodeIDSuffix> ids = new HashSet<NodeID.NodeIDSuffix>();
            int numIds = refNodeIds.getInt("num_ids");
            for (int i = 0; i < numIds; i++) {
                ids.add(new NodeIDSuffix(refNodeIds.getIntArray("ref_node_id_" + i)));
            }
            ModelContentRO model = metadata.getModelContent("input_ports");
            List<Port> inputPorts = loadPorts(model);

            model = metadata.getModelContent("output_ports");
            List<Port> outputPorts = loadPorts(model);

            entry = in.getNextEntry();
            if (!entry.getName().equals("workflow.bin")) {
                throw new IOException("Expected workflow.bin file in stream, got " + entry.getName());
            }

            byte[] wfmStream = IOUtils.toByteArray(in);

            return new WorkflowFragment(wfmStream, metadata.getString("name"), inputPorts, outputPorts, ids);
        } catch (InvalidSettingsException e) {
            throw new IOException("Failed loading workflow port object", e);
        }
    }

    private static void savePorts(final ModelContentWO model, final List<Port> ports) {
        model.addInt("num_ports", ports.size());
        for (int i = 0; i < ports.size(); i++) {
            Config portConf = model.addConfig("port_" + i);
            portConf.addString("node_id", ports.get(i).getNodeIDSuffix().toString());
            portConf.addInt("index", ports.get(i).getIndex());
            Config type = portConf.addConfig("type");
            savePortType(type, ports.get(i).getType().get());
        }
    }

    private static void savePortType(final Config typeConf, final PortType type) {
        typeConf.addString("portObjectClass", type.getPortObjectClass().getCanonicalName());
        typeConf.addBoolean("isOptional", type.isOptional());
    }

    private static List<Port> loadPorts(final ModelContentRO model) throws InvalidSettingsException {
        int size = model.getInt("num_ports");
        List<Port> ports = new ArrayList<WorkflowFragment.Port>(size);
        for (int i = 0; i < size; i++) {
            Config portConf = model.getConfig("port_" + i);
            ports.add(new Port(NodeIDSuffix.fromString(portConf.getString("node_id")), portConf.getInt("index"),
                loadPortType(portConf.getConfig("type"))));
        }
        return ports;
    }

    private static PortType loadPortType(final Config type) throws InvalidSettingsException {
        PortTypeRegistry pte = PortTypeRegistry.getInstance();
        Optional<Class<? extends PortObject>> objectClass = pte.getObjectClass(type.getString("portObjectClass"));
        if (objectClass.isPresent()) {
            return pte.getPortType(objectClass.get(), type.getBoolean("isOptional"));
        } else {
            return null;
        }
    }

    /**
     * References/marks ports in the workflow fragment by node id and index.
     */
    public static final class Port {
        private final NodeIDSuffix m_nodeIDSuffix;

        private final int m_idx;

        private final PortType m_type;

        /**
         * Creates an new port marker.
         *
         * @param nodeIDSuffix the node's id
         * @param idx port index
         * @param type - can be <code>null</code> if type couldn't be determined (because the respective plugin is not
         *            installed)
         */
        public Port(final NodeIDSuffix nodeIDSuffix, final int idx, final PortType type) {
            m_nodeIDSuffix = CheckUtils.checkArgumentNotNull(nodeIDSuffix);
            if (idx < 0) {
                throw new IllegalArgumentException(String.format("Port index %d out of bounds.", idx));
            }
            m_idx = idx;
            m_type = type;
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
            return m_idx;
        }

        /**
         * @return port type or an empty optional if port type couldn't be determined (e.g. because the respective
         *         plugin is not installed)
         */
        public Optional<PortType> getType() {
            return Optional.ofNullable(m_type);
        }
    }
}
