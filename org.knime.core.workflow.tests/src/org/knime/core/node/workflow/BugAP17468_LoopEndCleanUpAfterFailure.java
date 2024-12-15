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
 */
package org.knime.core.node.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests https://knime-com.atlassian.net/browse/AP-17468: 
 * Workflow is partially executed, expected to fail, and also expected to leave no files behind.
 * 
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public class BugAP17468_LoopEndCleanUpAfterFailure extends WorkflowTestCase { // NOSONAR

    /** A temp file is a data temp file if its name starts with that. If that name ever changes this test will fail. */
    private static final Predicate<File> IS_TEMP_FILE = f -> f.getName().startsWith("knime_container");

	@BeforeEach
	public void setUp() throws Exception {
		loadAndSetWorkflow();
	}

	@Test
	public void testExecutePlain() throws Exception {
		executeAllAndWait();
		final WorkflowManager manager = getManager();
		manager.getParent().resetAndConfigureNode(getManager().getID());
		final File tempDir = manager.getContext().getTempLocation();
		Thread.sleep(100); // give the file-in-background-deletion some time to do its work
		assertEquals(0, countFilesInDirectory(tempDir, IS_TEMP_FILE), String.format("Container files remaining in workflow temp directory after clear: %s.",
						Arrays.toString(tempDir.list())));
	}
}