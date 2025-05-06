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
 *   May 6, 2025 (hornm): created
 */
package org.knime.core.node.workflow.capture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.capture.WorkflowSegment.Input;
import org.knime.core.node.workflow.capture.WorkflowSegment.Output;
import org.knime.core.node.workflow.capture.WorkflowSegment.PortID;
import org.knime.core.util.Pair;

/**
 * TODO shared de-/serialization logic between {@link WorkflowPortObjectSpec} and {@link WorkflowSegment}
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
// TODO naming
final class SerializationUtil {

    private SerializationUtil() {
        // utility class
    }

    static void saveMetadata(final WorkflowSegment ws, final ModelContentWO model) throws IOException {
        saveMetadata(ws, null, null, null, model);
    }

    static void saveMetadata(final WorkflowSegment ws, final String customWorkflowName, final List<String> inputIDs,
        final List<String> outputIDs, final ModelContentWO model) {
        model.addString("name", ws.getName());

        ModelContentWO refNodeIds = model.addModelContent("ref_node_ids");
        refNodeIds.addInt("num_ids", ws.getPortObjectReferenceReaderNodes().size());
        int i = 0;
        for (NodeIDSuffix id : ws.getPortObjectReferenceReaderNodes()) {
            refNodeIds.addIntArray("ref_node_id_" + i, id.getSuffixArray());
            i++;
        }

        ModelContentWO inputPorts = model.addModelContent("input_ports");
        saveInputs(inputPorts, ws.getConnectedInputs(), inputIDs);

        ModelContentWO outputPorts = model.addModelContent("output_ports");
        saveOutputs(outputPorts, ws.getConnectedOutputs(), outputIDs);

        if (customWorkflowName != null) {
            model.addString("custom_workflow_name", customWorkflowName);
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
            if (inputIDs != null) {
                inputConf.addString("id", inputIDs.get(i));
            }
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

    private static void savePortID(final Config config, final PortID portID) {
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


    static WorkflowMetadata loadMetadata(final ModelContentRO metadata) throws InvalidSettingsException {
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
        return new WorkflowMetadata(metadata.getString("name"), customWfName, inputs.getFirst(), inputs.getSecond(),
            outputs.getFirst(), outputs.getSecond(), ids);
    }

    record WorkflowMetadata(String name, String customWorkflowName, List<Input> inputs, List<String> inputIDs,
        List<Output> outputs, List<String> outputIDs, Set<NodeIDSuffix> refNodeIds) {

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
                if (inputConf.containsKey("id")) {
                    inputIDs.add(inputConf.getString("id"));
                }
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
                if (outputConf.containsKey("id")) {
                    outputIDs.add(outputConf.getString("id"));
                }
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

}
