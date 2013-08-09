/**
 * 
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
     * @throws java.lang.Exception
     *             Should not happen.
     */
    @Before
    public void setUp() throws Exception {
        final Map<String, Map<String, String>> emptyMap = Collections.emptyMap();
        m_mockBoolExpression = new Expression() {

            @Override
            public List<DataType> getInputArgs() {
            	return Collections.emptyList();
            }

            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            @Override
            public ExpressionValue evaluate(DataRow row, VariableProvider provider) {
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
        };
        m_mockBoolExpressionOther = new Expression() {
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
            public ExpressionValue evaluate(DataRow row, VariableProvider provider) {
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
        };
        final DefaultRow row = new DefaultRow(new RowKey("x"), new DataCell[0]);
        m_rows = new DataRow[] { new DefaultRow(new RowKey("0"), row), new DefaultRow(new RowKey("1"), row),
                new DefaultRow(new RowKey("2"), row), new DefaultRow(new RowKey("3"), row),
                new DefaultRow(new RowKey("4"), new DataCell[] {DataType.getMissingCell()}) };
    }

    /**
     * Test method for {@link ExpressionFactory#not(Expression)} .
     */
    @Test
    public void testNot() {
        Expression not = m_factory.not(m_mockBoolExpression);
        ExpressionValue firstValue = not.evaluate(m_rows[0], null);
        ExpressionValue secondValue = not.evaluate(m_rows[1], null);
        ExpressionValue thirdValue = not.evaluate(m_rows[4], null);
        assertEquals(BooleanCell.FALSE, firstValue.getValue());
        assertEquals(BooleanCell.TRUE, secondValue.getValue());
        assertEquals(DataType.getMissingCell(), thirdValue.getValue());
    }

    /**
     * Test method for {@link ExpressionFactory#and(Expression[])} .
     */
    @Test
    public void testAnd() {
        Expression andSingle = m_factory.and(m_mockBoolExpression);
        final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
        //DataCell missing = DataType.getMissingCell();
        testForValues(andSingle, trueCell, falseCell, trueCell, falseCell/*, missing*/);

        Expression andSame = m_factory.and(m_mockBoolExpression, m_mockBoolExpression);
        testForValues(andSame, trueCell, falseCell, trueCell, falseCell/*, missing*/);

        Expression andMixed = m_factory.and(m_mockBoolExpression, m_mockBoolExpressionOther);
        testForValues(andMixed, trueCell, falseCell, falseCell, falseCell/*, missing*/);
    }

    private void testForValues(Expression expression, DataCell... expectedValues) {
        for (int i = 0; i < expectedValues.length; ++i) {
            ExpressionValue returnValue = expression.evaluate(m_rows[i], null);
            assertEquals("Case: " + i, expectedValues[i], returnValue.getValue());
        }
    }

    /**
     * Test method for {@link ExpressionFactory#or(Expression[])} .
     */
    @Test
    public void testOr() {
        Expression orSingle = m_factory.or(m_mockBoolExpression);
        final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
        //DataCell missing = DataType.getMissingCell();
        testForValues(orSingle, trueCell, falseCell, trueCell, falseCell/*, missing*/);

        Expression orSame = m_factory.or(m_mockBoolExpression, m_mockBoolExpression);
        testForValues(orSame, trueCell, falseCell, trueCell, falseCell/*, missing*/);

        Expression orMixed = m_factory.or(m_mockBoolExpression, m_mockBoolExpressionOther);
        testForValues(orMixed, trueCell, trueCell, trueCell, falseCell/*, missing*/);
    }

    /**
     * Test method for {@link ExpressionFactory#xor(Expression[])} .
     */
    @Test
    public void testXor() {
        Expression xorSingle = m_factory.xor(m_mockBoolExpression);
        final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
        //DataCell missing = DataType.getMissingCell();
        testForValues(xorSingle, trueCell, falseCell, trueCell, falseCell/*, missing*/);

        Expression xorSame = m_factory.xor(m_mockBoolExpression, m_mockBoolExpression);
        testForValues(xorSame, falseCell, falseCell, falseCell, falseCell/*, missing*/);

        Expression xorMixed = m_factory.xor(m_mockBoolExpression, m_mockBoolExpressionOther);
        testForValues(xorMixed, falseCell, trueCell, trueCell, falseCell/*, missing*/);
    }

    /**
     * Test method for {@link ExpressionFactory#missing(Expression)} .
     */
    @Test
    public void testMissing() {
        Expression missingSingle = m_factory.missing(m_mockBoolExpression);
        final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
        testForValues(missingSingle, falseCell, falseCell, falseCell, falseCell, trueCell);
    }

    /**
     * Test method for
     * {@link ExpressionFactory#columnRef(DataTableSpec, java.lang.String)} .
     */
    @Test
    public void testColumnRef() {
        DefaultRow row0 = new DefaultRow(new RowKey("0"), new IntCell(3));
        DefaultRow row1 = new DefaultRow(new RowKey("1"), new IntCell(3), new DoubleCell(3.14159));
        DefaultRow row2 = new DefaultRow(new RowKey("1"), new DoubleCell(3.14159), new StringCell("Hi"));
        ExpressionValue firstVal = m_factory.columnRef(
                new DataTableSpec(new String[] { "Num" }, new DataType[] { IntCell.TYPE }), "Num").evaluate(row0, null);
        assertEquals(row0.getCell(0), firstVal.getValue());
        assertEquals(
                row1.getCell(0),
                m_factory
                        .columnRef(
                                new DataTableSpec(new String[] { "Num", "Pi" }, new DataType[] { IntCell.TYPE,
                                        DoubleCell.TYPE }), "Num").evaluate(row0, null).getValue());
        ExpressionValue secondVal = m_factory
                .columnRef(
                        new DataTableSpec(new String[] { "Num", "Pi" },
                                new DataType[] { IntCell.TYPE, DoubleCell.TYPE }), "Pi").evaluate(row1, null);
        assertEquals(row1.getCell(1), secondVal.getValue());
        ExpressionValue thirdVal = m_factory.columnRef(
                new DataTableSpec(new String[] { "Pi", "Greeting" },
                        new DataType[] { DoubleCell.TYPE, StringCell.TYPE }), "Greeting").evaluate(row2, null);
        assertEquals(row2.getCell(1), thirdVal.getValue());
    }

    /**
     * Test method for
     * {@link ExpressionFactory#columnRefForMissing(DataTableSpec, String)} .
     */
    @Test
    public void testColumnRefForMissing() {
    	DefaultRow row0 = new DefaultRow(new RowKey("0"), DataType.getMissingCell());
    	ExpressionValue firstVal = m_factory.columnRef(
    			new DataTableSpec(new String[] { "Num" }, new DataType[] { IntCell.TYPE }), "Num").evaluate(row0, null);
    	assertEquals(row0.getCell(0), firstVal.getValue());
    	assertEquals(
    			BooleanCell.FALSE,
    			m_factory
    			.columnRef(
    					new DataTableSpec(new String[] { "Bool"}, new DataType[] { BooleanCell.TYPE}), "Bool").evaluate(row0, null).getValue());
    	ExpressionValue secondVal = m_factory
    			.columnRefForMissing(
    					new DataTableSpec(new String[] { "Bool" },
    							new DataType[] { BooleanCell.TYPE}), "Bool").evaluate(row0, null);
    	assertEquals(row0.getCell(0), secondVal.getValue());
    }
    
    /**
     * Test method for
     * {@link ExpressionFactory#flowVarRef(java.util.Map, String)}.
     */
    @Test
    public void testFlowVarRef() {
        assertEquals(
                new StringCell("/tmp"),
                m_factory
                        .flowVarRef(
                                Collections.singletonMap("xknime.workspace", new FlowVariable("xknime.workspace",
                                        "/tmp")), "xknime.workspace").evaluate(null, new VariableProvider() {
                            @Override
                            public Object readVariable(String name, Class<?> type) {
                                return "/tmp";
                            }

                            @Override
                            public int getRowCount() {
                                // TODO Auto-generated method stub
                                return 0;
                            }

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
        Expression hello = m_factory.constant("Hello", StringCell.TYPE);
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
        Expression one = m_factory.constant(1.0);
        assertTrue(one.isConstant());
        assertEquals(new DoubleCell(1.0), one.evaluate(null, null).getValue());
        assertEquals(Collections.emptyMap(), one.evaluate(null, null).getMatchedObjects());
        Expression infinity = m_factory.constant(Double.POSITIVE_INFINITY);
        assertTrue(infinity.isConstant());
        assertEquals(new DoubleCell(Double.POSITIVE_INFINITY), infinity.evaluate(null, null).getValue());
        assertEquals(Collections.emptyMap(), infinity.evaluate(null, null).getMatchedObjects());
        Expression nan = m_factory.constant(Double.NaN);
        assertTrue(nan.isConstant());
        assertEquals(new DoubleCell(Double.NaN), nan.evaluate(null, null).getValue());
        Expression negZero = m_factory.constant(-.0);
        assertTrue(negZero.isConstant());
        assertEquals(new DoubleCell(-.0), negZero.evaluate(null, null).getValue());
    }

    /**
     * Test method for {@link ExpressionFactory#constant(int)}.
     */
    @Test
    public void testConstantInt() {
        Expression one = m_factory.constant(1);
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
                        m_factory.list(Arrays.asList(m_mockBoolExpression, m_mockBoolExpressionOther))), trueCell,
                trueCell, trueCell, trueCell, trueCell);
        testForValues(m_factory.in(m_mockBoolExpressionOther, m_factory.list(Arrays.asList(m_mockBoolExpression))),
                trueCell, falseCell, falseCell, trueCell, falseCell);
        testForValues(m_factory.in(m_mockBoolExpression, m_factory.list(Arrays.asList(m_mockBoolExpressionOther))),
                trueCell, falseCell, falseCell, trueCell, falseCell);
    }

    /**
     * Test method for
     * {@link ExpressionFactory#compare(Expression, Expression, DataValueComparator, int[])}
     * .
     */
    @Test
    public void testCompare() {
        assertEquals(
                BooleanCell.FALSE,
                m_factory
                        .compare(m_factory.constant(1), m_factory.constant(3.0),
                                DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), 1)
                        .evaluate(null, null).getValue());
        assertEquals(
                BooleanCell.FALSE,
                m_factory
                        .compare(m_factory.constant(1), m_factory.constant(3.0),
                                DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), 0)
                        .evaluate(null, null).getValue());
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
     * Test method for
     * {@link ExpressionFactory#like(Expression, Expression, java.lang.String)}
     * .
     */
    @Test
    public void testLike() {
        final ExpressionValue firstVal = m_factory.like(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant("H?llo*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.TRUE, firstVal.getValue());
        assertTrue(firstVal.getMatchedObjects().isEmpty());
        final ExpressionValue secondVal = m_factory.like(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant("Hallo*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.FALSE, secondVal.getValue());
        assertTrue(secondVal.getMatchedObjects().isEmpty());
    }

    /**
     * Test method for
     * {@link ExpressionFactory#contains(Expression, Expression, java.lang.String)}
     * .
     */
    @Test
    public void testContains() {
        final ExpressionValue firstVal = m_factory.contains(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant(".?llo*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.TRUE, firstVal.getValue());
        assertTrue(firstVal.getMatchedObjects().isEmpty());
        final ExpressionValue secondVal = m_factory.contains(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant("Hallo*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.FALSE, secondVal.getValue());
        assertTrue(secondVal.getMatchedObjects().isEmpty());
    }

    /**
     * Test method for
     * {@link ExpressionFactory#matches(Expression, Expression, java.lang.String)}
     * .
     */
    @Test
    public void testMatches() {
        final ExpressionValue firstVal = m_factory.matches(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant(".{2}llo .*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.TRUE, firstVal.getValue());
        assertTrue(firstVal.getMatchedObjects().isEmpty());
        final ExpressionValue secondVal = m_factory.matches(m_factory.constant("Hello world", StringCell.TYPE),
                m_factory.constant("Hallo*", StringCell.TYPE), null).evaluate(null, null);
        assertEquals(BooleanCell.FALSE, secondVal.getValue());
        assertTrue(secondVal.getMatchedObjects().isEmpty());
    }

    /**
     * Test method for {@link ExpressionFactory#tableRef(Rule.TableReference)} .
     */
    @Test
    public void testTableRef() {
        assertEquals(new IntCell(2), m_factory.tableRef(TableReference.RowCount).evaluate(null, new VariableProvider() {
            @Override
            public Object readVariable(String name, Class<?> type) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getRowCount() {
                return 2;
            }

            @Override
            public int getRowIndex() {
                // TODO Auto-generated method stub
                return 0;
            }
        }).getValue());
        assertEquals(new IntCell(3), m_factory.tableRef(TableReference.RowIndex).evaluate(null, new VariableProvider() {
            @Override
            public Object readVariable(String name, Class<?> type) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getRowCount() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int getRowIndex() {
                return 3;
            }
        }).getValue());
        assertEquals(
                new StringCell("Row0"),
                m_factory.tableRef(TableReference.RowId)
                        .evaluate(new DefaultRow(new RowKey("Row0"), new IntCell(3)), new VariableProvider() {
                            @Override
                            public Object readVariable(String name, Class<?> type) {
                                // TODO Auto-generated method stub
                                return null;
                            }

                            @Override
                            public int getRowCount() {
                                // TODO Auto-generated method stub
                                return 0;
                            }

                            @Override
                            public int getRowIndex() {
                                // TODO Auto-generated method stub
                                return 0;
                            }
                        }).getValue());
    }

    @Test
    public void testOrAssociativity() {
    	Expression trueE = m_factory.trueValue();
    	Expression falseE = m_factory.falseValue();
    	Expression missing = m_factory.columnRef(new DataTableSpec(new String[] {"b"}, new DataType[] {BooleanCell.TYPE}), "b");
    	Expression right = m_factory.or(trueE, m_factory.or(falseE, missing));
    	Expression left = m_factory.or(m_factory.or(trueE, falseE), missing);
    	Expression noParen = m_factory.or(trueE, falseE, missing);
    	ExpressionValue r = right.evaluate(m_rows[4], null);
    	ExpressionValue l = left.evaluate(m_rows[4], null);
    	ExpressionValue n = noParen.evaluate(m_rows[4], null);
    	assertEquals(BooleanCell.TRUE, n.getValue());
    	assertEquals(r.getValue(), l.getValue());
    	assertEquals(n.getValue(), l.getValue());
    }
}
