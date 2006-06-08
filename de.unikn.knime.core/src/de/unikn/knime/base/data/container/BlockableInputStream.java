/*  
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 */
package de.unikn.knime.base.data.container;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input Stream that wraps a given input stream and searches for terminated end
 * blocks. Such a block is typically a serialized <code>DataCell</code>.
 * It will simulate an eof file if it entcounters a block end. This class is 
 * intended to be used on streams written with the 
 * <code>BlockableOutStream</code> class.
 * 
 * @see de.unikn.knime.base.data.container.BlockableOutputStream
 * @author wiswedel, University of Konstanz
 */
final class BlockableInputStream extends InputStream {

    /** Input stream to wrap. */
    private final InputStream m_inStream;
    /** If an block end has been encountered, it will return -1 on subsequent
     * read() unless endBlock() is called.
     */
    private boolean m_simulateTerminate;

    /**
     * Inits the Stream.
     * @param inStream The stream to wrap. Must not be <code>null</code>.
     */
    BlockableInputStream(final InputStream inStream) {
        m_inStream = inStream;
        m_simulateTerminate = false;
    }

    /**
     * Reads next byte from stream and if it is an escape byte, it returns the
     * next following byte. If it is a terminate byte, it returns -1, i.e. eof.
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        if (m_simulateTerminate) {
            return -1;
        }
        int c = m_inStream.read();
        switch (c) {
        case BlockableOutputStream.TC_TERMINATE:
            m_simulateTerminate = true;
            return -1;
        case BlockableOutputStream.TC_ESCAPE:
            return m_inStream.read();
        default:
            return c;
        }
    }
    
    /** Finishes the current block, i.e. when the block end has already been
     * reached, releases the block and allows further read() operations. 
     * Otherwise it will subsequently call read() until the block end is 
     * reached.
     * @throws IOException If read() fails.
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

}
