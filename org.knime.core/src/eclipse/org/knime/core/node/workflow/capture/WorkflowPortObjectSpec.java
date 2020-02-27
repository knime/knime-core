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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ModelContentOutPortView;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.capture.WorkflowFragment.Input;
import org.knime.core.node.workflow.capture.WorkflowFragment.Output;
import org.knime.core.node.workflow.capture.WorkflowFragment.PortID;
import org.knime.core.util.Pair;

/**
 * The workflow port object spec essentially wrapping a {@link WorkflowFragment}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public class WorkflowPortObjectSpec implements PortObjectSpec {

    private WorkflowFragment m_wf;

    private String m_customWorkflowName = null;

    private List<String> m_inputIDs;

    private List<String> m_outputIDs;

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

    private static final String INPUT_ID_PREFIX = "Input";
    private static final String OUTPUT_ID_PREFIX = "Output";

    /**
     * Makes sure that the list of ids are of the expected length.
     *
     * If not, it will either be truncated or extended with running default ids.
     *
     * @param ids the ids to test
     * @param numInputs the expected number of ids
     * @return the fixed list or the same if nothing needed to be fixed
     */
    public static List<String> ensureInputIDsCount(final List<String> ids, final int numInputs) {
        return ensureInputOutputIDsValidity(ids, numInputs, INPUT_ID_PREFIX);
    }

    /**
     * Makes sure that the list of ids are of the expected length.
     *
     * If not, it will either be truncated or extended with running default ids.
     *
     * @param ids the ids to test
     * @param numOutputs the expected number of ids
     * @return the fixed list or the same if nothing needed to be fixed
     */
    public static List<String> ensureOutputIDsCount(final List<String> ids, final int numOutputs) {
        return ensureInputOutputIDsValidity(ids, numOutputs, OUTPUT_ID_PREFIX);
    }

    private static List<String> ensureInputOutputIDsValidity(final List<String> ids, final int expectedSize,
        final String defaultPrefix) {
        if (expectedSize == ids.size()) {
            return ids;
        } else if (expectedSize < ids.size()) {
            return ids.stream().limit(expectedSize).collect(Collectors.toList());
        } else {
            List<String> res = new ArrayList<>(ids);
            for (int i = ids.size(); i < expectedSize; i++) {
                res.add(defaultPrefix + " " + (i + 1));
            }
            return res;
        }
    }

    /**
     * Makes sure that the list of ids doesn't contain duplicates.
     *
     * @param ids the list to check
     * @return an empty optional if there are no duplicates, otherwise a list with fixed ids to be unique ('*' appended)
     */
    public static Optional<List<String>> ensureUniqueness(final List<String> ids) {
        Set<String> set = new HashSet<>(ids);
        if (set.size() == ids.size()) {
            return Optional.empty();
        }
        List<String> res = new ArrayList<>();
        set.clear();
        for (int i = 0; i < ids.size(); i++) {
            if (set.contains(ids.get(i))) {
                res.add(ids.get(i) + "*");
            } else {
                res.add(ids.get(i));
            }
            set.add(res.get(i));
        }
        return Optional.of(res);
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
     * @param customWorkflowName a custom workflow name or <code>null</code> if the name of the original
     *            {@link WorkflowFragment} should be used
     * @param inputIDs a unique id for each input in the order of the inputs
     * @param outputIDs a unique id for each output in the order of the outputs
     */
    public WorkflowPortObjectSpec(final WorkflowFragment wf, final String customWorkflowName,
        final List<String> inputIDs, final List<String> outputIDs) {
        CheckUtils.checkNotNull(wf);
        CheckUtils.checkNotNull(inputIDs);
        CheckUtils.checkNotNull(outputIDs);
        m_wf = wf;
        m_customWorkflowName = customWorkflowName;
        m_inputIDs = ensureInputIDsCount(inputIDs, wf.getConnectedInputs().size());
        m_inputIDs = ensureUniqueness(m_inputIDs).orElse(m_inputIDs);
        m_outputIDs = ensureOutputIDsCount(outputIDs, wf.getConnectedOutputs().size());
        m_outputIDs = ensureUniqueness(m_outputIDs).orElse(m_outputIDs);
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
     * @return a map from id to input with deterministic iteration order(!)
     */
    public Map<String, Input> getInputs() {
        List<Input> inputs = m_wf.getConnectedInputs();
        Map<String, Input> res = new LinkedHashMap<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            res.put(m_inputIDs.get(i), inputs.get(i));
        }
        return res;
    }

    /**
     * @return a map from id to output with deterministic iteration order(!)
     */
    public Map<String, Output> getOutputs() {
        List<Output> outputs = m_wf.getConnectedOutputs();
        Map<String, Output> res = new LinkedHashMap<>(outputs.size());
        for (int i = 0; i < outputs.size(); i++) {
            res.put(m_outputIDs.get(i), outputs.get(i));
        }
        return res;
    }

    /**
     * @return the list of unique input ids in order
     */
    public List<String> getInputIDs() {
        return m_inputIDs;
    }

    /**
     * @return the list of unique output ids in order
     */
    public List<String> getOutputIDs() {
        return m_outputIDs;
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
        saveInputs(inputPorts, m_wf.getConnectedInputs(), m_inputIDs);

        ModelContentWO outputPorts = model.addModelContent("output_ports");
        saveOutputs(outputPorts, m_wf.getConnectedOutputs(), m_outputIDs);

        if (m_customWorkflowName != null) {
            model.addString("custom_workflow_name", m_customWorkflowName);
        }
    }

    private static void saveInputs(final ModelContentWO model, final List<Input> inputs, final List<String> inputIDs) {
        model.addInt("num_inputs", inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            Input input = inputs.get(i);
            Config inputConf = model.addConfig("input_" + i);
            if (input.getType().isPresent()) {
                Config type = inputConf.addConfig("type");
                savePortType(type, input.getType().get());
            }
            Optional<DataTableSpec> optionalSpec = input.getSpec();
            if (optionalSpec.isPresent()) {
                Config spec = inputConf.addConfig("spec");
                saveSpec(spec, input.getSpec().get());
            }

            Set<PortID> connectedPorts = input.getConnectedPorts();
            Config portsConf = inputConf.addConfig("connected_ports");
            portsConf.addInt("num_ports", connectedPorts.size());
            int j = 0;
            for (PortID pid : connectedPorts) {
                Config portConf = portsConf.addConfig("port_" + j);
                savePortID(portConf, pid);
                j++;
            }
            inputConf.addString("id", inputIDs.get(i));
        }
    }

    private static void saveOutputs(final ModelContentWO model, final List<Output> outputs,
        final List<String> outputIDs) {
        model.addInt("num_outputs", outputs.size());
        for (int i = 0; i < outputs.size(); i++) {
            Output output = outputs.get(i);
            Config outputConf = model.addConfig("output_" + i);
            if (output.getType().isPresent()) {
                Config type = outputConf.addConfig("type");
                savePortType(type, output.getType().get());
            }
            Optional<DataTableSpec> optionalSpec = output.getSpec();
            if (optionalSpec.isPresent()) {
                Config spec = outputConf.addConfig("spec");
                saveSpec(spec, output.getSpec().get());
            }

            Optional<PortID> connectedPort = output.getConnectedPort();
            if (connectedPort.isPresent()) {
                Config portConf = outputConf.addConfig("connected_port");
                savePortID(portConf, connectedPort.get());
            }
            outputConf.addString("id", outputIDs.get(i));
        }
    }

    static void savePortID(final Config config, final PortID portID) {
        config.addString("node_id", portID.getNodeIDSuffix().toString());
        config.addInt("index", portID.getIndex());
    }

    private static void savePortType(final Config typeConf, final PortType type) {
        if (type != null) {
            typeConf.addString("portObjectClass", type.getPortObjectClass().getCanonicalName());
            typeConf.addBoolean("isOptional", type.isOptional());
        }
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
        Pair<List<Input>, List<String>> inputs = loadInputs(model);

        model = metadata.getModelContent("output_ports");
        Pair<List<Output>, List<String>> outputs = loadOutputs(model);

        String customWfName = null;
        if (metadata.containsKey("custom_workflow_name")) {
            customWfName = metadata.getString("custom_workflow_name");
        }

        WorkflowFragment wf =
            new WorkflowFragment(metadata.getString("name"), inputs.getFirst(), outputs.getFirst(), ids);
        return new WorkflowPortObjectSpec(wf, customWfName, inputs.getSecond(), outputs.getSecond());
    }

    private static Pair<List<Input>, List<String>> loadInputs(final ModelContentRO model)
        throws InvalidSettingsException {
        int size = model.getInt("num_inputs");
        List<Input> inputs = new ArrayList<>(size);
        List<String> inputIDs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Config inputConf = model.getConfig("input_" + i);
            Set<PortID> connectedPorts = new HashSet<>();
            Config portsConf = inputConf.getConfig("connected_ports");
            for (int j = 0; j < portsConf.getInt("num_ports"); j++) {
                connectedPorts.add(loadPortID(portsConf.getConfig("port_" + j)));
            }
            Input input = new Input(inputConf.containsKey("type") ? loadPortType(inputConf.getConfig("type")) : null,
                inputConf.containsKey("spec") ? loadTableSpec(inputConf.getConfig("spec")) : null, connectedPorts);
            inputs.add(input);
            inputIDs.add(inputConf.getString("id"));
        }
        return Pair.create(inputs, inputIDs);
    }

    private static Pair<List<Output>, List<String>> loadOutputs(final ModelContentRO model)
        throws InvalidSettingsException {
        int size = model.getInt("num_outputs");
        List<Output> outputs = new ArrayList<>(size);
        List<String> outputIDs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Config outputConf = model.getConfig("output_" + i);
            PortID connectedPort = null;
            if (outputConf.containsKey("connected_port")) {
                connectedPort = loadPortID(outputConf.getConfig("connected_port"));
            }
            Output output =
                new Output(outputConf.containsKey("type") ? loadPortType(outputConf.getConfig("type")) : null,
                    outputConf.containsKey("spec") ? loadTableSpec(outputConf.getConfig("spec")) : null, connectedPort);
            outputs.add(output);
            outputIDs.add(outputConf.getString("id"));
        }
        return Pair.create(outputs, outputIDs);
    }

    static PortID loadPortID(final Config portConf) throws InvalidSettingsException {
        return new PortID(NodeIDSuffix.fromString(portConf.getString("node_id")), portConf.getInt("index"));
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
