/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.util.tokenizer;

import java.io.StringReader;

import junit.framework.TestCase;

/**
 * JUnit test for the <code>Tokenizer</code>.
 *
 * @author Peter Ohl, University of Konstanz
 */
public final class TokenizerTest extends TestCase {

    /**
     * System entry point calls the
     * <code>junit.textui.TestRunner.run(TokenizerTest.class)</code> to
     * start this <code>TestCase</code>.
     *
     * @param args Command line parameter(s).
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(TokenizerTest.class);
    }

    /**
     * Tests the constructor.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testTokenizer() throws TokenizerException {
        // create a default Tokenizer and see if the default behaviour
        // is as documented.
        // The default doesn't support any comment, delimiter, or quotes.
        // So, this string should come back in one piece.
        String token;
        final String inputString =
            "123,234,\",,3 4 5\n,'456'\n\na�?#~\\,,\n\n";
        Tokenizer ft = new Tokenizer(new StringReader(inputString));

        token = ft.nextToken();
        assertEquals(token, "123,234,\",,3 4 5\n,'456'\n\na�?#~\\,,\n\n");
        token = ft.nextToken();
        assertNull(token);
    }

    /**
     * Settings are set all over the place here in the test. So the only thing
     * we test here is that getSettings returns the same thing that is set with
     * setSettings
     */
    public void testSetSettings() {
        TokenizerSettings fts = new TokenizerSettings();
        final String inputString = "";
        Tokenizer ft = new Tokenizer(new StringReader(inputString));
        fts.addBlockCommentPattern("/*", "*/", false, false);
        fts.addQuotePattern("'", "'");
        fts.addDelimiterPattern("\n", false, false, false);
        fts.addDelimiterPattern(" ", false, true, false);
        fts.addDelimiterPattern(",", false, true, false);

        ft.setSettings(fts);

        String ftsFirst = fts.toString();
        // the one we set and the one we get back should be same
        assertEquals(ftsFirst, ft.getSettings().toString());

        // once more
        ft.setSettings(ft.getSettings());
        assertEquals(ftsFirst, ft.getSettings().toString());
    }

    /**
     * Test for <code>resetToDefault()</code> method. Sets CommentPatterns, a
     * QuotePattern, and DelimiterPatterns (makes sure the input is cut in
     * peaces as expected) then resets to default and tests that all previously
     * defined patterns are not effective anymore.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testResetToDefault() throws TokenizerException {
        // setup some custom behaviour and check default behaviour after reset.
        String token;
        final String inputString = "123 234\n/*f \noo */'8 9' , end\n";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);

