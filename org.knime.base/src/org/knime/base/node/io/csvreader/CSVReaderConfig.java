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
 * History
 *   Dec 4, 2009 (wiswedel): created
 */
package org.knime.base.node.io.csvreader;

import java.net.MalformedURLException;
import java.net.URL;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Config for CSV reader.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class CSVReaderConfig {

    /** Config key for the URL property. */
    static final String CFG_URL = "url";

    private URL m_url;
    private String m_colDelimiter;
    private String m_rowDelimiter;
    private String m_quoteString;
    private boolean m_hasRowHeader;
    private boolean m_hasColHeader;
    private String m_commentStart;


    /**
     * Creates a new CSVReaderConfig with default values for all settings
     * except the url.
     */
    public CSVReaderConfig() {
        super();
        m_colDelimiter = ",";
        m_rowDelimiter = "\n";
        m_quoteString = "\"";
        m_commentStart = "#";
        m_hasRowHeader = true;
        m_hasColHeader = true;
    }

    /** Load settings, used in dialog (no errors).
     * @param settings To load from.
     */
    public void loadSettingsInDialog(final NodeSettingsRO settings) {
        String urlS = settings.getString(CFG_URL, null);
        if (urlS != null) {
            try {
                m_url = new URL(urlS);
            } catch (MalformedURLException e) {
                m_url = null;
            }
        }
        m_colDelimiter = settings.getString("colDelimiter", m_colDelimiter);
        m_rowDelimiter = settings.getString("rowDelimiter", m_rowDelimiter);
        m_quoteString = settings.getString("quote", m_quoteString);
        m_commentStart = settings.getString("commentStart", m_commentStart);
        m_hasRowHeader = settings.getBoolean("hasRowHeader", m_hasRowHeader);
        m_hasColHeader = settings.getBoolean("hasColHeader", m_hasColHeader);
    }

    /** Load in model, fail if settings are invalid.
     * @param settings To load from.
     * @throws InvalidSettingsException If invalid.
     */
    public void loadSettingsInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        String urlS = settings.getString(CFG_URL);
        if (urlS == null) {
            throw new InvalidSettingsException("URL must not be null");
        }
        try {
            m_url = new URL(urlS);
        } catch (MalformedURLException e) {
            throw new InvalidSettingsException("Invalid URL: "
                    + e.getMessage(), e);
        }
        m_colDelimiter = settings.getString("colDelimiter");
        m_rowDelimiter = settings.getString("rowDelimiter");
        m_quoteString = settings.getString("quote");
        m_commentStart = settings.getString("commentStart");
        m_hasRowHeader = settings.getBoolean("hasRowHeader");
        m_hasColHeader = settings.getBoolean("hasColHeader");
    }

    /** Save configuration to argument.
     * @param settings To save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_url != null) {
            settings.addString(CFG_URL, m_url.toString());
        }
        settings.addString("colDelimiter", m_colDelimiter);
        settings.addString("rowDelimiter", m_rowDelimiter);
        settings.addString("quote", m_quoteString);
        settings.addString("commentStart", m_commentStart);
        settings.addBoolean("hasRowHeader", m_hasRowHeader);
        settings.addBoolean("hasColHeader", m_hasColHeader);
    }

    /** @return the url */
    URL getUrl() {
        return m_url;
    }

    /** @param url the url to set */
    void setUrl(final URL url) {
        m_url = url;
    }

    /** @return the colDelimiter */
    String getColDelimiter() {
        return m_colDelimiter;
    }

    /** @param colDelimiter the colDelimiter to set */
    void setColDelimiter(final String colDelimiter) {
        m_colDelimiter = colDelimiter;
    }

    /** @return the rowDelimiter */
    String getRowDelimiter() {
        return m_rowDelimiter;
    }

    /** @param rowDelimiter the rowDelimiter to set */
    void setRowDelimiter(final String rowDelimiter) {
        m_rowDelimiter = rowDelimiter;
    }

    /** @return the quoteString */
    String getQuoteString() {
        return m_quoteString;
    }

    /** @param quoteString the quoteString to set */
    void setQuoteString(final String quoteString) {
        m_quoteString = quoteString;
    }

    /** @return the hasRowHeader */
    boolean hasRowHeader() {
        return m_hasRowHeader;
    }

    /** @param hasRowHeader the hasRowHeader to set */
    void setHasRowHeader(final boolean hasRowHeader) {
        m_hasRowHeader = hasRowHeader;
    }

    /** @return the hasColHeader */
    boolean hasColHeader() {
        return m_hasColHeader;
    }

    /** @param hasColHeader the hasColHeader to set */
    void setHasColHeader(final boolean hasColHeader) {
        m_hasColHeader = hasColHeader;
    }

    /** @return the commentStart */
    String getCommentStart() {
        return m_commentStart;
    }

    /** @param commentStart the commentStart to set */
    void setCommentStart(final String commentStart) {
        m_commentStart = commentStart;
    }


}
