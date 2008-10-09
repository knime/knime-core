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
public class HiLiteHandlerAdapter extends HiLiteHandler {
    
    /**
     * Create default hilite handler.
     */
    public HiLiteHandlerAdapter() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHiLiteListener(final HiLiteListener listener) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireClearHiLiteEvent() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireHiLiteEvent(final RowKey... ids) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireHiLiteEvent(final Set<RowKey> ids) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireUnHiLiteEvent(final RowKey... ids) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireUnHiLiteEvent(final Set<RowKey> ids) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RowKey> getHiLitKeys() {
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHiLit(final RowKey... ids) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllHiLiteListeners() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeHiLiteListener(final HiLiteListener listener) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fireClearHiLiteEventInternal(final KeyEvent event) {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fireHiLiteEventInternal(final KeyEvent event) {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fireUnHiLiteEventInternal(final KeyEvent event) {
        
    }

}
