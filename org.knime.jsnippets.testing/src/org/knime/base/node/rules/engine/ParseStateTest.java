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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import org.junit.Test;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.BaseRuleParser.ParseState;

/**
 * Tests for {@link ParseState}.
 * 
 * @author Gabor Bakos
 */
public class ParseStateTest {

    private ParseState m_empty;
    private ParseState m_hello;
    private ParseState m_h;
    private ParseState m_hello1;
    private ParseState m_string;
    private ParseState m_stringWithQuote;
    private ParseState m_flowVarError;
    private ParseState m_flowVarError1;
    private ParseState m_rowIndex;
    private ParseState m_column;
    private ParseState m_column1;
    private ParseState m_columnError;
    private ParseState m_flowVar;
    private ParseState m_flowVarError2;
    private ParseState[] m_d;
    private String[] m_numbers;
    private String[] m_partialNumbers;
    private ParseState[] m_d2;
    private String[] m_partialNumberMatches;

    /**
     * Initialize the test constants.
     */
    @org.junit.Before
    public void setup() {
        m_empty = new ParseState("");
        m_hello = new ParseState("Hello");
        m_hello1 = new ParseState("   Hello   ");
        m_h = new ParseState("H");
        m_string = new ParseState("\"Hello\"");
        m_stringWithQuote = new ParseState("\"Hello\\\" continue\"");
        m_flowVar = new ParseState("$${S flowvar ok }$$");
        m_flowVarError = new ParseState("$${D flowvar without end");
        m_flowVarError1 = new ParseState("$${S flowvar without end $       ");
        m_flowVarError2 = new ParseState("$${I flowvar without end $   $$");
        m_rowIndex = new ParseState("$$ROWINDEX$$ Hello");
        m_column = new ParseState("$col0$");
        m_column1 = new ParseState("$col1  $");
        m_columnError = new ParseState("$col0 ");
        m_numbers = new String[] { "-4.6", "-Infinity", "Infinity", "3", ".4",
                ".3E43", ".3E-2" };
        m_d = new ParseState[m_numbers.length];
        for (int i = 0; i < m_numbers.length; i++) {
            m_d[i] = new ParseState(m_numbers[i]);
        }
        m_partialNumbers = new String[] { "-.3E", "-.3E-", ".3E-", "3E", "3e.",
                "3.e" };
        m_partialNumberMatches = new String[] { "-.3", "-.3", ".3", "3", "3",
                "3." };
        m_d2 = new ParseState[m_partialNumbers.length];
        for (int i = 0; i < m_partialNumbers.length; i++) {
            m_d2[i] = new ParseState(m_partialNumbers[i]);
        }
    }

    /**
     * Tests {@link ParseState#isEnd()}.
     */
    @Test
    public void testIsEnd() {
        assertTrue(m_empty.isEnd());
        assertFalse(m_h.isEnd());
        assertFalse(m_hello.isEnd());
        m_hello.setPosition(5);
        assertTrue(m_hello.isEnd());
    }

    /**
     * Tests {@link ParseState#skipWS()}.
     */
    @Test
    public void testSkipWS() {
        m_empty.skipWS();
        assertTrue(m_empty.isEnd());
        assertEquals(0, m_empty.getPosition());
        m_h.skipWS();
        assertEquals(0, m_h.getPosition());
        m_hello1.skipWS();
        assertEquals(3, m_hello1.getPosition());
    }

