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
 *   29 Nov 2023 (carlwitt): created
 */
package org.knime.core.node.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.CoreToDefUtil;

/**
 * Test that node properties are correctly computed by the NodeSpec.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class NodeSpecTest {

    private static final String TEST_NODE_FACTORY_CLASS_NAME = TestNodeFactory.class.getName();

    private static NodeFactoryExtension getTestExtension() {
        // given a node factory extension that has a regular node description
        final var optExt = NodeFactoryProvider.getInstance().getAllExtensions().values().stream() //
            .flatMap(Set::stream) //
            .flatMap(nodeOrNodeSet -> ClassUtils.castStream(NodeFactoryExtension.class, nodeOrNodeSet)) //
            .filter(nfe -> TEST_NODE_FACTORY_CLASS_NAME.equals(nfe.getFactoryClassName())) //
            .findAny();
        assertThat(optExt).as("Test node factory extension could node be found.").isPresent();
        final var ext = optExt.get();
        return ext;
    }

    private static NodeSpec getTestNodeSpec() {
        final var ext = getTestExtension();

        final Map<String, CategoryExtension> categoryExtensions = Map.of( //
            "/root", CategoryExtension.builder("One", "root").build(), //
            "/root/level", CategoryExtension.builder("Two", "level").build(), //
            "/root/level/leaf", CategoryExtension.builder("Three", "leaf").build());
        return NodeSpec.of(categoryExtensions, ext).get(0);
    }

    /**
     * Individual node spec properties: factory, node type, ports, metadata, etc.
     */

    @Test
    void testNodeFactory() {
        // given the test node spec
        final var nodeSpec = getTestNodeSpec();

        // when retrieving the node factory information
        final var factory = nodeSpec.factory();

        assertThat(factory.id()).isEqualTo("org.knime.core.node.extension.TestNodeFactory");
        assertThat(factory.className()).isEqualTo(TEST_NODE_FACTORY_CLASS_NAME);
        assertThat(factory.factorySettings()).isNull(); // not a configurable
    }

    /**
     * Test port descriptions generated by node spec.
     */
    @Test
    void testPortDescriptions() {
        final var nodeSpec = getTestNodeSpec();

        final var tableType = CoreToDefUtil.toPortTypeDef(BufferedDataTable.TYPE);
        final var inputPorts = List.of(
            new NodeSpec.Ports.Port(0, tableType, "inport1", "inport1 description"),
            new NodeSpec.Ports.Port(1, tableType, "inport2", "inport2 description"));
        final var outputPorts = List.of(
            new NodeSpec.Ports.Port(0, tableType, "outport1", "outport1 description"),
            new NodeSpec.Ports.Port(1, tableType, "outport2", "outport2 description"));
        // the node spec should have a port description for each port
        assertEquals(inputPorts, nodeSpec.ports().inputPorts());
        assertEquals(outputPorts, nodeSpec.ports().outputPorts());
    }

    /**
     * Test metadata generated by node spec.
     */
    @Test
    void testMetadata() {
        // given the test node spec
        final var nodeSpec = getTestNodeSpec();

        // when retrieving the metadata
        final var metadata = nodeSpec.metadata();

        // vendor
        assertThat(metadata.vendor().bundle().getName()).isEqualTo("KNIME Core API");
        assertThat(metadata.vendor().bundle().getSymbolicName()).isEqualTo("org.knime.core");
        assertThat(metadata.vendor().bundle().getVendor()).isEqualTo("KNIME AG, Zurich, Switzerland");

        // node name is "node name" but since the deprecated flag is set, the suffix is added
        assertThat(metadata.nodeName()).isEqualTo("node name (deprecated)");

        // category path
        assertThat(metadata.categoryPath()).isEqualTo("/root/level/leaf");

        // after ID
        assertThat(metadata.afterID()).isEqualTo("afterId");

        // keywords
        assertThat(metadata.keywords()).containsExactly("keyword1", "keyword2", "keyword3");

        //tags
        assertThat(metadata.tags()).containsExactlyInAnyOrder("One", "Two", "Three");
    }

    @Test
    void testMiscellaneous() throws MalformedURLException {
        // given the test node spec
        final var nodeSpec = getTestNodeSpec();

        // when retrieving the non-nested factory information
        assertThat(nodeSpec.type()).isEqualTo(NodeType.Configuration);
        assertThat(nodeSpec.icon().toString()).endsWith("org/knime/core/node/extension/help.png");
        assertThat(nodeSpec.deprecated()).isTrue();
        assertThat(nodeSpec.hidden()).isTrue();
    }

}
