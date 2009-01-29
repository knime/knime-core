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
package org.knime.timeseries.node.MA;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelOddIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Rosaria Silipo
 */
public class MovingAverageNodeModel extends NodeModel {
    /** Config identifier: column name. */
    static final String CFG_COLUMN_NAMES = "column_names";
    
    /** Config identifier: date format. */
    static final String CFG_WEIGHTS = "weights";
    
    /** Config identifier: window length. */
    static final String CFG_WIN_LENGTH = "win_length";
    
    /** Default length for moving average window.*/
    static final int DEFAULT_ELEMENTS = 21;

    /** Minimum length for moving average window. */
    static final int MIN_ELEMENTS = 3;

    /** Default maximum length for moving average window. */
    static final int MAX_ELEMENTS = 1001;

    private int m_defaultWinLength = DEFAULT_ELEMENTS;
    private int m_minWinLength = MIN_ELEMENTS;
    private int m_maxWinLength = MAX_ELEMENTS;
  
    private MovingAverage[] m_mas;
    private int m_winLength = -1;

    private SettingsModelOddIntegerBounded m_winLengthSettings =
         new SettingsModelOddIntegerBounded(CFG_WIN_LENGTH, 
                 m_defaultWinLength,
                 m_minWinLength, m_maxWinLength);

    private SettingsModelFilterString m_columnNames =
            new SettingsModelFilterString(CFG_COLUMN_NAMES);

    private SettingsModelString m_weights =
        new SettingsModelString(CFG_WEIGHTS, "simple");

     /** Init node, 1 input, 1 output. */
    public MovingAverageNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        // define moving average window length
        m_winLength = m_winLengthSettings.getIntValue();
        if (m_winLength == -1) {
            throw new InvalidSettingsException(
                    "Window length is not selected.");
        } 

        // define column name on which to apply MA
        if ((m_columnNames.getIncludeList().size() == 0)
                && (m_columnNames.getExcludeList().size() == 0)) {
            throw new InvalidSettingsException(
                  "No double columns available.");
        }

        // define weight function 
        if (m_weights.getStringValue() == null) {
            throw new InvalidSettingsException(
                    "No weight function selected.");
        } else {
           String weights = m_weights.getStringValue();
           // create one MA-compute engine per column (overkill, I know
           // but much easier to reference later on in our DataCellFactory
           m_mas = new MovingAverage[inSpecs[0].getNumColumns()];
           for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
               m_mas[i] = new MovingAverage(m_winLength, weights);
           }
        }

        ColumnRearranger c = createColRearranger(inSpecs[0]);
        return new DataTableSpec[]{c.createSpec()};
    }

    
    private ColumnRearranger createColRearranger(final DataTableSpec spec) {
        ColumnRearranger result = new ColumnRearranger(spec);

        for (String thisCol : m_columnNames.getIncludeList()) {
            final int colIndex =
                spec.findColumnIndex(thisCol);
            DataColumnSpec newColSpec =
                new DataColumnSpecCreator(
                        "MA(" + thisCol + ")",
                        DoubleCell.TYPE).createSpec();
        
            SingleCellFactory c = new SingleCellFactory(newColSpec) {
                @Override
                public DataCell getCell(final DataRow row) {
                    DataCell cell = row.getCell(colIndex);
                    if (cell.isMissing() || !(cell instanceof DoubleValue)) {
                        return DataType.getMissingCell();
                    }
                    return m_mas[colIndex]
                                .maValue(((DoubleValue)cell).getDoubleValue()); 
                 }
            };
            result.replace(c, colIndex);
        }
        
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        
        if (m_winLength < inData[0].getRowCount()) {
            ColumnRearranger c = createColRearranger(
                    inData[0].getDataTableSpec());
            return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                inData[0], c, exec)};
        } else {
            throw new Exception(
                    "Number of total samples in time series smaller than "
                    + "moving Average window length ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelFilterString temp =
            new SettingsModelFilterString(CFG_COLUMN_NAMES);        
        temp.loadSettingsFrom(settings);
 
        SettingsModelString temp1 =
            new SettingsModelString(CFG_WEIGHTS, null);        
        temp1.loadSettingsFrom(settings);

        SettingsModelOddIntegerBounded temp2 =
            new SettingsModelOddIntegerBounded(CFG_WIN_LENGTH, 
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
        m_columnNames.loadSettingsFrom(settings);
        m_weights.loadSettingsFrom(settings);
        m_winLengthSettings.loadSettingsFrom(settings);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnNames.saveSettingsTo(settings);
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
