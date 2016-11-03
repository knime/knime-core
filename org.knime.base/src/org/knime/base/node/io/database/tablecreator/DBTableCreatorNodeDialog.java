package org.knime.base.node.io.database.tablecreator;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.io.database.tablecreator.util.AdditionalSQLStatementPanel;
import org.knime.base.node.io.database.tablecreator.util.ColumnsPanel;
import org.knime.base.node.io.database.tablecreator.util.DBTableCreatorConfiguration;
import org.knime.base.node.io.database.tablecreator.util.KNIMEBasedMappingPanel;
import org.knime.base.node.io.database.tablecreator.util.KeysPanel;
import org.knime.base.node.io.database.tablecreator.util.NameBasedKeysPanel;
import org.knime.base.node.io.database.tablecreator.util.NameBasedMappingPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the "DBTableCreator" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Budi Yanto, KNIME.com
 */
public class DBTableCreatorNodeDialog extends NodeDialogPane {

    private static final int PANEL_DEFAULT_WIDTH = 720;

    private static final int PANEL_DEFAULT_HEIGHT = 380;

    private final DBTableCreatorConfiguration m_config = new DBTableCreatorConfiguration();

    private final ColumnsPanel m_columnsPanel;

    private final KeysPanel m_keysPanel;

    private final AdditionalSQLStatementPanel m_additionalSQLStatementPanel;

    private final NameBasedMappingPanel m_nameBasedMappingPanel;

    private final KNIMEBasedMappingPanel m_knimeTypeBasedMappingPanel;

    private final NameBasedKeysPanel m_nameBasedKeysPanel;

    private final JPanel m_dynamicTypePanel;

