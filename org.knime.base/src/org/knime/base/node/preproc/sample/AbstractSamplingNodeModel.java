/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 */
package org.knime.base.node.preproc.sample;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * NodeModel implementation to sample rows from an input table, thus, this node
 * has one inport. The number of outports is defined by the derived class
 *
 * @see SamplingNodeModel
 * @see org.knime.base.node.preproc.partition.PartitionNodeModel
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class AbstractSamplingNodeModel extends NodeModel {
    private final SamplingNodeSettings m_settings = new SamplingNodeSettings();

    /**
     * Empty constructor, defines one inport and a given number of outports.
     *
     * @param outs the number of outports
     */
    public AbstractSamplingNodeModel(final int outs) {
        super(1, outs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SamplingNodeSettings temp = new SamplingNodeSettings();
        temp.loadSettingsFrom(settings, false);

        if (temp.method() == SamplingNodeSettings.Methods.Relative) {
            if (temp.fraction() <= 0.0 || temp.fraction() > 1.0) {
                NumberFormat f = NumberFormat.getPercentInstance(Locale.US);
                String p = f.format(100.0 * temp.fraction());
                throw new InvalidSettingsException("Invalid percentage: " + p);
            }
        } else if (temp.method() == SamplingNodeSettings.Methods.Absolute) {
            if (temp.count() <= 0) {
                throw new InvalidSettingsException("Invalid count: "
                        + temp.count());
            }
        } else {
            throw new InvalidSettingsException("Unknown method: "
                    + temp.method());
        }

        if (temp.stratifiedSampling() && (temp.classColumn() == null)) {
            throw new InvalidSettingsException(
                    "No class column for stratified sampling selected");
        }
        if (temp.stratifiedSampling() && !temp.random()) {
            throw new InvalidSettingsException(
                    "Stratified sampling only works with random row selection");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings, false);
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
     * Method to be used in the execute method to determine the row filter for
     * the sampling.
     *
     * @param in the data table from the inport
     * @param exec the execution monitor to check for cancelation
     * @return a row filter for sampling according to current settings
     * @throws CanceledExecutionException if exec request canceling
     * @throws InvalidSettingsException if current settings are invalid
     */
    protected RowFilter getSamplingRowFilter(final DataTable in,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            InvalidSettingsException {
        Random rand;
        if (m_settings.random()) {
            rand =
                    m_settings.seed() != null ? new Random(m_settings.seed())
                            : new Random();
        } else {
            rand = null;
        }

        RowFilter rowFilter;
        if (m_settings.method() == SamplingNodeSettings.Methods.Absolute) {
            if (rand != null) {
                if (m_settings.stratifiedSampling()) {
                    rowFilter = new StratifiedSamplingRowFilter(in,
                            m_settings.classColumn(), m_settings.count(),
                            rand, exec);
                } else {
                    rowFilter =
                            Sampler.createSampleFilter(in, m_settings.count(),
                                    rand, exec);
                }
            } else {
                rowFilter = Sampler.createRangeFilter(m_settings.count());
            }
        } else if (m_settings.method() == SamplingNodeSettings.Methods.Relative) {
            if (rand != null) {
                if (m_settings.stratifiedSampling()) {
                    rowFilter = new StratifiedSamplingRowFilter(in,
                            m_settings.classColumn(), m_settings.fraction(),
                            rand, exec);
                } else {
                    rowFilter =
                            Sampler.createSampleFilter(in, m_settings
                                    .fraction(), rand, exec);
                }
            } else {
                rowFilter =
                        Sampler.createRangeFilter(in, m_settings.fraction(),
                                exec);
            }
        } else {
            throw new InvalidSettingsException("Unknown method: "
                    + m_settings.method());
        }
        return rowFilter;
    }

    /**
     * Has the node been configured, i.e. a method has been set
     *
     * @return <code>true</code> if the node is configured
     *
     * @deprecated use {@link #checkSettings(DataTableSpec)} instead because
     * this also checks for the class column if stratified sampling has been
     * selected
     */
    @Deprecated
    protected boolean hasBeenConfigured() {
        return m_settings.method() != null;
    }


    /**
     * Checks if the node settings are valid, i.e. a method has been set and the
     * class column exists if stratified sampling has been chosen.
     *
     * @param inSpec the input table's spec
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected void checkSettings(final DataTableSpec inSpec)
        throws InvalidSettingsException {
        if (m_settings.method() == null) {
            throw new InvalidSettingsException("No sampling method selected");
        }
        if (m_settings.stratifiedSampling() && !inSpec
                        .containsName(m_settings.classColumn())) {
            throw new InvalidSettingsException("Column '"
                    + m_settings.classColumn() + "' for stratified sampling "
                    + "does not exist");
        }
    }

    /**
     * Returns the settings of this object.
     *
     * @return a reference to the the settings
     */
    public SamplingNodeSettings getSettings() {
        return m_settings;
    }
}
