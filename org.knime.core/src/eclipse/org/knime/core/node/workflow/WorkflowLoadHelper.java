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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.WorkflowFileStoreHandlerRepository;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FileWorkflowPersistor.LoadVersion;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;

/**
 * Callback class that is used during loading of a workflow to read
 * user credentials and other information.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class WorkflowLoadHelper {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowLoadHelper.class);

    /** Default (pessimistic) default load helper. */
    public static final WorkflowLoadHelper INSTANCE = new WorkflowLoadHelper();

    private final boolean m_isTemplate;

    private final WorkflowContext m_workflowContext;

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
    public WorkflowLoadHelper(final File workflowLocation) {
        m_isTemplate = false;
        m_workflowContext = new WorkflowContext.Factory(workflowLocation).createContext();
    }

    /**
     * Creates a new load helper with the given workflow context.
     *
     * @param workflowContext a workflow context
     * @since 2.8
     */
    public WorkflowLoadHelper(final WorkflowContext workflowContext) {
        m_isTemplate = false;
        m_workflowContext = workflowContext;
    }


    /**
     * Creates a new load helper with the given workflow context.
     *
     * @param isTemplate whether this is a template loader
     * @param workflowContext a workflow context
     * @since 3.3
     */
    public WorkflowLoadHelper(final boolean isTemplate, final WorkflowContext workflowContext) {
        m_isTemplate = isTemplate;
        m_workflowContext = workflowContext;
    }


    /** @param isTemplate whether this is a template loader */
    public WorkflowLoadHelper(final boolean isTemplate) {
        m_isTemplate = isTemplate;
        m_workflowContext = null;
    }

    /**
     * Caller method invoked when credentials are needed during loading
     * of a workflow.
     * @param credentials to be initialized
     * @return a list of new <code>Credentials</code>
     * (this implementation the argument)
     */
    public List<Credentials> loadCredentials(
            final List<Credentials> credentials) {
        return credentials;
    }

    /** Callback if an unknown version string is encountered in the KNIME
     * workflow.
     * @param workflowVersionString The version string as in the workflow file
     *        (possibly null or otherwise meaningless).
     * @return A non-null policy (this implementation return "Abort").
     */
    public UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(
            final String workflowVersionString) {
        return UnknownKNIMEVersionLoadPolicy.Abort;
    }

    /** Returns true if the workflow is a template flow, i.e. it will be
     * disconnected from the location where it is loaded from and data will
     * not be imported.
     * @return If flow is a template flow (defaults to <code>false</code>).
     */
    public boolean isTemplateFlow() {
        return m_isTemplate;
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
    public WorkflowContext getWorkflowContext() {
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
            throw new IOException("Can't read metanode/template directory " + directory);
        }

        // template.knime or workflow.knime
        ReferencedFile dotKNIMERef = new ReferencedFile(new ReferencedFile(directory), fileName);
        File dotKNIME = dotKNIMERef.getFile();
        if (!dotKNIME.isFile()) {
            throw new IOException("No \"" + fileName + "\" file in directory \"" + directory.getAbsolutePath() + "\"");
        }

        NodeSettingsRO settings = NodeSettings.loadFromXML(new BufferedInputStream(new FileInputStream(dotKNIME)));
        // CeBIT 2006 version did not contain a version string.
        String versionString;
        if (settings.containsKey(WorkflowManager.CFG_VERSION)) {
            try {
                versionString = settings.getString(WorkflowManager.CFG_VERSION);
            } catch (InvalidSettingsException e) {
                throw new IOException("Can't read version number from \"" + dotKNIME.getAbsolutePath() + "\"", e);
            }
        } else {
            versionString = "0.9.0";
        }
        LoadVersion version = FileWorkflowPersistor.parseVersion(versionString);
        boolean needsResetAfterLoad = false;
        if (version == null) { // future version
            StringBuilder versionDetails = new StringBuilder(versionString);
            String createdBy = settings.getString(WorkflowManager.CFG_CREATED_BY, null);
            if (createdBy != null) {
                versionDetails.append(" (created by KNIME ").append(createdBy).append(")");
            }
            String v = versionDetails.toString();
            switch (getUnknownKNIMEVersionLoadPolicy(v)) {
            case Abort:
                throw new UnsupportedWorkflowVersionException(String.format("Unable to load %s, version string \"%s\" "
                        + "is unknown", isTemplateFlow() ? "template" : "workflow", v));
            default:
                version = LoadVersion.FUTURE;
                needsResetAfterLoad = true;
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
                templateInfo = MetaNodeTemplateInformation.load(settings, version);
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
        // TODO only create new hash map if workflow is a project?
        HashMap<Integer, ContainerTable> tableRep = new GlobalTableRepository();
        WorkflowFileStoreHandlerRepository fileStoreHandlerRepository = new WorkflowFileStoreHandlerRepository();
        // ordinary workflow is loaded
        if (templateInfo == null) {
            persistor = new FileWorkflowPersistor(tableRep, fileStoreHandlerRepository, dotKNIMERef,
                this, version, !isTemplateFlow());
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
                    persistor = new FileWorkflowPersistor(tableRep, fileStoreHandlerRepository, workflowDotKNIME,
                        this, version, !isTemplateFlow());
                    break;
                case SubNode:
                    final ReferencedFile settingsDotXML = new ReferencedFile(dotKNIMERef.getParent(),
                        SingleNodeContainerPersistor.SETTINGS_FILE_NAME);
                    persistor = new FileSubNodeContainerPersistor(settingsDotXML, this, version,
                        tableRep, fileStoreHandlerRepository, true);
                    break;
                default:
                    throw new IllegalStateException("Unsupported template type");
            }
        }
        if (templateInfo != null) {
            persistor.setOverwriteTemplateInformation(templateInfo.createLink(templateSourceURI));
            persistor.setNameOverwrite(directory.getName());
        }
        if (needsResetAfterLoad) {
            persistor.setDirtyAfterLoad();
        }
        return persistor;
    }

}
