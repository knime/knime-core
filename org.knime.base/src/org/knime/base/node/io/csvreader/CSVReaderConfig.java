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
 * ------------------------------------------------------------------------
 *
 * History
 *   Dec 4, 2009 (wiswedel): created
 */
package org.knime.base.node.io.csvreader;

import java.time.Duration;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Config for CSV reader.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
// public scope as it's used in the widedata bundle. Scope to be reduced once both bundles are merged.
public final class CSVReaderConfig {

    /** Config key for the URL property. */
    static final String CFG_URL = "url";

    private String m_location;
    private String m_colDelimiter;
    private String m_rowDelimiter;
    private String m_quoteString;
    private boolean m_hasRowHeader;
    private boolean m_hasColHeader;
    private String m_commentStart;
    private boolean m_supportShortLines;
    private long m_limitRowsCount;
    private int m_skipFirstLinesCount;
    private String m_charSet;
    private Duration m_connectTimeout;


    /**
     * Creates a new CSVReaderConfig with default values for all settings
     * except the url.
     */
    public CSVReaderConfig() {
        m_colDelimiter = ",";
        m_rowDelimiter = "\n";
        m_quoteString = "\"";
        m_commentStart = "#";
        m_hasRowHeader = true;
        m_hasColHeader = true;
        m_supportShortLines = false;
        m_limitRowsCount = -1L;
        m_skipFirstLinesCount = -1;
        m_charSet = null; // uses default encoding
    }

    /** Load settings, used in dialog (no errors).
     * @param settings To load from.
     */
    public void loadSettingsInDialog(final NodeSettingsRO settings) {
        m_location = settings.getString(CFG_URL, null);
        m_colDelimiter = settings.getString("colDelimiter", m_colDelimiter);
        m_rowDelimiter = settings.getString("rowDelimiter", m_rowDelimiter);
        m_quoteString = settings.getString("quote", m_quoteString);
        m_commentStart = settings.getString("commentStart", m_commentStart);
        m_hasRowHeader = settings.getBoolean("hasRowHeader", m_hasRowHeader);
        m_hasColHeader = settings.getBoolean("hasColHeader", m_hasColHeader);
        m_supportShortLines = settings.getBoolean("supportShortLines", m_supportShortLines);
        m_limitRowsCount = settings.getLong("limitRowsCount", m_limitRowsCount);
        m_skipFirstLinesCount = settings.getInt("skipFirstLinesCount", m_skipFirstLinesCount);
        m_charSet = settings.getString("characterSetName", null); // if key doesn't exist use default encoding
        try {
            m_connectTimeout = Duration.ofSeconds(settings.getInt("connectTimeoutInSeconds"));
        } catch (InvalidSettingsException ex) {
            m_connectTimeout = null; // use default value
        }
    }

