/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   10.02.2011 (hofer): created
 */
package org.knime.base.node.io.xml.reader;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author hofer, University of Konstanz
 */
public class XMLReaderNodeSettings {
    private static final String FILE_URL = "fileUrl";
    private static final String USE_XPATH_FILTER = "useXPathFilter";
    private static final String XPATH = "xpath";
    private static final String NS_PREFIXES = "nsPrefixes";
    private static final String NAMESPACES = "namespaces";

    private String m_fileURL = null;
    private boolean m_useXPathFilter = false;
    private String m_xpath = "";
    private String[] m_nsPrefixes = new String[0];
    private String[] m_namespaces = new String[0];

    /**
     * @return the fileURL
     */
    String getFileURL() {
        return m_fileURL;
    }

    /**
     * @param fileURL the fileURL to set
     */
    void setFileURL(final String fileURL) {
        m_fileURL = fileURL;
    }


    /**
     * @return the useXPathFilter
     */
    boolean getUseXPathFilter() {
        return m_useXPathFilter;
    }

    /**
     * @param useXPathFilter the useXPathFilter to set
     */
    void setUseXPathFilter(final boolean useXPathFilter) {
        m_useXPathFilter = useXPathFilter;
    }

    /**
     * @return the xpath
     */
    String getXpath() {
        return m_xpath;
    }

    /**
     * @param xpath the xpath to set
     */
    void setXpath(final String xpath) {
        m_xpath = xpath;
    }

    /**
     * @return the nsPrefixes
     */
    String[] getNsPrefixes() {
        return m_nsPrefixes;
    }

    /**
     * @param nsPrefixes the nsPrefixes to set
     */
    void setNsPrefixes(final String[] nsPrefixes) {
        m_nsPrefixes = nsPrefixes;
    }

    /**
     * @return the namespaces
     */
    String[] getNamespaces() {
        return m_namespaces;
    }

    /**
     * @param namespaces the namespaces to set
     */
    void setNamespaces(final String[] namespaces) {
        m_namespaces = namespaces;
    }

    /** Called from dialog when settings are to be loaded.
     * @param settings To load from
     * @param inSpec Input spec
     */
    void loadSettingsDialog(final NodeSettingsRO settings,
            final DataTableSpec inSpec) {
        m_fileURL = settings.getString(FILE_URL, null);
        m_useXPathFilter = settings.getBoolean(USE_XPATH_FILTER, false);
        m_xpath = settings.getString(XPATH, "");
        m_nsPrefixes = settings.getStringArray(NS_PREFIXES, new String[0]);
        m_namespaces = settings.getStringArray(NAMESPACES, new String[0]);
    }

    /** Called from model when settings are to be loaded.
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    void loadSettingsModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_fileURL = settings.getString(FILE_URL);
        m_useXPathFilter = settings.getBoolean(USE_XPATH_FILTER);
        m_xpath = settings.getString(XPATH);
        m_nsPrefixes = settings.getStringArray(NS_PREFIXES);
        m_namespaces = settings.getStringArray(NAMESPACES);
    }

    /** Called from model and dialog to save current settings.
     * @param settings To save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addString(FILE_URL, m_fileURL);
        settings.addBoolean(USE_XPATH_FILTER, m_useXPathFilter);
        settings.addString(XPATH, m_xpath);
        settings.addStringArray(NS_PREFIXES, m_nsPrefixes);
        settings.addStringArray(NAMESPACES, m_namespaces);
    }

}