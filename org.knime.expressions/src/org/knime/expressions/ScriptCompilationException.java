package org.knime.expressions;

import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;

public class ScriptCompilationException extends Exception {
	/**
	 * serial version.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor taking the exception that caused the failure of the script.
	 * 
	 * @param ex
	 *            Causing exception.
	 */
	public ScriptCompilationException(Exception ex) {
		super(ex);
	}

	/**
	 * Constructor taking the message of the exception.
	 * 
	 * @param message
	 *            Message of the exception.
	 */
	public ScriptCompilationException(String message) {
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
	public ScriptCompilationException(String message, Exception ex) {
		super(message, ex);
	}

	/**
	 * Returns the error message. If the cause is due to a
	 * {@link MultipleCompilationErrorsException}, its message will be altered.
	 */
	@Override
	public String getMessage() {
		Throwable throwable = getCause();

		if (throwable instanceof MultipleCompilationErrorsException) {
			ErrorCollector errorCol = ((MultipleCompilationErrorsException) getCause()).getErrorCollector();

			String syntaxErrorMessage = errorCol.getSyntaxError(0).getOriginalMessage().toLowerCase();

			/*
			 * -- These messages are derived from
			 * org.codehaus.groovy.antlr.AntlrParserPlugin. --
			 */

			if (syntaxErrorMessage.contains("class definition not expected here")) {
				// Class definitions not allowed as the input script is wrapped
				// into a method
				// which is wrapped in class.
				return "Class definition not allowed in the expression";
			} else if (syntaxErrorMessage.contains("method definition not expected here")) {
				// Method definitions not allowed as the input script is wrapped
				// into a method.
				return "Method definition not allowed in the expression";
			}
		}

		return super.getMessage();
	}
}
