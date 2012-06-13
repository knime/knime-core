/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2011
  * KNIME.com, Zurich, Switzerland
  *
  * You may not modify, publish, transmit, transfer or sell, reproduce,
  * create derivative works from, distribute, perform, display, or in
  * any way exploit any of the content, in whole or in part, except as
  * otherwise expressly permitted in writing by the copyright owner or
  * as specified in the license file distributed with this product.
  *
  * If you have any questions please contact the copyright holder:
  * website: www.knime.com
  * email: contact@knime.com
  * ---------------------------------------------------------------------
  *
  * History
  *   Jun 7, 2011 (morent): created
  */

package org.knime.workbench.repository.util;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.ContextAwareNodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public final class ContextAwareNodeFactoryMapper {
    private ContextAwareNodeFactoryMapper() {
        // utility class should not be instantiated
    }
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ContextAwareNodeFactoryMapper.class);
    private static final Map<String, Class<?
            extends ContextAwareNodeFactory<NodeModel>>>
            EXTENSION_REGISTRY;

    static {
        EXTENSION_REGISTRY = new TreeMap<String,
                Class<? extends ContextAwareNodeFactory<NodeModel>>>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        for (IConfigurationElement element : registry
                .getConfigurationElementsFor(
                   "org.knime.workbench.repository.registeredFileExtensions")) {
            try {
                /*
                 * Use the configuration element method to load an object of the
                 * given class name. This method ensures the correct classpath
                 * is used providing access to all extension points.
                 */
                final Object o =
                        element.createExecutableExtension("NodeFactory");
                @SuppressWarnings("unchecked")
                Class<? extends ContextAwareNodeFactory<NodeModel>> clazz =
                        (Class<? extends ContextAwareNodeFactory<NodeModel>>)
                                o.getClass();

                for (IConfigurationElement child : element.getChildren()) {
                    String extension = child.getAttribute("extension");
                    if (EXTENSION_REGISTRY.get(extension) == null) {
                        // add class
                        EXTENSION_REGISTRY.put(extension, clazz);
                    } // else already registered -> first come first serve
                }
            } catch (InvalidRegistryObjectException e) {
                throw new IllegalArgumentException(e);
            } catch (CoreException e) {
                throw new IllegalArgumentException(e);
            }
        }
        for (String key : EXTENSION_REGISTRY.keySet()) {
            LOGGER.debug("File extension: \"" + key + "\" registered for "
                    + "Node Factory: "
                    + EXTENSION_REGISTRY.get(key).getSimpleName() + ".");
        }
    }

     /**
      * @param url the url for which a node factory should be returned
      * @return the node factory registered for this extension, or null if
      *      the extension is not registered.
      */
     public static Class<? extends ContextAwareNodeFactory<NodeModel>>
             getNodeFactory(final String url) {
         for (Map.Entry<String,
                 Class<? extends ContextAwareNodeFactory<NodeModel>>>e
                         : EXTENSION_REGISTRY.entrySet()) {
             if (url.endsWith(e.getKey())) {
                 return e.getValue();
             }
         }
         return null;
     }
}
