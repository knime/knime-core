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
 *   12 Dec 2019 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.util.CoreConstants;

/**
 * Unit tests related to the {@link FlowVariable} class.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class FlowVariableTest {

	/**
	 * Tests that variables of type String operate as they used to in KAP 4.0 and
	 * earlier when their value is null.
	 */
	@Test
	public void testNullStringVariable() {
		String nullString = null;
		FlowVariable var = new FlowVariable("some name", "");
		FlowVariable nullVar1 = new FlowVariable("some name", nullString);
		FlowVariable nullVar2 = new FlowVariable("some name", nullString);

		Assertions.assertEquals(nullVar1, nullVar2);
		Assertions.assertNotEquals(var, nullVar1);
		Assertions.assertNotEquals(nullVar1, var);

		Assertions.assertEquals(nullVar1.getStringValue(), nullString);
		Assertions.assertEquals(nullVar1.getDoubleValue(), Double.NaN, 0d);
		Assertions.assertEquals(nullVar1.getIntValue(), 0);
		Assertions.assertEquals(nullVar1.getValue(StringType.INSTANCE), nullString);
		Assertions.assertEquals(nullVar1.getValueAsString(), nullString);
	}

	@Test
	public void testHideVariableNaming() {
		String[] varNames = new String[] { "abc", "ab\\c", "ab(c", "ab)c", "(", ")", "#`^" };
		for (String varName : varNames) {
			FlowVariable fv = FlowVariable.newHidingVariable(varName);
			assertEquals(Scope.Hide, fv.getScope(), "Flow var type");
			assertEquals(varName, FlowVariable.extractIdentifierFromHidingFlowVariable(fv),
					"Flow Variable Name after extraction");
		}
		assertThrows(IllegalArgumentException.class, () -> FlowVariable.newHidingVariable(null), "Null name");
		assertThrows(IllegalArgumentException.class, () -> FlowVariable.newHidingVariable(""), "Empty name");
		assertThrows(IllegalArgumentException.class, () -> FlowVariable.newHidingVariable(""), "Blank name");
		assertThrows(IllegalFlowVariableNameException.class,
				() -> FlowVariable.newHidingVariable("knime f"), "Reserved name");
	}

	/**
	 * Test FlowVariable creation for system default credentials, ignores the
	 * workflow but specifically checks AP-22551.
	 */
    @Test
    public void testSystemCredentialsToFlowVariableConversion_AP22551() throws Exception {
		FlowVariable systemCredsVariable = CredentialsStore
				.newCredentialsFlowVariable(CoreConstants.CREDENTIALS_KNIME_SYSTEM_DEFAULT_ID, "", "", false, false);
		assertEquals(Scope.Global, systemCredsVariable.getScope());
		FlowVariable otherCredsVariable = CredentialsStore.newCredentialsFlowVariable("some-name", "", "", false,
				false);
		assertEquals(Scope.Flow, otherCredsVariable.getScope());
    }

}