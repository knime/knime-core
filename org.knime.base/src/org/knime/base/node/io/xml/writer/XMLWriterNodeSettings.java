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
package org.knime.base.node.io.xml.writer;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Heiko Hofer
 */
public class XMLWriterNodeSettings {
    private static final String INPUT_COLUMN = "inputColumn";
    private static final String FOLDER = "folder";
    private static final String OVERWRITE_EXISTING = "overwriteExistingFiles";
    private static final String COMBINE_ROWS = "combineRows";
    private static final String ROOT_ELEMENT = "rootElement";
    private static final String ATTRIBUTES = "attributes";
    private static final String ATTRIBUTE_VALUES = "attributeValues";

    private String m_inputColumn = null;
    private String m_folder = null;
    private boolean m_overwriteExistingFiles = false;
    private boolean m_combineRows = true;
    private String m_rootElement = null;
    private String[] m_attributes = new String[0];
    private String[] m_attributeValues = new String[0];


    /**
     * @return the inputColumn
     */
    String getInputColumn() {
        return m_inputColumn;
    }

    /**
     * @param inputColumn the inputColumn to set
     */
    void setInputColumn(final String inputColumn) {
        m_inputColumn = inputColumn;
    }

    /**
     * @return the folder
     */
    String getFolder() {
        return m_folder;
    }

    /**
     * @param folder the folder to set
     */
    void setFolder(final String folder) {
        m_folder = folder;
    }

    /**
     * @return the overwriteExistingFiles
     */
    boolean getOverwriteExistingFiles() {
        return m_overwriteExistingFiles;
    }

    /**
     * @param overwriteExistingFiles the overwriteExistingFiles to set
     */
    void setOverwriteExisting(final boolean overwriteExistingFiles) {
        m_overwriteExistingFiles = overwriteExistingFiles;
    }

    /**
     * @return the combineRows
     */
    boolean getCombineRows() {
        return m_combineRows;
    }

    /**
     * @param combineRows the combineRows to set
     */
    void setCombineRows(final boolean combineRows) {
        m_combineRows = combineRows;
    }

    /**
     * @return the rootElement
     */
    String getRootElement() {
        return m_rootElement;
    }

    /**
     * @param rootElement the rootElement to set
     */
    void setRootElement(final String rootElement) {
        m_rootElement = rootElement;
    }

    /**
     * @return the attributes
     */
    String[] getAttributeNames() {
        return m_attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    void setAttributeNames(final String[] attributes) {
        m_attributes = attributes;
    }

    /**
     * @return the attributeValues
     */
    String[] getAttributeValues() {
        return m_attributeValues;
    }

    /**
     * @param attributeValues the attributeValues to set
     */
    void setAttributeValues(final String[] attributeValues) {
        m_attributeValues = attributeValues;
    }

    /** Called from dialog when settings are to be loaded.
     * @param settings To load from
     * @param inSpec Input spec
     */
    void loadSettingsDialog(final NodeSettingsRO settings,
            final DataTableSpec inSpec) {
        m_inputColumn = settings.getString(INPUT_COLUMN, null);
        m_folder = settings.getString(FOLDER, null);
        m_overwriteExistingFiles =
            settings.getBoolean(OVERWRITE_EXISTING, false);
        m_combineRows = settings.getBoolean(COMBINE_ROWS, true);
        m_rootElement = settings.getString(ROOT_ELEMENT, null);
        m_attributes = settings.getStringArray(ATTRIBUTES, new String[0]);
        m_attributeValues = settings.getStringArray(ATTRIBUTE_VALUES, new String[0]);
    }

    /** Called from model when settings are to be loaded.
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    void loadSettingsModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_inputColumn = settings.getString(INPUT_COLUMN);
        m_folder = settings.getString(FOLDER);
        m_overwriteExistingFiles = settings.getBoolean(OVERWRITE_EXISTING);
        m_combineRows = settings.getBoolean(COMBINE_ROWS);
        m_rootElement = settings.getString(ROOT_ELEMENT);
        m_attributes = settings.getStringArray(ATTRIBUTES);
        m_attributeValues = settings.getStringArray(ATTRIBUTE_VALUES);
    }

    /** Called from model and dialog to save current settings.
     * @param settings To save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addString(INPUT_COLUMN, m_inputColumn);
        settings.addString(FOLDER, m_folder);
        settings.addBoolean(OVERWRITE_EXISTING, m_overwriteExistingFiles);
        settings.addBoolean(COMBINE_ROWS, m_combineRows);
        settings.addString(ROOT_ELEMENT, m_rootElement);
        settings.addStringArray(ATTRIBUTES, m_attributes);
        settings.addStringArray(ATTRIBUTE_VALUES, m_attributeValues);
    }

}