        fts.addBlockCommentPattern("/*", "*/", false, false);
        fts.addQuotePattern("'", "'");
        fts.addDelimiterPattern("\n", false, false, false);
        fts.addDelimiterPattern(" ", false, true, false);
        fts.addDelimiterPattern(",", false, true, false);
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, " ");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "8 9");
        token = ft.nextToken();
        assertEquals(token, " ");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, ",");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, " ");
        token = ft.nextToken();
        assertEquals(token, "end");
        token = ft.nextToken();
        assertNull(token);

        // Now, reset to default behaviour
        try {
            ft.resetToDefault();
            fail("resetting after reading from the tokenizer should blow off.");
        } catch (IllegalStateException ise) {
            // ending up here is fine.
        }
    } // testResetToDefault()

    /**
     * Test for <code>nextToken()</code> method. It just makes sure we don't
     * stumble over umlauts and other weird characters. The normal cases are
     * covered by all other tests. Also, it tries tenthousandtimes to get behind
     * the EOF...
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testNextToken() throws TokenizerException {
        String token;
        final String inputString =
            "1,2\n\n\n\n3\t����,4\b\r\"�^�`'#";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);

        fts.addDelimiterPattern(",", false, false, false);
        fts.addDelimiterPattern("\n", false, false, false);
        ft.setSettings(fts);

        // Let's make sure it won't get messed up with weird characters
        token = ft.nextToken();
        assertEquals(token, "1");
        token = ft.nextToken();
        assertEquals(token, "2");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "3\t����");
        token = ft.nextToken();
        assertEquals(token, "4\b\r\"�^�`'#");
        token = ft.nextToken();
        assertNull(token);

        // make sure we can't get behind the last token...
        final int foo = 10000; // to make the StyleChecker happy...
        for (int i = 0; i < foo; i++) {
            token = ft.nextToken();
            assertNull(token);
        }
    }

    /**
     * Test for <code>pushBack()</code> method. Pushs back a "normal" token
     * and expects it back. Pushs back same token 7 times. Pushs back
     * <code>null</code>.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testPushBack() throws TokenizerException {
        String token;
        final String inputString = "123\n234\n345\n";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);
        fts.addDelimiterPattern("\n", false, false, false);
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        ft.pushBack();
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "345");
        ft.pushBack();
        ft.pushBack();
        ft.pushBack();
        ft.pushBack();
        ft.pushBack();
        ft.pushBack();
        ft.pushBack();
        ft.pushBack();
        token = ft.nextToken();
        assertEquals(token, "345");
        token = ft.nextToken();
        assertNull(token);
        ft.pushBack();
        ft.pushBack();
        token = ft.nextToken();
        assertNull(token);
    } // testPushBack()

    /**
     * Tests with no quotes defined ('"' and ''' will not act as quotes). And,
     * with '"', ''', and "quote" defined as quote patterns only the defined
     * patterns will cause the flag to return <code>true</code>.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testLastTokenWasQuoted() throws TokenizerException {
        String token;
        final String inputString = "\"123\"\n'234'\n<quote>345</quote>\n";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);
        fts.addDelimiterPattern("\n", false, false, false);
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "\"123\"");
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
        token = ft.nextToken();
        assertEquals(token, "'234'");
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
        token = ft.nextToken();
        assertEquals(token, "<quote>345</quote>");
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
        token = ft.nextToken();
        assertNull(token);


        // reset stream
        strReader = new StringReader(inputString);

        ft = new Tokenizer(strReader);
        fts = new TokenizerSettings();
        fts.addDelimiterPattern("\n", false, false, false);
        fts.addQuotePattern("\"", "\"");
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        assertTrue(ft.lastTokenWasQuoted());
        assertEquals(ft.getLastQuoteBeginPattern(), "\"");
        assertEquals(ft.getLastQuoteEndPattern(), "\"");
        token = ft.nextToken();
        assertEquals(token, "'234'");
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
        token = ft.nextToken();
        assertEquals(token, "<quote>345</quote>");
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
        token = ft.nextToken();
        assertNull(token);
        assertFalse(ft.lastTokenWasQuoted());

        // reset stream
        strReader = new StringReader(inputString);

        ft = new Tokenizer(strReader);
        fts = new TokenizerSettings();
        fts.addDelimiterPattern("\n", false, false, false);
        fts.addQuotePattern("'", "'");
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "\"123\"");
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
        ft.pushBack();
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
        token = ft.nextToken();
        assertEquals(token, "\"123\"");
        token = ft.nextToken();
        assertEquals(token, "234");
        assertTrue(ft.lastTokenWasQuoted());
        assertEquals(ft.getLastQuoteBeginPattern(), "'");
        assertEquals(ft.getLastQuoteEndPattern(), "'");
        ft.pushBack();
        assertTrue(ft.lastTokenWasQuoted());
        assertEquals(ft.getLastQuoteBeginPattern(), "'");
        assertEquals(ft.getLastQuoteEndPattern(), "'");
        token = ft.nextToken();
        assertEquals(token, "234");
        assertTrue(ft.lastTokenWasQuoted());
        assertEquals(ft.getLastQuoteBeginPattern(), "'");
        assertEquals(ft.getLastQuoteEndPattern(), "'");
        token = ft.nextToken();
        assertEquals(token, "<quote>345</quote>");
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
        token = ft.nextToken();
        assertNull(token);
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());

        // reset stream
        strReader = new StringReader(inputString);

        ft = new Tokenizer(strReader);
        fts = new TokenizerSettings();
        fts.addDelimiterPattern("\n", false, false, false);
        fts.addQuotePattern("<quote>", "</quote>");
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "\"123\"");
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
        token = ft.nextToken();
        assertEquals(token, "'234'");
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
        token = ft.nextToken();
        assertEquals(token, "345");
        assertTrue(ft.lastTokenWasQuoted());
        assertEquals(ft.getLastQuoteBeginPattern(), "<quote>");
        assertEquals(ft.getLastQuoteEndPattern(), "</quote>");
        token = ft.nextToken();
        assertNull(token);
        assertFalse(ft.lastTokenWasQuoted());
        assertNull(ft.getLastQuoteBeginPattern());
        assertNull(ft.getLastQuoteEndPattern());
    } // testLastTokenWasQuoted()

    /**
     * Test for void <code>addQuotePattern(String, String, char)</code> First
     * tests illegal arguments, see method <code>weirdQuotePatterns</code>
     * then it tests the normal case: defines double quotes, no escape char.
     * Next, double quotes with escape character. Finally, a multicharacter
     * quote pattern is tested.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testAddQuotePattern() throws TokenizerException {

        // catch the abnormal first
        String token;
        String inputString;
        inputString = "123";
        StringReader strReader = new StringReader(inputString);
        Tokenizer ft = new Tokenizer(strReader);

        // test some normal double quote stuff with escape character.
        inputString = "123,\"23,4\"\"3,4\\\",5\",\"45,6\",\"\",\"3";
        strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addDelimiterPattern(",", false, false, false);
        fts.addQuotePattern("\"", "\"", '\\');
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        assertFalse(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "23,43,4\",5");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "45,6");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "3");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertNull(token);

        // reset stream
        /* "123,\"23,4\"\"3,4\\\",5\",\"45,6\",\"\",\"3" */
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addDelimiterPattern(",", false, false, false);
        // make sure quotes stay in the token
        fts.addQuotePattern("\"", "\"", '\\', true);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        assertFalse(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "\"23,4\"\"3,4\",5\"");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "\"45,6\"");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "\"\"");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "\"3");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertNull(token);

        // reset stream
        strReader = new StringReader(inputString);
        ft = new Tokenizer(strReader);
        fts = new TokenizerSettings();
        fts.addDelimiterPattern(",", false, false, false);
        fts.addQuotePattern("\"", "\"");
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        assertFalse(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "23,43,4\\");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "5,45");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "6,,3");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertNull(token);

        // lets use a loooong quote pattern.
        inputString = "123, quotebeginpattern2, 3, 4quoteendpattern, 345";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addQuotePattern("quotebeginpattern", "quoteendpattern");
        fts.addDelimiterPattern(", ", false, false, false);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        assertFalse(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "2, 3, 4");
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, "345");
        assertFalse(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertNull(token);
        assertFalse(ft.lastTokenWasQuoted());
    } // testAddQuotePattern()

    /**
     * Test for <code>addDelimiterPattern()</code> method. First it tests the
     * abnormal illegal argument situations. Then it tests all kinds of
     * delimiter/quote combinations, with all kinds of flags set: delimiter
     * inside quotes, consecutive delimiters - empty tokens inbetween or not -
     * included in the token, combined, or returned as separate token.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testAddDelimiterPattern1() throws TokenizerException {
        String token;
        String inputString;
        // catch the abnormal first, tests illegal arguments
        inputString = "123";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);
        inputString = "123, 234,\t345,\n456,567, \"one, token\",\t678, , 789, "
                + "890,\t,\t901, the end";
        strReader = new StringReader(inputString);
        ft = new Tokenizer(strReader);
        // two delimiters, one will be returned as separate token.
        // delimiter pattern inside quotes should not start a new token.
        fts.addDelimiterPattern(", ", false, false, false);
        fts.addDelimiterPattern(",\t", true, false, false);
        fts.addQuotePattern("\"", "\"");
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "345,\n456,567");
        token = ft.nextToken();
        assertEquals(token, "one, token");
        token = ft.nextToken();
        assertEquals(token, "678");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "789");
        token = ft.nextToken();
        assertEquals(token, "890");
        token = ft.nextToken();
        assertEquals(token, "901");
        token = ft.nextToken();
        assertEquals(token, "the end");
        token = ft.nextToken();
        assertNull(token);

        inputString = "123, 234, , , , 456,,567, 678, , 789, end";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        // tests combine consecutive delimiters
        fts.addDelimiterPattern(", ", true, false, false);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "456,,567");
        token = ft.nextToken();
        assertEquals(token, "678");
        token = ft.nextToken();
        assertEquals(token, "789");
        token = ft.nextToken();
        assertEquals(token, "end");
        token = ft.nextToken();
        assertNull(token);
        // delimiter coming back as separate token.

        // = "123, 234, , , , 456,,567, 678, , 789, end"
        // reset stream
        strReader = new StringReader(inputString);
        ft = new Tokenizer(strReader);
        fts = new TokenizerSettings();
        fts.addDelimiterPattern(", ", false, true, false);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, ", ");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, ", ");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, ", ");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, ", ");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, ", ");
        token = ft.nextToken();
        assertEquals(token, "456,,567");
        token = ft.nextToken();
        assertEquals(token, ", ");
        token = ft.nextToken();
        assertEquals(token, "678");
        token = ft.nextToken();
        assertEquals(token, ", ");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, ", ");
        token = ft.nextToken();
        assertEquals(token, "789");
        token = ft.nextToken();
        assertEquals(token, ", ");
        token = ft.nextToken();
        assertEquals(token, "end");
        token = ft.nextToken();
        assertNull(token);

    }

    /**
     * separating this into two functions to avoid warnings.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testAddDelimiterPattern2() throws TokenizerException {

        // all kinds of parameter combinations.
        String inputString = "123,++---234-,2+-,,6,,7**+**8**";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);
        /*                       (combine, return, include)*/
        fts.addDelimiterPattern(",", false, false, true);
        fts.addDelimiterPattern("+", false, true, false);
        fts.addDelimiterPattern("-", true, true, false);
        fts.addDelimiterPattern("*", true, false, true);
        ft.setSettings(fts);
        String token = ft.nextToken();
        assertEquals(token, "123,");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "+");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "+");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "-");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "-");
        token = ft.nextToken();
        assertEquals(token, ",");
        token = ft.nextToken();
        assertEquals(token, "2");
        token = ft.nextToken();
        assertEquals(token, "+");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "-");
        token = ft.nextToken();
        assertEquals(token, ",");
        token = ft.nextToken();
        assertEquals(token, ",");
        token = ft.nextToken();
        assertEquals(token, "6,");
        token = ft.nextToken();
        assertEquals(token, ",");
        token = ft.nextToken();
        assertEquals(token, "7*");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "+");
        token = ft.nextToken();
        assertEquals(token, "*");
        token = ft.nextToken();
        assertEquals(token, "8*");
        token = ft.nextToken();
        assertNull(token);

        inputString = "123\n\n\n2,3,4\n2\n6,,7\n\n\n8,7";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        /* ( combine, return, include */
        fts.addDelimiterPattern(",", false, false, false);
        fts.addDelimiterPattern("\n", true, true, false);
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "\n");
        token = ft.nextToken();
        assertEquals(token, "2");
        token = ft.nextToken();
        assertEquals(token, "3");
        token = ft.nextToken();
        assertEquals(token, "4");
        token = ft.nextToken();
        assertEquals(token, "\n");
        token = ft.nextToken();
        assertEquals(token, "2");
        token = ft.nextToken();
        assertEquals(token, "\n");
        token = ft.nextToken();
        assertEquals(token, "6");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "7");
        token = ft.nextToken();
        assertEquals(token, "\n");
        token = ft.nextToken();
        assertEquals(token, "8");
        token = ft.nextToken();
        assertEquals(token, "7");


        // ensure line continuation chars between tokens don't break combining
        inputString = "123---234-345--\\\n-243";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        /*                       (combine, return, include) */
        fts.addDelimiterPattern("-", true, true, false);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "-");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "-");
        token = ft.nextToken();
        assertEquals(token, "345");

        token = ft.nextToken();
        assertEquals(token, "-");
        token = ft.nextToken();
        assertEquals(token, "\\\n");
        token = ft.nextToken();
        assertEquals(token, "-");
        token = ft.nextToken();
        assertEquals(token, "243");
        token = ft.nextToken();
        assertNull(token);
    } // testAddDelimiterPatter()

    /**
     * Test for <code>addCommentPattern()</code> method. First tests the
     * abnormal illegal argument situation. Then defines C-style comments
     * (ignoring them), afterwards defines the block comments testing include in
     * token and return as separate token parameters.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testAddCommentPattern() throws TokenizerException {
        String inputString;
        String token;

        inputString = "123";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);

        // define C-style comments
        inputString = "123,234/*comment*/345//line comment, foo\nend";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addDelimiterPattern(",", false, false, false);
        fts.addDelimiterPattern("\n", false, false, false);
        fts.addBlockCommentPattern("/*", "*/", false, false);
        fts.addSingleLineCommentPattern("//", false, false);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234345");
        token = ft.nextToken();
        assertEquals(token, "end");
        token = ft.nextToken();
        assertNull(token);

        // block comments,
        inputString = "123,234/*comment*/345";
        strReader = new StringReader(inputString);
        ft = new Tokenizer(strReader);
        fts = new TokenizerSettings();
        fts.addDelimiterPattern(",", false, false, false);
        fts.addBlockCommentPattern("/*", "*/", false, false);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234345");
        token = ft.nextToken();
        assertNull(token);
        // returning them as separate token, and
        inputString = "123,234/*comment*/345";
        strReader = new StringReader(inputString);
        ft = new Tokenizer(strReader);
        fts = new TokenizerSettings();
        fts.addDelimiterPattern(",", false, false, false);
        fts.addBlockCommentPattern("/*", "*/", true, false);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "/*comment*/");
        token = ft.nextToken();
        assertEquals(token, "345");
        token = ft.nextToken();
        assertNull(token);
        // including them in the token.
        inputString = "123,234/*comment*/345";
        strReader = new StringReader(inputString);
        ft = new Tokenizer(strReader);
        fts = new TokenizerSettings();
        fts.addDelimiterPattern(",", false, false, false);
        fts.addBlockCommentPattern("/*", "*/", false, true);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234/*comment*/345");
        token = ft.nextToken();
        assertNull(token);
    } // testAddCommentPattern()

    /**
     * Tests settings that should fail, like setting different delimiters that
     * are prefixing each other - across different pattern groups.
     * <ul>
     * <li>1) A quote begin prefixing a comment begin pattern,
     * <li>2) A delimiter begin prexing a comment begin patten,
     * <li>3) A comment begin prexing a quote begin,
     * <li>4) A delim prexing a quote,
     * <li>5) A comment prexing a delimiter, and
     * <li>6) A quote prexing a delimiter.
     * </ul>
     * These cases should all throw an exception.
     */
    public void testPatternCombinations() {

        TokenizerSettings fts = new TokenizerSettings();

        /* do some checking across different pattern groups */
        fts.addBlockCommentPattern("LANG", "KURZ", false, false);
        fts.addQuotePattern("LQU", "RQU");
        fts.addDelimiterPattern("DELIM", false, false, false);
        try {
            fts.addQuotePattern("LA", "LURZ");
            fail();
        } catch (IllegalArgumentException iae) {
            // we should end up here.
        }
        try {
            fts.addDelimiterPattern("LAN", false, false, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // we should end up here.
        }

        try {
            fts.addBlockCommentPattern("LQ", "LE", false, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // we should end up here.
        }
        try {
            fts.addDelimiterPattern("LQ", false, false, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // we should end up here.
        }

        try {
            fts.addBlockCommentPattern("DELI", "DELE", false, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // we should end up here.
        }
        try {
            fts.addQuotePattern("DEL", "LED");
            fail();
        } catch (IllegalArgumentException iae) {
            // we should end up here.
        }
    } // testPatternCombinations()

    /**
     * Tests new weird pattern: prefixes existing of same kind, empty begin
     * pattern, empty end pattern, null begin, null end pattern, and both null.
     */
    public void testWeirdQuotePatterns() {
        TokenizerSettings fts = new TokenizerSettings();
        fts.addQuotePattern("left", "right");
        fts.addQuotePattern("tfel", "right"); // same right quote is okay
        try {
            fts.addQuotePattern("lefty", "alrighty"); // prefixing is not okay
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addQuotePattern("", "right"); // empty quote: baaad.
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addQuotePattern("foo", "");
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addQuotePattern(null, "alrighty");
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addQuotePattern("alright", null);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addQuotePattern(null, null);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
    } // weiredQuotePatterns()

    /**
     * Tests new wierd pattern: prefixes existing of same kind, empty begin
     * pattern, empty end pattern, null begin, null end pattern, and both null.
     */
    public void testWeirdCommentPatterns() {
        TokenizerSettings fts = new TokenizerSettings();

        /* ( return, include */
        fts.addBlockCommentPattern("CB", "CE", false, false);
        fts.addBlockCommentPattern("cb", "CE", false, false);

        try {
            fts.addBlockCommentPattern("C", "end", false, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addBlockCommentPattern("", "right", false, true);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addBlockCommentPattern("foo", "", true, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addBlockCommentPattern(null, "alrighty", true, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addBlockCommentPattern("alright", null, false, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addBlockCommentPattern(null, null, false, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addBlockCommentPattern("cbegin", "cend", true, true);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }

    } // weirdCommentPatterns()

    /**
     * Tests wierd new pattern: prefixes existing of same kind, empty begin
     * pattern, empty end pattern, null begin, null end pattern, and both null.
     */
    public void weirdDelimiterPatterns() {
        TokenizerSettings fts = new TokenizerSettings();

        /* combine, return, include */
        fts.addDelimiterPattern("foo", false, false, false);
        try {
            fts.addDelimiterPattern("fo", false, false, true);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addDelimiterPattern("foo", true, false, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addDelimiterPattern(",", true, true, true);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addDelimiterPattern(",", false, true, true);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addDelimiterPattern(null, false, false, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
        try {
            fts.addDelimiterPattern("", false, false, false);
            fail();
        } catch (IllegalArgumentException iae) {
            // ending up here is just fine.
        }
    } // weirdDelimiterPatterns(Tokenizer)

    /**
     * Tests the line continuation character: sets the '\' as line cont char,
     * adds a delimiter that starts with it and checks if the tokenizer can
     * handle it. Also tests if a change of the cont character has an effect.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testSetLineContinuationCharacter()
            throws TokenizerException {
        String token;
        String inputString;
        inputString = "123,234,foo\\\n \t  poo,moo\\dpoo,goo\\blah,"
                + "one\\\n  two\\\n\\\n   four,'quote\\\n   \\\n  cont',end";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);

        fts.addQuotePattern("'", "'");
        fts.addDelimiterPattern(",", false, false, false);
        fts.setLineContinuationCharacter('\\');
        // set a delimiter that starts with the line cont char.
        fts.addDelimiterPattern("\\d", false, false, false);
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "foo \t  poo"); // line cont char, no ws defined
        token = ft.nextToken();
        assertEquals(token, "moo"); // it was the delimiter "\d"
        token = ft.nextToken();
        assertEquals(token, "poo");
        token = ft.nextToken();
        assertEquals(token, "goo\\blah"); // line cont char NOT at the EOL
        token = ft.nextToken();
        assertEquals(token, "one  two   four"); // consectve line cont chars
        token = ft.nextToken();
        assertEquals(token, "quote     cont"); // mult. line cont in quotes
        token = ft.nextToken();
        assertEquals(token, "end");
        token = ft.nextToken();
        assertNull(token);

        // same thing with whitespace characters ' ' and '\t'
        inputString = "123,234,foo\\\n \t  poo,moo\\dpoo,goo\\blah,"
                + "one\\\n  two\\\n\\\n   four,'quote\\\n   \\\n  cont',end";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addQuotePattern("'", "'");
        fts.addDelimiterPattern(",", false, false, false);
        // set a delimiter that starts with the line cont char.
        fts.addDelimiterPattern("\\d", false, false, false);
        fts.addWhiteSpaceCharacter(' ');
        fts.addWhiteSpaceCharacter('\t');
        fts.setLineContinuationCharacter('\\');
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "foo \t  poo"); // not removing WS in tokens
        token = ft.nextToken();
        assertEquals(token, "moo"); // it was the delimiter "\d"
        token = ft.nextToken();
        assertEquals(token, "poo");
        token = ft.nextToken();
        assertEquals(token, "goo\\blah"); // line cont char NOT at the EOL
        token = ft.nextToken();
        assertEquals(token, "one  two   four"); // consectve line cont chars
        token = ft.nextToken();
        assertEquals(token, "quotecont"); // mult. line cont in quotes
        token = ft.nextToken();
        assertEquals(token, "end");
        token = ft.nextToken();
        assertNull(token);

        // Let's see what happens if we change the line cont character.
        inputString = "123,234,foo\\\n \t  poo,moo+dpoo,goo+blah,"
                + "one+\n  two+\n+\n   four\\\nfve,'quote\\\n   +\n  cont',end";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addQuotePattern("'", "'");
        fts.addDelimiterPattern(",", false, false, false);
        fts.addDelimiterPattern("+d", false, false, false);

        fts.setLineContinuationCharacter('\\');
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "foo \t  poo");
        // change it
        fts = ft.getSettings();
        fts.setLineContinuationCharacter('+');
        try {
            ft.setSettings(fts);
            fail("changing settings in the middle of things should blow off.");
        } catch (IllegalStateException ise) {
            // ending up here is gooood.
        }
    } // testSetLineContinuationCharacter()

    /**
     * Tests if we get the line continuation character back that was set before.
     * Also, after EOF, and after changing it - and always.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testGetLineContinuationCharacter()
            throws TokenizerException {
        String inputString = "123";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);

        fts.setLineContinuationCharacter('\0');
        assertEquals(fts.getLineContinuationCharacter(), "\0");
        ft.setSettings(fts);
        fts = ft.getSettings();
        assertEquals(fts.getLineContinuationCharacter(), "\0");

        // override it
        fts.setLineContinuationCharacter('g');
        assertEquals(fts.getLineContinuationCharacter(), "g");
        ft.setSettings(fts);
        fts = ft.getSettings();
        assertEquals(fts.getLineContinuationCharacter(), "g");

        // do some stuff.
        ft.nextToken();
        ft.pushBack();
        ft.nextToken();
        ft.nextToken();
        ft.getLineNumber();
        // should be still the same
        fts = ft.getSettings();
        assertEquals(fts.getLineContinuationCharacter(), "g");
    }

    public void testNewLineInQuotes() {
        TokenizerSettings fts = new TokenizerSettings();
        fts.addDelimiterPattern(",", false, false, false);
        fts.addDelimiterPattern("\n", false, true, false);
        fts.addQuotePattern("\"", "\"");
        String inputString = "A,B,\"C\"\n\"\",\"first\nsecond\"\n\"\n\",\"KNIME\n\",\"\nData\",\"\nfoo\n\",D,F";

        StringReader stringReader = new StringReader(inputString);
        Tokenizer ft = new Tokenizer(stringReader);
        ft.setSettings(fts);

        // new line in a quoted string is not allowed by default
        String token = ft.nextToken();
        assertEquals(token, "A");
        token = ft.nextToken();
        assertEquals(token, "B");
        token = ft.nextToken();
        assertEquals(token, "C");
        token = ft.nextToken();
        assertEquals(token, "\n");
        token = ft.nextToken();
        assertEquals(token, "");
        try {
            token = ft.nextToken();
            // error: NL character in quoted string
            fail("new line character in a quoted string should cause an error by default");
        } catch (TokenizerException te) {
        	// required exception
        }

        // allow LF in quotes
        fts.allowLFinQuotes(true);
        stringReader = new StringReader(inputString);
        ft = new Tokenizer(stringReader);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "A");
        token = ft.nextToken();
        assertEquals(token, "B");
        token = ft.nextToken();
        assertEquals(token, "C");
        token = ft.nextToken();
        assertEquals(token, "\n");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "first\nsecond");
        token = ft.nextToken();
        assertEquals(token, "\n");  // delimiter
        assertFalse(ft.lastTokenWasQuoted());
        assertTrue(ft.lastTokenWasDelimiter());
        token = ft.nextToken();
        assertEquals(token, "\n");  // quoted NL
        assertTrue(ft.lastTokenWasQuoted());
        assertFalse(ft.lastTokenWasDelimiter());
        token = ft.nextToken();
        assertEquals(token, "KNIME\n");
        token = ft.nextToken();
        assertEquals(token, "\nData");
        token = ft.nextToken();
        assertEquals(token, "\nfoo\n");
        token = ft.nextToken();
        assertEquals(token, "D");
        token = ft.nextToken();
        assertEquals(token, "F");

        inputString = "A,B,C\r\n\",\", zweiter, \"\tA\tB\"\r\n\"erster\", zweiter, \"AB\"\r\n\"erster\", zweiter, \"\nA\nB\"\r\n";
        stringReader = new StringReader(inputString);
        ft = new Tokenizer(stringReader);
        ft.setSettings(fts);
        token = ft.nextToken();
        assertEquals(token, "A");
        token = ft.nextToken();
        assertEquals(token, "B");
        token = ft.nextToken();
        assertEquals(token, "C");
        token = ft.nextToken();
        assertEquals(token, "\n");  // delimiter
        assertTrue(ft.lastTokenWasDelimiter());
        token = ft.nextToken();
        assertEquals(token, ",");
        assertFalse(ft.lastTokenWasDelimiter());  // not the delimiter
        assertTrue(ft.lastTokenWasQuoted());
        token = ft.nextToken();
        assertEquals(token, " zweiter");
        token = ft.nextToken();
        assertEquals(token, " \tA\tB");
        token = ft.nextToken();
        assertEquals(token, "\n");  // delimiter
        assertTrue(ft.lastTokenWasDelimiter());
        token = ft.nextToken();
        assertEquals(token, "erster");
        token = ft.nextToken();
        assertEquals(token, " zweiter");
        token = ft.nextToken();
        assertEquals(token, " AB");
        token = ft.nextToken();
        assertEquals(token, "\n");  // delimiter
        assertTrue(ft.lastTokenWasDelimiter());
        token = ft.nextToken();
        assertEquals(token, "erster");
        token = ft.nextToken();
        assertEquals(token, " zweiter");
        token = ft.nextToken();
        assertEquals(token, " \nA\nB");
        token = ft.nextToken();
        assertEquals(token, "\n");  // delimiter
        assertTrue(ft.lastTokenWasDelimiter());
    }

    /**
     * tests if multiple different, but consecutive, delimiters are combined
     * correctly - and ensures that they are still included/returned in/as
     * token, if set so.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testCombineMultipleDelimiters() throws TokenizerException {

        String token;
        String inputString = "123 \t 234   345, 567";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);
        // we want whitespaces and comma to be a seperator. Also combinations.
        fts.addDelimiterPattern(" ", true, false, false);
        fts.addDelimiterPattern("\t", true, false, false);
        fts.addDelimiterPattern(",", false, false, false);
        fts.setCombineMultipleDelimiters(true);

        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "345");
        token = ft.nextToken();
        assertEquals(token, "567");
        token = ft.nextToken();
        assertNull(token);

        // same thing. This time we want the comma returned.
        strReader = new StringReader(inputString); /* "123 \t 234 345, 567" */
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        // we want whitespaces and comma to be a seperator. Also combinations.
        fts.addDelimiterPattern(" ", true, false, false);
        fts.addDelimiterPattern("\t", true, false, false);
        fts.addDelimiterPattern(",", false, true, false);
        fts.setCombineMultipleDelimiters(true);

        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "345");
        token = ft.nextToken();
        assertEquals(token, ",");
        token = ft.nextToken();
        assertEquals(token, "567");
        token = ft.nextToken();
        assertNull(token);

        // same thing. But the comma is to be included in the token.
        strReader = new StringReader(inputString); /* "123 \t 234 345, 567" */
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        // we want whitespaces and comma to be a seperator. Also combinations.
        fts.addDelimiterPattern(" ", true, false, false);
        fts.addDelimiterPattern("\t", true, false, false);
        fts.addDelimiterPattern(",", false, false, true);
        fts.setCombineMultipleDelimiters(true);

        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "345,");
        token = ft.nextToken();
        assertEquals(token, "567");
        token = ft.nextToken();
        assertNull(token);

        // now make sure we don't combine too many delimiters.
        inputString = "123 \t 234   345, \t,  567";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        // we want whitespaces and comma to be a seperator. Also combinations.
        fts.addDelimiterPattern(" ", true, false, false);
        fts.addDelimiterPattern("\t", true, false, false);
        fts.addDelimiterPattern(",", false, false, false);
        // don't combine the two commas, return an emtpy token inbetween
        fts.setCombineMultipleDelimiters(true);

        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "345");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "567");
        token = ft.nextToken();
        assertNull(token);

        // lets see what happens if we ust return two different delimiters
        inputString = "123 \t 234   345, \t;  567";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        // we want whitespaces and comma to be a seperator. Also combinations.
        fts.addDelimiterPattern(" ", true, false, false);
        fts.addDelimiterPattern("\t", true, false, false);
        fts.addDelimiterPattern(",", false, true, false);
        fts.addDelimiterPattern(";", false, true, false);

        // don't combine the comma with the semicolon.
        fts.setCombineMultipleDelimiters(true);

        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "345");
        token = ft.nextToken();
        assertEquals(token, ",");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, ";");
        token = ft.nextToken();
        assertEquals(token, "567");
        token = ft.nextToken();
        assertNull(token);
    }

    /**
     * ensures correct white space character support. Defined WS chars will be
     * ignored - if not inside quotes. Any character can be defined as WS.
     * @throws TokenizerException if somethings goes wrong.
     */
    public void testAddWhiteSpaceCharacter() throws TokenizerException {

        String token;
        String inputString = "123 \t 234   345, 567";
        StringReader strReader = new StringReader(inputString);
        TokenizerSettings fts = new TokenizerSettings();
        Tokenizer ft = new Tokenizer(strReader);
        fts.addDelimiterPattern(",", false, false, false);
        fts.addWhiteSpaceCharacter(' ');
        fts.addWhiteSpaceCharacter('\t');
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123 \t 234   345");
        token = ft.nextToken();
        assertEquals(token, "567");
        token = ft.nextToken();
        assertNull(token);


        // no ws chars - all spaces should appear
        inputString = "123 \t 234   345, 567";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addDelimiterPattern(",", false, false, false);
        fts.addDelimiterPattern("\t", false, false, false);
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123 ");
        token = ft.nextToken();
        assertEquals(token, " 234   345");
        token = ft.nextToken();
        assertEquals(token, " 567");
        token = ft.nextToken();
        assertNull(token);

        // space is a whitespace AND delimiter. Delimiter should win.
        inputString = "123  234 345";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addDelimiterPattern(" ", false, false, false);
        fts.addWhiteSpaceCharacter(' ');
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "345");
        token = ft.nextToken();
        assertNull(token);

        // define '_' and '=' and '-' as whitespace
        inputString = "=123=  _234_ =345= ---------------"
                + " ------1_2_3_4_5-------";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addDelimiterPattern(" ", false, false, false);
        fts.addWhiteSpaceCharacter('_');
        fts.addWhiteSpaceCharacter('=');
        fts.addWhiteSpaceCharacter('-');
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "345");
        token = ft.nextToken();
        assertEquals(token, "");
        token = ft.nextToken();
        assertEquals(token, "1_2_3_4_5");
        token = ft.nextToken();
        assertNull(token);

        // make sure whitespaces are not swallowed if quoted
        inputString = "\"123 \", \" 234\" ,  34 5 , \" 5 6 7 \"  , \"987 \" ";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addDelimiterPattern(",", false, false, false);
        fts.addQuotePattern("\"", "\"");
        fts.addWhiteSpaceCharacter(' ');
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123 ");
        token = ft.nextToken();
        assertEquals(token, " 234");
        token = ft.nextToken();
        assertEquals(token, "34 5");
        token = ft.nextToken();
        assertEquals(token, " 5 6 7 ");
        token = ft.nextToken();
        assertEquals(token, "987 ");
        token = ft.nextToken();
        assertNull(token);

        // test a delimiter that starts with a whitespace
        inputString = "123 -  234 -  34 - 5 - 5 6 7 -   ";
        strReader = new StringReader(inputString);
        fts = new TokenizerSettings();
        ft = new Tokenizer(strReader);
        fts.addDelimiterPattern(" - ", false, false, false);
        fts.addWhiteSpaceCharacter(' ');
        ft.setSettings(fts);

        token = ft.nextToken();
        assertEquals(token, "123");
        token = ft.nextToken();
        assertEquals(token, "234");
        token = ft.nextToken();
        assertEquals(token, "34");
        token = ft.nextToken();
        assertEquals(token, "5");
        token = ft.nextToken();
        assertEquals(token, "5 6 7");
        token = ft.nextToken();
        assertNull(token);
    }
} // TokenizerTest
