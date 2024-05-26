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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.knime.core.customization.APCustomization;
import org.knime.core.customization.APCustomizationProviderService;
import org.knime.core.customization.APCustomizationProviderServiceImpl;
import org.knime.core.internal.CorePlugin;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.missing.MissingNodeFactory;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/** 
 * Tests node blacklisting via customization profile, specifically workflow load/save/load.
 * 
 * @author Bernd Wiswedel
 */
public class EnhAP22255_DisallowNodesOnWorkflowLoad extends WorkflowTestCase {

	private static final String DISALLOWED_NODES_CUSTOMIZATION_YML = """
	    nodes:
	      filter:
	      - scope: use
	        rule: deny
	        predicate:
	          type: pattern
	          patterns:
	            - org.knime.base.node.preproc.append.row.AppendedRowsNodeFactory
	            - org.knime.base.node.preproc.normalize3.Normalizer3NodeFactory
	          isRegex: false
	  """;

	@Rule
    public TemporaryFolder m_tempFolder = new TemporaryFolder();
	
	private File m_tempWorkflowDir;
	private ServiceRegistration<APCustomizationProviderService> m_temporaryCustomizationServiceRegistration;
	
	private NodeID m_tableCreator_3;
	private NodeID m_concatenate_2;
	private NodeID m_normalizer_4;
	private NodeID m_normalizerApply_8;
	private NodeID m_diffChecker_9;
	
	@Before
	public void setUp() throws Exception {
		m_tempWorkflowDir = m_tempFolder.newFolder(EnhAP22255_DisallowNodesOnWorkflowLoad.class.getSimpleName());
		final var defaultWorkflowDirectory = getDefaultWorkflowDirectory();
		m_tempWorkflowDir = new File(m_tempFolder.getRoot(), defaultWorkflowDirectory.getName());
		FileUtils.copyDirectory(defaultWorkflowDirectory, m_tempWorkflowDir);
	}
	
	@Override
	protected NodeID loadAndSetWorkflow() throws Exception {
		final var workflowID = loadAndSetWorkflow(m_tempWorkflowDir);
		m_tableCreator_3 = workflowID.createChild(3);
		m_concatenate_2 = workflowID.createChild(2);
		m_normalizer_4 = workflowID.createChild(4);
		m_normalizerApply_8 = workflowID.createChild(8);
		m_diffChecker_9 = workflowID.createChild(9);
		return workflowID;
	}

	/** Just a plain execute, no missing nodes, all green. */
	@Test
	public void testExecuteUnchanged() throws Exception {
		loadAndSetWorkflow();
		checkState(m_tableCreator_3, CONFIGURED);
		checkState(getManager(), CONFIGURED);
		executeAllAndWait();
		checkState(getManager(), EXECUTED);
	}
	
	/** Load workflow, expect missing nodes to be inserted, can't execute the missing nodes. */
	@Test
	public void testWithTemporaryCustomization() throws Exception {
	    applyCustomization();

		loadAndSetWorkflow();
        checkState(m_tableCreator_3, CONFIGURED);

        executeAllAndWait();
        checkState(m_tableCreator_3, EXECUTED);
        checkState(getManager(), IDLE);
		assertConcatenateNodeIsMissing();
		assertNormalizerNodeIsMissing();
        checkStateOfMany(IDLE, m_concatenate_2, m_normalizer_4);
        checkState(getManager(), IDLE);
	}
	
	/** Load workflow, expect missing nodes to be inserted, save & load, should still be missing. */
	@Test
	public void testWithTemporaryCustomizationThenSaveAndLoad() throws Exception {
		applyCustomization();
		
		loadAndSetWorkflow();
		
		executeAllAndWait();
		checkState(m_tableCreator_3, EXECUTED);
		assertConcatenateNodeIsMissing();
		
		getManager().save(m_tempWorkflowDir, new ExecutionMonitor(), true);
		closeWorkflow();
		loadAndSetWorkflow();
		
		assertConcatenateNodeIsMissing();
		assertNormalizerNodeIsMissing();
	}

