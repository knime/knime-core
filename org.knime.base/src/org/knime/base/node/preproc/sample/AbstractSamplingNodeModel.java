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
 */
package org.knime.base.node.preproc.sample;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.base.node.preproc.sample.SamplingNodeSettings.SamplingMethods;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
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

        if (temp.countMethod() == SamplingNodeSettings.CountMethods.Relative) {
            if (temp.fraction() <= 0.0 || temp.fraction() > 1.0) {
                NumberFormat f = NumberFormat.getPercentInstance(Locale.US);
                String p = f.format(100.0 * temp.fraction());
                throw new InvalidSettingsException("Invalid percentage: " + p);
            }
        } else if (temp.countMethod() == SamplingNodeSettings.CountMethods.Absolute) {
            if (temp.count() <= 0) {
                throw new InvalidSettingsException("Invalid count: "
                        + temp.count());
            }
        } else {
            throw new InvalidSettingsException("Unknown method: "
                    + temp.countMethod());
        }

        if (temp.samplingMethod().equals(SamplingMethods.Stratified)
                && (temp.classColumn() == null)) {
            throw new InvalidSettingsException(
                    "No class column for stratified sampling selected");
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
    protected RowFilter getSamplingRowFilter(final BufferedDataTable in,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            InvalidSettingsException {
        Random rand;
        if (m_settings.samplingMethod().equals(SamplingMethods.Random)
                || m_settings.samplingMethod().equals(
                        SamplingMethods.Stratified)) {
            rand =
                    m_settings.seed() != null ? new Random(m_settings.seed())
                            : new Random();
        } else {
            rand = null;
        }

        int rowCount;
        if (m_settings.countMethod().equals(
                SamplingNodeSettings.CountMethods.Relative)) {
            rowCount = (int)(m_settings.fraction() * in.getRowCount());
        } else {
            rowCount = m_settings.count();
        }

        RowFilter rowFilter;
        if (m_settings.samplingMethod().equals(SamplingMethods.Random)) {
            rowFilter = Sampler.createSampleFilter(in, rowCount, rand, exec);
        } else if (m_settings.samplingMethod().equals(
                SamplingMethods.Stratified)) {
            rowFilter =
                    new StratifiedSamplingRowFilter(in, m_settings
                            .classColumn(), rowCount, rand, exec);
        } else if (m_settings.samplingMethod().equals(SamplingMethods.Linear)) {
            rowFilter = new LinearSamplingRowFilter(in.getRowCount(), rowCount);
        } else {
            rowFilter = Sampler.createRangeFilter(rowCount);
        }
        return rowFilter;
    }

    /**
     * Has the node been configured, i.e. a method has been set
     *
     * @return <code>true</code> if the node is configured
     *
     * @deprecated use {@link #checkSettings(DataTableSpec)} instead because
     *             this also checks for the class column if stratified sampling
     *             has been selected
     */
    @Deprecated
    protected boolean hasBeenConfigured() {
        return m_settings.countMethod() != null;
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
        if (m_settings.countMethod() == null) {
            throw new InvalidSettingsException("No sampling method selected");
        }
        if (m_settings.samplingMethod().equals(SamplingMethods.Stratified)
                && !inSpec.containsName(m_settings.classColumn())) {
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
