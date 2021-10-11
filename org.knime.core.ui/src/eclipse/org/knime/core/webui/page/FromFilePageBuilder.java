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
 *   Sep 2, 2021 (hornm): created
 */
package org.knime.core.webui.page;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.util.CheckUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Adds additional methods to the {@link PageBuilder} which allows one to define the page content and associated
 * resources by referencing files.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public final class FromFilePageBuilder extends PageBuilder {

    private final Class<?> m_clazz;

    private final String m_bundleID;

    private final String m_basePath;

    private final boolean m_isComponent;

    FromFilePageBuilder(final Class<?> clazz, final String basePath, final String relativeFilePath) {
        super(createFileResource(clazz, null, basePath, relativeFilePath));
        m_clazz = clazz;
        m_bundleID = null;
        m_basePath = basePath;
        m_isComponent = relativeFilePath.endsWith(".js");
    }

    FromFilePageBuilder(final String bundleID, final String basePath, final String relativeFilePath) {
        super(createFileResource(null, bundleID, basePath, relativeFilePath));
        m_clazz = null;
        m_bundleID = bundleID;
        m_basePath = basePath;
        m_isComponent = relativeFilePath.endsWith(".js");
    }

    /**
     * Adds another resource file to the 'context' of a page (such as a js-file).
     *
     * @param relativeFilePath the relative path to the file
     * @return this page builder instance
     */
    public FromFilePageBuilder addResourceFile(final String relativeFilePath) {
        m_resources.add(createFileResource(m_clazz, m_bundleID, m_basePath, relativeFilePath));
        return this;
    }

    /**
     * Adds all files in the given directory to the 'context' of a page (a directory containing, e.g., js- and
     * css-files).
     *
     * @param relativeDirPath the relative path to the directory
     * @return this page builder instance
     */
    public FromFilePageBuilder addResourceDirectory(final String relativeDirPath) {
        var root = getAbsoluteBasePath(m_clazz, m_bundleID, m_basePath);
        createResourcesFromDir(root, root.resolve(relativeDirPath), m_resources);
        return this;
    }

    private static FileResource createFileResource(final Class<?> clazz, final String bundleID, final String basePath,
        final String relativeFilePath) {
        var relFile = Paths.get(relativeFilePath);
        Path file = getAbsoluteBasePath(clazz, bundleID, basePath).resolve(relFile);
        return createResourceFromFile(relFile, file);
    }

    private static Path getAbsoluteBasePath(final Class<?> clazz, final String bundleID, final String baseDir) {
        if (clazz != null) {
            return getAbsoluteBasePath(FrameworkUtil.getBundle(clazz), baseDir);
        } else {
            return getAbsoluteBasePath(Platform.getBundle(bundleID), baseDir);
        }
    }

    /*
     * The bundle path + base path.
     */
    private static Path getAbsoluteBasePath(final Bundle bundle, final String baseDir) {
        var bundleUrl = bundle.getEntry(".");
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

    @Override
    public Page build() {
        return new Page(m_pageResource, m_resources, m_isComponent);
    }

    /*
     * -------------------------------------------------------------------------------------------------
     * Methods overwritten from the parent class in order to narrow down the returned page builder type.
     * -------------------------------------------------------------------------------------------------
     */

    @Override
    public FromFilePageBuilder addResource(final Supplier<InputStream> content, final String relativePath) {
        super.addResource(content, relativePath);
        return this;

    }

    @Override
    public FromFilePageBuilder addResourceFromString(final Supplier<String> content, final String relativePath) {
        super.addResourceFromString(content, relativePath);
        return this;
    }

}
