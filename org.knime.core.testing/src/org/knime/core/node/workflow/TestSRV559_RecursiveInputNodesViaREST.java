/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.dialog.ExternalNodeData;

/**
 *
 * @author wiswedel, University of Konstanz
 */
@RunWith(Parameterized.class)
public class TestSRV559_RecursiveInputNodesViaREST extends WorkflowTestCase {

    private NodeID m_stringInputTopLevel_1;
    private NodeID m_stringInputMetanodeLevel_21_10;
    private NodeID m_credInputTopLevel_2;
    private NodeID m_credInputMetanodeLevel_21_11;
    private NodeID m_jsonOutputTopLevel_7;
    private NodeID m_jsonOutputMetanodeLevel_21_16;

    private NodeID m_javaEditValidateTopLevelFoo_4;
    private NodeID m_javaEditValidateTopLevelBar_3;
    private NodeID m_javaEditValidateMetanodeLevelFoo_13;
    private NodeID m_javaEditValidateMetanodeLevelBar_12;
    private NodeID m_credValidateTopLevelFoo_5;
    private NodeID m_credValidateTopLevelBar_6;
    private NodeID m_credValidateMetanodeLevelFoo_14;
    private NodeID m_credValidateMetanodeLevelBar_15;

    @Parameters(name="with-suffix={0}")
    public static Collection<Boolean> getParameters() {
        return Arrays.asList(new Boolean[] {true, false});
    }

    @Parameter(value = 0)
    public Boolean m_useSuffix;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_stringInputTopLevel_1 = baseID.createChild(1);
        m_credInputTopLevel_2 = baseID.createChild(2);
        m_jsonOutputTopLevel_7 = baseID.createChild(7);

        NodeID wfmInMetaNodeID = baseID.createChild(21).createChild(0);
        m_stringInputMetanodeLevel_21_10 = wfmInMetaNodeID.createChild(10);
        m_credInputMetanodeLevel_21_11 = wfmInMetaNodeID.createChild(11);
        m_jsonOutputMetanodeLevel_21_16 = wfmInMetaNodeID.createChild(16);

