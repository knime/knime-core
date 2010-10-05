/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * History
 *   Dec 7, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;

import org.knime.core.eclipseUtil.GlobalObjectInputStream;

/**
 * Object input stream, which can be used to load classes using a given 
 * preferred ClassLoader. This class loader is typically the bundle
 * class loader of the DataCell to be read next from the stream. 
 * @author Bernd Wiswedel, University of Konstanz
 */
class PriorityGlobalObjectInputStream extends GlobalObjectInputStream {
    private ClassLoader m_classLoader;
    
    /** Delegates to super.
     * @param in Delegated to super.
     * @throws IOException If super throws an exception.
     */
    PriorityGlobalObjectInputStream(final InputStream in) 
        throws IOException {
        super(in);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) 
        throws IOException, ClassNotFoundException {
        if (m_classLoader != null) {
            try {
                return Class.forName(desc.getName(), true, m_classLoader);
            } catch (ClassNotFoundException cnfe) {
                // ignore and let super do it.
            }
        }
        return super.resolveClass(desc);
    }
    
    /** Set the class loader to use next or <code>null</code> if to use 
     * the default.
     * @param l to use.
     */
    void setCurrentClassLoader(final ClassLoader l) {
        m_classLoader = l;
    }
}
