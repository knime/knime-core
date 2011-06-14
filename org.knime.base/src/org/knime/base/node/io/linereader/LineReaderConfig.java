/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
package org.knime.base.node.io.linereader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration for line reader node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class LineReaderConfig {

    /** Config key for URL. */
    static final String CFG_URL = "url";

    private String m_url;
    private String m_rowPrefix;
    private String m_columnHeader;
    private boolean m_skipEmptyLines;
    private int m_limitRowCount;

    /** @return the url */
    String getUrlString() {
        return m_url;
    }
    /** @param url the url to set */
    void setUrlString(final String url) {
        m_url = url;
    }
    /** Convert url string into URL, fail if invalid or null/empty.
      * @return The URL
      * @throws InvalidSettingsException if invalid. */
    URL getURL() throws InvalidSettingsException {
        if (m_url == null || m_url.length() == 0) {
            throw new InvalidSettingsException("Invalid (empty) URL");
        }
        if (m_url.toLowerCase().matches("^[a-z]+:/.*")) {
            URL url;
            try {
                url = new URL(m_url);
            } catch (MalformedURLException ex) {
                throw new InvalidSettingsException("Unable to parse URL \""
                        + m_url + "\": " + ex.getMessage(), ex);
            }
            if ("file".equals(url.getProtocol())) {
                try {
                    checkFileAccess(new File(url.toURI()));
                } catch (URISyntaxException e) {
                    throw new InvalidSettingsException(
                            "Can't convert file protocol to URI", e);
                }
            }
            return url;
        } else {
            File f = new File(m_url);
            checkFileAccess(f);
            try {
                return f.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new InvalidSettingsException("Can't retrieve local URL "
                        + "to file \"" + m_url + "\"", e);
            }
        }

    }

    private void checkFileAccess(final File f) throws InvalidSettingsException {
        if (!f.isFile()) {
            throw new InvalidSettingsException("Path \""
                    + f.getAbsolutePath() + "\" does not denote a file");
        }
    }

    /** @return the rowPrefix */
    String getRowPrefix() {
        return m_rowPrefix;
    }
    /** @param rowPrefix the rowPrefix to set */
    void setRowPrefix(final String rowPrefix) {
        m_rowPrefix = rowPrefix;
    }
    /** @return the columnHeader */
    String getColumnHeader() {
        return m_columnHeader;
    }
    /** @param columnHeader the columnHeader to set */
    void setColumnHeader(final String columnHeader) {
        m_columnHeader = columnHeader;
    }

    /** @return the limitRowCount */
    int getLimitRowCount() {
        return m_limitRowCount;
    }
    /** @param limitRowCount the limitRowCount to set */
    void setLimitRowCount(final int limitRowCount) {
        m_limitRowCount = limitRowCount;
    }
    /** @return the skipEmptyLines */
    boolean isSkipEmptyLines() {
        return m_skipEmptyLines;
    }
    /** @param skipEmptyLines the skipEmptyLines to set */
    void setSkipEmptyLines(final boolean skipEmptyLines) {
        m_skipEmptyLines = skipEmptyLines;
    }

    /** Save current configuration.
     * @param settings to save to. */
    final void saveConfiguration(final NodeSettingsWO settings) {
        settings.addString(CFG_URL, m_url);
        settings.addString("rowPrefix", m_rowPrefix);
        settings.addString("columnHeader", m_columnHeader);
        settings.addBoolean("skipEmptyLines", m_skipEmptyLines);
        settings.addInt("limitRowCount", m_limitRowCount);
    }

    /** Load configuration in NodeModel.
     * @param settings To load from.
     * @throws InvalidSettingsException If invalid. */
    final void loadConfigurationInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_url = settings.getString(CFG_URL);
        if (m_url == null || m_url.length() == 0) {
            throw new InvalidSettingsException("Invalid (empty) URL");
        }
        m_rowPrefix = settings.getString("rowPrefix");
        if (m_rowPrefix == null) {
            throw new InvalidSettingsException("Invalid (null) row prefix");
        }
        m_columnHeader = settings.getString("columnHeader");
        if (m_columnHeader == null || m_columnHeader.length() == 0) {
            throw new InvalidSettingsException(
                    "Invalid (empty) column header");
        }
        m_skipEmptyLines = settings.getBoolean("skipEmptyLines");
        m_limitRowCount = settings.getInt("limitRowCount");
    }

    /** Load configuration in dialog, init defaults if invalid.
     * @param settings To load from. */
    final void loadConfigurationInDialog(final NodeSettingsRO settings) {
        m_url = settings.getString(CFG_URL, "");
        m_rowPrefix = settings.getString("rowPrefix", "Row");
        m_columnHeader = settings.getString("columnHeader", "Column");
        m_skipEmptyLines = settings.getBoolean("skipEmptyLines", false);
        m_limitRowCount = settings.getInt("limitRowCount", -1);
    }

}
