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
 *   Nov 23, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import org.knime.base.node.util.DataArray;
import org.knime.core.data.DataRow;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * The SotaHelper class helps the SotaManager to manage the tree. SotaHelper has
 * to be implemented for helping the SotaManager with Fuzzy or Number data.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public abstract class SotaHelper {
    
    private ExecutionMonitor m_exec;
    
    private DataArray m_inDataContainer;

    private int m_dimension;

    /**
     * Constructor of SotaHelper, which sets the given rowContainer with the
     * training data.
     * 
     * @param rowContainer DataArray with the training data
     * @param exec the <code>ExecutionMonitor</code> to set.
     */
    public SotaHelper(final DataArray rowContainer, 
            final ExecutionMonitor exec) {
        m_inDataContainer = rowContainer;
        m_dimension = 0;
        m_exec = exec;
    }

    /**
     * Returns the DataArray with the training data.
     * 
     * @return the DataArray with the training data
     */
    public DataArray getRowContainer() {
        return m_inDataContainer;
    }

    /**
     * Returns the dimension.
     * 
     * @return the dimension
     */
    public int getDimension() {
        return m_dimension;
    }

    /**
     * Sets the given dimension.
     * 
     * @param dimension dimension to set
     */
    protected void setDimension(final int dimension) {
        m_dimension = dimension;
    }

    /**
     * Returns the number of a specific type of DataCells in a RowContainers
     * row. What specific type of DataCells can be specified in the concrete
     * implementation (i.e. Fuzzy or Number).
     * 
     * @return the number of a specific type of DataCells
     */
    public abstract int initializeDimension();

    /**
     * Initializes the Sota tree with specific SotaCells like SotaFuzzyCell or
     * SotaDoubleCell. Which kind of SotaCell is used is specified in the
     * concrete implementation.
     * 
     * @return the initialized tree with a ancestor node and two children cells
     * @throws CanceledExecutionException if execution was canceled.
     */
    public abstract SotaTreeCell initializeTree() 
        throws CanceledExecutionException;

    /**
     * Adjusts the given <code>SotaTreeCell</code> related to the given 
     * DataRow and learningrate and assigns the given class.
     * 
     * @param cell cell to adjust
     * @param row row to adjust the cell with
     * @param learningrate learningrate to adjust the cell with
     * @param cellClass The class to assign to the cell.
     */
    public abstract void adjustSotaCell(final SotaTreeCell cell, 
            final DataRow row, final double learningrate, 
            final String cellClass);

    /**
     * @return the <code>ExecutionMonitor</code>
     */
    protected ExecutionMonitor getExec() {
        return m_exec;
    }

    /**
     * @param exec the <code>ExecutionMonitor</code> to set
     */
    protected void setExec(final ExecutionMonitor exec) {
        this.m_exec = exec;
    }
}
