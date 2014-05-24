/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
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
