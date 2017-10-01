/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   Jan 24, 2016 (wiswedel): created
 */
package org.knime.orc.tableformat;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.CompressionKind;
import org.apache.orc.OrcFile;
import org.apache.orc.Reader;
import org.apache.orc.RecordReader;
import org.apache.orc.TypeDescription;
import org.apache.orc.Writer;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.storage.AbstractTableStoreReader.TableStoreCloseableRowIterator;
import org.knime.core.data.def.DefaultCellIterator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.orc.tableformat.OrcKNIMEType.StringOrcKNIMEType;

/**
 *
 * @author wiswedel
 */
public final class OrcKNIMEUtil {

    public static final class OrcWriterBuilder {

        private final Map<String, OrcKNIMEType> m_fieldMap;
        private final boolean m_hasRowKey;
        private final File m_file;

        /**
         *
         */
        public OrcWriterBuilder(final File file, final boolean hasRowKey) {
            m_file = Objects.requireNonNull(file);
            m_hasRowKey = hasRowKey;
            m_fieldMap = new LinkedHashMap<>();
            if (m_hasRowKey) {
                addField("row-key", OrcKNIMEType.STRING);
            }
        }

        /**
         *
         */
        public OrcWriterBuilder addField(final String name, final OrcKNIMEType type) {
            if (m_fieldMap.putIfAbsent(Objects.requireNonNull(name), Objects.requireNonNull(type)) != null) {
                throw new IllegalArgumentException(String.format("Name \"%s\" already assigned", name));
            }
            return this;
        }

        public OrcKNIMEWriter create() throws IOException {
            TypeDescription schema = TypeDescription.createStruct();
            for (Map.Entry<String, OrcKNIMEType> colEntry : m_fieldMap.entrySet()) {
                schema.addField(colEntry.getKey(), colEntry.getValue().getTypeDescription());
            }
            Configuration conf = new Configuration();
            Writer writer = OrcFile.createWriter(new Path(m_file.getAbsolutePath()),
                OrcFile.writerOptions(conf)
                  .setSchema(schema)
                  .stripeSize(100000)
                  .bufferSize(10000)
                  .compress(CompressionKind.SNAPPY)
                  .version(OrcFile.Version.V_0_12));
            VectorizedRowBatch rowBatch = schema.createRowBatch();
            return new OrcKNIMEWriter(writer, m_hasRowKey, rowBatch, getOrcKNIMETypes());
        }

        /**
         * @return
         */
        private OrcKNIMEType[] getOrcKNIMETypes() {
            return m_fieldMap.values().toArray(new OrcKNIMEType[m_fieldMap.size()]);
        }

        public OrcRowIterator createRowIterator() throws IOException {
            Reader reader = OrcFile.createReader(new Path(m_file.getAbsolutePath()),
                OrcFile.readerOptions(new Configuration()));
            return new OrcRowIterator(reader, m_hasRowKey, getOrcKNIMETypes());
        }

        public void writeSettings(final NodeSettingsWO settings) {
            NodeSettingsWO columnsSettings = settings.addNodeSettings("columns");
            for (Map.Entry<String, OrcKNIMEType> entry : m_fieldMap.entrySet()) {
                NodeSettingsWO colSetting = columnsSettings.addNodeSettings(entry.getKey());
                entry.getValue().save(colSetting);
            }
        }

