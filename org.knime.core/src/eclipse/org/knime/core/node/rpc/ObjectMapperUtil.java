package org.knime.core.node.rpc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Utility class around Jackson's {@link ObjectMapper}. It set's up an {@link ObjectMapper}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 * TODO review the necessity and content of this class.
 */
public final class ObjectMapperUtil {
    private static final ObjectMapperUtil INSTANCE = new ObjectMapperUtil();

    /**
     * Returns the singleton instance of this class.
     *
     * @return the singleton instance
     */
    public static ObjectMapperUtil getInstance() {
        return INSTANCE;
    }

    private ObjectMapper m_mapper = null;
    private ObjectMapper m_binaryMapper = null;
    private ObjectMapper m_nodeDataServiceMapper = null;

    private ObjectMapperUtil() {
        //utility class
    }

    private static void configureObjectMapper(final ObjectMapper mapper) {
        mapper.registerModule(new Jdk8Module());

        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    }

    /**
     * Returns the shared object mapper.
     *
     * @return an object mapper
     */
    public ObjectMapper getObjectMapper() {
        if (m_mapper == null) {
            m_mapper = createObjectMapper();
        }
        return m_mapper;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        configureObjectMapper(mapper);
        return mapper;
    }

    /**
     * @return object mapper used to marshall parameters and results for calls to a node model's data service
     * @since 4.11
     */
    public ObjectMapper getNodeDataServiceObjectMapper() {
        if (m_nodeDataServiceMapper == null) {
            m_nodeDataServiceMapper = createNodeDataServiceObjectMapper();
        }
        return m_nodeDataServiceMapper;
    }

    private static ObjectMapper createNodeDataServiceObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        configureObjectMapper(mapper);
        return mapper;
    }
}
