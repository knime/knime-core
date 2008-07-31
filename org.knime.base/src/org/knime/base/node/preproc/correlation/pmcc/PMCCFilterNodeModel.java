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
 *   18.02.2007 (wiswedel): created
 */
package org.knime.base.node.preproc.correlation.pmcc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class PMCCFilterNodeModel extends NodeModel {
    
    static final String CFG_THRESHOLD = "correlation_threshold";
    static final String CFG_MODEL = "correlation_model";
    
    private PMCCModel m_pmccModel;
    private double m_threshold = 1.0;
    
    public PMCCFilterNodeModel() {
        super(1, 1, 1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger arranger = 
            createColumnRearranger(inData[0].getDataTableSpec());
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], 
                arranger, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_pmccModel == null) {
            throw new InvalidSettingsException("No model available");
        }
        if (m_threshold < 0.0 || m_threshold > 1.0) {
            throw new IllegalArgumentException(
                    "No valid threshold: " + m_threshold);
        }
        ColumnRearranger arranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{arranger.createSpec()};
    }
    
    private ColumnRearranger createColumnRearranger(final DataTableSpec spec) 
        throws InvalidSettingsException {
        String[] includes = m_pmccModel.getReducedSet(m_threshold);
        HashSet<String> hash = new HashSet<String>(Arrays.asList(includes));
        ArrayList<String> includeList = new ArrayList<String>();
        HashSet<String> allColsInModel = new HashSet<String>(
                Arrays.asList(m_pmccModel.getColNames())); 
        ArrayList<String> allColsInSpec = new ArrayList<String>();
        for (DataColumnSpec s : spec) {
            String name = s.getName();
            // must not exclude columns which are not covered by the model
            if (!(s.getType().isCompatible(DoubleValue.class) 
                    || s.getType().isCompatible(NominalValue.class))
                    || !allColsInModel.contains(name)) {
                includeList.add(name);
                continue;
            } else {
                allColsInSpec.add(name);
                if (hash.contains(name)) {
                    includeList.add(name);
                }
            }
        }
        // sanity check if all numeric columns in spec are also in the model
        allColsInModel.removeAll(allColsInSpec);
        if (!allColsInModel.isEmpty()) {
            throw new InvalidSettingsException("Some columns are not present in"
                    + " the input table: " + allColsInModel.iterator().next());
        }
        ColumnRearranger result = new ColumnRearranger(spec);
        result.keepOnly(includeList.toArray(new String[includeList.size()]));
        return result;
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
    protected void loadModelContent(final int index, 
            final ModelContentRO predParams) throws InvalidSettingsException {
        assert index == 0 : "Invalid model index: " + index;
        if (predParams == null) {
            m_pmccModel = null;
        } else {
            m_pmccModel = PMCCModel.load(predParams);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_threshold = settings.getDouble(CFG_THRESHOLD);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addDouble(CFG_THRESHOLD, m_threshold);
        if (m_pmccModel != null) {
            NodeSettingsWO modelSet = settings.addNodeSettings(CFG_MODEL);
            m_pmccModel.save(modelSet);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        double d = settings.getDouble(CFG_THRESHOLD);
        if (d <= 0.0 || d > 1.0) {
            throw new InvalidSettingsException(
                    "Invalid correlation measure threshold: " + d);
        }
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

}
