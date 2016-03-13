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
 *   Jan 1, 2016 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.FileReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.avro.AvroReadSupport;
import org.apache.parquet.avro.AvroSchemaConverter;
import org.apache.parquet.avro.AvroWriteSupport;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.orc.OrcKNIMEUtil;
import org.knime.orc.OrcKNIMEUtil.OrcKNIMEWritableRow;
import org.knime.orc.OrcKNIMEUtil.OrcKNIMEWriter;
import org.knime.orc.OrcKNIMEUtil.OrcWriterBuilder;
import org.knime.orc.OrcKNIMEUtil.PrestoDrivenRow;
import org.knime.orc.OrcKNIMEUtil.PrestoDrivenRowIterator;

/**
 *
 * @author wiswedel
 */
public class ParquetTest {

    private static final long ROW_COUNT = 1 * 200 * 1000;
    private static final int COL_COUNT = 500;

    private enum TestRun {
        Dummy,
        Avro,
        Parquet,
        Orc,
        Plain,
        KNIME
    }

    private static final List<TestRun> RUNNERS = Arrays.asList(TestRun.Orc);
//    private static final List<TestRun> RUNNERS = Arrays.asList(TestRun.Parquet, TestRun.Avro);

    public static final Supplier<Object[]> getData() {
        final MutableLong index = new MutableLong();
        final Random r = new Random(3214013);
        return () -> {
            if (index.longValue() < ROW_COUNT) {
                index.increment();
                Object[] result = new Object[COL_COUNT];
                if (index.longValue() == 1) {
                    return result;
                }
                for (int i = 0; i < COL_COUNT; i++) {
                    Object o;
                    switch (i % 3) {
                        case 0:
                            o = RandomStringUtils.random(3 + r.nextInt(20), 0, 0, true, false, null, r);
                            break;
                        case 1:
                            o = new Double(50.0 + (r.nextInt(1600) / 10.0));
                            break;
                        case 2:
                            o = Integer.valueOf(r.nextInt(85));
                            break;
                        default:
                            throw new InternalError();
                    }
                    result[i] = o;
                }
                return result;
            }
            return null;
        };
    }

    @Test
    public void testCreateAndIgnore() {
        Assume.assumeTrue(RUNNERS.contains(TestRun.Dummy));
        Supplier<Object[]> supplier = getData();
        long start = System.currentTimeMillis();
        final long start0 = start;
        long i = 0L;
        Object[] data;
        while ((data = supplier.get()) != null) {
            i += 1L;
            if (i % (ROW_COUNT / 10L) == 0) {
                long end = System.currentTimeMillis();
                printProgress(start, end, i);
                start = end;
            }
        }
        printSummary(start0, new File("/tmp"));
    }

    @Test
    public void testPlain() throws Exception {
        Assume.assumeTrue(RUNNERS.contains(TestRun.Plain));
        long start = System.currentTimeMillis();
        final File file = new File("/tmp/written.plain");
        writePlain(file);
        printSummary(start, file);
        System.out.println();
        start = System.currentTimeMillis();
        readPlain(file);
        printSummary(start, file);
    }

    private static final byte CONTINUATION = 0xa;
    private static final byte END = 0xb;

    private void writePlain(final File file) throws Exception {
        long start = System.currentTimeMillis();
        try (DataOutputStream outputStream =
                new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            Supplier<Object[]> dataSupplier = getData();
            long i = 0;
            Object[] data;
            while ((data = dataSupplier.get()) != null) {
                i += 1;
                if (i % (ROW_COUNT / 100) == 0) {
                    long end = System.currentTimeMillis();
                    printProgress(start, end, i);
                    start = end;
                }
                outputStream.writeByte(CONTINUATION);
                forAllValues(data,
                    (o) -> outputStream.writeUTF((String)o),
                    (o) -> outputStream.writeDouble((Double)o),
                    (o) -> outputStream.writeInt((Integer)o));
            }
            outputStream.writeByte(END);
        }
    }

