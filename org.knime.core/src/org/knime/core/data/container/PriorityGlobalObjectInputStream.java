/* 
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
 *   Dec 7, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;

import org.knime.core.eclipseUtil.GlobalObjectInputStream;

/**
 * Object input stream, which can be used to load classes using a given 
 * preferred ClassLoader. This class loader is typically the bundle
 * class loader of the DataCell to be read next from the stream. 
 * @author Bernd Wiswedel, University of Konstanz
 */
class PriorityGlobalObjectInputStream extends GlobalObjectInputStream {
    private ClassLoader m_classLoader;
    
    /** Delegates to super.
     * @param in Delegated to super.
     * @throws IOException If super throws an exception.
     */
    PriorityGlobalObjectInputStream(final InputStream in) 
        throws IOException {
        super(in);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) 
        throws IOException, ClassNotFoundException {
        if (m_classLoader != null) {
            try {
                return Class.forName(desc.getName(), true, m_classLoader);
            } catch (ClassNotFoundException cnfe) {
                // ignore and let super do it.
            }
        }
        return super.resolveClass(desc);
    }
    
    /** Set the class loader to use next or <code>null</code> if to use 
     * the default.
     * @param l to use.
     */
    void setCurrentClassLoader(final ClassLoader l) {
        m_classLoader = l;
    }
}
