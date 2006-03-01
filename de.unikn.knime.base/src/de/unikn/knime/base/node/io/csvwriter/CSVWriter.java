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
 *   Dec 17, 2005 (wiswedel): created
 */
package de.unikn.knime.base.node.io.csvwriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.IntValue;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.StringValue;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.NodeLogger;

/**
 * Class to write a <code>DataTable</code> to a file or an output 
 * stream. Only known types can be written to it, i.e. each column must
 * be compatible to either <code>DoubleValue</code>, <code>IntValue</code>,
 *  or <code>StringValue</code>. 
 * @author wiswedel, University of Konstanz
 */
public class CSVWriter extends BufferedWriter {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(CSVWriter.class);
    
    /** true for: write also the row header values. */
    private boolean m_isWriteRowHeader = true;
    /** true for: write also the column header values. */
    private boolean m_isWriteColHeader = true;
    /** The string that's written when missing cells are encountered. */
    private String m_missing = "";
    /** Separation character. */
    private char m_sepChar = ',';
    /** Remove separation character if it appears inside a string (Excel...). */
    private boolean m_removeSepCharInStrings = false;

    /** Creates new instance which writes tables to the given writer class.
     * An immediate write operation, will write the table headers (both column
     * and row headers) and will write missing values as "" (empty string).
     * @param writer To write to
     */
    public CSVWriter(final Writer writer) {
        super(writer);
    }
    
    
    /**
     * Writes <code>table</code> with current settings.
     * @param table The table to write to the file.
     * @param exec An execution monitor where to check for canceled status
     * and report progress to. (In case of cancellation, the file will be 
     * deleted.)
     * @throws IOException If any related I/O error occurs.
     * @throws CanceledExecutionException If execution in <code>exec</code>
     * has been canceled.
     * @throws NullPointerException If table is <code>null</code>. 
     */
    public void write(final DataTable table, final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        DataTableSpec inSpec = table.getDataTableSpec();
        final int colCount = inSpec.getNumColumns();
        boolean first; // is it the first entry in the row (skip comma then)
        // write column names
        if (m_isWriteColHeader) {
            String debugmessage = "Writing Header (" + colCount + " columns)."; 
            LOGGER.debug(debugmessage);
            exec.setMessage(debugmessage);
            if (m_isWriteRowHeader) {
                write("\"rowkey\""); // rowheader header
                first = false;
            } else {
                first = true;
            }
            // 
            for (int i = 0; i < colCount; i++) {
                DataCell colName = inSpec.getColumnSpec(i).getName();
                if (!first) {
                    write(m_sepChar);
                }
                first = false;
                String cName = colName.toString();
                if (m_removeSepCharInStrings) {
                    cName.replace("" + m_sepChar, "");
                }
                write("\"" + cName + "\"");
            }
            newLine();
        } // if write column names

        // write data
        int i = 0;
        for (RowIterator it = table.iterator(); it.hasNext(); i++) {
            final DataRow next = it.next();
            String rowKey = next.getKey().toString();
            String debugMessage = "Writing row " + (i + 1) 
                + " (\"" + rowKey + "\")"; 
            exec.setProgress(-1, debugMessage);
            // Check if execution was canceled !
            exec.checkCanceled();

            first = true;
            if (m_isWriteRowHeader) {
                write("\"" + next.getKey().getId().toString() + "\"");
                first = false;
            }
            for (int c = 0; c < colCount; c++) {
                DataCell colValue = next.getCell(c);
                boolean isMissing = colValue.isMissing();
                if (!first) {
                    write(m_sepChar);
                }
                first = false;
                // write according to column type (quote strings)
                DataType type = inSpec.getColumnSpec(c).getType();
                String toString;
                if (isMissing) {
                    toString = m_missing;
                } else if (type.isCompatible(IntValue.class)) {
                    toString = "" + ((IntValue)colValue).getIntValue();
                } else if (type.isCompatible(DoubleValue.class)) {
                    toString = "" + ((DoubleValue)colValue).getDoubleValue();
                } else if (type.isCompatible(StringValue.class)) {
                    toString = "\"" + ((StringValue)colValue).getStringValue()
                            + "\"";
                    if (m_removeSepCharInStrings) {
                        toString.replace("" + m_sepChar, "");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Table must not contain other types than strings"
                                    + "or doubles. (\""
                                    + type.getClass().getName() + "\" at "
                                    + "column " + c + ")");
                }
                write(toString);
            }
            newLine();
        }
    }


    /**
     * @return Returns the isWriteColHeader.
     */
    public boolean isWriteColHeader() {
        return m_isWriteColHeader;
    }


    /**
     * @param isWriteColHeader The isWriteColHeader to set.
     */
    public void setWriteColHeader(final boolean isWriteColHeader) {
        m_isWriteColHeader = isWriteColHeader;
    }


    /**
     * @return Returns the isWriteRowHeader.
     */
    public boolean isWriteRowHeader() {
        return m_isWriteRowHeader;
    }


    /**
     * @param isWriteRowHeader The isWriteRowHeader to set.
     */
    public void setWriteRowHeader(final boolean isWriteRowHeader) {
        m_isWriteRowHeader = isWriteRowHeader;
    }

    /** use other than the usual "," charater inbetween columns. (Excel
     * says hi).
     * 
     * @param sepChar new separation character
     * @param removeFromStrings remove sep chars from strings (another
     *   Excel feature)
     */
    public void setSepChar(final char sepChar, final boolean removeFromStrings)
    {
        m_sepChar = sepChar;
        m_removeSepCharInStrings = removeFromStrings;
    }

    /**
     * @return Returns the missing.
     */
    public String getMissing() {
        return m_missing;
    }


    /**
     * The string for missing cells. Must not contain ',' (comma) as that serves
     * to separate fields. Also new line characters are not permitted.
     * <p><code>null</code> is ok (uses "" string).
     * @param missing The missing to set.
     */
    public void setMissing(final String missing) {
        String newMissing = missing == null ? "" : missing;
        if (newMissing.indexOf(',') >= 0) {
            throw new IllegalArgumentException(
                    "Comma not allowed as separator: " + newMissing);
        }
        if (newMissing.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(
                    "\\n not allowed as separator: " + newMissing);
        }
        m_missing = missing;
    }
    
}
