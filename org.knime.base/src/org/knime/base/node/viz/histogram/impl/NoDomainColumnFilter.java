/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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

package org.knime.base.node.viz.histogram.impl;

import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.util.ColumnFilter;


/**
 * This {@link ColumnFilter} implementation filters all columns which have
 * no {@link DataColumnDomain} information.
 * @author Tobias Koetter, University of Konstanz
 */
public final class NoDomainColumnFilter implements ColumnFilter {

    private static NoDomainColumnFilter instance;
    
    private NoDomainColumnFilter() {
        //private constructor to avoid object creation
    }
    
    /**
     * @return the single instance of this class
     */
    public static NoDomainColumnFilter getInstance() {
        if (instance == null) {
            instance = new NoDomainColumnFilter();
        }
        return instance;
    }
    
    /**
     * @see org.knime.core.node.util.ColumnFilter#allFilteredMsg()
     */
    public String allFilteredMsg() {
        return "No domain values available";
    }

    /**
     * @see org.knime.core.node.util.ColumnFilter#filterColumn(
     * org.knime.core.data.DataColumnSpec)
     */
    public boolean filterColumn(final DataColumnSpec colSpec) {
        if (colSpec == null) {
            throw new NullPointerException(
                    "Column specification must not be null");
        }
        final DataColumnDomain domain = colSpec.getDomain();
        if (domain == null) {
            return false;
        }
        if (colSpec.getType().isCompatible(NominalValue.class)) {
            if (domain.getValues() == null || domain.getValues().size() < 1) {
                return true;
            }
            return false;
        } else if (colSpec.getType().isCompatible(DoubleValue.class)) {
            if (domain.getLowerBound() == null 
                    || domain.getUpperBound() == null) {
                return true;
            }
            return false;
        }
        return true;
    }

}
