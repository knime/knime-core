/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 7, 2007 (ohl): created
 */
package org.knime.base.node.io.csvwriter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author ohl, University of Konstanz
 */
class FileWriterNodeSettings extends FileWriterSettings {
    
    /** Policy how to proceed when output file exists 
     * (overwrite, abort, append). */
    enum FileOverwritePolicy {
        /** Fail during configure/execute. */
        Abort,
        /** Overwrite existing file. */
        Overwrite,
        /** Append to existing file. */
        Append
    }

    public static final String CFGKEY_FILE = "filename";

    private static final String CFGKEY_COMMENT_BEGIN = "commentBegin";

    private static final String CFGKEY_COMMENT_END = "commentEnd";

    private static final String CFGKEY_ADD_TIME = "addTime";

    private static final String CFGKEY_ADD_USER = "addUser";

    private static final String CFGKEY_ADD_TABLENAME = "addTablename";

    private static final String CFGKEY_USERCOMMENT = "userCommentLine";

    private static final String CFGKEY_COLHEADER_SKIP_ON_APPEND =
            "skipWriteColHeaderOnAppend";

    /** If to append to existing file, replaced with 
     * {@link FileOverwritePolicy} since v2.1.
     */
    private static final String CFGKEY_APPEND = "isAppendToFile";
    
    private static final String CFGKEY_OVERWRITE_POLICY = "fileOverwritePolicy";

    private String m_fileName;

    /** Whether to skip writing the col header when file exists (applicable
     * only when on {@link FileOverwritePolicy#Append}.
     */
    private boolean m_skipColHeaderIfFileExists;

    // only used if one of the following is true
    private String m_commentBegin;

    private String m_commentEnd;

    private boolean m_addCreationTime;

    private boolean m_addCreationUser;

    private boolean m_addTableName;

    private String m_customCommentLine;
    
    private FileOverwritePolicy m_fileOverwritePolicy;

    /**
     *
     */
    FileWriterNodeSettings() {
        m_fileName = null;
        m_fileOverwritePolicy = FileOverwritePolicy.Abort;
        m_skipColHeaderIfFileExists = false;
        m_commentBegin = "";
        m_commentEnd = "";
        m_addCreationTime = false;
        m_addCreationUser = false;
        m_addTableName = false;
        m_customCommentLine = "";
    }

    /**
     * Constructs a new object reading the settings from the specified
     * NodeSettings object. If the settings object doesn't contain all settings
     * an exception is thrown. Settings are accepted and set internally even if
     * they are invalid or inconsistent.
     *
     * @param settings the object to read the initial values from.
     * @throws InvalidSettingsException if the settings object contains
     *             incomplete, invalid, or inconsistent values.
     */
    FileWriterNodeSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super(settings);
        m_fileName = settings.getString(CFGKEY_FILE);

        FileOverwritePolicy fileOverwritePolicy;
        if (settings.containsKey(CFGKEY_OVERWRITE_POLICY)) { // since v2.1
            String val = settings.getString(CFGKEY_OVERWRITE_POLICY, 
                    FileOverwritePolicy.Abort.toString());
            try {
                fileOverwritePolicy = FileOverwritePolicy.valueOf(val);
            } catch (Exception e) {
                throw new InvalidSettingsException("Unable to parse 'file " 
                        + "overwrite policy' field: " + val, e);
            }
        } else if (settings.containsKey(CFGKEY_APPEND)) { // v1.3 - v2.0
            if (settings.getBoolean(CFGKEY_APPEND)) {
                fileOverwritePolicy = FileOverwritePolicy.Append;
            } else {
                // preferably this should default to Abort but that would 
                // break existing flows (change in behavior)
                fileOverwritePolicy = FileOverwritePolicy.Overwrite;
            }
        } else {
            // way too old - setting meaningful defaults here
            fileOverwritePolicy = FileOverwritePolicy.Abort;
        }
        m_fileOverwritePolicy = fileOverwritePolicy;
        // only available since 1.1.x
        m_skipColHeaderIfFileExists =
                settings.getBoolean(CFGKEY_COLHEADER_SKIP_ON_APPEND, false);