    /**
     * @param file
     * @param outSchema
     * @throws IOException
     */
    private void readPlain(final File file) throws Exception {
        long start;
        Supplier<Object[]> dataSupplier = getData();
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            long i = 0L;
            start = System.currentTimeMillis();
            while (input.readByte() != END) {
                i += 1L;
                if (i % (ROW_COUNT / 10L) == 0) {
                    long end = System.currentTimeMillis();
                    printProgress(start, end, i);
                    start = end;
                }
//                Object[] reference = dataSupplier.get();
//                forAllValues(reference,
//                    r -> Assert.assertEquals("Line " + i, input.readUTF().toString(), r),
//                    r -> Assert.assertEquals("Line " + i, input.readDouble(), r),
//                    r -> Assert.assertEquals("Line " + i, input.readInt(), r));
                Object[] reference = new Object[COL_COUNT];
                forAllValues(reference, r -> input.readUTF(), r -> input.readDouble(), r -> input.readInt());
            }
        }
//        Assert.assertNull(dataSupplier.get());
    }

    @Test
    public void testAvro() throws Exception {
        Assume.assumeTrue(RUNNERS.contains(TestRun.Avro));
        long start = System.currentTimeMillis();
        final File file = new File("/tmp/written.avro");
        writeAvro(file);
        printSummary(start, file);

        System.out.println();
        start = System.currentTimeMillis();
        readAvro(file);
        printSummary(start, file);
    }

    private void writeAvro(final File file) throws Exception {
        Schema outSchema = createAvroSchema(COL_COUNT);

        long start = System.currentTimeMillis();
        try (DataFileWriter<Object> writer = new DataFileWriter<Object>(new GenericDatumWriter<Object>())) {
            writer.setCodec(CodecFactory.nullCodec());
            writer.create(outSchema, file);
            Supplier<Object[]> dataSupplier = getData();
            long i = 0;
            Object[] data;
            while ((data = dataSupplier.get()) != null) {
                i += 1;
                if (i % (ROW_COUNT / 10) == 0) {
                    long end = System.currentTimeMillis();
                    printProgress(start, end, i);
                    start = end;
                }
                Integer[] array = IntStream.range(0, COL_COUNT).boxed().toArray(Integer[]::new);
                final Object[] finalData = data;
                final GenericRecordBuilder builder = new GenericRecordBuilder(outSchema);
                forAllValues(array,
                    index -> builder.set("name_" + index, finalData[index]),
                    index -> builder.set("size_" + index, finalData[index]),
                    index -> builder.set("age_" + index, finalData[index]));
                writer.append(builder.build());
            }
        }
    }

    /**
     * @param file
     * @param outSchema
     * @throws IOException
     */
    private void readAvro(final File file) throws Exception {
        long start;
        Supplier<Object[]> dataSupplier = getData();
        try (FileReader<GenericRecord> reader = DataFileReader.<GenericRecord>openReader(file, new GenericDatumReader<>())) {
            GenericRecord record = null;
            long i = 0L;
            start = System.currentTimeMillis();
            while (reader.hasNext()) {
                i += 1L;
                if (i % (ROW_COUNT / 10L) == 0) {
                    long end = System.currentTimeMillis();
                    printProgress(start, end, i);
                    start = end;
                }
                record = reader.next(record);
                Integer[] array = IntStream.range(0, COL_COUNT).boxed().toArray(Integer[]::new);
                final GenericRecord finalRecord = record;
//                final Object[] finalData = dataSupplier.get();
//                forAllValues(array,
//                    index -> Assert.assertEquals(Objects.toString(finalRecord.get("name_" + index), null), finalData[index]),
//                    index -> Assert.assertEquals(finalRecord.get("size_" + index), finalData[index]),
//                    index -> Assert.assertEquals(finalRecord.get("age_" + index), finalData[index]));
                forAllValues(array,
                    index -> Objects.toString(finalRecord.get("name_" + index), null),
                    index -> finalRecord.get("size_" + index),
                    index -> finalRecord.get("age_" + index));
            }
        }
//        Assert.assertNull(dataSupplier.get());
    }

    @Test
    public void testParquet() throws Exception {
        Assume.assumeTrue(RUNNERS.contains(TestRun.Parquet));
        long start = System.currentTimeMillis();
        final File file = new File("/tmp/written.parquet");
//        writeParquet(file);
        long durationWrite = System.currentTimeMillis() - start;
//        printSummary(start, file);

        System.out.println();
        start = System.currentTimeMillis();
        readParquet(file);
        printSummary(start, file);
        System.out.printf("Write time Parquet %s%n", DurationFormatUtils.formatDurationHMS(durationWrite));
    }

    private void writeParquet(final File file) throws Exception {
        Schema outSchema = createAvroSchema(COL_COUNT);
        FileUtils.deleteQuietly(file);
        Path hadoopPath = new Path(file.getPath());
        long start = System.currentTimeMillis();
        Configuration testConf = createHadoopParquetConf();
        try (ParquetWriter<GenericRecord> writer =
                AvroParquetWriter.<GenericRecord>builder(hadoopPath).withSchema(outSchema)
                .withConf(testConf).withCompressionCodec(CompressionCodecName.SNAPPY).build()) {
            Supplier<Object[]> dataSupplier = getData();
            long i = 0;
            Object[] data;
            while ((data = dataSupplier.get()) != null) {
                i += 1;
                if (i % (ROW_COUNT / 10) == 0) {
                    long end = System.currentTimeMillis();
                    printProgress(start, end, i);
                    start = end;
                }
                Integer[] array = IntStream.range(0, COL_COUNT).boxed().toArray(Integer[]::new);
                final Object[] finalData = data;
                final GenericRecordBuilder builder = new GenericRecordBuilder(outSchema);
                forAllValues(array,
                    index -> builder.set("name_" + index, finalData[index]),
                    index -> builder.set("size_" + index, finalData[index]),
                    index -> builder.set("age_" + index, finalData[index]));
                writer.write(builder.build());
            }
        }
    }

    /**
     * @param file
     * @param outSchema
     * @throws IOException
     */
    private void readParquet(final File file) throws Exception {
        long start;
        Supplier<Object[]> dataSupplier = getData();
        Path hadoopPath = new Path(file.getPath());
        final Configuration hadoopParquetConf = createHadoopParquetConf();
        final int readColCount = COL_COUNT / 100;
        Schema filterSchema = createAvroSchema(readColCount);
        AvroReadSupport.setRequestedProjection(hadoopParquetConf, filterSchema);
        try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(hadoopPath)
                .disableCompatibility().withConf(hadoopParquetConf).build();) {
            long i = 0L;
            start = System.currentTimeMillis();
            GenericRecord record;
            while ((record = reader.read()) != null) {
                i += 1L;
                if (i % (ROW_COUNT / 10L) == 0) {
                    long end = System.currentTimeMillis();
                    printProgress(start, end, i);
                    start = end;
                }
                Integer[] array = IntStream.range(0, readColCount).boxed().toArray(Integer[]::new);
                final GenericRecord finalRecord = record;
//                final Object[] finalData = dataSupplier.get();
//                forAllValues(array,
//                    index -> Assert.assertEquals(Objects.toString(finalRecord.get("name_" + index), null), finalData[index]),
//                    index -> Assert.assertEquals(finalRecord.get("size_" + index), finalData[index]),
//                    index -> Assert.assertEquals(finalRecord.get("age_" + index), finalData[index]));
                forAllValues(array,
                    index -> Objects.toString(finalRecord.get("name_" + index), null),
                    index -> finalRecord.get("size_" + index),
                    index -> finalRecord.get("age_" + index));
            }
        }
