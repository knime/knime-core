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
import static org.assertj.core.api.Assertions.tuple;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.CoreToDefUtil;

/**
 * Nodes might throw exceptions when retrieving their metadata, e.g., when they use a custom node description
 * implementation that is buggy. The node spec should be robust against that because nodes will not show up in the
 * modern UI node repository otherwise.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class NodeSpecBuggyTest {

    private static final String BUGGY_NODE_FACTORY_CLASS_NAME = BuggyNodeDescriptionNodeFactory.class.getName();

    /**
     * @return
     */
    private static NodeFactoryExtension getBuggyTestExtension() {
        // given a node factory extension that has a buggy node description
        final var optExt = NodeFactoryProvider.getInstance().getAllExtensions().values().stream() //
            .flatMap(Set::stream) //
            .flatMap(nodeOrNodeSet -> ClassUtils.castStream(NodeFactoryExtension.class, nodeOrNodeSet)) //
            .filter(nfe -> BUGGY_NODE_FACTORY_CLASS_NAME.equals(nfe.getFactoryClassName())) //
            .findAny();
        assertThat(optExt).as("Buggy test node factory extension could node be found.").isPresent();
        final var ext = optExt.get();
        return ext;
    }

    /**
     * Test that the buggy node is not too buggy to be created.
     *
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
     * Test that node specs for buggy node descriptions can still be created via factory.
     */
    @Test
    void testOfBuggyNodeFactory()
        throws InstantiationException, IllegalAccessException, InvalidNodeFactoryExtensionException {
        // given a node factory that has a buggy node description
        final var optFactory = NodeFactoryProvider.getInstance().getNodeFactory(BUGGY_NODE_FACTORY_CLASS_NAME);
        final var factory = optFactory.get();

        // when creating a node spec from that factory
        final var optNodeSpec = NodeSpec.of(factory, "", Map.of(), "", false);

        // then exceptions should be caught and the node spec should be created
        assertThat(optNodeSpec).as("Node spec could not be created.").isPresent();
    }

    /**
     * Test that node specs for buggy node descriptions can still be created via extension.
     */
    @Test
    void testOfBuggyExtension() {
        final var ext = getBuggyTestExtension();

        // when creating a node spec from that factory
        var nodeSpecList = NodeSpec.of(Map.of(), ext);

        // then exceptions should be caught and the node spec should be created
        // exactly one node spec should be created
        assertThat(nodeSpecList).hasSize(1);
        final var nodeSpec = nodeSpecList.get(0);
        assertThat(nodeSpec).isNotNull();
    }

    /**
     * Test that in/out port types are correctly declared, even if retrieving names and descriptions throw exceptions.
     */
    @Test
    void testPortDescriptionsOfBuggyExtension() {
        final var tableType = CoreToDefUtil.toPortTypeDef(BufferedDataTable.TYPE);
        final var flowVarType = CoreToDefUtil.toPortTypeDef(FlowVariablePortObject.TYPE);

        // given a node factory that throws exceptions when retrieving it port names and descriptions
        final var ext = getBuggyTestExtension();

        // when creating a node spec
        final var nodeSpecList = NodeSpec.of(Map.of(), ext);

        // there should be a node spec
        assertThat(nodeSpecList).hasSize(1);
        final var nodeSpec = nodeSpecList.get(0);

        // and the node spec should contain the correct port types
        assertThat(nodeSpec.ports().inputPorts()) //
            .extracting(NodeSpec.Ports.Port::index, NodeSpec.Ports.Port::type)
            .containsExactly(tuple(0, tableType), tuple(1, flowVarType));
        assertThat(nodeSpec.ports().outputPorts()) //
            .extracting(NodeSpec.Ports.Port::index, NodeSpec.Ports.Port::type)
            .containsExactly(tuple(0, flowVarType), tuple(1, tableType));
    }

}
