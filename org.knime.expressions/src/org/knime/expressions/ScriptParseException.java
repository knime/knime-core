package org.knime.expressions;

/**
 * Exception thrown when script cannot be parsed. May be extended with more
 * information in the future.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class ScriptParseException extends Exception {

	private String m_line;
	private int m_lineNr;
	private String m_script;

	/**
	 * Default serial version.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor taking the exception that caused the failure of the script.
	 * 
	 * @param ex
	 *            Exception that indicates the cause of the failure of the
	 *            execution.
	 */
	public ScriptParseException(Exception ex) {
		super(ex);
	}

	/**
	 * Constructor taking the message of the exception.
	 * 
	 * @param message
	 *            Message of the exception.
	 */
	public ScriptParseException(String message) {
		super(message);
	}

	/**
	 * Constructor taking the message and the causing exception.
	 * 
	 * @param message
	 *            Message of the exception.
	 * @param ex
	 *            Causing exception.
	 */
	public ScriptParseException(String message, Exception ex) {
		super(message, ex);
	}

	/**
	 * Constructor taking the message and the causing exception.
	 * 
	 * @param message
	 *            Message of the exception.
	 * @param script
	 *            Script containing the error.
	 * @param lineNr
	 *            Line number where the error occurred.
	 * @param line
	 *            Specific line containing the error.
	 * @param Exception
	 *            that caused the error.
	 */
	public ScriptParseException(String message, String script, int lineNr, String line, Exception ex) {
		super(message, ex);
	}

	/**
	 * 
	 * @return The line containing the error.
	 */
	public String getLine() {
		return m_line;
	}

	/**
	 * 
	 * @return Line number of the script where the error occurred.
	 */
	public int getLineNr() {
		return m_lineNr;
	}

	/**
	 * 
	 * @return Script containing the error.
	 */
	public String getScript() {
		return m_script;
	}
}
