/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
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
 * @see de.unikn.knime.dev.node.partition.PartitionNodeModel
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
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
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
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings, false);
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * @see NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * @see NodeModel#saveInternals(File, ExecutionMonitor)
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
        RowFilter rowFilter;
        if (m_settings.method() == SamplingNodeSettings.Methods.Absolute) {
            if (m_settings.random()) {
                Random r = m_settings.seed() != null ? new Random(m_settings
                        .seed()) : new Random();
                rowFilter = Sampler.createSampleFilter(in, m_settings.count(),
                        r, exec);
            } else {
                rowFilter = Sampler.createRangeFilter(m_settings.count());
            }
        } else if (m_settings.method() == SamplingNodeSettings.Methods.Relative) {
            if (m_settings.random()) {
                Random r = m_settings.seed() != null ? new Random(m_settings
                        .seed()) : new Random();
                rowFilter = Sampler.createSampleFilter(in, m_settings
                        .fraction(), r, exec);
            } else {
                rowFilter = Sampler.createRangeFilter(in,
                        m_settings.fraction(), exec);
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
     */
    protected boolean hasBeenConfigured() {
        return m_settings.method() != null;
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
