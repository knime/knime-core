/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

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
    private int m_mode = NONORM_MODE;

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
        super(new PortType[]{BufferedDataTable.TYPE}, 
                new PortType[]{BufferedDataTable.TYPE, 
                NormalizerPortObject.TYPE});
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
        DataTableSpec spec = (DataTableSpec)inSpecs[0];
        // extract selected numeric columns
        m_columns = numericColumnSelection(spec);
        if (m_mode == NONORM_MODE) {
            return new PortObjectSpec[]{spec, new DataTableSpec()};
        }
        DataTableSpec modelSpec = 
            FilterColumnTable.createFilterTableSpec(spec, m_columns);
        return new PortObjectSpec[]{
                Normalizer.generateNewSpec(spec, m_columns), modelSpec};
    }

    private String[] numericColumnSelection(final DataTableSpec spec)
            throws InvalidSettingsException {
        // if the node has not been configured before OR all columns have been
        // selected in the dialog, then return all numeric columns from the 
        // input spec
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
                    super.setWarningMessage(
                        "All numeric columns are used for normalization.");
                }
            }
            return allNumColumns;
        }
        // sanity check: selected columns in actual spec?
        for (String name : m_columns) {
            if (!spec.containsName(name)) {
                throw new InvalidSettingsException("Could not"
                        + " find column \"" + name + "\""
                            + " in spec.");
            }
        }
        // no normalization
        if (m_mode == NONORM_MODE) {
            super.setWarningMessage("No normalization mode set.");
        }
        return m_columns;
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
     * New normalized {@link org.knime.core.data.DataTable} is created depending
     * on the mode.
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        DataTableSpec inSpec = inTable.getSpec();
        // extract selected numeric columns
        m_columns = numericColumnSelection(inSpec);
        Normalizer ntable = new Normalizer(inTable, m_columns);
        int rowcount = inTable.getRowCount();
        ExecutionMonitor prepareExec = exec.createSubProgress(0.3);
        AffineTransTable outTable;

        switch (m_mode) {
        case NONORM_MODE:
            NormalizerPortObject p = new NormalizerPortObject(
                    new DataTableSpec(), new AffineTransConfiguration());
            return new PortObject[]{inTable, p};
        case MINMAX_MODE:
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
        DataTableSpec modelSpec = 
            FilterColumnTable.createFilterTableSpec(inSpec, m_columns);
        AffineTransConfiguration configuration = outTable.getConfiguration();
        NormalizerPortObject modelPO = 
            new NormalizerPortObject(modelSpec, configuration);

        ExecutionMonitor normExec = exec.createSubProgress(.7);
        BufferedDataContainer container =
                exec.createDataContainer(outTable.getDataTableSpec());
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
        return new PortObject[]{container.getTable(), modelPO};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

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
}
