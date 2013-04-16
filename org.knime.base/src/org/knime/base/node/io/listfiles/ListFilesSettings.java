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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.listfiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.knime.base.node.io.listfiles.ListFiles.Filter;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.StringHistory;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ListFilesSettings {

    /** ID for the file history. */
    private static final String LIST_FILES_HISTORY_ID = "List Files History ID";

    /** ID for the extension history. */
    private static final String LIST_FILES_EXT_HISTORY_ID =
            "LIST_FILES_EXT_HISTORY_ID";

    /** Key to store the location settings. */
    public static final String LOCATION_SETTINGS = "FILESETTINGS";

    /** Key to store the RECURSIVE_SETTINGS. */
    public static final String RECURSIVE_SETTINGS = "RECURSIVE_SETTINGS";

    /** Key to store the Filter Settings. */
    public static final String FILTER_SETTINGS = "FILTER_SETTINGS";

    /** Key to store the case sensitive settings. */
    public static final String CASE_SENSITIVE_STRING = "CASESENSITVE";

    /** Key to store the extension_settings. */
    public static final String EXTENSIONS_SETTINGS = "EXTENSIONS";

    /** the folders to be analyzed. */
    private String m_locationString = null;

    /** contains the log-format of the files. */
    private String m_extensionsString;

    /** recursive flag. */
    private boolean m_recursive = false;

    /** Flag to switch between case sensitive and insensitive. */
    private boolean m_caseSensitive = false;

    /** Filter type. */
    private Filter m_filter = Filter.None;

    /** @return the locationString */
    public String getLocationString() {
        return m_locationString;
    }

    /** @param locationString the locationString to set */
    public void setLocationString(final String locationString) {
        m_locationString = locationString;
    }

    /** @return the extensionsString */
    public String getExtensionsString() {
        return m_extensionsString;
    }

    /** @param extensionsString the extensionsString to set */
    public void setExtensionsString(final String extensionsString) {
        m_extensionsString = extensionsString;
    }

    /** @return the recursive */
    public boolean isRecursive() {
        return m_recursive;
    }

    /** @param recursive the recursive to set */
    public void setRecursive(final boolean recursive) {
        m_recursive = recursive;
    }

    /** @return the caseSensitive */
    public boolean isCaseSensitive() {
        return m_caseSensitive;
    }

    /** @param caseSensitive the caseSensitive to set */
    public void setCaseSensitive(final boolean caseSensitive) {
        m_caseSensitive = caseSensitive;
    }

    /** @return the filter */
    public Filter getFilter() {
        return m_filter;
    }

    /**
     * @param filter the filter to set
     * @throws NullPointerException If argument is null.
     */
    public void setFilter(final Filter filter) {
        if (filter == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_filter = filter;
    }

    /**
     * Split location string by ";" and return individual directories.
     *
     * @return A list of files representing directories.
     * @throws InvalidSettingsException If the argument is invalid or does not
     *             represent a list of existing directories.
     */
    public Collection<File> getDirectoriesFromLocationString()
            throws InvalidSettingsException {
        if (m_locationString == null || m_locationString.equals("")) {
            throw new InvalidSettingsException("Please select a folder!");
        }
        String[] subs = m_locationString.split(";");
        List<File> result = new ArrayList<File>();
        for (String s : subs) {
            File f = new File(s);
            if (!f.isDirectory()) {
                try {
                    if (s.startsWith("file:")) {
                        s = s.substring(5);
                    }
                    f = new File(URIUtil.decode(s));
                } catch (URIException ex) {
                    throw new InvalidSettingsException("\"" + s
                            + "\" does not exist or is not a directory");
                }
                if (!f.isDirectory()) {
                    throw new InvalidSettingsException("\"" + s
                            + "\" does not exist or is not a directory");
                }
            }
            result.add(f);
        }
        return result;
    }

    /**
     * Load settings, fail if incomplete.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If that fails.
     */
    protected void loadSettingsInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_locationString = settings.getString(LOCATION_SETTINGS);
        if (m_locationString == null) {
            throw new InvalidSettingsException("No location given.");
        }
        m_extensionsString = settings.getString(EXTENSIONS_SETTINGS);
        m_recursive = settings.getBoolean(RECURSIVE_SETTINGS);
        String filterS = settings.getString(FILTER_SETTINGS);
        if (filterS == null) {
            throw new InvalidSettingsException("No filter provided");
        }
        try {
            m_filter = Filter.valueOf(filterS);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException("Invalid filter: " + filterS);
        }
        m_caseSensitive = settings.getBoolean(CASE_SENSITIVE_STRING);
    }

    /**
     * Load settings in dialog (no fail).
     *
     * @param settings To load from.
     */
    protected void loadSettingsInDialog(final NodeSettingsRO settings) {
        m_locationString = settings.getString(LOCATION_SETTINGS, "");
        m_extensionsString = settings.getString(EXTENSIONS_SETTINGS, "");
        m_recursive = settings.getBoolean(RECURSIVE_SETTINGS, false);
        final Filter defFilter = Filter.None;
        String filterS = settings.getString(FILTER_SETTINGS, defFilter.name());
        if (filterS == null) {
            filterS = defFilter.name();
        }
        try {
            m_filter = Filter.valueOf(filterS);
        } catch (IllegalArgumentException iae) {
            m_filter = defFilter;
        }
        m_caseSensitive = settings.getBoolean(CASE_SENSITIVE_STRING, false);
    }

    /**
     * Save settings in model & dialog.
     *
     * @param settings To save to.
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(EXTENSIONS_SETTINGS, m_extensionsString);
        settings.addString(LOCATION_SETTINGS, m_locationString);
        settings.addBoolean(RECURSIVE_SETTINGS, m_recursive);
        settings.addString(FILTER_SETTINGS, m_filter.name());
        settings.addBoolean(CASE_SENSITIVE_STRING, m_caseSensitive);

        if (m_locationString != null) {
            StringHistory h = StringHistory.getInstance(LIST_FILES_HISTORY_ID);
            h.add(m_locationString);
        }

        if (m_extensionsString != null) {
            StringHistory h =
                    StringHistory.getInstance(LIST_FILES_EXT_HISTORY_ID);
            h.add(m_extensionsString);

        }
    }

    /** @return the previously analyzed folders. */
    static String[] getLocationHistory() {
        StringHistory h = StringHistory.getInstance(LIST_FILES_HISTORY_ID);
        return h.getHistory();
    }

    /** @return the previous selected extension field strings. */
    static String[] getExtensionHistory() {
        StringHistory h = StringHistory.getInstance(LIST_FILES_EXT_HISTORY_ID);
        return h.getHistory();
    }

}
