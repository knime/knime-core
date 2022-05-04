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
 *   Jun 9, 2020 (carlwitt): created
 */
package org.knime.core.data.join;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.knime.core.data.join.JoinTestInput.cell;
import static org.knime.core.data.join.JoinTestInput.col;
import static org.knime.core.data.join.JoinTestInput.defaultRow;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.join.JoinSpecification.Builder;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.data.join.JoinTableSettings.SpecialJoinColumn;
import org.knime.core.data.join.implementation.JoinImplementation;
import org.knime.core.data.join.implementation.JoinerFactory.JoinAlgorithm;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;

/**
 * Tests
 * <ul>
 * <li>join table specification generation</li>
 * <li>default row key factories (sequence and concat) {@link JoinSpecification#createConcatRowKeysFactory(String)}</li>
 * <li>column remapping in {@link #testSwapInTables()}</li>
 * <li>creating {@link JoinTableSettings}</li>
 * </ul>
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("javadoc")
public class JoinSpecificationTest {

    protected static final int LEFT = 0;

    protected static final int RIGHT = 1;

    DataCell[][] m_dataCells = new DataCell[][]{
        new DataCell[]{cell("a"), cell("b"), cell("c"), cell("d"), cell("e")},
        new DataCell[]{cell("u"), cell("v"), cell("w"), cell("x"), cell("y"), cell("z")}
    };

    protected DataRow[] m_rows =
        new DataRow[]{JoinTestInput.defaultRow("left,a,b,c,d,e"), JoinTestInput.defaultRow("right,u,v,w,x,y,z")};

    DataTableSpec[] m_specs = new DataTableSpec[]{
        new DataTableSpec(col("A"), col("B"), col("C"), col("D"), col("E")),
        new DataTableSpec(col("U"), col("V"), col("W"), col("A"), col("Y"), col("Z"))};

    protected JoinTableSettings[] m_settings;

    public JoinSpecificationTest() throws InvalidSettingsException {
        m_settings = new JoinTableSettings[2];
        m_settings[LEFT] = new JoinTableSettings(true, JoinColumn.array("A", "B", "C", "D"),
            new String[]{"A", "C", "E"}, InputTable.LEFT, m_specs[LEFT]);
        m_settings[RIGHT] = new JoinTableSettings(true, JoinColumn.array("A", "Y", "Z", "A"),
            new String[]{"U", "V", "A"}, InputTable.RIGHT, m_specs[RIGHT]);
    }

    /**
     * The completeness of the copy constructor {@link Builder#from(JoinSpecification)} is extremely important because
     * disjunctive joins rely on it (a disjunctive join is reduced to a series of conjunctive joins, each copying and
     * modifying the original join specification - if a part of the specification is not copied, the join will not
     * adhere to the specification).
     *
     * When adding a new field to the join specification builder, this test will fail, reminding developers to add the
     * new field to the copy constructor {@link Builder#from(JoinSpecification)}
     *
     * Added in response to AP-18854: Inconsistent Behaviour between Match All and Match Any when comparing Row IDs
     */
    @Test
    public void checkCopyConstructorCoverage() {
        var publicBuilderMethods = Arrays.stream(JoinSpecification.Builder.class.getDeclaredMethods())//
            .filter(m -> Modifier.isPublic(m.getModifiers())).map(Method::getName)//
            .collect(Collectors.toSet());

        // before changing: make sure to add the new field to {@link Builder#from(JoinSpecification)}!
        Set<String> expectedBuilderMethods =
            Set.of("rowKeyFactory", "retainMatched", "mergeJoinColumns", "conjunctive", "build", "outputRowOrder",
                "dataCellComparisonMode", "from", "columnNameDisambiguator", "usingOnlyJoinClause");

        Set<String> newMethods = new HashSet<>(publicBuilderMethods);
        newMethods.removeAll(expectedBuilderMethods);
        assertTrue(String.format(
            "Builder has new methods %s, make sure the new fields are added to the copy constructor Builder#from",
            newMethods), newMethods.isEmpty());
    }

    /**
     * Test the data table specification generated for the join result table. This depends on
     * <ul>
     * <li>which columns are selected for inclusion in the result table</li>
     * <li>how column names are disambiguated</li>
     * <li>whether join columns should be merged</li>
     * </ul>
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testOutputSpec() throws InvalidSettingsException {
        JoinSpecification joinSpec = new JoinSpecification.Builder(m_settings[LEFT], m_settings[RIGHT])
            .columnNameDisambiguator(name -> name.concat(" (right table)")).mergeJoinColumns(false).build();

        { // output table specification for matches
            DataTableSpec spec = joinSpec.specForMatchTable();

            // expected output columns
            int[] indices = spec.columnsToIndices("A", "C", "E", "U", "V", "A (right table)");
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        { // output table specification for unmatched left rows
            DataTableSpec spec = joinSpec.specForUnmatched(InputTable.LEFT);
            // expected output columns
            int[] indices = spec.columnsToIndices("A", "C", "E");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, 3).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        { // output table specification for unmatched right rows
            DataTableSpec spec = joinSpec.specForUnmatched(InputTable.RIGHT);
            // columns should be named like this (no disambiguation)
            int[] indices = spec.columnsToIndices("U", "V", "A");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

    }

    /**
     * Test that column names are disambiguated against included columns. For instance, when joining
     *
     * <pre>
     *          A B C  ⨝  A B C
     * include  ✓   ✓       ✓
     * out      A C B
     * </pre>
     *
     * B from left is not included and thus B from right table doesn't doesn't need to disambiguation.
     *
     * When merging join columns
     *
     * <pre>
     *         A B C  ⨝  A B C A=B B=C
     * include ✓   ✓     ✓ ✓    ✓   ✓
     * join w/ B C
     * out     `A=B` C A `A=B (#1)` `B=C`
     * </pre>
     *
     * columns in the right table also need to be disambiguated against newly created merge columns (A=B), however, only
     * if the merge column merges at least one included column (B=C merges B and C, but neither is included), so B=C
     * from the right table doesn't need to be disambiguated.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testEffectiveDisambiguation() throws InvalidSettingsException {

        { // default case
            DataTableSpec abc = new DataTableSpec(col("A"), col("B"), col("C"));
            JoinTableSettings leftSettings =
                new JoinTableSettings(false, JoinColumn.array("A"), new String[]{"A", "C"}, InputTable.LEFT, abc);
            JoinTableSettings rightSettings =
                new JoinTableSettings(false, JoinColumn.array("A"), new String[]{"B"}, InputTable.RIGHT, abc);

            JoinSpecification joinSpec =
                new JoinSpecification.Builder(leftSettings, rightSettings).mergeJoinColumns(false).build();

            DataTableSpec spec = joinSpec.specForMatchTable();

            // expected output columns
            int[] indices = spec.columnsToIndices("A", "C", "B");
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        { // merge join columns
            DataTableSpec abc = new DataTableSpec(col("A"), col("B"), col("C"));
            DataTableSpec abcClash = new DataTableSpec(col("A"), col("B"), col("C"), col("A=B"), col("B=C"));
            JoinTableSettings leftSettings =
                new JoinTableSettings(false, JoinColumn.array("A", "B"), new String[]{"A", "C"}, InputTable.LEFT, abc);
            JoinTableSettings rightSettings = new JoinTableSettings(false, JoinColumn.array("B", "C"),
                new String[]{"A", "B", "A=B", "B=C"}, InputTable.RIGHT, abcClash);

            JoinSpecification joinSpec =
                new JoinSpecification.Builder(leftSettings, rightSettings)
                    .mergeJoinColumns(true)
                    .columnNameDisambiguator(s -> s + " (#1)")
                    .build();

            DataTableSpec spec = joinSpec.specForMatchTable();

            // expected output columns
            // B from right table is included explicitly but not present in the output because it is merged into A=B
            int[] indices = spec.columnsToIndices("A=B", "C", "A", "A=B (#1)", "B=C");
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }
    }

    /**
     * Test the case in which a column name disambiguator produces a column name that is also already taken in the left
     * table.
     */
    @Test
    public void testDoubleClash() throws InvalidSettingsException {
        DataTableSpec left = new DataTableSpec(col("A"), col("B"), col("A (disambiguated)"));
        DataTableSpec right = new DataTableSpec(col("A"), col("B"), col("C"));
        JoinTableSettings leftSettings = new JoinTableSettings(false, JoinColumn.array("A"),
            new String[]{"A", "B", "A (disambiguated)"}, InputTable.LEFT, left);
        JoinTableSettings rightSettings =
            new JoinTableSettings(false, JoinColumn.array("A"), new String[]{"A", "B", "C"}, InputTable.RIGHT, right);

        JoinSpecification joinSpec = new JoinSpecification.Builder(leftSettings, rightSettings)
            .columnNameDisambiguator(s -> s.concat(" (disambiguated)")) // won't disambiguate column name
            .build();

        DataTableSpec spec = joinSpec.specForMatchTable();
        System.out.println(String.format("spec=%s", spec));

        // named like this
        int[] indices = spec.columnsToIndices("A", "B", "A (disambiguated)", "A (disambiguated) (disambiguated)",
            "B (disambiguated)", "C");
        int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
        assertArrayEquals(expectedIndices, indices);

        assertEquals(expectedIndices.length, spec.getNumColumns());

    }

    /**
     * Test the output spec generation for two sequential joins with lots of duplicate columns and all combinations of
     * merge join columns on/off for the first and the second join, respectively. This was built around a use case where
     * the output spec contained duplicate column names.
     *
     * <pre>
     * original table  xx xy yx yy M
     * concated table  xx xy yx yy M xx=xy yx_ yy_ M_
     * </pre>
     *
     * For all combinations of mergeFirst, mergeSeconds
     * <ol>
     * <li>B = self-join the original table with merge join columns = mergeFirst</li>
     * <li>Perform a join of the original table with B using merge join columns = mergeSecond</li>
     * </ol>
     * {@link JoinSpecification#specForMatchTable()} will throw an Exception if column name deduplication does not work.
     */
    @Test
    public void joinJoinedResults() throws InvalidSettingsException {
        // a simple test table, with specs as produced by data generator node
        BufferedDataTable original = JoinTestInput.table("xx,xy,yx,yy,M", new DataRow[0]);
        JoinTableSettings firstLeftSettings = new JoinTableSettings(true, JoinColumn.array("xx"),
            original.getDataTableSpec().getColumnNames(), InputTable.LEFT, original);
        JoinTableSettings firstRightSettings = new JoinTableSettings(true, JoinColumn.array("yx"),
            original.getDataTableSpec().getColumnNames(), InputTable.RIGHT, original);

        //this has the output specs of a table produced by a concat operation of original with the self-join result (see below)
        BufferedDataTable concated = JoinTestInput.table("xx,xy,yx,yy,M,xx=xy,yx_,yy_,M_", new DataRow[0]);
        JoinTableSettings concatedSettings = new JoinTableSettings(true, JoinColumn.array("xx"),
            concated.getDataTableSpec().getColumnNames(), InputTable.LEFT, concated);

        for (boolean mergeFirst : new boolean[]{false, true}) {
            for (boolean mergeSecond : new boolean[]{false, true}) {
                // compute the output spec for a self join on the original table
                JoinSpecification selfJoin = new JoinSpecification.Builder(firstLeftSettings, firstRightSettings)
                        .mergeJoinColumns(mergeFirst)
                        .columnNameDisambiguator(s -> s + "_")
                        .build();

                DataTableSpec selfJoinSpec = null;
                try {
                    selfJoinSpec = selfJoin.specForMatchTable();
                } catch(IllegalArgumentException e) {
                    fail(String.format("Column name disambiguation failed for mergeFirst = %s %n%s",
                        mergeFirst, e.getMessage()));
                }


                // now join the self-join result with the concated table
                JoinTableSettings selfJoinSettings = new JoinTableSettings(true, JoinColumn.array("xx"),
                    selfJoinSpec.getColumnNames(), InputTable.RIGHT, selfJoinSpec);
                JoinSpecification followUpJoin = new JoinSpecification.Builder(concatedSettings, selfJoinSettings)
                        .columnNameDisambiguator(s -> s + "_")
                        .mergeJoinColumns(mergeSecond)
                        .build();

                try {
                    followUpJoin.specForMatchTable();
                } catch (IllegalArgumentException e) {
                    fail(String.format(
                        "Column name disambiguation failed for mergeFirst = %s mergeSecond = %s %n"
                            + "Self-join output spec: %s %n" + "%s",
                        mergeFirst, mergeSecond, selfJoinSpec.toString(), e.getMessage()));
                }
            }
        }
    }

    /**
     * Test that a column name disambiguator that doesn't change the string doesn't cause an infinite loop
     *
     * @throws InvalidSettingsException
     * @throws Exception
     */
    @Test
    public void testFaultyDisambiguator() throws InvalidSettingsException {
        DataTableSpec abc = new DataTableSpec(col("A"), col("B"), col("C"));
        JoinTableSettings leftSettings =
            new JoinTableSettings(false, JoinColumn.array("A"), new String[]{"A", "B", "C"}, InputTable.LEFT, abc);
        JoinTableSettings rightSettings =
            new JoinTableSettings(false, JoinColumn.array("A"), new String[]{"A", "B", "C"}, InputTable.RIGHT, abc);

        JoinSpecification joinSpec =
            new JoinSpecification.Builder(leftSettings, rightSettings).columnNameDisambiguator(s -> s) // won't disambiguate column name
                .build();

        DataTableSpec spec = joinSpec.specForMatchTable();

        // named like this
        int[] indices = spec.columnsToIndices("A", "B", "C", "AA", "BB", "CC");
        int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
        assertArrayEquals(expectedIndices, indices);

        assertEquals(expectedIndices.length, spec.getNumColumns());

    }

    /**
     * Test that appending a space as disambiguator works.
     */
    @Test
    public void spaceDisambiguator() throws InvalidSettingsException {
        DataTableSpec abc = new DataTableSpec(col("A"), col("B"), col("C"));
        JoinTableSettings leftSettings = new JoinTableSettings(false, JoinColumn.array(SpecialJoinColumn.ROW_KEY),
            new String[]{"A", "B", "C"}, InputTable.LEFT, abc);
        JoinTableSettings rightSettings = new JoinTableSettings(false, JoinColumn.array(SpecialJoinColumn.ROW_KEY),
            new String[]{"A", "B", "C"}, InputTable.RIGHT, abc);

        for (boolean mergeJoinColumns : new boolean[]{false, true}) {

            JoinSpecification joinSpec = new JoinSpecification.Builder(leftSettings, rightSettings)
                .columnNameDisambiguator(s -> s.concat(" ")).mergeJoinColumns(mergeJoinColumns).build();

            DataTableSpec spec = joinSpec.specForMatchTable();

            // named like this
            int[] indices = spec.columnsToIndices("A", "B", "C", "AA", "BB", "CC");
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);

            assertEquals(expectedIndices.length, spec.getNumColumns());
        }

    }

    /**
     * Test that the number of join conditions is returned correctly for match any and match all
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testGetNumJoinConditions() throws InvalidSettingsException {
        DataTableSpec abc = new DataTableSpec(col("A"), col("B"), col("C"));
        JoinTableSettings leftSettings = new JoinTableSettings(false, JoinColumn.array("A", "B", "C"),
            new String[]{"A", "B", "C"}, InputTable.LEFT, abc);
        JoinTableSettings rightSettings = new JoinTableSettings(false, JoinColumn.array("A", "B", "C"),
            new String[]{"A", "B", "C"}, InputTable.RIGHT, abc);

        { // match all gives one conjunctive groups containing all join column pairs
            JoinSpecification joinSpec =
                new JoinSpecification.Builder(leftSettings, rightSettings).conjunctive(true).build();

            assertEquals(3, joinSpec.getNumJoinClauses());
        }

        { // match any gives three conjunctive groups, one for each join column pair
            JoinSpecification joinSpec =
                new JoinSpecification.Builder(leftSettings, rightSettings).conjunctive(false).build();
            assertEquals(3, joinSpec.getNumJoinClauses());
        }

    }

    /**
     * Test remapping a join specification to a working table by making sure that applying project operations deliver
     * the same results both on the original table and the working table. Say the original left table is
     *
     * <pre>
     *          A B C D E
     * join     j   j
     * include          i
     * </pre>
     *
     * and the original right table is
     *
     * <pre>
     *          U V W X Y Z
     * join     j         j
     * include    i     i
     * </pre>
     *
     * Let the join specification be C = Z && A == U. <br/>
     * Then the working table specs with row offsets are
     *
     * <pre>
     * left:  _offset_ A C E
     * right: _offset_ U V Y Z
     * </pre>
     *
     * Applying the original join specification to the working table is no big deal, since everything we need is there.
     * However, the column accessors need to be remapped to the new spec, such that
     * {@link JoinTuple#get(JoinTableSettings, DataRow)} accesses the right rows and
     * {@link JoinSpecification#rowProjectOuter(InputTable, DataRow)} includes the right rows.
     *
     * @throws InvalidSettingsException
     *
     */
    @Test
    public void testSwapInTables() throws InvalidSettingsException {
        DataRow leftOriginalRow = JoinTestInput.defaultRow("LeftRow0,a,b,c,d,e");
        BufferedDataTable leftOriginalTable = JoinTestInput.table("A,B,C,D,E", leftOriginalRow);
        DataRow rightOriginalRow = JoinTestInput.defaultRow("RightRow0,u,v,w,x,y,z");
        BufferedDataTable rightOriginalTable = JoinTestInput.table("U,V,W,X,Y,Z", rightOriginalRow);

        JoinTableSettings leftSettings = new JoinTableSettings(false, JoinColumn.array("C", "A"), new String[]{"E"},
            InputTable.LEFT, leftOriginalTable);
        JoinTableSettings rightSettings = new JoinTableSettings(false, JoinColumn.array("Z", "U"),
            new String[]{"V", "Y"}, InputTable.RIGHT, rightOriginalTable);
        JoinSpecification original = new JoinSpecification.Builder(leftSettings, rightSettings).build();

        for (boolean storeRowOffsets : new boolean[]{true, false}) {
            DataRow leftWorkingRow, rightWorkingRow;
            if (!storeRowOffsets) {
                leftWorkingRow = JoinTestInput.defaultRow("LeftRow0,a,c,e");
                rightWorkingRow = JoinTestInput.defaultRow("RightRow0,u,v,y,z");
            } else {
                leftWorkingRow = JoinTestInput.defaultRow("LeftRow0,a,c,e", 0);
                rightWorkingRow = JoinTestInput.defaultRow("RightRow0,u,v,y,z", 0);
            }
            BufferedDataTable leftWorkingTable = JoinTestInput.table("A,C,E", storeRowOffsets, leftWorkingRow);
            BufferedDataTable rightWorkingTable = JoinTestInput.table("U,V,Y,Z", storeRowOffsets, rightWorkingRow);

            JoinTableSettings leftWorkingSettings = leftSettings.condensed(storeRowOffsets);
            leftWorkingSettings.setTable(leftWorkingTable);
            JoinTableSettings rightWorkingSettings = rightSettings.condensed(storeRowOffsets);
            rightWorkingSettings.setTable(rightWorkingTable);

            assertArrayEquals(leftSettings.get(leftOriginalRow), leftWorkingSettings.get(leftWorkingRow));

            JoinSpecification working = original.specWith(leftWorkingSettings, rightWorkingSettings);

            BiPredicate<DataRow, DataRow> same = JoinTest.compareDataRows::equals;

            DataRow expectedLeftCondensed = leftSettings.condensed(leftOriginalRow, 0, storeRowOffsets);
            assertTrue(String.format("expected %s actual %s", expectedLeftCondensed, leftWorkingRow),
                same.test(expectedLeftCondensed, leftWorkingRow));
            assertTrue(same.test(rightSettings.condensed(rightOriginalRow, 0, storeRowOffsets), rightWorkingRow));

            assertTrue(same.test(original.rowProjectOuter(InputTable.LEFT, leftOriginalRow),
                working.rowProjectOuter(InputTable.LEFT, leftWorkingRow)));

            assertTrue(same.test(original.rowProjectOuter(InputTable.RIGHT, rightOriginalRow),
                working.rowProjectOuter(InputTable.RIGHT, rightWorkingRow)));

        }

    }

    /**
     * Test the lookup of columns on which a column joins.
     */
    @Test
    public void testColumnJoinPartners() throws InvalidSettingsException {
        {
            // left table's A joins on right table's A
            JoinTableSettings left = new JoinTableSettings(true, JoinColumn.array("A", "B", "C", "D"),
                new String[]{"A", "C", "E"}, InputTable.LEFT, m_specs[LEFT]);
            // right table's A is joined upon from left table's A and D
            JoinTableSettings right = new JoinTableSettings(true, JoinColumn.array("A", "Y", "Z", "A"),
                new String[]{"U", "V", "A"}, InputTable.RIGHT, m_specs[RIGHT]);

            JoinSpecification spec = new JoinSpecification.Builder(left, right).mergeJoinColumns(true).build();

            List<String> joinPartnersA = spec.columnJoinPartners(InputTable.LEFT, "A").collect(Collectors.toList());
            assertArrayEquals(new String[] {"A"}, joinPartnersA.toArray(String[]::new));

            List<String> joinPartnersABackwards =
                spec.columnJoinPartners(InputTable.RIGHT, "A").collect(Collectors.toList());
            assertArrayEquals(new String[] {"A", "D"}, joinPartnersABackwards.toArray(String[]::new));
        }
        {
            // left table's A joins on A, Y, Row Key, and A
            JoinTableSettings left = new JoinTableSettings(true, JoinColumn.array("A", "A", "A", "A"),
                new String[]{"A", "C", "E"}, InputTable.LEFT, m_specs[LEFT]);
            JoinTableSettings right =
                new JoinTableSettings(true, JoinColumn.array("A", "Y", SpecialJoinColumn.ROW_KEY, "A"),
                    new String[]{"U", "V", "A"}, InputTable.RIGHT, m_specs[RIGHT]);

            JoinSpecification spec = new JoinSpecification.Builder(left, right).mergeJoinColumns(true).build();

            List<String> joinPartnersA = spec.columnJoinPartners(InputTable.LEFT, "A").collect(Collectors.toList());
            assertArrayEquals(new String[] {"A", "Y", "A"}, joinPartnersA.toArray(String[]::new));
        }
    }

    /**
     * When merging join columns, the same column can have multiple join partners. In this case the left column will
     * consume all right columns it joins on (because they're all the same). If one of the right columns has the same
     * name as the left column, the left original name is kept. Otherwise it will be named using the A=B form.
     *
     * <pre>
     *         A B C  ⨝  A B C
     * include ✓   ✓     ✓ ✓
     * left join clauses A = B right join clauses
     *                   A = A
     *                   A = C
     * </pre>
     *
     */
    @Test
    public void testMergeMultipleJoinsLeft() throws InvalidSettingsException {

        DataTableSpec abc = new DataTableSpec(col("A"), col("B"), col("C"));

        JoinTableSettings leftSettings = new JoinTableSettings(false, JoinColumn.array("A", "A", "A"),
            new String[]{"A", "B", "C"}, InputTable.LEFT, abc);
        JoinTableSettings rightSettings =
            new JoinTableSettings(false, JoinColumn.array("B", "A", "C"), new String[0], InputTable.RIGHT, abc);

        JoinSpecification joinSpec =
            new JoinSpecification.Builder(leftSettings, rightSettings).mergeJoinColumns(true).build();
        DataTableSpec spec = joinSpec.specForMatchTable();

        int[] indices = spec.columnsToIndices("A", "B", "C");
        int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
        assertArrayEquals(expectedIndices, indices);
    }

    /**
     * When multiple columns from the left table join on the same column from the right table, no merge is performed.
     *
     * <pre>
     *         A B C  ⨝  A B C
     * include ✓ ✓ ✓     ✓   ✓
     * join w/ B A
     * out     A `B=A` C C'
     * </pre>
     *
     */
    @Test
    public void testMergeMultipleJoinsRight() throws InvalidSettingsException {

        DataTableSpec abc = new DataTableSpec(col("A"), col("B"), col("C"));

        JoinTableSettings leftSettings =
            new JoinTableSettings(false, JoinColumn.array("A", "B"), new String[]{"A", "B", "C"}, InputTable.LEFT, abc);
        JoinTableSettings rightSettings =
            new JoinTableSettings(false, JoinColumn.array("A", "A"), new String[]{"A", "C"}, InputTable.RIGHT, abc);

        JoinSpecification joinSpec = new JoinSpecification.Builder(leftSettings, rightSettings).mergeJoinColumns(true)
            .columnNameDisambiguator(s -> s + "'").build();
        DataTableSpec spec = joinSpec.specForMatchTable();

        int[] indices = spec.columnsToIndices("A", "B=A", "C", "C'");
        int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
        assertArrayEquals(expectedIndices, indices);
    }

    /**
     * Test the data table specification generated for the join result table. This depends on
     * <ul>
     * <li>which columns are selected for inclusion in the result table</li>
     * <li>how column names are disambiguated</li>
     * <li>whether join columns should be merged</li>
     * </ul>
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testMergeJoinColumnsOutputSpec() throws InvalidSettingsException {
        JoinSpecification joinSpec = new JoinSpecification.Builder(m_settings[LEFT], m_settings[RIGHT])
            .columnNameDisambiguator(name -> name.concat(" (right table)")).mergeJoinColumns(true).build();

        { // output table specification for matches
            DataTableSpec spec = joinSpec.specForMatchTable();

            // joined table has seven columns
            assertEquals(6, spec.getNumColumns());
            // named like this
            int[] indices = spec.columnsToIndices("A", "C=Z", "D=A", "E", "U", "V");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, 6).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        { // output table specification for unmatched left rows
            DataTableSpec spec = joinSpec.specForUnmatched(InputTable.LEFT);
            assertEquals(3, spec.getNumColumns());
            // named like this
            int[] indices = spec.columnsToIndices("A", "C", "E");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        { // output table specification for unmatched right rows
            DataTableSpec spec = joinSpec.specForUnmatched(InputTable.RIGHT);
            // named like this (no disambiguation)
            int[] indices = spec.columnsToIndices("U", "V", "A");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

    }

    /**
     * Test the join merge columns feature in combination with special join columns (join on row keys).
     *
     * <pre>
     * left table RowID A B C D E
     * include          ✓   ✓   ✓
     *
     * right table  U V W A Y Z
     * include      ✓ ✓   ✓
     *
     * join clauses A = A
     *              RowKey = RowKey
     *              C = Z
     *              D = A
     * </pre>
     * Expected specification: "A", "C=Z", "D=A", "E", "U", "V"
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testMergeJoinColumnsWithRowKeys() throws InvalidSettingsException {
        JoinTableSettings leftSettings =
            new JoinTableSettings(true, JoinColumn.array("A", JoinTableSettings.SpecialJoinColumn.ROW_KEY, "C", "D"),
                new String[]{"A", "C", "E"}, InputTable.LEFT, m_specs[LEFT]);
        JoinTableSettings rightSettings =
            new JoinTableSettings(true, JoinColumn.array("A", JoinTableSettings.SpecialJoinColumn.ROW_KEY, "Z", "A"),
                new String[]{"U", "V", "A"}, InputTable.RIGHT, m_specs[RIGHT]);
        JoinSpecification joinSpec = new JoinSpecification.Builder(leftSettings, rightSettings)
            .columnNameDisambiguator(name -> name.concat(" (right table)")).mergeJoinColumns(true).build();

        { // output table specification for matches
            DataTableSpec spec = joinSpec.specForMatchTable();

            // output columns should be named like this
            int[] indices = spec.columnsToIndices("A", "C=Z", "D=A", "E", "U", "V");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        { // output table specification for unmatched left rows
            DataTableSpec spec = joinSpec.specForUnmatched(InputTable.LEFT);
            // named like this
            int[] indices = spec.columnsToIndices("A", "C", "E");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        { // output table specification for unmatched right rows
            DataTableSpec spec = joinSpec.specForUnmatched(InputTable.RIGHT);
            // named like this (no disambiguation)
            int[] indices = spec.columnsToIndices("U", "V", "A");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        // test project
        DataRow leftProjected = joinSpec.rowProjectOuter(InputTable.LEFT, m_rows[LEFT]);

        assertEquals(3, leftProjected.getNumCells());

        // projected row has the cell contents of columns A, C, E
        assertEquals(cell("a"), leftProjected.getCell(0));
        assertEquals(cell("c"), leftProjected.getCell(1));
        assertEquals(cell("e"), leftProjected.getCell(2));

        DataRow rightProjected = joinSpec.rowProjectOuter(InputTable.RIGHT, m_rows[RIGHT]);

        assertEquals(3, rightProjected.getNumCells());

        // projected row has the cell contents of columns U, V, A (#1)
        assertEquals(cell("u"), rightProjected.getCell(0));
        assertEquals(cell("v"), rightProjected.getCell(1));
        assertEquals(cell("x"), rightProjected.getCell(2));

    }

    /**
     * Test merging multiple columns with a row key column.
     *
     * <pre>
     * left table  A B C D E
     * include     ✓   ✓   ✓
     *
     * right table  U V W A Y Z
     * include      ✓     ✓ ✓
     *
     * join clauses RowKey = Y
     *              RowKey = Z
     *              A = RowKey
     *              A = U
     *
     * </pre>
     * Expected output spec: A=U C E A.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testMergeJoinColumnsWithMultipleRowKeys() throws InvalidSettingsException {
        JoinTableSettings leftSettings = new JoinTableSettings(true, JoinColumn.array(
            JoinTableSettings.SpecialJoinColumn.ROW_KEY,
            JoinTableSettings.SpecialJoinColumn.ROW_KEY, "A", "A"),
                new String[]{"A", "C", "E"}, InputTable.LEFT, m_specs[LEFT]);
        JoinTableSettings rightSettings =
            new JoinTableSettings(true, JoinColumn.array("Y", "Z", JoinTableSettings.SpecialJoinColumn.ROW_KEY, "U"),
                new String[]{"U", "A", "Y"}, InputTable.RIGHT, m_specs[RIGHT]);
        JoinSpecification joinSpec = new JoinSpecification.Builder(leftSettings, rightSettings)
            .columnNameDisambiguator(name -> name.concat(" (right table)")).mergeJoinColumns(true).build();

        { // output table specification for matches
            DataTableSpec spec = joinSpec.specForMatchTable();
            // output columns should be named like this
            int[] indices = spec.columnsToIndices("A=U", "C", "E", "A");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        { // output table specification for unmatched left rows
            DataTableSpec spec = joinSpec.specForUnmatched(InputTable.LEFT);
            // named like this
            int[] indices = spec.columnsToIndices("A", "C", "E");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        { // output table specification for unmatched right rows
            DataTableSpec spec = joinSpec.specForUnmatched(InputTable.RIGHT);
            // named like this (no disambiguation)
            int[] indices = spec.columnsToIndices("U", "A", "Y");
            // and in the above order
            int[] expectedIndices = IntStream.range(0, spec.getNumColumns()).toArray();
            assertArrayEquals(expectedIndices, indices);
        }

        // test project
        DataRow leftProjected = joinSpec.rowProjectOuter(InputTable.LEFT, m_rows[LEFT]);

        assertEquals(3, leftProjected.getNumCells());

        // projected row has the cell contents of columns A, C, E
        assertEquals(cell("a"), leftProjected.getCell(0));
        assertEquals(cell("c"), leftProjected.getCell(1));
        assertEquals(cell("e"), leftProjected.getCell(2));

        DataRow rightProjected = joinSpec.rowProjectOuter(InputTable.RIGHT, m_rows[RIGHT]);

        assertEquals(3, rightProjected.getNumCells());

        // projected row has the cell contents of columns U, A, Y
        assertEquals(cell("u"), rightProjected.getCell(0));
        assertEquals(cell("x"), rightProjected.getCell(1)); // column A contains value x
        assertEquals(cell("y"), rightProjected.getCell(2));

    }

    /**
     * This is used to project data rows from an input table down to the columns included in the joined table.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testProject() throws InvalidSettingsException {
        JoinSpecification jspec = new JoinSpecification.Builder(m_settings[LEFT], m_settings[RIGHT]).build();

        DataRow leftProjected = jspec.rowProjectOuter(InputTable.LEFT, m_rows[LEFT]);

        assertEquals(3, leftProjected.getNumCells());

        // projected row has the cell contents of columns A, C, E
        assertEquals(cell("a"), leftProjected.getCell(0));
        assertEquals(cell("c"), leftProjected.getCell(1));
        assertEquals(cell("e"), leftProjected.getCell(2));

        DataRow rightProjected = jspec.rowProjectOuter(InputTable.RIGHT, m_rows[RIGHT]);

        assertEquals(3, rightProjected.getNumCells());

        // projected row has the cell contents of columns U, V, A (#1)
        assertEquals(cell("u"), rightProjected.getCell(0));
        assertEquals(cell("v"), rightProjected.getCell(1));
        assertEquals(cell("x"), rightProjected.getCell(2));

    }

    /**
     * A normal join just concatenates the included columns. <br/>
     * <br/>
     * When merging join columns, use the values from the left table to fill the output row. Given the join method is
     * only applied when the rows actually match, it shouldn't matter though, because the values in both columns are
     * then identical.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testJoin() throws InvalidSettingsException {
        {// merge join columns
            JoinSpecification jspec =
                new JoinSpecification.Builder(m_settings[LEFT], m_settings[RIGHT]).mergeJoinColumns(true).build();

            DataRow joined = jspec.rowJoin(m_rows[LEFT], m_rows[RIGHT]);

            // joined row has columns "A", "C=Z", "D=A", "E", "U", "V"
            assertEquals(cell("a"), joined.getCell(0));
            assertEquals(cell("c"), joined.getCell(1));
            assertEquals(cell("d"), joined.getCell(2));
            assertEquals(cell("e"), joined.getCell(3));
            assertEquals(cell("u"), joined.getCell(4));
            assertEquals(cell("v"), joined.getCell(5));
        }
        { // do not merge join columns

            JoinSpecification jspec =
                new JoinSpecification.Builder(m_settings[LEFT], m_settings[RIGHT]).mergeJoinColumns(false).build();

            DataRow joined = jspec.rowJoin(m_rows[LEFT], m_rows[RIGHT]);

            // joined row has columns "A", "C", "E", "U", "V", "A (right table)"
            assertEquals(cell("a"), joined.getCell(0));
            assertEquals(cell("c"), joined.getCell(1));
            assertEquals(cell("e"), joined.getCell(2));
            assertEquals(cell("u"), joined.getCell(3));
            assertEquals(cell("v"), joined.getCell(4));
            assertEquals(cell("x"), joined.getCell(5));
        }
    }

    /**
     * When using createSequenceRowKeysFactory, rows in the result are just numbered from Row0 onwards.
     */
    @Test
    public void testCreateSequenceRowKeysFactory() {
        BiFunction<DataRow, DataRow, RowKey> factory = JoinSpecification.createSequenceRowKeysFactory();
        assertEquals(new RowKey("Row0"), factory.apply(m_rows[LEFT], m_rows[RIGHT]));
        assertEquals(new RowKey("Row1"), factory.apply(m_rows[LEFT], null));
        assertEquals(new RowKey("Row2"), factory.apply(null, m_rows[RIGHT]));
    }

    /**
     * When using createConcatRowKeysFactory, the original row keys are concatenated to create row keys for the rows in
     * the result.
     */
    @Test
    public void testCreateConcatRowKeysFactory() {
        BiFunction<DataRow, DataRow, RowKey> factory = JoinSpecification.createConcatRowKeysFactory("_");
        assertEquals(new RowKey("left_right"), factory.apply(m_rows[LEFT], m_rows[RIGHT]));
        assertEquals(new RowKey("left_?"), factory.apply(m_rows[LEFT], null));
        assertEquals(new RowKey("?_right"), factory.apply(null, m_rows[RIGHT]));
    }

    /**
     * When using createKeepRowKeysFactory, it is expected that: both row keys are present and equal, or exactly one row
     * key is present.
     * The present row key is then set as the row key of the output.
     */
    @Test
    public void testCreateKeepRowKeysFactory() {
        final BiFunction<DataRow, DataRow, RowKey> factory = new KeepRowKeysFactory();
        final DataRow left = JoinTestInput.defaultRow("key,a,b,c");
        final DataRow right = JoinTestInput.defaultRow("key,x,y,z");
        assertEquals(new RowKey("key"), factory.apply(left, right));
        assertEquals(new RowKey("key"), factory.apply(left, null));
        assertEquals(new RowKey("key"), factory.apply(null, right));
    }

    /**
     * When using createKeepRowKeysFactory, it is expected that: both row keys are present and equal, or exactly one row
     * key is present. The present row key is then set as the row key of the output.
     * @throws CanceledExecutionException
     */
    @Test
    public void testKeepRowKeysApplicable() throws InvalidSettingsException, CanceledExecutionException {
        final BufferedDataTable left = JoinTestInput.table("A", false, //
            defaultRow("Row0,a"), defaultRow("Row1,a"), defaultRow("Row2,a"));
        final BufferedDataTable right = JoinTestInput.table("X", false, //
            defaultRow("Row0,a"),defaultRow("Row1,x"),defaultRow("Row2,x"));

        JoinColumn[] onRowKey = new JoinColumn[]{new JoinColumn(SpecialJoinColumn.ROW_KEY)};

        // whether to retain matches, left unmatched rows, and right unmatched rows
        boolean[][] retainMatchesLeftRight = new boolean[][]{//
            new boolean[]{false, false, false}, //
            new boolean[]{false, false, true}, //
            new boolean[]{false, true, false}, //
            new boolean[]{false, true, true}, //
            new boolean[]{true, false, false}, //
            new boolean[]{true, false, true}, //
            new boolean[]{true, true, false}, //
            new boolean[]{true, true, true},//
        };
        // whether the factory is applicable under the above row retainment settings, depending on whether row keys are
        // guaranteed to match
        List<Predicate<Boolean>> applicable = List.of(//
            rowKeysMatch -> true, //
            rowKeysMatch -> true, //
            rowKeysMatch -> true, //
            rowKeysMatch -> false, //
            rowKeysMatch -> (Boolean)rowKeysMatch, //
            rowKeysMatch -> (Boolean)rowKeysMatch, //
            rowKeysMatch -> (Boolean)rowKeysMatch, //
            rowKeysMatch -> false//
        );

        for (int i = 0; i < retainMatchesLeftRight.length; i++) {
            boolean retainMatches = retainMatchesLeftRight[i][0];
            boolean retainLeft = retainMatchesLeftRight[i][1];
            boolean retainTrue = retainMatchesLeftRight[i][2];

            /**
             * Split table output
             */
            // row keys match: join on row keys
            {
                var rowKeysMatch = true;
                var splitOutput = true;
                boolean isApplicable = !retainMatches || rowKeysMatch;

                var leftSettings = new JoinTableSettings(retainLeft, onRowKey, new String[0], InputTable.LEFT, left);
                var rightSettings = new JoinTableSettings(retainTrue, onRowKey, new String[0], InputTable.RIGHT, right);
                var joinSpec = new JoinSpecification.Builder(leftSettings, rightSettings)//
                    .retainMatched(retainMatches)//
                    .conjunctive(true)//
                    .build();

                Optional<String> result = KeepRowKeysFactory.applicable(joinSpec, splitOutput);
                assertTrue(isApplicable ? result.isEmpty() : result.isPresent());

                JoinImplementation impl = JoinAlgorithm.AUTO.getFactory().create(joinSpec, JoinTestInput.EXEC);
                impl.joinOutputSplit().getResults();
            }

            // row keys don't match: join disjunctive on row keys and also something else
            {
                var rowKeysMatch = false;
                var splitOutput = true;
                boolean isApplicable = !retainMatches || rowKeysMatch;

                var leftSettings = new JoinTableSettings(retainLeft, //
                    new JoinColumn[]{onRowKey[0], new JoinColumn("A")}, //
                    new String[0], InputTable.LEFT, left);
                var rightSettings = new JoinTableSettings(retainTrue, //
                    new JoinColumn[]{onRowKey[0], new JoinColumn("X")}, //
                    new String[0], InputTable.RIGHT, right);
                var joinSpec = new JoinSpecification.Builder(leftSettings, rightSettings)//
                    .retainMatched(retainMatches)//
                    .conjunctive(false)//
                    .build();

                Optional<String> result = KeepRowKeysFactory.applicable(joinSpec, splitOutput);
                assertTrue(String.format("%s %s", Arrays.toString(retainMatchesLeftRight[i]), result.toString()), //
                    isApplicable ? result.isEmpty() : result.isPresent());

                JoinImplementation impl = JoinAlgorithm.AUTO.getFactory().create(joinSpec, JoinTestInput.EXEC);
                impl.joinOutputSplit().getResults();
            }

            /**
             * Single table output
             */
            // row keys match: join on row keys
            {
                var rowKeysMatch = true;
                var splitOutput = false;
                boolean isApplicable = applicable.get(i).test(rowKeysMatch);

                var leftSettings = new JoinTableSettings(retainLeft, onRowKey, new String[0], InputTable.LEFT, left);
                var rightSettings = new JoinTableSettings(retainTrue, onRowKey, new String[0], InputTable.RIGHT, right);
                var joinSpec = new JoinSpecification.Builder(leftSettings, rightSettings)//
                    .retainMatched(retainMatches)//
                    .conjunctive(true)//
                    .build();

                Optional<String> result = KeepRowKeysFactory.applicable(joinSpec, splitOutput);
                assertTrue(isApplicable ? result.isEmpty() : result.isPresent());

                JoinImplementation impl = JoinAlgorithm.AUTO.getFactory().create(joinSpec, JoinTestInput.EXEC);
                impl.joinOutputCombined().getResults();
            }

            // row keys don't match: join disjunctive on row keys and also something else
            {
                var rowKeysMatch = false;
                var splitOutput = false;
                boolean isApplicable = applicable.get(i).test(rowKeysMatch);

                var leftSettings = new JoinTableSettings(retainLeft, //
                    new JoinColumn[]{onRowKey[0], new JoinColumn("A")}, //
                    new String[0], InputTable.LEFT, left);
                var rightSettings = new JoinTableSettings(retainTrue, //
                    new JoinColumn[]{onRowKey[0], new JoinColumn("X")}, //
                    new String[0], InputTable.RIGHT, right);
                var joinSpec = new JoinSpecification.Builder(leftSettings, rightSettings)//
                    .retainMatched(retainMatches)//
                    .conjunctive(false)//
                    .build();

                Optional<String> result = KeepRowKeysFactory.applicable(joinSpec, splitOutput);
                assertTrue(isApplicable ? result.isEmpty() : result.isPresent());

                JoinImplementation impl = JoinAlgorithm.AUTO.getFactory().create(joinSpec, JoinTestInput.EXEC);
                impl.joinOutputCombined().getResults();
            }
        }

    }

    /**
     * Test that a specification rejects illegal {@link JoinTableSettings}.
     */
    @Test
    public void testIllegalArguments() {
        assertThrows(InvalidSettingsException.class, () ->
        // must not pass left settings as right settings.
        new JoinSpecification.Builder(m_settings[RIGHT], m_settings[LEFT]).build());

        final DataTableSpec abc = new DataTableSpec(col("A"), col("B"), col("C"));

        assertThrows(InvalidSettingsException.class, () ->
        // can't reference non-existing columns in join or include columns
        new JoinTableSettings(false, JoinColumn.array("non-existing join column"), new String[]{"A", "B", "C"},
            InputTable.LEFT, abc));

        assertThrows(InvalidSettingsException.class, () ->
        // can't reference non-existing columns in join or include columns
        new JoinTableSettings(false, JoinColumn.array("A"), new String[]{"non-existing include column"},
            InputTable.LEFT, abc));

        assertThrows(InvalidSettingsException.class, () -> {
            // can't declare unequal number of join columns in both settings
            JoinTableSettings left =
                new JoinTableSettings(false, JoinColumn.array("A"), new String[]{"A"}, InputTable.LEFT, abc);
            JoinTableSettings right =
                new JoinTableSettings(false, JoinColumn.array("A", "B"), new String[]{"B", "C"}, InputTable.RIGHT, abc);
            new JoinSpecification.Builder(left, right).build();
        });

        assertThrows(InvalidSettingsException.class, () ->
        // declare at least one join column.
        new JoinTableSettings(false, new JoinColumn[0], new String[]{"A"}, InputTable.LEFT, abc));

    }

}
