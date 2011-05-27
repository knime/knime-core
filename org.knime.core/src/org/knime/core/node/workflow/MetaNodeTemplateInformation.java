/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;


/**
 * Additional information that is associated with a meta node that are used
 * as templates. This includes their source URI and versioning information.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class MetaNodeTemplateInformation implements Cloneable {

    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** The template's role. */
    public enum Role {
        /** No role, i.e. ordinary workflow or meta node (no reference date). */
        None,
        /** The template itself (no uri but reference date). */
        Template,
        /** A link to a template (uri and reference date). */
        Link
    }

    /**
     *
     */
    public enum UpdateStatus {
        /** Up to date. */
        UpToDate,
        /** New Update available. */
        HasUpdate,
        /** Error checking for update. */
        Error
    }

    /** Default info object (no template). */
    public static final MetaNodeTemplateInformation NONE =
        new MetaNodeTemplateInformation();

    private final Role m_role;
    private final Date m_timestamp;
    private final URI m_sourceURI;

    /** see {@link #getUpdateStatus()}. */
    private UpdateStatus m_updateStatus = UpdateStatus.UpToDate;

    /** Create new meta node template (role {@link Role#None}). */
    private MetaNodeTemplateInformation() {
        this(Role.None, null, null);
    }

    /** Create new template with all infos.
     * @param role The role.
     */
    private MetaNodeTemplateInformation(final Role role,
            final URI uri, final Date timestamp) {
        if (role == null) {
            throw new NullPointerException("Role must not be null");
        }
        m_role = role;
        switch (role) {
        case None:
            m_sourceURI = null;
            m_timestamp = null;
            break;
        case Template:
            m_sourceURI = null;
            if (timestamp == null) {
                throw new NullPointerException(
                        "Template timestamp must not be null");
            }
            m_timestamp = timestamp;
            break;
        case Link:
            if (uri == null) {
                throw new NullPointerException("Link URI must not be null");
            }
            m_sourceURI = uri;
            if (timestamp == null) {
                throw new NullPointerException(
                    "Meta node link timestamp must not be null");
            }
            m_timestamp = timestamp;
            break;
        default:
            throw new IllegalStateException("Unsupported role " + role);
        }
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

    /** Transient field that is set by the update checker. It will be valid
     * if this template info represents a valid link to a template.
     * @return status of a potential update to the latest template. */
    public UpdateStatus getUpdateStatus() {
        return m_updateStatus;
    }

    /** Set update status field, only to be called via {@link WorkflowManager#
     * checkUpdateMetaNodeLink(NodeID, WorkflowLoadHelper)}.
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
     * @throws InvalidSettingsException If this object is not a template. */
    public MetaNodeTemplateInformation createLink(final URI sourceURI)
        throws InvalidSettingsException {
        if (sourceURI == null) {
            throw new NullPointerException("Can't create link to null URI");
        }
        switch (getRole()) {
        case Template:
            Date ts = getTimestamp();
            assert ts != null : "Templates must not have null timestamp";
            return new MetaNodeTemplateInformation(Role.Link, sourceURI, ts);
        default:
            throw new InvalidSettingsException("Can't link to meta node of role"
                    + " \"" + getRole() + "\" (URI: \"" + sourceURI + "\")");
        }
    }

    /** Saves this object to the argument settings.
     * @param settings To save to.
     */
    public void save(final NodeSettingsWO settings) {
        settings.addString("role", m_role.name());
        if (!Role.None.equals(m_role)) {
            String dateS = getTimeStampString();
            settings.addString("timestamp", dateS);
            String uriS = m_sourceURI == null ? null : m_sourceURI.toString();
            settings.addString("sourceURI", uriS);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected MetaNodeTemplateInformation clone() {
        try {
            return (MetaNodeTemplateInformation)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unable to clone object although class "
                    + "implements Cloneable", e);
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
    };

    /** @return A new template info representing the template itself. The time
     * stamp is set to the current time. */
    public static MetaNodeTemplateInformation createNewTemplate() {
        return new MetaNodeTemplateInformation(Role.Template, null, new Date());
    }

    /** Load information from argument, throw {@link InvalidSettingsException}
     * if that fails.
     * @param settings To load from.
     * @param version The version this workflow is loading from
     * @return a new template loading from the argument settings.
     * @throws InvalidSettingsException If that fails.
     */
    public static MetaNodeTemplateInformation load(
            final NodeSettingsRO settings, final LoadVersion version)
        throws InvalidSettingsException {
        String roleS = settings.getString("role");
        Role role;
        Date timestamp;
        URI sourceURI;
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
            timestamp = readTimestamp(settings);
            break;
        case Link:
            sourceURI = readURI(settings);
            timestamp = readTimestamp(settings);
            break;
        default:
            throw new InvalidSettingsException("Unsupported role: " + role);
        }
        return new MetaNodeTemplateInformation(role, sourceURI, timestamp);
    }

    /** @param settings
    /** @return
    /** @throws InvalidSettingsException */
    private static Date readTimestamp(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String dateS = settings.getString("timestamp");
        if (dateS == null || dateS.length() == 0) {
            throw new InvalidSettingsException(
                    "Cannot not read reference date from emtpy string");
        }
        Date date;
        try {
            synchronized (DATE_FORMAT) {
                date = DATE_FORMAT.parse(dateS);
            }
        } catch (ParseException pe) {
            throw new InvalidSettingsException(
                    "Cannot parse reference date \"" + dateS + "\": "
                    + pe.getMessage(), pe);
        }
        return date;
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
}
