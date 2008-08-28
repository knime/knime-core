/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Aug 27, 2008 (wiswedel): created
 */
package org.knime.core.node.port;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zip stream that is used to persist objects of class {@link PortObjectSpec}. 
 * Its current implementation offers the functionality provided by an 
 * {@link ZipOutputStream}; it is a separate class to enable future 
 * customization (for instance by adding methods that are needed in future
 * versions of KNIME).
 * 
 * <p>This class is not meant to be instantiated by ordinary node 
 * implementations. This class may change without notice. 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PortObjectSpecZipOutputStream extends ZipOutputStream {
    
    private boolean m_hasEntries = false;
    
    /** Delegates to underlying output stream.
     * @param outStream To write to.
     * @see ZipOutputStream#ZipOutputStream(OutputStream)
     */
    public PortObjectSpecZipOutputStream(final OutputStream outStream) {
        super(outStream);
    }
    
    /** {@inheritDoc} */
    @Override
    public void putNextEntry(final ZipEntry e) throws IOException {
        m_hasEntries = true;
        super.putNextEntry(e);
    }
    
    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        if (!m_hasEntries) {
            putNextEntry(new ZipEntry("empty_entry"));
        }
        super.close();
    }
}
