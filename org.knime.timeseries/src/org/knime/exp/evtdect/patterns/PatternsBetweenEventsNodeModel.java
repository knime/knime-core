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
package org.knime.exp.evtdect.patterns;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model for the node that extracts data from timestampFrom
 * to timestampTo from the input table.
 * 
 * @author Rosaria Silipo 
 */
public class PatternsBetweenEventsNodeModel extends NodeModel {
    /** Config identifier: column name. */
    static final String CFG_COLUMN_NAME_EVENTS = "column_name_events";

    /** Config identifier: column name to extract data. */
    static final String CFG_COLUMN_NAME_OUTPUT = "column_name_output";

    /** Config identifier: event string name. */
    static final String CFG_EVENT_NAME = "event_name";

    /** Config identifier: event string name. */
    static final String CFG_ALL_COLUMNS = "all columns";
    
    private SettingsModelString m_eventName =
         new SettingsModelString(CFG_EVENT_NAME, "split");

    private SettingsModelString m_columnNameEvents =
        new SettingsModelString(CFG_COLUMN_NAME_EVENTS, null);

    private SettingsModelString m_columnNameOutput =
        new SettingsModelString(CFG_COLUMN_NAME_OUTPUT, null);

    private SettingsModelBoolean m_allColumns =
        new SettingsModelBoolean(CFG_ALL_COLUMNS, false);

    private String m_event;
    
    private Boolean m_allColumnsIntoRow;   
    private int m_columnsNr = 1;
    private int[] m_columnIndeces;
 
        /** Inits node, 1 input, 1 output. */
    public PatternsBetweenEventsNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
                
           int colIndexEvents = -1;
           int colIndexOutput = -1;

           m_event = m_eventName.getStringValue();
           if (m_event == null) {
                throw new InvalidSettingsException(
                    "No \"event name\" selected."); 
           }
            
           m_allColumnsIntoRow =  m_allColumns.getBooleanValue();

           if (m_columnNameEvents.getStringValue() == null) {
                int i1 = 0;
                for (DataColumnSpec cs : inSpecs[0]) {
                    if (cs.getType().isCompatible(StringValue.class)) {
                        if (colIndexEvents != -1) {
                            throw new InvalidSettingsException(
                                    "No column selected.");
                        }
                        colIndexEvents = i1;
                    }
                    i1++;
                }

                if (colIndexEvents == -1) {
                    throw new InvalidSettingsException("No column selected.");
                }
                m_columnNameEvents.setStringValue(inSpecs[0]
                        .getColumnSpec(colIndexEvents).getName());
                setWarningMessage("Column '" 
                        + m_columnNameEvents.getStringValue()
                        + "' auto selected");
            } else {
                colIndexEvents =
                   inSpecs[0].findColumnIndex(
                           m_columnNameEvents.getStringValue());
                if (colIndexEvents < 0) {
                    throw new InvalidSettingsException("No such column: "
                            + m_columnNameEvents.getStringValue());
                }

                DataColumnSpec colSpec = 
                    inSpecs[0].getColumnSpec(colIndexEvents);
                if (!colSpec.getType().isCompatible(StringValue.class)) {
                  throw new InvalidSettingsException("Column \"" 
                            + m_columnNameEvents
                            + "\" does not contain string values: "
                            + colSpec.getType().toString());
                }
            }
        
