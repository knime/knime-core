/*
 * ------------------------------------------------------------------ *
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
 *   Jun 5, 2008 (wiswedel): created
 */
package org.knime.core.data.container;

import org.knime.core.data.RowIterator;

/**
 * A {@link RowIterator row iterator} that can be closed in order to save
 * resources. Iterator of this class are returned by tables created with a
 * {@link DataContainer} or {@link org.knime.core.node.BufferedDataContainer},
 * which typically read from file. If the iterator is not pushed to the end of
 * the table, the input stream is not closed, which can cause system failures.
 * This iterator allows the user to close the stream early on (before reaching
 * the end of the table in which case the stream is closed anyway).
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class CloseableRowIterator extends RowIterator {

    /** Closes this iterator. Subsequent calls of {@link RowIterator#hasNext()}
     * will return <code>false</code>. This method does not need to be called
     * if the iterator was pushed to the end (stream will be closed 
     * automatically). It's meant to be used in cases where the iterator might
     * not advance to the end of the table. 
     * 
     * <p>This method does nothing if the table is already closed (multiple
     * invocations are ignored). */
    public abstract void close();
}
