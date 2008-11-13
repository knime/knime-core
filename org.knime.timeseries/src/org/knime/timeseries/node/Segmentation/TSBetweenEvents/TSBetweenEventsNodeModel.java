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
package org.knime.timeseries.node.Segmentation.TSBetweenEvents;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Vector;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
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
 * This is the model for the node that extracts data from timestampFrom
 * to timestampTo from the input table.
 * 
 * @author Rosaria Silipo 
 */
public class TSBetweenEventsNodeModel extends NodeModel {
    /** Config identifier: column name. */
    static final String CFG_COLUMN_NAME = "column_name";

    /** Config identifier: column name. */
    static final String CFG_TIMESTAMP_COLUMN_NAME = "timestamp_column_name";

    /** Config identifier: event string name. */
    static final String CFG_EVENT_NAME = "event_name";

    /** Config identifier: date format. */
 //   static final String CFG_DATE_FORMAT = "date_format";

    /** Config identifier: date conatined in output table. */
    static final String CFG_TIMESTAMP_OUTPUT = "timeStampOutput";

    private SettingsModelString m_eventName =
         new SettingsModelString(CFG_EVENT_NAME, "split");

    private SettingsModelString m_columnName =
        new SettingsModelString(CFG_COLUMN_NAME, null);

//    private SettingsModelString m_dateFormat =
//        new SettingsModelString(CFG_DATE_FORMAT, "yyyy-MM-dd;HH:mm:ss.S");

    private SettingsModelString m_timeStampOutput =
        new SettingsModelString(CFG_TIMESTAMP_OUTPUT, "yyyy-MM-dd;HH:mm:ss.S");
   
    private SettingsModelString m_timestampColumnName =
        new SettingsModelString(CFG_TIMESTAMP_COLUMN_NAME, null);

    private String m_event; 
    private String m_dateOutput;
    private SimpleDateFormat m_df = new SimpleDateFormat("yyyy-MM-dd;HH:mm:ss.S"); 

        /** Inits node, 1 input, 1 output. */
    public TSBetweenEventsNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
                
           int colIndex = -1;

           m_event = m_eventName.getStringValue();
           if (m_event == null) {
                throw new InvalidSettingsException(
                    "No \"event name\" selected."); 
           }
           
/*
           if (m_dateFormat.getStringValue() == null) {
               throw new InvalidSettingsException(
                       "No format selected."); 
           } else {
               m_df = new SimpleDateFormat(m_dateFormat.getStringValue());
           }
           if (m_df == null) {
                   throw new InvalidSettingsException(
                   "Invalid format."); 
           }
           */

           m_dateOutput = m_timeStampOutput.getStringValue();
           if (m_dateOutput == null) {
                throw new InvalidSettingsException(
                    "No \"date for output table\" selected."); 
           }
          
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
                if (!colSpec.getType().isCompatible(StringValue.class)) {
                  throw new InvalidSettingsException("Column \"" + m_columnName
                            + "\" does not contain string values: "
                            + colSpec.getType().toString());
                }
            }

            
            if (m_timestampColumnName.getStringValue() == null) {
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
                setWarningMessage("Column '" 
                        + m_timestampColumnName.getStringValue()
                        + "' auto selected");
            } else {
                colIndex =
                   inSpecs[0].findColumnIndex(
                           m_timestampColumnName.getStringValue());
                if (colIndex < 0) {
                    throw new InvalidSettingsException("No such column: "
                            + m_timestampColumnName.getStringValue());
                }

                DataColumnSpec colSpec = inSpecs[0].getColumnSpec(colIndex);
                if (!colSpec.getType().isCompatible(TimestampValue.class)) {
                  throw new InvalidSettingsException("Column \"" 
                            + m_timestampColumnName
                            + "\" does not contain string values: "
                            + colSpec.getType().toString());
                }
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
        final int timestampColIndex =
            outs.findColumnIndex(m_timestampColumnName.getStringValue());
       
        int outputTableIndex = -1;
        
        TimestampCell tscOutput = 
            new TimestampCell(m_dateOutput, m_df);

        BufferedDataContainer t = exec.createDataContainer(outs);
        Vector<BufferedDataTable> tables = new Vector<BufferedDataTable>();
 
        try {
           int count = 0;
           for (DataRow r : in) {
              String event = r.getCell(colIndex).toString();
              TimestampValue tsc = (TimestampValue) r.getCell(timestampColIndex);
              
              if (event.equals(m_event)) {
                  t.close();
                  count++;
                  if (t.getTable() != null) {
                     tables.add(t.getTable());
                  }
                  t = exec.createDataContainer(outs);
              }
                            
              if (tsc.getDate().compareTo(tscOutput.getDate()) == 0) {
                  outputTableIndex = count;
              }
              t.addRowToTable(r);
            }
        } finally {
            t.close();
            if (t.getTable() != null) {
               tables.add(t.getTable());
            }
        }
        
        BufferedDataTable [] outputTables = new BufferedDataTable [1];
             
        if (outputTableIndex < 0) {
            throw new Exception("date to choose output table was not found");
        } else {
           tables.trimToSize();
           outputTables [0] = tables.elementAt(outputTableIndex);
        }
        return outputTables;
     }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelString temp =
            new SettingsModelString(CFG_EVENT_NAME, null);        
        temp.loadSettingsFrom(settings);
 
        SettingsModelString temp1 =
            new SettingsModelString(CFG_COLUMN_NAME, null);        
        temp1.loadSettingsFrom(settings);
        
        SettingsModelString temp2 =
            new SettingsModelString(CFG_TIMESTAMP_OUTPUT, null);        
        temp2.loadSettingsFrom(settings);
        
        SettingsModelString temp3 =
            new SettingsModelString(CFG_TIMESTAMP_COLUMN_NAME, null);        
        temp3.loadSettingsFrom(settings);
 
 //       SettingsModelString temp4 =
 //           new SettingsModelString(CFG_DATE_FORMAT, null);        
 //       temp4.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnName.loadSettingsFrom(settings);
        m_eventName.loadSettingsFrom(settings);
        m_timeStampOutput.loadSettingsFrom(settings);
        m_timestampColumnName.loadSettingsFrom(settings);
//        m_dateFormat.loadSettingsFrom(settings);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_eventName.saveSettingsTo(settings);
        m_columnName.saveSettingsTo(settings);
        m_timeStampOutput.saveSettingsTo(settings);
        m_timestampColumnName.saveSettingsTo(settings);
 //       m_dateFormat.saveSettingsTo(settings);
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
