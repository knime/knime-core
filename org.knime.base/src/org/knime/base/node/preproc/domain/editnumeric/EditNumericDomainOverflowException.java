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
 * ---------------------------------------------------------------------
 *
 * Created on 02.10.2013 by NanoTec
 */
package org.knime.base.node.preproc.domain.editnumeric;

import java.util.Locale;

import org.knime.base.node.preproc.domain.editnumeric.EditNumericDomainNodeModel.DomainOverflowPolicy;
import org.knime.core.data.RowKey;

/**
 * Thrown by EditNumericDomainNodeModel if the domain of a column and the defined one does not fit and the
 * {@link DomainOverflowPolicy#THROW_EXCEPTION} strategy is chosen.
 *
 * @author Marcel Hanser
 */
public class EditNumericDomainOverflowException extends RuntimeException {
    private static final long serialVersionUID = 8530230947466500182L;

    /**
     * Constructor.
     *
     * @param columnName the column name
     * @param value the actual cell value.
     * @param expectedMin expected lower bound
     * @param expectedMax expected upper bound
     * @param rowIndex the row index
     * @param rowKey the row key
     */
    public EditNumericDomainOverflowException(final String columnName, final double value, final double expectedMin,
        final double expectedMax, final long rowIndex, final RowKey rowKey) {
        super(String.format(Locale.US,
            "Column '%s' contains values outside the user-defined domain: %f not in [%f, %f] (row %d, id '%s')",
            columnName, value, expectedMin, expectedMax, rowIndex, rowKey.getString()));
    }
}
