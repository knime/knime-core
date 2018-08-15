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
 *   Aug 14, 2018 (awalter): created
 */
package org.knime.workbench.editor2.subnode;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.util.FileUtil;
import org.osgi.framework.Bundle;

/**
 *
 * @author Alison Walter, KNIME.com GmbH, Konstanz, Germany
 */
public final class WebResourceUtil {

    /** ID for web resources */
    public static final String ID_WEB_RES = "org.knime.js.core.webResources";

    /** ID for Javascript components */
    public static final String ID_JS_COMP = "org.knime.js.core.javascriptComponents";

    /** Implementation bundle ID */
    public static final String ID_IMPL_BUNDLE = "implementationBundleID";

    /** Import resource ID */
    public static final String ID_IMPORT_RES = "importResource";

    /** ID for web dependencies */
    public static final String ID_DEPENDENCY = "webDependency";

    /** Attribute ID for javascript components */
    public static final String ATTR_JS_ID = "javascriptComponentID";

    /** Attribute for namespace */
    public static final String ATTR_NAMESPACE = "namespace";

    /** Attribute for web resource bundle ID */
    public static final String ATTR_RES_BUNDLE_ID = "webResourceBundleID";

    /** Attribute for relative path */
    public static final String ATTR_PATH = "relativePath";

    /** Attribute for type */
    public static final String ATTR_TYPE = "type";

    /** Attribute for init method name */
    public static final String ATTR_INIT_METHOD_NAME = "init-method-name";

    /** Attribute for validate method name */
    public static final String ATTR_VALIDATE_METHOD_NAME = "validate-method-name";

    /** Attribute for get component value method name */
    public static final String ATTR_GETCOMPONENTVALUE_METHOD_NAME = "getComponentValue-method-name";

    /** Attribute for set validation error method name */
    public static final String ATTR_SETVALIDATIONERROR_METHOD_NAME = "setValidationError-method-name";

    /** ID for web resources */
    public static final String ID_WEB_RESOURCE = "webResource";

    /** Attribute for relative path source */
    public static final String ATTR_RELATIVE_PATH_SOURCE = "relativePathSource";

    /** Attribute for relative path target */
    public static final String ATTR_RELATIVE_PATH_TARGET = "relativePathTarget";

    /** Default dependency for web resources, KNIME Service */
    public static final String DEFAULT_DEPENDENCY = "knimeService_1.0";

