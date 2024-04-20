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
 * -------------------------------------------------------------------
 *
 * History
 *   Oct 13, 2006 (wiswedel): created
 */
package org.knime.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.eclipse.core.runtime.Platform;
import org.knime.core.customization.APCustomizationProviderService;
import org.knime.core.customization.APCustomizationProviderServiceImpl;
import org.knime.core.node.port.report.IReportService;
import org.knime.core.util.auth.DelegatingAuthenticator;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Plugin class that is initialized when the plugin project is started. It will
 * set the workspace path as KNIME home dir in the KNIMEConstants utility class.
 * @author wiswedel, University of Konstanz
 */
public class CorePlugin implements BundleActivator {
    private static CorePlugin instance;

    /** see {@link #setWrapColumnHeaderInTableViews(boolean)}. */
    private boolean m_isWrapColumnHeaderInTableViews = false;

    /** A property controlled by the UI preference page. We need a field in the core plugin as it does not access
     * to any UI plugin. We rely on the UI plugin to init/update this field.
     * @param value the isWrapColumnHeaderInTableViews to set
     * @noreference This method is not intended to be referenced by clients.
     * @since 2.8
     */
    public final void setWrapColumnHeaderInTableViews(final boolean value) {
        m_isWrapColumnHeaderInTableViews = value;
    }

    /** see {@link #setWrapColumnHeaderInTableViews(boolean)}.
     * @return the isWrapColumnHeaderInTableViews
     * @noreference This method is not intended to be referenced by clients.
     * @since 2.8
     */
    public final boolean isWrapColumnHeaderInTableViews() {
        return m_isWrapColumnHeaderInTableViews;
    }

    /**
     * Added as part of AP-20402 - report generation on subnodes will be done by an extra extension, which needs to be
     * installed. Presence of the service also is used as feature flag to disable user controls.
     */
    private ServiceTracker<IReportService, IReportService> m_reportServiceTracker;

    /** A service updated by the core plug-in. Updated when
     * profiles are read asynchronously from a remote endpoint. */
    private ServiceRegistration<APCustomizationProviderService> m_customizationServiceRegistration;

    /**
     * The tracker for the customization (currently in the same bundle). Using service tracking events just like any
     * other bundle should be notified when the service changes.
     */
    private ServiceTracker<APCustomizationProviderService, APCustomizationProviderService>
            m_customizationServiceTracker;

    @Override
    public void start(final BundleContext context)
        throws Exception {
        instance = this; // NOSONAR (static assignment)

        /* Unfortunately we have to activate the plugin
         * org.eclipse.ecf.filetransfer explicitly by accessing one of the
         * contained classed. This will trigger the initialization of the
         * extension point org.eclipse.ecf.filetransfer.urlStreamHandlerService
         * contained in the plugin which is necessary for registering the
         * "knime" URL protocol. */
        try {
            Class.forName("org.eclipse.ecf.filetransfer.IFileTransfer");
        } catch (ClassNotFoundException e) {
            // this may happen in a non-Eclipse OSGi environment
        }

        m_customizationServiceRegistration = context.registerService(APCustomizationProviderService.class,
            new APCustomizationProviderServiceImpl(), null);
        m_customizationServiceTracker = new ServiceTracker<>(context, APCustomizationProviderService.class, null);
        m_customizationServiceTracker.open();

        readMimeTypes();
        m_reportServiceTracker = new ServiceTracker<>(context, IReportService.class, null);
        m_reportServiceTracker.open();

        /*
         * Listening on the proxy service initialization, we can apply our popup suppressor (SuppressingAuthenticator).
         * Needs to happen early to avoid interference with org.apache.cxf.transport.http.ReferencingAuthenticator.
         */
        context.addServiceListener(event -> {
            if (event.getType() == ServiceEvent.REGISTERED) {
                DelegatingAuthenticator.installAuthenticators();
            }
        }, String.format("(%s=org.eclipse.core.net.proxy.IProxyService)", Constants.OBJECTCLASS));
    }

    private void readMimeTypes() throws IOException {
        Bundle utilBundle = Platform.getBundle("org.knime.core.util");
        URL mimeFile = utilBundle.getResource("META-INF/mime.types");
        if (mimeFile != null) {
            try (InputStream is = mimeFile.openStream()) {
                MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap(is);
                FileTypeMap.setDefaultFileTypeMap(mimeMap);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop(final BundleContext context) throws Exception {
        m_reportServiceTracker.close();
        m_reportServiceTracker = null;
        m_customizationServiceTracker.close();
        m_customizationServiceTracker = null;
        m_customizationServiceRegistration.unregister();
        m_customizationServiceRegistration = null;
        instance = null;
    }

    /**
     * Returns the singleton instance of this plugin.
     *
     * @return the plugin
     */
    public static CorePlugin getInstance() {
        return instance;
    }

    /**
     * @return the customization service, or an empty {@link Optional} if none is registered.
     * @since 5.3
     */
    public Optional<APCustomizationProviderService> getCustomizationService() {
        return Optional.ofNullable(m_customizationServiceTracker).map(ServiceTracker::getService);
    }

    /**
     * @return the report service, or an empty {@link Optional} if none is registered (extension not installed).
     * @since 5.1
     */
    public Optional<IReportService> getReportService() {
        return Optional.ofNullable(m_reportServiceTracker).map(ServiceTracker::getService);
    }

    /** Fetches a service implementing the {@link URIToFileResolve} interface
     * and returns the resolved file.
     * @param uri The URI to resolve
     * @return The local file underlying the URI (if any)
     * @throws IOException If no service is registered or the URI can't be
     * resolved.
     *
     * @deprecated Use {@link ResolverUtil#resolveURItoLocalFile(URI)} instead
     */
    @Deprecated
    public static File resolveURItoLocalFile(final URI uri) throws IOException {
        return ResolverUtil.resolveURItoLocalFile(uri);
    }

    /** Fetches a service implementing the {@link URIToFileResolve} interface
     * and returns the resolved file.
     * @param uri The URI to resolve
     * @return The local file or temporary copy of a remote file underlying the
     *      URI (if any)
     * @throws IOException If no service is registered or the URI can't be
     *      resolved.
     * @deprecated Use {@link ResolverUtil#resolveURItoLocalOrTempFile(URI)} instead
     */
    @Deprecated
    public static File resolveURItoLocalOrTempFile(final URI uri)
            throws IOException {
        return ResolverUtil.resolveURItoLocalOrTempFile(uri);
    }
}
