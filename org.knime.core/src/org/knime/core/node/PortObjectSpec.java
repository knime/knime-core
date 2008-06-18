/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   10.09.2007 (mb): created
 */
package org.knime.core.node;

import java.io.File;
import java.io.IOException;

import org.knime.core.internal.SerializerMethodLoader.Serializer;

/**
 * General interface for object specifications that are passed along node
 * connections. Most prominent example of such a class is
 * {@link org.knime.core.data.DataTableSpec}, which is used to represent table
 * specification. <code>PortObjectSpec</code> objects represent the
 * information that is necessary during a node's
 * {@link GenericNodeModel#configure(PortObjectSpec[]) configuration step}.
 * They are assumed to be fairly small objects (usually reside in memory) and
 * describe the general structure of {@link PortObject} objects (which are
 * passed along the connections during a node's execution). Both the class of a
 * <code>PortObjectSpec</code> and a {@link PortObject} describe
 * {@link PortType}.
 * 
 * <p>
 * <b>Important:</b> Implementors of this interface must also provide a
 * {@link PortObjectSpecSerializer}, which is used to save and load instances.
 * The framework will try to invoke a static method defined in the
 * implementation with the following signature:
 * 
 * <pre>
 *  public static PortObjectSpecSerializer&lt;FooPortObjectSpec&gt; 
 *          getPortObjectSpecSerializer(final File directory) 
 *              throws IOException {...}
 * </pre>
 * 
 * If the class does not have such a static method (or it has the wrong
 * signature), an exception will be thrown at runtime.
 * 
 * @see org.knime.core.data.DataTableSpec
 * @see PortObject
 * @see PortType
 * @author M. Berthold & B. Wiswedel, University of Konstanz
 */
public interface PortObjectSpec {

    /**
     * Factory class that's used for writing and loading objects of class
     * denoted by <code>T</code>. See description of class
     * {@link PortObjectSpec} for details.
     * 
     * @param <T> class of the object to save or load.
     */
    abstract static class PortObjectSpecSerializer
        <T extends PortObjectSpec> implements Serializer<T> {
        
        /** Saves the port specification to a directory location.
         * @param portObjectSpec The spec to save.
         * @param directory Where to save to
         * @throws IOException If that fails for IO problems.
         */
        protected abstract void savePortObjectSpec(final T portObjectSpec,
                final File directory)
        throws IOException;
        
        /** Load a specification from a directory location.
         * @param directory Where to load from
         * @return The restored object.
         * @throws IOException If that fails for IO problems.
         */
        protected abstract T loadPortObjectSpec(final File directory)
            throws IOException;
    }
}
