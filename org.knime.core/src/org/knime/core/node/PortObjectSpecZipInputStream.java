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
package org.knime.core.node;

import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * Counterpart to {@link PortObjectZipOutputStream}.
 * 
 * <p>Not meant to be instantiated or sub-classed.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PortObjectSpecZipInputStream extends ZipInputStream {
    
    /**
     * Instantiates stream based on argument stream.
     * @param inStream Delegated to super implementation.
     * @see ZipInputStream#ZipInputStream(InputStream)
     */
    public PortObjectSpecZipInputStream(final InputStream inStream) {
        super(inStream);
    }

}
