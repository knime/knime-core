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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
  * History
  *   May 13, 2011 (morent): created
  */

package org.knime.core.util;

import java.io.File;

import org.knime.core.node.workflow.SingleNodeContainerPersistorVersion200;
import org.knime.core.node.workflow.WorkflowPersistor;


/**
 * Important: This class is no public api but for internal usage only!
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class KnimeFileUtil {
    private KnimeFileUtil() {
        // hiding default constructor of utility class
    }

    /**
     * @param file the file to check
     * @return true if the file represents a workflow, false otherwise
     * @noreference This method is not intended to be referenced by clients.
     */
    public static boolean isWorkflow(final File file) {
        if (file == null || !file.exists() || !file.isDirectory()) {
            return false;
        }
        // It is a workflow if it contains a workflow file but not its parent.
        File parent = file.getParentFile();
        if (parent != null) {
            if (new File(parent, WorkflowPersistor.WORKFLOW_FILE).exists()) {
                return false;
            }
        }
        return new File(file, WorkflowPersistor.WORKFLOW_FILE).exists();
    }

    /**
     * @param file the file to check
     * @return true if the file represents a workflow group, false otherwise
     * @noreference This method is not intended to be referenced by clients.
     */
    public static boolean isWorkflowGroup(final File file) {
        if (file == null || !file.exists() || !file.isDirectory()) {
            return false;
        }
        if (new File(file, WorkflowPersistor.METAINFO_FILE).exists()) {
            return true;
        }  else if (
                // workflow or meta node
                new File(file, WorkflowPersistor.WORKFLOW_FILE).exists()
                // workflow template
                || new File(file, WorkflowPersistor.TEMPLATE_FILE).exists()
                // node
                || new File(file, SingleNodeContainerPersistorVersion200.
                        NODE_FILE).exists()) {
            return false;
        }
        return true;
    }

    /**
     * @param file  the file to check
     * @return true if the file represents a meta node template, false
     *      otherwise
     * @noreference This method is not intended to be referenced by clients.
     */
    public static boolean isMetaNodeTemplate(final File file) {
        if (file == null || !file.exists() || !file.isDirectory()) {
            return false;
        }
        File templateFile = new File(file, WorkflowPersistor.TEMPLATE_FILE);
        return templateFile.exists();
    }
}
