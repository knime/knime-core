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
 *   May 27, 2020 (carlwitt): created
 */
package org.knime.core.data.join;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.implementation.OrderedRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;

import com.google.common.collect.ImmutableList;

/**
 * Bundle included columns, join columns, etc. for a given input table. Two {@link JoinTableSettings} can be combined
 * into a {@link JoinSpecification}. <br/>
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public class JoinTableSettings {

    /**
     * Describes the left-hand side or right-hand side of a join equality predicate, e.g., col1 = col2. Which side
     * depends on {@link JoinTableSettings#getSide()}. Each {@link JoinColumn} is either a String which contains the
     * name of a column in a table or to a {@link SpecialJoinColumn} which describes for instance the row key. So
     * {@link JoinColumn} is a union type for {@link String} and {@link SpecialJoinColumn}.
     */
    public static class JoinColumn {
        private final String m_columnName;

        private final SpecialJoinColumn m_specialColumn;

        /**
         * Constructor for {@link String} instances.
         *
         * @param columnName the name of a column in a table
         */
        public JoinColumn(final String columnName) {
            m_columnName = columnName;
            this.m_specialColumn = null;

        }

        /**
         * Constructor for {@link SpecialJoinColumn} instances.
         *
         * @param specialColumn the object representing a join criterion involving a non-existing column in the table,
         *            e.g., row keys.
         */
        public JoinColumn(final SpecialJoinColumn specialColumn) {
            m_specialColumn = specialColumn;
            this.m_columnName = null;
        }

        /**
         * @param spec the data table specification in which to look up the column's name
         * @return if this is a String instance, the column index as returned by
         *         {@link DataTableSpec#findColumnIndex(String)}. If this is a {@link SpecialJoinColumn} then the
         *         indicator index returned by {@link SpecialJoinColumn#getColumnIndexIndicator()}
         */
        int toColumnIndex(final DataTableSpec spec) {
            if (m_columnName == null) {
                return m_specialColumn.getColumnIndexIndicator();
            } else {
                return spec.findColumnIndex(m_columnName);
            }
        }

        /**
         * @return if this is a String instance, the column name passed to {@link JoinColumn#JoinColumn(String)}. If
         *         this is a {@link SpecialJoinColumn} instance, the column name indicator, as returned by
         *         {@link SpecialJoinColumn#getColumnNameIndicator()}.
         */
        public String toColumnName() {
            if (m_columnName == null) {
                return m_specialColumn.getColumnNameIndicator();
            } else {
                return m_columnName;
            }
        }

        /**
         * @param string column name, either from an input table or a reserved name as per
         *            {@link SpecialJoinColumn#getColumnNameIndicator()}
         * @return if none of the reserved names matches, a {@link String} instance of a {@link JoinColumn}, otherwise a
         *         {@link SpecialJoinColumn} instance of a {@link JoinColumn}
         */
        public static JoinColumn fromString(final String string) {
            // if any of the special join columns' column names matches, return the according object
            return Arrays.stream(SpecialJoinColumn.values())
                .filter(special -> special.getColumnNameIndicator().equals(string)).map(JoinColumn::new).findFirst()
                .orElse(new JoinColumn(string));
        }

        /**
         * @param clauses either
         * @return JoinClause objects instantiated from either {@link String}s or {@link SpecialJoinColumn}s
         */
        public static JoinColumn[] array(final Object... clauses) {
            boolean valid = Arrays.stream(clauses).allMatch(o -> o instanceof String || o instanceof SpecialJoinColumn);
            if (!valid) {
                throw new IllegalArgumentException(
                    "Use either String or SpecialJoinColumn objects to instantiante a JoinClause array.");
            }
            return Arrays.stream(clauses).map(o -> {
                if (o instanceof String) {
                    return new JoinColumn((String)o);
                }
                return new JoinColumn((SpecialJoinColumn)o);
            }).toArray(JoinColumn[]::new);
        }

        /**
         * @return true if this is a String instance (was created using {@link JoinColumn#JoinColumn(String)}.
         */
        public boolean isColumn() {
            return m_columnName != null;
        }

        @Override
        public String toString() {
            return String.format("JoinClause (%s) %s", isColumn() ? "String" : "SpecialJoinColumn", toColumnName());
        }

    }

    /**
     * The values of this enum indicate special join columns, such as joining on a row key.
     */
    public enum SpecialJoinColumn {
            /**
             * Indicates that the row key of a data row shall be used as value in a join column equality clause.
             */
            ROW_KEY("$RowID$", -100);

        static boolean isNot(final Object o) {
            return !(o instanceof SpecialJoinColumn);
        }

        private final String m_columnNameIndicator;

        private final int m_columnIndexIndicator;

        private SpecialJoinColumn(final String defaultColumnName, final int columnIndexIndicator) {
            if (columnIndexIndicator >= 0) {
                throw new IllegalArgumentException("Special join columns can only reserve negative column indices.");
            }
            m_columnNameIndicator = defaultColumnName;
            m_columnIndexIndicator = columnIndexIndicator;
        }

        /**
         * @return the reserved name for this special join column
         */
        String getColumnNameIndicator() {
            return m_columnNameIndicator;
        }

        /**
         * @return the reserved column offset for this non-existing join column
         */
        public int getColumnIndexIndicator() {
            return m_columnIndexIndicator;
        }

    }

    /** @see JoinTableSettings#getSide() */
    private final InputTable m_side;

    /** @see JoinTableSettings#isRetainUnmatched() */
    final boolean m_retainUnmatched;

    /**
     * Contains either the left-hand sides or the right-hand sides of the join equality clauses, depending on
     * {@link #getSide()}. A side of a clause can be either a {@link SpecialJoinColumn} or a {@link String} denoting a
     * column name in {@link #getTableSpec()}. The contents do not have to be unique, as for instance a column name may
     * appear in multiple join clauses.
     */
    private final ImmutableList<JoinColumn> m_joinClauses;

    /**
     * Column offsets containing the values for the join clauses, might include negative values for
     * {@link SpecialJoinColumn}s.
     */
    private final int[] m_joinClauseColumns;

    /**
     * @see #getJoinColumnNames()
     */
    private final ImmutableList<String> m_joinColumnNames;

    /**
     * Names of the columns to include from this table in the result table. The contents are unique, as each column is
     * either included or not. This doesn't have to cover join columns. The result table may also include columns from
     * the other input table.
     */
    private final ImmutableList<String> m_includeColumnNames;

    /** Column offsets of the included columns in the original table. */
    final int[] m_includeColumns;

    /** Which columns of the original table to materialize; union of join columns and retain columns. */
    private final int[] m_materializeColumnIndices;

    /**
     * The table for which these settings have been created. Used for instance in
     */
    private Optional<BufferedDataTable> m_forTable;

    private DataTableSpec m_tableSpec;

    private Optional<Long> m_materializedCells;

    /**
     * @param retainUnmatched whether to output unmatched rows from this table
     * @param joinColumns a join column can either be a String denoting a name of a column in the spec or a
     *            {@link SpecialJoinColumn} value indicating a special join column. If anything other than a String or a
     *            {@link SpecialJoinColumn} is passed, it will be converted to a String and interpreted as if it was
     *            passed as a String parameter in the first place.
     * @param includeColumns the names of the columns to include in the output
     * @param side either left hand input table of a join or right input table
     * @param spec the spec of the input table to be joined with another input table
     * @throws InvalidSettingsException if the include or join column names do not exist in the spec, or are not present
     *             (empty array or null)
     */
    public JoinTableSettings(final boolean retainUnmatched, final JoinColumn[] joinColumns,
        final String[] includeColumns, final InputTable side, final DataTableSpec spec)
        throws InvalidSettingsException {

        if (joinColumns == null || joinColumns.length == 0) {
            throw new InvalidSettingsException("No join columns passed.");
        }
        if (includeColumns == null) {
            throw new InvalidSettingsException("No include columns passed.");
        }

        m_side = side;
        m_retainUnmatched = retainUnmatched;

        m_joinClauses = ImmutableList.copyOf(joinColumns);

        m_includeColumnNames = ImmutableList.copyOf(includeColumns);
        validate(m_includeColumnNames, "include", spec);

        // join and include columns
        m_joinColumnNames = ImmutableList.copyOf(
            m_joinClauses.stream().filter(JoinColumn::isColumn).map(JoinColumn::toColumnName).toArray(String[]::new));

        // make sure join column names are present in the data table specification
        validate(m_joinColumnNames, "join", spec);

        m_joinClauseColumns = m_joinClauses.stream().mapToInt(clause -> clause.toColumnIndex(spec)).toArray();

        m_includeColumns = spec.columnsToIndices(includeColumns);
        // working table specification
        m_materializeColumnIndices =
            IntStream.concat(Arrays.stream(m_joinClauseColumns), Arrays.stream(m_includeColumns))
                // column is materialized only once
                .distinct()
                // only actual columns, no special join columns
                .filter(i -> i >= 0)
                // in order of appearance in the original table
                .sorted().toArray();

        m_materializedCells = Optional.empty();
        m_forTable = Optional.empty();
        m_tableSpec = spec;

    }

    /**
     * @param retainUnmatched whether to output unmatched rows from this table
     * @param joinColumns a join column can either be a String denoting a name of a column in the spec or a
     *            {@link SpecialJoinColumn} value indicating a special join column. If anything other than a String or a
     *            {@link SpecialJoinColumn} is passed, it will be converted to a String and interpreted as if it was
     *            passed as a String parameter in the first place.
     * @param includeColumns the names of the columns to include in the output
     * @param side either left hand input table of a join or right input table * @param table the data for which these
     *            settings are created
     * @param table the table providing the data for one side of the join
     *
     * @throws InvalidSettingsException if the include or join column names do not exist in the spec, or are not present
     *             (empty array or null)
     */
    public JoinTableSettings(final boolean retainUnmatched, final JoinColumn[] joinColumns,
        final String[] includeColumns, final JoinSpecification.InputTable side, final BufferedDataTable table)
        throws InvalidSettingsException {
        this(retainUnmatched, joinColumns, includeColumns, side, table.getDataTableSpec());
        setTable(table);
    }

    /**
     * @return the number of cells in join and include columns, or zero if no table has been set
     *         {@link #setTable(BufferedDataTable)}
     */
    public Optional<Long> getMaterializedCells() {
        return m_materializedCells;
    }

    /**
     * @param bufferedDataTable
     */
    public final void setTable(final BufferedDataTable bufferedDataTable) {
        m_forTable = Optional.of(bufferedDataTable);
        m_materializedCells = Optional.of(getMaterializeColumnIndices().length * bufferedDataTable.size());
        m_tableSpec = bufferedDataTable.getDataTableSpec();
    }

    /**
     * @return the table that holds the data rows that are the input of the join operation (whether for the left or
     *         right table depends on {@link #getSide()})
     */
    public Optional<BufferedDataTable> getTable() {
        return m_forTable;
    }

    /**
     * @return whether the input data returned by {@link #getTable()} is already available
     */
    public boolean hasTable() {
        return getTable().isPresent();
    }

    /**
     * @param tableSpec the tableSpec to set
     */
    void setTableSpec(final DataTableSpec tableSpec) {
        m_tableSpec = tableSpec;
    }

    /**
     * @return the format of the table holding the input data, as returned by {@link #getTable()}. However, sometimes
     *         the spec is created first, in which case this object just holds the specification until
     *         {@link #setTable(BufferedDataTable)} is called.
     */
    DataTableSpec getTableSpec() {
        return m_tableSpec;
    }

    /**
     * Validate join and include column names by verifying that they exist in the given table spec.
     *
     * @param joinColumnNames join or include column names
     * @throws InvalidSettingsException if a column name does not exist in the spec
     */
    private static void validate(final List<String> joinColumnNames, final String columnType, final DataTableSpec spec)
        throws InvalidSettingsException {
        Predicate<String> validColumnName = spec::containsName;

        List<String> invalidColumnNames =
            joinColumnNames.stream().filter(validColumnName.negate()).collect(Collectors.toList());

        if (!invalidColumnNames.isEmpty()) {
            throw new InvalidSettingsException(String.format(
                "The %s column names %s do not exist in the target table, which only specifies the column names %s",
                columnType, invalidColumnNames, Arrays.toString(spec.getColumnNames())));
        }
    }

    /**
     * Retain only columns needed to perform the join, i.e., join columns and included columns, and optionally a row
     * offset if the output is to be sorted.
     *
     * @param storeRowOffsets whether to add a column for row offsets
     * @return the format of the table used to store rows for a disk-based join
     */
    JoinTableSettings condensed(final boolean storeRowOffsets) {

        // keep only materialized columns
        final ColumnRearranger materializedColFilter = new ColumnRearranger(getTableSpec());
        materializedColFilter.keepOnly(getMaterializeColumnIndices());
        final DataTableSpec materializedColumns = materializedColFilter.createSpec();

        // add row offset column if necessary
        final DataTableSpec outputSpec =
            storeRowOffsets ? OrderedRow.withOffset(materializedColumns) : materializedColumns;

        JoinTableSettings result;
        try {
            result = new JoinTableSettings(m_retainUnmatched, getJoinClauses().toArray(new JoinColumn[0]),
                getIncludeColumnNames().toArray(new String[0]), getSide(), outputSpec);
            return result;

        } catch (InvalidSettingsException ex) {
            // This can't happen since we can't invalidate valid settings by removing unused columns
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Remove all join clauses except the specified one. Used when breaking down disjunctive joins (match rows that
     * agree in at least one join clause).
     *
     * @param clause zero-based offset of the join clause, e.g., when the join table settings passed to the constructor
     *            specify join clauses A = X, B = Y, C = Z, and clause is 1, then the new join specification will
     *            specify only the second join clause, B = Y
     * @return
     * @throws InvalidSettingsException if the clause with the given offset does not exist
     */
    JoinTableSettings usingOnlyJoinClause(final int clause) throws InvalidSettingsException {
        if (clause < 0 || clause >= getJoinClauses().size()) {
            throw new InvalidSettingsException(
                "Cannot reduce join input table settings to join clause " + clause + ", it does not exist.");
        }
        JoinColumn[] joinColumn = new JoinColumn[]{getJoinClauses().get(clause)};
        String[] includeColumns = getIncludeColumnNames().toArray(String[]::new);
        return new JoinTableSettings(isRetainUnmatched(), joinColumn, includeColumns, getSide(), getTableSpec());
    }

    /**
     * Projects a row down to its materialized columns, optionally adding an offset.
     *
     * @param row a row from a table that has the same spec as {@link #getTableSpec()}
     * @param rowOffset optional offset of the row in its source table
     * @param storeOffset whether to persist the passed rowOffset parameter
     * @return a data row ready for addition in a {@link BufferedDataContainer} with the same spec as returned by
     *         {@link #condensed(boolean)}.
     */
    DataRow condensed(final DataRow row, final long rowOffset, final boolean storeOffset) {
        return OrderedRow.materialize(this, row, rowOffset, storeOffset);
    }

    /** @return whether this is the configuration for the left-hand input table of a join or the right-hand input. */
    public InputTable getSide() {
        return m_side;
    }

    /** @return Whether to output unmatched rows from this input table in the join results. */
    public boolean isRetainUnmatched() {
        return m_retainUnmatched;
    }

    /**
     * @return the set of column names mentioned in the {@link #getJoinClauses()}, i.e., not the special join columns
     */
    List<String> getJoinColumnNames() {
        return m_joinColumnNames;
    }

    /**
     * @return the set of column names selected for inclusion in the output
     */
    List<String> getIncludeColumnNames() {
        return m_includeColumnNames;
    }

    /**
     * @return the sequence of left/right hand sides of the join clauses
     */
    List<JoinColumn> getJoinClauses() {
        return m_joinClauses;
    }

    /**
     * @return the joinClauseColumns
     */
    public int[] getJoinClauseColumns() {
        return m_joinClauseColumns;
    }

    /**
     * @return the indices of the columns to include in the result
     */
    int[] getIncludeColumns() {
        return m_includeColumns;
    }

    /**
     * @return the materializeColumnIndices
     */
    public int[] getMaterializeColumnIndices() {
        return m_materializeColumnIndices;
    }

    /**
     *
     * @param row the row to extract the join column values from
     * @return null if any of the values is missing. Otherwise, the column values of the row in the order they are
     *         referenced in {@link JoinTableSettings#getJoinClauses()}. The array may contain multiple references to
     *         the same data cell, if the corresponding column (or special join column) appears multiple times in the
     *         join clauses. For instance, for A=X && A=Z this method would return [A, A] for the left table side.
     */
    public DataCell[] get(final DataRow row) {
        int[] joinClauseColumns = getJoinClauseColumns();
        DataCell[] cells = new DataCell[joinClauseColumns.length];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = joinClauseColumns[i] == SpecialJoinColumn.ROW_KEY.getColumnIndexIndicator()
                ? new StringCell(row.getKey().getString()) : row.getCell(joinClauseColumns[i]);
            if (cells[i].isMissing()) {
                return null;
            }
        }
        return cells;
    }

}