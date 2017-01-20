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
 * History
 *   Dec 2, 2016 (hornm): created
 */
package org.knime.core.gateway.codegen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.knime.core.gateway.codegen.types.MethodParam;
import org.knime.core.gateway.codegen.types.ServiceDef;
import org.knime.core.gateway.codegen.types.ServiceMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class ServiceGenerator {

    private String m_templateFile;

    private String m_destDirectory;

    private String m_destFileName;

    /**
     * @param templateFile the template file
     * @param destDirectory the destination directory for the generated java-files
     * @param destFileName the name of the destination file. The placeholder "##serviceName##" will be replaced with the
     *            actual service name.
     *
     */
    public ServiceGenerator(final String templateFile, final String destDirectory, final String destFileName) {
        m_templateFile = templateFile;
        m_destDirectory = destDirectory;
        m_destFileName = destFileName;
    }



    public void generate() {

        try {
            Velocity.init();
            VelocityContext context = new VelocityContext();
            List<ServiceDef> serviceDefs = getServiceDefs();


            for (ServiceDef serviceDef : serviceDefs) {
                context.put("name", serviceDef.getName());

                context.put("methods", serviceDef.getMethods());

                List<String> imports = new ArrayList<String>(serviceDef.getImports());

                context.put("imports", imports);

                Template template = null;
                try {
                    template = Velocity.getTemplate(m_templateFile);
                } catch (ResourceNotFoundException rnfe) {
                    System.out.println("Example : error : cannot find template " + m_templateFile);
                } catch (ParseErrorException pee) {
                    System.out.println("Example : Syntax error in template " + m_templateFile + ":" + pee);
                }

                String destFileName = m_destFileName.replace("##serviceName##", serviceDef.getName());

                /*
                 *  Now have the template engine process your template using the
                 *  data placed into the context.  Think of it as a  'merge'
                 *  of the template and the data to produce the output stream.
                 */
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(m_destDirectory + destFileName + ".java"))) {
                    if (template != null) {
                        template.merge(context, writer);
                    }
                }
                String[] pathSegments = "services.".concat(serviceDef.getPackage()).split("\\.");
                Path path = Paths.get("api", pathSegments);
                Files.createDirectories(path);
                Path filePath = path.resolve(destFileName + ".json");
                try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(writer, serviceDef);
                    ServiceDef d = mapper.readValue(Files.newBufferedReader(filePath), ServiceDef.class);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }



    public static List<ServiceDef> getServiceDefs() {
        return Arrays.asList(
            new ServiceDef(
                "Defines service methods to query and manipulate the content of a workflow.",
                "org.knime.core.workflow", "WorkflowService",
                new ServiceMethod(
                    "getWorkflow",
                    "Get the workflow entity for a given ID.", "WorkflowEnt",
                    new MethodParam("The identifier as per #getAllWorkflows", "id", "EntityID")),
                new ServiceMethod(
                    "updateWorkflow",
                    "Trigger an update of the given workflow (@Martin, what's that mean?)", "void",
                    new MethodParam("The workflow to be updated", "wf", "WorkflowEnt")),
                new ServiceMethod(
                    "getAllWorkflows",
                    "Get a list of IDs for all known workflows", "List<EntityID>"))
            .addImports(
                "org.knime.core.gateway.v0.workflow.entity.EntityID",
                "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt",
                "java.util.List"),
            new ServiceDef(
                "Defines service methods to query the list of available nodes and metanodes (node repository).",
                "org.knime.core.repository", "RepositoryService",
                new ServiceMethod(
                    "getNodeRepository",
                    "Get the list of nodes in the node repository, categories and metanodes included.", "List<RepoCategoryEnt>"),
                new ServiceMethod(
                    "getNodeDescription",
                    "Get the node description (html) for a given node", "String",
                    new MethodParam(
                        "The identifier for a given node",
                        "nodeTypeID", "String")))
            .addImports("java.util.List", "org.knime.core.gateway.v0.workflow.entity.RepoCategoryEnt"),
            new ServiceDef(
                "Defines service methods to query details on an individual node or metanode.",
                "org.knime.core.workflow", "NodeContainerService",
                new ServiceMethod(
                    "getNodeSettingsJSON",
                    "Get the settings tree for a given node in a JSON structure.", "String",
                    new MethodParam(
                        "The id of the workflow the node is in.",
                        "workflowID", "EntityID"),
                    new MethodParam(
                        "The id of the node itself.",
                        "nodeID", "String")))
                .addImports("org.knime.core.gateway.v0.workflow.entity.EntityID"),
            new ServiceDef(
                "Defines service methods run or reset a workflow.",
                "org.knime.core.workflow", "ExecutionService",
                new ServiceMethod(
                    "canExecuteUpToHere",
                    "Test if a given node can be executed (e.g. not yet executed and not executing).", "boolean",
                    new MethodParam(
                        "The id of the workflow the node is in.",
                        "workflowID", "EntityID"),
                    new MethodParam(
                        "The id of the node itself.",
                        "nodeID", "String")),
                new ServiceMethod(
                    "executeUpToHere",
                    "Trigger an execution of a single node or a chain of nodes (endpoint reference).", "WorkflowEnt",
                    new MethodParam(
                        "The id of the workflow the node is in.",
                        "workflowID", "EntityID"),
                    new MethodParam(
                        "The id of the node itself.",
                        "nodeID", "String")))
            .addImports("org.knime.core.gateway.v0.workflow.entity.EntityID", "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt"),
            new ServiceDef(
                "A simple test service.",
                "org.knime.core.test", "TestService",
                new ServiceMethod(
                    "test",
                    "Some example documentation.", "TestEnt",
                    new MethodParam(
                        "Some example documentation.",
                        "id", "TestEnt")),
                new ServiceMethod(
                    "testList",
                    "Some example documentation.", "List<TestEnt>",
                    new MethodParam(
                        "Some example documentation.",
                        "list", "List<TestEnt>")),
                new ServiceMethod(
                    "primitives",
                    "Some example documentation.", "double",
                    new MethodParam(
                        "Some example documentation.",
                        "s", "String"),
                    new MethodParam("Some example documentation.",
                        "stringlist", "List<String>")))
            .addImports("org.knime.core.gateway.v0.workflow.entity.TestEnt",
                        "java.util.List"));
    }

}
