/* -------------------------------------------------------------------
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
 *   07.07.2005 (mb): created
 *   09.01.2006(all): reviewed
 */
package de.unikn.knime.core.data;

import java.io.Serializable;

/**
 * Abstract base class of all data cells defining the common abilities to
 * retrieve the cell type, to find out if this cell is missing, and also a
 * string representation, all cells must provide.
 * <p>
 * 
 * Derived classes will extend this class and implement (at least one) interface
 * derived from <code>DataValue</code>. The instantiated derived object must
 * be read-only, no setter methods must be implemented.
 * 
 * @author M. Berthold, University of Konstanz
 */
public abstract class DataCell implements Serializable {

    /**
     * Returns this cell's <code>DataType</code>.
     * 
     * @return The <code>DataType</code>.
     */
    public abstract DataType getType();

    /**
     * Only derived classes representing missing value cells will override this.
     * 
     * @return <code>false</code> always.
     */
    public boolean isMissing() {
        return false;
    }

    /**
     * Returns the String representation for this cell. All cells must provide
     * this as a fall back in case the actual type of the cell is unknown (or
     * unexpected).
     * 
     * @return A string representation of the value of this cell.
     */
    public abstract String toString();

    /**
     * Implements an equal method which returns true only if both cells are of
     * the same class and are equal. For that, this final method calls the type 
     * specific <code>equalsDataCell</code> method, which all derived data cells
     * must provide. It handles the missing value and null cases, in all other 
     * cases it delegates to the specific method.
     * 
     * @param o The other object to check.
     * @return <b>true </b> if this instance and the given object are instances
     *         of the same class and of equal value (or both representing
     *         missing values).
     */
    public final boolean equals(final Object o) {

        // true of called on the same objects
        if (this == o) {
            return true;
        }
        // check for null pointer
        if (o == null) {
            return false;
        }
        // only cells of identical classes can possibly be equal
        if (this.getClass() == o.getClass()) {
            // if both cells are missing they are equal
            if (this.isMissing() && ((DataCell)o).isMissing()) {
                return true;
            }
            // if only one of both cells is missing they can not be equal
            if (this.isMissing() || ((DataCell)o).isMissing()) {
                return false;
            }
            // now call the datacell class specific equals method
            return equalsDataCell((DataCell)o);
        }
        // sorry, not equal.
        return false;
    }

    /**
     * Derived classes implement their specific equals function here. The
     * argument is guaranteed to be not null, to be of the same class than this,
     * and not representing a missing value.
     * 
     * @param dc the cell to compare this to. Won't be null, is of this.class,
     *            and not missing.
     * @return true if this is equal to the argument, false if not.
     */
    protected abstract boolean equalsDataCell(final DataCell dc);

    /**
     * This method must be implemented in order to ensure that two equal
     * <code>DataCell</code> objects return the same hash code.
     * 
     * @return The hash code of your specific data cell.
     * 
     * @see java.lang.Object#hashCode()
     * @see #equals
     */
    public abstract int hashCode();

}
