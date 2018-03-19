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
 *   01.08.2005 (cebron): created
 */
package org.knime.base.node.preproc.shuffle;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.sort.Shuffler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Implementation of the Fisher Yates shuffle, that guarantees that all n!
 * possible outcomes are possible and equally likely. The shuffling procedure
 * requires only linear runtime. For further details see "Fisher-Yates shuffle",
 * from Dictionary of Algorithms and Data Structures, Paul E. Black, ed., NIST.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class ShuffleNodeModel extends NodeModel {

    /** Config identifier for seed field. */
    static final String CFG_SEED = "random_seed";

    /**
     * The seed to use or null to use always a different one.
     */
    private Long m_seed;

    /**
     *
     */
    public ShuffleNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return inSpecs;
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        Shuffler shuffler = new Shuffler(inData[0], m_seed == null ? new Random().nextLong() : m_seed);
        return new BufferedDataTable[]{shuffler.shuffle(exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
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
        String seedText = m_seed != null ? Long.toString(m_seed) : null;
        settings.addString(CFG_SEED, seedText);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        // seed was not available in knime 1.1.2, backward compatibility
        String seedText = settings.getString(CFG_SEED, null);
        if (seedText != null) {
            try {
                Long.parseLong(seedText);
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException("Unable to parse seed \""
                        + seedText + "\" as number.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        // seed was not available in knime 1.1.2, backward compatibility
        String seedText = settings.getString(CFG_SEED, null);
        if (seedText != null) {
            try {
                m_seed = Long.parseLong(seedText);
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException("Unable to parse seed \""
                        + seedText + "\" as number.");
            }
        } else {
            m_seed = null;
        }
    }

}
