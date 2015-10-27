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
 *   27.08.2014 (koetter): created
 */
package org.knime.core.node.port.database.aggregation.function;


import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.aggregation.function.booleanoption.AbstractBooleanDBAggregationFunction;

/**
 * Abstract class that provides a dialog where the user can select if distinct should be used in the function.
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public abstract class AbstractDistinctDBAggregationFunction extends AbstractBooleanDBAggregationFunction {

    /**The string that is attached to the label as returned by the {@link #getLabel()} method to generate the id.*/
    protected static final String LABEL_POSTIX = "_DISTINCT";

    /**
     * @param distinct <code>true</code> for distinct
     */
    protected AbstractDistinctDBAggregationFunction(final boolean distinct) {
        super("distinct", distinct);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return getLabel() + LABEL_POSTIX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName() {
        if (isSelected()) {
            return getLabel() + LABEL_POSTIX;
        }
        return getLabel();
    }

    /**
     * @return the database function to use e.g. SUM
     * @since 3.1
     */
    protected abstract String getFunction();

    /**
     * {@inheritDoc}
     * @since 3.1
     */
    @Override
    public String getSQLFragment(final StatementManipulator manipulator, final String tableName, final String columnName) {
        return getFunction() + "(" + (isSelected() ? "DISTINCT " : "")
                + manipulator.quoteIdentifier(tableName) + "." + manipulator.quoteIdentifier(columnName) + ")";
    }

    /**
     * {@inheritDoc}
     * @since 3.1
     */
    @Override
    public String getSQLFragment4SubQuery(final StatementManipulator manipulator, final String tableName, final String subQuery) {
        return getFunction() + "(" + (isSelected() ? "DISTINCT (" : "") + subQuery + (isSelected() ? ")" : "") + ")";
    }
}
