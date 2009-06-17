/*
 * --------------------------------------------------------------------
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
 * --------------------------------------------------------------------
 *
 * History
 *   03.07.2007 (cebron): created
 */
package org.knime.base.node.preproc.colconvert.stringtonumber;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * The NodeModel for the String to Number Node that converts strings to numbers.
 *
 * @author cebron, University of Konstanz
 */
public class StringToNumberNodeModel extends NodeModel {

    /**
     * The possible types that the string can be converted to.
     */
    public static final DataType[] POSSIBLETYPES =
        new DataType[]{DoubleCell.TYPE, IntCell.TYPE};

    /* Node Logger of this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(StringToNumberNodeModel.class);

    /**
     * Key for the included columns in the NodeSettings.
     */
    public static final String CFG_INCLUDED_COLUMNS = "include";

    /**
     * Key for the decimal separator in the NodeSettings.
     */
    public static final String CFG_DECIMALSEP = "decimal_separator";

    /**
     * Key for the thousands separator in the NodeSettings.
     */
    public static final String CFG_THOUSANDSSEP = "thousands_separator";

    /**
     * Key for the parsing type in the NodeSettings.
     */
    public static final String CFG_PARSETYPE = "parse_type";

    /**
     * The default decimal separator.
     */
    public static final String DEFAULT_DECIMAL_SEPARATOR = ".";

    /**
     * The default thousands separator.
     */
    public static final String DEFAULT_THOUSANDS_SEPARATOR = "";

    /*
     * The included columns.
     */
    private SettingsModelFilterString m_inclCols =
            new SettingsModelFilterString(CFG_INCLUDED_COLUMNS);

    /*
     * The decimal separator
     */
    private String m_decimalSep = DEFAULT_DECIMAL_SEPARATOR;

    /*
     * The thousands separator
     */
    private String m_thousandsSep = DEFAULT_THOUSANDS_SEPARATOR;

    private DataType m_parseType = POSSIBLETYPES[0];

    /**
     * Constructor with one inport and one outport.
     */
    public StringToNumberNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        StringBuilder warnings = new StringBuilder();
        List<String> inclcols = m_inclCols.getIncludeList();
        if (inclcols.size() == 0) {
            warnings.append("No columns selected");
        }
        // find indices to work on.
        Vector<Integer> indicesvec = new Vector<Integer>();

