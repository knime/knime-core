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
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 */
package org.knime.core.data;

import javax.swing.Icon;

import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.knime.core.data.renderer.StringValueRenderer;


/**
 * Interface of a {@link org.knime.core.data.def.StringCell}, forces method to
 * return string value.
 * 
 * @author M. Berthold, University of Konstanz
 */
public interface StringValue extends DataValue {
    
    /** Meta information to this value type.
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new StringUtilityFactory();

    /**
     * @return A String value.
     */
    String getStringValue();
    
    /** Implementations of the meta information of this value class. */
    public static class StringUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON = 
            loadIcon(StringValue.class, "/icon/stringicon.png");

        private static final StringValueComparator STRING_COMPARATOR = 
            new StringValueComparator();
        
        /** Only subclasses are allowed to instantiate this class. */
        protected StringUtilityFactory() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Icon getIcon() {
            return ICON;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueComparator getComparator() {
            return STRING_COMPARATOR;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueRendererFamily getRendererFamily(
                final DataColumnSpec spec) {
            return new DefaultDataValueRendererFamily(
                    StringValueRenderer.INSTANCE);
        }

    }

}
