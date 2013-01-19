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
 */
package org.knime.core.node.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;


/**
 * Utility class that keeps a list of recently used String, mostly
 * used in File-Reader and Writer that keep track of a list of recently
 * accessed file names. This class also makes sure that the list is made
 * persistent and saved to disk when the application closes. The list
 * is read again from file on startup.
 *
 * <p>Usage in a short way: Determine a (possibly unique) ID for your
 * history to use (I assume that there are not that many?) and get a history
 * object by invoking <code>StringHistory.getInstance(yourID)</code>. You
 * can add recently used String to this object by invoking the
 * <code>add(String)</code> method and get a list of String from the
 * <code>getHistory()</code> method which will return the history in the order
 * the add method on the String objects has been called with the last element
 * added being the first element in the history result.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class StringHistory {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(StringHistory.class);

    /** List of all StringHistory elements, filled lazy, i.e. on request */
    private static final Hashtable<String, StringHistory> INSTANCES =
        new Hashtable<String, StringHistory>();

    /** Get a history for an ID. If that ID doesn't yet exist, a new
     * object is created.
     * @param id The ID of interest
     * @return The History for this id. If this id has been used in a
     *         previous run, the history is loaded from file
     */
    public static synchronized StringHistory getInstance(final String id) {
        StringHistory value = INSTANCES.get(id);
        if (value == null) {
            value = new StringHistory(8, id);
            INSTANCES.put(id, value);
        }
        return value;
    }

    private final int m_maxLength;
    private final File m_historyFile;
    private final LinkedList<String> m_list;

    /**
     * private constructor.
     * @param maxLength The length of the history
     * @param id The id
     */
    private StringHistory(final int maxLength, final String id) {
        m_maxLength = maxLength;
        String file = KNIMEConstants.getKNIMEHomeDir() + File.separator
            + "history_" + id + ".txt";
        m_historyFile = new File(file);
        m_list = new LinkedList<String>();
        load();
    }

    /** Adds a new String to the history. If this string already exists
     * in the history, it is marked as most recently added and will be returned
     * first on a immediate call of <code>getHistory()</code>
     * @param str The string to add.
     * @throws NullPointerException If argument is null.
     */
    public synchronized void add(final String str) {
        if (str == null) {
            throw new NullPointerException("String must not be null.");
        }
        int index = m_list.indexOf(str);
        if (index >= 0) {
            m_list.remove(index);
        } else {
            if (m_list.size() >= m_maxLength) {
                m_list.removeLast();
            }
        }
        m_list.addFirst(str);
        save();
    }

    /** Get the history in an array with the most recently added element first.
     * @return The history.
     */
    public synchronized String[] getHistory() {
        return m_list.toArray(new String[0]);
    }

    /** Removes all entries from the history. */
    public synchronized void clearHistory() {
        m_list.clear();
        save();
    }

    /** Loads from file. */
    private void load() {
        m_list.clear();
        if (!m_historyFile.exists()) {
            LOGGER.info("History file '" + m_historyFile.getAbsolutePath()
                    + "' does not exist.");
            return;
        }
        try {
            BufferedReader reader =
                new BufferedReader(new FileReader(m_historyFile));
            String hist = null;
            while ((hist = reader.readLine()) != null
                    && m_list.size() < m_maxLength) {
                m_list.add(hist);
            }
            reader.close();
        } catch (IOException ioe) {
            LOGGER.warn("Can't read history file '"
                    + m_historyFile.getAbsolutePath() + "'", ioe);
        }
    }

    /** Saves to file. */
    private void save() {
        try {
            BufferedWriter writer =
                new BufferedWriter(new FileWriter(m_historyFile));
            for (String hist : m_list) {
                writer.write(hist);
                writer.newLine();
            }
            writer.close();
        } catch (IOException ioe) {
            LOGGER.warn("Unable to write to file '"
                    + m_historyFile.getAbsolutePath() + "'", ioe);
        }
    }
}
