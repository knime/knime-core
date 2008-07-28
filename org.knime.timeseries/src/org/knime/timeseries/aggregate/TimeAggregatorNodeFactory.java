package org.knime.timeseries.aggregate;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "TimeAggregator" Node.
 * Appends a time column with the time in another column to a higher aggregation level (e.g. month to quarter)
 *
 * @author KNIME GmbH
 */
public class TimeAggregatorNodeFactory extends NodeFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        return new TimeAggregatorNodeModel();
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
    public NodeView createNodeView(final int viewIndex,
            final NodeModel nodeModel) {
        throw new IllegalArgumentException(
        		"Time Aggregator node has no view");
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
        return new TimeAggregatorNodeDialog();
    }

}

