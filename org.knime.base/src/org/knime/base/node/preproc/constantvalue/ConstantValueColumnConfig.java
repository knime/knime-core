/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 */
package org.knime.base.node.preproc.constantvalue;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration object to the Constant Value Column.
 *
 * @author Marcel Hanser
 */
final class ConstantValueColumnConfig {

    private static final String NEW_COLUMN_NAME = "new-column-name";

    private static final String REPLACED_COLUMN = "replaced-column";

    private static final String TYPE = "column-type";

    /**
     * The config key for the value.
     */
    static final String VALUE = "column-value";

    private static final String DATE_FORMAT = "date-format";

    private String m_replacedColumn;

    private String m_newColumnName;

    private String m_dateFormat;

    private String m_value;

    private TypeCellFactory m_cellFactory;

    /**
     * @return the replacedColumn
     */
    public String getReplacedColumn() {
        return m_replacedColumn;
    }

    /**
     * @param replacedColumn the replacedColumn to set
     */
    public void setReplacedColumn(final String replacedColumn) {
        m_replacedColumn = replacedColumn;
    }

    /**
     * @return the newColumnName
     */
    public String getNewColumnName() {
        return m_newColumnName;
    }

    /**
     * @param newColumnName the newColumnName to set
     */
    public void setNewColumnName(final String newColumnName) {
        m_newColumnName = newColumnName;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return m_value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(final String value) {
        m_value = value;
    }

    /**
     * @return the cellFactory
     */
    public TypeCellFactory getCellFactory() {
        return m_cellFactory;
    }

    /**
     * @param cellFactory the cellFactory to set
     */
    public void setCellFactory(final TypeCellFactory cellFactory) {
        m_cellFactory = cellFactory;
    }

    /**
     * Save current configuration.
     *
     * @param settings To save to.
     */
    void save(final NodeSettingsWO settings) {
        settings.addString(REPLACED_COLUMN, m_replacedColumn);
        settings.addString(VALUE, m_value);
        settings.addString(NEW_COLUMN_NAME, m_newColumnName);
        settings.addString(TYPE, m_cellFactory.toString());
        settings.addString(DATE_FORMAT, m_dateFormat);
    }

    /**
     * Load config in node model.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If invalid.
     */
    void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_replacedColumn = settings.getString(REPLACED_COLUMN);
        m_newColumnName = settings.getString(NEW_COLUMN_NAME);
        m_value = settings.getString(VALUE);
        m_cellFactory = getEnum(settings.getString(TYPE));
        m_dateFormat = settings.getString(DATE_FORMAT);
    }

    /**
     * Load config in dialog.
     *
     * @param settings To load from
     * @param in Current input spec
     */
    void loadInDialog(final NodeSettingsRO settings, final DataTableSpec in) {
        m_replacedColumn = settings.getString(REPLACED_COLUMN, null);
        m_newColumnName = settings.getString(NEW_COLUMN_NAME, null);
        m_value = settings.getString(VALUE, null);
        try {
            m_cellFactory = getEnum(settings.getString(TYPE, TypeCellFactory.STRING.toString()));
        } catch (InvalidSettingsException e) {
            m_cellFactory = TypeCellFactory.STRING;
        }
        m_dateFormat = settings.getString(DATE_FORMAT, "dd.MM.yyyy");
    }

    /**
     * @param string
     * @param b
     * @return
     */
    private TypeCellFactory getEnum(final String string) throws InvalidSettingsException {
        try {
            return TypeCellFactory.valueOf(string);
        } catch (IllegalArgumentException e) {
            // NOOP
        }
        throw new InvalidSettingsException("invalid type: " + string);
    }

    /**
     * @return the dateFormat
     */
    public String getDateFormat() {
        return m_dateFormat;
    }

    /**
     * @param dateFormat the dateFormat to set
     */
    public void setDateFormat(final String dateFormat) {
        m_dateFormat = dateFormat;
    }
}
