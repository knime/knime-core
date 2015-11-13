package org.knime.base.node.preproc.rank;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Rank" Node.
 * This node ranks the input data based on the selected ranking field and ranking mode
 *
 * @author Adrian Nembach, Ferry Abt
 */
public class RankNodeFactory
        extends NodeFactory<RankNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public RankNodeModel createNodeModel() {
        return new RankNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<RankNodeModel> createNodeView(final int viewIndex,
            final RankNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new RankNodeDialog();
    }

}

