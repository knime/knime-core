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
