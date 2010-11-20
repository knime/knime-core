/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 25, 2007 (ohl): created
 */
package org.knime.base.node.io.filereader;

import org.knime.base.node.util.BufferedFileReader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

/**
 *
 * @author ohl, University of Konstanz
 */
public class BufferedFileReaderTest extends TestCase {

    private BufferedFileReader reader = null;

    private BufferedReader correct = null;

    /**
     * tests if the read method of the BufferedFileReader works the same as the
     * one of the BufferedReader.
     *
     * @throws IOException if an I/O error occurred.
     */
    public void testRead() throws IOException {
        readEqualTest("");
        reader.read();
        readEqualTest("\n");
        reader.read();

        readEqualTest("a\nb\nc\n"); // a LF b LF c LF
        reader.read();
        readEqualTest("aa\nb\nc\n"); // aa LF b LF c LF
        reader.read();
        readEqualTest("\nb\nc\n"); // empty line at the beginning
        reader.read();
        readEqualTest("a\nb\nc"); // no LF at the end.
        reader.read();
        readEqualTest("a\nb\n\nc"); // empty line in the middle
        reader.read();

        readEqualTest("a\r\nb\r\nc\r\n"); // \r\n = CR LF
        reader.read();
        readEqualTest("\r\nb\r\nc\r\n"); // empty line at the beginning
        reader.read();
        readEqualTest("a\r\nb\r\nc"); // no LF at the end.
        reader.read();
        readEqualTest("a\r\nb\r\n\r\nc"); // empty line in the middle
        reader.read();

        reader.close();
        try {
            reader.read();
            fail(); // reading after close should throw an exception
        } catch (final IOException ioe) {
            // nice catch
        }

    }

    /**
     * tests if the readLine method of the BufferedFileReader works the same as
     * the one of the BufferedReader.
     *
     * @throws IOException if an I/O error occurred.
     */
    public void testReadLine() throws IOException {
        readLineEqualTest("");
        reader.readLine();
        readLineEqualTest("\n");
        reader.readLine();

        readLineEqualTest("a\nb\nc\n"); // a LF b LF c LF
        reader.readLine();
        readLineEqualTest("aa\nb\nc\n"); // aa LF b LF c LF
        reader.readLine();
        readLineEqualTest("\nb\nc\n"); // Empty line at the beginning
        reader.readLine();
        readLineEqualTest("a\nb\nc"); // no LF at the end.
        reader.readLine();
        readLineEqualTest("a\nb\n\nc"); // Empty line in the middle
        reader.readLine();

        readLineEqualTest("a\r\nb\r\nc\r\n"); // \r\n = CR LF
        reader.readLine();
        readLineEqualTest("\r\nb\r\nc\r\n"); // empty line at the beginning
        reader.readLine();
        readLineEqualTest("a\r\nb\r\nc"); // no LF at the end.
        reader.readLine();
        readLineEqualTest("a\r\nb\r\n\r\nc"); // empty line in the middle
        reader.readLine();

        reader.close();
        try {
            reader.readLine();
            fail(); // reading after close should throw an exception
        } catch (final IOException ioe) {
            // nice catch
        }

    }

    /**
     * tests if the read(char[]) method of the BufferedFileReader works the same
     * as the one of the BufferedReader.
     *
     * @throws IOException if an I/O error occurred.
     */
    public void testReadArray() throws IOException {

        // check the zero character reading
        initReaders("foo");
        final int r = reader.read(new char[7], 3, 0);
        assertEquals(r, 0);

        // this does a one character to "length" character reading:
        readArrayEqualTest("");
        reader.read(new char[17]);

        readArrayEqualTest("\n");
        reader.read(new char[17]);

        readArrayEqualTest("a\nb\nc\n"); // a LF b LF c LF
        reader.read(new char[17]);
        readArrayEqualTest("aa\nb\nc\n"); // aa LF b LF c LF
        reader.read(new char[17]);
        readArrayEqualTest("\nb\nc\n"); // emtpy line at the beginning
        reader.read(new char[17]);
        readArrayEqualTest("a\nb\nc"); // no LF at the end.
        reader.read(new char[17]);
        readArrayEqualTest("a\nb\n\nc"); // empty line in the middle
        reader.read(new char[17]);

        readArrayEqualTest("a\r\nb\r\nc\r\n"); // \r\n = CR LF
        reader.read(new char[17]);
        readArrayEqualTest("\r\nb\r\nc\r\n"); // emtpy line at the beginning
        reader.read(new char[17]);
        readArrayEqualTest("a\r\nb\r\nc"); // no LF at the end.
        reader.read(new char[17]);
        readArrayEqualTest("a\r\nb\r\n\r\nc"); // empty line in the middle
        reader.read(new char[17]);

        reader.close();
        try {
            reader.read(new char[17]);
            fail(); // reading after close should throw an exception
        } catch (final IOException ioe) {
            // nice catch
        }

    }

