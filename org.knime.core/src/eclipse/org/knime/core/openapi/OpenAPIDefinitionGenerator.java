package org.knime.core.openapi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowSaveHook;

/**
 * Application and singleton class that allows to create OpenAPI fragments that describes the in- and output of
 * workflows when used as REST resources.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 3.5
 */
public class OpenAPIDefinitionGenerator extends WorkflowSaveHook {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(OpenAPIDefinitionGenerator.class);

	private static final OpenAPIDefinitionGenerator INSTANCE = new OpenAPIDefinitionGenerator();

    private JsonWriterFactory m_writerFactory =
        Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

    /**
     * Name of the file that contains the input parameter definition: {@value}.
     */
    public static final String INPUT_PARAMETERS_FILE = "openapi-input-parameters.json";

	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static final OpenAPIDefinitionGenerator getInstance() {
		return INSTANCE;
	}

	/**
	 * Analyzes the given workflow and generated an OpenAPI fragment for the workflow's input parameters. The returned
	 * object is the schema description of all input parameters. Example:
	 * <pre>
	 * {
     *    "type: "object",
     *    "properties": {
     *       "int-input-7": {
     *         "type":"object",
     *           "properties": {
     *             "integer": {
     *                "type":"integer",
     *                "default":42
     *             }
     *           },
     *           "description": "Enter a number for this value",
     *           "example": {
     *             "integer": 42
     *           }
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
	 */
	public JsonObject createInputParametersDescription(final WorkflowManager wfm) {
		JsonObjectBuilder root = Json.createObjectBuilder();
		if (!wfm.getInputNodes().isEmpty()) {
			JsonObjectBuilder properties = Json.createObjectBuilder();

			for (Map.Entry<String, ExternalNodeData> e : wfm.getInputNodes().entrySet()) {
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

	private JsonObjectBuilder translateToSchema(final JsonValue v) {
		JsonObjectBuilder node = Json.createObjectBuilder();

		if (v instanceof JsonObject) {
			node.add("type", "object");

			JsonObjectBuilder properties = Json.createObjectBuilder();
			for (Map.Entry<String, JsonValue> e : ((JsonObject) v).entrySet()) {
				JsonObjectBuilder child = translateToSchema(e.getValue());
				properties.add(e.getKey(), child);
			}

			node.add("properties", properties);
		} else if (v instanceof JsonNumber) {
			JsonNumber number = (JsonNumber) v;

			if (number.isIntegral()) {
				node.add("type", "integer");
				node.add("default", number.longValue());
			} else {
				node.add("type", "number");
				node.add("default", number.doubleValue());
			}
		} else if (v instanceof JsonString) {
			node.add("type", "string");
			node.add("default", ((JsonString) v).getString());
		} else if ((v.getValueType() == ValueType.FALSE) || (v.getValueType() == ValueType.TRUE)) {
			node.add("type", "boolean");
			node.add("default", v.toString());
		} else if (v instanceof JsonArray) {
			node.add("type", "array");
			if (!((JsonArray) v).isEmpty()) {
				JsonObjectBuilder child = translateToSchema(((JsonArray) v).get(0));
				node.add("items", child);
			}
		}

		return node;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSave(final WorkflowManager workflow, final boolean isSaveData, final File artifactsFolder) throws IOException {
        JsonObject api = createInputParametersDescription(workflow);

        if (!api.isEmpty()) {
            LOGGER.debug("Writing OpenAPI definition for parameters of " + workflow.getName());
            try (JsonWriter out = m_writerFactory
                .createWriter(new FileOutputStream(new File(artifactsFolder, "openapi-input-parameters.json")))) {
                out.write(api);
            }
        }
    }
}
