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
 *   24 Oct 2023 (carlwitt): created
 */
package org.knime.core.node.extension;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.CoreToDefUtil;
import org.knime.shared.workflow.def.impl.VendorDefBuilder;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class DiskBasedNodeSpecCacheTest {

    private static final Map<String, NodeSpec> TEST_DATA;

    //    private static final class TestFactory extends WebUINodeFactory<NodeModel> {
    //
    //        private static final class Settings implements DefaultNodeSettings {
    //
    //        }
    //
    //        private static final WebUINodeConfiguration CONF =
    //            WebUINodeConfiguration.builder().name("Test node").icon("test.png").shortDescription("Short description")
    //                .fullDescription("Full description").modelSettingsClass(Settings.class)
    //                .addInputPort("Input table", BufferedDataTable.TYPE, "Input table port description").build();
    //
    //        protected TestFactory(final WebUINodeConfiguration configuration) {
    //            super(CONF);
    //        }
    //
    //        @Override
    //        public NodeModel createNodeModel() {
    //            return null;
    //        }
    //        //        @Override
    //        //        protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
    //        //            var pcb = new PortsConfigurationBuilder();
    //        //            pcb.addExtendableInputPortGroup("inputs", BufferedDataTable.TYPE);
    //        //            pcb.addFixedOutputPortGroup("output", DatabaseConnectionPortObject.TYPE);
    //        //            return Optional.of(pcb);
    //        //        }
    //
    //    }

    static {
        // factory
        var factorySettings = new NodeSettings("factorysettings");
        factorySettings.addString("stringKey", "stringValue");
        var factory = new NodeSpec.Factory("globally unique factory id", "factory class name", factorySettings);

        // ports
        var table = CoreToDefUtil.toPortTypeDef(BufferedDataTable.TYPE);
        var in = List.of(new NodeSpec.Ports.Port(1, table, "name 1", "description"));
        var out = List.of(new NodeSpec.Ports.Port(2, table, "name 2", "description"),
            new NodeSpec.Ports.Port(3, table, "name 3", "description"));
        //        var nodeCreationConfig = Optional.of(new TestFactory().createNodeCreationConfig());
        var supportedInputPorts = List.of(table);
        var ports = new NodeSpec.Ports(in, supportedInputPorts, out);

        // metadata
        var feature = new VendorDefBuilder().setName("feature").setSymbolicName("feature symbolic name")
            .setVersion("feature version").build();
        var bundle = new VendorDefBuilder().setName("bundle").setSymbolicName("bundle symbolic name")
            .setVersion("bundle version").build();
        var vendor = new NodeSpec.Metadata.Vendor(feature, bundle);
        var nodeName = "Database reader";
        var nodeType = NodeType.Source;
        var categoryPath = "/IO/Database";
        var afterID = "org.knime.base.node.io.database.DBReaderNodeFactory";
        var keywords = List.of("database", "reader");
        var tags = List.of("io", "database");
        //        var nodeDescription = WebUINodeFactory.createNodeDescription(TestFactory.CONF);
        var metadata = new NodeSpec.Metadata(vendor, nodeName, nodeType, categoryPath, afterID, keywords, tags);

        // functionality description
        URL icon = null;
        try {
            icon = new URL("file://icons/database.png");
        } catch (MalformedURLException e) {
        }

        // status
        var deprecated = true;
        var hidden = true;

        NodeSpec metadata1 = new NodeSpec(factory, NodeType.Manipulator, ports, metadata, icon, deprecated, hidden);

        TEST_DATA = Map.of("node1", metadata1, "node2", metadata1);
    }

    /**
     * Test read load write cycle.
     *
     * @throws Exception
     */
    @Test
    void testReadLoadWrite() throws Exception {
        // given test data
        var bundle = FrameworkUtil.getBundle(getClass());
        // when write and load
        DiskBasedNodeSpecCache.testWrite(TEST_DATA);

        var diskLoadResult = DiskBasedNodeSpecCache.testRead();
        // then load result is not null
        assertThat(diskLoadResult).isNotNull();
        // and loaded data is same as test data
        assertThat(diskLoadResult).containsExactlyEntriesOf(TEST_DATA);
    }

    private static void measureTime(final Runnable task, final String description) throws Exception {
        measureTime(() -> {
            task.run();
            return Void.TYPE;
        }, description);
    }

    private static <T> T measureTime(final Callable<T> task, final String description) throws Exception {
        long startTime = System.currentTimeMillis();
        var result = task.call();
        long endTime = System.currentTimeMillis();
        System.out.println("Completed %s in %s ms".formatted(description, endTime - startTime));
        return result;
    }

}
