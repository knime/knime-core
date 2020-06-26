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

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;

/**
 * Bundle included columns, join columns, etc. for a given input table. Two {@link JoinTableSettings} can be combined
 * into a {@link JoinSpecification}. <br/>
 * Use the static methods {@link #left(boolean, Object[], String[], DataTableSpec)} or
 * {@link #left(boolean, Object[], String[], BufferedDataTable)} to create settings for the left input table and the
 * analogous methods for the right input table.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public class JoinTableSettings {

    /**
     * The values of this enum indicate special join columns, such as joining on a row key.
     */
    public enum SpecialJoinColumn {
            /**
             * Indicates that the row key of a data row shall be used as value in a join column equality clause.
             */
            ROW_KEY("___$RowKey$____ac59075b", -100);

        final String m_defaultColumnName;
        final int m_columnIndexIndicator;
        private SpecialJoinColumn(final String defaultColumnName, final int columnIndexIndicator) {
            m_defaultColumnName = defaultColumnName;
            m_columnIndexIndicator = columnIndexIndicator;
        }

        @Override public String toString() {
            return m_defaultColumnName;
        }
//        public String safeColumnName(final Predicate<String> isAmbiguous) {
//            return JoinSpecification.disambiguateName(m_defaultColumnName, isAmbiguous, s -> s.concat("$"));
//        }
        static boolean isNot(final Object o) {
            return ! (o instanceof SpecialJoinColumn);
        }

//        public Predicate<String> isDefaultColumnName() { return m_defaultColumnName::equals; }
        public Object inflate(final String columnName) {
            return m_defaultColumnName.equals(columnName) ? this : columnName;
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
    private final List<Object> m_joinClauses;

    /**
     * Column offsets containing the values for the join clauses, might include negative values for
     * {@link SpecialJoinColumn}s.
     */
    private final int[] m_joinClauseColumns;

    /**
     * @see #getJoinColumnNames()
     */
    private final List<String> m_joinColumnNames;

    /**
     * Names of the columns to include from this table in the result table. The contents are unique, as each column is
     * either included or not. This doesn't have to cover join columns. The result table may also include columns from
     * the other input table.
     */
    private final List<String> m_includeColumnNames;

    /** Column offsets of the included columns in the original table. */
    final int[] m_includeColumns;

    /** Which columns of the original table to materialize; union of join columns and retain columns. */
    final int[] m_materializeColumnIndices;

    /**
     * The table for which these settings have been created. Used for instance in
     */
    private Optional<BufferedDataTable> m_forTable;

    private DataTableSpec m_tableSpec;

    private Optional<Long> m_materializedCells;

    /**
     *
     * @param retainUnmatched whether to output unmatched rows from this table
     * @param joinColumns a join column can either be a String denoting a name of a column in the spec or a
     *            {@link SpecialJoinColumn} value indicating a special join column. If anything other than a String or a
     *            {@link SpecialJoinColumn} is passed, it will be converted to a String and interpreted as if it was
     *            passed as a String parameter in the first place.
     * @param includeColumns name of a column in the spec
     * @param side either left hand input table of a join or right input table
     * @param spec the spec of the input table to be joined with another input table
     * @throws InvalidSettingsException if the include or join column names do not exist in the spec, or are not present (empty array or null)
     */
    private JoinTableSettings(final boolean retainUnmatched, final Object[] joinColumns, final String[] includeColumns,
        final InputTable side, final DataTableSpec spec) throws InvalidSettingsException {

        if(joinColumns == null || joinColumns.length == 0) {
            throw new InvalidSettingsException("No join columns passed.");
        }
        if (includeColumns == null) {
            throw new InvalidSettingsException("No include columns passed.");
        }

        m_side = side;
        m_retainUnmatched = retainUnmatched;

        // just adding a fictional column name isn't the solution. see m_joinColumnNames
        m_joinClauses = Arrays.asList(joinColumns);
        //                Arrays.stream(joinColumns)
        //            .filter(IS_SPECIAL_COLUMN.negate())
        //            .toArray(String[]::new);
        //            .map(col -> col instanceof SpecialJoinColumn ?
        //((SpecialJoinColumn)col).safeColumnName(spec::containsName) : col.toString())
        m_includeColumnNames = Arrays.stream(includeColumns).collect(Collectors.toList());

        // join and include columns
        m_joinColumnNames = getJoinClauses().stream().filter(SpecialJoinColumn::isNot).map(Object::toString)
            .collect(Collectors.toList());

        // make sure join column names are present in the data table specification
        validate(getJoinColumnNames(), "join", spec);
        validate(getIncludeColumnNames(), "include", spec);

        m_joinClauseColumns = getJoinClauses().stream()
                .mapToInt(clause -> {
                    if (clause == SpecialJoinColumn.ROW_KEY) {
                        return SpecialJoinColumn.ROW_KEY.m_columnIndexIndicator;
                    } else {
                        return spec.findColumnIndex(clause.toString());
                    }
                }).toArray();


        m_includeColumns = spec.columnsToIndices(includeColumns);
        // working table specification
        m_materializeColumnIndices =
            IntStream.concat(Arrays.stream(m_joinClauseColumns), Arrays.stream(m_includeColumns))
                .distinct()             // column is materialized only once
                .filter(i -> i >= 0)    // only actual columns, no special join columns
                .sorted()
                .toArray();

        m_materializedCells = Optional.empty();
        m_forTable = Optional.empty();
        m_tableSpec = spec;

    }

    JoinTableSettings(final boolean retainUnmatched, final Object[] joinColumns, final String[] includeColumns,
        final JoinSpecification.InputTable side, final BufferedDataTable table) throws InvalidSettingsException {
        this(retainUnmatched, joinColumns, includeColumns, side, table.getDataTableSpec());
        setTable(table);
    }

    /**
     * Join columns can contain non-existing column names (e.g., $Row Key$) to indicate joining on row keys.
     *
     * @param spec
     * @return
     */
    //        int[] findJoinColumns(final DataTableSpec spec) {
    //            return Arrays.stream(joinClauses).mapToInt(name -> {
    //                int col = spec.findColumnIndex(name);
    //                return m_settings.getRowKeyIndicator().equals(name) ? ROW_KEY_COLUMN_INDEX_INDICATOR : col;
    //            }).toArray();
    //        }

    //    /**
    //     * Extract the values from the join columns from the row.
    //     * @param workingRow
    //     *
    //     * @return
    //     */
    //    protected JoinTuple getJoinTupleWorkingRow(final DataRow workingRow) {
    //        int[] indices = m_joinColumnsWorkingTable;
    //        DataCell[] cells = new DataCell[indices.length];
    //        for (int i = 0; i < cells.length; i++) {
    //            if (indices[i] >= 0) {
    //                cells[i] = workingRow.getCell(indices[i]);
    //            } else if (indices[i] == JoinImplementation.ROW_KEY_COLUMN_INDEX_INDICATOR) {
    //                // create a StringCell since row IDs may match
    //                // StringCell's
    //                cells[i] = new StringCell(workingRow.getKey().getString());
    //            }
    //        }
    //        return new JoinTuple(cells);
    //    }


    //    /**
    //     * @param innerSettings
    //     * @return
    //     */
    //    DataTableSpec workingTableWith(final JoinTableSettings innerSettings) {
    //        return new DataTableSpec(
    //            Stream.concat(m_workingTableSpec.stream(), innerSettings.m_workingTableSpec.stream().skip(1))
    //                .toArray(DataColumnSpec[]::new));
    //    }

        /**
         * @param retainUnmatched output unmatched rows from this table
         * @param joinColumns the left hand sides of the join predicate clauses
         * @param includeColumnNames the names of the columns to include in the output
         * @param table the data for which these settings are created
         * @return join table settings marked as settings for the left (a.k.a outer) join table
         * @throws InvalidSettingsException
         */
        public static JoinTableSettings left(final boolean retainUnmatched, final Object[] joinColumns, final String[] includeColumnNames, final BufferedDataTable table) throws InvalidSettingsException {
            return new JoinTableSettings(retainUnmatched, joinColumns, includeColumnNames, InputTable.LEFT, table);
        }

    /**
     * @param retainUnmatched output unmatched rows from this table
     * @param joinColumns the left hand sides of the join predicate clauses
     * @param includeColumnNames the names of the columns to include in the output
     * @param spec the table schema for which these settings are created
     * @return join table settings marked as settings for the left (a.k.a outer) join table
     * @throws InvalidSettingsException
     */
    public static JoinTableSettings left(final boolean retainUnmatched, final Object[] joinColumns, final String[] includeColumnNames, final DataTableSpec spec) throws InvalidSettingsException {
        return new JoinTableSettings(retainUnmatched, joinColumns, includeColumnNames, InputTable.LEFT, spec);
    }

    /**
     * @param retainUnmatched output unmatched rows from this table
     * @param joinColumnNames the left hand sides of the join predicate clauses
     * @param includeColumnNames the names of the columns to include in the output
     * @param table the data for which these settings are created
     * @return join table settings marked as settings for the right (a.k.a inner) join table
     * @throws InvalidSettingsException
     */
    public static JoinTableSettings right(final boolean retainUnmatched, final Object[] joinColumnNames, final String[] includeColumnNames, final BufferedDataTable table) throws InvalidSettingsException {
        return new JoinTableSettings(retainUnmatched, joinColumnNames, includeColumnNames, InputTable.RIGHT, table);
    }

    /**
     * @param retainUnmatched output unmatched rows from this table
     * @param joinColumnNames the left hand sides of the join predicate clauses
     * @param includeColumnNames the names of the columns to include in the output
     * @param spec the table schema for which these settings are created
     * @return join table settings marked as settings for the right (a.k.a inner) join table
     * @throws InvalidSettingsException
     */
    public static JoinTableSettings right(final boolean retainUnmatched, final Object[] joinColumnNames, final String[] includeColumnNames, final DataTableSpec spec) throws InvalidSettingsException {
        return new JoinTableSettings(retainUnmatched, joinColumnNames, includeColumnNames, InputTable.RIGHT, spec);
    }

    /**
     * @return the number of cells in join and include columns, or zero if no table has been set {@link #setTable(BufferedDataTable)}
     */
    protected Optional<Long> getMaterializedCells() {
        return m_materializedCells;
    }

    /**
     * @param bufferedDataTable
     */
    public void setTable(final BufferedDataTable bufferedDataTable) {
        m_forTable = Optional.of(bufferedDataTable);
        m_materializedCells = Optional.of(m_materializeColumnIndices.length * bufferedDataTable.size());
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
    protected void setTableSpec(final DataTableSpec tableSpec) {
        m_tableSpec = tableSpec;
    }

    /**
     * @return the format of the table holding the input data, as returned by {@link #getTable()}. However, sometimes
     *         the spec is created first, in which case this object just holds the specification until
     *         {@link #setTable(BufferedDataTable)} is called.
     */
    public DataTableSpec getTableSpec() {
        return m_tableSpec;
    }

    /**
     * Validate join and include column names by verifying that they exist in the given table spec.
     * @param joinColumnNames join or include column names
     * @throws InvalidSettingsException if a column name does not exist in the spec
     */
    private static void validate(final List<String> joinColumnNames, final String columnType, final DataTableSpec spec) throws InvalidSettingsException {
        Predicate<String> validColumnName = spec::containsName;

        List<String> invalidColumnNames = joinColumnNames.stream()
                .filter(validColumnName.negate())
                .collect(Collectors.toList());

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
    public JoinTableSettings condensed(final boolean storeRowOffsets) {

        // keep only materialized columns
        final ColumnRearranger materializedColFilter = new ColumnRearranger(getTableSpec());
        materializedColFilter.keepOnly(m_materializeColumnIndices);
        final DataTableSpec materializedColumns = materializedColFilter.createSpec();

        // add row offset column if necessary
        final DataTableSpec outputSpec =
            storeRowOffsets ? OrderedRow.withOffset(materializedColumns) : materializedColumns;

        JoinTableSettings result;
        try {
            result = new JoinTableSettings(m_retainUnmatched, getJoinClauses().toArray(new Object[0]),
                getIncludeColumnNames().toArray(new String[0]), getSide(), outputSpec);
            return result;

        } catch (InvalidSettingsException ex) {
            // This can't happen since we can't invalidate valid settings by removing unused columns
            assert false;
            return null;
        }
//        System.out.println(String.format("condensed %s", getSide()));
//        System.out.println("m_materializeColumnIndices=" + Arrays.toString(m_materializeColumnIndices));
//        System.out.println(String.format("condensed outputSpec=%s", outputSpec));
//        System.out.println(String.format("Arrays.toString(result.getJoinClauseColumns()=%s", Arrays.toString(result.getJoinClauseColumns())));
//        System.out.println("result.m_includeColumns=" + Arrays.toString(result.m_includeColumns));

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
    public DataRow condensed(final DataRow row, final long rowOffset, final boolean storeOffset) {
        return OrderedRow.materialize(this, row, rowOffset, storeOffset);
    }

    /** @return whether this is the configuration for the left-hand input table of a join or the right-hand input. */
    public InputTable getSide() {
        return m_side;
    }

    /** @return Whether to output unmatched rows from this input table in the join results. */
    protected boolean isRetainUnmatched() {
        return m_retainUnmatched;
    }

    /**
     * @return the set of column names mentioned in the {@link #getJoinClauses()}, i.e., not the special join columns
     */
    public List<String> getJoinColumnNames() {
        return m_joinColumnNames;
    }

    /**
     * @return the set of column names selected for inclusion in the output
     */
    public List<String> getIncludeColumnNames() {
        return m_includeColumnNames;
    }

    /**
     * @return the sequence of left/right hand sides of the join clauses
     */
    public List<Object> getJoinClauses() {
        return m_joinClauses;
    }

    /**
     * @return the joinClauseColumns
     */
    protected int[] getJoinClauseColumns() {
        return m_joinClauseColumns;
    }

    /**
     * @return the indices of the columns to include in the result
     */
    public int[] getIncludeColumns() {
        return m_includeColumns;
    }

}