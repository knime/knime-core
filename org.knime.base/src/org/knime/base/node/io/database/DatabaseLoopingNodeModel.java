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
 * --------------------------------------------------------------------- *
 *
 * History
 *   19.06.2007 (gabriel): created
 */
package org.knime.base.node.io.database;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.util.MutableInteger;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class DatabaseLoopingNodeModel extends DBReaderNodeModel {

    private final SettingsModelString m_columnModel
        = DatabaseLoopingNodeDialogPane.createColumnModel();

    private final SettingsModelBoolean m_aggByRow
        = DatabaseLoopingNodeDialogPane.createAggregateModel();

    private final SettingsModelBoolean m_appendGridColumn
        = DatabaseLoopingNodeDialogPane.createGridColumnModel();

    private final SettingsModelIntegerBounded m_noValues
        = DatabaseLoopingNodeDialogPane.createNoValuesModel();

    private final HiLiteHandler m_hilite = new HiLiteHandler();

    /** Place holder for table name. */
    private static final String TABLE_NAME_PLACE_HOLDER = "<table_name>";
    /** Place holder for table column name. */
    private static final String TABLE_COLUMN_PLACE_HOLDER = "<table_column>";
    /** Place holder for the possible values. */
    private static final String IN_PLACE_HOLDER = "#PLACE_HOLDER_DO_NOT_EDIT#";

    /**
     *
     */
    DatabaseLoopingNodeModel() {
        super(1, 1);
        setQuery("SELECT * FROM " + TABLE_NAME_PLACE_HOLDER
                + " WHERE " + TABLE_COLUMN_PLACE_HOLDER
                + " IN ('" + IN_PLACE_HOLDER + "')");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        String column = m_columnModel.getStringValue();
        if (!inSpecs[0].containsName(column)) {
            throw new InvalidSettingsException("Column '" + column
                    + "' not found in input data.");
        }
        final String oQuery = getQuery();
        DataTableSpec spec = null;
        try {
            String newQuery = oQuery.replace(IN_PLACE_HOLDER, "");
            setQuery(newQuery);
            spec = super.configure(inSpecs)[0];
        } finally {
            setQuery(oQuery);
        }
        return new DataTableSpec[]{createSpec(spec,
                inSpecs[0].getColumnSpec(column))};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        String column = m_columnModel.getStringValue();
        DataTableSpec spec = inData[0].getDataTableSpec();
        int colIdx = spec.findColumnIndex(column);
        HashSet<DataCell> values = new HashSet<DataCell>();
        BufferedDataContainer buf = null;
        final String oQuery = getQuery();
        final Collection<DataCell> curSet = new LinkedHashSet<DataCell>();
        try {
            final int noValues = m_noValues.getIntValue();
            MutableInteger rowCnt = new MutableInteger(0);
            for (Iterator<DataRow> it = inData[0].iterator(); it.hasNext();) {
                exec.checkCanceled();
                DataCell cell = it.next().getCell(colIdx);
                if (values.contains(cell)) {
                    if (!it.hasNext() && curSet.size() == 0) {
                        continue;
                    }
                }
                values.add(cell);
                curSet.add(cell);
                if (curSet.size() == noValues || !it.hasNext()) {
                    StringBuilder queryValues = new StringBuilder();
                    for (DataCell v : curSet) {
                        if (queryValues.length() > 0) {
                            queryValues.append("','");
                        }
                        queryValues.append(v.toString());
                    }
                    String newQuery = oQuery.replaceAll(
                            IN_PLACE_HOLDER, queryValues.toString());
                    setQuery(newQuery);
                    exec.setProgress(values.size() * (double) noValues
                            / inData[0].getRowCount(),
                            "Selecting all values \"" + queryValues + "\"...");
                    BufferedDataTable table = super.execute(inData, exec)[0];
                    if (buf == null) {
                        DataTableSpec resSpec = table.getDataTableSpec();
                        buf = exec.createDataContainer(createSpec(resSpec,
                                spec.getColumnSpec(column)));
                    }
                    if (m_aggByRow.getBooleanValue()) {
                        aggregate(table, rowCnt, buf,
                                CollectionCellFactory.createListCell(curSet));
                    } else {
                        notAggregate(table, rowCnt, buf,
                                CollectionCellFactory.createListCell(curSet));
                    }
                    curSet.clear();
                }
            }
        } finally {
            if (buf != null) {
                buf.close();
            }
            setQuery(oQuery);
        }
        return new BufferedDataTable[]{buf.getTable()};
    }

    private DataTableSpec createSpec(final DataTableSpec spec,
            final DataColumnSpec gridSpec) {
        int nrCols = spec.getNumColumns();
        DataColumnSpec[] cspecs;
        if (m_appendGridColumn.getBooleanValue()) {
            cspecs = new DataColumnSpec[nrCols + 1];
            DataColumnSpecCreator crSpec =
                new DataColumnSpecCreator(gridSpec);
            crSpec.setType(ListCell.getCollectionType(StringCell.TYPE));
            if (spec.containsName(gridSpec.getName())) {
                crSpec.setName(spec.getName() + "#" + gridSpec.getName());
            }
            cspecs[nrCols] = crSpec.createSpec();
        } else {
            cspecs = new DataColumnSpec[nrCols];
        }
        for (int i = 0; i < nrCols; i++) {
            DataColumnSpec cspec = spec.getColumnSpec(i);
            if (m_aggByRow.getBooleanValue()) {
                cspec = new DataColumnSpecCreator(cspec.getName(),
                    StringCell.TYPE).createSpec();
            }
            cspecs[i] = cspec;
        }
        return new DataTableSpec(cspecs);
    }

    private void aggregate(final DataTable table, final MutableInteger rowCnt,
            final BufferedDataContainer buf, final DataCell gridValue) {
        final DataTableSpec spec = table.getDataTableSpec();
        Set<DataCell>[] values = new LinkedHashSet[spec.getNumColumns()];
        for (final DataRow resRow : table) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    values[i] = new LinkedHashSet<DataCell>(1);
                }
                values[i].add(resRow.getCell(i));
            }
        }
        DataCell[] cells;
        if (m_appendGridColumn.getBooleanValue()) {
            cells = new DataCell[values.length + 1];
            cells[cells.length - 1] = gridValue;
        } else {
            cells = new DataCell[values.length];
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                cells[i] = DataType.getMissingCell();
            } else {
                StringBuilder builder = new StringBuilder();
                for (DataCell cell : values[i]) {
                    if (builder.length() > 0) {
                        builder.append(",");
                    }
                    builder.append(cell.toString());
                }
                cells[i] = new StringCell(builder.toString());
            }
        }
        rowCnt.inc();
        final RowKey rowKey = RowKey.createRowKey(rowCnt.intValue());
        buf.addRowToTable(new DefaultRow(rowKey, cells));
    }

    private void notAggregate(final DataTable table,
            final MutableInteger rowCnt,
            final BufferedDataContainer buf, final DataCell gridValue) {
        for (final DataRow resRow : table) {
            rowCnt.inc();
            final RowKey rowKey = RowKey.createRowKey(rowCnt.intValue());
            // override data row to replace row key
            buf.addRowToTable(new DataRow() {
                private final int m_nrCells =
                    (m_appendGridColumn.getBooleanValue()
                            ? resRow.getNumCells() + 1 : resRow.getNumCells());
                public DataCell getCell(final int index) {
                    if (m_appendGridColumn.getBooleanValue()
                            && index == resRow.getNumCells()) {
                        return gridValue;
                    } else {
                        return resRow.getCell(index);
                    }
                }
                public RowKey getKey() {
                    return rowKey;
                }
                public int getNumCells() {
                    return m_nrCells;
                }
                public Iterator<DataCell> iterator() {
                    return resRow.iterator();
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        String query = getQuery();
        if (query.contains(TABLE_COLUMN_PLACE_HOLDER)) {
            throw new InvalidSettingsException(
                    "Table column placeholder not replaced.");
        }
        if (query.contains(TABLE_NAME_PLACE_HOLDER)) {
            throw new InvalidSettingsException(
                    "Table name placeholder not replaced.");
        }
        if (!query.contains(IN_PLACE_HOLDER)) {
            throw new InvalidSettingsException(
                "Do not replace WHERE-clause placeholder in SQL query.");
        }
        m_columnModel.loadSettingsFrom(settings);
        m_aggByRow.loadSettingsFrom(settings);
        m_appendGridColumn.loadSettingsFrom(settings);
        m_noValues.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_columnModel.saveSettingsTo(settings);
        m_aggByRow.saveSettingsTo(settings);
        m_appendGridColumn.saveSettingsTo(settings);
        m_noValues.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        m_columnModel.validateSettings(settings);
        m_aggByRow.validateSettings(settings);
        m_appendGridColumn.validateSettings(settings);
        m_noValues.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hilite;
    }
}
