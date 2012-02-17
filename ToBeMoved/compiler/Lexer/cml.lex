package eu.compassresearch.cml.compiler;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Vector;
import eu.compassresearch.cml.compiler.CmlParser.Lexer;
import eu.compassresearch.cml.compiler.CmlParser.Location;

/*
class CmlContext {
  
}
*/
class LexicographicalRuntimeException extends RuntimeException
{
  private int pos,line;
  private  String value;
  public LexicographicalRuntimeException(String chars)
    {
      this.pos = CMLToken.curPos;
      this.line = CMLToken.curLine;
      this.value = chars;
    }
  
  @Override
    public String toString()
    {
      return "Offending syntax "+(value != null ? "starting with \""+value+"\" ":"")+"found at line "+(line+1)+" position "+(pos+1);
    }
}

class CmlLexeme {
  
    private Position startPos;
    private Position endPos;
    protected  String value;
    private int lex;

    public CmlLexeme(Position startPos, Position endPos, int lex, String value)
    {
	this.value = value;
	this.startPos = startPos;
	this.endPos = endPos;
	this.lex = lex;
    }

    public Position getStartPos()
    {
	return startPos;
    }

    public Position getEndPos()
    {
	return endPos;
    }

    public int getLexValue()
    {
	return lex;
    }
    
    public String getValue()
    {
	return this.value;
    }

    public String toString()
    {
	return value + " " + startPos;
    }
}

class CMLToken {
  public static int curLine;
  public static int curPos; 

  private int line;
  private int pos;
  protected  String value;
  public CMLToken(String value)
  {
    this.value = value;
    this.line = line;
    this.pos = pos;
  }

  public String getValue()
  {
    return this.value;
  }

  public static void main(String[] args) throws Exception
  {
    try{
    new CmlLexer(System.in).yylex();
    } catch (LexicographicalRuntimeException e)
	{
	  System.out.println(e);
	}
  }
}

class StringToken extends CMLToken {
  static StringToken currentString;
  private StringBuilder currentLine;
  private List<String> lines = new LinkedList<String>();

  private int endLine;
  private int endPos;

  public StringToken(int startLine, int startPos)
  {
    super("\"");
    currentLine = new StringBuilder();
  }

  public void append(String chars)
  {
    currentLine.append(chars);
  }

  public void newLine()
  {
    lines.add(currentLine.toString());
    currentLine = new StringBuilder();
  }

  public void endString(int line, int pos)
  {
    lines.add(currentLine.toString());
    this.endLine = line;
    this.endPos = pos;
    StringBuilder sb = new StringBuilder();
    for(String s : lines)
      sb.append(s);
    super.value = sb.toString();
    lines=null;
  }
}

class CommentBlock extends CMLToken {
  static CommentBlock current;
  private int level;
  StringBuilder content;

  public CommentBlock()
  {
    super("--");
    current=this;
    content=new StringBuilder();
    level = 1;
  }

  public void appendLine(String line)
  {
    this.content.append(line);
  }

  public void increaseLevel()
  {
    level ++;
  }

  public boolean decreaseLevel()
  {
    level--;
    return level == 0;
  }
}

%%
// ******************************************
// *** JFLEX SCANNER GENERATOR DIRECTIVES ***
// ******************************************
// The Lexer output class name 
%class CmlLexer
//The Lexer interface generated by the parser
%implements Lexer
//input files will be in unicode
%unicode
 //used when interfacing with bison  
%byaccj
// yylex return CmlLexeme
//%type CmlLexeme 
// all JFLEX internal operations, including yylex, are defined as private
//%apiprivate
%line
%column
%char
 //%debug
%public