    /**
     * Tests {@link ParseState#readString()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testReadString() throws ParseException {
        assertEquals("Hello", m_string.readString());
        assertTrue(m_string.isEnd());
        assertEquals("Hello\\", m_stringWithQuote.readString());
        // Uncomment when quoting gets support
        // assertEquals("Hello\" continue", m_stringWithQuote.readString());
        // assertTrue(m_stringWithQuote.isEnd());
    }

    /**
     * Tests {@link ParseState#expect(char)}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testExpect() throws ParseException {
        m_string.expect('"');
        assertEquals(0, m_string.getPosition());
        m_hello1.expect(' ');
        m_hello1.skipWS();
        m_hello1.expect('H');
        assertEquals(3, m_hello1.getPosition());
        m_hello.expect('H');
        m_hello.expect('H');
    }

    /**
     * Tests {@link ParseState#expect(char)}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testExpectFail() throws ParseException {
        m_empty.expect('"');
    }

    /**
     * Tests {@link ParseState#peekChar()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testPeekChar() throws ParseException {
        assertEquals('H', m_hello.peekChar());
        assertEquals('H', m_h.peekChar());
        assertEquals(' ', m_hello1.peekChar());
        assertEquals('"', m_string.peekChar());
        assertEquals('"', m_stringWithQuote.peekChar());
    }

    /**
     * Tests {@link ParseState#peekNext()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testPeekCharFail() throws ParseException {
        m_empty.peekChar();
    }

    /**
     * Tests {@link ParseState#consume()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testConsume() throws ParseException {
        m_h.consume();
        assertTrue(m_h.isEnd());
        m_hello.consume();
        assertEquals('e', m_hello.peekChar());
    }

    /**
     * Tests {@link ParseState#consume()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testConsumeFail() throws ParseException {
        m_empty.consume();
    }

    /**
     * Tests {@link ParseState#peekNext()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testPeekNext() throws ParseException {
        assertEquals('e', m_hello.peekNext());
        assertEquals('H', m_string.peekNext());
        assertEquals(' ', m_hello1.peekNext());
    }

    /**
     * Tests {@link ParseState#peekNext()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testPeekNextFail() throws ParseException {
        m_h.peekNext();
    }

    /**
     * Tests {@link ParseState#peekText(String)}.
     */
    @Test
    public void testPeekText() {
        assertFalse(m_h.peekText("Hello world"));
        assertFalse(m_hello.peekText("Hello world"));
        assertFalse(m_hello1.peekText("Hello world"));
        assertTrue(m_hello1.peekText("   Hello"));
        assertTrue(m_hello.peekText("Hello"));
        m_hello1.skipWS();
        assertTrue(m_hello1.peekText("Hello"));
    }

    /**
     * Tests {@link ParseState#consumeText(String)}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testConsumeText() throws ParseException {
        assertEquals("H", m_h.consumeText("H"));
        assertEquals("   ", m_hello1.consumeText("   "));
        assertEquals("Hello", m_hello.consumeText("Hello"));
        assertTrue(m_hello.isEnd());
        assertEquals("Hello", m_hello1.consumeText("Hello"));
    }

    /**
     * Tests {@link ParseState#consumeText(String)}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testConsumeTextFail() throws ParseException {
        assertEquals("H", m_h.consumeText("Abba"));
    }

    /**
     * Tests {@link ParseState#isFlowVariableRef()}.
     */
    @Test
    public void testIsFlowVariableRef() {
        assertTrue(m_flowVar.isFlowVariableRef());
        assertTrue(m_flowVarError.isFlowVariableRef());
        assertTrue(m_flowVarError1.isFlowVariableRef());
        assertFalse(m_rowIndex.isFlowVariableRef());
        assertFalse(m_empty.isFlowVariableRef());
        assertFalse(m_h.isFlowVariableRef());
        assertFalse(m_hello.isFlowVariableRef());
        assertFalse(m_column.isFlowVariableRef());
        assertFalse(m_columnError.isFlowVariableRef());
        assertFalse(m_column1.isFlowVariableRef());
    }

    /**
     * Tests {@link ParseState#isColumnRef()}.
     */
    @Test
    public void testIsColumnRef() {
        assertFalse(m_flowVar.isColumnRef());
        assertFalse(m_flowVarError.isColumnRef());
        assertFalse(m_flowVarError1.isColumnRef());
        assertFalse(m_rowIndex.isColumnRef());
        assertFalse(m_empty.isColumnRef());
        assertFalse(m_h.isColumnRef());
        assertFalse(m_hello.isColumnRef());
        assertTrue(m_column.isColumnRef());
        assertTrue(m_columnError.isColumnRef());
        assertTrue(m_column1.isColumnRef());
    }

