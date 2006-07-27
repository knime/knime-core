/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 */
package de.unikn.knime.core.data;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Interface supporting fuzzy interval cells holding support and core min and
 * max values.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public interface FuzzyIntervalValue extends DataValue {
    
    /** Meta information to this value type.
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = 
        new FuzzyIntervalUtilityFactory();

    /**
     * @return Minimum support value.
     */
    public double getMinSupport();
    
    /**
     * @return Minimum core value.
     */
    public double getMinCore();
    
    /**
     * @return Maximum core value.
     */
    public double getMaxCore();
    
    /**
     * @return Maximum support value.
     */
    public double getMaxSupport();
    
    /** 
     * @return The center of gravity of this fuzzy membership function.
     */
    public double getCenterOfGravity();
    
    /** Implementations of the meta information of this value class. */
    public static class FuzzyIntervalUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON;

        /** Load icon, use <code>null</code> if not available. */
        static {
            ImageIcon icon;
            try {
                ClassLoader loader = FuzzyIntervalValue.class.getClassLoader();
                String path = FuzzyIntervalValue.class.getPackage().
                    getName().replace('.', '/');
                icon = new ImageIcon(loader.getResource(
                        path + "/icon/fuzzyintervalicon.png"));
            } catch (Exception e) {
                icon = null;
            }
            ICON = icon;
        }

        private static final FuzzyIntervalValueComparator COMPARATOR =
            new FuzzyIntervalValueComparator();
        
        /** Only subclasses are allowed to instantiate this class. */
        protected FuzzyIntervalUtilityFactory() {
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

    }

}
