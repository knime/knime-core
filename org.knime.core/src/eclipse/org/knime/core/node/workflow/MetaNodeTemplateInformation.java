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

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.LoadVersion;

/**
 * Additional information that is associated with a metanode that are used
 * as templates. This includes their source URI and versioning information.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class MetaNodeTemplateInformation implements Cloneable {

    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** The template's role. */
    public enum Role {
        /** No role, i.e. ordinary workflow or metanode (no reference date). */
        None,
        /** The template itself (no uri but reference date). */
        Template,
        /** A link to a template (uri and reference date). */
        Link
    }

    /** Class instance saved as part of the settings to properly read them back later. */
    public enum TemplateType {
        /** Instance of {@link WorkflowManager}. */
        MetaNode,
        /** Instance of {@link SubNodeContainer}. */
        SubNode;

        /** Map class to template type, fail with runtime exception if not supported.
         * @param cl Non null class (WFM or sub node).
         * @return The non-null template type.
         * @throws IllegalArgumentException If unsupported. */
        static TemplateType get(final Class<? extends NodeContainerTemplate> cl) {
            if (WorkflowManager.class.isAssignableFrom(cl)) {
                return MetaNode;
            } else if (SubNodeContainer.class.isAssignableFrom(cl)) {
                return SubNode;
            } else {
                throw new IllegalArgumentException(String.format("Instances of %s cannot be saved as template", cl));
            }
        }
    }

    /** */
    public enum UpdateStatus {
        /** Up to date. */
        UpToDate,
        /** New Update available. */
        HasUpdate,
        /** Error checking for update. */
        Error
    }

    /** Default info object (no template). */
    public static final MetaNodeTemplateInformation NONE = new MetaNodeTemplateInformation();

    private final Role m_role;
    private final TemplateType m_type;
    private final Date m_timestamp;
    private final URI m_sourceURI;

    /** see {@link #getUpdateStatus()}. */
    private UpdateStatus m_updateStatus = UpdateStatus.UpToDate;

    private NodeSettingsRO m_exampleInputDataInfo;

    private List<FlowVariable> m_incomingFlowVariables;

    /** Create new metanode template (role {@link Role#None}). */
    private MetaNodeTemplateInformation() {
        this(Role.None, null, null, null, null, null);
    }

    /** Create new template with all infos.
     * @param role The role.
     * @param type the type (non-null for templates, null for links)
     */
    private MetaNodeTemplateInformation(final Role role, final TemplateType type, final URI uri, final Date timestamp,
        final NodeSettingsRO exampleInputDataInfo, final List<FlowVariable> incomingFlowVariables) {
        if (role == null) {
            throw new NullPointerException("Role must not be null");
        }
        m_role = role;
        switch (role) {
        case None:
            m_sourceURI = null;
            m_timestamp = null;
            m_type = null;
            break;
        case Template:
            CheckUtils.checkNotNull(timestamp, "Template timestamp must not be null");
            CheckUtils.checkNotNull(type, "Type must not be null");
            m_sourceURI = null;
            m_type = type;
            m_timestamp = timestamp;
            break;
        case Link:
            CheckUtils.checkNotNull(uri, "Link URI must not be null");
            CheckUtils.checkNotNull(timestamp, "Metanode link timestamp must not be null");
            m_sourceURI = uri;
            m_type = null;
            m_timestamp = timestamp;
            break;
        default:
            throw new IllegalStateException("Unsupported role " + role);
        }
        m_exampleInputDataInfo = exampleInputDataInfo;
        m_incomingFlowVariables = incomingFlowVariables;
    }

    /** @return the timestamp date or null if this is not a link. */
    public Date getTimestamp() {
        return m_timestamp;
    }

    /** The timestamp formatted as string or null if this is not a link.
     * @return This string or null. */
    public String getTimeStampString() {
        if (m_timestamp == null) {
            return null;
        }
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(m_timestamp);
        }
    }

    /** @return the sourceURI */
    public URI getSourceURI() {
        return m_sourceURI;
    }
    /** @return the role */
    public Role getRole() {
        return m_role;
    }

    /** @return the type (non-null only for templates). */
    public TemplateType getNodeContainerTemplateType() {
        return m_type;
    }

    /** Transient field that is set by the update checker. It will be valid
     * if this template info represents a valid link to a template.
     * @return status of a potential update to the latest template. */
    public UpdateStatus getUpdateStatus() {
        return m_updateStatus;
    }

    /** Set update status field, only to be called via
     * {@link WorkflowManager#checkUpdateMetaNodeLink(NodeID, WorkflowLoadHelper)}.
     * @param updateStatus The field
     * @return Whether the field has changed (caller needs notifying listeners).
     */
    boolean setUpdateStatusInternal(final UpdateStatus updateStatus) {
        if (m_updateStatus != updateStatus) {
            m_updateStatus = updateStatus;
            return true;
        }
        return false;
    }

    /** Create a new link template info based on this template, which is
     * supposed to be accessible under the argument URI.
     * @param sourceURI The sourceURI, must not be null.
     * @return a new template linking to the argument URI, using the timestamp
     *         of this object.
     * @throws IllegalStateException If this object is not a template. */
    public MetaNodeTemplateInformation createLink(final URI sourceURI) {
        if (sourceURI == null) {
            throw new NullPointerException("Can't create link to null URI");
        }
        switch (getRole()) {
        case Template:
            Date ts = getTimestamp();
            assert ts != null : "Templates must not have null timestamp";
            return new MetaNodeTemplateInformation(Role.Link, null, sourceURI, ts, m_exampleInputDataInfo,
                 m_incomingFlowVariables);
            default:
            throw new IllegalStateException("Can't link to metanode of role"
                    + " \"" + getRole() + "\" (URI: \"" + sourceURI + "\")");
        }
    }

    /**
     * Create a new link template info based on this template (which must be a link already), which is supposed to be
     * accessible under the argument URI.
     *
     * @param newSource The sourceURI, must not be null.
     * @return a new template linking to the argument URI, using the timestamp of this object.
     * @throws InvalidSettingsException If this object is not a template.
     * @since 2.8
     */
    public MetaNodeTemplateInformation createLinkWithUpdatedSource(final URI newSource)
        throws InvalidSettingsException {
        if (newSource == null) {
            throw new NullPointerException("Can't create link to null URI");
        }
        switch (getRole()) {
        case Link:
            Date ts = getTimestamp();
            assert ts != null : "Templates must not have null timestamp";
            MetaNodeTemplateInformation newInfo
                = new MetaNodeTemplateInformation(Role.Link, null, newSource, ts, null, null);
            newInfo.m_updateStatus = m_updateStatus;
            return newInfo;
        default:
            throw new InvalidSettingsException("Can't link to metanode of role"
                    + " \"" + getRole() + "\" (URI: \"" + m_sourceURI + "\")");
        }
    }

    /**
     * @return example input data stored with a component that is not embedded in a workflow or an empty optional, if
     *         non available
     *
     * @since 4.1
     */
    public Optional<NodeSettingsRO> getExampleInputDataInfo() {
        return Optional.ofNullable(m_exampleInputDataInfo);
    }

    /**
     * @return incoming flow variables stored with a component that is not embedded in a workflow. Never
     *         <code>null</code>, possibly empty
     * @since 4.1
     */
    public List<FlowVariable> getIncomingFlowVariables() {
        return m_incomingFlowVariables;
    }

    /** Key for workflow template information. */
    private static final String CFG_TEMPLATE_INFO = "workflow_template_information";

    /** Saves this object to the argument settings.
     * @param settings To save to.
     */
    public void save(final NodeSettingsWO settings) {
        save(settings, false);
    }

    /**
     * Saves this object to the argument settings, optionally including the information about example input data. The
     * later is only required if the respective node is a "shared" template, i.e. stored outside of a workflow. In that
     * case, the information is stored into the {@link WorkflowPersistor#TEMPLATE_FILE} file.
     *
     * @param settings to save to
     *
     * @since 4.1
     */
    private void save(final NodeSettingsWO settings, final boolean withExampleInputDataInfo) {
        if (!Role.None.equals(m_role)) {
            NodeSettingsWO nestedSettings = settings.addNodeSettings(CFG_TEMPLATE_INFO);
            nestedSettings.addString("role", m_role.name());
            String dateS = getTimeStampString();
            nestedSettings.addString("timestamp", dateS);
            String uriS = m_sourceURI == null ? null : m_sourceURI.toString();
            nestedSettings.addString("sourceURI", uriS);
            if (Role.Template.equals(m_role)) {
                nestedSettings.addString("templateType", m_type.name());
            }
            if (withExampleInputDataInfo) {
                if (m_exampleInputDataInfo != null) {
                    NodeSettingsWO exampleInputDataInfo = nestedSettings.addNodeSettings("exampleInputDataInfo");
                    m_exampleInputDataInfo.copyTo(exampleInputDataInfo);
                }
                if (m_incomingFlowVariables != null && !m_incomingFlowVariables.isEmpty()) {
                    NodeSettingsWO incomingFlowVarSettings = nestedSettings.addNodeSettings("incomingFlowVariables");
                    int i = 0;
                    for (FlowVariable fv : m_incomingFlowVariables) {
                        if (!fv.isGlobalConstant()) {
                            fv.save(incomingFlowVarSettings.addNodeSettings("Var_" + (i++)));
                        }
                    }
                }
            }
        }
    }



    /** {@inheritDoc} */
    @Override
    protected MetaNodeTemplateInformation clone() {
        try {
            return (MetaNodeTemplateInformation)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unable to clone object although class implements Cloneable", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(m_role.toString());
        switch (m_role) {
        case None:
            break;
        case Link:
            b.append(" to \"").append(m_sourceURI.toString());
            b.append("\", version: ").append(getTimeStampString());
            b.append("\", update: ").append(m_updateStatus);
            break;
        case Template:
            b.append(" version ").append(getTimeStampString());
            break;
        default:
        }
        return b.toString();
    }

    /** Checks if this template or link has a newer timestamp than the argument.
     * @param other Other to check.
     * @return True if newer than argument
     * @throws IllegalStateException If this and/or other is not a link or template.
     */
    boolean isNewerThan(final MetaNodeTemplateInformation other) {
        if (m_timestamp == null) {
            throw new IllegalStateException("Not a template or link: " + this);
        }
        if (other.m_timestamp == null) {
            throw new IllegalStateException("Argument not a template or link: " + this);
        }
        return m_timestamp.after(other.m_timestamp);
    }

    /** @param cl The non-null class (used to derive {@link TemplateType}).
     * @return A new template info representing the template itself. The time
     * stamp is set to the current time. */
    public static MetaNodeTemplateInformation createNewTemplate(
        final Class<? extends NodeContainerTemplate> cl) {
        TemplateType type = TemplateType.get(cl);
        return new MetaNodeTemplateInformation(Role.Template, type, null, new Date(), null, null);
    }

    /**
     * Creates the template information object for a component that also includes example input data.
     *
     * @param exampleInputDataInfo infos on the example input data, such as location, file names, port types
     * @param incomingFlowVariables incoming flow variables to be stored with the template
     * @return A new template info representing the template itself. The time stamp is set to the current time.
     * @since 4.1
     */
    public static MetaNodeTemplateInformation createNewTemplate(final NodeSettingsRO exampleInputDataInfo,
        final List<FlowVariable> incomingFlowVariables) {
        return new MetaNodeTemplateInformation(Role.Template, TemplateType.SubNode, null, new Date(),
            exampleInputDataInfo, incomingFlowVariables);
    }


    /** Load information from argument, throw {@link InvalidSettingsException}
     * if that fails.
     * @param settings To load from.
     * @param version The version this workflow is loading from
     * @return a new template loading from the argument settings.
     * @throws InvalidSettingsException If that fails.
     * @since 3.7
     */
    public static MetaNodeTemplateInformation load(final NodeSettingsRO settings, final LoadVersion version)
        throws InvalidSettingsException {
        return load(settings, version, false);
    }

    /**
     * Load information from argument, throw {@link InvalidSettingsException} if that fails.
     *
     * @param settings To load from.
     * @param version The version this workflow is loading from
     * @param withExampleInputDataInfo if the info about example input data should be loaded too (only makes sense if
     *            template is directly opened in the workflow editor as a project and not embedded in a workflow)
     * @return a new template loading from the argument settings.
     * @throws InvalidSettingsException If that fails.
     */
    static MetaNodeTemplateInformation load(final NodeSettingsRO settings, final LoadVersion version,
        final boolean withExampleInputDataInfo) throws InvalidSettingsException {
        if (!settings.containsKey(CFG_TEMPLATE_INFO)) {
            return NONE;
        }
        NodeSettingsRO nestedSettings = settings.getNodeSettings(CFG_TEMPLATE_INFO);
        String roleS = nestedSettings.getString("role");
        Role role;
        Date timestamp;
        URI sourceURI;
        TemplateType templateType;
        try {
            role = Role.valueOf(roleS);
        } catch (Exception e) {
            throw new InvalidSettingsException("Cannot parse template role \""
                    + roleS + "\": " + e.getMessage(), e);
        }

        switch (role) {
        case None:
            return NONE;
        case Template:
            sourceURI = null;
            timestamp = readTimestamp(nestedSettings);
            templateType = readTemplateType(nestedSettings, version);
            break;
        case Link:
            sourceURI = readURI(nestedSettings);
            templateType = null;
            timestamp = readTimestamp(nestedSettings);
            break;
        default:
            throw new InvalidSettingsException("Unsupported role: " + role);
        }


        NodeSettingsRO exampleInputDataInfo = null;
        List<FlowVariable> incomingFlowVariables = Collections.emptyList();
        if (withExampleInputDataInfo) {
            // added in 4.1, load example input data meta info
            if (nestedSettings.containsKey("exampleInputDataInfo")) {
                exampleInputDataInfo = nestedSettings.getNodeSettings("exampleInputDataInfo");
            }
            if (nestedSettings.containsKey("incomingFlowVariables")) {
                NodeSettingsRO flowVarSettings = nestedSettings.getNodeSettings("incomingFlowVariables");
                incomingFlowVariables = new ArrayList<FlowVariable>();
                for (String key : flowVarSettings.keySet()) {
                    incomingFlowVariables.add(FlowVariable.load(flowVarSettings.getNodeSettings(key)));
                }
            }
        }

        return new MetaNodeTemplateInformation(role, templateType, sourceURI, timestamp, exampleInputDataInfo,
            incomingFlowVariables);
    }

    /** @param settings
    /** @return
    /** @throws InvalidSettingsException */
    private static Date readTimestamp(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String dateS = settings.getString("timestamp");
        if (dateS == null || dateS.length() == 0) {
            throw new InvalidSettingsException("Cannot not read reference date from emtpy string");
        }
        Date date;
        try {
            synchronized (DATE_FORMAT) {
                date = DATE_FORMAT.parse(dateS);
            }
        } catch (ParseException pe) {
            throw new InvalidSettingsException("Cannot parse reference date \"" + dateS + "\": " + pe.getMessage(), pe);
        }
        return date;
    }

    /** Read type, only called for templates.
     * @param settings ...
     * @param loadVersion ...
     * @return non-null template type.
     * @throws InvalidSettingsException ...
     */
    private static TemplateType readTemplateType(final NodeSettingsRO settings,
        final LoadVersion loadVersion) throws InvalidSettingsException {
        if (loadVersion.isOlderThan(LoadVersion.V2100)) { // no subnode prio 2.10
            return TemplateType.MetaNode;
        }
        String s = settings.getString("templateType");
        CheckUtils.checkSetting(s != null, "Template type must not be null");
        try {
            return TemplateType.valueOf(s);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException("Invalid template type \"" + s + "\"", iae);
        }
    }

    /** @param settings
    /** @throws InvalidSettingsException */
    private static URI readURI(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String uriS = settings.getString("sourceURI");
        if (uriS == null || uriS.length() == 0) {
            throw new InvalidSettingsException(
                "Cannot not read source URI from emtpy string");
        }
        try {
            return new URI(uriS);
        } catch (Exception e) {
            throw new InvalidSettingsException("Cannot parse template "
                    + "URI \"" + uriS + "\": " + e.getMessage(), e);
        }
    }

    static NodeSettings createNodeSettingsForTemplate(final NodeContainerTemplate template) {
        NodeSettings settings = new NodeSettings(Role.Template.toString());
        FileWorkflowPersistor.saveHeader(settings);
        template.getTemplateInformation().save(settings, true);
        FileWorkflowPersistor.saveWorkflowCipher(settings, template.getWorkflowCipher());
        return settings;
    }

//    static final class TemplateLoadHelper {
//
//        private final LoadVersion m_loadVersion;
//        private final String m_name;
//        private final URI m_templateURI;
//        private final WorkflowCipher m_cipher;
//        private final MetaNodeTemplateInformation.Role m_role;
//
//
//    }

}
