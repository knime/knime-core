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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.mine.smote;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SmoteNodeModel extends NodeModel {
    /** NodeSettings key for method. */
    public static final String CFG_METHOD = "method";

    /** NodeSettings key for oversampling rate. */
    public static final String CFG_RATE = "rate";

    /** NodeSettings key for target column. */
    public static final String CFG_CLASS = "class";

    /** NodeSettings key for kNN parameter. */
    public static final String CFG_KNN = "kNN";

    /** Method: oversample all classes equally to a given rate. */
    public static final String METHOD_ALL = "oversample_all";

    /** Method: oversample only minority classes. */
    public static final String METHOD_MAJORITY = "oversample_equal";

    private String m_method;

    private double m_rate;

    private String m_class;

    private int m_kNN;

    /**
     * Default constructor which sets one input, one output port.
     */
    public SmoteNodeModel() {
        super(1, 1);
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_class != null) {
            settings.addString(CFG_METHOD, m_method);
            settings.addDouble(CFG_RATE, m_rate);
            settings.addString(CFG_CLASS, m_class);
            settings.addInt(CFG_KNN, m_kNN);
        }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadSettings(settings, false);
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadSettings(settings, true);
    }

    private void loadSettings(final NodeSettingsRO settings, 
            final boolean write) throws InvalidSettingsException {
        String method = settings.getString(CFG_METHOD);
        double rate = 1.0;
        String clas = settings.getString(CFG_CLASS);
        int kNN = settings.getInt(CFG_KNN);
        if (METHOD_ALL.equals(method)) {
            // must be in there
            rate = settings.getDouble(CFG_RATE);
        } else if (METHOD_MAJORITY.equals(method)) {
            // may be in there
            rate = settings.getDouble(CFG_RATE, m_rate);
        } else {
            throw new InvalidSettingsException("Unknown method: " + method);
        }
        if (kNN < 1) {
            throw new InvalidSettingsException("Invalid #neighbors: " + kNN);
        }
        if (rate <= 0.0) {
            throw new InvalidSettingsException("Rate illegal: " + rate);
        }
        if (write) {
            m_method = method;
            m_rate = rate;
            m_class = clas;
            m_kNN = kNN;
        }
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        BufferedDataTable in = inData[0];
        Smoter smoter = new Smoter(in, m_class, exec);
        if (m_method.equals(METHOD_ALL)) {
            // count number of rows to add
            int nrRowsToAdd = 0;
            for (Iterator<DataCell> it = smoter.getClassValues(); 
                it.hasNext();) {
                int count = smoter.getCount(it.next());
                nrRowsToAdd += (int)(count * m_rate);
            }
            int currentRow = 0;
            for (Iterator<DataCell> it = smoter.getClassValues(); 
                it.hasNext();) {
                DataCell cur = it.next();
                int count = smoter.getCount(cur);
                int newCount = (int)(count * m_rate);
                exec.setMessage("Smoting '" + cur.toString() + "'");
                ExecutionMonitor subExec = exec.createSubProgress(newCount
                        / (double)nrRowsToAdd);
                smoter.smote(cur, newCount, m_kNN, subExec);
                currentRow += newCount;
            }
        } else if (m_method.equals(METHOD_MAJORITY)) {
            DataCell majority = smoter.getMajorityClass();
            int majorityCount = smoter.getCount(majority);
            Iterator<DataCell> it = smoter.getClassValues();
            int nrRowsToAdd = 0;
            while (it.hasNext()) {
                DataCell cur = it.next();
                nrRowsToAdd += (majorityCount - smoter.getCount(cur));
            }
            it = smoter.getClassValues();
            while (it.hasNext()) {
                DataCell cur = it.next();
                int count = smoter.getCount(cur);
                int newCount = majorityCount - count;
                exec.setMessage("Smoting '" + cur.toString() + "'");
                ExecutionMonitor subExec = exec.createSubProgress(newCount
                        / (double)nrRowsToAdd);
                smoter.smote(cur, newCount, m_kNN, subExec);
            }
        }
        smoter.close();
        DataTable out = smoter.getSmotedTable();
        return new BufferedDataTable[]{exec.createBufferedDataTable(out, exec)};
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
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        if (!inSpec.containsName(m_class)) {
            throw new InvalidSettingsException("No such column: " + m_class);
        }
        if (!inSpec.containsCompatibleType(DoubleValue.class)) {
            throw new InvalidSettingsException("No double column in data.");
        }
        DataTableSpec outSpec = Smoter.createFinalSpec(inSpec);
        return new DataTableSpec[]{outSpec};
    }
}
