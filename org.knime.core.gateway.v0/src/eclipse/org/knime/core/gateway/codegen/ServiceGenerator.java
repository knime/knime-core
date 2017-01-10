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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.knime.core.gateway.codegen.types.DefaultMethodParam;
import org.knime.core.gateway.codegen.types.DefaultServiceDef;
import org.knime.core.gateway.codegen.types.DefaultServiceMethod;
import org.knime.core.gateway.codegen.types.ServiceDef;

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
                FileWriter fileWriter = new FileWriter(m_destDirectory + destFileName + ".java");
                BufferedWriter writer = new BufferedWriter(fileWriter);

                if (template != null) {
                    template.merge(context, writer);
                }

                /*
                 *  flush and cleanup
                 */

                writer.flush();
                writer.close();
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }



    public static List<ServiceDef> getServiceDefs() {
        return Arrays.asList(
            new DefaultServiceDef("WorkflowService",
                new DefaultServiceMethod("getWorkflow", "WorkflowEnt",
                    new DefaultMethodParam("id", "EntityID")),
                new DefaultServiceMethod("updateWorkflow", "void",
                    new DefaultMethodParam("wf", "WorkflowEnt")),
                new DefaultServiceMethod("getAllWorkflows", "List<EntityID>"))
            .addImports(
                "org.knime.core.gateway.v0.workflow.entity.EntityID",
                "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt",
                "java.util.List"),
            new DefaultServiceDef("NodeService",
                new DefaultServiceMethod("getNodeRepository", "List<RepoCategoryEnt>"),
                new DefaultServiceMethod("getNodeDescription", "String",
                    new DefaultMethodParam("factoryID", "NodeFactoryIDEnt")))
            .addImports("java.util.List", "org.knime.core.gateway.v0.workflow.entity.RepoCategoryEnt", "org.knime.core.gateway.v0.workflow.entity.NodeFactoryIDEnt"),
            new DefaultServiceDef("ExecutionService",
                new DefaultServiceMethod("canExecuteUpToHere", "boolean",
                    new DefaultMethodParam("workflowID", "EntityID"),
                    new DefaultMethodParam("nodeID", "String")),
                new DefaultServiceMethod("executeUpToHere", "WorkflowEnt",
                    new DefaultMethodParam("workflowID", "EntityID"),
                    new DefaultMethodParam("nodeID", "String")))
            .addImports("org.knime.core.gateway.v0.workflow.entity.EntityID", "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt"),
            new DefaultServiceDef("TestService",
                new DefaultServiceMethod("test", "TestEnt",
                    new DefaultMethodParam("id", "TestEnt")),
                new DefaultServiceMethod("testList", "List<TestEnt>",
                    new DefaultMethodParam("list", "List<TestEnt>")),
                new DefaultServiceMethod("primitives", "double",
                    new DefaultMethodParam("s", "String"),
                    new DefaultMethodParam("stringlist", "List<String>")))
            .addImports("org.knime.core.gateway.v0.workflow.entity.TestEnt",
                        "java.util.List"));
    }

}
