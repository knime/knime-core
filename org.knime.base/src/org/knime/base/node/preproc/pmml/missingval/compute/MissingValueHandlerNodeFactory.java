package org.knime.base.node.preproc.pmml.missingval.compute;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "CompiledModelReader" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
public class MissingValueHandlerNodeFactory
        extends NodeFactory<MissingValueHandlerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MissingValueHandlerNodeModel createNodeModel() {
        return new MissingValueHandlerNodeModel();
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
    public NodeView<MissingValueHandlerNodeModel> createNodeView(final int viewIndex,
            final MissingValueHandlerNodeModel nodeModel) {
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
        return new MissingValueHandlerNodeDialog();
    }

}

