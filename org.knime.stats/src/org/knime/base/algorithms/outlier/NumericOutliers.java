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
 *   Feb 15, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.algorithms.outlier;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.knime.base.algorithms.outlier.listeners.NumericOutlierWarningListener;
import org.knime.base.algorithms.outlier.options.NumericOutliersDetectionOption;
import org.knime.base.algorithms.outlier.options.NumericOutliersReplacementStrategy;
import org.knime.base.algorithms.outlier.options.NumericOutliersTreatmentOption;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * The algorithm to identify and treat outliers based on the interquartile range.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class NumericOutliers {

    /** Maximum number of outlier columns to print. */
    private static final int MAX_PRINT = 3;

    /** The summary prefix. */
    private static final String SUMMARY_PREFIX = "Outlier treatment for columns: ";

    /** The intervals calculator. */
    private final NumericOutliersIntervalsCalculator m_calculator;

    /** The outlier reviser. */
    private final NumericOutliersReviser m_reviser;

    /** The port object. */
    private NumericOutliersPortObject m_outlierPort;

    /** The table after all outliers have been treated. */
    private BufferedDataTable m_outTable;

    /**
     * Builder of the NumericOutliers.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public final static class Builder {

        /** The builder of the outlier intervals calculator. */
        private final NumericOutliersIntervalsCalculator.Builder m_intervalsBuilder;

        /** The builder of the outlier reviser calculator. */
        private final NumericOutliersReviser.Builder m_reviserBuilder;

        /** The list managing the warning listeners. */
        private final List<NumericOutlierWarningListener> m_listener;

        /**
         * Constructor initializting all builders and setting the outlier column names.
         *
         * @param outlierColNames the outlier column names to be used
         */
        public Builder(final String[] outlierColNames) {
            m_intervalsBuilder = new NumericOutliersIntervalsCalculator.Builder(outlierColNames);
            m_reviserBuilder = new NumericOutliersReviser.Builder();
            m_listener = new LinkedList<NumericOutlierWarningListener>();
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
         * Defines how outlier have to be treated, see {@link NumericOutliersTreatmentOption}.
         *
         * @param treatment the treatment option to be used
         * @return the builder itself
         */
        public Builder setTreatmentOption(final NumericOutliersTreatmentOption treatment) {
            m_reviserBuilder.setTreatmentOption(treatment);
            return this;
        }

        /**
         * Defines the outlier replacement strategy, see {@link NumericOutliersReplacementStrategy}.
         *
         * @param repStrategy the replacement strategy
         * @return the builder itself
         */
        public Builder setReplacementStrategy(final NumericOutliersReplacementStrategy repStrategy) {
            m_reviserBuilder.setReplacementStrategy(repStrategy);
            return this;
        }

        /**
         * Defines the outlier detection option, see {@link NumericOutliersDetectionOption}
         *
         * @param detectionOption the detection option
         * @return the builder itself
         */
        public Builder setDetectionOption(final NumericOutliersDetectionOption detectionOption) {
            m_reviserBuilder.setDetectionOption(detectionOption);
            return this;
        }

        /**
         * Sets the group column names.
         *
         * @param groupColNames the group column names
         * @return the builder itself
         */
        public Builder setGroupColumnNames(final String[] groupColNames) {
            m_intervalsBuilder.setGroupColumnNames(groupColNames);
            return this;
        }

        /**
         * Sets the quartiles computation type.
         *
         * @param useHeuristic the accuracy to be used
         * @return the builder itself
         */
        public Builder useHeuristic(final boolean useHeuristic) {
            m_intervalsBuilder.useHeuristic(useHeuristic);
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
         * Sets the interquartile range multiplier.
         *
         * @param iqrMultiplier the interquartile multiplier
         * @return the builder itself
         */
        public Builder setIQRMultiplier(final double iqrMultiplier) {
            m_intervalsBuilder.setIQRMultiplier(iqrMultiplier);
            return this;
        }

        /**
         * Adds a listener that gets triggered whenever the {@link NumericOutliers} creates a warning.
         *
         * @param listener the listener to be added
         * @return the builder itself
         */
        public Builder addWarningListener(final NumericOutlierWarningListener listener) {
            m_listener.add(listener);
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
         * Constructs the {@link NumericOutliers} using the settings provided by the builder.
         *
         * @return the {@link NumericOutliers} using the settings provided by the builder
         */
        public NumericOutliers build() {
            return new NumericOutliers(this);
        }

    }

    /**
     * Constructor.
     *
     * @param b the builder providing all settings
     */
    private NumericOutliers(final Builder b) {
        m_calculator = b.m_intervalsBuilder.build();
        m_reviser = b.m_reviserBuilder.build();
        b.m_listener.forEach(l -> m_reviser.addListener(l));
    }

    /**
     * Returns the outlier free data table.
     *
     * @return the outlier free data table
     */
    public BufferedDataTable getOutTable() {
        return m_outTable;
    }

    /**
     * Returns the spec of the outlier free data table.
     *
     * @param inSpec the spec of the input data table
     * @return the spec of the outlier free data table
     */
    public static DataTableSpec getOutTableSpec(final DataTableSpec inSpec) {
        return NumericOutliersReviser.getOutTableSpec(inSpec);
    }

    /**
     * Returns the data table storing the permitted intervals and additional information about member counts.
     *
     * @return the data table storing the summary
     */
    public BufferedDataTable getSummaryTable() {
        return m_reviser.getSummaryTable();
    }

    /**
     * Returns the spec of the table storing the permitted intervals and additional information about member counts.
     *
     * @param inSpec the in spec
     * @param groupColNames the group column names
     *
     * @return the spec of the data table storing the summary
     */
    public static DataTableSpec getSummaryTableSpec(final DataTableSpec inSpec, final String[] groupColNames) {
        return NumericOutliersReviser.getSummaryTableSpec(inSpec, groupColNames);
    }

    /**
     * Returns the outlier port object.
     *
     * @return the outlier port object
     */
    public NumericOutliersPortObject getOutlierPort() {
        return m_outlierPort;
    }

    /**
     * Returns the oulier port spec.
     *
     * @param inSpec the in spec
     * @param groupColNames the group column names
     * @param outlierColNames the outlier column names
     * @return the outlier port spec
     */
    public static PortObjectSpec getOutlierPortSpec(final DataTableSpec inSpec, final String[] groupColNames,
        final String[] outlierColNames) {
        return NumericOutliersPortObject.getPortSpec(inSpec, groupColNames, outlierColNames);
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
            intervalsProgress = 0.6;
        } else {
            intervalsProgress = 0.8;
        }
        final double treatmentProgress = 1 - intervalsProgress;

        // calculate the permitted intervals
        final NumericOutliersModel permittedIntervals =
            m_calculator.calculatePermittedIntervals(in, exec.createSubExecutionContext(intervalsProgress));

        m_outlierPort = new NumericOutliersPortObject(
            SUMMARY_PREFIX + ConvenienceMethods
                .getShortStringFrom(Arrays.asList(permittedIntervals.getOutlierColNames()), MAX_PRINT),
            in.getDataTableSpec(), permittedIntervals, m_reviser);

        // treat the outliers
        m_outTable = m_reviser.treatOutliers(exec.createSubExecutionContext(treatmentProgress), in, permittedIntervals);

        exec.setProgress(1);
    }

}