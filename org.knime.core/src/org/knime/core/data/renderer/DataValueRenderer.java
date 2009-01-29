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
