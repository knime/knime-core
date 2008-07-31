/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * 
 * History
 *   Nov 3, 2006 (sieb): created
 */
package org.knime.core.util;


/**
 * An object that implements this interface can register at
 * {@link KnimeEncryption}. The object is invoked by the encryptor if no key is
 * available at the moment of a encrypt or decrypt task.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public interface EncryptionKeySupplier {

    /**
     * This method must return a key that should be use to encrypt/decrypt Knime
     * used passwords, keys, etc.
     * 
     * @return the encryption key as string
     */
    public String getEncryptionKey();

}
