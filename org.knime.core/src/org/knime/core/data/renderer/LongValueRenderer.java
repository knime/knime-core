/* ------------------------------------------------------------------
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
 *   21.01.2009 (meinl): created
 */
package org.knime.core.data.renderer;

import org.knime.core.data.LongValue;

/**
 * Renderer for long values that simply prints the value.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public final class LongValueRenderer extends DefaultDataValueRenderer {
    /** Instance to be used. */
    public static final LongValueRenderer INSTANCE =
        new LongValueRenderer();

    /**
     * Default Initialization is empty.
     */
    private LongValueRenderer() {
    }

    /**
     * Tries to cast to LongValue and will set the long in the super class.
     * If that fails, the object's toString() method is used.
     * @param value the object to be rendered, should be a LongValue
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof LongValue) {
            super.setValue(Long.toString(((LongValue)value).getLongValue()));
        } else {
            super.setValue(value);
        }
    }
}
