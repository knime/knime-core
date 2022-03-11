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
 *   9 Mar 2022 (carlwitt): created
 */
package org.knime.core.node.workflow.loader;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.Version;
import org.knime.core.workflow.def.CreatorDef;
import org.knime.core.workflow.def.impl.CreatorDefBuilder;

/**
 * Extracts basic information such as workflow format version from a directory containing a top-level workflow,
 * component, or metanode.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class CreatorLoader {

    /** String constants, such as key names in the workflow configuration, file names, etc. */
    public enum Const {
            /**
             * @see {@link CreatorDef#isCreatorIsNightly()}
             * @see {@link KNIMEConstants#isNightlyBuild()}
             */
            CREATOR_IS_NIGHTLY("created_by_nightly"),
            /** @see CreatorDef#getSavedWithVersion() */
            CREATOR_KNIME_VERSION("created_by"),
            /** Used when the workflow is so old that it doesn't contain a workflow format version yet. */
            ANCIENT_LOAD_VERSION_STRING("0.9.0"),
            /** @see CreatorDef#getWorkflowFormatVersion() */
            WORKFLOW_FORMAT_VERSION("version");

        final String m_const;

        Const(final String key) {
            m_const = key;
        }

        /** @return the string constant. */
        public String get() {
            return m_const;
        }
    }

    /**
     * @param workflowConfig the parsed contents of the workflow.knime XML file
     * @return the version of the KNIME instance that was used to create the workflow.
     * @throws InvalidSettingsException
     */
    private static boolean loadCreatorIsNightly(final ConfigBaseRO workflowConfig) throws InvalidSettingsException {
        if (!workflowConfig.containsKey(Const.CREATOR_IS_NIGHTLY.get())) {
            throw new InvalidSettingsException(
                "Workflow settings do not specify whether the workflow was created by a nightly version of KNIME ("
                    + Const.CREATOR_IS_NIGHTLY.get() + ")");
        }
        return workflowConfig.getBoolean(Const.CREATOR_IS_NIGHTLY.get());
    }

    /**
     * @param workflowConfig the parsed contents of the workflow.knime XML file
     * @return the version of the KNIME instance that was used to create the workflow.
     * @throws InvalidSettingsException when the version string in the settings cannot be converted to a {@link Version}
     */
    private static Version loadCreatorVersion(final ConfigBaseRO workflowConfig) throws InvalidSettingsException {
        Version createdWith = null;
        var createdWithString = workflowConfig.getString(Const.CREATOR_KNIME_VERSION.get(), null);
        if (createdWithString != null) {
            try {
                createdWith = new Version(createdWithString);
            } catch (IllegalArgumentException e) {
                var message = String.format("Unable to parse version string \"%s\" (file \"%s\")", createdWithString,
                    workflowConfig.getKey());
                throw new InvalidSettingsException(message, e);
            }
        }
        return createdWith;
    }

    /**
     * @param workflowConfig the parsed contents of the workflow.knime XML file
     * @return load version, see {@link LoadVersion}
     * @throws InvalidSettingsException if the settings do not contain a workflow format version
     */
    private static String loadWorkflowFormatVersionString(final ConfigBaseRO workflowConfig)
        throws InvalidSettingsException {
        String versionString;
        if (workflowConfig.containsKey(Const.WORKFLOW_FORMAT_VERSION.get())) {
            try {
                versionString = workflowConfig.getString(Const.WORKFLOW_FORMAT_VERSION.get());
            } catch (InvalidSettingsException e) {
                throw new InvalidSettingsException("Can't read version number from \"" + workflowConfig.getKey() + "\"",
                    e);
            }
            // CeBIT 2006 version did not contain a version string.
        } else {
            versionString = Const.ANCIENT_LOAD_VERSION_STRING.get();
        }
        return versionString;
    }

    private final CreatorDef m_def;

    private ConfigBaseRO m_workflowConfig;

    private LoadVersion m_workflowFormatVersion;

    /**
     * @param directory that contains the top-level project (workflow, component, etc.)
     * @throws IOException if the workflow.knime cannot be accessed in the given directory or its contents cannot be
     *             parsed
     */
    public CreatorLoader(final File directory) throws IOException {
        m_workflowConfig = LoaderUtils.parseWorkflowConfig(directory);

        m_def = new CreatorDefBuilder()//
            .setWorkflowFormatVersion(() -> loadWorkflowFormatVersionString(m_workflowConfig),
                LoadVersion.UNKNOWN.toString())//
            .setSavedWithVersion(() -> loadCreatorVersion(m_workflowConfig).toString(), LoadVersion.UNKNOWN.toString())//
            .setCreatorIsNightly(() -> loadCreatorIsNightly(m_workflowConfig), true)//
            .build();

        m_workflowFormatVersion = LoadVersion.fromVersionString(m_def.getWorkflowFormatVersion());

    }

    /**
     * @return the workflow settings extracted from the directory given to the constructor
     */
    ConfigBaseRO getWorkflowConfig() {
        return m_workflowConfig;
    }

    LoadVersion getWorkflowFormatVersion() {
        return m_workflowFormatVersion;
    }

    /**
     * @return the extracted information (workflow format version etc.) about the project (e.g., workflow, shared
     *         component)
     */
    CreatorDef getCreatorDef() {
        return m_def;
    }

}
