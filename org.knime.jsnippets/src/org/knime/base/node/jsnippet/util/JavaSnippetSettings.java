/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   24.11.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.util;

import org.knime.base.node.jsnippet.JavaSnippet;
import org.knime.base.node.jsnippet.util.JavaFieldList.InColList;
import org.knime.base.node.jsnippet.util.JavaFieldList.InVarList;
import org.knime.base.node.jsnippet.util.JavaFieldList.OutColList;
import org.knime.base.node.jsnippet.util.JavaFieldList.OutVarList;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The settings of the java snippet node.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class JavaSnippetSettings {
    private static final String SCRIPT_IMPORTS = "scriptImports";
    private static final String SCRIPT_FIELDS = "scriptFields";
    private static final String SCRIPT_BODY = "scriptBody";
    private static final String JAR_FILES = "jarFiles";
    private static final String BUNDLES = "bundles";
    private static final String OUT_COLS = "outCols";
    private static final String OUT_VARS = "outVars";
    private static final String IN_COLS = "inCols";
    private static final String IN_VARS = "inVars";
    private static final String TEMPLATE_UUID = "templateUUID";
    private static final String VERSION = "version";
    private static final String RUN_ON_EXECUTE = "runOnExecute";

    /** Custom imports. */
    private String m_scriptImports;
    /** Custom fields. */
    private String m_scriptFields;
    /** Custom script. */
    private String m_scriptBody;

    /** Custom jar files. */
    private String[] m_jarFiles;

    /** Additional bundles. */
    private String[] m_bundles;

    /** Input columns definition. */
    private InColList m_inCols;
    /** Input variables definitions. */
    private InVarList m_inVars;
    /** Output columns definition. */
    private OutColList m_outCols;
    /** Output variables definition. */
    private OutVarList m_outVars;

    /** The UUID of the blueprint for this setting. */
    private String m_templateUUID;

    /** The version of the java snippet. */
    private String m_version;

    /** If Java Edit Variable should be run during execute, not configure. */
    private boolean m_runOnExecute;

    /**
     * Create a new instance.
     */
    public JavaSnippetSettings() {
        this("");
    }

    /**
     * Create a new instance.
     * @param defaultContent the default content of the method
     */
    public JavaSnippetSettings(final String defaultContent) {
        m_scriptImports = "// Your custom imports:\n";
        m_scriptFields = "// Your custom variables:\n";
        m_scriptBody = "// Enter your code here:\n\n\t\t" + defaultContent + "\n\n";
        m_jarFiles = new String[0];
        m_bundles = new String[0];
        m_outCols = new OutColList();
        m_outVars = new OutVarList();
        m_inCols = new InColList();
        m_inVars = new InVarList();
        m_version = JavaSnippet.VERSION_1_X;
        m_templateUUID = null;
        m_runOnExecute = false;
    }


    /**
     * @return the scriptImports
     */
    public String getScriptImports() {
        return m_scriptImports;
    }


    /**
     * @param scriptImports the scriptImports to set
     */
    public void setScriptImports(final String scriptImports) {
        m_scriptImports = scriptImports;
    }


    /**
     * @return the scriptFields
     */
    public String getScriptFields() {
        return m_scriptFields;
    }


    /**
     * @param scriptFields the scriptFields to set
     */
    public void setScriptFields(final String scriptFields) {
        m_scriptFields = scriptFields;
    }


    /**
     * @return the scriptBody
     */
    public String getScriptBody() {
        return m_scriptBody;
    }


    /**
     * @param scriptBody the scriptBody to set
     */
    public void setScriptBody(final String scriptBody) {
        m_scriptBody = scriptBody;
    }


    /**
     * @return the jarFiles
     */
    public String[] getJarFiles() {
        return m_jarFiles;
    }


    /**
     * @param jarFiles the jarFiles to set
     */
    public void setJarFiles(final String[] jarFiles) {
        m_jarFiles = jarFiles;
    }

    /**
     * @return the bundles
     */
    public String[] getBundles() {
        return m_bundles;
    }


    /**
     * @param bundles the jarFiles to set
     */
    public void setBundles(final String[] bundles) {
        m_bundles = bundles;
    }


    /**
     * @return the version
     */
    String getVersion() {
        return m_version;
    }


    /**
     * @param version the version to set
     */
    void setVersion(final String version) {
        m_version = version;
    }

    /**
     * Get the system fields definitions of the java snippet.
     * @return the system fields definitions of the java snippet
     */
    public JavaSnippetFields getJavaSnippetFields() {
        return new JavaSnippetFields(
                m_inCols, m_inVars, m_outCols, m_outVars);
    }

    /**
     * @return the templateUUID
     */
    public String getTemplateUUID() {
        return m_templateUUID;
    }


    /**
     * @param templateUUID the templateUUID to set
     */
    public void setTemplateUUID(final String templateUUID) {
        m_templateUUID = templateUUID;
    }

    /**
     * @return the m_runOnExecute
     */
    public boolean isRunOnExecute() {
        return m_runOnExecute;
    }

    /**
     * @param runOnExecute the runOnExecute to set
     */
    public void setRunOnExecute(final boolean runOnExecute) {
        m_runOnExecute = runOnExecute;
    }

    /**
     * Set the system fields definitions of the java snippet.
     * @param fields the system fields definitions of the java snippet
     */
    public void setJavaSnippetFields(final JavaSnippetFields fields) {
        m_inCols = fields.getInColFields();
        m_inVars = fields.getInVarFields();
        m_outCols = fields.getOutColFields();
        m_outVars = fields.getOutVarFields();
    }


    /** Saves current parameters to settings object.
     * @param settings To save to.
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(SCRIPT_IMPORTS, m_scriptImports);
        settings.addString(SCRIPT_FIELDS, m_scriptFields);
        settings.addString(SCRIPT_BODY, m_scriptBody);
        settings.addStringArray(JAR_FILES, m_jarFiles);
        m_outCols.saveSettings(settings.addConfig(OUT_COLS));
        m_outVars.saveSettings(settings.addConfig(OUT_VARS));
        m_inCols.saveSettings(settings.addConfig(IN_COLS));
        m_inVars.saveSettings(settings.addConfig(IN_VARS));
        settings.addString(VERSION, m_version);
        settings.addString(TEMPLATE_UUID, m_templateUUID);
        settings.addBoolean(RUN_ON_EXECUTE, m_runOnExecute);

        // added in 3.6
        settings.addStringArray(BUNDLES, m_bundles);
    }

    /** Loads parameters in NodeModel.
     * @param settings To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_scriptImports = settings.getString(SCRIPT_IMPORTS);
        m_scriptFields = settings.getString(SCRIPT_FIELDS);
        m_scriptBody = settings.getString(SCRIPT_BODY);
        m_jarFiles = settings.getStringArray(JAR_FILES);
        m_outCols.loadSettings(settings.getConfig(OUT_COLS));
        m_outVars.loadSettings(settings.getConfig(OUT_VARS));
        m_inCols.loadSettings(settings.getConfig(IN_COLS));
        m_inVars.loadSettings(settings.getConfig(IN_VARS));
        m_version = settings.getString(VERSION);
        if (settings.containsKey(TEMPLATE_UUID)) {
            m_templateUUID = settings.getString(TEMPLATE_UUID);
        }
        // added in 2.8 (only java edit variable) -- 2.7 scripts were always run on execute()
        m_runOnExecute = settings.getBoolean(RUN_ON_EXECUTE, true);

        // added in 3.6
        m_bundles = settings.getStringArray(BUNDLES, new String[0]);
    }


    /** Loads parameters in Dialog.
     * @param settings To load from.
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_scriptImports = settings.getString(SCRIPT_IMPORTS, "");
        m_scriptFields = settings.getString(SCRIPT_FIELDS, "");
        m_scriptBody = settings.getString(SCRIPT_BODY, null);
        m_jarFiles = settings.getStringArray(JAR_FILES, new String[0]);

        // Even if the settings fail to load, we still complete loading
        // for dialog, otherwise the user may not see his code and think
        // its lost.
        try {
            m_outCols.loadSettingsForDialog(settings.getConfig(OUT_COLS));
        } catch (InvalidSettingsException e) {}
        try {
            m_outVars.loadSettingsForDialog(settings.getConfig(OUT_VARS));
        } catch (InvalidSettingsException e) {}
        try {
            m_inCols.loadSettingsForDialog(settings.getConfig(IN_COLS));
        } catch (InvalidSettingsException e) {}
        try {
            m_inVars.loadSettingsForDialog(settings.getConfig(IN_VARS));
        } catch (InvalidSettingsException e) {}

        m_version = settings.getString(VERSION, JavaSnippet.VERSION_1_X);
        m_templateUUID = settings.getString(TEMPLATE_UUID, null);
        // added in 2.8 (only java edit variable)
        m_runOnExecute = settings.getBoolean(RUN_ON_EXECUTE, false);

        // added in 3.6
        m_bundles = settings.getStringArray(BUNDLES, new String[0]);
    }
}
