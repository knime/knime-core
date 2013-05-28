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
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.base.node.rules.engine.Rule.TableReference;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
	
	private ExpressionFactory factory = ExpressionFactory.getInstance();
	private Expression mockBoolExpression, mockBoolExpressionOther;
	private DataRow [] rows;
	private VariableProvider mockProvider;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		final Map<String, Map<String, String>> emptyMap = Collections.emptyMap();
		mockBoolExpression = new Expression() {

			@Override
			public List<DataType> getInputArgs() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public DataType getOutputType() {
				return BooleanCell.TYPE;
			}

			@Override
			public ExpressionValue evaluate(DataRow row,
					VariableProvider provider) {
				final DataCell cell;
				switch (Integer.parseInt(row.getKey().getString())) {
				case 0: cell = BooleanCell.TRUE; break;
				case 1: cell = BooleanCell.FALSE; break;
				case 2: cell = BooleanCell.TRUE; break;
				case 3: cell = BooleanCell.FALSE; break;
				case 4: cell = DataType.getMissingCell(); break;
				default: throw new UnsupportedOperationException();
				}
				return new ExpressionValue(cell, emptyMap);
			}

			@Override
			public boolean isConstant() {
				return false;
			}};
		mockBoolExpressionOther = new Expression() {
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
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public ExpressionValue evaluate(DataRow row, VariableProvider provider) {
				final DataCell cell;
				switch (Integer.parseInt(row.getKey().getString())) {
				case 0: cell = BooleanCell.TRUE; break;
				case 1: cell = BooleanCell.TRUE; break;
				case 2: cell = BooleanCell.FALSE; break;
				case 3: cell = BooleanCell.FALSE; break;
				case 4: cell = BooleanCell.TRUE; break;
				default: throw new UnsupportedOperationException();
				}
				return new ExpressionValue(cell, emptyMap);
			}
		};
		//mockBoolExpression = mock(Expression.class);
		//mockBoolExpressionOther = mock(Expression.class);
//		rows = new DataRow[] {mock(DataRow.class), mock(DataRow.class), mock(DataRow.class), mock(DataRow.class), mock(DataRow.class)};
		final DefaultRow row = new DefaultRow(new RowKey("x"), new DataCell[0]);
		rows = new DataRow[] {new DefaultRow(new RowKey("0"), row), new DefaultRow(new RowKey("1"), row), new DefaultRow(new RowKey("2"), row), new DefaultRow(new RowKey("3"), row), new DefaultRow(new RowKey("4"), row)};
//		when(mockBoolExpression.isConstant()).thenReturn(false);
//		when(mockBoolExpression.getOutputType()).thenReturn(BooleanCell.TYPE);
//		when(mockBoolExpressionOther.isConstant()).thenReturn(false);
//		when(mockBoolExpressionOther.getOutputType()).thenReturn(BooleanCell.TYPE);
//		mockProvider = mock(VariableProvider.class);
		mockProvider = null;
