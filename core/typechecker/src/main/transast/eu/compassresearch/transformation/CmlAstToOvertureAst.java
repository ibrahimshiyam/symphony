package eu.compassresearch.transformation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.compassresearch.ast.analysis.AnalysisException;
import eu.compassresearch.ast.analysis.AnswerAdaptor;
import eu.compassresearch.ast.definitions.AClassParagraphDefinition;
import eu.compassresearch.ast.definitions.PDefinition;
import eu.compassresearch.ast.expressions.ABooleanLiteralExp;
import eu.compassresearch.ast.expressions.ABracketedExp;
import eu.compassresearch.ast.expressions.ACaseAlternative;
import eu.compassresearch.ast.expressions.AModifyBinaryExp;
import eu.compassresearch.ast.expressions.ANameExp;
import eu.compassresearch.ast.expressions.ARecordExp;
import eu.compassresearch.ast.lex.LexCharacterToken;
import eu.compassresearch.ast.lex.LexIdentifierToken;
import eu.compassresearch.ast.lex.LexIntegerToken;
import eu.compassresearch.ast.lex.LexNameToken;
import eu.compassresearch.ast.lex.LexQuoteToken;
import eu.compassresearch.ast.lex.LexToken;
import eu.compassresearch.ast.node.GraphNodeList;
import eu.compassresearch.ast.node.NodeListList;
import eu.compassresearch.ast.types.AAccessSpecifier;
import eu.compassresearch.ast.types.PType;

import org.overture.ast.definitions.AClassClassDefinition;
import org.overture.ast.definitions.PAccess;
import org.overture.ast.expressions.ABooleanConstExp;
import org.overture.ast.expressions.AFieldExp;
import org.overture.ast.expressions.AMkBasicExp;
import org.overture.ast.expressions.AMkTypeExp;
import org.overture.ast.expressions.AMuExp;
import org.overture.ast.expressions.APlusPlusBinaryExp;
import org.overture.ast.expressions.AVariableExp;
import org.overture.ast.expressions.PExp;
import org.overture.ast.lex.LexBooleanToken;
import org.overture.ast.lex.LexLocation;
import org.overture.ast.node.INode;
import org.overture.ast.node.NodeList;
import org.overture.ast.node.tokens.TAsync;
import org.overture.ast.node.tokens.TStatic;
import org.overture.ast.patterns.PPattern;
import org.overture.ast.types.AAccessSpecifierAccessSpecifier;

