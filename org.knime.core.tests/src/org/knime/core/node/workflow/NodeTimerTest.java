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
 *   Oct 13, 2023 (hornm): created
 */
package org.knime.core.node.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeTimer.GlobalNodeStats;
import org.knime.core.node.workflow.NodeTimer.GlobalNodeStats.NodeCreationType;
import org.knime.core.node.workflow.NodeTimer.GlobalNodeStats.WorkflowType;
import org.knime.testing.util.WorkflowManagerUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

/**
 * Tests {@link NodeTimer}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeTimerTest {

    private static final String EXPECTED_NODESTATS_NODE_1 = """
            {
                "id": "org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory",
                "nodename": "PortObject Reference Reader",
                "nrexecs": 0,
                "nrfails": 0,
                "exectime": 0,
                "nrcreated": 1,
                "successor": "NodeContainer",
                "successornodename": "component",
                "nrsettingsChanged": 1
            }
                     """;

    private static final String EXPECTED_NODESTATS_NODE_2 = """
            {
                "id": "org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory",
                "nodename": "PortObject Reference Reader",
                "nrexecs": 0,
                "nrfails": 0,
                "exectime": 0,
                "nrcreated": 2,
                "successor": "org.knime.core.node.workflow.SubNodeContainer",
                "successornodename": "component",
                "nrsettingsChanged": 1
            }
                     """;

    private static final String EXPECTED_NODESTATS_METANODES = """
            {
                "nodename": "component",
                "nrexecs": 0,
                "nrfails": 0,
                "exectime": 0,
                "nrcreated": 1,
                "nrsettingsChanged": 0
            }
                """;

    private static final String EXPECTED_NODESTATS_WRAPPEDNODES = """
            {
                "nodename": "component",
                "nrexecs": 0,
                "nrfails": 0,
                "exectime": 0,
                "nrcreated": 1,
                "nrsettingsChanged": 0
            },
                    """;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WorkflowManager m_wfm;

    @BeforeEach
    void setup() throws IOException {
        NodeTimer.GLOBAL_TIMER.resetAllCounts();
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
        NodeTimer.GLOBAL_TIMER.incWorkflowCreate(WorkflowType.LOCAL);
    }

    /**
     * Tests the 'nodestats'-section in the {@link GlobalNodeStats#FILENAME} file.
     *
     * @throws IOException
     */
    @Test
    void testNodeStatsInNodeUsageFile() throws IOException {
        var nnc = createNode();
        NodeTimer.GLOBAL_TIMER.addNodeCreation(nnc);
        NodeTimer.GLOBAL_TIMER.incNodeSettingsChanged(nnc);
        NodeTimer.GLOBAL_TIMER.incNodeCreatedVia(NodeCreationType.WEB_UI);

        var wfm = createMetanode();
        NodeTimer.GLOBAL_TIMER.addNodeCreation(wfm);
        repeat(() -> NodeTimer.GLOBAL_TIMER.addConnectionCreation(nnc, wfm), 20);

        NodeTimer.GLOBAL_TIMER.writeToFile(false);
        var statsFile = new File(KNIMEConstants.getKNIMEHomeDir(), GlobalNodeStats.FILENAME);
        var jsonString = Files.readString(statsFile.toPath());
        assertThatJson(jsonString, "/nodestats/nodes/0").isEqualTo(EXPECTED_NODESTATS_NODE_1);
        assertThatJson(jsonString, "/nodestats/metaNodes").isEqualTo(EXPECTED_NODESTATS_METANODES);
        assertThatJson(jsonString, "/nodestats/wrappedNodes").isEqualTo("{}");
        assertThatJson(jsonString, "/nodestats/createdVia").isEqualTo("""
                {"WEB_UI" : 1}
                """);
        // the empty workflow created in `@BeforeEach`
        assertThatJson(jsonString, "/workflowsCreated").isEqualTo("1");

        var schema = readNodeTimerSchema();
        var errors = schema.validate(MAPPER.readTree(jsonString));
        assertThat(errors).as("Node timer schema validation errors").isEmpty();

        NodeTimer.GLOBAL_TIMER.addNodeCreation(nnc);
        NodeTimer.GLOBAL_TIMER.incNodeCreatedVia(NodeCreationType.WEB_UI_HUB);
        var snc = createComponent();
        NodeTimer.GLOBAL_TIMER.addNodeCreation(snc);
        repeat(() -> NodeTimer.GLOBAL_TIMER.addConnectionCreation(nnc, snc), 20);

        NodeTimer.GLOBAL_TIMER.writeToFile(false);
        statsFile = new File(KNIMEConstants.getKNIMEHomeDir(), GlobalNodeStats.FILENAME);
        jsonString = Files.readString(statsFile.toPath());
        assertThatJson(jsonString, "/nodestats/nodes/0").isEqualTo(EXPECTED_NODESTATS_NODE_2);
        assertThatJson(jsonString, "/nodestats/wrappedNodes").isEqualTo(EXPECTED_NODESTATS_WRAPPEDNODES);
    }

    private static JsonSchema readNodeTimerSchema() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        InputStream is = NodeTimerTest.class.getResourceAsStream("NodeTimerSchema.json");
        return factory.getSchema(is);
    }

    private NativeNodeContainer createNode() {
        return WorkflowManagerUtil.createAndAddNode(m_wfm, new PortObjectInNodeFactory());
    }

    private WorkflowManager createMetanode() {
        return m_wfm.createAndAddSubWorkflow(new PortType[0], new PortType[0], "component");
    }

    private SubNodeContainer createComponent() {
        var metanode = createMetanode();
        var componentId = m_wfm.convertMetaNodeToSubNode(metanode.getID()).getConvertedNodeID();
        return m_wfm.getNodeContainer(componentId, SubNodeContainer.class, false);
    }

    private static JsonIsEqualTo assertThatJson(final String expected, final String path) {
        return json -> assertThat(MAPPER.readTree(expected).at(path)).isEqualTo(MAPPER.readTree(json));
    }

    private interface JsonIsEqualTo {
        void isEqualTo(String json) throws JsonMappingException, JsonProcessingException;
    }

    private static void repeat(final Runnable run, final int times) {
        for (int i = 0; i < times; i++) {
            run.run();
        }
    }

    @AfterEach
    void shutdown() {
        NodeTimer.GLOBAL_TIMER.resetAllCounts();
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
    }

}
