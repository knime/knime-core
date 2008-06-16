/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 13, 2008 (sellien): created
 */
package org.knime.base.util.coordinate;

import org.knime.core.data.DataColumnSpec;

/**
 * Interface for a coordinate factory.
 *
 * @author Stephan Sellien, University of Konstanz
 */
public interface CoordinateFactory {
    /**
     * Factory method for creating a {@link Coordinate} according to a
     * {@link DataColumnSpec}.
     *
     * @param columnSpec the {@link DataColumnSpec}
     * @return the according {@link Coordinate}, or <code>null</code> if for
     * some reason an appropriate coordinate cannot be created
     */
    public Coordinate createCoordinate(DataColumnSpec columnSpec);
}
