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
 *   24.01.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;

import org.knime.core.node.util.DefaultConfigTableModel;

/**
 * Extends the table model by validation methods.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("serial")
public abstract class FieldsTableModel extends DefaultConfigTableModel {
    /**
     * Create a model with the given column names.
     * @param columns the column names.
     */
    public FieldsTableModel(final String[] columns) {
        super(columns);
    }

    /**
     * Test whether the value of the cell defined by row and column is valid.
     * @param row the row index of the cell
     * @param column the column index of the cell
     * @return true when cell value is valid
     */
    abstract boolean isValidValue(int row, int column);

    /**
     * Gives the error message when isValidValue(row, column) returns false.
     * @param row the row index of the cell
     * @param column the column index of the cell
     * @return the error message or null when cell value is valid.
     */
    abstract String getErrorMessage(int row, int column);

    /**
     * The possible java types for the given row. For the input table it depends
     * to what java type the given column / flow variable can be converted.
     * For the output table it gives all supported output types.
     * @param row the row
     * @return the allowed java types
     */
    @SuppressWarnings("rawtypes")
    abstract Class[] getAllowedJavaTypes(int row);

    /**
     * Checks whether the cell values in the given row are valid.
     * @param row the row to check
     * @return true when all tested values are valid
     */
    public boolean validateValues(final int row) {
        for (int c = 0; c < getColumnCount(); c++) {
            if (!isValidValue(row, c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the all cell values are valid.
     * @return true when all values are valid
     */
    public boolean validateValues() {
        for (int r = 0; r < getRowCount(); r++) {
            if (!validateValues(r)) {
                return false;
            }
        }
        return true;
    }

}