    /**
     * Tests {@link ParseState#readTablePropertyReference()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testReadTablePropertyReference() throws ParseException {
        assertEquals("ROWINDEX", m_rowIndex.readTablePropertyReference());
        assertFalse(m_rowIndex.isEnd());
        assertEquals("ROWCOUNT",
                new ParseState("$$ROWCOUNT$$").readTablePropertyReference());
        assertEquals("ROWID",
                new ParseState("$$ROWID$$").readTablePropertyReference());
    }

    /**
     * Tests {@link ParseState#readFlowVariable()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testReadFlowVariable() throws ParseException {
        assertEquals(" flowvar ok ", m_flowVar.readFlowVariable());
        assertTrue(m_flowVar.isEnd());
    }

    /**
     * Tests {@link ParseState#readFlowVariable()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testReadFlowVariableFail0() throws ParseException {
        m_empty.readFlowVariable();
    }

    /**
     * Tests {@link ParseState#readFlowVariable()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testReadFlowVariableFail1() throws ParseException {
        m_column.readFlowVariable();
    }

    /**
     * Tests {@link ParseState#readFlowVariable()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testReadFlowVariableFail2() throws ParseException {
        m_flowVarError.readFlowVariable();
    }

    /**
     * Tests {@link ParseState#readFlowVariable()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testReadFlowVariableFail3() throws ParseException {
        m_flowVarError1.readFlowVariable();
    }

    /**
     * Tests {@link ParseState#readFlowVariable()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testReadFlowVariableFail4() throws ParseException {
        m_flowVarError2.readFlowVariable();
    }

    /**
     * Tests {@link ParseState#readColumnRef()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testReadColumnRef() throws ParseException {
        assertEquals("col0", m_column.readColumnRef());
        assertEquals("col1  ", m_column1.readColumnRef());
        m_rowIndex.consume();
        assertEquals("ROWINDEX", m_rowIndex.readColumnRef());
        m_flowVarError1.consume();
        assertEquals("{S flowvar without end ", m_flowVarError1.readColumnRef());
    }

    /**
     * Tests {@link ParseState#readColumnRef()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testReadColumnRefFail0() throws ParseException {
        m_empty.readColumnRef();
    }

    /**
     * Tests {@link ParseState#readColumnRef()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testReadColumnRefFail1() throws ParseException {
        m_flowVar.readFlowVariable();
        m_flowVar.readColumnRef();
    }

    /**
     * Tests {@link ParseState#readColumnRef()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testReadColumnRefFail2() throws ParseException {
        m_columnError.readColumnRef();
    }

    /**
     * Tests {@link ParseState#readColumnRef()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testReadColumnRefFail3() throws ParseException {
        m_flowVarError.consume();
        m_flowVarError.readColumnRef();
    }

    /**
     * Tests {@link ParseState#readColumnRef()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testReadColumnRefFail4() throws ParseException {
        m_hello.readColumnRef();
    }

    /**
     * Tests {@link ParseState#getPosition()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testGetPosition() throws ParseException {
        assertEquals(0, m_empty.getPosition());
        assertEquals(0, m_h.getPosition());
        m_h.consume();
        assertEquals(1, m_h.getPosition());
    }

    /**
     * Tests {@link ParseState#setPosition(int)}.
     */
    @Test
    public void testSetPosition() {
        m_hello.setPosition(3);
        assertEquals(3, m_hello.getPosition());
    }

    /**
     * Tests {@link ParseState#parseNumber()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testParseNumber() throws ParseException {
        for (int i = 0; i < m_d.length; i++) {
            ParseState ps = m_d[i];
            assertEquals(m_numbers[i], m_numbers[i], ps.parseNumber());
            Double.parseDouble(m_numbers[i]);
        }
        for (int i = 0; i < m_d2.length; i++) {
            ParseState ps = m_d2[i];
            assertEquals(m_partialNumbers[i], m_partialNumberMatches[i],
                    ps.parseNumber());
        }
        assertEquals("3", new ParseState("3 ").parseNumber());
    }

    /**
     * Tests {@link ParseState#parseNumber()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testParseNumberFail0() throws ParseException {
        new ParseState("-").parseNumber();
    }

    /**
     * Tests {@link ParseState#parseNumber()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testParseNumberFail1() throws ParseException {
        new ParseState("-.").parseNumber();
    }

    /**
     * Tests {@link ParseState#parseNumber()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testParseNumberFail2() throws ParseException {
        new ParseState("..4.e").parseNumber();
    }

    /**
     * Tests {@link ParseState#parseNumber()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testParseNumberFail3() throws ParseException {
        new ParseState("E44").parseNumber();
    }

    /**
     * Tests {@link ParseState#parseNumber()}.
     * 
     * @throws ParseException
     *             Should happen.
     */
    @Test(expected = ParseException.class)
    public void testParseNumberFail4() throws ParseException {
        new ParseState("-.3EE-").parseNumber();
    }

    /**
     * Tests {@link ParseState#parseOperator()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testParseOperator() throws ParseException {
        for (Operators op : Operators.values()) {
            assertEquals(op, new ParseState(op.toString()).parseOperator());
        }
    }

    /**
     * Tests {@link ParseState#expectWS()}.
     * 
     * @throws ParseException
     *             Should not happen.
     */
    @Test
    public void testExpectWS() throws ParseException {
        m_hello1.skipWS();
        m_hello1.consumeText("Hello");
        m_hello1.expectWS();
        m_hello1.consume();
        m_hello1.expectWS();
        m_hello1.consume();
        m_hello1.expectWS();
        m_hello1.consume();
        assertTrue(m_hello1.isEnd());
    }
}
