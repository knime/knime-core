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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.ComponentMetadata.ComponentNodeType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LoadVersion;

/**
 * Tests to save and load component metadata with a component.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class EnhAP13038_ComponentMetadata extends WorkflowTestCase {

    private File m_workflowDir;

    private NodeID m_component_4;

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
        m_component_4 = new NodeID(baseID, 4);
        return loadResult;
    }

    /**
     * Checks that loading, saving and reloading of component metadata works as expected.
     *
     * @throws Exception
     */
    @Test
    public void testLoadSaveAndReloadComponentMetadata() throws Exception {
        SubNodeContainer component = (SubNodeContainer)getManager().getNodeContainer(m_component_4);
        ComponentMetadata metadata = component.getMetadata();
        assertTrue("unexpected load version", getManager().getLoadVersion().isOlderThan(LoadVersion.V4010));
        assertThat("unexpected metadata", metadata.getDescription().get(), containsString("original"));
        assertThat("unexpected metadata", metadata.getInPortNames().get()[0], containsString("original"));
        assertThat("unexpected metadata", metadata.getOutPortNames().get()[0], containsString("original"));
        assertThat("unexpected metadata", metadata.getInPortDescriptions().get()[0], containsString("original"));
        assertThat("unexpected metadata", metadata.getOutPortDescriptions().get()[0], containsString("original"));
        assertFalse("unexpected metadata", metadata.getIcon().isPresent());
        assertFalse("unexpected metadata", metadata.getNodeType().isPresent());

        //set new metadata
        component.setMetadata(createComponentMetadata());
        checkInOutNodes(component);

        //save and re-load workflow and check metadata
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
        closeWorkflow();
        initWorkflowFromTemp();
        component = (SubNodeContainer)getManager().getNodeContainer(m_component_4);
        assertThat("unexpected load version", getManager().getLoadVersion(), is(LoadVersion.V4010));
        metadata = component.getMetadata();
        checkComponentMetadata(metadata);
        checkInOutNodes(component);
    }

    private static ComponentMetadata createComponentMetadata() {
        return ComponentMetadata.builder().description("new").addInPortNameAndDescription("new name", "new desc")
            .addOutPortNameAndDescription("new out name", "new out desc").icon("test_icon".getBytes())
            .type(ComponentNodeType.MANIPULATOR).build();
    }

    private static void checkComponentMetadata(final ComponentMetadata metadata) {
        assertThat("unexpected metadata", metadata.getDescription().get(), is("new"));
        assertThat("unexpected metadata", metadata.getInPortNames().get()[0], is("new name"));
        assertThat("unexpected metadata", metadata.getOutPortNames().get()[0], is("new out name"));
        assertThat("unexpected metadata", metadata.getInPortDescriptions().get()[0], is("new desc"));
        assertThat("unexpected metadata", metadata.getOutPortDescriptions().get()[0], is("new out desc"));
        assertThat("unexpected metadata", new String(metadata.getIcon().get()), is("test_icon"));
        assertThat("unexpected metadata", metadata.getNodeType().get(), is(ComponentNodeType.MANIPULATOR));
    }

    @SuppressWarnings("deprecation")
    private static void checkInOutNodes(final SubNodeContainer component) {
        //make sure that descriptions are also stored in input and output node (for backwards compatibility)
        assertThat("metadata not transfert to input node", component.getVirtualInNodeModel().getSubNodeDescription(),
            is("new"));
        assertThat("metadata not transfert to input node", component.getVirtualInNodeModel().getPortNames()[0],
            is("new name"));
        assertThat("metadata not transfert to input node", component.getVirtualInNodeModel().getPortDescriptions()[0],
            is("new desc"));
        assertThat("metadata not transfert to output node", component.getVirtualOutNodeModel().getPortNames()[0],
            is("new out name"));
        assertThat("metadata not transfert to output node", component.getVirtualOutNodeModel().getPortDescriptions()[0],
            is("new out desc"));
    }

    /**
     * Checks that adding and removing ports is reflected in the metadata port names and descriptions, for both, legacy
     * and up-to-date workflow.
     *
     * @throws Exception
     */
    @Test
    public void testAddRemovePorts() throws Exception {
        assertTrue("unexpected load version", getManager().getLoadVersion().isOlderThan(LoadVersion.V4010));
        testAddRemovePortsInternal();

        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
        closeWorkflow();
        initWorkflowFromTemp();
        assertThat("unexpected load version", getManager().getLoadVersion(), is(LoadVersion.V4010));
        testAddRemovePortsInternal();
    }

    private void testAddRemovePortsInternal() {
        SubNodeContainer component = (SubNodeContainer)getManager().getNodeContainer(m_component_4);
        //and an new in- and out-port and check resulting names and description
        getManager().changeSubNodeInputPorts(component.getID(),
            new MetaPortInfo[]{
                MetaPortInfo.builder().setOldIndex(0).setNewIndex(0).setIsConnected(false)
                    .setPortType(FlowVariablePortObject.TYPE).build(),
                MetaPortInfo.builder().setOldIndex(1).setNewIndex(1).setIsConnected(true)
                    .setPortType(BufferedDataTable.TYPE).build(),
                MetaPortInfo.builder().setOldIndex(-1).setNewIndex(2).setIsConnected(false)
                    .setPortType(BufferedDataTable.TYPE).build()});
        getManager().changeSubNodeOutputPorts(component.getID(),
            new MetaPortInfo[]{
                MetaPortInfo.builder().setOldIndex(0).setNewIndex(0).setIsConnected(false)
                    .setPortType(FlowVariablePortObject.TYPE).build(),
                MetaPortInfo.builder().setOldIndex(1).setNewIndex(1).setIsConnected(true)
                    .setPortType(BufferedDataTable.TYPE).build(),
                MetaPortInfo.builder().setOldIndex(-1).setNewIndex(2).setIsConnected(false)
                    .setPortType(BufferedDataTable.TYPE).build()});
        ComponentMetadata metadata = component.getMetadata();
        assertThat("no default set for new in port", metadata.getInPortNames().get()[1], is("Port 2"));
        assertThat("no default set for new in port", metadata.getInPortDescriptions().get()[1], is(""));
        assertThat("no default set for new out port", metadata.getOutPortNames().get()[1], is("Port 2"));
        assertThat("no default set for new out port", metadata.getOutPortDescriptions().get()[1], is(""));

        //remove in- and out-port again
        getManager().changeSubNodeInputPorts(component.getID(),
            new MetaPortInfo[]{
                MetaPortInfo.builder().setOldIndex(0).setNewIndex(0).setIsConnected(false)
                    .setPortType(FlowVariablePortObject.TYPE).build(),
                MetaPortInfo.builder().setOldIndex(1).setNewIndex(1).setIsConnected(true)
                    .setPortType(BufferedDataTable.TYPE).build()});
        getManager().changeSubNodeOutputPorts(component.getID(),
            new MetaPortInfo[]{
                MetaPortInfo.builder().setOldIndex(0).setNewIndex(0).setIsConnected(false)
                    .setPortType(FlowVariablePortObject.TYPE).build(),
                MetaPortInfo.builder().setOldIndex(1).setNewIndex(1).setIsConnected(true)
                    .setPortType(BufferedDataTable.TYPE).build()});
        metadata = component.getMetadata();
        assertThat("unexpected number of inport names", metadata.getInPortNames().get().length, is(1));
        assertThat("unexpected number of inport descriptions", metadata.getInPortNames().get().length, is(1));
        assertThat("unexpected number of outport names", metadata.getOutPortNames().get().length, is(1));
        assertThat("unexpected number of outport descriptions", metadata.getOutPortNames().get().length, is(1));
    }

    /**
     * Makes sure that all metadata is copied, too, when a component is copied.
     */
    @Test
    public void testCopyPasteComponentWithMetadata() {
        SubNodeContainer component = (SubNodeContainer)getManager().getNodeContainer(m_component_4);
        component.setMetadata(createComponentMetadata());
        WorkflowCopyContent.Builder cntBuilder = WorkflowCopyContent.builder();
        cntBuilder.setNodeIDs(m_component_4);
        WorkflowPersistor copy = getManager().copy(cntBuilder.build());
        WorkflowCopyContent paste = getManager().paste(copy);
        component = (SubNodeContainer)getManager().getNodeContainer(paste.getNodeIDs()[0]);
        checkComponentMetadata(component.getMetadata());
    }

}
