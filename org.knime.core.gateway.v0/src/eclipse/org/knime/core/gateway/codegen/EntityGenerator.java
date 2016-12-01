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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * NOTES:
 * there are no inheritance hierarchies among entities. However, to avoid the repeated definition of fields, they can be taken over from other entities via ...
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

            Map<String, EntityDef> entitiyDefMap = new HashMap<String, EntityGenerator.EntityDef>();
            for(EntityDef entityDef : entityDefs) {
                entitiyDefMap.put(entityDef.getName(), entityDef);
            }

            for (EntityDef entityDef : entityDefs) {
                context.put("name", entityDef.getName());

                List<EntityField> fields = new ArrayList<EntityGenerator.EntityField>(entityDef.getFields());
                List<String> imports = new ArrayList<String>(entityDef.getImports());

                //add fields and imports from other entities, too
                for(String other : entityDef.getCommonEntities()) {
                    fields.addAll(entitiyDefMap.get(other).getFields());
                    imports.addAll(entitiyDefMap.get(other).getImports());
                }

                context.put("fields", fields);

                context.put("imports", imports);

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
                new DefaultEntityField("Parent", "EntityID"),
                new DefaultEntityField("JobManager", "JobManagerEnt"),
                new DefaultEntityField("NodeMessage", "NodeMessageEnt"),
                new DefaultEntityField("InPorts", "List<NodeInPortEnt>"),
                new DefaultEntityField("OutPorts", "List<NodeOutPortEnt>"),
                new DefaultEntityField("Name", "String"),
                new DefaultEntityField("NodeID", "String"),
                new DefaultEntityField("NodeType", "String"),
                new DefaultEntityField("Bounds", "BoundsEnt"),
                new DefaultEntityField("IsDeletable", "boolean"),
                new DefaultEntityField("NodeState", "String"))
                .addImports(
                    "org.knime.core.gateway.v0.workflow.entity.EntityID",
                    "org.knime.core.gateway.v0.workflow.entity.JobManagerEnt",
                    "org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt",
                    "org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt",
                    "org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt",
                    "org.knime.core.gateway.v0.workflow.entity.BoundsEnt",
                    "java.util.List"),
            new DefaultEntityDef("NativeNodeEnt",
                new DefaultEntityField("NodeFactoryID", "NodeFactoryIDEnt"))
                .addFieldsFrom("NodeEnt")
                .addImports("org.knime.core.gateway.v0.workflow.entity.NodeFactoryIDEnt"),
            new DefaultEntityDef("NodeFactoryIDEnt",
                new DefaultEntityField("ClassName", "String"),
                new DefaultEntityField("NodeName", "String")),
            new DefaultEntityDef("ConnectionEnt",
                new DefaultEntityField("Dest", "EntityID"),
                new DefaultEntityField("DestPort", "int"),
                new DefaultEntityField("Source", "EntityID"),
                new DefaultEntityField("SourcePort", "int"),
                new DefaultEntityField("IsDeleteable", "boolean"),
                new DefaultEntityField("BendPoints", "List<XYEnt>"),
                new DefaultEntityField("Type", "String"))
                .addImports("org.knime.core.gateway.v0.workflow.entity.EntityID",
                    "org.knime.core.gateway.v0.workflow.entity.XYEnt",
                    "java.util.List"),
            new DefaultEntityDef("NodePortEnt",
                new DefaultEntityField("PortIndex", "int"),
                new DefaultEntityField("PortType", "PortTypeEnt"),
                new DefaultEntityField("PortName", "String"))
                .addImports("org.knime.core.gateway.v0.workflow.entity.PortTypeEnt"),
            new DefaultEntityDef("NodeInPortEnt").addFieldsFrom("NodePortEnt").addImports("org.knime.core.gateway.v0.workflow.entity.NodePortEnt"),
            new DefaultEntityDef("NodeOutPortEnt").addFieldsFrom("NodePortEnt").addImports("org.knime.core.gateway.v0.workflow.entity.NodePortEnt"),
            new DefaultEntityDef("PortTypeEnt",
                new DefaultEntityField("Name", "String"),
                new DefaultEntityField("PortObjectClassName", "String"),
                new DefaultEntityField("IsOptional", "boolean"),
                new DefaultEntityField("Color", "int"),
                new DefaultEntityField("IsHidden", "boolean")),
            new DefaultEntityDef("NodeMessageEnt",
                new DefaultEntityField("Message", "String"),
                new DefaultEntityField("Type", "String")),
            new DefaultEntityDef("JobManagerEnt",
                new DefaultEntityField("Name", "String"),
                new DefaultEntityField("JobManagerID", "String")),
            new DefaultEntityDef("BoundsEnt",
                new DefaultEntityField("X", "int"),
                new DefaultEntityField("Y", "int"),
                new DefaultEntityField("Width", "int"),
                new DefaultEntityField("Height", "int")),
            new DefaultEntityDef("XYEnt",
                new DefaultEntityField("X", "int"),
                new DefaultEntityField("Y", "int")),
            new DefaultEntityDef("WorkflowAnnotationEnt",
                new DefaultEntityField("Text", "String"),
                new DefaultEntityField("Bounds", "BoundsEnt"),
                new DefaultEntityField("BgColor", "int"),
                new DefaultEntityField("BorderSize", "int"),
                new DefaultEntityField("BorderColor", "int"),
                new DefaultEntityField("FontSize", "int"),
                new DefaultEntityField("Alignment", "String"))
                .addImports("org.knime.core.gateway.v0.workflow.entity.BoundsEnt"),
            new DefaultEntityDef("WorkflowEnt",
                new DefaultEntityField("Nodes", "List<NodeEnt>"),
                new DefaultEntityField("Connections", "List<ConnectionEnt>"))
                .addImports("java.util.List", "org.knime.core.gateway.v0.workflow.entity.ConnectionEnt", "org.knime.core.gateway.v0.workflow.entity.NodeEnt"));
    }


    public static interface EntityDef {

        String getName();

        List<EntityField> getFields();

        List<String> getCommonEntities();

        List<String> getImports();

    }

    public static interface EntityField {

        String getName();

        String getType();
    }

    private static class DefaultEntityDef implements EntityDef {

        private List<EntityField> m_fields;

        private String m_name;

        private List<String> m_commonEntities = new ArrayList<String>();

        private List<String> m_imports = new ArrayList<String>();

        /**
         *
         */
        public DefaultEntityDef(final String name, final EntityField... entityFields) {
            m_fields = Arrays.asList(entityFields);
            m_name = name;
        }

        public DefaultEntityDef addFieldsFrom(final String... entities) {
            for(String s : entities) {
                m_commonEntities.add(s);
            }
            return this;
        }

        public DefaultEntityDef addImports(final String... imports) {
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
        public List<EntityField> getFields() {
            return m_fields;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getCommonEntities() {
            return m_commonEntities;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getImports() {
            return m_imports;
        }
    }

    private static class DefaultEntityField implements EntityField {

        private String m_name;

        private String m_type;

        /**
         *
         */
        public DefaultEntityField(final String name, final String type) {
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
