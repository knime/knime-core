/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;

/**
 * General <code>BasisFunctionLearnerRow</code> prototype which provides
 * functions to shrink, cover, and reset rules; and to be compared with others 
 * by its coverage. This basis function also keeps a list of all covered 
 * training examples.
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
    
    /** Keeps key of all covered pattern. */
    private final Set<RowKey> m_coveredPattern;

    /**
     * Initialise a new basisfunction rule with one covered pattern since this
     * rule is also covered by itself. The number of explained pattern is set to
     * zero.
     * 
     * @param key of this row
     * @param centroid the initial center vector
     * @param classInfo the class info value
     */
    protected BasisFunctionLearnerRow(final RowKey key, final DataRow centroid,
            final DataCell classInfo) {
        // check inputs
        assert (key != null);
        assert (centroid != null);
        assert (classInfo != null);
        m_key = key;
        m_centroid = centroid;
        m_classInfo = classInfo;
        m_coveredPattern = new LinkedHashSet<RowKey>();
    }

    /**
     * @return underlying predictor row
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
     * {@inheritDoc}
     */
    public int getNumCells() {
        // #attributes + class, weight, spread, features, variance
        return m_centroid.getNumCells() + 5; 
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public DataCell getCell(final int index) {
        final int nrCells = m_centroid.getNumCells();
        if (index < nrCells) {
            return getFinalCell(index);
        } else if (index == nrCells) {
            return getClassLabel();
        } else if (index == nrCells + 1) {
            assert m_coveredPattern.size() > 0;
            return new IntCell(m_coveredPattern.size());
        } else if (index == nrCells + 2) {
            return new DoubleCell(getPredictorRow().computeSpread());
        } else if (index == nrCells + 3) {
            return new IntCell(getPredictorRow().getNrUsedFeatures());
        } else {
            return new DoubleCell(getVariance());
        }
    }

    /**
     * {@inheritDoc}
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
    public abstract DataCell getFinalCell(final int index);

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
    public abstract boolean compareCoverage(final BasisFunctionLearnerRow o,
            final DataRow r);

    /**
     * Returns a set which contains all input training pattern covered by this
     * basis function.
     * 
     * @return set of covered input pattern
     */
    public final Set<RowKey> getAllCoveredPattern() {
        return Collections.unmodifiableSet(m_coveredPattern);
    }
    
    /**
     * Returns the within-cluster variance.
     * 
     * @return within-cluster variance
     */
    public final double getVariance() {
        return getPredictorRow().getVariance();
    }
    
    /**
     * If a new instance of this class is covered.
     * 
     * @param row to cover
     * @param classInfo and class.
     */
    public final void addCovered(final DataRow row, final DataCell classInfo) {
        if (m_coveredPattern.add(row.getKey())) {
            getPredictorRow().cover(row, classInfo);
        }
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
        addCovered(row, m_classInfo);
        // called in derived class
        cover(row);
    }
    
    /**
     * Computes the intersection of instances covered by this and the other
     * basisfunction - its fraction to the total number of instances is 
     * returned.
     * @param bf the other basisfunction to get covered instances from
     * @return the intersection ratio of both covered sets
     */
    public final double computeCoverage(final BasisFunctionLearnerRow bf) {
        Set<RowKey> c1 = this.getAllCoveredPattern();
        Set<RowKey> c2 = bf.getAllCoveredPattern();
        int cnt = 0;
        for (RowKey cell : c1) {
            if (c2.contains(cell)) {
               cnt++;
            }
        }
        int sizeC1 = c1.size();
        int sizeC2 = c2.size();
        if (sizeC1 + sizeC2 == 0) {
            assert cnt == 0;
            return 0;
        }
        return 1.0 * cnt / (sizeC1 + sizeC2 - cnt);
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
    public abstract boolean covers(final DataRow row);

    /**
     * Returns <code>true</code> if the input row is explained by this row,
     * otherwise <code>false</code>. Means, the maximum coverage criteria is
     * fulfilled.
     * 
     * @param row to check coverage
     * @return <code>true</code> if explained, otherwise <code>false</code>
     */
    public abstract boolean explains(final DataRow row);

    /**
     * Called if a new row has to be adjusted.
     * 
     * @param row conflicting pattern
     * @return a value greater zero if a conflict has to be solved. The value
     *         indicates relative loss in coverage for this basis function.
     */
    public abstract boolean getShrinkValue(final DataRow row);

    /**
     * Called if a new row has to be adjusted, all conflicting rows are
     * shrunken.
     * 
     * @param row conflicting pattern
     * @return <code>true</code> if this basis function was effected by any
     *         change, otherwise <code>false</code>
     */
    public abstract boolean shrink(final DataRow row);

    /**
     * Called if the algorithms starts a new run overall input pattern; some
     * variables might need to be reset.
     */
    public abstract void reset();

    /**
     * Called if a row covers a new <code>DataRow</code>.
     * 
     * @param row the new covered <code>DataRow</code>
     */
    public abstract void cover(final DataRow row);

    /**
     * Check if two BasisFunctionLearnerRow objects are equal if their 
     * centroids and class labels are equal.
     * 
     * @param o the other object to check
     * @return <b>true</b> if this instance and the given object are instances
     *         of the same class and the centroid vector and class label are
     *         equal
     * 
     * {@inheritDoc}
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
     *
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getAnchor().hashCode() * m_classInfo.hashCode();
    }   
    
}
