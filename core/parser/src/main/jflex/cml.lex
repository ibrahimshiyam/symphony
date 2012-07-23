package eu.compassresearch.core.lexer;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Vector;
import java.util.Stack;
import eu.compassresearch.core.parser.CmlParser;
import eu.compassresearch.core.parser.CmlParser.Lexer;
import eu.compassresearch.core.parser.CmlParser.Location;
import eu.compassresearch.ast.lex.*;

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
  /* used to concatenate strings together */
  private static StringBuilder stringBuilder;

  private static Stack<Integer> stateStack = new Stack<Integer>();

  private CmlLexeme yylvalue;
  private int offset = 0;

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
  public void yyerror (Location loc, String err)
  {
    String msg = new String(err);
    if (yylvalue != null) {
      CmlLexeme currentToken = (CmlLexeme) yylvalue;
      if (currentToken.getValue().length() > 0) {
	// add position info iff the scanner found a lexem
	msg += " at (" + currentToken.getStartPos().line + ", " + currentToken.getStartPos().column + ")";
	msg += " after reading token \"" + currentToken.getValue() + "\"";
	parseErrors.add(new ParserError(err+" after reading token \"" +
					currentToken.getValue() + "\"",
					currentToken.getStartPos().line,
					currentToken.getStartPos().column,
					yychar,
					currentToken.getValue().toString()));
      }
    }
    System.out.println(msg);
    errors++;
  }

  // ************************************
  // *** AUXILIARY PRIVATE OPERATIONS ***
  // ************************************

  // helper function for default token creation
  private int defaultToken()
  {
    return createToken(yytext().charAt(0));
  }

  private int createToken(int lex, String value)
  {
    int line = yyline + 1;
    int column = yycolumn;
    int startOffset = offset;
    offset += value.length();

    try {
      yylvalue = new CmlLexeme(new Position(line,column,startOffset),
                               new Position(line,column + value.length(),offset),
                               lex,
                               value);
      return lex;
    } catch (Exception cge) {
      cge.printStackTrace();
      return -1;
    }
  }

  /*
     Helper function to return the correct integer for the parser
     and create the correct Lexeme (semantic) values for the parser and beyond
  */
  private int createToken(int lex)
  {
    String value = yytext();
    return createToken(lex, value);
  }

%}

// *****************************
// *** SHORTHAND DEFINITIONS ***
// *****************************

ucode                                   = [\u0100-\ufff0]
hexdigit                                = [0-9ABCDEF]|[0-9abcdef]
hexquad                                 = {hexdigit}{hexdigit}{hexdigit}{hexdigit}
hexliteral                              = 0[x X]{hexdigit}+
universalcharactername                  = (\\u{hexquad})|(\\U{hexquad})
letter                                  = [A-Za-z]|#[A-Za-z]|{universalcharactername}|{ucode}
digit                                   = [0-9]
/* rtfuniversalcharacter        = \\u{hexquad}[A-Za-z] */
/* identifierorkeyword          = {letter}([0-9\'_]|{letter})* */

numeral                                 = {digit}+
/* realliteral                          = [0-9]+(("."[0-9]+)|([Ee]("+"|"-")?[0-9]+)|("."[0-9]+[Ee]("+"|"-")?[0-9]+))  */

/* embeddedctrlchar             = [\000-\037] */
/* backslashed                          = \\c.|\\x..|\\[\\nrabtvef\'\"]|\\[0-3][0-7][0-7] */
/* highbitchar                          = [\200-\377] */
/* deletechar                           = \177 */
/* characterliteral             = "'"([\040-\133\135-\176]|{embeddedctrlchar}|{backslashed}|{deletechar}|{highbitchar}|{universalcharactername}|{rtfuniversalcharacter}|{ucode})"'" */

/* textliteral                          = \"([\040-\041\043-\133\135-\176]|{embeddedctrlchar}|{backslashed}|{deletechar}|{highbitchar}|{universalcharactername}|{ucode})*\" */

