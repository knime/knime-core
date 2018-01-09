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
 * Created on 2013.08.12. by Gabor Bakos
 */
package org.knime.base.node.rules.engine;

import java.util.List;
import java.util.Map;

import org.knime.base.node.rules.engine.Rule.TableReference;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Interface to create expressions not resulting in Boolean results.
 *
 * @author Gabor Bakos
 */
public interface ReferenceExpressionFactory {

    /**
     * Creates a reference {@link Expression} to a column in a {@link DataTable}.
     *
     * @param spec The {@link DataTableSpec}.
     * @param columnRef Name of the column in the {@code spec}. (As-is, no escapes.)
     * @return The {@link Expression} computing the value of the column.
     * @throws IllegalStateException If the {@code columnRef} is not present in {@code spec}.
     */
    public Expression columnRef(final DataTableSpec spec, final String columnRef);

    /**
     * Creates a reference {@link Expression} to a boolean column in a {@link DataTable} for missing operator. Cannot be
     * called with non-Boolean columns.
     *
     * @param spec The {@link DataTableSpec}.
     * @param columnRef Name of the column in the {@code spec}. (As-is, no escapes.)
     * @return The {@link Expression} computing the value of the column, or false if that value is missing for a boolean
     *         column.
     * @throws IllegalStateException If the {@code columnRef} is not present in {@code spec}.
     */
    public Expression columnRefForMissing(final DataTableSpec spec, final String columnRef);

    /**
     * Creates a reference {@link Expression} to a flow variable.<br/>
     * It performs {@link Expression#isConstant()} optimization.
     *
     * @param flowVariables All available flow variables.
     * @param flowVarRef The name of the flow variable. (As-is, no escapes.)
     * @return An Expression computing the flow variable's value.
     * @throws IllegalStateException If the {@code flowVarRef} is not present in the {@code flowVariables}.
     */
    public Expression flowVarRef(final Map<String, FlowVariable> flowVariables, final String flowVarRef);

    /**
     * Creates a constant {@link Expression} from a {@link String} using the {@link DataType}'s single argument (
     * {@link String}) constructor. Can be used to create xml or svg cells too.
     *
     * @param text The constant text of the result.
     * @param type The DataType of the result.
     * @return An {@link Expression} always returning the same text.
     * @throws IllegalStateException if cannot create the specified {@link DataCell}. (Should not happen with
     *             {@link StringCell}s.)
     */
    public Expression constant(final String text, final DataType type);

    /**
     * Creates a constant expression for a double value.
     *
     * @param real The value to return.
     * @return A new constant expression always returning {@code real}.
     */
    public Expression constant(final double real);

    /**
     * Creates a constant expression for an int value.
     *
     * @param integer The value to return.
     * @return A new constant expression always returning {@code integer}.
     */
    public Expression constant(final int integer);

    /**
     * Constructs an {@link Expression} for the table specific expressions.<br/>
     * No optimization is done for {@link Expression#isConstant()} {@link Expression}.
     *
     * @param reference A {@link TableReference} enum.
     * @return An {@link Expression} to compute the value referred by {@code reference}.
     */
    public Expression tableRef(final TableReference reference);

    /**
     * Creates an {@link Expression} generating {@link ListCell} values. The matched objects get merged. <br/>
     * It performs {@link Expression#isConstant()} optimization.
     *
     * @param operands The expressions.
     * @return An {@link Expression} generating {@link ListCell}s from the {@code operands}' results.
     */
    public Expression list(final List<Expression> operands);

    /**
     * @return The constant true expression.
     */
    public Expression trueRefValue();

    /**
     * @return The constant false expression.
     */
    public Expression falseRefValue();

}
