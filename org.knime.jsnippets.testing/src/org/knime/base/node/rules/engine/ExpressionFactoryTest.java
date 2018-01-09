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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.rules.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.knime.base.node.rules.engine.Rule.TableReference;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Tests {@link ExpressionFactory}.
 *
 * @author Gabor Bakos
 */
public class ExpressionFactoryTest {

    private ExpressionFactory m_factory = ExpressionFactory.getInstance();

    private Expression m_mockBoolExpression, m_mockBoolExpressionOther;

    private DataRow[] m_rows;

    /**
     * Sets up the mock objects.
     *
     * @throws java.lang.Exception Should not happen.
     */
    @Before
    public void setUp() throws Exception {
        final Map<String, Map<String, String>> emptyMap = Collections.emptyMap();
        m_mockBoolExpression = new Expression.Base() {

            @Override
            public List<DataType> getInputArgs() {
                return Collections.emptyList();
            }

            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                final DataCell cell;
                switch (Integer.parseInt(row.getKey().getString())) {
                    case 0:
                        cell = BooleanCell.TRUE;
                        break;
                    case 1:
                        cell = BooleanCell.FALSE;
                        break;
                    case 2:
                        cell = BooleanCell.TRUE;
                        break;
                    case 3:
                        cell = BooleanCell.FALSE;
                        break;
                    case 4:
                        cell = DataType.getMissingCell();
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                return new ExpressionValue(cell, emptyMap);
            }

            @Override
            public boolean isConstant() {
                return false;
            }

            @Override
            public ASTType getTreeType() {
                return ASTType.ColRef;
            }
        };
        m_mockBoolExpressionOther = new Expression.Base() {
            @Override
            public boolean isConstant() {
                return false;
            }

            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            @Override
            public List<DataType> getInputArgs() {
                return Collections.emptyList();
            }

            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                final DataCell cell;
                switch (Integer.parseInt(row.getKey().getString())) {
                    case 0:
                        cell = BooleanCell.TRUE;
                        break;
                    case 1:
                        cell = BooleanCell.TRUE;
                        break;
                    case 2:
                        cell = BooleanCell.FALSE;
                        break;
                    case 3:
                        cell = BooleanCell.FALSE;
                        break;
                    case 4:
                        cell = BooleanCell.TRUE;
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                return new ExpressionValue(cell, emptyMap);
            }

            @Override
            public ASTType getTreeType() {
                return ASTType.ColRef;
            }
        };
        final DefaultRow row = new DefaultRow(new RowKey("x"), new DataCell[0]);
        m_rows =
            new DataRow[]{new DefaultRow(new RowKey("0"), row), new DefaultRow(new RowKey("1"), row),
                new DefaultRow(new RowKey("2"), row), new DefaultRow(new RowKey("3"), row),
                new DefaultRow(new RowKey("4"), new DataCell[]{DataType.getMissingCell()})};
    }

    /**
     * Test method for {@link ExpressionFactory#not(Expression)} .
     */
    @Test
    public void testNot() {
        final Expression not = m_factory.not(m_mockBoolExpression);
        final ExpressionValue firstValue = not.evaluate(m_rows[0], null);
        final ExpressionValue secondValue = not.evaluate(m_rows[1], null);
        final ExpressionValue thirdValue = not.evaluate(m_rows[4], null);
        assertEquals(BooleanCell.FALSE, firstValue.getValue());
        assertEquals(BooleanCell.TRUE, secondValue.getValue());
        assertEquals(DataType.getMissingCell(), thirdValue.getValue());
    }

    /**
     * Test method for {@link ExpressionFactory#and(List)} .
     */
    @Test
    public void testAnd() {
        final Expression andSingle = m_factory.and(Arrays.asList(m_mockBoolExpression));
        final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
        //DataCell missing = DataType.getMissingCell();
        testForValues(andSingle, trueCell, falseCell, trueCell, falseCell/*, missing*/);

        final Expression andSame = m_factory.and(Arrays.asList(m_mockBoolExpression, m_mockBoolExpression));
        testForValues(andSame, trueCell, falseCell, trueCell, falseCell/*, missing*/);

        final Expression andMixed = m_factory.and(Arrays.asList(m_mockBoolExpression, m_mockBoolExpressionOther));
        testForValues(andMixed, trueCell, falseCell, falseCell, falseCell/*, missing*/);
    }

