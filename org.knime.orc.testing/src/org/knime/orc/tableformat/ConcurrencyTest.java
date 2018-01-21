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
 * -------------------------------------------------------------------
 *
 * History
 *   Jan 5, 2018 (wiswedel): created
 */
package org.knime.orc.tableformat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;

import java.io.File;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeSettings;
import org.knime.orc.tableformat.OrcKNIMEUtil.OrcKNIMEWriter;
import org.knime.orc.tableformat.OrcKNIMEUtil.OrcRowIterator;
import org.knime.orc.tableformat.OrcKNIMEUtil.OrcWriterBuilder;

/**
 *
 * @author wiswedel
 */
public final class ConcurrencyTest {

    @Rule
    public TemporaryFolder m_tempFolder = new TemporaryFolder();

    /** Writes and reads many ORC files concurrently, data contains primitives with some being missing.
     * (Test developed while hunting a problem when running the workflow tests;
     * I thought it's worthwhile to keep the test)
     */
    @Test
    public void readWriteConcurrent() throws Exception {

        final DataRow[] rowsToWrite = IntStream.range(0, 2)
                .mapToObj(i -> new DefaultRow(RowKey.createRowKey((long)i), new IntCell(i),
                    i % 2 == 1 ? DataType.getMissingCell() : new StringCell(Integer.toString(i))))
                .toArray(DataRow[]::new);

        final int threadCount = 20;
        final ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        try {
            CompletionService<FileAndSettings> runWriterCompletionService = new ExecutorCompletionService<>(threadPool);
            for (int i = 0; i < threadCount; i++) {
                final int iFinal = i;
                runWriterCompletionService.submit(() -> {
                    File tempFile = new File(m_tempFolder.getRoot(), "file-" + iFinal + ".orc");
                    OrcWriterBuilder writeBuilder = new OrcWriterBuilder(tempFile, true);
                    writeBuilder.addField("intsAsInt", OrcKNIMEType.INT);
                    writeBuilder.addField("intsAsString", OrcKNIMEType.STRING);
                    OrcKNIMEWriter orcWriter = writeBuilder.create();
                    for (int j = 0; j < rowsToWrite.length; j++) {
                        orcWriter.addRow(rowsToWrite[j]);
                    }
                    orcWriter.close();
                    NodeSettings settings = new NodeSettings("temp");
                    writeBuilder.writeSettings(settings);
                    return new FileAndSettings(tempFile, settings);
                });
            }

            CompletionService<Void> runReaderCompletionService = new ExecutorCompletionService<>(threadPool);
            for (int i = 0; i < threadCount; i++) {
                Future<FileAndSettings> completeWriterFuture = runWriterCompletionService.take();
                FileAndSettings fileAndWriterSettings = completeWriterFuture.get();
                runReaderCompletionService.submit(() -> {
                    OrcWriterBuilder readBuilder = new OrcWriterBuilder(fileAndWriterSettings.m_file, true);
                    readBuilder.fromSettings(fileAndWriterSettings.m_settings);

                    OrcRowIterator rowIterator = readBuilder.createRowIterator();
                    for (int j = 0; j < rowsToWrite.length; j++) {
                        Assert.assertThat("Iterator has rows", rowIterator.hasNext(), is(true));
                        DataRow refRow = rowsToWrite[j];
                        DataRow dataRow = rowIterator.next();
                        for (int cellIndex = 0; cellIndex < refRow.getNumCells(); cellIndex++) {
                            DataCell refCell = refRow.getCell(cellIndex);
                            DataCell dataCell = dataRow.getCell(cellIndex);
                            if (refCell.isMissing()) {
                                Assert.assertThat("Cell " + cellIndex + " in Row " + j + " is missing",
                                    dataCell.isMissing(), is(true));
                            } else {
                                Assert.assertThat("Cell " + cellIndex + " in Row " + j,
                                    dataRow.getCell(cellIndex), not(sameInstance(refRow.getCell(cellIndex))));
                                Assert.assertThat("Cell " + cellIndex + " in Row " + j,
                                    dataRow.getCell(cellIndex), equalTo(refRow.getCell(cellIndex)));
                            }
                        }
                        Assert.assertThat("Row key in row " + j, dataRow.getKey(), equalTo(refRow.getKey()));
                    }
                    Assert.assertThat("Iterator with more " + rowsToWrite.length + " rows", rowIterator.hasNext(), is(false));
                    return null;
                });
            }

            for (int i = 0; i < threadCount; i++) {
                runReaderCompletionService.take().get(); // throws exception if...
            }
        } finally {
            threadPool.shutdown();
        }
    }

    static final class FileAndSettings {
        private final File m_file;
        private final NodeSettings m_settings;
        FileAndSettings(final File file, final NodeSettings settings) {
            m_file = file;
            m_settings = settings;
        }
    }

}
