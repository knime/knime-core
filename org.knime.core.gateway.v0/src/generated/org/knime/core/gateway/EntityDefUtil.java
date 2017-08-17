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
 */
package org.knime.core.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Utility class that provides programmatic access to entity definitions of this project.
 *
 * @author Martin Horn, University of Konstanz
 */
public class EntityDefUtil {

    private static List<Pair<String, String>> ENTITY_DEFS;

    static {
        List<Pair<String, String>> list = new ArrayList<>();
        list.add(Pair.of("TestEnt", "test.entity"));
        list.add(Pair.of("RepoCategoryEnt", "node.entity"));
        list.add(Pair.of("RepoNodeTemplateEnt", "node.entity"));
        list.add(Pair.of("MetaPortInfoEnt", "workflow.entity"));
        list.add(Pair.of("WorkflowUIInfoEnt", "workflow.entity"));
        list.add(Pair.of("BoundsEnt", "workflow.entity"));
        list.add(Pair.of("ConnectionEnt", "workflow.entity"));
        list.add(Pair.of("NodeEnt", "workflow.entity"));
        list.add(Pair.of("WorkflowAnnotationEnt", "workflow.entity"));
        list.add(Pair.of("NodeAnnotationEnt", "workflow.entity"));
        list.add(Pair.of("WorkflowNodeEnt", "workflow.entity"));
        list.add(Pair.of("NodeInPortEnt", "workflow.entity"));
        list.add(Pair.of("NativeNodeEnt", "workflow.entity"));
        list.add(Pair.of("NodeOutPortEnt", "workflow.entity"));
        list.add(Pair.of("NodeFactoryIDEnt", "workflow.entity"));
        list.add(Pair.of("XYEnt", "workflow.entity"));
        list.add(Pair.of("PortTypeEnt", "workflow.entity"));
        list.add(Pair.of("StyleRangeEnt", "workflow.entity"));
        list.add(Pair.of("AnnotationEnt", "workflow.entity"));
        list.add(Pair.of("JobManagerEnt", "workflow.entity"));
        list.add(Pair.of("WorkflowEnt", "workflow.entity"));
        list.add(Pair.of("NodeMessageEnt", "workflow.entity"));
        list.add(Pair.of("NodePortEnt", "workflow.entity"));
        ENTITY_DEFS = Collections.unmodifiableList(list);
    }

    private EntityDefUtil() {
        // utility class
    }

    /**
     * @return all names and namespaces of the available entities
     */
    public static Collection<Pair<String, String>> getEntities() {
        return ENTITY_DEFS;
    }

}
