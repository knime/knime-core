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
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.io.extractcontextprop;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowManager.AuthorInformation;

/**
 *
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 */
final class ReadContextPropertyConfiguration {

    /** Context variable name for workflow name. */
    public static final String CONTEXT_WORKFLOW_NAME = "context.workflow.name";
    /** Context variable name for mount-point-relative workflow path. */
    public static final String CONTEXT_WORKFLOW_PATH = "context.workflow.path";
    /** Context variable name for absolute workflow path. */
    public static final String CONTEXT_WORKFLOW_ABSOLUTE_PATH = "context.workflow.absolute-path";
    /** Context variable name for workflow user. */
    public static final String CONTEXT_SERVER_USER = "context.workflow.user";
    /** Context variable name for workflow temporary location. */
    public static final String CONTEXT_TEMP_LOCATION = "context.workflow.temp.location";
    /** Context variable name for workflow author's name. */
    public static final String CONTEXT_AUTHOR = "context.workflow.author.name";
    /** Context variable name for workflow last editor's name. */
    public static final String CONTEXT_EDITOR = "context.workflow.last.editor.name";
    /** Context variable name for the creation date of the workflow. */
    public static final String CONTEXT_CREATION_DATE = "context.workflow.creation.date";
    /** Context variable name for last modified time of workflow. */
    public static final String CONTEXT_LAST_MODIFIED = "context.workflow.last.time.modified";

    private static List<String> contextProperties = new ArrayList<String>();
    static {
        contextProperties.add(CONTEXT_WORKFLOW_NAME);
        contextProperties.add(CONTEXT_WORKFLOW_PATH);
        contextProperties.add(CONTEXT_WORKFLOW_ABSOLUTE_PATH);
        contextProperties.add(CONTEXT_SERVER_USER);
        contextProperties.add(CONTEXT_TEMP_LOCATION);
        contextProperties.add(CONTEXT_AUTHOR);
        contextProperties.add(CONTEXT_EDITOR);
        contextProperties.add(CONTEXT_CREATION_DATE);
        contextProperties.add(CONTEXT_LAST_MODIFIED);
    }

    private boolean m_isExtractAllProps;
    private String[] m_selectedProps;
    /** @return the isExtractAllProps */


    boolean isExtractAllProps() {
        return m_isExtractAllProps;
    }
    /** @param isExtractAllProps the isExtractAllProps to set */
    void setExtractAllProps(final boolean isExtractAllProps) {
        m_isExtractAllProps = isExtractAllProps;
    }
    /** @return the selectedProps */
    String[] getSelectedProps() {
        return m_selectedProps;
    }
    /** @param selectedProps the selectedProps to set */
    void setSelectedProps(final String[] selectedProps) {
        m_selectedProps = selectedProps;
    }

