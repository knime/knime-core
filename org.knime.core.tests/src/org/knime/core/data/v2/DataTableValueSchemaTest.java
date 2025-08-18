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
 *   Feb 23, 2021 (benjamin): created
 */
package org.knime.core.data.v2;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.collection.SparseListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.internal.FileStoreProxy.FlushCallback;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.v2.schema.DataTableValueSchema;
import org.knime.core.data.v2.schema.DataTableValueSchemaUtils;
import org.knime.core.data.v2.schema.ValueSchema;
import org.knime.core.data.v2.schema.ValueSchemaLoadContext;
import org.knime.core.data.v2.value.BooleanListValueFactory;
import org.knime.core.data.v2.value.BooleanSetValueFactory;
import org.knime.core.data.v2.value.BooleanSparseListValueFactory;
import org.knime.core.data.v2.value.DoubleListValueFactory;
import org.knime.core.data.v2.value.DoubleSetValueFactory;
import org.knime.core.data.v2.value.DoubleSparseListValueFactory;
import org.knime.core.data.v2.value.IntListValueFactory;
import org.knime.core.data.v2.value.IntSetValueFactory;
import org.knime.core.data.v2.value.IntSparseListValueFactory;
import org.knime.core.data.v2.value.LongListValueFactory;
import org.knime.core.data.v2.value.LongSetValueFactory;
import org.knime.core.data.v2.value.LongSparseListValueFactory;
import org.knime.core.data.v2.value.StringListValueFactory;
import org.knime.core.data.v2.value.StringSetValueFactory;
import org.knime.core.data.v2.value.StringSparseListValueFactory;
import org.knime.core.data.v2.value.VoidRowKeyFactory;
import org.knime.core.data.v2.value.cell.DictEncodedDataCellValueFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.table.schema.DefaultColumnarSchema;

