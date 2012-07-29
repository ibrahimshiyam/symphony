package eu.compassresearch.core.parser;

import eu.compassresearch.ast.expressions.*;
import eu.compassresearch.ast.statements.*;
import eu.compassresearch.ast.actions.*;
import eu.compassresearch.ast.process.*;
import eu.compassresearch.ast.lex.*; 
import java.util.*;

public class Path
{
    public class PathConvertException extends Exception
    {
	public PathConvertException(String message){
	    super(message);
	}
    }

    public enum PathKind {
	UNIT,
	TILDE,
	DOT,
	BACKTICK,
	DOTHASH,
	APPLY,
	SEQRANGE;
    }

    public final PathKind kind;
    public final Unit unit;
    public final Path subPath;
    public final List<PExp> expList;
    public final Integer numeral;
    //public final LexLocation location;

    public Path(PathKind kind, Unit unit)
    {
	this.unit = unit;
	this.kind = kind;
	expList = null;
	subPath = null;
	numeral = null;
	//location = unit.value.getLocation();
    }
    
    public Path(PathKind kind, Path subPath)
    {
	this.unit = null;
	this.kind = kind;
	expList = null;
	this.subPath = subPath;
	numeral = null;
    }
    
    public Path(PathKind kind, Path subPath, Integer numeral)
    {
	this.unit = null;
	this.kind = kind;
	expList = null;
	this.subPath = subPath;
	this.numeral = numeral;
	//location = unit.value.getLocation();
    }

    public Path(PathKind kind, Path subPath, Unit unit)
    {
	this.unit = unit;
	this.kind = kind;
	expList = null;
	this.subPath = subPath;
	numeral = null;
    }
    
    public Path(PathKind kind, Path subPath, List<PExp> expList)
    {
	this.unit = null;
	this.kind = kind;
	this.expList = expList;
	this.subPath = subPath;
	numeral = null;
    }

    /*
     * Public Methods
     */

    public PExp convertToExpression() throws PathConvertException
    {
	PExp exp = null;
	switch(kind){
	case UNIT:
	    {
		LexNameToken name = unit.convertToName();
		switch(unit.kind){
		case SELF:
		    exp = new ASelfExp(name.getLocation(),name);
		    break;
		case IDENTIFIER:
		    exp = new ANameExp(name.getLocation(),name);
		    break;
		}
	    }
	    break;
	case TILDE:
	    {
		PExp name = this.subPath.convertToExpression();
		switch(name.kindPExp()){
		case NAME:
		    ANameExp nameExp = (ANameExp)name;
		    nameExp.setName(nameExp.getName().getOldName());
		    exp = nameExp;
		    break;
		default:
		    throw new PathConvertException("Illigal path for old name expression");
		}
	    }
	    break;
	case DOT:
	    {
		PExp root = this.subPath.convertToExpression();
		//TODO: The location is not correct
		exp = new AFieldExp(root.getLocation(), 
				    root, 
				    null/*LexNameToken memberName_???*/,
				    this.unit.value);
	    }
	    break;
	case BACKTICK:
	    if (this.subPath.kind == PathKind.UNIT){
		LexNameToken name = this.unit.convertToName(subPath.unit.value.getName());
		exp = new ANameExp(name.getLocation(),name);
	    }
	    else{
		throw new PathConvertException("Illigal path for expression");
	    }
	    break;
	case DOTHASH:
	    {
		PExp tuple = this.subPath.convertToExpression();
		//TODO: The location is not correct
		exp = new ATupleSelectExp(null/*PType type_*/, 
					  tuple.getLocation(), 
					  tuple, 
					  numeral);
	    }
	    break;
	case APPLY:
	    {
		PExp root = this.subPath.convertToExpression();
		//TODO: The location is not correct
		exp = new AApplyExp(root.getLocation(), 
				    root, 
				    expList);
	    }
	    break;
	case SEQRANGE:
	    {
		PExp seq = this.subPath.convertToExpression();

		if(this.expList.size() != 2)
		    throw new PathConvertException("A subset sequence expression must have 2 expression arguments");

		//TODO: The location is not correct
		exp = new ASubseqExp(seq.getLocation(), 
				     seq, 
				     this.expList.get(0), 
				     this.expList.get(1));
	    }
	    break;
	}

	return exp;
    }
    
    public PStateDesignator convertToStateDesignator() throws PathConvertException
    {
	if(unit != null && unit.kind == Unit.UnitKind.SELF)
	    throw new PathConvertException("stateDesignators cannot contains self expressions");
	
	PStateDesignator sd = null;
	switch(kind){
	case UNIT:
	    {
		LexNameToken name = unit.convertToName();
		sd = new AIdentifierStateDesignator(name.getLocation(), 
						    name);
	    }
	    break;
	case DOT:
	    {
		PStateDesignator object = this.subPath.convertToStateDesignator();
		LexLocation location = combineLexLocation(object.getLocation(),
							  unit.value.getLocation());

		sd = new AFieldStateDesignator(location, 
					       object, 
					       unit.value);
	    }
	    break;
	case BACKTICK:
	    {
		LexNameToken name = convertToName();
		sd = new AIdentifierStateDesignator(name.getLocation(), 
						    name);
	    }
	    break;
	case APPLY:
	    {
		PStateDesignator object = this.subPath.convertToStateDesignator();
		LexLocation location = object.getLocation();
		
		if(this.expList.size() != 1)
		    throw new PathConvertException("A map or sequence reference must have 1 argument");
		sd = new  AMapSeqStateDesignator(location, 
						 object, 
						 this.expList.get(0));
	    }
	    break;
	default:
	    throw new PathConvertException("Illigal path for stateDesignator : " + kind);
	}

	return sd;
    }

