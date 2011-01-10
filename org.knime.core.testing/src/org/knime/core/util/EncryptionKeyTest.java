/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   23.10.2008 (gabriel): created
 */
package org.knime.core.util;

import junit.framework.TestCase;

/**
 * Tests the encryption key supplier.
 *  
 * @author Thomas Gabriel, University of Konstanz
 */
public class EncryptionKeyTest extends TestCase {
    
    static {
        // override current encryption key supplier
        KnimeEncryption.setEncryptionKeySupplier(new EncryptionKeySupplier() {
            /**
             * @return <code>KNIME</code> always
             * {@inheritDoc}
             */
           public String getEncryptionKey() {
               return "KNIME";
           }
        });
    }
    
    private static final String PASSWORD = "tommy&vonny";
    
    private String m_encryptedPassword;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        m_encryptedPassword = KnimeEncryption.encrypt(PASSWORD.toCharArray());
        assertFalse(m_encryptedPassword == null);
    }

    public final void testPassword() throws Exception {
        super.tearDown();
        assertTrue(KnimeEncryption.decrypt(
                m_encryptedPassword).equals(PASSWORD));
    }

}
