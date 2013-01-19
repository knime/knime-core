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
 *   18.04.2005 (cebron): created
 */
package org.knime.base.node.viz.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.knime.base.data.statistics.StatisticsTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The StatisticsNodeModel creates a new StatisticTable based on the input data
 * table.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class StatisticsNodeModel extends NodeModel {
    /*
     * Statistical values obtained from input DataTable.
     */
    private double[] m_min;

    private double[] m_max;

    private double[] m_mean;

    private double[] m_stddev;

    private double[] m_variance;

    /*
     * Column Names
     */
    private String[] m_columnNames;

    /**
     * One input, no output.
     */
    StatisticsNodeModel() {
        super(1, 0);
    }

    /**
     * Output table is like the input table. After we are executed we can
     * deliver a better table spec. But not before.
     * 
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{};
    }

    /**
     * Computes the statistics for the DataTable at the inport. Use the view on
     * this node to see them.
     * 
     * @see org.knime.core.node.NodeModel
     *      #execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        StatisticsTable statTable = new StatisticsTable(inData[0], exec);
        m_min = statTable.getdoubleMin();
        m_max = statTable.getdoubleMax();
        m_mean = statTable.getMean();
        m_stddev = statTable.getStandardDeviation();
        m_variance = statTable.getVariance();

        DataTableSpec inspec = inData[0].getDataTableSpec();
        m_columnNames = new String[inspec.getNumColumns()];
        int position = 0;
        for (DataColumnSpec colspec : inspec) {
            m_columnNames[position] = colspec.getName();
            position++;
        }
        return new BufferedDataTable[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_min = null;
        m_max = null;
        m_mean = null;
        m_stddev = null;
        m_variance = null;
        m_columnNames = null;
    }

    /**
     * @return Returns the column Names.
     */
    String[] getColumnNames() {
        return m_columnNames;
    }

    /**
     * @return Returns the max.
     */
    double[] getMax() {
        return m_max;
    }

    /**
     * @return Returns the mean.
     */
    double[] getMean() {
        return m_mean;
    }

    /**
     * @return Returns the min.
     */
    double[] getMin() {
        return m_min;
    }

    /**
     * @return Returns the stddev.
     */
    double[] getStddev() {
        return m_stddev;
    }

    /**
     * @return Returns the variance.
     */
    double[] getVariance() {
        return m_variance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, "Statistics");
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
        int nrCols = in.readInt();
        m_columnNames = new String[nrCols];
        for (int i = 0; i < nrCols; i++) {
            m_columnNames[i] = in.readUTF();
        }
        m_min = new double[nrCols];
        for (int i = 0; i < nrCols; i++) {
            m_min[i] = in.readDouble();
        }
        m_max = new double[nrCols];
        for (int i = 0; i < nrCols; i++) {
            m_max[i] = in.readDouble();
        }
        m_mean = new double[nrCols];
        for (int i = 0; i < nrCols; i++) {
            m_mean[i] = in.readDouble();
        }
        m_stddev = new double[nrCols];
        for (int i = 0; i < nrCols; i++) {
            m_stddev[i] = in.readDouble();
        }
        m_variance = new double[nrCols];
        for (int i = 0; i < nrCols; i++) {
            m_variance[i] = in.readDouble();
        }
        in.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, "Statistics");
        ObjectOutputStream out = 
            new ObjectOutputStream(new FileOutputStream(f));
        int nrCols = m_columnNames.length;
        out.writeInt(nrCols);
        for (String colname : m_columnNames) {
            out.writeUTF(colname);
        }
        for (double min : m_min) {
            out.writeDouble(min);
        }
        for (double max : m_max) {
            out.writeDouble(max);
        }
        for (double mean : m_mean) {
            out.writeDouble(mean);
        }
        for (double stddev : m_stddev) {
            out.writeDouble(stddev);
        }
        for (double variance : m_variance) {
            out.writeDouble(variance);
        }
        out.close();
    }
}
