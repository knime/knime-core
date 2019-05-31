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
 */
package org.knime.core.node.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * Gives access to the extensions that provide {@link FileSystemBrowser}-implementations.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.0
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class FileSystemBrowserExtension {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileSystemBrowserExtension.class);

    private static final String EXT_POINT_ID = "org.knime.core.FileSystemBrowser";

    private static final String EXT_POINT_ATTR_CLASS_NAME = "class";

    private FileSystemBrowserExtension() {
        //utility class
    }

    /**
     * Collects the file system browser registered as extensions.
     *
     * @return the collected browsers or an empty list
     */
    public static List<FileSystemBrowser> collectFileSystemBrowsers() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
            return Collections.emptyList();
        }

        List<FileSystemBrowser> resultList = new ArrayList<FileSystemBrowser>();
        for (IConfigurationElement elem : point.getConfigurationElements()) {
            String workflowSaveHookClassName = elem.getAttribute(EXT_POINT_ATTR_CLASS_NAME);
            String decl = elem.getDeclaringExtension().getUniqueIdentifier();

            if (StringUtils.isEmpty(workflowSaveHookClassName)) {
                LOGGER.errorWithFormat("The extension '%s' doesn't provide the required attribute '%s' - ignoring it",
                    decl, EXT_POINT_ATTR_CLASS_NAME);
                continue;
            }

            FileSystemBrowser instance = null;
            try {
                instance = (FileSystemBrowser)elem.createExecutableExtension(EXT_POINT_ATTR_CLASS_NAME);
            } catch (Throwable t) {
                LOGGER.error("Problems during initialization of file system browser (class '"
                    + workflowSaveHookClassName + "'.)", t);
                if (decl != null) {
                    LOGGER.error("Extension " + decl + " ignored.");
                }
            }
            resultList.add(instance);
        }
        return Collections.unmodifiableList(resultList);
    }
}
