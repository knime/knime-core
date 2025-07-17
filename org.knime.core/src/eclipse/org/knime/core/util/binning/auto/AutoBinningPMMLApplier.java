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
package org.knime.core.util.binning.auto;

import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.util.binning.numeric.NumericBin2;
import org.knime.core.util.binning.numeric.PMMLPreprocDiscretizeTranslatorConfiguration;

/**
 * Converts a {@link PMMLPreprocDiscretizeTranslatorConfiguration} into a {@link ColumnRearranger} and/or the output
 * from a NodeModel's execute method.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @since 5.6
 *
 * @see AutoBinningPMMLCreator
 */
public final class AutoBinningPMMLApplier {

    private AutoBinningPMMLApplier() {
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
        pmmlSpecCreator.setPreprocColNames(targetColumnNames);
        return pmmlSpecCreator.createSpec();
    }

    /**
     * Creates a {@link ColumnRearranger} that rearranges the columns of the input table according to the
     * {@link PMMLPreprocDiscretizeTranslatorConfiguration} given.
     *
     * @param config the configuration that contains the target column names, output column names and discretizations
     * @param inSpec the input table spec that contains the columns to discretize
     * @return a {@link ColumnRearranger} that rearranges the columns of the input table
     * @throws InvalidSettingsException if the input table spec does not contain all target columns
     */
    public static ColumnRearranger createColumnRearranger( //
        final PMMLPreprocDiscretizeTranslatorConfiguration config, //
        final DataTableSpec inSpec //
    ) throws InvalidSettingsException {

        // Check if columns to discretize exist
        for (String name : config.getTargetColumnNames()) {
            if (!inSpec.containsName(name)) {
                throw new InvalidSettingsException("Column " + "\"" + name + "\"" + "is missing in the input table.");
            }
        }

        var rearranger = new ColumnRearranger(inSpec);

        for (int i = 0; i < config.getDiscretizations().size(); ++i) {
            var inputColumnName = config.getTargetColumnNames().get(i);
            var outputColumnName = config.getOutputColumnNames().get(i);
            var possibleBins = config.getDiscretizations().get(inputColumnName);

            var cellFactory = new BinningCellFactory( //
                outputColumnName, //
                inSpec.findColumnIndex(inputColumnName), //
                possibleBins //
            );

            if (inputColumnName.equals(outputColumnName)) {
                rearranger.replace(cellFactory, inputColumnName);
            } else {
                rearranger.append(cellFactory);
            }
        }

        return rearranger;
    }

    /**
     * Creates the output for the execution of a NodeModel whose first output port is a {@link BufferedDataTable} and
     * the second output port is a {@link PMMLPortObject} that contains the PMML specification of the discretizations.
     *
     * @param inData the input data table that contains the columns to discretize
     * @param translatorConfig the configuration that contains the target column names, output column names and
     *            discretizations
     * @param exec the execution context that is used to create the output table and monitor/cancel progress
     * @return an array of {@link PortObject}s, the first element is a {@link BufferedDataTable} that contains the
     *         discretized columns and the second element is a {@link PMMLPortObject} that contains the PMML.
     * @throws CanceledExecutionException if the execution was canceled
     * @throws InvalidSettingsException if the input table spec does not contain all target columns or if the
     *             discretizations are not valid.
     */
    public static PortObject[] createExecutionOutput( //
        final BufferedDataTable inData, //
        final PMMLPreprocDiscretizeTranslatorConfiguration translatorConfig, //
        final ExecutionContext exec //
    ) throws CanceledExecutionException, InvalidSettingsException {
        var inSpec = inData.getDataTableSpec();

        var columnRearranger = createColumnRearranger(translatorConfig, inSpec);
        var outSpec = columnRearranger.createSpec();

        var table = exec.createColumnRearrangeTable(inData, columnRearranger, exec);

        var pmmlSpec = createPMMLOutSpec(outSpec, translatorConfig.getTargetColumnNames());
        var outputPmml = new PMMLPortObject(pmmlSpec);
        outputPmml.addGlobalTransformations(translatorConfig.toTranslator().exportToTransDict());

        return new PortObject[]{ //
            table, //
            outputPmml, //
        };
    }

    /**
     * A {@link SingleCellFactory} that creates a cell for each row in the data table, containing the bin for each row
     * in the specified target column.
     */
    static class BinningCellFactory extends SingleCellFactory {

        private final int m_targetColumnIndex;

        private final List<NumericBin2> m_bins;

        BinningCellFactory( //
            final String newColName, //
            final int targetColumnIndex, //
            final List<NumericBin2> binsIncludingOutliers //
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
