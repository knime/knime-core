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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * <code>NodeModel</code> implementation.
 * @author Tobias Koetter, University of Konstanz
 */
public class SubsetMatcherNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SubsetMatcherNodeModel.class);

    /**Processing the subset matching take {@value} times longer than the
     * subset matcher creation.**/
    private static final int SET_PROCESSING_FACTOR = 100;

    private final SettingsModelColumnName m_setIDCol =
        createSetIDColNameModel();

    private final SettingsModelString m_setCol =
        createSetColNameModel();

    private final SettingsModelString m_subsetCol =
        createSubsetColNameModel();

    private final SettingsModelBoolean m_appendSetListCol =
        createAppendSetListColModel();

    /**The internal used data container that is used in the analysis threads.*/
    private BufferedDataContainer m_dc;

    /**The row id variable that is used in the analysis threads.*/
    private final AtomicInteger m_rowId = new AtomicInteger(0);

    /**The number of skipped rows.*/
    private final AtomicInteger m_skipCounter = new AtomicInteger(0);

    /**The number of processed sets.*/
    private final AtomicInteger m_setCounter = new AtomicInteger(0);

    /**Constructor for class ItemSetMatcherNodeModel.*/
    public SubsetMatcherNodeModel() {
        super(2, 1);
    }

    /**
     * @return transaction column name model
     */
    static SettingsModelString createSetColNameModel() {
        return new SettingsModelString("setColumn", null);
    }

    /**
     * @return the item set column model
     */
    static SettingsModelString createSubsetColNameModel() {
        return new SettingsModelString("subsetColumn", null);
    }

    /**
     * @return the transaction id column model.
     */
    static SettingsModelColumnName createSetIDColNameModel() {
        return new SettingsModelColumnName("setIDCol", null);
    }

    /**
     * @return the append item set column model
     */
    static SettingsModelBoolean createAppendSetListColModel() {
        return new SettingsModelBoolean("appendSetListCol", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        final DataTableSpec subsetTableSpec = inSpecs[0];
        checkPresetColumn(subsetTableSpec, m_subsetCol, "Subset");
        final DataTableSpec setTableSpec = inSpecs[1];
        checkPresetColumn(setTableSpec, m_setCol, "Set");
        DataColumnSpec setIDSpec;
        if (!m_setIDCol.useRowID()) {
            final String colName = m_setIDCol.getColumnName();
            if (colName == null || colName.isEmpty()) {
                m_setIDCol.useRowID();
                setWarningMessage("Set id column preset to row id");
            } else {
                if (!setTableSpec.containsName(colName)) {
                    throw new InvalidSettingsException(
                        "Set id column '" + colName
                        + "' does not exist");
                }
            }
            setIDSpec =
                setTableSpec.getColumnSpec(m_setIDCol.getColumnName());
        } else {
            setIDSpec = null;
        }
        if (!m_setIDCol.useRowID()
                && !setTableSpec.containsName(m_setCol.getStringValue())) {
            throw new InvalidSettingsException("Set column '"
                    + m_setCol.getStringValue() + "' not found");
        }
        final DataColumnSpec setListSpec =
            setTableSpec.getColumnSpec(m_setCol.getStringValue());
        final DataColumnSpec subsetSpec =
            subsetTableSpec.getColumnSpec(m_subsetCol.getStringValue());
        if (setListSpec == null || subsetSpec == null) {
            throw new InvalidSettingsException("No set and/or"
                + " subset column are selected.");
        }
        final DataType setType =
            setListSpec.getType().getCollectionElementType();
        final DataType subsetType =
            subsetSpec.getType().getCollectionElementType();
        if (setType == null || subsetType == null) {
            throw new InvalidSettingsException("Set and/or"
                + " subset column are not a collection.");
        }
        if (!setType.equals(subsetType)) {
            throw new InvalidSettingsException("Set and "
                    + "subset column have to be of the same type.");
        }
        final DataTableSpec resultSpec = createTableSpec(setIDSpec,
                setListSpec, subsetSpec, m_appendSetListCol.getBooleanValue());
        return new DataTableSpec[] {resultSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable subsetTable = inData[0];
        final DataTableSpec subsetTableSpec = subsetTable.getSpec();
        final int subsetColIdx =
                subsetTableSpec.findColumnIndex(m_subsetCol.getStringValue());
        //the comparator that should be used to sort the subset AND the
        //set list
        final Comparator<DataCell> comparator = subsetTableSpec.getColumnSpec(
                subsetColIdx).getType().getComparator();

        final BufferedDataTable setTable = inData[1];
        final DataTableSpec setTableSpec = setTable.getSpec();
        final int setIDColIdx;
        final DataColumnSpec setIDSpec;
        if (m_setIDCol.useRowID()) {
            setIDColIdx = -1;
            setIDSpec = null;
        } else {
            setIDColIdx = setTableSpec.findColumnIndex(m_setIDCol
                    .getStringValue());
            setIDSpec = setTableSpec.getColumnSpec(setIDColIdx);
        }
        final int transColIdx =
            setTableSpec.findColumnIndex(m_setCol.getStringValue());
        final boolean appendSetCol =
            m_appendSetListCol.getBooleanValue();

        //create the data container
        final DataTableSpec resultSpec =
            createTableSpec(setIDSpec, setTableSpec.getColumnSpec(transColIdx),
                    subsetTableSpec.getColumnSpec(subsetColIdx), appendSetCol);
        m_dc = exec.createDataContainer(resultSpec);

        final int subsetRowCount = subsetTable.getRowCount();
        if (subsetRowCount == 0) {
            setWarningMessage("Empty subset table found");
            m_dc.close();
            return new BufferedDataTable[] {m_dc.getTable()};
        }
        final int setRowCount = setTable.getRowCount();
        if (setRowCount == 0) {
            setWarningMessage("Empty set table found");
            m_dc.close();
            return new BufferedDataTable[] {m_dc.getTable()};
        }
        final double totalRowCount = subsetRowCount
                                        + setRowCount * SET_PROCESSING_FACTOR;
        final ExecutionMonitor subsetExec =
            exec.createSubProgress(subsetRowCount / totalRowCount);

        //create the rule model
        exec.setMessage("Generating subset base...");
        final SubsetMatcher[] sortedMatcher = createSortedMatcher(subsetExec,
                subsetTable, subsetColIdx, comparator);
        subsetExec.setProgress(1.0);
        if (sortedMatcher.length < 1) {
            setWarningMessage("No item sets found");
            m_dc.close();
            return new BufferedDataTable[] {m_dc.getTable()};
        }

        final ExecutionMonitor setExec = exec.createSubProgress(
                (setRowCount * SET_PROCESSING_FACTOR) / totalRowCount);
        //create the matching processes
        exec.setMessage("Processing sets... ");
        // initialize the thread pool for parallelization of the set
        // analysis
        final ThreadPool pool =
                KNIMEConstants.GLOBAL_THREAD_POOL.createSubPool();
        for (final DataRow row : setTable) {
            exec.checkCanceled();
            DataCell setIDCell;
            if (setIDColIdx < 0) {
                final RowKey key = row.getKey();
                setIDCell = new StringCell(key.getString());
            } else {
                setIDCell = row.getCell(setIDColIdx);
            }
            final DataCell setCell =
                row.getCell(transColIdx);
            if (!(setCell instanceof CollectionDataValue)) {
                setExec.setProgress(m_setCounter.incrementAndGet()
                        / (double)setRowCount);
                m_skipCounter.incrementAndGet();
                continue;
            }
            final CollectionDataValue setList =
                (CollectionDataValue)setCell;
            if (setList.size() < 1) {
                //skip empty sets
                setExec.setProgress(m_setCounter.incrementAndGet()
                        / (double)setRowCount);
                m_skipCounter.incrementAndGet();
                continue;
            }
            // submit for each set a job in the thread pool
            pool.enqueue(createRunnable(setExec, setRowCount, setIDCell,
                    setList, appendSetCol, comparator, sortedMatcher));
        }
        // wait until all jobs are finished before closing the container
        // and returning the method
        pool.waitForTermination();
        exec.setMessage("Creating data table...");
        m_dc.close();
        if (m_skipCounter.intValue() > 0) {
            setWarningMessage("No matching subsets found for " + m_skipCounter
                    + " out of " + setRowCount + " sets");
        }
        exec.setProgress(1.0);
        return new BufferedDataTable[]{m_dc.getTable()};
    }


    private Runnable createRunnable(final ExecutionMonitor exec,
            final int noOfSets, final DataCell setIDCell,
            final CollectionDataValue setCell, final boolean appendSetCol,
            final Comparator<DataCell> comparator,
            final SubsetMatcher[] sortedMatcher) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    //check cancel prior updating the global counter!!!
                    exec.checkCanceled();
                    final int transCounter =
                        m_setCounter.incrementAndGet();
                    exec.setMessage(setIDCell.toString() + " ("
                            + transCounter + " of "
                            + noOfSets + ")");
                    if (setCell.size() <= 0) {
                        //skip empty collections
                        exec.setProgress(
                                transCounter / (double)noOfSets);
                        m_skipCounter.incrementAndGet();
                        return;
                    }
                    final DataCell[] sortedItems = collectionCell2SortedArray(
                            setCell, comparator);
                    if (sortedItems.length < 1) {
                        exec.setProgress(
                                transCounter / (double)noOfSets);
                        m_skipCounter.incrementAndGet();
                        return;
                    }
                    //try to match the sorted transaction items and the sorted
                    //matcher until all items or all matchers are processed
                    int matcherStartIdx = 0;
                    int itemIdx = 0;
                    final Collection<DataCell> matchingSets =
                        new LinkedList<DataCell>();
                    while (itemIdx < sortedItems.length
                            && matcherStartIdx < sortedMatcher.length) {
                        final DataCell subItem = sortedItems[itemIdx];
                        //match the current item with all remaining matchers
                        for (int i = matcherStartIdx;
                                i < sortedMatcher.length; i++) {
                            final SubsetMatcher matcher = sortedMatcher[i];
                            final int result = matcher.compare(subItem);
                            if (result > 0) {
                                //the smallest matcher is bigger then this item
                                //exit the loop and continue with the next item
                                break;
                            } else if (result == 0) {
                                matcher.match(sortedItems, itemIdx,
                                    matchingSets, new LinkedList<DataCell>());
                            }
                            //this matcher has matched this time
                            //                  or
                            //the subItem is bigger than the matcher thus all
                            //subsequent items will be bigger as well
                            //-> start the next time with the next child matcher
                            matcherStartIdx++;
                        }
                        //go to the next index
                        itemIdx++;
                    }
                    if (matchingSets.size() < 1) {
                        exec.setProgress(
                                transCounter / (double)noOfSets);
                        m_skipCounter.incrementAndGet();
                        return;
                    }
                    for (final DataCell matchingSet : matchingSets) {
                        exec.checkCanceled();
                        //create for each matching subset a result row
                        final List<DataCell> cells = new LinkedList<DataCell>();
                        cells.add(setIDCell);
                        if (appendSetCol) {
                            cells.add((DataCell)setCell);
                        }
                        //the subset column
                        cells.add(matchingSet);
                        final RowKey rowKey =
                            RowKey.createRowKey(m_rowId.getAndIncrement());
                        final DefaultRow row = new DefaultRow(rowKey, cells);
                        synchronized (m_dc) {
                            exec.checkCanceled();
                            m_dc.addRowToTable(row);
                        }
                    }
                    exec.setProgress(transCounter / (double)noOfSets);
                } catch (final CanceledExecutionException e) {
                    // ignore this just exit the run method
                } catch (final Exception e) {
                    LOGGER.error("Exception while matching sub sets: "
                            + e.getMessage());
                }
            }
        };
    }

    private SubsetMatcher[] createSortedMatcher(
            final ExecutionMonitor exec, final BufferedDataTable table,
            final int colIdx, final Comparator<DataCell> comparator)
            throws CanceledExecutionException {
        final Map<DataCell, SubsetMatcher> map =
            new HashMap<DataCell, SubsetMatcher>();
        final int rowCount = table.getRowCount();
        if (rowCount < 1) {
            return new SubsetMatcher[0];
        }
        int counter = 1;
        for (final DataRow row : table) {
            exec.checkCanceled();
            exec.setProgress(counter / (double) rowCount,
                    "Processing subset " + counter + " of " + rowCount);
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
            SubsetMatcher matcher = map.get(rootItem);
            if (matcher == null) {
                matcher = new SubsetMatcher(rootItem, comparator);
                map.put(rootItem, matcher);
            }
            matcher.appendChildMatcher(itemSet, 1);
            counter++;
        }
        final ArrayList<SubsetMatcher> matchers =
            new ArrayList<SubsetMatcher>(map.values());
        Collections.sort(matchers);
        return matchers.toArray(new SubsetMatcher[0]);
    }

    /**
     * The table specification has the following structure:
     * <ol>
     * <li>set id</li>
     * <li>matching subset list(optional)</li>
     * <li>set</li>
     * </ol>.
     * @param setIDCol the set id column spec
     * @param setCol the set column spec
     * @param subsetCol the subset column spec
     * @param appendSetListCol <code>true</code> if the set list
     * should be appended
     * @return the {@link DataTableSpec}
     */
    private DataTableSpec createTableSpec(final DataColumnSpec setIDCol,
            final DataColumnSpec setCol, final DataColumnSpec subsetCol,
            final boolean appendSetListCol) {
        final List<DataColumnSpec> specs = new LinkedList<DataColumnSpec>();
        if (setIDCol != null) {
            specs.add(setIDCol);
        } else {
            final DataColumnSpecCreator specCreator =
                new DataColumnSpecCreator("ID", StringCell.TYPE);
            specs.add(specCreator.createSpec());
        }
        if (appendSetListCol) {
            if (subsetCol != null && setCol != null
                    && subsetCol.getName().equals(setCol.getName())) {
                //check for a duplicate name
                final DataColumnSpecCreator creator =
                    new DataColumnSpecCreator(setCol);
                creator.setName(setCol.getName() + "#1");
                specs.add(creator.createSpec());
            } else {
                specs.add(setCol);
            }
        }
        specs.add(subsetCol);
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
        }
        if (model.getStringValue() == null
                || model.getStringValue().isEmpty()) {
            throw new InvalidSettingsException(
                    "No collection column found in "
                    + colType.toLowerCase() + " table");
        }
        if (!spec.containsName(model.getStringValue())) {
            throw new InvalidSettingsException(
                    "Column '" + model.getStringValue() + "' does not exist in "
                    + colType.toLowerCase() + " table");
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
        m_setCol.validateSettings(settings);
        m_subsetCol.validateSettings(settings);
        m_setIDCol.validateSettings(settings);
        m_appendSetListCol.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_setCol.loadSettingsFrom(settings);
        m_subsetCol.loadSettingsFrom(settings);
        m_setIDCol.loadSettingsFrom(settings);
        m_appendSetListCol.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_setCol.saveSettingsTo(settings);
        m_subsetCol.saveSettingsTo(settings);
        m_setIDCol.saveSettingsTo(settings);
        m_appendSetListCol.saveSettingsTo(settings);
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
        m_setCounter.set(0);
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
