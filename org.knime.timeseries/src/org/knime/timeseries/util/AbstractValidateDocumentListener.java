/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   30.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.util;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Convenience implementation of a {@link DocumentListener} which calls for 
 * every change, insert, or remove the abstract {@link #validate()} method.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractValidateDocumentListener 
    implements DocumentListener {

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void changedUpdate(final DocumentEvent e) {
        validate();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void insertUpdate(final DocumentEvent e) {
        validate();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void removeUpdate(final DocumentEvent e) {
        validate();
    }
    
    /**
     * Validate the entered value.
     */
    protected abstract void validate();

}
