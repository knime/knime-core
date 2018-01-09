/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   19.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.normalize3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.data.normalize.AffineTransConfiguration;
import org.knime.base.data.normalize.AffineTransTable;
import org.knime.base.data.normalize.Normalizer2;
import org.knime.base.data.normalize.NormalizerPortObject;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * The Normalizer3NodeModel uses the Normalizer to normalize the input DataTable.
 *
 * @see Normalizer2
 * @author Nicolas Cebron, University of Konstanz
 * @author Marcel Hanser, University of Konstanz
 */
public class Normalizer3NodeModel extends NodeModel {
    private static final int MAX_UNKNOWN_COLS = 3;

    /** Configuration. */
    private NormalizerConfig m_config;

    /**
     * Creates an new normalizer. One input, two outputs (one of which is the model).
     */
    public Normalizer3NodeModel() {
        this(NormalizerPortObject.TYPE);
    }

    /**
     * @param modelPortType the port type of the model
     */
    protected Normalizer3NodeModel(final PortType modelPortType) {
        super(PMMLPortObject.TYPE.equals(modelPortType)
            ? new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE_OPTIONAL}
            : new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE, modelPortType});
    }

    /**
     * All {@link org.knime.core.data.def.IntCell} columns are converted to {@link org.knime.core.data.def.DoubleCell}
     * columns.
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec spec = (DataTableSpec)inSpecs[0];
        // extract selected numeric columns
        String[] columns = getIncludedComlumns(spec);
        DataTableSpec modelSpec = FilterColumnTable.createFilterTableSpec(spec, columns);
        return new PortObjectSpec[]{Normalizer2.generateNewSpec(spec, columns), modelSpec};
    }

    /**
     * Finds all numeric columns in spec.
     *
     * @param spec input table spec
     * @return array of numeric column names
     */
    static final String[] findAllNumericColumns(final DataTableSpec spec) {
        int nrcols = spec.getNumColumns();
        List<String> poscolumns = new ArrayList<String>();
        for (int i = 0; i < nrcols; i++) {
            if (spec.getColumnSpec(i).getType().isCompatible(DoubleValue.class)) {
                poscolumns.add(spec.getColumnSpec(i).getName());
            }
        }
        return poscolumns.toArray(new String[poscolumns.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        CalculationResult result = calculate(inObjects, exec);
        NormalizerPortObject p = new NormalizerPortObject(result.getSpec(), result.getConfig());
        return new PortObject[]{result.getDataTable(), p};
    }

    /**
     * New normalized {@link org.knime.core.data.DataTable} is created depending on the mode.
     */
    /**
     * @param inData The input data.
     * @param exec For BufferedDataTable creation and progress.
     * @return the result of the calculation
     * @throws Exception If the node calculation fails for any reason.
     */
    protected CalculationResult calculate(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        DataTableSpec inSpec = inTable.getSpec();
        // extract selected numeric columns
        String[] includedColumns = getIncludedComlumns(inSpec);
        Normalizer2 ntable = new Normalizer2(inTable, includedColumns);

        long rowcount = inTable.size();
        ExecutionContext prepareExec = exec.createSubExecutionContext(0.3);
        AffineTransTable outTable;
        boolean fixDomainBounds = false;
        switch (m_config.getMode()) {
            case MINMAX:
                fixDomainBounds = true;
                outTable = ntable.doMinMaxNorm(m_config.getMax(), m_config.getMin(), prepareExec);
                break;
            case Z_SCORE:
                outTable = ntable.doZScoreNorm(prepareExec);
                break;
            case DECIMALSCALING:
                outTable = ntable.doDecimalScaling(prepareExec);
                break;
            default:
                throw new InvalidSettingsException("No mode set");
        }
        if (outTable.getErrorMessage() != null) {
            // something went wrong, report and throw an exception
            throw new Exception(outTable.getErrorMessage());
        }
        if (ntable.getErrorMessage() != null) {
            // something went wrong during initialization, report.
            setWarningMessage(ntable.getErrorMessage());
        }

        DataTableSpec modelSpec = FilterColumnTable.createFilterTableSpec(inSpec, includedColumns);
        AffineTransConfiguration configuration = outTable.getConfiguration();
        DataTableSpec spec = outTable.getDataTableSpec();
        // fix the domain to min/max in case of MINMAX_MODE; fixes bug #1187
        // ideally this goes into the AffineTransConfiguration/AffineTransTable,
        // but that will not work with the applier node (which will apply
        // the same transformation, which is not guaranteed to snap to min/max)
        if (fixDomainBounds) {
            DataColumnSpec[] newColSpecs = new DataColumnSpec[spec.getNumColumns()];
            for (int i = 0; i < newColSpecs.length; i++) {
                newColSpecs[i] = spec.getColumnSpec(i);
            }
            for (int i = 0; i < includedColumns.length; i++) {
                int index = spec.findColumnIndex(includedColumns[i]);
                DataColumnSpecCreator creator = new DataColumnSpecCreator(newColSpecs[index]);
                DataColumnDomainCreator domCreator = new DataColumnDomainCreator(newColSpecs[index].getDomain());
                domCreator.setLowerBound(new DoubleCell(m_config.getMin()));
                domCreator.setUpperBound(new DoubleCell(m_config.getMax()));
                creator.setDomain(domCreator.createDomain());
                newColSpecs[index] = creator.createSpec();
            }
            spec = new DataTableSpec(spec.getName(), newColSpecs);
        }
        ExecutionMonitor normExec = exec.createSubProgress(.7);
        BufferedDataContainer container = exec.createDataContainer(spec);
        long count = 1;
        for (DataRow row : outTable) {
            normExec.checkCanceled();
            normExec.setProgress(count / (double)rowcount, "Normalizing row no. " + count + " of " + rowcount + " (\""
                + row.getKey() + "\")");
            container.addRowToTable(row);
            count++;
        }
        container.close();
        return new CalculationResult(container.getTable(), modelSpec, configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config = new NormalizerConfig();
        m_config.loadConfigurationInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config != null) {
            m_config.saveSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config = new NormalizerConfig();
        m_config.loadConfigurationInModel(settings);
    }

    /**
     * @param spec the data table spec
     * @throws InvalidSettingsException if no normalization mode is set
     * @return the included columns
     */
    private String[] getIncludedComlumns(final DataTableSpec spec) throws InvalidSettingsException {
        boolean hasGuessedDefaults = false;
        if (m_config == null) {
            NormalizerConfig config = new NormalizerConfig();
            config.guessDefaults(spec);
            hasGuessedDefaults = true;
            m_config = config;
        }

        FilterResult filterResult = m_config.getDataColumnFilterConfig().applyTo(spec);
        String[] includes = filterResult.getIncludes();

        if (includes.length == 0) {
            StringBuilder warnings = new StringBuilder("No columns included - input stays unchanged.");
            if (filterResult.getRemovedFromIncludes().length > 0) {
                warnings.append("\nThe following columns were included before but no longer exist:\n");
                warnings.append(ConvenienceMethods.getShortStringFrom(
                    Arrays.asList(filterResult.getRemovedFromIncludes()), MAX_UNKNOWN_COLS));
            }
            setWarningMessage(warnings.toString());
        } else if (hasGuessedDefaults) {
            setWarningMessage("Auto-configure: [0, 1] normalization on all numeric columns: "
                    + ConvenienceMethods.getShortStringFrom(Arrays.asList(includes), MAX_UNKNOWN_COLS));
        }

        return includes;
    }

    /**
     * Helper class for being able to return all necessary information in the
     * {@link #calculate(PortObject[], ExecutionContext)} method.
     *
     * @author Dominik Morent, KNIME AG, Zurich, Switzerland
     */
    private static final class CalculationResult {
        private final BufferedDataTable m_dataTable;

        private final DataTableSpec m_spec;

        private final AffineTransConfiguration m_config;

        /**
         * @param m_dataTable
         * @param m_spec
         * @param config
         */
        private CalculationResult(final BufferedDataTable dataTable, final DataTableSpec spec,
            final AffineTransConfiguration config) {
            super();
            this.m_dataTable = dataTable;
            this.m_spec = spec;
            this.m_config = config;
        }

        /**
         * @return the m_dataTable
         */
        public BufferedDataTable getDataTable() {
            return m_dataTable;
        }

        /**
         * @return the m_spec
         */
        public DataTableSpec getSpec() {
            return m_spec;
        }

        /**
         * @return the config
         */
        public AffineTransConfiguration getConfig() {
            return m_config;
        }
    }
}
