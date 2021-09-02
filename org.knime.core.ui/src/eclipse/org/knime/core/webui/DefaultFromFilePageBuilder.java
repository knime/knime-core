/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Sep 1, 2021 (hornm): created
 */
package org.knime.core.webui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.core.runtime.FileLocator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.webui.FromFilePageBuilder;
import org.knime.core.node.webui.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * The default implementation of the {@link FromFilePageBuilder}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class DefaultFromFilePageBuilder extends DefaultPageBuilder implements FromFilePageBuilder {

    private final Class<?> m_clazz;

    private final String m_basePath;

    DefaultFromFilePageBuilder(final Class<?> clazz, final String basePath) {
        m_clazz = clazz;
        m_basePath = basePath;
    }

    @Override
    public FromFilePageBuilder contentFromFile(final String relativeFilePath) {
        m_pageResource = createFileResource(m_clazz, m_basePath, relativeFilePath);
        return this;
    }

    @Override
    public DefaultFromFilePageBuilder addResourceFile(final String relativeFilePath) {
        m_resources.add(createFileResource(m_clazz, m_basePath, relativeFilePath));
        return this;
    }

    @Override
    public DefaultFromFilePageBuilder addResourceDirectory(final String relativeDirPath) {
        Path root = getAbsoluteBasePath(m_clazz, m_basePath);
        createResourcesFromDir(root, root.resolve(relativeDirPath), m_resources);
        return this;
    }

    private static FileResource createFileResource(final Class<?> clazz, final String basePath,
        final String relativeFilePath) {
        Path relFile = Paths.get(relativeFilePath);
        Path file = getAbsoluteBasePath(clazz, basePath).resolve(relFile);
        return createResourceFromFile(relFile, file);
    }

    /*
     * The bundle path + base path.
     */
    private static Path getAbsoluteBasePath(final Class<?> clazz, final String baseDir) {
        Bundle bundle = FrameworkUtil.getBundle(clazz);
        URL bundleUrl = bundle.getEntry(".");
        try {
            return Paths.get(FileLocator.toFileURL(bundleUrl).toURI()).resolve(baseDir).normalize();
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Failed to resolve the directory " + baseDir, ex);
        }
    }

    private static FileResource createResourceFromFile(final Path relativeFilePath, final Path file) {
        assert !relativeFilePath.isAbsolute();
        CheckUtils.checkArgument(Files.isRegularFile(file), "The file '%s' doesn't exist (or is not a regular file)",
            file);
        return new FileResource(file, relativeFilePath);
    }

    private static void createResourcesFromDir(final Path root, final Path dir, final List<Resource> res) {
        assert Files.isDirectory(dir);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            dirStream.forEach(f -> {
                if (Files.isDirectory(f)) {
                    createResourcesFromDir(root, f, res);
                } else {
                    res.add(createResourceFromFile(root.relativize(f).normalize(), f));
                }
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to resolve resources from directory " + dir, ex);
        }
    }

    /*
     * -------------------------------------------------------------------------------------------------
     * Methods overwritten from the parent class in order to narrow down the returned page builder type.
     * -------------------------------------------------------------------------------------------------
     */

    @Override
    public FromFilePageBuilder initData(final Supplier<String> data) {
        super.initData(data);
        return this;
    }

    @Override
    public FromFilePageBuilder content(final Supplier<InputStream> content, final String relativePath) {
        super.content(content, relativePath);
        return this;
    }

    @Override
    public FromFilePageBuilder contentFromString(final Supplier<String> content, final String relativePath) {
        super.contentFromString(content, relativePath);
        return null;
    }

    @Override
    public FromFilePageBuilder addResource(final Supplier<InputStream> content, final String relativePath) {
        addResource(content, relativePath);
        return this;
    }

    @Override
    public FromFilePageBuilder addResourceFromString(final Supplier<String> content, final String relativePath) {
        super.addResourceFromString(content, relativePath);
        return this;
    }
}
