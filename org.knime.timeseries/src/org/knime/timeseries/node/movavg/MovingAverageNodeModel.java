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
package org.knime.timeseries.node.movavg;

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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelOddIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Rosaria Silipo
 */
public class MovingAverageNodeModel extends NodeModel {

    
    
    private MovingAverage[] m_mas;

    private final SettingsModelOddIntegerBounded m_winLength
        = MovingAverageDialog.createWindowLengthModel();

    private final SettingsModelFilterString m_columnNames 
        = MovingAverageDialog.createColumnNamesModel();

    private final SettingsModelString m_weights 
        = MovingAverageDialog.createWeightModel();
    
    private final SettingsModelBoolean m_replace 
        = MovingAverageDialog.createReplaceColumnModel();

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
        // define column name on which to apply MA
        if ((m_columnNames.getIncludeList().size() == 0)
                && (m_columnNames.getExcludeList().size() == 0)) {
            throw new InvalidSettingsException(
                  "No double columns available.");
        }
        // check for the existence of the selected columns
        for (String colName : m_columnNames.getIncludeList()) {
            if (!inSpecs[0].containsName(colName)) {
                throw new InvalidSettingsException(
                        "Column \"" + colName + "\" not found in input data!");
            }
        }

        // define moving average window length
        int winLength = m_winLength.getIntValue();
        if (winLength == -1) {
            throw new InvalidSettingsException(
            "Window length is not selected.");
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
               m_mas[i] = new MovingAverage(m_winLength.getIntValue(), weights);
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
                        DataTableSpec.getUniqueColumnName(spec, 
                                "MA(" + thisCol + ")"),
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
            if (m_replace.getBooleanValue()) {
                result.replace(c, colIndex);
            } else {
                result.append(c);
            }
        }
        
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        
        if (m_winLength.getIntValue() < inData[0].getRowCount()) {
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
        m_columnNames.validateSettings(settings);
        m_replace.validateSettings(settings);
        m_weights.validateSettings(settings);
        m_winLength.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnNames.loadSettingsFrom(settings);
        m_weights.loadSettingsFrom(settings);
        m_winLength.loadSettingsFrom(settings);
        m_replace.loadSettingsFrom(settings);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnNames.saveSettingsTo(settings);
        m_winLength.saveSettingsTo(settings);
        m_weights.saveSettingsTo(settings);
        m_replace.saveSettingsTo(settings);
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
