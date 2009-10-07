/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   06.10.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract;

import java.util.Collection;

import org.knime.core.data.container.ColumnRearranger;
import org.knime.timeseries.node.extract.date.DateFieldExtractorNodeModel;
import org.knime.timeseries.node.extract.time.TimeFieldExtractorNodeModel;

/**
 * Helper class to bundle an appropriately configured {@link ColumnRearranger} 
 * and the actually used {@link AbstractTimeExtractorCellFactory}s.
 * This is used in order to retrieve the number of produced missing values at 
 * the end of the execute method, when the data was produced.
 * @see DateFieldExtractorNodeModel
 * @see TimeFieldExtractorNodeModel
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class SingleCellFactoryCompound {
    
    private final ColumnRearranger m_rearranger;
    
    private final Collection<AbstractTimeExtractorCellFactory>
        m_usedCellFactories;
    
    
    /**
     * 
     * @param rearranger the column rearranger appropriately configured
     * @param usedCellFactories the used cell factories
     */
    public SingleCellFactoryCompound(final ColumnRearranger rearranger,
            final Collection<AbstractTimeExtractorCellFactory> 
                usedCellFactories) {
        if (rearranger == null) {
            throw new IllegalArgumentException(
                    "Column rearranger must not be null!");
        }
        if (usedCellFactories == null) {
            throw new IllegalArgumentException(
                    "Used cell factories may be empty, but must not be null!");
        }
        m_rearranger = rearranger;
        m_usedCellFactories = usedCellFactories;
    }

    /**
     * 
     * @return the column rearranger
     */
    public ColumnRearranger getColumnRearranger() {
        return m_rearranger;
    }
    
    /**
     * 
     * @return the actually used cell factories
     */
    public Collection<AbstractTimeExtractorCellFactory> getUsedCellFactories() {
        return m_usedCellFactories;
    }
    
}
