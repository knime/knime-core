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
 *   Oct 25, 2023 (hornm): created
 */
package org.knime.core.node;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Test;
import org.knime.core.node.extension.NodeFactoryProvider;
import org.knime.core.node.missing.MissingNodeFactory;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.FileUtil;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests related to node-factory-id.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeFactoryIdTest {

    /**
     * Tests {@link NodeFactory#getFactoryId()} and whether {@link DynamicNodeFactory#getFactoryIdUniquifier()} is saved
     * with the node settings.
     *
     * @throws Exception
     */
    @Test
    public void testNodeFactoryIdAndFactoryIdUniquifier() throws Exception {
        testNodeFactoryIdAndFactoryIdUniquifier(
            "org.knime.core.node.NodeFactoryIdTestNodeSetFactory$NodeFactoryIdTestDynamicNodeFactory1",
            "factory-id-uniquifier-1", FactoryType.DYNAMIC_FACTORY);

        testNodeFactoryIdAndFactoryIdUniquifier(
            "org.knime.core.node.NodeFactoryIdTestNodeSetFactory$NodeFactoryIdTestDynamicNodeFactory2", null,
            FactoryType.DYNAMIC_FACTORY_PRIOR_AP_5_2);

        testNodeFactoryIdAndFactoryIdUniquifier(
            "org.knime.core.node.NodeFactoryIdTestNodeSetFactory$NodeFactoryIdTestNodeFactory", null,
            FactoryType.NORMAL_FACTORY);

    }

    /**
     * Tests {@link MissingNodeFactory#getFactoryId()}.
     */
    @Test
    public void testMissingNodeFactoryId() {
        var factory = new MissingNodeFactory(new NodeAndBundleInformationPersistor("TEST"), null, new PortType[0],
            new PortType[0]);
        factory.init();
        assertThat(factory.getFactoryId()).isEqualTo("TEST#MISSING TEST");
    }

    private static enum FactoryType {
            DYNAMIC_FACTORY, DYNAMIC_FACTORY_PRIOR_AP_5_2, NORMAL_FACTORY;
    }

    private static void testNodeFactoryIdAndFactoryIdUniquifier(final String factoryClassName,
            final String expectedFactoryIdUniquifier, final FactoryType factoryType) throws Exception {
        // create workflow and check
        var newWfm = WorkflowManagerUtil.createEmptyWorkflow();
        var factory = NodeFactoryProvider.getInstance().getNodeFactory(factoryClassName).orElseThrow();
        var nnc = WorkflowManagerUtil.createAndAddNode(newWfm, factory);
        var expectedFactoryId = switch (factoryType) {
            case DYNAMIC_FACTORY -> factoryClassName + "#" + expectedFactoryIdUniquifier;
            case DYNAMIC_FACTORY_PRIOR_AP_5_2 -> factoryClassName + "#" + factory.getNodeName();
            case NORMAL_FACTORY -> factoryClassName;
        };

        factory = nnc.getNode().getFactory();
        switch (factoryType) {
            case DYNAMIC_FACTORY -> {
                var idUniquifier = ((DynamicNodeFactory<?>)factory).getFactoryIdUniquifier();
                assertThat(idUniquifier).isEqualTo(expectedFactoryIdUniquifier);
            }
            case DYNAMIC_FACTORY_PRIOR_AP_5_2 -> assertThat(((DynamicNodeFactory<?>)factory).getFactoryIdUniquifier())
                .isNull();
            case NORMAL_FACTORY -> assertThat(factory).isNotInstanceOf(DynamicNodeFactory.class);
        }
        assertThat(factory.getFactoryId()).isEqualTo(expectedFactoryId);

        // save
        var wfDir = FileUtil.createTempDir("workflow");
        newWfm.save(wfDir, new ExecutionMonitor(), false);
        WorkflowManagerUtil.disposeWorkflow(newWfm);

        // check saved node settings
        var wfSettings =
            NodeSettings.loadFromXML(new FileInputStream(new File(wfDir, WorkflowPersistor.WORKFLOW_FILE)));
        var relNodeSettingsFilePath =
            wfSettings.getNodeSettings("nodes").getNodeSettings("node_1").getString("node_settings_file");
        var nodeSettings = NodeSettings.loadFromXML(new FileInputStream(new File(wfDir, relNodeSettingsFilePath)));
        var factoryIdUniquifierKey = "factory-id-uniquifier";
        switch (factoryType) {
            case DYNAMIC_FACTORY -> {
                var savedFactoryIdUniquifier = nodeSettings.getString(factoryIdUniquifierKey);
                assertThat(savedFactoryIdUniquifier).isEqualTo(expectedFactoryIdUniquifier);
            }
            case DYNAMIC_FACTORY_PRIOR_AP_5_2 -> {
                var savedFactoryIdUniquifier = nodeSettings.getString(factoryIdUniquifierKey);
                assertThat(savedFactoryIdUniquifier).isEqualTo(null);
            }
            case NORMAL_FACTORY -> {
                assertThat(nodeSettings.containsKey(factoryIdUniquifierKey)).isFalse();
            }
        }
    }

}
