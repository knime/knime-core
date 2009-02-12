/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   14.05.2007 (thor): created
 */
package org.knime.base.util;

import java.io.PrintStream;
import java.util.Locale;

/**
 * Very simple stream that just swallows everything that is written to it. This
 * class is a singleton, use the {@link #INSTANCE} field to get access to the
 * its single instance.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class NullPrintStream extends PrintStream {
    /**
     * The singleton instance of this NullPrintStream.
     */
    public static final NullPrintStream INSTANCE = new NullPrintStream();

    private NullPrintStream() {
        super(System.out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream append(final char c) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream append(final CharSequence csq, final int start,
            final int end) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream append(final CharSequence csq) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkError() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream format(final Locale l, final String format,
            final Object... args) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream format(final String format, final Object... args) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final boolean b) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final char c) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final char[] s) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final double d) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final float f) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final int i) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final long l) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final Object obj) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final String s) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream printf(final Locale l, final String format,
            final Object... args) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintStream printf(final String format, final Object... args) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println(final boolean x) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println(final char x) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println(final char[] x) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println(final double x) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println(final float x) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println(final int x) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println(final long x) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println(final Object x) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println(final String x) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte[] buf, final int off, final int len) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final int b) {
    }
}
