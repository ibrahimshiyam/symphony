package eu.compassresearch.core.interpreter.cml.core;

import java.util.List;
import java.util.Stack;

import org.overture.ast.node.INode;
import org.overture.interpreter.runtime.Context;

import eu.compassresearch.core.interpreter.Pair;
import eu.compassresearch.core.interpreter.api.CmlSupervisorEnvironment;
import eu.compassresearch.core.interpreter.events.CmlProcessStateEvent;
import eu.compassresearch.core.interpreter.events.CmlProcessStateObserver;
import eu.compassresearch.core.interpreter.events.CmlProcessTraceObserver;
import eu.compassresearch.core.interpreter.events.EventSourceHandler;
import eu.compassresearch.core.interpreter.events.TraceEvent;

class RestorePoint {

	public RestorePoint(Stack<Pair<INode,Context>> executionStack,
			CmlBehaviour 				parent,
			List<CmlBehaviour> 		children,
			CmlProcessState 					state,
			CmlSupervisorEnvironment 			env,
			CmlAlphabet 						hidingAlphabet,
			CmlTrace 							trace,
			EventSourceHandler<CmlProcessStateObserver,CmlProcessStateEvent>  stateEventhandler,
			EventSourceHandler<CmlProcessTraceObserver,TraceEvent>  traceEventHandler)
	{
		this.executionStack = executionStack;
		this.parent = parent;
		this.children = children;
		this.state = state;
		this.env = env;
		this.hidingAlphabet = hidingAlphabet;
		this.trace = trace;
		this.stateEventhandler = stateEventhandler;
		this.traceEventHandler = traceEventHandler;
	}
	
	public final Stack<Pair<INode,Context>> 		executionStack;
	public final CmlBehaviour 				parent;
	public final List<CmlBehaviour> 		children;
	public final CmlProcessState 					state;
	public final CmlSupervisorEnvironment 			env;
	public final CmlAlphabet 						hidingAlphabet;
	public final CmlTrace 							trace;
	public final EventSourceHandler<CmlProcessStateObserver,CmlProcessStateEvent>  stateEventhandler;
	public final EventSourceHandler<CmlProcessTraceObserver,TraceEvent>  traceEventHandler;
}