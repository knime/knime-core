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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.CoreConstants;
import org.knime.core.util.FileUtil;

/**
 * Tests that the top-level configurations (configuration-/dialog nodes and workflow variables) can be correctly set via
 * their respective toJson methods.
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class EnhAP12286_LoadFromJson extends WorkflowTestCase {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private File m_workflowDir;

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

    private void initWorkflowFromTemp() throws Exception {
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
        setManager(loadResult.getWorkflowManager());
    }

    /**
     * Checks that the configuration files in the .artifacts folder remains the same after save, i.e. that we
     * didn't change the schema by accident.
     *
     * @throws Exception
     */
    @Test
    public void testSaveWorkflowConfiguration() throws Exception {
        File artifactsDirectory = getArtifactsDirectory(m_workflowDir);
        File expectedWorkflowConfig = new File(m_workflowDir, "/Data/workflow-configuration.json");
        File actualWorkflowConfig = new File(artifactsDirectory, "workflow-configuration.json");

        JsonObject expectedConfigContent = null;

        try (final JsonReader reader = Json.createReader(FileUtils.openInputStream(expectedWorkflowConfig))) {
            expectedConfigContent = reader.readObject();
        }

        //modify the workflow
        executeAllAndWait();

        //... and save
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);

        assertTrue("'workflow-configuration.json' missing", actualWorkflowConfig.exists());
        JsonObject actualConfigContent = null;

        try (final JsonReader reader = Json.createReader(FileUtils.openInputStream(actualWorkflowConfig))) {
            actualConfigContent = reader.readObject();
        }

        assertThat("Configuration changed", expectedConfigContent, is(actualConfigContent));

        expectedWorkflowConfig = new File(m_workflowDir, "/Data/workflow-configuration-representation.json");
        actualWorkflowConfig = new File(artifactsDirectory, "workflow-configuration-representation.json");

        assertTrue("'workflow-configuration.json' missing", actualWorkflowConfig.exists());

        try (final JsonReader reader = Json.createReader(FileUtils.openInputStream(expectedWorkflowConfig))) {
            expectedConfigContent = reader.readObject();
        }
        try (final JsonReader reader = Json.createReader(FileUtils.openInputStream(actualWorkflowConfig))) {
            actualConfigContent = reader.readObject();
        }

        assertThat("Configuration changed", expectedConfigContent, is(actualConfigContent));
    }

    /**
     * Checks that the configuration files in the .artifacts folder remains the same after save.
     *
     * @throws Exception
     */
    @Test
    public void testLoadFromJson() throws Exception {
        File input = new File(m_workflowDir, "/Data/input.json");
        File conf = new File(m_workflowDir, "/Data/workflow-configuration-updated.json");
        JsonObject workflowConfigContentExp = null;// FileUtils.readFileToString(conf, StandardCharsets.UTF_8);
        JsonObject inputJson = null;

        try (final JsonReader reader = Json.createReader(FileUtils.openInputStream(input))) {
            inputJson = reader.readObject();
        }
        try (final JsonReader reader = Json.createReader(FileUtils.openInputStream(conf))) {
            workflowConfigContentExp = reader.readObject();
        }

        final Map<String, JsonValue> inputMap = new HashMap<>();
        inputJson.entrySet().stream()
            .filter(e -> !(e.getKey().startsWith(CoreConstants.WORKFLOW_CREDENTIALS)
                || e.getKey().startsWith(CoreConstants.WORKFLOW_VARIABLES)))
            .forEach(e -> inputMap.put(e.getKey(), e.getValue()));

        getManager().setConfigurationNodes(inputMap);

        for (Entry<String, DialogNode> entry : getManager().getConfigurationNodes(true).entrySet()) {
            final JsonValue expectedValue = workflowConfigContentExp.get(entry.getKey());
            final JsonValue actualValue = entry.getValue().getDialogValue().toJson();

            assertThat("Wrong JsonValue for key '" + entry.getKey() + "'", actualValue, is(expectedValue));
        }
    }
}
