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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   16.04.2008 (gabriel): created
 */
package org.knime.core.node.property.hilite;

import java.util.Collections;
import java.util.Set;

import org.knime.core.data.RowKey;

/**
 * An abstract adapter class for handling hilite events. All methods in this 
 * class are empty.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class HiLiteHandlerAdapter implements HiLiteHandler {

    /**
     * {@inheritDoc}
     */
    public void addHiLiteListener(final HiLiteListener listener) {

    }

    /**
     * {@inheritDoc}
     */
    public void fireClearHiLiteEvent() {

    }

    /**
     * {@inheritDoc}
     */
    public void fireHiLiteEvent(final RowKey... ids) {

    }

    /**
     * {@inheritDoc}
     */
    public void fireHiLiteEvent(final Set<RowKey> ids) {

    }

    /**
     * {@inheritDoc}
     */
    public void fireUnHiLiteEvent(final RowKey... ids) {

    }

    /**
     * {@inheritDoc}
     */
    public void fireUnHiLiteEvent(final Set<RowKey> ids) {

    }

    /**
     * {@inheritDoc}
     */
    public Set<RowKey> getHiLitKeys() {
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isHiLit(final RowKey... ids) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void removeAllHiLiteListeners() {

    }

    /**
     * {@inheritDoc}
     */
    public void removeHiLiteListener(final HiLiteListener listener) {

    }

}
