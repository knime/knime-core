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
 * History
 *   02.12.2004 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.util.Vector;

import org.knime.core.node.NodeLogger;


/**
 * An object to pass messages. It supports information, warning, and error
 * messages.
 *
 * @author Peter Ohl, University of Konstanz
 * @deprecated use org.knime.core.util.tokenizer.SettingsStatus instead
 */
@Deprecated
public class SettingsStatus {

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(SettingsStatus.class);

    private final Vector<String> m_infos;

    private final Vector<String> m_warnings;

    private final Vector<String> m_errors;

    /** Creates a new status object with no messages. */
    public SettingsStatus() {
        m_infos = new Vector<String>();
        m_warnings = new Vector<String>();
        m_errors = new Vector<String>();
    }

    /**
     * Adds an informational message to the status object.
     *
     * @param msg the info message to add
     */
    public void addInfo(final String msg) {
        m_infos.add(msg);
    }

    /**
     * @return the number of informational messages stored
     */
    public int getNumOfInfos() {
        return m_infos.size();
    }

    /**
     * @param idx the index of the info message to return (must be
     *            0...NumOfInfos-1)
     * @return the idx-th informational message
     */
    public String getInfoMessage(final int idx) {
        return m_infos.get(idx);
    }

    /**
     * Adds a warning message to the status object.
     *
     * @param msg the warning message to add
     */
    public void addWarning(final String msg) {
        m_warnings.add(msg);
    }

    /**
     * @return the number of warning messages stored.
     */
    public int getNumOfWarnings() {
        return m_warnings.size();
    }

    /**
     * @param idx the index of the warning message to return (must be
     *            0...NumOfWarnings-1)
     * @return the idx-th warning message
     */
    public String getWarningMessage(final int idx) {
        return m_warnings.get(idx);
    }

    /**
     * Adds an error message to the status object.
     *
     * @param msg the error message to add
     */
    public void addError(final String msg) {
        m_errors.add(msg);
    }

    /**
     * @return the number of error messages stored
     */
    public int getNumOfErrors() {
        return m_errors.size();
    }

    /**
     * @param idx the index of the error message to return (must be
     *            0...NumOfErors-1)
     * @return the idx-th error message
     */
    public String getErrorMessage(final int idx) {
        return m_errors.get(idx);
    }

    /**
     * @return the number of all messages, i.e. number of errors plus infos plus
     *         warnings.
     */
    public int getNumOfAllMessages() {
        return m_errors.size() + m_warnings.size() + m_infos.size();
    }

    /**
     * Prints all error messages into the logger as error. It will print "Error: ",
     * the error message and a new line character.
     */
    public void printErrors() {
        for (int e = 0; e < getNumOfErrors(); e++) {
            LOGGER.error(getErrorMessage(e));
        }
    }

    /**
     * Prints all warning messages into the logger as warning. It will print
     * "Warning: ", the message and a new line character.
     */
    public void printWarnings() {
        for (int w = 0; w < getNumOfWarnings(); w++) {
            LOGGER.warn(getWarningMessage(w));
        }
    }

    /**
     * Prints all info messages into the logger as info. It will print "Info: ",
     * the message and a new line character.
     */
    public void printInfos() {
        for (int i = 0; i < getNumOfInfos(); i++) {
            LOGGER.info(getInfoMessage(i));
        }

    }

    /**
     * Creates a string containing concatenated error messages, separated by a
     * new line ('\n') character. The maximum number of error messages included
     * is determined by the parameter maxMsg. If more error than maxMsg messages
     * are stored in the status object, the subset of messages included in the
     * string is determined by the order the messages were added to the status
     * object. The first maxMsg messages will be used. Result could be an empty
     * string, never <code>null</code>.
     *
     * @param maxMsg the max number of error messages to include in the result.
     *            If set to a number less than or equal 0 all error messages are
     *            included.
     * @return a String containing the first maxMsg error messages. Result will
     *         be an empty string if status contains no error messages.
     */
    public String getAllErrorMessages(final int maxMsg) {

        StringBuffer result = new StringBuffer();
        for (int e = 0; e < getNumOfErrors(); e++) {
            if (e > 0) {
                result.append('\n');
            }
            result.append(getErrorMessage(e));

            if ((maxMsg > 0) && ((e + 1) >= maxMsg)) {
                break;
            }
        }
        return result.toString();
    }
}