    /** Set of {@link WebResourceLocator}s for the default web resource */
    //    private static final Set<WebResourceLocator> DEFAULT_RES =
    //            getResourcesFromExtension(getConfigurationFromID(ID_WEB_RES, ATTR_RES_BUNDLE_ID, DEFAULT_DEPENDENCY));

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WebResourceUtil.class);

    private WebResourceUtil() {
        // Prevent instantiation of util class
    }

    /**
     * Creates a temporary directory for Java with the given name.
     *
     * @param name the name of the temporary directory
     * @return the temporary directory
     * @throws IOException
     */
    public static File createTempDirectory(final String name) throws IOException {
        File tempDir = null;
        final String tempPath = System.getProperty("java.io.tmpdir");
        if (tempPath != null) {
            tempDir = new File(tempPath);
        }
        return FileUtil.createTempDir(name, tempDir, true);
    }

    /**
     * Copies web resource files to the given temporary directory.
     *
     * @param tempDirectory the temporary directory
     * @param resources a {@code Map} of the desired files to the relative file location in the temporary directory
     * @throws IOException
     */
    public static void copyWebResourcesToTemp(final File tempDirectory, final Map<File, String> resources) throws IOException {
        for (final Entry<File, String> copyEntry : resources.entrySet()) {
            final File src = copyEntry.getKey();
            final File dest = new File(tempDirectory, FilenameUtils.separatorsToSystem(copyEntry.getValue()));
            if (src.isDirectory()) {
                FileUtils.copyDirectory(src, dest);
            } else {
                FileUtils.copyFile(src, dest);
            }
        }
    }

    /**
     * Returns the first {@link IConfigurationElement} with the given ID.
     *
     * @param extensionPointId the extension point to check
     * @param configurationID the configuration attribute to retrieve
     * @param jsObjectID the value of the configuration attribute
     * @return the first matching {@link IConfigurationElement}
     */
    public static IConfigurationElement getConfigurationFromID(final String extensionPointId,
        final String configurationID, final String jsObjectID) {
        if (jsObjectID != null) {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IConfigurationElement[] configurationElements = registry.getConfigurationElementsFor(extensionPointId);
            for (final IConfigurationElement element : configurationElements) {
                if (jsObjectID.equals(element.getAttribute(configurationID))) {
                    return element;
                }
            }
        }
        return null;
    }

    /**
     * Get all web resources for a given {@link IConfigurationElement}.
     * @param resConfig element whose web resources will be returned
     * @return a {@code Map} of the web resource's source file/directory to the path of its relative target directory/file
     */
    public static Map<File, String> getWebResources(final IConfigurationElement resConfig) {
        if (resConfig == null) {
            return Collections.emptyMap();
        }
        final Map<File, String> copyLocations = new HashMap<File, String>();
        final String pluginName = resConfig.getDeclaringExtension().getContributor().getName();
        final Bundle bundle = Platform.getBundle(pluginName);
        for (final IConfigurationElement resElement : resConfig.getChildren(ID_WEB_RESOURCE)) {
            final String relSource = resElement.getAttribute(ATTR_RELATIVE_PATH_SOURCE);

            try {
                resolveResource(copyLocations, bundle, resElement, relSource);
            } catch (IOException | URISyntaxException ex) {
                LOGGER.errorWithFormat(
                    "Web resource source file '%s' from plug-in %s could not be resolved: %s", relSource,
                    pluginName, ex.getMessage(), ex);
            }
        }
        return copyLocations;
    }

    /**
     * Get all web resources and their dependencies for a given {@link IConfigurationElement}.
     * @param resConfig element whose web resources and dependencies will be returned
     * @return a {@code Map} of the web resource's source file/directory to the path of its relative target directory/file
     */
    public static Map<File, String> getWebResourceAndDependencies(final IConfigurationElement resConfig) {
        if (resConfig == null) {
            return Collections.emptyMap();
        }
        final Map<File, String> copyLocations = getWebResources(resConfig);
        final String pluginName = resConfig.getDeclaringExtension().getContributor().getName();
        final Bundle bundle = Platform.getBundle(pluginName);
        addDependencies(copyLocations, resConfig, bundle, pluginName);

        return copyLocations;
    }

    // -- Helper methods --

    private static void resolveResource(final Map<File, String> copyLocations, final Bundle bundle,
        final IConfigurationElement resElement, final String relSource) throws IOException, URISyntaxException {
        final URL sourceURL = FileLocator.find(bundle, new Path(relSource), null);
        if (sourceURL == null) {
            throw new IOException("Cannot find location of '" + relSource + "' in bundle");
        }

        final URL sourceFileURL = FileLocator.toFileURL(sourceURL);
        final java.nio.file.Path sourcePath = FileUtil.resolveToPath(sourceFileURL);
        if (sourcePath == null) {
            throw new IOException("Cannot resolve '" + sourceFileURL + "' to local file");
        }

        String relTarget = resElement.getAttribute(ATTR_RELATIVE_PATH_TARGET);
        if (StringUtils.isEmpty(relTarget)) {
            relTarget = relSource;
        }
        copyLocations.put(sourcePath.toFile(), relTarget);
    }

    private static void addDependencies(final Map<File, String> copyLocations, final IConfigurationElement resConfig,
        final Bundle bundle, final String pluginName) {
        for (final IConfigurationElement resElement : resConfig.getChildren(ID_DEPENDENCY)) {
            final String bundleID = resElement.getAttribute(ATTR_RES_BUNDLE_ID);
            final IConfigurationElement dependency = getConfigurationFromID(ID_WEB_RES, ATTR_RES_BUNDLE_ID, bundleID);
            final Map<File, String> dependencyPaths = getWebResources(dependency);
            dependencyPaths.forEach((f, s) -> copyLocations.put(f, s));
            addDependencies(copyLocations, dependency, bundle, pluginName);
        }
    }
}
