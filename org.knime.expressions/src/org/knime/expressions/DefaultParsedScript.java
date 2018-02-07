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

import java.util.Collections;
import java.util.Map;

/**
 * Default implementation of a {@link ParsedScript}.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class DefaultParsedScript implements ParsedScript {
	private final String m_script;
	private final Class<?> m_returnType;

	private final Map<String, Integer> m_parsedNameIndexMap;

	private final Map<String, String> m_columnNameMap;
	private final Map<String, Integer> m_columnIndexMap;

	private final Class<?>[] m_columnFieldTypes;

	private final String m_rowidField;
	private final String m_rowindexField;
	private final String m_rowcountField;

	/**
	 * Constructor creating a ParsedScript consisting of script and used columns
	 * only.
	 * 
	 * @param script
	 *            Parsed script consisting of a class, fields, and denoted start
	 *            function.
	 * @param columnNameMap
	 *            Mapping from the original column names to the field names in the
	 *            script.
	 * @param parsedNameIndexMap
	 *            Mapping from the parsed name to their field index in the script.
	 * @param columnIndexMap
	 *            Mapping from the original column names to the original index in
	 *            the column spec.
	 * @param columnFieldTypes
	 *            Java types of the column fields.
	 */
	public DefaultParsedScript(String script, Class<?> returnType, Map<String, Integer> parsedNameIndexMap,
			Map<String, String> columnNameMap, Map<String, Integer> columnIndexMap, Class<?>[] columnFieldTypes) {
		this(script, returnType, parsedNameIndexMap, columnNameMap, columnIndexMap, columnFieldTypes, null, null, null);
	}

	/**
	 * Constructor creating a ParsedScript consisting of script, columns, flow
	 * variables, and special expressions such as ROWID, ROWCOUNT, and ROWINDEX.
	 * 
	 * @param script
	 *            Parsed script consisting of a class, fields, and denoted start
	 *            function.
	 * @param columnNameMap
	 *            Mapping from the original column names to the field names in the
	 *            script.
	 * @param columnIndexMap
	 *            Mapping from the original column names to the original index in
	 *            the column spec.
	 * @param parsedNameIndexMap
	 *            Mapping from the parsed name to their field index in the script.
	 * @param columnFieldTypes
	 *            Java types of the column fields.
	 * @param rowid
	 *            Field name of the row id in the script.
	 * @param rowindex
	 *            Field name of the row index in the script.
	 * @param rowcount
	 *            Field name of the row count in the script.
	 */
	public DefaultParsedScript(String script, Class<?> returnType, Map<String, Integer> parsedNameIndexMap,
			Map<String, String> columnNameMap, Map<String, Integer> columnIndexMap, Class<?>[] columnFieldTypes,
			String rowid, String rowindex, String rowcount) {
		m_script = script;
		m_returnType = returnType;
		m_columnNameMap = columnNameMap;
		m_rowidField = rowid;
		m_rowindexField = rowindex;
		m_rowcountField = rowcount;

		m_columnIndexMap = columnIndexMap;

		m_columnFieldTypes = columnFieldTypes;

		m_parsedNameIndexMap = parsedNameIndexMap;
	}

	/**
	 * 
	 */
	public boolean isUseRowId() {
		return m_rowidField != null && !m_rowidField.equals("");
	}

	/**
	 * 
	 */
	public boolean isUseRowIndex() {
		return m_rowindexField != null && !m_rowindexField.equals("");
	}

	/**
	 * 
	 */
	public boolean isUseRowCount() {
		return m_rowcountField != null && !m_rowcountField.equals("");
	}

	/**
	 * 
	 */
	public boolean usesColumns() {
		return m_columnNameMap != null && m_columnNameMap.size() > 0;
	}

	/**
	 * 
	 */
	public String getScript() {
		return m_script;
	}

	/**
	 * 
	 */
	public Class<?> getReturnType() {
		return m_returnType;
	}

	/**
	 *
	 */
	public Map<String, String> getColumnNameMap() {
		return Collections.unmodifiableMap(m_columnNameMap);
	}

	/**
	 * 
	 */
	public Map<String, Integer> getColumnTableMap() {
		return Collections.unmodifiableMap(m_columnIndexMap);
	}

	/**
	 * 
	 */
	public Class<?>[] getFieldTypes() {
		return m_columnFieldTypes;
	}

	/**
	 * 
	 */
	public Map<String, Integer> getFieldInputMap() {
		return Collections.unmodifiableMap(m_parsedNameIndexMap);
	}

	/**
	 * 
	 */
	public String getRowIdField() {
		return m_rowidField;
	}

	/**
	 * 
	 */
	public String getRowIndexField() {
		return m_rowindexField;
	}

	/**
	 * 
	 */
	public String getRowCountField() {
		return m_rowcountField;
	}
}
