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
                new DefaultServiceMethod("getWorkflow",
                    new DefaultReturnType("WorkflowEnt", false),
                    new DefaultMethodParam("id", "EntityID")),
                new DefaultServiceMethod("updateWorkflow", DefaultReturnType.VOID,
                    new DefaultMethodParam("wf", "WorkflowEnt")),
                new DefaultServiceMethod("getAllWorkflows", new DefaultReturnType("EntityID", true)))
            .addImports(
                "org.knime.core.gateway.v0.workflow.entity.EntityID",
                "org.knime.core.gateway.v0.workflow.entity.WorkflowEnt",
                "java.util.List"));
    }

    public static interface ServiceDef {

        String getName();

        List<ServiceMethod> getMethods();

        List<String> getImports();

    }

    public static interface ServiceMethod {

        String getName();

        ReturnType getReturnType();

        List<MethodParam> getParameters();

    }

    public static interface MethodParam {

        String getName();

        String getType();
    }

    public static interface ReturnType {

        String getType();

        boolean isList();

        boolean isVoid();
    }

    private static class DefaultServiceDef implements ServiceDef {

        private String m_name;
        private List<ServiceMethod> m_methods;
        private List<String> m_imports = new ArrayList<String>();

        /**
         *
         */
        public DefaultServiceDef(final String name, final ServiceMethod... methods) {
            m_name = name;
            m_methods = Arrays.asList(methods);
        }

        public DefaultServiceDef addImports(final String... imports) {
            for(String s : imports) {
                m_imports.add(s);
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return m_name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<ServiceMethod> getMethods() {
            return m_methods;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getImports() {
            return m_imports;
        }

    }

    private static class DefaultServiceMethod implements ServiceMethod {

        private String m_name;
        private ReturnType m_returnType;
        private List<MethodParam> m_parameters;

        /**
         *
         */
        public DefaultServiceMethod(final String name, final ReturnType returnType, final MethodParam... parameters) {
            m_name = name;
            m_returnType = returnType;
            m_parameters = Arrays.asList(parameters);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return m_name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ReturnType getReturnType() {
            return m_returnType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<MethodParam> getParameters() {
            return m_parameters;
        }

    }

    private static class DefaultMethodParam implements MethodParam {

        private String m_name;
        private String m_type;

        /**
         *
         */
        public DefaultMethodParam(final String name, final String type) {
            m_name = name;
            m_type = type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return m_name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getType() {
            return m_type;
        }
    }

    private static class DefaultReturnType implements ReturnType {

        public static final ReturnType VOID = new DefaultReturnType(null, false);

        private String m_type;
        private boolean m_isList;

        /**
         * @param type <code>null</code> if void
         * @param isList
         *
         */
        public DefaultReturnType(final String type, final boolean isList) {
            m_type = type;
            m_isList = isList;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getType() {
            return m_type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isList() {
            return m_isList;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isVoid() {
            return m_type == null;
        }


    }

}