	/**
	 * Load workflow without customization, run to completion, then save, apply
	 * customization, load, expect nodes to be executed but not executable.
	 */
	@Test
	public void testLoadExecutedFlowIntoTemporaryCustomizationThenSaveAndLoad() throws Exception {
		loadAndSetWorkflow();
		executeAllAndWait();
		checkState(getManager(), EXECUTED);
		getManager().save(m_tempWorkflowDir, new ExecutionMonitor(), true);
		closeWorkflow();

		applyCustomization();
		loadAndSetWorkflow();
		
		checkState(getManager(), EXECUTED);

		assertConcatenateNodeIsMissing();
		checkState(m_concatenate_2, EXECUTED);

		assertNormalizerNodeIsMissing();
		checkState(m_normalizer_4, EXECUTED);
		
		reset(m_normalizerApply_8);
		executeAllAndWait();
		checkState(m_normalizerApply_8, EXECUTED);
		checkState(m_diffChecker_9, EXECUTED);
		
		reset(m_normalizer_4);
		executeAllAndWait();
		checkState(m_normalizer_4, IDLE);
		checkState(getManager(), IDLE);
	}
	
	/**
	 * Most complex: save fully executed workflow, load with customization applied, mark all dirty, save and reopen
	 * without customization - expecting fully executed workflow, which can be reset and executed.  
	 * @throws Exception
	 */
	@Test
	public void testFullRoundtrip() throws Exception {
		loadAndSetWorkflow();
		executeAllAndWait();
		checkState(getManager(), EXECUTED);
		getManager().save(m_tempWorkflowDir, new ExecutionMonitor(), true);
		closeWorkflow();
		
		applyCustomization();
		loadAndSetWorkflow();
		
		checkState(getManager(), EXECUTED);
		for (NodeContainer nc : getManager().getNodeContainers()) {
			nc.setDirty();
		}
		getManager().save(m_tempWorkflowDir, new ExecutionMonitor(), true);
		closeWorkflow();
		
		unsetCustomization();
		loadAndSetWorkflow();
		checkState(getManager(), EXECUTED);

		reset(m_tableCreator_3);
		executeAllAndWait();
		checkState(getManager(), EXECUTED);
	}
	
	@Override
	public void tearDown() throws Exception {
		unsetCustomization();
		super.tearDown();
	}

	private void assertConcatenateNodeIsMissing() {
		final WorkflowManager wfm = getManager();
		final NativeNodeContainer nnc = wfm.getNodeContainer(m_concatenate_2, NativeNodeContainer.class, true);
		assertThat("Concatenate has been replaced by missing node", nnc.getNode().getFactory(),
				isA((Class) MissingNodeFactory.class));
		// 5 real ports + flow var
		assertThat("Missing node placeholder for concatenate node has number of inputs", nnc.getNrInPorts(), is(6));
		assertThat("Missing node placeholder for concatenate node has data output", nnc.getOutputType(1),
				is(BufferedDataTable.TYPE));
	}
	
	private void assertNormalizerNodeIsMissing() {
		final WorkflowManager wfm = getManager();
		final NativeNodeContainer nnc = wfm.getNodeContainer(m_normalizer_4, NativeNodeContainer.class, true);
		assertThat("Normalizer has been replaced by missing node", nnc.getNode().getFactory(),
				isA((Class) MissingNodeFactory.class));
		// 2 real ports + flow var
		assertThat("Missing node placeholder for normalizer node has number of inputs", nnc.getNrInPorts(), is(2));
		assertThat("Missing node placeholder for normalizer node has number of outputs", nnc.getNrOutPorts(), is(3));
		assertThat("Missing node placeholder for concatenate node has data output", nnc.getOutputType(2).getName(),
				containsString("Normalizer"));
	}
	
	
	/** Applies a customization that forbids the use of both the Concatenate and Normalizer Node. */
	private void applyCustomization() throws JsonProcessingException, JsonMappingException {
		// Register the test-specific customization service
	    final var context = FrameworkUtil.getBundle(CorePlugin.class).getBundleContext();
	    final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);  // Use the highest possible ranking
        
		final APCustomization tempCustomization = new ObjectMapper(new YAMLFactory())
				.readValue(DISALLOWED_NODES_CUSTOMIZATION_YML, APCustomization.class);

        m_temporaryCustomizationServiceRegistration = context.registerService(
            APCustomizationProviderService.class, () -> tempCustomization, properties);
	}

	private void unsetCustomization() {
		if (m_temporaryCustomizationServiceRegistration != null) {
			m_temporaryCustomizationServiceRegistration.unregister();
			m_temporaryCustomizationServiceRegistration = null;
			final var context = FrameworkUtil.getBundle(CorePlugin.class).getBundleContext();
			context.registerService(APCustomizationProviderService.class, new APCustomizationProviderServiceImpl(),
					null);
		}
	}

	
}
