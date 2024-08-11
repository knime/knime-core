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
 *   Aug 4, 2024 (wiswedel): created
 */
package org.knime.core.data.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowWrite;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.data.v2.value.ValueInterfaces.DoubleWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.IntWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringWriteValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.testing.core.ExecutionContextExtension;
import org.knime.testing.data.blob.LargeBlobCell;
import org.knime.testing.data.filestore.LargeFile;
import org.knime.testing.data.filestore.LargeFileStoreCell;

/**
 * Some table copying tests added as part of AP-23029. Used for tables backed by the row backend and filled/copied with
 * the columnar backend API. Especially the {@link RowWrite#setFrom(RowRead)} method is tested here.
 *
 * @author wiswedel
 */
@ExtendWith({ExecutionContextExtension.class})
class BufferedRowContainerTest {

    private static final long MAX_CREATION_TIME_MS = 1000L;

    private static final int MAX_ROWS = 500;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(BufferedRowContainerTest.class);

    private static final DataContainerSettings DATA_CONTAINER_SETTINGS = DataContainerSettings.internalBuilder() //
        .withMaxCellsInMemory(0) //
        .withDomainUpdate(true) // should not have a significant effect, blobs and FStores ignored
        .withForceSequentialRowHandling(true) // more deterministic timings (and better debugging)
        .build();


