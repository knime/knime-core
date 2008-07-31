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
    
    private final double[] m_min;
    
    private final double[] m_max;
    
    private double[] m_scales;
    
    private double[] m_translations;
    
     /**
     * Creates new row iterator given an AffineTransTable with its informations.
     * 
     * @param originalTable the original table that will be normalized.
     * @param table the AffineTransformTable containing the information to 
     * normalize the input data.
     */
    AffineTransRowIterator(final DataTable originalTable,
            final AffineTransTable table) {
        if (table == null || table == null) {
            throw new NullPointerException("Arguments must not be null.");
        }
       m_transtable = table;
       m_min = m_transtable.getMin();
       m_max = m_transtable.getMax();
       m_scales = m_transtable.getScales();
       m_translations = m_transtable.getTranslations();
       m_it = originalTable.iterator();
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
        final DataRow in = m_it.next();
        final DataCell[] cells = new DataCell[in.getNumCells()];
        for (int i = 0; i < cells.length; i++) {
            final DataCell oldCell = in.getCell(i);
            if (oldCell.isMissing() || Double.isNaN(m_scales[i])) {
                cells[i] = oldCell;
            } else {
                double interval = m_max[i] - m_min[i];
                double oldDouble = ((DoubleValue)oldCell).getDoubleValue();
                double newDouble = m_scales[i] * oldDouble + m_translations[i];
                if (!Double.isNaN(m_min[i])) {
                    if (newDouble < m_min[i]) {
                        if ((m_min[i] - newDouble) 
                                / interval < AffineTransTable.VERY_SMALL) {
                            newDouble = m_min[i];
                        } else {
                            m_transtable
                                    .setErrorMessage(
                                            "Normalized value is out of bounds."
                                            + " Original value: "
                                            + oldDouble
                                            + " Transformed value: "
                                            + newDouble
                                            + " Lower Bound: "
                                            + m_min[i]);
                        }
                    }
                }
                if (!Double.isNaN(m_max[i])) {
                    if (newDouble > m_max[i]) {
                        if ((newDouble - m_max[i]) 
                                / interval < AffineTransTable.VERY_SMALL) {
                            newDouble = m_max[i];
                        } else {
                            m_transtable.setErrorMessage(
                                    "Normalized value is out of bounds."
                                            + " Original value: "
                                            + oldDouble
                                            + " Transformed value: "
                                            + newDouble
                                            + " Upper Bound: "
                                            + m_max[i]);
                        }
                    }
                }
                cells[i] = new DoubleCell(newDouble);
            }
        }
        return new DefaultRow(in.getKey(), cells);
    }
}