        // these options are only available since 1.2.++
        m_commentBegin = settings.getString(CFGKEY_COMMENT_BEGIN, "");
        m_commentEnd = settings.getString(CFGKEY_COMMENT_END, "");
        m_addCreationTime = settings.getBoolean(CFGKEY_ADD_TIME, false);
        m_addCreationUser = settings.getBoolean(CFGKEY_ADD_USER, false);
        m_addTableName = settings.getBoolean(CFGKEY_ADD_TABLENAME, false);
        m_customCommentLine = settings.getString(CFGKEY_USERCOMMENT, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addString(CFGKEY_FILE, m_fileName);
        settings.addString(CFGKEY_OVERWRITE_POLICY, 
                m_fileOverwritePolicy.toString());
        settings.addBoolean(CFGKEY_COLHEADER_SKIP_ON_APPEND,
                m_skipColHeaderIfFileExists);
        settings.addString(CFGKEY_COMMENT_BEGIN, m_commentBegin);
        settings.addString(CFGKEY_COMMENT_END, m_commentEnd);
        settings.addBoolean(CFGKEY_ADD_TIME, m_addCreationTime);
        settings.addBoolean(CFGKEY_ADD_USER, m_addCreationUser);
        settings.addBoolean(CFGKEY_ADD_TABLENAME, m_addTableName);
        settings.addString(CFGKEY_USERCOMMENT, m_customCommentLine);

    }

    /*
     * ----------------------------------------------------------------------
     * Setter and getter methods for all settings.
     * ----------------------------------------------------------------------
     */

    /**
     * @return the addCreationTime
     */
    boolean addCreationTime() {
        return m_addCreationTime;
    }

    /**
     * @param addCreationTime the addCreationTime to set
     */
    void setAddCreationTime(final boolean addCreationTime) {
        m_addCreationTime = addCreationTime;
    }

    /**
     * @return the addCreationUser
     */
    boolean addCreationUser() {
        return m_addCreationUser;
    }

    /**
     * @param addCreationUser the addCreationUser to set
     */
    void setAddCreationUser(final boolean addCreationUser) {
        m_addCreationUser = addCreationUser;
    }

    /**
     * @return the addTableName
     */
    boolean addTableName() {
        return m_addTableName;
    }

    /**
     * @param addTableName the addTableName to set
     */
    void setAddTableName(final boolean addTableName) {
        m_addTableName = addTableName;
    }

    /**
     * @return the commentBegin
     */
    String getCommentBegin() {
        return m_commentBegin;
    }
    
    /**
     * @param fileOverwritePolicy the fileOverwritePolicy to set
     */
    void setFileOverwritePolicy(final FileOverwritePolicy fileOverwritePolicy) {
        if (fileOverwritePolicy == null) {
            m_fileOverwritePolicy = FileOverwritePolicy.Abort;
        } else {
            m_fileOverwritePolicy = fileOverwritePolicy;
        }
    }
    
    /**
     * @return the fileOverwritePolicy, never null
     */
    FileOverwritePolicy getFileOverwritePolicy() {
        return m_fileOverwritePolicy;
    }

    /**
     * @param commentBegin the commentBegin to set
     */
    void setCommentBegin(final String commentBegin) {
        m_commentBegin = commentBegin;
    }

    /**
     * @return the commentEnd
     */
    String getCommentEnd() {
        return m_commentEnd;
    }

    /**
     * @param commentEnd the commentEnd to set
     */
    void setCommentEnd(final String commentEnd) {
        m_commentEnd = commentEnd;
    }

    /**
     * @return the customCommentLine
     */
    String getCustomCommentLine() {
        return m_customCommentLine;
    }

    /**
     * @param customCommentLine the customCommentLine to set
     */
    void setCustomCommentLine(final String customCommentLine) {
        m_customCommentLine = customCommentLine;
    }

    /**
     * @return the fileName
     */
    String getFileName() {
        return m_fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    void setFileName(final String fileName) {
        m_fileName = fileName;
    }

    /**
     * @return the skipColHeaderIfFileExists
     */
    boolean skipColHeaderIfFileExists() {
        return m_skipColHeaderIfFileExists;
    }

    /**
     * @param skipColHeaderIfFileExists the skipColHeaderIfFileExists to set
     */
    void setSkipColHeaderIfFileExists(final boolean skipColHeaderIfFileExists) {
        m_skipColHeaderIfFileExists = skipColHeaderIfFileExists;
    }

}
