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
 *   Jun 2, 2025 (david): created
 */
package org.knime.core.util.binning.numeric;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * Discretization configuration as a PMML translator, which can be used as in port objects for node in/outputs.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 *
 * @since 5.6
 */
public class PMMLPreprocDiscretizeTranslatorConfiguration {

    private final Map<String, List<NumericBin2>> m_binsByColumnName;

    private final List<String> m_orderedInputColumnNames;

    private final List<String> m_orderedOutputColumnNames;

    /**
     * Constructs a new Configuration with the specified bins indexed column name.
     *
     * @param orderedInputColumnNames a list of column names in the order they should be processed
     * @param orderedOutputColumnNames the output column names, which should match the orderedColumnNames
     * @param binsByColumnName a map of column names to their respective bins
     */
    public PMMLPreprocDiscretizeTranslatorConfiguration( //
        final List<String> orderedInputColumnNames, //
        final List<String> orderedOutputColumnNames, //
        final Map<String, List<NumericBin2>> binsByColumnName //
    ) {
        m_binsByColumnName = binsByColumnName;
        m_orderedInputColumnNames = orderedInputColumnNames;
        m_orderedOutputColumnNames = orderedOutputColumnNames;

        if (m_orderedOutputColumnNames.size() != m_orderedInputColumnNames.size()) {
            throw new IllegalArgumentException("Output column names must match the ordered column names.");
        }

        // also check that the keyset of m_binsByColumnName matches the ordered column names
        if (!m_binsByColumnName.keySet().containsAll(m_orderedInputColumnNames)
            || binsByColumnName.size() != m_orderedInputColumnNames.size()) {
            throw new IllegalArgumentException("Bins must be provided for all ordered column names.");
        }
    }

    /**
     * Sets the lower outlier bin name for all columns in this configuration. This is the bin that will contain any
     * value that is below the lower bound of the first bin.
     *
     * @param name the name of the lower outlier bin to be added
     * @return this, for method chaining
     */
    public PMMLPreprocDiscretizeTranslatorConfiguration withLowerOutlierBin(final String name) {
        // set the lower outlier bin name for all columns
        m_binsByColumnName.forEach((columnName, bins) -> {
            if (bins.isEmpty()) {
                throw new IllegalStateException(
                    "No bins defined for column " + columnName + ". Cannot set lower outlier bin name.");
            }

            var firstNonOutlierBin = bins.get(0);
            bins.add(0, new NumericBin2( //
                name, //
                false, //
                Double.NEGATIVE_INFINITY, //
                !firstNonOutlierBin.isLeftOpen(), //
                firstNonOutlierBin.getLeftValue() //
            ));
        });

        return this;
    }

    /**
     * Sets the upper outlier bin name for all columns in this configuration. This is the bin that will contain any
     * value that is above the upper bound of the last bin.
     *
     * @param name the name of the upper outlier bin to be added
     * @return this, for method chaining
     */
    public PMMLPreprocDiscretizeTranslatorConfiguration withUpperOutlierBin(final String name) {
        // set the upper outlier bin name for all columns
        m_binsByColumnName.forEach((columnName, bins) -> {
            if (bins.isEmpty()) {
                throw new IllegalStateException(
                    "No bins defined for column " + columnName + ". Cannot set upper outlier bin name.");
            }

            var lastNonOutlierBin = bins.get(bins.size() - 1);
            bins.add(new NumericBin2( //
                name, //
                !lastNonOutlierBin.isRightOpen(), //
                lastNonOutlierBin.getRightValue(), //
                false, //
                Double.POSITIVE_INFINITY //
            ));
        });

        return this;
    }

