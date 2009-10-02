package org.knime.timeseries.node.diff;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TimeDifference" Node.
 * Appends the difference between two dates.
 *
 * @author KNIME GmbH
 */
public class TimeDifferenceNodeFactory 
    extends NodeFactory<TimeDifferenceNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeDifferenceNodeModel createNodeModel() {
        return new TimeDifferenceNodeModel();
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
    public NodeView<TimeDifferenceNodeModel> createNodeView(
            final int viewIndex,
            final TimeDifferenceNodeModel nodeModel) {
        throw new IllegalArgumentException(
            "TimeDifference node has no view!");
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
        return new TimeDifferenceNodeDialog();
    }

}

