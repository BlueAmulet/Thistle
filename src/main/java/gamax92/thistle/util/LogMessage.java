package gamax92.thistle.util;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;

public class LogMessage implements MessageFactory {

	private String prefix;

	public LogMessage(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public Message newMessage(Object message) {
		return new SimpleMessage("[" + prefix + "] " + message);
	}

	@Override
	public Message newMessage(String message) {
		return new SimpleMessage("[" + prefix + "] " + message);
	}

	@Override
	public Message newMessage(String message, Object... params) {
		return new SimpleMessage("[" + prefix + "] " + new ParameterizedMessage(message, params).getFormattedMessage());
	}

}
