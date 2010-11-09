/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 1, 2010 (wiswedel): created
 */
package org.knime.ext.sun.nodes.script.compile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class InMemoryJavaFileManager
    extends ForwardingJavaFileManager<StandardJavaFileManager>
    implements StandardJavaFileManager {

    private final Map<String, InMemoryClassJavaFileObject> m_classMap;

    /**
     * @param delegate
     */
    protected InMemoryJavaFileManager(final StandardJavaFileManager delegate) {
        super(delegate);
        m_classMap = new HashMap<String, InMemoryClassJavaFileObject>();
    }

    /** {@inheritDoc} */
    @Override
    public JavaFileObject getJavaFileForOutput(final Location location,
            final String className, final Kind kind,
            final FileObject sibling) throws IOException {
        if (StandardLocation.CLASS_OUTPUT.equals(location)
                && JavaFileObject.Kind.CLASS.equals(kind)) {
            InMemoryClassJavaFileObject clazz =
                new InMemoryClassJavaFileObject(className);
            m_classMap.put(className, clazz);
            return clazz;
        } else {
            return super.getJavaFileForOutput(
                    location, className, kind, sibling);
        }
    }

    byte[] getClassByteCode(final String name) throws ClassNotFoundException {
        InMemoryClassJavaFileObject classObject = m_classMap.get(name);
        if (classObject == null) {
            throw new ClassNotFoundException("No class \""
                    + name + "\" in " + getClass().getSimpleName());
        }
        return classObject.getClassStream();
    }

    /** {@inheritDoc} */
    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(
            final Iterable<? extends File> files) {
        return fileManager.getJavaFileObjectsFromFiles(files);
    }

    /** {@inheritDoc} */
    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(
            final File... files) {
        return fileManager.getJavaFileObjects(files);
    }

    /** {@inheritDoc} */
    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(
            final Iterable<String> names) {
        return fileManager.getJavaFileObjectsFromStrings(names);
    }

    /** {@inheritDoc} */
    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(
            final String... names) {
        return fileManager.getJavaFileObjects(names);
    }

    /** {@inheritDoc} */
    @Override
    public void setLocation(final Location location,
            final Iterable<? extends File> path) throws IOException {
        fileManager.setLocation(location, path);
    }

    /** {@inheritDoc} */
    @Override
    public Iterable<? extends File> getLocation(final Location location) {
        return fileManager.getLocation(location);
    }

}