    private void testForValues(final Expression expression, final DataCell... expectedValues) {
        for (int i = 0; i < expectedValues.length; ++i) {
            final ExpressionValue returnValue = expression.evaluate(m_rows[i], null);
            assertEquals("Case: " + i, expectedValues[i], returnValue.getValue());
        }
    }

    /**
     * Test method for {@link ExpressionFactory#or(List)} .
     */
    @Test
    public void testOr() {
        final Expression orSingle = m_factory.or(Arrays.asList(m_mockBoolExpression));
        final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
        //DataCell missing = DataType.getMissingCell();
        testForValues(orSingle, trueCell, falseCell, trueCell, falseCell/*, missing*/);

        final Expression orSame = m_factory.or(Arrays.asList(m_mockBoolExpression, m_mockBoolExpression));
        testForValues(orSame, trueCell, falseCell, trueCell, falseCell/*, missing*/);

        final Expression orMixed = m_factory.or(Arrays.asList(m_mockBoolExpression, m_mockBoolExpressionOther));
        testForValues(orMixed, trueCell, trueCell, trueCell, falseCell/*, missing*/);
    }

    /**
     * Test method for {@link ExpressionFactory#xor(List)} .
     */
    @Test
    public void testXor() {
        final Expression xorSingle = m_factory.xor(Arrays.asList(m_mockBoolExpression));
        final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
        //DataCell missing = DataType.getMissingCell();
        testForValues(xorSingle, trueCell, falseCell, trueCell, falseCell/*, missing*/);

        final Expression xorSame = m_factory.xor(Arrays.asList(m_mockBoolExpression, m_mockBoolExpression));
        testForValues(xorSame, falseCell, falseCell, falseCell, falseCell/*, missing*/);

        final Expression xorMixed = m_factory.xor(Arrays.asList(m_mockBoolExpression, m_mockBoolExpressionOther));
        testForValues(xorMixed, falseCell, trueCell, trueCell, falseCell/*, missing*/);
    }

    /**
     * Test method for {@link ExpressionFactory#missing(Expression)} .
     */
    @Test
    public void testMissing() {
        final Expression missingSingle = m_factory.missing(m_mockBoolExpression);
        final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
        testForValues(missingSingle, falseCell, falseCell, falseCell, falseCell, trueCell);
    }

    /**
     * Test method for {@link ExpressionFactory#columnRef(DataTableSpec, java.lang.String)} .
     */
    @Test
    public void testColumnRef() {
        final DefaultRow row0 = new DefaultRow(new RowKey("0"), new IntCell(3));
        final DefaultRow row1 = new DefaultRow(new RowKey("1"), new IntCell(3), new DoubleCell(3.14159));
        final DefaultRow row2 = new DefaultRow(new RowKey("1"), new DoubleCell(3.14159), new StringCell("Hi"));
        final ExpressionValue firstVal =
            m_factory.columnRef(new DataTableSpec(new String[]{"Num"}, new DataType[]{IntCell.TYPE}), "Num").evaluate(
                row0, null);
        assertEquals(row0.getCell(0), firstVal.getValue());
        assertEquals(
            row1.getCell(0),
            m_factory
                .columnRef(new DataTableSpec(new String[]{"Num", "Pi"}, new DataType[]{IntCell.TYPE, DoubleCell.TYPE}),
                    "Num").evaluate(row0, null).getValue());
        final ExpressionValue secondVal =
            m_factory.columnRef(
                new DataTableSpec(new String[]{"Num", "Pi"}, new DataType[]{IntCell.TYPE, DoubleCell.TYPE}), "Pi")
                .evaluate(row1, null);
        assertEquals(row1.getCell(1), secondVal.getValue());
        final ExpressionValue thirdVal =
            m_factory.columnRef(
                new DataTableSpec(new String[]{"Pi", "Greeting"}, new DataType[]{DoubleCell.TYPE, StringCell.TYPE}),
                "Greeting").evaluate(row2, null);
        assertEquals(row2.getCell(1), thirdVal.getValue());
    }

