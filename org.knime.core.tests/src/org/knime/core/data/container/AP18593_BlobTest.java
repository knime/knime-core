/*
 * ------------------------------------------------------------------------
 *
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
 *   3 Apr 2020 ("Marc Bux, KNIME GmbH, Berlin, Germany"): created
 */
package org.knime.core.data.container;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowTableBackendSettingsTestUtils;

/**
 * Test case(s) around blob handling in {@link Buffer} and {@link DataContainer}.
 *
 * The problem only existed in the old table backend (where blobs were real files and not inlined).
 *
 * https://knime-com.atlassian.net/browse/AP-18593
 */
final class AP18593_BlobTest {

    /** Used to copy blobs from and into a workflow. */
    private WorkflowManager m_manager;
    private ExecutionContext m_execContext;

    private static DataTableSpec TEST_TABLE_SPEC =
        new DataTableSpec(new DataColumnSpecCreator("xml", XMLCell.TYPE).createSpec());

    /** Creates a single-node workflow into which a table with blobs is copied. */
    @BeforeEach
    void initWorkflowManager() throws Exception {
        m_manager = WorkflowManager.ROOT.createAndAddProject(
            getClass().getSimpleName(), new WorkflowCreationHelper());
        // at some point the old table backend will be deprecated and removed; this is when this test case is obsolete
        WorkflowTableBackendSettingsTestUtils.forceOldTableBackendOnto(m_manager);
        var nodeID = m_manager.createAndAddNode(new PortObjectInNodeFactory());
        var nodeContainer = m_manager.getNodeContainer(nodeID, NativeNodeContainer.class, true);
        m_execContext = nodeContainer.createExecutionContext();
        NodeContext.pushContext(nodeContainer);
    }

    @AfterEach
    void destroyWorkflowManager() {
        NodeContext.removeLastContext();
        m_execContext = null;
        m_manager.getParent().removeProject(m_manager.getID());
        m_manager = null;
    }


    /** Creates a simple DataTable via {@link DataContainer}. These tables will have a bufferID = -1. Blobs in these
     * tables need to be copied when the row is added into a workflow-aware table.
     *
     * This test also succeeded without the fix of AP-18593.
     */
    @Test
    void testCopyFromStandaloneToBufferedDataTableByRow() throws Exception {
        var notInWorkflowTable = createNonWorkflowTableWithBlobs(new DataContainer(TEST_TABLE_SPEC));
        var inWorkflowContainer = m_execContext.createDataContainer(TEST_TABLE_SPEC);
        for (DataRow row : notInWorkflowTable) {
            inWorkflowContainer.addRowToTable(row);
        }
        inWorkflowContainer.close();
        try (var bdTable = inWorkflowContainer.getBufferedTable()) {
            assertThat("Concreate container table", bdTable, is(instanceOf(BufferedContainerTable.class)));
            var bufferID = ((BufferedContainerTable)bdTable).getBuffer().getBufferID();
            assertThat("Container table in workflow have IDs > 0", bufferID, is(not(-1)));
            for (DataRow row : bdTable) {
                assertBlobAddress(row, bufferID);
            }
        }
    }

    /** Copies a row from a detached table cell by cell into a {@link BufferedDataTable}. This did not succeed without
     * the fix in AP-18593.
     */
    @Test
    void testCopyFromStandaloneToBufferedDataTableByCell() throws Exception {
        var notInWorkflowTable = createNonWorkflowTableWithBlobs(new DataContainer(TEST_TABLE_SPEC));
        var inWorkflowContainer = m_execContext.createDataContainer(TEST_TABLE_SPEC);
        for (DataRow row : notInWorkflowTable) {
            DataRow copy = new DefaultRow(row.getKey(), row);
            inWorkflowContainer.addRowToTable(copy);
        }
        inWorkflowContainer.close();
        try (var bdTable = inWorkflowContainer.getBufferedTable()) {
            assertThat("Container table impl", bdTable, is(instanceOf(BufferedContainerTable.class)));
            var bufferID = ((BufferedContainerTable)bdTable).getBuffer().getBufferID();
            assertThat("Container table in workflow have IDs > 0", bufferID, is(not(-1)));
            for (DataRow row : bdTable) {
                assertBlobAddress(row, bufferID);
            }
        }
    }

    private static void assertBlobAddress(final DataRow row, final int expectedOwnerID) throws Exception {
        assertThat("Row implementation", row, is(instanceOf(BlobSupportDataRow.class)));
        var cell = ((BlobSupportDataRow)row).getRawCell(0);
        assertThat("Cell implementation", cell, is(instanceOf(BlobWrapperDataCell.class)));
        var blobWrapperAddress = ((BlobWrapperDataCell)cell).getAddress();
        assertThat("Owner buffer ID in wrapper", blobWrapperAddress.getBufferID(), is(expectedOwnerID));
        var blobCell = ((BlobWrapperDataCell)cell).getCell();
        assertThat("xml cell", blobCell, is(instanceOf(BlobDataCell.class)));
        assertThat("Owner buffer ID of blob cell",
            ((BlobDataCell)blobCell).getBlobAddress().getBufferID(), is(expectedOwnerID));
        DataCell refCell = createXmlCell();
        assertThat("cell reference", blobCell, is(not(sameInstance(refCell))));
        assertThat("xml cell content", blobCell, is(refCell));
    }

    private static DataTable createNonWorkflowTableWithBlobs(final DataContainer container) throws Exception {
        var xmlS = createXmlCell();
        DataCell blobCell = XMLCellFactory.create(xmlS.toString());
        MatcherAssert.assertThat("Class of test DataCell", blobCell, is(instanceOf((BlobDataCell.class))));
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(0L), blobCell));
        container.close();
        return container.getTable();
    }

    private static DataCell createXmlCell() throws Exception {
        var charArray = new char[XMLCellFactory.DEF_MIN_BLOB_SIZE_IN_BYTES];
        Arrays.fill(charArray, 'a');
        String xmlString = new StringBuilder("<foo>").append(new String(charArray)).append("</foo>").toString();
        return XMLCellFactory.create(xmlString);
    }

}
