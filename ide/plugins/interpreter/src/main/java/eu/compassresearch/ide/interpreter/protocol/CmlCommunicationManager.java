package eu.compassresearch.ide.interpreter.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;

import eu.compassresearch.core.interpreter.debug.Choice;
import eu.compassresearch.core.interpreter.debug.CmlDbgCommandMessage;
import eu.compassresearch.core.interpreter.debug.CmlDbgStatusMessage;
import eu.compassresearch.core.interpreter.debug.CmlDbgpStatus;
import eu.compassresearch.core.interpreter.debug.CmlDebugCommand;
import eu.compassresearch.core.interpreter.utility.messaging.CmlRequest;
import eu.compassresearch.core.interpreter.utility.messaging.Message;
import eu.compassresearch.core.interpreter.utility.messaging.MessageCommunicator;
import eu.compassresearch.core.interpreter.utility.messaging.MessageContainer;
import eu.compassresearch.core.interpreter.utility.messaging.RequestMessage;
import eu.compassresearch.ide.interpreter.CmlDebugPlugin;
import eu.compassresearch.ide.interpreter.ICmlDebugConstants;
import eu.compassresearch.ide.interpreter.model.CmlChoiceMediator;
import eu.compassresearch.ide.interpreter.model.CmlDebugTarget;

public class CmlCommunicationManager extends Thread
{
	interface MessageEventHandler<T extends Message>
	{
		public boolean handleMessage(T message);
	};

	private Map<String, MessageEventHandler<RequestMessage>> requestHandlers;
	private Map<String, MessageEventHandler<CmlDbgStatusMessage>> statusHandlers;
	private BufferedReader fRequestReader;
	final CmlDebugTarget target;

	// socket to communicate with VM
	private Socket fRequestSocket;
	private OutputStream requestOutputStream;

	private CmlThreadManager threadManager;
	private int port;

	public CmlCommunicationManager(CmlDebugTarget target,
			CmlThreadManager threadManager, int port)
	{
		this.target = target;
		this.threadManager = threadManager;
		this.port = port;
	}

	/**
	 * Private methods
	 */
	/**
	 * Sends a command to the running interpreter
	 * 
	 * @param cmd
	 */
	public void sendCommandMessage(CmlDebugCommand cmd)
	{
		CmlDbgCommandMessage message = new CmlDbgCommandMessage(cmd);
		MessageCommunicator.sendMessage(requestOutputStream, message);
	}

	public void sendMessage(Message message)
	{
		MessageCommunicator.sendMessage(requestOutputStream, message);
	}

	/**
	 * Initialisation methods
	 */
	public void initializeHandlers()
	{
		requestHandlers = initializeRequestHandlers();
		statusHandlers = initializeStatusHandlers();

	}

	/**
	 * Initialises all the request message handlers
	 * 
	 * @return
	 */
	private Map<String, MessageEventHandler<RequestMessage>> initializeRequestHandlers()
	{
		Map<String, MessageEventHandler<RequestMessage>> handlers = new HashMap<String, MessageEventHandler<RequestMessage>>();

		// Handler for the Choice request
		handlers.put(CmlRequest.CHOICE.toString(), new MessageEventHandler<RequestMessage>()
		{

			@Override
			public boolean handleMessage(RequestMessage message)
			{
				// Type listType = new TypeToken<List<String>>(){}.getType();

				final List<Choice> events = message.getContent();
				new CmlChoiceMediator(target, CmlCommunicationManager.this).setChoiceOptions(events, message);
				return true;
			}
		});

		return handlers;
	}

	/**
	 * Initializes all the status message handlers
	 * 
	 * @return
	 */
	private Map<String, MessageEventHandler<CmlDbgStatusMessage>> initializeStatusHandlers()
	{
		Map<String, MessageEventHandler<CmlDbgStatusMessage>> handlers = new HashMap<String, MessageEventHandler<CmlDbgStatusMessage>>();

		handlers.put(CmlDbgpStatus.STARTING.toString(), new MessageEventHandler<CmlDbgStatusMessage>()
		{
			@Override
			public boolean handleMessage(CmlDbgStatusMessage message)
			{
				for (IBreakpoint b : target.getBreakpoints())
				{
					try
					{
						if (b.isEnabled())
						{
							System.out.println("Adding breakpoint: " + b);
							// TODO communnicate the setting of the breakpoint to the interpreter
						}
					} catch (CoreException e)
					{
						CmlDebugPlugin.logError("Failed to set breakpoint", e);
					}
				}
				return true;
			}
		});

		handlers.put(CmlDbgpStatus.RUNNING.toString(), new MessageEventHandler<CmlDbgStatusMessage>()
		{
			@Override
			public boolean handleMessage(CmlDbgStatusMessage message)
			{
				threadManager.started(message.getInterpreterStatus());
				return true;
			}
		});

		handlers.put(CmlDbgpStatus.CHOICE.toString(), new MessageEventHandler<CmlDbgStatusMessage>()
		{
			@Override
			public boolean handleMessage(CmlDbgStatusMessage message)
			{
				threadManager.updateDebuggerInfo(message.getInterpreterStatus());
				return true;
			}
		});

		handlers.put(CmlDbgpStatus.STOPPING.toString(), new MessageEventHandler<CmlDbgStatusMessage>()
		{
			@Override
			public boolean handleMessage(CmlDbgStatusMessage message)
			{
				threadManager.stopping();
				return true;
			}
		});

		handlers.put(CmlDbgpStatus.STOPPED.toString(), new MessageEventHandler<CmlDbgStatusMessage>()
		{
			@Override
			public boolean handleMessage(CmlDbgStatusMessage message)
			{
				threadManager.updateDebuggerInfo(message.getInterpreterStatus());
				return false;
			}
		});

		handlers.put(CmlDbgpStatus.CONNECTION_CLOSED.toString(), new MessageEventHandler<CmlDbgStatusMessage>()
		{
			@Override
			public boolean handleMessage(CmlDbgStatusMessage message)
			{
				connectionClosed();
				return false;
			}
		});

		return handlers;
	}

