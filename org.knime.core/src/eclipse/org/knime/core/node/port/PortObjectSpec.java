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
 * ---------------------------------------------------------------------
 * 
 * History
 *   10.09.2007 (mb): created
 */
package org.knime.core.node.port;

import java.io.IOException;

import javax.swing.JComponent;

import org.knime.core.internal.SerializerMethodLoader.Serializer;
import org.knime.core.node.NodeModel;

/**
 * General interface for object specifications that are passed along node
 * connections. Most prominent example of such a class is
 * {@link org.knime.core.data.DataTableSpec}, which is used to represent table
 * specification. <code>PortObjectSpec</code> objects represent the
 * information that is necessary during a node's
 * {@link NodeModel#configure(PortObjectSpec[]) configuration step}.
 * They are assumed to be fairly small objects (usually reside in memory) and
 * describe the general structure of {@link PortObject} objects (which are
 * passed along the connections during a node's execution). Both the class of a
 * <code>PortObjectSpec</code> and a {@link PortObject} describe
 * {@link PortType}.
 * 
 * <p>
 * <b>Important:</b> Implementors of this interface must also provide a
 * {@link PortObjectSpecSerializer}, which is used to save and load instances.
 * The framework will try to invoke a static method defined in the
 * implementation with the following signature:
 * 
 * <pre>
 *  public static PortObjectSpecSerializer&lt;FooPortObjectSpec&gt; 
 *          getPortObjectSpecSerializer(final File directory) 
 *              throws IOException {...}
 * </pre>
 * 
 * If the class does not have such a static method (or it has the wrong
 * signature), an exception will be thrown at runtime.
 * 
 * <p>
 * <b>Note:</b> The API of this class is not finalized and may slightly change 
 * in future versions. 

 * @see org.knime.core.data.DataTableSpec
 * @see PortObject
 * @see PortType
 * @since 2.0
 * @author M. Berthold & B. Wiswedel, University of Konstanz
 */
public interface PortObjectSpec {

    /**
     * Factory class that's used for writing and loading objects of class
     * denoted by <code>T</code>. See description of class
     * {@link PortObjectSpec} for details.
     * 
     * @param <T> class of the object to save or load.
     */
    abstract static class PortObjectSpecSerializer
        <T extends PortObjectSpec> implements Serializer<T> {
        
        /** Saves the port specification to an output stream.
         * @param portObjectSpec The spec to save.
         * @param out Where to save to
         * @throws IOException If that fails for IO problems.
         */
        public abstract void savePortObjectSpec(final T portObjectSpec,
                final PortObjectSpecZipOutputStream out)
        throws IOException;
        
        /** Load a specification from an input stream.
         * @param in Where to load from
         * @return The restored object.
         * @throws IOException If that fails for IO problems.
         */
        public abstract T loadPortObjectSpec(
                final PortObjectSpecZipInputStream in)
            throws IOException;
    }
    
    /**
     * The returned views are displayed in the out port view of the referring 
     * node. Each component is displayed in an extra tab. The name of the 
     * component is used as the title for the tab. It is important that no 
     * external members are kept in the component so it can be deleted, when 
     * the port object is deleted. If the port object has no view return an 
     * empty array.
     * 
     * @return an array of views for the port object spec, each displayed as a 
     * tab in the out port view 
     */
    public JComponent[] getViews();
    
}
