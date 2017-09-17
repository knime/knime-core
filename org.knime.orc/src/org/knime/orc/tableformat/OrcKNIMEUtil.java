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

import static io.airlift.units.DataSize.Unit.MEGABYTE;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.io.orc.CompressionKind;
import org.apache.hadoop.hive.ql.io.orc.OrcFile;
import org.apache.hadoop.hive.ql.io.orc.Writer;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.joda.time.DateTimeZone;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.orc.tableformat.OrcKNIMEType.StringOrcKNIMEType;
import org.knime.orc.tableformat.OrcWriter.OrcField;
import org.knime.orc.tableformat.OrcWriter.OrcRow;
import org.knime.orc.tableformat.OrcWriter.OrcRowInspector;

import com.facebook.presto.orc.FileOrcDataSource;
import com.facebook.presto.orc.OrcPredicate;
import com.facebook.presto.orc.OrcReader;
import com.facebook.presto.orc.OrcRecordReader;
import com.facebook.presto.orc.memory.AggregatedMemoryContext;
import com.facebook.presto.orc.metadata.OrcMetadataReader;
import com.facebook.presto.spi.block.Block;
import com.facebook.presto.spi.type.Type;

import io.airlift.units.DataSize;

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

        public OrcWriterBuilder fromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            NodeSettingsRO columnsSettings = settings.getNodeSettings("columns");
            for (String colString : columnsSettings.keySet()) {
                NodeSettingsRO colSetting = columnsSettings.getNodeSettings(colString);
                OrcKNIMEType t = OrcKNIMEType.load(colSetting);
                m_fieldMap.put(colString, t);
            }
            return this;
        }


        public OrcKNIMEWriter create() throws IllegalArgumentException, IOException {
            Configuration conf = new Configuration();

            AtomicInteger index = new AtomicInteger(0);
            OrcField[] orcFields = m_fieldMap.entrySet().stream().map(e -> new OrcField(e.getKey(),
                e.getValue().getObjectInspectorFactory(), index.getAndIncrement())).toArray(OrcField[]::new);

            ObjectInspector ObjInspector = new OrcRowInspector(orcFields);
            return new OrcKNIMEWriter(OrcFile.createWriter(new Path(m_file.getAbsolutePath()),
                OrcFile.writerOptions(conf)
                .inspector(ObjInspector)
                .stripeSize(100000)
                .bufferSize(10000)
                .compress(CompressionKind.SNAPPY)
                .version(OrcFile.Version.V_0_12)));
        }

        public OrcKNIMEWritableRow createOrcKNIMEWritableRow() {
            return new OrcKNIMEWritableRow(m_fieldMap.values().toArray(new OrcKNIMEType[m_fieldMap.size()]), m_hasRowKey);
        }

        public PrestoDrivenRowIterator createRowIterator() throws IOException {
            FileOrcDataSource orcDataSource = new FileOrcDataSource(
                m_file, new DataSize(1, MEGABYTE), new DataSize(8, MEGABYTE), new DataSize(8, MEGABYTE));
            OrcReader orcReader = new OrcReader(orcDataSource, new OrcMetadataReader(),
                new DataSize(1, MEGABYTE), new DataSize(8, MEGABYTE));

            OrcKNIMEType[] orcKNIMETypes = m_fieldMap.values().toArray(new OrcKNIMEType[m_fieldMap.size()]);
            Map<Integer, Type> colMap = IntStream.range(0, orcKNIMETypes.length).boxed().collect(
                Collectors.toMap(i -> i, i -> orcKNIMETypes[i].getPrestoType()));
            OrcRecordReader recordReader = orcReader.createRecordReader(colMap,
                OrcPredicate.TRUE, DateTimeZone.getDefault(), new AggregatedMemoryContext());
            return new PrestoDrivenRowIterator(orcKNIMETypes, recordReader, m_hasRowKey);
        }

        public void writeSettings(final NodeSettingsWO settings) {
            NodeSettingsWO columnsSettings = settings.addNodeSettings("columns");
            for (Map.Entry<String, OrcKNIMEType> entry : m_fieldMap.entrySet()) {
                NodeSettingsWO colSetting = columnsSettings.addNodeSettings(entry.getKey());
                entry.getValue().save(colSetting);
            }
        }
    }

    public static final class OrcKNIMEWriter implements AutoCloseable {
        private final Writer m_writer;

        OrcKNIMEWriter(final Writer writer) {
            m_writer = writer;
        }

        public void addRow(final OrcKNIMEWritableRow row) throws IOException {
            m_writer.addRow(row.getOrcRow());
        }

        @Override
        public void close() throws IOException {
            m_writer.close();
        }

    }

    public static final class OrcKNIMEWritableRow {

        private final OrcKNIMEType[] m_knimeTypes;
        private final Field[] m_fields;
        private final OrcRow m_orcRow;
        private boolean m_hasRowKey;

        /**
         * @param hasRowKey TODO
         *
         */
        OrcKNIMEWritableRow(final OrcKNIMEType[] knimeTypes, final boolean hasRowKey) {
            m_knimeTypes = knimeTypes;
            m_hasRowKey = hasRowKey;
            m_fields = Arrays.stream(knimeTypes).map(e -> new Field(e)).toArray(Field[]::new);
            m_orcRow = new OrcRow(m_fields.length);
            IntStream.range(0, m_fields.length).forEach(i -> m_orcRow.setFieldValue(i, m_fields[i].getHadoopValue()));
        }

        public OrcKNIMEWritableRow set(final DataRow row) {
            int start = 0;
            int length = row.getNumCells();
            if (m_hasRowKey) {
                ((StringOrcKNIMEType)m_knimeTypes[0]).set(row.getKey().getString(), m_fields[0].getHadoopValue());
                start = 1;
                length += 1;
            }
            for (int i = start; i < length; i++) {
                DataCell c = row.getCell(i - start);
                Field f = m_fields[i];
                if (c.isMissing()) {
                    setHadoopValue(i, null);
                } else {
                    Object hadoopValue = f.getHadoopValue();
                    m_knimeTypes[i].set(c, hadoopValue);
                    setHadoopValue(i, hadoopValue);
                }
            }
            return this;
        }

        public void set(final int index, final String value) {
            Field f = getField(index, OrcKNIMEType.STRING);
            if (value == null) {
                setHadoopValue(index, null);
            } else {
                Text t = (Text)f.getHadoopValue();
                t.set(value);
                setHadoopValue(index, t);
            }
        }

        public void set(final int index, final Double value) {
            Field f = getField(index, OrcKNIMEType.DOUBLE);
            if (value == null) {
                setHadoopValue(index, null);
            } else {
                DoubleWritable t = (DoubleWritable)f.getHadoopValue();
                t.set(value);
                setHadoopValue(index, t);
            }
        }

        public void set(final int index, final Integer value) {
            Field f = getField(index, OrcKNIMEType.LONG);
            if (value == null) {
                setHadoopValue(index, (Object)null);
            } else {
                LongWritable t = (LongWritable)f.getHadoopValue();
                t.set(value);
                setHadoopValue(index, t);
            }
        }

        public void set(final int index, final byte[] value) {
            Field f = getField(index, OrcKNIMEType.BYTE_ARRAY);
            if (value == null) {
                setHadoopValue(index, null);
            } else {
                BytesWritable t = (BytesWritable)f.getHadoopValue();
                t.set(value, 0, value.length);
                setHadoopValue(index, t);
            }
        }

        private void setHadoopValue(final int index, final Object hadoopValue) {
            m_orcRow.setFieldValue(index, hadoopValue);
        }

        private Field getField(final int index, final OrcKNIMEType clientType) {
            final Field field = m_fields[index];
            if (!Objects.equals(clientType, field.getType())) {
                throw new IllegalArgumentException(
                    String.format("Expected type \"%s\", got \"%s\"", field.getType(), clientType));
            }
            return field;
        }

        OrcRow getOrcRow() {
            return m_orcRow;
        }

    }

    static final class Field {
        private final OrcKNIMEType m_type;
        private final Object m_hadoopValue;
        /**
         * @param type
         * @param hadoopValue
         */
        Field(final OrcKNIMEType type) {
            m_type = type;
            m_hadoopValue = m_type.createHadoopObject();
        }

        OrcKNIMEType getType() {
            return m_type;
        }

        Object getHadoopValue() {
            return m_hadoopValue;
        }

    }

    public static final class PrestoDrivenRowIterator implements AutoCloseable {

        private final OrcRecordReader m_recordReader;
        private final OrcKNIMEType[] m_colTypes;
        private final boolean m_hasRowKey;
        private Block[] m_currentBlocks;
        private int m_indexInBatch;
        private int m_batchSize;

        PrestoDrivenRowIterator(final OrcKNIMEType[] colTypes, final OrcRecordReader recordReader, final boolean hasRowKey) {
            m_colTypes = colTypes;
            m_recordReader = recordReader;
            m_hasRowKey = hasRowKey;
        }

        public PrestoDrivenRow next(final PrestoDrivenRow r) {
            if (m_indexInBatch >= m_batchSize) {
                if (m_batchSize >= 0) {
                    try {
                        m_batchSize = m_recordReader.nextBatch();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (m_batchSize <= 0) {
                    return null;
                }
                m_indexInBatch = 0;
                m_currentBlocks = new Block[m_colTypes.length];
                for (int i = 0; i < m_colTypes.length; i++) {
                    final int finalI = i;
//                    // TODO this may be called too late (when on new batch already)
//                    m_currentBlocks[i] = new LazyBlock(m_batchSize, new LazyBlockLoader<LazyBlock>() {
//                        volatile boolean isLoaded = false;
//                        @Override
//                        public void load(final LazyBlock block) {
//                            if (isLoaded) {
//                                return;
//                            }
//                            isLoaded = true;
//                            try {
//                                block.setBlock(m_recordReader.readBlock(m_colTypes[finalI].getPrestoType(), finalI));
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                    });
                    try {
                        m_currentBlocks[i] = m_recordReader.readBlock(m_colTypes[finalI].getPrestoType(), i);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            PrestoDrivenRow row = r == null ? new PrestoDrivenRow(m_colTypes, m_hasRowKey) : r;
            row.set(m_currentBlocks, m_indexInBatch++);
            return row;
        }

        /** {@inheritDoc} */
        @Override
        public void close() throws IOException {
            m_recordReader.close();
        }

    }

    public static final class PrestoDrivenRow implements DataRow {

        private final OrcKNIMEType[] m_colTypes;
        private Block[] m_blocks;
        private int m_position;
        private boolean m_hasRowKey;

        PrestoDrivenRow(final OrcKNIMEType[] colTypes, final boolean hasRowKey) {
            m_colTypes = colTypes;
            m_hasRowKey = hasRowKey;
        }

        void set(final Block[] blocks, final int position) {
            m_blocks = blocks;
            m_position = position;
        }

        public Object getValue(final int colIndex) {
            return m_colTypes[colIndex].getPrestoType().getObjectValue(null, m_blocks[colIndex], m_position);
        }
        private static final RowKey NO_KEY = new RowKey("no-key");

        /** {@inheritDoc} */
        @Override
        public RowKey getKey() {
            if (m_hasRowKey) {
                Object value = getValue(0);
                return new RowKey(((StringOrcKNIMEType)m_colTypes[0]).getString(value));
            } else {
                return NO_KEY;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final int index) {
            final int orcIndex = m_hasRowKey ? index + 1 : index;
            return m_colTypes[orcIndex].get(m_blocks[orcIndex], m_position);
        }

        /** {@inheritDoc} */
        @Override
        public Iterator<DataCell> iterator() {
            return new DefaultCellIterator(this);
        }

        /** {@inheritDoc} */
        @Override
        public int getNumCells() {
            return m_hasRowKey ? m_colTypes.length - 1 : m_colTypes.length;
        }

    }


}
