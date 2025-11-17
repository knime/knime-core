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
 *   Dec 19, 2023 (lw): created
 */
package org.knime.core.eclipseUtil;

import org.eclipse.core.net.proxy.IProxyService;
import org.knime.core.internal.CorePlugin;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.IEarlyStartup;
import org.knime.core.util.auth.DelegatingAuthenticator;
import org.knime.core.util.proxy.ProxySelectorAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Initializes Eclipse's proxy service early in the application startup before other plugins (using CXF) can initialize.
 * <p>
 * The intent is the install a suppressing measure for the {@link org.eclipse.ui.internal.net.auth.NetAuthenticator}
 * since it automatically displays UI popups to the user when authentication failed. The installation happens in the
 * {@link CorePlugin} via a listener on this proxy service initialization.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
public class EclipseProxyServiceInitializer implements IEarlyStartup {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(EclipseProxyServiceInitializer.class);

    private static final String PROXY_SERVICE_NAME = "org.eclipse.core.net.proxy.IProxyService";

    /**
     * Flag indicating whether {@link #runInstallation()} has already been invoked.
     */
    private static boolean isInstalledGlobally;

    private static ServiceTracker<IProxyService, IProxyService> tracker;

    private static synchronized void runInstallation() {
        try {
            if (!isInstalledGlobally) {
                // (3) Actually invoke KNIME-custom, proxy-supporting services and adapters.
                DelegatingAuthenticator.installAuthenticators();
                ProxySelectorAdapter.installProxySelector();
                NativeProxyProviderWrapper.installNativeWrapper();
                isInstalledGlobally = true;

                LOGGER.debug("(3/3) Successfully installed proxy support.");
            }
        } finally {
            tracker.close();
            tracker = null;
        }
    }

    /**
     * Starts listening on the service registration of {@link IProxyService} and executes
     * the installation of proxy-supporting services, modules, and adapters in the KAP.
     * <p>
     * If the service is already present and the installation was *not* called yet, it is
     * immediately run. If the service is not present, and the installation was not called yet,
     * a listener is defined to run upon service registration. Both of these actions are
     * performed via the {@link ServiceTrackerCustomizer#addingService(ServiceReference)} API.
     * </p>
     * If the installation was already performed and {@link #startListening(BundleContext)} were
     * to be called again (independent of the service's presence), the installation is *not* re-run.
     * @param context the bundle context
     *
     * @noreference This method is not intended to be referenced by clients.
     * @since 5.4
     */
    public static synchronized void startListening(final BundleContext context) {
        // (1) Define a tracker that both acts as listener and getter. If the service is already present,
        // calling ServiceTracker#open will invoke the #runInstallation method immediately.
        tracker = new ServiceTracker<>(context, PROXY_SERVICE_NAME, new ServiceTrackerCustomizer<>() {

            @Override
            public IProxyService addingService(final ServiceReference<IProxyService> reference) {
                runInstallation();
                return context.getService(reference);
            }

            @Override
            public void modifiedService(final ServiceReference<IProxyService> reference, //
                final IProxyService service) {
                // nothing to do
            }

            @Override
            public void removedService(final ServiceReference<IProxyService> reference, //
                final IProxyService service) {
                context.ungetService(reference);
            }
        });
        tracker.open();
        LOGGER.debug("(1/3) Opened OSGi service tracker for '%s'.".formatted(PROXY_SERVICE_NAME));
    }

    /**
     * Ensures that Eclipse's proxy service is initialized. To retrieve the actual service object, use a
     * {@link ServiceTracker} on the {@link IProxyService} class.
     */
    public static void ensureInitialized() {
        /*
         * Using the proxy service class object initializes the class, as well as its bundle activator
         * (being org.eclipse.core.internal.net.Activator). This activator starts Eclipse's proxy service
         * which we want to handle early in the application startup (see above).
         */
        IProxyService.class.getName();
    }

    @Override
    public void run() {
        // (2) Initialize Eclipse proxy service (using the above-set listeners and properties).
        LOGGER.debug("(2/3) Initializing OSGi service '%s'...".formatted(PROXY_SERVICE_NAME));
        ensureInitialized();
    }
}
