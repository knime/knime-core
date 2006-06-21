/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 */
package de.unikn.knime.core.data;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import de.unikn.knime.core.data.renderer.DataValueRendererFamily;
import de.unikn.knime.core.data.renderer.DefaultDataValueRendererFamily;
import de.unikn.knime.core.data.renderer.IntValueRenderer;

/**
 * Interface supporting generic int values.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public interface IntValue extends DataValue {
    
    /** Meta information to this value type.
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new IntUtilityFactory();

    /**
     * @return A generic <code>int</code> value.
     */
    int getIntValue();
    
    /** Implementations of the meta information of this value class. */
    public static class IntUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON;

        /** Load icon, use <code>null</code> if not available. */
        static {
            ImageIcon icon;
            try {
                ClassLoader loader = IntValue.class.getClassLoader();
                String path = 
                    IntValue.class.getPackage().getName().replace('.', '/');
                icon = new ImageIcon(
                        loader.getResource(path + "/icon/integericon.png"));
            } catch (Exception e) {
                icon = null;
            }
            ICON = icon;
        }

        private static final IntValueComparator COMPARATOR = 
            new IntValueComparator();
        
        /** Only subclasses are allowed to instantiate this class. */
        protected IntUtilityFactory() {
        }

        /**
         * @see DataValue.UtilityFactory#getIcon()
         */
        @Override
        public Icon getIcon() {
            return ICON;
        }

        /**
         * @see UtilityFactory#getComparator()
         */
        @Override
        protected DataValueComparator getComparator() {
            return COMPARATOR;
        }
        
        /**
         * @see DataValue.UtilityFactory#getRendererFamily(DataColumnSpec)
         */
        @Override
        protected DataValueRendererFamily getRendererFamily(
                final DataColumnSpec spec) {
            return new DefaultDataValueRendererFamily(
                    IntValueRenderer.INSTANCE);
        }

    }
}
