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
package org.knime.timeseries.node.stringtotimestamp;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.StringHistory;

/**
 * This is the model for the node that converts
 * {@link org.knime.core.data.def.StringCell}s into {@link DateAndTimeCell}s.
 *
 * @author Rosaria Silipo
 * @author Fabian Dill, KNIME.com GmbH, Zurich, Switzerland
 */
public class String2DateNodeModel extends NodeModel {
    
    /** 
     * Suffix to be appended to the selected column name to create a proposed 
     * new column name. 
     */
    static final String DEFAUL_COLUMN_NAME_SUFFIX = "time";
    
    private final SettingsModelString m_selectedColModel
        = String2DateDialog.createColumnSelectionModel();
    private final SettingsModelString m_newColNameModel 
        = String2DateDialog.createColumnNameModel();
    private final SettingsModelBoolean m_replace 
        = String2DateDialog.createReplaceModel();
    private final SettingsModelString m_formatModel 
        = String2DateDialog.createFormatModel();
    private final SettingsModelBoolean m_cancelOnFail 
        = String2DateDialog.createCancelOnFailModel();
    private final SettingsModelInteger m_failNumberModel 
        = String2DateDialog.createFailNumberModel();

    private SimpleDateFormat m_dateFormat;
    
    private boolean m_useDate;
    
    private boolean m_useTime;
    
    private boolean m_useMillis;
    
    /** Inits node, 1 input, 1 output. */
    public String2DateNodeModel() {
        super(1, 1);
        String2DateDialog.addColSelectionListener(m_selectedColModel, 
                m_newColNameModel, DEFAUL_COLUMN_NAME_SUFFIX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_formatModel.getStringValue() == null) {
            throw new InvalidSettingsException("No format selected.");
        }
        m_dateFormat = new SimpleDateFormat(m_formatModel.getStringValue());
        if (m_dateFormat == null) {
            throw new InvalidSettingsException("Invalid format: "
                    + m_formatModel.getStringValue());
        }
        m_dateFormat.setTimeZone(DateAndTimeCell.UTC_TIMEZONE);

        if (m_selectedColModel.getStringValue() == null 
                || m_selectedColModel.getStringValue().isEmpty()) {
            // try to find first String compatible one and auto-guess it
            for (DataColumnSpec cs : inSpecs[0]) {
                if (cs.getType().isCompatible(StringValue.class)) {
                    m_selectedColModel.setStringValue(cs.getName());
                    setWarningMessage(
                            "Auto-guessing first String compatible column: "
                            + cs.getName());
                    break;
                }
            }
        }
        String selectedCol = m_selectedColModel.getStringValue();
        // if still null -> no String compatible column at all
        if (selectedCol == null || selectedCol.isEmpty()) {
            throw new InvalidSettingsException(
                    "No String compatible column found!");
        }
        int colIndex = inSpecs[0]
                .findColumnIndex(selectedCol);
        if (colIndex < 0) {
            throw new InvalidSettingsException("No such column: " 
                    + selectedCol);
        }
        DataColumnSpec colSpec = inSpecs[0].getColumnSpec(colIndex);
        if (!colSpec.getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException("Column \"" 
                    + selectedCol + "\" does not contain string values: "
                    + colSpec.getType().toString());
        }
        ColumnRearranger c = createColRearranger(inSpecs[0]);
        return new DataTableSpec[]{c.createSpec()};
    }

