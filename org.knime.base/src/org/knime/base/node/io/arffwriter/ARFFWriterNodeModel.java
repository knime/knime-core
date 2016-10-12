/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   17.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffwriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;

/**
 *
 * @author Peter Ohl, University of Konstanz
 */
public class ARFFWriterNodeModel extends NodeModel {

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ARFFWriterNodeModel.class);

    /** The key used to store the filename in the model spec. */
    static final String CFGKEY_FILENAME = "ARFFoutput";

    /** The key used to store the filename in the model spec. */
    static final String CFGKEY_SPARSE = "sparseARFF";

    /** The key used to store the Overwrite OK in the model spec. */
    static final String CFGKEY_OVERWRITE_OK = "overwriteOK";

    /* the file we write to. Must be writable! */
    private String m_location;

    /** whether overwriting the file is ok. */
    private boolean m_overwriteOK;

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
    }

    /**
     * Creates an immediately executable model.
     *
     * @param outFileName the file name to write the data to. Must specify a
     *            writable file.
     */
    public ARFFWriterNodeModel(final String outFileName) {
        this();
        m_location = outFileName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_location != null) {
            settings.addString(CFGKEY_FILENAME, m_location);
            settings.addBoolean(CFGKEY_OVERWRITE_OK, m_overwriteOK);
        }
        if (m_sparse) {
            settings.addBoolean(CFGKEY_SPARSE, m_sparse);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String fileName = settings.getString(CFGKEY_FILENAME);
        if (fileName == null || fileName.length() == 0) {
            throw new InvalidSettingsException("Missing output file name.");
        }
        // overwrite flag added in v2.1 - not required here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_location = settings.getString(CFGKEY_FILENAME);
        // added in v2.1
        m_overwriteOK = settings.getBoolean(CFGKEY_OVERWRITE_OK, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        checkFileAccess(m_location, false);
        URL url = FileUtil.toURL(m_location);
        Path localPath = FileUtil.resolveToPath(url);

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

        LOGGER.info("ARFF Writer: ARFFing into '" + m_location + "'.");

        try (BufferedWriter writer = openWriter(localPath, url)) {
            // Write ARFF header
            writer.write("%\n");
            writer.write("% ARFF data file, generated by KNIME\n");
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
            long rowCnt = inData[0].size();
            long rowNr = 0;
            for (DataRow row : inData[0]) {

                rowNr++;
                exec.setProgress(rowNr / (double)rowCnt, "Writing row " + rowNr
                        + " ('" + row.getKey() + "') of " + rowCnt);

                if (m_sparse) {
                    writer.write("{");
                }
                boolean first = true; // flag to skip comma in first column
                for (int c = 0; c < row.getNumCells(); c++) {
                    DataCell cell = row.getCell(c);

                    if (m_sparse && !cell.isMissing()) {
                        // we write only non-zero values in a sparse file
                        if ((cell instanceof IntValue) && (((IntValue)cell).getIntValue() == 0)) {
                            continue;
                        }
                        if ((cell instanceof DoubleValue) && (Math.abs(((DoubleValue)cell).getDoubleValue()) < 1e-29)) {
                            continue;
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
                exec.checkCanceled();
            } // while (!rIter.atEnd())
        } catch (CanceledExecutionException ex) {
            if (localPath != null) {
                Files.deleteIfExists(localPath);
                LOGGER.debug("File '" + localPath + "' deleted.");
            }
            throw ex;
        }

        // execution successful return empty array
        return new BufferedDataTable[0];
    }

    private static BufferedWriter openWriter(final Path localPath, final URL url) throws IOException {
        if (localPath != null) {
            return Files.newBufferedWriter(localPath, Charset.forName("UTF-8"));
        } else {
            OutputStream os = FileUtil.openOutputConnection(url, "PUT").getOutputStream();
            return new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        }
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // doodledoom.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkFileAccess(m_location, true);

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

    /**
     * Helper that checks some properties for the file argument.
     *
     * @param fileName The file to check
     * @throws InvalidSettingsException if that fails
     */
    private void checkFileAccess(final String fileName, final boolean showWarnings)
            throws InvalidSettingsException {
        String warning = CheckUtils.checkDestinationFile(fileName, m_overwriteOK);
        if ((warning != null) && showWarnings) {
            setWarningMessage(warning);
        }
    }
}
