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
 * 21.06.06 (bw & po): reviewed.
 */
package de.unikn.knime.core.data;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import de.unikn.knime.core.data.renderer.DataValueRendererFamily;

/**
 * The interface all value interfaces of data cells are derived from. 
 * <code>DataCell</code>s implement different <code>DataValue</code> interfaces
 * to allow access to generic (or complex) fields from the cell. Typically a 
 * <code>DataValue</code> brings along its own (set of) renderer, an icon 
 * (which is displayed in table column headers, for instance) and a comparator, 
 * which are all defined through the definition of a static member UTITLITY. 
 * For more information regarding definition of new data cells see the
 * <a href="package-summary.html">package description</a> and the
 * <a href="doc-files/newtypes.html">manual on how to define new types</a>, 
 * in particular the <a href="doc-files/newtypes.html#newtypes">
 * remarks on DataValue</a>
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataValue {

    /** Static singleton for meta description. This field is accessed via
     * reflection in the DataType class. It is being used to determine renderer,
     * comparator, etc. Sub-Interfaces will &quot;override&quot; this 
     * static member if they desire to define own renderes.
     */
    public static final UtilityFactory UTILITY = new UtilityFactory();

    /** Implementation of the meta information to a <code>DataValue</code>.
     * DataValue implemenations with customized meta information must provide
     * a static final member called UTILTIY of this class.
     */
    public static class UtilityFactory {

        /*
         * Icon which is used as "fallback" representative when no specialized
         * icon is found in derivates of this class
         */
        private static final Icon ICON;

        /*
         * try loading this icon, if it fails we use null in the probably silly
         * assumtion everyone can deal with that
         */
        static {
            ImageIcon icon;
            try {
                ClassLoader loader = DataValue.class.getClassLoader();
                String path = DataValue.class.getPackage().getName().replace(
                        '.', '/');
                icon = new ImageIcon(loader.getResource(path
                        + "/icon/defaulticon.png"));
            } catch (Exception e) {
                icon = null;
            }
            ICON = icon;
        }
        
        /** Only subclasses are allowed to instantiate this class. This
         * constructor does nothing.
         */
        protected UtilityFactory() {
        }

        /**
         * Get an icon representing this value. This is used in table headers 
         * and lists, for instance.
         * 
         * <p>
         * Implementors who derive this class are invited to override this
         * method and return a more specific icon (of size 16x16).
         * 
         * @return An icon for this value.
         */
        public Icon getIcon() {
            return ICON;
        }

        /**
         * Get all renderers this type natively supports. Derived classes should
         * override this method to provide their own renderer family for the
         * native value class.
         * 
         * <p>
         * Views that rely on renderer implementations will get a list of all
         * available renderer by invoking
         * <code>getRenderer(DataColumnSpec)</code> on the column's DataType
         * which makes sure that all renderer implementations of compatible 
         * values are returned.
         * 
         * @param spec The column spec to the column for which the renderer will
         *            be used. Most of the renderer implementations won't need
         *            column domain information but some do. For instance a
         *            class that renders the double value in the column
         *            according to the min/max values in the column domain.
         * @return <code>null</code>
         */
        protected DataValueRendererFamily getRendererFamily(
                final DataColumnSpec spec) {
            // avoid compiler warnings as spec is never read locally
            assert spec == spec;
            return null;
        }

        /**
         * Derived classes should override this and provide a comparator that
         * compare the respective DataValue. If null is returned the
         * cell implementing the DataValue interface is said to be not 
         * comparable with respect to this DataValue interface. If none
         * of the implemented DataValue interfaces is comparable, a fallback
         * comparator based on the cell's toString() method is used.
         *
         * <p>This default implementation returns <code>null</code>.
         * @return A comparator to compare values or <code>null</code>.
         */
        protected DataValueComparator getComparator() {
            return null;
        }
    }
}
