/*
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
 *   Jul 12, 2006 (wiswedel): created
 */
package de.unikn.knime.core.data;

/** Interface which may be implemented by DataTable implementations which
 * do know their row count. Classes that deal with DataTables can then check
 * if the object at hand implements this interface and can retrieve their 
 * row count for, e.g. progress information. 
 * 
 * @author wiswedel, University of Konstanz
 */
public interface KnowsRowCount {
    
    /** Get the number or rows in this object. This method may return 
     * a negative value to indicate that the row count is not known. If it
     * returns a positive value this value must be correct! 
     * <p>This method must not throw an exception!
     * @return The row count of the this DataTable. A negative value if
     * unknown.
     */
    int getRowCount();

}
