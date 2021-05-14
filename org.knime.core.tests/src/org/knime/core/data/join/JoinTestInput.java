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
 *   Jun 12, 2020 (carlwitt): created
 */
package org.knime.core.data.join;

import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.data.join.JoinTest.JoinMode;
import org.knime.core.data.join.implementation.OrderedRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 * Provides two tables to join, a partial join specification (which columns to compare and to include in the output) and
 * the expected results for different join modes (inner join, outer join, anti join, etc.)
 */
@SuppressWarnings({"unchecked", "rawtypes", "javadoc"})
public abstract class JoinTestInput {

    private static final NodeProgressMonitor PROGRESS;

    public static final ExecutionContext EXEC;

    static {
        PROGRESS = new DefaultNodeProgressMonitor();

        EXEC = new ExecutionContext(PROGRESS,
            new Node((NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0])),
            SingleNodeContainer.MemoryPolicy.CacheSmallInMemory, NotInWorkflowDataRepository.newInstance());
    }

    protected JoinColumn[] m_leftJoinColumns;

    protected JoinColumn[] m_rightJoinColumns;

    protected String[] m_leftIncludeColumns;

    protected String[] m_rightIncludeColumns;

    protected JoinTestInput() {
        configureJoin();
    }

    /**
     * Hook to initialize members in subclasses {@link #m_leftJoinColumns}, {@link #m_leftIncludeColumns}, etc.
     */
    abstract void configureJoin();

    /**
     * @return the left input table to the join operation
     */
    abstract BufferedDataTable left();

    /**
     * @return the right input table to the join operation
     */
    abstract BufferedDataTable right();

    /**
     * @return the expected rows in the joined table in the specified order.
     */
    public abstract DataRow[] ordered(final JoinMode mode, final OutputRowOrder order);

    /**
     * @param order the order in which to return the rows
     * @return the unmatched rows from the left table
     */
    public abstract DataRow[] leftOuter(OutputRowOrder order);

    /**
     * @param order the order in which to return the rows
     * @return the unmatched rows from the right table
     */
    public abstract DataRow[] rightOuter(OutputRowOrder order);

    public JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder) throws InvalidSettingsException {

        JoinTableSettings leftSettings = new JoinTableSettings(joinMode.m_retainLeftUnmatched, m_leftJoinColumns,
            m_leftIncludeColumns, InputTable.LEFT, left());
        JoinTableSettings rightSettings = new JoinTableSettings(joinMode.m_retainRightUnmatched, m_rightJoinColumns,
            m_rightIncludeColumns, InputTable.RIGHT, right());

        return new JoinSpecification.Builder(leftSettings, rightSettings)
            .columnNameDisambiguator(name -> name.concat("*"))
            // only default, is overridden in, e.g., consensusMatchAny
            .mergeJoinColumns(false)
            // only default, is overridden in disjunctive test inputs
            .conjunctive(true)
            .outputRowOrder(outputRowOrder).rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
            .retainMatched(joinMode.m_retainMatches).build();
    }

    /**
     * Produce both matches and unmatched rows in both input tables. Include one non-join column from each table.
     *
     * Left input table:
     * <table>
     * <thead>
     * <tr>
     * <th>RowID</th>
     * <th>Join Column</th>
     * <th>Nonjoin1</th>
     * <th>Nonjoin2</th>
     * </tr>
     * </thead><tbody>
     * <tr>
     * <td>A</td>
     * <td>A</td>
     * <td>1</td>
     * <td>2</td>
     * </tr>
     * <tr>
     * <td>B</td>
     * <td>B</td>
     * <td>3</td>
     * <td>4</td>
     * </tr>
     * <tr>
     * <td>X</td>
     * <td>X</td>
     * <td>100</td>
     * <td>101</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * Right input table:
     * <table>
     * <thead>
     * <tr>
     * <th>RowID</th>
     * <th>Join Column</th>
     * <th>Nonjoin1</th>
     * <th>Nonjoin2</th>
     * </tr>
     * </thead><tbody>
     * <tr>
     * <td>X</td>
     * <td>X</td>
     * <td>102</td>
     * <td>103</td>
     * </tr>
     * <tr>
     * <td>B</td>
     * <td>B</td>
     * <td>7</td>
     * <td>6</td>
     * </tr>
     * <tr>
     * <td>C</td>
     * <td>?</td>
     * <td>7</td>
     * <td>8</td>
     * </tr>
     * <tr>
     * <td>D</td>
     * <td>D</td>
     * <td>9</td>
     * <td>10</td>
     * </tr>
     * </tbody>
     * </table>
     *
     */
    abstract static class SmallInput extends JoinTestInput {

        // left table
        DataRow leftA = defaultRow("A,A,1,2"), // left outer
                leftB = defaultRow("B,B,3,4"), // inner
                leftX = defaultRow("X,X,100,101"); // inner

        // right table
        DataRow rightX = defaultRow("X,X,102,103"), // inner
                rightB = defaultRow("B,B,5,6"), // inner
                rightC = defaultRow("C,?,7,8"), // right outer
                rightD = defaultRow("D,D,9,10"); // right outer

        DataRow leftAProjected = defaultRow("A,1"), leftBProjected = defaultRow("B,3"),
                leftXProjected = defaultRow("X,100");

        DataRow rightXProjected = defaultRow("X,103"), rightBProjected = defaultRow("B,6"),
                rightCProjected = defaultRow("C,8"), rightDProjected = defaultRow("D,10");

        @Override
        BufferedDataTable left() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", leftA, leftB, leftX);
        }

        @Override
        BufferedDataTable right() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", rightX, rightB, rightC, rightD);
        }

        // results
        DataRow innerX = defaultRow("X+X,100,103");

        DataRow innerB = defaultRow("B+B,3,6");

        DataRow leftOuterA = defaultRow("A+?,1,?");

        DataRow leftOuterB = defaultRow("B+?,3,?");

        DataRow leftOuterX = defaultRow("X+?,100,?");

        DataRow rightOuterX = defaultRow("?+X,?,103");

        DataRow rightOuterB = defaultRow("?+B,?,6");

        DataRow rightOuterC = defaultRow("?+C,?,8");

        DataRow rightOuterD = defaultRow("?+D,?,10");

        @Override
        void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = JoinColumn.array("Join Column");
            m_rightJoinColumns = JoinColumn.array("Join Column");
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                    switch (mode) {
                        case INNER:
                            return new DataRow[]{innerB, innerX};
                        case LEFT_OUTER:
                            return new DataRow[]{innerX, leftOuterA, innerB};
                        case RIGHT_OUTER:
                            return new DataRow[]{innerX, innerB, rightOuterC, rightOuterD};
                        case FULL_OUTER:
                            return new DataRow[]{innerX, rightOuterC, leftOuterA, innerB, rightOuterD};
                        case LEFT_ANTI:
                            return new DataRow[]{leftOuterA};
                        case RIGHT_ANTI:
                            return new DataRow[]{rightOuterD, rightOuterC};
                        case FULL_ANTI:
                            return new DataRow[]{rightOuterD, rightOuterC, leftOuterA};
                        case EMPTY:
                            return new DataRow[]{};
                        default:
                            throw new IllegalStateException();
                    }
                case DETERMINISTIC:
                    switch (mode) {
                        // innerX comes first because the right table dictates major row order (because it is larger
                        // and thus used as probe table)
                        case INNER:
                            return new DataRow[]{innerX, innerB};
                        case LEFT_OUTER:
                            return new DataRow[]{innerX, innerB, leftOuterA};
                        case RIGHT_OUTER:
                            return new DataRow[]{innerX, innerB, rightOuterC, rightOuterD};
                        case FULL_OUTER:
                            return new DataRow[]{innerX, innerB, leftOuterA, rightOuterC, rightOuterD};
                        case LEFT_ANTI:
                            return new DataRow[]{leftOuterA};
                        case RIGHT_ANTI:
                            return new DataRow[]{rightOuterC, rightOuterD};
                        case FULL_ANTI:
                            return new DataRow[]{leftOuterA, rightOuterC, rightOuterD};
                        case EMPTY:
                            return new DataRow[]{};
                        default:
                            throw new IllegalStateException();
                    }
                case LEFT_RIGHT:
                    switch (mode) {
                        // innerB comes first because the left table dictates the major row order
                        case INNER:
                            return new DataRow[]{innerB, innerX};
                        case LEFT_OUTER:
                            return new DataRow[]{innerB, innerX, leftOuterA};
                        case RIGHT_OUTER:
                            return new DataRow[]{innerB, innerX, rightOuterC, rightOuterD};
                        case FULL_OUTER:
                            return new DataRow[]{innerB, innerX, leftOuterA, rightOuterC, rightOuterD};
                        case LEFT_ANTI:
                            return new DataRow[]{leftOuterA};
                        case RIGHT_ANTI:
                            return new DataRow[]{rightOuterC, rightOuterD};
                        case FULL_ANTI:
                            return new DataRow[]{leftOuterA, rightOuterC, rightOuterD};
                        case EMPTY:
                            return new DataRow[]{};
                        default:
                            throw new IllegalStateException();
                    }
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public DataRow[] leftOuter(final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                case DETERMINISTIC:
                case LEFT_RIGHT:
                    return new DataRow[]{leftAProjected};
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public DataRow[] rightOuter(final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                    return new DataRow[]{rightDProjected, rightCProjected};
                case DETERMINISTIC:
                case LEFT_RIGHT:
                    return new DataRow[]{rightCProjected, rightDProjected};
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public String toString() {
            return "SmallInput";
        }

    }

    /**
     * Test two small input tables that produce innner, left outer, and right outer results.
     */
    public static final JoinTestInput allResultTypes = new SmallInput() {
        @Override
        void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = JoinColumn.array("Join Column");
            m_rightJoinColumns = JoinColumn.array("Join Column");
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public String toString() {
            return "allResultTypes";
        }
    };

    /**
     * Test self join.
     */
    public static final JoinTestInput selfJoin = new SmallInput() {
        BufferedDataTable table = JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", leftA, leftB, leftX);

        @Override
        void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = JoinColumn.array("Join Column");
            m_rightJoinColumns = JoinColumn.array("Join Column");
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        BufferedDataTable left() {
            return table;
        }

        @Override
        BufferedDataTable right() {
            return table;
        }

        @Override
        public DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {
            switch (mode) {
                case INNER:
                case LEFT_OUTER:
                case RIGHT_OUTER:
                case FULL_OUTER:
                    return new DataRow[]{JoinTestInput.defaultRow("A+A,1,2"), JoinTestInput.defaultRow("B+B,3,4"),
                        JoinTestInput.defaultRow("X+X,100,101")};
                case EMPTY:
                case LEFT_ANTI:
                case RIGHT_ANTI:
                case FULL_ANTI:
                    return new DataRow[0];
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public DataRow[] leftOuter(final OutputRowOrder order) {
            return new DataRow[0];
        }

        @Override
        public DataRow[] rightOuter(final OutputRowOrder order) {
            return new DataRow[0];
        }

        @Override
        public String toString() {
            return "self join";
        }

    };

    /**
     * Produces the same output as {@link SmallInput}, but by comparing the row key to "Join Column"
     */
    public static final JoinTestInput mixRowKeyWithNormalColumn = new SmallInput() {
        @Override
        void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY);
            m_rightJoinColumns = JoinColumn.array("Join Column");
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public String toString() {
            return "mixRowKeyWithNormalColumn";
        }
    };

    /**
     * Produces the same output as {@link SmallInput} but by comparing the row keys instead of the join columns.
     */
    public static final JoinTestInput joinOnRowKeys = new SmallInput() {
        @Override
        void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY);
            m_rightJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY);
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public String toString() {
            return "joinOnRowKeys";
        }
    };

    /**
     * Test that duplicate join clauses (e.g., A=A && A=A) work as expected.
     */
    public static final JoinTestInput joinOnRedundantColumns = new SmallInput() {
        @Override
        void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY, "Join Column",
                "Join Column", JoinTableSettings.SpecialJoinColumn.ROW_KEY);
            m_rightJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY, "Join Column",
                "Join Column", JoinTableSettings.SpecialJoinColumn.ROW_KEY);
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public String toString() {
            return "joinOnRedundantColumns";
        }
    };

    /**
     * Test match any with a single clause - gives same result as conjunctive with single clause.
     */
    public static final JoinTestInput singleColumnMatchAny = new SmallInput() {
        @Override
        void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY);
            m_rightJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY);
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder) throws InvalidSettingsException {

            JoinTableSettings leftSettings = new JoinTableSettings(joinMode.m_retainLeftUnmatched, m_leftJoinColumns,
                m_leftIncludeColumns, InputTable.LEFT, left());
            JoinTableSettings rightSettings = new JoinTableSettings(joinMode.m_retainRightUnmatched, m_rightJoinColumns,
                m_rightIncludeColumns, InputTable.RIGHT, right());

            return new JoinSpecification.Builder(leftSettings, rightSettings)
                .columnNameDisambiguator(name -> name.concat("*")).mergeJoinColumns(false)
                // match any
                .conjunctive(false).outputRowOrder(outputRowOrder)
                .rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
                .retainMatched(joinMode.m_retainMatches).build();
        }

        @Override
        public String toString() {
            return "singleColumnMatchAny";
        }

    };

    /**
     * Test match any with a clause that doesn't change results (because it produces no matches).
     */
    public static final JoinTestInput nonAdditionalMatchAny = new SmallInput() {
        @Override
        void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY, "Join Column");
            m_rightJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY, "Nonjoin1");
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder) throws InvalidSettingsException {

            JoinTableSettings leftSettings = new JoinTableSettings(joinMode.m_retainLeftUnmatched, m_leftJoinColumns,
                m_leftIncludeColumns, InputTable.LEFT, left());
            JoinTableSettings rightSettings = new JoinTableSettings(joinMode.m_retainRightUnmatched, m_rightJoinColumns,
                m_rightIncludeColumns, InputTable.RIGHT, right());

            return new JoinSpecification.Builder(leftSettings, rightSettings)
                .columnNameDisambiguator(name -> name.concat("*")).mergeJoinColumns(false)
                // match any
                .conjunctive(false).outputRowOrder(outputRowOrder)
                .rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
                .retainMatched(joinMode.m_retainMatches).build();
        }

        @Override
        public String toString() {
            return "nonAdditionalMatchAny";
        }

    };

    /**
     * Test match any with a duplicated join clause - joining on RowID = RowID OR RowID = RowID. This must not change
     * the results.
     */
    public static final JoinTestInput redundantMatchAny = new SmallInput() {
        @Override
        void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY,
                JoinTableSettings.SpecialJoinColumn.ROW_KEY);
            m_rightJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY,
                JoinTableSettings.SpecialJoinColumn.ROW_KEY);
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder) throws InvalidSettingsException {

            JoinTableSettings leftSettings = new JoinTableSettings(joinMode.m_retainLeftUnmatched, m_leftJoinColumns,
                m_leftIncludeColumns, InputTable.LEFT, left());
            JoinTableSettings rightSettings = new JoinTableSettings(joinMode.m_retainRightUnmatched, m_rightJoinColumns,
                m_rightIncludeColumns, InputTable.RIGHT, right());

            return new JoinSpecification.Builder(leftSettings, rightSettings)
                .columnNameDisambiguator(name -> name.concat("*")).mergeJoinColumns(false)
                // match any
                .conjunctive(false).outputRowOrder(outputRowOrder)
                .rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
                .retainMatched(joinMode.m_retainMatches).build();
        }

        @Override
        public String toString() {
            return "redundantMatchAny";
        }

    };

    /**
     * Test match any in combination with merge join columns.
     */
    public static final JoinTestInput mergeJoinColumnsMatchAny = new SmallInput() {
        @Override
        void configureJoin() {
            // this configuration produces the default results
            m_leftJoinColumns = JoinColumn.array("Join Column", "Join Column");
            m_rightJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY, "Join Column");
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder) throws InvalidSettingsException {

            JoinTableSettings leftSettings =
                new JoinTableSettings(joinMode.m_retainLeftUnmatched, m_leftJoinColumns,
                m_leftIncludeColumns, InputTable.LEFT, left());
            JoinTableSettings rightSettings = new JoinTableSettings(joinMode.m_retainRightUnmatched, m_rightJoinColumns,
                m_rightIncludeColumns, InputTable.RIGHT, right());

            return new JoinSpecification.Builder(leftSettings, rightSettings)
                .columnNameDisambiguator(name -> name.concat("*"))
                // merge join columns
                .mergeJoinColumns(true)
                // match any
                .conjunctive(false).outputRowOrder(outputRowOrder)
                .rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
                .retainMatched(joinMode.m_retainMatches).build();
        }

        @Override
        public String toString() {
            return "mergeJoinColumnsMatchAny";
        }

    };

    /**
     * Test the contents of cells that belong to a column that results from three merged join columns.
     */
    public static final JoinTestInput consensusMatchAny = new JoinTestInput() {

        @Override
        BufferedDataTable left() {
            return JoinTestInput.table("A,B",
                // matches Row1 (1, 10) output (?, 999) 1=1≠10
                // matches Row2 (10, 1) output (?, 999) 1≠10≠1
                // matches Row3 (1, ?)  output (?, 999) 1=1≠?
                // matches Row4 (?, 1)  output (?, 999) 1≠?≠1
                // matches Row5 (1, 1)  output (1, 999) 1=1=1
                "Row1,1,999",
                // doesn't match anything: output left unmatched (?, 999)
                "Row2,?,999",
                // doesn't match anything: output left unmatched (-1, 999)
                "Row3,-1,999"
                );
        }

        @Override
        BufferedDataTable right() {
            return JoinTestInput.table("X,Y",
                        "Row1,1,10",
                        "Row2,10,1",
                        "Row3,1,?",
                        "Row4,?,1",
                        "Row5,1,1",
                        // unmatched; strictly spoken, although X=Y the output row should be (?, ?) since A=? and thus
                        // A≠X=Y, however, it seems more convenient to define the merge join columns such that as much
                        // information as possible is preserved
                        "Row6,200,200",
                        // unmatched, output should be (?, ?) since X≠Y and B=? for a right outer match
                        "Row7,200,300"
                        );
        }

        @Override
        void configureJoin() {
            m_leftJoinColumns = JoinColumn.array("A", "A");
            m_rightJoinColumns = JoinColumn.array("X", "Y");
            m_leftIncludeColumns = new String[]{"A", "B"};
            m_rightIncludeColumns = new String[]{"X", "Y"};
        }

        @Override
        public JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder)
            throws InvalidSettingsException {
            JoinTableSettings leftSettings = new JoinTableSettings(joinMode.m_retainLeftUnmatched, m_leftJoinColumns,
                m_leftIncludeColumns, InputTable.LEFT, left());
            JoinTableSettings rightSettings = new JoinTableSettings(joinMode.m_retainRightUnmatched, m_rightJoinColumns,
                m_rightIncludeColumns, InputTable.RIGHT, right());

            return new JoinSpecification.Builder(leftSettings, rightSettings)
                .columnNameDisambiguator(name -> name.concat("*"))
                .mergeJoinColumns(true)
                // match any
                .conjunctive(false).outputRowOrder(outputRowOrder)
                .rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
                .retainMatched(joinMode.m_retainMatches).build();
        }

        @Override
        public DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {

            switch (mode) {
                case INNER:
                    return new DataRow[] {
                        defaultRow("Row1+Row1,?,999"),
                        defaultRow("Row1+Row2,?,999"),
                        defaultRow("Row1+Row3,?,999"),
                        defaultRow("Row1+Row4,?,999"),
                        defaultRow("Row1+Row5,1,999")
                    };
                case LEFT_OUTER:
                    return new DataRow[] {
                        defaultRow("Row1+Row1,?,999"),
                        defaultRow("Row1+Row2,?,999"),
                        defaultRow("Row1+Row3,?,999"),
                        defaultRow("Row1+Row4,?,999"),
                        defaultRow("Row1+Row5,1,999"),
                        defaultRow("Row2+?,?,999"),
                        defaultRow("Row3+?,-1,999"),
                    };
                case RIGHT_OUTER:
                    return new DataRow[] {
                        defaultRow("Row1+Row1,?,999"),
                        defaultRow("Row1+Row2,?,999"),
                        defaultRow("Row1+Row3,?,999"),
                        defaultRow("Row1+Row4,?,999"),
                        defaultRow("Row1+Row5,1,999"),
                        defaultRow("?+Row6,200,?"),
                        defaultRow("?+Row7,?,?"),
                    };
                case FULL_OUTER:
                    return new DataRow[] {
                        defaultRow("Row1+Row1,?,999"),
                        defaultRow("Row1+Row2,?,999"),
                        defaultRow("Row1+Row3,?,999"),
                        defaultRow("Row1+Row4,?,999"),
                        defaultRow("Row1+Row5,1,999"),
                        defaultRow("Row2+?,?,999"),
                        defaultRow("Row3+?,-1,999"),
                        defaultRow("?+Row6,200,?"),
                        defaultRow("?+Row7,?,?"),
                    };
                case LEFT_ANTI:
                    return new DataRow[]{
                        defaultRow("Row2+?,?,999"),
                        defaultRow("Row3+?,-1,999"),};
                case RIGHT_ANTI:
                    return new DataRow[] {
                        defaultRow("?+Row6,200,?"),
                        defaultRow("?+Row7,?,?"),
                    };
                case FULL_ANTI:
                    return new DataRow[] {
                        defaultRow("Row2+?,?,999"),
                        defaultRow("Row3+?,-1,999"),
                        defaultRow("?+Row6,200,?"),
                        defaultRow("?+Row7,?,?"),
                    };
                case EMPTY:
                    return new DataRow[]{};
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public String toString() {
            return "consensus with match any test";
        }

        @Override
        public DataRow[] leftOuter(final OutputRowOrder order) {
            return new DataRow[]{
                defaultRow("Row2,?,999"),
                defaultRow("Row3,-1,999")};
        }

        @Override
        public DataRow[] rightOuter(final OutputRowOrder order) {
            // output spec is X, Y
            return new DataRow[]{
                defaultRow("Row6,200,200"),
                defaultRow("Row7,200,300")};
        }
    };

    /**
     * Self join a table with match any and test that the various duplicate matches are filtered out as well as various
     * false positive unmatched rows.
     *
     * Consider for instance row (B, B, B) which matches with itself under all three join clauses.
     *
     * Consider for instance joining on column 1=2. This leaves rows (D, C, C) and (E, C, C) unmatched. However, these
     * rows match under join clause 2=3, such that their unmatched status needs to be canceled throughout the join
     * process.
     *
     * <table>
     * <thead>
     * <tr>
     * <th>1</th>
     * <th>2</th>
     * <th>3</th>
     * </tr>
     * </thead><tbody>
     * <tr>
     * <td>A</td>
     * <td>A</td>
     * <td>B</td>
     * </tr>
     * <tr>
     * <td>B</td>
     * <td>B</td>
     * <td>B</td>
     * </tr>
     * <tr>
     * <td>C</td>
     * <td>B</td>
     * <td>B</td>
     * </tr>
     * <tr>
     * <td>D</td>
     * <td>C</td>
     * <td>C</td>
     * </tr>
     * <tr>
     * <td>E</td>
     * <td>C</td>
     * <td>C</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * Expected output of joining T on T with 1=2 OR 1=3 OR 2=3: (0,0), (1,0), (1,1), (1,2), (2,0), (2,1), (2,2), (2,3),
     * (2,4), (3,3), (3,4), (4,3), (4,4)
     *
     */
    public static final JoinTestInput selfJoinMatchAny = new JoinTestInput() {

        BufferedDataTable m_table =
            JoinTestInput.table("1,2,3", "0,A,A,B", "1,B,B,B", "2,C,B,B", "3,D,C,C", "4,E,C,C");

        @Override
        void configureJoin() {
            m_leftJoinColumns = JoinColumn.array("1","1","2");
            m_rightJoinColumns = JoinColumn.array("2","3","3");
            m_leftIncludeColumns = new String[]{};
            m_rightIncludeColumns = new String[]{};
        }

        @Override
        BufferedDataTable left() {
            return m_table;
        }

        @Override
        BufferedDataTable right() {
            return m_table;
        }

        @Override
        public JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder) throws InvalidSettingsException {
            JoinTableSettings leftSettings = new JoinTableSettings(joinMode.m_retainLeftUnmatched, m_leftJoinColumns,
                m_leftIncludeColumns, InputTable.LEFT, left());
            JoinTableSettings rightSettings = new JoinTableSettings(joinMode.m_retainRightUnmatched, m_rightJoinColumns,
                m_rightIncludeColumns, InputTable.RIGHT, right());

            return new JoinSpecification.Builder(leftSettings, rightSettings)
                .columnNameDisambiguator(name -> name.concat("*")).mergeJoinColumns(false)
                // match any
                .conjunctive(false).outputRowOrder(outputRowOrder)
                .rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
                .retainMatched(joinMode.m_retainMatches).build();
        }

        @Override
        public DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {

            switch (mode) {
                case INNER:
                case LEFT_OUTER:
                case RIGHT_OUTER:
                case FULL_OUTER:
                    return new DataRow[] {
                        defaultRow("0+0"),
                        defaultRow("1+0"),
                        defaultRow("1+1"),
                        defaultRow("1+2"),
                        defaultRow("2+0"),
                        defaultRow("2+1"),
                        defaultRow("2+2"),
                        defaultRow("2+3"),
                        defaultRow("2+4"),
                        defaultRow("3+3"),
                        defaultRow("3+4"),
                        defaultRow("4+3"),
                        defaultRow("4+4")
                    };
                case LEFT_ANTI:
                case RIGHT_ANTI:
                case FULL_ANTI:
                case EMPTY:
                    return new DataRow[]{};
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public String toString() {
            return "self join (no outer in match any)";
        }

        @Override
        public DataRow[] leftOuter(final OutputRowOrder order) {
            return new DataRow[0];
        }

        @Override
        public DataRow[] rightOuter(final OutputRowOrder order) {
            return new DataRow[0];
        }

    };

    /**
     * Test that joining on row keys and mixing a special column with a normal column does not throw any errors. Also
     * covers the case where no matches are produced at all.
     */
    public static final JoinTestInput emptyJoinOnRowKeys = new SmallInput() {
        @Override
        void configureJoin() {
            m_leftJoinColumns = JoinColumn.array(JoinTableSettings.SpecialJoinColumn.ROW_KEY,
                JoinTableSettings.SpecialJoinColumn.ROW_KEY);
            m_rightJoinColumns = JoinColumn.array("Nonjoin1", "Nonjoin2");
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                case DETERMINISTIC:
                case LEFT_RIGHT:
                    switch (mode) {
                        case EMPTY:
                        case INNER:
                            return new DataRow[0];
                        case LEFT_ANTI:
                        case LEFT_OUTER:
                            return new DataRow[]{leftOuterA, leftOuterB, leftOuterX};
                        case RIGHT_ANTI:
                        case RIGHT_OUTER:
                            return new DataRow[]{rightOuterX, rightOuterB, rightOuterC, rightOuterD};
                        case FULL_ANTI:
                        case FULL_OUTER:
                            return new DataRow[]{leftOuterA, leftOuterB, leftOuterX, rightOuterX, rightOuterB,
                                rightOuterC, rightOuterD};
                        default:
                            throw new IllegalStateException();
                    }
                default:
                    throw new IllegalStateException();
            }

        }

        /** empty join -> all left rows are unmatched */
        @Override
        public DataRow[] leftOuter(final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                    return new DataRow[]{leftAProjected, leftXProjected, leftBProjected};
                case DETERMINISTIC:
                case LEFT_RIGHT:
                    return new DataRow[]{leftAProjected, leftBProjected, leftXProjected};
                default:
                    throw new IllegalStateException();
            }
        }

        /** empty join -> all right rows are unmatched */
        @Override
        public DataRow[] rightOuter(final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                    return new DataRow[]{rightDProjected, rightXProjected, rightCProjected, rightBProjected};
                case DETERMINISTIC:
                case LEFT_RIGHT:
                    return new DataRow[]{rightXProjected, rightBProjected, rightCProjected, rightDProjected};
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public String toString() {
            return "emptyJoinOnRowKeys";
        }

    };

    /**
     * Test the case where one table contains no rows at all.
     */
    public static final JoinTestInput emptyHashTable = new SmallInput() {
        @Override
        void configureJoin() {
            m_leftJoinColumns = JoinColumn.array("Join Column");
            m_rightJoinColumns = JoinColumn.array("Join Column");
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        BufferedDataTable left() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", new DataRow[0]);
        }

        @Override
        BufferedDataTable right() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", rightX, rightB, rightC, rightD);
        }

        @Override
        public DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                case DETERMINISTIC:
                case LEFT_RIGHT:
                    switch (mode) {
                        case EMPTY:
                        case LEFT_ANTI:
                        case LEFT_OUTER:
                        case INNER:
                            return new DataRow[0];
                        case RIGHT_ANTI:
                        case RIGHT_OUTER:
                        case FULL_ANTI:
                        case FULL_OUTER:
                            return new DataRow[]{rightOuterX, rightOuterB, rightOuterC, rightOuterD};
                        default:
                            throw new IllegalStateException();
                    }
                default:
                    throw new IllegalStateException();
            }

        }

        /** empty join -> all left rows are unmatched */
        @Override
        public DataRow[] leftOuter(final OutputRowOrder order) {
            return new DataRow[0];
        }

        /** empty join -> all right rows are unmatched */
        @Override
        public DataRow[] rightOuter(final OutputRowOrder order) {
            switch (order) {
                case ARBITRARY:
                    return new DataRow[]{rightDProjected, rightXProjected, rightCProjected, rightBProjected};
                case DETERMINISTIC:
                case LEFT_RIGHT:
                    return new DataRow[]{rightXProjected, rightBProjected, rightCProjected, rightDProjected};
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public String toString() {
            return "empty hash table";
        }

    };

    /**
     * Just a single inner join result row.
     */
    public static final JoinTestInput singleInnerJoin = new JoinTestInput() {

        @Override
        BufferedDataTable left() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", "Left B,B,3,4");
        }

        @Override
        BufferedDataTable right() {
            return JoinTestInput.table("Join Column,Nonjoin1,Nonjoin2", "Right B,B,5,6");
        }

        @Override
        void configureJoin() {
            m_leftJoinColumns = JoinColumn.array("Join Column");
            m_rightJoinColumns = JoinColumn.array("Join Column");
            m_leftIncludeColumns = new String[]{"Nonjoin1"};
            m_rightIncludeColumns = new String[]{"Nonjoin2"};
        }

        @Override
        public DataRow[] ordered(final JoinMode mode, final OutputRowOrder order) {

            DataRow innerA = defaultRow("Left B+Right B,3,6");

            switch (mode) {
                case INNER:
                case LEFT_OUTER:
                case RIGHT_OUTER:
                case FULL_OUTER:
                    return new DataRow[]{innerA};
                case LEFT_ANTI:
                case RIGHT_ANTI:
                case FULL_ANTI:
                case EMPTY:
                    return new DataRow[]{};
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public String toString() {
            return "single inner join";
        }

        @Override
        public DataRow[] leftOuter(final OutputRowOrder order) {
            return new DataRow[0];
        }

        @Override
        public DataRow[] rightOuter(final OutputRowOrder order) {
            return new DataRow[0];
        }

    };

    public static final JoinTestInput[] CONJUNCTIVE =
        new JoinTestInput[]{allResultTypes, selfJoin, mixRowKeyWithNormalColumn, joinOnRowKeys, joinOnRedundantColumns,
            emptyJoinOnRowKeys, emptyHashTable, singleInnerJoin};

    public static final JoinTestInput[] DISJUNCTIVE =
        new JoinTestInput[]{nonAdditionalMatchAny, singleColumnMatchAny, redundantMatchAny, selfJoinMatchAny,
            mergeJoinColumnsMatchAny, consensusMatchAny};

    /**
     * Helper to create tables in concise notation.
     *
     * @param columnNames comma-separated header, e.g., "Column1,ColumnB,ColumnZ"
     * @param rows each string being input to {@link #defaultRow(String)}
     * @return a table with the given rows
     */
    public static BufferedDataTable table(final String columnNames, final String... rows) {
        return table(columnNames, Arrays.stream(rows).map(JoinTestInput::defaultRow).toArray(DataRow[]::new));
    }

    /**
     * @param columnNames
     * @param rows
     * @return table with the given rows
     */
    public static BufferedDataTable table(final String columnNames, final DataRow... rows) {
        return table(columnNames, false, rows);
    }

    /**
     * Helper to create tables in concise notation.
     *
     * @param columnNames comma-separated header, e.g., "Column1,ColumnB,ColumnZ"
     * @param rows each string being input to {@link #defaultRow(String...)}
     */
    public static BufferedDataTable table(final String columnNames, final boolean storeRowOffsets,
        final DataRow... rows) {
        DataColumnSpec[] columns = Arrays.stream(columnNames.split(","))
            .map(name -> new DataColumnSpecCreator(name, StringCell.TYPE).createSpec()).toArray(DataColumnSpec[]::new);
        DataTableSpec spec = new DataTableSpec(columns);
        if (storeRowOffsets) {
            spec = OrderedRow.withOffset(spec);
        }

        BufferedDataContainer container = EXEC.createDataContainer(spec);
        Arrays.stream(rows).forEach(container::addRowToTable);
        container.close();
        return container.getTable();
    }

    /**
     * @param compactFormat comma-separated values, first is row key, rest denotes cell contents, e.g., "Row0,a,2,e"
     * @return data row constructed from string
     */
    public static DataRow defaultRow(final String compactFormat) {
        String[] keyAndValues = compactFormat.split(",");
        DataCell[] cells = Arrays.stream(keyAndValues).skip(1)
            .map(value -> "?".equals(value) ? DataType.getMissingCell() : new StringCell(value))
            .toArray(DataCell[]::new);
        return new DefaultRow(new RowKey(keyAndValues[0]), cells);
    }

    /**
     * @param compactFormat as in {@link #defaultRow(String)}
     * @param offset a number indicating the offset of the row in it's original table
     * @return @return data row constructed from string
     */
    public static DataRow defaultRow(final String compactFormat, final long offset) {
        DataRow defaultRow = defaultRow(compactFormat);
        return OrderedRow.withOffset(defaultRow, offset);
    }

    /**
     * @param row data row to display
     * @return readable representation of the object
     */
    public static String dataRowToString(final DataRow row) {
        StringBuilder buffer = new StringBuilder(row.getKey().toString());
        buffer.append(": (");
        for (int i = 0; i < row.getNumCells(); i++) {
            buffer.append(row.getCell(i).toString());
            // separate by ", "
            if (i != row.getNumCells() - 1) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

    /**
     * @param name name of the column
     * @return a data column specification for a string column
     */
    public static DataColumnSpec col(final String name) {
        DataColumnSpecCreator creator = new DataColumnSpecCreator(name, StringCell.TYPE);
        return creator.createSpec();
    }

    /**
     * @param content cell contents
     * @return a string cell of a row with the given content
     */
    public static DataCell cell(final String content) {
        return new StringCell(content);
    }
}