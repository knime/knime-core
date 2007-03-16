/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
package org.knime.core.data;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Interface supporting interval cells holding minimum and maximum boundaries.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface IntervalValue extends DataValue {
    
    /** 
     * Meta information to this value type.
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = 
        new IntervalUtilityFactory();

    /**
     * @return minimum border
     */
    public double getLeftBound();
    
    /**
     * @return maximum border
     */
    public double getRightBound();
    
    /** Implementations of the meta information of this value class. */
    public static class IntervalUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON;

        /** Load icon, use <code>null</code> if not available. */
        static {
            ImageIcon icon;
            try {
                ClassLoader loader = IntervalValue.class.getClassLoader();
                String path = IntervalValue.class.getPackage().
                    getName().replace('.', '/');
                icon = new ImageIcon(loader.getResource(
                        path + "/icon/intervalicon.png"));
            } catch (Exception e) {
                icon = null;
            }
            ICON = icon;
        }

        private static final IntervalValueComparator COMPARATOR =
            new IntervalValueComparator();
        
        /** Only subclasses are allowed to instantiate this class. */
        protected IntervalUtilityFactory() {
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
