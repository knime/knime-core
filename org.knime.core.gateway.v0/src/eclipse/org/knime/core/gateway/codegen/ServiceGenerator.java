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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.knime.core.gateway.codegen.spec.ObjectSpec;
import org.knime.core.gateway.codegen.types.MethodParam;
import org.knime.core.gateway.codegen.types.ObjectDef;
import org.knime.core.gateway.codegen.types.ServiceDef;
import org.knime.core.gateway.codegen.types.ServiceMethod;

/**
 * Generates java source files (classes or interfaces) from a velocity template and service definitions (that are read in
 * from the respective json-files).
 *
 * @author Martin Horn, University of Konstanz
 */
public class ServiceGenerator extends SourceFileGenerator {

    private final String m_outputFolder;

    private final String m_templateFile;

    private ObjectSpec m_serviceSpec;

    private ObjectSpec[] m_dependentEntitySpecs = new ObjectSpec[0];

    private ObjectSpec[] m_dependentServiceSpecs = new ObjectSpec[0];

    /**
     * @param outputFolder the folder the files are written to
     * @param templateFile the velocity template file
     * @param serviceSpec an object specification (i.e. the actual package name, class name pattern, etc.)
     *
     */
    public ServiceGenerator(final String outputFolder, final String templateFile, final ObjectSpec serviceSpec) {
        m_outputFolder = outputFolder;
        m_templateFile = templateFile;
        m_serviceSpec = serviceSpec;
    }


    /**
     * Additional specs for 'this' service to be imported (each spec is one additional import).
     *
     * @param serviceSpecs
     * @return this
     */
    public ServiceGenerator setServiceImport(final ObjectSpec... serviceSpecs) {
        m_dependentServiceSpecs = serviceSpecs;
        return this;
    }


    /**
     * Sets the imports specification to be generated for all entity fields.
     *
     * @param entitySpecs
     * @return this
     */
    public ServiceGenerator setEntityFieldsImports(final ObjectSpec... entitySpecs) {
        m_dependentEntitySpecs = entitySpecs;
        return this;
    }

