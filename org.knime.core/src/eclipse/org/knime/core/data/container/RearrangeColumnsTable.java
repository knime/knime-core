/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 *
 * History
 *   Jul 5, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.DatatypeConverter;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger.SpecAndFactoryObject;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.MultiThreadWorker;
import org.knime.core.util.Pair;


/**
 * Table implementation that is created based on a ColumnRearranger. This class
 * is not intended for subclassing or to be used directly in any node
 * implementation. Instead use the functionality provided through the
 * {@link ColumnRearranger} and the {@link org.knime.core.node.ExecutionContext}
 * that is provided in the NodeModel's execute method. See
 * {@link ColumnRearranger} for more details on how to use them.
 * @author Bernd Wiswedel, University of Konstanz
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class RearrangeColumnsTable
    implements DataTable, KnowsRowCountTable {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RearrangeColumnsTable.class);

    private static final RowKey DUMMY_KEY = new RowKey("non-existing");
    private static final DataRow DUMMY_ROW =
        new DefaultRow(DUMMY_KEY, new DataCell[0]);

    /** If this table just filters columns from the reference table, we use
     * this dummy iterator to provide empty appended cells.
     */
    private static final CloseableRowIterator EMPTY_ITERATOR =
        new CloseableRowIterator() {
        @Override
        public boolean hasNext() {
            return true;
        }
        @Override
        public DataRow next() {
            return DUMMY_ROW;
        }
        @Override
        public void close() {
            // no op
        }
    };

    private static final String CFG_INTERNAL_META = "meta_internal";
    private static final String CFG_REFERENCE_ID = "table_reference_ID";
    private static final String CFG_MAP = "table_internal_map";
    private static final String CFG_FLAGS = "table_internal_flags";

    private final DataTableSpec m_spec;
    private final BufferedDataTable m_reference;
    private final int[] m_map;
    private final boolean[] m_isFromRefTable;
    private final ContainerTable m_appendTable;

    /*
     * Used from the factory method, see below.
     * @see #create(ColumnRearranger, BufferedDataTable, ExecutionMonitor)
     */
    private RearrangeColumnsTable(final BufferedDataTable reference,
            final int[] map, final boolean[] isFromRefTable,
            final DataTableSpec spec, final ContainerTable appendTbl) {
        m_spec = spec;
        m_reference = reference;
        m_appendTable = appendTbl;
        m_map = map;
        m_isFromRefTable = isFromRefTable;
    }

    /**
     * Creates new object based on the content in <code>settings</code> and
     * the content from the file <code>f</code>. Used when the data is restored
     * from disc.
     *
     * <p><b>Note</b>: You should not be required to use this constructor!
     * @param f The file to read from the newly appended columns.
     * @param settings The settings containing the information how to assemble
     *         the table.
     * @param tblRep The table repository (only available during start)
     * @param spec The data table spec of the resulting table. This argument
     * is <code>null</code> when the data to restore is written using
     * KNIME 1.1.x or before.
     * @param tableID buffer ID of underlying buffer.
     * @param bufferRep Repository of buffers for blob (de)serialization.
     * @param fileStoreHandlerRepository ...
     * @throws IOException If reading the fails.
     * @throws InvalidSettingsException If the settings are invalid.
     * @noreference
     */
    public RearrangeColumnsTable(final ReferencedFile f,
            final NodeSettingsRO settings,
            final Map<Integer, BufferedDataTable> tblRep,
            final DataTableSpec spec, final int tableID,
            final HashMap<Integer, ContainerTable> bufferRep,
            final FileStoreHandlerRepository fileStoreHandlerRepository)
        throws IOException, InvalidSettingsException {
        NodeSettingsRO subSettings =
            settings.getNodeSettings(CFG_INTERNAL_META);
        int refTableID = subSettings.getInt(CFG_REFERENCE_ID);
        m_reference = BufferedDataTable.getDataTable(tblRep, refTableID);
        m_map = subSettings.getIntArray(CFG_MAP);
        m_isFromRefTable = subSettings.getBooleanArray(CFG_FLAGS);
        DataColumnSpec[] appendColSpecs;
        int appendColCount = 0;
        for (int i = 0; i < m_isFromRefTable.length; i++) {
            if (!m_isFromRefTable[i]) {
                appendColCount += 1;
            }
        }
        // appendColCount may be null for a column filter, for instance.
        if (appendColCount > 0) {
            appendColSpecs = new DataColumnSpec[appendColCount];
            // was written with version 1.1.x or before (i.e. table spec
            // is contained in zip file)
            if (spec == null) {
                m_appendTable = DataContainer.readFromZip(
                        f, new NoKeyBufferCreator());
                DataTableSpec appendSpec = m_appendTable.getDataTableSpec();
                if (appendSpec.getNumColumns() != appendColCount) {
                    throw new IOException("Inconsistency in data file \""
                            + f + "\", read " + appendSpec.getNumColumns()
                            + " columns, expected " + appendColCount);
                }
                for (int i = 0; i < appendSpec.getNumColumns(); i++) {
                    appendColSpecs[i] = appendSpec.getColumnSpec(i);
                }
            } else { // version 1.2.0 or later.
                int index = 0;
                for (int i = 0; i < m_isFromRefTable.length; i++) {
                    if (!m_isFromRefTable[i]) {
                        assert m_map[i] == index;
                        appendColSpecs[index++] = spec.getColumnSpec(i);
                    }
                }
                assert index == appendColCount;
                DataTableSpec appendSpec = new DataTableSpec(appendColSpecs);
                CopyOnAccessTask noKeyBufferOnAccessTask = new CopyOnAccessTask(
                        f, appendSpec, tableID, bufferRep,
                        fileStoreHandlerRepository, new NoKeyBufferCreator());
                m_appendTable = DataContainer.readFromZipDelayed(
                        noKeyBufferOnAccessTask, appendSpec);
            }
        } else {
            m_appendTable = null;
            appendColSpecs = new DataColumnSpec[0];
        }
        DataTableSpec refSpec = m_reference.getDataTableSpec();
        DataColumnSpec[] colSpecs = new DataColumnSpec[m_isFromRefTable.length];
        for (int i = 0; i < colSpecs.length; i++) {
            if (m_isFromRefTable[i]) {
                colSpecs[i] = refSpec.getColumnSpec(m_map[i]);
            } else {
                colSpecs[i] = appendColSpecs[m_map[i]];
            }
        }
        m_spec = new DataTableSpec(colSpecs);
    }

    /** Get handle to the reference table in an array of length 1.
     * @return The table providing likely most of the columns and the rowkeys.
     */
    @Override
    public BufferedDataTable[] getReferenceTables() {
        return new BufferedDataTable[]{m_reference};
    }

    /** Get reference to the appended table. This table must not be used
     * publicly as the append table is corrupted: It does not contain proper
     * row keys (it contains only the appended columns). This method returns
     * null if this table only filters out some of the columns.
     * @return Reference to append table.
     */
    public ContainerTable getAppendTable() {
        return m_appendTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseableRowIterator iterator() {
        CloseableRowIterator appendIt;
        if (m_appendTable != null) {
            appendIt = m_appendTable.iterator();
        } else {
            appendIt = EMPTY_ITERATOR;
        }
        return new JoinTableIterator(
                m_reference.iterator(), appendIt, m_map, m_isFromRefTable);
    }

    /**
     * This factory method is intended to be used immediately before the
     * {@link BufferedDataTable} is created.
     * @param rearranger The meta information how to assemble everything.
     * @param table The reference table.
     * @param subProgress The progress monitor for progress/cancel.
     * @param context Used for data container creation.
     * @return The newly created table.
     * @throws CanceledExecutionException If canceled.
     * @throws IllegalArgumentException If the spec is not equal to the
     * spec of the rearranger.
     */
    public static RearrangeColumnsTable create(
            final ColumnRearranger rearranger, final BufferedDataTable table,
            final ExecutionMonitor subProgress, final ExecutionContext context)
        throws CanceledExecutionException {
        DataTableSpec originalSpec = rearranger.getOriginalSpec();
        Vector<SpecAndFactoryObject> includes = rearranger.getIncludes();
        // names and types of the specs must match
        if (!table.getDataTableSpec().equalStructure(originalSpec)) {
            throw new IllegalArgumentException(
                    "The argument table's spec does not match the original "
                    + "spec passed in the constructor.");
        }
        int size = includes.size();
        ArrayList<DataColumnSpec> newColSpecsList = new ArrayList<DataColumnSpec>();
        // the reduced set of SpecAndFactoryObject that models newly
        // appended/inserted columns; this vector is in most cases
        // considerably smaller than the vector includes
        NewColumnsProducerMapping newColsProducerMapping = createNewColumnsProducerMapping(includes);
        List<SpecAndFactoryObject> newColumnFactoryList = newColsProducerMapping.getAllNewColumnsList();
        // with v2.5 we added the ability to process the input concurrently
        // this field has the minimum worker count for all used factories
        // (or negative for sequential processing)
        int workerCount = Integer.MAX_VALUE;
        for (SpecAndFactoryObject s : newColumnFactoryList) {
            CellFactory factory = s.getFactory();
            if (factory instanceof AbstractCellFactory) {
                AbstractCellFactory acf = (AbstractCellFactory)factory;
                workerCount = Math.min(workerCount, acf.getMaxParallelWorkers());
            } else {
                // unknown factory - process sequentially
                workerCount = -1;
            }
            newColSpecsList.add(s.getColSpec());
        }
        final int newColCount = newColSpecsList.size();
        DataColumnSpec[] newColSpecs =
            newColSpecsList.toArray(new DataColumnSpec[newColSpecsList.size()]);
        ContainerTable appendTable;
        DataTableSpec appendTableSpec;
        // for a pure filter (a table that just hides some columns from
        // the reference table but does not add any new column we avoid to scan
        // the entire table (nothing is written anyway))
        if (newColCount > 0) {
            DataContainer container = context.createDataContainer(new DataTableSpec(newColSpecs));
            container.setBufferCreator(new NoKeyBufferCreator());
            assert newColumnFactoryList.size() == newColCount;
            try {
                if (workerCount <= 0) {
                    calcNewColsSynchronously(table, subProgress, newColsProducerMapping, container);
                } else {
                    calcNewColsASynchronously(table, subProgress, newColsProducerMapping, container);
                }
            } finally {
                container.close();
            }
            appendTable = container.getBufferedTable();
            appendTableSpec = appendTable.getDataTableSpec();
        } else {
            appendTable = null;
            appendTableSpec = new DataTableSpec();
        }
        boolean[] isFromRefTable = new boolean[size];
        int[] includesIndex = new int[size];
        // create the new spec. Do not use rearranger.createSpec because
        // that might lack the domain information!
        DataColumnSpec[] colSpecs = new DataColumnSpec[size];
        int newColIndex = 0;
        for (int i = 0; i < size; i++) {
            SpecAndFactoryObject c = includes.get(i);
            if (c.isConvertedColumn()) {
                isFromRefTable[i] = false;
                includesIndex[i] = newColIndex;
                colSpecs[i] = appendTableSpec.getColumnSpec(newColIndex);
                newColIndex++;
            } else if (c.isNewColumn()) {
                isFromRefTable[i] = false;
                includesIndex[i] = newColIndex;
                colSpecs[i] = appendTableSpec.getColumnSpec(newColIndex);
                newColIndex++;
            } else {
                isFromRefTable[i] = true;
                int originalIndex = c.getOriginalIndex();
                includesIndex[i] = originalIndex;
                colSpecs[i] = originalSpec.getColumnSpec(originalIndex);
            }
        }
        DataTableSpec spec = new DataTableSpec(colSpecs);
        finishProcessing(newColsProducerMapping);
        return new RearrangeColumnsTable(
                table, includesIndex, isFromRefTable, spec, appendTable);
    }

    /** Calls {@link AbstractCellFactory#afterProcessing()} on all unique
     * factories in the argument.
     * @param newColumnFactoryList */
    static void finishProcessing(final NewColumnsProducerMapping newColumnFactoryList) {
        for (CellFactory uniqueFactory : newColumnFactoryList.getUniqueCellFactoryMap().keySet()) {
            if (uniqueFactory instanceof AbstractCellFactory) {
                ((AbstractCellFactory)uniqueFactory).afterProcessing();
            }
        }
    }

    /** Processes input sequentially in the caller thread. */
    private static void calcNewColsSynchronously(final BufferedDataTable table,
            final ExecutionMonitor subProgress, final NewColumnsProducerMapping newColsProducerMapping,
            final DataContainer container) throws CanceledExecutionException {
        int finalRowCount = table.getRowCount();
        Set<CellFactory> newColsFactories = newColsProducerMapping.getUniqueCellFactoryMap().keySet();
        final int factoryCount = newColsFactories.size();
        int r = 0;
        CellFactory facForProgress = factoryCount > 0 ? newColsFactories.iterator().next() : null;
        for (RowIterator it = table.iterator(); it.hasNext(); r++) {
            DataRow row = it.next();
            DataRow append = calcNewCellsForRow(row, newColsProducerMapping);
            container.addRowToTable(append);
            if (facForProgress == null) {
                // no factory added means at least one columns gets type converted.
                assert !newColsProducerMapping.getConverterToIndexMap().isEmpty();
            } else {
                facForProgress.setProgress(r + 1, finalRowCount, row.getKey(), subProgress);
            }
            subProgress.checkCanceled();
        }
    }

    /** Processes input concurrently using a
     * {@link ConcurrentNewColCalculator}. */
    private static void calcNewColsASynchronously(final BufferedDataTable table,
            final ExecutionMonitor subProgress,
            final NewColumnsProducerMapping newColsProducerMapping,
            final DataContainer container)
            throws CanceledExecutionException {
        int finalRowCount = table.getRowCount();
        CellFactory facForProgress = null;
        int workers = Integer.MAX_VALUE;
        int queueSize = Integer.MAX_VALUE;
        Set<CellFactory> newColsFactories = newColsProducerMapping.getUniqueCellFactoryMap().keySet();
        for (CellFactory fac : newColsFactories) {
            if (fac instanceof AbstractCellFactory) {
                AbstractCellFactory acf = (AbstractCellFactory)fac;
                workers = Math.min(workers, acf.getMaxParallelWorkers());
                queueSize = Math.min(queueSize, acf.getMaxQueueSize());
            } else {
                throw new IllegalStateException("Coding problem: This method"
                        + " should not have been called as the cell factories"
                        + " do not allow parallel processing");
            }
            if (facForProgress == null) {
                facForProgress = fac;
            }
        }
        assert facForProgress != null;
        assert workers > 0 : "Nr workers <= 0: " + workers;
        assert queueSize > 0 : "queue size <= 0: " + queueSize;
        ConcurrentNewColCalculator calculator = new ConcurrentNewColCalculator(
                queueSize, workers, container, subProgress, finalRowCount,
                newColsProducerMapping, facForProgress);
        try {
            calculator.run(table);
        } catch (InterruptedException e) {
            CanceledExecutionException cee =
                new CanceledExecutionException(e.getMessage());
            cee.initCause(e);
            throw cee;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                cause = e;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static Set<Class<? extends CellFactory>> codingProblemsCellFactoryClasses;

    /** Calls for an input row the list of cell factories to produce the
     * output row (contains only the new cells, merged later).
     * @param unconvertedRow The input row to be processed
     * @param producerMap For each new (or replaced) column the factory.
     * @return The output row.
     */
    static DataRow calcNewCellsForRow(final DataRow unconvertedRow, final NewColumnsProducerMapping producerMap) {
        final int newColCount = producerMap.getAllNewColumnsList().size();
        DataCell[] newCells = new DataCell[newColCount];
        DataRow row = applyDataTypeConverters(unconvertedRow, producerMap, newCells);
        IdentityHashMap<CellFactory, List<Pair<Integer, Integer>>> uniqueCellFactoryMap =
                producerMap.getUniqueCellFactoryMap();
        for (Map.Entry<CellFactory, List<Pair<Integer, Integer>>> e : uniqueCellFactoryMap.entrySet()) {
            CellFactory factory = e.getKey();
            List<Pair<Integer, Integer>> list = e.getValue();
            DataCell[] fromFac = factory.getCells(row);
            if (fromFac.length != list.size()) {
                String error = String.format("New cells array length conflict: expected %d, actual %d (class %s)",
                                             fromFac.length, list.size(), factory.getClass().getName());
                if (fromFac.length < list.size()) {
                    throw new IndexOutOfBoundsException(error);
                } else {
                    // such problems were ignored until 2.6 -- print warning only
                    if (codingProblemsCellFactoryClasses == null) {
                        codingProblemsCellFactoryClasses = new HashSet<Class<? extends CellFactory>>();
                    }
                    if (codingProblemsCellFactoryClasses.add(factory.getClass())) {
                        LOGGER.coding(error);
                    }
                }
            }
            final int length = list.size();
            for (int i = 0; i < length; i++) {
                Pair<Integer, Integer> indexPair = list.get(i);
                int indexInNewCellsArray = indexPair.getFirst();
                int indexInFactory = indexPair.getSecond();
                assert newCells[indexInNewCellsArray] == null : "New cells array at index expected to be null";
                newCells[indexInNewCellsArray] = fromFac[indexInFactory];
            }
        }
        DataRow appendix = new DefaultRow(row.getKey(), newCells);
        return appendix;
    }

    /** Used when {@link ColumnRearranger#ensureColumnIsConverted(DataCellTypeConverter, int)} is called.
     * It preproccesses the row and replaces the column to be converted by the the result of the given converter.
     * @param row The original input row.
     * @param producerMap The object having the converter list (or not)
     * @param newCells
     * @return The input row if no converter applied or a modified copy of the input row.
     */
    private static DataRow applyDataTypeConverters(final DataRow row,
               final NewColumnsProducerMapping producerMap, final DataCell[] newCells) {
        List<Pair<SpecAndFactoryObject, Integer>> converterToIndexMap = producerMap.getConverterToIndexMap();
        if (!converterToIndexMap.isEmpty()) {
            DataCell[] inputRowCells = new DataCell[row.getNumCells()];
            for (int i = 0; i < inputRowCells.length; i++) {
                inputRowCells[i] = row instanceof BlobSupportDataRow
                        ? ((BlobSupportDataRow)row).getRawCell(i) : row.getCell(i);
            }
            for (Pair<SpecAndFactoryObject, Integer> entry : converterToIndexMap) {
                SpecAndFactoryObject specAndObject = entry.getFirst();
                DataCellTypeConverter converter = specAndObject.getConverter();
                int converterIndex = specAndObject.getConverterIndex();
                Integer index = entry.getSecond();
                DataCell convertedCell = converter.callConvert(row.getCell(converterIndex));
                newCells[index] = convertedCell;
                inputRowCells[converterIndex] = convertedCell;
            }
            return new BlobSupportDataRow(row.getKey(), inputRowCells);
        }
        return row;
    }

    /** Counts for the argument collection the number of unique cell factories.
     * @param facs To count in (length = number of newly created columns)
     * @return The number of unique factories (in most cases just 1)
     */
    static Collection<CellFactory> getUniqueProducerFactories(
            final Collection<SpecAndFactoryObject> facs) {
        IdentityHashMap<CellFactory, Object> counter =
            new IdentityHashMap<CellFactory, Object>();
        for (SpecAndFactoryObject s : facs) {
            CellFactory factory = s.getFactory();
            counter.put(factory, null);
        }
        return counter.keySet();
    }

    /** The list of SpecAndFactoryObject that produce new columns.
     * @param includes All column representations
     * @return The list of data producers.
     */
    static NewColumnsProducerMapping createNewColumnsProducerMapping(final Vector<SpecAndFactoryObject> includes) {
        return new NewColumnsProducerMapping(includes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_reference.getRowCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToFile(final File f, final NodeSettingsWO s, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        NodeSettingsWO subSettings = s.addNodeSettings(CFG_INTERNAL_META);
        subSettings.addInt(CFG_REFERENCE_ID, m_reference.getBufferedTableId());
        subSettings.addIntArray(CFG_MAP, m_map);
        subSettings.addBooleanArray(CFG_FLAGS, m_isFromRefTable);
        if (m_appendTable != null) {
            // subSettings argument is ignored in ContainerTable
            m_appendTable.saveToFile(f, subSettings, exec);
        }
    }

    /**
     * Do not call this method! It's used internally to delete temp files.
     * Any iteration on the table will fail!
     * @see KnowsRowCountTable#clear()
     */
    @Override
    public void clear() {
        if (m_appendTable != null) {
            m_appendTable.clear();
        }
    }

    /** Internal use.
     * {@inheritDoc} */
    @Override
    public void ensureOpen() {
        if (m_appendTable != null) {
            m_appendTable.ensureOpen();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putIntoTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
        if (m_appendTable != null) {
            rep.put(m_appendTable.getBufferID(), m_appendTable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeFromTableRepository(
            final HashMap<Integer, ContainerTable> rep) {
        if (m_appendTable != null) {
            int id = m_appendTable.getBufferID();
            if (rep.remove(id) == null) {
                LOGGER.debug("Failed to remove appended table with id "
                        + id + " from global table repository.");
                return false;
            }
            return true;
        }
        return false;
    }

    /** Creates NoKeyBuffer objects rather then Buffer objects. */
    private static class NoKeyBufferCreator
        extends DataContainer.BufferCreator {

        /** {@inheritDoc} */
        @Override
        Buffer createBuffer(final int rowsInMemory, final int bufferID,
                final Map<Integer, ContainerTable> globalTableRep,
                final Map<Integer, ContainerTable> localTableRep,
                final IWriteFileStoreHandler fileStoreHandler) {
            return new NoKeyBuffer(rowsInMemory, bufferID, globalTableRep,
                    localTableRep, fileStoreHandler);
        }

        /** {@inheritDoc} */
        @Override
        Buffer createBuffer(final File binFile, final File blobDir,
                final File fileStoreDir, final DataTableSpec spec,
                final InputStream metaIn, final int bufID,
                final Map<Integer, ContainerTable> tblRep, final FileStoreHandlerRepository fileStoreHandlerRepository)
            throws IOException {
            return new NoKeyBuffer(binFile, blobDir, spec,
                    metaIn, bufID, tblRep, fileStoreHandlerRepository);
        }
    }

    /** The MultiThreadWorker that processes the input rows concurrently. Only
     * used if the cell factory is an {@link AbstractCellFactory} with
     * parallel processing (
     * {@link AbstractCellFactory#setParallelProcessing(boolean)})
     */
    private static final class ConcurrentNewColCalculator extends MultiThreadWorker<DataRow, DataRow> {

        private final ExecutionMonitor m_subProgress;
        private NewColumnsProducerMapping m_reducedList;
        private DataContainer m_container;
        private final int m_totalRowCount;
        private final CellFactory m_facForProgress;

        /**
         * @param maxQueueSize
         * @param maxActiveInstanceSize
         * @param table
         * @param subProgress
         * @param reducedList
         * @param newColCount
         * @param container */
        private ConcurrentNewColCalculator(final int maxQueueSize,
                final int maxActiveInstanceSize,
                final DataContainer container,
                final ExecutionMonitor subProgress,
                final int totalRowCount,
                final NewColumnsProducerMapping reducedList,
                final CellFactory facForProgress) {
            super(maxQueueSize, maxActiveInstanceSize);
            m_container = container;
            m_subProgress = subProgress;
            m_totalRowCount = totalRowCount;
            m_reducedList = reducedList;
            m_facForProgress = facForProgress;
        }

        /** {@inheritDoc} */
        @Override
        protected DataRow compute(final DataRow in,
                final long index) throws Exception {
            return calcNewCellsForRow(in, m_reducedList);
        }

        /** {@inheritDoc} */
        @Override
        protected void processFinished(final ComputationTask task)
                throws ExecutionException, CancellationException, InterruptedException {
            int r = (int)task.getIndex(); // row count in table is integer
            RowKey key = task.getInput().getKey();
            DataRow append = task.get();  // exception falls through
            m_container.addRowToTable(append);
            m_facForProgress.setProgress(r + 1, m_totalRowCount, key, m_subProgress);
            try {
                m_subProgress.checkCanceled();
            } catch (CanceledExecutionException cee) {
                throw new CancellationException();
            }
        }

    }

    /** A class that helps to distinguish SpecAndFactoryObjects. There are three kinds: representing input columns,
     * created with a cell factory, created with a converter (often molecular type adapter)
     */
    static final class NewColumnsProducerMapping {

        private final List<SpecAndFactoryObject> m_allNewColumnsList;
        private final List<Pair<SpecAndFactoryObject, Integer>> m_converterToIndexMap;
        private final IdentityHashMap<CellFactory, List<Pair<Integer, Integer>>> m_uniqueCellFactoryMap;

        private NewColumnsProducerMapping(final Vector<SpecAndFactoryObject> includes) {
            m_allNewColumnsList = new ArrayList<SpecAndFactoryObject>();
            m_converterToIndexMap = new ArrayList<Pair<SpecAndFactoryObject, Integer>>();
            m_uniqueCellFactoryMap = new IdentityHashMap<CellFactory, List<Pair<Integer, Integer>>>();
            int newColumnIndex = 0;
            for (int i = 0; i < includes.size(); i++) {
                SpecAndFactoryObject s = includes.get(i);
                if (s.isConvertedColumn()) {
                    m_converterToIndexMap.add(new Pair<SpecAndFactoryObject, Integer>(s, newColumnIndex++));
                    m_allNewColumnsList.add(s);
                }
                if (s.isNewColumn()) {
                    CellFactory cellFac = s.getFactory();
                    List<Pair<Integer, Integer>> specAndObs = m_uniqueCellFactoryMap.get(cellFac);
                    if (specAndObs == null) {
                        specAndObs = new ArrayList<Pair<Integer, Integer>>();
                        m_uniqueCellFactoryMap.put(cellFac, specAndObs);
                    }
                    specAndObs.add(new Pair<Integer, Integer>(newColumnIndex++, s.getColumnInFactory()));
                    m_allNewColumnsList.add(s);
                }
            }

        }

        /**
         * @return the converterToIndexMap
         */
        List<Pair<SpecAndFactoryObject, Integer>> getConverterToIndexMap() {
            return m_converterToIndexMap;
        }

        /** @return the number of unique factories (mostly 0 or 1), excluding {@link DatatypeConverter}. */
        int getFactoryCount() {
            return m_uniqueCellFactoryMap.size();
        }

        /** @return The number of new columns (replaced, appended). Includes {@link DatatypeConverter}. */
        int getNewColumnCount() {
            return m_allNewColumnsList.size();
        }

        /** A map that for each cell factory (often only 0 or 1) keeps a list of two-index pairs. The first index
         * is the index in the created new cells array (containing all columns that need to be saved). The second
         * index describes the position of the created cell in the cell factories getCells() method.
         * @return the uniqueCellFactoryMap
         */
        IdentityHashMap<CellFactory, List<Pair<Integer, Integer>>> getUniqueCellFactoryMap() {
            return m_uniqueCellFactoryMap;
        }

        /**
         * @return the allNewColumnsList
         */
        List<SpecAndFactoryObject> getAllNewColumnsList() {
            return m_allNewColumnsList;
        }
    }
}
