/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   4 Jan 2022 (carlwitt): created
 */
package org.knime.core.data.join;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.data.join.JoinTableSettings.SpecialJoinColumn;

/**
 * Reuses the row key when joining two rows.
 *
 * Used in cases where the row keys presented to the join method are guaranteed to be either equal or only one key is
 * present. In addition, the join specification is expected to be such that no duplicate row keys are produced, even
 * though it is not checked for performance reasons.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.6
 */
public class KeepRowKeysFactory implements BiFunction<DataRow, DataRow, RowKey> {
    /**
     * @return the row key of the present row(s).
     */
    @Override
    public RowKey apply(final DataRow leftRow, final DataRow rightRow) {
        return leftRow != null ? leftRow.getKey() : rightRow.getKey();
    }

    /**
     * Used to determine whether the keep row keys factory is eligible under the given join specification.
     *
     * RowIDs can only be kept if they are guaranteed to be equal for each pair of matching rows and to be unique in the
     * output. This depends on what is included in the output (matches, left unmatched, right unmatched rows) and
     * whether the output is shipped as a single table or as multiple tables. Let C denote whether matching rows have
     * the same row keys, as computed by {@link #matchingRowsHaveEqualKeys(JoinSpecification)}. Then the table states
     * whether row keys can be kept or not, depending on what is included for single table output. For split table
     * output, we only need C = true.
     *
     * <table>
     * <tr>
     * <th>Matches</th>
     * <th>Left Unmatched</th>
     * <th>Right Unmatched</th>
     * <th>is keeping row keys possible</th>
     * </tr>
     * <tr>
     * <td>n</td>
     * <td>n</td>
     * <td>n</td>
     * <td>y</td>
     * </tr>
     * <tr>
     * <td>n</td>
     * <td>n</td>
     * <td>y</td>
     * <td>y</td>
     * </tr>
     * <tr>
     * <td>n</td>
     * <td>y</td>
     * <td>n</td>
     * <td>y</td>
     * </tr>
     * <tr>
     * <td>n</td>
     * <td>y</td>
     * <td>y</td>
     * <td>n</td>
     * </tr>
     * <tr>
     * <td>y</td>
     * <td>n</td>
     * <td>n</td>
     * <td>C</td>
     * </tr>
     * <tr>
     * <td>y</td>
     * <td>n</td>
     * <td>y</td>
     * <td>C</td>
     * </tr>
     * <tr>
     * <td>y</td>
     * <td>y</td>
     * <td>n</td>
     * <td>C</td>
     * </tr>
     * <tr>
     * <td>y</td>
     * <td>y</td>
     * <td>y</td>
     * <td>n</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param joinSpec
     * @param isOutputUnmatchedRowsToSeparateOutputPort
     *
     * @return an empty Optional if keeping the row keys is possible. A message explaining why not otherwise.
     */
    public static Optional<String> applicable(final JoinSpecification joinSpec,
        final boolean isOutputUnmatchedRowsToSeparateOutputPort) {

        // determine if row key conflicts can occur among matching rows
        final var matchingRowsSafe = matchingRowsHaveEqualKeys(joinSpec);

        if (joinSpec.isRetainMatched() && !matchingRowsSafe) {
            return Optional.of("The join criteria do not explicitly ensure the equality of row keys: "
                + "The output table contains matched rows, but row keys are not among the join columns.");
        }

        // If everything goes into one table, we need to consider possible row key collisions depending on what is
        // included into the table.
        if (!isOutputUnmatchedRowsToSeparateOutputPort) {
            // merging tables may introduce duplicate row keys
            if (joinSpec.isRetainUnmatched(InputTable.LEFT) && joinSpec.isRetainUnmatched(InputTable.RIGHT)) { // NOSONAR better readability
                return Optional.of(
                    "When putting left and right unmatched rows in the same table, the row keys might not be unique.");
            }
            // NB: if including matches and only either left or right unmatched rows, row keys cannot collide because
            // the sets are mutually exclusive (can't be in matched and unmatched rows)
        }
        // NB: if isOutputUnmatchedRowsToSeparateOutputPort == true, we have three output tables.
        // In this case, conflicts can only occur for the matched rows (because the unmatched rows are a
        // subset of their input tables). However, conflicts for matching rows have been handled above.

        return Optional.empty();
    }

    /**
     * If we join on row key and the join is conjunctive, each pair of matching rows has the same row keys.
     *
     * @param joinSpec
     *
     * @return true iff join criteria guarantee that all joined (matching) output rows have the same row keys.
     */
    private static boolean matchingRowsHaveEqualKeys(final JoinSpecification joinSpec) {
        // determine if the join is conjunctive
        // match any (disjunctive join clauses) with only one clause is effectively conjunctive
        final var triviallyConjunctive = !joinSpec.isConjunctive() && joinSpec.getNumJoinClauses() == 1;
        final var conjunctive = joinSpec.isConjunctive() || triviallyConjunctive;

        // determine if we join on row key
        final var onRowKey = new JoinColumn(SpecialJoinColumn.ROW_KEY);
        List<JoinColumn> leftHandSides = joinSpec.getSettings(InputTable.LEFT).getJoinClauses();
        List<JoinColumn> rightHandSides = joinSpec.getSettings(InputTable.RIGHT).getJoinClauses();
        var joinsOnRowID = false;
        for (var i = 0; i < leftHandSides.size() && !joinsOnRowID; i++) {
            joinsOnRowID = leftHandSides.get(i).equals(onRowKey) && rightHandSides.get(i).equals(onRowKey);
            // loop ends when true
        }
        return conjunctive && joinsOnRowID;
    }
}