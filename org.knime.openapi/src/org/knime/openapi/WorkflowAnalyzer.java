package org.knime.openapi;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.LockFailedException;

/**
 * Application and singleton class that allows to create OpenAPI fragments that describes the in- and output of
 * workflows when used as REST resources.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class WorkflowAnalyzer implements IApplication {
	private final JsonWriterFactory m_writerFactory;

	private static final WorkflowAnalyzer INSTANCE = new WorkflowAnalyzer();

	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static final WorkflowAnalyzer getInstance() {
		return INSTANCE;
	}

	/**
	 * Only used by the Eclipse framework. Use {@link #getInstance()} instead.
	 */
	public WorkflowAnalyzer() {
		Map<String, Object> props = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);
		m_writerFactory = Json.createWriterFactory(props);
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

	private boolean writeOpenAPIInfo(final Path workflowDir) throws IOException, InvalidSettingsException,
			CanceledExecutionException, UnsupportedWorkflowVersionException, LockFailedException {
		WorkflowLoadHelper lh = new WorkflowLoadHelper(workflowDir.toFile());
		WorkflowManager wfm = WorkflowManager.loadProject(workflowDir.toFile(), new ExecutionMonitor(), lh)
				.getWorkflowManager();

		JsonObject api = createInputParametersDescription(wfm);

		if (!api.isEmpty()) {
			try (JsonWriter out = m_writerFactory
					.createWriter(Files.newOutputStream(workflowDir.resolve("input-parameters.json")))) {
				out.write(api);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

		if (args.length != 1) {
		    System.out.println("Usage: OpenAPIWorkflowAnalyzer <dir>");
		    System.out.println(" <dir>: a directory that is recursivly scanned for workflow");
		    return IApplication.EXIT_OK;
		}

		Path root = Paths.get(args[0]);
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
				Path workflowFile = dir.resolve(WorkflowPersistor.WORKFLOW_FILE);
				if (Files.exists(workflowFile)) {
					try {
						if (writeOpenAPIInfo(dir)) {
							System.out.println("Created OpenAPI information for " + dir);
						} else {
							System.out.println("No input node in " + dir);
						}
					} catch (InvalidSettingsException | CanceledExecutionException | UnsupportedWorkflowVersionException
							| LockFailedException ex) {
						System.err.println("Could not analyze " + dir + ": " + ex.getMessage());
					}
					return FileVisitResult.SKIP_SUBTREE;
				} else {
					return FileVisitResult.CONTINUE;
				}
			}
		});

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
	}
}
