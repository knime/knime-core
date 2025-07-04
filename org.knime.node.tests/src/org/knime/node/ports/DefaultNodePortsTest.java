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
 *   Jul 2, 2025 (Paul Bärnreuther): created
 */
package org.knime.node.ports;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.node.RequirePorts.DynamicPortsAdder;
import org.knime.node.RequirePorts.PortsAdder;
import org.knime.node.testing.TestWithWorkflowManager;

class DefaultNodePortsTest extends TestWithWorkflowManager {

    @Test
    void testInputTable() {
        Consumer<PortsAdder> ports = p -> p.addInputTable("An input table", "");
        final var nc = addNodeWithPorts(ports);

        assertThat(nc.getInPort(1).getPortType()).isEqualTo(BufferedDataTable.TYPE);
    }

    @Test
    void testOutputTable() {
        Consumer<PortsAdder> ports = p -> p.addOutputTable("An output table", "");
        final var nc = addNodeWithPorts(ports);

        assertThat(nc.getOutPort(1).getPortType()).isEqualTo(BufferedDataTable.TYPE);
    }

    @Test
    void testInputPort() {
        Consumer<PortsAdder> ports = p -> p.addInputPort("Input", "", FlowVariablePortObject.TYPE);
        final var nc = addNodeWithPorts(ports);

        assertThat(nc.getInPort(1).getPortType()).isEqualTo(FlowVariablePortObject.TYPE);
    }

    @Test
    void testOutputPort() {
        Consumer<PortsAdder> ports = p -> p.addOutputPort("Input", "", FlowVariablePortObject.TYPE);
        final var nc = addNodeWithPorts(ports);

        assertThat(nc.getOutPort(1).getPortType()).isEqualTo(FlowVariablePortObject.TYPE);
    }

    @Test
    void testOptionalInputPortGroup() {
        Consumer<DynamicPortsAdder> ports = d -> d.addInputPortGroup("optionalInput", g -> g.name("Optional Input")
            .description("An optional input port").optional().supportedTypes(BufferedDataTable.TYPE));
        final var nc = addNodeWithDynamicPorts(ports);

        // Optional port group: initially not present
        assertThat(nc.getNrInPorts()).isEqualTo(1);
    }

    @Test
    void testExtendableInputPortGroup() {
        Consumer<DynamicPortsAdder> ports = d -> d.addInputPortGroup("extendableInput",
            g -> g.name("Extendable Input").description("An extendable input port group").extendable()
                .supportedTypes(BufferedDataTable.TYPE).defaultNonFixedTypes(BufferedDataTable.TYPE));
        final var nc = addNodeWithDynamicPorts(ports);

        // Should have one default (non-fixed) port of the specified type
        assertThat(nc.getNrInPorts()).isEqualTo(2);
        assertThat(nc.getInPort(1).getPortType()).isEqualTo(BufferedDataTable.TYPE);
    }

    @Test
    void testBoundExtendableInputPortGroup() {
        Consumer<DynamicPortsAdder> ports = d -> {
            d.addInputPortGroup("base", g -> g.name("Base").description("Base group").extendable()
                .supportedTypes(BufferedDataTable.TYPE).defaultNonFixedTypes(BufferedDataTable.TYPE));
            d.addInputPortGroup("bound",
                g -> g.name("Bound").description("Bound group").boundExtendable("base").defaultNumPorts(1));
        };
        final var nc = addNodeWithDynamicPorts(ports);

        // Should have one port in each group
        assertThat(nc.getNrInPorts()).isEqualTo(3);
        assertThat(nc.getInPort(1).getPortType()).isEqualTo(BufferedDataTable.TYPE);
        assertThat(nc.getInPort(2).getPortType()).isEqualTo(BufferedDataTable.TYPE);
    }

    @Test
    void testExchangeableInputPortGroup() {
        Consumer<DynamicPortsAdder> ports = d -> d.addInputPortGroup("exchangeableInput",
            g -> g.name("Exchangeable Input").description("An exchangeable input port")
                .exchangable(BufferedDataTable.TYPE)
                .supportedTypes(BufferedDataTable.TYPE, FlowVariablePortObject.TYPE));
        final var nc = addNodeWithDynamicPorts(ports);

        // Should have exactly one port of the default type
        assertThat(nc.getNrInPorts()).isEqualTo(2);
        assertThat(nc.getInPort(1).getPortType()).isEqualTo(BufferedDataTable.TYPE);
    }

    @Test
    void testInputAndOutputPortGroup() {
        Consumer<DynamicPortsAdder> ports = d -> d.addInputAndOutputPortGroup("paired",
            g -> g.inputName("Paired Input").inputDescription("Input of pair").outputName("Paired Output")
                .outputDescription("Output of pair").fixed(BufferedDataTable.TYPE));
        final var nc = addNodeWithDynamicPorts(ports);

        // Should have one input and one output port of the specified type
        assertThat(nc.getNrInPorts()).isEqualTo(2);
        assertThat(nc.getNrOutPorts()).isEqualTo(2);
        assertThat(nc.getInPort(1).getPortType()).isEqualTo(BufferedDataTable.TYPE);
        assertThat(nc.getOutPort(1).getPortType()).isEqualTo(BufferedDataTable.TYPE);
    }

}
