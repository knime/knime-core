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
import java.io.FileReader;
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
import org.knime.core.gateway.codegen.types.EntityDef;
import org.knime.core.gateway.codegen.types.EntityField;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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

            Map<String, EntityDef> entitiyDefMap = new HashMap<String, EntityDef>();
            for(EntityDef entityDef : entityDefs) {
                entitiyDefMap.put(entityDef.getName(), entityDef);
            }

            for (EntityDef entityDef : entityDefs) {
                context.put("name", entityDef.getName());

                List<EntityField> fields = new ArrayList<EntityField>(entityDef.getFields());
                List<String> imports = new ArrayList<String>(entityDef.getImports());
                List<String> superClasses = new ArrayList<String>();

                //add fields and imports from other entities, too
                for(String other : entityDef.getCommonEntities()) {
                    fields.addAll(entitiyDefMap.get(other).getFields());
                    imports.addAll(entitiyDefMap.get(other).getImports());
                    superClasses.add(other);
                }

                context.put("fields", fields);

                context.put("imports", imports);

                context.put("superClasses", superClasses);

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
                String path = "api/entities/" + destFileName + ".json";
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(writer, entityDef);
                    EntityDef def1 = mapper.readValue(new FileReader(path), EntityDef.class);
                }


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
            new EntityDef("NodeEnt",
                new EntityField("Parent", "EntityID"),
                new EntityField("JobManager", "JobManagerEnt"),
                new EntityField("NodeMessage", "NodeMessageEnt"),
                new EntityField("InPorts", "List<NodeInPortEnt>"),
                new EntityField("OutPorts", "List<NodeOutPortEnt>"),
                new EntityField("Name", "String"),
                new EntityField("NodeID", "String"),
                new EntityField("NodeTypeID", "String"),
                new EntityField("NodeType", "String"),
                new EntityField("Bounds", "BoundsEnt"),
                new EntityField("IsDeletable", "boolean"),
                new EntityField("NodeState", "String"),
                new EntityField("HasDialog", "boolean"),
                new EntityField("NodeAnnotation", "NodeAnnotationEnt"))
                .addImports(
                    "org.knime.core.gateway.v0.workflow.entity.EntityID",
                    "org.knime.core.gateway.v0.workflow.entity.JobManagerEnt",
                    "org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt",
                    "org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt",
                    "org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt",
                    "org.knime.core.gateway.v0.workflow.entity.BoundsEnt",
                    "org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt",
                    "java.util.List"),
            new EntityDef("NativeNodeEnt",
                new EntityField("NodeFactoryID", "NodeFactoryIDEnt"))
                .addFieldsFrom("NodeEnt")
                .addImports("org.knime.core.gateway.v0.workflow.entity.NodeFactoryIDEnt"),
            new EntityDef("NodeFactoryIDEnt",
                new EntityField("ClassName", "String"),
                new EntityField("NodeName", "String")),
            new EntityDef("ConnectionEnt",
                new EntityField("Dest", "String"),
                new EntityField("DestPort", "int"),
                new EntityField("Source", "String"),
                new EntityField("SourcePort", "int"),
                new EntityField("IsDeleteable", "boolean"),
                new EntityField("BendPoints", "List<XYEnt>"),
                new EntityField("Type", "String"))
                .addImports("org.knime.core.gateway.v0.workflow.entity.XYEnt",
                    "java.util.List"),
            new EntityDef("NodePortEnt",
                new EntityField("PortIndex", "int"),
                new EntityField("PortType", "PortTypeEnt"),
                new EntityField("PortName", "String"))
                .addImports("org.knime.core.gateway.v0.workflow.entity.PortTypeEnt"),
            new EntityDef("NodeInPortEnt").addFieldsFrom("NodePortEnt").addImports("org.knime.core.gateway.v0.workflow.entity.NodePortEnt"),
            new EntityDef("NodeOutPortEnt").addFieldsFrom("NodePortEnt").addImports("org.knime.core.gateway.v0.workflow.entity.NodePortEnt"),
            new EntityDef("PortTypeEnt",
                new EntityField("Name", "String"),
                new EntityField("PortObjectClassName", "String"),
                new EntityField("IsOptional", "boolean"),
                new EntityField("Color", "int"),
                new EntityField("IsHidden", "boolean")),
            new EntityDef("NodeMessageEnt",
                new EntityField("Message", "String"),
                new EntityField("Type", "String")),
            new EntityDef("JobManagerEnt",
                new EntityField("Name", "String"),
                new EntityField("JobManagerID", "String")),
            new EntityDef("BoundsEnt",
                new EntityField("X", "int"),
                new EntityField("Y", "int"),
                new EntityField("Width", "int"),
                new EntityField("Height", "int")),
            new EntityDef("XYEnt",
                new EntityField("X", "int"),
                new EntityField("Y", "int")),
            new EntityDef("WorkflowAnnotationEnt",
                new EntityField("Text", "String"),
                new EntityField("Bounds", "BoundsEnt"),
                new EntityField("BgColor", "int"),
                new EntityField("BorderSize", "int"),
                new EntityField("BorderColor", "int"),
                new EntityField("FontSize", "int"),
                new EntityField("Alignment", "String"))
                .addImports("org.knime.core.gateway.v0.workflow.entity.BoundsEnt"),
            new EntityDef("WorkflowEnt",
                new EntityField("Nodes", "Map<String, NodeEnt>"),
                new EntityField("Connections", "List<ConnectionEnt>"),
                new EntityField("MetaInPorts", "List<MetaPortEnt>"),
                new EntityField("MetaOutPorts", "List<MetaPortEnt>"))
                .addImports("java.util.List", "java.util.Map", "org.knime.core.gateway.v0.workflow.entity.ConnectionEnt", "org.knime.core.gateway.v0.workflow.entity.NodeEnt", "org.knime.core.gateway.v0.workflow.entity.MetaPortEnt")
                .addFieldsFrom("NodeEnt"),
            new EntityDef("EntityID",
                new EntityField("ID", "String"),
                new EntityField("Type", "String")),
            new EntityDef("AnnotationEnt",
                new EntityField("Text", "String"),
                new EntityField("BackgroundColor", "int"),
                new EntityField("X", "int"),
                new EntityField("Y", "int"),
                new EntityField("Width", "int"),
                new EntityField("Height", "int"),
                new EntityField("TextAlignment", "String"),
                new EntityField("BorderSize", "int"),
                new EntityField("BorderColor", "int"),
                new EntityField("DefaultFontSize", "int"),
                new EntityField("Version", "int")),
            new EntityDef("NodeAnnotationEnt",
                new EntityField("Node", "String"))
                .addFieldsFrom("AnnotationEnt"),
            new EntityDef("MetaPortEnt",
                new EntityField("PortType", "PortTypeEnt"),
                new EntityField("IsConnected", "boolean"),
                new EntityField("Message", "String"),
                new EntityField("OldIndex", "int"),
                new EntityField("NewIndex", "int"))
            .addImports("org.knime.core.gateway.v0.workflow.entity.PortTypeEnt"),
            new EntityDef("RepoCategoryEnt",
                new EntityField("Name", "String"),
                new EntityField("IconURL", "String"),
                new EntityField("Categories", "List<RepoCategoryEnt>"),
                new EntityField("Nodes", "List<RepoNodeTemplateEnt>"))
            .addImports("java.util.List", "org.knime.core.gateway.v0.workflow.entity.RepoNodeTemplateEnt"),
            new EntityDef("RepoNodeTemplateEnt",
                new EntityField("Name", "String"),
                new EntityField("Type", "String"),
                new EntityField("ID", "String"),
                new EntityField("IconURL", "String"),
                new EntityField("NodeTypeID", "String")),
            new EntityDef("TestEnt",
                new EntityField("xy", "XYEnt"),
                new EntityField("xylist", "List<XYEnt>"),
                new EntityField("other", "String"),
                new EntityField("primitivelist", "List<String>"),
                new EntityField("xymap", "Map<String, XYEnt>"),
                new EntityField("primitivemap", "Map<Integer, String>"))
            .addImports("org.knime.core.gateway.v0.workflow.entity.XYEnt",
                    "java.util.List", "java.util.Map"));
    }
}
