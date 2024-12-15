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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;
import org.knime.core.util.PathFilters;
import org.knime.core.util.PathUtils;

/**
 * AP-19046: Table Reader has problems with BlobDataCells when reading multiple tables
 * 
 * Workflow contains a "Create Blob Cells" node that is then split into two files, whereby one file will contain some 
 * duplicates. Both files are read back into KNIME, and blobs are validated. Also, the number of files will be checked.
 * 
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP19046_BlobDuplicatesInNewTableReader extends WorkflowTestCase { // NOSONAR

    private NodeID m_createBlob_707;
    private NodeID m_verifyBlob_717;
    private NodeID m_tableReader_715;

    @TempDir
    public File m_tempFolder;
	private File m_workflowDir;

    @BeforeEach
    public void setUp() throws Exception {
    	// will write to the folder, hence copy first
    	m_workflowDir = new File(m_tempFolder, getClass().getSimpleName());
    	FileUtils.copyDirectory(getDefaultWorkflowDirectory(), m_workflowDir);
    	initWorkflowFromTemp();
    }

    private WorkflowLoadResult initWorkflowFromTemp() throws Exception {
    	WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
    	setManager(loadResult.getWorkflowManager());
    	NodeID baseID = getManager().getID();
    	m_createBlob_707 = baseID.createChild(707);
    	m_verifyBlob_717 = baseID.createChild(717);
    	m_tableReader_715 = baseID.createChild(715);
    	return loadResult;
    }

    /** Find component, save into temp folder, instantiate fresh and check IDs. */
    @Test
    public void test_execute_save_count_files() throws Exception {
    	checkState(m_createBlob_707, CONFIGURED);
		assertThat("Number of files in temp-folder prior execution",
				countFilesInTempFolder(), is(1L)); // there is a bogus readme file in it
    	executeAllAndWait();
    	checkStateOfMany(EXECUTED, m_createBlob_707, m_verifyBlob_717);
    	assertThat("Number of files in temp-folder after execution",
    			countFilesInTempFolder(), is(3L)); // 
		assertThat("Number of blobs in file-1.table",
				countBlobFilesInTableFile(new File(m_workflowDir, "data/temp-folder"), "file-1.table"), is(5L));
		assertThat("Number of blobs in file-2.table",
				countBlobFilesInTableFile(new File(m_workflowDir, "data/temp-folder"), "file-2.table"), is(5L));
		getManager().save(m_workflowDir, new ExecutionMonitor(), true);
		assertThat("Number of blob files in Table Reader's 'data.zip' file",
				countBlobFilesInTableFile(
						getManager().getNodeContainer(m_tableReader_715).getNodeContainerDirectory().getFile(),
						"data.zip"),
				is(10L));
	}

	/**
	 * The number of files in <workflow>/data/temp-folder. Contains after execution
	 * these files: file-1.table, file-2.table.
	 */
	private long countFilesInTempFolder() throws IOException {
		return PathUtils.countFiles(m_workflowDir.toPath().resolve("data/temp-folder"), PathFilters.acceptAll);
	}

	/**
	 * After execution, look at the folder of the "Table Reader" node, find it's
	 * 'data.zip', count the blob files in it.
	 */
	private long countBlobFilesInTableFile(final File containingFolder, final String fileName) throws IOException {
		var iterator = FileUtils.iterateFiles(containingFolder, new NameFileFilter(fileName),
				TrueFileFilter.TRUE);
		try {
			assertTrue(iterator.hasNext(),
					String.format("Folder \"%s\" contains no file '%s'", containingFolder, fileName));
			File dataZip = iterator.next();
			try (ZipFile zipFile = new ZipFile(dataZip)) {
				return zipFile.stream().filter(entry -> !entry.isDirectory())
						.filter(zipEntry -> zipEntry.getName().contains("blobs")).count();
			}
		} finally {
			while (iterator.hasNext()) { // FileUtils.iterateFiles wants the stream to be consumed
				iterator.next();
			}
		}
	}

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteRecursively(m_workflowDir);
    }

}