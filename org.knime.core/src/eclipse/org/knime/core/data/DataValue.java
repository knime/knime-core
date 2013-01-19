/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 21.06.06 (bw & po): reviewed.
 */
package org.knime.core.data;

import javax.swing.Icon;

import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.node.util.ViewUtils;

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

        /** Convenience method to allow subclasses to load their icon. See
         * {@link ViewUtils#loadIcon(Class, String)} for details.
         * @param className The class object, from which to retrieve the
         * {@link Class#getPackage() package}, e.g. <code>FooValue.class</code>.
         * @param path The icon path relative to package associated with the
         * class argument.
         * @return the icon loaded from that path or null if it loading fails
         */
        protected static Icon loadIcon(
                final Class<?> className, final String path) {
            return ViewUtils.loadIcon(className, path);
        }
    }
}
