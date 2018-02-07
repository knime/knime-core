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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * A default {@link FunctionScript} used to execute a script provided by its
 * class.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class DefaultFunctionScript implements FunctionScript {
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
	 * @param input
	 *            Array providing the input arguments needed by the script. If the
	 *            script uses row id (1), row index (2), and/or row count (3) it is
	 *            supposed that the <STRONG>used</STRONG> parameters can be found in
	 *            the order (1), (2), (3) at the end of the input.
	 * @return Result of executing the script with the given parameters.
	 * @throws ScriptExecutionException
	 *             Exception when an error occurred during the computation.
	 */
	public Object apply(Object[] input) throws ScriptExecutionException {
		int expectedInputLength = getNrArgs();
		int inputLength = input == null ? 0 : input.length;

		if (expectedInputLength != inputLength) {
			throw new IllegalArgumentException("Number of input arguments (" + inputLength
					+ ") is not equal to the number of needed arguments (" + expectedInputLength + ")");
		}

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

		Map<String, String> columnNameMap = m_info.getColumnNameMap();

		Class<?>[] columnFieldTypes = m_info.getFieldTypes();
		Map<String, Integer> parsedColumnInputMap = m_info.getFieldInputMap();

		/* Sets the value for each field provided by input. */
		for (String fieldName : columnNameMap.values()) {
			String setterName = "set" + fieldName;

			try {
				Method method = m_scriptClass.getMethod(setterName,
						columnFieldTypes[parsedColumnInputMap.get(fieldName)]);

				method.invoke(m_scriptObject, input[parsedColumnInputMap.get(fieldName)]);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException
					| IllegalArgumentException ex) {
				// Should not happen as we generated the method previously.
				throw new ScriptExecutionException("Could not access setter method '" + setterName
						+ "'. Should not happen as it should have been generated during the parsing of the script. "
						+ "May be due to type miss-matching of the previously provided DataColumnSpecs and the current input.",
						ex);
			}
		}

		/* Set the special expressions, i.e., row id, row count, and row index. */
		int idx = input.length - 1;

		if (m_info.isUseRowCount()) {
			String setterName = "set" + m_info.getRowCountField();

			try {
				Method method = m_scriptClass.getMethod(setterName, Long.class);

				method.invoke(m_scriptObject, input[idx--]);
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

				method.invoke(m_scriptObject, input[idx--]);
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

				method.invoke(m_scriptObject, input[idx--]);
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
		Object returnValue = null;

		try {
			Method method = m_scriptClass.getMethod(METHOD_NAME);

			returnValue = method.invoke(m_scriptObject);
		} catch (InvocationTargetException ex) {
			throw new ScriptExecutionException(ex);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException ex) {
			// Should not happen as we generated the method previously.
			throw new IllegalStateException("Could not start the execution using the main method '" + METHOD_NAME
					+ "'. Should not happen as it should have been generated during the parsing of the script using the specified name.",
					ex);
		}

		return returnValue;
	}

	/**
	 * @return The length of the input array needed by the script.
	 */
	public int getNrArgs() {
		int nrArgs = 0;

		nrArgs += m_info.isUseRowId() ? 1 : 0;
		nrArgs += m_info.isUseRowIndex() ? 1 : 0;
		nrArgs += m_info.isUseRowCount() ? 1 : 0;
		nrArgs += m_info.usesColumns() ? m_info.getFieldTypes().length : 0;

		return nrArgs;
	}

	/**
	 * Returns the Java type of the input array with the given index.
	 * 
	 * @param idx
	 *            index of the input field.
	 * @return Java type of the specific input field.
	 */
	public Optional<Class<?>> type(int idx) {
		int nrColumns = m_info.usesColumns() ? 0 : m_info.getFieldTypes().length;

		if (idx >= nrColumns || idx < 0) {
			return Optional.empty();
		}

		return Optional.of(m_info.getFieldTypes()[idx]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int argIdxOf(String originalName) {
		Integer idx = m_info.getFieldInputMap().get(m_info.getColumnNameMap().get(originalName));

		return idx == null ? -1 : idx;
	}

	/**
	 * @return Java type of the output of the script.
	 */
	public Class<?> getReturnType() {
		return m_info.getReturnType();
	}

	/**
	 * Returns the original index for the specified column/flow variable.
	 * 
	 * @param originalName
	 *            original name of the column/flow variable.
	 * @return Original index of the variable. Returns -1, if the column/flow
	 *         variable is not used.
	 */
	public int originalIdxOf(String originalName) {
		Integer idx = m_info.getColumnTableMap().get(originalName);

		return idx == null ? -1 : idx;
	}

	/**
	 * @return The original names of the used columns.
	 */
	public String[] getColumnNames() {
		return m_info.getColumnNameMap().keySet().toArray(new String[m_info.getColumnNameMap().size()]);
	}

	/**
	 * @return {@code true} if the script uses the row id, {@code false} otherwise.
	 */
	public boolean isUseRowId() {
		return m_info.isUseRowId();
	}

	/**
	 * @return {@code true} if the script uses the row index, {@code false}
	 *         otherwise.
	 */
	public boolean isUseRowIndex() {
		return m_info.isUseRowIndex();
	}

	/**
	 * @return {@code true} if the script uses the row count, {@code false}
	 *         otherwise.
	 */
	public boolean isUseRowCount() {
		return m_info.isUseRowCount();
	}
}