        for (int i = 0; i < inclcols.size(); i++) {
            int colIndex = inSpecs[0].findColumnIndex(inclcols.get(i));
            if (colIndex >= 0) {
                DataType type = inSpecs[0].getColumnSpec(colIndex).getType();
                if (type.isCompatible(StringValue.class)) {
                    indicesvec.add(colIndex);
                } else {
                    warnings.append("Ignoring column \'"
                            + inSpecs[0].getColumnSpec(colIndex).getName()
                            + "\'\n");
                }
            } else {
                throw new InvalidSettingsException("Column index for "
                        + inclcols.get(i) + " not found.");
            }
        }
        if (warnings.length() > 0) {
            setWarningMessage(warnings.toString());
        }
        int[] indices = new int[indicesvec.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = indicesvec.get(i);
        }
        ConverterFactory converterFac =
                new ConverterFactory(indices, inSpecs[0], m_parseType);
        ColumnRearranger colre = new ColumnRearranger(inSpecs[0]);
        colre.replace(converterFac, indices);
        DataTableSpec newspec = colre.createSpec();
        return new DataTableSpec[]{newspec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        StringBuilder warnings = new StringBuilder();
        // find indices to work on.
        DataTableSpec inspec = inData[0].getDataTableSpec();
        List<String> inclcols = m_inclCols.getIncludeList();
        if (inclcols.size() == 0) {
            // nothing to convert, let's return the input table.
            setWarningMessage("No columns selected,"
                    + " returning input DataTable.");
            return new BufferedDataTable[]{inData[0]};
        }
        Vector<Integer> indicesvec = new Vector<Integer>();
        for (int i = 0; i < inclcols.size(); i++) {
            int colIndex = inspec.findColumnIndex(inclcols.get(i));
            if (colIndex >= 0) {
                DataType type = inspec.getColumnSpec(colIndex).getType();
                if (type.isCompatible(StringValue.class)) {
                    indicesvec.add(colIndex);
                } else {
                    warnings
                            .append("Ignoring column \'"
                                    + inspec.getColumnSpec(colIndex).getName()
                                    + "\'\n");
                }
            } else {
                throw new Exception("Column index for " + inclcols.get(i)
                        + " not found.");
            }
        }
        int[] indices = new int[indicesvec.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = indicesvec.get(i);
        }
        ConverterFactory converterFac =
                new ConverterFactory(indices, inspec, m_parseType);
        ColumnRearranger colre = new ColumnRearranger(inspec);
        colre.replace(converterFac, indices);

        BufferedDataTable resultTable =
                exec.createColumnRearrangeTable(inData[0], colre, exec);
        String errorMessage = converterFac.getErrorMessage();

        if (errorMessage.length() > 0) {
            warnings.append(warnings.length() > 0 ? "\n" : "");
            warnings.append(errorMessage);
        }
        if (warnings.length() > 0) {
            LOGGER.warn(errorMessage);
            setWarningMessage(warnings.toString());
        }
        return new BufferedDataTable[]{resultTable};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inclCols.loadSettingsFrom(settings);
        m_decimalSep =
                settings.getString(CFG_DECIMALSEP, DEFAULT_DECIMAL_SEPARATOR);
        m_thousandsSep =
                settings.getString(CFG_THOUSANDSSEP,
                        DEFAULT_THOUSANDS_SEPARATOR);
        m_parseType = settings.getDataType(CFG_PARSETYPE, POSSIBLETYPES[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inclCols.saveSettingsTo(settings);
        settings.addString(CFG_DECIMALSEP, m_decimalSep);
        settings.addString(CFG_THOUSANDSSEP, m_thousandsSep);
        settings.addDataType(CFG_PARSETYPE, m_parseType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inclCols.validateSettings(settings);
        String decimalsep =
                settings.getString(CFG_DECIMALSEP, DEFAULT_DECIMAL_SEPARATOR);
        String thousandssep =
                settings.getString(CFG_THOUSANDSSEP,
                        DEFAULT_THOUSANDS_SEPARATOR);
        if (decimalsep == null || thousandssep == null) {
            throw new InvalidSettingsException("Separators must not be null");
        }
        if (decimalsep.length() > 1 || thousandssep.length() > 1) {
            throw new InvalidSettingsException(
                    "Illegal separator length, expected a single character");
        }

        if (decimalsep.equals(thousandssep)) {
            throw new InvalidSettingsException(
                    "Decimal and thousands separator must not be the same.");
        }
        DataType myType = settings.getDataType(CFG_PARSETYPE, POSSIBLETYPES[0]);
        boolean found = false;
        for (DataType type : POSSIBLETYPES) {
            if (type.equals(myType)) {
                found = true;
            }
        }
        if (!found) {
            throw new InvalidSettingsException("Illegal parse type: " + myType);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty.
    }

    /**
     * The CellFactory to produce the new converted cells.
     *
     * @author cebron, University of Konstanz
     */
    private class ConverterFactory implements CellFactory {

        /*
         * Column indices to use.
         */
        private int[] m_colindices;

        /*
         * Original DataTableSpec.
         */
        private DataTableSpec m_spec;

        /*
         * Error messages.
         */
        private String m_error;

        /** Number of parsing errors. */
        private int m_parseErrorCount;

        private DataType m_type;

        /**
         *
         * @param colindices the column indices to use.
         * @param spec the original DataTableSpec.
         * @param type the {@link DataType} to convert to.
         */
        ConverterFactory(final int[] colindices, final DataTableSpec spec,
                final DataType type) {
            m_colindices = colindices;
            m_spec = spec;
            m_type = type;
            m_parseErrorCount = 0;
        }

        /**
         * {@inheritDoc}
         */
        public DataCell[] getCells(final DataRow row) {
            DataCell[] newcells = new DataCell[m_colindices.length];
            for (int i = 0; i < newcells.length; i++) {
                DataCell dc = row.getCell(m_colindices[i]);
                // should be a DoubleCell, otherwise copy original cell.
                if (!dc.isMissing()) {
                    final String s = ((StringValue)dc).getStringValue();
                    if (s.trim().length() == 0) {
                        newcells[i] = DataType.getMissingCell();
                        continue;
                    }
                    try {
                        String corrected = s;
                        if (m_thousandsSep != null
                                && m_thousandsSep.length() > 0) {
                            // remove thousands separator
                            corrected = s.replaceAll(
                                    Pattern.quote(m_thousandsSep),
                                    "");
                        }
                        if (!".".equals(m_decimalSep)) {
                            if (corrected.contains(".")) {
                                throw new NumberFormatException(
                                        "Invalid floating point number");
                            }
                            if (m_decimalSep != null
                                    && m_decimalSep.length() > 0) {
                                // replace custom separator with standard
                                corrected =
                                        corrected.replaceAll(Pattern
                                                .quote(m_decimalSep), ".");
                            }
                        }

                        if (m_type.equals(DoubleCell.TYPE)) {
                            double parsedDouble = Double.parseDouble(corrected);
                            newcells[i] = new DoubleCell(parsedDouble);
                        } else if (m_type.equals(IntCell.TYPE)) {
                            int parsedInteger = Integer.parseInt(corrected);
                            newcells[i] = new IntCell(parsedInteger);
                        } else {
                            m_error = "No valid parse type.";
                        }
                    } catch (NumberFormatException e) {
                        if (m_parseErrorCount == 0) {
                            m_error =
                                    "'" + s + "' (RowKey: "
                                            + row.getKey().toString()
                                            + ", Position: " + m_colindices[i]
                                            + ")";
                            LOGGER.debug(e.getMessage());
                        }
                        m_parseErrorCount++;
                        newcells[i] = DataType.getMissingCell();
                    }
                } else {
                    newcells[i] = DataType.getMissingCell();
                }
            }
            return newcells;
        }

        /**
         * {@inheritDoc}
         */
        public DataColumnSpec[] getColumnSpecs() {
            DataColumnSpec[] newcolspecs =
                    new DataColumnSpec[m_colindices.length];
            for (int i = 0; i < newcolspecs.length; i++) {
                DataColumnSpec colspec = m_spec.getColumnSpec(m_colindices[i]);
                DataColumnSpecCreator colspeccreator = null;
                if (m_type.equals(DoubleCell.TYPE)) {
                    // change DataType to DoubleCell
                    colspeccreator =
                            new DataColumnSpecCreator(colspec.getName(),
                                    DoubleCell.TYPE);
                } else if (m_type.equals(IntCell.TYPE)) {
                    // change DataType to IntCell
                    colspeccreator =
                            new DataColumnSpecCreator(colspec.getName(),
                                    IntCell.TYPE);
                } else {
                    colspeccreator =
                            new DataColumnSpecCreator("Invalid parse mode",
                                    DataType.getMissingCell().getType());
                }
                newcolspecs[i] = colspeccreator.createSpec();
            }
            return newcolspecs;
        }

        /**
         * {@inheritDoc}
         */
        public void setProgress(final int curRowNr, final int rowCount,
                final RowKey lastKey, final ExecutionMonitor exec) {
            exec.setProgress((double)curRowNr / (double)rowCount, "Converting");
        }

        /**
         * Error messages that occur during execution , i.e.
         * NumberFormatException.
         *
         * @return error message
         */
        public String getErrorMessage() {
            switch (m_parseErrorCount) {
            case 0:
                return "";
            case 1:
                return "Could not parse cell with value " + m_error;
            default:
                return "Values in " + m_parseErrorCount
                        + " cells could not be parsed, first error: " + m_error;
            }
        }

    } // end ConverterFactory
}
