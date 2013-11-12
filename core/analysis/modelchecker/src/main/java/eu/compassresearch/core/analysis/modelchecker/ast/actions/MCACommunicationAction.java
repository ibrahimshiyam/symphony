package eu.compassresearch.core.analysis.modelchecker.ast.actions;

import java.util.LinkedList;

import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.ExpressionEvaluator;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.MCCommEv;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.MCLieInFact;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCAEnumVarsetExpression;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCAFatEnumVarsetExpression;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCANameChannelExp;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCPVarsetExpression;
import eu.compassresearch.core.analysis.modelchecker.ast.types.MCPCMLType;
import eu.compassresearch.core.analysis.modelchecker.ast.types.MCVoidType;
import eu.compassresearch.core.analysis.modelchecker.visitors.NewCMLModelcheckerContext;
import eu.compassresearch.core.analysis.modelchecker.visitors.NewSetStack;

public class MCACommunicationAction implements MCPAction {

	private int counterId;
	private String identifier;
	private LinkedList<MCPCommunicationParameter> communicationParameters = new LinkedList<MCPCommunicationParameter>();
	private MCPAction action;
	
	public MCACommunicationAction(String identifier,
			LinkedList<MCPCommunicationParameter> communicationParameters,
			MCPAction action) {
		this.counterId = NewCMLModelcheckerContext.IOCOMM_COUNTER++;
		this.identifier = identifier;
		this.communicationParameters = communicationParameters;
		this.action = action;
	}



	@Override
	public String toFormula(String option) {
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		StringBuilder result = new StringBuilder();
		result.append("Prefix(IOComm(" + this.counterId + ",");
		result.append("\"" + this.identifier + "\"");
		result.append(",");
		result.append("\"" + buildIOCommExp(option) + "\"");
		result.append(",");
		result.append(buildIOCommActualParams(option));
		result.append(")"); //closes IOComm
		result.append(","); 
		result.append(this.action.toFormula(option));
		result.append(")"); //closes Prefix
		//if there is some set of event in the context we must generate lieIn events.
		NewSetStack<MCPVarsetExpression> chanSetStack = context.setStack.copy();
		while(!chanSetStack.isEmpty()){
			MCPVarsetExpression setExp = (MCPVarsetExpression)chanSetStack.pop();
			LinkedList<MCANameChannelExp> chanNames = null;
			if(setExp instanceof MCAEnumVarsetExpression){
				chanNames = ((MCAEnumVarsetExpression) setExp).getChannelNames();
			}
			if(setExp instanceof MCAFatEnumVarsetExpression){
				chanNames = ((MCAFatEnumVarsetExpression) setExp).getChannelNames();
			}
			if(chanNames != null){
				boolean generateLieIn = false;
				for (MCANameChannelExp aNameChannelExp : chanNames) {
					if(aNameChannelExp.getIdentifier().toString().equals(this.identifier.toString())){
						generateLieIn = true;
						break;
					}
				}
				if(!generateLieIn && chanSetStack.size()==0){
					break;
				}else{
					MCCommEv commEv = new MCCommEv(this.identifier,this.communicationParameters, new MCVoidType());
					MCLieInFact lieIn = new MCLieInFact(commEv,setExp); 
					if(!context.lieIn.contains(lieIn)){
						context.lieIn.add(lieIn);
					}
				}
			}
			
		}
		
		return result.toString();
	}

	private String buildIOCommExp(String option){
		StringBuilder result = new StringBuilder();
		result.append("novar");
		
		for (MCPCommunicationParameter param : this.communicationParameters) {
			result.append(param.toFormula(option));
		}
		
		return result.toString();
	}

	private String buildIOCommActualParams(String option){
	
		StringBuilder result = new StringBuilder();
		ExpressionEvaluator evaluator = ExpressionEvaluator.getInstance();
		MCPCMLType type = evaluator.instantiateMCTypeFromCommParams(this.communicationParameters);

		result.append(type.toFormula(option));
		
		return result.toString();
	}

	public String getIdentifier() {
		return identifier;
	}



	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}



	public LinkedList<MCPCommunicationParameter> getCommunicationParameters() {
		return communicationParameters;
	}



	public void setCommunicationParameters(
			LinkedList<MCPCommunicationParameter> communicationParameters) {
		this.communicationParameters = communicationParameters;
	}



	public MCPAction getAction() {
		return action;
	}



	public void setAction(MCPAction action) {
		this.action = action;
	}



	public int getCounterId() {
		return counterId;
	}



	public void setCounterId(int counterId) {
		this.counterId = counterId;
	}

	
	
}
