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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.CoreToDefUtil;

/**
 * Nodes might throw exceptions when retrieving their metadata. The node spec should be robust against that because the
 * nodes will not show up in the modern UI node repository otherwise.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class NodeSpecTest {

    /**
     * @return
     */
    private static NodeFactoryExtension getBuggyTestExtension() {
        // given a node factory extension that has a buggy node description
        final var optExt = NodeFactoryProvider.getInstance().getAllExtensions().values().stream() //
            .flatMap(Set::stream) //
            .flatMap(nodeOrNodeSet -> ClassUtils.castStream(NodeFactoryExtension.class, nodeOrNodeSet)) //
            .filter(nfe -> "org.knime.core.node.extension.BuggyNodeFactory".equals(nfe.getFactoryClassName())) //
            .findAny();
        assertTrue(optExt.isPresent(), "Buggy test node factory extension could node be found.");
        final var ext = optExt.get();
        return ext;
    }

     private static NodeFactoryExtension getTestExtension() {
        // given a node factory extension that has a regular node description
        final var optExt = NodeFactoryProvider.getInstance().getAllExtensions().values().stream() //
            .flatMap(Set::stream) //
            .flatMap(nodeOrNodeSet -> ClassUtils.castStream(NodeFactoryExtension.class, nodeOrNodeSet)) //
            .filter(nfe -> "org.knime.core.node.extension.TestNodeFactory".equals(nfe.getFactoryClassName())) //
            .findAny();
        assertTrue(optExt.isPresent(), "Test node factory extension could node be found.");
        final var ext = optExt.get();
        return ext;
    }

    /**
     * Test that the buggy node is not too buggy to be created.
     * @throws InvalidNodeFactoryExtensionException
     */
    @Test
    void testCreateBuggyTestNode() throws InvalidNodeFactoryExtensionException {
        // given a node factory that has a buggy node description
        final var ext = getBuggyTestExtension();

        // when creating a node
        final var node = new Node((NodeFactory<NodeModel>)ext.getFactory());

        // then no exceptions should be thrown
    }

    /**
     * Test that node specs for buggy node descriptions contain empty strings instead of failing to be created.
     */
    @Test
    void testOfBuggyNodeFactory()
        throws InstantiationException, IllegalAccessException, InvalidNodeFactoryExtensionException {
        // given a node factory that has a buggy node description
        final var optFactory =
            NodeFactoryProvider.getInstance().getNodeFactory("org.knime.core.node.extension.BuggyNodeFactory");
        assertTrue(optFactory.isPresent(), "Test node factory could node be found.");
        final var factory = optFactory.get();

        // when creating a node spec from that factory
        var optNodeSpec = NodeSpec.of(factory, "", Map.of(), "", false);

        // then exceptions should be caught and the node spec should be created
        assertTrue(optNodeSpec.isPresent());
    }

    /**
     * Test that node specs for buggy node descriptions contain empty strings instead of failing to be created.
     */
    @Test
    void testOfBuggyExtension() {
        final var ext = getBuggyTestExtension();

        // when creating a node spec from that factory
        var nodeSpecList = NodeSpec.of(Map.of(), ext);

        // then exceptions should be caught and the node spec should be created
        // exactly one node spec should be created
        assertEquals(1, nodeSpecList.size());
    }

    /**
     * Test port descriptions generated by node spec.
     */
    @Test
    void testPortDescriptions() {
        final var ext = getTestExtension();

        // when creating a node spec from the test factory
        var nodeSpecList = NodeSpec.of(Map.of(), ext);

        // there should be one node spec
        assertEquals(1, nodeSpecList.size());
        final var nodeSpec = nodeSpecList.get(0);

        final var tableType = CoreToDefUtil.toPortTypeDef(BufferedDataTable.TYPE);
        final var inputPorts = List.of (
            new NodeSpec.Ports.Port(1, tableType, "inport1", "inport1 description"),
            new NodeSpec.Ports.Port(2, tableType, "inport2", "inport2 description")
        );
        final var outputPorts = List.of (
            new NodeSpec.Ports.Port(1, tableType, "outport1", "outport1 description"),
            new NodeSpec.Ports.Port(2, tableType, "outport2", "outport2 description")
        );
        // the node spec should have a port description for each port
        assertEquals(inputPorts, nodeSpec.ports().inputPorts());
        assertEquals(outputPorts, nodeSpec.ports().outputPorts());
    }

}
