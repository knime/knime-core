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
 */
package org.knime.core.data.renderer;

import org.knime.core.data.IntValue;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public final class IntValueRenderer extends DefaultDataValueRenderer {

    /** Instance to be used. */
    public static final IntValueRenderer INSTANCE = 
        new IntValueRenderer();
    
    /**
     * Default Initialization is empty.
     */
    private IntValueRenderer() {
    }
    
    /**
     * Tries to cast o IntValue and will set the integer in the super class.
     * If that fails, the object's toString() method is used.
     * @param value The object to be renderered, should be an IntValue.
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof IntValue) {
            super.setValue(Integer.toString(((IntValue)value).getIntValue()));
        } else {
            super.setValue(value);
        }
    }

}