%{
  static private Stack<int> stateStack = new Stack<int>();
  
  // placeholder for the reserved word (keyword) table
  static private HashMap<String,Integer> keywords = null;
  
  // initialize the reserved word table as a static constructor
  static {
    keywords = new HashMap<String,Integer>();
    
    keywords.put("abs", CmlParser.ABS);
    //keywords.put("all", CmlParser.ALL);
    //keywords.put("always", CmlParser.ALWAYS);
    keywords.put("and", CmlParser.AND);
    keywords.put("atomic", CmlParser.ATOMIC);
    //keywords.put("async", CmlParser.ASYNC);
    //keywords.put("be", CmlParser.BE);
    keywords.put("bool", CmlParser.TBOOL);
    //keywords.put("by", CmlParser.BY);
    keywords.put("card", CmlParser.CARD);
    keywords.put("cases", CmlParser.CASES);
    keywords.put("char", CmlParser.TCHAR);
    keywords.put("class", CmlParser.CLASS);
    /*
    keywords.put("comp", CmlParser.COMP);
    keywords.put("compose", CmlParser.COMPOSE);
    keywords.put("conc", CmlParser.CONC);
    keywords.put("cycles", CmlParser.CYCLES);
    keywords.put("dcl", CmlParser.DCL);
    keywords.put("def", CmlParser.DEF);
    keywords.put("dinter", CmlParser.DINTER);
    keywords.put("div", CmlParser.ARITHMETIC_INTEGER_DIVISION);
    keywords.put("do", CmlParser.DO);
    keywords.put("dom", CmlParser.DOM);
    keywords.put("dunion", CmlParser.DUNION);
    keywords.put("duration", CmlParser.DURATION);
    keywords.put("elems", CmlParser.ELEMS);
    keywords.put("else", CmlParser.ELSE);
    keywords.put("elseif", CmlParser.ELSEIF);
    */
    keywords.put("end", CmlParser.END);
    /*
    keywords.put("error", CmlParser.ERROR);
    keywords.put("errs", CmlParser.ERRS);
    keywords.put("exists", CmlParser.EXISTS);
    keywords.put("exists1", CmlParser.EXISTS1);
    keywords.put("exit", CmlParser.EXIT);
    keywords.put("ext", CmlParser.EXT);
    keywords.put("false", CmlParser.bool_false);
    keywords.put("floor", CmlParser.FLOOR);
    keywords.put("for", CmlParser.FOR);
    keywords.put("forall", CmlParser.FORALL);
    keywords.put("from", CmlParser.FROM);
    keywords.put("functions", CmlParser.FUNCTIONS);
    keywords.put("hd", CmlParser.HD);
    keywords.put("if", CmlParser.IF);
    keywords.put("in", CmlParser.IN);
    keywords.put("inds", CmlParser.INDS);
    keywords.put("inmap", CmlParser.INMAP);
    keywords.put("instance", CmlParser.INSTANCE);
    keywords.put("int", CmlParser.INT);
    keywords.put("inter", CmlParser.SET_INTERSECTION);
    keywords.put("inv", CmlParser.INV);
    keywords.put("inverse", CmlParser.INVERSE);
    keywords.put("iota", CmlParser.IOTA);
    keywords.put("is", CmlParser.IS);
    keywords.put("is_", CmlParser.IS_);
    keywords.put("isofbaseclass", CmlParser.ISOFBASECLASS);
    keywords.put("isofclass", CmlParser.ISOFCLASS);
    keywords.put("lambda", CmlParser.LAMBDA);
    keywords.put("len", CmlParser.LEN);
    keywords.put("let", CmlParser.LET);
    keywords.put("map", CmlParser.MAP);
    keywords.put("merge", CmlParser.DMERGE);
    keywords.put("mk_", CmlParser.MK_);
    keywords.put("mod", CmlParser.MOD);
    keywords.put("mu", CmlParser.MU);
    keywords.put("munion", CmlParser.MAP_MERGE);
    keywords.put("mutex", CmlParser.MUTEX);
    */
    keywords.put("nat", CmlParser.TNAT);
    keywords.put("nat1", CmlParser.TNAT1);
    /*
    keywords.put("new", CmlParser.NEW);
    keywords.put("nil", CmlParser.NIL);
    keywords.put("not", CmlParser.NOT);
    keywords.put("of", CmlParser.OF);
    keywords.put("operations", CmlParser.OPERATIONS);
    keywords.put("or", CmlParser.OR);
    keywords.put("others", CmlParser.OTHERS);
    keywords.put("per", CmlParser.PER);
    keywords.put("periodic", CmlParser.PERIODIC);
    keywords.put("post", CmlParser.POST);
    keywords.put("power", CmlParser.POWER);
    keywords.put("pre", CmlParser.PRE);
    keywords.put("pre_", CmlParser.PRECONDAPPLY);
    */
    //keywords.put("private", CmlParser.PRIVATE);
    keywords.put("process", CmlParser.PROCESS);
    //keywords.put("protected", CmlParser.PROTECTED);
    keywords.put("psubset", CmlParser.PROPER_SUBSET);
    //keywords.put("public", CmlParser.PUBLIC);
    keywords.put("rat", CmlParser.TRAT);
    keywords.put("rd", CmlParser.VDMRD);
    keywords.put("real", CmlParser.TREAL);
    /*
    keywords.put("rem", CmlParser.REM);
    keywords.put("responsibility", CmlParser.RESPONSIBILITY);
    keywords.put("return", CmlParser.RETURN);
    keywords.put("reverse", CmlParser.REVERSE);
    keywords.put("rng", CmlParser.RNG);
    keywords.put("samebaseclass", CmlParser.SAMEBASECLASS);
    keywords.put("sameclass", CmlParser.SAMECLASS);
    keywords.put("self", CmlParser.SELF);
    keywords.put("seq", CmlParser.SEQ);
    keywords.put("seq1", CmlParser.SEQ1);
    keywords.put("set", CmlParser.SET);
    keywords.put("skip", CmlParser.SKIP);
    keywords.put("specified", CmlParser.SPECIFIED);
    keywords.put("st", CmlParser.ST);
    keywords.put("start", CmlParser.START);
    keywords.put("startlist", CmlParser.STARTLIST);
    keywords.put("static", CmlParser.STATIC);
    keywords.put("subclass", CmlParser.SUBCLASS);
    keywords.put("subset", CmlParser.SUBSET);
    keywords.put("sync", CmlParser.SYNC);
    keywords.put("system", CmlParser.SYSTEM);
    keywords.put("then", CmlParser.THEN);
    keywords.put("thread", CmlParser.THREAD);
    keywords.put("threadid", CmlParser.THREADID);
    keywords.put("time", CmlParser.TIME);
    keywords.put("tixe", CmlParser.TIXE);
    keywords.put("tl", CmlParser.TL);
    keywords.put("to", CmlParser.TO);
    */
    keywords.put("token", CmlParser.TTOKEN);
    /*
    keywords.put("trap", CmlParser.TRAP);
    keywords.put("true", CmlParser.bool_true);
    */
    keywords.put("types", CmlParser.TYPES);
    //keywords.put("undefined", CmlParser.UNDEFINED);
    keywords.put("union", CmlParser.UNION);
    keywords.put("values", CmlParser.VALUES);
    keywords.put("variables", CmlParser.INSTANCEVARS);
    //keywords.put("while", CmlParser.WHILE);
    //keywords.put("with", CmlParser.WITH);
    keywords.put("wr", CmlParser.VDMWR);
    //keywords.put("yet", CmlParser.YET);
    
  }


  private CmlLexeme yylvalue;
  
  public int errors = 0;
public List<ParserError> parseErrors = new Vector<ParserError>();
  
    /**
     * Method to retrieve the beginning position of the last scanned token.
     * @return the position at which the last scanned token starts.  */
  public Position getStartPos () { return yylvalue.getStartPos(); }

    /**
     * Method to retrieve the ending position of the last scanned token.
     * @return the first position beyond the last scanned token.  */
  public Position getEndPos () { return yylvalue.getEndPos(); }

    /**
     * Method to retrieve the semantic value of the last scanned token.
     * @return the semantic value of the last scanned token.  */
  public Object getLVal () { return yylvalue; }
  
    /**
     * Entry point for error reporting.  Emits an error
     * referring to the given location in a user-defined way.
     *
     * @param loc The location of the element to which the
     *                error message is related
     * @param s The string for the error message.  */
  public void yyerror (Location loc, String err) { 
    
    System.err.println("Error : " + err + " at " + loc.begin.toString()); 
    
      String msg = new String(err);
	    if (yylvalue != null) {
	    	CmlLexeme currentToken = (CmlLexeme) yylvalue;
	    	if (currentToken.getValue().length() > 0) {
	    		// add position info iff the scanner found a lexem
	    	    msg += " at (" + currentToken.getStartPos().line + ", " + currentToken.getStartPos().column + ")";
	    		msg += " after reading token \"" + currentToken.getValue() + "\"";
	    		
	    		parseErrors.add(new ParserError(err+" after reading token \"" + currentToken.getValue() + "\"", 
	    		currentToken.getStartPos().line, currentToken.getStartPos().column));
	    	}
	    }
		System.out.println(msg);
		errors++;
    
  }
   

  // ************************************
  // *** AUXILIARY PRIVATE OPERATIONS ***
  // ************************************

  // helper function for checking reserved words and identifiers
  private int checkIdentifier(String id) {
      
      int line = yyline + 1;
      int column = yycolumn;
      String value = yytext();
      
      try {
	  if (keywords.containsKey(id)) {
	      //return new OmlLexem(line, column, new Long(keywords.get(id)), id, IOmlLexem.ILEXEMKEYWORD);
	      //return new CmlLexeme(line, column,IDENTIFIER, id);
	      yylvalue = new CmlLexeme(new Position(line,column),new Position(line,column + value.length()),keywords.get(id),value);
	      return keywords.get(id);
	      
	  } else {
	      //DEBUG String theText = yytext();
	      //DEBUG System.out.print(theText + " = ");
	      //DEBUG for (int idx=0; idx< theText.length(); idx++) System.out.format("%04x ", (int) theText.charAt(idx));
	      //DEBUG System.out.println();
	      //return new OmlLexem(line, column, new Long(LEX_identifier), id, IOmlLexem.ILEXEMIDENTIFIER);
	      //return new CmlLexeme(line, column, IDENTIFIER, id);
	      yylvalue = new CmlLexeme(new Position(line,column),new Position(line,column + value.length()),CmlParser.IDENTIFIER,value);
	      return CmlParser.IDENTIFIER;
	  }
      }
      catch (Exception cge) {
	  cge.printStackTrace();
	  return -1;
      }
  }
  
  // helper function for default token creation
  private int defaultToken()
  {
    return createToken(yytext().charAt(0));
  }
  
  
  /* Helper function to return the correct integer for the parser and create the correct Lexeme (semantic) values for the parser and beyond */
  private int createToken(int lex)
  {
    int line = yyline + 1;
    int column = yycolumn;
    String value = yytext();
    try {
      yylvalue = new CmlLexeme(new Position(line,column),new Position(line,column + value.length()),lex,value);
      //return new CmlLexeme(line, column, lex, yytext());
      return lex;
    }
    catch (Exception cge) {
      cge.printStackTrace();
      return -1;
    }
  }
  
%}

