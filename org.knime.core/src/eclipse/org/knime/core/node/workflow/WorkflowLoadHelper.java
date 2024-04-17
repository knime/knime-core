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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.CoreConstants;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.Version;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.core.util.pathresolve.URIToFileResolve.KNIMEURIDescription;

/**
 * Callback class that is used during loading of a workflow to read
 * user credentials and other information.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class WorkflowLoadHelper {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowLoadHelper.class);

    /** Default (pessimistic) default load helper. */
    public static final WorkflowLoadHelper INSTANCE = new WorkflowLoadHelper();

    /** Workflow version, indicates the "oldest" version that is compatible to the current workflow format. */
    static final String CFG_VERSION = "version";

    /** Version of KNIME that has written the workflow. */
    static final String CFG_CREATED_BY = "created_by";

    /** Whether the workflow was saved with a nightly build ({@link KNIMEConstants#isNightlyBuild()}.
     * @since 3.5 */
    static final String CFG_NIGHTLY = "created_by_nightly";


    private final boolean m_isTemplate;

    private final boolean m_isTemplateProject;

    private final WorkflowContextV2 m_workflowContext;

    private DataContainerSettings m_dataContainerSettings;

    /** How to proceed when a workflow written with a future KNIME version is
     * loaded. */
    public enum UnknownKNIMEVersionLoadPolicy {
        /** Abort loading. */
        Abort,
        /** Try anyway. */
        Try
    }

    /** Default instance. */
    public WorkflowLoadHelper() {
        this(false);
    }

    /**
     * Creates a new load helper for the workflow at the specified location. The location is passed into the workflow
     * context that can be retrieved via {@link #getWorkflowContext()} later.
     *
     * @param workflowLocation the location of the workflow
     * @since 2.8
     */
    @Deprecated(since = "4.7.0")
    public WorkflowLoadHelper(final File workflowLocation) {
        this(false, WorkflowContextV2.forTemporaryWorkflow(workflowLocation.toPath(), null));
    }

    @Deprecated(since = "4.7.0")
    public WorkflowLoadHelper(final File workflowDir, final DataContainerSettings settings) {
        this(workflowDir);
        this.m_dataContainerSettings = settings;
    }

    /**
     * Creates a new load helper with the given workflow context.
     *
     * @param workflowContext a workflow context
     * @since 4.7
     */
    public WorkflowLoadHelper(final WorkflowContextV2 workflowContext) {
        this(false, workflowContext);
    }

    /**
     * Creates a new load helper with the given workflow context and data container settings.
     *
     * @param context workflow context
     * @param settings data container settings
     * @since 4.7
     */
    public WorkflowLoadHelper(final WorkflowContextV2 context, final DataContainerSettings settings) {
        this(false, context);
        this.m_dataContainerSettings = settings;
    }

    /**
     * Creates a new load helper with the given workflow context.
     *
     * @param isTemplate whether this is a template loader
     * @param workflowContext a workflow context
     * @since 4.7
     */
    public WorkflowLoadHelper(final boolean isTemplate, final WorkflowContextV2 workflowContext) {
        this(isTemplate, false, workflowContext);
    }

    /**
     * Creates a new load helper with the given workflow context.
     *
     * @param isTemplate whether this is a template loader
     * @param isTemplateProject whether this template is a template project, i.e. not part of a workflow. If
     *            <code>true</code>, <code>isTemplate</code> must be <code>true</code>, too.
     * @param workflowContext a workflow context
     * @since 4.7
     * @throws IllegalStateException if <code>isTemplateProject</code> is <code>true</code>, but <code>isTemplate</code>
     *             isn't
     */
    public WorkflowLoadHelper(final boolean isTemplate, final boolean isTemplateProject,
        final WorkflowContextV2 workflowContext) {
        if (isTemplateProject && !isTemplate) {
            throw new IllegalStateException("A template project is a template, too");
        }
        m_isTemplate = isTemplate;
        m_isTemplateProject = isTemplateProject;
        m_workflowContext = workflowContext;
    }

    /** @param isTemplate whether this is a template loader */
    public WorkflowLoadHelper(final boolean isTemplate) {
        this(isTemplate, null);
    }

    /**
     * Pre-populates the {@link CoreConstants#CREDENTIALS_KNIME_SYSTEM_DEFAULT_ID system credentials} with user name and
     * password in case it was set by a 3rd party via {@link CredentialsStore#setKNIMESystemDefault(String, String)}.
     * It then calls {@link #loadCredentials(List)} and returns its result.
     * @param credentials the list of credentials to load.
     * @return result of {@link #loadCredentials(List)} after pre-filling system defaults.
     * @since 4.0
     */
    public final List<Credentials> loadCredentialsPrefilled(final List<Credentials> credentials) {
        if (CredentialsStore.systemCredentialsPassword != null || CredentialsStore.systemCredentialsUserName != null) {
            for (Credentials c : credentials) {
                if (c.getName().equals(CoreConstants.CREDENTIALS_KNIME_SYSTEM_DEFAULT_ID)) {
                    c.setLogin(CredentialsStore.systemCredentialsUserName);
                    c.setPassword(CredentialsStore.systemCredentialsPassword);
                }
            }
        }
        return loadCredentials(credentials);
    }

    /**
     * Caller method invoked when credentials are needed during loading of a workflow.
     * @param credentials to be initialized
     * @return a list of new <code>Credentials</code> (this implementation the argument)
     */
    protected List<Credentials> loadCredentials(final List<Credentials> credentials) {
        return credentials;
    }

    /**
     * Callback if an unknown version string is encountered in the KNIME workflow. This method gets only called if
     * either
     * <ol>
     * <li>the version of the workflow comes from the future (like loading a 5.x workflow in KNIME 3.5)</li>
     * <li>the workflow was created using a {@linkplain KNIMEConstants#isNightlyBuild() NIGHTLY build} and the current
     * instance is not a NIGHTLY build</li>
     * </ol>
     *
     * @param workflowKNIMEVersion The load version of the workflow(.knime) itself (includes table formats, workflow
     *            connection table etc), not null.
     * @param createdByKNIMEVersion The version that created the workflow, corresponds to
     *            {@link KNIMEConstants#VERSION}. This may be null in case the workflow.knime file is corrupt.
     * @param isNightlyBuild Whether the workflow was saved using a nightly build
     *            {@link KNIMEConstants#isNightlyBuild()}.
     * @return A non-null policy (this implementation returns "Abort").
     * @since 3.7
     */
    // see also org.knime.core.node.workflow.FileWorkflowPersistor.saveHeader(NodeSettings)
    public UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(final LoadVersion workflowKNIMEVersion,
        final Version createdByKNIMEVersion, final boolean isNightlyBuild) {
        return UnknownKNIMEVersionLoadPolicy.Abort;
    }

    /**
     * Returns true if the workflow is a template flow.
     *
     * @return If flow is a template flow (defaults to <code>false</code>).
     */
    public boolean isTemplateFlow() {
        return m_isTemplate;
    }


    /**
     * Returns <code>true</code> if the workflow is a template project, i.e. a template not embedded in a workflow. If
     * <code>false</code> (i.e. template is embedded in a workflow), it will be disconnected from the location where it
     * is loaded from and data will not be imported.
     *
     * @return if the template is a project
     * @since 4.1
     */
    public boolean isTemplateProject() {
        return m_isTemplateProject;
    }

    /** Get the name of the *.knime file. This is "template.knime" for
     * templates ({@link #isTemplateFlow()} and "workflow.knime" for workflows.
     * The loader for templates in the node repository (e.g. X-Val Loop) will
     * overwrite this.
     * @return the name of the .knime file. */
    public String getDotKNIMEFileName() {
        return isTemplateFlow() ? WorkflowPersistor.TEMPLATE_FILE : WorkflowPersistor.WORKFLOW_FILE;
    }


    /**
     * Returns a context for the workflow that is being loaded. If not context is available <code>null</code> is
     * returned.
     *
     * @return a workflow context or <code>null</code>
     * @since 2.8
     */
    public WorkflowContextV2 getWorkflowContext() {
        return m_workflowContext;
    }


    /**Create persistor for a workflow or template.
     * @noreference Clients should only be required to load projects using
     * {@link WorkflowManager#loadProject(File, ExecutionMonitor, WorkflowLoadHelper)}
     * @param directory The directory to load from
     * @return The persistor
     * @throws IOException If an IO error occured
     * @throws UnsupportedWorkflowVersionException If the workflow is of an unsupported version
     */
    FromFileNodeContainerPersistor createLoadPersistor(final File directory)
            throws IOException, UnsupportedWorkflowVersionException {
        return createTemplateLoadPersistor(directory, null);
    }

    /** Create persistor for a workflow or template.
     * @noreference Clients should only be required to load projects using
     * {@link WorkflowManager#loadProject(File, ExecutionMonitor, WorkflowLoadHelper)}
     * @param directory The directory to load from
     * @param templateSourceURI URI of the link to the template (will load a template and when the template is
     * instantiated and put into the workflow a link is created)
     * @return The persistor
     * @throws IOException If an IO error occured
     * @throws UnsupportedWorkflowVersionException If the workflow is of an unsupported version
     */
    public final TemplateNodeContainerPersistor createTemplateLoadPersistor(
        final File directory, final URI templateSourceURI)
            throws IOException, UnsupportedWorkflowVersionException {
        if (directory == null) {
            throw new NullPointerException("Arguments must not be null.");
        }
        String fileName = getDotKNIMEFileName();
        if (!directory.isDirectory() || !directory.canRead()) {
            throw new IOException("Can't read template directory " + directory);
        }

        // template.knime or workflow.knime
        ReferencedFile dotKNIMERef = new ReferencedFile(new ReferencedFile(directory), fileName);
        File dotKNIME = dotKNIMERef.getFile();
        if (!dotKNIME.isFile()) {
            throw new IOException("No \"" + fileName + "\" file in directory \"" + directory.getAbsolutePath() + "\"");
        }

        NodeSettingsRO settings;
        try (var bin = new BufferedInputStream(new FileInputStream(dotKNIME))) {
            settings = NodeSettings.loadFromXML(bin);
        }
        // CeBIT 2006 version did not contain a version string.
        String versionString;
        if (settings.containsKey(WorkflowLoadHelper.CFG_VERSION)) {
            try {
                versionString = settings.getString(WorkflowLoadHelper.CFG_VERSION);
            } catch (InvalidSettingsException e) {
                throw new IOException("Can't read version number from \"" + dotKNIME.getAbsolutePath() + "\"", e);
            }
        } else {
            versionString = "0.9.0";
        }
        LoadVersion version = FileWorkflowPersistor.parseVersion(versionString); // might also be FUTURE
        boolean isSetDirtyAfterLoad = false;
        StringBuilder versionDetails = new StringBuilder(versionString);
        String createdBy = settings.getString(WorkflowLoadHelper.CFG_CREATED_BY, null);
        Version createdByVersion = null;
        if (createdBy != null) {
            versionDetails.append(" (created by KNIME ").append(createdBy).append(")");
            try {
                createdByVersion = new Version(createdBy);
            } catch (IllegalArgumentException e) {
                // ideally this goes into the 'LoadResult' but it's not instantiated yet
                LOGGER.warn(String.format("Unable to parse version string \"%s\" (file \"%s\"): %s", createdBy,
                    dotKNIME.getAbsolutePath(), e.getMessage()), e);
            }
        }
        boolean isNightly = settings.getBoolean(CFG_NIGHTLY, false); // added 3.5.0
        boolean isRunningNightly = KNIMEConstants.isNightlyBuild();
        boolean isFutureWorkflow = createdByVersion != null &&
                !new Version(KNIMEConstants.VERSION).isSameOrNewer(createdByVersion);
        if (version == LoadVersion.FUTURE || isFutureWorkflow || (!isRunningNightly && isNightly)) {
            switch (getUnknownKNIMEVersionLoadPolicy(version, createdByVersion, isNightly)) {
            case Abort:
                StringBuilder e = new StringBuilder("Unable to load ");
                e.append(isTemplateFlow() ? "template, " : "workflow, ");
                if (version == LoadVersion.FUTURE || isFutureWorkflow) {
                    e.append("it was created with a future version of KNIME (").append(createdBy).append("). ");
                    e.append("You are running ").append(KNIMEConstants.VERSION).append(".");
                } else {
                    e.append("it was created with a nightly build of KNIME (version ").append(createdBy).append("). ");
                    e.append("You are running ").append(KNIMEConstants.VERSION).append(".");
                }
                throw new UnsupportedWorkflowVersionException(e.toString());
            default:
                isSetDirtyAfterLoad = true;
            }
        } else if (version.isOlderThan(LoadVersion.V200)) {
            LOGGER.warn("The current KNIME version (" + KNIMEConstants.VERSION + ") is different from the one that "
                    + "created the workflow (" + version + ") you are trying to load. In some rare cases, it  "
                    + "might not be possible to load all data or some nodes can't be configured. "
                    + "Please re-configure and/or re-execute these nodes.");
        }

        final MetaNodeTemplateInformation templateInfo;
        if (isTemplateFlow() && templateSourceURI != null) {
            try {
                templateInfo = MetaNodeTemplateInformation.load(settings, version, isTemplateProject());
                CheckUtils.checkSetting(Role.Template.equals(templateInfo.getRole()),
                    "Role is not '%s' but '%s'", Role.Template, templateInfo.getRole());
            } catch (InvalidSettingsException e) {
                throw new IOException(String.format(
                    "Attempting to load template from \"%s\" but can't locate template information: %s",
                    dotKNIME.getAbsolutePath(), e.getMessage()), e);
            }
        } else if (isTemplateFlow()) {
//            LOGGER.coding("Supposed to instantiate a template but the link URI is not set");
            // metanode template from node repository
            templateInfo = null;
        } else {
            templateInfo = null;
        }

        final TemplateNodeContainerPersistor persistor;
        // TODO only create new repo if workflow is a project?
        WorkflowDataRepository workflowDataRepository = new WorkflowDataRepository();
        // ordinary workflow is loaded
        if (templateInfo == null) {
            persistor = new FileWorkflowPersistor(workflowDataRepository, dotKNIMERef, this,
                version, !isTemplateFlow(), isTemplateProject());
        } else {
            // some template is loaded
            switch (templateInfo.getNodeContainerTemplateType()) {
                case MetaNode:
                    final ReferencedFile workflowDotKNIME;
                    if (version.isOlderThan(LoadVersion.V2100)) {
                        workflowDotKNIME = dotKNIMERef; // before 2.10 everything was stored in template.knime
                    } else {
                        workflowDotKNIME = new ReferencedFile(dotKNIMERef.getParent(), WorkflowPersistor.WORKFLOW_FILE);
                    }
                    persistor = new FileWorkflowPersistor(workflowDataRepository, workflowDotKNIME, this,
                        version, !isTemplateFlow(), isTemplateProject());
                    break;
                case SubNode:
                    final ReferencedFile settingsDotXML = new ReferencedFile(dotKNIMERef.getParent(),
                        SingleNodeContainerPersistor.SETTINGS_FILE_NAME);
                    persistor = new FileSubNodeContainerPersistor(settingsDotXML, this, version,
                        workflowDataRepository, true);
                    break;
                default:
                    throw new IllegalStateException("Unsupported template type");
            }
        }
        if (templateInfo != null) {
            persistor.setOverwriteTemplateInformation(templateInfo.createLink(templateSourceURI, isTemplateProject()));

            final var optName = Optional.of(templateSourceURI) //
                .filter(u -> KnimeUrlType.SCHEME.equals(u.getScheme())) //
                .flatMap(u -> ResolverUtil.toDescription(u, new NullProgressMonitor())) //
                .map(KNIMEURIDescription::getName);

            if (optName.isPresent()) {
                // we could get the template's name via the resolver
                persistor.setNameOverwrite(optName.get());
            } else {
                // try to extract the name from the URL, best-effort
                new URIBuilder(templateSourceURI).getPathSegments().stream() // decoded path segments, never `null`
                    .filter(StringUtils::isNotEmpty) // skip empty segments introduced by trailing slashes
                    .reduce((before, after) -> after) // reduce to the last segment
                    .filter(lastSegment -> !lastSegment.startsWith("*")) // reject Hub Item IDs
                    .ifPresent(persistor::setNameOverwrite);
            }
        }
        if (isSetDirtyAfterLoad) {
            persistor.setDirtyAfterLoad();
        }
        return persistor;
    }

    /**
     * Returns the system default credentials when run on KNIME Server. These credentials are derived from the logged in
     * user and are used for the Credentials nodes (Quickform, Widget, and Configuration).
     *
     * @return The system default credentials or an empty Optional if not present.
     * @since 4.3
     */
    public Optional<Credentials> getSystemDefaultCredentials() {
        return Optional.empty();
    }

    public final void setDataContainerSettings(final DataContainerSettings settings) {
        m_dataContainerSettings = settings;
    }

    public final Optional<DataContainerSettings> getDataContainerSettings() {
        return Optional.ofNullable(m_dataContainerSettings);
    }
}
