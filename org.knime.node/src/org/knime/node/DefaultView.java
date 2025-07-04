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
 *   Dec 13, 2024 (hornm): created
 */
package org.knime.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.FluentNodeAPI;
import org.knime.core.node.port.PortObject;
import org.knime.core.util.Pair;
import org.knime.core.webui.data.DataService.DataServiceBuilder;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.InitialDataService.Serializer;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.data.RpcDataService.RpcDataServiceBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.Page.RequireFromFileOrString;
import org.knime.node.DefaultModel.ExecuteOutput;
import org.knime.node.DefaultView.DefaultInitialDataService;

/**
 * Fluent API to create a node view - not to be created directly but via the {@link DefaultNode}
 *
 * The view of the node defines how the data is visualized. It requires the view settings, a location of the view page,
 * and a view description. Optionally, an initial data service or data services can be added to pass the data of the
 * node to the view.<br>
 * Unlike model settings, view settings do not alter the data, but can alter the visualization
 * (cf. @{@link DefaultModel}). Therefore, the node is not reexecuted when a view setting changes.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultView implements FluentNodeAPI {

    private final Class<? extends DefaultNodeSettings> m_settingsClass;

    final String m_description;

    final Page m_page;

    Function<ViewInput, InitialDataService<?>> m_initialDataServiceFct;

    Function<ViewInput, RpcDataService> m_rpcDataServiceFct;

    static RequireViewSettings create() {
        return settingsClass -> description -> pageFct -> new DefaultView(settingsClass, description,
            pageFct.apply(Page.create()));
    }

    private DefaultView(final Class<? extends DefaultNodeSettings> settingsClass, final String description,
        final Page page) {
        m_settingsClass = settingsClass;
        m_description = description;
        m_page = page;

    }

    Optional<Class<? extends DefaultNodeSettings>> getSettingsClass() {
        return Optional.ofNullable(m_settingsClass);
    }

    /* REQUIRED PROPERTIES */

    /**
     * The build stage that requires the view settings.
     */
    public interface RequireViewSettings {

        /**
         * @param settingsClass the view settings of the node
         * @return the subsequent build stage
         */
        RequireDescription settingsClass(Class<? extends DefaultNodeSettings> settingsClass);

        /**
         * Indicates that the model does not have view settings.
         *
         * @return the next build stage
         */
        default RequireDescription withoutSettings() {
            return settingsClass(null);
        }
    }

    /**
     * The build stage that requires the view description, which is shown next to the ports' and options' description.
     */
    public interface RequireDescription {

        /**
         * @param description a description of the provided view
         * @return the next build stage
         */
        RequirePage description(String description);
    }

    /**
     * The build stage that requires the page of the view
     */
    public interface RequirePage {

        /**
         * @param page the page at which the view is located
         * @return the {@link DefaultView}
         */
        DefaultView page(Function<RequireFromFileOrString, Page> page);

    }

    /* OPTIONAL PROPERTIES */

    /**
     * Specify the initial data passed to a view
     *
     * @param <D> the type of the initial data
     * @param initialDataSupplier a function receiving the {@link ViewInput} and the {@link RequireInitialData} and
     *            returning an initial data service
     * @return this
     */
    public <D> DefaultView
        initialData(final Function<RequireInitialData, DefaultInitialDataService<D>> initialDataSupplier) {
        m_initialDataServiceFct =
            vi -> initialDataSupplier.apply(DefaultInitialDataService.create()).toInitialDataService(vi);
        return this;
    }

    /**
     * Specify the data passed to a view
     *
     * @param dataService a function receiving the {@link ViewInput} and the {@link RequireDataService} and returning an
     *            rpc data service
     * @return this
     */
    public DefaultView dataService(final Function<RequireDataService, DefaultRpcDataService> dataService) {
        m_rpcDataServiceFct = vi -> dataService.apply(DefaultRpcDataService.create()).toRpcDataService(vi);
        return this;
    }

    static abstract class AbstractDataService<T extends DataServiceBuilder> implements DataServiceBuilder {
        protected Runnable m_dispose;

        protected Runnable m_deactivate;

        @Override
        public T onDispose(final Runnable dispose) {
            if (m_dispose != null) {
                throw new IllegalStateException("onDispose already set");
            }
            m_dispose = dispose;
            return thisAsT();
        }

        @Override
        public T onDeactivate(final Runnable deactivate) {
            if (m_deactivate != null) {
                throw new IllegalStateException("onDeactivate already set");
            }
            m_deactivate = deactivate;
            return thisAsT();
        }

        @SuppressWarnings("unchecked")
        T thisAsT() {
            return (T)this;
        }
    }

    /**
     * Constructs an RPC data service. Use {@code create()} to begin building a new instance.
     */
    public static abstract class DefaultRpcDataService extends AbstractDataService<DefaultRpcDataService>
        implements FluentNodeAPI {

        abstract RpcDataServiceBuilder toRpcDataServiceBuilder(final ViewInput vi);

        RpcDataService toRpcDataService(final ViewInput vi) {
            final var builder = toRpcDataServiceBuilder(vi);
            if (m_dispose != null) {
                builder.onDispose(m_dispose);
            }
            if (m_deactivate != null) {
                builder.onDeactivate(m_deactivate);
            }
            return builder.build();
        }

        static RequireDataService create() {
            return new RequireDataService() {
                @Override
                public SingleRpcDataService service(final Function<ViewInput, Object> service) {
                    return new SingleRpcDataService(service);
                }

                @Override
                public MultipleNamedRpcDataServices addService(final String name, final Function<ViewInput, Object> service) {
                    return new MultipleNamedRpcDataServices(name, service);
                }
            };

        }

    }

    /**
     * Construct an initial data service. Use {@code create()} to begin building a new instance.
     *
     * @param <D> the type of the initial data
     */
    public static final class DefaultInitialDataService<D> extends AbstractDataService<DefaultInitialDataService<D>>
        implements FluentNodeAPI {
        private Function<ViewInput, D> m_dataSupplier;

        private Serializer<D> m_serializer;

        DefaultInitialDataService(final Function<ViewInput, D> dataSupplier) {
            m_dataSupplier = dataSupplier;
        }

        InitialDataService toInitialDataService(final ViewInput vi) {
            final var builder = InitialDataService.builder(() -> m_dataSupplier.apply(vi));
            if (m_dispose != null) {
                builder.onDispose(m_dispose);
            }
            if (m_deactivate != null) {
                builder.onDeactivate(m_deactivate);
            }
            if (m_serializer != null) {
                builder.serializer(m_serializer);
            }
            return builder.build();
        }

        static RequireInitialData create() {
            return dataSupplier -> new DefaultInitialDataService(dataSupplier);
        }

        /**
         * @param serializer a custom serializer to turn the data object into a string
         * @return this builder
         */
        public DefaultInitialDataService<D> serializer(final Serializer<D> serializer) {
            if (m_serializer != null) {
                throw new IllegalStateException("Serializer already set.");
            }
            m_serializer = serializer;
            return this;
        }

    }

    public static final class SingleRpcDataService extends DefaultRpcDataService {

        private final Function<ViewInput, Object> m_service;

        SingleRpcDataService(final Function<ViewInput, Object> service) {
            m_service = service;
        }

        @Override
        RpcDataServiceBuilder toRpcDataServiceBuilder(final ViewInput vi) {
            return RpcDataService.builder(m_service.apply(vi));
        }

    }

    static final class MultipleNamedRpcDataServices extends DefaultRpcDataService implements AllowAddingNamedService {

        List<Pair<String, Function<ViewInput, Object>>> m_services = new ArrayList<>();

        MultipleNamedRpcDataServices(final String firstServiceName, final Function<ViewInput, Object> firstService) {
            m_services.add(new Pair<>(firstServiceName, firstService));
        }

        @Override
        public MultipleNamedRpcDataServices addService(final String name, final Function<ViewInput, Object> service) {
            m_services.add(new Pair<>(name, service));
            return this;
        }

        @Override
        RpcDataServiceBuilder toRpcDataServiceBuilder(final ViewInput vi) {
            final RpcDataServiceBuilder builder = RpcDataService.builder();
            for (final Pair<String, Function<ViewInput, Object>> service : m_services) {
                builder.addService(service.getFirst(), service.getSecond().apply(vi));
            }
            return builder;
        }

    }

    /**
     * The build stage which requires the initial data.
     */
    public static interface RequireInitialData {

        /**
         * @param supplier specify the initial data supplier
         * @return the default initial data service created from the supplier
         */
        <D> DefaultInitialDataService<D> data(Function<ViewInput, D> supplier);

    }

    /**
     * The build stage which requires the rpc data service.
     */
    public static interface RequireDataService extends AllowAddingNamedService {

        /**
         * @param service a class that specifies methods which can be called in the frontend to retrieve data
         * @return a {@link SingleRpcDataService} created from the given data service
         */
        SingleRpcDataService service(Function<ViewInput, Object> service);

    }

    /**
     * The build stage which allows adding further named data services.
     */
    public static interface AllowAddingNamedService {

        /**
         *
         * @param name the name of the data service
         * @param service a class that specifies methods which can be called in the frontend to retrieve data
         * @return a new {@link MultipleNamedRpcDataService} created from the given data service
         */
        MultipleNamedRpcDataServices addService(String name, Function<ViewInput, Object> service);

    }

    /**
     * This interface is used within {@link RequirePage#page(BiFunction)}, {@link DefaultView#initialData(BiFunction)}
     * or {@link DefaultView#dataService(BiFunction)} and provides access to the settings and internal data.
     */
    public static interface ViewInput {

        /**
         * @param <S> the type of the view settings
         * @return the view settings
         */
        <S extends DefaultNodeSettings> S getSettings();

        /**
         * Internal tables of a node can be set during execution in the {@link DefaultModel} (see
         * {@link ExecuteOutput#setInternalPortObjects(PortObject...)}
         *
         * @return the internal port objects of the node cast to {@link BufferedDataTable}s
         */
        default BufferedDataTable[] getInternalTables() {
            return Arrays.stream(getInternalPortObjects()).map(BufferedDataTable.class::cast)
                .toArray(BufferedDataTable[]::new);
        }

        /**
         * Internal port objects of a node can be set during execution in the {@link DefaultModel} (see
         * {@link ExecuteOutput#setInternalPortObjects(PortObject...)}
         *
         * @return the internal port objects of the node
         */
        PortObject[] getInternalPortObjects();

    }

}
