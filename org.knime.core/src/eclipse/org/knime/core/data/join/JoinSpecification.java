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
 */
package org.knime.core.data.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.data.join.results.JoinResult.OutputCombined;
import org.knime.core.data.join.results.JoinResult.OutputSplit;
import org.knime.core.node.InvalidSettingsException;

/**
 * This specifies the logical aspects of a join operation, such as which columns to join, which columns to include in
 * the results, whether the join is conjunctive or disjunctive, how to form the row keys of the joined rows, etc. <br/>
 * Output row order is not a classical concept in the database world, but in the KNIME universe it does play an
 * important role. <br/>
 *
 * Since there are relatively many methods in this class, the methods dealing with table specs are prefixed with "spec",
 * the methods that deal with specific column names and indices are prefixed with "column" and the methods that operate
 * on {@link DataRow}s are prefixed with "row".
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public final class JoinSpecification {

    /**
     * Constants to refer to either the left or right input table.
     */
    public enum InputTable {
            /**
             * Left input table. Columns provided by this table come first in the joined table.
             */
            LEFT,
            /**
             * Right input table. Join columns in this table are potentially merged into join column from the left
             * table.
             *
             * @see JoinSpecification#isMergeJoinColumns()
             */
            RIGHT;

        /** @return the opposite input table */
        public InputTable other() {
            return this == LEFT ? RIGHT : LEFT;
        }

        /** @return whether this input table refers to the left input table */
        public boolean isLeft() {
            return this == LEFT;
        }

        private static final List<InputTable> LEFT_RIGHT = List.of(LEFT, RIGHT);

        /**
         * @return both constants {@link #LEFT} and {@link #RIGHT}. One could also use {@link InputTable#values()}, but
         *         in case another constant is added, code may break in different places.
         */
        public static List<InputTable> both() {
            return LEFT_RIGHT;
        }
    }

    /**
     * Determines the order of the rows in the join result.
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     * @since 4.2
     */
    public enum OutputRowOrder {
            /** Output rows may be provided in any order. */
            ARBITRARY("Arbitrary output order (may vary randomly)"),
            /** Identical input tables and identical join specification must give identical output. */
            DETERMINISTIC("Fast sort (use any table to determine output order)"),
            /**
             * Rows are output in three blocks:
             * <ol>
             * <li>matched rows</li>
             * <li>unmatched rows from left table</li>
             * <li>unmatched rows from right table</li>
             * </ol>
             * Each block must be sorted by row offset in the left table, breaking ties by row offset in the right
             * table.
             */
            LEFT_RIGHT("Left-right output order (sort by row offset in left table, then right table)");

        private final String m_label;

        OutputRowOrder(final String label) {
            this.m_label = label;
        }

        @Override
        public String toString() {
            return m_label;
        }
    }

    /**
     * Options for comparing the values in join columns.
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     * @since 4.4
     */
    public enum DataCellComparisonMode {
            /** Two data cells will match if their types and values are equal. */
            STRICT,
            /** Two data cells will match if their string representations (toString) are equal. */
            AS_STRING,
            /**
             * Two data cells will match if their type and values are equal OR if they are numeric data cells
             * (instanceof {@link LongValue}) and their {@link LongValue#getLongValue()} are equal (==).
             */
            NUMERIC_AS_LONG;
    }

    /**
     * See {@link Builder#Builder(JoinTableSettings, JoinTableSettings)}
     */
    private final JoinTableSettings[] m_settings = new JoinTableSettings[2];

    /**
     * See {@link Builder#conjunctive(boolean)}
     */
    private final boolean m_conjunctive;

    /**
     * See {@link Builder#retainMatched(boolean)}
     */
    private final boolean m_retainMatched;

    /**
     * See {@link Builder#mergeJoinColumns(boolean)}
     */
    private final boolean m_mergeJoinColumns;

    /**
     * See {@link Builder#dataCellComparisonMode(DataCellComparisonMode)}
     */
    private final DataCellComparisonMode m_dataCellComparisonMode;

    /**
     * See {@link Builder#outputRowOrder(OutputRowOrder)}
     */
    private final OutputRowOrder m_outputRowOrder;

    /**
     * See {@link Builder#rowKeyFactory(BiFunction)}
     */
    private final BiFunction<DataRow, DataRow, RowKey> m_rowKeyFactory;

    /**
     * See {@link Builder#rowKeyFactory(BiFunction, boolean)}
     */
    private final boolean m_rowKeyFactoryCreatesUniqueKeys;

    /**
     * See {@link Builder#columnNameDisambiguator(BiFunction)}
     */
    private final UnaryOperator<String> m_columnNameDisambiguator;

    /**
     * Contains the offsets of the columns in the left and right table to include if join columns are merged. Is null if
     * {@link #isMergeJoinColumns()} is false.
     */
    private final int[][] m_mergeIncludes = new int[2][];

    /**
     * Determines for a column name whether it is implicitly included because {@link #isMergeJoinColumns()}. A join
     * column from the left table is included implicitly if its join partner column in the right table is included
     * explicitly (even if the left column is not explicitly included). This is used when creating output table specs in
     * {@link #specForMergedMatchTable()}.
     */
    private final Predicate<String> m_includedViaMerge;

    /**
     * See {@link #columnLeftMergedLocations()}
     */
    private final int[][] m_columnLeftMergedLocations;

    private JoinSpecification(final Builder builder) {
        this.m_conjunctive = builder.m_conjunctive;
        this.m_settings[InputTable.LEFT.ordinal()] = builder.m_leftSettings;
        this.m_settings[InputTable.RIGHT.ordinal()] = builder.m_rightSettings;
        this.m_retainMatched = builder.m_retainMatched;
        this.m_rowKeyFactory = builder.m_rowKeyFactory;
        this.m_rowKeyFactoryCreatesUniqueKeys = builder.m_rowKeyFactoryCreatesUniqueKeys;
        this.m_outputRowOrder = builder.m_outputRowOrder;
        this.m_columnNameDisambiguator = builder.m_columnNameDisambiguator;
        this.m_mergeJoinColumns = builder.m_mergeJoinColumns;
        this.m_dataCellComparisonMode = builder.m_dataCellComparisonMode;

        // merge join columns initialize this first, used in leftMergeIncludes
        m_includedViaMerge = new Predicate<String>() {

            Set<String> m_leftJoinColumns = new HashSet<>(getSettings(InputTable.LEFT).getJoinColumnNames());

            Set<String> m_rightIncludes = new HashSet<>(getSettings(InputTable.RIGHT).getIncludeColumnNames());

            @Override
            public boolean test(final String leftColName) {
                return m_leftJoinColumns.contains(leftColName) && columnJoinPartners(InputTable.LEFT, leftColName)
                    .anyMatch(joinPartner -> m_rightIncludes.contains(joinPartner));
            }
        };

        if (builder.m_mergeJoinColumns) {
            m_mergeIncludes[InputTable.LEFT.ordinal()] = columnLeftMergeIncludes();
            m_mergeIncludes[InputTable.RIGHT.ordinal()] = columnRightMergeIncludes();
            m_columnLeftMergedLocations = columnLeftMergedLocations();
        } else {
            m_columnLeftMergedLocations = null;
        }

    }

    /**
     * @return the specification of the table containing the inner join results (or all results if padded with missing
     *         values)
     */
    public DataTableSpec specForMatchTable() {
        if (m_mergeJoinColumns) {
            return specForMergedMatchTable();
        } else {
            return specForNormalMatchTable();
        }
    }

    /**
     * @return concatenate the columns of the left and right table and filter for included columns.
     */
    private DataTableSpec specForNormalMatchTable() {

        // add all included columns from the left table to the output spec
        // resolve names to column specifications
        final DataTableSpec leftSpec = getSettings(InputTable.LEFT).getTableSpec();
        final List<DataColumnSpec> resultColumns = Arrays.stream(getSettings(InputTable.LEFT).getIncludeColumns())
            .mapToObj(leftSpec::getColumnSpec).collect(Collectors.toList());

        // create a data structure for fast lookup of already used (thus reserved) column names
        final Set<String> takenNames = new HashSet<>(getSettings(InputTable.LEFT).getIncludeColumnNames());

        final DataTableSpec rightSpec = getSettings(InputTable.RIGHT).getTableSpec();
        // change names if they clash with column name from left table
        for (int includedColumn : getSettings(InputTable.RIGHT).getIncludeColumns()) {
            DataColumnSpec spec = rightSpec.getColumnSpec(includedColumn);
            DataColumnSpec disambiguated = columnDisambiguate(spec, takenNames::contains);
            takenNames.add(disambiguated.getName());
            resultColumns.add(disambiguated);
        }

        return new DataTableSpec(resultColumns.toArray(new DataColumnSpec[resultColumns.size()]));

    }

    /**
     * <ol>
     * <li>Decide which columns to include for the left table</li>
     * <ol>
     * A column must either
     * <ul>
     * <li>be in the included columns</li>
     * <li>have a join partner in the right table that is included</li>
     * </ul>
     * The name of the column is
     * <ul>
     * <li>unchanged if it has no join partners</li>
     * <li>set to A=B_1...=B_n where A is the name of the column and B_1...B_n are the names of the columns in the right
     * table to which A is compared</li>
     * </ul>
     * </ol>
     * <li>Decide which columns to include for the right table</li>
     * </ol>
     * See {@link Builder#mergeJoinColumns(boolean)}
     */
    private DataTableSpec specForMergedMatchTable() {

        final Set<String> leftIncludes = new HashSet<>(getSettings(InputTable.LEFT).getIncludeColumnNames());
        final Set<String> leftJoinColumns = new HashSet<>(getSettings(InputTable.LEFT).getJoinColumnNames());

        // create a data structure for fast lookup of already used (thus reserved) column names
        final Set<String> takenNames = new HashSet<>();

        List<DataColumnSpec> leftOutputColumns = new ArrayList<>();

        // columns in the left table must be included explicitly or have a join partner that is included explicitly
        // in order to be included in the output table spec
        for (DataColumnSpec colSpec : getSettings(InputTable.LEFT).getTableSpec()) {
            final String name = colSpec.getName();
            boolean includedDirectly = leftIncludes.contains(name);
            if (includedDirectly && !leftJoinColumns.contains(name)) {
                takenNames.add(colSpec.getName());
                leftOutputColumns.add(colSpec);
            } else {
                if (includedDirectly || m_includedViaMerge.test(name)) {
                    List<String> rightNames = columnJoinPartners(InputTable.LEFT, name).collect(Collectors.toList());
                    addLeftColumn(leftOutputColumns, colSpec, takenNames, rightNames);
                }
            }
        }

        final List<DataColumnSpec> rightOutputColumns = new ArrayList<>();

        for (int name : m_mergeIncludes[InputTable.RIGHT.ordinal()]) {
            DataColumnSpec columnSpec = getSettings(InputTable.RIGHT).getTableSpec().getColumnSpec(name);
            // in case the included column name clashes with a
            DataColumnSpec disambiguatedColumnSpec = columnDisambiguate(columnSpec, takenNames::contains);
            takenNames.add(disambiguatedColumnSpec.getName());
            rightOutputColumns.add(disambiguatedColumnSpec);
        }

        // concat left and right column specs
        return new DataTableSpec(
            Stream.concat(leftOutputColumns.stream(), rightOutputColumns.stream()).toArray(DataColumnSpec[]::new));

    }

    /**
     * @param leftOutputColumns column specs to construct, updated in this method
     * @param leftSpecToAdd column spec to add to the left table spec
     * @param takenNames column names no longer available, updated in this method
     * @param rightNames the column names of the right table
     */
    private void addLeftColumn(final List<DataColumnSpec> leftOutputColumns, final DataColumnSpec leftSpecToAdd,
        final Set<String> takenNames, final List<String> rightNames) {
        var name = leftSpecToAdd.getName();
        if (rightNames.contains(name)) {
            // if one of the join partner columns has the same name, use that name
            takenNames.add(name);
            leftOutputColumns.add(leftSpecToAdd);
        } else {
            // otherwise output a new column with name "Col1=Col2" instead of this column
            String newName = name;
            // can be empty if join partners are special join columns
            if (!rightNames.isEmpty()) {
                newName = name.concat("=").concat(String.join("=", rightNames));
            }
            var newSpec = new DataColumnSpecCreator(leftSpecToAdd);
            newSpec.setName(newName);
            newSpec.removeAllHandlers();
            DataColumnSpec disambiguatedColumn = columnDisambiguate(newSpec.createSpec(), takenNames::contains);
            takenNames.add(disambiguatedColumn.getName());
            leftOutputColumns.add(disambiguatedColumn);
        }
    }

    /**
     * This corresponds to the projection to included columns, independently of whether join columns are merged or not.
     *
     * @param side for which table
     * @return the specification of the table containing the unmatched rows from the left or right input table
     */
    public DataTableSpec specForUnmatched(final InputTable side) {
        final List<String> includes = getSettings(side).getIncludeColumnNames();
        DataColumnSpec[] colSpecs = getSettings(side).getTableSpec().stream()
            .filter(colSpec -> includes.contains(colSpec.getName())).toArray(DataColumnSpec[]::new);
        return new DataTableSpec(colSpecs);
    }

    /**
     * Remap the join and include columns to working tables containing only the join and include columns and optionally
     * a row offset, e.g., as computed by {@link JoinTableSettings#condensed(boolean)}. This recomputes the lookup
     * indices for join and include columns, e.g., {@link #getMatchTableIncludeIndices(InputTable)}.
     *
     * <h1>Usage</h1>
     *
     * {@link DiskBucket}s store only the columns required for joining. Their table format is computed with
     * {@code m_workingTable = m_superTable.condensed(m_storeRowOffsets)} and they are later joined with
     * {@code JoinSpecification joinDiskBuckets = m_joinSpecification.with(m_workingTable, other.m_workingTable)}
     *
     * @param joinTable one working table, is used as left input if it is declared as left table in
     *            {@link JoinTableSettings#getSide()}.
     * @param joinTable2 other working table
     * @return a {@link JoinSpecification} with identical settings except applied to different tables.
     * @throws InvalidSettingsException
     */
    JoinSpecification specWith(final JoinTableSettings joinTable, final JoinTableSettings joinTable2) {
        JoinTableSettings left = joinTable.getSide().isLeft() ? joinTable : joinTable2;
        JoinTableSettings right = joinTable.getSide().isLeft() ? joinTable2 : joinTable;
        return new JoinSpecification.Builder(left, right).conjunctive(m_conjunctive)
            .mergeJoinColumns(m_mergeJoinColumns).rowKeyFactory(m_rowKeyFactory).retainMatched(m_retainMatched)
            .outputRowOrder(m_outputRowOrder).columnNameDisambiguator(m_columnNameDisambiguator).buildTrusted();
    }

    /**
     * @param name a column name
     * @param isAmbiguous a test for whether that name is already taken by another column
     * @param disambiguator a way to make the name unique
     * @return the fixed column name, may be unchanged.
     */
    static String columnDisambiguate(final String name, final Predicate<String> isAmbiguous,
        final UnaryOperator<String> disambiguator) {
        String disambiguated = name;
        // detect if the disambiguator fails and fix by concatenating its answer to the previous name instead of using it
        while (isAmbiguous.test(disambiguated)) {
            // DataColumnSpec.setName(name) will trim the given name, so appending spaces won't help
            String effectiveColumnName = disambiguator.apply(disambiguated).trim();
            boolean faultyDisambiguator = disambiguated.equals(effectiveColumnName);
            disambiguated = faultyDisambiguator ? disambiguated.concat(disambiguated) : disambiguator.apply(disambiguated);
        }
        return disambiguated;

    }

    /**
     * Used to evade name clashes when combining tables or adding new columns (e.g., merging join columns).
     *
     * @param columnSpec the column spec to fix (or leave as is, if possible)
     * @param isAmbiguous determines whether a name is already taken
     * @param disambiguator modifies the given column name to evade a column name clash. Each application should make
     *            the name longer to eventually succeed, but the method has a safety net for that.
     * @return a data column spec with a unique name, according to predicate
     */
    public static DataColumnSpec columnDisambiguate(final DataColumnSpec columnSpec, final Predicate<String> isAmbiguous,
        final UnaryOperator<String> disambiguator) {

        String name = columnSpec.getName();

        // change spec only if necessary
        if (!isAmbiguous.test(name)) {
            return columnSpec;
        }

        var a = new DataColumnSpecCreator(columnSpec);
        a.setName(columnDisambiguate(name, isAmbiguous, disambiguator));
        a.removeAllHandlers();
        return a.createSpec();
    }

    /**
     * @param columnSpec the column spec to fix (or leave as is, if possible)
     * @param isAmbiguous determines whether a name is already taken
     * @return the result of {@link JoinSpecification#columnDisambiguate(DataColumnSpec, Predicate, UnaryOperator)} with this
     *         object's {@link #getColumnNameDisambiguator()}.
     */
    DataColumnSpec columnDisambiguate(final DataColumnSpec columnSpec, final Predicate<String> isAmbiguous) {
        return JoinSpecification.columnDisambiguate(columnSpec, isAmbiguous, getColumnNameDisambiguator());
    }

    /**
     * Columns to include from the given table when producing results in single table format
     * ({@link OutputCombined#getTable()}) or when producing join results for the matched rows output
     * ({@link OutputSplit#getMatches()}).
     *
     * @param side left or right input table
     * @return the indices of the columns to include. Result depends on whether {@link #isMergeJoinColumns()}.
     */
    public int[] getMatchTableIncludeIndices(final InputTable side) {
        return isMergeJoinColumns() ? m_mergeIncludes[side.ordinal()] : getSettings(side).getIncludeColumns();
    }

    /**
     * Which column(s) from the other table join on the specified column name ? Ignores special join columns, because
     * this is just used to determine output specs and special join columns are not materialized. For instance:
     *
     * <pre>
     * left table columns A B C D E
     * right table columns U V W A Y Z
     * left join clauses  A B C D A
     * right join clauses A Y Z A Y (which means A=A, B=Y, C=Z, D=A, A=Y)
     * e.g., joinPartners(LEFT, A) = [A, Y]
     *       joinPartners(RIGHT, A) = [A, D]
     *       joinPartners(Left, C) = [Z]
     * </pre>
     *
     * @param table the table containing the specified column
     * @param colName the name of the column, say X
     * @return column names Y from the other table for which X=Y is in the join conditions
     */
    Stream<String> columnJoinPartners(final InputTable table, final String colName) {
        List<JoinColumn> joinClauses = m_settings[table.ordinal()].getJoinClauses();
        List<JoinColumn> otherJoinClauses = m_settings[table.other().ordinal()].getJoinClauses();

        return IntStream.range(0, joinClauses.size())
            // this is a clause containing the desired column name
            .filter(i -> joinClauses.get(i).isColumn() && joinClauses.get(i).toColumnName().equals(colName))
            // get the join partner column
            .mapToObj(otherJoinClauses::get)
            // ignore special join columns
            .filter(JoinColumn::isColumn)
            .map(JoinColumn::toColumnName);
    }

    /**
     * @see #getColumnLeftMergedLocations()
     */
    int[][] columnLeftMergedLocations() {

        // this has as many columns as the left input table contributes to the merged match spec.
        int[] leftMergeSpecPart = m_mergeIncludes[InputTable.LEFT.ordinal()];
        var result = new int[leftMergeSpecPart.length][];
        DataTableSpec rightOriginal = getSettings(InputTable.RIGHT).getTableSpec();

        for (var i = 0; i < leftMergeSpecPart.length; i++) {
            // the i-th column in the merge spec references the j-th column in the original table
            int originalColumnIndex = leftMergeSpecPart[i];
            // use j to get the original column name in the left table
            String columnName =
                getSettings(InputTable.LEFT).getTableSpec().getColumnSpec(originalColumnIndex).getName();
            // use the column name to find the join partners
            int[] lookupIndex = columnJoinPartners(InputTable.LEFT, columnName)
                // look up where to get the join partner's value in the projected row format
                .mapToInt(rightOriginal::findColumnIndex)
                .toArray();
            result[i] = lookupIndex;
        }
        return result;
    }

    /**
     * If the merge join columns option is on, columns in the left table are included in the output if they are included
     * explicitly or their join partner column is included explicitly.
     *
     * @return the offsets of the columns to include from the left input table if the merge join columns option is on.
     */
    private int[] columnLeftMergeIncludes() {

        Set<String> leftIncludes = new HashSet<>(getSettings(InputTable.LEFT).getIncludeColumnNames());

        int leftColumns = getSettings(InputTable.LEFT).getTableSpec().getNumColumns();

        return IntStream.range(0, leftColumns).filter(colIdx -> {
            DataColumnSpec colSpec = getSettings(InputTable.LEFT).getTableSpec().getColumnSpec(colIdx);
            final String name = colSpec.getName();
            boolean includedDirectly = leftIncludes.contains(name);
            return includedDirectly || m_includedViaMerge.test(name);
        }).toArray();
    }

    /**
     * If the merge join columns option is on, columns in the right table must be included explicitly and not already
     * present through a merge column in order to be included in the output table spec.
     *
     * @return the offsets of the columns to include from the right input table.
     */
    private int[] columnRightMergeIncludes() {

        Set<String> rightIncludes = new HashSet<>(getSettings(InputTable.RIGHT).getIncludeColumnNames());
        Set<String> rightJoinColumns = new HashSet<>(getSettings(InputTable.RIGHT).getJoinColumnNames());

        int rightColumns = getSettings(InputTable.RIGHT).getTableSpec().getNumColumns();

        return IntStream.range(0, rightColumns).filter(colIdx -> {
            DataColumnSpec colSpec = getSettings(InputTable.RIGHT).getTableSpec().getColumnSpec(colIdx);
            final String name = colSpec.getName();
            if (!rightIncludes.contains(name)) {
                return false;
            }
            // do not include this column if it has been included via a merged join column in the left table
            // since this column is included, it's merged counterpart is also included and this is covered
            final boolean alreadyCovered = rightJoinColumns.contains(name);
            return !alreadyCovered;
        }).toArray();
    }

    /**
     * Concatenates and projects two rows to form an inner join output row. If merge join columns is selected, drops
     * join columns from the right table, see {@link Builder#mergeJoinColumns(boolean)} for details.
     *
     * If a disjunctive join is performed, the values of merged columns can differ. Thus, this method performs an
     * additional consensus step when merging in the values of some right columns into a left column. E.g., L1 = R1 OR
     * L1 = R2 with merge join columns yields a single column L1=R1=R2 whose contents are determined by
     * {@link #consensus(DataCell, DataRow, int[])}.
     *
     * @param left non-null row from the left input table
     * @param right non-null row from the right input table
     *
     * @return the output row for the inner join results
     * @see Builder#mergeJoinColumns(boolean)
     */
    @SuppressWarnings("javadoc")
    public DataRow rowJoin(final DataRow left, final DataRow right) {

        final int[] leftIncludes = getMatchTableIncludeIndices(InputTable.LEFT);
        final int[] rightIncludes = getMatchTableIncludeIndices(InputTable.RIGHT);

        final var dataCells = new DataCell[leftIncludes.length + rightIncludes.length];

        var cell = 0;

        if (isConjunctive()) {
            // just use the left table's values for merged join columns, the right tables values are the same
            for (var i = 0; i < leftIncludes.length; i++) {
                dataCells[cell] = left.getCell(leftIncludes[i]);
                cell++;
            }
            for (var i = 0; i < rightIncludes.length; i++) {
                dataCells[cell] = right.getCell(rightIncludes[i]);
                cell++;
            }
        } else {
            final int[][] mergeLocations = getColumnLeftMergedLocations();
            // use the left table's values for merged join columns, the right tables values are equal
            for (var i = 0; i < leftIncludes.length; i++) {
                DataCell leftCell = left.getCell(leftIncludes[i]);
                dataCells[cell] = isMergeJoinColumns() ? consensus(leftCell, right, mergeLocations[i]) : leftCell;
                cell++;
            }
            for (var i = 0; i < rightIncludes.length; i++) {
                dataCells[cell] = right.getCell(rightIncludes[i]);
                cell++;
            }
        }
        return new DefaultRow(m_rowKeyFactory.apply(left, right), dataCells);
    }

    /**
     * Used when joining matching rows under disjunctive join conditions.
     * @param mergeCell the data cell in the left input table that the right columns are merged into
     * @param right access to the values of the right columns
     * @param mergeLocations offsets of the columns in the right table that are merged into mergeCell
     * @return if all values (merge cell and all merge locations) agree: that value; otherwise: missing value
     */
    private static DataCell consensus(final DataCell mergeCell, final DataRow right, final int[] mergeLocations) {
        DataCell consensus = mergeCell;

        // compare the content of all cells merged into the left table's column.
        // if they agree, use the common value, otherwise return a missing value
        for (var i = 0; i < mergeLocations.length; i++) {
            if(!consensus.equals(right.getCell(mergeLocations[i]))) {
                return DataType.getMissingCell();
            }
        }

        return consensus;
    }

    /**
     * Determine the output of merging several columns from the right input table into one column.
     * Used for right unmatched rows that are output in a combined (single) table output format
     * with merge join columns on. E.g., the single table format may have a merge column for L1=R1=R2 in which case the
     * values of columns in the right table columns R1 and R2 are compared. If they are equal, output the common value,
     * if they are unequal, output a missing value.
     * @param rightUnmatched a row from the right input table
     * @param lookupColumns the column indices that are to be merged into a single column
     * @return a cell containing either the common value or a missing value
     */
    private static DataCell consensus(final DataRow rightUnmatched, final int[] lookupColumns) {
        // in case this is not a join column (no lookup columns) just use a missing value
        DataCell consensus = lookupColumns.length == 0 ? DataType.getMissingCell() : null;

        // in case this column joins on one or more other columns, check whether they all agree to use that value
        for (var i = 0; i < lookupColumns.length; i++) {
            // if any of the lookup values is missing, deliver a missing value
            if (lookupColumns[i] == -1) {
                return DataType.getMissingCell();
            } else {
                DataCell value = rightUnmatched.getCell(lookupColumns[i]);
                boolean firstValue = i == 0;
                if (firstValue) {
                    consensus = value;
                    // if at least one value does not equal the others, return a missing value
                } else if (!value.equals(consensus)) {
                    return DataType.getMissingCell();
                } else {
                    // value is not the first value and is equal to all previous values -> consensus can be left as is
                }
            }
        }
        return consensus;
    }

    /**
     * Projects a row to only the included columns. Used for split output, in which case the output spec for the equals
     * the input spec of a given table.
     *
     * For matches, getting rid of non-included columns is taken care of in
     * {@link #rowJoin(DataRow, DataRow)}.
     *
     * @param side whether to project to the included columns of the left or right input table
     * @param unmatchedRow a row without join partner from the given input table
     * @return a row containing only the cells selected for inclusion.
     */
    public DataRow rowProjectOuter(final InputTable side, final DataRow unmatchedRow) {
        int[] includes = getSettings(side).getIncludeColumns();
        final var dataCells = new DataCell[includes.length];

        var cell = 0;

        for (var i = 0; i < includes.length; i++) {
            dataCells[cell] = unmatchedRow.getCell(includes[i]);
            cell++;
        }
        return new DefaultRow(unmatchedRow.getKey(), dataCells);
    }

    /**
     * Construct a {@link JoinSpecification} by overriding default values where necessary.
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     */
    public static class Builder {

        private JoinTableSettings m_leftSettings;

        private JoinTableSettings m_rightSettings;

        private boolean m_conjunctive = true;

        private boolean m_retainMatched = true;

        private boolean m_mergeJoinColumns = false;

        private DataCellComparisonMode m_dataCellComparisonMode = DataCellComparisonMode.STRICT;

        private OutputRowOrder m_outputRowOrder = OutputRowOrder.ARBITRARY;

        private BiFunction<DataRow, DataRow, RowKey> m_rowKeyFactory = createSequenceRowKeysFactory();

        private boolean m_rowKeyFactoryCreatesUniqueKeys = false;

        private UnaryOperator<String> m_columnNameDisambiguator = s -> s.concat(" (#1)");

        /**
         * @param copyFrom a blueprint JoinSpecification
         * @return a JoinSpecification with identical settings
         */
        public static Builder from(final JoinSpecification copyFrom) {
            return new Builder(copyFrom.getSettings(InputTable.LEFT), copyFrom.getSettings(InputTable.RIGHT))
                .conjunctive(copyFrom.isConjunctive())//
                .retainMatched(copyFrom.isRetainMatched())//
                .rowKeyFactory(copyFrom.getRowKeyFactory())//
                .outputRowOrder(copyFrom.getOutputRowOrder())//
                .mergeJoinColumns(copyFrom.isMergeJoinColumns())//
                .dataCellComparisonMode(copyFrom.getDataCellComparisonMode())//
                .columnNameDisambiguator(copyFrom.getColumnNameDisambiguator());
        }

        /**
         * @param leftSettings specifies which columns to keep, join on, etc. for the left input table
         * @param rightSettings specifies which columns to keep, join on, etc. for the right input table
         */
        public Builder(final JoinTableSettings leftSettings, final JoinTableSettings rightSettings) {
            m_leftSettings = leftSettings;
            m_rightSettings = rightSettings;
        }

        /**
         * @param conjunctive true if all join clauses must evaluate to true to consider two rows matching ("match all"
         *            in the legacy joiner). False if at least one join clause must evaluate to true to consider two
         *            rows matching ("match any" in the legacy joiner).
         * @return this for fluent API
         */
        public Builder conjunctive(final boolean conjunctive) {
            this.m_conjunctive = conjunctive;
            return this;
        }

        /**
         * @param retainMatched whether to output the matched rows. <br/>
         *            For instance, not returning matched rows while retaining left outer matches results in a left
         *            antijoin (return only rows from the left table that have no match in the right table).
         * @return this for fluent API
         *
         */
        public Builder retainMatched(final boolean retainMatched) {
            this.m_retainMatched = retainMatched;
            return this;
        }

        /**
         *
         * @param rowKeyFactory the function used to create new row keys for rows in the join results. It has to have
         *            the following properties:
         *            <ul>
         *            <li>Either the left or right input row can be null in case of outer joins.</li>
         *            <li>Ideally, the factory creates unique keys given a left and a right row. If duplicate keys are
         *            encountered, table creation will fail.</li>
         *            </ul>
         * @return this for fluent API
         * @see Builder#rowKeyFactory(BiFunction, boolean)
         * @see JoinSpecification#createConcatRowKeysFactory(String)
         * @see JoinSpecification#createSequenceRowKeysFactory()
         * @see JoinSpecification#createRetainRowKeysFactory()
         */
        public Builder rowKeyFactory(final BiFunction<DataRow, DataRow, RowKey> rowKeyFactory) {
            this.m_rowKeyFactory = rowKeyFactory;
            return this;
        }

        /**
         *
         * @param rowKeyFactory the function used to create new row keys for rows in the join results. It has to have
         *            the following properties.
         *            <ul>
         *            <li>Either the left or right input row can be null in case of outer joins.</li>
         *            <li>If the second parameter is {@code true}, the factory MUST create unique keys for all joined
         *            rows in the output (i.e. for all row pairs from the input that will be joined together).</li>
         *            </ul>
         * @param createsUniqueKeys Flag whether this row key factory is guaranteed to create unique keys with the
         *            current specifications. If {@code true}, duplicate checking for the output table may be skipped.
         * @return this for fluent API
         * @see Builder#rowKeyFactory(BiFunction)
         * @see JoinSpecification#createConcatRowKeysFactory(String)
         * @see JoinSpecification#createSequenceRowKeysFactory()
         * @see JoinSpecification#createRetainRowKeysFactory()
         * @since 5.3
         */
        public Builder rowKeyFactory(final BiFunction<DataRow, DataRow, RowKey> rowKeyFactory,
            final boolean createsUniqueKeys) {
            this.m_rowKeyFactory = rowKeyFactory;
            this.m_rowKeyFactoryCreatesUniqueKeys = createsUniqueKeys;
            return this;
        }

        /**
         * @param outputRowOrder {@link OutputRowOrder}
         * @return this for fluent API
         */
        public Builder outputRowOrder(final OutputRowOrder outputRowOrder) {
            this.m_outputRowOrder = outputRowOrder;
            return this;
        }

        /**
         * When joining column A on column B, all output rows have the same value in both columns. Specifying the merge
         * join columns option merges A and B into a column named A=B. The resulting output table specification is of
         * the following form:
         * <ul>
         * <li>concatenate the columns of the left and right table</li>
         * <li>rename join columns in the left table: A=B if columns have different names, A if same name</li>
         * <li>include join columns in the left table if they are included directly or if their join partner is included
         * in the right table</li>
         * <li>remove all join columns from the right table, even if included explicitly (because they are already
         * included via the merge column in the left table)</li>
         * </ul>
         *
         * @param mergeJoinColumns whether to combine join column pairs into a single column each.
         * @return this for fluent API
         */
        public Builder mergeJoinColumns(final boolean mergeJoinColumns) {
            this.m_mergeJoinColumns = mergeJoinColumns;
            return this;
        }

        /**
         * Select how {@link DataCell}s are compared when joining.
         *
         * @param dataCellComparisonMode an object representing the desired comparison behavior
         * @return this for fluent API
         */
        public Builder dataCellComparisonMode(final DataCellComparisonMode dataCellComparisonMode) {
            this.m_dataCellComparisonMode = dataCellComparisonMode;
            return this;
        }

        /**
         * @param columnNameDisambiguator provides new names for the columns in the right table in case they are already
         *            used by the left table. The disambiguator is applied to the table spec of the left table and the
         *            proposed column name. It is expected to leave the column name unchanged in case
         * @return this for fluent API
         */
        public Builder columnNameDisambiguator(final UnaryOperator<String> columnNameDisambiguator) {
            this.m_columnNameDisambiguator = columnNameDisambiguator;
            return this;
        }

        /**
         * Remove all join clauses except the specified one. Used when breaking down disjunctive joins (match rows that agree in at least one join clause).
         * @param clause zero-based offset of the join clause, e.g., when the join table settings passed to the
         *            constructor specify join clauses A = X, B = Y, C = Z, and clause is 1, then the new join
         *            specification will specify only the second join clause, B = Y
         * @return this for fluent API
         * @throws InvalidSettingsException
         */
        public Builder usingOnlyJoinClause(final int clause) throws InvalidSettingsException {
            m_leftSettings = m_leftSettings.usingOnlyJoinClause(clause);
            m_rightSettings = m_rightSettings.usingOnlyJoinClause(clause);
            return this;
        }

        /**
         * @return the JoinSpecification
         * @throws InvalidSettingsException
         */
        public JoinSpecification build() throws InvalidSettingsException {

            // left settings need to state that it's for left table and vice versa
            if (!m_leftSettings.getSide().isLeft() || m_rightSettings.getSide().isLeft()) {
                throw new InvalidSettingsException(String.format(
                    "The first settings object must be declared as left and the other as right.\n"
                        + "First settings is declared as %s table, second is declared as %s table.",
                    m_leftSettings.getSide(), m_rightSettings.getSide()));
            }

            // the number of join clauses needs to be same on both sides
            int leftClauses = m_leftSettings.getJoinClauses().size();
            int rightClauses = m_rightSettings.getJoinClauses().size();
            if (leftClauses != rightClauses) {
                throw new InvalidSettingsException(
                    String.format("Left hand table specifies %s join clauses, right hand table specifies %s. "
                        + "Each side has to specify the same number of join clauses.", leftClauses, rightClauses));
            }

            return new JoinSpecification(this);
        }

        private JoinSpecification buildTrusted() {
            try {
                return build();
            } catch (InvalidSettingsException ex) { // NOSONAR
                assert false;
                return null;
            }
        }

    }

    /**
     * @param side left or right input table
     * @return the settings for the given input table
     */
    public JoinTableSettings getSettings(final InputTable side) {
        return m_settings[side.ordinal()];
    }

    /**
     * @return {@link Builder#conjunctive(boolean)}
     */
    public boolean isConjunctive() {
        return m_conjunctive;
    }

    /**
     * @return {@link Builder#retainMatched(boolean)}
     */
    public boolean isRetainMatched() {
        return m_retainMatched;
    }

    /**
     * @param side which input table (left or right)
     * @return whether to output unmatched rows from the input table
     */
    public boolean isRetainUnmatched(final InputTable side) {
        return m_settings[side.ordinal()].m_retainUnmatched;
    }

    /**
     * @return {@link Builder#mergeJoinColumns(boolean)}
     */
    public boolean isMergeJoinColumns() {
        return m_mergeJoinColumns;
    }

    /**
     * @return {@link Builder#dataCellComparisonMode(DataCellComparisonMode)}
     */
    public DataCellComparisonMode getDataCellComparisonMode() {
        return m_dataCellComparisonMode;
    }

    /**
     * @return {@link Builder#outputRowOrder(OutputRowOrder)}
     */
    public OutputRowOrder getOutputRowOrder() {
        return m_outputRowOrder;
    }

    /**
     * @return {@link Builder#rowKeyFactory(BiFunction)}
     */
    public BiFunction<DataRow, DataRow, RowKey> getRowKeyFactory() {
        return m_rowKeyFactory;
    }

    /**
     * @return the second argument of {@link Builder#rowKeyFactory(BiFunction, boolean)}
     * @since 5.3
     */
    public boolean isRowKeyFactoryCreatesUniqueKeys() {
        return m_rowKeyFactoryCreatesUniqueKeys;
    }

    /**
     * @return {@link Builder#columnNameDisambiguator(UnaryOperator)}
     */
    public UnaryOperator<String> getColumnNameDisambiguator() {
        return m_columnNameDisambiguator;
    }

    /**
     * @return the number of equality clauses (columnA = columnX) in the join predicate.
     */
    public int getNumJoinClauses() {
        // the number of join clauses is always equal for left and right side
        return getSettings(InputTable.LEFT).getJoinClauses().size();
    }

    /**
     * Used when producing single table output with merged join columns. This is only needed for right outer rows,
     * because their values are put into the corresponding merged join columns in the left part of the table spec. For
     * left outer rows, we can just append missing values at the end to fit the {@link #specForMatchTable()} format.
     *
     * @return a right unmatched row, the column indices in the right row (which has been formated by
     *         {@link #rowProjectOuter(InputTable, DataRow)}) where to extract the merged join column values from. For
     *         non-merged columns, returns -1 to indicate a missing value.
     */
    public int[][] getColumnLeftMergedLocations() {
        return m_columnLeftMergedLocations;
    }

    /**
     * Convert a row from the right input table to single match table format. <br/>
     * Depending on whether join columns are to be merged, the merged columns are removed from the row (also, the
     * columns not included are removed). If merge join columns is on, the present values from the right outer row are
     * written to the column of the left table that consumed the join column (can be multiple; if they are all equal,
     * the value will be used, otherwise a missing value is emitted).
     *
     * @param rightUnmatched an unmatched row from the original right input table
     * @return a data row that contains missing values for all included columns of the right table.
     */
    public DataRow rightToSingleTableFormat(final DataRow rightUnmatched) {

        // getMatchTableIncludeIndices is aware of whether merge join columns is selected
        int[] leftCells = getMatchTableIncludeIndices(InputTable.LEFT);
        int[] rightCells = getMatchTableIncludeIndices(InputTable.RIGHT);

        final var dataCells = new DataCell[leftCells.length + rightCells.length];
        var cell = 0;

        if (isMergeJoinColumns()) {

            int[][] lookupColumns = getColumnLeftMergedLocations();

            // put values from merged columns into left table
            for (var i = 0; i < leftCells.length; i++) {
                // use missing value if this column doesn't merge any columns from the right table
                dataCells[cell] = consensus(rightUnmatched, lookupColumns[i]);
                cell++;
            }
            // skip the merged join columns
            for (var i = 0; i < rightCells.length; i++) {
                dataCells[cell] = rightUnmatched.getCell(rightCells[i]);
                cell++;
            }

        } else {
            // just fill all left table columns with missing values
            for (var i = 0; i < leftCells.length; i++) {
                dataCells[cell] = DataType.getMissingCell();
                cell++;
            }

            // and append everything that has survived projection to right outer format
            for (var i = 0; i < rightCells.length; i++) {
                dataCells[cell] = rightUnmatched.getCell(rightCells[i]);
                cell++;
            }
        }
        RowKey newKey = getRowKeyFactory().apply(null, rightUnmatched);
        return new DefaultRow(newKey, dataCells);

    }

    /**
     * Convert a row from the left input table to the single match table format. <br/>
     * Depending on whether join columns in the right table are merged, fewer missing values are appended.
     *
     * @param leftUnmatched an unmatched row from the left table in the original input format
     * @return a data row that contains missing values for all included columns of the right table.
     */
    public DataRow leftToSingleTableFormat(final DataRow leftUnmatched) {

        int[] leftCells = getMatchTableIncludeIndices(InputTable.LEFT);
        // this skips merged join columns if merge join columns is on
        int[] rightCells = getMatchTableIncludeIndices(InputTable.RIGHT);

        final var dataCells = new DataCell[leftCells.length + rightCells.length];
        var cell = 0;

        for (var i = 0; i < leftCells.length; i++) {
            dataCells[cell] = leftUnmatched.getCell(leftCells[i]);
            cell++;
        }
        for (var i = 0; i < rightCells.length; i++) {
            dataCells[cell] = DataType.getMissingCell();
            cell++;
        }

        RowKey newKey = getRowKeyFactory().apply(leftUnmatched, null);
        return new DefaultRow(newKey, dataCells);
    }

    /**
     * @return a row key factory that outputs sequential row keys, i.e., Row0, Row1, Row2, ...
     */
    public static BiFunction<DataRow, DataRow, RowKey> createSequenceRowKeysFactory() {
        return new BiFunction<DataRow, DataRow, RowKey>() {
            long m_nextRowId = 0;

            @Override
            public RowKey apply(final DataRow t, final DataRow u) {
                return RowKey.createRowKey(m_nextRowId++); //NOSONAR
            }
        };
    }

    /**
     * @param separator delimiter for row keys, e.g., "_" for row keys of the form "Row1_Row2"
     * @return a row key factory that outputs row keys that are concatenated from the input row keys. If either row is
     *         null, it's row key will be replaced with "?", e.g., ?_Row0 for a right unmatched row.
     */
    public static BiFunction<DataRow, DataRow, RowKey> createConcatRowKeysFactory(final String separator) {
        return new BiFunction<DataRow, DataRow, RowKey>() {

            private final String m_leftUnmatchedSuffix = separator.concat("?");

            private final String m_rightUnmatchedPrefix = "?".concat(separator);

            @Override
            public RowKey apply(final DataRow left, final DataRow right) {
                if (left == null) {
                    return new RowKey(m_rightUnmatchedPrefix.concat(right.getKey().getString()));
                }
                if (right == null) {
                    return new RowKey(left.getKey().getString().concat(m_leftUnmatchedSuffix));
                }
                var leftKey = left.getKey().getString();
                var rightKey = right.getKey().getString();
                return new RowKey(leftKey.concat(separator).concat(rightKey));
            }
        };
    }

    /**
     * This must only be used of equality of keys is ensured for joined rows.
     *
     * @return a row key factory that outputs the key of one of the rows.
     * @since 5.3
     */
    public static BiFunction<DataRow, DataRow, RowKey> createRetainRowKeysFactory() {
        return (left, right) -> (left == null) ? right.getKey() : left.getKey();
    }
}
