package org.knime.base.node.io.database.parameterizedquery;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the parameterized database query node.
 *
 *
 * @author Budi Yanto, KNIME.com
 */
public class ParameterizedDBQueryNodeFactory
        extends NodeFactory<ParameterizedDBQueryNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedDBQueryNodeModel createNodeModel() {
        return new ParameterizedDBQueryNodeModel();
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
    public NodeView<ParameterizedDBQueryNodeModel> createNodeView(final int viewIndex,
            final ParameterizedDBQueryNodeModel nodeModel) {
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
        return new ParameterizedDBQueryNodeDialog();
    }

}

