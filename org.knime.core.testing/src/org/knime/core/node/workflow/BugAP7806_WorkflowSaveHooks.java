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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.openapi.OpenAPIDefinitionGenerator;
import org.knime.core.util.FileUtil;
import org.knime.core.util.TestWorkflowSaveHook;

/**
 * Runs the workflow save hooks (which counts nodes in a workflow ...)
 *
 * @author wiswedel, University of Konstanz
 */
public class BugAP7806_WorkflowSaveHooks extends WorkflowTestCase {

    private File m_workflowDir;

    private File m_testFile;

    private NodeID m_jsonInput;

    @Before
    public void setUp() throws Exception {
        TestWorkflowSaveHook.setEnabled(true);
        m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
        m_testFile =
            new File(new File(m_workflowDir, WorkflowSaveHook.ARTIFACTS_FOLDER_NAME), TestWorkflowSaveHook.OUT_FILE);
        initWorkflowFromTemp();
    }

    private WorkflowLoadResult initWorkflowFromTemp() throws Exception {
        // will save the workflow in one of the test ...don't write SVN folder
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
        setManager(loadResult.getWorkflowManager());
        NodeID baseID = getManager().getID();
        m_jsonInput = new NodeID(baseID, 7);
        return loadResult;
    }

    /** Just load and save with basic checks. */
    @Test
    public void testHookUnmodified() throws Exception {
        Assert.assertFalse("test file '" + m_testFile.getAbsolutePath() + "' not supposed to exist",
            m_testFile.isFile());
        getManager().setDirty();
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
        Assert.assertTrue("test file '" + m_testFile.getAbsolutePath() + "' supposed to exist", m_testFile.isFile());
        Assert.assertThat("Wrong number in test file", readFileContent(), is(3));
    }

    /** Test multiple saves with modification in between. */
    @Test
    public void testHookAfterNodeDeletion() throws Exception {
        getManager().setDirty();
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
        getManager().removeNode(m_jsonInput);
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
        Assert.assertThat("Wrong number in test file", readFileContent(), is(2));
    }

    /** Test a broken contribution of the extension point. */
    @Test
    public void testBrokenHook() throws Exception {
        getManager().setDirty();
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
        getManager().removeNode(m_jsonInput);
        TestWorkflowSaveHook.setWillFail(true);
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
        Assert.assertFalse("test file '" + m_testFile.getAbsolutePath() + "' not supposed to exist",
            m_testFile.isFile());
    }

    /** Read the file content and return the number contained in the file. */
    private int readFileContent() throws Exception {
        byte[] allBytes = Files.readAllBytes(m_testFile.toPath());
        String s = new String(allBytes, "UTF-8");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Unable to parse number from string \"" + s + "\"");
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        TestWorkflowSaveHook.setEnabled(false);
        FileUtil.deleteRecursively(m_workflowDir);
    }

    /**
     * Checks if the OpenAPI definition for the input parameters is generated correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testOpenAPIGeneration() throws Exception {
        getManager().setDirty();
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);

        Path openApiFragmentFile = m_workflowDir.toPath().resolve(WorkflowSaveHook.ARTIFACTS_FOLDER_NAME)
                .resolve(OpenAPIDefinitionGenerator.INPUT_PARAMETERS_FILE);

        String actualInputParameters;
        try (JsonReader reader = Json.createReader(Files.newInputStream(openApiFragmentFile))) {
            JsonObject o = reader.readObject();

            try (StringWriter sw = new StringWriter(); JsonWriter writer = Json.createWriter(sw)) {
                writer.write(o);
                actualInputParameters = sw.toString();
            }
        }

        String expectedInputParameters;
        try (JsonReader reader = Json.createReader(
            Files.newInputStream(m_workflowDir.toPath().resolve("openapi-input-parameters-reference.json")))) {
            JsonObject o = reader.readObject();

            try (StringWriter sw = new StringWriter(); JsonWriter writer = Json.createWriter(sw)) {
                writer.write(o);
                expectedInputParameters = sw.toString();
            }
        }

        assertThat("Unexpected input parameters definition written", actualInputParameters,
            is(expectedInputParameters));
    }
}
