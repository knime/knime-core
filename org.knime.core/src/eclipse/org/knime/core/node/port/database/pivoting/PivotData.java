/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 22, 2015 (Lara): created
 */
package org.knime.core.node.port.database.pivoting;

import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.port.database.StatementManipulator;

/**
 * This class holds pivot columns and corresponding values
 *
 * @author Lara Gorini
 * @since 3.1
 */
public class PivotData {

    private final StatementManipulator m_statementManipulator;

    private List<DataColumnSpec> m_myCols;

    private List<Object> m_myVals;

    /**
     * Constructor of class {@link PivotData}
     *
     * @param myCols List of {@link DataColumnSpec} holding pivot values
     * @param myVals List of pivot values
     * @param statementManipulator The {@link StatementManipulator} to use
     */
    public PivotData(final StatementManipulator statementManipulator, final List<DataColumnSpec> myCols,
        final List<Object> myVals) {
        m_statementManipulator = statementManipulator;
        m_myCols = myCols;
        m_myVals = myVals;
    }

    /**
     * @return String for pivot statement
     */
    public String getQuery() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < m_myCols.size(); i++) {
            if (i != 0) {
                buf.append(" AND ");
            }
            final DataColumnSpec col = m_myCols.get(i);
            buf.append(m_statementManipulator.quoteIdentifier(col.getName()));
            buf.append("=");
            final boolean quoteVal = !col.getType().isCompatible(DoubleValue.class);
            if (quoteVal) {
                buf.append("'");
            }
            buf.append(m_myVals.get(i));
            if (quoteVal) {
                buf.append("'");
            }
        }
        return buf.toString();
    }

    /**
     * @return List of values in pivot column
     */
    public List<Object> getValues() {
        return m_myVals;
    }

}