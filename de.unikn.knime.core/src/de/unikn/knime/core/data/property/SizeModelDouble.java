/* @(#)$RCSfile$ 
 * $Revision: 295 $ $Date: 2006-02-23 10:32:31 +0100 (Do, 23 Feb 2006) $ $Author: gabriel $
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   01.02.2006 (mb): created
 */
package de.unikn.knime.core.data.property;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.property.SizeHandler.SizeModel;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.config.Config;

/**
 * A Handler computing sizes of objects (rows) based on the double value of
 * a DataCell.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class SizeModelDouble implements SizeModel {

    /* store range of domain */
    private final double m_min;
    private final double m_max;

    /**
     * Create new SizeHandler based on double values and a given interval.
     * 
     * @param min minimum of domain
     * @param max maximum of domain
     */
    public SizeModelDouble(final double min, final double max) {
        assert min < max;
        m_min = min;
        m_max = max;
    }
    
    /**
     * Compute size based on actual value of this cell and the range
     * which was defined during contruction.
     * 
     * @param dc value to be used for size computation.
     * @return size in percent or -1 if cell type invalid or out of range
     * 
     * @see SizeHandler#getSize(DataCell)
     */
    public double getSize(final DataCell dc) {
        if (dc.isMissing()) {
            return -1;   // missing value: -1
        }
        if (dc.getType().isCompatible(DoubleValue.class)) {
            double d = ((DoubleValue)dc).getDoubleValue();
            if ((d < m_min) || (d > m_max)) {
                return -1;    // out of range!
            }
            return (d - m_min) / (m_max - m_min);
        }
        return -1;       // incomptible type: -1
    }
    
    public void save(final Config config) {
        config.addDouble("min", m_min);
        config.addDouble("max", m_max);
    }
    
    public static SizeModelDouble load(final Config config) 
            throws InvalidSettingsException {
        double min = config.getDouble("min");
        double max = config.getDouble("max");
        return new SizeModelDouble(min, max);
    }
    

}