    /**
     * New pane for configuring the DBTableCreator node.
     */
    protected DBTableCreatorNodeDialog() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(PANEL_DEFAULT_WIDTH, PANEL_DEFAULT_HEIGHT));

        final JTabbedPane tabs = new JTabbedPane();

        m_columnsPanel = new ColumnsPanel(DBTableCreatorConfiguration.CFG_COLUMNS_SETTINGS, m_config);
        m_keysPanel = new KeysPanel(DBTableCreatorConfiguration.CFG_KEYS_SETTINGS, m_config);
        m_additionalSQLStatementPanel = new AdditionalSQLStatementPanel(m_config);
        m_nameBasedMappingPanel =
            new NameBasedMappingPanel(DBTableCreatorConfiguration.CFG_NAME_BASED_TYPE_MAPPING, m_config);
        m_knimeTypeBasedMappingPanel =
            new KNIMEBasedMappingPanel(DBTableCreatorConfiguration.CFG_KNIME_BASED_TYPE_MAPPING, m_config);
        m_dynamicTypePanel = createDynamicPanel();
        m_nameBasedKeysPanel = new NameBasedKeysPanel(DBTableCreatorConfiguration.CFG_NAME_BASED_KEYS, m_config);

        tabs.add("Settings", createTableSettingsPanel());
        tabs.add(m_columnsPanel.getTitle(), m_columnsPanel);
        tabs.add(m_keysPanel.getTitle(), m_keysPanel);
        tabs.add("Additional Options", m_additionalSQLStatementPanel);
        tabs.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent ev) {
                if(tabs.getTitleAt(tabs.getSelectedIndex()).equals(m_columnsPanel.getTitle())) {
                    final DataTableSpec spec = m_config.getTableSpec();
                    if(m_config.useDynamicSettings() && spec != null) {
                        m_config.loadColumnSettingsFromTableSpec(spec);
                        m_columnsPanel.revalidateTable();
                    }
                }

                if(tabs.getTitleAt(tabs.getSelectedIndex()).equals(m_keysPanel.getTitle())) {
                    if(m_config.useDynamicSettings()) {
                        m_config.updateKeysWithDynamicSettings();
                        m_keysPanel.revalidateTable();
                    }
                }
            }
        });

        // Only shows the dynamic tabs if the checkbox "UseDynamicSettings" is checked
        final SettingsModelBoolean dynamicSettingsModel = m_config.getUseDynamicSettingsModel();
        if(dynamicSettingsModel.getBooleanValue()) {
            addDynamicTabs(tabs);
        }

        dynamicSettingsModel.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                if(dynamicSettingsModel.getBooleanValue()) {
                    addDynamicTabs(tabs);
                    activateStaticPanel(false);
                }else {
                    removeDynamicTabs(tabs);
                    activateStaticPanel(true);
                }
            }
        });

        panel.add(tabs);
        super.addTab("Table Creator Settings", panel);

    }

    /**
     * @return a newly created table settings panel
     */
    private JPanel createTableSettingsPanel() {
        final JPanel panel = new JPanel(new BorderLayout());

        final JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
            .createEtchedBorder(), "Table Settings"));
        final Box box = new Box(BoxLayout.Y_AXIS);
        final DialogComponentString schemaComp =
                new DialogComponentString(m_config.getSchemaSettingsModel(), "Schema: ");
        box.add(schemaComp.getComponentPanel());
        box.add(Box.createVerticalGlue());

        final DialogComponentString tableNameComp =
                new DialogComponentString(m_config.getTableNameSettingsModel(), "Table name: ");
        box.add(tableNameComp.getComponentPanel());
        box.add(Box.createVerticalGlue());

        final DialogComponentBoolean tempTableComp =
                new DialogComponentBoolean(m_config.getTempTableSettingsModel(), "Create temporary table");
        box.add(tempTableComp.getComponentPanel());
        box.add(Box.createVerticalGlue());

        final DialogComponentBoolean ifNotExistsComp =
                new DialogComponentBoolean(m_config.getIfNotExistsSettingsModel(),
                    "Create table if it does not exist");
        box.add(ifNotExistsComp.getComponentPanel());
        box.add(Box.createVerticalGlue());
        tablePanel.add(box);

        final JPanel dynamicPanel = new JPanel(new BorderLayout());
        dynamicPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
            .createEtchedBorder(), "Dynamic Settings"));
        final DialogComponentBoolean useDynamicSettingsComp =
                new DialogComponentBoolean(m_config.getUseDynamicSettingsModel(), "Use dynamic settings");
        dynamicPanel.add(useDynamicSettingsComp.getComponentPanel());

        panel.add(tablePanel, BorderLayout.CENTER);
        panel.add(dynamicPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Adds the dynamic tab to the tabbedPane
     * @param tabbedPane the tabbedPane where the dynamic tab should be added
     */
    private void addDynamicTabs(final JTabbedPane tabbedPane) {
        int idx = tabbedPane.indexOfComponent(m_dynamicTypePanel);
        if(idx < 0) {
            tabbedPane.add("Dynamic Type Settings", m_dynamicTypePanel);
        }
        idx = tabbedPane.indexOfComponent(m_nameBasedKeysPanel);
        if(idx < 0) {
            tabbedPane.add(m_nameBasedKeysPanel.getTitle(), m_nameBasedKeysPanel);
        }
    }

    /**
     * Removes the dynamic tab from the tabbedPane
     * @param tabbedPane the tabbedPane from where the dynamic tabs should be removed
     */
    private void removeDynamicTabs(final JTabbedPane tabbedPane) {
        int idx = tabbedPane.indexOfComponent(m_dynamicTypePanel);
        if(idx > -1) {
            tabbedPane.remove(m_dynamicTypePanel);
        }

        idx = tabbedPane.indexOfComponent(m_nameBasedKeysPanel);
        if(idx > -1) {
            tabbedPane.remove(m_nameBasedKeysPanel);
        }
    }

    /**
     * Activates or deactivates the static panel
     * @param activate <code>true</code> if the static panel should be activated, otherwise <code>false</code>
     */
    private void activateStaticPanel(final boolean activate) {
        m_columnsPanel.setEnabledAddButton(activate);
        m_columnsPanel.setEnabledRemoveButton(activate);
        m_keysPanel.setEnabledAddButton(activate);
        m_keysPanel.setEnabledRemoveButton(activate);

    }

    /**
     * @return a newly created dynamic panel
     */
    private JPanel createDynamicPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        final JTabbedPane tabs = new JTabbedPane();
        tabs.add(m_nameBasedMappingPanel.getTitle(), m_nameBasedMappingPanel);
        tabs.add(m_knimeTypeBasedMappingPanel.getTitle(), m_knimeTypeBasedMappingPanel);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_additionalSQLStatementPanel.onSave();
        m_columnsPanel.onSave();
        m_keysPanel.onSave();
        m_nameBasedMappingPanel.onSave();
        m_knimeTypeBasedMappingPanel.onSave();
        m_nameBasedKeysPanel.onSave();
        m_config.saveSettingsForDialog(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {

        // Prevents opening the dialog if no valid database connection is available
        if (specs[0] == null) {
            throw new NotConfigurableException(
                "Cannot open database table creator without a valid database connection");
        }
        m_config.loadSettingsForDialog(settings, specs);
        m_additionalSQLStatementPanel.onLoad();
        m_columnsPanel.onLoad();
        m_keysPanel.onLoad();
        m_nameBasedMappingPanel.onLoad();
        m_knimeTypeBasedMappingPanel.onLoad();
        m_nameBasedKeysPanel.onLoad();
    }

}
