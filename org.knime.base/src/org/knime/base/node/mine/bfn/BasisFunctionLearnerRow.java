/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;
import org.knime.core.data.def.IntCell;

/**
 * General <code>BasisFunctionLearnerRow</code> prototype which provides
 * functions to shink, cover, and reset rules; and to be compared with others by
 * its coverage. This basis function also keeps a list of all covered trainings
 * examples.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionLearnerRow implements DataRow {
    
    /** This row's identifier. */
    private final RowKey m_key;

    /** This row's class label. */
    private final DataCell m_classInfo;

    /** Keeps the initial anchor vector. */
    private final DataRow m_centroid;

    /** Default level, used for hierarchical rule learning only. */
    private static final DataCell DFT_LEVEL = new IntCell(1);

    /** Level of detail, by default all are assigned to 1. */
    private IntCell m_level = null;

    /** <code>true</code> if an additional column show the hierarchy level. */
    private final boolean m_hierarchy;
    
    /** Keeps the data cell ids of all pattern covered by this basisfunction. */
    private Map<DataCell, Set<DataCell>> m_coveredPattern;

    /**
     * Inits a new basisfunction rule with one covered pattern since this rule
     * is also covered by itself. The number of explained pattern is set to
     * zero.
     * 
     * @param key of this row
     * @param centroid the initial center vector
     * @param classInfo the class info value
     * @param hierarchy <code>true</code> if hierarchy is trained
     */
    protected BasisFunctionLearnerRow(final RowKey key, final DataRow centroid,
            final DataCell classInfo, final boolean hierarchy) {
        // check inputs
        assert (key != null);
        assert (centroid != null);
        assert (classInfo != null);
        m_key = key;
        m_centroid = centroid;
        m_classInfo = classInfo;
        m_hierarchy = hierarchy;
        m_coveredPattern = new LinkedHashMap<DataCell, Set<DataCell>>();
    }

    /**
     * @return Underlying predictor row.
     */
    public abstract BasisFunctionPredictorRow getPredictorRow();

    /**
     * Returns the basisfunction's anchor vector.
     * 
     * @return the initial anchor
     */
    public final DataRow getAnchor() {
        return m_centroid;
    }

    /**
     * @see org.knime.core.data.DataRow#getNumCells()
     */
    public final int getNumCells() {
        if (m_hierarchy) {
            return m_centroid.getNumCells() + 2; // + 1 for class label
            // + 1 for hierarchy level
        } else {
            return m_centroid.getNumCells() + 1; // + 1 for class label
        }
    }

    /** 
     * @see org.knime.core.data.DataRow#getKey()
     */
    public RowKey getKey() {
        return m_key;
    }

    /**
     * @return The class label for this rule.
     */
    public final DataCell getClassLabel() {
        return m_classInfo;
    }

    /**
     * @see org.knime.core.data.DataRow#getCell(int)
     */
    public final DataCell getCell(final int index) {
        if (index < m_centroid.getNumCells()) {
            return getFinalCell(index);
        } else {
            if (m_hierarchy && index == getNumCells() - 1) {
                if (m_level == null) {
                    return DFT_LEVEL;
                } else {
                    return m_level;
                }
            } else {
                return getClassLabel();
            }
        }
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }

    /**
     * Returns a basis function cell for the given index.
     * 
     * @param index cell for index
     * @return a basis function cell
     */
    protected abstract DataCell getFinalCell(final int index);

    /**
     * Sets a new level of detail.
     * 
     * @param level level of detail which has to be greater 0
     */
    public final void setLevel(final int level) {
        m_level = new IntCell(level);
    }

    /**
     * Returns the level of detail this basis function is assigned to.
     * 
     * @return level of detail
     */
    public final int getLevel() {
        return (m_level == null ? -1 : m_level.getIntValue());
    }

    /**
     * @param col the column index
     * @return a prediction for this basis function dimension
     */
    public abstract DoubleValue getMissingValue(final int col);

    /**
     * Compares coverage of this and another row. This method is used in order
     * to find the best basisfunction for a certain input pattern. If
     * <code>this</code> covers better, return <code>true</code>.
     * 
     * @param o the row to check with
     * @param r the row to compute coverage on
     * @return <code>true</code> this <code>BasisFunction</code> covers
     *         better than the other
     * @throws NullPointerException if the given <code>other</code> basis
     *             function is <code>null</code>
     */
    protected abstract boolean compareCoverage(final BasisFunctionLearnerRow o,
            final DataRow r);

    /**
     * Returns a set which contains all input training pattern covered by this
     * basis function.
     * 
     * @return set of covered input pattern
     */
    public final Set<DataCell> getAllCoveredPattern() {
        Set<DataCell> allCov = new LinkedHashSet<DataCell>();
        for (DataCell key : m_coveredPattern.keySet()) {
            allCov.addAll(m_coveredPattern.get(key));
        }
        return Collections.unmodifiableSet(allCov);
    }
    
    /**
     * Returns a value for the coverage of this basis function, e.g. the volume,
     * number of covered pattern.
     * 
     * @return a coverage value
     */
    public abstract double computeCoverage();

    /**
     * Computes the overlapping of two basis functions.
     * 
     * @param symetric if the result is proportional to both basis functions,
     *            and thus symmetric, or if it is proportional to the area of 
     *            the basisfunction on which the function is called.
     * @param bf the other basisfunction to compute overlapping with
     * @return if both are overlapping
     */
    public abstract double overlap(final BasisFunctionLearnerRow bf,
            final boolean symetric);

    /**
     * Computes the overlapping based on two lines.
     * 
     * @param minA left point line A
     * @param maxA right point line A
     * @param minB left point line B
     * @param maxB right point line B
     * @param symmetric if the result is proportional to both basis functions,
     *            and thus symetric, or if it is proportional to the area of the
     *            basis function on which the function is called
     * @return The positive overlapping spread of this two lines or zero if none
     */
    public static final double overlapping(final double minA,
            final double maxA, final double minB, final double maxB,
            final boolean symmetric) {
        assert (minA <= maxA && minB <= maxB);
        if (minA == minB && maxA == maxB) {
            return 1;
        }
        if (maxA < minB) {
            return 0; // maxA - minB;
        }
        if (maxB < minA) {
            return 0; // maxB - minA;
        }
        if (minA < minB) {

            if (maxA < maxB) {
                if (symmetric) {
                    return (maxA - minB + 1) / (maxB - minA + 1);
                } else {
                    return (maxA - minB + 1) / (maxA - minA + 1);
                }

            } else {
                return (maxB - minB + 1) / (maxA - minA + 1);
            }
        } else {
            if (minA == maxA || minB == maxB) {
                return 1;
            }
            if (maxA < maxB) {
                if (symmetric) {
                    return (maxA - minA + 1) / (maxB - minB + 1);
                } else {
                    return 1;
                }
            } else {
                if (symmetric) {
                    return (maxB - minA + 1) / (maxA - minB + 1);
                } else {
                    return (maxB - minA + 1) / (maxA - minA + 1);
                }
            }
        }
    }
    
    /**
     * If a new instance of this class is covered.
     * 
     * @param key the instance's key
     * @param classInfo and class.
     */
    public final void addCovered(final DataCell key, final DataCell classInfo) {
        Set<DataCell> covSet;
        if (m_coveredPattern.containsKey(classInfo)) {
            covSet = m_coveredPattern.get(classInfo);
        } else {
            covSet = new LinkedHashSet<DataCell>();
            m_coveredPattern.put(classInfo, covSet);
        }
        covSet.add(key);
        getPredictorRow().cover(classInfo);
    }

    /**
     * Resets the number of covered pattern to zero and calls the abstract
     * {@link #reset()}.
     */
    final void resetIntern() {
        // reset pattern covered
        m_coveredPattern.clear();
        // reset number of covered pattern
        getPredictorRow().resetCoveredPattern();
        // called in derived class
        reset();
    }

    /**
     * Covers a new row and decrease covered counter. Also calls the abstract
     * {@link #cover(DataRow)} method.
     * 
     * @param row the data row to cover
     */
    final void coverIntern(final DataRow row) {
        assert (row != null);
        // increase covered pattern
        addCovered(row.getKey().getId(), m_classInfo);
        // called in derived class
        cover(row);
    }

    /* --- print function --- */

    /**
     * Writes information retrieved from the {@link #toString()} method to the
     * given stream.
     * 
     * @param out the <code>PrintStream</code> to add info
     * @throws NullPointerException if <code>out</code> is <code>null</code>.
     * 
     * @see #toString()
     */
    public final void print(final PrintStream out) {
        // and invoke the toString of the derived class
        out.print(toString() + "\n");
    }

    /* --- functions from DataCell --- */

    /**
     * Returns a string summary of this basis function cell including the
     * assigned class, number of covered, as well as explained pattern. This
     * method is supposed to be overridden to add additional information for a
     * particular model.
     * 
     * @return string summary for this basisfunction cell
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("class=" + m_classInfo.toString() + " ");
        buf.append("(#covers=" + getPredictorRow().getNumAllCoveredPattern()
                + ")");
        return buf.toString();
    }

    /* --- abstract functions --- */

    /**
     * Computes activation for a given row using this basis function.
     * 
     * @param row the data row to compute activation with
     * @return the activation of the row
     */
    public abstract double computeActivation(final DataRow row);

    /**
     * Returns <code>true</code> if the input row is covered by this row,
     * otherwise <code>false</code>. Means, the minimum coverage criteria is
     * fulfilled.
     * 
     * @param row to check coverage
     * @return <code>true</code> if covered, otherwise <code>false</code>
     */
    protected abstract boolean covers(final DataRow row);

    /**
     * Returns <code>true</code> if the input row is explained by this row,
     * otherwise <code>false</code>. Means, the maximum coverage criteria is
     * fulfilled.
     * 
     * @param row to check coverage
     * @return <code>true</code> if explained, otherwise <code>false</code>
     */
    protected abstract boolean explains(final DataRow row);

    /**
     * Called if a new row has to be adjusted.
     * 
     * @param row conflicting pattern
     * @return a value greater zero if a conflict has to be solved. The value
     *         indicates relative loss in coverage for this basis function.
     */
    protected abstract boolean getShrinkValue(final DataRow row);

    /**
     * Called if a new row has to be adjusted, all conflicting rows are
     * shrunken.
     * 
     * @param row conflicting pattern
     * @return <code>true</code> if this basis function was effected by any
     *         change, otherwise <code>false</code>
     */
    protected abstract boolean shrink(final DataRow row);

    /**
     * Called if the algorithms starts a new run overall input pattern; some
     * variables might need to be reset.
     */
    protected abstract void reset();

    /**
     * Called if a row covers a new <code>DataRow</code>.
     * 
     * @param row the new covered <code>DataRow</code>
     */
    protected abstract void cover(final DataRow row);

    /**
     * Check if two BasisFunctionLearnerRow objects are equal by its centroid.
     * 
     * @param o the other object to check
     * @return <b>true</b> if this instance and the given object are instances
     *         of the same class and the centroid vector is equal
     * 
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(final Object o) {
        // true of called on the same objects
        if (this == o) {
            return true;
        }
        // check for null pointer
        if (o == null) {
            return false;
        }
        // only do a real check if objects are of same class
        if (this.getClass() == o.getClass()) {
            BasisFunctionLearnerRow bf = (BasisFunctionLearnerRow)o;
            // check if string representations are equal.
            return m_centroid.equals(bf.m_centroid)
                    && m_classInfo.equals(bf.m_classInfo);
        }
        // no, they are not equal
        return false;
    }

    /**
     * Returns a hash code computed by the product of the hash code of
     * anchor and class label.
     * @return A new hash code.
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getAnchor().hashCode() * m_classInfo.hashCode();
    }
    
}
