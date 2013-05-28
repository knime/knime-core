package org.knime.base.node.rules.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import org.junit.Test;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.SimpleRuleParser.ParseState;

public class ParseStateTest {

	private ParseState empty;
	private ParseState hello;
	private ParseState h;
	private ParseState hello1;
	private ParseState string;
	private ParseState stringWithQuote;
	private ParseState flowVarError;
	private ParseState flowVarError1;
	private ParseState rowIndex;
	private ParseState column;
	private ParseState column1;
	private ParseState columnError;
	private ParseState flowVar;
	private ParseState flowVarError2;
	private ParseState[] d;
	private String[] numbers;
	private String[] partialNumbers;
	private ParseState[] d2;
	private String[] partialNumberMatches;

	@org.junit.Before
	public void setup() {
		empty = new ParseState("");
		hello = new ParseState("Hello");
		hello1 = new ParseState("   Hello   ");
		h = new ParseState("H");
		string = new ParseState("\"Hello\"");
		stringWithQuote = new ParseState("\"Hello\\\" continue\"");
		flowVar = new ParseState("$${S flowvar ok }$$");
		flowVarError = new ParseState("$${D flowvar without end");
		flowVarError1 = new ParseState("$${S flowvar without end $       ");
		flowVarError2 = new ParseState("$${I flowvar without end $   $$");
		rowIndex = new ParseState("$$ROWINDEX$$ Hello");
		column = new ParseState("$col0$");
		column1 = new ParseState("$col1  $");
		columnError = new ParseState("$col0 ");
		numbers = new String[] {"-4.6", "-Infinity", "Infinity", "3", ".4", ".3E43", ".3E-2"};
		d = new ParseState[numbers.length];
		for (int i = 0; i < numbers.length; i++) {
			d[i] = new ParseState(numbers[i]);
		}
		partialNumbers = new String[] {"-.3E", "-.3E-", ".3E-", "3E", "3e.", "3.e"};
		partialNumberMatches = new String[] {"-.3", "-.3", ".3", "3", "3", "3."};
		d2 = new ParseState[partialNumbers.length];
		for (int i = 0; i < partialNumbers.length; i++) {
			d2[i] = new ParseState(partialNumbers[i]);
		}
	}

	@Test
	public void testIsEnd() {
		assertTrue(empty.isEnd());
		assertFalse(h.isEnd());
		assertFalse(hello.isEnd());
		hello.setPosition(5);
		assertTrue(hello.isEnd());
	}

	@Test
	public void testSkipWS() {
		empty.skipWS();
		assertTrue(empty.isEnd());
		assertEquals(0, empty.getPosition());
		h.skipWS();
		assertEquals(0, h.getPosition());
		hello1.skipWS();
		assertEquals(3, hello1.getPosition());
	}

	@Test
	public void testReadString() throws ParseException {
		assertEquals("Hello", string.readString());
		assertTrue(string.isEnd());
		assertEquals("Hello\\", stringWithQuote.readString());
		//Uncomment when quoting gets support
//		assertEquals("Hello\" continue", stringWithQuote.readString());
//		assertTrue(stringWithQuote.isEnd());
	}

	@Test
	public void testExpect() throws ParseException {
		string.expect('"');
		assertEquals(0, string.getPosition());
		hello1.expect(' ');
		hello1.skipWS();
		hello1.expect('H');
		assertEquals(3, hello1.getPosition());
		hello.expect('H');
		hello.expect('H');
	}

	@Test(expected = ParseException.class)
	public void testExpectFail() throws ParseException {
		empty.expect('"');
	}

	@Test
	public void testPeekChar() throws ParseException {
		assertEquals('H', hello.peekChar());
		assertEquals('H', h.peekChar());
		assertEquals(' ', hello1.peekChar());
		assertEquals('"', string.peekChar());
		assertEquals('"', stringWithQuote.peekChar());
	}

	@Test(expected = ParseException.class)
	public void testPeekCharFail() throws ParseException {
		empty.peekChar();
	}

	@Test
	public void testConsume() throws ParseException {
		h.consume();
		assertTrue(h.isEnd());
		hello.consume();
		assertEquals('e', hello.peekChar());
	}

	@Test(expected = ParseException.class)
	public void testConsumeFail() throws ParseException {
		empty.consume();
	}

	@Test
	public void testPeekNext() throws ParseException {
		assertEquals('e', hello.peekNext());
		assertEquals('H', string.peekNext());
		assertEquals(' ', hello1.peekNext());
	}

	@Test(expected = ParseException.class)
	public void testPeekNextFail() throws ParseException {
		h.peekNext();
	}

	@Test
	public void testPeekText() {
		assertFalse(h.peekText("Hello world"));
		assertFalse(hello.peekText("Hello world"));
		assertFalse(hello1.peekText("Hello world"));
		assertTrue(hello1.peekText("   Hello"));
		assertTrue(hello.peekText("Hello"));
		hello1.skipWS();
		assertTrue(hello1.peekText("Hello"));
	}

	@Test
	public void testConsumeText() throws ParseException {
		assertEquals("H", h.consumeText("H"));
		assertEquals("   ", hello1.consumeText("   "));
		assertEquals("Hello", hello.consumeText("Hello"));
		assertTrue(hello.isEnd());
		assertEquals("Hello", hello1.consumeText("Hello"));
	}

	@Test(expected = ParseException.class)
	public void testConsumeTextFail() throws ParseException {
		assertEquals("H", h.consumeText("Abba"));
	}

