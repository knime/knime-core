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
 *   Dec 9, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.workflow.capture;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.workflow.ModelContentOutPortView;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.capture.WorkflowFragment.Port;
import org.knime.core.node.workflow.capture.WorkflowFragment.PortID;

/**
 * The workflow port object spec essentially wrapping a {@link WorkflowFragment}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public class WorkflowPortObjectSpec implements PortObjectSpec {

    private WorkflowFragment m_wf;

    private final Map<PortID, String> m_inPortNames = new HashMap<>();

    private final Map<PortID, String> m_outPortNames = new HashMap<>();

    private String m_customWorkflowName = null;

    /**
     * Serializer as registered in extension point.
     */
    public static final class Serializer extends PortObjectSpecSerializer<WorkflowPortObjectSpec> {
        /** {@inheritDoc} */
        @Override
        public WorkflowPortObjectSpec loadPortObjectSpec(final PortObjectSpecZipInputStream in) throws IOException {
            ZipEntry entry = in.getNextEntry();
            if (!entry.getName().equals("metadata.xml")) {
                throw new IOException("Expected metadata.xml file in stream, got " + entry.getName());
            }

            try (InputStream noneCloseIn = new NonClosableInputStream.Zip(in)) {
                ModelContentRO metadata = ModelContent.loadFromXML(noneCloseIn);
                WorkflowPortObjectSpec spec = loadSpecMetadata(metadata);
                spec.getWorkflowFragment().loadWorkflowData(in);
                return spec;
            } catch (InvalidSettingsException e) {
                throw new IOException("Failed loading workflow port object", e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void savePortObjectSpec(final WorkflowPortObjectSpec portObjectSpec,
            final PortObjectSpecZipOutputStream out) throws IOException {
            out.putNextEntry(new ZipEntry("metadata.xml"));
            ModelContent metadata = new ModelContent("metadata.xml");
            portObjectSpec.saveSpecMetadata(metadata);
            try (final NonClosableOutputStream.Zip zout = new NonClosableOutputStream.Zip(out)) {
                metadata.saveToXML(zout);
            }
            portObjectSpec.m_wf.saveWorkflowData(out);
        }
    }

    /**
     * Don't use, framework constructor.
     */
    public WorkflowPortObjectSpec() {
        //
    }

    /**
     * Constructor.
     *
     * @param wf the workflow fragment to use
     */
    public WorkflowPortObjectSpec(final WorkflowFragment wf) {
        this(wf, null, null, null);
    }

    /**
     * Constructor.
     *
     * @param wf the workflow fragment to use
     * @param customWorkflowName a custom workflow name or <code>null</code> if the name of the original
     *            {@link WorkflowFragment} should be used
     * @param inPortNames optional names for the input ports, can be <code>null</code>
     * @param outPortNames optional names for the input ports, can be <code>null</code>
     */
    public WorkflowPortObjectSpec(final WorkflowFragment wf, final String customWorkflowName, final Map<PortID, String> inPortNames,
        final Map<PortID, String> outPortNames) {
        m_wf = wf;
        m_customWorkflowName = customWorkflowName;
        if (inPortNames != null) {
            m_inPortNames.putAll(inPortNames);
        }
        if (outPortNames != null) {
            m_outPortNames.putAll(outPortNames);
        }
    }

    /**
     * @return the workflow fragment
     */
    public WorkflowFragment getWorkflowFragment() {
        return m_wf;
    }

    /**
     * @return the workflow name
     */
    public String getWorkflowName() {
        return m_customWorkflowName == null ? m_wf.getName() : m_customWorkflowName;
    }

    /**
     * @return a map from port id to port name
     */
    public Map<PortID, String> getInputPortNamesMap() {
        return new HashMap<>(m_inPortNames);
    }

    /**
     * @return a map from port id to port name
     */
    public Map<PortID, String> getOutputPortNamesMap() {
        return new HashMap<>(m_outPortNames);
    }

    /**
     * Saves the workflow port object spec data to the supplied model object (without the actual workflow data!)
     *
     * @param model the model to save the metadata to
     */
    public void saveSpecMetadata(final ModelContentWO model) {
        model.addString("name", m_wf.getName());

        ModelContentWO refNodeIds = model.addModelContent("ref_node_ids");
        refNodeIds.addInt("num_ids", m_wf.getPortObjectReferenceReaderNodes().size());
        int i = 0;
        for (NodeIDSuffix id : m_wf.getPortObjectReferenceReaderNodes()) {
            refNodeIds.addIntArray("ref_node_id_" + i, id.getSuffixArray());
            i++;
        }

        ModelContentWO inputPorts = model.addModelContent("input_ports");
        savePorts(inputPorts, m_wf.getInputPorts());

        ModelContentWO outputPorts = model.addModelContent("output_ports");
        savePorts(outputPorts, m_wf.getOutputPorts());


        if (!m_inPortNames.isEmpty()) {
            ModelContentWO inPortNames = model.addModelContent("input_port_names");
            savePortNames(inPortNames, m_inPortNames);
        }

        if (!m_outPortNames.isEmpty()) {
            ModelContentWO outPortNames = model.addModelContent("output_port_names");
            savePortNames(outPortNames, m_outPortNames);
        }

        if (m_customWorkflowName != null) {
            model.addString("custom_workflow_name", m_customWorkflowName);
        }
    }

    private static void savePorts(final ModelContentWO model, final List<Port> ports) {
        model.addInt("num_ports", ports.size());
        for (int i = 0; i < ports.size(); i++) {
            Config portConf = model.addConfig("port_" + i);
            savePortID(portConf, ports.get(i).getID());
            Config type = portConf.addConfig("type");
            savePortType(type, ports.get(i).getType().get());
            Optional<DataTableSpec> optionalSpec = ports.get(i).getSpec();
            if (optionalSpec.isPresent()) {
                Config spec = portConf.addConfig("spec");
                saveSpec(spec, ports.get(i).getSpec().get());
            }
        }
    }

    static void savePortID(final Config config, final PortID portID) {
        config.addString("node_id", portID.getNodeIDSuffix().toString());
        config.addInt("index", portID.getIndex());
    }

    private static void savePortNames(final ModelContentWO model, final Map<PortID, String> portNames) {
        model.addInt("num_port_names", portNames.size());
        int i = 0;
        for (Entry<PortID, String> port : portNames.entrySet()) {
            Config portConf = model.addConfig("port_" + i);
            savePortID(portConf, port.getKey());
            portConf.addString("name", port.getValue());
            i++;
        }
    }

    private static void savePortType(final Config typeConf, final PortType type) {
        typeConf.addString("portObjectClass", type.getPortObjectClass().getCanonicalName());
        typeConf.addBoolean("isOptional", type.isOptional());
    }

    private static void saveSpec(final Config specConf, final DataTableSpec spec) {
        DataTableSpec dtspec = spec;
        dtspec.save(specConf);
    }

    /*
     * Helper to load the spec metadata, including the workflow fragment metadata
     * (returned as workflow fragment with pre-initialized metadata only).
     * The direct spec's metadata is set directly.
     */
    private static WorkflowPortObjectSpec loadSpecMetadata(final ModelContentRO metadata) throws InvalidSettingsException {
        ModelContentRO refNodeIds = metadata.getModelContent("ref_node_ids");
        Set<NodeIDSuffix> ids = new HashSet<>();
        int numIds = refNodeIds.getInt("num_ids");
        for (int i = 0; i < numIds; i++) {
            ids.add(new NodeIDSuffix(refNodeIds.getIntArray("ref_node_id_" + i)));
        }
        ModelContentRO model = metadata.getModelContent("input_ports");
        List<Port> inputPorts = loadPorts(model);

        model = metadata.getModelContent("output_ports");
        List<Port> outputPorts = loadPorts(model);

        Map<PortID, String> inPortNames = null;
        if (metadata.containsKey("input_port_names")) {
            model = metadata.getModelContent("input_port_names");
            inPortNames = loadPortNames(model);
        }

        Map<PortID, String> outPortNames = null;
        if (metadata.containsKey("output_port_names")) {
            model = metadata.getModelContent("output_port_names");
            outPortNames = loadPortNames(model);
        }

        String customWfName = null;
        if (metadata.containsKey("custom_workflow_name")) {
            customWfName = metadata.getString("custom_workflow_name");
        }

        WorkflowFragment wf = new WorkflowFragment(metadata.getString("name"), inputPorts, outputPorts, ids);
        return new WorkflowPortObjectSpec(wf, customWfName, inPortNames, outPortNames);
    }

    private static List<Port> loadPorts(final ModelContentRO model) throws InvalidSettingsException {
        int size = model.getInt("num_ports");
        List<Port> ports = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Config portConf = model.getConfig("port_" + i);
            ports.add(new Port(loadPortID(portConf), loadPortType(portConf.getConfig("type")),
                portConf.containsKey("spec") ? loadTableSpec(portConf.getConfig("spec")) : null));
        }
        return ports;
    }

    static PortID loadPortID(final Config portConf) throws InvalidSettingsException {
        return new PortID(NodeIDSuffix.fromString(portConf.getString("node_id")), portConf.getInt("index"));
    }

    private static Map<PortID, String> loadPortNames(final ModelContentRO model) throws InvalidSettingsException {
        int num = model.getInt("num_port_names");
        Map<PortID, String> res = new HashMap<>(num);
        for (int i = 0; i < num; i++) {
            Config portConf = model.getConfig("port_" + i);
            res.put(loadPortID(portConf), portConf.getString("name"));
        }
        return res;
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

    private static DataTableSpec loadTableSpec(final Config spec) throws InvalidSettingsException {
        return DataTableSpec.load(spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        ModelContent model = new ModelContent("Workflow Fragment Metadata");
        saveSpecMetadata(model);
        return new JComponent[]{new ModelContentOutPortView(model)};
    }
}
