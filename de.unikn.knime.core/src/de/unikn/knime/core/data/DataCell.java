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
 *   20.06.2006 (bw & po): reviewed
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
 * <p>This class implements Serializable. However, if you define a custom
 * <code>DataCell</code> implementation, consider to implement a factory that 
 * takes care of reading/writing a cell to a {@link java.io.DataInput} or 
 * {@link java.io.DataOutput} source. Ordinary Java serialization is 
 * considerably slower than using such a factory. To register such a factory, 
 * define a static method having the following signature:
 * <pre>
 *  public static final DataCellSerializer&lt;YourCellClass&gt; 
 *      getCellSerializer() {
 *    ...
 *  }
 * </pre>
 * where <i>YourCellClass</i> is the name of your customized 
 * <code>DataCell</code>.
 * This method will be called whenever the cell at hand needs to be written 
 * using reflection. This method is only called once, i.e. the first time
 * that it is used.
 * 
 * <p>Since <code>DataCell</code> may implement different {@link DataValue}
 * interfaces but only one is the <i>native</i> value class, consider to 
 * implement a static method in your derived class with the following signature:
 * <pre>
 *   public static final Class<? extends DataValue> getPreferredValueClass() {
 *       ...
 *   }
 * </pre>
 * This method is called when the runtime {@link DataType} of the cell is 
 * created using reflection. The associated {@link DataType} will set the 
 * renderer, icon, and comparator of this native value class as default. 
 * If you don't specify such method, the order on the value interfaces 
 * is undefined.
 * 
 * <p>For further details on data types, see also the 
 * <a href="package-summary.html">package description</a> and the 
 * <a href="doc-files/newtypes.html">manual on defining new KNIME cell 
 * types</a>.
 * @see DataType
 * @see de.unikn.knime.core.data.DataCellSerializer
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class DataCell implements DataValue, Serializable {

    /**
     * Returns this cell's <code>DataType</code>.
     * @return The <code>DataType</code>.
     * @see DataType#getType(Class)
     */
    public final DataType getType() {
        return DataType.getType(getClass());
    }
    
    /**
     * Does this cell represent a missing cell? The default implementation 
     * returns <code>false</code>. If you need to represent a missing cell
     * use the static method DataType.getMissingCell().
     * @return If the cell represents a missing value.
     */
    public final boolean isMissing() {
        /* 
         * Instead of testing of this != DataType.getMissing() we use here 
         * this slightly more complicated approach using the method 
         * isMissingInternal() with package scope in order to avoid class 
         * loading  problems. Especially in eclipse it is known that there are 
         * many different class loaders, each of which can potentially create 
         * its own DataType.MissingCell instance.  
         */
        return isMissingInternal();
    }
    
    /** Internal implemenation of getMissing(). It will return 
     * <code>false</code> and is only overridden in the missing cell 
     * implementation.
     * @return <code>false</code>.
     */
    boolean isMissingInternal() {
        return false;
    }

    /**
     * Returns the String representation for this cell. All cells must provide
     * this as a fall back in case the actual type of the cell is unknown (or
     * unexpected).
     * 
     * @return A string representation of the value of this cell.
     */
    @Override
    public abstract String toString();

    /**
     * Implements an equal method which returns <code>true</code> only if both
     * cells are of the same class and are equal. For that, this final method
     * calls the type specific <code>equalsDataCell</code> method, which all
     * derived data cells must provide. It handles the missing value and null
     * cases, in all other cases it delegates to the specific method.
     * 
     * @param o The other object to check.
     * @return <b>true </b> if this instance and the given object are instances
     *         of the same class and of equal value (or both representing
     *         missing values).
     */
    @Override
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
     * argument is guaranteed to be not <code>null</code>, to be of the same
     * class than this, and not representing a missing value.
     * 
     * @param dc the cell to compare this to.
     * @return <code>true</code> if this is equal to the argument,
     *         <code>false</code> if not.
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
    @Override
    public abstract int hashCode();

}
