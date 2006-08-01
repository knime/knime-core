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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 23, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import org.knime.base.node.util.DataArray;
import org.knime.core.data.DataRow;

/**
 * The SotaHelper class helps the SotaManager to manage the tree. SotaHelper has
 * to be implemented for helping the SotaManager with Fuzzy or Number data.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public abstract class SotaHelper {
    private DataArray m_inDataContainer;

    private int m_dimension;

    /**
     * Constructor of SotaHelper, which sets the given rowContainer with the
     * training data.
     * 
     * @param rowContainer DataArray with the training data
     */
    public SotaHelper(final DataArray rowContainer) {
        m_inDataContainer = rowContainer;
        m_dimension = 0;
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
     */
    public abstract SotaTreeCell initializeTree();

    /**
     * Adjusts the given SotaTreeCell related to the given DataRow and
     * learningrate.
     * 
     * @param cell cell to adjust
     * @param row row to adjust the cell with
     * @param learningrate learningrate to adjust the cell with
     */
    public abstract void adjustSotaCell(SotaTreeCell cell, DataRow row,
            double learningrate);
}
