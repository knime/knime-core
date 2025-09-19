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
 *   Jul 29, 2025 (david): created
 */
package org.knime.core.util.binning;

import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.DiscretizeBinDocument.DiscretizeBin;
import org.dmg.pmml.IntervalDocument.Interval.Closure;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.util.Pair;
import org.knime.core.util.binning.numeric.NumericBin;
import org.knime.core.util.binning.numeric.PMMLBinningTranslator;

/**
 * Converts a {@link PMMLPortObject} into a {@link ColumnRearranger}.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @since 5.8
 *
 * @see BinningUtil
 */
public final class BinningPMMLApplyUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(BinningPMMLApplyUtil.class);

    private BinningPMMLApplyUtil() {
        // Utility class, no instances allowed
    }

    /**
     * Creates a {@link PMMLPortObjectSpec} from a list of target column names and an output table spec. May be
     * convenient in NodeModel configure methods. The output table spec must contain all target columns.
     *
     * @param outputTableSpec the output table spec
     * @param targetColumnNames the target column names that should be included in the PMML spec, i.e. the subset of
     *            columns that should be binned.
     * @return a {@link PMMLPortObjectSpec} that contains the target columns
     * @throws InvalidSettingsException if the output table spec does not contain all target columns
     */
    public static PMMLPortObjectSpec createPMMLOutSpec( //
        final DataTableSpec outputTableSpec, //
        final List<String> targetColumnNames //
    ) throws InvalidSettingsException {
        if (!Arrays.asList(outputTableSpec.getColumnNames()).containsAll(targetColumnNames)) {
            throw new InvalidSettingsException(
                "Output table spec does not contain all target columns: this is an implementation error");
        }

        var pmmlSpecCreator = new PMMLPortObjectSpecCreator(outputTableSpec);
        pmmlSpecCreator.setTargetColsNames(targetColumnNames);
        return pmmlSpecCreator.createSpec();
    }

    /**
     * Creates a {@link ColumnRearranger} that rearranges the columns of the input table according to the
     * {@link PMMLPortObject} given.
     *
     * @param inSpec the input table spec that contains the columns to discretize
     * @param outputPmml the {@link PMMLPortObject} that contains the discretization information
     * @return a {@link ColumnRearranger} that rearranges the columns of the input table
     */
    public static ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final PMMLPortObject outputPmml) {
        final var rearranger = new ColumnRearranger(inSpec);
        final var derivedFields = outputPmml.getDerivedFields();
        final var derivedFieldMapper = new DerivedFieldMapper(derivedFields);

        var discretizeCount = 0;

        // Each derived field corresponds to the binning of one column
        for (int i = 0; i < derivedFields.length; i++) {
            final var derivedField = derivedFields[i];
            final var discretize = derivedField.getDiscretize();
            if (discretize == null) {
                LOGGER.warn(
                    String.format("Derived field \"%s\" at index %d does not contain a discretize element, skipping.",
                        derivedField.getName(), i));
                continue;
            }
            discretizeCount++;
            final var targetColumnName = discretize.getField();
            final var targetColumnIndex = inSpec.findColumnIndex(targetColumnName);
            if (targetColumnIndex < 0) {
                throw new IllegalStateException("Input column " + targetColumnName + " not found in input spec.");
            }
            var possibleBins = derivedField.getDiscretize().getDiscretizeBinList().stream()
                .map(BinningPMMLApplyUtil::discretizeBinToNumericBin).toList();
            final var outputColumnName = derivedFieldMapper.getColumnName(derivedField.getName());

            var cellFactory = new BinningCellFactory( //
                outputColumnName, //
                targetColumnIndex, //
                possibleBins //
            );

            if (inSpec.findColumnIndex(outputColumnName) < 0) {
                rearranger.append(cellFactory);
            } else {
                rearranger.replace(cellFactory, outputColumnName);
            }
        }

        if (discretizeCount == 0) {
            throw new IllegalArgumentException("PMML does not contain any derived fields with binning information.");
        }
        return rearranger;

    }

    /**
     * This method reverts the translation in {@link PMMLBinningTranslator}
     *
     * @param bin the {@link DiscretizeBin} to convert
     * @return a {@link NumericBin2} that represents the same bin
     */
    private static NumericBin discretizeBinToNumericBin(final DiscretizeBin bin) {
        final var interval = bin.getInterval();
        final var leftRightOpen = getBoundariesOpenValues(interval.getClosure());
        final var leftValue = interval.isSetLeftMargin() ? interval.getLeftMargin() : Double.NEGATIVE_INFINITY;
        final var rightValue = interval.isSetRightMargin() ? interval.getRightMargin() : Double.POSITIVE_INFINITY;

        return new NumericBin( //
            bin.getBinValue(), //
            leftRightOpen.getFirst(), //
            leftValue, //
            leftRightOpen.getSecond(), //
            rightValue //
        );
    }

    /**
     * Duplicated from DefaultDBBinner
     */
    private static Pair<Boolean, Boolean> getBoundariesOpenValues(final Closure.Enum closure) {
        if (closure == Closure.OPEN_CLOSED) {
            return new Pair<>(true, false);
        } else if (closure == Closure.OPEN_OPEN) {
            return new Pair<>(true, true);
        } else if (closure == Closure.CLOSED_OPEN) {
            return new Pair<>(false, true);
        } else if (closure == Closure.CLOSED_CLOSED) {
            return new Pair<>(false, false);
        } else {
            return new Pair<>(true, false);
        }
    }

    /**
     * A {@link SingleCellFactory} that creates a cell for each row in the data table, containing the bin for each row
     * in the specified target column.
     */
    static class BinningCellFactory extends SingleCellFactory {

        private final int m_targetColumnIndex;

        private final List<NumericBin> m_bins;

        BinningCellFactory( //
            final String newColName, //
            final int targetColumnIndex, //
            final List<NumericBin> binsIncludingOutliers //
        ) {
            super(new DataColumnSpecCreator(newColName, StringCell.TYPE).createSpec());
            m_targetColumnIndex = targetColumnIndex;
            m_bins = binsIncludingOutliers;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            var value = row.getCell(m_targetColumnIndex);

            if (value.isMissing()) {
                return value;
            }

            var firstMatchingBin = m_bins.stream() //
                .filter(bin -> bin.covers(value)) //
                .findFirst();

            if (firstMatchingBin.isEmpty()) {
                throw new IllegalStateException(
                    "No bin found for value " + value + " in column " + m_targetColumnIndex);
            } else {
                return StringCellFactory.create(firstMatchingBin.get().getBinName());
            }
        }
    }
}
