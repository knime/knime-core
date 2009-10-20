/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.mine.cluster.hierarchical.distfunctions;


import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;

/**
 * Calculates the distance for two data rows based on the Minkowski distance.
 * 
 * @author som-team, University of Konstanz
 */
public class MinkowskiDist implements DistanceFunction {
    
    /**
     * The power of this type of minkowski distance.
     */
    private final int m_p;

    /**
     * Creates a Minkowski distance object from an Minkowski distance.
     * 
     * @param p the power for this Minkowski distance
     */
    public MinkowskiDist(final int p) {
        m_p = p;
    }
    
    /**
     * Calculates the distance between two data rows based on the 
     * Minkowski distance.
     * 
     * @param firstDataRow the first data row used to calculate the distance
     * @param secondDataRow the second data row used to calculate the distance
     * @param includedCols the columns to include into the calculation
     * 
     * @return the distance of the two rows
     */
    public double calcDistance(final DataRow firstDataRow,
            final DataRow secondDataRow, final int[] includedCols) {
        
        double sumPowDist = 0;

        for (int i : includedCols) {
            // skip not included cells
            DataCell x = firstDataRow.getCell(i);
            DataCell y = secondDataRow.getCell(i);
            if (!x.isMissing() && x instanceof DoubleValue
            &&  !y.isMissing() && y instanceof DoubleValue) {
                DoubleValue clusterValue = (DoubleValue)firstDataRow.getCell(i);
                DoubleValue rowValue = (DoubleValue)secondDataRow.getCell(i);
                double dist = Math.abs(clusterValue.getDoubleValue() 
                        - rowValue.getDoubleValue());
                sumPowDist += Math.pow(dist, m_p);
            }
        }
        
        
        return Math.pow(sumPowDist, (double)1 / (double)m_p);
    }
    
    /**
     * @param o The object to compare with.
     * @return true if both instances (classes) are the same. 
     */
    @Override
    public boolean equals(final Object o) {
        if (o != null) {
            return this.getClass() == o.getClass();
        } else {
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
    
    /**
     * Returns the String representation of this distance function.
     * 
     * @return the String representation
     */
    @Override
    public String toString() {
        
        return "Minkowski Distance with p=" + m_p;
    }
}
