/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */
package org.knime.core.data.container;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input Stream that wraps a given input stream and searches for terminated end
 * blocks. Such a block is typically a serialized
 * {@link org.knime.core.data.DataCell}. It will simulate an eof file if
 * it encounters a block end. This class is intended to be used on streams
 * written with the
 * {@link org.knime.core.data.container.BlockableOutputStream} class.
 * 
 * @see org.knime.core.data.container.BlockableOutputStream
 * @author wiswedel, University of Konstanz
 */
final class BlockableInputStream 
    extends InputStream implements KNIMEStreamConstants {

    /** Input stream to wrap. */
    private final InputStream m_inStream;

    /**
     * If an block end has been encountered, it will return -1 on subsequent
     * {@link #read()} unless {@link #endBlock()} is called.
     */
    private boolean m_simulateTerminate;

    /**
     * Inits the Stream.
     * 
     * @param inStream the stream to wrap; must not be <code>null</code>
     */
    BlockableInputStream(final InputStream inStream) {
        m_inStream = inStream;
        m_simulateTerminate = false;
    }

    /**
     * Reads next byte from stream and if it is an escape byte, it returns the
     * next following byte. If it is a terminate byte, it returns -1, i.e. eof.
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        if (m_simulateTerminate) {
            return -1;
        }
        int c = m_inStream.read();
        switch (c) {
        case TC_TERMINATE:
            m_simulateTerminate = true;
            return -1;
        case TC_ESCAPE:
            return m_inStream.read();
        default:
            return c;
        }
    }

    /**
     * Finishes the current block, i.e. when the block end has already been
     * reached, releases the block and allows further {@link #read()}
     * operations. Otherwise it will subsequently call {@link #read()} until the
     * block end is reached.
     * 
     * @throws IOException if {@link #read()} fails
     */
    public void endBlock() throws IOException {
        // read until eof or terminate
        if (m_simulateTerminate) {
            m_simulateTerminate = false;
            return;
        }
        int c;
        do {
            c = read();
        } while (c >= 0 && !m_simulateTerminate);
        m_simulateTerminate = false;
    }
    
    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        m_inStream.close();
    }

}
