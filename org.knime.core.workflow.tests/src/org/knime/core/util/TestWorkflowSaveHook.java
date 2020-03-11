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
 *   Oct 25, 2017 (wiswedel): created
 */
package org.knime.core.util;


import static org.hamcrest.CoreMatchers.is;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assert;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowSaveHook;

/**
 * Tests that {@link WorkflowSaveHook} extensions are run.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class TestWorkflowSaveHook extends WorkflowSaveHook {

    public static final String FORCE_FAIL_ERROR = "Deliberately fails";
    public static final String OUT_FILE = "TestCountNodes.txt";

    private static boolean isEnabled;
    private static boolean willFail;

    /** Statically enables this hook to be run.
     * @param enabled The value to set */
    public static void setEnabled(final boolean enabled) {
        isEnabled = enabled;
        willFail = false;
    }

    /** @param value the willFail to set */
    public static void setWillFail(final boolean value) {
        willFail = value;
    }

    @Override
    protected boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void onSave(final WorkflowManager workflow,
        final boolean isSaveData, final File metadataFolder) throws IOException {
        Assert.assertThat("Shouldn't be run", isEnabled, is(true));
        if (willFail) {
            throw new IOException(FORCE_FAIL_ERROR);
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(new File(metadataFolder, OUT_FILE)))) {
            w.write(Integer.toString(workflow.getNodeContainers().size()));
        }
    }

}
