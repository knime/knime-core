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
import java.util.Arrays;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.knime.core.gateway.codegen.types.AbstractDef;
import org.knime.core.gateway.codegen.types.MethodParam;
import org.knime.core.gateway.codegen.types.ServiceDef;
import org.knime.core.gateway.codegen.types.ServiceMethod;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

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
                context.put("classDescription", serviceDef.getDescription());
                context.put("name", serviceDef.getName());
                context.put("methods", serviceDef.getMethods());
                context.put("imports", serviceDef.getImports());

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
                String[] pathSegments = serviceDef.getPackage().split("\\.");
                Path path = Paths.get("api", pathSegments);
                Files.createDirectories(path);
                Path filePath = path.resolve(destFileName + ".json");
                try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.setSerializationInclusion(Include.NON_NULL);
                    mapper.registerModule(new Jdk8Module());
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    // 4 space indentation
                    DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
                    DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
                    printer.indentObjectsWith(indenter);
                    printer.indentArraysWith(indenter);
                    mapper.writer(printer).writeValue(writer, serviceDef);
                    ServiceDef d = (ServiceDef)mapper.readValue(Files.newBufferedReader(filePath), AbstractDef.class);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    public static List<ServiceDef> getServiceDefs() {
        return Arrays.asList(
            new ServiceDef(
                "Defines service methods to query and manipulate the content of a workflow.",
                "org.knime.core.gateway.v0.workflow.services", "WorkflowService",
                new ServiceMethod(
                    "Workflow",
                    ServiceMethod.Operation.Get, "Get the workflow entity for a given ID.",
                    "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt", new MethodParam("The identifier as per #getAllWorkflows", "id", "org.knime.core.gateway.v0.workflow.entity.EntityID")),
                new ServiceMethod(
                    "Workflow",
                    ServiceMethod.Operation.Update, "Trigger an update of the given workflow (@Martin, what's that mean?)",
                    "void", new MethodParam("The workflow to be updated", "wf", "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt")),
                new ServiceMethod(
                    "AllWorkflows",
                    ServiceMethod.Operation.Get, "Get a list of IDs for all known workflows", "List<org.knime.core.gateway.v0.workflow.entity.EntityID>")),
            new ServiceDef(
                "Defines service methods to query the list of available nodes and metanodes (node repository).",
                "org.knime.core.gateway.v0.repository.services", "RepositoryService",
                new ServiceMethod(
                    "NodeRepository",
                    ServiceMethod.Operation.Get,
                    "Get the list of nodes in the node repository, categories and metanodes included.", "List<org.knime.core.gateway.v0.workflow.entity.RepoCategoryEnt>"),
                new ServiceMethod(
                    "NodeDescription",
                    ServiceMethod.Operation.Get, "Get the node description (html) for a given node",
                    "String", new MethodParam(
                        "The identifier for a given node",
                        "nodeTypeID", "String"))),
            new ServiceDef(
                "Defines service methods to query details on an individual node or metanode.",
                "org.knime.core.gateway.v0.workflow.services", "NodeContainerService",
                new ServiceMethod(
                    "NodeSettingsJSON",
                    ServiceMethod.Operation.Get, "Get the settings tree for a given node in a JSON structure.",
                    "String",
                    new MethodParam(
                        "The id of the workflow the node is in.",
                        "workflowID", "org.knime.core.gateway.v0.workflow.entity.EntityID"), new MethodParam(
                        "The id of the node itself.",
                        "nodeID", "String"))),
            new ServiceDef(
                "Defines service methods run or reset a workflow.",
                "org.knime.core.gateway.v0.workflow.services", "ExecutionService",
                new ServiceMethod(
                    "canExecuteUpToHere",
                    ServiceMethod.Operation.Get, "Test if a given node can be executed (e.g. not yet executed and not executing).",
                    "boolean",
                    new MethodParam(
                        "The id of the workflow the node is in.",
                        "workflowID", "org.knime.core.gateway.v0.workflow.entity.EntityID"), new MethodParam(
                        "The id of the node itself.",
                        "nodeID", "String")),
                new ServiceMethod(
                    "executeUpToHere",
                    ServiceMethod.Operation.Set, "Trigger an execution of a single node or a chain of nodes (endpoint reference).",
                    "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt",
                    new MethodParam(
                        "The id of the workflow the node is in.",
                        "workflowID", "org.knime.core.gateway.v0.workflow.entity.EntityID"), new MethodParam(
                        "The id of the node itself.",
                        "nodeID", "String"))),
            new ServiceDef(
                "A simple test service.",
                "org.knime.core.test.services", "TestService",
                new ServiceMethod(
                    "test",
                    ServiceMethod.Operation.Get, "Some example documentation.",
                    "org.knime.core.test.entity.TestEnt", new MethodParam(
                        "Some example documentation.",
                        "id", "org.knime.core.test.entity.TestEnt")),
                new ServiceMethod(
                    "testList",
                    ServiceMethod.Operation.Get, "Some example documentation.",
                    "List<org.knime.core.test.entity.TestEnt>", new MethodParam(
                        "Some example documentation.",
                        "list", "List<org.knime.core.test.entity.TestEnt>")),
                new ServiceMethod(
                    "primitives",
                    ServiceMethod.Operation.Get, "Some example documentation.",
                    "double",
                    new MethodParam(
                        "Some example documentation.",
                        "s", "String"), new MethodParam("Some example documentation.",
                        "stringlist", "List<String>"))));
    }

}
