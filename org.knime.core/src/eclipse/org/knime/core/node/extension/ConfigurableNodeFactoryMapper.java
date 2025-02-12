/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.extension;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeLogger;

import com.google.common.base.Suppliers;

/**
 * Mapper from file extensions to registered configurable node factories.
 *
 * @noreference This class is not intended to be referenced by clients.
 *
 * @since 5.5
 */
public final class ConfigurableNodeFactoryMapper {
    private ConfigurableNodeFactoryMapper() {
        // utility class should not be instantiated
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ConfigurableNodeFactoryMapper.class);

    private static final Map<String, ExtensionDetails> EXTENSION_REGISTRY;

    static {
        EXTENSION_REGISTRY = new TreeMap<>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        for (IConfigurationElement element : registry
            .getConfigurationElementsFor("org.knime.workbench.repository.registeredFileExtensions")) {
            try {
                final String factoryClassName = element.getAttribute("NodeFactory");

                // delay object instantiation until needed (AP-23071). To init Modern UI the
                // mapping '.ext' -> 'class name' is sufficient
                Supplier<Optional<FactoryClassAndIconUrl>> factoryClassSupplier = Suppliers.memoize(() -> { // NOSONAR
                    try {
                        final ConfigurableNodeFactory<?> factory =
                            (ConfigurableNodeFactory<?>)element.createExecutableExtension("NodeFactory");
                        @SuppressWarnings("unchecked")
                        final Class<? extends ConfigurableNodeFactory<?>> factoryClass =
                            (Class<? extends ConfigurableNodeFactory<?>>)factory.getClass();
                        return Optional.of(new FactoryClassAndIconUrl(factoryClass, factory.getIcon()));
                    } catch (Exception e) { // NOSONAR (3rd party code)
                        LOGGER.error(String.format("Error initializing factory class: \"%s\"", factoryClassName), e);
                        return Optional.empty();
                    }
                });

                for (IConfigurationElement child : element.getChildren()) {
                    String extension = child.getAttribute("extension");
                    EXTENSION_REGISTRY.putIfAbsent(extension,
                        new ExtensionDetails(factoryClassName, factoryClassSupplier));
                }
            } catch (InvalidRegistryObjectException e) {
                LOGGER.error(() -> String.format(
                    "File extension handler from contributor \"%s\" doesn't properly load -- ignoring it.",
                    element.getContributor().getName()), e);
            }
        }
        for (Map.Entry<String, ExtensionDetails> entry : EXTENSION_REGISTRY.entrySet()) {
            LOGGER.debugWithFormat("File extension: \"%s\" registered for Node Factory \"%s\".", entry.getKey(),
                StringUtils.substringAfterLast(entry.getValue().factoryClassName, '.'));
        }
    }

    /**
     * Determines node factories for all supported file extensions.
     *
     * @return a map from file extension to node factory
     */
    public static Map<String, String> getAllNodeFactoriesForFileExtensions() {
        return EXTENSION_REGISTRY.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().factoryClassName));
    }

    /**
     * @param url the url for which a node factory should be returned
     * @return the node factory registered for this extension, or {@code null} if the extension is not registered or in
     *         case of an error during factory init
     */
    public static Class<? extends ConfigurableNodeFactory<?>> getNodeFactory(final String url) { // NOSONAR
        return EXTENSION_REGISTRY.entrySet().stream() //
            .filter(e -> StringUtils.endsWithIgnoreCase(url, e.getKey())) //
            .map(e -> e.getValue().getOrLoadFactoryClass()) //
            .filter(Optional::isPresent) // may be empty in case of error during factory init (error logged)
            .map(t -> (Class<? extends ConfigurableNodeFactory<?>>)t.get()) //
            .findFirst() //
            .orElse(null);
    }

    /**
     * @param url
     * @return whether there is a node factory registered for the given extension
     */
    public static boolean hasNodeFactory(final String url) {
        return EXTENSION_REGISTRY.entrySet().stream() //
            .anyMatch(e -> StringUtils.endsWithIgnoreCase(url, e.getKey()));
    }

    /**
     * Return the image to the registered extension, of null, if it is not a registered extension, or the node doesn't
     * provide an image.
     *
     * @param url
     * @return the image, or {@code null} if the extension is not registered or in
     *         case of an error during factory init
     * @since 2.7
     */
    public static URL getIconUrl(final String url) {
        return EXTENSION_REGISTRY.entrySet().stream() //
            .filter(e -> StringUtils.endsWithIgnoreCase(url, e.getKey())) //
            .map(e -> e.getValue().getOrLoadIconUrl()) //
            .filter(Optional::isPresent) // may be empty in case of error during factory init (error logged)
            .map(t -> t.get()) //
            .findFirst() //
            .orElse(null);
    }

    /** Private record to store extension details, whereby any (expensive) class loading and object instantiation is
     * delayed until needed. Added as part of AP-23071. */
    private record ExtensionDetails(String factoryClassName, Supplier<Optional<FactoryClassAndIconUrl>> details) {

        Optional<Class<? extends ConfigurableNodeFactory<?>>> getOrLoadFactoryClass() { // NOSONAR
            return details.get().map(FactoryClassAndIconUrl::factoryClass);
        }

        Optional<URL> getOrLoadIconUrl() {
            return details.get().map(FactoryClassAndIconUrl::iconUrl);
        }
    }

    /** Pair of factory class and icon image. */
    private record FactoryClassAndIconUrl(Class<? extends ConfigurableNodeFactory<?>> factoryClass, URL iconUrl) {
    }
}