    /**
     * Test method for {@link ExpressionFactory#columnRefForMissing(DataTableSpec, String)} .
     */
    @Test
    public void testColumnRefForMissing() {
        final DefaultRow row0 = new DefaultRow(new RowKey("0"), DataType.getMissingCell());
        final ExpressionValue firstVal =
            m_factory.columnRef(new DataTableSpec(new String[]{"Num"}, new DataType[]{IntCell.TYPE}), "Num").evaluate(
                row0, null);
        assertEquals(row0.getCell(0), firstVal.getValue());
        assertEquals(BooleanCell.FALSE,
            m_factory.columnRef(new DataTableSpec(new String[]{"Bool"}, new DataType[]{BooleanCell.TYPE}), "Bool")
                .evaluate(row0, null).getValue());
        final ExpressionValue secondVal =
            m_factory.columnRefForMissing(new DataTableSpec(new String[]{"Bool"}, new DataType[]{BooleanCell.TYPE}),
                "Bool").evaluate(row0, null);
        assertEquals(row0.getCell(0), secondVal.getValue());
    }

    /**
     * Test method for {@link ExpressionFactory#flowVarRef(java.util.Map, String)}.
     */
    @Test
    public void testFlowVarRef() {
        assertEquals(
            new StringCell("/tmp"),
            m_factory
                .flowVarRef(Collections.singletonMap("xknime.workspace", new FlowVariable("xknime.workspace", "/tmp")),
                    "xknime.workspace").evaluate(null, new VariableProvider() {
                    @Override
                    public Object readVariable(final String name, final Class<?> type) {
                        return "/tmp";
                    }

                    @Deprecated
                    @Override
                    public int getRowCount() {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Deprecated
                    @Override
                    public int getRowIndex() {
                        // TODO Auto-generated method stub
                        return 0;
                    }
                }).getValue());
    }

    /**
     * Test method for {@link ExpressionFactory#constant(String, DataType)} .
     */
    @Test
    public void testConstantStringDataType() {
        final Expression hello = m_factory.constant("Hello", StringCell.TYPE);
        assertTrue(hello.isConstant());
        final ExpressionValue mockResult = hello.evaluate(m_rows[0], null);
        assertEquals(new StringCell("Hello"), mockResult.getValue());
        assertEquals(new StringCell("Hello"), hello.evaluate(m_rows[0], null).getValue());
        assertEquals(Collections.emptyMap(), mockResult.getMatchedObjects());

        final ExpressionValue nullResult = hello.evaluate(null, null);
        assertEquals(new StringCell("Hello"), nullResult.getValue());
        assertEquals(Collections.emptyMap(), nullResult.getMatchedObjects());
    }

    /**
     * Test method for {@link ExpressionFactory#constant(double)} .
     */
    @Test
    public void testConstantDouble() {
        final Expression one = m_factory.constant(1.0);
        assertTrue(one.isConstant());
        assertEquals(new DoubleCell(1.0), one.evaluate(null, null).getValue());
        assertEquals(Collections.emptyMap(), one.evaluate(null, null).getMatchedObjects());
        final Expression infinity = m_factory.constant(Double.POSITIVE_INFINITY);
        assertTrue(infinity.isConstant());
        assertEquals(new DoubleCell(Double.POSITIVE_INFINITY), infinity.evaluate(null, null).getValue());
        assertEquals(Collections.emptyMap(), infinity.evaluate(null, null).getMatchedObjects());
        final Expression nan = m_factory.constant(Double.NaN);
        assertTrue(nan.isConstant());
        assertEquals(new DoubleCell(Double.NaN), nan.evaluate(null, null).getValue());
        final Expression negZero = m_factory.constant(-.0);
        assertTrue(negZero.isConstant());
        assertEquals(new DoubleCell(-.0), negZero.evaluate(null, null).getValue());
    }

    /**
     * Test method for {@link ExpressionFactory#constant(int)}.
     */
    @Test
    public void testConstantInt() {
        final Expression one = m_factory.constant(1);
        assertTrue(one.isConstant());
        assertEquals(new IntCell(1), one.evaluate(null, null).getValue());
        assertEquals(Collections.emptyMap(), one.evaluate(null, null).getMatchedObjects());
    }

    /**
     * Test method for {@link ExpressionFactory#list(java.util.List)} .
     */
    @Test
    public void testList() {
        final BooleanCell trueCell = BooleanCell.TRUE;
        final BooleanCell falseCell = BooleanCell.FALSE;
        final ListCell trueList = CollectionCellFactory.createListCell(Arrays.asList(trueCell));
        final ListCell falseList = CollectionCellFactory.createListCell(Arrays.asList(falseCell));
        final ListCell missingCellList = CollectionCellFactory.createListCell(Arrays.asList(DataType.getMissingCell()));
        testForValues(m_factory.list(Arrays.asList(m_mockBoolExpression)), trueList, falseList, trueList, falseList,
            missingCellList);
        testForValues(m_factory.list(Arrays.asList(m_mockBoolExpression, m_mockBoolExpressionOther)),
            CollectionCellFactory.createListCell(Arrays.asList(trueCell, trueCell)),
            CollectionCellFactory.createListCell(Arrays.asList(falseCell, trueCell)),
            CollectionCellFactory.createListCell(Arrays.asList(trueCell, falseCell)),
            CollectionCellFactory.createListCell(Arrays.asList(falseCell, falseCell)),
            CollectionCellFactory.createListCell(Arrays.asList(DataType.getMissingCell(), trueCell)));
    }

    /**
     * Test method for {@link ExpressionFactory#in(Expression, Expression)} .
     */
    @Test
    public void testIn() {
        final BooleanCell trueCell = BooleanCell.TRUE;
        final BooleanCell falseCell = BooleanCell.FALSE;
        testForValues(
            m_factory.in(m_mockBoolExpression,
                m_factory.list(Arrays.asList(m_mockBoolExpression, m_mockBoolExpressionOther))), trueCell, trueCell,
            trueCell, trueCell, trueCell);
        testForValues(m_factory.in(m_mockBoolExpressionOther, m_factory.list(Arrays.asList(m_mockBoolExpression))),
            trueCell, falseCell, falseCell, trueCell, falseCell);
        testForValues(m_factory.in(m_mockBoolExpression, m_factory.list(Arrays.asList(m_mockBoolExpressionOther))),
            trueCell, falseCell, falseCell, trueCell, falseCell);
    }

    /**
     * Test method for {@link ExpressionFactory#compare(Expression, Expression, DataValueComparator, int[])} .
     */
    @Test
    public void testCompare() {
        assertEquals(
            BooleanCell.FALSE,
            m_factory
                .compare(m_factory.constant(1), m_factory.constant(3.0),
                    DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), 1).evaluate(null, null)
                .getValue());
        assertEquals(
            BooleanCell.FALSE,
            m_factory
                .compare(m_factory.constant(1), m_factory.constant(3.0),
                    DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), 0).evaluate(null, null)
                .getValue());
        assertEquals(
            BooleanCell.TRUE,
            m_factory
                .compare(m_factory.constant(1), m_factory.constant(3.0),
                    DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), -1, 0)
                .evaluate(null, null).getValue());
        assertEquals(
            BooleanCell.TRUE,
            m_factory
                .compare(m_factory.constant(1), m_factory.constant(3.0),
                    DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), -1)
                .evaluate(null, null).getValue());
        assertEquals(
            BooleanCell.FALSE,
            m_factory
                .compare(m_factory.constant(1), m_factory.constant(3.0),
                    DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), 1, 0)
                .evaluate(null, null).getValue());
    }

    /**
     * Test method for {@link ExpressionFactory#like(Expression, Expression, java.lang.String)} .
     */
    @Test
    public void testLike() {
        final ExpressionValue firstVal =
            m_factory.like(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant("H?llo*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.TRUE, firstVal.getValue());
        assertTrue(firstVal.getMatchedObjects().isEmpty());
        final ExpressionValue secondVal =
            m_factory.like(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant("Hallo*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.FALSE, secondVal.getValue());
        assertTrue(secondVal.getMatchedObjects().isEmpty());
    }

    /**
     * Test method for {@link ExpressionFactory#contains(Expression, Expression, java.lang.String)} .
     */
    @Test
    public void testContains() {
        final ExpressionValue firstVal =
            m_factory.contains(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant(".?llo*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.TRUE, firstVal.getValue());
        assertTrue(firstVal.getMatchedObjects().isEmpty());
        final ExpressionValue secondVal =
            m_factory.contains(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant("Hallo*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.FALSE, secondVal.getValue());
        assertTrue(secondVal.getMatchedObjects().isEmpty());
    }

    /**
     * Test method for {@link ExpressionFactory#matches(Expression, Expression, java.lang.String)} .
     */
    @Test
    public void testMatches() {
        final ExpressionValue firstVal =
            m_factory.matches(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant(".{2}llo .*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.TRUE, firstVal.getValue());
        assertTrue(firstVal.getMatchedObjects().isEmpty());
        final ExpressionValue secondVal =
            m_factory.matches(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant("Hallo*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.FALSE, secondVal.getValue());
        assertTrue(secondVal.getMatchedObjects().isEmpty());
    }

    /**
     * Test method for {@link ExpressionFactory#tableRef(Rule.TableReference)} .
     */
    @Test
    public void testTableRef() {
        assertEquals(new LongCell(2), m_factory.tableRef(TableReference.RowCount).evaluate(null, new VariableProvider() {
            @Override
            public Object readVariable(final String name, final Class<?> type) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public long getRowCountLong() {
                return 2L;
            }

            @Override
            public long getRowIndexLong() {
                return 0L;
            }

            @Deprecated
            @Override
            public int getRowCount() {
                return 0;
            }

            @Deprecated
            @Override
            public int getRowIndex() {
                return 0;
            }
        }).getValue());
        assertEquals(new LongCell(3), m_factory.tableRef(TableReference.RowIndex).evaluate(null, new VariableProvider() {
            @Override
            public Object readVariable(final String name, final Class<?> type) {
                return null;
            }

            @Override
            public long getRowCountLong() {
                return 0L;
            }

            @Override
            public long getRowIndexLong() {
                return 3L;
            }

            @Deprecated
            @Override
            public int getRowCount() {
                return 0;
            }

            @Deprecated
            @Override
            public int getRowIndex() {
                return 0;
            }
        }).getValue());
        assertEquals(
            new StringCell("Row0"),
            m_factory.tableRef(TableReference.RowId)
                .evaluate(new DefaultRow(new RowKey("Row0"), new IntCell(3)), new VariableProvider() {
                    @Override
                    public Object readVariable(final String name, final Class<?> type) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Deprecated
                    @Override
                    public int getRowCount() {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Deprecated
                    @Override
                    public int getRowIndex() {
                        // TODO Auto-generated method stub
                        return 0;
                    }
                }).getValue());
    }

    /**
     * Testing associativity of or.
     */
    @Test
    public void testOrAssociativity() {
        final Expression trueE = m_factory.trueValue();
        final Expression falseE = m_factory.falseValue();
        final Expression missing =
            m_factory.columnRef(new DataTableSpec(new String[]{"b"}, new DataType[]{BooleanCell.TYPE}), "b");
        final Expression right = m_factory.or(Arrays.asList(trueE, m_factory.or(Arrays.asList(falseE, missing))));
        final Expression left = m_factory.or(Arrays.asList(m_factory.or(Arrays.asList(trueE, falseE)), missing));
        final Expression noParen = m_factory.or(Arrays.asList(trueE, falseE, missing));
        final ExpressionValue r = right.evaluate(m_rows[4], null);
        final ExpressionValue l = left.evaluate(m_rows[4], null);
        final ExpressionValue n = noParen.evaluate(m_rows[4], null);
        assertEquals(BooleanCell.TRUE, n.getValue());
        assertEquals(r.getValue(), l.getValue());
        assertEquals(n.getValue(), l.getValue());
    }
}