//		when (mockBoolExpression.evaluate(rows[0], mockProvider)).thenReturn(new ExpressionValue(BooleanCell.TRUE, emptyMap));
//		when (mockBoolExpression.evaluate(rows[1], mockProvider)).thenReturn(new ExpressionValue(BooleanCell.FALSE, emptyMap));
//		when (mockBoolExpression.evaluate(rows[2], mockProvider)).thenReturn(new ExpressionValue(BooleanCell.TRUE, emptyMap));
//		when (mockBoolExpression.evaluate(rows[3], mockProvider)).thenReturn(new ExpressionValue(BooleanCell.FALSE, emptyMap));
//		when (mockBoolExpression.evaluate(rows[4], mockProvider)).thenReturn(new ExpressionValue(DataType.getMissingCell(), emptyMap));
//		when (mockBoolExpressionOther.evaluate(rows[0], mockProvider)).thenReturn(new ExpressionValue(BooleanCell.TRUE, emptyMap));
//		when (mockBoolExpressionOther.evaluate(rows[1], mockProvider)).thenReturn(new ExpressionValue(BooleanCell.TRUE, emptyMap));
//		when (mockBoolExpressionOther.evaluate(rows[2], mockProvider)).thenReturn(new ExpressionValue(BooleanCell.FALSE, emptyMap));
//		when (mockBoolExpressionOther.evaluate(rows[3], mockProvider)).thenReturn(new ExpressionValue(BooleanCell.FALSE, emptyMap));
//		when (mockBoolExpressionOther.evaluate(rows[4], mockProvider)).thenReturn(new ExpressionValue(BooleanCell.TRUE, emptyMap));
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#not(org.knime.base.node.rules.engine.SimpleRuleParser.Expression)}.
	 */
	@Test
	public void testNot() {
		Expression not = factory.not(mockBoolExpression);
		ExpressionValue firstValue = not.evaluate(rows[0], mockProvider);
		ExpressionValue secondValue = not.evaluate(rows[1], mockProvider);
		assertEquals(BooleanCell.FALSE, firstValue.getValue());
		assertEquals(BooleanCell.TRUE, secondValue.getValue());
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#and(org.knime.base.node.rules.engine.SimpleRuleParser.Expression[])}.
	 */
	@Test
	public void testAnd() {
		Expression andSingle = factory.and(mockBoolExpression);
		final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
		DataCell missing = DataType.getMissingCell();
		testForValues(andSingle, trueCell, falseCell, trueCell, falseCell, missing);

		Expression andSame = factory.and(mockBoolExpression, mockBoolExpression);
		testForValues(andSame, trueCell, falseCell, trueCell, falseCell, missing);
		
		Expression andMixed = factory.and(mockBoolExpression, mockBoolExpressionOther);
		testForValues(andMixed, trueCell, falseCell, falseCell, falseCell, missing);
	}
	
	private void testForValues(Expression expression, DataCell... expectedValues) {
		for (int i = 0; i < rows.length; ++i) {
			ExpressionValue returnValue = expression.evaluate(rows[i], mockProvider);
			assertEquals("Case: " + i, expectedValues[i], returnValue.getValue());
		}
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#or(org.knime.base.node.rules.engine.SimpleRuleParser.Expression[])}.
	 */
	@Test
	public void testOr() {
		Expression orSingle = factory.or(mockBoolExpression);
		final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
		DataCell missing = DataType.getMissingCell();
		testForValues(orSingle, trueCell, falseCell, trueCell, falseCell, missing);

		Expression orSame = factory.or(mockBoolExpression, mockBoolExpression);
		testForValues(orSame, trueCell, falseCell, trueCell, falseCell, missing);
		
		Expression orMixed = factory.or(mockBoolExpression, mockBoolExpressionOther);
		testForValues(orMixed, trueCell, trueCell, trueCell, falseCell, trueCell);
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#xor(org.knime.base.node.rules.engine.SimpleRuleParser.Expression[])}.
	 */
	@Test
	public void testXor() {
		Expression xorSingle = factory.xor(mockBoolExpression);
		final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
		DataCell missing = DataType.getMissingCell();
		testForValues(xorSingle, trueCell, falseCell, trueCell, falseCell, missing);

		Expression xorSame = factory.xor(mockBoolExpression, mockBoolExpression);
		testForValues(xorSame, falseCell, falseCell, falseCell, falseCell, missing);
		
		Expression xorMixed = factory.xor(mockBoolExpression, mockBoolExpressionOther);
		testForValues(xorMixed, falseCell, trueCell, trueCell, falseCell, missing);
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#missing(org.knime.base.node.rules.engine.SimpleRuleParser.Expression)}.
	 */
	@Test
	public void testMissing() {
		Expression missingSingle = factory.missing(mockBoolExpression);
		final BooleanCell trueCell = BooleanCell.TRUE, falseCell = BooleanCell.FALSE;
		testForValues(missingSingle, falseCell, falseCell, falseCell, falseCell, trueCell);
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#columnRef(org.knime.core.data.DataTableSpec, java.lang.String)}.
	 */
	@Test
	public void testColumnRef() {
		DefaultRow row0 = new DefaultRow(new RowKey("0"), new IntCell(3));
		DefaultRow row1 = new DefaultRow(new RowKey("1"), new IntCell(3), new DoubleCell(3.14159));
		DefaultRow row2 = new DefaultRow(new RowKey("1"), new DoubleCell(3.14159), new StringCell("Hi"));
		ExpressionValue firstVal = factory.columnRef(new DataTableSpec(new String[] {"Num"}, new DataType[] {IntCell.TYPE}), "Num").evaluate(row0, null);
		assertEquals(row0.getCell(0), firstVal.getValue());
		assertEquals(row1.getCell(0), factory.columnRef(new DataTableSpec(new String[] {"Num", "Pi"}, new DataType[] {IntCell.TYPE, DoubleCell.TYPE}), "Num").evaluate(row0, null).getValue());
		ExpressionValue secondVal = factory.columnRef(new DataTableSpec(new String[] {"Num", "Pi"}, new DataType[] {IntCell.TYPE, DoubleCell.TYPE}), "Pi").evaluate(row1, null);
		assertEquals(row1.getCell(1), secondVal.getValue());
		ExpressionValue thirdVal = factory.columnRef(new DataTableSpec(new String[] {"Pi", "Greeting"}, new DataType[] {DoubleCell.TYPE, StringCell.TYPE}), "Greeting").evaluate(row2, null);
		assertEquals(row2.getCell(1), thirdVal.getValue());
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#flowVarRef(java.util.Map, java.lang.String)}.
	 */
	@Test
	public void testFlowVarRef() {
		assertEquals(new StringCell("/tmp"), factory.flowVarRef(Collections.singletonMap("xknime.workspace", new FlowVariable("xknime.workspace", "/tmp")), "xknime.workspace").evaluate(null, new VariableProvider() {
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
			}}).getValue());
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#constant(java.lang.String, org.knime.core.data.DataType)}.
	 */
	@Test
	public void testConstantStringDataType() {
		Expression hello = factory.constant("Hello", StringCell.TYPE);
		assertTrue(hello.isConstant());
		final ExpressionValue mockResult = hello.evaluate(rows[0], mockProvider);
		assertEquals(new StringCell("Hello"), mockResult.getValue());
		assertEquals(new StringCell("Hello"), hello.evaluate(rows[0], mockProvider).getValue());
		assertEquals(Collections.emptyMap(), mockResult.getMatchedObjects());

		final ExpressionValue nullResult = hello.evaluate(null, null);
		assertEquals(new StringCell("Hello"), nullResult.getValue());
		assertEquals(Collections.emptyMap(), nullResult.getMatchedObjects());
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#constant(double)}.
	 */
	@Test
	public void testConstantDouble() {
		Expression one = factory.constant(1.0);
		assertTrue(one.isConstant());
		assertEquals(new DoubleCell(1.0), one.evaluate(null, null).getValue());
		assertEquals(Collections.emptyMap(), one.evaluate(null, null).getMatchedObjects());
		Expression infinity = factory.constant(Double.POSITIVE_INFINITY);
		assertTrue(infinity.isConstant());
		assertEquals(new DoubleCell(Double.POSITIVE_INFINITY), infinity.evaluate(null, null).getValue());
		assertEquals(Collections.emptyMap(), infinity.evaluate(null, null).getMatchedObjects());
		Expression nan = factory.constant(Double.NaN);
		assertTrue(nan.isConstant());
		assertEquals(new DoubleCell(Double.NaN), nan.evaluate(null, null).getValue());
		Expression negZero = factory.constant(-.0);
		assertTrue(negZero.isConstant());
		assertEquals(new DoubleCell(-.0), negZero.evaluate(null, null).getValue());
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#constant(int)}.
	 */
	@Test
	public void testConstantInt() {
		Expression one = factory.constant(1);
		assertTrue(one.isConstant());
		assertEquals(new IntCell(1), one.evaluate(null, null).getValue());
		assertEquals(Collections.emptyMap(), one.evaluate(null, null).getMatchedObjects());
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#list(java.util.List)}.
	 */
	@Test
	public void testList() {
		final BooleanCell trueCell = BooleanCell.TRUE;
		final BooleanCell falseCell = BooleanCell.FALSE;
		final ListCell trueList = CollectionCellFactory.createListCell(Arrays.asList(trueCell));
		final ListCell falseList = CollectionCellFactory.createListCell(Arrays.asList(falseCell));
		final ListCell missingCellList = CollectionCellFactory.createListCell(Arrays.asList(DataType.getMissingCell()));
		testForValues(factory.list(Arrays.asList(mockBoolExpression)), trueList, falseList, trueList, falseList, missingCellList);
		testForValues(factory.list(Arrays.asList(mockBoolExpression, mockBoolExpressionOther)), 
				CollectionCellFactory.createListCell(Arrays.asList(trueCell, trueCell)),
				CollectionCellFactory.createListCell(Arrays.asList(falseCell, trueCell)),
				CollectionCellFactory.createListCell(Arrays.asList(trueCell, falseCell)),
				CollectionCellFactory.createListCell(Arrays.asList(falseCell, falseCell)),
				CollectionCellFactory.createListCell(Arrays.asList(DataType.getMissingCell(), trueCell))
				);
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#in(org.knime.base.node.rules.engine.SimpleRuleParser.Expression, org.knime.base.node.rules.engine.SimpleRuleParser.Expression)}.
	 */
	@Test
	public void testIn() {
		final BooleanCell trueCell = BooleanCell.TRUE;
		final BooleanCell falseCell = BooleanCell.FALSE;
		testForValues(factory.in(mockBoolExpression, factory.list(Arrays.asList(mockBoolExpression, mockBoolExpressionOther))), trueCell, trueCell, trueCell, trueCell, trueCell);
		testForValues(factory.in(mockBoolExpressionOther, factory.list(Arrays.asList(mockBoolExpression))), trueCell, falseCell, falseCell, trueCell, falseCell);
		testForValues(factory.in(mockBoolExpression, factory.list(Arrays.asList(mockBoolExpressionOther))), trueCell, falseCell, falseCell, trueCell, falseCell);
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#compare(org.knime.base.node.rules.engine.SimpleRuleParser.Expression, org.knime.base.node.rules.engine.SimpleRuleParser.Expression, org.knime.core.data.DataValueComparator, int[])}.
	 */
	@Test
	public void testCompare() {
		assertEquals(BooleanCell.FALSE, factory.compare(factory.constant(1), factory.constant(3.0), DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), 1).evaluate(null, null).getValue());
		assertEquals(BooleanCell.FALSE, factory.compare(factory.constant(1), factory.constant(3.0), DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), 0).evaluate(null, null).getValue());
		assertEquals(BooleanCell.TRUE, factory.compare(factory.constant(1), factory.constant(3.0), DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), -1, 0).evaluate(null, null).getValue());
		assertEquals(BooleanCell.TRUE, factory.compare(factory.constant(1), factory.constant(3.0), DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), -1).evaluate(null, null).getValue());
		assertEquals(BooleanCell.FALSE, factory.compare(factory.constant(1), factory.constant(3.0), DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), 1, 0).evaluate(null, null).getValue());
//		testForValues(factory.compare(factory.constant(1), factory.constant(3.0), DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), 0), BooleanCell.FALSE);
//		testForValues(factory.compare(factory.constant(1), factory.constant(3.0), DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), -1, 0), BooleanCell.TRUE);
//		testForValues(factory.compare(factory.constant(1), factory.constant(3.0), DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), -1), BooleanCell.TRUE);
//		testForValues(factory.compare(factory.constant(1), factory.constant(3.0), DataType.getCommonSuperType(IntCell.TYPE, DoubleCell.TYPE).getComparator(), 1, 0), BooleanCell.FALSE);
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#like(org.knime.base.node.rules.engine.SimpleRuleParser.Expression, org.knime.base.node.rules.engine.SimpleRuleParser.Expression, java.lang.String)}.
	 */
	@Test
	public void testLike() {
		final ExpressionValue firstVal = factory.like(factory.constant("Hello world", StringCell.TYPE), factory.constant("H?llo*", StringCell.TYPE), null).evaluate(null, null);
		assertEquals(BooleanCell.TRUE, firstVal.getValue());
		assertTrue(firstVal.getMatchedObjects().isEmpty());
		final ExpressionValue secondVal = factory.like(factory.constant("Hello world", StringCell.TYPE), factory.constant("Hallo*", StringCell.TYPE), null).evaluate(null, null);
		assertEquals(BooleanCell.FALSE, secondVal.getValue());
		assertTrue(secondVal.getMatchedObjects().isEmpty());
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#contains(org.knime.base.node.rules.engine.SimpleRuleParser.Expression, org.knime.base.node.rules.engine.SimpleRuleParser.Expression, java.lang.String)}.
	 */
	@Test
	public void testContains() {
		final ExpressionValue firstVal = factory.contains(factory.constant("Hello world", StringCell.TYPE), factory.constant(".?llo*", StringCell.TYPE), null).evaluate(null, null);
		assertEquals(BooleanCell.TRUE, firstVal.getValue());
		assertTrue(firstVal.getMatchedObjects().isEmpty());
		final ExpressionValue secondVal = factory.contains(factory.constant("Hello world", StringCell.TYPE), factory.constant("Hallo*", StringCell.TYPE), null).evaluate(null, null);
		assertEquals(BooleanCell.FALSE, secondVal.getValue());
		assertTrue(secondVal.getMatchedObjects().isEmpty());
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#matches(org.knime.base.node.rules.engine.SimpleRuleParser.Expression, org.knime.base.node.rules.engine.SimpleRuleParser.Expression, java.lang.String)}.
	 */
	@Test
	public void testMatches() {
		final ExpressionValue firstVal = factory.matches(factory.constant("Hello world", StringCell.TYPE), factory.constant(".{2}llo .*", StringCell.TYPE), null).evaluate(null, null);
		assertEquals(BooleanCell.TRUE, firstVal.getValue());
		assertTrue(firstVal.getMatchedObjects().isEmpty());
		final ExpressionValue secondVal = factory.matches(factory.constant("Hello world", StringCell.TYPE), factory.constant("Hallo*", StringCell.TYPE), null).evaluate(null, null);
		assertEquals(BooleanCell.FALSE, secondVal.getValue());
		assertTrue(secondVal.getMatchedObjects().isEmpty());
	}

	/**
	 * Test method for {@link org.knime.base.node.rules.engine.ExpressionFactory#tableRef(org.knime.base.node.rules.engine.Rule.TableReference)}.
	 */
	@Test
	public void testTableRef() {
		assertEquals(new IntCell(2), factory.tableRef(TableReference.RowCount).evaluate(null, new VariableProvider() {
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
			}}).getValue());
		assertEquals(new IntCell(3), factory.tableRef(TableReference.RowIndex).evaluate(null, new VariableProvider() {
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
			}}).getValue());
		assertEquals(new StringCell("Row0"), factory.tableRef(TableReference.RowId).evaluate(new DefaultRow(new RowKey("Row0"), new IntCell(3)), new VariableProvider() {
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
			}}).getValue());
	}

}
