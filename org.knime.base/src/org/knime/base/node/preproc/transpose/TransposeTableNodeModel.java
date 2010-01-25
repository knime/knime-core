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
 *
 */
package org.knime.base.node.preproc.transpose;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Model of the transpose node which swaps rows and columns. In addition, a new 
 * <code>HiLiteHandler</code> is provided at the output.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class TransposeTableNodeModel extends NodeModel {
    
    /** Output hilite handler for new data generated during execute. */
    private final HiLiteHandler m_outHiLite;
    
    /** Chunk size model. */
    private final SettingsModelIntegerBounded m_chunkSize
        = TransposeTableNodeDialogPane.createChunkSizeModel();

    /**
     * Creates a transpose model with one data in- and output.
     *
     */
    TransposeTableNodeModel() {
        super(1, 1);
        m_outHiLite = new HiLiteHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_chunkSize.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO (tg) option not available before 2.0
        // m_chunkSize.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            m_chunkSize.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            // TODO (tg) before 2.0 this option was not available
            m_chunkSize.setIntValue(1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        // input column spec that will be transposed the output row IDs
        DataTableSpec spec = inData[0].getDataTableSpec();
        // if the input table does not contain any column, create empty rows
        // only using the column header as row IDs
        if (inData[0].getRowCount() == 0) {
            BufferedDataContainer cont = 
                exec.createDataContainer(new DataTableSpec());
            for (int i = 0; i < spec.getNumColumns(); i++) {
                String colName = spec.getColumnSpec(i).getName();
                cont.addRowToTable(new DefaultRow(colName, new DataCell[0]));
            }
            cont.close();
            return new BufferedDataTable[]{cont.getTable()};
            
        }
        // new number of columns = number of rows
        final int newNrCols = inData[0].getRowCount();
        // new column names
        final ArrayList<String> colNames = new ArrayList<String>();
        // new column types
        final ArrayList<DataType> colTypes = new ArrayList<DataType>();
        // over entire table
        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            exec.setMessage("Determine most-general column type for row: "
                    + row.getKey().getString());
            DataType type = null;
            // and all cells
            for (int i = 0; i < row.getNumCells(); i++) {
                DataType newType = row.getCell(i).getType();
                if (type == null) {
                    type = newType;
                } else {
                    type = DataType.getCommonSuperType(type, newType);
                }
            }
            if (type == null) {
                type = DataType.getType(DataCell.class);
            }
            colNames.add(row.getKey().getString().toString());
            colTypes.add(type);
        }
        // new number of rows
        int newNrRows = spec.getNumColumns();
        // create new specs
        final DataColumnSpec[] colSpecs = new DataColumnSpec[newNrCols];
        for (int c = 0; c < newNrCols; c++) {
            colSpecs[c] = new DataColumnSpecCreator(colNames.get(c), colTypes
                    .get(c)).createSpec();
            exec.checkCanceled();
        }
        BufferedDataContainer cont = exec
                .createDataContainer(new DataTableSpec(colSpecs));
        final int chunkSize = m_chunkSize.getIntValue();
        // total number of chunks
        final double nrChunks = Math.ceil((double) newNrRows / chunkSize);
        for (int chunkIdx = 0; chunkIdx < nrChunks; chunkIdx++) {
            // map of new row keys to cell arrays 
            Map<String, DataCell[]> map = 
                new LinkedHashMap<String, DataCell[]>(newNrRows);
            int rowIdx = 0;
            for (DataRow row : inData[0]) {
                exec.setProgress(((rowIdx + 1) * (chunkIdx + 1)) 
                        / (nrChunks * newNrCols), "Transpose row \"" 
                        + row.getKey().getString() + "\" to column.");
                int colIdx = chunkIdx * chunkSize;
                // iterate chunk of columns
                for (int r = colIdx; 
                        r < Math.min(newNrRows, colIdx + chunkSize); r++) { 
                    String newRowKey = spec.getColumnSpec(r).getName();
                    DataCell[] cellArray = map.get(newRowKey);
                    if (cellArray == null) {
                        cellArray = new DataCell[newNrCols]; 
                        map.put(newRowKey, cellArray);
                    }
                    cellArray[rowIdx] = row.getCell(r);
                }
                try {
                    exec.checkCanceled();
                } catch (CanceledExecutionException cee) {
                    cont.close();
                    throw cee;
                }
                rowIdx++;
            }
            // add chunk of rows to buffer
            for (Map.Entry<String, DataCell[]> e : map.entrySet()) {
                exec.setMessage("Adding row \"" + e.getKey() + "\" to table.");
                DataRow row = new DefaultRow(
                        e.getKey(), e.getValue());
                cont.addRowToTable(row);
            }
        }
        exec.setProgress(1.0, "Finished, closing buffer...");
        cont.close();
        return new BufferedDataTable[]{cont.getTable()};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_outHiLite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_outHiLite.fireClearHiLiteEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[1];
    }
}
