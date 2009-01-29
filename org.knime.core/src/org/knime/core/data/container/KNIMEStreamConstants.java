/* ------------------------------------------------------------------
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
 *   Aug 8, 2008 (wiswedel): created
 */
package org.knime.core.data.container;

import org.knime.core.data.RowKey;

/**
 * Defines some constants commonly used when writing {@link Buffer} files. 
 * @author Bernd Wiswedel, University of Konstanz
 */
interface KNIMEStreamConstants {
    
    /** The key being returned by a {@link NoKeyBuffer}. */
    static final RowKey DUMMY_ROW_KEY = new RowKey("non-existing");
    
    /** The byte being used as block terminate. */
    static final byte TC_TERMINATE = (byte)0x61;

    /**
     * The byte being used to escape the next byte. The next byte will therefore
     * neither be considered as terminate nor as escape byte.
     */
    static final byte TC_ESCAPE = (byte)0x62;
 
    /** The char for missing cells. */
    static final byte BYTE_TYPE_MISSING = Byte.MIN_VALUE;

    /** The char for cell whose type needs serialization. */
    static final byte BYTE_TYPE_SERIALIZATION = BYTE_TYPE_MISSING + 1;

    /** The first used char for the map char --> type. */
    static final byte BYTE_TYPE_START = BYTE_TYPE_MISSING + 2;

    /** Separator for different rows. */
    static final byte BYTE_ROW_SEPARATOR = BYTE_TYPE_MISSING + 3;
    

}
