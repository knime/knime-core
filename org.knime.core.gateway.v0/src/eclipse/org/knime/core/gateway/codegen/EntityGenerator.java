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
 *   Nov 30, 2016 (hornm): created
 */
package org.knime.core.gateway.codegen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public final class EntityGenerator {

    private String m_templateFile;

    private String m_destDirectory;

    private String m_destFileName;

    /**
     * @param templateFile the template file
     * @param destDirectory the destination directory for the generated java-files
     * @param destFileName the name of the destination file. The placeholder "##entityName##" will be replaced with the
     *            actual entity name.
     *
     */
    public EntityGenerator(final String templateFile, final String destDirectory, final String destFileName) {
        m_templateFile = templateFile;
        m_destDirectory = destDirectory;
        m_destFileName = destFileName;
    }

    public void generate() {

        try {
            Velocity.init();
            VelocityContext context = new VelocityContext();
            List<EntityDef> entityDefs = getEntityDefs();

            for (EntityDef entityDef : entityDefs) {
                context.put("name", entityDef.getName());
                context.put("properties", entityDef.getProperties());

                //TODO: support multiple super classes
                context.put("hasSuperClass", entityDef.getSuperClasses().size() > 0);
                if(entityDef.getSuperClasses().size() > 0) {
                    context.put("superClass", entityDef.getSuperClasses().get(0));
                }

                //TODO keep a map of imports and add them to the context

                Template template = null;
                try {
                    template = Velocity.getTemplate(m_templateFile);
                } catch (ResourceNotFoundException rnfe) {
                    System.out.println("Example : error : cannot find template " + m_templateFile);
                } catch (ParseErrorException pee) {
                    System.out.println("Example : Syntax error in template " + m_templateFile + ":" + pee);
                }

                String destFileName = m_destFileName.replace("##entityName##", entityDef.getName());

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

    public static List<EntityDef> getEntityDefs() {
        //TODO: e.g. read from a json file

        return Arrays.asList(
            new DefaultEntityDef("NodeEnt",
                new DefaultEntityProperty("Parent", "WorkflowEntID"),
                new DefaultEntityProperty("JobManager", "JobManagerEnt"),
                new DefaultEntityProperty("NodeMessage", "NodeMessageEnt"),
                new DefaultEntityProperty("InPorts", "List<NodeInPortEnt>"),
                new DefaultEntityProperty("OutPorts", "List<NodeOutPortEnt>"),
                new DefaultEntityProperty("Name", "String"),
                new DefaultEntityProperty("NodeID", "String"),
                new DefaultEntityProperty("NodeType", "String"),
                new DefaultEntityProperty("Bounds", "BoundsEnt"),
                new DefaultEntityProperty("IsDeletable", "boolean")),
            new DefaultEntityDef("NativeNodeEnt",
                new DefaultEntityProperty("NodeFactoryID", "NodeFactoryIDEnt")).addSuperClass("NodeEnt"),
            new DefaultEntityDef("NodeFactoryIDEnt",
                new DefaultEntityProperty("ClassName", "String"),
                new DefaultEntityProperty("NodeName", "String")),
            new DefaultEntityDef("ConnectionEnt",
                new DefaultEntityProperty("Dest", "EntityID"),
                new DefaultEntityProperty("DestPort", "int"),
                new DefaultEntityProperty("Source", "int"),
                new DefaultEntityProperty("SourcePort", "int"),
                new DefaultEntityProperty("IsDeleteable", "boolean"),
                new DefaultEntityProperty("BendPoints", "List<XYEnt>"),
                new DefaultEntityProperty("Type", "String")),
            new DefaultEntityDef("NodePortEnt",
                new DefaultEntityProperty("PortIndex", "int"),
                new DefaultEntityProperty("PortType", "PortTypeEnt"),
                new DefaultEntityProperty("PortName", "String")),
            new DefaultEntityDef("NodeInPortEnt").addSuperClass("NodePortEnt"),
            new DefaultEntityDef("NodeOutPortEnt").addSuperClass("NodePortEnt"),
            new DefaultEntityDef("PortTypeEnt",
                new DefaultEntityProperty("Name", "String"),
                new DefaultEntityProperty("PortObjectClassName", "String"),
                new DefaultEntityProperty("IsOptional", "boolean"),
                new DefaultEntityProperty("Color", "int"),
                new DefaultEntityProperty("IsHidden", "boolean")),
            new DefaultEntityDef("NodeMessageEnt",
                new DefaultEntityProperty("Message", "String"),
                new DefaultEntityProperty("Type", "String")),
            new DefaultEntityDef("JobManagerEnt",
                new DefaultEntityProperty("Name", "String"),
                new DefaultEntityProperty("ID", "String")),
            new DefaultEntityDef("BoundsEnt",
                new DefaultEntityProperty("X", "int"),
                new DefaultEntityProperty("Y", "int"), new DefaultEntityProperty("Width", "int"),
                new DefaultEntityProperty("Height", "int")),
            new DefaultEntityDef("XY",
                new DefaultEntityProperty("X", "int"),
                new DefaultEntityProperty("Y", "int")),
            new DefaultEntityDef("WorkflowAnnotationEnt",
                new DefaultEntityProperty("Text", "String"),
                new DefaultEntityProperty("Bounds", "BoundsEnt"),
                new DefaultEntityProperty("BgColor", "int"),
                new DefaultEntityProperty("BorderSize", "int"),
                new DefaultEntityProperty("BorderColor", "int"),
                new DefaultEntityProperty("FontSize", "int"),
                new DefaultEntityProperty("Alignment", "String")),
            new DefaultEntityDef("WorkflowEnt",
                new DefaultEntityProperty("Nodes", "List<NodeEnt>"),
                new DefaultEntityProperty("Connections", "List<ConnectionEnt>")));
    }

    public static Map<String, String> getImportsMap() {
        //TODO
        return null;
    }

    public static interface EntityDef {

        String getName();

        List<EntityProperty> getProperties();

        List<String> getSuperClasses();

    }

    public static interface EntityProperty {

        String getName();

        String getType();
    }

    private static class DefaultEntityDef implements EntityDef {

        private List<EntityProperty> m_properties;

        private String m_name;

        private List<String> m_superClasses = new ArrayList<String>();

        /**
         *
         */
        public DefaultEntityDef(final String name, final EntityProperty... entityProperties) {
            m_properties = Arrays.asList(entityProperties);
            m_name = name;
        }

        public EntityDef addSuperClass(final String clazz) {
            m_superClasses.add(clazz);
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
        public List<EntityProperty> getProperties() {
            return m_properties;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getSuperClasses() {
            return m_superClasses;
        }
    }

    private static class DefaultEntityProperty implements EntityProperty {

        private String m_name;

        private String m_type;

        /**
         *
         */
        public DefaultEntityProperty(final String name, final String type) {
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

}
