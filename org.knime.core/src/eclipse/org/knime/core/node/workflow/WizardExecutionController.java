/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on Jan 29, 2014 by wiswedel
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.web.DefaultWebTemplate;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebResourceLocator.WebResourceType;
import org.knime.core.node.web.WebTemplate;

/**
 * A utility class received from the workflow manager that allows to step back and forth in a wizard execution.
 *
 * <p>
 * Do not use, no public API.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Christian Albrecht, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public final class WizardExecutionController {

    // hide constructor
    private WizardExecutionController() { }

    private static final String ID_WEB_RES = "org.knime.js.core.webResources";

    private static final String ID_JS_COMP = "org.knime.js.core.javascriptComponents";

    private static final String ID_IMPL_BUNDLE = "implementationBundleID";

    private static final String ID_IMPORT_RES = "importResource";

    private static final String ID_DEPENDENCY = "webDependency";

    private static final String ATTR_JS_ID = "jsObjectID";

    private static final String ATTR_RES_BUNDLE_ID = "webResourceBundleID";

    private static final String ATTR_PATH = "relativePath";

    private static final String ATTR_TYPE = "type";

    /**
     * @param jsObjectID The JavaScript object ID used for locating the extension point.
     * @return A template object, being used to assamble views.
     */
    public static WebTemplate getWebTemplateFromJSObjectID(final String jsObjectID) {
        List<WebResourceLocator> webResList = new ArrayList<WebResourceLocator>();
        IConfigurationElement jsComponentExtension = getConfigurationFromID(ID_JS_COMP, ATTR_JS_ID, jsObjectID);
        String bundleID = jsComponentExtension.getAttribute(ID_IMPL_BUNDLE);
        IConfigurationElement implementationExtension =
            getConfigurationFromID(ID_WEB_RES, ATTR_RES_BUNDLE_ID, bundleID);
        List<WebResourceLocator> implementationRes = getResourcesFromExtension(implementationExtension);
        for (IConfigurationElement dependencyConf : jsComponentExtension.getChildren(ID_DEPENDENCY)) {
            String dependencyID = dependencyConf.getAttribute(ATTR_RES_BUNDLE_ID);
            IConfigurationElement dependencyExtension = getConfigurationFromID(ID_WEB_RES, ATTR_RES_BUNDLE_ID, dependencyID);
            List<WebResourceLocator> dependencyRes = getResourcesFromExtension(dependencyExtension);
            webResList.addAll(dependencyRes);
        }
        webResList.addAll(implementationRes);
        return new DefaultWebTemplate(webResList.toArray(new WebResourceLocator[0]), null, jsObjectID);
    }

    private static IConfigurationElement getConfigurationFromID(final String extensionPointId,
        final String configurationID, final String jsObjectID) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] configurationElements = registry.getConfigurationElementsFor(extensionPointId);
        for (IConfigurationElement element : configurationElements) {
            if (jsObjectID.equals(element.getAttribute(configurationID))) {
                return element;
            }
        }
        return null;
    }

    private static List<WebResourceLocator> getResourcesFromExtension(final IConfigurationElement resConfig) {
        String pluginName = resConfig.getContributor().getName();
        List<WebResourceLocator> locators = new ArrayList<WebResourceLocator>();
        for (IConfigurationElement resElement : resConfig.getChildren(ID_IMPORT_RES)) {
            String path = resElement.getAttribute(ATTR_PATH);
            String type = resElement.getAttribute(ATTR_TYPE);
            if (path != null && type != null) {
                WebResourceType resType = WebResourceType.FILE;
                if (type.equalsIgnoreCase("javascript")) {
                    resType = WebResourceType.JAVASCRIPT;
                } else if (type.equalsIgnoreCase("css")) {
                    resType = WebResourceType.CSS;
                }
                locators.add(new WebResourceLocator(pluginName, path, resType));
            }
        }
        return locators;
    }

}
