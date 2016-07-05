/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */

package org.knime.core.data.convert.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ClassLoader which searches through multiple ClassLoaders. Important for dynamic uses of the converter framework.
 * Allows creating a class loader which knows classes of org.knime.core.data.convert and the user bundle.
 *
 * <p>
 * <b>Example:</b>
 *
 * <pre>
 * final ClassLoader loader =
 *     new ClassLoader(getClass().getClassLoader(), DataCellToJavaConverterRegistry.class.getClassLoader());
 * </pre>
 * <p>
 *
 * A call to {@link ClassLoader#loadClass(String)} will now first check the <code>getClass().getClassLoader()</code> and
 * if that does not contain the class we are trying to load, it will check
 * <code>DataCellToJavaConverterRegistry.class.getClassLoader()</code>.
 *
 * @author Jonathan Hale
 * @since 3.2
 */
public class MultiParentClassLoader extends ClassLoader {
    /*
     * List of class loaders which will be searched
     */
    private final List<ClassLoader> m_classLoaders;

    /**
     * @param classLoaders ClassLoaders to search in the given order when trying to find a class
     */
    public MultiParentClassLoader(final ClassLoader... classLoaders) {
        m_classLoaders = new ArrayList<>(Arrays.asList(classLoaders));
    }

    @Override
    public URL findResource(final String name) {
        for (final ClassLoader loader : m_classLoaders) {
            final URL resource = loader.getResource(name);

            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        for (final ClassLoader loader : m_classLoaders) {
            try {
                return loader.loadClass(name);
            } catch (final ClassNotFoundException e) {
                // thrown when loader cannot find the class.
            }
        }
        return super.findClass(name); // just throws ClassNotFoundException
    }

}
