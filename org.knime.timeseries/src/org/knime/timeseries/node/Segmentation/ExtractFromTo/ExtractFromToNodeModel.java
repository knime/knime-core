/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   Jan 24, 2007 (rs): created
 */
package org.knime.timeseries.node.Segmentation.ExtractFromTo;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.types.TimestampCell;
import org.knime.timeseries.types.TimestampValue;

/**
 * This is the model for the node that extracts data from timestampFrom
 * to timestampTo from the input table.
 * 
 * @author Rosaria Silipo 
 */
public class ExtractFromToNodeModel extends NodeModel {
    /** Config identifier: column name. */
    static final String CFG_COLUMN_NAME = "column_name";

    /** Config identifier: FROM Timestamp. */
    static final String CFG_TIMESTAMP_FROM = "timestamp_from";

    /** Config identifier: TO Timestamp. */
    static final String CFG_TIMESTAMP_TO = "timestamp_to";

    /** Config identifier: TO Timestamp. */
    static final String DATE_FORMAT = "1992-10-01;09:00:00";

    private SettingsModelString m_timestampFrom =
         new SettingsModelString(CFG_TIMESTAMP_FROM, DATE_FORMAT);

    private SettingsModelString m_timestampTo =
        new SettingsModelString(CFG_TIMESTAMP_TO, DATE_FORMAT);

    private SettingsModelString m_columnName =
        new SettingsModelString(CFG_COLUMN_NAME, null);

    private TimestampCell m_tscFrom; 
    private TimestampCell m_tscTo; 
    private SimpleDateFormat m_df = new SimpleDateFormat("yyyy-MM-dd;HH:mm:ss");
 
        /** Inits node, 1 input, 1 output. */
    public ExtractFromToNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
                
        try {
            int colIndex = -1;

            if (m_timestampFrom.getStringValue() == null) {
                throw new InvalidSettingsException(
                    "No \"FROM:\" Timestamp selected."); 
            } else {
                m_tscFrom = 
                    new TimestampCell(m_timestampFrom.getStringValue(), m_df);
            }
            if (m_tscFrom == null) {
                throw new InvalidSettingsException(
                "FROM: Invalid timestamp value."); 
            }
        
            if (m_timestampTo.getStringValue() == null) {
                throw new InvalidSettingsException(
                    "No \"TO:\" Timestamp selected."); 
            } else {
                m_tscTo = 
                    new TimestampCell(m_timestampTo.getStringValue(), m_df);
            }
            if (m_tscTo == null) {
                throw new InvalidSettingsException(
                "TO: Invalid timestamp value."); 
            }
            
            if (m_columnName.getStringValue() == null) {
                int i = 0;
                for (DataColumnSpec cs : inSpecs[0]) {
                    if (cs.getType().isCompatible(TimestampValue.class)) {
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
                if (!colSpec.getType().isCompatible(TimestampValue.class)) {
                  throw new InvalidSettingsException("Column \"" + m_columnName
                            + "\" does not contain string values: "
                            + colSpec.getType().toString());
                }
            }

        } catch (ParseException pe) {
            throw new InvalidSettingsException(
                    "Invalid timestamp value. Parse error."); 
        }
        
        DataTableSpec [] outs = inSpecs.clone();
        return outs;
    }
            
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
 
        BufferedDataTable in = inData[0];
        DataTableSpec outs = in.getDataTableSpec();
        
        final int colIndex =
            outs.findColumnIndex(m_columnName.getStringValue());
        BufferedDataContainer t = exec.createDataContainer(outs);

//        final int totalRowCount = in.getRowCount();
        try {
           for (DataRow r : in) {
              TimestampValue tsc = (TimestampValue) r.getCell(colIndex);
            
                java.util.Date d1 = tsc.getDate();
              if (d1.after(m_tscFrom.getDate())
                       && d1.before(m_tscTo.getDate())) {
                   t.addRowToTable(r);
                }
            }
        } finally {
            t.close();
        }
        return new BufferedDataTable[]{t.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelString temp =
            new SettingsModelString(CFG_TIMESTAMP_FROM, null);        
        temp.loadSettingsFrom(settings);
 
        SettingsModelString temp1 =
            new SettingsModelString(CFG_COLUMN_NAME, null);        
        temp1.loadSettingsFrom(settings);

        SettingsModelString temp2 =
            new SettingsModelString(CFG_TIMESTAMP_TO, null);        
        temp2.loadSettingsFrom(settings);
}

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnName.loadSettingsFrom(settings);
        m_timestampFrom.loadSettingsFrom(settings);
        m_timestampTo.loadSettingsFrom(settings);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_timestampFrom.saveSettingsTo(settings);
        m_timestampTo.saveSettingsTo(settings);
        m_columnName.saveSettingsTo(settings);
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