            if (m_columnNameOutput.getStringValue() == null) {
                int i1 = 0;
                for (DataColumnSpec cs : inSpecs[0]) {
                    if (cs.getType().isCompatible(DoubleValue.class)) {
                        if (colIndexOutput != -1) {
                            throw new InvalidSettingsException(
                                    "No column selected.");
                        }
                        colIndexOutput = i1;
                    }
                    i1++;
                }

                if (colIndexOutput == -1) {
                    throw new InvalidSettingsException("No column selected.");
                }
                m_columnNameOutput.setStringValue(inSpecs[0]
                        .getColumnSpec(colIndexOutput).getName());
                setWarningMessage("Column '" 
                        + m_columnNameOutput.getStringValue()
                        + "' auto selected");
            } else {
                colIndexOutput =
                   inSpecs[0].findColumnIndex(
                           m_columnNameOutput.getStringValue());
                if (colIndexOutput < 0) {
                    throw new InvalidSettingsException("No such column: "
                            + m_columnNameOutput.getStringValue());
                }

                DataColumnSpec colSpec = 
                    inSpecs[0].getColumnSpec(colIndexOutput);
                if (!colSpec.getType().isCompatible(DoubleValue.class)) {
                  throw new InvalidSettingsException("Column \"" 
                            + m_columnNameOutput
                            + "\" does not contain string values: "
                            + colSpec.getType().toString());
                }
           }
        DataTableSpec [] outs = new DataTableSpec[]  {null};
        return outs;
    }
            
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
 
        BufferedDataTable in = inData[0];
        DataTableSpec inSpecs = in.getDataTableSpec();
     
        final int colIndexEvents =
            inSpecs.findColumnIndex(m_columnNameEvents.getStringValue());
        final int colIndexOutput =
            inSpecs.findColumnIndex(m_columnNameOutput.getStringValue());

        int maxRowLength = defineMaxRowLength(colIndexEvents, in);
        setColumnNames(colIndexOutput, inSpecs);
        BufferedDataTable [] outs = 
            splitColumnsIntoRow(maxRowLength,  
                    colIndexEvents, in, exec);
        
        return outs;
        
    }
    
    /** defines maximum length of row for one column.
     * @param colIndexEvents column index where events are stored
     * @param in input DataBufferedTable
     * @return maximum row length*/
    protected int defineMaxRowLength(
            final int colIndexEvents, final BufferedDataTable in) {
        
        int count = 0;
        int maxRowLength = 0;
        
        try {
            for (DataRow r : in) {
                
               String event = r.getCell(colIndexEvents).toString();
               
               if (event.equals(m_event)) {
                 if (maxRowLength < count) {
                     maxRowLength = count;
                 }
                 count = 0;
               }
               count++;
             }
        } finally {
            // last check
            if (maxRowLength < count) {
                maxRowLength = count;
            }
         }
        return maxRowLength;
    }
    
    /**selects indeces of those columns to report consecutively on a row.
     * @param colIndexOutput selected column for output data from dialog window
     * @param inSpecs inspec buffer table
      */
    protected void setColumnNames(final int colIndexOutput, 
            final DataTableSpec inSpecs) {
        
        if (m_allColumnsIntoRow) {
            Vector<Integer> v = new Vector<Integer>();
            int i = 0;
            
            for (DataColumnSpec cs : inSpecs) {
                if (cs.getType().isCompatible(DoubleValue.class)) {
                    v.add(i);
                }
                i++;
            }
            v.trimToSize();
            m_columnIndeces = new int[v.capacity()];
            for (int i1 = 0; i1 < m_columnIndeces.length; i1++) {
                m_columnIndeces[i1] = v.elementAt(i1);
            }
        } else {
            m_columnIndeces = new int [1];
            m_columnIndeces[0] = colIndexOutput;
        }
        m_columnsNr = m_columnIndeces.length;
    }
            
    /** Splits column from event(i) to event(i+1) and puts it into a row.
     * 
     * @param maxRowLength maximum length of row
     * @param colIndexEvents column index where events are stored
     * @param in input DataBufferedTable 
     * @param exec executionContext
     * @return BufferedDataTable with output data*/
    protected BufferedDataTable [] splitColumnsIntoRow(
            final int maxRowLength, 
            final int colIndexEvents, final BufferedDataTable in,
            final ExecutionContext exec) {
        
        int count = 0;
        DataTableSpec outs;
        BufferedDataContainer t;
                    
        DataCell [] newCells = new DataCell [maxRowLength * m_columnsNr];
       
        String[] newColumnNames = new String [maxRowLength * m_columnsNr];
        DataType [] types = new DataType [maxRowLength * m_columnsNr];
 
        for (int j = 0; j < m_columnsNr; j++) {
          for (int i = 0; i < maxRowLength; i++) {
            int index = j * maxRowLength + i;
            newColumnNames[index] = 
              in.getDataTableSpec().getColumnSpec(m_columnIndeces[j]).getName()
                + "(t" + i + ")";
            types[index] = DoubleCell.TYPE;
          }
        }
        
        outs = new DataTableSpec(newColumnNames, types);
        t = exec.createDataContainer(outs);
    
        try { 
              int rowLength = 0;
              int index = 0;
                              
              for (DataRow r1 : in) {
                  
                String event = r1.getCell(colIndexEvents).toString();
                
                for (int j1 = 0; j1 < m_columnIndeces.length; j1++) {
                    
                    if (event.equals(m_event)) {
                        // fill array with missing values if needed
                        for (int i = rowLength; i < maxRowLength; i++) {
                             index = j1 * maxRowLength + i;
                             newCells[index] = DataType.getMissingCell();
                        }
                        if (j1 == m_columnIndeces.length - 1) {
                            rowLength = 0;
                        }
                    } else {
                       index = j1 * maxRowLength + rowLength;
                       newCells[index] = r1.getCell(m_columnIndeces[j1]);
                    }
                }
                rowLength++;
                
                if (event.equals(m_event)) {
                    RowKey rk = new RowKey("" + count);
                    DefaultRow outRow = new DefaultRow(rk, newCells);
                    t.addRowToTable(outRow);               
                    count++;
                  }

              }
        } finally {      
            RowKey rk = new RowKey("" + count);
            DefaultRow outRow = new DefaultRow(rk, newCells);
            t.addRowToTable(outRow);               
            count++;

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
            new SettingsModelString(CFG_EVENT_NAME, null);        
        temp.loadSettingsFrom(settings);
 
        SettingsModelString temp1 =
            new SettingsModelString(CFG_COLUMN_NAME_EVENTS, null);        
        temp1.loadSettingsFrom(settings);
        
        SettingsModelString temp2 =
            new SettingsModelString(CFG_COLUMN_NAME_OUTPUT, null);        
        temp2.loadSettingsFrom(settings);
        
        SettingsModelBoolean temp3 =
            new SettingsModelBoolean(CFG_ALL_COLUMNS, false);        
        temp3.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnNameEvents.loadSettingsFrom(settings);
        m_columnNameOutput.loadSettingsFrom(settings);
        m_eventName.loadSettingsFrom(settings);
        m_allColumns.loadSettingsFrom(settings);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_eventName.saveSettingsTo(settings);
        m_columnNameEvents.saveSettingsTo(settings);
        m_columnNameOutput.saveSettingsTo(settings);
        m_allColumns.saveSettingsTo(settings);
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
