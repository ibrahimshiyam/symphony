package eu.compassresearch.core.typechecker;

import java.util.LinkedList;
import java.util.List;

import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.analysis.QuestionAnswerAdaptor;
import org.overture.ast.assistant.type.PTypeAssistant;
import org.overture.ast.definitions.AAssignmentDefinition;
import org.overture.ast.definitions.AExplicitFunctionDefinition;
import org.overture.ast.definitions.AImplicitFunctionDefinition;
import org.overture.ast.definitions.ALocalDefinition;
import org.overture.ast.definitions.APerSyncDefinition;
import org.overture.ast.definitions.PDefinition;
import org.overture.ast.expressions.AApplyExp;
import org.overture.ast.expressions.AFuncInstatiationExp;
import org.overture.ast.expressions.AIsExp;
import org.overture.ast.expressions.ANilExp;
import org.overture.ast.expressions.ASelfExp;
import org.overture.ast.expressions.AVariableExp;
import org.overture.ast.expressions.PExp;
import org.overture.ast.factory.AstFactory;
import org.overture.ast.lex.LexIdentifierToken;
import org.overture.ast.lex.LexNameToken;
import org.overture.ast.node.INode;
import org.overture.ast.typechecker.NameScope;
import org.overture.ast.types.AClassType;
import org.overture.ast.types.AFunctionType;
import org.overture.ast.types.AOperationType;
import org.overture.ast.types.AUnknownType;
import org.overture.ast.types.PType;
import org.overture.ast.types.SMapType;
import org.overture.ast.types.SSeqType;
import org.overture.ast.util.PTypeSet;
import org.overture.config.Release;
import org.overture.config.Settings;
import org.overture.parser.messages.VDMError;
import org.overture.typechecker.LexNameTokenAssistent;
import org.overture.typechecker.TypeCheckException;
import org.overture.typechecker.TypeCheckInfo;
import org.overture.typechecker.TypeChecker;
import org.overture.typechecker.TypeCheckerErrors;
import org.overture.typechecker.assistant.definition.PDefinitionAssistantTC;
import org.overture.typechecker.assistant.expression.AApplyExpAssistantTC;
import org.overture.typechecker.assistant.type.AFunctionTypeAssistantTC;
import org.overture.typechecker.assistant.type.AOperationTypeAssistantTC;
import org.overture.typechecker.assistant.type.PTypeAssistantTC;
import org.overture.typechecker.visitor.TypeCheckerExpVisitor;

import eu.compassresearch.ast.analysis.QuestionAnswerCMLAdaptor;
import eu.compassresearch.ast.definitions.AClassDefinition;
import eu.compassresearch.ast.definitions.SCmlOperationDefinition;
import eu.compassresearch.ast.expressions.ABracketedExp;
import eu.compassresearch.ast.expressions.AEnumVarsetExpression;
import eu.compassresearch.ast.expressions.AUnresolvedPathExp;
import eu.compassresearch.ast.types.AChannelType;
import eu.compassresearch.ast.types.AErrorType;
import eu.compassresearch.core.typechecker.api.CmlTypeChecker;
import eu.compassresearch.core.typechecker.api.TypeComparator;
import eu.compassresearch.core.typechecker.api.TypeErrorMessages;
import eu.compassresearch.core.typechecker.api.TypeIssueHandler;
import eu.compassresearch.core.typechecker.api.TypeWarningMessages;

