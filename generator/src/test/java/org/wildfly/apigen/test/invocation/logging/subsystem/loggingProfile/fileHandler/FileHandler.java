package org.wildfly.apigen.test.invocation.logging.subsystem.loggingProfile.fileHandler;

import org.wildfly.apigen.invocation.Address;
import org.wildfly.apigen.invocation.Binding;
import java.util.Map;
/**
 * Defines a handler which writes to a file.
 */
@Address("/subsystem=logging/logging-profile=*/file-handler=*")
public class FileHandler {

	private String key;
	private Boolean append;
	private Boolean autoflush;
	private Boolean enabled;
	private String encoding;
	private Map file;
	private String filterSpec;
	private String formatter;
	private String level;
	private String name;
	private String namedFormatter;

	public FileHandler(String key) {
		this.key = key;
	}

	public String getKey() {
		return this.key;
	}

	/**
	 * Specify whether to append to the target file.
	 */
	@Binding(detypedName = "append")
	public Boolean append() {
		return this.append;
	}

	/**
	 * Specify whether to append to the target file.
	 */
	public FileHandler append(Boolean value) {
		this.append = value;
		return this;
	}

	/**
	 * Automatically flush after each write.
	 */
	@Binding(detypedName = "autoflush")
	public Boolean autoflush() {
		return this.autoflush;
	}

	/**
	 * Automatically flush after each write.
	 */
	public FileHandler autoflush(Boolean value) {
		this.autoflush = value;
		return this;
	}

	/**
	 * If set to true the handler is enabled and functioning as normal, if set to false the handler is ignored when processing log messages.
	 */
	@Binding(detypedName = "enabled")
	public Boolean enabled() {
		return this.enabled;
	}

	/**
	 * If set to true the handler is enabled and functioning as normal, if set to false the handler is ignored when processing log messages.
	 */
	public FileHandler enabled(Boolean value) {
		this.enabled = value;
		return this;
	}

	/**
	 * The character encoding used by this Handler.
	 */
	@Binding(detypedName = "encoding")
	public String encoding() {
		return this.encoding;
	}

	/**
	 * The character encoding used by this Handler.
	 */
	public FileHandler encoding(String value) {
		this.encoding = value;
		return this;
	}

	/**
	 * The file description consisting of the path and optional relative to path.
	 */
	@Binding(detypedName = "file")
	public Map file() {
		return this.file;
	}

	/**
	 * The file description consisting of the path and optional relative to path.
	 */
	public FileHandler file(Map value) {
		this.file = value;
		return this;
	}

	/**
	 * A filter expression value to define a filter. Example for a filter that does not match a pattern: not(match("JBAS.*"))
	 */
	@Binding(detypedName = "filter-spec")
	public String filterSpec() {
		return this.filterSpec;
	}

	/**
	 * A filter expression value to define a filter. Example for a filter that does not match a pattern: not(match("JBAS.*"))
	 */
	public FileHandler filterSpec(String value) {
		this.filterSpec = value;
		return this;
	}

	/**
	 * Defines a pattern for the formatter.
	 */
	@Binding(detypedName = "formatter")
	public String formatter() {
		return this.formatter;
	}

	/**
	 * Defines a pattern for the formatter.
	 */
	public FileHandler formatter(String value) {
		this.formatter = value;
		return this;
	}

	/**
	 * The log level specifying which message levels will be logged by this logger. Message levels lower than this value will be discarded.
	 */
	@Binding(detypedName = "level")
	public String level() {
		return this.level;
	}

	/**
	 * The log level specifying which message levels will be logged by this logger. Message levels lower than this value will be discarded.
	 */
	public FileHandler level(String value) {
		this.level = value;
		return this;
	}

	/**
	 * The name of the handler.
	 */
	@Binding(detypedName = "name")
	public String name() {
		return this.name;
	}

	/**
	 * The name of the handler.
	 */
	public FileHandler name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * The name of the defined formatter to be used on the handler.
	 */
	@Binding(detypedName = "named-formatter")
	public String namedFormatter() {
		return this.namedFormatter;
	}

	/**
	 * The name of the defined formatter to be used on the handler.
	 */
	public FileHandler namedFormatter(String value) {
		this.namedFormatter = value;
		return this;
	}
}