    /** Utility table with a number of rows and columns of primitive types and blobs and filestores. */
    static BufferedDataTable createTable(final ExecutionContext context) throws IOException {
        // create table spec of four columns: int, double, string, blob (LargeBlobCell)
        final DataTableSpec spec = new DataTableSpecCreator().addColumns( //
            new DataColumnSpecCreator("Int", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Double", DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("String", StringCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Blob", LargeBlobCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Filestore", LargeFileStoreCell.TYPE).createSpec()) //
            .createSpec();
        final BufferedDataContainer container = context.createDataContainer(spec, DATA_CONTAINER_SETTINGS);
        final var now = System.currentTimeMillis();
        for (long i = 0; i < MAX_ROWS; i++) {
            if (System.currentTimeMillis() - now > MAX_CREATION_TIME_MS) {
                // should be much quicker but knowing the outdated macs that are used....
                LOGGER.infoWithFormat("Stopping creation of table after %d rows (timeout of 1s reached)", i);
                break;
            }
            final FileStore fileStore = context.createFileStore("file-" + i);
            container.addRowToTable(//
                new DefaultRow(RowKey.createRowKey(i), //
                    new IntCell((int)i), //
                    new DoubleCell(i + 0.5), //
                    new StringCell("Row " + i), //
                    new LargeBlobCell("Large Blob " + i, 200 * 1024), //
                    new LargeFileStoreCell(LargeFile.create(fileStore, i, false), i)));
        }
        container.close();
        BufferedDataTable table = container.getTable();
        LOGGER.infoWithFormat("Created table with %d rows in %d ms", table.size(), System.currentTimeMillis() - now);

        // need this code so that copying table finds blobs in the table repository. It fakes what usually is done
        // in a workflow when a node finishes. We don't have nodes here so we pretend this table is created by an
        // upstream node.
        BufferedDataContainerDelegate containerDelegate =
                (BufferedDataContainerDelegate)container.getDataContainerDelegate();
        containerDelegate.getDataRepository().addTable(table.getBufferedTableId(), containerDelegate.getTable());
        return table;
    }

    /** Copies the original table twice, once per 'setFrom', another time via cell-by-cell copying. Asserts
     * data is correct and that there is a noticeable runtime improvement. */
    @SuppressWarnings("static-method")
    @Test
    final void testAP23029_SetFromVsCopyCell(final ExecutionContext context) throws Exception {
        final BufferedDataTable origTable = createTable(context);

        /* Copy via 'setFrom' - this is supposed to be significantly faster than using cell-by-cell (new table API) */
        BufferedDataTable setFromCopyTable;
        final long nowSetFrom = System.currentTimeMillis();
        try (final RowContainer rowContainer = context.createRowContainer(origTable.getSpec(), DATA_CONTAINER_SETTINGS);
                final RowWriteCursor writeCursor = rowContainer.createCursor();
                final RowCursor readCursor = origTable.cursor()) {
            while (readCursor.canForward()) {
                writeCursor.forward().setFrom(readCursor.forward());
                context.checkCanceled();
            }
            setFromCopyTable = rowContainer.finish();
        }

        final long timeSetFrom = System.currentTimeMillis() - nowSetFrom;
        assertTrue(timeSetFrom < MAX_CREATION_TIME_MS / 10, String.format(
            "Copying via 'setFrom' took too long: %dms, max allowed: %dms", timeSetFrom, MAX_CREATION_TIME_MS / 10));

        /* Copy cell-by-cell - known to be not overly efficient in case of blobs */
        BufferedDataTable cellByCellCopyTable;
        final long nowCellByCell = System.currentTimeMillis();
        try (final RowContainer rowContainer = context.createRowContainer(origTable.getSpec(), DATA_CONTAINER_SETTINGS);
                final RowWriteCursor writeCursor = rowContainer.createCursor();
                final RowCursor readCursor = origTable.cursor()) {
            while (readCursor.canForward()) {
                RowRead read = readCursor.forward();
                RowWrite write = writeCursor.forward();
                write.setRowKey(read.getRowKey());
                for (int i = 0, end = readCursor.getNumColumns(); i < end; i++) {
                    // no missings in data
                    write.getWriteValue(i).setValue(read.getValue(i));
                }
                context.checkCanceled();
            }
            cellByCellCopyTable = rowContainer.finish();
        }

        assertEquals(origTable.size(), cellByCellCopyTable.size());
        final long timeCellByCell = System.currentTimeMillis() - nowCellByCell;
        // speedup is usually 30+ times for the data above (at time of writing this)
        // but this requires the VM to be "warm", no concurrent tests, speedy machines, etc
        assertTrue(timeSetFrom < 0.3
            * timeCellByCell,
            String.format(
                "Copying via 'setFrom' expected to run significantly faster than 'cell-by-cell' (%dms vs. %dms)",
                timeSetFrom, timeCellByCell));
        LOGGER.infoWithFormat("Copying via 'setFrom' took %dms, copying cell-by-cell took %dms (%.2f times faster)",
            timeSetFrom, timeCellByCell, (double)timeCellByCell / timeSetFrom);

        try (CloseableRowIterator origIterator = origTable.iterator(); //
                CloseableRowIterator setFromCopyIterator = setFromCopyTable.iterator(); //
                CloseableRowIterator cellByCellCopyIterator = cellByCellCopyTable.iterator()) {
            for (long rowIndex = 0; origIterator.hasNext(); rowIndex++) {
                final DataRow origRow = origIterator.next();
                final DataRow setFromCopyRow = setFromCopyIterator.next();
                final DataRow cellByCellCopyRow = cellByCellCopyIterator.next();
                assertNotSame(origRow, setFromCopyRow, "Row (set from) " + rowIndex);
                assertNotSame(origRow, cellByCellCopyRow, "Row (cell by cell) " + rowIndex);
                // iterate rowkey and all cells and call equals on them
                assertEquals(origRow.getKey(), setFromCopyRow.getKey(), "RowKey of row index " + rowIndex);
                assertEquals(origRow.getKey(), cellByCellCopyRow.getKey(), "RowKey of row index " + rowIndex);
                for (int cellIndex = 0; cellIndex < origRow.getNumCells(); cellIndex++) {
                    assertEquals(origRow.getCell(cellIndex), setFromCopyRow.getCell(cellIndex),
                        "Cell " + cellIndex + " of row " + rowIndex);
                    assertEquals(origRow.getCell(cellIndex), cellByCellCopyRow.getCell(cellIndex),
                        "Cell " + cellIndex + " of row " + rowIndex);
                }
            }
        }
    }

    /**
     * A test that creates a small table, copies it via `setFrom`, but modifies the RowRead after `setFrom` is called.
     */
    @Test
    void testSetFromWithModifiedRowRead(final ExecutionContext context) throws Exception {
        final DataTableSpec spec = new DataTableSpecCreator().addColumns( //
            new DataColumnSpecCreator("Int", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Double", DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("String", StringCell.TYPE).createSpec()) //
            .createSpec();
        final BufferedDataContainer container = context.createDataContainer(spec, DATA_CONTAINER_SETTINGS);
        container.addRowToTable(//
            new DefaultRow(new RowKey("unmodified"), //
                new IntCell(0), //
                new DoubleCell(0.0), //
                DataType.getMissingCell()));
        container.addRowToTable(//
            new DefaultRow(new RowKey("modified-key"), //
                new IntCell(1), //
                new DoubleCell(1.0), //
                new StringCell("Row 1")));
        container.addRowToTable(//
            new DefaultRow(new RowKey("modified-value"), //
                new IntCell(2), //
                new DoubleCell(2.0), //
                new StringCell("Row 2")));
        container.addRowToTable(//
            new DefaultRow(new RowKey("Row 3"), //
                new IntCell(3), //
                new DoubleCell(3.0), //
                new StringCell("Row 3")));
        container.close();
        final BufferedDataTable table = container.getTable();

        final BufferedDataTable copyTable;
        try ( //
            final RowContainer rowContainer = context.createRowContainer(table.getSpec(), DATA_CONTAINER_SETTINGS);
            final RowWriteCursor writeCursor = rowContainer.createCursor();
            final RowCursor readCursor = table.cursor()) {

            // row 0 is unmodified
            RowRead read = readCursor.forward();
            RowWrite write = writeCursor.forward();
            write.setFrom(read);

            // row 1 modifies row key after `setFrom` is called
            read = readCursor.forward();
            write = writeCursor.forward();
            write.setFrom(read);
            write.setRowKey(new RowKey("modified-key-after-copy"));

            // row 2 modifies cell value after `setFrom` is called
            read = readCursor.forward();
            write = writeCursor.forward();
            write.setFrom(read);
            ((IntWriteValue)write.getWriteValue(0)).setIntValue(-2);

            // row 3 sets one cell missing
            read = readCursor.forward();
            write = writeCursor.forward();
            write.setFrom(read);
            write.setMissing(1);

            copyTable = rowContainer.finish();
        }

        // for all three rows check their row keys and values; do not use a loop for neither the rows nor the columns
        try (CloseableRowIterator origIterator = table.iterator(); //
                CloseableRowIterator copyIterator = copyTable.iterator()) {
            DataRow origRow = origIterator.next();
            DataRow copyRow = copyIterator.next();
            assertEquals(origRow.getKey(), copyRow.getKey(), "RowKey of row 0");
            assertEquals(origRow.getCell(0), copyRow.getCell(0), "Cell 0 of row 0");
            assertEquals(origRow.getCell(1), copyRow.getCell(1), "Cell 1 of row 0");
            assertTrue(copyRow.getCell(2).isMissing(), "Cell 2 of row 0");

            origRow = origIterator.next();
            copyRow = copyIterator.next();
            assertEquals(new RowKey("modified-key-after-copy"), copyRow.getKey(), "RowKey of row 1");
            assertEquals(origRow.getCell(0), copyRow.getCell(0), "Cell 0 of row 1");
            assertEquals(origRow.getCell(1), copyRow.getCell(1), "Cell 1 of row 1");
            assertEquals(origRow.getCell(2), copyRow.getCell(2), "Cell 2 of row 1");

            origRow = origIterator.next();
            copyRow = copyIterator.next();
            assertEquals(origRow.getKey(), copyRow.getKey(), "RowKey of row 2");
            assertEquals(-2, ((IntValue)copyRow.getCell(0)).getIntValue(), "Cell 0 of row 2");
            assertEquals(origRow.getCell(1), copyRow.getCell(1), "Cell 1 of row 2");
            assertEquals(origRow.getCell(2), copyRow.getCell(2), "Cell 2 of row 2");

            origRow = origIterator.next();
            copyRow = copyIterator.next();
            assertEquals(origRow.getKey(), copyRow.getKey(), "RowKey of row 3");
            assertEquals(origRow.getCell(0), copyRow.getCell(0), "Cell 0 of row 3");
            assertTrue(copyRow.getCell(1).isMissing(), "Cell 1 of row 3");
            assertEquals(origRow.getCell(2), copyRow.getCell(2), "Cell 2 of row 3");
        }
    }

    /** Tests adding three rows 'manually' via WriteValue, including missing. */
    @Test
    void testAddRowsViaWriteValue(final ExecutionContext context) throws Exception {
        final DataTableSpec spec = new DataTableSpecCreator().addColumns( //
            new DataColumnSpecCreator("Int", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Double", DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("String", StringCell.TYPE).createSpec()) //
            .createSpec();
        BufferedDataTable table;
        try (RowContainer container = context.createRowContainer(spec, DATA_CONTAINER_SETTINGS);
                RowWriteCursor writeCursor = container.createCursor()) {
            RowWrite write = writeCursor.forward();
            write.setRowKey(new RowKey("Row 0"));
            ((IntWriteValue)write.getWriteValue(0)).setIntValue(0);
            ((DoubleWriteValue)write.getWriteValue(1)).setDoubleValue(0.0);
            ((StringWriteValue)write.getWriteValue(2)).setStringValue("Row 0");

            write = writeCursor.forward();
            write.setRowKey(new RowKey("Row 1"));
            write.setMissing(0);
            ((DoubleWriteValue)write.getWriteValue(1)).setDoubleValue(1.0);
            ((StringWriteValue)write.getWriteValue(2)).setStringValue("Row 1");

            table = container.finish();
        }

        // assert the table is as expected
        try (CloseableRowIterator it = table.iterator()) {
            DataRow row = it.next();
            assertEquals(new RowKey("Row 0"), row.getKey());
            assertEquals(new IntCell(0), row.getCell(0));
            assertEquals(new DoubleCell(0.0), row.getCell(1));
            assertEquals(new StringCell("Row 0"), row.getCell(2));

            row = it.next();
            assertEquals(new RowKey("Row 1"), row.getKey());
            assertTrue(row.getCell(0).isMissing());
            assertEquals(new DoubleCell(1.0), row.getCell(1));
            assertEquals(new StringCell("Row 1"), row.getCell(2));
        }
    }

    /**
     * Tests setting value via WriteValue and explicit setMissing calls, but then overwriting those values via
     * 'setFrom'.
     */
    @Test
    void testSetFromOverwritesWriteValue(final ExecutionContext context) throws Exception {
        final DataTableSpec spec = new DataTableSpecCreator().addColumns( //
            new DataColumnSpecCreator("Int", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Double", DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("String", StringCell.TYPE).createSpec()) //
            .createSpec();
        BufferedDataTable table;
        try (RowContainer container = context.createRowContainer(spec, DATA_CONTAINER_SETTINGS);
                RowWriteCursor writeCursor = container.createCursor()) {
            RowWrite write = writeCursor.forward();
            write.setRowKey(new RowKey("Row 0"));
            ((IntWriteValue)write.getWriteValue(0)).setIntValue(0);
            ((DoubleWriteValue)write.getWriteValue(1)).setDoubleValue(0.0);
            ((StringWriteValue)write.getWriteValue(2)).setStringValue("Row 0");

            write = writeCursor.forward();
            write.setRowKey(new RowKey("Row 1"));
            write.setMissing(0);
            ((DoubleWriteValue)write.getWriteValue(1)).setDoubleValue(1.0);
            ((StringWriteValue)write.getWriteValue(2)).setStringValue("Row 1");

            // now setFrom the first row to the second row
            write = writeCursor.forward();
            write.setRowKey(new RowKey("Row 2"));
            write.setMissing(0);
            ((DoubleWriteValue)write.getWriteValue(1)).setDoubleValue(2.0);
            ((StringWriteValue)write.getWriteValue(2)).setStringValue("Row 2");

            table = container.finish();
        }

        // create a copy table by copying row by row, do not use loops
        BufferedDataTable copyTable;
        try (RowContainer container = context.createRowContainer(spec, DATA_CONTAINER_SETTINGS);
                RowWriteCursor writeCursor = container.createCursor();
                RowCursor readCursor = table.cursor()) {
            RowRead read = readCursor.forward();
            RowWrite write = writeCursor.forward();
            ((IntWriteValue)write.getWriteValue(0)).setIntValue(-1); // expected to be overwritten
            write.setFrom(read);

            read = readCursor.forward();
            write = writeCursor.forward();
            write.setMissing(2); // expected to be overwritten
            write.setFrom(read);

            read = readCursor.forward();
            write = writeCursor.forward();
            write.setFrom(read);

            copyTable = container.finish();
        }

        // assert the table is as expected
        try (CloseableRowIterator it = copyTable.iterator()) {
            DataRow row = it.next();
            assertEquals(new RowKey("Row 0"), row.getKey());
            assertEquals(new IntCell(0), row.getCell(0));
            assertEquals(new DoubleCell(0.0), row.getCell(1));
            assertEquals(new StringCell("Row 0"), row.getCell(2));

            row = it.next();
            assertEquals(new RowKey("Row 1"), row.getKey());
            assertTrue(row.getCell(0).isMissing());
            assertEquals(new DoubleCell(1.0), row.getCell(1));
            assertEquals(new StringCell("Row 1"), row.getCell(2));

            row = it.next();
            assertEquals(new RowKey("Row 2"), row.getKey());
            assertTrue(row.getCell(0).isMissing());
            assertEquals(new DoubleCell(2.0), row.getCell(1));
            assertEquals(new StringCell("Row 2"), row.getCell(2));
        }
    }
}
