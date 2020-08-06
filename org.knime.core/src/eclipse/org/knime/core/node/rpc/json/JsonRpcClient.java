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
 *   Jul 14, 2020 (carlwitt): created
 */
package org.knime.core.node.rpc.json;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.node.rpc.AbstractRpcClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * A JSON-RPC based implementation of a node data service client.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @since 4.3
 */
public class JsonRpcClient extends AbstractRpcClient {

    private static final Supplier<ObjectMapper> OBJECT_MAPPER = () -> ObjectMapperUtil.getInstance().getObjectMapper();

    private final ObjectMapper m_mapper;

    /**
     * Used in the JSON-RPC call as id to correlate the request with the response.
     */
    private long m_callId = 0;

    /**
     * The json rpc client initialized with the default object mapper.
     */
    public JsonRpcClient() {
        this(OBJECT_MAPPER.get());
    }

    /**
     * @param mapper used to provide custom serialization for the parameters and results of a remote procedure call
     */
    public JsonRpcClient(final ObjectMapper mapper) {
        super();
        m_mapper = mapper;
    }

    @Override
    protected String convertCall(final String serviceName, final Method method, final Object[] args) {
        String res = convertCall(serviceName, method, args, m_mapper, m_callId);
        m_callId++;
        return res;
    }

    static String convertCall(final String serviceName, final Method method, final Object[] args, final ObjectMapper mapper,
        final long callId) {
        ObjectNode request = mapper.createObjectNode();

        // if a method has zero parameters, add an empty params array
        ArrayNode parameters = request.arrayNode();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                parameters.addPOJO(args[i]);
            }
        }

        request.put("jsonrpc", "2.0").put("id", callId)
            .put("method", (serviceName != null ? (serviceName + ".") : "") + method.getName())
            .set("params", parameters);

        return request.toString();
    }

    @Override
    protected <R> R convertResult(final String response, final Type valueType) throws Exception {
        return convertResult(response, valueType, m_mapper);

    }

    @SuppressWarnings("unchecked")
    static <R> R convertResult(final String response, final Type valueType, final ObjectMapper mapper)
        throws Exception { //NOSONAR
        if (valueType == void.class) {
            return null;
        }

        // special handling when the result is of type java.util.Optional
        boolean outerTypeIsOptional = valueType instanceof ParameterizedType
            && ((ParameterizedType)valueType).getRawType().equals(Optional.class);

        try {
            // inflate the JSON-RPC response to retrieve result
            JsonNode jsonRpcResponse = mapper.readValue(response, JsonNode.class);
            JsonNode resultNode = jsonRpcResponse.get("result");

            // instead of an result, an error can be set
            if (resultNode == null) {
                errorResponseToException(jsonRpcResponse);
            }

            if (outerTypeIsOptional) {
                // this case exists because Jackson 2.11 seems to deserialize Optional incorrectly
                // (always returns Optional.empty, even when providing a value, because it's using the BeanDeserializer?)
                ParameterizedType parameterizedType = (ParameterizedType)valueType;
                // the type that is wrapped by the optional
                Class<?> innerType = (Class<?>)parameterizedType.getActualTypeArguments()[0];
                // restore POJO from JSON tree
                Object result = mapper.treeToValue(resultNode, innerType);
                return (R)Optional.ofNullable(result);
            } else {
                // in case we have generic types (List, Map, etc.), we need to convert JSON to POJO with a complex type
                JavaType complexType = TypeFactory.defaultInstance().constructType(valueType);
                Object result = mapper.convertValue(resultNode, complexType);
                return (R)result;
            }
        } catch (JsonProcessingException jsonException) {
            throw new IllegalStateException("Problem while deserialization of json rpc response", jsonException);
        }
    }

    private static void errorResponseToException(final JsonNode jsonRpcResponse) throws Exception { //NOSONAR
        JsonNode errorNode = jsonRpcResponse.get("error");
        try {
            String exceptionClassName = errorNode.get("data").get("exceptionTypeName").asText();
            @SuppressWarnings("unchecked")
            Class<? extends Exception> exceptionClass = (Class<? extends Exception>)Class.forName(exceptionClassName);
            throw exceptionClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new IllegalStateException(
                String.format("The error returned by rpc server couldn't be turned into an exception: %s", errorNode),
                ex);
        }
    }

}
