/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
