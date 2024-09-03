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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.knime.core.node.ExecutionMonitor;

/** 
 * Flow variable filtering test. 
 * 
 * @author Bernd Wiswedel, KNIME
 */
public class EnhAP16515_FlowVariableFilter extends WorkflowTestCase {

    private NodeID m_varCreator_NoScope_1;
    private NodeID m_flowVarFilter_NoScope_6;
    private NodeID m_flowVarFilter_WithScope_15;
    private NodeID m_diffChecker_NoScope_10;
    private NodeID m_diffChecker_WithScope_14;
    private NodeID m_loopEnd_WithScope_17;
    
    @Rule
    public TemporaryFolder m_tempFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws Exception {
    	File wfDir = m_tempFolder.newFolder(getClass().getSimpleName());
    	FileUtils.copyDirectory(getDefaultWorkflowDirectory(), wfDir);
    	loadAndSetWorkflow(wfDir);
    }
    
    @Override
    protected NodeID loadAndSetWorkflow(File workflowDir) throws Exception {
		NodeID baseID = super.loadAndSetWorkflow(workflowDir);
        m_varCreator_NoScope_1 = baseID.createChild(1);
        m_flowVarFilter_NoScope_6 = baseID.createChild(6);
        m_flowVarFilter_WithScope_15 = baseID.createChild(15);
        m_diffChecker_NoScope_10 = baseID.createChild(10);
        m_diffChecker_WithScope_14 = baseID.createChild(14);
        m_loopEnd_WithScope_17 = baseID.createChild(17);
        return baseID;
	}

    @Test
	public void testFullExecute() throws Exception {
    	executeAllAndWait();
    	checkState(getManager(), InternalNodeContainerState.EXECUTED);
	}

    @Test
    public void testLoopScopeAfterLoad() throws Exception {
    	executeAndWait(m_loopEnd_WithScope_17);
    	checkState(m_loopEnd_WithScope_17, InternalNodeContainerState.EXECUTED);
		checkVariablesOfFilterNodeInScope();
		
		final WorkflowManager managerPriorSave = getManager();
		final File wfDir = managerPriorSave.getWorkingDir().getFile();
		managerPriorSave.save(wfDir, new ExecutionMonitor(), true);
		
		closeWorkflow();
		loadAndSetWorkflow(wfDir);
		WorkflowManager managerAfterLoad = getManager();
		assertThat("saved and loaded workflow different", managerAfterLoad, is(not(sameInstance(managerPriorSave))));
		checkState(m_loopEnd_WithScope_17, InternalNodeContainerState.EXECUTED);
		
		executeAndWait(m_diffChecker_WithScope_14);
		checkState(m_diffChecker_WithScope_14, InternalNodeContainerState.EXECUTED);

		checkVariablesOfFilterNodeInScope();
    }

	protected void checkVariablesOfFilterNodeInScope() {
		final FlowObjectStack flowObjectStack = findNodeContainer(m_flowVarFilter_WithScope_15).getOutPort(1)
				.getFlowObjectStack();
		Set<String> flowVarNames = new LinkedHashSet<>(flowObjectStack.getAllAvailableFlowVariables().keySet());
		flowVarNames.removeIf(s -> s.startsWith(FlowVariable.Scope.Global.getPrefix()));
		final String msg = "Port output of filter node in scope";
		
    	assertThat(msg, flowVarNames, hasItems("string-a")); // configured to remain
		final String valueA = flowObjectStack.peekFlowVariable("string-a", VariableType.StringType.INSTANCE).get()
				.getStringValue();
		assertThat("Value of string-a", valueA, is("value-a"));
		
		// outside variable (int-b = 2)
		// then overwritten within loop with int-b = 5
		// then filtered in the loop but still there because it's defined upstream (design decision): int-b = 2 
    	assertThat(msg, flowVarNames, hasItems("int-b"));
		final int valueB = flowObjectStack.peekFlowVariable("int-b", VariableType.IntType.INSTANCE).orElseThrow()
				.getIntValue();
    	assertThat("Value of int-b", valueB, is(2));
    	
    	// inside loop variables - they can be removed (not asking if that's a smart idea)
		assertThat(msg, flowVarNames, not(hasItem("maxIterations")));
    	assertThat(msg, flowVarNames, not(hasItem("currentIteration")));
	}

	@Test
	public void testNoScopeAfterLoad() throws Exception {
		checkState(m_varCreator_NoScope_1, InternalNodeContainerState.CONFIGURED);
		checkState(m_flowVarFilter_NoScope_6, InternalNodeContainerState.CONFIGURED);
		checkVariablesOfFilterNodeNoScope();
		
		executeAndWait(m_diffChecker_NoScope_10);
		checkState(m_diffChecker_NoScope_10, InternalNodeContainerState.EXECUTED);
		checkVariablesOfFilterNodeNoScope();
		
		final WorkflowManager managerPriorSave = getManager();
		final File wfDir = managerPriorSave.getWorkingDir().getFile();
		managerPriorSave.save(wfDir, new ExecutionMonitor(), true);
		
		closeWorkflow();
		loadAndSetWorkflow(wfDir);
		WorkflowManager managerAfterLoad = getManager();
		assertThat("saved and loaded workflow different", managerAfterLoad, is(not(sameInstance(managerPriorSave))));
		checkState(m_diffChecker_NoScope_10, InternalNodeContainerState.EXECUTED);
		checkVariablesOfFilterNodeNoScope();
		
		reset(m_flowVarFilter_NoScope_6);
		executeAndWait(m_diffChecker_NoScope_10);
		checkState(m_diffChecker_NoScope_10, InternalNodeContainerState.EXECUTED);
	}
	
	protected void checkVariablesOfFilterNodeNoScope() {
		Set<String> flowVarNames = new LinkedHashSet<>(findNodeContainer(m_flowVarFilter_NoScope_6).getOutPort(1)
				.getFlowObjectStack().getAllAvailableFlowVariables().keySet());
		flowVarNames.removeIf(s -> s.startsWith(FlowVariable.Scope.Global.getPrefix()));
		final String msg = "Port output of filter node not in scope";
		
		assertThat(msg, flowVarNames, hasItem("string-a"));
		assertThat(msg, flowVarNames, not(hasItem("int-b")));
	}
	
}
