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
package org.knime.core.openapi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowSaveHook;
import org.knime.core.util.JsonUtil;

import com.google.common.net.MediaType;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

/**
 * Application and singleton class that allows to create OpenAPI fragments that describes the in- and output of
 * workflows when used as REST resources.
 *
 * <p>This class is used by the KNIME Server software. While it has public scope it is not considered table API.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 3.5
 */
public class OpenAPIDefinitionGenerator extends WorkflowSaveHook {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(OpenAPIDefinitionGenerator.class);

    private static final OpenAPIDefinitionGenerator INSTANCE = new OpenAPIDefinitionGenerator();

    private JsonWriterFactory m_writerFactory =
        JsonUtil.getProvider().createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

    /**
     * Name of the file that contains the input parameter definition: {@value}.
     */
    public static final String INPUT_PARAMETERS_FILE = "openapi-input-parameters.json";

    /**
     * Name of the file that contains the input resource definition: {@value}.
     *
     * @since 4.5
     */
    public static final String INPUT_RESOURCES_FILE = "openapi-input-resources.json";

    /**
     * Name of the file that contains the output resource definition: {@value}.
     *
     * @since 4.5
     */
    public static final String OUTPUT_RESOURCES_FILE = "openapi-output-resources.json";

    /**
     * Name of the file that contains the output parameter definition: {@value}.
     */
    public static final String OUTPUT_PARAMETERS_FILE = "openapi-output-parameters.json";


    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance
     */
    public static final OpenAPIDefinitionGenerator getInstance() {
        return INSTANCE;
    }

    /**
     * Analyzes the given workflow and generates an OpenAPI fragment for the workflow's input parameters. The returned
     * object is the schema description of all input parameters.<br/>
     * Example:
     *
     * <pre>
     * {
     *    "type: "object",
     *    "properties": {
     *       "int-input-7": {
     *         "type":"object",
     *         "properties": {
     *            "integer": {
     *               "type":"integer",
     *               "default":42
     *            }
     *         },
     *         "description": "Enter a number for this value",
     *         "example": {
     *           "integer": 42
     *         }
     *       },
     *    "string-input-1": {
     *       "type":"object",
     *       "properties": {
     *         "string": {
     *           "type": "string",
     *            "default": "Default value from the dialog"
     *         }
     *       },
     *       "description": "Enter a string here",
     *       "example": {
     *         "string": "Default value from the dialog"
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * In case the workflow doesn't have any input parameters an empty object is returned.
     *
     * @param wfm a workflow manager, must not be <code>null</code>
     * @return a JSON object
     * @since 5.1
     */
    public JsonObject createInputParametersDescription(final WorkflowManager wfm) {
        return createParametersDescription(wfm.getInputNodes());
    }

