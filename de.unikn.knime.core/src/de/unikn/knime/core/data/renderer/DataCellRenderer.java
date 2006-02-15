/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
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
 * 
 */
package de.unikn.knime.core.data.renderer;

import java.awt.Dimension;

import javax.swing.ListCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * General interface for classes that are able to render special derivates of
 * <code>DataCell</code>. 
 * 
 * <p>This interface extends <code>TableCellRenderer</code> and
 * <code>ListCellRenderer</code>. Thus, it can be easily used in lists and 
 * tables. The preferred way of instantiating an object of a renderer is to use
 * a <code>DataCellRendererFamily</code>.
 * 
 * @see de.unikn.knime.core.data.renderer.DataCellRendererFamily
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataCellRenderer extends TableCellRenderer, ListCellRenderer {
    
    /** Get a description for this renderer implementation. It will serve 
     * to identify this renderer when the user has the choice of different
     * renderer that are available. Make sure that this description is likely
     * to be unique (if not only one of renderer with this ID 
     * is shown as available) and this description is short but "expressive" so
     * that it can be shown as label in menus, for instance.
     * @return A description for this renderer.
     */
    public String getDescription();
    
    /**
     * Get the dimension which the renderer component will preferrably occupy.
     * @return Size of the component being rendererd.
     */
    Dimension getPreferredSize();

}
