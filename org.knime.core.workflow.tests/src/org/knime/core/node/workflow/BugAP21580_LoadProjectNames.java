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
 *
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;

/**
 * Test for https://knime-com.atlassian.net/browse/AP-21580
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public class BugAP21580_LoadProjectNames extends WorkflowTestCase {

    private NodeID m_baseId;

    @Before
    public void setUp() throws Exception {
        m_baseId = loadAndSetWorkflow();
    }

    @Test
    public void testLoadWorkflowComponent() throws Exception {
        WorkflowManager wfm = getManager();

        // top-level project name should be the directory name, the name field from disk should be ignored
        final var classSimpleName = getClass().getSimpleName();
        final var dirName = classSimpleName.substring(0, 1).toLowerCase() + classSimpleName.substring(1);
        assertEquals(dirName, wfm.getName());
        assertEquals(null, wfm.getNameField());

        // check old components
        final var comp1Id = new NodeID(m_baseId, 2);
        checkNodeContainerNames(wfm, comp1Id, true);

        // update the component
        final var loadHelper = new WorkflowLoadHelper(true, wfm.getContextV2());
        assertTrue(wfm.checkUpdateMetaNodeLink(comp1Id, loadHelper));
        assertTrue(wfm.hasUpdateableMetaNodeLink(comp1Id));
        final var updateResult = wfm.updateMetaNodeLink(comp1Id, new ExecutionMonitor(), loadHelper);
        assertEquals(LoadResultEntryType.Ok, updateResult.getType());

        // check updated components
        checkNodeContainerNames(wfm, comp1Id, true);

        // execute the workflow and check that the workflow path from the innermost context is correct
        executeAllAndWait();
        final var comp1 = wfm.getNodeContainer(comp1Id, SubNodeContainer.class, true);
        assertTrue(comp1.getNodeContainerState().isExecuted());
        final var flowVar = comp1.getOutgoingFlowObjectStack().getAvailableFlowVariables(StringType.INSTANCE) //
                .get("context.workflow.absolute-path");
        assertEquals(wfm.getNodeContainerDirectory().getFile(), new File(flowVar.getStringValue()));

        // load the outer component as project and check that the outer name field is `null`
        final var componentProject = loadComponentProject(new File(getDefaultWorkflowDirectory(), "data/Comp1"));
        try {
            checkNodeContainerNames(WorkflowManager.ROOT, componentProject.getID(), false);
        } finally {
            WorkflowManager.ROOT.removeNode(componentProject.getID());
        }
    }

    private static void checkNodeContainerNames(final WorkflowManager wfm, final NodeID comp1Id,
            final boolean outerHasNameSet) {
        // the outer component should sometimes have a name set
        final var comp1 = wfm.getNodeContainer(comp1Id, SubNodeContainer.class, true);
        final var comp1Wfm = comp1.getWorkflowManager();
        assertEquals("Comp1", comp1Wfm.getName());
        assertEquals(outerHasNameSet ? "Comp1" : null, comp1Wfm.getNameField());

        // the inner component should have a name set
        final var comp2Id = new NodeID(new NodeID(comp1Id, 0), 4);
        var comp2 = comp1Wfm.getNodeContainer(comp2Id, SubNodeContainer.class, true);
        var comp2Wfm = comp2.getWorkflowManager();
        assertEquals("Comp2", comp2Wfm.getName());
        assertEquals("Comp2", comp2Wfm.getNameField());

        // the metanode should have a name set
        final var metanodeId = new NodeID(new NodeID(comp2Id, 0), 5);
        var metanode = comp2Wfm.getNodeContainer(metanodeId, WorkflowManager.class, true);
        assertEquals("Metanode", metanode.getName());
        assertEquals("Metanode", metanode.getNameField());
    }

    private static SubNodeContainer loadComponentProject(final File componentTemplate) throws IOException,
            UnsupportedWorkflowVersionException, InvalidSettingsException, CanceledExecutionException {
        final var templateURI = componentTemplate.toURI();
        final var compProjLoadHelper = new WorkflowLoadHelper(true, true,
                WorkflowContextV2.forTemporaryWorkflow(componentTemplate.toPath(), null));
        final var loadPersistor = compProjLoadHelper.createTemplateLoadPersistor(componentTemplate, templateURI);
        final var loadResult = new MetaNodeLinkUpdateResult("Shared instance from \"" + templateURI + "\"");
        WorkflowManager.ROOT.load(loadPersistor, loadResult, new ExecutionMonitor(), false);
        assertEquals(LoadResultEntryType.Ok, loadResult.getType());
        return (SubNodeContainer)loadResult.getLoadedInstance();
    }
}
