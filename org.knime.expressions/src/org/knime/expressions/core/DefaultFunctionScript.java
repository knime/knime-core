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
package org.knime.expressions.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.expressions.core.exceptions.ScriptExecutionException;

/**
 * A default {@link FunctionScript} used to execute a script, which returns a
 * {@link DataCell} provided by its class.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
class DefaultFunctionScript implements FunctionScript<ScriptRowInput, DataCell> {
	private final Class<?> m_scriptClass;
	private final ParsedScript m_info;
	private Object m_scriptObject;

	public DefaultFunctionScript(Class<?> scriptClass, ParsedScript info) {
		m_scriptClass = scriptClass;
		m_info = info;
	}

	/**
	 * Executes the script for the given input.
	 * 
	 * @param inputRow
	 *            A {@link ScriptRowInput} containing all data needed by the script.
	 * @return {@link DataCell} containing the result of the script.
	 * @throws ScriptExecutionException
	 *             If the script cannot be executed for any reason.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DataCell apply(ScriptRowInput inputRow) throws ScriptExecutionException {

		/* Creates object of the compiled script only once. */
		if (m_scriptObject == null) {
			try {
				m_scriptObject = m_scriptClass.newInstance();
			} catch (InstantiationException | IllegalAccessException ex) {
				// Should not happen as we generated that class using public modifiers.
				throw new IllegalStateException(
						"Could not create a new object from the script-class. Should not happen, as the class is public and generated previousely.",
						ex);
			}

		}

		DataRow row = inputRow == null ? null : inputRow.getDataRow();

		Map<String, String> columnNameMap = m_info.getColumnNameMap();

		Class<?>[] columnFieldTypes = m_info.getFieldTypes();
		Map<String, Integer> parsedColumnInputMap = m_info.getFieldInputMap();

		/* Sets the value for each field provided by input. */
		for (String originalName : columnNameMap.keySet()) {
			String fieldName = columnNameMap.get(originalName);
			String setterName = "set" + fieldName;

			DataCell cell = row.getCell(m_info.getColumnTableMap().get(originalName));

			Object setterInput = null;

			if (!(cell instanceof MissingCell)) {
				try {
					DataCellToJavaConverter<?, ?> converter = ExpressionConverterUtils
							.getDataCellToJavaConverter(cell.getType());

					if (converter == null) {
						throw new ScriptExecutionException("No converter exists for KNIME type ('"
								+ cell.getType().toPrettyString() + "') to Java.");
					}

					setterInput = converter.convertUnsafe(cell);
				} catch (Exception ex) {
					throw new ScriptExecutionException("Error occurred during the conversion from KNIME type ('"
							+ cell.getType().toPrettyString() + "') to Java.", ex);
				}
			}

			try {
				Method method = m_scriptClass.getMethod(setterName,
						columnFieldTypes[parsedColumnInputMap.get(fieldName)]);

				method.invoke(m_scriptObject, setterInput);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException
					| IllegalArgumentException ex) {
				// Should not happen as we generated the method previously.
				throw new ScriptExecutionException("Could not access setter method '" + setterName
						+ "'. Should not happen as it should have been generated during the parsing of the script. "
						+ "May be due to type miss-matching of the previously provided DataColumnSpecs and the current input.",
						ex);
			}
		}

		if (m_info.isUseRowCount()) {
			String setterName = "set" + m_info.getRowCountField();

			try {
				Method method = m_scriptClass.getMethod(setterName, Long.class);

				method.invoke(m_scriptObject, inputRow.getTotalRowCount());
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException ex) {
				// Should not happen as we generated the method previously.
				throw new IllegalStateException("Could not access setter method '" + setterName
						+ "'. Should not happen as it should have been generated during the parsing of the script.",
						ex);
			}
		}

		if (m_info.isUseRowIndex()) {
			String setterName = "set" + m_info.getRowIndexField();

			try {
				Method method = m_scriptClass.getMethod(setterName, Long.class);

				method.invoke(m_scriptObject, inputRow.getRowIndex());
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException ex) {
				// Should not happen as we generated the method previously.
				throw new IllegalStateException("Could not access setter method '" + setterName
						+ "'. Should not happen as it should have been generated during the parsing of the script.",
						ex);
			}
		}

		if (m_info.isUseRowId()) {
			String setterName = "set" + m_info.getRowIdField();

			try {
				Method method = m_scriptClass.getMethod(setterName, String.class);

				method.invoke(m_scriptObject, row.getKey().getString());
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException ex) {
				// Should not happen as we generated the method previously.
				throw new IllegalStateException("Could not access setter method '" + setterName
						+ "'. Should not happen as it should have been generated during the parsing of the script.",
						ex);
			}
		}

		/*
		 * Executes the script by simply invoking the main method (METHOD_NAME) and
		 * returning the last computed statement.
		 */
		Object scriptReturnValue = null;

		try {

			Method method = m_scriptClass.getMethod(ExpressionUtils.getInvokeMethodName());

			scriptReturnValue = method.invoke(m_scriptObject);
		} catch (InvocationTargetException ex) {
			throw new ScriptExecutionException(ex);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException ex) {
			// Should not happen as we generated the method previously.
			throw new IllegalStateException("Could not start the execution using the main method '"
					+ ExpressionUtils.getInvokeMethodName()
					+ "'. Should not happen as it should have been generated during the parsing of the script using the specified name.",
					ex);
		}

		@SuppressWarnings("rawtypes")
		JavaToDataCellConverter converter = ExpressionConverterUtils.getJavaToDataCellConverter(m_info.getReturnType(),
				null);

		if (converter == null) {
			throw new ScriptExecutionException(
					"No converter to KNIME type ('" + m_info.getReturnType().getName() + "') exists.");
		}

		try {
			return converter.convert(scriptReturnValue);
		} catch (Exception ex) {
			throw new ScriptExecutionException("Error occurred during the conversion of the result to KNIME type ('"
					+ m_info.getReturnType().getName() + "')", ex);
		}
	}
}
