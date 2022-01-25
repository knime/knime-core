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
package org.knime.core.node.workflow.def;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.knime.core.workflow.def.ComponentDef.Attribute.IN_PORTS;
import static org.knime.core.workflow.def.ComponentDef.Attribute.OUT_PORTS;

import java.awt.Component;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.Test;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.util.LoadVersion;
import org.knime.core.node.workflow.DefWorkflowManagerWrapper;
import org.knime.core.node.workflow.FileWorkflowLoader;
import org.knime.core.node.workflow.WorkflowDataRepository;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.WorkflowTestCase;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.workflow.def.ComponentDef;
import org.knime.core.workflow.def.Load;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.WorkflowProjectDef;
import org.knime.core.workflow.def.impl.DefaultWorkflowDef;
import org.knime.core.workflow.def.impl.DefaultWorkflowProjectDef;
import org.knime.core.workflow.def.loader.ComponentLoader;
import org.knime.core.workflow.def.loader.WorkflowLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class FileWorkflowLoaderTest extends WorkflowTestCase {

	/**
	 */
	@Test
	public void testLoad() throws Exception {
		
		File workflowDirectory = new File("src/test/resources/enh11762_WorkflowRepresentation");
		
		final WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(workflowDirectory);
		
		// TODO introduce a factory that chooses the correct loader for a stream/directory/file
		FileWorkflowLoader fwl = new FileWorkflowLoader(loadHelper); 
		
		fwl.getDef(new WorkflowLoadResult("workflow name"));
		
		// TODO fix a JSON to load the ground truth Defs from and compare them to the file loaded Defs.
		
	}
	
	@Test
	public void testFilter() {
		
		// only load workflow annotations
		new WorkflowLoader.Filter().include(WorkflowDef.Attribute.ANNOTATIONS);
		
		// load everything from (top-level) components except their recursive contents
        new ComponentLoader.Filter().includeAll().except(ComponentDef.Attribute.WORKFLOW);
        
	}

}
