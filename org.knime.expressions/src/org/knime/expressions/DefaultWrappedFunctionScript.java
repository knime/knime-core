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
package org.knime.expressions;

import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverter;

/**
 * Default wrapper implementing {@link Function} and that wraps a
 * {@link FunctionScript} and executes the script on the provided
 * {@link DataRow} while returning the {@link DataCell} with the result.
 * Additionally {@link #apply(DataRow, String, long, long)} exists to provide
 * more information to the script, i.e. row id, row index, and row count.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class DefaultWrappedFunctionScript implements FunctionalScript<DataRow, DataCell> {

	private final FunctionScript m_functionScript;

	/**
	 * Creates an object.
	 * 
	 * @param functionScript
	 *            The function script which contains the script that shall be
	 *            executed.
	 */
	public DefaultWrappedFunctionScript(FunctionScript functionScript) {
		m_functionScript = functionScript;
	}

	/**
	 * Applies the provided script to the given {@link DataRow} and returns its
	 * result as a {@link DataCell} using the preferred converters. Additionally to
	 * {@link #apply(DataRow)} parameters for the row id, row count, and row index
	 * can be provided in case they are used by the script.
	 * 
	 * @param inputRow
	 *            {@link DataRow} of the table for which the script shall be
	 *            executed.
	 * @param rowIndex
	 *            Index of the row.
	 * @param rowCount
	 *            Total number of rows of the table.
	 * 
	 * @return The result of the function.
	 * 
	 * @throws ScriptExecutionException
	 *             Exception when error occurred during the computation.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public DataCell apply(DataRow inputRow, Long rowIndex, Long rowCount) throws ScriptExecutionException {
		Object[] input = new Object[m_functionScript.getNrArgs()];

		/* Creates the input for the script. May be empty if no column is being used. */
		for (String name : m_functionScript.getColumnNames()) {
			int col = m_functionScript.originalIdxOf(name);

			DataCell cell = inputRow.getCell(col);

			/*
			 * Missing cells are simply null in the script. May be possible to define a
			 * class/object for this purpose.
			 */
			if (cell instanceof MissingCell) {
				input[m_functionScript.argIdxOf(name)] = null;
			} else {
				try {
					DataCellToJavaConverter<?, ?> converter = ExpressionConverterUtils
							.getDataCellToJavaConverter(cell.getType());

					if (converter == null) {
						throw new ScriptExecutionException("No converter exists for KNIME type ('"
								+ cell.getType().toPrettyString() + "') to Java.");
					}

					input[m_functionScript.argIdxOf(name)] = converter.convertUnsafe(cell);
				} catch (Exception ex) {
					throw new ScriptExecutionException("Error occurred during the conversion from KNIME type ('"
							+ cell.getType().toPrettyString() + "') to Java.", ex);
				}
			}
		}

		int idx = m_functionScript.getColumnNames().length;

		if (m_functionScript.isUseRowId()) {
			input[idx++] = inputRow.getKey().getString();
		}
		if (m_functionScript.isUseRowIndex()) {
			input[idx++] = rowIndex;
		}
		if (m_functionScript.isUseRowCount()) {
			input[idx++] = rowCount;
		}

		/* Executes the script and converts the result to a DataCell. */
		Object result = m_functionScript.apply(input);

		JavaToDataCellConverter converter = ExpressionConverterUtils
				.getJavaToDataCellConverter(m_functionScript.getReturnType(), null);

		if (converter == null) {
			throw new ScriptExecutionException(
					"No converter to KNIME type ('" + m_functionScript.getReturnType().getName() + "') exists.");
		}

		try {
			return converter.convert(result);
		} catch (Exception ex) {
			throw new ScriptExecutionException("Error occurred during the conversion of the result to KNIME type ('"
					+ m_functionScript.getReturnType().getName() + "')", ex);
		}
	}

	/**
	 * Applies the provided script to the given {@link DataRow} and returns its
	 * result as a {@link DataCell} using the preferred converters.
	 * 
	 * @param inputRow
	 *            {@link DataRow} of the table for which the script shall be
	 *            executed.
	 * @throws ScriptExecutionException
	 *             Exception when error occurred during the computation.
	 */
	@Override
	public DataCell apply(DataRow inputRow) throws ScriptExecutionException {
		return apply(inputRow, null, null);
	}
}