    /** Load in model, fail if settings are invalid.
     * @param settings To load from.
     * @throws InvalidSettingsException If invalid.
     */
    public void loadSettingsInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_location = settings.getString(CFG_URL);
        m_colDelimiter = settings.getString("colDelimiter");
        m_rowDelimiter = settings.getString("rowDelimiter");
        m_quoteString = settings.getString("quote");
        m_commentStart = settings.getString("commentStart");
        m_hasRowHeader = settings.getBoolean("hasRowHeader");
        m_hasColHeader = settings.getBoolean("hasColHeader");
        // added in 2.7
        m_supportShortLines = settings.getBoolean("supportShortLines", m_supportShortLines);
        // added in 2.8
        m_limitRowsCount = settings.getLong("limitRowsCount", m_limitRowsCount);
        m_skipFirstLinesCount = settings.getInt("skipFirstLinesCount", m_skipFirstLinesCount);
        // added in 3.1
        m_charSet = settings.getString("characterSetName", null);
        // added in 3.4
        try {
            m_connectTimeout = Duration.ofSeconds(settings.getInt("connectTimeoutInSeconds"));
        } catch (InvalidSettingsException ex) {
            m_connectTimeout = null; // use default value
        }
    }

    /** Save configuration to argument.
     * @param settings To save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_location != null) {
            settings.addString(CFG_URL, m_location.toString());
        }
        settings.addString("colDelimiter", m_colDelimiter);
        settings.addString("rowDelimiter", m_rowDelimiter);
        settings.addString("quote", m_quoteString);
        settings.addString("commentStart", m_commentStart);
        settings.addBoolean("hasRowHeader", m_hasRowHeader);
        settings.addBoolean("hasColHeader", m_hasColHeader);
        settings.addBoolean("supportShortLines", m_supportShortLines);
        settings.addLong("limitRowsCount", m_limitRowsCount);
        settings.addInt("skipFirstLinesCount", m_skipFirstLinesCount);
        settings.addString("characterSetName", m_charSet);
        if (m_connectTimeout != null) {
            settings.addInt("connectTimeoutInSeconds", (int) (m_connectTimeout.toMillis() / 1000));
        }
    }

    /** @return the location */
    public String getLocation() {
        return m_location;
    }

    /** @param location the location to set */
    public void setLocation(final String location) {
        m_location = location;
    }

    /** @return the colDelimiter */
    public String getColDelimiter() {
        return m_colDelimiter;
    }

    /** @param colDelimiter the colDelimiter to set */
    void setColDelimiter(final String colDelimiter) {
        m_colDelimiter = colDelimiter;
    }

    /** @return the rowDelimiter */
    public String getRowDelimiter() {
        return m_rowDelimiter;
    }

    /** @param rowDelimiter the rowDelimiter to set */
    void setRowDelimiter(final String rowDelimiter) {
        m_rowDelimiter = rowDelimiter;
    }

    /** @return the quoteString */
    public String getQuoteString() {
        return m_quoteString;
    }

    /** @param quoteString the quoteString to set */
    void setQuoteString(final String quoteString) {
        m_quoteString = quoteString;
    }

    /** @return the hasRowHeader */
    public boolean hasRowHeader() {
        return m_hasRowHeader;
    }

    /** @param hasRowHeader the hasRowHeader to set */
    void setHasRowHeader(final boolean hasRowHeader) {
        m_hasRowHeader = hasRowHeader;
    }

    /** @return the hasColHeader */
    public boolean hasColHeader() {
        return m_hasColHeader;
    }

    /** @param hasColHeader the hasColHeader to set */
    void setHasColHeader(final boolean hasColHeader) {
        m_hasColHeader = hasColHeader;
    }

    /** @return the commentStart */
    public String getCommentStart() {
        return m_commentStart;
    }

    /** @param commentStart the commentStart to set */
    void setCommentStart(final String commentStart) {
        m_commentStart = commentStart;
    }

    /**
     * @param supportShortLines the supportShortLines to set
     */
    void setSupportShortLines(final boolean supportShortLines) {
        m_supportShortLines = supportShortLines;
    }

    /** @return the supportShortLines */
    public boolean isSupportShortLines() {
        return m_supportShortLines;
    }

    /** @return the limitRowsCount (smaller 0 if unlimited). */
    public long getLimitRowsCount() {
        return m_limitRowsCount;
    }

    /** @param value the limitRowsCount to set (smaller 0 if unlimited). */
    void setLimitRowsCount(final long value) {
        m_limitRowsCount = value;
    }

    /** @return the skipFirstLinesCount (smaller 0 if none to skip). */
    public int getSkipFirstLinesCount() {
        return m_skipFirstLinesCount;
    }

    /** @param value the skipFirstLinesCount to set (smaller 0 if none to skip). */
    void setSkipFirstLinesCount(final int value) {
        m_skipFirstLinesCount = value;
    }

    /** @return the set encoding, or null if the default character set should be used. */
    public String getCharSetName() {
        return m_charSet;
    }

    /** @param charSet name of the new encoding, or null if the default should be used. */
    void setCharSetName(final String charSet) {
        m_charSet = charSet;
    }

    /** @return the timeout for remote files or null if the default value should be used */
    public Duration getConnectTimeout(){
        return m_connectTimeout;
    }

    /** @param value the connect timeout to set or <code>null</code> to use default value. */
    void setConnectTimeout(final Duration value){
        m_connectTimeout = value;
    }
}
