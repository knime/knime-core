/* 
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
 */
package de.unikn.knime.core.data.renderer;

/**
 * Container for different <code>DataValueRenderer</code> which can be used
 * to render one particular kind of <code>DataValue</code>. This interface
 * itself extends <code>DataValueRenderer</code>, thus it can be used in lists,
 * tables and such. Among the renderer in this container there is always one 
 * active one which will be used to delegate request to.
 * 
 * <p>Classes implementing this interface can instantiate different kind
 * of <code>DataValueRenderer</code>. The number of different renderers is
 * defined by the number of different descriptions returned by the 
 * <code>getRendererDescriptions()</code> method. Which renderer is actually
 * used (i.e. active) is defined by the <code>setActiveRenderer(int)</code>
 * method.
 * 
 * <p>There is description assigned to each of the available renderers.
 * This string can be used in menus and so on to give a choice of the different
 * renderer available.
 * 
 * <p>Refer to the {@link de.unikn.knime.core.data.renderer package description}
 * to see how to couple a <code>DataValueRendererFamily</code> with a 
 * <code>DataValue</code> implementation.
 * 
 * @see de.unikn.knime.core.data.DataValue
 * @see de.unikn.knime.core.data.renderer.DataValueRenderer
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataValueRendererFamily extends DataValueRenderer {
    
    /**
     * Get a "unique" description for all renderer that are available in this
     * family. 
     * @return The Descriptions of all renderer that can be used here.
     */
    String[] getRendererDescriptions();
    
    /**
     * Set the currently active renderer. The argument must be an element of
     * whatever is returned in <code>getRendererDescription()</code>. If it is
     * not this method won't do anything. 
     * @param description The ID of the renderer to be used.
     */
    void setActiveRenderer(final String description);
}