        m_javaEditValidateTopLevelFoo_4 = baseID.createChild(4);
        m_javaEditValidateTopLevelBar_3 = baseID.createChild(3);
        m_javaEditValidateMetanodeLevelFoo_13 = baseID.createChild(13);
        m_javaEditValidateMetanodeLevelBar_12 = baseID.createChild(12);
        m_credValidateTopLevelFoo_5 = baseID.createChild(5);
        m_credValidateTopLevelBar_6 = baseID.createChild(6);
        m_credValidateMetanodeLevelFoo_14 = baseID.createChild(14);
        m_credValidateMetanodeLevelBar_15 = baseID.createChild(15);
    }

    @Test
    public void testPlainExecAll() throws Exception {
        assumeRunOnlyOnce();
        checkStateOfMany(CONFIGURED, m_stringInputTopLevel_1, m_stringInputMetanodeLevel_21_10, m_credInputTopLevel_2,
            m_credInputMetanodeLevel_21_11, m_jsonOutputTopLevel_7, m_jsonOutputMetanodeLevel_21_16);
        executeAllAndWait();
        checkStateOfMany(EXECUTED, m_stringInputTopLevel_1, m_stringInputMetanodeLevel_21_10, m_credInputTopLevel_2,
            m_credInputMetanodeLevel_21_11, m_jsonOutputTopLevel_7, m_jsonOutputMetanodeLevel_21_16);
        checkState(m_javaEditValidateTopLevelFoo_4, EXECUTED);
    }

    @Test
    public void testListExternalDataNodes() throws Exception {
        assumeRunOnlyOnce();
        WorkflowManager manager = getManager();
        executeAllAndWait();
        Map<String, ExternalNodeData> inputNodes = manager.getInputNodes();
        assertThat("Wrong number inputs", inputNodes.size(), is(4));
        assertThat("Input parameter list wrong", inputNodes.keySet(), containsInAnyOrder(
            "top-level-string-input-1",
            "top-level-credentials-input-2",
            "metanode-level-string-input-21:10",
            "metanode-level-credentials-input-21:11"));

        ExternalNodeData topLevelStringInputData = inputNodes.get("top-level-string-input-1");
        assertThat(topLevelStringInputData.getJSONValue().toString(), is("\"foo\""));

        ExternalNodeData topLevelCredInputData = inputNodes.get("top-level-credentials-input-2");
        assertThat(topLevelCredInputData.getJSONValue(), is(toJson("{\"username\":\"foo-login\", \"password\":null}")));

        ExternalNodeData metanodeLevelStringInputData = inputNodes.get("metanode-level-string-input-21:10");
        assertThat(metanodeLevelStringInputData.getJSONValue().toString(), is("\"foo\""));

        ExternalNodeData metanodeLevelCredInputData = inputNodes.get("metanode-level-credentials-input-21:11");
        assertThat(metanodeLevelCredInputData.getJSONValue(),
            is(toJson("{\"username\":\"foo-login\",\"password\":null}")));

        Map<String, ExternalNodeData> outputs = manager.getExternalOutputs();
        assertThat(outputs.size(), is(2));
        assertThat(outputs.keySet(), containsInAnyOrder("top-level-json-output-7", "metanode-level-json-output-21:16"));

        ExternalNodeData outputDataTopLevel = outputs.get("top-level-json-output-7");
        assertThat(outputDataTopLevel.getID(), is("top-level-json-output"));

        assertThat(outputDataTopLevel.getJSONValue(),
            is(toJson("{\"top-level-string-input\":\"foo\", \"metanode-level-string-input\":\"foo\"}")));

        ExternalNodeData outputDataMetanodeLevel = outputs.get("metanode-level-json-output-21:16");
        assertThat(outputDataMetanodeLevel.getID(), is("metanode-level-json-output"));

        assertThat(outputDataMetanodeLevel.getJSONValue(), is(toJson("{\"metanode-level-string-input\":\"foo\"}")));
    }

    @Test
    public void testSetInputNodesViaJSON() throws Exception {
        WorkflowManager manager = getManager();
        executeAllAndWait(); // should work either way - executed or not

        Map<String, ExternalNodeData> inputMap = new HashMap<>();
        final String toplevelStringInputKey;
        final String toplevelCredsInputKey;
        final String metanodelevelStringInputKey;
        final String metanodelevelCredsInputKey;
        if (m_useSuffix) {
            toplevelStringInputKey = "top-level-string-input-1";
            toplevelCredsInputKey = "top-level-credentials-input-2";
            metanodelevelStringInputKey = "metanode-level-string-input-21:10";
            metanodelevelCredsInputKey = "metanode-level-credentials-input-21:11";
        } else {
            toplevelStringInputKey = "top-level-string-input";
            toplevelCredsInputKey = "top-level-credentials-input";
            metanodelevelStringInputKey = "metanode-level-string-input";
            metanodelevelCredsInputKey = "metanode-level-credentials-input";
        }

        inputMap.put(toplevelStringInputKey, ExternalNodeData.builder(toplevelStringInputKey)
            .jsonValue(toJson("{\"string\":\"bar\"}")).build());
        inputMap.put(toplevelCredsInputKey, ExternalNodeData.builder(toplevelCredsInputKey).
            jsonValue(toJson("{\"username\":\"bar-login\", \"password\":\"bar-password\"}")).build());
        inputMap.put(metanodelevelStringInputKey, ExternalNodeData.builder(toplevelStringInputKey)
            .jsonValue(toJson("{\"string\":\"bar\"}")).build());
        inputMap.put(metanodelevelCredsInputKey, ExternalNodeData.builder(toplevelCredsInputKey).
            jsonValue(toJson("{\"username\":\"bar-login\", \"password\":\"bar-password\"}")).build());

        manager.setInputNodes(inputMap);

        checkStateOfMany(CONFIGURED, m_credInputTopLevel_2, m_stringInputTopLevel_1);
        executeAllAndWait();

        checkStateOfMany(EXECUTED, m_credInputTopLevel_2, m_stringInputTopLevel_1,
            m_javaEditValidateTopLevelBar_3, m_credValidateTopLevelBar_6);

        checkStateOfMany(IDLE, m_javaEditValidateTopLevelFoo_4, m_credValidateTopLevelFoo_5);

        Map<String, ExternalNodeData> outputs = manager.getExternalOutputs();
        ExternalNodeData outputDataTopLevel = outputs.get("top-level-json-output-7");
        assertThat(outputDataTopLevel.getJSONValue(), is(toJson(
            "{\"top-level-string-input\":\"bar\","
            + "\"metanode-level-string-input\":\"bar\"}")));

        ExternalNodeData outputDataMetanodeLevel = outputs.get("metanode-level-json-output-21:16");
        assertThat(outputDataMetanodeLevel.getJSONValue(), is(toJson("{\"metanode-level-string-input\":\"bar\"}")));

    }

    @Test(expected = InvalidSettingsException.class)
    public void testSetInvalidViaJSON() throws Exception {
        WorkflowManager manager = getManager();
        executeAllAndWait(); // should work either way - executed or not

        Map<String, ExternalNodeData> inputMap = new HashMap<>();
        final String stringInputKey = m_useSuffix ? "top-level-string-input-1" : "top-level-string-input";
        final String credsInputKey = m_useSuffix ? "top-level-credentials-input-2" : "top-level-credentials-input";

        inputMap.put(stringInputKey, ExternalNodeData.builder(stringInputKey)
            .jsonValue(toJson("{\"string\":\"valid\"}")).build());
        inputMap.put(credsInputKey, ExternalNodeData.builder(credsInputKey).
            jsonValue(toJson("{\"username\":\"bar-login\", \"password\":\"bar-password\"}")).build());

        manager.setInputNodes(inputMap);
    }

    @Test
    public void testSetInputNodesViaString() throws Exception {
        assumeRunOnlyOnce();
        WorkflowManager manager = getManager();
        NodeContainer nc = manager.findNodeContainer(m_stringInputMetanodeLevel_21_10);
        WorkflowCopyContent c = WorkflowCopyContent.builder().setNodeID(m_stringInputMetanodeLevel_21_10, 1234, null).build();
        final NodeID copiedNodeID = manager.getID().createChild(1234);
        manager.paste(nc.getParent().copy(c));
        executeAllAndWait(); // should work either way - executed or not

        Map<String, ExternalNodeData> inputMap = new HashMap<>();

        inputMap.put("top-level-string-input-1",
            ExternalNodeData.builder("top-level-string-input").stringValue("bar").build());
        inputMap.put("metanode-level-string-input-1234",
            ExternalNodeData.builder("metanode-level-string-input").stringValue("bar").build());

        manager.setInputNodes(inputMap);

        checkStateOfMany(CONFIGURED, m_stringInputTopLevel_1, copiedNodeID);
        executeAllAndWait();
        checkStateOfMany(EXECUTED, m_stringInputTopLevel_1, copiedNodeID);

        inputMap = new HashMap<>();
        inputMap.put("metanode-level-string-input",
            ExternalNodeData.builder("metanode-level-string-input").stringValue("bar").build());

        try {
            manager.setInputNodes(inputMap);
            fail("Should have failed because parameter node is not uniquely identified");
        } catch (InvalidSettingsException ise) {
            Assert.assertThat(ise.getMessage(), containsString("doesn't match"));
        }
    }

    @Test
    public void testAmbigiousParameterName() throws Exception {
        WorkflowManager manager = getManager();
        executeAllAndWait(); // should work either way - executed or not

        Map<String, ExternalNodeData> inputMap = new HashMap<>();
        final String stringInputKey = m_useSuffix ? "top-level-string-input-1" : "top-level-string-input";
        final String credsInputKey = m_useSuffix ? "top-level-credentials-input-2" : "top-level-credentials-input";

        inputMap.put(stringInputKey, ExternalNodeData.builder(stringInputKey).stringValue("bar").build());
        inputMap.put(credsInputKey, ExternalNodeData.builder(credsInputKey).stringValue("bar-login:bar-password").build());

        manager.setInputNodes(inputMap);

        checkStateOfMany(CONFIGURED, m_credInputTopLevel_2, m_stringInputTopLevel_1);
        executeAllAndWait();

        checkStateOfMany(EXECUTED, m_credInputTopLevel_2, m_stringInputTopLevel_1,
            m_javaEditValidateTopLevelBar_3, m_credValidateTopLevelBar_6);

        checkStateOfMany(IDLE, m_javaEditValidateTopLevelFoo_4, m_credValidateTopLevelFoo_5);

        Map<String, ExternalNodeData> outputs = manager.getExternalOutputs();
        ExternalNodeData outputData = outputs.get("top-level-json-output-7");
        assertThat(outputData.getJSONValue(), is(toJson(
            "{\"top-level-string-input\":\"bar\","
                    + "\"metanode-level-string-input\":\"foo\"}")));
    }

    @Test(expected = InvalidSettingsException.class)
    public void testSetInvalidValueViaString() throws Exception {
        WorkflowManager manager = getManager();
        executeAllAndWait(); // should work either way - executed or not

        Map<String, ExternalNodeData> inputMap = new HashMap<>();
        final String stringInputKey = m_useSuffix ? "top-level-string-input-1" : "top-level-string-input";
        final String credsInputKey = m_useSuffix ? "top-level-credentials-input-2" : "top-level-credentials-input";

        inputMap.put(stringInputKey, ExternalNodeData.builder(stringInputKey).stringValue("invalid").build());
        inputMap.put(credsInputKey, ExternalNodeData.builder(credsInputKey).stringValue("bar-login:bar-password").build());

        manager.setInputNodes(inputMap);
    }

    @Test(expected = InvalidSettingsException.class)
    public void testSetInvalidKeyViaString() throws Exception {
        assumeRunOnlyOnce();
        WorkflowManager manager = getManager();
        executeAllAndWait(); // should work either way - executed or not

        Map<String, ExternalNodeData> inputMap = new HashMap<>();
        inputMap.put("invalid-level-string-input-1",
            ExternalNodeData.builder("invalid-level-string-input").stringValue("foo").build());

        manager.setInputNodes(inputMap);
    }

    @Test(expected = InvalidSettingsException.class)
    public void testSetInvalidSuffixViaString() throws Exception {
        assumeRunOnlyOnce();
        WorkflowManager manager = getManager();
        executeAllAndWait(); // should work either way - executed or not

        Map<String, ExternalNodeData> inputMap = new HashMap<>();
        inputMap.put("top-level-string-input-3",
            ExternalNodeData.builder("invalid-level-string-input").stringValue("foo").build());

        manager.setInputNodes(inputMap);
    }

    private static JsonValue toJson(final String s) {
        try (JsonReader r = Json.createReader(new StringReader(s))) {
            return r.read();
        }
    }

    /** Uses {@link Assume} construct to run the test only once -- depending on {@link #m_useSuffix}. */
    private void assumeRunOnlyOnce() {
        // this method doesn't use the suffix query so run it only once.
        Assume.assumeTrue("Non-paramerized method - method already run or will run", m_useSuffix);
    }

}
