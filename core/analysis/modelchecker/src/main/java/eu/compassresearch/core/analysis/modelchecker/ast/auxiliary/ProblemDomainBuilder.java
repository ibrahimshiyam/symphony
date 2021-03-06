package eu.compassresearch.core.analysis.modelchecker.ast.auxiliary;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import eu.compassresearch.core.analysis.modelchecker.ast.MCNode;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAChannelDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAExplicitCmlOperationDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAProcessDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAValueDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCSCmlOperationDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCASBinaryExp;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCPCMLExp;
import eu.compassresearch.core.analysis.modelchecker.visitors.NewCMLModelcheckerContext;

public class ProblemDomainBuilder {
	
	public String buildProblemDomain(){
		
		StringBuilder content = new StringBuilder();
		String option = MCNode.DEFAULT;
		
		
		//generate value definitions
		generateValueDefinitions(content,option);
				
		//generate auxiliary definitions (actions, operationDefs, iocommdefs, guardDefs, etc.) 
		//generateAuxiliaryActions(content,option);
		
		//generates process definitions 
		generateProcessDefinitions(content, option);
		
		//generate the initial state
		generateInitialState(content,option);
		
		//generates operation definitions
		generateOperationDefinitions(content,option);
				
		//generates guard definitions
		generateGuardDefinitions(content,option);
		
		
		//generates assignment definitions
		generateAssignDefinitions(content, option);
		
		
		
		
		//generate iocom defs
		generateIOCommDefs(content,option);
		
		
		//generate Channel facts possibly depending on primitive wrappers to deal with infinite types
		generateChannels(content, option);
		
		//generate lieIn Facts
		generateLieInFacts(content, option);
		
		//generate conforms clause
		generateConforms(content,option);
		
		return content.toString();
	}
	
	

	private void generateProcessDefinitions(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		//it generates the top level process definition
		
		StringBuilder topLevelProcDef = new StringBuilder();
		topLevelProcDef.append("  ProcDef(\"" + context.mainProcessName + "MAIN" + "\",void,proc(\""+ context.mainProcessName + "\",void)).\n\n");
		content.append(topLevelProcDef.toString());

		//it generates all process definitions
		for (MCAProcessDefinition processDef : context.processDefinitions) {
			content.append(processDef.toFormula(option));
		}
	}
	private void generateInitialState(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		content.append("  State(");
		content.append(context.maximalBinding.toFormula(option));
		content.append(",pBody)  :- GivenProc(np), ProcDef(np,nopar,pBody)");
		content.append(".\n");
	}
	
	private void generateGuardDefinitions(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
	
		for (Iterator<Entry<MCPCMLExp,LinkedList<MCGuardDef>>> iterator = context.guardDefs.entrySet().iterator(); iterator.hasNext();) {
			Entry<MCPCMLExp,LinkedList<MCGuardDef>> item = (Entry<MCPCMLExp,LinkedList<MCGuardDef>>) iterator.next();
			LinkedList<MCGuardDef> guardDefList = item.getValue();
			for (MCGuardDef mcGuardDef : guardDefList) {
				content.append(mcGuardDef.toFormula(option));
			}
		}
		
		for (Iterator<Entry<MCPCMLExp,LinkedList<NewMCGuardDef>>> iterator = context.stmGuardDefs.entrySet().iterator(); iterator.hasNext();) {
			Entry<MCPCMLExp,LinkedList<NewMCGuardDef>> item = (Entry<MCPCMLExp,LinkedList<NewMCGuardDef>>) iterator.next();
			LinkedList<NewMCGuardDef> guardDefList = item.getValue();
			for (NewMCGuardDef mcGuardDef : guardDefList) {
				content.append(mcGuardDef.toFormula(option));
			}
		}
		
	}
	
	private void generateAssignDefinitions(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		if(context.assignDefs.size() != 0){
			for (MCAssignDef assingnDef : context.assignDefs) {
				content.append(assingnDef.toFormula(option) + "\n");
			}
		}
	}
	
	private void generateOperationDefinitions(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		for (MCSCmlOperationDefinition opDef : context.operations) {
			if(opDef instanceof MCAExplicitCmlOperationDefinition){
				MCAExplicitCmlOperationDefinition op = (MCAExplicitCmlOperationDefinition) opDef;
				String operationCode = op.toFormula(MCNode.NAMED); 
				content.append(operationCode);
			}
		}
	}
	
	private void generateIOCommDefs(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		for (MCIOCommDef ioCommDef : context.ioCommDefs) {
			content.append(ioCommDef.toFormula(option));
			content.append("\n");
		}
	}
	private void generateValueDefinitions(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		for (MCAValueDefinition valueDef : context.valueDefinitions) {
			content.append(valueDef.toFormula(option));
			content.append("\n");
		}
	}
	
	private void generateSetFacts(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		for (MCASBinaryExp setExp : context.setExpressioFacts) {
			content.append(setExp.toFormula(option));
			content.append(".\n");
		}
	}
	
	private void generateChannels(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		for (MCAChannelDefinition chanDef : context.channelDefs) {
			content.append(chanDef.toFormula(option));
			if(chanDef.isTyped()){
				content.append("\n");
			}
		}
	}
	private void generateLieInFacts(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		for (MCLieInFact lieIn : context.lieIn) {
			//content.append(lieIn.toFormula(option) + "\n");
			lieIn.prepareLieInFact();
		}
		for (MCLieInFact lieIn :  context.realLieInFacts) {
			String lieInFactStr = lieIn.toFormula(option);
			if(content.indexOf(lieInFactStr) == -1){
				content.append(lieIn.toFormula(option) + ".\n");
			}
		}
	}
	private void generateConforms(StringBuilder content, String option){
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		content.append("  conforms := " + context.propertiesDomain.getName() + "." + context.propertyToCheck + ".\n");
	}
	
}
