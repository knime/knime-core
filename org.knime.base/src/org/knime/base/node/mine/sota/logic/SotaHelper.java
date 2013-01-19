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
