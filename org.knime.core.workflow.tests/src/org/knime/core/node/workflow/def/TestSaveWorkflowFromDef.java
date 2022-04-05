package org.knime.core.node.workflow.def;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;
import org.knime.core.node.workflow.DefWorkflowManagerWrapper;
import org.knime.core.node.workflow.FileWorkflowPersistor;
import org.knime.core.node.workflow.FileWorkflowSaver;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowTestCase;
import org.knime.core.workflow.def.StandaloneDef;

/**
 * Integration test to 1) load a workflow from knwf into the workflow manager,
 * 2) convert it to Def format and 3) back to knwf, making sure that 4) no
 * information gets lost.
 * 
 * 1) Loading a workflow directory is done via the {@link #loadAndSetWorkflow()}
 * provided by {@link WorkflowTestCase} which uses the legacy persistor code
 * under the hood, e.g., {@link FileWorkflowPersistor}.
 * 
 * 2) The conversion of the loaded workflow (which is basically an instance of
 * {@link WorkflowManager}) to POJOS (Def objects) is handled by wrapping the
 * workflow manager into adapter code, e.g., {@link DefWorkflowManagerWrapper}.
 * 
 * 3) TODO Not yet implemented. Building blocks for saving are in
 * {@link FileWorkflowSaver} but those methods still use business logic objects
 * from knime-core. Instead, they need to be rewritten to pull all the data from
 * Def objects.
 * 
 * 4) Comparing the workflows by comparing the source and target directory
 * contents. Tip: there's a compare function in KNIME AP that might be useful,
 * select two workflows in the Explorer window, right click and select compare.
 * 
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class TestSaveWorkflowFromDef extends WorkflowTestCase {

	@Test
	public void testRoundtrip() throws Exception {

		var workflowDef = loadWorkflow();
		// TODO save def to directory
		// TODO validate contents of output directory
		fail();

	}

	StandaloneDef loadWorkflow() throws Exception {
		// TODO pick a test workflow or inspect this one before settling on it
		loadAndSetWorkflow(new File("src/test/resources/enh11762_WorkflowRepresentation"));
		WorkflowManager wfm = getManager();

		// use an adapter to extract the information from the workflow manager into
		// POJOs
		// TODO the adapter code is not final, it may need fixing
		DefWorkflowManagerWrapper def = new DefWorkflowManagerWrapper(wfm);
		return def.asProjectDef();
	}

}
