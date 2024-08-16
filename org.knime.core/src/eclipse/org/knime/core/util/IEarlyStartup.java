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
 *   10.12.2015 (thor): created
 */
package org.knime.core.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.EnumUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.osgi.service.datalocation.Location;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.WorkflowManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.application.ApplicationHandle;

/**
 * This interface is used by the extension point <tt>org.knime.core.EarlyStartup</tt>. The {@link #run()} method is
 * executed once before the first workflow is loaded and in the thread that triggered the workflow loading. This can
 * be any thread therefore don't make any assumptions (such as the thread being the main thread). The {@link #run()}
 * methods are executed synchronously therefore you shouldn't perform any long-running operations in them.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 3.2
 * @noreference This interface is not intended to be referenced by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
@FunctionalInterface
public interface IEarlyStartup {

    /* In general, we assume this order of events:
    * - Application starts
    * - EARLIEST stage is executed as part of org.knime.core bundle activation (loads native libraries etc)
    * - If applicable, Display object is created
    * - Instance location / Workspace is queried and set
    * - Profiles are download (if the application is profile aware, KNIME AP or executor)
    * - AFTER_PROFILES_SET stage is executed
    * - ...
    * - WorkflowManager is about to be instantiated
    * - BEFORE_WFM_CLASS_LOADED stage is executed
    * - WorkflowManager is instantiated
    * - ...
    */

    /**
     * Stages of the startup at which the {@link #run()} method is supposed to be called.
     * @since 5.3
     */
    enum StartupStage {

        /**
         * Right at the start of the Eclipse application's
         * {@link IApplication#start(org.eclipse.equinox.app.IApplicationContext)} method.
         */
        EARLIEST,
        /**
         * After {@link #EARLIEST} and after the user has chosen a workspace for the session (if applicable) and
         * after the profiles have been set (optional operation in the KNIME application and KNIME executor
         * application).
         */
        AFTER_PROFILES_SET,
        /**
         * Inside a static initializer of the {@link WorkflowManager} class, before the first workflow has been
         * loaded.
         */
        BEFORE_WFM_CLASS_LOADED;
    }

    /**
     * Executes the early startup code.
     */
    void run();

    /**
     * Helper to execute the code registered at this extension point. Must intentionally be called either before or
     * after the start of the KNIME Application (i.e. the KNIME-specific implementation of {@link IApplication}).
     *
     * @param beforeKNIMEApplicationStart if {@code true} it will only execute the extension points which wish to be
     *            called right before the start of the KNIME Application; if {code false} it will call the extension
     *            points which wish to be called later after most of the KNIME Application has been started already.
     *            NOTE: if {@code true}, the {@link IEarlyStartup}-implementation must not access the
     *            {@link KNIMEConstants}-class - because its initialization prevents the workspace-selection-prompt from
     *            being shown.
     *
     * @noreference This method is not intended to be referenced by clients.
     * @deprecated Replaced by code in the Core plugin's activator and {@link #runBeforeWFMClassLoaded()}.
     */
    @Deprecated(forRemoval = true, since = "5.3.0")
    static void executeEarlyStartup(final boolean beforeKNIMEApplicationStart) {
        // The first stage is actually handled by the `CorePlugin#start(BundleContext)` method, nothing left to do
        if (!beforeKNIMEApplicationStart) {
            runBeforeWFMClassLoaded();
        }
    }

    /**
     * To be called exactly once from a static initializer in {@link WorkflowManager}.
     *
     * @noreference This method is not intended to be referenced by clients.
     * @since 5.3
     */
    static void runBeforeWFMClassLoaded() {
        EarlyStartupState.runBeforeWFMClassLoaded();
    }

    /**
     * To be called exactly once the org.knime.product.profiles.ProfileManager located in the KNIME Product.
     *
     * @noreference This method is not intended to be referenced by clients.
     * @since 5.3
     */
    static void runAfterProfilesLoaded() {
        EarlyStartupState.runAfterProfilesLoaded();
    }

    /**
    * State machine managing the stages of the {@link IEarlyStartup} process.
    *
    * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
    * @noreference This class is not intended to be referenced by clients.
    * @since 5.3
    */
    final class EarlyStartupState {

        private static final NodeLogger LOGGER = NodeLogger.getLogger(IEarlyStartup.class); // NOSONAR

        /**
         * Applications which will delay the run of the AFTER_PROFILES_SET phase since they will first download and
         * apply profiles and then run this phase (calling EarlyStartupState#initialize(BundleContext) manually).
         */
        static final String[] PROFILE_AWARE_APPLICATIONS = { //
            "org.knime.product.KNIME_APPLICATION", //
            "com.knime.enterprise.slave.KNIME_REMOTE_APPLICATION", //
            "org.knime.testing.NGTestflowRunner"
        };

        private static final IExtensionPoint EXTENSION_POINT;
        static {
            final var extPointID = "org.knime.core.EarlyStartup";
            EXTENSION_POINT = CheckUtils.checkNotNull(Platform.getExtensionRegistry().getExtensionPoint(extPointID),
                "Invalid extension point ID: " + extPointID);
        }

        private static final ServiceListener WORKSPACE_LOC_MODIFIED_LISTENER = event -> {
            if (event.getType() == ServiceEvent.MODIFIED) {
                moveToStage(StartupStage.AFTER_PROFILES_SET);
            }
        };

        private static StartupStage previousStage;

        private static boolean initializing;

        private static boolean wfmStaticInitializerStageRequested;

        private EarlyStartupState() {
        }

        /**
         * Runs the EARLIEST stage of the startup process. This method is called by the CorePlugin#start method. In case
         * the current application is not a KNIME application, the AFTER_PROFILES_SET stage is run as soon as the
         * instance location (workspace) is set.
         *
         * @param context bundle context of the core plug-in, used to lookup services and determine the application id
         * @noreference This method is not intended to be referenced by clients.
         */
        public static synchronized void initialize(final BundleContext context) {
            final Optional<String> applicationId = getApplicationId(context);
            if (previousStage == null) {
                LOGGER.debugWithFormat("Running application \"%s\"", applicationId.orElse("<unknown>"));
            } else {
                LOGGER.debug("EarlyStartup state already initialized", new IllegalStateException());
                return;
            }

            try {
                // block the last stage from being triggered by the earlier ones
                initializing = true;

                moveToStage(StartupStage.EARLIEST);

                if (wfmStaticInitializerStageRequested) {
                    // `WorkflowManager` has been instantiated at stage 1, execute stages 2 and 3 immediately
                    moveToStage(StartupStage.BEFORE_WFM_CLASS_LOADED);
                    return;
                }

                // this is not a KNIME application, run the AFTER_PROFILES_SET stage as soon as the workspace is set
                if (!Arrays.asList(PROFILE_AWARE_APPLICATIONS).contains(applicationId.orElse(null))) {
                    final var instanceLocation = Platform.getInstanceLocation();
                    if (instanceLocation == null || !instanceLocation.isSet()) {
                        try { // NOSONAR (nesting)
                            // wait for workspace location to be set
                            context.addServiceListener(WORKSPACE_LOC_MODIFIED_LISTENER, Location.INSTANCE_FILTER);
                            return;
                        } catch (final InvalidSyntaxException ex) {
                            LOGGER.coding("Unable to register service listener for the instance location", ex);
                        }
                    } else {
                        // instance location is already set (via cmd line `-data`), execute now (e.g. in unit tests)
                        moveToStage(StartupStage.AFTER_PROFILES_SET);
                    }
                }

                if (wfmStaticInitializerStageRequested) {
                    // `WorkflowManager` has been instantiated at stage 2, execute stage 3 immediately
                    moveToStage(StartupStage.BEFORE_WFM_CLASS_LOADED);
                }

            } finally {
                initializing = false;
            }
        }

        /**
         * Determine the IApplication id that is currently running, e.g. {@code org.knime.product.KNIME_APPLICATION} or
         * {@code org.knime.product.KNIME_BATCH_APPLICATION}.
         * @return The application id or an empty optional if it's not possible to determine it (no such service)
         */
        private static Optional<String> getApplicationId(final BundleContext context) {
            final ServiceReference<ApplicationHandle> appRef = context.getServiceReference(ApplicationHandle.class);
            final ApplicationHandle appHandle;
            if (appRef != null) {
                appHandle = context.getService(appRef);
                if (appHandle != null) {
                    try {
                        final String appId = appHandle.getApplicationDescriptor().getApplicationId();
                        return Optional.of(appId);
                    } finally {
                        context.ungetService(appRef);
                    }
                }
            }
            return Optional.empty();
        }

        private static synchronized void runBeforeWFMClassLoaded() {
            if (initializing) {
                wfmStaticInitializerStageRequested = true;
            } else {
                moveToStage(StartupStage.BEFORE_WFM_CLASS_LOADED);
            }
        }

        private static synchronized void runAfterProfilesLoaded() {
            // this method is only (to be) called from the KNIME(Executor)Application code, so it is safe to assume
            // that this invocation is not indirectly called from some IEarlyStartup contribution
            CheckUtils.checkState(!initializing, "Can not run AFTER_PROFILES_SET stage while initializing");
            moveToStage(StartupStage.AFTER_PROFILES_SET);
        }

        /**
         * Moves this state machine to the specified stage of the startup process.
         *
         * @param targetStage stage to which to move the state machine
         */
        private static synchronized void moveToStage(final StartupStage targetStage) {
            final int start;
            if (previousStage == null) {
                start = 0;
            } else {
                if (previousStage.compareTo(targetStage) >= 0) {
                    LOGGER.coding(() -> "Can only move to later stage: %s (current) >= %s (new)" //
                        .formatted(previousStage, targetStage), new IllegalStateException());
                    return;
                }
                start = previousStage.ordinal() + 1;
            }
            LOGGER.debugWithFormat("Moving from stage %s to stage %s",
                Objects.requireNonNullElse(previousStage, "<none>"), targetStage);

            final var stages = StartupStage.values();
            if (start < targetStage.ordinal()) {
                // could happen in unit tests where the earliest phase is not triggered by the core and product plugin
                LOGGER.debug(() -> "Stages have been requested out of order: Expected %s, found %s" //
                    .formatted(stages[start], targetStage), new IllegalStateException());
            }

            // make sure we execute all stages in order
            for (var i = start; i <= targetStage.ordinal(); i++) {
                final var stage = stages[i];
                try {
                    runHooksAtStage(stage);
                } finally {
                    // always mark the stage as done even if errors occur, otherwise it is repeated later
                    previousStage = stage;
                }
            }
        }

        private static void runHooksAtStage(final StartupStage stage) {
            if (stage == StartupStage.AFTER_PROFILES_SET) {
                // unregister the listener, either the workspace is set or the default has been chosen
                final var bundleContext = FrameworkUtil.getBundle(IEarlyStartup.class).getBundleContext();
                bundleContext.removeServiceListener(WORKSPACE_LOC_MODIFIED_LISTENER);
            }

            final var registeredProvidersIter = Stream.of(EXTENSION_POINT.getExtensions()) //
                .flatMap(ext -> Stream.of(ext.getConfigurationElements())) //
                .filter(provider -> isRequestedStage(provider, stage)) //
                .iterator();

            while (registeredProvidersIter.hasNext()) {
                final IConfigurationElement provider = registeredProvidersIter.next();
                try {
                    LOGGER.debug(() -> "Executing startup hook `%s#run()` at stage %s" //
                        .formatted(provider.getAttribute("class"), stage)); // NOSONAR
                    ((IEarlyStartup)provider.createExecutableExtension("class")).run();
                } catch (CoreException e) {
                    final var message = "Could not create early startup object of class '%s' from plug-in '%s': %s" //
                        .formatted(provider.getAttribute("class"), provider.getContributor().getName(), e.getMessage());
                    LOGGER.error(message, e);
                } catch (Exception | ExceptionInInitializerError e) { // NOSONAR
                    final var message = "Early startup in '%s' from plug-in '%s' has thrown an uncaught exception: %s"//
                        .formatted(provider.getAttribute("class"), provider.getContributor().getName(), e.getMessage());
                    LOGGER.error(message, e);
                }
            }
        }

        private static boolean isRequestedStage(final IConfigurationElement provider, final StartupStage stage) {
            final var stageAttribute = provider.getAttribute("startupStage");
            final StartupStage requestedStage;
            if (stageAttribute == null) {
                // fallback to the old two-stage model, missing attribute is interpreted as `false`
                final var earliestStageFlag = provider.getAttribute("callBeforeKNIMEApplicationStart");
                requestedStage = Boolean.parseBoolean(earliestStageFlag) ? StartupStage.EARLIEST
                    : StartupStage.BEFORE_WFM_CLASS_LOADED;
            } else {
                requestedStage = EnumUtils.getEnum(StartupStage.class, stageAttribute);
                if (requestedStage == null && stage == StartupStage.EARLIEST) {
                    // only log the error once in the `EARLIEST` stage
                    LOGGER.error(() -> "Unknown startup stage '%s' specified in contribution '%s' from plug-in '%s'" //
                        .formatted(stageAttribute, provider.getAttribute("class"),
                            provider.getContributor().getName()));
                }
            }
            return requestedStage == stage;
        }

    }
}
