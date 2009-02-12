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
 *   Jan 24, 2007 (rs): created
 */
package org.knime.timeseries.node.Segmentation.OneSampleperDay;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.TimestampValue;
import org.knime.core.data.def.TimestampCell;
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

/**
 * This is the model for the node that extracts one row of data 
 * per day from the input table.
 * 
 * @author Rosaria Silipo 
 */
public class OneSampleperDayNodeModel extends NodeModel {
    /** Config identifier: column name. */
    static final String CFG_COLUMN_NAME = "column_name";

    /** Config identifier: AT Timestamp. */
    static final String CFG_TIMESTAMP_AT = "timestamp_at";

    /** Config identifier: TO Timestamp. */
 //   static final String DATE_FORMAT = "1992-10-01;09:00:00";
    static final String DATE_FORMAT = "09:00:00";

    private SettingsModelString m_timestampAt =
         new SettingsModelString(CFG_TIMESTAMP_AT, DATE_FORMAT);

    private SettingsModelString m_columnName =
        new SettingsModelString(CFG_COLUMN_NAME, null);

    private TimestampCell m_tscAt; 
    private SimpleDateFormat m_df = new SimpleDateFormat("yyyy-MM-dd;HH:mm:ss");
    private int m_hours;
    private int m_minutes;
    private int m_seconds;
 
        /** Inits node, 1 input, 1 output. */
    public OneSampleperDayNodeModel() {
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

            if (m_timestampAt.getStringValue() == null) {
                throw new InvalidSettingsException(
                    "No \"AT:\" Timestamp selected."); 
            } else {
                String time = "1992-10-01;" + m_timestampAt.getStringValue();
                m_tscAt = 
                    new TimestampCell(time, m_df);
            }
            if (m_tscAt == null) {
                throw new InvalidSettingsException(
                "AT: Invalid timestamp value."); 
            }
            
            Calendar d2 = Calendar.getInstance();
            d2.setTime(m_tscAt.getDate());
            m_hours = d2.get(Calendar.HOUR_OF_DAY);
            m_minutes = d2.get(Calendar.MINUTE);
            m_seconds = d2.get(Calendar.SECOND);

            
            if (m_columnName.getStringValue() == null) {
                int i = 0;
                for (DataColumnSpec cs : inSpecs[0]) {
                    if (cs.getType().isCompatible(TimestampValue.class)) {
                        if (colIndex != -1) {
                            throw new InvalidSettingsException(
                                    "1. No column selected.");
                        }
                        colIndex = i;
                    }
                    i++;
                }

                if (colIndex == -1) {
                    throw new InvalidSettingsException("2. No column selected.");
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

        try {
           for (DataRow r : in) {
              TimestampValue tsc = (TimestampValue) r.getCell(colIndex);
            
              Calendar d1 = Calendar.getInstance();
              d1.setTime(tsc.getDate());
              int hours = d1.get(Calendar.HOUR_OF_DAY);
              int minutes = d1.get(Calendar.MINUTE);
              int seconds = d1.get(Calendar.SECOND);
              
              if (hours == m_hours 
                      && minutes == m_minutes 
                      && seconds == m_seconds) {
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
            new SettingsModelString(CFG_TIMESTAMP_AT, null);        
        temp.loadSettingsFrom(settings);
 
        SettingsModelString temp1 =
            new SettingsModelString(CFG_COLUMN_NAME, null);        
        temp1.loadSettingsFrom(settings);
}

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnName.loadSettingsFrom(settings);
        m_timestampAt.loadSettingsFrom(settings);
  }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_timestampAt.saveSettingsTo(settings);
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
