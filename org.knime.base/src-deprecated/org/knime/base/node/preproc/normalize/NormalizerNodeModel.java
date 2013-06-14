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
 * -------------------------------------------------------------------
 *
 * History
 *   19.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.normalize;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.data.normalize.AffineTransConfiguration;
import org.knime.base.data.normalize.AffineTransTable;
import org.knime.base.data.normalize.Normalizer;
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

/**
 * The NormalizeNodeModel uses the Normalizer to normalize the input DataTable.
 *
 * @see Normalizer
 * @author Nicolas Cebron, University of Konstanz
 */
public class NormalizerNodeModel extends NodeModel {

    /** Key to store the new minimum value (in min/max mode). */
    public static final String NEWMIN_KEY = "newmin";

    /** Key to store the new maximum value (in min/max mode). */
    public static final String NEWMAX_KEY = "newmax";

    /** Key to store the mode. */
    public static final String MODE_KEY = "mode";

    /** Key to store the columns to use. */
    public static final String COLUMNS_KEY = "columns";

    /** No Normalization mode. */
    public static final int NONORM_MODE = 0;

    /** MINMAX mode. */
    public static final int MINMAX_MODE = 1;

    /** ZSCORE mode. */
    public static final int ZSCORE_MODE = 2;

    /** DECIMAL SCALING mode. */
    public static final int DECIMALSCALING_MODE = 3;

    /** Default mode is NONORM mode. */
    private int m_mode = ZSCORE_MODE;

    /** Default minimum zero. */
    private double m_min = 0;

    /** Default maximum one. */
    private double m_max = 1;

    /** Columns used for normalization. */
    private String[] m_columns = null;

    /** Key to store if all numeric columns are used for normalization. */
    static final String CFG_USE_ALL_NUMERIC = "all_numeric_columns_used";

    /** All numeric columns are used for normalization. */
    private boolean m_allNumericColumns;

    /** The config key under which the model is stored. */
    static final String CFG_MODEL_NAME = "normalize";

    /**
     * Creates an new normalizer. One input, two outputs (one of which is the
     * model).
     */
    public NormalizerNodeModel() {
        this(NormalizerPortObject.TYPE);
    }

