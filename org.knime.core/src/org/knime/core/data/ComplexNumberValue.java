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
 * History
 *   23.03.2006 (cebron): created
 */
package org.knime.core.data;

import javax.swing.Icon;

/**
 * Interface supporting generic complex number values.
 * 
 * @author ciobaca, University of Konstanz
 */
public interface ComplexNumberValue extends DataValue {
    
    /** Meta information.
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = 
        new ComplexNumberUtilityFactory();

    /**
     * @return The real part of the complex number
     */
    double getRealValue();

    /**
     * @return The imaginary part of the complex number
     */
    double getImaginaryValue();
    
    /** Meta information to the complex number value. */
    public static class ComplexNumberUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON = 
            loadIcon(ComplexNumberValue.class, "/icon/complexnumbericon.png");

        private static final ComplexNumberValueComparator COMPARATOR =
            new ComplexNumberValueComparator();
        
        /** Only subclasses are allowed to instantiate this class. */
        protected ComplexNumberUtilityFactory() {
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
