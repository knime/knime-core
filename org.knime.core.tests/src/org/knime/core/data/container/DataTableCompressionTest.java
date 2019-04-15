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
 *   Jan 16, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.container;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DefaultTableStoreFormat.CompressionFormat;
import org.knime.core.data.container.DefaultTableStoreFormat.DefaultTableStoreSettings;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.Pair;

import junit.framework.TestCase;

/**
 * Class testing that the different compression formats run properly and that changes in the
 * {@link DataContainerSettings} change the DataContainer writer/reader behavior.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class DataTableCompressionTest extends TestCase {

    /** Number of generated rows. */
    private static final int ROW_COUNT = 500;

    /**
     * Ensures that the different compressions run properly, by testing that the table is written/read using the proper
     * compression format. The test include (i) writing, (ii) reading, and (iii) finally checking that the written table
     * matches the input table.
     *
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("static-method")
    @Test
    public void testCompressions()
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        // initial settings
        final DataContainerSettings settings =
            DataContainerSettings.getDefault().withMaxCellsInMemory(0).withInitializedDomain(false);
        // create the data
        final Pair<DataTableSpec, DataRow[]> data = createData(ROW_COUNT);

        for (final CompressionFormat cFormat : CompressionFormat.values()) {

            // store the data to a table that writes its content to a compressed file
            DataContainer cont = new DataContainer(data.getFirst(),
                settings.withBufferSettings(BufferSettings.getDefault().withOutputFormat(
                    new DefaultTableStoreFormat(DefaultTableStoreSettings.getDefault().withCompression(cFormat)))));

            // write the data
            writeData(data.getSecond(), cont);

            // get the buffer and close the data container
            final Buffer b = cont.getBuffer();
            cont.close();

            // check that the proper compressor has been used to write the file
            testRead(b, cFormat);

            // check that file extension is adequate
            Assert.assertThat("Compressed file has wrong file extension:",
                b.getBinFile().getName().substring(b.getBinFile().getName().indexOf(".")),
                equalTo(cFormat.getFileExtension()));

            // check that the written table equals the input table
            read(b, data.getSecond());
        }
    }

    /**
     * Creates the data.
     *
     * @param rowCount number of rows
     * @return pair of data table spec and data rows
     */
    private static Pair<DataTableSpec, DataRow[]> createData(final int rowCount) {
        final DataTableSpec spec = new DataTableSpec(new DataColumnSpecCreator("int", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("string", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("long", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("double", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("boolean", BooleanCell.TYPE).createSpec());

        final DataRow[] rows =
            IntStream.range(0, rowCount)
                .mapToObj(i -> new DefaultRow(RowKey.createRowKey((long)i), new IntCell(i),
                    new StringCell(Integer.toString(i)), new LongCell(i), new DoubleCell(i + .5),
                    i % 2 == 1 ? BooleanCell.TRUE : BooleanCell.FALSE))
                .toArray(DataRow[]::new);

        return new Pair<DataTableSpec, DataRow[]>(spec, rows);
    }

    /**
     * Writes the data to the data container.
     *
     * @param dataRows the rows to write
     * @param cont the container to write to
     */
    private static void writeData(final DataRow[] dataRows, final DataContainer cont) {
        for (final DataRow r : dataRows) {
            cont.addRowToTable(r);
        }
    }

    /**
     * Reads the compressed file and ensures that the proper compressor has been used.
     *
     * @param b the Buffer
     * @param cFormat the compression format
     * @throws Error - If the fail does not exists or has been written using the wrong compressor
     */
    private static void testRead(final Buffer b, final CompressionFormat cFormat) {
        try (InputStream inStream = cFormat.getInputStream(b.getBinFile())) {
            while (inStream.read() != -1) {
            }
        } catch (IOException e) {
            throw new Error(
                "Either the compressed file was not written or it has been written using the wrong compressor.");
        }
    }

    /**
     * Reads the table from the compressed file and verifies that is matches the table we have written to exactly this
     * file.
     *
     * @param b the Buffer
     * @param rows the original data
     */
    private static void read(final Buffer b, final DataRow[] rows) {
        // read the data
        try (final CloseableRowIterator rowIt = b.iterator()) {
            for (int i = 0; i < rows.length; i++) {
                Assert.assertThat("Iterator has rows", rowIt.hasNext(), is(true));
                DataRow refRow = rows[i];
                DataRow dataRow = rowIt.next();
                RowKey refKey = refRow.getKey();
                RowKey dataKey = dataRow.getKey();
                for (int j = 0; j < refRow.getNumCells(); j++) {
                    DataCell refCell = refRow.getCell(j);
                    DataCell dataCell = dataRow.getCell(j);
                    if (refCell.isMissing()) {
                        Assert.assertThat("Cell " + j + " in Row " + i + " is missing", dataCell.isMissing(), is(true));
                        Assert.assertThat("Error message of missing cell " + j + " in Row " + i,
                            ((MissingValue)refCell).getError(), equalTo(((MissingValue)dataCell).getError()));
                    } else {
                        if (!(refCell instanceof BooleanCell)) {
                            Assert.assertThat("Cell " + j + " in Row " + i, dataCell, not(sameInstance(refCell)));
                        }
                        Assert.assertThat("Cell " + j + " in Row " + i, dataCell, equalTo(refCell));
                    }
                }
                Assert.assertThat("Row key in row " + i, dataKey, equalTo(refKey));
            }
            Assert.assertThat("Iterator with more than " + rows.length + " rows", rowIt.hasNext(), is(false));
        }
    }

}
