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
 *   9 Mar 2022 (Dionysios Stolis): created
 */
package org.knime.core.node.workflow.loader;

import java.io.File;
import java.io.IOException;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.loader.WorkflowLoader.Const;

/**
 * //TODO We can add all the read from file methods for the files workflow.knime, settings.xml, template.knime.
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
public class LoaderUtils {

    private static final String WORKFLOW_SETTINGS_FILE_NAME = "workflow.knime";

    private static final String NODE_SETTINGS_FILE_NAME = "settings.xml";

    static ConfigBaseRO readNodeConfigFromFile(final File nodeDirectory) throws IOException {
        var nodeSettingsFile = new File(nodeDirectory, NODE_SETTINGS_FILE_NAME);
        try {
            return SimpleConfig.parseConfig(nodeSettingsFile.getAbsolutePath(), nodeSettingsFile);
        } catch (IOException e) {
            throw new IOException("Cannot load the " + NODE_SETTINGS_FILE_NAME, e);
        }
    }

    static ConfigBaseRO readWorkflowConfigFromFile(final File nodeDirectory) throws IOException {
        var workflowSettingsFile = new File(nodeDirectory, WORKFLOW_SETTINGS_FILE_NAME);
        try {
            return SimpleConfig.parseConfig(workflowSettingsFile.getAbsolutePath(), workflowSettingsFile);
        } catch (IOException e) {
            throw new IOException("Cannot load the " + WORKFLOW_SETTINGS_FILE_NAME, e);
        }
    }

    /**
     * Also validates the directory argument.
     *
     * @param workflowDirectory from which to load the workflow.
     * @return the workflow.knime file in the given directory.
     */
    static File getWorkflowDotKnimeFile(final File workflowDirectory) throws IOException {
        if (workflowDirectory == null) {
            throw new IllegalArgumentException("Directory must not be null.");
        }
        if (!workflowDirectory.isDirectory()) {
            throw new IOException("Not a directory: " + workflowDirectory);
        }
        if (!workflowDirectory.canRead()) {
            throw new IOException("Cannot read from directory: " + workflowDirectory);
        }

        // template.knime or workflow.knime
        // TODO ReferencedFile usage
        var dotKNIMERef = new ReferencedFile(new ReferencedFile(workflowDirectory), Const.WORKFLOW_FILE_NAME.get());
        var dotKNIME = dotKNIMERef.getFile();

        if (!dotKNIME.isFile()) {
            throw new IOException(String.format("No %s file in directory %s", Const.WORKFLOW_FILE_NAME.get(),
                workflowDirectory.getAbsolutePath()));
        }
        return dotKNIME;
    }

    /**
     * Parses the file (usually workflow.knime) that describes the workflow.
     *
     * @param workflowDirectory containing the workflow
     */
    static ConfigBaseRO parseWorkflowConfig(final File workflowDirectory) throws IOException {
        var workflowDotKnime = LoaderUtils.getWorkflowDotKnimeFile(workflowDirectory);
        return SimpleConfig.parseConfig(workflowDotKnime.getAbsolutePath(), workflowDotKnime);
    }


    /**
     * The node settings file is typically named settings.xml (for native nodes and components) and workflow.knime for
     * meta nodes. However, the actual name is stored in the parent workflow's entry that describes the node.
     *
     * @param workflowNodeConfig the configuration tree in the workflow description (workflow.knime) that describes the
     *            node
     * @param workflowDir the directory that contains the node directory
     * @return
     * @throws InvalidSettingsException
     */
    static File loadNodeFile(final ConfigBaseRO workflowNodeConfig, final File workflowDir)
        throws InvalidSettingsException {
        // relative path to node configuration file
        var fileString = workflowNodeConfig.getString(Const.KEY_NODE_SETTINGS_FILE.get());
        if (fileString == null) {
            throw new InvalidSettingsException(
                "Unable to read settings " + "file for node " + workflowNodeConfig.getKey());
        }
        // fileString is something like "File Reader(#1)/settings.xml", thus
        // it contains two levels of the hierarchy. We leave it here to the
        // java.io.File implementation to resolve these levels
        var nodeFile = new File(workflowDir, fileString);
        if (!nodeFile.isFile() || !nodeFile.canRead()) {
            throw new InvalidSettingsException("Unable to read settings " + "file " + nodeFile.getAbsolutePath());
        }
        return nodeFile;
    }
}