    /**
     * @param modelPortType the port type of the model
     */
    protected NormalizerNodeModel(final PortType modelPortType) {
        super(PMMLPortObject.TYPE.equals(modelPortType)
                ? new PortType[]{BufferedDataTable.TYPE,
                    new PortType(PMMLPortObject.class, true)}
                : new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE,
                modelPortType});
    }

    /**
     * @param inSpecs An array of DataTableSpecs (as many as this model has
     *            inputs).
     * @return An array of DataTableSpecs (as many as this model has outputs)
     *
     * @throws InvalidSettingsException if the <code>#configure()</code> failed,
     *             that is, the settings are inconsistent with given
     *             DataTableSpec elements.
     */
    protected PortObjectSpec[] prepareConfigure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec spec = (DataTableSpec)inSpecs[0];
        // extract selected numeric columns
        updateNumericColumnSelection(spec);
        if (m_mode == NONORM_MODE) {
            return new PortObjectSpec[]{spec, new DataTableSpec()};
        }
        DataTableSpec modelSpec =
            FilterColumnTable.createFilterTableSpec(spec, m_columns);
        return new PortObjectSpec[]{
                Normalizer.generateNewSpec(spec, m_columns), modelSpec};
    }

    /**
     * @return the columns used for normalization.
     */
    protected String[] getColumns() {
        return m_columns;
    }

    /**
     * All {@link org.knime.core.data.def.IntCell} columns are converted to
     * {@link org.knime.core.data.def.DoubleCell} columns.
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        return prepareConfigure(inSpecs);
    }

    /**
     * @param spec the data table spec
     * @throws InvalidSettingsException if no normalization mode is set
     */
    protected void updateNumericColumnSelection(final DataTableSpec spec)
            throws InvalidSettingsException {
        // if the node has not been configured before OR all columns have been
        // selected in the dialog, then return all numeric columns from the
        // input m_spec
        if (m_columns == null || m_allNumericColumns) {
            String[] allNumColumns = findAllNumericColumns(spec);
            // no normalization
            if (m_mode == NONORM_MODE) {
                super.setWarningMessage("No normalization mode set.");
            } else {
            // set warning when the node has not been configured (all
            // columns are used by default) OR all columns have been
            // selected previously in the dialog AND the current spec
            // contains more or less columns
                if (m_columns == null || (m_allNumericColumns
                        && !Arrays.deepEquals(m_columns, allNumColumns))) {
                    String mode;
                    switch (m_mode) {
                    case NONORM_MODE:
                        mode = "No Normalization";
                        break;
                    case MINMAX_MODE:
                        mode = "Min-Max Normalization";
                        break;
                    case ZSCORE_MODE:
                        mode = "Z-Score Normalization";
                        break;
                    case DECIMALSCALING_MODE:
                        mode = "Normalization by Decimal Scaling";
                        break;
                    default:
                        throw new InvalidSettingsException("No mode set");
                    }
                    super.setWarningMessage("All numeric columns are used "
                            + "for normalization. Mode: " + mode);
                }
            }
            m_columns = allNumColumns;
        }
        // sanity check: selected columns in actual spec?
        for (String name : m_columns) {
            if (!spec.containsName(name)) {
                throw new InvalidSettingsException("Could not"
                        + " find column \"" + name + "\""
                            + " in m_spec.");
            }
        }
        // no normalization
        if (m_mode == NONORM_MODE) {
            super.setWarningMessage("No normalization mode set.");
        }
    }

    /**
     * Finds all numeric columns in spec.
     * @param spec input table spec
     * @return array of numeric column names
     */
    static final String[] findAllNumericColumns(final DataTableSpec spec) {
        int nrcols = spec.getNumColumns();
        Vector<String> poscolumns = new Vector<String>();
        for (int i = 0; i < nrcols; i++) {
            if (spec.getColumnSpec(i).getType().isCompatible(
                    DoubleValue.class)) {
                poscolumns.add(spec.getColumnSpec(i).getName());
            }
        }
        return poscolumns.toArray(new String[poscolumns.size()]);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        CalculationResult result = calculate(inObjects, exec);
        NormalizerPortObject p = new NormalizerPortObject(
                result.getSpec(), result.getConfig());
        return new PortObject[] {result.getDataTable(), p};
    }

    /**
     * New normalized {@link org.knime.core.data.DataTable} is created depending
     * on the mode.
     */
    /**
     * @param inData The input data.
     * @param exec For BufferedDataTable creation and progress.
     * @return the result of the calculation
     * @throws Exception If the node calculation fails for any reason.
     */
    protected CalculationResult calculate(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        DataTableSpec inSpec = inTable.getSpec();
        // extract selected numeric columns
        updateNumericColumnSelection(inSpec);
        Normalizer ntable = new Normalizer(inTable, m_columns);

        int rowcount = inTable.getRowCount();
        ExecutionMonitor prepareExec = exec.createSubProgress(0.3);
        AffineTransTable outTable;
        boolean fixDomainBounds = false;
        switch (m_mode) {
        case NONORM_MODE:
            return new CalculationResult(inTable, new DataTableSpec(),
                    new AffineTransConfiguration());
        case MINMAX_MODE:
            fixDomainBounds = true;
            outTable = ntable.doMinMaxNorm(m_max, m_min, prepareExec);
            break;
        case ZSCORE_MODE:
            outTable = ntable.doZScoreNorm(prepareExec);
            break;
        case DECIMALSCALING_MODE:
            outTable = ntable.doDecimalScaling(prepareExec);
            break;
        default:
            throw new Exception("No mode set");
        }
        if (outTable.getErrorMessage() != null) {
            // something went wrong, report and throw an exception
            throw new Exception(outTable.getErrorMessage());
        }
        if (ntable.getErrorMessage() != null) {
            // something went wrong during initialization, report.
            setWarningMessage(ntable.getErrorMessage());
        }
        DataTableSpec modelSpec =
            FilterColumnTable.createFilterTableSpec(inSpec, m_columns);
        AffineTransConfiguration configuration = outTable.getConfiguration();
        DataTableSpec spec = outTable.getDataTableSpec();
        // fix the domain to min/max in case of MINMAX_MODE; fixes bug #1187
        // ideally this goes into the AffineTransConfiguration/AffineTransTable,
        // but that will not work with the applier node (which will apply
        // the same transformation, which is not guaranteed to snap to min/max)
        if (fixDomainBounds) {
            DataColumnSpec[] newColSpecs =
                new DataColumnSpec[spec.getNumColumns()];
            for (int i = 0; i < newColSpecs.length; i++) {
                newColSpecs[i] = spec.getColumnSpec(i);
            }
            for (int i = 0; i < m_columns.length; i++) {
                int index = spec.findColumnIndex(m_columns[i]);
                DataColumnSpecCreator creator =
                    new DataColumnSpecCreator(newColSpecs[index]);
                DataColumnDomainCreator domCreator =
                    new DataColumnDomainCreator(newColSpecs[index].getDomain());
                domCreator.setLowerBound(new DoubleCell(m_min));
                domCreator.setUpperBound(new DoubleCell(m_max));
                creator.setDomain(domCreator.createDomain());
                newColSpecs[index] = creator.createSpec();
            }
            spec = new DataTableSpec(spec.getName(), newColSpecs);
        }
        ExecutionMonitor normExec = exec.createSubProgress(.7);
        BufferedDataContainer container = exec.createDataContainer(spec);
        int count = 1;
        for (DataRow row : outTable) {
            normExec.checkCanceled();
            normExec.setProgress(count / (double)rowcount,
                    "Normalizing row no. " + count + " of " + rowcount
                    + " (\"" + row.getKey() + "\")");
            container.addRowToTable(row);
            count++;
        }
        container.close();
        return new CalculationResult(container.getTable(), modelSpec,
                configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_mode = settings.getInt(MODE_KEY);
        m_min = settings.getDouble(NEWMIN_KEY);
        m_max = settings.getDouble(NEWMAX_KEY);
        m_columns = settings.getStringArray(COLUMNS_KEY);
        m_allNumericColumns = settings.getBoolean(CFG_USE_ALL_NUMERIC, false);
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
        settings.addInt(MODE_KEY, m_mode);
        settings.addDouble(NEWMIN_KEY, m_min);
        settings.addDouble(NEWMAX_KEY, m_max);
        settings.addStringArray(COLUMNS_KEY, m_columns);
        settings.addBoolean(CFG_USE_ALL_NUMERIC, m_allNumericColumns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        int mode = settings.getInt(MODE_KEY);
        switch (mode) {
        case NONORM_MODE: break;
        case MINMAX_MODE: double min = settings.getDouble(NEWMIN_KEY);
                          double max = settings.getDouble(NEWMAX_KEY);
                          if (min > max) {
                              throw new InvalidSettingsException("New minimum"
                                   + " value should be smaller than new "
                                   + " maximum value.");
                          }
                          break;
        case ZSCORE_MODE: break;
        case DECIMALSCALING_MODE: break;
        default:
            throw new InvalidSettingsException("INVALID MODE");
        }
    }

    /**
     * Helper class for being able to return all necessary information in the
     * {@link #calculate(PortObject[], ExecutionContext)} method.

     * @author Dominik Morent, KNIME.com, Zurich, Switzerland
     */
    protected final class CalculationResult {
        private final BufferedDataTable m_dataTable;
        private final DataTableSpec m_spec;
        private final AffineTransConfiguration m_config;
        /**
         * @param m_dataTable
         * @param m_spec
         * @param config
         */
        private CalculationResult(final BufferedDataTable dataTable,
                final DataTableSpec spec,
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

    /**
     * @param columns the columns to set
     */
    protected void setColumns(final String[] columns) {
        m_columns = columns;
    }

    /**
     * @return the mode
     */
    protected int getMode() {
        return m_mode;
    }

}
