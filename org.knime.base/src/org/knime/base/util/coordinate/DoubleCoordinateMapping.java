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
 *   02.02.2006 (sieb): created
 */
package org.knime.base.util.coordinate;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * Holds the original value according to the domain and its mapping.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class DoubleCoordinateMapping extends CoordinateMapping {

    /**
     * Constructs a coordinate mapping.
     *
     * @param stringDomainValue the domain value as string
     * @param domainValue the domain value
     * @param mappingValue the corresponding mapped value
     */
    DoubleCoordinateMapping(final String stringDomainValue,
            final DoubleValue domainValue, final double mappingValue) {
        super(stringDomainValue, mappingValue);
        setValues(domainValue);
    }

    /**
     * Constructs a coordinate mapping.
     *
     * @param stringDomainValue the domain value as string
     * @param domainValue the domain value
     * @param mappingValue the corresponding mapped value
     */
    public DoubleCoordinateMapping(final String stringDomainValue,
            final double domainValue, final double mappingValue) {
        super(stringDomainValue, mappingValue);
        setValues(new DoubleCell(domainValue));
    }
}
