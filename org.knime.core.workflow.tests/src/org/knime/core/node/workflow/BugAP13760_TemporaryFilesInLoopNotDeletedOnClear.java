package org.knime.core.node.workflow;

import org.junit.Before;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;

public class BugAP13760_TemporaryFilesInLoopNotDeletedOnClear extends WorkflowTestCase {

	@Before
	public void setUp() throws Exception {
		loadAndSetWorkflow();
	}

	@Test
	public void testExecutePlain() throws Exception {
		executeAllAndWait();
		final WorkflowManager manager = getManager();
		manager.getParent().resetAndConfigureNode(getManager().getID());
		final File tempDir = manager.getContext().getTempLocation();
		Thread.sleep(100); // give the file-in-background-deletion some time to do its work
		assertEquals(String.format("Files remaining in workflow temp directory after clear: %s.",
				Arrays.toString(tempDir.list())), 0, countFilesInDirectory(tempDir));
	}

}
