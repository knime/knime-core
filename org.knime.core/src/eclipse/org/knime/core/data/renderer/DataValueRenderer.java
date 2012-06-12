/*  
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * 
 */
package org.knime.core.data.renderer;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.ListCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;

/**
 * General interface for classes that are able to render special derivatives of
 * <code>DataValue</code>. 
 * 
 * <p>This interface extends <code>TableCellRenderer</code> and
 * <code>ListCellRenderer</code>. Thus, it can be easily used in lists and 
 * tables. The preferred way of instantiating an object of a renderer is to use
 * a <code>DataValueRendererFamily</code>.
 * 
 * @see org.knime.core.data.DataValue
 * @see org.knime.core.data.renderer.DataValueRendererFamily
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataValueRenderer extends TableCellRenderer, ListCellRenderer {
    
    /** The property identifier that is read from, for instance the table view
     * to determine which particular renderer (from the set of available 
     * renderers to a column) is to be used. This property should be attached
     * using a {@link DataColumnSpec#getDomain() DataColumnSpec's 
     * domain information} and should map to the description string of the 
     * renderer. A sample code that sets this property on a newly created 
     * double column to use the bar renderer is as follows:
     * <pre>
     * DataColumnSpecCreator creator = new DataColumnSpecCreator(
     *          newName, DoubleCell.TYPE);
     * creator.setProperties(new DataColumnProperties(
     *          Collections.singletonMap(
     *              DataValueRenderer.PROPERTY_PREFERRED_RENDERER, 
     *              DoubleBarRenderer.DESCRIPTION)));
     * creator.setDomain(new DataColumnDomainCreator(
     *          new DoubleCell(0.0), new DoubleCell(1.0)).createDomain());
     * DataColumnSpec spec = creator.createSpec();
     * </pre> 
     * <p>Keep in mind that setting the preferred renderer to an instance 
     * that inherently depends on proper domain information to be available 
     * (for instance [0,1]) requires the spec's {@link DataColumnDomain} 
     * to be set appropriately.
     */ 
    public static final String PROPERTY_PREFERRED_RENDERER = 
        "preferred.renderer";
    
    /** Get a description for this renderer implementation. It will serve 
     * to identify this renderer when the user has the choice of different
     * renderer that are available. Make sure that this description is likely
     * to be unique (if not only one of renderer with this ID 
     * is shown as available) and this description is short but "expressive" so
     * that it can be shown as label in menus, for instance.
     * @return A description for this renderer.
     */
    String getDescription();
    
    /**
     * Get the dimension which the renderer component will preferably occupy.
     * @return Size of the component being rendered.
     */
    Dimension getPreferredSize();
    
    /**
     * Get a component that visualizes a given object. This object, generally,
     * is a {@link org.knime.core.data.DataCell} implementing the underlying 
     * <code>DataValue</code> interface. The implementation, however, needs to 
     * handle other cases as well, such as <code>null</code> arguments,
     * missing <code>DataCell</code> or generic objects.
     * @param val The value to render
     * @return A component displaying the content of <code>val</code>.
     */
    Component getRendererComponent(final Object val);
    
    /** Is this renderer instance able to render the content of the column
     * given by <code>spec</code>. Most implementations will return 
     * <code>true</code> here but some may require some properties to be
     * set, for instance a molecule renderer needs to have 2D coordinates
     * in the column.
     * 
     * <p>Note: This method is not used to test whether arbitrary types
     * can be renderer by this renderer instance but rather if the specific
     * runtime column spec (containing the type) is appropriate, for instance
     * contains domain information or certain properties are set.
     * @param spec The column spec to check.
     * @return <code>true</code> if this renderer can be chosen to render
     * the content of the column.
     */
    boolean accepts(final DataColumnSpec spec);

}
