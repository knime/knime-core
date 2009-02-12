/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class PMCCFilterNodeModel extends NodeModel {
    
    /** Config key for threshold. */
    static final String CFG_THRESHOLD = "correlation_threshold";
    
    private double m_threshold = 1.0;
    
    /** Empty constructor, 2 ins, 1 out. */
    public PMCCFilterNodeModel() {
        super(new PortType[]{PMCCPortObjectAndSpec.TYPE, 
                BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        PMCCPortObjectAndSpec model = (PMCCPortObjectAndSpec)inData[0];
        BufferedDataTable in = (BufferedDataTable)inData[1];
        ColumnRearranger arranger = createColumnRearranger(in.getSpec(), model);
        BufferedDataTable out = exec.createColumnRearrangeTable(
                in, arranger, exec);
        return new BufferedDataTable[]{out};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PMCCPortObjectAndSpec modelS = (PMCCPortObjectAndSpec)inSpecs[0];
        DataTableSpec dataS = (DataTableSpec)inSpecs[1];
        if (m_threshold < 0.0 || m_threshold > 1.0) {
            throw new IllegalArgumentException(
                    "No valid threshold: " + m_threshold);
        }
        ColumnRearranger arranger = createColumnRearranger(dataS, modelS);
        if (arranger == null) {
            return null;
        }
        return new DataTableSpec[]{arranger.createSpec()};
    }
    
    private ColumnRearranger createColumnRearranger(
            final DataTableSpec spec, final PMCCPortObjectAndSpec pmccModel) 
        throws InvalidSettingsException {
        for (String c : pmccModel.getColNames()) {
            if (!spec.containsName(c)) {
                throw new InvalidSettingsException("No such column in input " 
                        + "table: " + c);
            }
        }
        if (!pmccModel.hasData()) { // settings ok but can't determine output 
            return null;
        }
        String[] includes = pmccModel.getReducedSet(m_threshold);
        HashSet<String> hash = new HashSet<String>(Arrays.asList(includes));
        ArrayList<String> includeList = new ArrayList<String>();
        HashSet<String> allColsInModel = new HashSet<String>(
                Arrays.asList(pmccModel.getColNames())); 
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
