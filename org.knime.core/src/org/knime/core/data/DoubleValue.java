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
 * History
 *   07.07.2005 (mb): created
 */
package org.knime.core.data;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.knime.core.data.renderer.DoubleBarRenderer;
import org.knime.core.data.renderer.DoubleGrayValueRenderer;
import org.knime.core.data.renderer.DoubleValueRenderer;


/**
 * Interface supporting generic double values.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public interface DoubleValue extends DataValue {
    
    /** Meta information to double values.
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new DoubleUtilityFactory();
    
    /**
     * @return A generic <code>double</code> value.
     */
    double getDoubleValue();
    
    /** Implementations of the meta information of this value class. */
    public static class DoubleUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON;

        /** Load double icon, use <code>null</code> if not available. */
        static {
            ImageIcon icon;
            try {
                ClassLoader loader = DoubleValue.class.getClassLoader();
                String path = 
                    DoubleValue.class.getPackage().getName().replace('.', '/');
                icon = new ImageIcon(
                        loader.getResource(path + "/icon/doubleicon.png"));
            } catch (Exception e) {
                icon = null;
            }
            ICON = icon;
        }

        private static final DoubleValueComparator COMPARATOR =
            new DoubleValueComparator();
        
        /** Only subclasses are allowed to instantiate this class. */
        protected DoubleUtilityFactory() {
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
         * @see UtilityFactory#getRendererFamily(DataColumnSpec)
         */
        @Override
        protected DataValueRendererFamily getRendererFamily(
                final DataColumnSpec spec) {
            return new DefaultDataValueRendererFamily(
                    DoubleValueRenderer.STANDARD_RENDERER, 
                    DoubleValueRenderer.PERCENT_RENDERER,
                    new DoubleGrayValueRenderer(spec),
                    new DoubleBarRenderer(spec)); 
        }
    }
}
