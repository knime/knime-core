/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   Apr 13, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.scorer.entrop;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;


/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
class EntropyNodeModel extends NodeModel {
    // DO NOT SWAP VALUES WITHOUT UPDATING THE XML!
    /** Inport port of the reference clustering. */
    static final int INPORT_REFERENCE = 0;

    /** Inport port of the clustering to judge. */
    static final int INPORT_CLUSTERING = 1;

    /** Config identifier: column name in reference table. */
    static final String CFG_REFERENCE_COLUMN = "reference_table_col_column";

    /** Config identifier: column name in clustering table. */
    static final String CFG_CLUSTERING_COLUMN = "clustering_table_col_column";

    private String m_referenceCol;

    private String m_clusteringCol;

    private EntropyCalculator m_calculator;

    private final HiLiteTranslator m_translator = new HiLiteTranslator();

    /**
     * The Entropy node model with two data inports for the two clustering
     * results.
     * @param enableOutput whether to enable output port (no outport in 1.x.x).
     */
    EntropyNodeModel(final boolean enableOutput) {
        super(2, enableOutput ? 1 : 0);
    }

    /**
     * Get the hilite handler that the view talks to. So far only needed to
     * trigger hilite events (unless there are duplicate views).
     * 
     * @return the hilite handler for the view
     */
    public HiLiteHandler getViewHiliteHandler() {
        return m_translator.getFromHiLiteHandler();
    }

    /**
     * @return get the statistics which has been created in the last execute or
     *         <code>null</code> if the node has been reset
     */
    public EntropyCalculator getCalculator() {
        return m_calculator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_referenceCol != null) {
            settings.addString(CFG_REFERENCE_COLUMN, m_referenceCol);
            settings.addString(CFG_CLUSTERING_COLUMN, m_clusteringCol);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(CFG_REFERENCE_COLUMN);
        settings.getString(CFG_CLUSTERING_COLUMN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_referenceCol = settings.getString(CFG_REFERENCE_COLUMN);
        m_clusteringCol = settings.getString(CFG_CLUSTERING_COLUMN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTable reference = inData[INPORT_REFERENCE];
        DataTable clustering = inData[INPORT_CLUSTERING];
        int referenceColIndex = reference.getDataTableSpec().findColumnIndex(
                m_referenceCol);
        int clusteringColIndex = clustering.getDataTableSpec().findColumnIndex(
                m_clusteringCol);
        m_calculator = new EntropyCalculator(reference, clustering,
                referenceColIndex, clusteringColIndex, exec);
        Map<RowKey, Set<RowKey>> map = m_calculator.getClusteringMap();
        m_translator.setMapper(new DefaultHiLiteMapper(map));
        if (getNrOutPorts() > 0) {
            BufferedDataTable out = exec.createBufferedDataTable(
                    m_calculator.getScoreTable(), exec);
            return new BufferedDataTable[]{out};
        }
        return new BufferedDataTable[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_calculator = null;
        m_translator.setMapper(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_referenceCol == null || m_clusteringCol == null) {
            throw new InvalidSettingsException(
                    "No auto configuration available\n"
                            + "Please configure in dialog.");

        }
        DataTableSpec reference = inSpecs[INPORT_REFERENCE];
        DataTableSpec clustering = inSpecs[INPORT_CLUSTERING];
        if (!reference.containsName(m_referenceCol)) {
            throw new InvalidSettingsException("Invalid reference column name "
                    + m_referenceCol);
        }
        if (!clustering.containsName(m_clusteringCol)) {
            throw new InvalidSettingsException(
                    "Invalid clustering column name " + m_clusteringCol);
        }
        if (getNrOutPorts() > 0) {
            return new DataTableSpec[]{EntropyCalculator.getScoreTableSpec()};
        }
        return new DataTableSpec[0];
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex, 
            final HiLiteHandler hiLiteHdl) {
        if (inIndex == 1) {
            m_translator.removeAllToHiliteHandlers();
            m_translator.addToHiLiteHandler(hiLiteHdl);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_translator.getFromHiLiteHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        try {
            m_calculator = EntropyCalculator.load(internDir, exec);
            m_translator.setMapper(new DefaultHiLiteMapper(m_calculator
                    .getClusteringMap()));
        } catch (InvalidSettingsException ise) {
            IOException ioe = new IOException("Unable to read settings.");
            ioe.initCause(ise);
            throw ioe;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        assert m_calculator != null;
        m_calculator.save(internDir, exec);
    }
}
