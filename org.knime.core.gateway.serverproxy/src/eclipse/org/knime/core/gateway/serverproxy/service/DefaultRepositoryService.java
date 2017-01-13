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
 *   Dec 23, 2016 (hornm): created
 */
package org.knime.core.gateway.serverproxy.service;

import static org.knime.core.gateway.entities.EntityBuilderManager.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.gateway.v0.workflow.entity.RepoCategoryEnt;
import org.knime.core.gateway.v0.workflow.entity.RepoNodeTemplateEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.RepoCategoryEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.RepoNodeTemplateEntBuilder;
import org.knime.core.gateway.v0.workflow.service.RepositoryService;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.DynamicNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.util.NodeFactoryHTMLCreator;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class DefaultRepositoryService implements RepositoryService {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNodeDescription(final String nodeTypeID) {
        //TODO support for dynamic node factory (i.e. id consists of class-name#node-name)
        NodeTemplate nodeTemplate = RepositoryManager.INSTANCE.getNodeTemplate(nodeTypeID);
        try {
            NodeFactory<? extends NodeModel> factoryInstance = nodeTemplate.createFactoryInstance();
            String nodeDescription =
                    NodeFactoryHTMLCreator.instance.readFullDescription(factoryInstance.getXMLDescription());
            return nodeDescription;
        } catch (Exception ex) {
            //TODO better exception handling
            throw new RuntimeException(ex);
        }

    }

    @Override
    public List<RepoCategoryEnt> getNodeRepository() {
        return Arrays.stream(RepositoryManager.INSTANCE.getRoot().getChildren()).map(r -> {
            if(r instanceof Category) {
                return fillNodeRepository((Category) r);
            } else {
                return null;
            }
        }).filter(r -> r != null).collect(Collectors.toList());
    }

    private RepoCategoryEnt fillNodeRepository(final Category root) {
        List<RepoCategoryEnt> cats = new ArrayList<RepoCategoryEnt>();
        List<RepoNodeTemplateEnt> nodes = new ArrayList<RepoNodeTemplateEnt>();
        Arrays.stream(root.getChildren()).forEach(r -> {
            if(r instanceof Category) {
                cats.add(fillNodeRepository((Category) r));
            } else if(r instanceof NodeTemplate) {
                String nodeTypeID = ((NodeTemplate)r).getFactory().getCanonicalName();
                if(r instanceof DynamicNodeTemplate) {
                    nodeTypeID = nodeTypeID +  "#" + r.getName();
                }
                nodes.add(builder(RepoNodeTemplateEntBuilder.class)
                    .setName(r.getName())
                    .setType("TODO")
                    .setID(r.getID())
                    .setIconURL("TODO")
                    .setNodeTypeID(nodeTypeID).build());
            }
         });
        return builder(RepoCategoryEntBuilder.class)
                .setCategories(cats)
                .setNodes(nodes)
                .setName(root.getName())
                .setIconURL("TODO").build();
    }

}
