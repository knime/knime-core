/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 6, 2006 (wiswedel): created
 */
package org.knime.base.node.meta.xvalidation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AggregateOutputNodeModel extends NodeModel {
    
    /** Config key target column. */
    static final String CFG_TARGET_COL = "target_columns";
    /** Config key prediction column. */
    static final String CFG_PREDICT_COL = "prediction_column";
    
    private String m_targetCol;
    private String m_predictCol;
    
    private HashMap<RowKey, DataCell> m_predictMap;
    private ArrayList<DataRow> m_foldStatistics;
    
    private boolean m_isIgnoreReset;
    
    /**
     * One input, one output.
     */
    public AggregateOutputNodeModel() {
        super(1, 2);
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec in = inSpecs[0];
        if (m_targetCol == null && m_predictCol == null) {
            for (int i = in.getNumColumns() - 1; i >= 0; i--) {
                DataColumnSpec c = in.getColumnSpec(i);
                if (c.getType().isCompatible(StringValue.class)) {
                    if (m_predictCol == null) {
                        m_predictCol = c.getName();
                    } else {
                        assert m_targetCol == null;
                        m_targetCol = c.getName();
                        break; // both columns assigned
                    }
                }
            }
            if (m_targetCol == null) {
                throw new InvalidSettingsException(
                        "Invalid input: Need at least two string columns.");
            }
            setWarningMessage("Auto configuration: Using \"" + m_targetCol 
                    + "\" as target and \"" + m_predictCol 
                    + "\" as prediction");
        }
        int targetColIndex = in.findColumnIndex(m_targetCol);
        if (targetColIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column: " + m_targetCol);
        }
        int predictColIndex = in.findColumnIndex(m_predictCol);
        if (predictColIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column: " + m_predictCol);
        }
        return new DataTableSpec[]{in, createSpecPort1()};
    }
    
    /**
     * A column rearranger to be used for the first outport.
     * @param metaIn The input spec of the outer meta node. 
     * @return A new column rearranger.
     */
    ColumnRearranger createColumnRearrangerPort0(final DataTableSpec metaIn) {
        String predictColName = "prediction";
        while (metaIn.containsName(predictColName)) {
            predictColName = predictColName.concat("_");
        }
        ColumnRearranger result = new ColumnRearranger(metaIn);
        DataColumnSpecCreator creator = new DataColumnSpecCreator(
                predictColName, StringCell.TYPE); 
        SingleCellFactory cellF = new SingleCellFactory(creator.createSpec()) {
            @Override
            public DataCell getCell(final DataRow row) {
                RowKey key = row.getKey();
                DataCell c = m_predictMap.get(key);
                return (c == null ? DataType.getMissingCell() : c);
            }
        };
        result.append(cellF);
        return result;
    }
    
    /**
     * Generates new table spec for the second outport, i.e. statistics.
     * @return A new spec
     */
    DataTableSpec createSpecPort1() {
        return new DataTableSpec(
                new DataColumnSpecCreator(
                        "Error in %", DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator(
                        "Size of Test Set", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator(
                        "Error Count", IntCell.TYPE).createSpec());
    }

    /** Creates the table for the second meta outport.
     * @param c To create a container from.
     * @return The table.
     */
    BufferedDataTable createOutputPort1(final ExecutionContext c) {
        BufferedDataContainer con = c.createDataContainer(createSpecPort1());
        for (DataRow r : m_foldStatistics) {
            con.addRowToTable(r);
        }
        con.close();
        return con.getTable();
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        if (m_predictMap == null) {
            m_predictMap = new HashMap<RowKey, DataCell>();
            m_foldStatistics = new ArrayList<DataRow>();
        }
        BufferedDataTable in = inData[0];
        final int rowCount = in.getRowCount();
        int r = 0;
        int targetColIndex = in.getDataTableSpec().findColumnIndex(m_targetCol);
        int predictColIndex = 
            in.getDataTableSpec().findColumnIndex(m_predictCol);
        int correct = 0;
        int incorrect = 0;
        ExecutionMonitor subExec = exec.createSubProgress(0.5);
        for (DataRow row : in) {
            RowKey key = row.getKey();
            DataCell target = row.getCell(targetColIndex);
            DataCell predict = row.getCell(predictColIndex);
            if (target.equals(predict)) {
                correct++;
            } else {
                incorrect++;
            }
            r++;
            subExec.setProgress(r / (double)rowCount, 
                    "Calculating output " + r + "/" + in.getRowCount() 
                    + " (\"" + key + "\")");
            subExec.checkCanceled();
            m_predictMap.put(key, predict);
        }
        DataRow stats = new DefaultRow(
                new RowKey("fold " + m_foldStatistics.size()), 
                new DoubleCell(incorrect / (double)rowCount),
                new IntCell(rowCount),
                new IntCell(incorrect));
        BufferedDataContainer bufOut2 = 
            exec.createDataContainer(createSpecPort1());
        bufOut2.addRowToTable(stats);
        bufOut2.close();
        m_foldStatistics.add(stats);
        return new BufferedDataTable[]{in, bufOut2.getTable()};
    }

    /**
     * @see NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_targetCol = settings.getString(CFG_TARGET_COL);
        m_predictCol = settings.getString(CFG_PREDICT_COL);
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
        if (!isIgnoreReset()) {
            m_foldStatistics = null;
            m_predictMap = null;
        }
    }

    /**
     * @see NodeModel#saveInternals(File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_targetCol != null) {
            settings.addString(CFG_TARGET_COL, m_targetCol);
            settings.addString(CFG_PREDICT_COL, m_predictCol);
        }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (settings.getString(CFG_PREDICT_COL) == null) {
            throw new InvalidSettingsException("No prediction column set.");
        }
        if (settings.getString(CFG_TARGET_COL) == null) {
            throw new InvalidSettingsException("No target column set.");
        }
    }

    /**
     * @return the isIgnoreReset
     */
    final boolean isIgnoreReset() {
        return m_isIgnoreReset;
    }

    /**
     * @param isIgnoreReset the isIgnoreReset to set
     */
    final void setIgnoreReset(final boolean isIgnoreReset) {
        m_isIgnoreReset = isIgnoreReset;
    }

}
