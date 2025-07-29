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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.10.2012 (meinl): created
 */
package org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.knime.core.util.FileUtil;
import org.knime.core.util.MutableInteger;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Testcases for the BatchExecutor
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BatchExecutorTestcase {
	private static File standardTestWorkflowZip;

	private static File csvOut;

	/**
	 * Copy the test workflow zip into a temporary file.
	 *
	 * @throws Exception if an error occurs
	 */
	@BeforeAll
	public static void setup() throws Exception {
		standardTestWorkflowZip = findInPlugin("/files/BatchExecutorTestflow.zip");
		csvOut = File.createTempFile("BatchExecutorTest", ".csv");
		csvOut.deleteOnExit();
	}

	/**
	 * Creates a temporary file for the output file of the standard workflow.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@BeforeEach
	public void beforeEachTest() throws IOException {
		csvOut.delete();
	}

	/**
	 * Executed after each test, checks that there are no open workflows dangling
	 * around.
	 */
	@AfterEach
	public void checkDanglingWorkflows() {
		Collection<NodeContainer> openWorkflows = WorkflowManager.ROOT.getNodeContainers().stream()
				.filter(nc -> !StringUtils.containsAny(nc.getName(), WorkflowTestCase.KNOWN_CHILD_WFM_NAME_SUBSTRINGS))
				.collect(Collectors.toList());
		assertTrue(openWorkflows.isEmpty(), openWorkflows.size() + " dangling workflow(s) detected: " + openWorkflows);
	}

	/**
	 * Test if (in)valid arguments are correctly recognized.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testArguments() throws Exception {
		assertEquals(0, BatchExecutor.mainRun(new String[0]), "Non-zero return value for 0 arguments");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-XXXX123YYY" }),
				"Wrong return value for unknown argument");

		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-masterkey=key" }),
				"Wrong return value for correct masterkey");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-masterkey=" }),
				"Wrong return value for empty masterkey");

		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-credential" }),
				"Wrong return value for invalid credential argument");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-credential=" }),
				"Wrong return value for invalid credential argument");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-credential=;aaa" }),
				"Wrong return value for missing credential name");

		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-preferences" }),
				"Wrong return value for invalid preferences argument");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor.mainRun(new String[] { "-preferences=/aaa/bbb/ccc" }),
				"Wrong return value for missing preference file");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor
						.mainRun(new String[] { "-preferences=" + File.createTempFile("BatchExecutorTest", "prefs") }),
				"Wrong return value for invalid preferences file");

		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-workflowFile" }),
				"Wrong return value for invalid workflowFile argument");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor.mainRun(new String[] { "-workflowFile=/aaa/bbb/ddd" }),
				"Wrong return value for non-existing workflowFile");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor.mainRun(new String[] { "-workflowFile=" + System.getProperty("user.home") }),
				"Wrong return value for directory used as workflowFile");

		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-workflowDir" }),
				"Wrong return value for invalid workflowFile argument");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor.mainRun(new String[] { "-workflowDir=/aaa/bbb/ddd" }),
				"Wrong return value for non-existing workflowDir");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor.mainRun(new String[] { "-workflowDir=" + standardTestWorkflowZip }),
				"Wrong return value for file used as workflowDir");
		assertEquals(BatchExecutor.EXIT_ERR_LOAD,
				BatchExecutor.mainRun(new String[] { "-workflowDir=" + System.getProperty("java.home") }),
				"Wrong return value for non-workflow directory");
		assertEquals(BatchExecutor.EXIT_ERR_LOAD,
				BatchExecutor.mainRun(new String[] { "-workflowDir=" + FileUtil.createTempDir("BatchExecutorTest") }),
				"Wrong return value for empty workflow directory");

		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-destFile" }),
				"Wrong return value for invalid destFile argument");

		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-destDir" }),
				"Wrong return value for invalid destDir argument");

		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-workflow.variable" }),
				"Wrong return value for invalid workflow.variable argument");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-workflow.variable=name" }),
				"Wrong return value for invalid workflow variable");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor.mainRun(new String[] { "-workflow.variable=name,value" }),
				"Wrong return value for invalid workflow variable");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor.mainRun(new String[] { "-workflow.variable=name,value,type" }),
				"Wrong return value for invalid workflow variable type");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor.mainRun(new String[] { "-workflow.variable=name,value,int" }),
				"Wrong return value for wrong workflow variable type");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor.mainRun(new String[] { "-workflow.variable=name,value,double" }),
				"Wrong return value for wrong workflow variable type");

		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-option" }),
				"Wrong return value for invalid option argument");
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART,
				BatchExecutor.mainRun(new String[] { "-option=nodeId,name,value" }),
				"Wrong return value for invalid option");

		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, BatchExecutor.mainRun(new String[] { "-nosave" }),
				"Wrong return value for missing input");
	}

	/**
	 * Test if loading a workflow from a ZIP file works.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testLoadOnlyFromZip() throws Exception {
		int ret = BatchExecutor.mainRun(
				new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(), "-noexecute", "-nosave" });
		assertEquals(0, ret, "Non-zero return value");
	}

	/**
	 * Test if loading a workflow from a directory works.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testLoadOnlyFromDir() throws Exception {
		File tempDir = FileUtil.createTempDir("BatchExecutorTest");
		FileUtil.unzip(standardTestWorkflowZip, tempDir);

		int ret = BatchExecutor
				.mainRun(new String[] { "-workflowDir=" + tempDir.getAbsolutePath(), "-noexecute", "-nosave" });
		assertEquals(0, ret, "Non-zero return value");

		ret = BatchExecutor.mainRun(
				new String[] { "-workflowDir=" + tempDir.listFiles()[0].getAbsolutePath(), "-noexecute", "-nosave" });
		assertEquals(0, ret, "Non-zero return value");
	}

	/**
	 * Test if workflows in a ZIP file can be executed.
	 *
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void testExecuteFromZip() throws Exception {
		int ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-nosave", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String" });
		assertEquals(0, ret, "Non-zero return value");
		assertEquals(1001, countWrittenLines(csvOut), "Wrong number of lines in written CSV file");
	}

	/**
	 * Test if workflows from a directory can be executed.
	 *
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void testExecuteFromDir() throws Exception {
		File tempDir = FileUtil.createTempDir("BatchExecutorTest");
		FileUtil.unzip(standardTestWorkflowZip, tempDir);

		int ret = BatchExecutor.mainRun(new String[] { "-workflowDir=" + tempDir.getAbsolutePath(), "-nosave",
				"-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String" });
		assertEquals(0, ret, "Non-zero return value");
		assertEquals(1001, countWrittenLines(csvOut), "Wrong number of lines in written CSV file");

		csvOut.delete();
		ret = BatchExecutor.mainRun(new String[] { "-workflowDir=" + tempDir.listFiles()[0].getAbsolutePath(),
				"-nosave", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String" });
		assertEquals(0, ret, "Non-zero return value");
		assertEquals(1001, countWrittenLines(csvOut), "Wrong number of lines in written CSV file");
	}

	/**
	 * Test if changing workflow variables from command line works.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testWorkflowVariables() throws Exception {
		final int maxRows = 100;
		int ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-nosave", "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
				"-workflow.variable=maxRows," + maxRows + ",int" });
		assertEquals(0, ret, "Non-zero return value");
		assertEquals(maxRows + 1, countWrittenLines(csvOut), "Wrong number of lines in written CSV file");

		ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-nosave", "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
				"-workflow.variable=minimumValue,0.5,double" });
		assertEquals(0, ret, "Non-zero return value");
		assertEquals(439, countWrittenLines(csvOut), "Wrong number of lines in written CSV file");

		// unknown workflow variables only issue a warning but are ignored otherwise
		ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-nosave", "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
				"-workflow.variable=unknown,0.5,double" });
		assertEquals(0, ret, "Non-zero return value");
		ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-nosave", "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
				"-workflow.variable=unknown,0.5,double", "-workflow.variable=unknown2,nix,String" });
		assertEquals(0, ret, "Non-zero return value");
	}

	/**
	 * Test if settings node options via command line works.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testSetOption() throws Exception {
		int ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-nosave", "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
				"-option=0/5,rowFilter/RowRangeStart,1000,int" });
		assertEquals(0, ret, "Non-zero return value");
		assertEquals(1, countWrittenLines(csvOut), "Wrong number of lines in written CSV file");

		ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-nosave", "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
				"-option=0/5,rowFilter/RowRangeStart,1000,int", "-option=0/5,rowFilter/longValue,1000,long",
				"-option=0/5,rowFilter/shortValue,1000,short", "-option=0/5,rowFilter/byteValue,100,byte",
				"-option=0/5,rowFilter/booleanValue,true,boolean", "-option=0/5,rowFilter/charValue,X,char",
				"-option=0/5,rowFilter/floatValue,1000.1,float", "-option=0/5,rowFilter/floatValue,1000.1,double",
				"-option=0/5,rowFilter/StringValue,test,String",
				"-option=0/5,rowFilter/StringCellValue,1000.1,StringCell",
				"-option=0/5,rowFilter/DoubleCellValue,1000.1,DoubleCell",
				"-option=0/5,rowFilter/IntCellValue,1000,IntCell",
				"-option=0/5,rowFilter/LongCellValue,10000000000000000,LongCell",
				"-option=0/99,nonexistingNode,1000,IntCell", });
		assertEquals(0, ret, "Non-zero return value");

		ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-nosave", "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
				"-option=0/5,rowFilter/RowRangeStart,1000,unknownType" });
		assertEquals(BatchExecutor.EXIT_ERR_PRESTART, ret, "Wrong option type not reported");
	}

	/**
	 * Test if loading preferences works (no real test of they are actually
	 * applied).
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testPreferences() throws Exception {
		File prefs = findInPlugin("/files/batch.prefs");
		int ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-noexecute", "-nosave", "-preferences=" + prefs.getAbsolutePath() });
		assertEquals(0, ret, "Loading preferences failed");
	}

	/**
	 * Test if load errors are reported correctly.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testFailOnLoadError() throws Exception {
		File testflowZip = findInPlugin("/files/BatchExecutorTestflowLoadError.zip");
		int ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + testflowZip.getAbsolutePath(), "-noexecute",
				"-nosave", "-failonloaderror" });
		assertEquals(BatchExecutor.EXIT_ERR_LOAD, ret, "Fail-on-load-error does not work");

		testflowZip = findInPlugin("/files/BatchExecutorTestflowUnsupportedVersion.zip");
		ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + testflowZip.getAbsolutePath(), "-noexecute",
				"-nosave", "-failonloaderror" });
		assertEquals(BatchExecutor.EXIT_ERR_LOAD, ret, "Unsupported workflow version not handled correctly");
	}

	/**
	 * Test if canceling using the .cancel-file works.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testCancel() throws Exception {
		final MutableInteger ret = new MutableInteger(-1);
		final AtomicReference<Exception> exception = new AtomicReference<Exception>();

		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					ret.setValue(BatchExecutor
							.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
									"-nosave", "-workflow.variable=skipLongrunner,0,int" }));
				} catch (Exception ex) {
					exception.set(ex);
				}
			}
		};
		t.start();

		Thread.sleep(2000);
		File ws = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		File cancelFile = new File(ws, ".cancel");
		cancelFile.createNewFile();

		t.join(5000);
		assertFalse(t.isAlive(), "Workflow did not finish 5000ms after cancel request");
		assertEquals(BatchExecutor.EXIT_ERR_EXECUTION, ret.intValue(), "Wrong return value for canceled execution");
		if (exception.get() != null) {
			throw exception.get();
		}
		t.interrupt();
	}

	/**
	 * Test if saving the executed workflow to a zip file works.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testSaveToZip() throws Exception {
		File destFile = File.createTempFile("BatchExecutorTest", ".zip");
		destFile.deleteOnExit();

		int ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
				"-destFile=" + destFile.getAbsolutePath() });
		assertEquals(0, ret, "Non-zero return value");
		assertTrue(destFile.length() > 0.9 * standardTestWorkflowZip.length(), "ZIP file is too small");

		// check if it is really a zip file and the workflow is named correctly
		try (ZipFile zf = new ZipFile(destFile)) {
			String expectedWorkflowName = destFile.getName().replaceAll("\\.zip$", "");
			assertThat("Workflow not named correctly", zf.getEntry(expectedWorkflowName), is(not(nullValue())));
		}
		// save in place
		FileUtil.copy(standardTestWorkflowZip, destFile);
		long timestamp = destFile.lastModified();
		Thread.sleep(1000);
		ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + destFile.getAbsolutePath(),
				"-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String" });
		assertEquals(0, ret, "Non-zero return value");
		assertTrue(destFile.length() > 0.9 * standardTestWorkflowZip.length(), "ZIP file is too small");
		// check if it is really a zip file
		try (ZipFile zf = new ZipFile(destFile)) {
		}
		assertTrue(destFile.lastModified() > timestamp, "Workflow not altered after in-place save");
	}

	/**
	 * Test if saving the executed workflow to a directory works.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testSaveToDir() throws Exception {
		File destDir = FileUtil.createTempDir("BatchExecutorTest");

		int ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
				"-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
				"-destDir=" + destDir.getAbsolutePath() });
		assertEquals(0, ret, "Non-zero return value");
		assertTrue(destDir.list().length > 5, "Not enough file in destination directory");
		assertTrue(new File(destDir, WorkflowPersistor.WORKFLOW_FILE).isFile(), "No workflow in destination directory");

		// save in place
		FileUtil.deleteRecursively(destDir);
		destDir.mkdir();
		FileUtil.unzip(standardTestWorkflowZip, destDir);
		destDir = destDir.listFiles()[0]; // workflow is in a subdirectory of the zip
		File workflowFile = new File(destDir, WorkflowPersistor.WORKFLOW_FILE);
		long timestamp = workflowFile.lastModified();
		Thread.sleep(1000);
		ret = BatchExecutor.mainRun(new String[] { "-workflowDir=" + destDir.getAbsolutePath(),
				"-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String" });
		assertEquals(0, ret, "Non-zero return value");
		assertTrue(destDir.list().length > 5, "Not enough file in workflow directory after save");
		assertTrue(new File(destDir, WorkflowPersistor.WORKFLOW_FILE).isFile(), "No workflow in destination directory");
		assertTrue(workflowFile.lastModified() > timestamp, "Workflow not altered after in-place save");
	}

	/**
	 * Test if loading credentials works.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	@DisabledOnOs(value = OS.MAC,
	    disabledReason = "SQLite used in deprecated Database Writer does not support macos-aarch64")
	public void testLoadCredentials() throws Exception {
		File credentialsFlow = findInPlugin("/files/BatchExecutorTestflowCredentials.zip");

		File database = File.createTempFile("BatchExecutorTest", ".db");
		database.deleteOnExit();
		int ret = BatchExecutor.mainRun(new String[] { "-workflowFile=" + credentialsFlow.getAbsolutePath(), "-nosave",
				"-credential=database;thor;test", "-credential=nonExisting", "-credential=noUser;;password",
				"-credential=noPassword;user",
				"-workflow.variable=databaseUrl,jdbc:sqlite:" + database.getAbsolutePath() + ",String" });
		assertEquals(0, ret, "Non-zero return value");
		assertTrue(database.length() > 1000, "Database file not written");

		// We cannot really test if credentials are indeed set without a much more
		// complex setup. The SQLite driver
		// does not need credentials and currently there is no node besides database
		// nodes that uses credentials.
	}

	private int countWrittenLines(final File outputFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(outputFile));
		int count = 0;
		while (in.readLine() != null) {
			count++;
		}
		in.close();
		return count;
	}

	private static File findInPlugin(final String name) throws IOException {
		Bundle thisBundle = FrameworkUtil.getBundle(BatchExecutorTestcase.class);
		URL url = FileLocator.find(thisBundle, new Path(name), null);
		if (url == null) {
			throw new FileNotFoundException(thisBundle.getLocation() + name);
		}
		return new File(FileLocator.toFileURL(url).getPath());
	}
}