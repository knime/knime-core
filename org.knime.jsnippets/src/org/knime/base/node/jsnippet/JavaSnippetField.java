/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   15.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet;

import org.knime.core.data.DataType;

/**
 *
 * @author Heiko Hofer
 */
public class JavaSnippetField {
	private String m_fieldName;
	private String m_fieldJavaType;
	private String m_columnName;
	private DataType m_dataType;
	private boolean m_isArray;

	/**
	 * @param fieldName
	 * @param fieldJavaType
	 * @param columnName
	 * @param dataType
	 * @param isArray
	 */
	public JavaSnippetField(final String fieldName, final String fieldJavaType,
			final String columnName, final DataType dataType,
			final boolean isArray) {
		super();
		m_fieldName = fieldName;
		m_fieldJavaType = fieldJavaType;
		m_columnName = columnName;
		m_dataType = dataType;
		m_isArray = isArray;
	}




	/**
	 * @return
	 */
	public String getJavaType() {
		return m_fieldJavaType;
	}


	/**
	 * @return
	 */
	public String getColumnName() {
		return m_columnName;
	}

	/**
	 * @return
	 */
	public DataType getDataType() {
		return m_dataType;
	}


	/**
	 * @return
	 */
	public String getFieldName() {
		return m_fieldName;
	}

}