// *****************************
// *** SHORTHAND DEFINITIONS ***
// *****************************

/* ucode					= [\u0100-\ufff0] */
/* hexdigit 				= [0-9ABCDEF]|[0-9abcdef] */
/* hexquad 				= {hexdigit}{hexdigit}{hexdigit}{hexdigit} */
/* universalcharactername  = (\\u{hexquad})|(\\U{hexquad}) */
/* letter 					= [A-Za-z]|#[A-Za-z]|{universalcharactername}|{ucode} */
/* digit 					= [0-9] */
/* prime 					= \` */
/* hook 					= \~ */
/* rtfuniversalcharacter	= \\u{hexquad}[A-Za-z] */
/* identifierorkeyword		= {letter}([0-9\'_]|{letter})* */

/* numericliteral 			= {digit}+ */
/* realliteral				= [0-9]+(("."[0-9]+)|([Ee]("+"|"-")?[0-9]+)|("."[0-9]+[Ee]("+"|"-")?[0-9]+)) */

/* embeddedctrlchar 		= [\000-\037] */
/* backslashed				= \\c.|\\x..|\\[\\nrabtvef\'\"]|\\[0-3][0-7][0-7] */
/* highbitchar				= [\200-\377] */
/* deletechar				= \177 */
/* characterliteral		= "'"([\040-\133\135-\176]|{embeddedctrlchar}|{backslashed}|{deletechar}|{highbitchar}|{universalcharactername}|{rtfuniversalcharacter}|{ucode})"'" */

