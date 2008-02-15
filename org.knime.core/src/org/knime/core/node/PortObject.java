/* ------------------------------------------------------------------
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
 * General interface for objects that are passed along node 
 * connections. Most prominent example of such an object is 
 * {@link org.knime.core.node.BufferedDataTable}. 
 * <code>PortObjects</code> contain the actual data or models, which are used
 * during a node's 
 * {@link GenericNodeModel#execute(PortObject[], ExecutionContext) execution}.
 * They are (meta-)described by {@link PortObjectSpec} objects.
 * Both a specific class of a <code>PortObjectSpec</code> and 
 * a {@link PortObject} describe {@link PortType}.
 * 
 * <p><b>Important:</b>Along with the methods defined by this interface, 
 * implementors also need to define a static method with the following 
 * signature:
 * <pre>
 *  public static PortObject loadPortObject(final File directory,
 *      final ExecutionMonitor exec) 
 *      throws IOException, CanceledExecutionException {...}
 * </pre>
 * This method is used when the workflow is restored from disk; it is
 * the counterpart to the {@link #savePortObject(File, ExecutionMonitor) 
 * save method} (which is an object method).  
 * @see org.knime.core.node.BufferedDataTable
 * @see PortObjectSpec
 * @see PortType
 * @author M. Berthold & B. Wiswedel, University of Konstanz
 */
public interface PortObject {
    
    static abstract class PortObjectSerializer<T extends PortObject> 
        implements Serializer<T> {
        
        /** Saves the portObject to a directory location.
         * @param portObject The object to save.
         * @param directory Where to save to
         * @param exec To report progress to and to check for cancelation.
         * @throws IOException If that fails for IO problems.
         * @throws CanceledExecutionException If canceled.
         */
        protected abstract void savePortObject(final T portObject,
                final File directory, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException;
        
        /** Load a portObject from a directory location.
         * @param directory Where to load from
         * @param exec To report progress to and to check for cancelation.
         * @return The restored object.
         * @throws IOException If that fails for IO problems.
         * @throws CanceledExecutionException If canceled.
         */
        protected abstract T loadPortObject(
                final File directory, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException;
    }
}