public class CmlAstToOvertureAst extends AnswerAdaptor<INode>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Map<org.overture.ast.node.INode, eu.compassresearch.ast.node.INode> nodeMap;
	public Map<org.overture.ast.node.INode, eu.compassresearch.ast.node.INode> getNodeMap()
	{
		return nodeMap;
	}
	
	public CmlAstToOvertureAst()
	{
		this.nodeMap = new HashMap<org.overture.ast.node.INode, eu.compassresearch.ast.node.INode>();
	}
	
	private static Class<?>[] typesFromArgs(Object[] args){
		if (args == null) return new Class<?>[0];
		Class<?>[] res = new Class<?>[args.length];
		for(int i = 0; i < args.length; i++)
		{
			Object o = args[i];
			if (o == null) continue;
			res[i] = o.getClass();
		}
		return res;
	}


	private static <T> T create(Class<T> clz,Object... params) throws Exception
	{
		Class<?>[] argTypes = typesFromArgs(params);
		Constructor<T> ctor = clz.getConstructor(argTypes);
		return ctor.newInstance(params);
	}

	private Class<?> loadOvtEquivalent(Class<?> cmlClz) throws AnalysisException
	{
		String cmlName = cmlClz.getName();
		String ovtName = cmlName.replace("eu.compassresearch", "org.overture");

		// Dark magic
		ClassLoader loader = getClass().getClassLoader();
		Class<?> ovtClz = null;
		try {
			ovtClz = loader.loadClass(ovtName);
		} catch (ClassNotFoundException e) {
			throw new AnalysisException(e);
		}

		return ovtClz;
	}



	@Override
	public INode defaultINode(eu.compassresearch.ast.node.INode node)
			throws AnalysisException {

		try {
			Class<?> cmlClz = node.getClass();

			Class<?> ovtClz = loadOvtEquivalent(cmlClz);
			Object ovtInstance = null;

			ovtInstance =create(ovtClz);

			nodeMap.put((INode)ovtInstance, node);

			// In the hope of nice code style
			for(Method om : ovtClz.getMethods())
			{
				String n =om.getName(); 
				if (n.startsWith("set"))
				{
					for(Method cm : cmlClz.getMethods())
					{
						String wantedGetterName = om.getName().replace("set", "get");
						Class<?>[] oparamTypes = om.getParameterTypes();
						if (cm.getName().equals(wantedGetterName))
							if (oparamTypes.length == 1)
							{
								Object translated = null;
								Object cmlGetterRes = null;
								cmlGetterRes = cm.invoke(node, new Object[] {});
								if(cmlGetterRes == null) continue;
								translated = translate(cmlGetterRes);
								String s = translated.getClass().getName();
								om.invoke(ovtInstance, translated);
								break;
							}
					}
				}
			}

			return (INode)ovtInstance;
		}			
		catch (AnalysisException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new AnalysisException(e);
		}

	}

	// Special case
	private org.overture.ast.lex.LexIntegerToken convertLexIntegerToken(Object node) throws AnalysisException {
		org.overture.ast.lex.LexIntegerToken result = null;
		LexIntegerToken cmlInt = (eu.compassresearch.ast.lex.LexIntegerToken)node;
		LexLocation loc = (LexLocation)translate(cmlInt.location);
		result = new org.overture.ast.lex.LexIntegerToken(cmlInt.value, loc);
		return result;
	}

	private org.overture.ast.lex.LexLocation convertLexLocation(Object node) throws AnalysisException
	{
		LexLocation res = null;
		eu.compassresearch.ast.lex.LexLocation cmlLoc = (eu.compassresearch.ast.lex.LexLocation) node;
		res = new LexLocation(cmlLoc.file, cmlLoc.module, cmlLoc.startLine, cmlLoc.startPos, cmlLoc.endLine, cmlLoc.endPos, cmlLoc.startOffset, cmlLoc.endOffset);
		return res;
	}

	
	private org.overture.ast.node.NodeList<INode> convertNodeList(eu.compassresearch.ast.node.NodeList<eu.compassresearch.ast.node.INode> nl) throws AnalysisException
	{
		
		org.overture.ast.node.NodeList<INode> res = new NodeList<INode>(null);
		for(Object n : nl)
		{
			Object o = translate(n);
			res.add( (INode)o);
		}
		return res;
	}
	
	private Object translate(Object cmlGetterRes) throws AnalysisException {
		
		if (cmlGetterRes == null) return null;
		String clzz = cmlGetterRes.getClass().getName();
		if (!clzz.startsWith("eu.compassresearch")) return cmlGetterRes;
		else {
			Class<?> cmlClz = cmlGetterRes.getClass();
			if (LexIntegerToken.class == cmlClz) 
				return convertLexIntegerToken(cmlGetterRes);
			if (eu.compassresearch.ast.lex.LexLocation.class == cmlClz) 
				return convertLexLocation(cmlGetterRes);
			if (eu.compassresearch.ast.lex.LexToken.class == cmlClz) 
				return convertLexToken(cmlGetterRes);
			if (eu.compassresearch.ast.lex.LexCharacterToken.class == cmlClz) 
				return convertLexCharToken(cmlGetterRes);
			if (eu.compassresearch.ast.lex.LexNameToken.class == cmlClz)
				return convertLexNameToken((eu.compassresearch.ast.lex.LexNameToken)cmlGetterRes);
			if (eu.compassresearch.ast.node.NodeList.class == cmlClz) return convertNodeList((eu.compassresearch.ast.node.NodeList<eu.compassresearch.ast.node.INode>)cmlGetterRes);
			if (eu.compassresearch.ast.node.GraphNodeList.class == cmlClz) return convertGraphNodeList((eu.compassresearch.ast.node.GraphNodeList)cmlGetterRes);
			if (eu.compassresearch.ast.node.NodeListList.class == cmlClz) return convertNodeListList((eu.compassresearch.ast.node.NodeListList)cmlGetterRes);
			if (eu.compassresearch.ast.types.AAccessSpecifier.class == cmlClz) return convertAAccessSpecifier((eu.compassresearch.ast.types.AAccessSpecifier)cmlGetterRes);
			if (eu.compassresearch.ast.definitions.AClassParagraphDefinition.class == cmlClz) return convertClassParagraph( (eu.compassresearch.ast.definitions.AClassParagraphDefinition)cmlGetterRes);
			if (eu.compassresearch.ast.expressions.ABooleanLiteralExp.class == cmlClz) return convertBooleanLiteralExp( (eu.compassresearch.ast.expressions.ABooleanLiteralExp)cmlGetterRes);
			if (eu.compassresearch.ast.expressions.ARecordExp.class == cmlClz) return covertRecordExp( (eu.compassresearch.ast.expressions.ARecordExp)cmlGetterRes);
			if (eu.compassresearch.ast.expressions.ANameExp.class == cmlClz) return convertNameExp ( (eu.compassresearch.ast.expressions.ANameExp) cmlGetterRes);
			if (eu.compassresearch.ast.lex.LexQuoteToken.class == cmlClz) return convertLexQuoteToken( (eu.compassresearch.ast.lex.LexQuoteToken)cmlGetterRes);
			if (eu.compassresearch.ast.expressions.ABracketedExp.class == cmlClz) return convertBrackedExp( (eu.compassresearch.ast.expressions.ABracketedExp) cmlGetterRes);
			if (eu.compassresearch.ast.expressions.AModifyBinaryExp.class == cmlClz) return convertModifyBinaryExp( (eu.compassresearch.ast.expressions.AModifyBinaryExp)cmlGetterRes);
			if (eu.compassresearch.ast.expressions.ACaseAlternative.class == cmlClz) return convertACaseAlternative( (eu.compassresearch.ast.expressions.ACaseAlternative)cmlGetterRes);
			if (eu.compassresearch.ast.lex.LexIdentifierToken.class == cmlClz) return convertLexIdentifierToken( (eu.compassresearch.ast.lex.LexIdentifierToken)cmlGetterRes);
			
			else  return defaultINode((eu.compassresearch.ast.node.INode)cmlGetterRes);
		}
	}



	private Object convertLexIdentifierToken(LexIdentifierToken cmlGetterRes) throws AnalysisException {
		org.overture.ast.lex.LexIdentifierToken res = new org.overture.ast.lex.LexIdentifierToken(cmlGetterRes.getName(), false, (LexLocation)translate(cmlGetterRes.getLocation()));
		return res;
	}

	private Object convertACaseAlternative(ACaseAlternative cmlGetterRes) throws AnalysisException {
		
		org.overture.ast.expressions.ACaseAlternative res = new org.overture.ast.expressions.ACaseAlternative();
		res.setCexp( (PExp) translate(cmlGetterRes.getCexp()));
		res.setDefs((List)translate(cmlGetterRes.getDefs()));
		res.setExpType( (org.overture.ast.types.PType)translate(cmlGetterRes.getExpType()));
		res.setPattern( (PPattern) translate(cmlGetterRes.getPattern().get(0)));
		res.setResult( (PExp) translate(cmlGetterRes.getResult()));
		
		return res;
	}

	private Object convertModifyBinaryExp(AModifyBinaryExp cmlGetterRes) throws AnalysisException {
		
		org.overture.ast.expressions.APlusPlusBinaryExp res = new APlusPlusBinaryExp();
		res.setLeft((PExp)translate(cmlGetterRes.getLeft()));
		res.setOp( (org.overture.ast.lex.LexToken)translate(cmlGetterRes.getOp()));
		res.setRight( (PExp)translate(cmlGetterRes.getRight()));
		return res;
	}

	private Object convertBrackedExp(ABracketedExp cmlGetterRes) throws AnalysisException {
		return translate(cmlGetterRes.getExpression());
	}

	private Object convertLexQuoteToken(LexQuoteToken cmlGetterRes) throws AnalysisException {
		return new org.overture.ast.lex.LexQuoteToken(cmlGetterRes.value, (LexLocation)translate(cmlGetterRes.location));
	}

	private Object convertNameExp(ANameExp cmlGetterRes) throws AnalysisException {
		org.overture.ast.expressions.AFieldExp ovt = new AFieldExp();
		//ovt.setField( (org.overture.ast.lex.LexIdentifierToken)translate(cmlGetterRes.getName()));
		ovt.setMemberName( (org.overture.ast.lex.LexNameToken) translate(cmlGetterRes.getName()));
		return ovt;
	}

	private Object covertRecordExp(ARecordExp cmlGetterRes) throws AnalysisException {
		// FIXME: This translation is a bit off setRecordType on Overture exp not done
		org.overture.ast.expressions.AMkTypeExp o = new AMkTypeExp(); 
		o.setArgs((List)translate(cmlGetterRes.getExprs()));
		o.setType( (org.overture.ast.types.PType) translate(cmlGetterRes.getType()));
		o.setTypeName( (org.overture.ast.lex.LexNameToken) translate ( cmlGetterRes.getName() ) );
		return o;
	}

	private Object convertBooleanLiteralExp(ABooleanLiteralExp cmlGetterRes) throws AnalysisException {
		
		LexLocation loc = (org.overture.ast.lex.LexLocation)translate(cmlGetterRes.getLocation());
		org.overture.ast.expressions.ABooleanConstExp e = new ABooleanConstExp();
		e.setLocation(loc);
		e.setValue(new LexBooleanToken(cmlGetterRes.getValue().value, loc ));
		return e;
	}


	private Object convertClassParagraph(AClassParagraphDefinition cmlGetterRes) throws AnalysisException {
		List<org.overture.ast.definitions.PDefinition> defs = new LinkedList<org.overture.ast.definitions.PDefinition>();
		org.overture.ast.definitions.AClassClassDefinition ovtClass = new AClassClassDefinition();
		for(PDefinition def : cmlGetterRes.getDefinitions())
		{
			defs.add( (org.overture.ast.definitions.PDefinition) translate(def));
		}
		ovtClass.setDefinitions(defs);
		return ovtClass;
	}


	private Object convertLexNameToken(LexNameToken cmlGetterRes) throws AnalysisException {
		return new org.overture.ast.lex.LexNameToken(cmlGetterRes.module, cmlGetterRes.name, convertLexLocation(cmlGetterRes.location),false,false);
	}


	private Object convertNodeListList(NodeListList cmlGetterRes) throws AnalysisException {
		
		org.overture.ast.node.NodeListList res = new org.overture.ast.node.NodeListList(null);
		for(Object o : cmlGetterRes)
		{
			Object trans = translate(o);
			res.add((List<Object>)trans);
		}
		return res;
	}


	private Object convertLexCharToken(Object cmlGetterRes) throws AnalysisException {
		eu.compassresearch.ast.lex.LexCharacterToken cml = (LexCharacterToken) cmlGetterRes;
		org.overture.ast.lex.LexCharacterToken ovt = new org.overture.ast.lex.LexCharacterToken(cml.unicode, convertLexLocation(cml.location));
		return ovt;
	}


	private Object convertGraphNodeList(GraphNodeList cmlGetterRes) throws AnalysisException {
		org.overture.ast.node.GraphNodeList res = new org.overture.ast.node.GraphNodeList<INode>(null);
		for (Object node : cmlGetterRes)
		{
			Object o = translate(node);
			res.add(o);
		}
		return res;
	}


	private Object convertAAccessSpecifier(AAccessSpecifier cmlGetterRes) throws AnalysisException {
		PAccess access_ = (PAccess)translate((eu.compassresearch.ast.definitions.PAccess)cmlGetterRes.getAccess());
		TStatic static_ = (TStatic)translate((eu.compassresearch.ast.node.tokens.TStatic)cmlGetterRes.getStatic());
		TAsync async_   = (TAsync)translate((eu.compassresearch.ast.node.tokens.TAsync)cmlGetterRes.getAsync());
		AAccessSpecifierAccessSpecifier res = new AAccessSpecifierAccessSpecifier(access_, static_, async_);
		return res;
	}


	private org.overture.ast.lex.VDMToken fromCml(eu.compassresearch.ast.lex.VDMToken t)
	{
		for(org.overture.ast.lex.VDMToken ot : org.overture.ast.lex.VDMToken.values())
			if (t.name().equals(ot.name())) return ot;
		return null;
	}

	private org.overture.ast.lex.LexToken convertLexToken(Object cmlGetterRes) throws AnalysisException {
		org.overture.ast.lex.LexToken res = null;
		eu.compassresearch.ast.lex.LexToken cmlTok = (LexToken) cmlGetterRes;
		org.overture.ast.lex.VDMToken vdmTok = fromCml(cmlTok.type);
		org.overture.ast.lex.LexLocation loc = (LexLocation) translate(cmlTok.location);
		res = new org.overture.ast.lex.LexToken(loc,vdmTok);
		return res;
	}



}