    @Override
    public void generate() {

        try {
            Velocity.init();
            VelocityContext context = new VelocityContext();
            List<ServiceDef> serviceDefs = ObjectDef.readAll(ServiceDef.class);

            for (ServiceDef serviceDef : serviceDefs) {
                context.put("classDescription", serviceDef.getDescription());
                context.put("namespace", serviceDef.getNamespace());
                context.put("name", serviceDef.getName());
                context.put("methods", serviceDef.getMethods());
                Set<String> imports = new HashSet<String>();
                for(ObjectSpec s : m_dependentServiceSpecs) {
                    imports.add(s.getFullyQualifiedName(serviceDef.getNamespace(), serviceDef.getName()));
                }
                for (ObjectSpec t : m_dependentEntitySpecs) {
                    imports.addAll(getImports(serviceDef, t));
                }
                context.put("imports", imports);

                StringBuilder fullPackageStringBuilder = new StringBuilder(m_serviceSpec.getPackagePrefix());
                if (StringUtils.isNotEmpty(serviceDef.getNamespace())) {
                    fullPackageStringBuilder.append('.').append(serviceDef.getNamespace());
                }
                if (StringUtils.isNotEmpty(m_serviceSpec.getPackageSuffix())) {
                    fullPackageStringBuilder.append('.').append(m_serviceSpec.getPackageSuffix());
                }
                final String fullPackageString = fullPackageStringBuilder.toString();
                context.put("package", fullPackageString);

                Template template = null;
                try {
                    template = Velocity.getTemplate(m_templateFile);
                } catch (ResourceNotFoundException rnfe) {
                    System.out.println("Example : error : cannot find template " + m_templateFile);
                } catch (ParseErrorException pee) {
                    System.out.println("Example : Syntax error in template " + m_templateFile + ":" + pee);
                }

                /*
                 *  Now have the template engine process your template using the
                 *  data placed into the context.  Think of it as a  'merge'
                 *  of the template and the data to produce the output stream.
                 */
                String[] packages = fullPackageString.split("\\.");
                Path outputPath = Paths.get(m_outputFolder, packages);
                Files.createDirectories(outputPath);

                String finalSimpleClassName = m_serviceSpec.getPattern().replace("##name##", serviceDef.getName());

                try (BufferedWriter writer =
                    Files.newBufferedWriter(outputPath.resolve(finalSimpleClassName + ".java"))) {
                    if (template != null) {
                        template.merge(context, writer);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private List<String> getImports(final ServiceDef serviceDef, final ObjectSpec entitySpec) {
        List<ServiceMethod> methods = serviceDef.getMethods();
        return methods.stream().flatMap(m -> getImports(m, entitySpec)).sorted().distinct()
            .collect(Collectors.toList());
    }

    private Stream<String> getImports(final ServiceMethod serviceMethod, final ObjectSpec entitySpec) {
        return Stream.concat(serviceMethod.getResult().getType().getAllImports(entitySpec).stream(), serviceMethod
            .getParameters().stream().map(MethodParam::getType).flatMap(t -> t.getAllImports(entitySpec).stream()));
    }

    //    public static List<ServiceDef> getServiceDefs() {
    //        return Arrays.asList(
    //            new ServiceDef(
    //                "Defines service methods to query and manipulate the content of a workflow.",
    //                "org.knime.core.gateway.v0.workflow.services", "WorkflowService",
    //                new ServiceMethod(
    //                    "Workflow",
    //                    ServiceMethod.Operation.Get, "Get the workflow entity for a given ID.",
    //                    "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt", new MethodParam("The identifier as per #getAllWorkflows", "id", "org.knime.core.gateway.v0.workflow.entity.EntityID")),
    //                new ServiceMethod(
    //                    "Workflow",
    //                    ServiceMethod.Operation.Update, "Trigger an update of the given workflow (@Martin, what's that mean?)",
    //                    "void", new MethodParam("The workflow to be updated", "wf", "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt")),
    //                new ServiceMethod(
    //                    "AllWorkflows",
    //                    ServiceMethod.Operation.Get, "Get a list of IDs for all known workflows", "List<org.knime.core.gateway.v0.workflow.entity.EntityID>")),
    //            new ServiceDef(
    //                "Defines service methods to query the list of available nodes and metanodes (node repository).",
    //                "org.knime.core.gateway.v0.repository.services", "RepositoryService",
    //                new ServiceMethod(
    //                    "NodeRepository",
    //                    ServiceMethod.Operation.Get,
    //                    "Get the list of nodes in the node repository, categories and metanodes included.", "List<org.knime.core.gateway.v0.workflow.entity.RepoCategoryEnt>"),
    //                new ServiceMethod(
    //                    "NodeDescription",
    //                    ServiceMethod.Operation.Get, "Get the node description (html) for a given node",
    //                    "String", new MethodParam(
    //                        "The identifier for a given node",
    //                        "nodeTypeID", "String"))),
    //            new ServiceDef(
    //                "Defines service methods to query details on an individual node or metanode.",
    //                "org.knime.core.gateway.v0.workflow.services", "NodeContainerService",
    //                new ServiceMethod(
    //                    "NodeSettingsJSON",
    //                    ServiceMethod.Operation.Get, "Get the settings tree for a given node in a JSON structure.",
    //                    "String",
    //                    new MethodParam(
    //                        "The id of the workflow the node is in.",
    //                        "workflowID", "org.knime.core.gateway.v0.workflow.entity.EntityID"), new MethodParam(
    //                        "The id of the node itself.",
    //                        "nodeID", "String"))),
    //            new ServiceDef(
    //                "Defines service methods run or reset a workflow.",
    //                "org.knime.core.gateway.v0.workflow.services", "ExecutionService",
    //                new ServiceMethod(
    //                    "canExecuteUpToHere",
    //                    ServiceMethod.Operation.Get, "Test if a given node can be executed (e.g. not yet executed and not executing).",
    //                    "boolean",
    //                    new MethodParam(
    //                        "The id of the workflow the node is in.",
    //                        "workflowID", "org.knime.core.gateway.v0.workflow.entity.EntityID"), new MethodParam(
    //                        "The id of the node itself.",
    //                        "nodeID", "String")),
    //                new ServiceMethod(
    //                    "executeUpToHere",
    //                    ServiceMethod.Operation.Set, "Trigger an execution of a single node or a chain of nodes (endpoint reference).",
    //                    "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt",
    //                    new MethodParam(
    //                        "The id of the workflow the node is in.",
    //                        "workflowID", "org.knime.core.gateway.v0.workflow.entity.EntityID"), new MethodParam(
    //                        "The id of the node itself.",
    //                        "nodeID", "String"))),
    //            new ServiceDef(
    //                "A simple test service.",
    //                "org.knime.core.test.services", "TestService",
    //                new ServiceMethod(
    //                    "test",
    //                    ServiceMethod.Operation.Get, "Some example documentation.",
    //                    "org.knime.core.test.entity.TestEnt", new MethodParam(
    //                        "Some example documentation.",
    //                        "id", "org.knime.core.test.entity.TestEnt")),
    //                new ServiceMethod(
    //                    "testList",
    //                    ServiceMethod.Operation.Get, "Some example documentation.",
    //                    "List<org.knime.core.test.entity.TestEnt>", new MethodParam(
    //                        "Some example documentation.",
    //                        "list", "List<org.knime.core.test.entity.TestEnt>")),
    //                new ServiceMethod(
    //                    "primitives",
    //                    ServiceMethod.Operation.Get, "Some example documentation.",
    //                    "double",
    //                    new MethodParam(
    //                        "Some example documentation.",
    //                        "s", "String"), new MethodParam("Some example documentation.",
    //                        "stringlist", "List<String>"))));
    //    }

}
