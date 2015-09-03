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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.eclipseUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.app.CommandLineArgs;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.EclipseUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * This class contains some methods to access OSGI information that may not be
 * available if the program is not running in a complete OSGI framework.
 *
 * @author Robert Cauble, Pervasive
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
@SuppressWarnings("restriction")
public final class OSGIHelper {
    private static Map<Bundle, IInstallableUnit> bundleToFeatureMap = new HashMap<>();
    private static IProfile p2Profile;

    /**
     * Returns the bundle that contains the given class.
     *
     * @param cls a class
     * @return a bundle or <code>null</code> if the bundle can not be determined
     */
    public static Bundle getBundle(final Class<?> cls) {
        if (hasOSGIFramework()) {
            return BundleGetter.get(cls);
        } else {
            return null;
        }
    }


    /**
     * Returns the feature in which the given bundle is packaged. If there is more than one feature providing the bundle
     * then one of them (in no particular order) is returned. If the bundle cannot be found in any feature then an
     * empty result is returned.
     *
     * @param bundle any bundle
     * @return the installable unit containing the bundle
     * @since 3.0
     */
    public static synchronized Optional<IInstallableUnit> getFeature(final Bundle bundle) {
        IInstallableUnit u = bundleToFeatureMap.get(bundle);
        if (u != null) {
            return Optional.of(u);
        } else if (getP2Profile() != null) {
            IQueryResult<IInstallableUnit> queryResult = getP2Profile().query(QueryUtil.createIUGroupQuery(), null);
            for (IInstallableUnit unit : queryResult) {
                for (IArtifactKey afk : unit.getArtifacts()) {
                    if (afk.getId().equals(bundle.getSymbolicName())) {
                        bundleToFeatureMap.put(bundle, unit);
                        return Optional.of(unit);
                    }
                }

                for (IRequirement r : unit.getRequirements()) {
                    if (r instanceof IRequiredCapability) {
                        if (((IRequiredCapability)r).getName().equals(bundle.getSymbolicName())) {
                            bundleToFeatureMap.put(bundle, unit);
                            return Optional.of(unit);
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private synchronized static IProfile getP2Profile() {
        if (EclipseUtil.isRunFromSDK()) {
            return null; // when started from the SDK there is no installation profile
        }

        if (p2Profile == null) {
            BundleContext ctx = FrameworkUtil.getBundle(OSGIHelper.class).getBundleContext();
            ServiceReference<IProvisioningAgentProvider> ref = ctx.getServiceReference(IProvisioningAgentProvider.class);
            if (ref == null) {
                NodeLogger.getLogger(OSGIHelper.class).debug("Strange, I couldn't get a service reference for "
                    + IProvisioningAgentProvider.class);
                return null;
            }

            IProvisioningAgentProvider agentProvider = ctx.getService(ref);
            if (agentProvider == null) {
                NodeLogger.getLogger(OSGIHelper.class).debug("Strange, I couldn't get a service for "
                        + IProvisioningAgentProvider.class);
                return null;
            }

            try {
                IProvisioningAgent provisioningAgent = agentProvider.createAgent(null);
                IProfileRegistry profileRegistry =
                    (IProfileRegistry)provisioningAgent.getService(IProfileRegistry.SERVICE_NAME);
                p2Profile = profileRegistry.getProfile(IProfileRegistry.SELF);
                if (p2Profile == null) {
                    NodeLogger.getLogger(OSGIHelper.class).debug("Couldn't get the p2 installation profile");
                    return null; // happens when started from the SDK
                }
            } catch (ProvisionException ex) {
                NodeLogger.getLogger(OSGIHelper.class).error("Could not create provisioning agent: " + ex.getMessage(),
                    ex);
            } finally {
                ctx.ungetService(ref);
            }
        }

        return p2Profile;
    }

    /**
     * Returns the name of the current Eclipse application.
     *
     * @return the application name
     */
    public static String getApplicationName() {
        if (hasOSGIFramework() && hasEclipsePlatform()) {
            return ProductNameGetter.get();
        } else {
            return "<unknown>";
        }
    }

    /**
     * Returns the resolved bundle with the specified symbolic name that has the
     * highest version. If no resolved bundles are installed that have the
     * specified symbolic name, or if this is not an Eclipse instance then
     * <code>null</code> is returned.
     *
     * @param symbolicName the symbolic name of the bundle to be returned.
     * @return the bundle that has the specified symbolic name with the highest
     *         version, or <tt>null</tt> if no bundle is found.
     */
    public static Bundle getBundle(final String symbolicName) {
        if (hasEclipsePlatform()) {
            return Platform.getBundle(symbolicName);
        } else {
            return null;
        }
    }

    private static boolean hasOSGIFramework() {
        try {
            Class.forName("org.osgi.framework.FrameworkUtil");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean hasEclipsePlatform() {
        try {
            Class.forName("org.eclipse.core.runtime.Platform");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static class BundleGetter {
        public static Bundle get(final Class<?> cls) {
            return FrameworkUtil.getBundle(cls);
        }
    }

    private static class ProductNameGetter {
        public static String get() {
            IProduct product = Platform.getProduct();
            if (product != null) {
                return product.getApplication();
            } else {
                String[] args = CommandLineArgs.getAllArgs();
                for (int i = 0; i < args.length; i++) {
                    if ("-application".equals(args[i])) {
                        if (args.length > (i + 1)) {
                            return args[i + 1];
                        } else {
                            return "<unknown>";
                        }
                    }
                }
                return "<unknown>";
            }
        }
    }
}