/**
 * Tests for the {@link DefaultDataTableValueSchema} and the {@link ValueFactory}s used by it.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public class DataTableValueSchemaTest {

    /** Test saving and loading a schema with all registered DataTypes */
    @Test
    public void testSchemaSaveLoadRegisteredDataTypes() {
        final DataTypeRegistry registry = DataTypeRegistry.getInstance();
        final Collection<DataType> dataTypes = registry.availableDataTypes();
        for (final DataType type : dataTypes) {
            final Optional<Class<? extends ValueFactory<?, ?>>> optFactoryClass = registry.getValueFactoryFor(type);
            final Class<? extends ValueFactory<?, ?>> factoryClass =
                optFactoryClass.isPresent() ? optFactoryClass.get() : DictEncodedDataCellValueFactory.class;
            try {
                testSchemaSaveLoadDataType(type, factoryClass);
            } catch (final Exception e) { // NOSONAR: We fail on every exception by adding our message.
                // Add information about the DataType which failed.
                throw new AssertionError("The ValueFactory could not be saved and loaded for the DataType '" + type
                    + "' with the cell class '" + type.getCellClass() + "'.", e);
            }
        }
    }

    // Double

    /** Test saving and loading a schema with a double list */
    @Test
    public void testSchemaSaveLoadDoubleList() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(ListCell.class, DoubleCell.TYPE), DoubleListValueFactory.class);
    }

    /** Test saving and loading a schema with a double set */
    @Test
    public void testSchemaSaveLoadDoubleSet() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(SetCell.class, DoubleCell.TYPE), DoubleSetValueFactory.class);
    }

    /** Test saving and loading a schema with a sparse double list */
    @Test
    public void testSchemaSaveLoadDoubleSparseList() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(SparseListCell.class, DoubleCell.TYPE),
            DoubleSparseListValueFactory.class);
    }

    // Int

    /** Test saving and loading a schema with a int list */
    @Test
    public void testSchemaSaveLoadIntList() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(ListCell.class, IntCell.TYPE), IntListValueFactory.class);
    }

    /** Test saving and loading a schema with a int set */
    @Test
    public void testSchemaSaveLoadIntSet() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(SetCell.class, IntCell.TYPE), IntSetValueFactory.class);
    }

    /** Test saving and loading a schema with a sparse int list */
    @Test
    public void testSchemaSaveLoadIntSparseList() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(SparseListCell.class, IntCell.TYPE),
            IntSparseListValueFactory.class);
    }

    // Long

    /** Test saving and loading a schema with a long list */
    @Test
    public void testSchemaSaveLoadLongList() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(ListCell.class, LongCell.TYPE), LongListValueFactory.class);
    }

    /** Test saving and loading a schema with a long set */
    @Test
    public void testSchemaSaveLoadLongSet() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(SetCell.class, LongCell.TYPE), LongSetValueFactory.class);
    }

    /** Test saving and loading a schema with a sparse long list */
    @Test
    public void testSchemaSaveLoadLongSparseList() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(SparseListCell.class, LongCell.TYPE),
            LongSparseListValueFactory.class);
    }

    // String

    /** Test saving and loading a schema with a String list */
    @Test
    public void testSchemaSaveLoadStringList() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(ListCell.class, StringCell.TYPE),
            StringListValueFactory.class);
    }

    /** Test saving and loading a schema with a String set */
    @Test
    public void testSchemaSaveLoadStringSet() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(SetCell.class, StringCell.TYPE),
            StringSetValueFactory.class);
    }

    /** Test saving and loading a schema with a sparse String list */
    @Test
    public void testSchemaSaveLoadStringSparseList() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(SparseListCell.class, StringCell.TYPE),
            StringSparseListValueFactory.class);
    }

    // Boolean

    /** Test saving and loading a schema with a boolean list */
    @Test
    public void testSchemaSaveLoadBooleanList() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(ListCell.class, BooleanCell.TYPE), BooleanListValueFactory.class);
    }

    /** Test saving and loading a schema with a boolean set */
    @Test
    public void testSchemaSaveLoadBooleanSet() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(SetCell.class, BooleanCell.TYPE), BooleanSetValueFactory.class);
    }

    /** Test saving and loading a schema with a sparse boolean list */
    @Test
    public void testSchemaSaveLoadBooleanSparseList() throws InvalidSettingsException {
        testSchemaSaveLoadDataType(DataType.getType(SparseListCell.class, BooleanCell.TYPE),
            BooleanSparseListValueFactory.class);
    }

    private static void testSchemaSaveLoadDataType(final DataType type, final Class<?> factoryClass)
        throws InvalidSettingsException {
        final DataColumnSpec columnSpec = new DataColumnSpecCreator("0", type).createSpec();
        final DataTableSpec tableSpec = new DataTableSpec(columnSpec);
        final IDataRepository dataRepository = null;
        final IWriteFileStoreHandler fileStoreHandler = new DummyWriteFileStoreHandler();

        // Create the schema and check
        final DataTableValueSchema schema = DataTableValueSchemaUtils.create(tableSpec, RowKeyType.NOKEY, fileStoreHandler);
        assertEquals(2, schema.numColumns());
        var rowKeyFactory = schema.getValueFactory(0);
        assertEquals(VoidRowKeyFactory.class, rowKeyFactory.getClass());
        var dataFactory = schema.getValueFactory(1);
        assertEquals(factoryClass, dataFactory.getClass());

        // Save to some note settings
        final NodeSettings settings = new NodeSettings("valueSchema");
        DataTableValueSchemaUtils.save(schema, settings);

        var columnarSchema = DefaultColumnarSchema.builder()//
                .addColumn(rowKeyFactory.getSpec(), ValueFactoryUtils.getTraits(rowKeyFactory))//
                .addColumn(dataFactory.getSpec(), ValueFactoryUtils.getTraits(dataFactory))//
                .build();
        var loadContext = mock(ValueSchemaLoadContext.class);
        when(loadContext.getTableSpec()).thenReturn(tableSpec);
        when(loadContext.getDataRepository()).thenReturn(dataRepository);
        when(loadContext.getSettings()).thenReturn(settings);
        // Load back and check
        final ValueSchema loadedSchema = DataTableValueSchemaUtils.load(columnarSchema, loadContext);
        assertEquals(VoidRowKeyFactory.class, loadedSchema.getValueFactory(0).getClass());
        assertEquals(2, loadedSchema.numColumns());
        assertEquals(factoryClass, loadedSchema.getValueFactory(1).getClass());
    }

    public static class DummyWriteFileStoreHandler implements IWriteFileStoreHandler {

        @Override
        public IDataRepository getDataRepository() {
            return null;
        }

        @Override
        public void clearAndDispose() {
            throw getException();
        }

        @Override
        public FileStore getFileStore(final FileStoreKey key) {
            throw getException();
        }

        @Override
        public FileStore createFileStore(final String name) throws IOException {
            throw getException();
        }

        @Override
        public FileStore createFileStore(final String name, final int[] nestedLoopPath, final int iterationIndex)
            throws IOException {
            throw getException();
        }

        @Override
        public void open(final ExecutionContext exec) {
            throw getException();
        }

        @Override
        public void addToRepository(final IDataRepository repository) {
            throw getException();
        }

        @Override
        public void close() {
            throw getException();
        }

        @Override
        public void ensureOpenAfterLoad() throws IOException {
            throw getException();
        }

        @Override
        public FileStoreKey translateToLocal(final FileStore fs, final FlushCallback flushCallback) {
            throw getException();
        }

        @Override
        public boolean mustBeFlushedPriorSave(final FileStore fs) {
            throw getException();
        }

        @Override
        public UUID getStoreUUID() {
            throw getException();
        }

        @Override
        public File getBaseDir() {
            throw getException();
        }

        @Override
        public boolean isReference() {
            throw getException();
        }

        private static IllegalStateException getException() {
            return new IllegalStateException("Dummy should not be called");
        }
    }
}
