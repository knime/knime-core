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
import java.util.List;
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
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.data.join.results.JoinResult;
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
public class JoinSpecification {

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

        InputTable other() {
            return this == LEFT ? RIGHT : LEFT;
        }

        /** @return whether this input table refers to the left input table */
        public boolean isLeft() {
            return this == LEFT;
        }

        private static final InputTable[] LEFT_RIGHT = new InputTable[] {LEFT, RIGHT};

        /**
         * @return an array containing the constants {@link #LEFT} and {@link #RIGHT}. One could also use
         *         {@link InputTable#values()}, but in case another constant is added, code may break in different
         *         places.
         */
        public static InputTable[] leftRight() {
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
     * See {@link Builder#outputRowOrder(OutputRowOrder)}
     */
    private final OutputRowOrder m_outputRowOrder;

    /**
     * See {@link Builder#rowKeyFactory(BiFunction)}
     */
    private final BiFunction<DataRow, DataRow, RowKey> m_rowKeyFactory;

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
        this.m_outputRowOrder = builder.m_outputRowOrder;
        this.m_columnNameDisambiguator = builder.m_columnNameDisambiguator;
        this.m_mergeJoinColumns = builder.m_mergeJoinColumns;

        // merge join columns initialize this first, used in leftMergeIncludes
        m_includedViaMerge = new Predicate<String>() {

            List<String> m_leftJoinColumns = getSettings(InputTable.LEFT).getJoinColumnNames();

            List<String> m_rightIncludes = getSettings(InputTable.RIGHT).getIncludeColumnNames();

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
        // look up the data column specifications in the left table by their name
        final DataTableSpec leftSpec = getSettings(InputTable.LEFT).getTableSpec();
        // look up the right data column specifications by name
        // change names if they clash with column name from left table
        DataTableSpec rightSpec = getSettings(InputTable.RIGHT).getTableSpec();
        List<DataColumnSpec> rightColSpecs = new ArrayList<>();
        List<String> leftIncludes = getSettings(InputTable.LEFT).getIncludeColumnNames();

        // look up column specs for names and change column names to getRightTargetColumnNames
        int[] rightIncludes = getSettings(InputTable.RIGHT).m_includeColumns;
        for (int i = 0; i < rightIncludes.length; i++) {
            DataColumnSpec spec = rightSpec.getColumnSpec(rightIncludes[i]);
            rightColSpecs.add(columnDisambiguate(spec, s -> leftSpec.containsName(s) && leftIncludes.contains(s)));
        }

        return new DataTableSpec(
            Stream.concat(leftIncludes.stream().map(leftSpec::getColumnSpec), rightColSpecs.stream())
                .toArray(DataColumnSpec[]::new));

    }

    /**
     * See {@link #isMergeJoinColumns()}
     */
    private DataTableSpec specForMergedMatchTable() {

        List<String> leftIncludes = getSettings(InputTable.LEFT).getIncludeColumnNames();
        List<String> leftJoinColumns = getSettings(InputTable.LEFT).getJoinColumnNames();

        // columns in the left table must be included explicitly or have a join partner that is included explicitly
        // in order to be included in the output table spec
        List<DataColumnSpec> leftOutputColumns = new ArrayList<>();
        for (DataColumnSpec colSpec : getSettings(InputTable.LEFT).getTableSpec()) {
            final String name = colSpec.getName();
            boolean includedDirectly = leftIncludes.contains(name);
            if (includedDirectly && !leftJoinColumns.contains(name)) {
                leftOutputColumns.add(colSpec);
            } else {
                if (includedDirectly || m_includedViaMerge.test(name)) {
                    List<String> rightNames = columnJoinPartners(InputTable.LEFT, name).collect(Collectors.toList());
                    if (rightNames.contains(name)) {
                        // if one of the join partner columns has the same name, use that name
                        leftOutputColumns.add(colSpec);
                    } else {
                        // otherwise output a new column with name "Col1=Col2" instead of this column
                        String newName = name.concat("=").concat(String.join("=", rightNames));
                        DataColumnSpecCreator newSpec = new DataColumnSpecCreator(colSpec);
                        newSpec.setName(newName);
                        newSpec.removeAllHandlers();
                        leftOutputColumns.add(columnDisambiguate(newSpec.createSpec(),
                            s -> leftOutputColumns.stream().anyMatch(spec -> spec.getName().equals(s))));
                    }
                }
            }
        }

        final List<DataColumnSpec> rightOutputColumns = new ArrayList<>();
        for (int name : m_mergeIncludes[InputTable.RIGHT.ordinal()]) {
            DataColumnSpec columnSpec = getSettings(InputTable.RIGHT).getTableSpec().getColumnSpec(name);
            // in case the included column name clashes with a
            DataColumnSpec disambiguatedColumnSpec =
                columnDisambiguate(columnSpec, s -> Stream.concat(leftOutputColumns.stream(), rightOutputColumns.stream())
                    .anyMatch(col -> col.getName().equals(s)));
            rightOutputColumns.add(disambiguatedColumnSpec);
        }
        return new DataTableSpec(
            Stream.concat(leftOutputColumns.stream(), rightOutputColumns.stream()).toArray(DataColumnSpec[]::new));

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
    public static String columnDisambiguate(final String name, final Predicate<String> isAmbiguous,
        final UnaryOperator<String> disambiguator) {
        String disambiguated = name;
        // detect if the disambiguator fails and fix by concatenating its answer to the previous name instead of using it
        while (isAmbiguous.test(disambiguated)) {
            // use the disambiguator output only on first attempt, afterwards
            boolean faultyDisambiguator = disambiguated.equals(disambiguator.apply(disambiguated));
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

        DataColumnSpecCreator a = new DataColumnSpecCreator(columnSpec);
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
    public DataColumnSpec columnDisambiguate(final DataColumnSpec columnSpec, final Predicate<String> isAmbiguous) {
        return JoinSpecification.columnDisambiguate(columnSpec, isAmbiguous, getColumnNameDisambiguator());
    }

    /**
     * Columns to include from the given table when producing results in single table format
     * ({@link JoinResult#getTable()}) or when producing join results for the matched rows output
     * ({@link JoinResult#getMatches()}).
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
        int[][] result = new int[leftMergeSpecPart.length][];
        DataTableSpec rightOriginal = getSettings(InputTable.RIGHT).getTableSpec();

        for (int i = 0; i < leftMergeSpecPart.length; i++) {
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

        List<String> leftIncludes = getSettings(InputTable.LEFT).getIncludeColumnNames();

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

        List<String> rightIncludes = getSettings(InputTable.RIGHT).getIncludeColumnNames();
        List<String> rightJoinColumns = getSettings(InputTable.RIGHT).getJoinColumnNames();

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
     * @param left non-null row from the left input table
     * @param right non-null row from the right input table
     *
     * @return the output row for the inner join results
     * @see Builder#mergeJoinColumns(boolean)
     */
    public DataRow rowJoin(final DataRow left, final DataRow right) {
        int[] leftIncludes = getMatchTableIncludeIndices(InputTable.LEFT);
        int[] rightIncludes = getMatchTableIncludeIndices(InputTable.RIGHT);

        final DataCell[] dataCells = new DataCell[leftIncludes.length + rightIncludes.length];

        int cell = 0;

        for (int i = 0; i < leftIncludes.length; i++) {
            dataCells[cell++] = left.getCell(leftIncludes[i]);
        }

        for (int i = 0; i < rightIncludes.length; i++) {
            dataCells[cell++] = right.getCell(rightIncludes[i]);
        }

        //        System.out.println(String.format("join %s with %s", left,right));
        //        System.out.println(Arrays.toString(leftIncludes));
        //        System.out.println(Arrays.toString(rightIncludes));

        return new DefaultRow(m_rowKeyFactory.apply(left, right), dataCells);
    }

    /**
     * Projects a row to only the included columns. This does not depend on {@link #isMergeJoinColumns()} as it assumes
     * that we're producing separate tables as a basis for a single combined table {@link JoinResult#getTable()}.
     * Note that for matches, getting rid of non-included columns is taken care of in
     * {@link #rowJoin(DataRow, DataRow)}.
     *
     * @param side whether to project to the included columns of the left or right input table
     * @param row a row from the given input table
     * @return only the cells selected for inclusion. For the left table, the included columns in the separate output are the
     * same as
     * , either to be output directly as unmatched row, or to be padded
     *         with missing values when producing {@link JoinResult#getTable()}
     */
    public DataRow rowProjectOuter(final InputTable side, final DataRow row) {
        int[] includes = getSettings(side).getIncludeColumns();
        final DataCell[] dataCells = new DataCell[includes.length];

        int cell = 0;

        for (int i = 0; i < includes.length; i++) {
            dataCells[cell++] = row.getCell(includes[i]);
        }
        return new DefaultRow(row.getKey(), dataCells);
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

        private OutputRowOrder m_outputRowOrder = OutputRowOrder.ARBITRARY;

        private BiFunction<DataRow, DataRow, RowKey> m_rowKeyFactory = createSequenceRowKeysFactory();

        private UnaryOperator<String> m_columnNameDisambiguator = s -> s.concat(" (#1)");

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
         *            the following properties.
         *            <ul>
         *            <li>Given the row key of a row in the left table and a row in the right table, create a unique row
         *            key for a row in the output table that joins both input rows.</li>
         *            <li>Either the left or right input row can be null in case of outer joins.</li>
         *            </ul>
         * @return this for fluent API
         * @see JoinSpecification#createConcatRowKeysFactory(String)
         * @see JoinSpecification#createSequenceRowKeysFactory()
         */
        public Builder rowKeyFactory(final BiFunction<DataRow, DataRow, RowKey> rowKeyFactory) {
            this.m_rowKeyFactory = rowKeyFactory;
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

        public JoinSpecification buildTrusted() {
            try {
                return build();
            } catch (InvalidSettingsException ex) {
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
     * @return {@link Builder#columnNameDisambiguator(UnaryOperator)}
     */
    public UnaryOperator<String> getColumnNameDisambiguator() {
        return m_columnNameDisambiguator;
    }

    /**
     * @return the number of conjunctions in the join predicate.
     */
    public int numConjunctiveGroups() {
        // the number of join clauses is always equal for left and right side
        int numJoinClauses = getSettings(InputTable.LEFT).getJoinClauses().size();
        return isConjunctive() ? 1 : numJoinClauses;
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
     * @return a row key factory that outputs sequential row keys, i.e., Row0, Row1, Row2, ...
     */
    public static BiFunction<DataRow, DataRow, RowKey> createSequenceRowKeysFactory() {
        return new BiFunction<DataRow, DataRow, RowKey>() {
            long m_nextRowId = 0;

            @Override
            public RowKey apply(final DataRow t, final DataRow u) {
                return RowKey.createRowKey(m_nextRowId++);
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

            final String m_leftUnmatchedSuffix = separator.concat("?");

            final String m_rightUnmatchedPrefix = "?".concat(separator);

            @Override
            public RowKey apply(final DataRow left, final DataRow right) {
                if (left == null) {
                    return new RowKey(m_rightUnmatchedPrefix.concat(right.getKey().getString()));
                }
                if (right == null) {
                    return new RowKey(left.getKey().getString().concat(m_leftUnmatchedSuffix));
                }
                String leftKey = left.getKey().getString();
                String rightKey = right.getKey().getString();
                return new RowKey(leftKey.concat(separator).concat(rightKey));
            }
        };
    }

}
