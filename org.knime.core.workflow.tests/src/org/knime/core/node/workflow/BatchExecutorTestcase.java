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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
    @BeforeClass
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
    @Before
    public void beforeEachTest() throws IOException {
        csvOut.delete();
    }

    /**
     * Executed after each test, checks that there are no open workflows dangling around.
     */
    @After
    public void checkDanglingWorkflows() {
        Collection<NodeContainer> openWorkflows = WorkflowManager.ROOT.getNodeContainers().stream()
            .filter(nc -> !StringUtils.containsAny(nc.getName(), WorkflowTestCase.KNOWN_CHILD_WFM_NAME_SUBSTRINGS))
            .collect(Collectors.toList());
        assertTrue(openWorkflows.size() + " dangling workflow(s) detected: " + openWorkflows, openWorkflows.isEmpty());
    }

    /**
     * Test if (in)valid arguments are correctly recognized.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testArguments() throws Exception {
        assertEquals("Non-zero return value for 0 arguments", 0, BatchExecutor.mainRun(new String[0]));
        assertEquals("Wrong return value for unknown argument", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-XXXX123YYY"}));

        assertEquals("Wrong return value for correct masterkey", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-masterkey=key"}));
        assertEquals("Wrong return value for empty masterkey", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-masterkey="}));

        assertEquals("Wrong return value for invalid credential argument", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-credential"}));
        assertEquals("Wrong return value for invalid credential argument", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-credential="}));
        assertEquals("Wrong return value for missing credential name", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-credential=;aaa"}));

        assertEquals("Wrong return value for invalid preferences argument", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-preferences"}));
        assertEquals("Wrong return value for missing preference file", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-preferences=/aaa/bbb/ccc"}));
        assertEquals("Wrong return value for invalid preferences file", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-preferences=" + File.createTempFile("BatchExecutorTest", "prefs")}));

        assertEquals("Wrong return value for invalid workflowFile argument", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflowFile"}));
        assertEquals("Wrong return value for non-existing workflowFile", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflowFile=/aaa/bbb/ddd"}));
        assertEquals("Wrong return value for directory used as workflowFile", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + System.getProperty("user.home")}));

        assertEquals("Wrong return value for invalid workflowFile argument", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflowDir"}));
        assertEquals("Wrong return value for non-existing workflowDir", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflowDir=/aaa/bbb/ddd"}));
        assertEquals("Wrong return value for file used as workflowDir", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflowDir=" + standardTestWorkflowZip}));
        assertEquals("Wrong return value for non-workflow directory", BatchExecutor.EXIT_ERR_LOAD,
            BatchExecutor.mainRun(new String[]{"-workflowDir=" + System.getProperty("java.home")}));
        assertEquals("Wrong return value for empty workflow directory", BatchExecutor.EXIT_ERR_LOAD,
            BatchExecutor.mainRun(new String[]{"-workflowDir=" + FileUtil.createTempDir("BatchExecutorTest")}));

        assertEquals("Wrong return value for invalid destFile argument", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-destFile"}));

        assertEquals("Wrong return value for invalid destDir argument", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-destDir"}));

        assertEquals("Wrong return value for invalid workflow.variable argument", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflow.variable"}));
        assertEquals("Wrong return value for invalid workflow variable", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflow.variable=name"}));
        assertEquals("Wrong return value for invalid workflow variable", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflow.variable=name,value"}));
        assertEquals("Wrong return value for invalid workflow variable type", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflow.variable=name,value,type"}));
        assertEquals("Wrong return value for wrong workflow variable type", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflow.variable=name,value,int"}));
        assertEquals("Wrong return value for wrong workflow variable type", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-workflow.variable=name,value,double"}));

        assertEquals("Wrong return value for invalid option argument", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-option"}));
        assertEquals("Wrong return value for invalid option", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-option=nodeId,name,value"}));

        assertEquals("Wrong return value for missing input", BatchExecutor.EXIT_ERR_PRESTART,
            BatchExecutor.mainRun(new String[]{"-nosave"}));
    }

    /**
     * Test if loading a workflow from a ZIP file works.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testLoadOnlyFromZip() throws Exception {
        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
                "-noexecute", "-nosave"});
        assertEquals("Non-zero return value", 0, ret);
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

        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowDir=" + tempDir.getAbsolutePath(), "-noexecute", "-nosave"});
        assertEquals("Non-zero return value", 0, ret);

        ret =
            BatchExecutor.mainRun(new String[]{"-workflowDir=" + tempDir.listFiles()[0].getAbsolutePath(),
                "-noexecute", "-nosave"});
        assertEquals("Non-zero return value", 0, ret);
    }

    /**
     * Test if workflows in a ZIP file can be executed.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testExecuteFromZip() throws Exception {
        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(), "-nosave",
                "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String"});
        assertEquals("Non-zero return value", 0, ret);
        assertEquals("Wrong number of lines in written CSV file", 1001, countWrittenLines(csvOut));
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

        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowDir=" + tempDir.getAbsolutePath(), "-nosave",
                "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String"});
        assertEquals("Non-zero return value", 0, ret);
        assertEquals("Wrong number of lines in written CSV file", 1001, countWrittenLines(csvOut));

        csvOut.delete();
        ret =
            BatchExecutor.mainRun(new String[]{"-workflowDir=" + tempDir.listFiles()[0].getAbsolutePath(), "-nosave",
                "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String"});
        assertEquals("Non-zero return value", 0, ret);
        assertEquals("Wrong number of lines in written CSV file", 1001, countWrittenLines(csvOut));
    }

    /**
     * Test if changing workflow variables from command line works.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testWorkflowVariables() throws Exception {
        final int maxRows = 100;
        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(), "-nosave",
                "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
                "-workflow.variable=maxRows," + maxRows + ",int"});
        assertEquals("Non-zero return value", 0, ret);
        assertEquals("Wrong number of lines in written CSV file", maxRows + 1, countWrittenLines(csvOut));

        ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(), "-nosave",
                "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
                "-workflow.variable=minimumValue,0.5,double"});
        assertEquals("Non-zero return value", 0, ret);
        assertEquals("Wrong number of lines in written CSV file", 439, countWrittenLines(csvOut));

        // unknown workflow variables only issue a warning but are ignored otherwise
        ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(), "-nosave",
                "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
                "-workflow.variable=unknown,0.5,double"});
        assertEquals("Non-zero return value", 0, ret);
        ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(), "-nosave",
                "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
                "-workflow.variable=unknown,0.5,double", "-workflow.variable=unknown2,nix,String"});
        assertEquals("Non-zero return value", 0, ret);
    }

    /**
     * Test if settings node options via command line works.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testSetOption() throws Exception {
        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(), "-nosave",
                "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
                "-option=0/5,rowFilter/RowRangeStart,1000,int"});
        assertEquals("Non-zero return value", 0, ret);
        assertEquals("Wrong number of lines in written CSV file", 1, countWrittenLines(csvOut));

        ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(), "-nosave",
                "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
                "-option=0/5,rowFilter/RowRangeStart,1000,int", "-option=0/5,rowFilter/longValue,1000,long",
                "-option=0/5,rowFilter/shortValue,1000,short", "-option=0/5,rowFilter/byteValue,100,byte",
                "-option=0/5,rowFilter/booleanValue,true,boolean", "-option=0/5,rowFilter/charValue,X,char",
                "-option=0/5,rowFilter/floatValue,1000.1,float", "-option=0/5,rowFilter/floatValue,1000.1,double",
                "-option=0/5,rowFilter/StringValue,test,String",
                "-option=0/5,rowFilter/StringCellValue,1000.1,StringCell",
                "-option=0/5,rowFilter/DoubleCellValue,1000.1,DoubleCell",
                "-option=0/5,rowFilter/IntCellValue,1000,IntCell",
                "-option=0/5,rowFilter/LongCellValue,10000000000000000,LongCell",
                "-option=0/99,nonexistingNode,1000,IntCell",});
        assertEquals("Non-zero return value", 0, ret);

        ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(), "-nosave",
                "-reset", "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
                "-option=0/5,rowFilter/RowRangeStart,1000,unknownType"});
        assertEquals("Wrong option type not reported", BatchExecutor.EXIT_ERR_PRESTART, ret);
    }

    /**
     * Test if loading preferences works (no real test of they are actually applied).
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testPreferences() throws Exception {
        File prefs = findInPlugin("/files/batch.prefs");
        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
                "-noexecute", "-nosave", "-preferences=" + prefs.getAbsolutePath()});
        assertEquals("Loading preferences failed", 0, ret);
    }

    /**
     * Test if load errors are reported correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testFailOnLoadError() throws Exception {
        File testflowZip = findInPlugin("/files/BatchExecutorTestflowLoadError.zip");
        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + testflowZip.getAbsolutePath(), "-noexecute",
                "-nosave", "-failonloaderror"});
        assertEquals("Fail-on-load-error does not work", BatchExecutor.EXIT_ERR_LOAD, ret);

        testflowZip = findInPlugin("/files/BatchExecutorTestflowUnsupportedVersion.zip");
        ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + testflowZip.getAbsolutePath(), "-noexecute",
                "-nosave", "-failonloaderror"});
        assertEquals("Unsupported workflow version not handled correctly", BatchExecutor.EXIT_ERR_LOAD, ret);
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
                    ret.setValue(BatchExecutor.mainRun(new String[]{
                        "-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(), "-nosave",
                        "-workflow.variable=skipLongrunner,0,int"}));
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
        assertFalse("Workflow did not finish 5000ms after cancel request", t.isAlive());
        assertEquals("Wrong return value for canceled execution", BatchExecutor.EXIT_ERR_EXECUTION, ret.intValue());
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

        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
                "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
                "-destFile=" + destFile.getAbsolutePath()});
        assertEquals("Non-zero return value", 0, ret);
        assertTrue("ZIP file is too small", destFile.length() > 0.9 * standardTestWorkflowZip.length());

        // check if it is really a zip file and the workflow is named correctly
        ZipFile zf = new ZipFile(destFile);

        String expectedWorkflowName = destFile.getName().replaceAll("\\.zip$", "");
        assertThat("Workflow not named correctly", zf.getEntry(expectedWorkflowName), is(not(nullValue())));

        // save in place
        FileUtil.copy(standardTestWorkflowZip, destFile);
        long timestamp = destFile.lastModified();
        Thread.sleep(1000);
        ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + destFile.getAbsolutePath(),
                "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String"});
        assertEquals("Non-zero return value", 0, ret);
        assertTrue("ZIP file is too small", destFile.length() > 0.9 * standardTestWorkflowZip.length());
        // check if it is really a zip file
        new ZipFile(destFile);
        assertTrue("Workflow not altered after in-place save", destFile.lastModified() > timestamp);
    }

    /**
     * Test if saving the executed workflow to a directory works.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testSaveToDir() throws Exception {
        File destDir = FileUtil.createTempDir("BatchExecutorTest");

        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + standardTestWorkflowZip.getAbsolutePath(),
                "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String",
                "-destDir=" + destDir.getAbsolutePath()});
        assertEquals("Non-zero return value", 0, ret);
        assertTrue("Not enough file in destination directory", destDir.list().length > 5);
        assertTrue("No workflow in destination directory", new File(destDir, WorkflowPersistor.WORKFLOW_FILE).isFile());

        // save in place
        FileUtil.deleteRecursively(destDir);
        destDir.mkdir();
        FileUtil.unzip(standardTestWorkflowZip, destDir);
        destDir = destDir.listFiles()[0]; // workflow is in a subdirectory of the zip
        File workflowFile = new File(destDir, WorkflowPersistor.WORKFLOW_FILE);
        long timestamp = workflowFile.lastModified();
        Thread.sleep(1000);
        ret =
            BatchExecutor.mainRun(new String[]{"-workflowDir=" + destDir.getAbsolutePath(),
                "-workflow.variable=destinationFile," + csvOut.getAbsolutePath() + ",String"});
        assertEquals("Non-zero return value", 0, ret);
        assertTrue("Not enough file in workflow directory after save", destDir.list().length > 5);
        assertTrue("No workflow in destination directory", new File(destDir, WorkflowPersistor.WORKFLOW_FILE).isFile());
        assertTrue("Workflow not altered after in-place save", workflowFile.lastModified() > timestamp);
    }

    /**
     * Test if loading credentials works.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testLoadCredentials() throws Exception {
        File credentialsFlow = findInPlugin("/files/BatchExecutorTestflowCredentials.zip");

        File database = File.createTempFile("BatchExecutorTest", ".db");
        database.deleteOnExit();
        int ret =
            BatchExecutor.mainRun(new String[]{"-workflowFile=" + credentialsFlow.getAbsolutePath(), "-nosave",
                "-credential=database;thor;test", "-credential=nonExisting", "-credential=noUser;;password",
                "-credential=noPassword;user",
                "-workflow.variable=databaseUrl,jdbc:sqlite:" + database.getAbsolutePath() + ",String"});
        assertEquals("Non-zero return value", 0, ret);
        assertTrue("Database file not written", database.length() > 1000);

        // We cannot really test if credentials are indeed set without a much more complex setup. The SQLite driver
        // does not need credentials and currently there is no node besides database nodes that uses credentials.
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
