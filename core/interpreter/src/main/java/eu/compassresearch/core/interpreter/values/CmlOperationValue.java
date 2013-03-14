package eu.compassresearch.core.interpreter.values;

import java.util.List;
import java.util.Vector;

import org.overture.ast.definitions.AStateDefinition;
import org.overture.ast.definitions.SClassDefinition;
import org.overture.ast.lex.LexLocation;
import org.overture.ast.lex.LexNameToken;
import org.overture.ast.patterns.APatternListTypePair;
import org.overture.ast.patterns.PPattern;
import org.overture.ast.types.AOperationType;
import org.overture.interpreter.runtime.ClassContext;
import org.overture.interpreter.runtime.Context;
import org.overture.interpreter.runtime.ObjectContext;
import org.overture.interpreter.runtime.RootContext;
import org.overture.interpreter.runtime.StateContext;
import org.overture.interpreter.values.FunctionValue;
import org.overture.interpreter.values.ObjectValue;
import org.overture.interpreter.values.OperationValue;
import org.overture.interpreter.values.Value;
import org.overture.typechecker.assistant.definition.PAccessSpecifierAssistantTC;

import eu.compassresearch.ast.actions.PAction;
import eu.compassresearch.ast.definitions.AExplicitCmlOperationDefinition;
import eu.compassresearch.ast.definitions.AImplicitCmlOperationDefinition;
import eu.compassresearch.core.interpreter.cml.CmlBehaviourThread;

public class CmlOperationValue extends Value {

	//The name of the special return value in a assignment call context
	//This is used to retrieve the result of a operation
	public static LexNameToken ReturnValueName()
	{
		return new LexNameToken("|CALL|","|RETURN_VALUE|",new LexLocation());
	}
	
	private static final long serialVersionUID = 1L;
	public final AExplicitCmlOperationDefinition expldef;
	public final AImplicitCmlOperationDefinition impldef;
	public final LexNameToken name;
	private final AOperationType type;
	private final List<PPattern> paramPatterns;

	public final FunctionValue precondition;
	public final FunctionValue postcondition;
	public final AStateDefinition state;

	private PAction body;
	private LexNameToken stateName = null;
	private Context stateContext = null;
	private ObjectValue self = null;

	public boolean isConstructor = false;
	public boolean isStatic = false;
	
	private CmlBehaviourThread currentlyExecutingThread = null;

	public CmlOperationValue(AExplicitCmlOperationDefinition def,
		FunctionValue precondition, FunctionValue postcondition,
		AStateDefinition state)
	{
		this.expldef = def;
		this.impldef = null;
		this.name = def.getName();
		this.type = (AOperationType)def.getType();
		this.paramPatterns = def.getParameterPatterns();
		this.setBody(def.getBody());
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.state = state;
	}

	public CmlOperationValue(AImplicitCmlOperationDefinition def,
		FunctionValue precondition, FunctionValue postcondition,
		AStateDefinition state)
	{
		this.impldef = def;
		this.expldef = null;
		this.name = def.getName();
		this.type = (AOperationType)def.getType();
		this.paramPatterns = new Vector<PPattern>();

		for (APatternListTypePair ptp : def.getParameterPatterns())
		{
			getParamPatterns().addAll(ptp.getPatterns());
		}

		this.precondition = precondition;
		this.postcondition = postcondition;
		this.state = state;
	}
	
	public PAction getBody() {
		return body;
	}

	private void setBody(PAction body) {
		this.body = body;
	}

	@Override
	public String toString()
	{
		return getType().toString();
	}

	public void setSelf(ObjectValue self)
	{
		if (!isStatic)
		{
			this.self = self;
		}
	}

	public ObjectValue getSelf()
	{
		return self;
	}
	
	@Override
	public boolean equals(Object other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int hashCode() {
		return type.hashCode();
	}

	@Override
	public String kind() {
		return "cml operation";
	}

	@Override
	public Object clone() {
		if (expldef != null)
		{
			return new CmlOperationValue(expldef, precondition, postcondition,
				state);
		}
		else
		{
			return new CmlOperationValue(impldef, precondition, postcondition,
				state);
		}
	}

	public List<PPattern> getParamPatterns() {
		return paramPatterns;
	}

	public AOperationType getType() {
		return type;
	}

	public CmlBehaviourThread getCurrentlyExecutingThread() {
		return currentlyExecutingThread;
	}

	public void setCurrentlyExecutingThread(CmlBehaviourThread currentlyExecutingThread) {
		this.currentlyExecutingThread = currentlyExecutingThread;
	}

//	public RootContext newContext(LexLocation from, String title, Context ctxt)
//	{
//		RootContext argContext;
//
//		if (self != null)
//		{
//			argContext = new ObjectContext(from, title, ctxt, self);
//		}
//		else if (classdef != null)
//		{
//			argContext = new ProcessContext(from, title, ctxt, classdef);
//		}
//		else
//		{
//			argContext = new StateContext(from, title, ctxt, stateContext);
//		}
//
//		return argContext;
//	}
	
}
