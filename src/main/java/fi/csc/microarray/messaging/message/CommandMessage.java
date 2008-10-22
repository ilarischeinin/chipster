package fi.csc.microarray.messaging.message;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * For sending a named command and a list of parameters.
 *  
 * @author akallio
 */
public class CommandMessage extends ParameterMessage {

	
	private final static String KEY_COMMAND = "command";

	public final static String COMMAND_CANCEL = "cancel";
	public final static String COMMAND_ACK = "acknowledge";
	public final static String COMMAND_OFFER = "offer";
	public final static String COMMAND_ACCEPT_OFFER = "choose";

	
	private String command;
	

	/**
	 * When only one kind of commands are passed to a given topic, this constructor
	 * can be used to create anonymous command.
	 */
	public CommandMessage() {
		this(null, null);
	}
	
	public CommandMessage(String command) {
		this(command, null);		
	}
	
	public CommandMessage(String command, List<String> parameters) {
		super(parameters);
		this.command = command;		
	}
	
	@Override
	public void unmarshal(MapMessage from) throws JMSException {
		super.unmarshal(from);
		this.command = from.getString(KEY_COMMAND);
	}
	
	@Override
	public void marshal(MapMessage to) throws JMSException {
		super.marshal(to);
		to.setString(KEY_COMMAND, command);
	}
	
	/**
	 * Returns the command or null if it is anonymous.
	 */
	public String getCommand() {
		return command;
	}
}
