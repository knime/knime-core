/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Jan 14, 2007 (rs): created
 */
package org.knime.timeseries.node.FilterWindows;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Rosaria Silipo 
 */
public class FilterWindowsNodeModel extends NodeModel {
    /** Config identifier: column name. */
    static final String CFG_COLUMN_NAME = "column_name";
    
    /** Config identifier: date format. */
    static final String CFG_WEIGHTS = "weights";
    
    /** Config identifier: window length. */
    static final String CFG_WIN_LENGTH = "win_length";
    
    private int m_defaultWinLength = 21;
    private int m_minWinLength = 3;
    private int m_maxWinLength = 101;
  
    private FilterWindows m_MA;

    private SettingsModelIntegerBounded m_winLengthSettings =
         new SettingsModelIntegerBounded(CFG_WIN_LENGTH, 
                 m_defaultWinLength,
                 m_minWinLength, m_maxWinLength);

    private SettingsModelString m_columnName =
            new SettingsModelString(CFG_COLUMN_NAME, null);

    private SettingsModelString m_weights =
        new SettingsModelString(CFG_WEIGHTS, null);

     /** Inits node, 1 input, 1 output. */
    public FilterWindowsNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        int colIndex = -1;
        int winLength = -1;
        String weights = null;
        
        // define moving average window length
        winLength = m_winLengthSettings.getIntValue();
        if (winLength == -1) {
            throw new InvalidSettingsException(
                    "Window length is not selected.");
        } 

        // define weight function 
        if (m_weights.getStringValue() == null) {
            throw new InvalidSettingsException(
                    "No weight function selected.");
        } else {
           weights = m_weights.getStringValue();               
           m_MA = new FilterWindows(winLength, weights);
        }
         
        // define column name on which to apply MA
        if (m_columnName.getStringValue() == null) {
            int i = 0;
            for (DataColumnSpec cs : inSpecs[0]) {
                if (cs.getType().isCompatible(StringValue.class)) {
                    if (colIndex != -1) {
                        throw new InvalidSettingsException(
                                "No column selected.");
                    }
                    colIndex = i;
                }
                i++;
            }

            if (colIndex == -1) {
                throw new InvalidSettingsException("No column selected.");
            }
            m_columnName.setStringValue(inSpecs[0].getColumnSpec(colIndex)
                    .getName());
            setWarningMessage("Column '" + m_columnName.getStringValue()
                    + "' auto selected");
        } else {
            colIndex =
                    inSpecs[0].findColumnIndex(m_columnName.getStringValue());
            if (colIndex < 0) {
                throw new InvalidSettingsException("No such column: "
                        + m_columnName.getStringValue());
            }

            DataColumnSpec colSpec = inSpecs[0].getColumnSpec(colIndex);
            if (!colSpec.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Column \"" + m_columnName
                        + "\" does not contain double values: "
                        + colSpec.getType().toString());
            }
        }

        ColumnRearranger c = createColRearranger(inSpecs[0]);
        return new DataTableSpec[]{c.createSpec()};
    }

    
    private ColumnRearranger createColRearranger(final DataTableSpec spec) {
        ColumnRearranger result = new ColumnRearranger(spec);
        final int colIndex =
                spec.findColumnIndex(m_columnName.getStringValue());
        DataColumnSpec newColSpec =
                new DataColumnSpecCreator(
                        "MA(" + m_columnName.getStringValue() + ")",
                        DoubleCell.TYPE).createSpec();
        
        SingleCellFactory c = new SingleCellFactory(newColSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(colIndex);
                if (cell.isMissing() || !(cell instanceof DoubleValue)) {
                    return DataType.getMissingCell();
                }
                return m_MA.maValue(((DoubleValue)cell).getDoubleValue()); 
             }
        };
        
        result.append(c);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger c = createColRearranger(inData[0].getDataTableSpec());
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                inData[0], c, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelString temp =
            new SettingsModelString(CFG_COLUMN_NAME, null);        
        temp.loadSettingsFrom(settings);
 
        SettingsModelString temp1 =
            new SettingsModelString(CFG_WEIGHTS, null);        
        temp1.loadSettingsFrom(settings);

        SettingsModelIntegerBounded temp2 =
            new SettingsModelIntegerBounded(CFG_WIN_LENGTH, 
                    m_defaultWinLength,
                    m_minWinLength, m_maxWinLength);
        temp2.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnName.loadSettingsFrom(settings);
        m_weights.loadSettingsFrom(settings);
        m_winLengthSettings.loadSettingsFrom(settings);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnName.saveSettingsTo(settings);
        m_winLengthSettings.saveSettingsTo(settings);
        m_weights.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }
}
