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
 *    17.10.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.ungroup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class UngroupNodeModel extends NodeModel {

    /**The config key of the collections column names.*/
    private static final String CFG_COL_NAMES = "columnNames";

    private final SettingsModelColumnFilter2 m_collCols =
            createCollectionColsModel();

    private final SettingsModelString m_columnName =
        createColumnModel();

    private final SettingsModelBoolean m_removeCollectionCol =
        createRemoveCollectionColModel();

    private final SettingsModelBoolean m_skipMissingVal =
        createSkipMissingValModel();

    private final SettingsModelBoolean m_enableHilite =
        createEnableHiliteModel();

    /**
     * Node returns a new hilite handler instance.
     */
    private HiLiteTranslator m_trans = null;

    private final HiLiteHandler m_hilite = new HiLiteHandler();

    /**Constructor for class AppenderNodeModel.
     *
     */
    public UngroupNodeModel() {
        super(1, 1);
    }

    /**
     * @return the collection columns model
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createCollectionColsModel() {
        return new SettingsModelColumnFilter2(CFG_COL_NAMES, CollectionDataValue.class);
    }

    /**
     * @return the enable hilite translation model
     */
    static SettingsModelBoolean createEnableHiliteModel() {
        return new SettingsModelBoolean("enableHilite", false);
    }

    /**
     * @return the ignore missing value model
     */
    static SettingsModelBoolean createSkipMissingValModel() {
        return new SettingsModelBoolean("skipMissingValues", false);
    }

    /**
     * @return the remove collection column model
     */
    static SettingsModelBoolean createRemoveCollectionColModel() {
        return new SettingsModelBoolean("removeCollectionCol", true);
    }

    /**
     * @return the column name settings model
     */
    private static SettingsModelString createColumnModel() {
        return new SettingsModelString("columnName", null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        final DataTableSpec spec = inSpecs[0];
        final DataTableSpec resultSpec = compatibleCreateResultSpec(spec);
        return new DataTableSpec[] {resultSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        if (inData == null || inData.length != 1) {
            throw new InvalidSettingsException("Invalid input data");
        }
        final BufferedDataTable table = inData[0];
        int[] colIdxs = compatibleGetSelectedColIds(table);
        if (colIdxs == null || colIdxs.length <= 0) {
            setWarningMessage("No ungroup column selected. Node returns input table.");
            return inData;
        }
        final boolean removeCollectionCol =
            m_removeCollectionCol.getBooleanValue();
        final boolean skipMissingVals = m_skipMissingVal.getBooleanValue();
        final boolean enableHilite = m_enableHilite.getBooleanValue();
        final Map<RowKey, Set<RowKey>> hiliteMapping =
            new HashMap<RowKey, Set<RowKey>>();
        final DataTableSpec newSpec = compatibleCreateResultSpec(table.getDataTableSpec());
        final BufferedDataContainer dc = exec.createDataContainer(newSpec);
        if (table.getRowCount() == 0) {
            dc.close();
            return new BufferedDataTable[] {dc.getTable()};
        }
        @SuppressWarnings("unchecked")
        Iterator<DataCell>[] iterators = new Iterator[colIdxs.length];
        final DataCell[] missingCells = new DataCell[colIdxs.length];
        Arrays.fill(missingCells, DataType.getMissingCell());
        final int totalRowCount = table.getRowCount();
        final double progressPerRow = 1.0 / totalRowCount;
        int rowCounter = 0;
        for (final DataRow row : table) {
            rowCounter++;
            exec.checkCanceled();
            exec.setProgress(rowCounter * progressPerRow,
                    "Processing row " + rowCounter + " of " + totalRowCount);
            boolean allMissing = true;
            for (int i = 0, length = colIdxs.length; i < length; i++) {
                final DataCell cell = row.getCell(colIdxs[i]);
                        final  CollectionDataValue listCell;
                final Iterator<DataCell> iterator;
                if (cell instanceof  CollectionDataValue) {
                    listCell = (CollectionDataValue)cell;
                    iterator = listCell.iterator();
                    allMissing = false;
                } else {
                    iterator = null;
                }
                iterators[i] = iterator;
            }
            if (allMissing) {
                //all collection column cells are missing cells append a row
                //with missing cells as well if the skip missing value option is disabled
                if (!skipMissingVals) {
                    final DefaultRow newRow =
                            createClone(row.getKey(), row, colIdxs,
                                        removeCollectionCol, missingCells);
                    if (enableHilite) {
                        //create the hilite entry
                        final Set<RowKey> keys = new HashSet<RowKey>(1);
                        keys.add(row.getKey());
                        hiliteMapping.put(row.getKey(), keys);
                    }
                    dc.addRowToTable(newRow);
                }
                continue;
            }
            int counter = 1;
            final Set<RowKey> keys;
            if (enableHilite) {
                keys = new HashSet<RowKey>();
            } else {
                keys = null;
            }
            boolean continueLoop = false;
            boolean allEmpty = true;
            do {
                //reset the loop flag
                allMissing = true;
                continueLoop = false;
                final DataCell[] newCells = new DataCell[iterators.length];
                for (int i = 0, length = iterators.length; i < length; i++) {
                    Iterator<DataCell> iterator = iterators[i];
                    DataCell newCell;
                    if (iterator != null && iterator.hasNext()) {
                        allEmpty = false;
                        continueLoop = true;
                        newCell = iterator.next();
                    } else {
                        if (iterator == null) {
                            allEmpty = false;
                        }
                        newCell = DataType.getMissingCell();
                    }
                    if (!newCell.isMissing()) {
                        allMissing = false;
                    }
                    newCells[i] = newCell;
                }
                if (!allEmpty && !continueLoop) {
                    break;
                }
                if (!allEmpty && allMissing && skipMissingVals) {
                    continue;
                }
                final RowKey oldKey = row.getKey();
                final RowKey newKey = new RowKey(oldKey.getString()
                                                 + "_" + counter++);
                final DefaultRow newRow = createClone(newKey, row, colIdxs,
                                                  removeCollectionCol, newCells);
                dc.addRowToTable(newRow);
                if (keys != null) {
                    keys.add(newKey);
                }
            } while(continueLoop);
            if (keys != null && !keys.isEmpty()) {
                hiliteMapping.put(row.getKey(), keys);
            }
        }
        dc.close();
        if (enableHilite) {
            m_trans.setMapper(
                    new DefaultHiLiteMapper(hiliteMapping));
        }
        return new BufferedDataTable[] {dc.getTable()};
    }

    private DefaultRow createClone(final RowKey newKey, final DataRow row,
            final int[] colIdxs, final boolean removeCollectionCol,
            final DataCell[] newCells) {
        assert colIdxs.length == newCells.length;
        final Map<Integer, DataCell> map = new HashMap<Integer, DataCell>(newCells.length);
        for (int i = 0, length = newCells.length; i < length; i++) {
            map.put(Integer.valueOf(colIdxs[i]), newCells[i]);
        }
        final int cellCount;
        if (removeCollectionCol) {
            cellCount = row.getNumCells();
        } else {
            cellCount = row.getNumCells() + colIdxs.length;
        }
        final DataCell[] cells = new DataCell[cellCount];
        int cellIdx = 0;
        int newCellidx = 0;
        for (int i = 0, length = row.getNumCells(); i < length; i++) {
            if (map.containsKey(Integer.valueOf(i))) {
                if (!removeCollectionCol) {
                    cells[cellIdx++] = row.getCell(i);
                }
                cells[cellIdx++] = newCells[newCellidx++];
            } else {
                cells[cellIdx++] = row.getCell(i);
            }
        }
        return new DefaultRow(newKey, cells);
    }

    private int[] getSelectedColIdxs(final DataTableSpec spec,
            final String... colNames)
    throws InvalidSettingsException {
        final int[] idxs = new int[colNames.length];
        for (int i = 0, length = colNames.length; i < length; i++) {
            final String name = colNames[i];
            idxs[i] = spec.findColumnIndex(name);
            if (idxs[i] < 0) {
                throw new InvalidSettingsException("Column with name "
                        + name + " not found in input table");
            }
        }
        return idxs;
    }

    /**
     * @param spec original spec
     * @param removeCollectionCol <code>true</code> if the collection
     * column should be removed
     * @param colNames the collection column names
     * @return the new spec
     * @throws InvalidSettingsException if an exception occurs
     */
    private static DataTableSpec createTableSpec(final DataTableSpec spec,
            final boolean removeCollectionCol, final String... colNames)
            throws InvalidSettingsException {
        if (colNames == null || colNames.length <= 0) {
            //the user has not selected any column
            return spec;
        }
        final Collection<DataColumnSpec> specs =
                new LinkedList<DataColumnSpec>();
        final Map<String, DataType> collectionColsMap =
                new LinkedHashMap<String, DataType>(colNames.length);
        for (final String colName : colNames) {
            final int index = spec.findColumnIndex(colName);
            if (index < 0) {
                throw new InvalidSettingsException(
                                       "Invalid column name '" + colName + "'");
            }
            final DataColumnSpec colSpec = spec.getColumnSpec(index);
            final DataType type = colSpec.getType();
            final DataType basicType = type.getCollectionElementType();
            if (basicType == null) {
                throw new InvalidSettingsException("Column '" + colName + "' is not of collection type");
            }
            collectionColsMap.put(colName, basicType);
        }
        final DataColumnSpecCreator specCreator =
                new DataColumnSpecCreator("dummy", StringCell.TYPE);
        for (final DataColumnSpec origColSpec: spec) {
            final String origColName = origColSpec.getName();
            final DataType resultType = collectionColsMap.get(origColName);
            if (resultType != null) {
                if (!removeCollectionCol) {
                    specs.add(origColSpec);
                    specCreator.setName(DataTableSpec.getUniqueColumnName(spec, origColName));
                } else {
                    specCreator.setName(origColName);
                }
                specCreator.setType(resultType);
                specs.add(specCreator.createSpec());
            } else {
                specs.add(origColSpec);
            }
        }
//        final DataColumnSpecCreator specCreator =
//                new DataColumnSpecCreator("dummy", StringCell.TYPE);
//        for (Entry<String, DataType> entry : collectionColsMap.entrySet()) {
//            if (removeCollectionCol) {
//                //keep the original column name if the collection columns are removed
//                specCreator.setName(entry.getKey());
//            } else {
//                specCreator.setName(DataTableSpec.getUniqueColumnName(
//                                               spec, entry.getKey()));
//            }
//            specCreator.setType(entry.getValue());
//            specs.add(specCreator.createSpec());
//        }
        final DataTableSpec resultSpec =
            new DataTableSpec(specs.toArray(new DataColumnSpec[0]));
        return resultSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_trans != null) {
            m_trans.setMapper(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        if (m_trans == null) {
            m_trans = new HiLiteTranslator(hiLiteHdl);
            m_trans.addToHiLiteHandler(m_hilite);
        } else if (m_trans.getFromHiLiteHandler() != hiLiteHdl) {
            m_trans.removeAllToHiliteHandlers();
            m_trans.setMapper(null);
            m_trans.addToHiLiteHandler(m_hilite);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hilite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_removeCollectionCol.validateSettings(settings);
        m_skipMissingVal.validateSettings(settings);
        m_enableHilite.validateSettings(settings);
        if (settings.containsKey(CFG_COL_NAMES)) {
            //this option has been introduced in KNIME 2.8
            m_collCols.validateSettings(settings);
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_removeCollectionCol.loadSettingsFrom(settings);
        m_skipMissingVal.loadSettingsFrom(settings);
        m_enableHilite.loadSettingsFrom(settings);
        try {
            // this option has been introduced in KNIME 2.8
            m_collCols.loadSettingsFrom(settings);
            //set the old column name setting to null to indicate that the new settings should be used
            m_columnName.setStringValue(null);
        } catch (InvalidSettingsException e) {
            //load and use the old settings
            m_columnName.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_removeCollectionCol.saveSettingsTo(settings);
        m_skipMissingVal.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
        m_collCols.saveSettingsTo(settings);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettings config = new NodeSettings("hilite_mapping");
            ((DefaultHiLiteMapper) m_trans.getMapper()).save(config);
            config.saveToXML(new GZIPOutputStream(new FileOutputStream(new File(
                    nodeInternDir, "hilite_mapping.xml.gz"))));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettingsRO config = NodeSettings.loadFromXML(
                    new GZIPInputStream(new FileInputStream(
                    new File(nodeInternDir, "hilite_mapping.xml.gz"))));
            try {
                m_trans.setMapper(DefaultHiLiteMapper.load(config));
            } catch (final InvalidSettingsException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    /**
     * @param table
     * @return
     * @throws InvalidSettingsException
     */
    private int[] compatibleGetSelectedColIds(final BufferedDataTable table) throws InvalidSettingsException {
        final DataTableSpec spec = table.getDataTableSpec();
        final String[] columnNames;
        if (m_columnName.getStringValue() == null) {
            //the column filter has been introduced in KNIME 2.8
            final FilterResult filterResult = m_collCols.applyTo(spec);
            columnNames = filterResult.getIncludes();
        } else {
            columnNames = new String[] {m_columnName.getStringValue()};
        }
        return getSelectedColIdxs(spec, columnNames);
    }

    /**
     * @param spec
     * @return
     * @throws InvalidSettingsException
     */
    private DataTableSpec compatibleCreateResultSpec(final DataTableSpec spec)
            throws InvalidSettingsException {
        final DataTableSpec resultSpec;
        if (m_columnName.getStringValue() == null) {
            //the column filter has been introduced in KNIME 2.8
            final FilterResult filterResult = m_collCols.applyTo(spec);
            String[] colNames = filterResult.getIncludes();
            resultSpec = createTableSpec(spec, m_removeCollectionCol.getBooleanValue(),
                                     colNames);
        } else {
            resultSpec = createTableSpec(spec, m_removeCollectionCol.getBooleanValue(),
                                         m_columnName.getStringValue());
        }
        return resultSpec;
    }
}
