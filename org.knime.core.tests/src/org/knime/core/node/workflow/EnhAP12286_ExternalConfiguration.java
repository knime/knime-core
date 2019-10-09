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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonValue;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.CoreConstants;
import org.knime.core.util.FileUtil;

/**
 * Tests that the top-level configurations (configuration-/dialog nodes and workflow variables) are correctly written
 * into the .artifacts-folder and that the configuration can correctly be set and retrieved to/from the workflow
 * manager.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class EnhAP12286_ExternalConfiguration extends WorkflowTestCase {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private File m_workflowDir;

    private NodeID m_stringConfiguration_1;

    private NodeID m_doubleConfiguration_2;

    /**
     * Creates and copies the workflow into a temporary directory.
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
        initWorkflowFromTemp();
    }

    private WorkflowLoadResult initWorkflowFromTemp() throws Exception {
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
        setManager(loadResult.getWorkflowManager());
        NodeID baseID = getManager().getID();
        m_stringConfiguration_1 = new NodeID(baseID, 1);
        m_doubleConfiguration_2 = new NodeID(baseID, 2);
        return loadResult;
    }

    /**
     * Checks the configuration files in the .artifacts folder.
     *
     * @throws Exception
     */
    @Test
    public void testSaveWorkflowConfigurationInArtifactsFolder() throws Exception {
        assertFalse("no artifacts folder expected", getArtifactsDirectory(m_workflowDir).exists());

        //modify the workflow
        executeAllAndWait();

        //... and save
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);

        //check for expected artifacts
        File artifactsDirectory = getArtifactsDirectory(m_workflowDir);
        File workflowConfig = new File(artifactsDirectory, "workflow-configuration.json");
        assertTrue("'workflow-configuration.json' missing", workflowConfig.exists());

        //test few random samples of the file content
        String workflowConfigContent = FileUtils.readFileToString(workflowConfig, StandardCharsets.UTF_8);
        assertThat("unexpected file content", workflowConfigContent, containsString("entry1"));
        assertThat("unexpected file content", workflowConfigContent, containsString("multiple-selection-14"));
        assertThat("unexpected file content", workflowConfigContent,
            containsString(CoreConstants.WORKFLOW_VARIABLES + "test2"));
        assertThat("unexpected file content", workflowConfigContent,
            containsString("213.0"));
        assertThat("unexpected file content", workflowConfigContent,
            containsString("password"));
        assertThat("unexpected file content", workflowConfigContent,
            containsString(CoreConstants.WORKFLOW_CREDENTIALS + "test2"));
    }

    /**
     * Tests the {@link WorkflowManager#getConfigurationNodes()} and
     * {@link WorkflowManager#setConfigurationNodes(Map)}-methods.
     *
     * @throws Exception
     */
    @Test
    public void testGetAndSetConfigurationNodes() throws Exception {
        executeAllAndWait();
        checkState(m_stringConfiguration_1, InternalNodeContainerState.EXECUTED);

        Map<String, DialogNode> configurationNodes = getManager().getConfigurationNodes(true);
        assertThat("unexptected number of config nodes", configurationNodes.size(), is(10));

        Map<String, JsonValue> configuration = new HashMap<>();
        configuration.put("string-input", Json.createObjectBuilder().add("string", "new_config").build().get("string"));
        getManager().setConfigurationNodes(configuration);
        checkState(m_stringConfiguration_1, InternalNodeContainerState.CONFIGURED);
        checkState(m_doubleConfiguration_2, InternalNodeContainerState.EXECUTED);
        FlowObjectStack outgoingFlowObjectStack =
            ((NativeNodeContainer)getManager().getNodeContainer(m_stringConfiguration_1)).getOutgoingFlowObjectStack();
        assertThat("flow variable hasn't been set",
            outgoingFlowObjectStack.getAvailableFlowVariables().get("string-input").getStringValue(), is("new_config"));
    }

    /**
     * Tests correctly failing validation of configuration values.
     *
     * @throws JsonException
     * @throws InvalidSettingsException
     */
    @Test
    public void testSetInvalidConfiguration() throws JsonException, InvalidSettingsException {
        Map<String, JsonValue> configuration = new HashMap<>();
        configuration.put("number-input-2", Json.createObjectBuilder().add("double", -1.0).build().get("double"));

        exception.expect(InvalidSettingsException.class);
        exception.expectMessage("The set double -1.0 is smaller than the allowed minimum of 0.0");
        getManager().setConfigurationNodes(configuration);
    }

    /**
     * Tests setting of configuration parameters that are ambiguous (i.e. two configuration nodes have the same
     * parameter name set).
     *
     * @throws Exception
     */
    @Test
    public void testSetAmbiguousConfigurationNode() throws Exception {
        Map<String, JsonValue> configuration = new HashMap<>();
        configuration.put("number-input", Json.createObjectBuilder().add("double", 3.14).build().get("double"));

        exception.expect(InvalidSettingsException.class);
        exception.expectMessage("Parameter name \"number-input\" doesn't match any node in the workflow");
        getManager().setConfigurationNodes(configuration);
    }
}
