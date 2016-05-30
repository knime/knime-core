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
 *   Mar 16, 2016 (hornm): created
 */
package org.knime.workbench.workflowcoach.data;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.node.NodeTriple;
import org.knime.workbench.workflowcoach.NodeRecommendationManager.NodeRecommendation;
import org.knime.workbench.workflowcoach.ui.WorkflowCoachView;

/**
 * Class that provides triples of nodes and their count. The statistics of node triples are used to generate
 * {@link NodeRecommendation}s, e.g. based on a currently selected node. Those recommendations are displayed in a table
 * within the {@link WorkflowCoachView}.
 *
 * @author Martin Horn, University of Konstanz
 */
public interface NodeTripleProvider {

    /**
     * @return a name for the provider (that will, e.g., appear in the header of the frequency column in the node
     *         recommendation table
     */
    String getName();

    /**
     * A short description of the node triple provide, e.g. where it is generated from etc. It appears, e.g. as tool tip
     * of the frequency column header.
     *
     * @return the description
     */
    String getDescription();

    /**
     * The ID of the preference page used to configure the particular node provide. If null, no preference page exists.
     *
     * @return the preference page id
     */
    String getPreferencePageID();

    /**
     * @return whether the triple provider is enabled. This is usually configured by the user via the provider's preference page
     */
    boolean isEnabled();

    /**
     * Returns all available {@link NodeTriple}s as a {@link Stream}.
     *
     * @return a stream of all {@link NodeTriple}s
     * @throws IOException a possible exception thrown usually when something went wrong to access the underlying source
     *             of the node triples (e.g. a corrupt file)
     */
     Stream<NodeTriple> getNodeTriples() throws IOException;

     /**
      * Returns the time when this provider was last updated. If the provider hasn't been updated at all (i.e. its
      * data is missing) then an empty optional is returned.
      *
      * @return the last update time or an empty optional
      */
     Optional<LocalDateTime> getLastUpdate();
}
