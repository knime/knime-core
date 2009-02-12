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
 *   29.07.2005 (bernd): created
 */
package org.knime.base.data.normalize;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;


/**
 * RowIterator that wraps another iterator and performs an affine
 * transformation, i.e. y = a*x + b where a and be b are parameters, x the input
 * value and y the transformed output.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AffineTransRowIterator extends RowIterator {
   
    private final AffineTransTable m_transtable;
    
    private final RowIterator m_it;
    
     /**
     * Creates new row iterator given an AffineTransTable with its informations.
     * 
     * @param originalTable the original table that will be normalized.
     * @param table the AffineTransformTable containing the information to 
     * normalize the input data.
     */
    AffineTransRowIterator(final DataTable originalTable,
            final AffineTransTable table) {
        if (originalTable == null || table == null) {
            throw new NullPointerException("Arguments must not be null.");
        }
        m_it = originalTable.iterator();
        m_transtable = table;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_it.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        AffineTransConfiguration config = m_transtable.getConfiguration();
        int[] indices = m_transtable.getIndicesInConfiguration();
        double[] scales = config.getScales();
        double[] translations = config.getTranslations();
        double[] min = config.getMin();
        double[] max = config.getMax();
        final DataRow in = m_it.next();
        final DataCell[] cells = new DataCell[in.getNumCells()];
        for (int i = 0; i < cells.length; i++) {
            final DataCell oldCell = in.getCell(i);
            if (oldCell.isMissing() || indices[i] == -1) {
                cells[i] = oldCell;
            } else {
                int index = indices[i];
                double interval = max[index] - min[index];
                double oldDouble = ((DoubleValue)oldCell).getDoubleValue();
                double newDouble =  
                    scales[index] * oldDouble + translations[index];
                if (!Double.isNaN(min[index])) {
                    if (newDouble < min[index]) {
                        if ((min[index] - newDouble) 
                                / interval < AffineTransTable.VERY_SMALL) {
                            newDouble = min[index];
                        } else {
                            m_transtable
                                    .setErrorMessage(
                                            "Normalized value is out of bounds."
                                            + " Original value: "
                                            + oldDouble
                                            + " Transformed value: "
                                            + newDouble
                                            + " Lower Bound: "
                                            + min[index]);
                        }
                    }
                }
                if (!Double.isNaN(max[index])) {
                    if (newDouble > max[index]) {
                        if ((newDouble - max[index]) 
                                / interval < AffineTransTable.VERY_SMALL) {
                            newDouble = max[index];
                        } else {
                            m_transtable.setErrorMessage(
                                    "Normalized value is out of bounds."
                                            + " Original value: "
                                            + oldDouble
                                            + " Transformed value: "
                                            + newDouble
                                            + " Upper Bound: "
                                            + max[index]);
                        }
                    }
                }
                cells[i] = new DoubleCell(newDouble);
            }
        }
        return new DefaultRow(in.getKey(), cells);
    }
}
