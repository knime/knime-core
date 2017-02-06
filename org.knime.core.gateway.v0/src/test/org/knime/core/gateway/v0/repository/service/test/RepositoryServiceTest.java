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
package org.knime.core.gateway.v0.repository.service.test;

import static org.knime.core.gateway.services.ServiceManager.service;
import static org.mockito.Mockito.mock;

import org.knime.core.gateway.v0.test.repository.entity.test.RepoCategoryEntTest;
import java.util.List;
import org.knime.core.gateway.v0.repository.entity.RepoCategoryEnt;
import org.knime.core.gateway.v0.repository.service.RepositoryService;

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
public class RepositoryServiceTest {

    RepositoryService m_serviceMock;

    @Before
    public void setup() throws Exception {
        m_serviceMock = mock(RepositoryService.class);

        //spin-up the server with the mocked service
        KnimeGatewayServerManager.startAllForTesting(3000, m_serviceMock);
    }

    @Test
    public void test_getNodeRepository() {
    
    	//create parameter values (if there are any)
     
		//create return value
        List<RepoCategoryEnt> list = new ArrayList<>(5);
        List<List<Object>> valueLists = new ArrayList<List<Object>>(5);
        for (int i = 0; i < 5; i++) {
            List<Object> valList = RepoCategoryEntTest.createValueList();
            valueLists.add(valList);
            list.add(RepoCategoryEntTest.createEnt(valList));
        }
        
        //mock method
        Mockito.when(m_serviceMock.getNodeRepository()).thenReturn(list);

		//compare results
        List<RepoCategoryEnt> methodRes = service(RepositoryService.class).getNodeRepository();
        for (int i = 0; i < methodRes.size(); i++) {
            RepoCategoryEntTest.testEnt(methodRes.get(i), valueLists.get(i));
        }
    }

    @Test
    public void test_getNodeDescription() {
    
    	//create parameter values (if there are any)
		//TODO random generate values for primitive parameters
		String nodeTypeID;
     
		//TODO compare primitive return values
    }

 
    @After
    public void shutdown() throws Exception {
        KnimeGatewayServerManager.stopAll();
    }

}