    /**
     * Tests if the reader always returns the entire current line.
     *
     * @throws IOException if it does.
     */
    public void testGetCurrentLine() throws IOException {

        initReaders("foo");

        // before we read anything the line is empty
        assertEquals(reader.getCurrentLine(), "");
        reader.read();
        assertEquals(reader.getCurrentLine(), "foo");

        initReaders("");
        reader.read();
        assertEquals(reader.getCurrentLine(), "");

        initReaders("\n");
        reader.read();
        assertEquals(reader.getCurrentLine(), "");
        reader.read();
        assertEquals(reader.getCurrentLine(), "");
        reader.read();
        assertEquals(reader.getCurrentLine(), "");

        initReaders("a\nb\nc\n"); // a LF b LF c LF
        assertEquals(reader.getCurrentLine(), "");
        assertEquals(reader.getCurrentLineNumber(), 0);
        assertEquals(reader.read(), 'a');
        assertEquals(reader.getCurrentLine(), "a");
        assertEquals(reader.getCurrentLineNumber(), 1);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "a");
        assertEquals(reader.getCurrentLineNumber(), 1);
        assertEquals(reader.read(), 'b');
        assertEquals(reader.getCurrentLine(), "b");
        assertEquals(reader.getCurrentLineNumber(), 2);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "b");
        assertEquals(reader.getCurrentLineNumber(), 2);
        assertEquals(reader.read(), 'c');
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), -1);
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), -1);
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);

        initReaders("aa\nb\nc\n"); // aa LF b LF c LF
        assertEquals(reader.getCurrentLine(), "");
        assertEquals(reader.getCurrentLineNumber(), 0);
        assertEquals(reader.read(), 'a');
        assertEquals(reader.getCurrentLine(), "aa");
        assertEquals(reader.getCurrentLineNumber(), 1);
        assertEquals(reader.read(), 'a');
        assertEquals(reader.getCurrentLine(), "aa");
        assertEquals(reader.getCurrentLineNumber(), 1);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "aa");
        assertEquals(reader.getCurrentLineNumber(), 1);
        assertEquals(reader.read(), 'b');
        assertEquals(reader.getCurrentLine(), "b");
        assertEquals(reader.getCurrentLineNumber(), 2);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "b");
        assertEquals(reader.getCurrentLineNumber(), 2);
        assertEquals(reader.read(), 'c');
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), -1);
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), -1);
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);

        initReaders("\nb\nc\n"); // emtpy line at the beginning
        assertEquals(reader.getCurrentLine(), "");
        assertEquals(reader.getCurrentLineNumber(), 0);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "");
        assertEquals(reader.getCurrentLineNumber(), 1);
        assertEquals(reader.read(), 'b');
        assertEquals(reader.getCurrentLine(), "b");
        assertEquals(reader.getCurrentLineNumber(), 2);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "b");
        assertEquals(reader.getCurrentLineNumber(), 2);
        assertEquals(reader.read(), 'c');
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), -1);
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), -1);
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);

        initReaders("a\nb\nc"); // no LF at the end
        assertEquals(reader.getCurrentLine(), "");
        assertEquals(reader.getCurrentLineNumber(), 0);
        assertEquals(reader.read(), 'a');
        assertEquals(reader.getCurrentLine(), "a");
        assertEquals(reader.getCurrentLineNumber(), 1);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "a");
        assertEquals(reader.getCurrentLineNumber(), 1);
        assertEquals(reader.read(), 'b');
        assertEquals(reader.getCurrentLine(), "b");
        assertEquals(reader.getCurrentLineNumber(), 2);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "b");
        assertEquals(reader.getCurrentLineNumber(), 2);
        assertEquals(reader.read(), 'c');
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), -1);
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), -1);
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);

        initReaders("a\n\nc\n"); // emtpy line in the middle
        assertEquals(reader.getCurrentLine(), "");
        assertEquals(reader.getCurrentLineNumber(), 0);
        assertEquals(reader.read(), 'a');
        assertEquals(reader.getCurrentLine(), "a");
        assertEquals(reader.getCurrentLineNumber(), 1);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "a");
        assertEquals(reader.getCurrentLineNumber(), 1);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "");
        assertEquals(reader.getCurrentLineNumber(), 2);
        assertEquals(reader.read(), 'c');
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), '\n');
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), -1);
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);
        assertEquals(reader.read(), -1);
        assertEquals(reader.getCurrentLine(), "c");
        assertEquals(reader.getCurrentLineNumber(), 3);

        reader.close();
        try {
            reader.getCurrentLine();
            fail(); // reading after close should throw an exception
        } catch (final IOException ioe) {
            // nice catch
        }
        try {
            reader.getCurrentLineNumber();
            fail(); // reading after close should throw an exception
        } catch (final IOException ioe) {
            // nice catch
        }


    }

    /**
     * tests if skip works. Works across multiple lines. Works behind EOF, etc.
     * @throws IOException if it does.
     */
    public void testSkip() throws IOException {


        skipEqualTest("");
        skipEqualTest("\n");
        skipEqualTest("\n\r");


        skipEqualTest("a\nb\nc\n"); // a LF b LF c LF
        skipEqualTest("aa\nb\nc\n"); // aa LF b LF c LF
        skipEqualTest("\nb\nc\n"); // empty line at the beginning
        skipEqualTest("a\nb\nc"); // no LF at the end.
        skipEqualTest("a\nb\n\nc"); // empty line in the middle

        skipEqualTest("a\r\nb\r\nc\r\n"); // \r\n = CR LF
        skipEqualTest("\r\nb\r\nc\r\n"); // empty line at the beginning
        skipEqualTest("a\r\nb\r\nc"); // no LF at the end.
        skipEqualTest("a\r\nb\r\n\r\nc"); // empty line in the middle

        // see if getCurrentLine works with skip
        initReaders("ab\ncd\nef");
        reader.skip(1); // skips a
        assertEquals(reader.getCurrentLine(), "ab");
        assertEquals(reader.getCurrentLineNumber(), 1);
        reader.skip(1); // skips b
        assertEquals(reader.getCurrentLine(), "ab");
        assertEquals(reader.getCurrentLineNumber(), 1);
        reader.skip(1); // skips \n
        assertEquals(reader.getCurrentLine(), "ab");
        assertEquals(reader.getCurrentLineNumber(), 1);
        reader.skip(1); // skips c
        assertEquals(reader.getCurrentLine(), "cd");
        assertEquals(reader.getCurrentLineNumber(), 2);
        reader.skip(1); // skips d
        assertEquals(reader.getCurrentLine(), "cd");
        assertEquals(reader.getCurrentLineNumber(), 2);
        reader.skip(1); // skips \n
        assertEquals(reader.getCurrentLine(), "cd");
        assertEquals(reader.getCurrentLineNumber(), 2);
        reader.skip(1); // skips e
        assertEquals(reader.getCurrentLine(), "ef");
        assertEquals(reader.getCurrentLineNumber(), 3);
        reader.skip(1); // skips f
        assertEquals(reader.getCurrentLine(), "ef");
        assertEquals(reader.getCurrentLineNumber(), 3);
        reader.skip(1); // skips nothing
        assertEquals(reader.getCurrentLine(), "ef");
        assertEquals(reader.getCurrentLineNumber(), 3);




        reader.close();
        try {
            reader.skip(17);
            fail(); // reading after close should throw an exception
        } catch (final IOException ioe) {
            // nice catch
        }


    }

    /*
     * sets the given string in both readers, reads from them, and checks if the
     * read data is the same for both readers.
     */
    private void readEqualTest(final String s) throws IOException {

        initReaders(s);

        int r;
        int c;
        do {
            r = reader.read();
            c = correct.read();
            assertEquals(r, c);
        } while (r != -1);

    }

    /*
     * sets the given string in both readers, reads from them, and checks if the
     * read data is the same for both readers.
     */
    private void readLineEqualTest(final String s) throws IOException {

        initReaders(s);

        String r;
        String c;
        do {
            r = reader.readLine();
            c = correct.readLine();
            assertEquals(r, c);
        } while (r != null);

    }

    private void readArrayEqualTest(final String s) throws IOException {

        for (int c = 1; c <= s.length(); c++) {
            readArrayEqualTest(s, c);
        }
    }

    /*
     * sets the given string in both readers, reads from them, and checks if the
     * read data is the same for both readers.
     */
    private void readArrayEqualTest(final String s, final int count)
            throws IOException {

        initReaders(s);

        char[] rr = new char[count];
        char[] cc = new char[count];
        int r;
        int c;
        do {
            r = reader.read(rr);
            c = correct.read(cc);
            assertEquals(r, c);
            for (int i = 0; i < count; i++) {
                assertEquals(rr[i], cc[i]);
            }
        } while (r != -1);

        // do the same thing with offset and len
        final int off = 3;
        initReaders(s);

        rr = new char[s.length() + off];
        cc = new char[s.length() + off];
        do {
            r = reader.read(rr, off, count);
            c = correct.read(cc, off, count);
            assertEquals(r, c);
            for (int i = 0; i < count; i++) {
                assertEquals(rr[i], cc[i]);
            }
        } while (r != -1);
    }

    /*
     * skips a lot.
     */
    private void skipEqualTest(final String s) throws IOException {
        for (int i = 0; i < s.length() + 2; i++) {
            skipEqualTest(s, i);
        }
    }

    /*
     * initializes the readers with the string, reads one byte, skips n bytes
     * and reads one character. Tests if the characters returned by the two
     * readers are equal.
     */
    private void skipEqualTest(final String s, final int n) throws IOException {

        initReaders(s);

        // skip n bytes
        assertEquals(reader.skip(n), correct.skip(n));

        // read one more byte
        assertEquals(reader.read(), correct.read());


        initReaders(s);

        // read one byte
        assertEquals(reader.read(), correct.read());

        // skip n bytes
        assertEquals(reader.skip(n), correct.skip(n));

        // read one more byte
        assertEquals(reader.read(), correct.read());
    }

    /**
     * Places the specified content into the two global readers.
     *
     * @param content the string to place into the two global readers.
     */
    private void initReaders(final String content) {

        reader =
                BufferedFileReader.createNewReader(new ByteArrayInputStream(
                        content.getBytes()));
        correct = new BufferedReader(new StringReader(content));
    }

}