    private ColumnRearranger createColRearranger(final DataTableSpec spec) {
        ColumnRearranger result = new ColumnRearranger(spec);
        final int colIndex = spec
                .findColumnIndex(m_selectedColModel.getStringValue());
        String uniqueColName = m_selectedColModel.getStringValue();
        if (!m_replace.getBooleanValue()) {
            // if we do not have a default new column name yet
            // create one as done in 
            // check whether the new column name is unique...
            uniqueColName = DataTableSpec.getUniqueColumnName(spec,
                    m_newColNameModel.getStringValue());
            m_newColNameModel.setStringValue(uniqueColName);
        }
        DataColumnSpec newColSpec = new DataColumnSpecCreator(uniqueColName,
                DateAndTimeCell.TYPE).createSpec();
        m_dateFormat = new SimpleDateFormat(m_formatModel.getStringValue());
        m_dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SingleCellFactory c = new SingleCellFactory(newColSpec) {
            private int m_failCounter = 0;
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(colIndex);
                if (cell.isMissing() || !(cell instanceof StringValue)) {
                    return DataType.getMissingCell();
                }
                try {
                    String source = ((StringValue)cell).getStringValue();
                    Date date = m_dateFormat.parse(source);
                    Calendar c = DateAndTimeCell.getUTCCalendar();
                    c.setTimeInMillis(date.getTime());
                    m_failCounter = 0;
                    // dependent on the type create the referring cell
                    DateAndTimeCell result = new DateAndTimeCell(
                            c.getTimeInMillis(), 
                            m_useDate, m_useTime, m_useMillis);
                    return result;
                } catch (ParseException pe) {
                    setWarningMessage("Missing Cell due to Parse Exception.\n"
                            + "Date format incorrect?");
                    m_failCounter++;
                    if (m_cancelOnFail.getBooleanValue() 
                            && m_failCounter >= m_failNumberModel
                                .getIntValue()) {
                        throw new RuntimeException(
                                "Maximum number of fails reached: " 
                                + m_failNumberModel.getIntValue());
                    }
                    return DataType.getMissingCell();
                }
            }
        };
        if (m_replace.getBooleanValue()) {
            result.replace(c, colIndex);
        } else {
            result.append(c);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger c = createColRearranger(inData[0].getDataTableSpec());
        BufferedDataTable out = exec.createColumnRearrangeTable(
                inData[0], c, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedColModel.validateSettings(settings);
        m_newColNameModel.validateSettings(settings);
        m_replace.validateSettings(settings);
        m_formatModel.validateSettings(settings);
        SettingsModelString formatClone = m_formatModel
            .createCloneWithValidatedValue(settings);
        String format = formatClone.getStringValue();
        if (format == null || format.length() == 0) {
            throw new InvalidSettingsException("Format must not be empty!");
        }
        try {
            new SimpleDateFormat(format);
        } catch (Exception e) {
            String msg = "Invalid date format: \"" + format + "\".";
            String errMsg = e.getMessage(); 
            if (errMsg != null && !errMsg.isEmpty()) {
                msg += " Reason: " + errMsg; 
            }
            throw new InvalidSettingsException(msg);
        }
        m_cancelOnFail.validateSettings(settings);
        m_failNumberModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedColModel.loadSettingsFrom(settings);
        m_newColNameModel.loadSettingsFrom(settings);
        m_replace.loadSettingsFrom(settings);
        m_formatModel.loadSettingsFrom(settings);
        m_cancelOnFail.loadSettingsFrom(settings);
        m_failNumberModel.loadSettingsFrom(settings);
        // define the type
        // if it contains H, m, s -> time
        // if it contains y, M or d -> date
        String dateformat = m_formatModel.getStringValue();
        m_useTime = containsTime(dateformat);
        m_useDate = containsDate(dateformat);
        m_useMillis = containsMillis(dateformat);
        // if it is not a predefined one -> store it
        if (!String2DateDialog.PREDEFINED_FORMATS.contains(dateformat)) {
            StringHistory.getInstance(String2DateDialog.FORMAT_HISTORY_KEY).add(
                    dateformat);
        }
    }
    
    private boolean containsTime(final String dateFormat) {
        return dateFormat.contains("H") || dateFormat.contains("m")
            || dateFormat.contains("s");
    }
    
    private boolean containsMillis(final String dateFormat) {
        if (!containsTime(dateFormat)) {
            return false;
        }
        return dateFormat.contains("S");
    }
    
    private boolean containsDate(final String dateFormat) {
        return dateFormat.contains("y") || dateFormat.contains("M")
            || dateFormat.contains("d");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedColModel.saveSettingsTo(settings);
        m_newColNameModel.saveSettingsTo(settings);
        m_replace.saveSettingsTo(settings);
        m_formatModel.saveSettingsTo(settings);
        m_cancelOnFail.saveSettingsTo(settings);
        m_failNumberModel.saveSettingsTo(settings);
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