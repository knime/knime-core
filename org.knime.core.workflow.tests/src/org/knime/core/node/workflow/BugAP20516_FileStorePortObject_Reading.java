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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.io.File;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests https://knime-com.atlassian.net/browse/AP-20516: 
 * PortObjects backed by FileStore are leaving temp files behind.
 * 
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public class BugAP20516_FileStorePortObject_Reading extends WorkflowTestCase { // NOSONAR

    private static final Predicate<File> IS_TEMP_FILE = File::isFile;

    @TempDir
    public File m_tempFolder;

    /** Matches names of non-table files, e.g. 
     * <pre>/tmp/knime_bugAP20516_File_17805/fs-Count_5-6-58195/000/000/0_0_first.bin</pre>
     */
    private static final IOFileFilter FS_FILE_FILTER = new AbstractFileFilter() {
    	@Override
    	public boolean accept(File dir, String name) {
    		return !name.startsWith("knime_container");
    	}
    };

    private NodeID m_createFS_4;
    private NodeID m_modelWriter_5;

	@BeforeEach
	public void setUp() throws Exception {
		final var wkfDir = new File(m_tempFolder, getDefaultWorkflowDirectory().getName());
		wkfDir.mkdir();
		FileUtils.copyDirectory(getDefaultWorkflowDirectory(), wkfDir);

		final NodeID id = loadAndSetWorkflow(wkfDir);
		m_createFS_4 = id.createChild(4);
		m_modelWriter_5 = id.createChild(5);

		final WorkflowManager manager = getManager();
		final File tempLocation = getWorkflowTempLocation(manager);
		assertEquals(0, countFilesInDirectory(tempLocation, IS_TEMP_FILE), "File stores in temp before execution");
	}

	@Test
	public void testSelectiveNoLoopExecute() throws Exception {
		final WorkflowManager manager = getManager();
		final File tempLocation = getWorkflowTempLocation(manager);
		executeAndWait(m_createFS_4);
		assertThat("Number temp files after full execute", 
				FileUtils.listFiles(tempLocation, FS_FILE_FILTER, DirectoryFileFilter.DIRECTORY),
				Matchers.hasSize(/* from source node */ 2));
		executeAndWait(m_modelWriter_5);
		checkStateOfMany(EXECUTED, m_createFS_4, m_modelWriter_5);
		manager.getParent().resetAndConfigureNode(getManager().getID());
		// if this should fail, then consider adding some delay (or semaphore) to complete background delete threads
		assertThat("Temp folder is empty after reset (file count)",  
				countFilesInDirectory(tempLocation, IS_TEMP_FILE), is(0));
		executeAndWait(m_createFS_4);
		assertThat("Number temp files after full execute", 
				FileUtils.listFiles(tempLocation, FS_FILE_FILTER, DirectoryFileFilter.DIRECTORY),
				Matchers.hasSize(/* from source node */ 2));
	}

	@Test
	public void testExecuteAndResetAll() throws Exception {
		final WorkflowManager manager = getManager();
		final File tempLocation = getWorkflowTempLocation(manager);
		assertEquals(0, countFilesInDirectory(tempLocation, IS_TEMP_FILE), "File stores in temp before execution");
		executeAllAndWait();
		checkState(manager, EXECUTED);
		// when looking at the workflow you would think there are 22 file stores in the fully executed workflow
		// - 2 from the original create node (single port object having 2 file stores)
		// - 10 loops with 2 file stores each in the reader
		// ... except that the reader in the loop detects that it reads the same file 
		//     (some UUID of the NotInWorkflowFileStoreHandler, so it will re-use the file)
		// we could argue this is wrong behavior but it's also very convenient. Reading different models in each 
		// iteration will cause different file stores to be created/kept
		final Collection<File> fileList = 
				FileUtils.listFiles(tempLocation, FS_FILE_FILTER, DirectoryFileFilter.DIRECTORY);
		assertThat(
				"Number temp files after full execute:\n"
						+ fileList.stream().map(File::getAbsolutePath).collect(Collectors.joining("\n")),
				fileList,
				Matchers.hasSize(/* from source node */ 2 + /* 10 loop iterations a 2 files but duplicates */ 2));
		manager.getParent().resetAndConfigureNode(getManager().getID());
		Thread.sleep(100); // give the file-in-background-deletion some time to do its work
		assertThat("Temp folder is empty after reset", countFilesInDirectory(tempLocation, IS_TEMP_FILE), is(0));
	}

	/**
	 * @param manager
	 * @return
	 */
	private static File getWorkflowTempLocation(final WorkflowManager manager) {
		return manager.getContextV2().getExecutorInfo().getTempFolder().toFile();
	}

}