        public OrcWriterBuilder fromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            NodeSettingsRO columnsSettings = settings.getNodeSettings("columns");
            for (String colString : columnsSettings.keySet()) {
                NodeSettingsRO colSetting = columnsSettings.getNodeSettings(colString);
                OrcKNIMEType t = OrcKNIMEType.load(colSetting);
                m_fieldMap.put(colString, t);
            }
            return this;
        }

    }

    public static final class OrcKNIMEWriter implements AutoCloseable {
        private final Writer m_writer;
        private final OrcKNIMEType[] m_columnTypes;
        private final VectorizedRowBatch m_rowBatch;
        private final boolean m_hasRowKey;
        private final int m_batchSize;

        OrcKNIMEWriter(final Writer writer, final boolean hasRowKey, final VectorizedRowBatch rowBatch,
            final OrcKNIMEType[] columnTypes) {
            m_writer = writer;
            m_hasRowKey = hasRowKey;
            m_columnTypes = columnTypes;
            m_rowBatch = rowBatch;
            m_batchSize = rowBatch.getMaxSize();
        }

        public void addRow(final DataRow row) throws IOException {
            final int rowInBatch = m_rowBatch.size;
            int c = 0;
            if (m_hasRowKey) {
                ((StringOrcKNIMEType)m_columnTypes[0]).writeValue(
                    m_rowBatch.cols[0], rowInBatch, row.getKey().getString());
                c += 1;
            }
            for (; c < m_rowBatch.numCols; c++) {
                DataCell cell = row.getCell(m_hasRowKey ? c - 1 : c);
                m_columnTypes[c].writeValue(m_rowBatch.cols[c], rowInBatch, cell);
            }
            if (rowInBatch == m_batchSize - 1) {
                m_writer.addRowBatch(m_rowBatch);
                m_rowBatch.reset();
            }
        }

        @Override
        public void close() throws IOException {
            if (m_rowBatch.size != 0) {
                m_writer.addRowBatch(m_rowBatch);
                m_rowBatch.reset();
            }
            m_writer.close();
        }

    }

    static final class OrcRowIterator extends TableStoreCloseableRowIterator {

        private static final NodeLogger LOGGER = NodeLogger.getLogger(OrcRowIterator.class);

        private final OrcKNIMEType[] m_columnTypes;
        private final VectorizedRowBatch m_rowBatch;
        private final boolean m_hasRowKey;
        private final RecordReader m_rows;

        private int m_rowInBatch;
        private boolean m_isClosed;

        /**
         * @param reader
         * @throws IOException
         */
        OrcRowIterator(final Reader reader, final boolean hasRowKey, final OrcKNIMEType[] columnTypes) throws IOException {
            m_hasRowKey = hasRowKey;
            m_columnTypes = columnTypes;
            m_rows = reader.rows();
            m_rowBatch = reader.getSchema().createRowBatch();
            internalNext();
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return m_rowInBatch >= 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            OrcRow orcRow = new OrcRow(this, m_rowInBatch);
            try {
                internalNext();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return orcRow;
        }

        private void internalNext() throws IOException {
            if (m_rows.nextBatch(m_rowBatch)) {
                m_rowInBatch = 0;
            } else {
                m_rowInBatch = -1;
            }
        }

        @Override
        public boolean performClose() throws IOException {
            if (m_isClosed) {
                return false;
            }
            m_rows.close();
            m_rowInBatch = -1;
            m_isClosed = true;
            return true;
        }
    }


    static final class OrcRow implements DataRow {

        private static final RowKey NO_KEY = new RowKey("no-key");

        private final OrcRowIterator m_iterator;
        private final int m_rowInBatch;
        /**
         * @param iterator
         * @param rowInBatch
         */
        OrcRow(final OrcRowIterator iterator, final int rowInBatch) {
            m_iterator = iterator;
            m_rowInBatch = rowInBatch;
        }

        @Override
        public RowKey getKey() {
            if (m_iterator.m_hasRowKey) {
                String str = ((StringOrcKNIMEType)m_iterator.m_columnTypes[0]).readString(
                    m_iterator.m_rowBatch.cols[0], m_rowInBatch);
                return new RowKey(str);
            } else {
                return NO_KEY;
            }
        }

        @Override
        public DataCell getCell(final int index) {
            // TODO do not eval lazy as m_rowBatch may have been updated
            int c = index + (m_iterator.m_hasRowKey ? 1 : 0);
            return m_iterator.m_columnTypes[c].readValue(m_iterator.m_rowBatch.cols[c], m_rowInBatch);
        }

        @Override
        public int getNumCells() {
            return m_iterator.m_columnTypes.length - (m_iterator.m_hasRowKey ? 1 : 0);
        }

        @Override
        public Iterator<DataCell> iterator() {
            return new DefaultCellIterator(this);
        }


    }


}
