/*
 * ------------------------------------------------------------------
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
 * History:
 *   Dec 17, 2005 (wiswedel): created
 *   Mar  7, 2007 (ohl): extended with more options
 */
package org.knime.base.node.io.csvwriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.knime.base.node.io.csvwriter.FileWriterNodeSettings.FileOverwritePolicy;
import org.knime.core.data.DataTable;
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
import org.knime.core.node.util.StringHistory;

/**
 * NodeModel to write a DataTable to a CSV (comma separated value) file.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class CSVWriterNodeModel extends NodeModel {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(CSVWriterNodeModel.class);

    private FileWriterNodeSettings m_settings;

    /**
     * Identifier for StringHistory.
     *
     * @see StringHistory
     */
    public static final String FILE_HISTORY_ID = "csvwrite";

    /**
     * Constructor, sets port count.
     */
    public CSVWriterNodeModel() {
        super(1, 0);
        m_settings = new FileWriterNodeSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        // the constructor complains if settings are missing
        FileWriterNodeSettings fws = new FileWriterNodeSettings(settings);

        // check consistency of settings

        String fileName = fws.getFileName();
        if (fileName == null || fileName.length() == 0) {
            throw new InvalidSettingsException("Missing output file name.");
        }

        // the separator must not be contained in the missing value pattern
        // nor in the quote begin pattern.
        if (notEmpty(fws.getColSeparator())) {
            if (notEmpty(fws.getMissValuePattern())) {
                if (fws.getMissValuePattern().contains(fws.getColSeparator())) {
                    throw new InvalidSettingsException(
                            "The pattern for missing values ('"
                                    + fws.getMissValuePattern()
                                    + "') must not contain the data "
                                    + "separator ('" + fws.getColSeparator()
                                    + "').");
                }
            }

            if (notEmpty(fws.getCommentBegin())) {
                if (fws.getCommentBegin().contains(fws.getColSeparator())) {
                    throw new InvalidSettingsException(
                            "The left quote pattern ('" + fws.getQuoteBegin()
                                    + "') must not contain the data "
                                    + "separator ('" + fws.getColSeparator()
                                    + "').");
                }
            }
        }

        // if we are supposed to add some creation data, we need to know
        // the comment pattern
        if (fws.addCreationTime() || fws.addCreationUser()
                || fws.addTableName() || notEmpty(fws.getCustomCommentLine())) {
            if (isEmpty(fws.getCommentBegin())) {
                throw new InvalidSettingsException(
                        "The comment pattern must be defined in order to add "
                                + "user, creation date or table name");
            }
            // if the end pattern is empty, assume a single line comment and
            // write the comment begin pattern in every line.
        }

        // if a custom comment line is specified, is must not contain the
        // comment end pattern
        if (notEmpty(fws.getCustomCommentLine())
                && notEmpty(fws.getCommentEnd())) {
            if (fws.getCustomCommentLine().contains(fws.getCommentEnd())) {
                throw new InvalidSettingsException(
                        "The specified comment to add must not contain the"
                                + " comment end pattern.");
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings = new FileWriterNodeSettings(settings);

        if (notEmpty(m_settings.getFileName())) {
            StringHistory history = StringHistory.getInstance(FILE_HISTORY_ID);
            history.add(m_settings.getFileName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException,
            IOException {

        DataTable in = data[0];

        File file = new File(m_settings.getFileName());
        File parentDir = file.getParentFile();
        boolean dirCreated = false;
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IllegalStateException("Unable to create directory"
                        + " for specified output file: "
                        + parentDir.getAbsolutePath());
            }
            LOGGER.info("Created directory for specified output file: "
                    + parentDir.getAbsolutePath());
            dirCreated = true;
        }

        // figure out if the writer is actually supposed to write col headers
        boolean writeColHeader = m_settings.writeColumnHeader();
        if (writeColHeader && file.exists()) {
            if (m_settings.getFileOverwritePolicy().equals(
                    FileOverwritePolicy.Append)) {
                // do not write headers if the file exists and we append to it
                writeColHeader = !m_settings.skipColHeaderIfFileExists();
            }
        }
        // make a copy of the settings with the modified value
        FileWriterSettings writerSettings = new FileWriterSettings(m_settings);
        writerSettings.setWriteColumnHeader(writeColHeader);

        boolean appendToFile;
        if (file.exists()) {
            switch (m_settings.getFileOverwritePolicy()) {
            case Append:
                appendToFile = true;
                break;
            case Abort:
                throw new RuntimeException("File \"" + file.getAbsolutePath()
                        + "\" exists, must not overwrite it (check "
                        + "dialog settings)");
            case Overwrite:
                appendToFile = false;
                break;
            default:
                throw new InternalError("Unknown case: "
                        + m_settings.getFileOverwritePolicy());
            }
        } else {
            appendToFile = false;
        }
        CSVWriter tableWriter =
                new CSVWriter(new FileWriter(file, appendToFile),
                        writerSettings);

        // write the comment header, if we are supposed to
        writeCommentHeader(m_settings, tableWriter, data[0], appendToFile);

        try {
            tableWriter.write(in, exec);
        } catch (CanceledExecutionException cee) {
            LOGGER.info("Table FileWriter canceled.");
            if (dirCreated) {
                LOGGER.warn("The directory for the output file was created and"
                        + " is not removed.");
            }
            tableWriter.close();
            if (file.delete()) {
                LOGGER.debug("File " + m_settings.getFileName() + " deleted.");
                if (dirCreated) {
                    LOGGER.debug("The directory created for the output file"
                            + " is not removed though.");
                }
            } else {
                LOGGER.warn("Unable to delete file '"
                        + m_settings.getFileName() + "' after cancellation.");
            }
            throw cee;
        }

        tableWriter.close();

        if (tableWriter.hasWarningMessage()) {
            setWarningMessage(tableWriter.getLastWarningMessage());
        }

        // execution successful return empty array
        return new BufferedDataTable[0];
    }

    /**
     * Writes a comment header to the file, if specified so in the settings.
     *
     * @param settings where it is specified if and how to write the comment
     *            header
     * @param file the writer to write the header out to.
     * @param inData the table that is going to be written in the file.
     * @param append If the output will be appended to an existing file
     * @throws IOException if something went wrong during writing.
     */
    private void writeCommentHeader(final FileWriterNodeSettings settings,
            final BufferedWriter file, final DataTable inData,
            final boolean append) throws IOException {
        if ((file == null) || (settings == null)) {
            return;
        }
        if (isEmpty(settings.getCommentBegin())) {
            return;
        }

        // figure out if we have to write anything at all:
        boolean writeComment = false;
        writeComment |= settings.addCreationTime();
        writeComment |= settings.addCreationUser();
        writeComment |= settings.addTableName();
        writeComment |= notEmpty(settings.getCustomCommentLine());

        if (!writeComment) {
            return;
        }

        // if we have block comment patterns we write them only once. Otherwise
        // we add the commentBegin to every line.
        boolean blockComment = notEmpty(settings.getCommentEnd());

        if (blockComment) {
            file.write(settings.getCommentBegin());
            file.newLine();
        }

        // add date/time and user, if we are supposed to
        if (settings.addCreationTime() || settings.addCreationUser()) {
            if (!blockComment) {
                file.write(settings.getCommentBegin());
            }
            if (append) {
                file.write("   The following data was added ");
            } else {
                file.write("   This file was created ");
            }
            if (settings.addCreationTime()) {
                file.write("on " + new Date() + " ");
            }
            if (settings.addCreationUser()) {
                file.write("by user '" + System.getProperty("user.name") + "'");
            }
            file.newLine();
        }

        // add the table name
        if (settings.addTableName()) {
            if (!blockComment) {
                file.write(settings.getCommentBegin());
            }
            file.write("   The data was read from the \""
                    + inData.getDataTableSpec().getName() + "\" data table.");
            file.newLine();
        }

        // at last: add the user comment line
        if (notEmpty(settings.getCustomCommentLine())) {
            String[] lines = settings.getCustomCommentLine().split("\n");
            for (String line : lines) {
                if (!blockComment) {
                    file.write(settings.getCommentBegin());
                }
                file.write("   " + line);
                file.newLine();
            }
        }

        // close the block comment
        if (blockComment) {
            file.write(settings.getCommentEnd());
            file.newLine();
        }

    }

    /**
     * Ignored.
     *
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to save.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        String warnMsg = "";

        /*
         * check file access
         */
        String fileName = m_settings.getFileName();
        if (fileName == null || fileName.length() == 0) {
            throw new InvalidSettingsException("No output file specified.");
        }
        File file = new File(fileName);

        if (file.isDirectory()) {
            throw new InvalidSettingsException("Specified location  is a "
                    + "directory (\"" + file.getAbsolutePath() + "\").");
        }
        if (file.exists()) {
            if (!file.canWrite()) {
                throw new InvalidSettingsException("Cannot write to existing "
                        + "file \"" + file.getAbsolutePath() + "\".");
            }
            switch (m_settings.getFileOverwritePolicy()) {
            case Abort:
                throw new InvalidSettingsException("File \""
                        + file.getAbsolutePath()
                        + "\" exists, must not overwrite it (check "
                        + "dialog settings)");
            case Overwrite:
                warnMsg +=
                        "Selected output file exists and will be overwritten!";
                break;
            default:
            }
        } else {
            File parentDir = file.getParentFile();
            if (parentDir == null) {
                throw new InvalidSettingsException("Can't determine parent "
                        + "directory of file \"" + file + "\"");
            }
            if (!parentDir.exists()) {
                warnMsg +=
                        "Directory of specified output file doesn't exist"
                                + " and will be created.";
            }
        }

        /*
         * check settings
         */
        if (isEmpty(m_settings.getColSeparator())
                && isEmpty(m_settings.getMissValuePattern())
                && (isEmpty(m_settings.getQuoteBegin()) || isEmpty(m_settings
                        .getQuoteEnd()))) {
            // we will write the table out - but they will have a hard
            // time reading it in again.
            warnMsg +=
                    "No separator and no quotes and no missing value "
                            + "pattern set."
                            + "\nWritten data will be hard to read!";
        }

        DataTableSpec inSpec = inSpecs[0];
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataType c = inSpec.getColumnSpec(i).getType();
            if (!c.isCompatible(DoubleValue.class)
                    && !c.isCompatible(IntValue.class)
                    && !c.isCompatible(StringValue.class)) {
                throw new InvalidSettingsException(
                        "Input table must only contain "
                                + "String, Int, or Doubles");
            }
        }
        if (inSpec.containsCompatibleType(DoubleValue.class)) {
            if (m_settings.getColSeparator().indexOf(
                    m_settings.getDecimalSeparator()) >= 0) {
                warnMsg +=
                        "The data separator contains (or is equal to) the "
                                + "decimal separator\nWritten data will be hard to read!";
            }
        }

        if (notEmpty(warnMsg)) {
            setWarningMessage(warnMsg);
        }

        return new DataTableSpec[0];
    }

    /**
     * @param s the String to test
     * @return true only if s is not null and not empty (i.e. not of length 0)
     */
    static boolean notEmpty(final String s) {
        if (s == null) {
            return false;
        }
        return (s.length() > 0);
    }

    /**
     * @param s the String to test
     * @return true if s is null or of length zero.
     */
    static boolean isEmpty(final String s) {
        return !notEmpty(s);
    }
}
