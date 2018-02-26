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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Feb 15, 2018 (ortmann): created
 */
package org.knime.base.algorithms.outlier;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.knime.base.algorithms.outlier.listeners.WarningListener;
import org.knime.base.algorithms.outlier.options.OutlierReplacementStrategy;
import org.knime.base.algorithms.outlier.options.OutlierTreatmentOption;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

/**
 * The algorithm to identify and treat outliers based on the interquartile range.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class OutlierDetector {

    /** The intervals calculator. */
    private final OutlierIntervalsCalculator m_calculator;

    /** The outlier reviser. */
    private final OutlierReviser m_reviser;

    /**
     * Builder of the OutlierDetector.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public static class Builder {

        private final OutlierIntervalsCalculator.Builder m_intervalsBuilder;

        private final OutlierReviser.Builder m_reviserBuilder;

        /**
         * Sets the outlier column names.
         *
         * @param outlierColNames the outlier column names to be used
         */
        public Builder(final String[] outlierColNames) {
            m_intervalsBuilder = new OutlierIntervalsCalculator.Builder(outlierColNames);
            m_reviserBuilder = new OutlierReviser.Builder(outlierColNames);
        }

        /**
         * Sets the estimation type, used for the in memory quartile computation. The estimation type is ignored when
         * the computation is not carried out in memory.
         *
         * @param estimationType the estimation type to be used
         * @return the builder itself
         */
        public Builder setEstimationType(final EstimationType estimationType) {
            m_intervalsBuilder.setEstimationType(estimationType);
            return this;
        }

        /**
         * Defines how outlier have to be treated, see {@link OutlierTreatmentOption}.
         *
         * @param treatment the treatment option to be used
         * @return the builder itself
         */
        public Builder setTreatmentOption(final OutlierTreatmentOption treatment) {
            m_reviserBuilder.setTreatmentOption(treatment);
            return this;
        }

        /**
         * Defines the outlier replacement strategy, see {@link OutlierReplacementStrategy}.
         *
         * @param repStrategy the replacement strategy
         * @return the builder itself
         */
        public Builder setReplacementStrategy(final OutlierReplacementStrategy repStrategy) {
            m_reviserBuilder.setReplacementStrategy(repStrategy);
            return this;
        }

        /**
         * Sets the group column names.
         *
         * @param groupColNames the group column names
         * @return the builder itself
         */
        public Builder setGroupColumnNames(final List<String> groupColNames) {
            m_intervalsBuilder.setGroupColumnNames(groupColNames);
            m_reviserBuilder.setGroupColumnNames(groupColNames);
            return this;
        }

        /**
         * Sets the in memory calculation flag. Note that the in memory calculation requires far more space, than the
         * out-of-memory computation.
         *
         * @param inMemory the in memory calculation flag
         * @return the builder itself
         */
        public Builder calcInMemory(final boolean inMemory) {
            m_intervalsBuilder.calcInMemory(inMemory);
            return this;
        }

        /**
         * Sets the interquartile range scalar.
         *
         * @param iqrScalar the interquartile scalar
         * @return the builder itself
         */
        public Builder setIQRScalar(final double iqrScalar) {
            m_intervalsBuilder.setIQRScalar(iqrScalar);
            return this;
        }

        /**
         * Adds a listener that gets triggered whenever the {@link OutlierDetector} creates a warning.
         *
         * @param listener the listener to be added
         * @return the builder itself
         */
        public Builder addWarningListener(final WarningListener listener) {
            m_reviserBuilder.addListener(listener);
            return this;
        }

        /**
         * Sets the domain policy flag.
         *
         * @param updateDomain the domain policy
         * @return the builder itself
         */
        public Builder updateDomain(final boolean updateDomain) {
            m_reviserBuilder.updateDomain(updateDomain);
            return this;
        }

        /**
         * Constructs the outlier detector using the settings provided by the builder.
         *
         * @return the outlier detector using the settings provided by the builder
         */
        public OutlierDetector build() {
            return new OutlierDetector(this);
        }

    }

    /**
     * Constructor.
     *
     * @param b the builder providing all settings
     */
    private OutlierDetector(final Builder b) {
        m_calculator = b.m_intervalsBuilder.build();
        m_reviser = b.m_reviserBuilder.build();
    }

    /**
     * Returns the outlier free data table.
     *
     * @return the outlier free data table
     */
    public BufferedDataTable getOutTable() {
        return m_reviser.getOutTable();
    }

    /**
     * Returns the spec of the outlier free data table.
     *
     * @param inSpec the spec of the input data table
     * @return the spec of the outlier free data table
     */
    public DataTableSpec getOutTableSpec(final DataTableSpec inSpec) {
        return m_reviser.getOutTableSpec(inSpec);
    }

    /**
     * Returns the data table storing the allowed intervals and member counts.
     *
     * @return the data table storing the allowed intervals and member counts
     */
    public BufferedDataTable getSummaryTable() {
        return m_reviser.getSummaryTable();
    }

    /**
     * Returns the spec of the table storing the overview, i.e., permitted intervals, member counts.
     *
     * @param inSpec the spec of the input data table
     * @return the spec of the table storing the allowed intervals
     */
    public DataTableSpec getSummaryTableSpec(final DataTableSpec inSpec) {
        return m_reviser.getSummaryTableSpec(m_calculator.getIntervalsTableSpec(inSpec));
    }

    /**
     * Detects and treats the outliers.
     *
     * @param in the data table for which the outliers have to be detected
     * @param exec the execution context
     * @throws Exception if the execution failed, due to internal reasons or cancelation from the outside.
     */
    public void execute(final BufferedDataTable in, final ExecutionContext exec) throws Exception {

        // the intervals calculation progress
        final double intervalsProgress;
        if (m_calculator.inMemory()) {
            intervalsProgress = 0.4;
        } else {
            intervalsProgress = 0.8;
        }
        final double writeProgress = 0.02;
        final double treatmentProgrss = 1 - intervalsProgress - writeProgress;

        // calculate the permitted intervals
        final BufferedDataTable permittedIntervals =
            m_calculator.calculateIntervals(in, exec.createSubExecutionContext(intervalsProgress));

        // treat the outliers
        m_reviser.treatOutliers(exec.createSubExecutionContext(treatmentProgrss), in, permittedIntervals);

        exec.setProgress(1);
    }

}
