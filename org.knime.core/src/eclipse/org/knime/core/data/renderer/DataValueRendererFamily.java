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
 */
package org.knime.core.data.renderer;

import org.knime.core.data.DataColumnSpec;

/**
 * Container for different
 * {@link org.knime.core.data.renderer.DataValueRenderer}s which can be
 * used to render one particular kind of
 * {@link org.knime.core.data.DataValue}. This interface itself extends
 * {@link org.knime.core.data.renderer.DataValueRenderer}, thus it can be
 * used in lists, tables and such. Among the renderer in this container there is
 * always one active one which will be used to delegate request to.
 * 
 * <p>
 * Classes implementing this interface can instantiate different kind of
 * {@link org.knime.core.data.renderer.DataValueRenderer}. The number of
 * different renderers is defined by the number of different descriptions
 * returned by the {@link #getRendererDescriptions()} method. Which renderer is
 * actually used (i.e. active) is defined by the
 * {@link #setActiveRenderer(String)} method.
 * 
 * <p>
 * There is description assigned to each of the available renderers. This string
 * can be used in menus and so on to give a choice of the different renderer
 * available.
 * 
 * <p>
 * Refer to the <a href="package-summary.html">package description</a> to
 * see how to couple a <code>DataValueRendererFamily</code> with a
 * {@link org.knime.core.data.DataValue} implementation.
 * 
 * @see org.knime.core.data.DataValue
 * @see org.knime.core.data.renderer.DataValueRenderer
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface DataValueRendererFamily extends DataValueRenderer {

    /**
     * Get a "unique" description for all renderer that are available in this
     * family.
     * 
     * @return The Descriptions of all renderer that can be used here.
     */
    String[] getRendererDescriptions();

    /**
     * Set the currently active renderer. The argument must be an element of
     * whatever is returned in <code>getRendererDescription()</code>. If it
     * is not this method won't do anything.
     * 
     * @param description The ID of the renderer to be used.
     */
    void setActiveRenderer(final String description);
    
    /** 
     * Is the renderer with the given description able to render the
     * content of <code>spec</code>.
     * @param desc The description of the renderer.
     * @param spec The column spec to check.
     * @return If the renderer can render the column with the given spec.
     * @see DataValueRenderer#accepts(DataColumnSpec)
     * @throws IllegalArgumentException If the description is unknown.
     */
    boolean accepts(final String desc, final DataColumnSpec spec);
}
