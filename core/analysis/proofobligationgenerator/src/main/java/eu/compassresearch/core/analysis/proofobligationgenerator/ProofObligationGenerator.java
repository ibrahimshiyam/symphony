/**
 * Proof Obligation Generator Analysis
 *
 * Description: 
 * 
 * This analysis extends the QuestionAnswerAdaptor to generate
 * POs from the AST generated by the CML parser
 *
 */

package eu.compassresearch.core.analysis.proofobligationgenerator;

/**
 * Core stuff needed in this simple analysis.
 *
 */
import org.overture.ast.analysis.DepthFirstAnalysisAdaptor;
import org.overture.ast.expressions.ADivideNumericBinaryExp;
import org.overture.ast.declarations.ATypeDeclaration;
import org.overture.ast.lex.LexLocation;
/**
 * Java libraries 
 */
import java.util.LinkedList;
import java.util.List;

public class ProofObligationGenerator extends DepthFirstAnalysisAdaptor
{
    // Constants
    private final static String ANALYSIS_NAME = "Proof Obligation Generator";
    private final static String ANALYSIS_STRING = "Tree location: ";

    // Analysis Result
    private List<String> pos; //should change to ProofObligation type?

    // Constructor setting warnings up
    public ProofObligationGenerator()
    {
		pos = new LinkedList<String>(); //change inline with pos type
    }

    /*
 	 * When the DepthFirstAnalysisAdaptor reaches a Divide binary
 	 * expression this method is invoke. Here this analysis wants to
 	 * create a warning and add it to its output.
 	 */
 	@Override
 	public void caseADivideNumericBinaryExp(ADivideNumericBinaryExp node) {
 		super.caseADivideNumericBinaryExp(node);
 		pos.add(prettyPrintLocation(node.getLocation()));
 	}

    /*
 	 * When the DepthFirstAnalysisAdaptor reaches a Type declaration
 	 * this method is invoked. Here this analysis wants to
 	 * create a warning and add it to its output.
 	 */ 	 
 	@Override
 	public void caseATypeDeclaration(ATypeDeclaration node) {
 		super.caseATypeDeclaration(node);
 		pos.add(prettyPrintLocation(node.getLocation()));
 	}

    // Pretty warning for the result
    private static String prettyPrintLocation(LexLocation loc)
    {
		StringBuilder sb = new StringBuilder();
		sb.append(ANALYSIS_STRING);
		sb.append(" Node found at: ");
		sb.append(loc.startLine +":"+loc.startPos);
		sb.append(" to " + loc.endLine + ":"+loc.endPos);
		return sb.toString();
    }
    
    //output analysis results
 		

    /**
     * Test Method to acquire the result produced by this analysis.
     */
    public void getResults()
    {
    	System.out.println("   Generation complete. Results:");
    	for(String s :pos)
 		{
 			System.out.println("\t"+s);
 		}
    }
    
    /**
     * The ide/cmdline tool will pick this method up and use it for
     * pretty printing the analysis name. If this method is missing
     * the cmdline tool will use the class name.
     *
     * @return usefriendly name for this analysis.
     */
    public String getAnalysisName() 
    {
		return ANALYSIS_NAME;
    }
    
    
}