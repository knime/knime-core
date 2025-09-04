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
 *   Aug 27, 2024 (lw): created
 */
package org.knime.core.eclipseUtil;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.internal.net.AbstractProxyProvider;
import org.eclipse.core.internal.net.PreferenceManager;
import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.knime.core.internal.CorePlugin;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.proxy.EnvironmentProxyConfigProvider;
import org.knime.core.util.proxy.ExcludedHostsTokenizer;
import org.knime.core.util.proxy.GlobalProxyConfig;
import org.knime.core.util.proxy.ProxyProtocol;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Enriches the native {@link AbstractProxyProvider} with full environment variables
 * support via the {@link EnvironmentProxyConfigProvider}.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 */
@SuppressWarnings("restriction")
public final class NativeProxyProviderWrapper extends AbstractProxyProvider {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NativeProxyProviderWrapper.class);

    private static final IProxyData[] EMPTY_IPROXY_DATA = new IProxyData[0];

    /**
     * Flag indicating whether {@link #installNativeWrapper()} has already been invoked.
     */
    private static boolean isInstalledGlobally;

    private final PublicProxyProvider m_wrappedProvider;

    private final Map<ProxyProtocol, IProxyData> m_envProxyConfigs;

    private final String[] m_envProxyExclusions;

    private NativeProxyProviderWrapper(final PublicProxyProvider wrapped) {
        m_wrappedProvider = Objects.requireNonNull(wrapped);
        final Map<ProxyProtocol, IProxyData> configs = new EnumMap<>(ProxyProtocol.class);
        final Set<String> exclusions = new HashSet<>();
        for (var entry : EnvironmentProxyConfigProvider.getAllEnvironmentProxies().entrySet()) {
            final var config = entry.getValue();
            configs.put(entry.getKey(), toIProxyData(config));
            if (config.useExcludedHosts()) {
                ExcludedHostsTokenizer.tokenizeAsStream(config.excludedHosts()).forEach(exclusions::add);
            }
        }
        m_envProxyConfigs = Collections.unmodifiableMap(configs);
        m_envProxyExclusions = exclusions.toArray(String[]::new);
    }

    private static IProxyData toIProxyData(final GlobalProxyConfig config) {
        final var data = new ProxyData(config.protocol().name(), config.host(), config.intPort(), //
            config.useAuthentication(), null);
        if (config.useAuthentication()) {
            data.setUserid(config.username());
            data.setPassword(config.password());
        }
        return data;
    }

    // -- PROXY PROVIDER API --

    /**
     * Based on the cached proxy data, returns a version filtered subset based on the
     * {@link ProxyProtocol} that was given. If the protocol is {@code null}, all cached
     * proxy data is returned as an array.
     *
     * @param protocol the {@link ProxyProtocol} to filter for
     * @return fitlered data as array
     */
    private IProxyData[] getEnvironmentProxies(final ProxyProtocol protocol) {
        if (protocol == null) {
            return m_envProxyConfigs.values().toArray(IProxyData[]::new);
        }
        final var proxy = m_envProxyConfigs.get(protocol);
        return proxy != null ? new IProxyData[]{proxy} : EMPTY_IPROXY_DATA;
    }

    @Override
    public IProxyData[] select(final URI uri) {
        if (m_wrappedProvider.hasProxies()) {
            return m_wrappedProvider.select(uri);
        }
        ProxyProtocol protocol = null;
        if (uri != null) {
            protocol = EnumUtils.getEnum(ProxyProtocol.class, StringUtils.upperCase(uri.getScheme()));
        }
        return getEnvironmentProxies(protocol);
    }

    @Override
    protected IProxyData[] getProxyData() {
        if (m_wrappedProvider.hasProxies()) {
            return m_wrappedProvider.getProxyData();
        }
        return getEnvironmentProxies((ProxyProtocol)null);
    }

    @Override
    protected String[] getNonProxiedHosts() {
        if (m_wrappedProvider.hasProxies()) {
            return m_wrappedProvider.getNonProxiedHosts();
        }
        return m_envProxyExclusions;
    }

    // -- WRAPPED PROVIDER ACCESS --

    /**
     * Sets this {@link AbstractProxyProvider} as the native proxy provider in the shared
     * instance of {@link ProxyManager#getProxyManager()}. Keeps a reference to the original
     * native proxy provider which is used as fallback (the wrapped instance).
     *
     * @noreference This method is not intended to be referenced by clients.
     * @since 5.4
     */
    public static synchronized void installNativeWrapper() {
        if (isInstalledGlobally) {
            return;
        }
        try {
            if (ProxyManager.getProxyManager() instanceof ProxyManager manager) {
                final var wrapped = createWrappedProvider(manager);
                setFieldReflective(manager, "nativeProxyProvider", new NativeProxyProviderWrapper(wrapped));
                isInstalledGlobally = true;
            }
        } catch (InvocationTargetException e) {
            LOGGER.error("Could not install native proxy provider wrapper", e);
        }
    }

    private static PublicProxyProvider createWrappedProvider(final ProxyManager manager)
        throws InvocationTargetException {
        // (1) copy proxy manager to keep reference on native proxy provider
        final var proxyManager = invokeDefaultConstructorReflective(manager);

        // (2) modify copied manager to always have native proxies enabled
        final var prefManager = createDummyPreferenceManager();
        prefManager.putBoolean(PreferenceManager.ROOT, "proxiesEnabled", true);
        prefManager.putBoolean(PreferenceManager.ROOT, "systemProxiesEnabled", true);
        setFieldReflective(proxyManager, "preferenceManager", prefManager);

        // (3) make protected native provider provider API public via manager
        return new PublicProxyProvider() {

            @Override
            public boolean hasProxies() {
                return this.getProxyData().length > 0;
            }

            @Override
            public IProxyData[] select(final URI uri) {
                return proxyManager.hasSystemProxies() ? proxyManager.select(uri) : EMPTY_IPROXY_DATA;
            }

            @Override
            public IProxyData[] getProxyData() {
                return Objects.requireNonNullElse(proxyManager.getNativeProxyData(), EMPTY_IPROXY_DATA) ;
            }

            @Override
            public String[] getNonProxiedHosts() {
                return proxyManager.getNativeNonProxiedHosts();
            }
        };
    }

    /**
     * This is a dummy preference manager since it (1) has constant preference values
     * and (2) is only used in the context of one {@link ProxyManager} instance. It is possible
     * to use a dummy ID for the preference namespace here, since the preferences within the
     * {@link ProxyManager} are referenced via this instance, not re-fetched from the global scope.
     * <p>
     * We use the invented {@code internal.org.knime.core.net} ID here, to avoid clashes with
     * current definitions of preference namespaces. Further, it mimics {@code org.eclipse.core.net},
     * the proper, configurable preference namespace regarding (native) proxies.
     * </p>
     *
     * @return a dummy preference manager instance
     */
    private static PreferenceManager createDummyPreferenceManager() {
        // used this ID before, but it caused confusion when exported, switching V1 -> V2
        // makes the internal dummy preference namespace blend it better
        final var dummyIDv1 = "foo.bar.baz";
        try {
            for (var scope : List.of(ConfigurationScope.INSTANCE, DefaultScope.INSTANCE)) {
                // remove if the old ID has been persisted in any scope
                if (scope.getNode("").nodeExists(dummyIDv1)) {
                    scope.getNode(dummyIDv1).removeNode();
                }
            }
        } catch (BackingStoreException ignored) { // NOSONAR
        }
        final var dummyIDv2 = String.format("internal.%s.net", CorePlugin.PLUGIN_SYMBOLIC_NAME);
        return PreferenceManager.createConfigurationManager(dummyIDv2);
    }

    private static void setFieldReflective(final Object obj, final String name, final Object value)
        throws InvocationTargetException {
        final var clazz = obj.getClass();
        try {
            final var field = clazz.getDeclaredField(name);
            field.setAccessible(true); // NOSONAR
            field.set(obj, value); // NOSONAR
            LOGGER.debug(() -> String.format("Forcibly set field \"%s\" in class \"%s\" to \"%s\" via reflection", //
                name, clazz, value));
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new InvocationTargetException(e,
                "Forcibly setting field \"%s\" in class \"%s\" ".formatted(name, clazz)
                    + "failed during installation of native proxy provider wrapper");
        }
    }

    private static <T> T invokeDefaultConstructorReflective(final T obj) throws InvocationTargetException {
        @SuppressWarnings("unchecked")
        final var clazz = (Class<T>)obj.getClass();
        try {
            final var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true); // NOSONAR
            final var instance = constructor.newInstance();
            LOGGER.debug(() -> String.format("Invoked default constructor in class \"%s\" via reflection" //
                + "to forcibly create new instance", clazz));
            return instance;
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchMethodException
                | InstantiationException e) {
            throw new InvocationTargetException(e,
                "Forcibly invoking default constructor in class \"%s\" ".formatted(clazz)
                    + "failed during installation of native proxy provider wrapper");
        }
    }

    /**
     * Same methods as the {@link AbstractProxyProvider} but public instead of protected.
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    private interface PublicProxyProvider {

        boolean hasProxies();

        IProxyData[] select(URI uri);

        IProxyData[] getProxyData();

        String[] getNonProxiedHosts();
    }
}
