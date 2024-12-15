MISSINGpackage org.knime.core.node.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;

public class BugAP13760_TemporaryFilesInLoopNotDeletedOnClear extends WorkflowTestCase {

	@BeforeEach
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
		assertEquals(0, countFilesInDirectory(tempDir), String.format("Files remaining in workflow temp directory after clear: %s.",
				Arrays.toString(tempDir.list())));
	}

}