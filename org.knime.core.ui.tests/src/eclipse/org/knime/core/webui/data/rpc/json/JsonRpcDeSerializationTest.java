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
 *   Aug 6, 2020 (hornm): created
 */
package org.knime.core.webui.data.rpc.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.knime.core.util.Pair;
import org.knime.core.webui.data.rpc.RpcClient;
import org.knime.core.webui.data.rpc.RpcSingleClient;
import org.knime.core.webui.data.rpc.json.impl.JsonRpcClient;
import org.knime.core.webui.data.rpc.json.impl.JsonRpcServer;
import org.knime.core.webui.data.rpc.json.impl.JsonRpcSingleClient;
import org.knime.core.webui.data.rpc.json.impl.JsonRpcSingleServer;
import org.knime.core.webui.data.rpc.json.impl.JsonRpcTestUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test the de-/serialization of 'rpc-calls' which are provided by a service interface + implementation. Only the case
 * with the default {@link ObjectMapper} and without any jackson annotation is tested.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class JsonRpcDeSerializationTest {

    /**
     * Tests de-/serialization using the {@link JsonRpcSingleClient} and {@link JsonRpcSingleServer}.
     */
    @Test
    public void testJsonRpcSingleClientDeSerialization() {
        RpcSingleClient<Service> client =
            JsonRpcTestUtil.createRpcSingleClientInstanceForTesting(Service.class, new ServiceImpl(), null);
        Service service = client.getService();
        assertNotNull(service
            .listOfObjectsWithComplexParam(Arrays.asList(new ObjectWithPublicFields(), new ObjectWithPublicFields())));
        service.voidFunction("bar");
        assertNotNull(service.optionalReturn().get()); //NOSONAR
        assertFalse(service.optionalReturnEmpty().isPresent());
        ObjectWithGettersAndSetters obj = service.getObjectWithGettersAndSetters();
        assertThat(obj.getProp1(), is("test"));
        assertThat(obj.getProp2(), is(123.321));
    }

    /**
     * Tests de-/serialization using the {@link JsonRpcClient} and {@link JsonRpcServer}.
     */
    @Test
    public void testJsonRpcClientDeSerialization() {
        // test registration of two instances of the same service
        Pair<String, Object> service1a = Pair.create("Service1a", new ServiceImpl());
        Pair<String, Object> service1b = Pair.create("Service1b", new ServiceImpl());
        Pair<String, Object> service2 = Pair.create("Service2", new OtherServiceImpl());
        @SuppressWarnings("unchecked")
        RpcClient client = JsonRpcTestUtil.createRpcClientInstanceForTesting(null, service1a, service1b, service2);

        // test the two instances of Service
        for (Service serviceImpl : new Service[]{client.getService(Service.class, "Service1a"),
            client.getService(Service.class, "Service1b")}) {

            assertNotNull(serviceImpl.listOfObjectsWithComplexParam(
                Arrays.asList(new ObjectWithPublicFields(), new ObjectWithPublicFields())));
            serviceImpl.voidFunction("bar");
            assertNotNull(serviceImpl.optionalReturn().get()); //NOSONAR
            assertFalse(serviceImpl.optionalReturnEmpty().isPresent());
            ObjectWithGettersAndSetters obj = serviceImpl.getObjectWithGettersAndSetters();
            assertThat(obj.getProp1(), is("test"));
            assertThat(obj.getProp2(), is(123.321));
        }

        // the the OtherService instance
        OtherService otherServiceImpl = client.getService(OtherService.class, "Service2");
        Map<String, ObjectWithGettersAndSetters> map = otherServiceImpl.map();
        assertNotNull(map);
        assertThat(map.size(), equalTo(1));
    }

    /**
     * Tests correct conversion of an exception thrown in a service method into the same re-thrown exception (parsed
     * from a JSON-RPC error).
     */
    @Test(expected = IllegalStateException.class)
    public void testJsonRpcSingleClientServerError() {
        RpcSingleClient<Service> client =
            JsonRpcTestUtil.createRpcSingleClientInstanceForTesting(Service.class, new ServiceImpl(), null);
        Service service = client.getService();
        service.throwsRuntimeException();
    }

    @SuppressWarnings("javadoc")
    public interface Service {
        List<ObjectWithPublicFields> listOfObjectsWithComplexParam(List<ObjectWithPublicFields> param);

        Map<String, String> mapResult();

        public void voidFunction(String param);

        Optional<ObjectWithPublicFields> optionalReturn();

        Optional<ObjectWithPublicFields> optionalReturnEmpty();

        void throwsRuntimeException();

        ObjectWithGettersAndSetters getObjectWithGettersAndSetters();

    }

    @SuppressWarnings("javadoc")
    public static class ServiceImpl implements Service {

        @Override
        public List<ObjectWithPublicFields> listOfObjectsWithComplexParam(final List<ObjectWithPublicFields> param) {
            return Arrays.asList(new ObjectWithPublicFields(), new ObjectWithPublicFields());
        }

        @Override
        public void voidFunction(final String param) {
            //
        }

        @Override
        public Optional<ObjectWithPublicFields> optionalReturn() {
            return Optional.of(new ObjectWithPublicFields());
        }

        @Override
        public Optional<ObjectWithPublicFields> optionalReturnEmpty() {
            return Optional.empty();
        }

        @Override
        public Map<String, String> mapResult() {
            Map<String, String> res = new HashMap<>();
            res.put("foo", "bar");
            return res;
        }

        @Override
        public void throwsRuntimeException() {
            throw new IllegalStateException("problem");
        }

        @Override
        public ObjectWithGettersAndSetters getObjectWithGettersAndSetters() {
            return new ObjectWithGettersAndSetters();
        }
    }

    @SuppressWarnings("javadoc")
    public interface OtherService {
        public Map<String, ObjectWithGettersAndSetters> map();
    }

    @SuppressWarnings("javadoc")
    public static class OtherServiceImpl implements OtherService {
        @Override
        public Map<String, ObjectWithGettersAndSetters> map() {
            Map<String, ObjectWithGettersAndSetters> res = new HashMap<>();
            res.put(new ObjectWithPublicFields().toString(), new ObjectWithGettersAndSetters());
            return res;
        }
    }

    @SuppressWarnings("javadoc")
    public static class ObjectWithPublicFields {
        public String prop1 = "foo"; //NOSONAR

        public double prop2 = 42.42; //NOSONAR
    }

    @SuppressWarnings("javadoc")
    public static class ObjectWithGettersAndSetters {
        private String m_prop1 = "test";

        private double m_prop2 = 123.321;

        public void setProp1(final String prop1) {
            m_prop1 = prop1;
        }

        public String getProp1() {
            return m_prop1;
        }

        public void setProp2(final double prop2) {
            m_prop2 = prop2;
        }

        public double getProp2() {
            return m_prop2;
        }
    }

}