    public PProcess convertToProcess() throws PathConvertException
    {
	PProcess process = null;
	switch(kind){
	case UNIT:
	    {
		LexNameToken name = this.unit.convertToName();
		process = new AInstantiationProcess(name.getLocation(), 
						    null, 
						    name, 
						    null); 
	    }
	    break;
	// case DOT:
	//     {
	//     }
	//     break;
	// case BACKTICK:
	//     {
	//     }
	//     break;
	
	// This Apply form is : path LPAREN expressionList RPAREN
	// Which should be converted into
	// [ object designator '.' ] name '(' [ expressionList ] ')'
	    
	case APPLY:
	    {
		Pair<Path,LexNameToken> pair = this.subPath.extractPostfixName();
		LexNameToken name = pair.second;
		if(pair.first != null){
		    throw new PathConvertException("Illigal path for instantiation proces : ");
		}
		
		process = new AInstantiationProcess(name.getLocation(), 
						    null, 
						    name, 
						    this.expList);
	    }
	    break;
	default:
	    throw new PathConvertException("Illigal path for process : " + kind);
	}

	return process;
    }

    public PAction convertToAction() throws PathConvertException
    {
	PAction action = null;
	switch(kind){
	case UNIT:
	    {
		LexNameToken actionName = this.unit.convertToName();
		action = new AIdentifierAction(actionName.getLocation(), 
					       actionName);
	    }
	    break;
	// case DOT:
	//     {
	//     }
	//     break;
	// case BACKTICK:
	//     {
	//     }
	//     break;
	
	// This Apply form is : path LPAREN expressionList RPAREN
	// Which should be converted into
	// [ object designator '.' ] name '(' [ expressionList ] ')'
	    
	case APPLY:
	    {
		Pair<Path,LexNameToken> pair = this.subPath.extractPostfixName();

		PObjectDesignator objectDesignator = null;

		//if this holds we need to exstract the ObjectDesignator 
		//from the returned path 
		if(pair.first != null){
		    objectDesignator = pair.first.convertToObjectDesignator();
		}
		
		action = new ACallControlStatementAction(pair.second.getLocation(), 
							 objectDesignator, 
							 pair.second, 
							 this.expList);
	    }
	    break;
	default:
	    throw new PathConvertException("Illigal path for action : " + kind);
	}

	return action;
    }

    private class Pair<F, S> {
	public final F first; //first member of pair
	public final S second; //second member of pair
	
	public Pair(F first, S second) {
	    this.first = first;
	    this.second = second;
	}
    }

    //names: ['.'] id
    //       ['.'] id`id
    private Pair<Path, LexNameToken> extractPostfixName() throws PathConvertException
    {
	LexNameToken name = null;
	Path path = null;
	switch(kind){
	case UNIT:
	    {
		name = this.unit.convertToName();
	    }
	    break;
	
	// case BACKTICK:
	//     {
	// 	if (this.subPath.kind == PathKind.UNIT){
	// 	    name = this.unit.convertToName(subPath.unit.value.getName());
	// 	}
	// 	else{
	// 	    throw new PathConvertException("Illigal path for expression");
	// 	}
	//     }
	//     break;
	default:
	    throw new PathConvertException("Illigal path for call : " + kind);
	}
	return new Pair<Path,LexNameToken>(path,name);
    }
    
    public LexNameToken convertToName() throws PathConvertException
    {
	LexNameToken name = null;
	switch(kind){
	case UNIT:
	    {
		name = unit.convertToName();
	    }
	    break;
	// case TILDE:
	//     {
	//     }
	//     break;
	
	case BACKTICK:
	    {
		if (this.subPath.kind == PathKind.UNIT){
		    name = this.unit.convertToName(subPath.unit.value.getName());
		}
		else{
		    throw new PathConvertException("Illigal path for expression");
		}
	    }
	    break;
	default:
	    throw new PathConvertException("Illigal path for name : " + kind);
	}

	return name;
    }

    public PObjectDesignator convertToObjectDesignator() throws PathConvertException
    {
	PObjectDesignator od = null;
	switch(kind){
	// case UNIT:
	//     {
	
	//     }
	//     break;
	// case TILDE:
	//     {
	//     }
	//     break;
	// case DOT:
	//     {
	//     }
	//     break;
	// case BACKTICK:
	//     {
	//     }
	//     break;
	// case DOTHASH:
	//     {
	//     }
	//     break;
	// case APPLY:
	//     {
	//     }
	//     break;
	// case SEQRANGE:
	//     {
	//     }
	//     break;
	default:
	    throw new PathConvertException("Illigal path for objectDesignator : " + kind);
	}

	//return od;
    }

    /*
     * Private Methods
     */

    private LexLocation combineLexLocation(LexLocation start, LexLocation end)
    {
	return new LexLocation(start.resource, "Default",
			       start.startLine, start.startPos,
			       end.endLine, 
			       end.endPos,
			       start.startOffset,
			       end.endOffset);
    }


}