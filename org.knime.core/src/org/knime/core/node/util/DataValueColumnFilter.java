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
 *    12.03.2007 (Tobias Koetter): created
 */

package org.knime.core.node.util;

import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;


/**
 * This {@link ColumnFilter} implementation filters all value which are not
 * compatible to the provided {@link DataValue} classes.
 * @author Tobias Koetter, University of Konstanz
 */
public class DataValueColumnFilter implements ColumnFilter {

    
    private final Class<? extends DataValue>[] m_filterClasses;

    /**Constructor for class DataValueColumnFilter.
     * @param filterValueClasses classes derived from DataValue. 
     * All other columns will be filtered.
     */
    public DataValueColumnFilter(
            final Class<? extends DataValue>... filterValueClasses) {
        if (filterValueClasses == null || filterValueClasses.length == 0) {
            throw new NullPointerException("Classes must not be null");
        }
        List<Class<? extends DataValue>> list = 
            Arrays.asList(filterValueClasses);
        if (list.contains(null)) {
            throw new NullPointerException("List of value classes must not " 
                    + "contain null elements.");
        }
        m_filterClasses = filterValueClasses;
    }

    /**
     * {@inheritDoc}
     */
    public boolean includeColumn(final DataColumnSpec colSpec) {
        if (colSpec == null) {
            throw new NullPointerException(
                    "Column specification must not be null");
        }
        for (Class<? extends DataValue> cl : m_filterClasses) {
            if (colSpec.getType().isCompatible(cl)) {
               return true;
            }
        }
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public String allFilteredMsg() {
        StringBuffer error = new StringBuffer(
        "No column in spec compatible to");
        if (m_filterClasses.length == 1) {
            error.append(" \"");
            error.append(m_filterClasses[0].getSimpleName());
            error.append('"');
        } else {
            for (int i = 0; i < m_filterClasses.length; i++) {
                error.append(" \"");
                error.append(m_filterClasses[i].getSimpleName());
                error.append('"');
                if (i == m_filterClasses.length - 2) { // second last
                    error.append(" or");
                }
            }
        }
        error.append('.');
        return error.toString();
    }
}
