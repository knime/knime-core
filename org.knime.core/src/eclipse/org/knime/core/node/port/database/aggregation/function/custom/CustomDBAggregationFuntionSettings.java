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
 * -------------------------------------------------------------------
 */

package org.knime.core.node.port.database.aggregation.function.custom;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.database.StatementManipulator;


/**
 * Class that save the settings of the {@link CustomDBAggregationFuntionSettingsPanel}.
 *
 * @author Tobias Koetter
 * @since 2.11
 */
public class CustomDBAggregationFuntionSettings {

    private static final String CFG_CUSTOM_FUNCTION = "customFunction";

    /**The column name place holder.*/
    public static final String COLUMN_NAME = "#COLUMN_NAME#";

    /**The optional second column name place holder.*/
    public static final String SECOND_COLUMN_NAME = "#SECOND_COLUMN_NAME#";

    /**The default custom function.*/
    public static final String DEFAULT_FUNCTION = "FUNCTION(" + COLUMN_NAME + ")";

    private final SettingsModelString m_function;

    private final SettingsModelString m_resultColumnName = new SettingsModelString("columnName", "CUSTOM");

    private final SettingsModelString m_secondColumn = new SettingsModelString("optionalSecondColumn", null);

    /**
     * Constructor.
     */
    public CustomDBAggregationFuntionSettings() {
        this(DEFAULT_FUNCTION);
    }

    private CustomDBAggregationFuntionSettings(final String fragment) {
        m_function = new SettingsModelString(CFG_CUSTOM_FUNCTION, fragment);
    }

    /**
     * @return the separator model
     */
    SettingsModelString getFunctionModel() {
        return m_function;
    }

    /**
     * @return the resultColumnName
     */
    SettingsModelString getResultColumnNameModel() {
        return m_resultColumnName;
    }

    /**
     * @return the secondColumn
     */
    SettingsModelString getSecondColumnModel() {
        return m_secondColumn;
    }

    /**
     * @param settings the {@link NodeSettingsRO} to read the settings from
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        validateState(((SettingsModelString) m_function.createCloneWithValidatedValue(settings)).getStringValue(),
            ((SettingsModelString) m_secondColumn.createCloneWithValidatedValue(settings)).getStringValue(),
            ((SettingsModelString)m_resultColumnName.createCloneWithValidatedValue(settings)).getStringValue());
    }

    /**
     * Validates the internal settings.
     * @throws InvalidSettingsException if the internal settings are invalid
     */
    public void validate() throws InvalidSettingsException {
        validateState(m_function.getStringValue(), m_secondColumn.getStringValue(),
            m_resultColumnName.getStringValue());
    }

    /**
     * @param function
     * @param secondCol
     * @param resultCol
     * @throws InvalidSettingsException
     */
    private void validateState(final String function, final String secondCol, final String resultCol)
        throws InvalidSettingsException {
        if (function == null || function.trim().isEmpty()) {
            throw new InvalidSettingsException("Please specify the custom function");
        }
        if (function.contains(SECOND_COLUMN_NAME) && (secondCol == null || secondCol.isEmpty())) {
            throw new InvalidSettingsException("Second column name pattern found but no second column selected");
        }
        if (resultCol == null || resultCol.trim().isEmpty()) {
            throw new InvalidSettingsException("Please specify the result column name");
        }
    }

    /**
     * @param settings the {@link NodeSettingsRO} to read the settings from
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_function.loadSettingsFrom(settings);
        m_resultColumnName.loadSettingsFrom(settings);
        m_secondColumn.loadSettingsFrom(settings);
    }

    /**
     * @param settings the {@link NodeSettingsWO} to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_function.saveSettingsTo(settings);
        m_resultColumnName.saveSettingsTo(settings);
        m_secondColumn.saveSettingsTo(settings);
    }

    /**
     * @return a clone of this settings object
     */
    public CustomDBAggregationFuntionSettings createClone() {
        return new CustomDBAggregationFuntionSettings();
    }

    /**
     * @param manipulator {@link StatementManipulator}
     * @param tableName the table name
     * @param colName the column name
     * @return the custom aggregation function
     */
    public String getSQLFragment(final StatementManipulator manipulator, final String tableName, final String colName) {
        final String quoteColName = manipulator.quoteIdentifier(tableName) + "." + manipulator.quoteIdentifier(colName);
        final String function = m_function.getStringValue();
        String replacedFunction = function.replaceAll(COLUMN_NAME, quoteColName);
        final String secondCol = m_secondColumn.getStringValue();
        if (secondCol != null && !secondCol.isEmpty()) {
            final String quotedSecondCol =
                    manipulator.quoteIdentifier(tableName) + "." + manipulator.quoteIdentifier(secondCol);
            replacedFunction = replacedFunction.replaceAll(SECOND_COLUMN_NAME, quotedSecondCol);
        }
        return replacedFunction;
    }


    /**
     * @param manipulator {@link StatementManipulator}
     * @param tableName the table name
     * @param subQuery the sub query to use as first db column
     * @return the custom aggregation function
     */
    public String getSQLFragment4SubQuery(final StatementManipulator manipulator, final String tableName, final String subQuery) {
        String replacedFunction = m_function.getStringValue();
        final String secondCol = m_secondColumn.getStringValue();
        final String firstCol;
        if (secondCol != null && !secondCol.isEmpty()) {
            firstCol = "(" + subQuery + ")";
            final String quotedSecondCol =
                    manipulator.quoteIdentifier(tableName) + "." + manipulator.quoteIdentifier(secondCol);
            replacedFunction = replacedFunction.replaceAll(SECOND_COLUMN_NAME, quotedSecondCol);
        } else {
            firstCol = subQuery;
        }
        replacedFunction = replacedFunction.replaceAll(COLUMN_NAME, firstCol);
        return replacedFunction;
    }

    /**
     * @return the name to use for the result column
     */
    public String getResultColumnName() {
        return m_resultColumnName.getStringValue();
    }

    /**
     * @param spec the inut {@link DataTableSpec}
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void configure(final DataTableSpec spec) throws InvalidSettingsException {
        final String secondCol = m_secondColumn.getStringValue();
        if (secondCol != null && !secondCol.isEmpty()) {
            if (!spec.containsName(secondCol)) {
                throw new InvalidSettingsException("Input spec does not contain second column with name: " + secondCol);
            }
        }
    }
}
