/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.08.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

/**
 * Represents a count histogram for nominal values. It counts the class values
 * for each nominal value. This histogram has all information to calculate the
 * information quality, either for normal nominal splits as well as for binary
 * nominal subset splits.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class NominalValueHistogram {

    /**
     * The histogram is a 2D double array. The first dimension are the possible
     * nominal values, the second dimension are the class values.
     */
    private double[][] m_histogram;

    /**
     * The missing values (encoded as not a number (NaN) are counted without
     * separating the class values.
     */
    private double m_missingValueCount;

    /**
     * Creates a nominal value histogram.
     */
    public NominalValueHistogram() {
        // the histogram is initiallized for one nominal value and one
        // class value
        m_histogram = new double[1][];
        m_histogram[0] = new double[1];
    }

    /**
     * Creates a nominal value histogram from the given template. The template
     * supplies the histogram sizes.
     *
     * @param template the template histogram from which to initialize the new
     *            histogram
     */
    public NominalValueHistogram(final NominalValueHistogram template) {
        // the histogram is initialized from the template
        m_histogram = new double[template.m_histogram.length][];
        for (int i = 0; i < m_histogram.length; i++) {
            m_histogram[i] = new double[template.m_histogram[i].length];
        }
    }

    /**
     * Increments the histogram count at the given position for the given
     * weight.
     *
     * @param nominalValueMapping the nominal index for which to increment the
     *            count
     * @param classValueMapping the class index for which to increment the count
     * @param weight the amount to increase the count
     */
    public void increment(final double nominalValueMapping,
            final int classValueMapping, final double weight) {
        if (Double.isNaN(nominalValueMapping)) {
            m_missingValueCount += weight;
            return;
        }
        int oldSize = m_histogram.length;
        int nominalValueCast = (int)nominalValueMapping;
        // first ensure that the histogram is large enough
        if (nominalValueCast >= m_histogram.length) {
            // if not, enlarge the array and copy the old values
            double[][] enlargedArray = new double[nominalValueCast + 1][];
            System.arraycopy(m_histogram, 0, enlargedArray, 0,
                    m_histogram.length);
            m_histogram = enlargedArray;
            // now add a large enough second dimension to the nominal index
            // for all second dimensions between the old border and the new
            // also insert a double array of this size
            int secondDimSize =
                    Math.max(m_histogram[0].length, classValueMapping + 1);
            for (int i = oldSize; i < nominalValueCast + 1; i++) {
                m_histogram[i] = new double[secondDimSize];
            }
        }
        // now the histogram is large enough according to the first dimension
        // of nominal values; now if the class value mapping is larger than
        // the second dimension of the "old" part of the histo array enlarge
        // those
        if (classValueMapping >= m_histogram[0].length) {
            // enlarge the second dimension array and copy the old values
            // for all old second dimensions
            for (int i = 0; i < oldSize; i++) {
                double[] enlargedSedondArray =
                        new double[classValueMapping + 1];
                System.arraycopy(m_histogram[i], 0, enlargedSedondArray, 0,
                        m_histogram[i].length);
                m_histogram[i] = enlargedSedondArray;
            }
        }

        // finally, after all arrays have been enlarged in all dimensions
        // add the weight to the given class value of the given nominal value
        m_histogram[nominalValueCast][classValueMapping] += weight;
    }

    /**
     * Returns the sum of weights of the missing values.
     *
     * @return the sum of weights of the missing values
     */
    public double getMissingValueCount() {
        return m_missingValueCount;
    }

    /**
     * Returns the count histogram. First dimension are the valid (non-missing)
     * nominal values. The second dimension are the valid class values.
     *
     * @return the count histogram; first dimension are the valid (non-missing)
     *         nominal values. The second dimension are the valid class values.
     */
    public double[][] getHistogram() {
        return m_histogram;
    }

    /**
     * Returns the number of class values of the second dimension of the
     * histogram. NOTE: The value can increase when performing increments.
     *
     * @return the number of class values of the second dimension of the
     *         histogram
     */
    public int getNumClassValues() {
        return m_histogram[0].length;
    }
}
