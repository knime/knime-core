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
 *   15.05.2007 (thor): created
 */
package org.knime.base.util;

import java.io.IOException;
import java.io.Writer;

/**
 * This writer just swallows everything that is written to it.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public final class NullWriter extends Writer {
    /**
     * The singleton instance of the NullWriter.
     * @deprecated Do not use this public instance because Writers are
     * internally synchronized on themselves.
     */
    @Deprecated
    public static final NullWriter INSTANCE = new NullWriter();

    /**
     * Creates a new NullWriter.
     */
    public NullWriter() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Writer append(final char c) throws IOException {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Writer append(final CharSequence csq, final int start, final int end)
            throws IOException {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Writer append(final CharSequence csq) throws IOException {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final char[] cbuf) throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final int c) throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final String str, final int off, final int len)
            throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final String str) throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final char[] cbuf, final int off, final int len)
            throws IOException {
    }
}
