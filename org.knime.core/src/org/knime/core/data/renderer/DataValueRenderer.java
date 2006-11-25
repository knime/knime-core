/*  
 * -------------------------------------------------------------------
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
     * is a {@link DataCell} implementing the underlying 
     * <code>DataValue</code> interface. The implementation, however, needs to 
     * handle other cases as well, such as <code>null</code> arguments,
     * missing <code>DataCell</code> or generic objects.
     * @param val The value to render
     * @return A component displaying the content of <code>val</code>.
     */
    Component getRendererComponent(final Object val);

}
