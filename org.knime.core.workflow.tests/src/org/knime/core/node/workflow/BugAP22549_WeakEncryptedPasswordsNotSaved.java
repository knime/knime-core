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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.customization.APCustomization;
import org.knime.core.customization.APCustomizationProviderService;
import org.knime.core.customization.APCustomizationProviderServiceImpl;
import org.knime.core.internal.CorePlugin;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.missing.MissingNodeFactory;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Tests whether passwords can be saved/not-saved depending on AP customizations profiles.
 * 
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
@ExtendWith(TemporaryFolder.class)
public class BugAP22549_WeakEncryptedPasswordsNotSaved extends WorkflowTestCase {

	private static final String DISALLOWED_PASSWORD_SAVING_YML = """
		    version: 'customization-v1.0'
		    workflow:
		      disablePasswordSaving: true
		  """;

	@TempDir
	public File m_tempFolder;

	private File m_tempWorkflowDir;
	private ServiceRegistration<APCustomizationProviderService> m_temporaryCustomizationServiceRegistration;

	private NodeID m_credentialsConfiguration_1;
	private NodeID m_credentialsValidate_2;

	@BeforeEach
	public void setUp() throws Exception {
		m_tempWorkflowDir = new File(m_tempFolder, BugAP22549_WeakEncryptedPasswordsNotSaved.class.getSimpleName());
		final var defaultWorkflowDirectory = getDefaultWorkflowDirectory();
		FileUtils.copyDirectory(defaultWorkflowDirectory, m_tempWorkflowDir);
	}

	@Override
	protected NodeID loadAndSetWorkflow() throws Exception {
		final var workflowID = loadAndSetWorkflow(m_tempWorkflowDir);
		m_credentialsConfiguration_1 = workflowID.createChild(1);
		m_credentialsValidate_2 = workflowID.createChild(2);
		return workflowID;
	}

	/** Just a plain execute, load password from stored workflow. */
	@Test
	public void testExecuteUnchanged() throws Exception {
		loadAndSetWorkflow();
		checkState(m_credentialsConfiguration_1, CONFIGURED);
		checkState(getManager(), CONFIGURED);
		executeAllAndWait();
		checkState(getManager(), EXECUTED);
	}

	/** Load workflow, expect missing nodes to be inserted, can't execute the missing nodes. */
	@Test
	public void testWithTemporaryCustomization() throws Exception {
		loadAndSetWorkflow();
		applyCustomization();

		executeAllAndWait();
		checkState(getManager(), EXECUTED);

		reset(m_credentialsConfiguration_1);
		getManager().save(m_tempWorkflowDir, new ExecutionMonitor(), true);
		closeWorkflow();

		loadAndSetWorkflow();
		checkState(m_credentialsConfiguration_1, CONFIGURED);

		executeAllAndWait();
		checkState(m_credentialsConfiguration_1, EXECUTED); // execute but no password
		checkState(m_credentialsValidate_2, IDLE); // failed because of missing password
		final NodeMessage validateNodeMessage = findNodeContainer(m_credentialsValidate_2).getNodeMessage();
		assertThat("Node message after load", validateNodeMessage.getMessageType(), is(NodeMessage.Type.WARNING));
		assertThat("Node message after load", validateNodeMessage.getMessage(), containsString("password"));
	}

	@Override
	public void tearDown() throws Exception {
		unsetCustomization();
		super.tearDown();
	}

	/** Applies a customization that forbids the use of both the Concatenate and Normalizer Node. */
	private void applyCustomization() throws JsonProcessingException, JsonMappingException {
		// Register the test-specific customization service
		final var context = FrameworkUtil.getBundle(CorePlugin.class).getBundleContext();
		final Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);  // Use the highest possible ranking

		final APCustomization tempCustomization = new ObjectMapper(new YAMLFactory())
				.readValue(DISALLOWED_PASSWORD_SAVING_YML, APCustomization.class);

		m_temporaryCustomizationServiceRegistration = context.registerService(
				APCustomizationProviderService.class, () -> tempCustomization, properties);
	}

	private void unsetCustomization() {
		if (m_temporaryCustomizationServiceRegistration != null) {
			m_temporaryCustomizationServiceRegistration.unregister();
			m_temporaryCustomizationServiceRegistration = null;
			final var context = FrameworkUtil.getBundle(CorePlugin.class).getBundleContext();
			context.registerService(APCustomizationProviderService.class, new APCustomizationProviderServiceImpl(),
					null);
		}
	}
}