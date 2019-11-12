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
package org.knime.core.node.workflow.artifacts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.SubNodeDescriptionProvider;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowSaveHook;
import org.knime.core.util.CoreConstants;
import org.knime.core.util.CoreConstants.ConfigurationType;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;

/**
 * {@link WorkflowSaveHook} that saves the top-level input configurations (defined by {@link DialogNode}s, aka
 * configuration nodes) as well as the workflow variables and credentials into respective artifact-files.
 *
 * <p>
 * This class is used by the KNIME Server software. While it has public scope it is not considered stable API.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public class WorkflowConfigArtifactsGenerator extends WorkflowSaveHook {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowConfigArtifactsGenerator.class);

    private static final WorkflowConfigArtifactsGenerator INSTANCE = new WorkflowConfigArtifactsGenerator();

    private JsonWriterFactory m_writerFactory =
        Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

    private final ObjectMapper m_mapper;

    /**
     * Generates a new artifacts generator.
     */
    public WorkflowConfigArtifactsGenerator() {
        m_mapper = new ObjectMapper();
        m_mapper.registerModule(new JSR353Module());
        m_mapper.registerModule(new Jdk8Module());
        m_mapper.registerModule(new JavaTimeModule());
        m_mapper.enable(SerializationFeature.INDENT_OUTPUT);
        m_mapper.setSerializationInclusion(Include.NON_NULL);
        m_mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        m_mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        m_mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance
     */
    public static final WorkflowConfigArtifactsGenerator getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("rawtypes")
    private static void extractTopLevelConfiguration(final WorkflowManager wfm, final JsonObjectBuilder builder) {
        Map<String, DialogNode> configurationNodes = wfm.getConfigurationNodes(true);
        if (!configurationNodes.isEmpty()) {
            configurationNodes.entrySet().forEach(e -> {
                final JsonValue value = e.getValue().getDefaultValue().toJson();
                if (e.getValue().getDialogRepresentation() instanceof SubNodeDescriptionProvider
                    && value.getValueType() == ValueType.OBJECT) {
                    final JsonObject object = enrich((JsonObject)value, "label",
                        ((SubNodeDescriptionProvider)e.getValue().getDialogRepresentation()).getLabel());
                    builder.add(e.getKey(), object);
                } else {
                    builder.add(e.getKey(), value);
                }
            });
        }
    }

    private static JsonObject enrich(final JsonObject source, final String key, final String value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(key, value);
        source.entrySet().forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build();
    }

    @SuppressWarnings("rawtypes")
    private JsonObject extractTopLevelConfigurationRepresentation(final WorkflowManager wfm) {
        final JsonObjectBuilder root = Json.createObjectBuilder();
        final Map<String, DialogNode> configurationNodes = wfm.getConfigurationNodes(true);

        if (!configurationNodes.isEmpty()) {
            configurationNodes.entrySet().stream().filter(e -> m_mapper.canSerialize(e.getClass())).forEach(e -> {
                try {
                    root.add(e.getKey(),
                        Json.createReader(new StringReader(m_mapper.writerWithView(CoreConstants.ArtifactsView.class)
                            .writeValueAsString(e.getValue().getDialogRepresentation()))).readObject());
                } catch (JsonProcessingException ex) {
                    LOGGER.warn("Could not serialize representation of configuration '" + e.getKey() + "'", ex);
                }
            });
        }

        return root.build();
    }

    private static void extractWorkflowVariables(final WorkflowManager wfm, final JsonObjectBuilder builder) {
        List<FlowVariable> workflowVariables = wfm.getWorkflowVariables();
        for (FlowVariable v : workflowVariables) {
            JsonObjectBuilder val = Json.createObjectBuilder();
            switch (v.getType()) {
                case INTEGER:
                    val.add("type", "integer");
                    val.add("default", v.getIntValue());
                    break;
                case DOUBLE:
                    val.add("type", "number");
                    double d = v.getDoubleValue();
                    if (Double.isNaN(d)) {
                        d = 0;
                        val.add("NaN", true);
                    } else if (Double.isInfinite(d)) {
                        d = 0;
                        if (d < 0) {
                            val.add("NEGATIVE_INFINITY", true);
                        } else {
                            val.add("POSITIVE_INFINITY", true);
                        }
                    }
                    val.add("default", d);
                    break;
                case STRING:
                    val.add("type", "string");
                    val.add("default", v.getStringValue());
                    break;
                default:
                    throw new IllegalStateException("Unexpected flow variable type: " + v.getType());
            }
            builder.add(CoreConstants.WORKFLOW_VARIABLES + v.getName(), val);
        }
    }

    private static void extractWorkflowCredentials(final WorkflowManager wfm, final JsonObjectBuilder builder) {
        Iterable<Credentials> credentials = wfm.getCredentialsStore().getCredentials();
        for (Credentials c : credentials) {
            JsonObjectBuilder val = Json.createObjectBuilder();
            val.add("type", "object");
            JsonObjectBuilder login = Json.createObjectBuilder();
            login.add("type", "string");
            login.add("default", c.getLogin());
            val.add("login", login);
            JsonObjectBuilder pwd = Json.createObjectBuilder();
            pwd.add("type", "string");
            pwd.addNull("default");
            val.add("password", pwd);

            builder.add(CoreConstants.WORKFLOW_CREDENTIALS + c.getName(), val);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSave(final WorkflowManager workflow, final boolean isSaveData, final File artifactsFolder)
        throws IOException {
        final JsonObject config = getWorkflowConfiguration(workflow, ConfigurationType.CONFIGURATION_TEMPLATE);

        LOGGER.debug("Writing configuration of workflow " + workflow.getName());
        try (FileOutputStream fos =
            new FileOutputStream(new File(artifactsFolder, ConfigurationType.CONFIGURATION_TEMPLATE.getFileName()));
                JsonWriter out = m_writerFactory.createWriter(fos)) {
            out.write(config);
        }

        JsonObject representation = getWorkflowConfiguration(workflow, ConfigurationType.CONFIGURATION_REPRESENTATION);
        LOGGER.debug("Writing configuration representation of workflow " + workflow.getName());
        try (FileOutputStream fos = new FileOutputStream(
            new File(artifactsFolder, ConfigurationType.CONFIGURATION_REPRESENTATION.getFileName()));
                JsonWriter out = m_writerFactory.createWriter(fos)) {
            out.write(representation);
        }
    }

    /**
     * Assembles the workflow configuration for the given {@link WorkflowManager}.
     *
     * @param workflowManager the workflow manager
     * @param configurationType the configuration that type that shall be returned.
     * @return the workflow configuration template
     */
    public JsonObject getWorkflowConfiguration(final WorkflowManager workflowManager,
        final ConfigurationType configurationType) {
        if (configurationType == ConfigurationType.CONFIGURATION_REPRESENTATION) {
            return extractTopLevelConfigurationRepresentation(workflowManager);
        }

        final JsonObjectBuilder confBuilder = Json.createObjectBuilder();

        extractTopLevelConfiguration(workflowManager, confBuilder);
        extractWorkflowVariables(workflowManager, confBuilder);
        extractWorkflowCredentials(workflowManager, confBuilder);

        return confBuilder.build();
    }
}
