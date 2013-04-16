/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2013
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
import org.eclipse.swt.graphics.Image;
import org.knime.core.node.ContextAwareNodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.util.Pair;
import org.knime.workbench.core.util.ImageRepository;

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
    private static final Map<String, Pair<Class<? extends ContextAwareNodeFactory>, Image>> EXTENSION_REGISTRY;

    static {
        EXTENSION_REGISTRY = new TreeMap<String, Pair<Class<? extends ContextAwareNodeFactory>,Image>>();
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
                @SuppressWarnings("unchecked")
                final ContextAwareNodeFactory<NodeModel> o =
                        (ContextAwareNodeFactory<NodeModel>)element.createExecutableExtension("NodeFactory");
                Class<? extends ContextAwareNodeFactory> clazz = o.getClass();

                for (IConfigurationElement child : element.getChildren()) {
                    String extension = child.getAttribute("extension");
                    if (EXTENSION_REGISTRY.get(extension) == null) {
                        Image icon = null;
                        if (!Boolean.getBoolean("java.awt.headless")) {
                            // Load images from declaring plugin
                            icon = ImageRepository.getScaledImage(o, 16, 16);
                        }
                        EXTENSION_REGISTRY.put(extension,
                                               new Pair<Class<? extends ContextAwareNodeFactory>, Image>(clazz, icon));
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
                    + EXTENSION_REGISTRY.get(key).getFirst().getSimpleName() + ".");
        }
    }

     /**
      * @param url the url for which a node factory should be returned
      * @return the node factory registered for this extension, or null if
      *      the extension is not registered.
      */
     public static Class<? extends ContextAwareNodeFactory> getNodeFactory(final String url) {
        for (Map.Entry<String, Pair<Class<? extends ContextAwareNodeFactory>, Image>> e : EXTENSION_REGISTRY.entrySet()) {
            if (url.endsWith(e.getKey())) {
                return e.getValue().getFirst();
            }
        }
         return null;
     }

     /**
      * Return the image to the registered extension, of null, if it is not a registered extension, or the node doesn't
      * provide an image.
      * @param url
      * @return
      * @since 2.7
      */
     public static Image getImage(final String url) {
         for (Map.Entry<String, Pair<Class<? extends ContextAwareNodeFactory>, Image>> e : EXTENSION_REGISTRY.entrySet()) {
             if (url.endsWith(e.getKey())) {
                 return e.getValue().getSecond();
             }
         }
          return null;
     }
}
