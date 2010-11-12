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
 *   24.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator.table;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;

/**
 * A single cell in a spreadsheet.
 *
 * @author Heiko Hofer
 */
class Cell {
    private String m_text;
    private DataCell m_value;
    private DataType m_type;
    private String m_errorMessage;
    private String m_missingValuePattern;

    /**
     * @param text The entered text
     * @param type The data type of the cell
     * @param missingValuePattern The pattern when text is treated as missing cell
     */
    Cell(final String text, final DataType type,
            final String missingValuePattern) {
        m_text = text;
        m_missingValuePattern = missingValuePattern;
        if (null == type) {
            m_type = DataType.getType(StringCell.class);
        } else {
            m_type = type;
        }
    }


    /**
     * @return the text
     */
    final String getText() {
        return m_text;
    }

    /**
     * @return the value
     */
    final DataCell getValue() {
        if (m_value == null) {
            DataCellFactory cellFactory = new DataCellFactory();
            cellFactory.setMissingValuePattern(m_missingValuePattern);
            m_value = cellFactory.createDataCellOfType(m_type, m_text);
            m_errorMessage = cellFactory.getErrorMessage() == null ? ""
                    : cellFactory.getErrorMessage();
        }
        return m_value;
    }

    /**
     * @return the type
     */
    final DataType getType() {
        return m_type;
    }

    /**
     * @param type the type to set
     */
    final void setType(final DataType type) {
        if (!m_type.equals(type)) {
            m_type = type;
            m_value = null;
            m_errorMessage = null;
        }
    }

    /**
     * @return the errorMessage
     */
    final String getErrorMessage() {
        if (null == m_errorMessage) {
            // parse m_text
            getValue();
        }
        return m_errorMessage;
    }


    /**
     * @param missVal
     */
    public void setMissingValuePattern(final String missVal) {
        if ((null == m_missingValuePattern && missVal != null)
                || (null != m_missingValuePattern
                        && !m_missingValuePattern.equals(missVal))) {
            if (null == m_value) {
                m_errorMessage = null;
            } else {
                if (m_text.equals(m_missingValuePattern)) {
                    m_value = null;
                    m_errorMessage = null;
                }
                if (m_text.equals(missVal)) {
                    m_value = DataType.getMissingCell();
                    m_errorMessage = null;
                }
            }
            m_missingValuePattern = missVal;
        }
    }




}
