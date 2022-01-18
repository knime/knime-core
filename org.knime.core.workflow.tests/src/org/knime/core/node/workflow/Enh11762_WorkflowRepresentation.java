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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.WorkflowProjectDef;
import org.knime.core.workflow.def.impl.DefaultWorkflowDef;
import org.knime.core.workflow.def.impl.DefaultWorkflowProjectDef;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Enh11762_WorkflowRepresentation extends WorkflowTestCase {

	/**
	 * Serialize a workflow from the JVM into JSON by wrapping the
	 * {@link WorkflowManager} to get a {@link WorkflowDef}-implementing version of
	 * it that can then be used to initialize a jackson-serializable object of class
	 * {@link DefaultWorkflowDef}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testToFile() throws Exception {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		String json = workflowToString(mapper);

		// restore POJO from String representation
		mapper.readValue(json, WorkflowProjectDef.class);
	}

	/**
	 * Restore a workflow from its JSON representation to an executable workflow in
	 * the JVM.
	 * @throws Exception 
	 */
	@Test
	public void testFromString() throws Exception {

		// get JSON representation of example workflow
		final ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		String json = workflowToString(mapper);

		// convert String to POJO
		WorkflowProjectDef pojo = mapper.readValue(json, WorkflowProjectDef.class);
		
		// create a workflow manager from the POJO using an appropriate persistor
		ExecutionMonitor exec = new ExecutionMonitor();
		WorkflowLoadHelper loader = new WorkflowLoadHelper(pojo);
		WorkflowLoadResult loadResult = WorkflowManager.ROOT.load(pojo, exec, loader, true);
        WorkflowManager restoredManager = loadResult.getWorkflowManager();

        //FIXME check that the workflow manager is present and looking good
		if (restoredManager == null) {
			fail("Errors reading workflow: " + loadResult.getFilteredError("", LoadResultEntryType.Ok));
			throw new Exception();
		}
//		if (loadResult.getType() != LoadResultEntryType.Ok) {
//			fail("Errors reading workflow: " + loadResult.getFilteredError("", LoadResultEntryType.Warning));
//		}

		// FIXME execute the restored workflow
//		restoredManager.executeAllAndWaitUntilDone();
//		var resultFromRestored = restoredManager.createExecutionResult(exec);

		// execute the original workflow...
		WorkflowManager originalManager = getManager();
		originalManager.executeAllAndWaitUntilDone();
		var resultFromOriginal = originalManager.createExecutionResult(exec);

		// FIXME implement a deep comparison for the execution results
//		assertEquals(resultFromOriginal, resultFromRestored);
	}

	/**
	 * Load the example workflow and generate a textual representation of it.
	 * 
	 * @param mapper Jackson mapper to use for POJO with Jackson annotations to JSON
	 *               conversion
	 * @return JSON representation of an example workflow.
	 * @throws Exception
	 */
	private String workflowToString(ObjectMapper mapper) throws Exception {
		loadAndSetWorkflow(new File("src/test/resources/enh11762_WorkflowRepresentation"));
		WorkflowManager wfm = getManager();

		// wrap workflow manager to get a workflow project definition interface implementation
		DefWorkflowManagerWrapper def = new DefWorkflowManagerWrapper(wfm);
		// copy information to information-only POJO
		DefaultWorkflowProjectDef projectDef = new DefaultWorkflowProjectDef(def.asProjectDef());

		// serialize into JSON
		String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(projectDef);

		// FIXME remove debug output
	    Path path = Paths.get(new File("src/test/resources").getAbsolutePath(), "Enh11762ExampleOutput.json");
		Files.writeString(path, pretty, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		
		return pretty;
	}

}
