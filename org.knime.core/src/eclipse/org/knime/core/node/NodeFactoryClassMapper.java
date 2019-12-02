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
 * ---------------------------------------------------------------------
 *
 * Created on Oct 10, 2013 by wiswedel
 */
package org.knime.core.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * Base class for {@value #EXT_POINT_ID} extension point to allow node vendors to define a {@link NodeFactory} class
 * mapping. Used when a persisted workflow is referencing an outdated (and refactored) node implementation.
 *
 * <p>
 * Further details are in the corresponding extension point definition.
 * </p>
 *
 * <p>
 * If this class is extended directly the node mappings will not be exposed, which means it may not be possible to
 * replicate their behavior in other platforms. Instead extend {@link MapNodeFactoryClassMapper} or
 * {@link RegexNodeFactoryClassMapper} which do expose the mappings.
 * </p>
 *
 * @noreference This class is not intended to be referenced by clients.
 * @see MapNodeFactoryClassMapper
 * @see RegexNodeFactoryClassMapper
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 2.9
 */
public abstract class NodeFactoryClassMapper {

    /*--------- Static declarations ---------*/
    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeFactoryClassMapper.class);
    private static final String EXT_POINT_ID = "org.knime.core.NodeFactoryClassMapper";
    private static final String EXT_POINT_ATTR_CLASS_NAME = "classMapper";

    /*--------- Interface definition ---------*/

    /** Called during workflow load to allow extensions to map a factory class name to an
     * actual node factory instance. If the factory class name is unknown, implementations should
     * return null.
     * @param factoryClassName The class name as per the saved workflow (e.g. com.company.node.MyNodeFactory)
     * @return The mapped factory class or null if unknown.
     */
    public abstract NodeFactory<? extends NodeModel> mapFactoryClassName(final String factoryClassName);

    /*--------- (Static) Extension Point Handling ---------*/

    /** List containing all class mappers, collected from extension point contributions. */
    private static List<NodeFactoryClassMapper> mapperInstanceList;

    /** Called by the framework class to get the list of registered mappers.
     * @return A non-null, read-only list of registered mappers (potentially empty).
     * @noreference This method is not intended to be referenced by clients.
     */
    public static synchronized List<NodeFactoryClassMapper> getRegisteredMappers() {
        if (mapperInstanceList == null) {
            mapperInstanceList = collectNodeFactoryMappers();
        }
        return mapperInstanceList;
    }

    private static List<NodeFactoryClassMapper> collectNodeFactoryMappers() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
            return Collections.emptyList();
        }

        List<NodeFactoryClassMapper> resultList = new ArrayList<NodeFactoryClassMapper>();
        for (IConfigurationElement elem : point.getConfigurationElements()) {
            String classMapperCLName = elem.getAttribute(EXT_POINT_ATTR_CLASS_NAME);
            String decl = elem.getDeclaringExtension().getUniqueIdentifier();

            if (classMapperCLName == null || classMapperCLName.isEmpty()) {
                LOGGER.error("The extension '" + decl + "' doesn't provide the required attribute '"
                    + EXT_POINT_ATTR_CLASS_NAME + "' - ignoring it");
                continue;
            }

            NodeFactoryClassMapper instance = null;
            try {
                instance = (NodeFactoryClassMapper)elem.createExecutableExtension(EXT_POINT_ATTR_CLASS_NAME);
            } catch (Throwable t) {
                LOGGER.error("Problems during initialization of node factory class mapper (with id '"
                        + classMapperCLName + "'.)", t);
                if (decl != null) {
                    LOGGER.error("Extension " + decl + " ignored.");
                }
            }
            if (instance != null) { // We do not want to add invalid NodeFactoryClassMappers to this list.
                resultList.add(instance);
            }
        }
        return Collections.unmodifiableList(resultList);

    }

}