    /**
     * Use this configuration to create a {@link PMMLBinningTranslator}.
     *
     * @return the binning translator
     */
    public PMMLBinningTranslator2 toTranslator() {
        var mapper = new DerivedFieldMapper((DerivedField[])null);

        var stringInputOutputs = IntStream.range(0, m_orderedInputColumnNames.size()) //
            .mapToObj(i -> Map.entry(m_orderedInputColumnNames.get(i), m_orderedOutputColumnNames.get(i))) //
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var convertedBinsByColumnName = m_binsByColumnName.entrySet().stream() //
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(NumericBin2[]::new)));

        return new PMMLBinningTranslator2(convertedBinsByColumnName, stringInputOutputs, mapper);
    }

    /**
     * Get the names of the columns that are discretized and their respective discretizations.
     *
     * @return the map of column names to bins
     */
    public Map<String, List<NumericBin2>> getDiscretizations() {
        return Collections.unmodifiableMap(m_binsByColumnName);
    }

    /**
     * Get the names of the columns that are discretized.
     *
     * @return a set of column names
     */
    public List<String> getTargetColumnNames() {
        return Collections.unmodifiableList(m_orderedInputColumnNames);
    }

    /**
     * Get the names of the columns that are outputted by the discretization.
     *
     * @return a set of output column names
     */
    public List<String> getOutputColumnNames() {
        return Collections.unmodifiableList(m_orderedOutputColumnNames);
    }

    /**
     * Enumeration for the closure style of intervals (aka closedness of the margins) for creating PMML documents.
     */
    public enum ClosureStyle {

            /** Open both ends (a, b) */
            OPEN_OPEN, //
            /** Closed both ends [a, b] */
            CLOSED_CLOSED, //
            /** Open left end, closed right end (a, b] */
            OPEN_CLOSED, //
            /** Closed left end, open right end [a, b) */
            CLOSED_OPEN;

        /**
         * Creates a {@link ClosureStyle} based on the closedness of the left and right margins.
         *
         * @param leftClosed true if the left margin is closed, false if open
         * @param rightClosed true if the right margin is closed, false if open
         * @return the corresponding {@link ClosureStyle}
         */
        public static ClosureStyle from(final boolean leftClosed, final boolean rightClosed) {
            if (leftClosed && rightClosed) {
                return CLOSED_CLOSED;
            } else if (leftClosed) {
                return CLOSED_OPEN;
            } else if (rightClosed) {
                return OPEN_CLOSED;
            } else {
                return OPEN_OPEN;
            }
        }

        /**
         * @return true if this bin's right boundary is closed, false otherwise.
         */
        public boolean isClosedRight() {
            return this == CLOSED_CLOSED || this == OPEN_CLOSED;
        }

        /**
         * @return true if this bin's left boundary is closed, false otherwise.
         */
        public boolean isClosedLeft() {
            return this == CLOSED_CLOSED || this == CLOSED_OPEN;
        }
    }

    /**
     * Represents an interval with left and right margins and a closure style.
     *
     * @param leftMargin the left margin of the interval
     * @param rightMargin the right margin of the interval
     * @param closure the closure style of the interval
     */
    public static record Interval(double leftMargin, double rightMargin, ClosureStyle closure) {

        /**
         * Creates a new Interval with the specified margins and closure style. Check that the left margin is less than
         * or equal to the right margin.
         *
         * @param leftMargin the left margin of the interval
         * @param rightMargin the right margin of the interval
         * @param closure the closure style of the interval
         */
        public Interval {
            if (leftMargin > rightMargin) {
                throw new IllegalArgumentException("Left margin must be less than or equal to right margin.");
            }
        }

        /**
         * Checks if the given value is covered by this interval based on its closure style.
         *
         * @param value the value to check
         * @return true if the value is within the interval, false otherwise
         */
        public boolean covers(final double value) {
            return switch (closure) {
                case OPEN_OPEN -> value > leftMargin && value < rightMargin;
                case CLOSED_CLOSED -> value >= leftMargin && value <= rightMargin;
                case OPEN_CLOSED -> value > leftMargin && value <= rightMargin;
                case CLOSED_OPEN -> value >= leftMargin && value < rightMargin;
            };
        }

        /**
         * Converts this interval to a {@link NumericBin2} with the specified name.
         *
         * @param name the name of the bin
         * @return a new NumericBin2 instance representing this interval
         */
        public NumericBin2 toNumericBin(final String name) {
            return new NumericBin2(name, !closure.isClosedLeft(), leftMargin, !closure.isClosedRight(), rightMargin);
        }

        @Override
        public String toString() {
            var bottomClosed = closure == ClosureStyle.CLOSED_CLOSED || closure == ClosureStyle.CLOSED_OPEN;
            var topClosed = closure == ClosureStyle.CLOSED_CLOSED || closure == ClosureStyle.OPEN_CLOSED;

            var leftMarginStr = bottomClosed ? "[" : "(";
            var rightMarginStr = topClosed ? "]" : ")";

            return leftMarginStr + leftMargin + ", " + rightMargin + rightMarginStr;
        }
    }
}
