/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   02.12.2004 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.util.Vector;

import org.knime.core.node.NodeLogger;

/**
 * @deprecated use {@link org.knime.core.util.tokenizer.SettingsStatus} instead.
 *             Will be removed in Ver3.0.
 */
@Deprecated
public class SettingsStatus {

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SettingsStatus.class);

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
     * Prints all error messages into the logger as error. It will print
     * "Error: ", the error message and a new line character.
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
