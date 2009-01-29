/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 *   20.06.2006 (bw & po): reviewed
 */
package org.knime.core.data;

import java.io.Serializable;

import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.BlobWrapperDataCell;

/**
 * Abstract base class of all <code>DataCell</code>s, which acts as a container 
 * for arbitrary values and defines the common abilities all cells must provide,
 * that is: retrieve the cell type, a string representation of the value, 
 * find out if this cell is missing, and test whether it is equal to another 
 * one.
 * 
 * <p>
 * Derived classes have to implement at least one interface
 * derived from {@link DataValue}. The derived class must be
 * read-only, i.e. setter methods must not be implemented.
 * 
 * <p>
 * This class implements {@link java.io.Serializable}. However, if
 * you define a custom <code>DataCell</code> implementation, consider to
 * implement a factory that takes care of reading and writing a cell to a
 * {@link java.io.DataInput} or
 * {@link java.io.DataOutput} source. Ordinary Java
 * serialization is considerably slower than implementing your own 
 * {@link DataCellSerializer}. To register
 * such a serializer, define a static method having the following signature:
 * 
 * <pre>
 *   public static final {@link DataCellSerializer}&lt;YourCellClass&gt; 
 *       getCellSerializer() {
 *     ...
 *   }
 * </pre>
 * 
 * where <i>YourCellClass</i> is the name of your <code>DataCell</code> 
 * implementation. This method will be called by reflection, whenever the cell 
 * at hand needs to be written or read. 
 * 
 * <p>
 * <a name="preferredvalueclass"/>
 * Since <code>DataCell</code> may implement different {@link DataValue}
 * interfaces but only one is the <i>preferred</i> value class, 
 * implement a static method in your derived class with the following signature:
 * 
 * <pre>
 *    public static final Class&lt;? extends DataValue&gt; 
 *      getPreferredValueClass() {
 *        ...
 *    }
 * </pre>
 * 
 * This method is called once when the runtime {@link DataType} of the cell is
 * created using reflection. The associated {@link DataType} provides the
 * renderer, icon, and comparator of this preferred value. If this method is 
 * not implemented, the order on the value interfaces is undefined.
 * 
 * <p>
 * For further details on data types, see also the <a
 * href="package-summary.html">package description</a> and the <a
 * href="doc-files/newtypes.html">manual</a> on defining new cell types.
 * 
 * @see DataType
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class DataCell implements DataValue, Serializable {

    /**
     * Returns this cell's <code>DataType</code>. This method is provided for 
     * convenience only, it is a shortcut for 
     * <code>DataType.getType(o.getClass())</code>, where <i>o</i> is the 
     * runtime <code>DataCell</code> object.
     * 
     * @return the <code>DataType</code>
     * @see DataType#getType(Class)
     */
    public final DataType getType() {
        DataType elementType = null;
        if (this instanceof CollectionDataValue) {
            elementType = ((CollectionDataValue)this).getElementType();
        }
        return DataType.getType(getClass(), elementType);
    }
    
    /**
     * Returns <code>true</code> if this represents missing cell, 
     * <code>false</code> otherwise. The default implementation returns 
     * <code>false</code>. If you need a missing cell use the static method 
     * {@link DataType#getMissingCell()}.
     * 
     * @return <code>true</code> if the cell represents a missing value, 
     *         <code>false</code> otherwise
     * @see DataType#getMissingCell()
     */
    public final boolean isMissing() {
        /* 
         * Instead of testing "this != DataType.getMissing()" we use here 
         * the slightly more complicated approach using the method 
         * isMissingInternal() with package scope in order to avoid class 
         * loading  problems. Especially in eclipse it is known that there are 
         * many different class loaders, each of which can potentially create 
         * its own DataType.MissingCell instance.  
         */
        return isMissingInternal();
    }
    
    /** 
     * Internal implementation of {@link DataCell#isMissing()}. It will return 
     * <code>false</code> and is only overridden in the 
     * {@link DataType#getMissingCell()} implementation.
     * 
     * @return <code>false</code>
     */
    boolean isMissingInternal() {
        return false;
    }

    /**
     * Returns the String representation of this cell's value. 
     * 
     * @return a String representation of this cell's value
     */
    @Override
    public abstract String toString();

    /**
     * Implements the equals method which returns <code>true</code> only if both
     * cells are of the same class and {@link #equalsDataCell(DataCell)} returns
     * <code>true</code>. For that, this final method calls the type specific 
     * {@link #equalsDataCell(DataCell)} method, which all derived 
     * <code>DataCell</code>s must provide. This method handles the missing 
     * value and <code>null</code> cases, in all other cases it delegates
     * to the specific method.
     * 
     * @param o the other object to check
     * @return <code>true</code> if this instance and the given object are 
     *         instances of the same class and of equal value (or both 
     *         representing missing values)
     */
    @Override
    public final boolean equals(final Object o) {

        // true of called on the same objects
        if (this == o) {
            return true;
        }
        // also handles null cases
        if (!(o instanceof DataCell)) {
            return false;
        }
        DataCell thisDelegate = this;
        while (thisDelegate instanceof BlobWrapperDataCell) {
            thisDelegate = ((BlobWrapperDataCell)thisDelegate).getCell();
        }
        
        DataCell otherDelegate = (DataCell)o;
        while (otherDelegate instanceof BlobWrapperDataCell) {
            otherDelegate = ((BlobWrapperDataCell)otherDelegate).getCell();
        }
        
        // only cells of identical classes can possibly be equal
        if (thisDelegate.getClass().equals(otherDelegate.getClass())) {
            // if both cells are missing they are equal
            if (thisDelegate.isMissing() && otherDelegate.isMissing()) {
                return true;
            }
            // if only one of both cells is missing they can not be equal
            if (thisDelegate.isMissing() || otherDelegate.isMissing()) {
                return false;
            }
            // now call the datacell class specific equals method
            return thisDelegate.equalsDataCell(otherDelegate);
        }
        // not of the same class
        return false;
    }

    /**
     * Derived classes implement their specific equals function here. The
     * argument is guaranteed to be not <code>null</code> or a missing value, 
     * to be of the same class like this.
     * 
     * @param dc the cell to compare this to
     * @return <code>true</code> if this is equal to the argument,
     *         <code>false</code> if not
     */
    protected abstract boolean equalsDataCell(final DataCell dc);

    /**
     * This method must be implemented in order to ensure that two equal
     * <code>DataCell</code> objects return the same hash code.
     * 
     * @return the hash code of your specific <code>DataCell</code>
     * 
     * @see java.lang.Object#hashCode()
     * @see #equals
     */
    @Override
    public abstract int hashCode();

}
