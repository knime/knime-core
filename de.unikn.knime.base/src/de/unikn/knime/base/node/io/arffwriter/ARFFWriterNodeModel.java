/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   17.02.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.arffwriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.IntValue;
import de.unikn.knime.core.data.StringValue;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class ARFFWriterNodeModel extends NodeModel {

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger.
            getLogger(ARFFWriterNodeModel.class);

    /** the key used to store the filename in the model spec. */
    static final String CFGKEY_FILENAME = "ARFFoutput";

    /** the key used to store the filename in the model spec. */
    static final String CFGKEY_SPARSE = "sparseARFF";

    /* the file we write to. Must be writable! */
    private File m_file;

    /* indicates that we are supposed to write a sparse ARFF file */
    private boolean m_sparse;

    /* the string printed after the "@relation" keyword */
    private String m_relationName;

    /**
     * Creates a new ARFF writer model.
     */
    public ARFFWriterNodeModel() {
        super(1, 0); // We need one input. No output.
        m_sparse = false;
        m_relationName = "DataTable";
        m_file = null;
    }

    /**
     * creates an immediately executable model.
     * 
     * @param outFileName the file name to write the data to. Must specify a
     *            writable file.
     */
    public ARFFWriterNodeModel(final String outFileName) {
        this();
        try {
            m_file = stringToWriteableFile(outFileName);
        } catch (InvalidSettingsException ise) {
            LOGGER.error("ARFF Writer: Couldn't set default file. \n", ise);
        }
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings) {
        if (m_file != null) {
            settings.addString(CFGKEY_FILENAME, m_file.getAbsolutePath());
        }
        if (m_sparse) {
            settings.addBoolean(CFGKEY_SPARSE, m_sparse);
        }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettings)
     */
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        stringToWriteableFile(settings.getString(CFGKEY_FILENAME));
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettings)
     */
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        m_file = stringToWriteableFile(settings.getString(CFGKEY_FILENAME));
        if (m_file.exists()) {
            LOGGER.warn("ARFF Writer: File exists - will be overriden!");
        }

    }

    /**
     * @see NodeModel#execute(DataTable[],ExecutionMonitor)
     */
    protected DataTable[] execute(final DataTable[] inData,
            final ExecutionMonitor exec) throws Exception {

        DataTableSpec inSpec = inData[0].getDataTableSpec();
        int numOfCols = inSpec.getNumColumns();

        for (int c = 0; c < numOfCols; c++) {
            DataType colType = inSpec.getColumnSpec(c).getType();
            if (!colType.isCompatible(IntValue.class)
                    && !colType.isCompatible(DoubleValue.class)
                    && !colType.isCompatible(StringValue.class)) {
                throw new IllegalStateException("Can only write Double, Int,"
                        + " and String columns to ARFF file.");
            }
        }

        LOGGER.info("ARFF Writer: ARFFing into file '" + m_file + "'.");

        BufferedWriter writer = new BufferedWriter(new FileWriter(m_file));

        // Write ARFF header
        writer.write("%\n");
        writer.write("% ARFF data file, ");
        writer.write("generated by KNIME - Konstanz Information Miner\n");
        writer.write("% KNIME (c) 2003-2006 Uni Konstanz, Prof. Berthold\n");
        writer.write("%\n");
        writer.write("% Date: " + new Date(System.currentTimeMillis()) + "\n");
        try {
            writer.write("% User: " + System.getProperty("user.name") + "\n");
        } catch (SecurityException se) {
            // okay - we don't add the user name.
        }
        writer.write("%\n");

        writer.write("\n@RELATION " + m_relationName + "\n");

        // write the attribute part, i.e. the columns' name and type
        for (int c = 0; c < numOfCols; c++) {
            DataColumnSpec cSpec = inSpec.getColumnSpec(c);
            writer.write("@ATTRIBUTE ");
            if (needsQuotes(cSpec.getName().toString())) {
                writer.write("'" + cSpec.getName().toString() + "'");
            } else {
                writer.write(cSpec.getName().toString());
            }
            writer.write("\t");
            writer.write(colspecToARFFType(cSpec));
            writer.write("\n");
        }

        // finally add the data
        writer.write("\n@DATA\n");
        for (DataRow row : inData[0]) {
            if (m_sparse) {
                writer.write("{");
            }
            boolean first = true; // flag to skip comma in first column
            for (int c = 0; c < row.getNumCells(); c++) {
                DataCell cell = row.getCell(c);

                if (m_sparse && !cell.isMissing()) {
                    // we write only non-zero values in a sparce file
                    if (cell instanceof IntValue) {
                        if (((IntValue)cell).getIntValue() == 0) {
                            continue;
                        }
                    }
                    if (cell instanceof DoubleValue) {
                        if (Math.abs(((DoubleValue)cell).getDoubleValue()) 
                                < 1e-29) {
                            continue;
                        }
                    }
                }

                String data = "?";
                if (!cell.isMissing()) {
                    data = cell.toString();
                }

                // see if we need to quote it. A space, tab, etc. or a comma
                // trigger quotes.
                if (needsQuotes(data)) {
                    data = "'" + data + "'";
                }

                // now spit it out
                if (!first) {
                    // print column separator
                    writer.write(",");
                } else {
                    first = false;
                }

                // data in sparse file must be proceeded by the column number
                if (m_sparse) {
                    writer.write("" + c + " ");
                }

                writer.write(data);

            } // for (all cells of this row)

            if (m_sparse) {
                writer.write("}");
            }
            writer.write("\n");

            // see if user told us to stop.
            // Check if execution was canceled !
            try {
                exec.checkCanceled();
            } catch (CanceledExecutionException cee) {
                LOGGER.info("ARFF Writer canceled");
                writer.close();
                if (m_file.delete()) {
                    LOGGER.debug("File '" + m_file.getName() + "' deleted.");
                } else {
                    LOGGER.warn("Unable to delete file '" + m_file.getName()
                            + "' after ARFF writer cancelation.");
                }
                throw cee;
            }

        } // while (!rIter.atEnd())

        writer.flush();
        writer.close();

        // execution successful return empty array
        return new DataTable[0];
    }

    /*
     * translates the KNIME type into a string suitable for the <datatype>
     * argument to the "@attribute" statement. It will return one of the
     * following strings: "STRING", "REAL", "INTEGER", or "{ <values>}" with
     * <values> being a comma separated list of all possible values of that
     * column.
     */
    private String colspecToARFFType(final DataColumnSpec cSpec) {

        DataType type = cSpec.getType();

        // first try integer, as it is the "simplest" representation
        if (type.isCompatible(IntValue.class)) {
            return "INTEGER";
        }
        // now double, adding "only" some decimal places...
        if (type.isCompatible(DoubleValue.class)) {
            return "REAL";
        }

        // that's all we can handle
        assert type.isCompatible(StringValue.class);

        if ((cSpec.getDomain().getValues() == null)
                || (cSpec.getDomain().getValues().size() == 0)) {
            return "STRING";
        }

        // here we have to build the list of all nominal values.
        String nomValues = "{";
        for (DataCell cell : cSpec.getDomain().getValues()) {
            String value = cell.toString();
            if (needsQuotes(value)) {
                nomValues += "'" + value + "',";
            } else {
                nomValues += value + ",";
            }
        }
        // cut off the last comma and add a closing braket.
        return nomValues.subSequence(0, nomValues.length() - 1) + "}";
    }

    /*
     * returns true if the specified string contains characters below the ASCII
     * 20 or a comma.
     */
    private boolean needsQuotes(final String str) {
        for (int s = 0; s < str.length(); s++) {
            if ((str.charAt(s) <= 32) || (str.charAt(s) == ',')) {
                return true;
            }
        }
        return false;
    }

    /* tries to create a file for the passed file name. */
    private File stringToWriteableFile(final String fileName)
            throws InvalidSettingsException {

        File result = new File(fileName);

        if (result.isDirectory()) {
            throw new InvalidSettingsException("\"" + result.getAbsolutePath()
                    + "\" is a directory.");
        }
        if (!result.exists()) {
            return result;
        }
        if (!result.canWrite()) {
            throw new InvalidSettingsException("Cannot write to file \""
                    + result.getAbsolutePath() + "\".");
        }
        return result;
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    protected void reset() {
        // doodledoom.
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_file == null) {
            throw new InvalidSettingsException("File not specified.");
        }
        for (int c = 0; c < inSpecs[0].getNumColumns(); c++) {
            DataType colType = inSpecs[0].getColumnSpec(c).getType();

            if (!colType.isCompatible(IntValue.class)
                    && !colType.isCompatible(DoubleValue.class)
                    && !colType.isCompatible(StringValue.class)) {
                throw new InvalidSettingsException("Class " + colType
                        + " not supported.");
            }
        }
        return new DataTableSpec[0];
    }
}
