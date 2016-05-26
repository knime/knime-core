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
 *   Feb 12, 2016 (hornm): created
 */
package org.knime.workbench.workflowcoach;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.knime.core.node.NodeInfo;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeTriple;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.workflowcoach.data.NodeTripleProvider;
import org.knime.workbench.workflowcoach.data.UpdatableNodeTripleProvider;
import org.knime.workbench.workflowcoach.ui.WorkflowCoachView;

/**
 * Class that manages the node recommendations. It represents the node recommendations in memory for quick retrieval and
 * provides them accordingly. The {@link #loadStatistics()}-method updates the statistics, the
 * {@link #getNodeRecommendationFor(NativeNodeContainer...)} gives the actual recommendations.
 *
 * @author Martin Horn, University of Konstanz
 */
public class NodeRecommendationManager {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeRecommendationManager.class);

    private static final String SOURCE_NODES_KEY = "<source_nodes>";

    private static final String NODE_NAME_SEP = "#";

    private static final NodeRecommendationManager INSTANCE = new NodeRecommendationManager();

    private List<Map<String, List<NodeRecommendation>>> m_recommendations;

    private NodeRecommendationManager() {
        //try to load statistics if possible
        try {
            loadStatistics();
        } catch (Exception e) {
            m_recommendations = null;
        }
    }

    /**
     * Returns the singleton instance for this class.
     *
     * @return a singleton instance
     */
    public static NodeRecommendationManager getInstance() {
        return INSTANCE;
    }

    /**
     * (Re-)Loads the statistics for the node recommendation engine from a given set of {@link NodeTripleProvider}s.
     * @throws Exception if something went wrong while loading the statistics (e.g. a corrupt file)
     */
    public void loadStatistics() throws Exception {
        //read from multiple frequency sources
        List<NodeTripleProvider> providers = KNIMEWorkflowCoachPlugin.getDefault().getNodeTripleProviders();
        List<Map<String, List<NodeRecommendation>>> recommendations = new ArrayList<>(providers.size());
        for (NodeTripleProvider provider : providers) {
            if (provider.isEnabled() && !updateRequired(provider)) {
                Map<String, List<NodeRecommendation>> recommendationMap = new HashMap<>();
                recommendations.add(recommendationMap);

                provider.getNodeTriples().forEach(nf -> fillRecommendationsMap(recommendationMap, nf));

                //aggregate multiple occurring id's
                recommendationMap.values().stream().forEach(l -> aggregate(l));
            }
        } //end for
        m_recommendations = recommendations;
    }

    private void fillRecommendationsMap(final Map<String, List<NodeRecommendation>> recommendationMap,
        final NodeTriple nf) {
        /* considering the successor only, i.e. for all entries where the predecessor and the node
         * itself is not present
         */
        if (!nf.getNode().isPresent() && !nf.getPredecessor().isPresent()) {
            add(recommendationMap, SOURCE_NODES_KEY, nf.getSuccessor(), nf.getCount());
        }

        /* considering the the node itself as successor, but only for those nodes that don't have a
         * predecessor -> source nodes, i.e. nodes without an input port
         */
        if (!nf.getPredecessor().isPresent() && nf.getNode().isPresent()) {
            add(recommendationMap, SOURCE_NODES_KEY, nf.getNode().get(), nf.getCount());
        }

        /* without predecessor but with the node, if given*/
        if (nf.getNode().isPresent()) {
            add(recommendationMap, getKey(nf.getNode().get()), nf.getSuccessor(), nf.getCount());
        }

        /* considering predecessor, if given */
        if (nf.getPredecessor().isPresent() && nf.getNode().isPresent()) {
            add(recommendationMap,
                getKey(nf.getPredecessor().get()) + NODE_NAME_SEP + getKey(nf.getNode().get()),
                nf.getSuccessor(), nf.getCount());
        }
    }

    /**
     * Checks whether the given {@link NodeTripleProvider} requires an update.
     *
     * @param ntp the {@link NodeTripleProvider}
     * @return <code>true</code> if an update is required before the ntp can be used
     */
    private boolean updateRequired(final NodeTripleProvider ntp) {
        return (ntp instanceof UpdatableNodeTripleProvider) && ((UpdatableNodeTripleProvider)ntp).updateRequired();
    }

    /**
     * Adds a new node recommendation to the map.
     */
    private void add(final Map<String, List<NodeRecommendation>> recommendation, final String key, final NodeInfo ni,
        final int count) {
        List<NodeRecommendation> p = recommendation.computeIfAbsent(key, k -> new ArrayList<>());
        //create the new node recommendation
        NodeTemplate nt = RepositoryManager.INSTANCE.getNodeTemplate(ni.getFactory());
        if (nt == null) {
            //the node to look for might be a dynamically generated node
            //in that case the node template's id is <node factory-class name>#<node name>
            nt = RepositoryManager.INSTANCE.getNodeTemplate(getKey(ni));
        }
        if (nt == null) {
            LOGGER.info("The node " + ni + " listed in the node recommendation statistics is not installed.");
        } else {
            p.add(new NodeRecommendation(nt, count));
        }
    }

    /**
     * Aggregates multiple occurring id's and takes the mean of the frequencies
     *
     * @param l the list is manipulated directly
     */
    private void aggregate(final List<NodeRecommendation> l) {

        Map<String, NodeRecommendation> aggregates =
            new HashMap<String, NodeRecommendationManager.NodeRecommendation>();
        for (NodeRecommendation np : l) {
            if (aggregates.containsKey(np.toString())) {
                //aggregate
                NodeRecommendation np2 = aggregates.get(np.toString());
                np2.increaseFrequency(np.getFrequency());
            } else {
                aggregates.put(np.toString(), np);
            }
        }
        l.clear();
        l.addAll(aggregates.values());
    }

    /**
     * Determines lists of node recommendation based on the given nodes (e.g. that are selected in the workflow editor).
     * The {@link NodeRecommendation}s are determined based on the statistics of {@link NodeTriple}s (i.e. predecessor,
     * node, successor, count -> (p,n,s,c)) that are provided by {@link NodeTripleProvider}s.
     *
     * Given the list's of node triples, {(predecessor, node, successor, count/frequency)} = {(p,n,s,c)} and given a
     * selected node 'sn', the recommendations are determined for each node-triple-list as follows:
     *
     * (1) find all node triples (p,n,s,c) where n==sn and add them to the result list; in that case the predecessor is
     * essentially ignored and recommendation are determined only based on n. The recommendation is the successor 's'
     * given by each found triple. Since there will be multiple triples for the same 'n' and therewith successor
     * duplicates (i.e. recommendations), those will be joined by taking the mean of the respective frequencies 'c' (2)
     * determine all current predecessors ('sp') of the selected node 'sn' and find all node triples that match the
     * given predecessor-node pairs ('sp','sn') (i.e. 'sp'='p' and 'sn'='n'). The recommended nodes are the successor
     * nodes 's' given by the found triples. Those are added to the same list as the recommendations of (1). (3)
     * Post-processing: duplicate recommendations are resolved by removing the recommendations with a smaller
     * counts/frequencies
     *
     * If the array of given nodes is empty, all potential source nodes are recommended, i.e. all nodes 'n' in the node
     * triples list that don't have a predecessor 'p'.
     *
     * @param nnc if it's an empty array, source nodes only will be recommended, if more than one node is given, the
     *            node recommendations for different nodes will end up in the same list
     * @return an array of lists of node recommendations, i.e. a list of node recommendations for each used node
     *         {@link NodeTripleProvider}. It will return <code>null</code> if something went wrong with loading the
     *         node statistics!
     */
    public List<NodeRecommendation>[] getNodeRecommendationFor(final NativeNodeContainer... nnc) {
        if (m_recommendations == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<NodeRecommendation>[] res = new List[m_recommendations.size()];
        for (int idx = 0; idx < res.length; idx++) {
            if (nnc.length == 0) {
                //recommendations if no node is given -> source nodes are recommended
                res[idx] = m_recommendations.get(idx).get(SOURCE_NODES_KEY);
                if (res[idx] == null) {
                    res[idx] = Collections.emptyList();
                }
            } else if (nnc.length == 1) {
                String nodeID = getKey(nnc[0]);
                Set<NodeRecommendation> set = new HashSet<NodeRecommendationManager.NodeRecommendation>();

                /* recommendations based on the given node and possible predecessors */
                for (int i = 0; i < nnc[0].getNrInPorts(); i++) {
                    ConnectionContainer cc = nnc[0].getParent().getIncomingConnectionFor(nnc[0].getID(), i);
                    if (cc != null) {
                        NodeContainer predecessor = nnc[0].getParent().getNodeContainer(cc.getSource());
                        if (predecessor instanceof NativeNodeContainer) {
                            List<NodeRecommendation> l = m_recommendations.get(idx)
                                .get(getKey((NativeNodeContainer)predecessor) + NODE_NAME_SEP + getKey(nnc[0]));
                            if (l != null) {
                                set.addAll(l);
                            }
                        }
                    }
                }

                /* recommendation based on the given node only */
                List<NodeRecommendation> p1 = m_recommendations.get(idx).get(nodeID);
                if (p1 != null) {
                    set.addAll(p1);
                }

                //add to the result list
                res[idx] = new ArrayList<NodeRecommendationManager.NodeRecommendation>(set.size());
                res[idx].addAll(set);
            } else {
                throw new UnsupportedOperationException(
                    "Recommendations for more than one node are not supported, yet.");
            }

            /* post-process result */
            Collections.sort(res[idx]);

            //update the total frequencies
            int tmpFreqs = 0;
            for (NodeRecommendation np : res[idx]) {
                tmpFreqs += np.getFrequency();
            }
            for (NodeRecommendation np : res[idx]) {
                np.setTotalFrequency(tmpFreqs);
            }
        }
        return res;
    }

    /**
     * @return the number of registered and enabled {@link NodeTripleProvider}s (e.g. resulting a the according number
     *         of frequencies in node recommendation table of the {@link WorkflowCoachView})
     */
    public int getNumNodeTripleProvider() {
        if (m_recommendations == null) {
            return 0;
        } else {
            return m_recommendations.size();
        }
    }

    /**
     * @param nnc the native node container to create the key for
     * @return the key to be used to look up the node recommendations
     */
    private static String getKey(final NativeNodeContainer nnc) {
        return nnc.getNode().getFactory().getClass().getName() + NODE_NAME_SEP + nnc.getName();
    }

    /**
     * @param nt the node info to create the key for
     * @return the key to be used to look up the node recommendations
     */
    private static String getKey(final NodeInfo ni) {
        return ni.getFactory() + NODE_NAME_SEP + ni.getName();
    }

    /**
     * Object representing one node recommendation, including the node template itself and a frequency as a measure of a
     * certainty for the given recommendation.
     *
     * @author Martin Horn, University of Konstanz
     */
    public static class NodeRecommendation implements Comparable<NodeRecommendation> {

        private int m_frequency;

        private NodeTemplate m_node;

        private int m_totalFrequency;

        private int m_num = 1;

        /**
         * @param node the node
         * @param frequency a frequency of usage
         *
         */
        public NodeRecommendation(final NodeTemplate node, final int frequency) {
            m_node = node;
            m_frequency = frequency;
            m_totalFrequency = frequency;
        }

        /**
         * @return the frequency, i.e. how often this node recommendation appears in a node triple or tuple (given by a
         *         {@link NodeTripleProvider}
         */
        public int getFrequency() {
            return (int)Math.round(m_frequency / (double)m_num);
        }

        /**
         * @return the total frequency summed over all node recommendations that have the SAME predecessor
         */
        public int getTotalFrequency() {
            return m_totalFrequency;
        }

        /**
         * Sets the overall frequency that is needed to express the frequency as a probability later on.
         *
         * @param frequency
         */
        private void setTotalFrequency(final int frequency) {
            m_totalFrequency = frequency;
        }

        /**
         * Increases the frequency by the given amount. Needed in order to aggregate frequencies of node recommendations
         * that recommend the same node (e.g. if the selected node only is taken into account and the predecessor
         * ignored). See {@link NodeRecommendationManager#aggregate(List)}.
         *
         * @param amount
         */
        private void increaseFrequency(final int amount) {
            m_frequency += amount;
            m_num += 1;
        }

        /**
         * Gives the recommended node as {@link NodeTemplate}.
         *
         * @return the node template
         */
        public NodeTemplate getNodeTemplate() {
            return m_node;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final NodeRecommendation o) {
            return -Integer.compare(getFrequency(), o.getFrequency());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_node.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((m_node == null) ? 0 : m_node.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NodeRecommendation other = (NodeRecommendation)obj;
            return Objects.equals(this.m_node, other.m_node);
        }
    }
}
