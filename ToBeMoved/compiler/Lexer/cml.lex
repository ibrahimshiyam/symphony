package eu.compassresearch.cml.compiler;
import java.util.LinkedList;
import java.util.List;
import eu.compassresearch.cml.compiler.CmlParser.Lexer;
import eu.compassresearch.cml.compiler.CmlParser.Location;

class CmlContext {
  
}

class ParserInterface implements Lexer {
    /**
     * Method to retrieve the beginning position of the last scanned token.
     * @return the position at which the last scanned token starts.  */
  public Position getStartPos () { return null; }

    /**
     * Method to retrieve the ending position of the last scanned token.
     * @return the first position beyond the last scanned token.  */
  public Position getEndPos () { return null; }

    /**
     * Method to retrieve the semantic value of the last scanned token.
     * @return the semantic value of the last scanned token.  */
  public Object getLVal () { return null; }

    /**
     * Entry point for the scanner.  Returns the token identifier corresponding
     * to the next token and prepares to return the semantic value
     * and beginning/ending positions of the token. 
     * @return the token identifier corresponding to the next token. */
  public int yylex () throws java.io.IOException { return 0; }

    /**
     * Entry point for error reporting.  Emits an error
     * referring to the given location in a user-defined way.
     *
     * @param loc The location of the element to which the
     *                error message is related
     * @param s The string for the error message.  */
  public void yyerror (Location loc, String s) { return ; }

}

class LexicographicalRuntimeException extends RuntimeException
{
  private int pos,line;
  private  String value;
  public LexicographicalRuntimeException(String chars)
    {
      this.pos = Yytoken.curPos;
      this.line = Yytoken.curLine;
      this.value = chars;
    }
  
  @Override
    public String toString()
    {
      return "Offending syntax "+(value != null ? "starting with \""+value+"\" ":"")+"found at line "+(line+1)+" position "+(pos+1);
    }
}

class Yytoken {
  public static int curLine;
  public static int curPos; 

  private int line;
  private int pos;
  protected  String value;
  public Yytoken(String value)
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

class StringToken extends Yytoken {
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

class CommentBlock extends Yytoken {
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
%class CmlLexer
%unicode
%line
%char

newline=\n
ws=[\t ]
digit=[0-9]
digits={digit}*
decdigits=[1-9][0-9]*
octdigits=0[1-7][0-7]*
identifier=[a-zA-Z$_][0-9a-zA-Z$_]*
equalsign==
minus=-
plus=\+
times=\*
divide=/
backslash=\\
lbrace=\{
rbrace=\}
lparen=\(
rparen=\)
lbrack=\[
rbrack=\]
quote=\"
colon=:
semicolon=;
string=[^\"]*
gt=>
lt=<
at=@
comma=,
KW_Class=class
KW_for=for
KW_if=if
KW_then=then
KW_else=else
KW_define=define
KW_begin=begin
KW_end=end
KW_internalChoice=|~|
KW_externalChoice=\[\]
KW_assign=:=
%state STRING, STRINGESCAPE, COMMENT
%%
<YYINITIAL>--               { yybegin(COMMENT);  Yytoken.curPos+=yylength();}
<COMMENT>`                 { CommentBlock.current.increaseLevel();Yytoken.curPos+=yylength(); }
<COMMENT>\'                { if (CommentBlock.current.decreaseLevel()) { yybegin(YYINITIAL);} Yytoken.curPos += yylength(); System.out.println("Leaving block"); }
<COMMENT>[a-zA-Z]*         { CommentBlock.current.appendLine(yytext()); Yytoken.curPos+=yylength(); }
<COMMENT>{newline}         { CommentBlock.current.appendLine("\n");Yytoken.curLine++;Yytoken.curPos= 0; }

<YYINITIAL>{quote}         { yybegin(STRING); StringToken.currentString = new StringToken(Yytoken.curLine,Yytoken.curPos);Yytoken.curPos += yylength(); }
<STRING>{newline}          { Yytoken.curLine++;Yytoken.curPos=0;StringToken.currentString.newLine();}
<STRING>{string}           { Yytoken.curPos += yylength(); StringToken.currentString.append(yytext()); }
<STRING>{backslash}        { Yytoken.curPos++; yybegin(STRINGESCAPE);}
<STRINGESCAPE>{quote}      { Yytoken.curPos++; yybegin(STRING);StringToken.currentString.append("\""); }
<STRING>{quote}            { yybegin(YYINITIAL); Yytoken.curPos += yylength() ; StringToken.currentString.endString(Yytoken.curLine, Yytoken.curPos);System.out.println(StringToken.currentString.getValue()); }

<YYINITIAL>{newline}       { Yytoken.curLine++; Yytoken.curPos = 0; }
<YYINITIAL>{ws}            { Yytoken.curPos += yylength() ; }
<YYINITIAL>{decdigits}     { Yytoken.curPos += yylength() ; }         
<YYINITIAL>{octdigits}     { Yytoken.curPos += yylength() ; }
<YYINITIAL>{digits}        { Yytoken.curPos += yylength() ; }
<YYINITIAL>{identifier}    { Yytoken.curPos += yylength() ; }
<YYINITIAL>{equalsign}     { Yytoken.curPos += yylength() ; }
<YYINITIAL>{minus}         { Yytoken.curPos += yylength() ; }
<YYINITIAL>{plus}          { Yytoken.curPos += yylength() ; }
<YYINITIAL>{times}         { Yytoken.curPos += yylength() ; }
<YYINITIAL>{divide}        { Yytoken.curPos += yylength() ; }
<YYINITIAL>{backslash}     { Yytoken.curPos += yylength() ; }
<YYINITIAL>{lbrace}        { Yytoken.curPos += yylength() ; }
<YYINITIAL>{rbrace}        { Yytoken.curPos += yylength() ; }
<YYINITIAL>{lparen}        { Yytoken.curPos += yylength() ; }
<YYINITIAL>{rparen}        { Yytoken.curPos += yylength() ; }
<YYINITIAL>{lbrack}        { Yytoken.curPos += yylength() ; }
<YYINITIAL>{rbrack}        { Yytoken.curPos += yylength() ; }
<YYINITIAL>{colon}         { Yytoken.curPos += yylength() ; }
<YYINITIAL>{semicolon}     { Yytoken.curPos += yylength() ; }
<YYINITIAL>{at}            { Yytoken.curPos += yylength() ; }
<YYINITIAL,STRING>.        { throw new LexicographicalRuntimeException(yytext());}
