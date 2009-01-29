/*
 * ---------------------------------------------------------------------
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
 */
package org.knime.base.data.filter.row;

import org.knime.core.data.DataRow;

/**
 * Generator interface for filtering {@link org.knime.core.data.DataRow}s
 * from a {@link org.knime.core.data.DataTable}.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface FilterRowGenerator {
    /**
     * @param row the data row to check
     * @return <code>true</code> if the given <code>row</code> belongs to
     *         the set of included rows
     */
    boolean isIn(final DataRow row);
}
