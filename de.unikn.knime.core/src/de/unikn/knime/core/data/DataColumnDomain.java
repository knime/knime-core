/* --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.data;

import java.util.Set;

/**
 * A column domain object holds information about one column, that is the
 * possible values, and an upper and lower bound.
 * 
 * Note: It is crutial that the creator of a column domain ensures that the data
 * filled in is correct. In the sense that no value in the data table will be
 * outside the provided bounds and that no other value appears in that column
 * than the one listed in the array returned by <code>getValues()</code>. It
 * is assumed that the domain - if available - contains reliable data. If you
 * are not sure about the data to come in your column, don't provide a domain.
 * 
 * @author Thomas Gabriel, Konstanz University
 */
public interface DataColumnDomain {

    /**
     * Return all possible values in this column. Note that this array can be
     * <code>null</code> if this information is not available (for continuous
     * double values, for example) and that the <code>DataCell</code>s in the
     * returned array do not have to be of the same type (but the column type
     * should be compatible to all of their values).<br>
     * If the returned array is not null, the corresponding column must not
     * contain any value other than the ones contained in the array. The array
     * could contain a superset of the values in the table though.
     * 
     * @return An array of possible values or <code>null</code>.
     * 
     * @see #hasValues()
     */
    Set<DataCell> getValues();

    /**
     * @return true, if this column spec has nominal values defined (i.e. the
     *         <code>getValues</code> method returns a non-null value).
     * 
     * @see #getValues()
     */
    boolean hasValues();

    /**
     * Return the lower bound of the domain of this column, if available. Note
     * that this value does not necessarily need to actually occur in the
     * corresponding <code>DataTable</code> but it is describing the range of
     * the domain of this attribute.
     * 
     * @return a DataCell with the lowest possible value or <code>null</code>.
     * 
     * @see #hasLowerBound()
     */
    DataCell getLowerBound();

    /**
     * @return true ,if the lower bound value has been defined.
     * 
     * @see #getLowerBound()
     */
    boolean hasLowerBound();

    /**
     * Return the upper bound of the domain of this column, if available. Note
     * that this value does not necessarily need to actually occur in the
     * corresponding <code>DataTable</code> but it is describing the range of
     * the domain of this attribute.
     * 
     * @return a DataCell with the largest possible value or <code>null</code>.
     * 
     * @see #hasUpperBound()
     */
    DataCell getUpperBound();

    /**
     * @return true, if the upper bound value has been defined.
     * 
     * @see #getUpperBound()
     */
    boolean hasUpperBound();
    
    /**
     * @return true, if lower and upper bound are defined. 
     */
    boolean hasBounds();

} // DataColumnDomain
