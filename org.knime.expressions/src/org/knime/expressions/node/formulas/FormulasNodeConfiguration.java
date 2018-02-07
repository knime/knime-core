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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
package org.knime.expressions.node.formulas;

import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
final class FormulasNodeConfiguration {

	/*
	 * 2D array storing the column names (first row) and the expressions (second
	 * row).
	 */
	private String[][] m_expressionTable;
	private DataType[] m_dataTypes;

	/* Keys used to store and save data. */
	private static final String TYPE_KEY = "types";
	private static final String EXPRESSION_KEY = "expressions";
	private static final String COLUMN_NAME_KEY = "columnNames";

	/**
	 * Saves settings stored in the configuration.
	 * 
	 * @param settings
	 *            to save to.
	 */
	void saveSettingsTo(final NodeSettingsWO settings) {
		if (m_expressionTable != null) {
			settings.addStringArray(COLUMN_NAME_KEY, m_expressionTable[FormulasNodeDialog.getNameColumn() - 1]);
			settings.addStringArray(EXPRESSION_KEY, m_expressionTable[FormulasNodeDialog.getExpressionColumn() - 1]);
			settings.addDataTypeArray(TYPE_KEY, m_dataTypes);
		}
	}

	/**
	 * Loads settings into the model, uses default value if incomplete.
	 * 
	 * @param settings
	 *            to load from.
	 */
	void loadSettingsIntoDialog(final NodeSettingsRO settings) {
		String[] colNames = null;
		String[] expressions = null;

		try {
			colNames = settings.getStringArray(COLUMN_NAME_KEY);
			expressions = settings.getStringArray(EXPRESSION_KEY);

			m_expressionTable = new String[][] { colNames, expressions };
			m_dataTypes = settings.getDataTypeArray(TYPE_KEY);
		} catch (InvalidSettingsException e) {
			m_expressionTable = new String[0][0];
			m_dataTypes = new DataType[0];
		}
	}

	/**
	 * Sets the expression table containing column names and the expressions.
	 * 
	 * @param expressionTable
	 *            2D array where the first row contains the column names and the
	 *            second row contains the expressions.
	 */
	void setExpressionTable(String[][] expressionTable) throws IllegalArgumentException {
		if (expressionTable.length != 2) {
			throw new IllegalArgumentException("Number of rows (" + expressionTable.length
					+ ") of the provided expression table is not equal to 2.");
		}

		m_expressionTable = expressionTable;
	}

	/**
	 * 
	 * @return 2D array containing column names in the first row and expressions in
	 *         the second row.
	 */
	String[][] getExpressionTable() {
		return m_expressionTable;
	}

	/**
	 * Loads settings from the model, fails if incomplete.
	 * 
	 * @param settings
	 *            to load from.
	 * @throws InvalidSettingsException
	 */
	void loadSettingsInModel(NodeSettingsRO settings) throws InvalidSettingsException {
		String[][] expressionTable = new String[2][];

		try {
			expressionTable[FormulasNodeDialog.getNameColumn() - 1] = settings.getStringArray(COLUMN_NAME_KEY);
		} catch (InvalidSettingsException e) {
			throw new InvalidSettingsException("No column names set.", e);
		}

		try {
			expressionTable[FormulasNodeDialog.getExpressionColumn() - 1] = settings.getStringArray(EXPRESSION_KEY);
		} catch (InvalidSettingsException e) {
			throw new InvalidSettingsException("No expressions set.", e);
		}
		
		setExpressionTable(expressionTable);

		try {
			setDataTypes(settings.getDataTypeArray(TYPE_KEY));
		} catch (InvalidSettingsException e) {
			throw new InvalidSettingsException("No types set.");
		}
	}

	/**
	 * 
	 * @param dataTypeArray
	 *            array containing the data types of the columns.
	 */
	public void setDataTypes(DataType[] dataTypeArray) {
		if (m_expressionTable != null && m_expressionTable[0].length != dataTypeArray.length) {
			throw new IllegalArgumentException(
					"The number of provided data types is not the same as the number of columns. ");
		}

		m_dataTypes = dataTypeArray;
	}

	/**
	 * 
	 * @return the data types for each column.
	 */
	public DataType[] getDataTypes() {
		return m_dataTypes;
	}
}