/* textliteral				= \"([\040-\041\043-\133\135-\176]|{embeddedctrlchar}|{backslashed}|{deletechar}|{highbitchar}|{universalcharactername}|{ucode})*\" */

/* quoteliteral 			= \<{identifierorkeyword}\> */

/* range					= ","({separator}*)"..."({separator}*)"," */

identifier      = [A-Za-z0-9]*
process         = [Pp][Rr][Oo][Cc][Ee][Ss][Ss]
begin           = [Bb][Ee][Gg][Ii][Nn]
end             = [Ee][Nn][Dd]
types           = [Tt][Yy][Pp][Ee][Ss]

%states PROCESS TYPES STATE
%xstates COMMENT

%%
									  
// ********************************
// *** SCANNER PRODUCTION RULES ***
// ********************************

<YYINITIAL> {
  {process}                             { yybegin(PROCESS); return createToken(CmlParser.PROCESS); }
}

<PROCESS> {
  begin                                 { return createToken(CmlParser.BEGIN); }
}

<TYPES> {
  ";"                                   { return createToken(CmlParser.SEMI); }
  ":-"                                  { return createToken(CmlParser.VDMTYPENCMP); }
  "::"                                  { return createToken(CmlParser.VDMRECORDDEF); }
}

<STATE> {
  "of"                                  { return createToken(CmlParser.VDMOF); }
}

<PROCESS,STATE> {
  end                                   { return createToken(CmlParser.END); }
}

<TYPES,STATE> {
  ":"[^:-]                              { return createToken(CmlParser.VDMTYPE); }
}

<PROCESS,TYPES,STATE> {
  {types}                               { yybegin(TYPES); return createToken(CmlParser.TYPES); }
  "="                                   { return createToken(CmlParser.EQUALS); }
  {identifier}                          { return createToken(CmlParser.IDENTIFER); }
}


"//".*                                  { /* match comment; do nothing */ }
"/*"                                    { stateStack.push(yystate); yybegin(COMMENT); }
<COMMENT>"*/"                           { yybegin(stateStack.pop()); }
<COMMENT>[^*]                           { /* match comment text; do nothing */ }
<COMMENT>\**[^/]                        { /* match comment text; do nothing */ }

[:whitespace:]                          { /* match whitespace; do nothing */ }

// default catch-all production rule is to return the current character
/* .								{ return defaultToken(); } */

// production rule to handle end-of-file
/* <<EOF>>									{ return 0; } */
