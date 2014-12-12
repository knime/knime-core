package org.knime.base.node.mine.knn.pmml;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TransformationsMerger" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
public class PMMLKNNNodeFactory
        extends NodeFactory<PMMLKNNNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLKNNNodeModel createNodeModel() {
        return new PMMLKNNNodeModel();
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
    public NodeView<PMMLKNNNodeModel> createNodeView(final int viewIndex,
            final PMMLKNNNodeModel nodeModel) {
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
        return new PMMLKNNNodeDialog();
    }

}

