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
 * -------------------------------------------------------------------
 *
 * History
 *   27.04.2005 (ohl): created
 */
package org.knime.base.node.io.filereader;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.knime.base.node.io.filereader.FileAnalyzer.HeaderHelper;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.tokenizer.Quote;

import junit.framework.TestCase;

/**
 *
 * @author Peter Ohl, University of Konstanz
 */
public class FileAnalyzerTest extends TestCase {

    /**
     * tests if column headers are correctly detected. The file has col headers,
     * if the first line contains a non-empty string in each column that has an
     * increasing number at its end (increasing over the different cols).
     */
    public void testColHeader() {

        URL url;
        FileReaderNodeSettings settings;
        FileReaderNodeSettings analSettings;

        try {
            /*
             * give it some nice column headers in the first line
             */
            url = initTempFile("column1,column2,column3,column4\n"
                    + "foo,poo,moo,zoo\n" + "oof,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertTrue(analSettings.getFileHasColumnHeaders());
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            /*
             * just numbers will be considered col headers
             */
            url = initTempFile("1,2,3,4\n" + "foo,poo,moo,zoo\n"
                    + "oof,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertTrue(analSettings.getFileHasColumnHeaders());
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            /*
             * numbers not increasing: no headers
             */
            url = initTempFile("1,2,5,4\n" + "foo,poo,moo,zoo\n"
                    + "oof,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasColumnHeaders());
            /*
             * different prefixes: no headers
             */
            url = initTempFile("col1,col2,foo3,col4\n" + "foo,poo,moo,zoo\n"
                    + "oof,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasColumnHeaders());
            /*
             * different prefixes: no headers
             */
            url = initTempFile("col1,2,foo3,4\n" + "foo,poo,moo,zoo\n"
                    + "oof,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasColumnHeaders());
            /*
             * different prefixes: no headers
             */
            url = initTempFile("1,col2,3,c4\n" + "foo,poo,moo,zoo\n"
                    + "oof,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasColumnHeaders());
            /*
             * empty cell: no headers
             */
            url = initTempFile("col1,,col3,col4\n" + "foo,poo,moo,zoo\n"
                    + "oof,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasColumnHeaders());
            /*
             * empty cell: no headers
             */
            url = initTempFile(",col1,col3,col4\n" + "foo,poo,moo,zoo\n"
                    + "oof,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasColumnHeaders());
            /*
             * don't choke on a huge index
             */
            url = initTempFile("col9999999999999999999999999999999999999999999,"
                    + "col9999999999999999999999999999999999999999999999999998,"
                    + "col3,"
                    + "col999999999999999999999999999999999999999999999999997\n"
                    + "foo,poo,moo,zoo\n" + "oof,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasColumnHeaders());

        } catch (IOException ioe) {
            // if this goes off the temp file couldn't be created.
            assertTrue(false);
        }
    }

    /**
     * makes sure double quotes and single quotes are only supported when they
     * appear in even numbers.
     */
    public void testQuote() {

        URL url;
        FileReaderNodeSettings settings;
        FileReaderNodeSettings analSettings;

        try {
            /*
             * nice quoting
             */
            url = initTempFile("\"col1\",'col2',col3,col4\n"
                    + "\"foo\",poo,\"moo\",zoo\n" + "oof,'oo p',oom,' ooz '");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertTrue(analSettings.getFileHasColumnHeaders());
            assertEquals(analSettings.getNumberOfColumns(), 4);
            Vector<Quote> quotes = analSettings.getAllQuotes();
            // we support '"' and '
            assertEquals(quotes.size(), 2);
            assertEquals((quotes.get(0)).getLeft(), "\"");
            assertEquals((quotes.get(0)).getRight(), "\"");
            assertTrue((quotes.get(0)).hasEscapeChar());
            assertEquals((quotes.get(0)).getEscape(), '\\');
            assertEquals((quotes.get(1)).getLeft(), "'");
            assertEquals((quotes.get(1)).getRight(), "'");
            assertTrue((quotes.get(1)).hasEscapeChar());
            assertEquals((quotes.get(1)).getEscape(), '\\');

            /*
             * the tick (') is part of the data - don't consider it a quote (it
             * must show up an odd number of times...)
             */
            url = initTempFile("\"col1\",col2,col3,col4\n"
                    + "\"foo\",poo,\"moo\",zoo\n" + "oo'f,o'op,o'om,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertTrue(analSettings.getFileHasColumnHeaders());
            assertEquals(analSettings.getNumberOfColumns(), 4);
            quotes = analSettings.getAllQuotes();
            // we support '"' still
            assertEquals(quotes.size(), 1);
            assertEquals(quotes.get(0).getLeft(), "\"");
            assertEquals((quotes.get(0)).getRight(), "\"");
            assertTrue((quotes.get(0)).hasEscapeChar());
            assertEquals((quotes.get(0)).getEscape(), '\\');
            /*
             * there is also a single double quote in the data
             */
            url = initTempFile("\"col1,col2,col3,col4\n"
                    + "fo\"o,poo,moo,zoo\n" + "oo'f,o'op,o'om,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertEquals(analSettings.getAllQuotes().size(), 0);

            /*
             * don't stumble over escaped quotes
             */
            url = initTempFile("col1,col2,col3,col4\n"
                    + "\"foo\",\"po\\\"o\",moo,zoo\n" + "oo'f,o'op,o'om,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertEquals(analSettings.getNumberOfColumns(), 4);
            // we must support the double quotes with the escape char
            quotes = analSettings.getAllQuotes();
            assertEquals(quotes.size(), 1);
            assertEquals((quotes.get(0)).getLeft(), "\"");
            assertEquals((quotes.get(0)).getRight(), "\"");
            assertTrue((quotes.get(0)).hasEscapeChar());
            assertEquals((quotes.get(0)).getEscape(), '\\');

        } catch (IOException ioe) {
            // if this goes off the temp file couldn't be created.
            assertTrue(false);
        }

    }

    /**
     * tests if row headers are correctly detected. The file has row headers, if
     * the first column of each row contains a non-empty string in each column
     * that consists of the same prefix with an increasing number following
     * (increasing over the different rows).
     */
    public void testRowHeader() {

        URL url;
        FileReaderNodeSettings settings;
        FileReaderNodeSettings analSettings;

        try {
            /*
             * test nice row header
             */
            url = initTempFile("\"\",col0,col1,col2\n" + "row1,poo,moo,zoo\n"
                    + "row2,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 3);
            assertTrue(analSettings.getFileHasRowHeaders());

            /*
             * again nice row headers.
             */
            url = initTempFile(",col0,col1,col2\n" + "row1,poo,moo,zoo\n"
                    + "row2,poo,moo,zoo\n" + "row3,poo,moo,zoo\n"
                    + "row4,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 3);
            assertTrue(analSettings.getFileHasRowHeaders());

            /*
             * these are not row headers.
             */
            url = initTempFile(",col0,col1,col2\n" + "row1,poo,moo,zoo\n"
                    + "row2,poo,moo,zoo\n" + "row2.5,poo,moo,zoo\n"
                    + "row3,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasRowHeaders());

            /*
             * don't choke on a huge indices
             */
            url = initTempFile("\"\",col0,col1,col2\n"
                    + "row9999999999999999999999999999999999999999999999999998,"
                    + "poo,moo,zoo\n"
                    + "row9999999999999999999999999999999999999999999999999999,"
                    + "oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            // just to be on the safe side
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasRowHeaders());


            /*
             * these are not row headers (no increasing index)
             */
            url = initTempFile(",col0,col1,col2\n" + "row1,poo,moo,zoo\n"
                    + "row2,poo,moo,zoo\n" + "row5,poo,moo,zoo\n"
                    + "row3,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasRowHeaders());

            /*
             * these are not row headers (not the same prefix)
             */
            url = initTempFile(",col0,col1,col2\n" + "row1,poo,moo,zoo\n"
                    + "rom2,poo,moo,zoo\n" + "row3,poo,moo,zoo\n"
                    + "row4,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasRowHeaders());

            /*
             * these are not row headers (not the same prefix)
             */
            url = initTempFile(",col0,col1,col2\n" + "row1,poo,moo,zoo\n"
                    + "low2,poo,moo,zoo\n" + "row3,poo,moo,zoo\n"
                    + "row4,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasRowHeaders());

            /*
             * these are not row headers (not the same prefix)
             */
            url = initTempFile(",col0,col1,col2\n" + "row1,poo,moo,zoo\n"
                    + "row 2,poo,moo,zoo\n" + "row3,poo,moo,zoo\n"
                    + "row4,oop,oom,ooz");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertEquals(analSettings.getNumberOfColumns(), 4);
            assertFalse(analSettings.getFileHasRowHeaders());

        } catch (IOException ioe) {
            // if this goes off the temp file couldn't be created.
            assertTrue(false);
        }
    }

    /**
     * that's a file causing the analyze to puke and die. Lets make sure it
     * doesn't cause any harm in the future.
     */
    public void testBug107() {

        URL url;
        FileReaderNodeSettings settings;
        FileReaderNodeSettings analSettings;

        try {
            /*
             * test nice row header
             */
            url = initTempFile("Covariance: =\n"
            + " 0.6856935123 -0.0424340045 1.2743154362 0.5162706935\n"
            + " -0.0424340045 0.1899794183 -0.3296563758 -0.1216393736\n"
            + " 1.2743154362 -0.3296563758 3.1162778523 1.2956093960\n"
            + " 0.5162706935 -0.1216393736 1.2956093960 0.5810062640\n"
            + "\n"
            + "\n"
            + "V:  =\n"
            + " -0.3154871929 -0.5820298513 -0.6565887713 0.3613865918\n"
            + " 0.3197231037 0.5979108301 -0.7301614348 -0.0845225141\n"
            + " 0.4798389870 0.0762360758 0.1733726628 0.8566706059\n"
            + " -0.7536574253 0.5458314320 0.0754810199 0.3582891972\n"
            + "\n"
            + "\n"
            + "sorted V:  =\n"
            + " 0.3613865918 -0.6565887713 -0.5820298513 -0.3154871929\n"
            + " -0.0845225141 -0.7301614348 0.5979108301 0.3197231037\n"
            + " 0.8566706059 0.1733726628 0.0762360758 0.4798389870\n"
            + " 0.3582891972 0.0754810199 0.5458314320 -0.7536574253");
            settings = new FileReaderNodeSettings();
            settings.setDataFileLocationAndUpdateTableName(url);
            analSettings = FileAnalyzer.analyze(settings, null);
            assertNotNull(analSettings);
        } catch (IOException ioe) {
            // if this goes off the temp file couldn't be created.
            assertTrue(false);
        }

    }

    /**
     * tests the helper class that detects consecutive headers
     */
    public void testHeaderHelperClass1() {
        /*
         * pure numerical headers are okay, as long as they are increasing
         */
        FileAnalyzer.HeaderHelper hh =
            HeaderHelper.extractPrefixAndIndexFromHeader("123");
        assertTrue(hh.testNextHeader("124"));
        assertFalse(hh.testNextHeader("124"));
        assertTrue(hh.testNextHeader("10456643"));
        assertFalse(hh.testNextHeader(""));
        assertFalse(hh.testNextHeader("Foo10456644"));
        assertTrue(hh.testNextHeader("10456644"));

    }

    /**
     * tests the helper class that detects consecutive headers
     */
    public void testHeaderHelperClass2() {
        /*
         * prefixed headers
         */
        FileAnalyzer.HeaderHelper hh =
            HeaderHelper.extractPrefixAndIndexFromHeader("Foo123");
        assertTrue(hh.testNextHeader("Foo124"));
        assertFalse(hh.testNextHeader("Foo124"));

        assertTrue(hh.testNextHeader("Foo125"));
        assertFalse(hh.testNextHeader("Fo126"));
        assertFalse(hh.testNextHeader("127"));

        assertTrue(hh.testNextHeader("Foo128"));
        assertFalse(hh.testNextHeader("Fool129"));

        assertTrue(hh.testNextHeader("Foo10456643"));
        assertFalse(hh.testNextHeader("Foo"));
        assertFalse(hh.testNextHeader("Fop10456644"));
        assertTrue(hh.testNextHeader("Foo10456644"));
    }

    /**
     * tests the helper class that detects consecutive headers
     */
    public void testHeaderHelperClass3() {
        /*
         * headers without index are not okay
         */
        assertEquals(HeaderHelper.extractPrefixAndIndexFromHeader("Foo"), null);
        assertEquals(HeaderHelper.extractPrefixAndIndexFromHeader(""), null);
        assertEquals(HeaderHelper.extractPrefixAndIndexFromHeader(null), null);
        assertEquals(HeaderHelper.extractPrefixAndIndexFromHeader("Foo17Oops"),
                null);

    }

    /**
     * Creates a temp file, writes the string into it and closes it again. It
     * will return an URL to that file then.
     *
     * @param contents the string written into the newly created temp file.
     * @return url to the created file
     * @throws IOException If the file be instantiated or written.
     */
    private URL initTempFile(final String contents) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmssSSS");
        String date = sdf.format(new Date());
        String fileName = "knime_fileanalyzer_test_" + date + "_";
        String suffix = ".dat";

        File tempFile = File.createTempFile(fileName, suffix);
        tempFile.deleteOnExit();

        FileWriter out = new FileWriter(tempFile);
        out.write(contents);
        out.close();

        return tempFile.toURI().toURL();

    } // initTempFile()

    /**
     * Testcase for bug AP-4163.
     *
     * @throws Exception if an error occurs
     */
    public void testDatesWithThousandSeparator() throws Exception {
        // URL url = initTempFile("1.000,5;24.05.2015\n");
        URL url = initTempFile("1.000,5;24.05.2015\n1.000,5;24.05.2015\n");
        FileReaderNodeSettings settings = new FileReaderNodeSettings();
        settings.setFileHasRowHeadersUserSet(true);
        settings.setFileHasRowHeaders(false);
        settings.setFileHasColumnHeadersUserSet(true);
        settings.setFileHasColumnHeaders(false);
        settings.setDecimalSeparator(',');
        settings.setThousandsSeparator('.');
        settings.setDataFileLocationAndUpdateTableName(url);
        FileReaderNodeSettings analSettings = FileAnalyzer.analyze(settings, null);

        assertThat("Unexpected number of columns detected", analSettings.getColumnProperties().size(), is(2));
        assertThat("Unexpected guessed type for double column",
            analSettings.getColumnProperties().get(0).getColumnSpec().getType(), is(DoubleCell.TYPE));
        assertThat("Unexpected guessed type for string (date) column",
            analSettings.getColumnProperties().get(1).getColumnSpec().getType(), is(StringCell.TYPE));
    }

    /**
     * Testcase for regression AP-7645.
     *
     * Checks that all kinds of numbers are correctly parsed as doubles.
     *
     * @throws Exception if an errors occurs
     */
    public void testNumbersWithThousandseparator() throws Exception {
        String withThousandSeparator = "1\n-1\n1\n1,123,456\n-1,123,456\n+1,123,456\n" +
                "1.0\n+1.0\n-1.0\n1,123,456.789\n-1,123,456.789\n+1,123,456.789\n" +
                ".5\n-.5\n+.5\n" +
                "1.\n-1.\n+1.\n1,123,456.\n-1,123,456.\n+1,123,456.\n" +
                "2e2\n-2e2\n+2e2\n" +
                ".05e2\n-.05e2\n+.05e2\n" +
                "2.e2\n-2.e2\n+2.e2\n" +
                "2.05e2\n-2.05e2\n+2.05e2\n" +
                "1,123,456e2\n-1,123,456e2\n+1,123,456e2\n" +
                "1,123,456.e2\n-1,123,456.e2\n+1,123,456.e2\n" +
                "1,123,456.789e2\n-1,123,456.789e2\n+1,123,456.789e2\n" +
                "2f\n-2f\n+2f\n2.f\n-2.f\n+2.f\n" +
                "0.2f\n-0.2f\n+0.2f\n.2f\n-.2f\n+.2f\n" +
                "1,123,456f\n-1,123,456f\n+1,123,456f\n" +
                "1,123,456.f\n-1,123,456.f\n+1,123,456f\n" +
                "1,123,456.05f\n-1,123,456.05f\n+1,123,456.05f\n" +
                "2d\n-2d\n+2d\n2.d\n-2.d\n+2d\n" +
                "0.2d\n-0.2d\n+0.2d\n.2d\n-.2d\n+.2d\n" +
                "1,123,456d\n-1,123,456d\n+1,123,456d\n" +
                "1,123,456.d\n-1,123,456.d\n+1,123,456.d\n" +
                "1,123,456.05d\n-1,123,456.05d\n+1,123,456.05d\n" +
                "1,123,456.05e-2f\n-1,123,456.05e2f\n1,123,456.05e+2f" +
                "1,123,456.05e-2d\n-1,123,456.05e2d\n1,123,456.05e+2d";
        URL separatorDoc = initTempFile(withThousandSeparator);

        FileReaderNodeSettings settings = new FileReaderNodeSettings();
        settings.setFileHasRowHeadersUserSet(true);
        settings.setFileHasRowHeaders(false);
        settings.setFileHasColumnHeadersUserSet(true);
        settings.setFileHasColumnHeaders(false);
        settings.setDecimalSeparator('.');
        settings.setThousandsSeparator(',');
        settings.setDataFileLocationAndUpdateTableName(separatorDoc);
        FileReaderNodeSettings analSettings = FileAnalyzer.analyze(settings, null);

        assertThat("Unexpected number of columns detected", analSettings.getColumnProperties().size(), is(1));
        assertThat("Unexpected guessed type for double column",
            analSettings.getColumnProperties().get(0).getColumnSpec().getType(), is(DoubleCell.TYPE));
    }
}
