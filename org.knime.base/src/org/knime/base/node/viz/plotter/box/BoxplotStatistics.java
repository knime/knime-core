/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   08.06.2015 (Alexander): created
 */
package org.knime.base.node.viz.plotter.box;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;


/**
 *
 * @author Alexander Fillbrunn
 * @since 2.12
 */
public class BoxplotStatistics {

    private double m_min;
    private double m_max;
    private double m_lowerWhisker;
    private double m_lowerQuartile;
    private double m_median;
    private double m_upperQuartile;
    private double m_upperWhisker;
    private Set<Outlier> m_mildOutliers;
    private Set<Outlier> m_extremeOutliers;

    /**
     * Creates an instance of BoxplotStatistics.
     * @param mildOutliers set of mild outliers
     * @param extremeOutliers set of extreme outliers
     * @param min minimum value
     * @param max maximum value
     * @param lowerWhisker value of lower whisker
     * @param lowerQuartile value of lower quartile
     * @param median value of median
     * @param upperQuartile  value of upper quartile
     * @param upperWhisker value of upper whisker
     */
    public BoxplotStatistics(final Set<Outlier> mildOutliers, final Set<Outlier> extremeOutliers,
                                final double min, final double max,
                                final double lowerWhisker, final double lowerQuartile,
                                final double median, final double upperQuartile, final double upperWhisker) {
        m_min = min;
        m_max = max;
        m_lowerWhisker = lowerWhisker;
        m_lowerQuartile = lowerQuartile;
        m_median = median;
        m_upperQuartile = upperQuartile;
        m_upperWhisker = upperWhisker;
        m_mildOutliers = mildOutliers;
        m_extremeOutliers = extremeOutliers;
    }

    /**
     * Deserialization constructor.
     */
    @JsonCreator
    private BoxplotStatistics() {

    }

    /**
     * @param min the min to set
     */
    public void setMin(final double min) {
        m_min = min;
    }

    /**
     * @param max the max to set
     */
    public void setMax(final double max) {
        m_max = max;
    }

    /**
     * @param lowerWhisker the lowerWhisker to set
     */
    public void setLowerWhisker(final double lowerWhisker) {
        m_lowerWhisker = lowerWhisker;
    }

    /**
     * @param lowerQuartile the lowerQuartile to set
     */
    public void setLowerQuartile(final double lowerQuartile) {
        m_lowerQuartile = lowerQuartile;
    }

    /**
     * @param median the median to set
     */
    public void setMedian(final double median) {
        m_median = median;
    }

    /**
     * @param upperQuartile the upperQuartile to set
     */
    public void setUpperQuartile(final double upperQuartile) {
        m_upperQuartile = upperQuartile;
    }

    /**
     * @param upperWhisker the upperWhisker to set
     */
    public void setUpperWhisker(final double upperWhisker) {
        m_upperWhisker = upperWhisker;
    }

    /**
     * @param mildOutliers the mildOutliers to set
     */
    public void setMildOutliers(final Set<Outlier> mildOutliers) {
        m_mildOutliers = mildOutliers;
    }

    /**
     * @param extremeOutliers the extremeOutliers to set
     */
    public void setExtremeOutliers(final Set<Outlier> extremeOutliers) {
        m_extremeOutliers = extremeOutliers;
    }

    /**
     * @return the min
     */
    public double getMin() {
        return m_min;
    }

    /**
     * @return the max
     */
    public double getMax() {
        return m_max;
    }

    /**
     * @return the lowerWhisker
     */
    public double getLowerWhisker() {
        return m_lowerWhisker;
    }

    /**
     * @return the lowerQuartile
     */
    public double getLowerQuartile() {
        return m_lowerQuartile;
    }

    /**
     * @return the median
     */
    public double getMedian() {
        return m_median;
    }

    /**
     * @return the upperQuartile
     */
    public double getUpperQuartile() {
        return m_upperQuartile;
    }

    /**
     * @return the upperWhisker
     */
    public double getUpperWhisker() {
        return m_upperWhisker;
    }

    /**
     * @return the mildOutliers
     */
    public Set<Outlier> getMildOutliers() {
        return m_mildOutliers;
    }

    /**
     * @return the extremeOutliers
     */
    public Set<Outlier> getExtremeOutliers() {
        return m_extremeOutliers;
    }
}
