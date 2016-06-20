package org.knime.base.node.io.database.parameterizedquery;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the parameterized database query node.
 *
 * @author Budi Yanto, KNIME.com
 */
public class ParameterizedDBQueryNodeDialog extends NodeDialogPane {

    private ParameterizedDBQueryPanel m_panel = new ParameterizedDBQueryPanel();

    /**
     * New pane for configuring the DBLooper node.
     */
    protected ParameterizedDBQueryNodeDialog() {
        addTab("DB Looper", m_panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_panel.saveSettingsTo(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {

        final DataTableSpec inSpec = (DataTableSpec) specs[0];
        if(inSpec.getNumColumns() < 1) {
            throw new NotConfigurableException("No column spec available");
        }

        if(specs[1] == null){
            throw new NotConfigurableException("No valid database connection available.");
        }

        m_panel.loadSettingsFrom(settings, specs, getAvailableFlowVariables().values());

    }
}

