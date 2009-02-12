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
        private static final Icon ICON = 
            loadIcon(FuzzyNumberValue.class, "/icon/fuzzyicon.png");

        private static final FuzzyNumberValueComparator COMPARATOR =
            new FuzzyNumberValueComparator();
        
        /** Only subclasses are allowed to instantiate this class. */
        protected FuzzyNumberUtilityFactory() {
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
            return COMPARATOR;
        }

    }

}