identifier      = {letter}([0-9\'_]|{letter})*

/* FIXME mk_name need to be extended to allow actual identifiers */
mk_name         = "mk_"{identifier}
/* FIXME and we need a is_name macro as well for the (to be created) ISUNDERNAME token  */
quoteliteral    = \<{identifier}\>
LineTerminator  = \r|\n|\r\n
/* InputCharacter  = [^\r\n] */
WhiteSpace      = {LineTerminator} | [ \t\f]

/* We need COMMENT and STRING to be exclusionary states, to avoid
 * inadvertantly matching things inside them.
 */
%xstates COMMENT STRING

%%

// ********************************
// *** SCANNER PRODUCTION RULES ***
// ********************************

"//".*                        { offset += yytext().length(); }
"--".*                        { offset += yytext().length(); }

"/*"                          { stateStack.push(yystate()); yybegin(COMMENT); }
<COMMENT> {
  "*/"                        { offset += yytext().length();yybegin(stateStack.pop()); }
  [^*]                        { offset += yytext().length(); }
  \**[^/]                     { offset += yytext().length();/* match comment text; */ }
}

"Chaos"                       { return createToken(CmlParser.CSPCHAOS); }
"Skip"                        { return createToken(CmlParser.CSPSKIP); }
"Stop"                        { return createToken(CmlParser.CSPSTOP); }
"Wait"                        { return createToken(CmlParser.CSPWAIT); }
"abs"                         { return createToken(CmlParser.ABS); }
"actions"                     { return createToken(CmlParser.ACTIONS); }
"and"                         { return createToken(CmlParser.AND); }
"atomic"                      { return createToken(CmlParser.ATOMIC); }
"begin"                       { return createToken(CmlParser.BEGIN); }
"bool"                        { return createToken(CmlParser.TBOOL); }
"card"                        { return createToken(CmlParser.CARD); }
"cases"                       { return createToken(CmlParser.CASES); }
"channels"                    { return createToken(CmlParser.CHANNELS); }
"chansets"                    { return createToken(CmlParser.CHANSETS); }
"char"                        { return createToken(CmlParser.TCHAR); }
"class"                       { return createToken(CmlParser.CLASS); }
"compose"                     { return createToken(CmlParser.COMPOSE); }
"conc"                        { return createToken(CmlParser.CONC); }
"dcl"                         { return createToken(CmlParser.DCL); }
"dinter"                      { return createToken(CmlParser.DINTER); }
"div"                         { return createToken(CmlParser.DIVIDE); }
"dom"                         { return createToken(CmlParser.DOM); }
"dunion"                      { return createToken(CmlParser.DUNION); }
"elems"                       { return createToken(CmlParser.ELEMS); }
"else"                        { return createToken(CmlParser.ELSE); }
"elseif"                      { return createToken(CmlParser.ELSEIF); }
"end"                         { return createToken(CmlParser.END); }
"endsby"                      { return createToken(CmlParser.ENDSBY); }
"exists"                      { return createToken(CmlParser.EXISTS); }
"exists1"                     { return createToken(CmlParser.EXISTS1); }
"floor"                       { return createToken(CmlParser.FLOOR); }
"forall"                      { return createToken(CmlParser.FORALL); }
"frame"                       { return createToken(CmlParser.FRAME); }
"functions"                   { return createToken(CmlParser.FUNCTIONS); }
"hd"                          { return createToken(CmlParser.HD); }
"if"                          { return createToken(CmlParser.IF); }
"in set"                      { return createToken(CmlParser.INSET); }
"in"                          { return createToken(CmlParser.IN); }
"inds"                        { return createToken(CmlParser.INDS); }
"inmap"                       { return createToken(CmlParser.INMAPOF); }
"int"                         { return createToken(CmlParser.TINT); }
"inter"                       { return createToken(CmlParser.INTER); }
":inter"                      { return createToken(CmlParser.COLONINTER); }
"inv"                         { return createToken(CmlParser.INV); }
"inverse"                     { return createToken(CmlParser.INVERSE); }
"is not yet specified"        { return createToken(CmlParser.NOTYETSPEC); }
"is subclass responsibility"  { return createToken(CmlParser.SUBCLASSRESP); }
"lambda"                      { return createToken(CmlParser.LAMBDA); }
"len"                         { return createToken(CmlParser.LEN); }
"let"                         { return createToken(CmlParser.LET); }
"logical"                     { return createToken(CmlParser.LOGICAL); }
"map"                         { return createToken(CmlParser.MAPOF); }
"measure"                     { return createToken(CmlParser.MEASURE); }
"merge"                       { return createToken(CmlParser.MERGE); }
"mod"                         { return createToken(CmlParser.MOD); }
"munion"                      { return createToken(CmlParser.MAPMERGE); }
"nat"                         { return createToken(CmlParser.TNAT); }
"nat1"                        { return createToken(CmlParser.TNAT1); }
"not in set"                  { return createToken(CmlParser.NOTINSET); }
"not"                         { return createToken(CmlParser.NOT); }
"not"                         { return createToken(CmlParser.NOT); }
"operations"                  { return createToken(CmlParser.OPERATIONS); }
"or"                          { return createToken(CmlParser.OR); }
"others"                      { return createToken(CmlParser.OTHERS); }
"post"                        { return createToken(CmlParser.POST); }
"power"                       { return createToken(CmlParser.POWER); }
"pre"                         { return createToken(CmlParser.PRE); }
"private"                     { return createToken(CmlParser.PRIVATE); }
"process"                     { return createToken(CmlParser.PROCESS); }
"protected"                   { return createToken(CmlParser.PROTECTED); }
"public"                      { return createToken(CmlParser.PUBLIC); }
"rat"                         { return createToken(CmlParser.TRAT); }
"rd"                          { return createToken(CmlParser.RD); }
"real"                        { return createToken(CmlParser.TREAL); }
"res"                         { return createToken(CmlParser.RES); }
"return"                      { return createToken(CmlParser.RETURN); }
"reverse"                     { return createToken(CmlParser.REVERSE); }
"rng"                         { return createToken(CmlParser.RNG); }
"seq of"                      { return createToken(CmlParser.SEQOF); }
"seq1 of"                     { return createToken(CmlParser.SEQ1OF); }
"set of"                      { return createToken(CmlParser.SETOF); }
"startby"                     { return createToken(CmlParser.STARTBY); }
"state"                       { return createToken(CmlParser.STATE); }
"subset"                      { return createToken(CmlParser.SUBSET); }
"then"                        { return createToken(CmlParser.THEN); }
"tl"                          { return createToken(CmlParser.TL); }
"to"                          { return createToken(CmlParser.TO); }
"token"                       { return createToken(CmlParser.TTOKEN); }
"types"                       { return createToken(CmlParser.TYPES); }
"union"                       { return createToken(CmlParser.UNION); }
":union"                      { return createToken(CmlParser.COLONUNION); }
"val"                         { return createToken(CmlParser.VAL); }
"values"                      { return createToken(CmlParser.VALUES); }
"vres"                        { return createToken(CmlParser.VRES); }
"wr"                          { return createToken(CmlParser.WR); }

{quoteliteral}                { return createToken(CmlParser.QUOTE_LITERAL); }
{mk_name}                     { return createToken(CmlParser.MKUNDERNAME); }
"mk_"                         { return createToken(CmlParser.MKUNDER); }

"&"                           { return createToken(CmlParser.AMP); }
"@"                           { return createToken(CmlParser.AT); }
"\\"                          { return createToken(CmlParser.BACKSLASH); }
"!"                           { return createToken(CmlParser.BANG); }
"|"                           { return createToken(CmlParser.BAR); }
"|->"                         { return createToken(CmlParser.BARRARROW); }
"|}"                          { return createToken(CmlParser.BARRCURLY); }
"|>"                          { return createToken(CmlParser.BARGT); }
"|]"                          { return createToken(CmlParser.BARRSQUARE); }
"|~|"                         { return createToken(CmlParser.BARTILDEBAR); }
"^"                           { return createToken(CmlParser.CARET); }
":"                           { return createToken(CmlParser.COLON); }
":\\"                         { return createToken(CmlParser.COLONBACKSLASH); }
":-"                          { return createToken(CmlParser.COLONDASH); }
":->"                         { return createToken(CmlParser.COLONDASHGT); }
":="                          { return createToken(CmlParser.COLONEQUALS); }
":>"                          { return createToken(CmlParser.COLONGT); }
":}"                          { return createToken(CmlParser.COLONRCURLY); }
/* ":]"                          { return createToken(CmlParser.COLONRSQUARE); } */
","                           { return createToken(CmlParser.COMMA); }
"||"                          { return createToken(CmlParser.DBAR); }
"||]"                         { return createToken(CmlParser.DBARRSQUARE); }
"::"                          { return createToken(CmlParser.DCOLON); }
"==>"                         { return createToken(CmlParser.DEQRARROW); }
"=="                          { return createToken(CmlParser.DEQUALS); }
"[["                          { return createToken(CmlParser.DLSQUARE); }
"."                           { return createToken(CmlParser.DOT); }
".:"                          { return createToken(CmlParser.DOTCOLON); }
".#"                          { return createToken(CmlParser.DOTHASH); }
"++"                          { return createToken(CmlParser.DPLUS); }
"]]"                          { return createToken(CmlParser.DRSQUARE); }
"**"                          { return createToken(CmlParser.DSTAR); }
// yes, the ellipsis includes the commas all as a single token
","[ \t]*"..."[ \t]*","       { return createToken(CmlParser.ELLIPSIS); }
"=>"                          { return createToken(CmlParser.EQRARROW); }
"="                           { return createToken(CmlParser.EQUALS); }
">"                           { return createToken(CmlParser.GT); }
">="                          { return createToken(CmlParser.GTE); }
"<-"                          { return createToken(CmlParser.LARROW); }
"{"                           { return createToken(CmlParser.LCURLY); }
"{|"                          { return createToken(CmlParser.LCURLYBAR); }
"{:"                          { return createToken(CmlParser.LCURLYCOLON); }
"("                           { return createToken(CmlParser.LPAREN); }
"()"                          { return createToken(CmlParser.LRPAREN); }
"[]"                          { return createToken(CmlParser.LRSQUARE); }
"["                           { return createToken(CmlParser.LSQUARE); }
"[|"                          { return createToken(CmlParser.LSQUAREBAR); }
"[||"                         { return createToken(CmlParser.LSQUAREDBAR); }
/* "[:"                          { return createToken(CmlParser.LSQUARECOLON); } */
"[>"                          { return createToken(CmlParser.LSQUAREGT); }
"<"                           { return createToken(CmlParser.LT); }
"<:"                          { return createToken(CmlParser.LTCOLON); }
"<-:"                         { return createToken(CmlParser.LTDASHCOLON); }
"<="                          { return createToken(CmlParser.LTE); }
"<=>"                         { return createToken(CmlParser.LTEQUALSGT); }
"-"                           { return createToken(CmlParser.MINUS); }
"<>"                          { return createToken(CmlParser.NEQ); }
"+"                           { return createToken(CmlParser.PLUS); }
"+>"                          { return createToken(CmlParser.PLUSGT); }
"?"                           { return createToken(CmlParser.QUESTION); }
"->"                          { return createToken(CmlParser.RARROW); }
"}"                           { return createToken(CmlParser.RCURLY); }
")"                           { return createToken(CmlParser.RPAREN); }
"]"                           { return createToken(CmlParser.RSQUARE); }
";"                           { return createToken(CmlParser.SEMI); }
"/"                           { return createToken(CmlParser.SLASH); }
"/\\"                         { return createToken(CmlParser.SLASHBACKSLASH); }
"/:"                          { return createToken(CmlParser.SLASHCOLON); }
"*"                           { return createToken(CmlParser.STAR); }
"|||"                         { return createToken(CmlParser.TBAR); }
"~"                           { return createToken(CmlParser.TILDE); }
"`"                           { return createToken(CmlParser.BACKTICK); }

/* ---- complex terminals below ---- */

"\""                          { offset += yytext().length();stateStack.push(yystate());yybegin(STRING); stringBuilder = new StringBuilder(); }
<STRING>"\""                  { offset += yytext().length();yybegin(stateStack.pop()); return createToken(CmlParser.STRING, stringBuilder.toString()); }
<STRING>[^\"]                 { stringBuilder.append(yytext()); }

{WhiteSpace}                  { offset += yytext().length(); }
{identifier}                  { return createToken(CmlParser.IDENTIFIER); }
{numeral}                     { return createToken(CmlParser.NUMERAL); }
{hexliteral}                  { return createToken(CmlParser.HEX_LITERAL); }

.                             { throw new IllegalArgumentException("Syntax at line "+(yyline+1)+" position "+yycolumn+" \"" + yytext() + "\" was unexpected at this time."); }

<<EOF>>                       { stateStack.clear(); return 0; }
