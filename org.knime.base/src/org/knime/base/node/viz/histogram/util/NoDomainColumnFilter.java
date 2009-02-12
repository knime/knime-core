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
 *    12.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.util;

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
     * {@inheritDoc}
     */
    public String allFilteredMsg() {
        return "No domain values available";
    }

    /**
     * {@inheritDoc}
     */
    public boolean includeColumn(final DataColumnSpec colSpec) {
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
                return false;
            }
            return true;
        } else if (colSpec.getType().isCompatible(DoubleValue.class)) {
            if (domain.getLowerBound() == null
                    || domain.getUpperBound() == null) {
                return false;
            }
            final double lowerVal =
                ((DoubleValue)domain.getLowerBound()).getDoubleValue();
            if (Double.isInfinite(lowerVal) || Double.isNaN(lowerVal)) {
                return false;
            }
            final double upperVal =
                ((DoubleValue)domain.getUpperBound()).getDoubleValue();
            if (Double.isInfinite(upperVal) || Double.isNaN(upperVal)) {
                return false;
            }
            return true;
        }
        return false;
    }

}
