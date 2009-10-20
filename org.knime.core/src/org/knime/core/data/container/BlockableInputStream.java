/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