//        Assert.assertNull(dataSupplier.get());
    }

    @Test
    public void testORC() throws Exception {
        Assume.assumeTrue(RUNNERS.contains(TestRun.Orc));
        long start = System.currentTimeMillis();
        final File file = new File("/tmp/written.orc");
        writeORC(file);
        printSummary(start, file);
        long durationWrite = System.currentTimeMillis() - start;

        System.out.println();
        start = System.currentTimeMillis();
        readORC(file);
        printSummary(start, file);
        System.out.printf("Write time ORC %s%n", DurationFormatUtils.formatDurationHMS(durationWrite));
    }

    private void writeORC(final File file) throws Exception {
        file.delete();

        final OrcWriterBuilder builder = createORCBuilder(file);

        Integer[] array = IntStream.range(0, COL_COUNT).boxed().toArray(Integer[]::new);
        long start = System.currentTimeMillis();
        Supplier<Object[]> dataSupplier = getData();
        try (OrcKNIMEWriter orcWriter = builder.create()) {
            OrcKNIMEWritableRow rowReference = builder.createOrcKNIMEWritableRow();
            long i = 0;
            Object[] data;
            while ((data = dataSupplier.get()) != null) {
                i += 1;
                if (i % (ROW_COUNT / 10) == 0) {
                    long end = System.currentTimeMillis();
                    printProgress(start, end, i);
                    start = end;
                }

                final Object[] finalData = data;
                forAllValues(array,
                    index -> rowReference.set(index, (String)finalData[index]),
                    index -> rowReference.set(index, (Double)finalData[index]),
                    index -> rowReference.set(index, (Integer)finalData[index]));
                orcWriter.addRow(rowReference);
            }
        }
    }

    /**
     * @param file
     * @return
     * @throws Exception
     */
    private OrcWriterBuilder createORCBuilder(final File file) throws Exception {
        final OrcWriterBuilder builder = new OrcWriterBuilder(file);
        forAllValues(IntStream.range(0, COL_COUNT).boxed().toArray(Integer[]::new),
            index -> builder.addField("name_" + index, OrcKNIMEUtil.MyType.StringType),
            index -> builder.addField("size_" + index, OrcKNIMEUtil.MyType.DoubleType),
            index -> builder.addField("age_" + index, OrcKNIMEUtil.MyType.BigIntType));
        return builder;
    }

    /**
     * @param file
     * @param outSchema
     * @throws IOException
     */
    private void readORC(final File file) throws Exception {
        OrcWriterBuilder orcBuilder = createORCBuilder(file);
        Supplier<Object[]> dataSupplier = getData();
        PrestoDrivenRowIterator rowIterator = orcBuilder.createRowIterator();

        long start = System.currentTimeMillis();
        final int readColCount = COL_COUNT / 3;
        PrestoDrivenRow row = null;
        long i = 0L;
        Integer[] array = IntStream.range(0, readColCount).boxed().toArray(Integer[]::new);
        while ((row = rowIterator.next(row)) != null) {
            i += 1L;
            if (i % (ROW_COUNT / 10L) == 0) {
                long end = System.currentTimeMillis();
                printProgress(start, end, i);
                start = end;
            }
            final PrestoDrivenRow finalRow = row;
            final Object[] finalData = dataSupplier.get();
            forAllValues(array,
                index -> Assert.assertEquals(finalData[index], Objects.toString(finalRow.getValue(index), null)),
                index -> Assert.assertEquals(finalData[index], finalRow.getValue(index)),
                index -> Assert.assertEquals(
                    finalData[index] == null ? null : new Long(((Integer)finalData[index]).longValue()),
                    finalRow.getValue(index)));
//            forAllValues(array,
//                index -> Objects.toString(finalRow.getValue(index), null),
//                index -> finalRow.getValue(index),
//                index -> finalRow.getValue(index));

        }
//        Assert.assertNull(dataSupplier.get());
    }

    @Test
    public void testKNIME() throws Exception {
        Assume.assumeTrue(RUNNERS.contains(TestRun.KNIME));
        long start = System.currentTimeMillis();
        ContainerTable table = writeKNIME();
        final File file = table.getBuffer().getBinFile();
        long durationWrite = System.currentTimeMillis() - start;
        printSummary(start, file);

        System.out.println();
        start = System.currentTimeMillis();
        readKNIME(table);
        printSummary(start, file);
    }

    private ContainerTable writeKNIME() throws Exception {
        DataContainer container = new DataContainer(createKNIMESpec(COL_COUNT));
        long start = System.currentTimeMillis();
        Supplier<Object[]> dataSupplier = getData();
        long i = 0;
        Object[] data;
        while ((data = dataSupplier.get()) != null) {
            i += 1;
            if (i % (ROW_COUNT / 10) == 0) {
                long end = System.currentTimeMillis();
                printProgress(start, end, i);
                start = end;
            }
            Integer[] array = IntStream.range(0, COL_COUNT).boxed().toArray(Integer[]::new);
            final Object[] finalData = data;
            final DataCell[] finalCellArray = new DataCell[data.length];
            forAllValues(array,
                index -> finalCellArray[index] = new StringCell((String)finalData[index]),
                index -> finalCellArray[index] = new DoubleCell((Double)finalData[index]),
                index -> finalCellArray[index] = new IntCell((Integer)finalData[index]));
            container.addRowToTable(new BlobSupportDataRow(RowKey.createRowKey(i), finalCellArray));
        }
        container.close();
        return container.getBufferedTable();
    }

    /**
     * @param file
     * @param outSchema
     * @throws IOException
     */
    private void readKNIME(final ContainerTable table) throws Exception {
        long start;
        Supplier<Object[]> dataSupplier = getData();
        long i = 0L;
        start = System.currentTimeMillis();
        for (DataRow r : table) {
            i += 1L;
            if (i % (ROW_COUNT / 10L) == 0) {
                long end = System.currentTimeMillis();
                printProgress(start, end, i);
                start = end;
            }
            Integer[] array = IntStream.range(0, COL_COUNT).boxed().toArray(Integer[]::new);
            forAllValues(array,
                index -> ((StringValue)r.getCell(index)).getStringValue(),
                index -> ((DoubleValue)r.getCell(index)).getDoubleValue(),
                index -> ((IntValue)r.getCell(index)).getIntValue());

//            final Object[] finalData = dataSupplier.get();
//            forAllValues(array,
//                index -> Assert.assertEquals(((StringValue)r.getCell(index)).getStringValue(), finalData[index]),
//                index -> Assert.assertEquals(((DoubleValue)r.getCell(index)).getDoubleValue(), finalData[index]),
//                index -> Assert.assertEquals(((IntValue)r.getCell(index)).getIntValue(), finalData[index]));
//                forAllValues(array,
//                    index -> Assert.assertEquals(finalRecord.get("size_" + index), finalData[index]),
//                    index -> Assert.assertEquals(finalRecord.get("age_" + index), finalData[index]));
        }
//        Assert.assertNull(dataSupplier.get());
    }

    /**
     * @param colCount TODO
     * @return
     */
    private static Schema createAvroSchema(final int colCount) throws Exception {
        final FieldAssembler<Schema> fieldAssembler = SchemaBuilder.builder().record("record").fields();
        Integer[] array = IntStream.range(0, colCount).boxed().toArray(Integer[]::new);
        forAllValues(array,
            index -> fieldAssembler.optionalString("name_" + index),
            index -> fieldAssembler.optionalDouble("size_" + index),
            index -> fieldAssembler.optionalInt("age_" + index));
        return fieldAssembler.endRecord();
    }

    /**
     * @param colCount TODO
     * @return
     */
    private static DataTableSpec createKNIMESpec(final int colCount) throws Exception {
        final DataTableSpecCreator c = new DataTableSpecCreator();
        Integer[] array = IntStream.range(0, colCount).boxed().toArray(Integer[]::new);
        forAllValues(array,
            index -> c.addColumns(new DataColumnSpecCreator("name_" + index, StringCell.TYPE).createSpec()),
            index -> c.addColumns(new DataColumnSpecCreator("size_" + index, DoubleCell.TYPE).createSpec()),
            index -> c.addColumns(new DataColumnSpecCreator("age_" + index, IntCell.TYPE).createSpec()));
        return c.createSpec();
    }

    /**
     * @return
     */
    private static Configuration createHadoopParquetConf() {
        Configuration testConf = new Configuration();
        testConf.setBoolean(AvroReadSupport.AVRO_COMPATIBILITY, false);
        testConf.setBoolean(AvroSchemaConverter.ADD_LIST_ELEMENT_RECORDS, false);
        testConf.setBoolean(AvroWriteSupport.WRITE_OLD_LIST_STRUCTURE, false);
        return testConf;
    }

    /**
     * @param start
     * @param end
     * @param count
     */
    private static void printProgress(final long start, final long end, final long count) {
        System.out.printf("Written %,11d (%s)%n", count,
            DurationFormatUtils.formatDuration(end - start, "mm:ss.S", true));
    }

    /**
     * @param start
     * @param file
     */
    private static void printSummary(final long start, final File file) {
        System.out.printf("Written %sMB in %s%n", FileUtils.sizeOf(file) / FileUtils.ONE_MB,
            DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start));
    }

    private static <OBJ> void forAllValues(final OBJ[] objects, final OperationOnOne<OBJ> mod0,
        final OperationOnOne<OBJ> mod1, final OperationOnOne<OBJ> mod2) throws Exception {
        for (int index = 0; index < objects.length; index++) {
            final OBJ obj = objects[index];
            switch (index % 3) {
                case 0:
                    mod0.doIt(obj);
                    break;
                case 1:
                    mod1.doIt(obj);
                    break;
                case 2:
                    mod2.doIt(obj);
                    break;
                default:
                    throw new InternalError();
            }
        }
    }

    private static <REF, OBJ> void forAllValues(final REF[] refs, final OBJ[] objects,
        final OperationOnTwo<REF, OBJ> mod0,
        final OperationOnTwo<REF, OBJ> mod1,
        final OperationOnTwo<REF, OBJ> mod2) throws Exception {
        for (int index = 0; index < objects.length; index++) {
            final REF ref = refs != null ? refs[index] : null;
            final OBJ obj = objects[index];
            switch (index % 3) {
                case 0:
                    mod0.doIt(ref, obj);
                    break;
                case 1:
                    mod1.doIt(ref, obj);
                    break;
                case 2:
                    mod2.doIt(ref, obj);
                    break;
                default:
                    throw new InternalError();
            }
        }
    }

    @FunctionalInterface
    private static interface OperationOnOne<O> {
        public void doIt(O o) throws Exception;
    }

    @FunctionalInterface
    private static interface OperationOnTwo<REF, OBJ> {
        public void doIt(REF ref, OBJ o) throws Exception;
    }
}
