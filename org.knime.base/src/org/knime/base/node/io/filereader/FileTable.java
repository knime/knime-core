/*
 * ---------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.io.filereader;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * Implements a {@link DataTable} that reads data from an ASCII file.
 *
 * To instantiate this table you need to specify {@link FileReaderSettings} and
 * a {@link org.knime.core.data.DataTableSpec}. File reader settings define
 * from where and how to read the data, the table spec specifies the structure
 * of the table to create.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class FileTable implements DataTable {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(FileTable.class);

    // the spec of the structure of the table
    private final DataTableSpec m_tableSpec;

    // the settings for the file reader and tokenizer
    private final FileReaderSettings m_frSettings;

    // the execution context to which the progress is reported
    private final ExecutionContext m_exec;

    private final boolean[] m_skipColums;

    // list of all iterators to close the source, when the table is disposed of
    private final LinkedList<WeakReference<FileRowIterator>> m_iterators;

    /**
     * Creates a new file table with the structure defined in tableSpec and
     * using the settings in frSettings when the file is read.
     *
     * @param tableSpec a table spec defining the structure of the table to
     *            create
     * @param frSettings FileReaderSettings specifying the wheres and hows for
     *            reading the ASCII data file
     * @param exec the execution context the progress is reported to; if null,
     *            no progress is reported
     */
    public FileTable(final DataTableSpec tableSpec,
            final FileReaderSettings frSettings, final ExecutionContext exec) {
        this(tableSpec, frSettings,
                createFalseArray(tableSpec.getNumColumns()), exec);
    }

    /**
     * Creates a new file table with the structure defined in tableSpec and
     * using the settings in frSettings when the file is read.
     *
     * @param tableSpec a table spec defining the structure of the table to
     *            create
     * @param frSettings FileReaderSettings specifying the wheres and hows for
     *            reading the ASCII data file
     * @param skipColumns array with the element set to true if the
     *            corresponding column should be skipped (i.e. read but not be
     *            included in the row). The array must have the length of the
     *            'original' column number (in the file), the specified table
     *            spec is the new one (with less columns).
     * @param exec the execution context the progress is reported to; if null,
     *            no progress is reported
     */
    public FileTable(final DataTableSpec tableSpec,
            final FileReaderSettings frSettings, final boolean[] skipColumns,
            final ExecutionContext exec) {

        if ((tableSpec == null) || (frSettings == null)) {
            throw new NullPointerException("Must specify non-null table spec"
                    + " and file reader settings for file table.");
        }
        if (skipColumns.length < tableSpec.getNumColumns()) {
            throw new IllegalArgumentException("The number of columns can't"
                    + " be larger than the spec for columns to skip");
        }
        int cols = 0;
        for (boolean b : skipColumns) {
            if (!b) {
                cols++;
            }
        }
        if (cols != tableSpec.getNumColumns()) {
            throw new IllegalArgumentException("The number of columns to "
                    + "include is different from the number of columns in the"
                    + " table spec.");
        }
        m_iterators = new LinkedList<WeakReference<FileRowIterator>>();
        m_tableSpec = tableSpec;
        m_frSettings = frSettings;
        m_skipColums = skipColumns;
        m_exec = exec;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Call this before releasing the last reference to this table and all its
     * iterators. It closes the underlying source for all iterators. Especially
     * if an iterator didn't run to the end of the table, it is required to call
     * this method. Otherwise the file handle is not released until the garbage
     * collector cleans up. A call to <code>next()</code> of any iterator
     * after disposing of the iterator has undefined behavior.
     */
    public void dispose() {
        synchronized (m_iterators) {
            for (WeakReference<FileRowIterator> w : m_iterators) {
                FileRowIterator i = w.get();
                if (i != null) {
                    i.dispose();
                }
            }
            m_iterators.clear();
        }
    }

    private static boolean[] createFalseArray(final int length) {
        boolean[] result = new boolean[length];
        Arrays.fill(result, false);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public FileRowIterator iterator() {
        try {
            synchronized (m_iterators) {
                FileRowIterator i =
                        new FileRowIterator(m_frSettings, m_tableSpec,
                                m_skipColums, m_exec);
                m_iterators.add(new WeakReference<FileRowIterator>(i));
                return i;

            }
        } catch (IOException ioe) {
            LOGGER.error("I/O Error occurred while trying to open a stream"
                    + " to '" + m_frSettings.getDataFileLocation().toString()
                    + "'.");
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_tableSpec;
    }

    /**
     * Method to check consistency and completeness of the current settings. It
     * will return a {@link SettingsStatus} object which contains info, warning
     * and error messages. Or if the settings are alright it will return null.
     *
     * @param openDataFile tells whether or not this method should try to access
     *            the data file. This will - if set <code>true</code> - verify
     *            the accessibility of the data.
     * @return a SettingsStatus object containing info, warning and error
     *         messages, or <code>null</code> if no messages were generated
     *         (i.e. all settings are just fine)
     */
    public SettingsStatus getStatusOfSettings(final boolean openDataFile) {

        SettingsStatus status = new SettingsStatus();

        addStatusOfSettings(status, openDataFile);

        return status;
    }

    /**
     * Adds its status messages to a passed status object.
     *
     * @param status the object to add messages to - if any.
     * @param openDataFile specifies if we should check the accessibility of the
     *            data file.
     */

    public void addStatusOfSettings(final SettingsStatus status,
            final boolean openDataFile) {

        if (m_tableSpec == null) {
            status.addError("DataTableSpec not set!");
        }
        if (m_frSettings == null) {
            status.addError("FileReader settings not set!");
        } else {
            // we do that here - still.
            addTableSpecStatusOfSettings(status, m_tableSpec);
            m_frSettings.addStatusOfSettings(status, openDataFile, m_tableSpec);
        }

    }

    /*
     * Method to check consistency and completeness of the current settings of
     * the data table spec set. It add info, warning and error messages to the
     * status object passed, if something is wrong with the settings.
     */
    private void addTableSpecStatusOfSettings(final SettingsStatus status,
            final DataTableSpec tableSpec) {
        // check the number of columns. Must be set to a number > 0.
        if (tableSpec.getNumColumns() < 1) {
            status.addError("Number of columns must be greater than zero.");
        }

        // hash map for faster column name uniqueness checking
        HashMap<String, Integer> colNames = new HashMap<String, Integer>();

        // we need a column spec for each column - and if set we need types,
        // names, and if possible values are set they must not be null.
        for (int c = 0; c < tableSpec.getNumColumns(); c++) {

            if (tableSpec.getColumnSpec(c) == null) {
                status.addError("Column spec for column with index '" + c
                        + "' is not set.");
            } else {
                // check col type
                DataColumnSpec cSpec = tableSpec.getColumnSpec(c);
                DataType cType = cSpec.getType();
                if (cType == null) {
                    status.addError("Column type for column with index '" + c
                            + "' is not set.");
                } else {
                    if (!DataType.class.isAssignableFrom(cType.getClass())) {
                        status.addError("The type of the column with index '"
                                + c + "' is not derived from DataType.");
                    }
                }

                // check col name
                String cName = cSpec.getName();
                if (cName == null) {
                    status.addError("Column name for column with index '" + c
                            + "' is not set.");
                } else {
                    // make sure it's unique
                    Integer sameCol = colNames.put(cName, c);
                    if (sameCol != null) {
                        status.addError("Column with index " + c
                                + " has the same name as column " + sameCol
                                + " ('" + cName + "').");
                    }
                }

                // check the possible values, in case they are set.
                Set<DataCell> values = cSpec.getDomain().getValues();
                if (values == null) {
                    status.addInfo("No possible values set for column with"
                            + " index '" + c + "'.");
                } else {
                    if (values.size() == 0) {
                        status.addWarning("The container for the possible "
                                + " values of the column with index '" + c
                                + "' is empty!");
                    }
                    // if set they must be not null
                    for (DataCell v : values) {
                        if (v == null) {
                            status.addError("One of the possible values set"
                                    + " for the column with index '" + c
                                    + "' is" + " null.");
                            // adding this message once for each col is enough
                            break;
                        }
                    }
                }
            } // end of if column spec is not null
        } // end of for all columns

    } // addTableSpecStatusOfSettings(SettingsStatus, DataTableSpec)

    /**
     * Returns a string summary for this table which is the entire table
     * content. Note, this call might be time consuming since this method
     * iterates over the table to retrieve all data.
     *
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        // maximum number of chars to print
        final int colLength = 15;
        RowIterator rowIterator = iterator();
        DataRow row;
        StringBuffer result = new StringBuffer();

        // Create a column header
        // Cell (0,0)
        result.append(sprintDataCell(" ", colLength));
        // "<ColName>[Type]"
        for (int i = 0; i < m_tableSpec.getNumColumns(); i++) {
            if (m_tableSpec.getColumnSpec(i).getType().equals(
                    StringCell.TYPE)) {
                result.append(sprintDataCell(
                        m_tableSpec.getColumnSpec(i).getName().toString()
                        + "[Str]", colLength));
            } else if (m_tableSpec.getColumnSpec(i).getType().equals(
                    IntCell.TYPE)) {
                result.append(sprintDataCell(
                        m_tableSpec.getColumnSpec(i).getName().toString()
                        + "[Int]", colLength));
            } else if (m_tableSpec.getColumnSpec(i).getType().equals(
                    DoubleCell.TYPE)) {
                result.append(sprintDataCell(
                        m_tableSpec.getColumnSpec(i).getName().toString()
                        + "[Dbl]", colLength));
            } else {
                result.append(sprintDataCell(
                        m_tableSpec.getColumnSpec(i).getName().toString()
                        + "[UNKNOWN!!]", colLength));
            }
        }
        result.append("\n");
        while (rowIterator.hasNext()) {
            row = rowIterator.next();
            result.append(sprintDataCell(row.getKey().getString(), colLength));
            for (int i = 0; i < row.getNumCells(); i++) {
                result.append(
                        sprintDataCell(row.getCell(i).toString(), colLength));
            }
            result.append("\n");
        }
        return result.toString();
    } // toString()

    /*
     * Returns a left aligned, 'length' characters (or more) long string
     * containing the string representation of the value in the DataCell dc.
     * @param dc The value. @param length The length of chars. @return A left
     * aligned string representation.
     */
    private static String sprintDataCell(final String dc, final int length) {
        assert (dc != null);
        // the final string, with all the spaces
        final StringBuffer result = new StringBuffer(dc);
        for (int i = result.length(); i < length; i++) {
            result.append(" ");
        }
        return result.toString();
    }
}