    /** Saves current configuration to argument.
     * @param settings to save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean("isExtractAllProps", m_isExtractAllProps);
        if (!m_isExtractAllProps) {
            settings.addStringArray("selectedProps", m_selectedProps);
        }
    }

    /** Loads configuration, fails if fields are missing/invalid.
     * @param settings to load from.
     * @throws InvalidSettingsException If that fails.
     */
    void loadSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_isExtractAllProps = settings.getBoolean("isExtractAllProps");
        if (!m_isExtractAllProps) {
            m_selectedProps = settings.getStringArray("selectedProps");
            boolean isNothingSelected =
                m_selectedProps == null || m_selectedProps.length == 0;
            if (isNothingSelected) {
                throw new InvalidSettingsException("Nothing selected");
            }
        }
    }

    /** Loads configuration, inits defaults if fields are missing/invalid.
     * @param settings to load from.
     */
    void loadSettingsNoFail(final NodeSettingsRO settings) {
        m_isExtractAllProps = settings.getBoolean("isExtractAllProps", true);
        if (!m_isExtractAllProps) {
            Set<String> allPropKeys = readAllProps().keySet();
            String[] defSelProps = allPropKeys.toArray(
                    new String[allPropKeys.size()]);
            m_selectedProps =
                settings.getStringArray("selectedProps", defSelProps);
            boolean isNothingSelected =
                m_selectedProps == null || m_selectedProps.length == 0;
            if (isNothingSelected) {
                m_selectedProps = defSelProps;
            }
        }
    }

    /** Read all java properties into map of Strings.
     * @return A new map of string containing all system properties.
     */
    static Map<String, String> readAllProps() {
        Map<String, String> result = new LinkedHashMap<String, String>();

        for (String property : contextProperties) {
            String value = extractContextProperty(property);
            result.put(property, value);
        }
        return result;
    }


    private static String extractContextProperty(final String property) {
        WorkflowManager manager = NodeContext.getContext().getWorkflowManager();
        if (CONTEXT_WORKFLOW_NAME.equals(property)) {
            return manager.getName();
        }
        if (CONTEXT_WORKFLOW_PATH.equals(property)) {
            WorkflowContext context = manager.getContext();
            File wfLocation = context.getOriginalLocation() == null
                    ? context.getCurrentLocation() : context.getOriginalLocation();
            File mpLocation = context.getMountpointRoot();
            if (mpLocation == null || wfLocation == null) {
                return "";
            }
            String wfPath = wfLocation.getAbsolutePath();
            String mpPath = mpLocation.getAbsolutePath();
            assert wfPath.startsWith(mpPath);
            String resultPath = wfPath.substring(mpPath.length());
            return resultPath.replace("\\", "/");
        }
        if (CONTEXT_WORKFLOW_ABSOLUTE_PATH.equals(property)) {
            WorkflowContext context = manager.getContext();
            File wfLocation = context.getCurrentLocation();
            if (wfLocation == null) {
                return "";
            }
            return wfLocation.getAbsolutePath().replace("\\", "/");
        }
        if (CONTEXT_SERVER_USER.equals(property)) {
            return manager.getContext().getUserid();
        }
        if (CONTEXT_TEMP_LOCATION.equals(property)) {
            return manager.getContext().getTempLocation().getAbsolutePath();
        }
        AuthorInformation author = manager.getAuthorInformation();
        if (author != null) {
            if (CONTEXT_AUTHOR.equals(property)) {
                return author.getAuthor();
            }
            if (CONTEXT_EDITOR.equals(property)) {
                return author.getLastEditor();
            }
            if (CONTEXT_CREATION_DATE.equals(property)) {
                Date creationDate = author.getAuthoredDate();
                if (creationDate != null) {
                    return creationDate.toString();
                }
            }
            if (CONTEXT_LAST_MODIFIED.equals(property)) {
                Date modDate = author.getLastEditDate();
                if (modDate != null) {
                    return modDate.toString();
                }
            }
        }

        return null;
    }

    /** Creates a result object for the current configuration.
     * @return A new result.
     * @throws InvalidSettingsException If any property is missing and the
     *         fail-on-missing flag is set.
     */
    Result createResult() throws InvalidSettingsException {
        Map<String, String> props;
        String message = null;
        if (isExtractAllProps()) {
            props = readAllProps();
        } else {
            props = new LinkedHashMap<String, String>();
            String[] selectedProps = getSelectedProps();
            if (selectedProps == null || selectedProps.length == 0) {
                throw new InvalidSettingsException("No properties selected");
            }
            List<String> ignored = new ArrayList<String>();
            for (String s : selectedProps) {
                String value = extractContextProperty(s);
                if (value == null) {
                    ignored.add(s);
                } else {
                    props.put(s, value);
                }
            }
            if (!ignored.isEmpty()) {
                message = "Ignoring selected system properties: "
                    + ignored.toString();
            }
        }
        return new Result(props, message);
    }

    /** Class representing a result. It contains a map containing the key-value
     * pairs and possibly a warning message that should be displayed after
     * execution.
     */
    static final class Result {

        private final String m_warningMessage;
        private final Map<String, String> m_props;

        private Result(final Map<String, String> props,
                final String warningMessage) {
            m_props = props;
            m_warningMessage = warningMessage;
        }

        /** @return the props */
        Map<String, String> getProps() {
            return m_props;
        }

        /** @return the warningMessage */
        String getWarningMessage() {
            return m_warningMessage;
        }

    }

}
