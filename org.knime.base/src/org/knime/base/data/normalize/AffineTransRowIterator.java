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
