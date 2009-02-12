/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 20, 2008 (sellien): created
 */
package org.knime.base.util.coordinate;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;

/**
 * Interface for a mapping method which is executed before calculating ticks and
 * mapping.
 *
 * @author Stephan Sellien, University of Konstanz
 */
public interface MappingMethod {
    /**
     * Maps the value according to its task.
     *
     * @param cell the value to map
     * @return the mapped value
     */
    public DataCell doMapping(DataCell cell);

    /**
     * Maps a value back to the original domain value for tick generation.
     * Is the inverse function.
     * @param cell the value
     * @return the inverse value of cell according to this mapping
     */
    public double getLabel(DataCell cell);

    /**
     * Returns the display name of this {@link MappingMethod}.
     * @return the display name
     */
    public String getDisplayName();

    /**
     * Checks compatibility with a domain. Returns <code>true</code> by default.
     * @param domain the domain
     * @return <code>true</code>, if this mapping method is compatible.
     */
    public boolean isCompatibleWithDomain(final DataColumnDomain domain);
}
