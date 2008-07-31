/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * 21.06.06 (bw & po): reviewed.
 */
package org.knime.core.data;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.node.NodeLogger;

/**
 * The interface all value interfaces of {@link org.knime.core.data.DataCell}s 
 * are derived from.
 * {@link org.knime.core.data.DataCell}s implement different 
 * <code>DataValue</code> interfaces to allow access to generic 
 * (or complex) fields from the cell.
 * Typically a <code>DataValue</code> brings along its own 
 * (set of) renderers, an icon (which is displayed in table column headers, 
 * for instance) and a comparator, which are all defined through the definition 
 * of a static member {@link DataValue#UTILITY}.
 *  
 * <p>
 * For more information regarding the definition of new <code>DataCell</code>s 
 * see the <a href="package-summary.html">package description</a> and the 
 * <a href="doc-files/newtypes.html">manual</a> on how to define new types, in
 * particular the <a href="doc-files/newtypes.html#newtypes">remarks</a> on
 * <code>DataValue</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataValue {

    /**
     * Static singleton for meta description. This field is accessed via
     * reflection in the {@link org.knime.core.data.DataType} class. It is 
     * used to determine renderer, comparator, and icon. Sub-Interfaces 
     * will &quot;override&quot; this static member, if they desire to define 
     * own renderers, comparator, and/or icon.
     */
    public static final UtilityFactory UTILITY = new UtilityFactory();

    /**
     * Implementation of the meta information to a <code>DataValue</code>.
     * <code>DataValue</code> implementations with customized meta information 
     * must provide a static final member called <code>UTILTIY</code> of this 
     * class.
     */
    public static class UtilityFactory {

        /**
         * Icon which is used as "fallback" representative when no specialized
         * icon is found in derivates of this class.
         */
        private static final Icon ICON = loadIcon(
                DataCell.class, "/icon/defaulticon.png");

        /**
         * Only subclasses are allowed to instantiate this class. This
         * constructor does nothing.
         */
        protected UtilityFactory() {
        }

        /**
         * Get an icon representing this value. This is used in table headers
         * and lists, for instance.
         * 
         * <p>
         * It is recommended to override this method and return an appropriate 
         * icon of size 16x16px.
         * 
         * @return an icon for this value
         */
        public Icon getIcon() {
            return ICON;
        }

        /**
         * Returns a family of all renderers this type natively supports. 
         * Derived classes should override this method to provide their own 
         * renderer family for the native value class.
         * 
         * <p>
         * Views that rely on renderer implementations will get a list of all
         * available renderers by invoking
         * {@link DataType#getRenderer(DataColumnSpec)} on the column's 
         * {@link org.knime.core.data.DataType}
         * which makes sure that all renderer implementations of compatible
         * values are returned.
         * 
         * @param spec the {@link org.knime.core.data.DataColumnSpec} of the 
         *            column for which the renderers are
         *            used. Most of the renderer implementations won't need
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
         * Derived classes should override this and provide a 
         * {@link org.knime.core.data.DataValueComparator} that
         * compares the respective <code>DataValue</code>. If <code>null</code> 
         * is returned the cell implementing the <code>DataValue</code> 
         * interface is said to be not comparable with respect to this 
         * <code>DataValue</code> interface. If none of the implemented
         * <code>DataValue</code> interfaces is comparable, the fallback 
         * comparator based on the cell's <code>toString()</code> method is 
         * used.
         * 
         * @return this default implementation returns <code>null</code>
         */
        protected DataValueComparator getComparator() {
            return null;
        }
        
        /** Convenience method to allow subclasses to load their icon. The icon
         * is supposed to be located relative to the package associated with the
         * argument class under the path <code>path</code>. This method will 
         * not throw an exception when the loading fails but instead return a 
         * <code>null</code> icon.
         * @param className The class object, from which to retrieve the 
         * {@link Class#getPackage() package}, e.g. <code>FooValue.class</code>.
         * @param path The icon path relative to package associated with the 
         * class argument. 
         * @return the icon loaded from that path or null if it loading fails
         */
        protected static Icon loadIcon(
                final Class<?> className, final String path) {
            ImageIcon icon;
            try {
                ClassLoader loader = className.getClassLoader(); 
                String packagePath = 
                    className.getPackage().getName().replace('.', '/');
                String correctedPath = path;
                if (!path.startsWith("/")) {
                    correctedPath = "/" + path;
                }
                icon = new ImageIcon(
                        loader.getResource(packagePath + correctedPath));
            } catch (Exception e) {
                NodeLogger.getLogger(DataValue.class).debug(
                        "Unable to load icon at path " + path, e);
                icon = null;
            }
            return icon;
        }
    }
}
