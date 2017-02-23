/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.core.gateway.v0.test.service.test;

import static org.knime.core.gateway.services.ServiceManager.service;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.knime.core.gateway.v0.test.service.TestService;
import org.knime.core.gateway.v0.test.entity.TestEnt;
import org.knime.core.gateway.v0.test.test.entity.test.TestEntTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.gateway.server.KnimeGatewayServerManager;
import org.mockito.Matchers;
import org.mockito.Mockito;

/**
 * Tests essentially the client-server communication but not the service logic itself (the service is mocked). It tests
 * whether the service methods are called properly and the de-/serialization of the entities passed and returned.
 *
 * Test needs to be run as plugin test with the following system property set:
 * '-Djavax.ws.rs.ext.RuntimeDelegate=org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl'
 *
 * @author Martin Horn, University of Konstanz
 */
// AUTO-GENERATED CODE; DO NOT MODIFY
public class TestServiceTest {

    TestService m_serviceMock;

    @Before
    public void setup() throws Exception {
        m_serviceMock = mock(TestService.class);

        //spin-up the server with the mocked service
        KnimeGatewayServerManager.startAllForTesting(3000, m_serviceMock);
    }

    @Test
    public void test_getTest() {
		//create parameter values
		TestEnt id = TestEntTest.createEnt(TestEntTest.createValueList());
 
		//create return value   
		List<Object> values = TestEntTest.createValueList();
        TestEnt res = TestEntTest.createEnt(values);

		//mock return value
		Mockito.when(m_serviceMock.getTest(Matchers.anyVararg())).thenReturn(res);

		//call method
		TestEnt methodRes = service(TestService.class).getTest(id);

		//compare results
        TestEntTest.testEnt(methodRes, values);
    }

    @Test
    public void test_getTestList() {
		//create parameter values
		List<TestEnt> list = new ArrayList<>();
		list.add(TestEntTest.createEnt(TestEntTest.createValueList()));
		list.add(TestEntTest.createEnt(TestEntTest.createValueList()));
		list.add(TestEntTest.createEnt(TestEntTest.createValueList()));
		list.add(TestEntTest.createEnt(TestEntTest.createValueList()));
		list.add(TestEntTest.createEnt(TestEntTest.createValueList()));
 
		//create return value   
		List<TestEnt> res = new ArrayList<>(5);
        List<List<Object>> valueLists = new ArrayList<List<Object>>(5);
        List<Object> valList1 = TestEntTest.createValueList();
        valueLists.add(valList1);
        res.add(TestEntTest.createEnt(valList1));
        List<Object> valList2 = TestEntTest.createValueList();
        valueLists.add(valList2);
        res.add(TestEntTest.createEnt(valList2));
        List<Object> valList3 = TestEntTest.createValueList();
        valueLists.add(valList3);
        res.add(TestEntTest.createEnt(valList3));
        List<Object> valList4 = TestEntTest.createValueList();
        valueLists.add(valList4);
        res.add(TestEntTest.createEnt(valList4));
        List<Object> valList5 = TestEntTest.createValueList();
        valueLists.add(valList5);
        res.add(TestEntTest.createEnt(valList5));

		//mock return value
		Mockito.when(m_serviceMock.getTestList(Matchers.anyVararg())).thenReturn(res);

		//call method
		List<TestEnt> methodRes = service(TestService.class).getTestList(list);

		//compare results
        for (int i = 0; i < methodRes.size(); i++) {
            TestEntTest.testEnt(methodRes.get(i), valueLists.get(i));
        }
    }

    @Test
    public void test_getPrimitives() {
		//create parameter values
		String s = "EgOGH";
		List<String> stringlist = new ArrayList<>();
		stringlist.add("fDlno");
		stringlist.add("npbCn");
		stringlist.add("8GulR");
		stringlist.add("kAZ6A");
		stringlist.add("lMo9q");
 
		//create return value   
		double res = 0.4272465819903001;        

		//mock return value
		Mockito.when(m_serviceMock.getPrimitives(Matchers.anyVararg(), Matchers.anyVararg())).thenReturn(res);

		//call method
		double methodRes = service(TestService.class).getPrimitives(s, stringlist);

		//compare results
		assertEquals(res, methodRes);
    }

 
    @After
    public void shutdown() throws Exception {
        KnimeGatewayServerManager.stopAll();
    }

}