	@Test
	public void testIsFlowVariableRef() {
		assertTrue(flowVar.isFlowVariableRef());
		assertTrue(flowVarError.isFlowVariableRef());
		assertTrue(flowVarError1.isFlowVariableRef());
		assertFalse(rowIndex.isFlowVariableRef());
		assertFalse(empty.isFlowVariableRef());
		assertFalse(h.isFlowVariableRef());
		assertFalse(hello.isFlowVariableRef());
		assertFalse(column.isFlowVariableRef());
		assertFalse(columnError.isFlowVariableRef());
		assertFalse(column1.isFlowVariableRef());
	}

	@Test
	public void testIsColumnRef() {
		assertFalse(flowVar.isColumnRef());
		assertFalse(flowVarError.isColumnRef());
		assertFalse(flowVarError1.isColumnRef());
		assertFalse(rowIndex.isColumnRef());
		assertFalse(empty.isColumnRef());
		assertFalse(h.isColumnRef());
		assertFalse(hello.isColumnRef());
		assertTrue(column.isColumnRef());
		assertTrue(columnError.isColumnRef());
		assertTrue(column1.isColumnRef());
	}
	
	@Test
	public void testReadTablePropertyReference() throws ParseException {
		assertEquals("ROWINDEX", rowIndex.readTablePropertyReference());
		assertFalse(rowIndex.isEnd());
		assertEquals("ROWCOUNT", new ParseState("$$ROWCOUNT$$").readTablePropertyReference());
		assertEquals("ROWID", new ParseState("$$ROWID$$").readTablePropertyReference());
	}

	@Test
	public void testReadFlowVariable() throws ParseException {
		assertEquals(" flowvar ok ", flowVar.readFlowVariable());
		assertTrue(flowVar.isEnd());
	}

	@Test(expected = ParseException.class)
	public void testReadFlowVariableFail0() throws ParseException {
		empty.readFlowVariable();
	}

	@Test(expected = ParseException.class)
	public void testReadFlowVariableFail1() throws ParseException {
		column.readFlowVariable();
	}

	@Test(expected = ParseException.class)
	public void testReadFlowVariableFail2() throws ParseException {
		flowVarError.readFlowVariable();
	}

	@Test(expected = ParseException.class)
	public void testReadFlowVariableFail3() throws ParseException {
		flowVarError1.readFlowVariable();
	}

	@Test(expected = ParseException.class)
	public void testReadFlowVariableFail4() throws ParseException {
		flowVarError2.readFlowVariable();
	}

	@Test
	public void testReadColumnRef() throws ParseException {
		assertEquals("col0", column.readColumnRef());
		assertEquals("col1  ", column1.readColumnRef());
		rowIndex.consume();
		assertEquals("ROWINDEX", rowIndex.readColumnRef());
		flowVarError1.consume();
		assertEquals("{S flowvar without end ", flowVarError1.readColumnRef());
	}

	@Test(expected = ParseException.class)
	public void testReadColumnRefFail0() throws ParseException {
		empty.readColumnRef();
	}

	@Test(expected = ParseException.class)
	public void testReadColumnRefFail1() throws ParseException {
		flowVar.readFlowVariable();
		flowVar.readColumnRef();
	}

	@Test(expected = ParseException.class)
	public void testReadColumnRefFail2() throws ParseException {
		columnError.readColumnRef();
	}

	@Test(expected = ParseException.class)
	public void testReadColumnRefFail3() throws ParseException {
		flowVarError.consume();
		flowVarError.readColumnRef();
	}

	@Test(expected = ParseException.class)
	public void testReadColumnRefFail4() throws ParseException {
		hello.readColumnRef();
	}

	@Test
	public void testGetPosition() throws ParseException {
		assertEquals(0, empty.getPosition());
		assertEquals(0, h.getPosition());
		h.consume();
		assertEquals(1, h.getPosition());
	}

	@Test
	public void testSetPosition() {
		hello.setPosition(3);
		assertEquals(3, hello.getPosition());
	}

	@Test
	public void testParseNumber() throws ParseException {
		for (int i = 0; i < d.length; i++) {
			ParseState ps = d[i];
			assertEquals(numbers[i], numbers[i], ps.parseNumber());
			Double.parseDouble(numbers[i]);
		}
		for (int i = 0; i < d2.length; i++) {
			ParseState ps = d2[i];
			assertEquals(partialNumbers[i], partialNumberMatches[i], ps.parseNumber());
		}
		assertEquals("3", new ParseState("3 ").parseNumber());
	}
	
	@Test(expected=ParseException.class)
	public void testParseNumberFail0() throws ParseException {
		new ParseState("-").parseNumber();
	}

	@Test(expected=ParseException.class)
	public void testParseNumberFail1() throws ParseException {
		new ParseState("-.").parseNumber();
	}
	
	@Test(expected=ParseException.class)
	public void testParseNumberFail2() throws ParseException {
		new ParseState("..4.e").parseNumber();
	}
	
	@Test(expected=ParseException.class)
	public void testParseNumberFail3() throws ParseException {
		new ParseState("E44").parseNumber();
	}
	
	@Test(expected=ParseException.class)
	public void testParseNumberFail4() throws ParseException {
		new ParseState("-.3EE-").parseNumber();
	}
	
	@Test
	public void testParseOperator() throws ParseException {
		for (Operators op : Operators.values()) {
			assertEquals(op, new ParseState(op.toString()).parseOperator());
		}
	}

	@Test
	public void testExpectWS() throws ParseException {
		hello1.skipWS();
		hello1.consumeText("Hello");
		hello1.expectWS();
		hello1.consume();
		hello1.expectWS();
		hello1.consume();
		hello1.expectWS();
		hello1.consume();
		assertTrue(hello1.isEnd());
	}
}
