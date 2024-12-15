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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;

/**
 * AP-18797: File Download Widget: Download Link not working if Component ID is 0
 * 
 * The test case loads a workflow, finds an existing component (there is really only one component in the wkf), 
 * saves the component to a folder, instantiates a new instance from that component, assert that the IDs of the new 
 * instance(s) are as expected ( != 0)
 *
 * https://knime-com.atlassian.net/browse/AP-18797
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP18797_ComponentLinksStartingWithID0 extends WorkflowTestCase { // NOSONAR

    private NodeID m_component_2;

    @BeforeEach
    public void setUp() throws Exception {
        var baseID = loadAndSetWorkflow();
        m_component_2 = baseID.createChild(2);
    }

    /** Find component, save into temp folder, instantiate fresh and check IDs. */
    @Test
    public void test_shareComponent_thenInstantiate(@TempDir Path tempFolderRoot) throws Exception {
        var mgr = getManager();
        assertThat("Number nodes in loaded workflow", mgr.getNodeContainers().size(), is(1));
        var subNode = mgr.getNodeContainer(m_component_2, SubNodeContainer.class, true);

        /* Save existing component to a template */
        var tempFolder = Files.createDirectory(tempFolderRoot.resolve(subNode.getName())).toFile();
        subNode.saveAsTemplate(tempFolder, new ExecutionMonitor());

        /* Load from template location, expect ID = 1 (this is the change in AP-18797) */
        loadInstanceAndCheckID(tempFolder, 1);

        /* Next ID = 3 (because 2 is used by the original component above) */
        loadInstanceAndCheckID(tempFolder, 3);
    }

    private void loadInstanceAndCheckID(File tempFolder, int expectedIDIndex) throws IOException,
            UnsupportedWorkflowVersionException, InvalidSettingsException, CanceledExecutionException {
        WorkflowManager mgr = getManager();
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true);
        var templateLoadPersistor = loadHelper.createTemplateLoadPersistor(tempFolder,
                tempFolder.getAbsoluteFile().toURI());
        MetaNodeLinkUpdateResult loadResult = new MetaNodeLinkUpdateResult(
                "Meta Node Loading from " + tempFolder.getAbsolutePath());
        mgr.load(templateLoadPersistor, loadResult, new ExecutionMonitor(), false);
        assertThat("Error status of template loading", loadResult.getType(), is(LoadResultEntryType.Ok));
        assertThat("Type of loaded instance", loadResult.getLoadedInstance(), is(instanceOf(SubNodeContainer.class)));
        var container = (SubNodeContainer) loadResult.getLoadedInstance();
        assertThat("ID of loaded instance", container.getID().getIndex(), is(expectedIDIndex));
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

}