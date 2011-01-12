/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   May 10, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import org.knime.testing.node.differNode.DiffNodeDialog.Evaluators;

/**
 * 
 * @author ritmeier, University of Konstanz
 */
public class DiffNodeModel extends NodeModel {

    /** Config key for the evaluator. */
    public static final String CFGKEY_EVALUATORKEY = "TESTEVALUATOR";

    /** Config key for the lower tolerance. */
    public static final String CFGKEY_LOWERTOLERANCEKEY = "LOWERTOLLERANCE";

    /** Config key for the upper tolerance. */
    public static final String CFGKEY_UPPERERTOLERANCEKEY = "UPPERTOLLERANCE";

    private DataTable m_diffTable;

    private Evaluators m_evaluator = Evaluators.TableDiffer;

    private int m_lowerTolerance;

    private int m_upperTolerance;

    /**
     * Creates a model with two data inports. The first port for the new table,
     * the second forr the original ("golden") table.
     * 
     */
    public DiffNodeModel() {
        super(2, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFGKEY_EVALUATORKEY, m_evaluator == null ? ""
                : m_evaluator.name());
        if (m_evaluator != null
                && m_evaluator.equals(Evaluators.LearnerScoreComperator)) {
            settings.addInt(DiffNodeModel.CFGKEY_LOWERTOLERANCEKEY,
                    m_lowerTolerance);
            settings.addInt(DiffNodeModel.CFGKEY_UPPERERTOLERANCEKEY,
                    m_upperTolerance);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String evaluatorString = settings.getString(CFGKEY_EVALUATORKEY);
        Evaluators eval = null;
        try {
            eval = Evaluators.valueOf(evaluatorString);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException("no valid evaluator");
        }
        if (eval == null) {
            throw new InvalidSettingsException("no valid evaluator");
        }

        if (eval.equals(DiffNodeDialog.Evaluators.LearnerScoreComperator)) {
            settings.getInt(CFGKEY_LOWERTOLERANCEKEY);
            settings.getInt(CFGKEY_UPPERERTOLERANCEKEY);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String evaluatorString = settings.getString(CFGKEY_EVALUATORKEY);
        m_evaluator = DiffNodeDialog.Evaluators.valueOf(evaluatorString);
        if (m_evaluator
                .equals(DiffNodeDialog.Evaluators.LearnerScoreComperator)) {
            m_lowerTolerance = settings.getInt(CFGKEY_LOWERTOLERANCEKEY);
            m_upperTolerance = settings.getInt(CFGKEY_UPPERERTOLERANCEKEY);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        TestEvaluator eval = m_evaluator.getInstance();
        if (eval instanceof LearnerScoreComperator) {
            ((LearnerScoreComperator)eval).setTolerance(m_lowerTolerance,
                    m_upperTolerance);
        }
        eval.compare(inData[0], inData[1]);

        return new BufferedDataTable[]{};
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{};
    }

    /**
     * Returns the result of the table differ.
     * 
     * @return - the result of the table differ.
     */
    public DataTable getDiffTable() {
        return m_diffTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) throws IOException, 
            CanceledExecutionException {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) throws IOException, 
            CanceledExecutionException {
        // TODO Auto-generated method stub
        
    }
    
    

}
