/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
 *   02.09.2008 (ohl): created
 */
package org.knime.core.data.renderer;

import org.knime.core.data.vector.bytevector.ByteVectorValue;

/**
 * Renderer for byte vector values showing the string representation.
 *
 * @author ohl, University of Konstanz
 */
public class ByteVectorValueStringRenderer extends DefaultDataValueRenderer {

    /**
     * Singleton instance to be used.
     */
    public static final ByteVectorValueStringRenderer INSTANCE =
            new ByteVectorValueStringRenderer();

    /**
     * Default Initialization is empty.
     */
    private ByteVectorValueStringRenderer() {
        // this is an empty constructor.
    }

    /**
     * Tries to cast o IntValue and will set the integer in the super class. If
     * that fails, the object's toString() method is used.
     *
     * @param value The object to be rendered, should be an
     *            {@link ByteVectorValue}.
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof ByteVectorValue) {
            super.setValue(value.toString());
        } else {
            super.setValue(value);
        }
    }

}
