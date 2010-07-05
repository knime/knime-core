/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 */

package org.knime.base.node.preproc.matcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.ThreadPool;


/**
 * <code>NodeModel</code> implementation.
 * @author Tobias Koetter, University of Konstanz
 */
public class SubsetMatcherNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SubsetMatcherNodeModel.class);

    private final SettingsModelColumnName m_transactionIDCol =
        createTransactionIDColNameModel();

    private final SettingsModelString m_transactionCol =
        createTransactionColNameModel();

    private final SettingsModelString m_itemSetCol =
        createItemSetColNameModel();

    private final SettingsModelBoolean m_appendTransactionListCol =
        createAppendTransactionListColModel();

    /**The internal used data container that is used in the analysis threads.*/
    private BufferedDataContainer m_dc;

    /**The row id variable that is used in the analysis threads.*/
    private final AtomicInteger m_rowId = new AtomicInteger(0);

    /**The number of skipped rows.*/
    private final AtomicInteger m_skipCounter = new AtomicInteger(0);
    
    /**The number of processed transactions.*/
    private final AtomicInteger m_transactionCounter = new AtomicInteger(0);

    /**Constructor for class ItemSetMatcherNodeModel.*/
    public SubsetMatcherNodeModel() {
        super(2, 1);
    }

    /**
     * @return transaction column name model
     */
    static SettingsModelString createTransactionColNameModel() {
        return new SettingsModelString("transactionColumn", null);
    }

    /**
     * @return the item set column model
     */
    static SettingsModelString createItemSetColNameModel() {
        return new SettingsModelString("itemSetColumn", null);
    }

    /**
     * @return the transaction id column model.
     */
    static SettingsModelColumnName createTransactionIDColNameModel() {
        return new SettingsModelColumnName("transactionIDCol", null);
    }

    /**
     * @return the append item set column model
     */
    static SettingsModelBoolean createAppendTransactionListColModel() {
        return new SettingsModelBoolean("appendTransactionListCol", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        final DataTableSpec setSpec = inSpecs[0];
        checkPresetColumn(setSpec, m_itemSetCol, "Item set");
        final DataTableSpec transSpec = inSpecs[1];
        checkPresetColumn(transSpec, m_transactionCol, "Transaction");
        DataColumnSpec transactionIDSpec;
        if (!m_transactionIDCol.useRowID()) {
            final String colName = m_transactionIDCol.getColumnName();
            if (colName == null || colName.isEmpty()) {
                m_transactionIDCol.useRowID();
                setWarningMessage("Transaction id column preset to row id");
            } else {
                if (!transSpec.containsName(colName)) {
                    throw new InvalidSettingsException(
                        "Transaction id column does not exist");
                }
            }
            transactionIDSpec =
                transSpec.getColumnSpec(m_transactionIDCol.getColumnName());
        } else {
            transactionIDSpec = null;
        }
        final DataColumnSpec transactionListSpec =
            transSpec.getColumnSpec(m_transactionCol.getStringValue());
        final DataColumnSpec itemSetSpec =
            setSpec.getColumnSpec(m_itemSetCol.getStringValue());
        if (transactionListSpec == null || itemSetSpec == null) {
            throw new InvalidSettingsException("No transaction and/or"
                + " itemset column are selected.");
        }
        DataType transType = 
            transactionListSpec.getType().getCollectionElementType();
        DataType itemsetType = itemSetSpec.getType().getCollectionElementType();
        if (transType == null || itemsetType == null) {
            throw new InvalidSettingsException("Transaction and/or"
                + " itemset column are not a collection.");
        }
        if (!transType.equals(itemsetType)) {
            throw new InvalidSettingsException("Transaction and "
                    + "item set column have to be of the same type.");
        }
        final DataTableSpec resultSpec = createTableSpec(transactionIDSpec,
                transactionListSpec, itemSetSpec,
                m_appendTransactionListCol.getBooleanValue());
        return new DataTableSpec[] {resultSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable setTable = inData[0];
        final DataTableSpec setSpec = setTable.getSpec();
        final int setColIdx =
                setSpec.findColumnIndex(m_itemSetCol.getStringValue());
        //the comparator that should be used to sort the item set AND the
        //transaction list
        final Comparator<DataCell> comparator =
            setSpec.getColumnSpec(setColIdx).getType().getComparator();
        final BufferedDataTable transTable = inData[1];
        final DataTableSpec transSpec = transTable.getSpec();
        final int transIDColIdx;
        final DataColumnSpec idSpec;
        if (m_transactionIDCol.useRowID()) {
            transIDColIdx = -1;
            idSpec = null;
        } else {
            transIDColIdx = transSpec.findColumnIndex(m_transactionIDCol
                    .getStringValue());
            idSpec = transSpec.getColumnSpec(transIDColIdx);
        }
        final int transColIdx =
            transSpec.findColumnIndex(m_transactionCol.getStringValue());
        final boolean appendTransCol =
            m_appendTransactionListCol.getBooleanValue();

        //create the data container
        final DataTableSpec resultSpec =
            createTableSpec(idSpec, transSpec.getColumnSpec(transColIdx),
                    setSpec.getColumnSpec(setColIdx), appendTransCol);
        m_dc = exec.createDataContainer(resultSpec);

        final int setRowCount = setTable.getRowCount();
        if (setRowCount == 0) {
            setWarningMessage("Empty subset table found");
            m_dc.close();
            return new BufferedDataTable[] {m_dc.getTable()};
        }
        final int transRowCount = transTable.getRowCount();
        if (setRowCount == 0) {
            setWarningMessage("Empty set table found");
            m_dc.close();
            return new BufferedDataTable[] {m_dc.getTable()};
        }
        final double totalRowCount = setRowCount + transRowCount;
        final ExecutionMonitor setExec =
            exec.createSubProgress(setRowCount / totalRowCount);
        final ExecutionMonitor transExec =
            exec.createSubProgress(transRowCount / totalRowCount);

        //create the rule model
        exec.setMessage("Generating subset base...");
        final Map<DataCell, SubsetMatcher> matcherMap =
            createMatcherMap(setExec, setTable, setColIdx, comparator);
        setExec.setProgress(1.0);
        if (matcherMap.isEmpty()) {
            setWarningMessage("No item sets found");
            m_dc.close();
            return new BufferedDataTable[] {m_dc.getTable()};
        }

        //create the matching processes
        exec.setMessage("Processing sets... ");
        // initialize the thread pool for parallelization of the transaction
        // analysis
        final ThreadPool pool =
                KNIMEConstants.GLOBAL_THREAD_POOL.createSubPool();
        for (final DataRow row : transTable) {
            DataCell idCell;
            if (transIDColIdx < 0) {
                final RowKey key = row.getKey();
                idCell = new StringCell(key.getString());
            } else {
                idCell = row.getCell(transIDColIdx);
            }
            final DataCell transactionCell =
                row.getCell(transColIdx);
            if (!(transactionCell instanceof CollectionDataValue)) {
                transExec.setProgress(m_transactionCounter.incrementAndGet()
                        / (double)transRowCount);
                m_skipCounter.incrementAndGet();
                continue;
            }
            final CollectionDataValue transactionList =
                (CollectionDataValue)transactionCell;
            if (transactionList.size() < 1) {
                //skip empty transaction lists
                transExec.setProgress(m_transactionCounter.incrementAndGet()
                        / (double)transRowCount);
                m_skipCounter.incrementAndGet();
                continue;
            }
            // submit for each transaction a job in the thread pool
            pool.enqueue(createRunnable(transExec, transRowCount, idCell,
                    transactionList, appendTransCol, comparator, matcherMap));
        }
        // wait until all jobs are finished before closing the container
        // and returning the method
        pool.waitForTermination();
        exec.setMessage("Creating data table...");
        m_dc.close();
        if (m_skipCounter.intValue() > 0) {
            setWarningMessage("No matching subsets found for " + m_skipCounter
                    + " of " + transRowCount + " sets");
        }
        exec.setProgress(1.0);
        return new BufferedDataTable[]{m_dc.getTable()};
    }


    private Runnable createRunnable(final ExecutionMonitor exec,
            final int noOfTransactions, final DataCell idCell,
            final CollectionDataValue transactionCell,
            final boolean appendTransCol, final Comparator<DataCell> comparator,
            final Map<DataCell, SubsetMatcher> matcherMap) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    //check cancel prior updating the global counter!!!
                    exec.checkCanceled();
                    final int transCounter =
                        m_transactionCounter.incrementAndGet();
                    exec.setMessage(idCell.toString() + " "
                            + transCounter + " of "
                            + noOfTransactions);
                    if (transactionCell.size() <= 0) {
                        //skip empty collections
                        exec.setProgress(
                                transCounter / (double)noOfTransactions);
                        m_skipCounter.incrementAndGet();
                        return;
                    }
                    final DataCell[] items = collectionCell2SortedArray(
                            transactionCell, comparator);
                    if (items.length < 1) {
                        exec.setProgress(
                                transCounter / (double)noOfTransactions);
                        m_skipCounter.incrementAndGet();
                        return;
                    }
                    int idx = 0;
                    //try to find all matching matcher for the given
                    //transaction if any
                    final Collection<DataCell> subSets =
                        new LinkedList<DataCell>();
                    while (idx < items.length) {
                        final DataCell item = items[idx];
                        final SubsetMatcher matcher = matcherMap.get(item);
                        if (matcher != null) {
                            matcher.match(items, idx, subSets,
                                    new LinkedList<DataCell>());
                        }
                        idx++;
                    }
                    if (subSets.size() < 1) {
                        exec.setProgress(
                                transCounter / (double)noOfTransactions);
                        m_skipCounter.incrementAndGet();
                        return;
                    }
                    for (final DataCell itemSet : subSets) {
                        exec.checkCanceled();
                        //create for each item set a result row
                        final List<DataCell> cells = new LinkedList<DataCell>();
                        cells.add(idCell);
                        if (appendTransCol) {
                            cells.add((DataCell)transactionCell);
                        }
                        //the item set column
                        cells.add(itemSet);
                        final RowKey rowKey =
                            RowKey.createRowKey(m_rowId.getAndIncrement());
                        final DefaultRow row = new DefaultRow(rowKey, cells);
                        synchronized (m_dc) {
                            m_dc.addRowToTable(row);
                        }
                    }
                    exec.setProgress(transCounter / (double)noOfTransactions);
                } catch (final CanceledExecutionException e) {
                    // ignore this just exit the run method
                } catch (final Exception e) {
                    LOGGER.error("Exception while matching sub sets: "
                            + e.getMessage());
                }
            }
        };
    }

    private Map<DataCell, SubsetMatcher> createMatcherMap(
            final ExecutionMonitor exec, final BufferedDataTable table,
            final int colIdx, final Comparator<DataCell> comparator)
            throws CanceledExecutionException {
        final Map<DataCell, SubsetMatcher> map =
            new HashMap<DataCell, SubsetMatcher>();
        final int rowCount = table.getRowCount();
        if (rowCount < 1) {
            return map;
        }
        int counter = 1;
        for (final DataRow row : table) {
            exec.checkCanceled();
            exec.setProgress(counter / (double) rowCount,
                    "Processing item set " + counter + " of " + rowCount);
            final DataCell cell =
                row.getCell(colIdx);
            if (!(cell instanceof CollectionDataValue)) {
                //skip missing cells and none collection cells
                continue;
            }
            final CollectionDataValue collectionCell =
                (CollectionDataValue)cell;
            if (collectionCell.size() <= 0) {
                //skip empty collections
                continue;
            }
            final DataCell[] itemSet =
                collectionCell2SortedArray(collectionCell, comparator);
            final DataCell rootItem = itemSet[0];
            SubsetMatcher rootMatcher = map.get(rootItem);
            if (rootMatcher == null) {
                rootMatcher = new SubsetMatcher(rootItem, comparator);
                map.put(rootItem, rootMatcher);
            }
            rootMatcher.appendChildMatcher(itemSet, 1);
            counter++;
        }
        return map;
    }

    /**
     * The table specification has the following structure:
     * <ol>
     * <li>transaction id</li>
     * <li>transaction list (optional)</li>
     * <li>item set</li>
     * </ol>.
     * @param idCol the transaction id column spec
     * @param listCol the transaction list column spec
     * @param itemSetCol the item set column spec
     * @param appendListCol <code>true</code> if the transaction id list
     * should be appended
     * @return the {@link DataTableSpec}
     */
    private DataTableSpec createTableSpec(final DataColumnSpec idCol,
            final DataColumnSpec listCol, final DataColumnSpec itemSetCol,
            final boolean appendListCol) {
        final List<DataColumnSpec> specs = new LinkedList<DataColumnSpec>();
        if (idCol != null) {
            specs.add(idCol);
        } else {
            final DataColumnSpecCreator specCreator =
                new DataColumnSpecCreator("ID", StringCell.TYPE);
            specs.add(specCreator.createSpec());
        }
        if (appendListCol) {
            if (itemSetCol != null && listCol != null
                    && itemSetCol.getName().equals(listCol.getName())) {
                //check for a duplicate name
                final DataColumnSpecCreator creator =
                    new DataColumnSpecCreator(listCol);
                creator.setName(listCol.getName() + "#1");
                specs.add(creator.createSpec());
            } else {
                specs.add(listCol);
            }
        }
        specs.add(itemSetCol);
        return new DataTableSpec(specs.toArray(new DataColumnSpec[0]));
    }

    private void checkPresetColumn(final DataTableSpec spec,
            final SettingsModelString model, final String colType)
            throws InvalidSettingsException {
        final String colName = model.getStringValue();
        if (colName == null || colName.isEmpty()) {
            for (final DataColumnSpec colSpec : spec) {
                if (colSpec.getType().isCollectionType()) {
                    model.setStringValue(colSpec.getName());
                    break;
                }
            }
            setWarningMessage(colType + " column preset to "
                    + model.getStringValue());
        } else {
            if (!spec.containsName(colName)) {
                throw new InvalidSettingsException(
                        colType + " column does not exist");
            }
        }
    }

    /**
     * @param cell the {@link CollectionDataValue} to sort
     * @param comparator the comparator to use
     * @return the array that contains the values of the given cell
     * sorted by the given comparator
     */
    static DataCell[] collectionCell2SortedArray(
            final CollectionDataValue cell,
            final Comparator<DataCell> comparator) {
        final DataCell[] array = new DataCell[cell.size()];
        int idx = 0;
        for (final DataCell itemCell : cell) {
            array[idx++] = itemCell;
        }
        //sort the array with the given comparator
        Arrays.sort(array, comparator);
        return array;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_transactionCol.validateSettings(settings);
        m_itemSetCol.validateSettings(settings);
        m_transactionIDCol.validateSettings(settings);
        m_appendTransactionListCol.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_transactionCol.loadSettingsFrom(settings);
        m_itemSetCol.loadSettingsFrom(settings);
        m_transactionIDCol.loadSettingsFrom(settings);
        m_appendTransactionListCol.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_transactionCol.saveSettingsTo(settings);
        m_itemSetCol.saveSettingsTo(settings);
        m_transactionIDCol.saveSettingsTo(settings);
        m_appendTransactionListCol.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //reset the data container
        m_dc = null;
        m_rowId.set(0);
        m_skipCounter.set(0);
        m_transactionCounter.set(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        //nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }
}
