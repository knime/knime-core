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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.TimestampCell;
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
 * This is the model for the node that converts
 * {@link org.knime.core.data.def.StringCell}s into {@link TimestampCell}s.
 * 
 * @author Rosaria Silipo
 */
public class String2DateNodeModel extends NodeModel {
    /** Config identifier: column name. */
    static final String CFG_COLUMN_NAME = "column_name";

    /** Config identifier: date format. */
    // static final String CFG_DATE_FORMAT = "date_format";
    /** Config identifier: edited date format. */
    static final String CFG_EDITED_DATE_FORMAT = "edited_date_format";

    // private SettingsModelString m_dateFormat =
    // new SettingsModelString(CFG_DATE_FORMAT, null);

    private SettingsModelString m_edDateFormat = new SettingsModelString(
            CFG_EDITED_DATE_FORMAT, "yyyy-MM-dd;HH:mm:ss.S");

    private SettingsModelString m_columnName = new SettingsModelString(
            CFG_COLUMN_NAME, null);

    private final SettingsModelBoolean m_replace = String2DateDialog
            .createReplaceModel();

    private SimpleDateFormat m_df;

    /** Inits node, 1 input, 1 output. */
    public String2DateNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        int colIndex = -1;

        if (m_edDateFormat.getStringValue() == null) {
            throw new InvalidSettingsException("No format selected.");
        } else {
            m_df = new SimpleDateFormat(m_edDateFormat.getStringValue());
        }

        if (m_df == null) {
            throw new InvalidSettingsException("Invalid format.");
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
            colIndex = inSpecs[0]
                    .findColumnIndex(m_columnName.getStringValue());
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

        ColumnRearranger c = createColRearranger(inSpecs[0]);
        return new DataTableSpec[]{c.createSpec()};
    }

    private ColumnRearranger createColRearranger(final DataTableSpec spec) {
        ColumnRearranger result = new ColumnRearranger(spec);
        final int colIndex = spec
                .findColumnIndex(m_columnName.getStringValue());
        String uniqueColName = DataTableSpec.getUniqueColumnName(spec,
                m_columnName.getStringValue() + "_date");
        // m_columnName.setStringValue(uniqueColName);
        DataColumnSpec newColSpec = new DataColumnSpecCreator(uniqueColName,
                TimestampCell.TYPE).createSpec();
        SingleCellFactory c = new SingleCellFactory(newColSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(colIndex);
                if (cell.isMissing() || !(cell instanceof StringValue)) {
                    return DataType.getMissingCell();
                }
                try {
                    DataCell dc = new TimestampCell(((StringValue)cell)
                            .getStringValue(), m_df);
                    return dc;
                } catch (ParseException pe) {
                    setWarningMessage("Missing Cell due to Parse Exception.\n"
                            + "Date format incorrect?");
                    return DataType.getMissingCell();
                }
            }
        };
        if (m_replace.getBooleanValue()) {
            result.replace(c, colIndex);
        } else {
            result.insertAt(colIndex + 1, c);
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
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                inData[0], c, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelString temp = new SettingsModelString(CFG_COLUMN_NAME,
                null);
        temp.loadSettingsFrom(settings);

        // SettingsModelString temp1 =
        // new SettingsModelString(CFG_DATE_FORMAT, null);
        // temp1.loadSettingsFrom(settings);

        SettingsModelString temp2 = new SettingsModelString(
                CFG_EDITED_DATE_FORMAT, null);
        temp2.loadSettingsFrom(settings);

        m_replace.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnName.loadSettingsFrom(settings);
        // m_dateFormat.loadSettingsFrom(settings);
        m_edDateFormat.loadSettingsFrom(settings);
        m_replace.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnName.saveSettingsTo(settings);
        m_edDateFormat.saveSettingsTo(settings);
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