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

/**
 * Interface supporting fuzzy numbers defined by min and max support, and core. 
 * 
 * @author Michael Berthold, University of Konstanz
 */
public interface FuzzyNumberValue extends DataValue {

    /** Meta information to this value type.
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = 
        new FuzzyNumberUtilityFactory();
    
    /**
     * @return Minimum support value.
     */
    public double getMinSupport();
    
    /**
     * @return Core value.
     */
    public double getCore();
    
    /**
     * @return Maximum support value.
     */
    public double getMaxSupport();
    
    /** Implementations of the meta information of this value class. */
    public static class FuzzyNumberUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON;

        /** Load icon, use <code>null</code> if not available. */
        static {
            ImageIcon icon;
            try {
                ClassLoader loader = FuzzyNumberValue.class.getClassLoader();
                String path = FuzzyNumberValue.class.getPackage().
                    getName().replace('.', '/');
                icon = new ImageIcon(
                        loader.getResource(path + "/icon/fuzzyicon.png"));
            } catch (Exception e) {
                icon = null;
            }
            ICON = icon;
        }

        private static final FuzzyNumberCellComparator COMPARATOR =
            new FuzzyNumberCellComparator();
        
        /** Only subclasses are allowed to instantiate this class. */
        protected FuzzyNumberUtilityFactory() {
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
        protected DataCellComparator getComparator() {
            return COMPARATOR;
        }

    }

}