class TCExpressionVisitor extends
QuestionAnswerCMLAdaptor<org.overture.typechecker.TypeCheckInfo, PType> {

	private TypeComparator typeComparator;
	
	
	@Override
	public PType caseANilExp(ANilExp node, TypeCheckInfo question)
			throws AnalysisException {
		return node.getType();
	}



	
	
	@Override
	public PType caseAApplyExp(AApplyExp node, TypeCheckInfo question)
			throws AnalysisException {

		/*
		 * The following is copied from Overture TypeCheckerExpVisitor
		 * 
		 */
		for (PExp a : node.getArgs()) {
			question.qualifiers = null;
			node.getArgtypes().add(a.apply(parent, question));
		}

		node.setType(node.getRoot().apply(
				parent,
				new TypeCheckInfo(question.env, question.scope, node
						.getArgtypes())));

		if (PTypeAssistantTC.isUnknown(node.getType())) {
			return node.getType();
		}

		PDefinition func = question.env.getEnclosingDefinition();

		boolean inFunction = (func instanceof AExplicitFunctionDefinition
				|| func instanceof AImplicitFunctionDefinition || func instanceof APerSyncDefinition);

		if (inFunction) {
			LexNameToken called = null;

			if (node.getRoot() instanceof AVariableExp) {
				AVariableExp var = (AVariableExp) node.getRoot();
				called = var.getName();
			} else if (node.getRoot() instanceof AFuncInstatiationExp) {
				AFuncInstatiationExp fie = (AFuncInstatiationExp) node
						.getRoot();

				if (fie.getExpdef() != null) {
					called = fie.getExpdef().getName();
				} else if (fie.getImpdef() != null) {
					called = fie.getImpdef().getName();
				}
			}

			if (called != null) {
				if (func instanceof AExplicitFunctionDefinition) {
					AExplicitFunctionDefinition def = (AExplicitFunctionDefinition) func;

					if (LexNameTokenAssistent.isEqual(called, def.getName())) {
						node.setRecursive(def);
						def.setRecursive(true);
					}
				} else if (func instanceof AImplicitFunctionDefinition) {
					AImplicitFunctionDefinition def = (AImplicitFunctionDefinition) func;

					if (LexNameTokenAssistent.isEqual(called, def.getName())) {
						node.setRecursive(def);
						def.setRecursive(true);
					}
				}
			}
		}

		boolean isSimple = !PTypeAssistantTC.isUnion(node.getType());
		PTypeSet results = new PTypeSet();

		if (PTypeAssistantTC.isFunction(node.getType())) {
			AFunctionType ft = PTypeAssistantTC.getFunction(node.getType());
			AFunctionTypeAssistantTC.typeResolve(ft, null, (QuestionAnswerAdaptor<TypeCheckInfo, PType>) parent,
					question);
			results.add(AApplyExpAssistantTC.functionApply(node, isSimple, ft));
		}

		if (PTypeAssistantTC.isOperation(node.getType())) {
			AOperationType ot = PTypeAssistantTC.getOperation(node.getType());
			AOperationTypeAssistantTC.typeResolve(ot, null, (QuestionAnswerAdaptor<TypeCheckInfo, PType>) parent,
					question);

			if (inFunction && Settings.release == Release.VDM_10) {
				TypeCheckerErrors.report(3300, "Operation '" + node.getRoot()
						+ "' cannot be called from a function",
						node.getLocation(), node);
				results.add(AstFactory.newAUnknownType(node.getLocation()));
			} else {
				results.add(AApplyExpAssistantTC.operationApply(node, isSimple,
						ot));
			}
		}

		if (PTypeAssistantTC.isSeq(node.getType())) {
			SSeqType seq = PTypeAssistantTC.getSeq(node.getType());
			results.add(AApplyExpAssistantTC.sequenceApply(node, isSimple, seq));
		}

		if (PTypeAssistantTC.isMap(node.getType())) {
			SMapType map = PTypeAssistantTC.getMap(node.getType());
			results.add(AApplyExpAssistantTC.mapApply(node, isSimple, map));
		}
		/*
		 * Overture copy STOP
		 * 
		 */
		
		// RWL: Type check an apply of a cml Operation (implicit and explicit)
		if (node instanceof SCmlOperationDefinition)
		{

			// Check the node is type checked and is an operation type
			if (node.getType() == null || !(node.getType() instanceof AOperationType))
			{
				node.setType(issueHandler.addTypeError(node, TypeErrorMessages.INCOMPATIBLE_TYPE.customizeMessage("Operation", ""+node.getType())));
				return node.getType();
			}
			
			// get type and check arg types
			AOperationType ot = (AOperationType)node.getType();
			LinkedList<PType> argTypes = node.getArgtypes();
			LinkedList<PType> typTypes = ot.getParameters();
			List<PExp> args = node.getArgs();
			
			if (argTypes.size() != typTypes.size())
			{
				node.setType(issueHandler.addTypeError(node, TypeErrorMessages.WRONG_NUMBER_OF_ARGUMENTS.customizeMessage(argTypes+"", typTypes+"")));
				return node.getType();
			}
			
			for(int i = 0; i < argTypes.size();++i)
			{
				if (!typeComparator.isSubType(argTypes.get(i), typTypes.get(i)))
				{
					node.setType(issueHandler.addTypeError(args.get(i), TypeErrorMessages.INCOMPATIBLE_TYPE.customizeMessage(""+typTypes.get(i),""+argTypes.get(i))));
					return node.getType();
				}
			}
			
			// set the result of this apply expression to the return type of the operation.
			results.add(ot.getResult());
		}
		
		if (results.isEmpty()) {
			TypeCheckerErrors.report(3054, "Type " + node.getType()
					+ " cannot be applied", node.getLocation(), node);
			return AstFactory.newAUnknownType(node.getLocation());
		}

		
		node.setType(results.getType(node.getLocation()));
		return node.getType(); // Union of possible applications
	}





	@Override
	public PType caseAEnumVarsetExpression(AEnumVarsetExpression node,
			TypeCheckInfo question) throws AnalysisException {

		LinkedList<LexIdentifierToken> ids = node.getIdentifiers();
		List<PDefinition> defs = new LinkedList<PDefinition>();
		for(LexIdentifierToken id : ids)
		{
			ALocalDefinition idDef = AstFactory.newALocalDefinition(node.getLocation(), (LexNameToken) id, NameScope.LOCAL, AstFactory.newAUnknownType(node.getLocation()));
			defs.add(idDef);
		}
		
		// TODO RWL I am not really sure what to do here ?
		issueHandler.addTypeWarning(node, TypeWarningMessages.INCOMPLETE_TYPE_CHECKING.customizeMessage(""+node));
		
		AChannelType result = new AChannelType();
		result.setDefinitions(defs);
		return result;
	}



	@Override
	public PType caseAVariableExp(AVariableExp node, TypeCheckInfo question)
			throws AnalysisException {

		// for convenience take out name and env
		org.overture.typechecker.Environment env = question.env;
		LexNameToken name = node.getName();

		// the qualifiers are the argument names for potential function 
		// or operation... We wish to find an abstraction with the right args.
		name.setTypeQualifier(question.qualifiers);

		// The defintion is set on the VariableExp for convenience 
		node.setVardef(env.findName(name,question.scope));

		do {

			// definition successfully found with qualifiers on
			if (node.getVardef() != null) break;

			// The name was not found immediately, it could be a sequence or map
			// in which case the qualifiers are not supposed to be set. See 
			// TypeCheckExpVisitor.java:2510,
			if (question.qualifiers != null)
			{
				name.setTypeQualifier(null);
				node.setVardef(env.findName(name, question.scope));
				if (node.getVardef() == null) name.setTypeQualifier(question.qualifiers);
				else break;
			}

			// Definition still not found, we may be looking for a bare function/op "x",
			// when in fact there is one with a qualified name "x(args)". So we check the 
			// possible matches - if there is precisely one, we pick it else we raise an 
			// ambiguity error. See TypeCheckerExpVisitor.java: 2527
			//
			//
			// RWL: Update we can't do this as CML Definitions makes the findMatches function
			// fail in the PDefinitionAssistantTC.java:87 as a switch does not have CML cases and
			// returns null which are added to the list ... (sick, there should be a check)
			//
			// 
			//			for(PDefinition possible : env.findMatches(name))
			//			{
			//				if (PDefinitionAssistantTC.isFunctionOrOperation(possible))
			//				{
			//					if (node.getVardef() != null)
			//						return issueHandler.addTypeError(node, "Ambiguous function/operation name: "+name);
			//					node.setVardef(possible);
			//				}
			//			}
			//			if (node.getVardef() != null)
			//				break;
			//
			// Hopefully the CmlTypeCheckInfo will find what we are looking for.
			// Manual search:

			node.setVardef(CmlTCUtil.findNearestFunctionOrOperationInEnvironment(name, env));
		} while(false);

		// The name this variable expressions points to was found.
		if (node.getVardef() != null)
		{
			
			PType type = PDefinitionAssistantTC.getType(node.getVardef());
			if (type == null)
				type = node.getVardef().getType();
			try {
			node.setType(PTypeAssistantTC.typeResolve(type, null, (QuestionAnswerAdaptor<TypeCheckInfo, PType>) parent, question));
			} catch (TypeCheckException tce)
			{
				node.setType(issueHandler.addTypeError(node, tce.getMessage()));
			}
			return node.getType();
		}

		// Okay given our best efforts the Overture Type Checking strategy could not find
		// what we are looking for. Maybe its a CML class we are looking at.
		CmlTypeCheckInfo nearestCmlEnvironment = question.contextGet(CmlTypeCheckInfo.class);
		if (nearestCmlEnvironment == null)
		{
			node.setType(issueHandler.addTypeError(node,TypeErrorMessages.ILLEGAL_CONTEXT.customizeMessage(node+"")));
			return node.getType();
		}

		// CML also uses LexNameToken.equals to compare names therefore the 
		// qualifiers must be taking in to account.
		PDefinition definition  = null;
		do {
			// Lookup in the CML context without the type qualifiers (arg types/param names)
			name.setTypeQualifier(null);
			definition = nearestCmlEnvironment.lookup(name, PDefinition.class);
			if (definition != null ) break; else name.setTypeQualifier(question.qualifiers);

			// Lookup in the CML context with qualifiers (for operations)
			definition = nearestCmlEnvironment.lookup(name, PDefinition.class);

		} while(false);

		// any luck?
		if (definition != null)
		{
			node.setVardef(definition);
			node.setType(definition.getType());
		}
		else // guess not
		{
			name.setTypeQualifier(null);
			node.setType(issueHandler.addTypeError(node, TypeErrorMessages.UNDEFINED_SYMBOL.customizeMessage(name+"")));
		}

		return node.getType();
	}



	/**
	 * 
	 */
	private static final long serialVersionUID = -6509187123701383525L;

	// A parent checker may actually not be necessary on this
	final private CmlTypeChecker parent;
	private final TypeIssueHandler issueHandler;
	private CmlAssistant assist;

	TCExpressionVisitor(CmlTypeChecker parentChecker, TypeIssueHandler issueHandler, CmlAssistant assistant, TypeComparator typeComparator)
	{
		this.parent=parentChecker;
		this.issueHandler = issueHandler;
		this.assist = assistant;
		this.typeComparator = typeComparator;

	}

	TCExpressionVisitor(VanillaCmlTypeChecker parentChecker,
			TypeIssueHandler issueHandler, TypeComparator typeComparator) {
		this(parentChecker,issueHandler,new CmlAssistant(), typeComparator );
	}

	/**
	 * Type check expression using Overture's type checker.
	 * 
	 * @param node
	 *            - the expression to type check
	 * @param question
	 *            - environmental stuff, green trees whatever
	 * @return A type checked cml expression
	 * @throws AnalysisException
	 *             - if anythings goes wrong that is not just a type error.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public PType defaultPExp(PExp node,
			org.overture.typechecker.TypeCheckInfo question)
					throws AnalysisException {
		org.overture.typechecker.TypeChecker.clearErrors();

		INode ovtNode = node;

		OvertureRootCMLAdapter overtureExpVisitor = new OvertureRootCMLAdapter(
				parent, issueHandler);

		org.overture.typechecker.TypeCheckInfo quest = new org.overture.typechecker.TypeCheckInfo(
				question.env);
		quest.scope = NameScope.NAMES;
		quest.qualifiers = new LinkedList<PType>();
		try {
			OvertureRootCMLAdapter.pushQuestion(question);
			ovtNode.apply(overtureExpVisitor, quest);
			OvertureRootCMLAdapter.popQuestion(question);
		} catch (org.overture.ast.analysis.AnalysisException e1) {
			e1.printStackTrace();
		}

		if (org.overture.typechecker.TypeChecker.getErrorCount() > 0) {
			List<VDMError> errorList = TypeChecker.getErrors();
			for (VDMError err : errorList) {
				issueHandler.addTypeError(node, err.toProblemString());
			}
			return new AErrorType(node.getLocation(), true);
		}

		return node.getType();
	}


	@Override
	public PType caseAUnresolvedPathExp(AUnresolvedPathExp node,
			TypeCheckInfo question) throws AnalysisException {

		// So we are going to look up a path of the form <class>.<member> or <identifier>.<member>
		// To find that class there must be a CML Environment as Classes are 
		// top-level and CML Specific.
		// 


		CmlTypeCheckInfo cmlQuestion = CmlTCUtil.getCmlEnv(question);
		if (cmlQuestion == null)
		{
			node.setType(issueHandler.addTypeError(node, TypeErrorMessages.ILLEGAL_CONTEXT.customizeMessage(node+"")));
			return node.getType();
		}

		// All right lets get all the identifiers used in this path
		LinkedList<LexIdentifierToken> identifiers = node.getIdentifiers();

		// Get parent identifier
		LexNameToken rootName = new LexNameToken("",identifiers.get(0));

		// is it a type like a class or global type
		PDefinition root = question.env.findType(rootName,"");

		// no then it may be a variable 
		if (root == null)
			root = question.env.findName(rootName, NameScope.LOCAL);

		if (root == null)
			root= question.env.findName(rootName, NameScope.NAMES);

		if (root==null)
			root= question.env.findName(rootName, NameScope.GLOBAL);

		// RWL: UGLY Re-factor some day
		if (root instanceof AAssignmentDefinition)
		{
			AAssignmentDefinition adef = (AAssignmentDefinition)root;
			PType type = adef.getType();
			if (type instanceof AClassType)
			{
				AClassType clzType = (AClassType)type;
				root = question.env.findName(clzType.getName(), NameScope.GLOBAL);
			}
		}

		// last option it is not in something else then in must be in this class
		if (root == null)
		{
			root = question.env.getEnclosingDefinition();
			if (root != null)
				root = assist.findMemberName(root, rootName, cmlQuestion);
		}

		// Use Cml environment to determine what rootName is
		if (root == null)
			root = cmlQuestion.lookup(rootName, PDefinition.class);

		// did we find the top-level
		if (root == null)
		{
			node.setType(issueHandler.addTypeError(node, TypeErrorMessages.UNDEFINED_SYMBOL.customizeMessage(""+rootName)));
			return node.getType();
		}

		// Now the root identifier is resolved, lets look for the first member
		// We assume the identifiers are given in order with the outer most 
		// definitions coming first
		PType leafType = null;
		PDefinition prevRoot = null;
		for(int i = 1; i < identifiers.size();i++)
		{
			LexIdentifierToken id = identifiers.get(i);
			LexNameToken idName = new LexNameToken("",id);
			PDefinition def = assist.findMemberName(root, idName,cmlQuestion, prevRoot);
			if (def == null)
			{
				node.setType(issueHandler.addTypeError(node, TypeErrorMessages.UNDEFINED_SYMBOL.customizeMessage(id+" in "+ node)));
				return node.getType();
			}
			leafType = def.getType();
		}

		node.setType(leafType);
		return node.getType();
	}



	@Override
	public PType caseABracketedExp(ABracketedExp node, TypeCheckInfo question)
			throws AnalysisException {

		PType type = node.getExpression().apply(this, question);
		node.setType(type);
		return type;
	}

	
	/*
	 * Copied from Overture and modified to handle CML Classes.
	 * 
	 * (non-Javadoc)
	 * @see org.overture.ast.analysis.QuestionAnswerAdaptor#caseAIsExp(org.overture.ast.expressions.AIsExp, java.lang.Object)
	 */
	@Override
	public PType caseAIsExp(AIsExp node, TypeCheckInfo question)
			throws AnalysisException {

		question.qualifiers = null;
		node.getTest().apply(parent, question);

		PType basictype = node.getBasicType();

		if (basictype != null) {
			try
			{
			basictype = PTypeAssistantTC.typeResolve(basictype, null,
					(QuestionAnswerAdaptor<TypeCheckInfo, PType>) parent, question);
			} catch (TypeCheckException tce)
			{
				node.setType(issueHandler.addTypeError(node,tce.getMessage()));
				return node.getType();
			}
		}

		LexNameToken typename = node.getTypeName();

		if (typename != null) {
			PDefinition typeFound = question.env.findType(typename,
					node.getLocation().module);
			
			// It maybe an CML Class typically it will be lets look it up in the nearest cml environment
			if (typeFound == null) {
				CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question);
				typeFound = cmlEnv.lookup(typename, AClassDefinition.class);
			}
			
			if (typeFound == null) {
				TypeCheckerErrors.report(3113, "Unknown type name '" + typename
						+ "'", node.getLocation(), node);
				node.setType(node.getTest().getType());
				return node.getType();
			}
			node.setTypedef(typeFound.clone());

		}

		node.setType(AstFactory.newABooleanBasicType(node.getLocation()));
		return node.getType();
	}
	
	/*
	 * Copied from Overture and modified to lookup in surrounding CML environment if necessary.
	 * 
	 * (non-Javadoc)
	 * @see org.overture.ast.analysis.QuestionAnswerAdaptor#caseASelfExp(org.overture.ast.expressions.ASelfExp, java.lang.Object)
	 */
	@Override
	public PType caseASelfExp(ASelfExp node, TypeCheckInfo question) {
		
		
		PDefinition cdef = question.env
				.findName(node.getName(), question.scope);

		if (cdef == null)
		{
			// Get Cml Environment
			CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question);
			if (cmlEnv == null)
			{
				node.setType(issueHandler.addTypeError(node, TypeErrorMessages.ILLEGAL_CONTEXT.customizeMessage(""+node)));
				return node.getType();
			}
			
			// look up
			cdef = cmlEnv.lookup(node.getName(), PDefinition.class);
		}
		
		if (cdef == null) {
			TypeCheckerErrors.report(3154, node.getName() + " not in scope",
					node.getLocation(), node);
			node.setType(AstFactory.newAUnknownType(node.getLocation()));
			return node.getType();
		}

		node.setType(cdef.getType());
		return cdef.getType();
	}

}