	/**
	 * Receives a message from the debugger
	 * 
	 * @return The received message wrapped in a MessageContainer
	 * @throws IOException
	 */
	public MessageContainer receiveMessage() throws IOException
	{
		return MessageCommunicator.receiveMessage(fRequestReader, new MessageContainer(new CmlDbgStatusMessage(CmlDbgpStatus.CONNECTION_CLOSED)));
	}

	/**
	 * Dispatches the message to the corresponding message handler
	 * 
	 * @param handlers
	 *            The corresponding message handler map for this message type
	 * @param message
	 *            The message to be processed
	 * @return true if the event loop should continue otherwise false
	 */
	private <H extends Message> boolean dispatchMessageHandler(
			Map<String, MessageEventHandler<H>> handlers, H message)
	{
		boolean result = false;

		if (handlers.containsKey(message.getKey()))
			result = handlers.get(message.getKey()).handleMessage(message);

		return result;
	}

	/**
	 * Dispatches the message from messageContainer to the assigned handler of this message type
	 * 
	 * @param messageContainer
	 * @return
	 */
	public boolean processMessage(MessageContainer messageContainer)
	{
		boolean result = false;

		switch (messageContainer.getType())
		{
			case STATUS:
				return dispatchMessageHandler(statusHandlers, (CmlDbgStatusMessage) messageContainer.getMessage());
			case REQUEST:
				return dispatchMessageHandler(requestHandlers, (RequestMessage) messageContainer.getMessage());
			default:
				break;
		}

		return result;
	}

	private void connectionClosed()
	{
		try
		{
			this.fRequestSocket.close();
		} catch (IOException e)
		{
			e.printStackTrace();

		}
	}

	@Override
	public void run()
	{
		// CmlDebugStatusMessage event = new CmlDebugStatusMessage(CmlDbgpStatus.WAITING_FOR_CONNECTION);
		MessageContainer message = null;
		try
		{
			do
			{
				message = receiveMessage();
				System.out.println(message);
			}
			// while (!isTerminated() && message != null && processMessage(message));
			while (processMessage(message));
		} catch (IOException e)
		{
			System.out.println(e);
			connectionClosed();
			threadManager.terminated();

		} finally
		{
			connectionClosed();
			threadManager.terminated();
		}
	}

	public boolean isDisconnected()
	{
		return fRequestSocket.isClosed();
	}

	public void connect() throws IOException
	{
		if (waitForConnect(port))
		{
			initializeHandlers();
			start();
		}
	}

	private boolean waitForConnect(int requestPort) throws IOException
	{
		ServerSocket requestAcceptor = null;
		try
		{
			requestAcceptor = new ServerSocket(requestPort);
			// FIXME change to config
			int timeout = 5000;
			if (target.getLaunch().getLaunchConfiguration().getAttribute(ICmlDebugConstants.CML_LAUNCH_CONFIG_REMOTE_DEBUG, false))
			{
				timeout = 30000;
			}
			requestAcceptor.setSoTimeout(timeout);

			fRequestSocket = requestAcceptor.accept();
			requestOutputStream = fRequestSocket.getOutputStream();
			fRequestReader = new BufferedReader(new InputStreamReader(fRequestSocket.getInputStream()));
			return true;
		} catch (CoreException e)
		{
			CmlDebugPlugin.logError("Failed to obtain remote debug flag from launch config", e);
		} finally
		{
			if (requestAcceptor != null)
				requestAcceptor.close();
		}
		return false;
	}

	public void terminate()
	{

		synchronized (fRequestSocket)
		{
			sendCommandMessage(CmlDebugCommand.STOP);
		}

	}

	public void disconnect()
	{
		sendCommandMessage(CmlDebugCommand.DISCONNECT);
	}

	public boolean isConnected()
	{
		return (fRequestSocket == null ? false : !fRequestSocket.isClosed());
	}

}