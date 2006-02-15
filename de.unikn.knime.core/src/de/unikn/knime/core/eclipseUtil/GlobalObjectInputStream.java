/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * 
 * History
 *   04.07.2005 (mb): created
 */
package de.unikn.knime.core.eclipseUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/** 
 * <code>ObjectInputStream</code> which uses the <code>GlobalClassCreator</code>
 * to resolve Objects by class name. 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class GlobalObjectInputStream extends ObjectInputStream {

    /**
     * @see ObjectInputStream#ObjectInputStream(InputStream)
     */
    public GlobalObjectInputStream(final InputStream in) throws IOException {
        super(in);
    }
    
    /**
     * Returns the Class for the given <code>ObjectStreamClass</code> which is
     * initialized by the <code>GlobalClassCreator</code> or - if this fails -
     * by the super class.
     * @param   desc an instance of class <code>ObjectStreamClass</code>
     * @return  a <code>Class</code> object corresponding to <code>desc</code>
     * @throws  IOException any of the usual input/output exceptions
     * @throws  ClassNotFoundException if class of a serialized object cannot
     *      be found
     * 
     * @see #resolveClass
     */
    protected Class<?> resolveClass(final ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        try {
             return GlobalClassCreator.createClass(desc.getName());
        } catch (ClassNotFoundException cnfe) {
              return super.resolveClass(desc);
        }
    }
    
}
