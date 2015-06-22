package org.knime.base.node.mine.treeensemble.node.shrinker;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @author Patrick Winter, University of Konstanz
 */
class TreeEnsembleShrinkerNodeConfig {

    public static final String SIZE_TYPE_RELATIVE = "relative";

    public static final String SIZE_TYPE_ABSOLUTE = "absolute";

    public static final String SIZE_TYPE_AUTOMATIC = "automatic";

    private static final String RESULT_SIZE_TYPE_CFG = "resultSizeType";

    private static final String RESULT_SIZE_RELATIVE_CFG = "resultSizeRelative";

    private static final String RESULT_SIZE_ABSOLUTE_CFG = "resultSizeAbsolute";

    private static final String TARGET_COLUMN_CFG = "targetColumn";

    private static final String RESULT_SIZE_TYPE_DEFAULT = SIZE_TYPE_AUTOMATIC;

    private static final int RESULT_SIZE_RELATIVE_DEFAULT = 10;

    private static final int RESULT_SIZE_ABSOLUTE_DEFAULT = 10;

    private static final String TARGET_COLUMN_DEFAULT = "";

    private String m_resultSizeType = RESULT_SIZE_TYPE_DEFAULT;

    private int m_resultSizeRelative = RESULT_SIZE_RELATIVE_DEFAULT;

    private int m_resultSizeAbsolute = RESULT_SIZE_ABSOLUTE_DEFAULT;

    private String m_targetColumn = TARGET_COLUMN_DEFAULT;

    public String getResultSizeType() {
        return m_resultSizeType;
    }

    public void setResultSizeType(final String resultSizeType) {
        m_resultSizeType = resultSizeType;
    }

    public int getResultSizeRelative() {
        return m_resultSizeRelative;
    }

    public void setResultSizeRelative(final int resultSizeRelative) {
        m_resultSizeRelative = resultSizeRelative;
    }

    public int getResultSizeAbsolute() {
        return m_resultSizeAbsolute;
    }

    public void setResultSizeAbsolute(final int resultSizeAbsolute) {
        m_resultSizeAbsolute = resultSizeAbsolute;
    }

    public boolean isResultSizeAutomatic() {
        return m_resultSizeType.equals(SIZE_TYPE_AUTOMATIC);
    }

    /**
     * @param originalSize Size of the original ensemble
     * @return The absolute size based on the configuration or -1 if automatic
     */
    public int getResultSize(final int originalSize) {
        if (m_resultSizeType.equals(SIZE_TYPE_RELATIVE)) {
            return (int)Math.round((m_resultSizeRelative / (double)100) * originalSize);
        } else if (m_resultSizeType.equals(SIZE_TYPE_ABSOLUTE)) {
            return m_resultSizeAbsolute;
        } else {
            return -1;
        }
    }

    public String getTargetColumn() {
        return m_targetColumn;
    }

    public void setTargetColumn(final String targetColumn) {
        m_targetColumn = targetColumn;
    }

    public void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_resultSizeType = settings.getString(RESULT_SIZE_TYPE_CFG);
        m_resultSizeRelative = settings.getInt(RESULT_SIZE_RELATIVE_CFG);
        m_resultSizeAbsolute = settings.getInt(RESULT_SIZE_ABSOLUTE_CFG);
        m_targetColumn = settings.getString(TARGET_COLUMN_CFG);
    }

    public void loadInDialog(final NodeSettingsRO settings) {
        m_resultSizeType = settings.getString(RESULT_SIZE_TYPE_CFG, RESULT_SIZE_TYPE_DEFAULT);
        m_resultSizeRelative = settings.getInt(RESULT_SIZE_RELATIVE_CFG, RESULT_SIZE_RELATIVE_DEFAULT);
        m_resultSizeAbsolute = settings.getInt(RESULT_SIZE_ABSOLUTE_CFG, RESULT_SIZE_ABSOLUTE_DEFAULT);
        m_targetColumn = settings.getString(TARGET_COLUMN_CFG, TARGET_COLUMN_DEFAULT);
    }

    public void save(final NodeSettingsWO settings) {
        settings.addString(RESULT_SIZE_TYPE_CFG, m_resultSizeType);
        settings.addInt(RESULT_SIZE_RELATIVE_CFG, m_resultSizeRelative);
        settings.addInt(RESULT_SIZE_ABSOLUTE_CFG, m_resultSizeAbsolute);
        settings.addString(TARGET_COLUMN_CFG, m_targetColumn);
    }

}