    /**
     * Creates the content for input resources that can be used to describe POST requests for multipart/form-data.
     * Example:
     *
     * <pre>
     * {
     *   "schema" : {
     *     "type" : "object",
     *     "properties" : {
     *       "upload-input" : {
     *         "type" : "string",
     *         "format" : "binary"
     *       }
     *     }
     *   },
     *   "encoding": {
     *     "upload-input" : {
     *       "contentType" : "image/png"
     *     }
     *   }
     * }
     * </pre>
     *
     * See https://swagger.io/docs/specification/describing-request-body/multipart-requests/ for mor information.
     *
     * @param wfm the workflow manager
     * @return a JSON object
     * @since 5.1
     */
    public JsonObject createInputResourceContent(final WorkflowManager wfm) {
        final var nodeData =
            wfm.getInputNodes().entrySet().stream().filter(entry -> entry.getValue().getResource() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return createResourceContent(nodeData);
    }

    /**
     * Creates the content for input resources that can be used to define output resources of the job using a special
     * extention (see https://swagger.io/docs/specification/openapi-extensions/).<br/>
     * Example:
     *
     * <pre>
     * {
     *   "schema" : {
     *     "type" : "object",
     *     "properties" : {
     *       "upload-input" : {
     *         "type" : "string",
     *         "format" : "binary"
     *       }
     *     }
     *   },
     *   "encoding": {
     *     "upload-input" : {
     *       "contentType" : "image/png"
     *     }
     *   }
     * }
     * </pre>
     *
     * See https://swagger.io/docs/specification/describing-request-body/multipart-requests/ for mor information.
     *
     * @param wfm the workflow manager
     * @return a JSON object
     * @since 5.1
     */
    public JsonObject createOutputResourceContent(final WorkflowManager wfm) {
        final var nodeData =
            wfm.getExternalOutputs().entrySet().stream().filter(entry -> entry.getValue().getResource() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return createResourceContent(nodeData);
    }

    private static JsonObject createResourceContent(final Map<String, ExternalNodeData> nodeData) {
        final var root = JsonUtil.getProvider().createObjectBuilder();

        if (!nodeData.isEmpty()) {
            final var schema = JsonUtil.getProvider().createObjectBuilder();
            final var properties = JsonUtil.getProvider().createObjectBuilder();
            final var encoding = JsonUtil.getProvider().createObjectBuilder();

            // This is static since application form data accepts binary streams.
            final var schemaObject = JsonUtil.getProvider().createObjectBuilder()//
                .add("type", "string")//
                .add("format", "binary")//
                .build();

            for (final var e : nodeData.entrySet()) {
                properties.add(e.getKey(), schemaObject);

                final var dataEncoding = JsonUtil.getProvider().createObjectBuilder()//
                    .add("contentType", e.getValue().getContentType().orElse(MediaType.OCTET_STREAM.toString()))//
                    .build();

                encoding.add(e.getKey(), dataEncoding);
            }

            schema.add("type", "object").add("properties", properties.build());
            root.add("schema", schema.build());
            root.add("encoding", encoding.build());
        }

        return root.build();
    }

    /**
     * @param value
     * @return
     */
    private JsonObjectBuilder translateToMultipartSchema(final ExternalNodeData value) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Analyzes the given workflow and generates an OpenAPI fragment for the workflow's output parameters. The returned
     * object is the schema description of all output parameters. Example:
     *
     * <pre>
     * {
     *    "type: "object",
     *    "properties": {
     *       "json-output-1": {
     *         "type":"object",
     *         "properties": {
     *           "integer": {
     *             "type":"integer",
     *             "default":42
     *           }
     *         },
     *         "description": "JSON output from the workflow",
     *         "example": {
     *           "integer": 42
     *         }
     *       },
     *     }
     *   }
     * }
     * </pre>
     *
     * In case the workflow doesn't have any input parameters an empty object is returned.
     *
     * @param wfm a workflow manager, must not be <code>null</code>
     * @return a JSON object
     * @since 5.1
     */
    public JsonObject createOutputParametersDescription(final WorkflowManager wfm) {
        return createParametersDescription(wfm.getExternalOutputs());
    }


    private static JsonObject createParametersDescription(final Map<String, ExternalNodeData> nodes) {
        JsonObjectBuilder root = JsonUtil.getProvider().createObjectBuilder();
        if (!nodes.isEmpty()) {
            JsonObjectBuilder properties = JsonUtil.getProvider().createObjectBuilder();

            for (Map.Entry<String, ExternalNodeData> e : nodes.entrySet()) {
                if (e.getValue().getJSONValue() != null) {
                    JsonObjectBuilder input = translateToSchema(e.getValue().getJSONValue());
                    e.getValue().getDescription().ifPresent(d -> input.add("description", d));
                    input.add("example", e.getValue().getJSONValue());
                    properties.add(e.getKey(), input);
                }
            }
            root.add("type", "object");
            root.add("properties", properties);
        }

        return root.build();
    }


    /**
     * Translates a {@link JsonValue} into a json-schema/open-api-schema.
     *
     * @param v the json value
     * @return the resulting json
     */
    private static JsonObjectBuilder translateToSchema(final JsonValue v) {
        final JsonObjectBuilder node = JsonUtil.getProvider().createObjectBuilder();

        if (v instanceof JsonObject) {
            node.add("type", "object");

            final JsonObjectBuilder properties = JsonUtil.getProvider().createObjectBuilder();
            for (Map.Entry<String, JsonValue> e : ((JsonObject)v).entrySet()) {
                final JsonObjectBuilder child = translateToSchema(e.getValue());
                properties.add(e.getKey(), child);
            }

            node.add("properties", properties);
        } else if (v instanceof JsonNumber) {
            final JsonNumber number = (JsonNumber)v;

            if (number.isIntegral()) {
                node.add("type", "integer");
                node.add("default", number.longValue());
            } else {
                node.add("type", "number");
                node.add("default", number.doubleValue());
            }
        } else if (v instanceof JsonString) {
            node.add("type", "string");
            node.add("default", ((JsonString)v).getString());
        } else if ((v.getValueType() == ValueType.FALSE) || (v.getValueType() == ValueType.TRUE)) {
            node.add("type", "boolean");
            node.add("default", v.toString());
        } else if (v instanceof JsonArray) {
            node.add("type", "array");
            if (!((JsonArray)v).isEmpty()) {
                JsonObjectBuilder child = translateToSchema(((JsonArray)v).get(0));
                node.add("items", child);
            }
        }

        return node;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSave(final WorkflowManager workflow, final boolean isSaveData, final File artifactsFolder)
        throws IOException {
        JsonObject api = createInputParametersDescription(workflow);

        if (!api.isEmpty()) {
            LOGGER.debug("Writing OpenAPI definition for input parameters of " + workflow.getName());
            try (final var os = new FileOutputStream(new File(artifactsFolder, INPUT_PARAMETERS_FILE));
                    final var out = m_writerFactory.createWriter(os)) {
                out.write(api);
            }
        }

        JsonObject outApi = createOutputParametersDescription(workflow);

        if (!outApi.isEmpty()) {
            LOGGER.debug("Writing OpenAPI definition for output parameters of " + workflow.getName());
            try (final var os = new FileOutputStream(new File(artifactsFolder, OUTPUT_PARAMETERS_FILE));
                    final var out = m_writerFactory.createWriter(os)) {
                out.write(outApi);
            }
        }

        final var inputResources = createInputResourceContent(workflow);
        if (!inputResources.isEmpty()) {
            LOGGER.debug("Writing OpenAPI definition for input resources of " + workflow.getName());
            try (final var os = new FileOutputStream(new File(artifactsFolder, INPUT_RESOURCES_FILE));
                    final var writer = m_writerFactory.createWriter(os)) {
                writer.write(inputResources);
            }
        }

        final var outputResources = createOutputResourceContent(workflow);
        if (!outputResources.isEmpty()) {
            LOGGER.debug("Writing OpenAPI definition for output resources of " + workflow.getName());
            try (final var os = new FileOutputStream(new File(artifactsFolder, OUTPUT_RESOURCES_FILE));
                    final var writer = m_writerFactory.createWriter(os)) {
                writer.write(outputResources);
            }
        }
    }
}
