package eu.compassresearch.ide.cml.ui.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.overture.ast.lex.LexLocation;
import org.overture.ast.node.INode;

import eu.compassresearch.ast.program.PSource;
import eu.compassresearch.core.common.Registry;
import eu.compassresearch.core.common.RegistryFactory;
import eu.compassresearch.core.typechecker.VanillaFactory;
import eu.compassresearch.core.typechecker.api.CmlTypeChecker;
import eu.compassresearch.core.typechecker.api.TypeIssueHandler;
import eu.compassresearch.core.typechecker.api.TypeIssueHandler.CMLIssueList;
import eu.compassresearch.core.typechecker.api.TypeIssueHandler.CMLTypeError;
import eu.compassresearch.core.typechecker.api.TypeIssueHandler.CMLTypeWarning;
import eu.compassresearch.ide.cml.core.ICmlCoreConstants;
import eu.compassresearch.ide.cml.ui.editor.core.dom.CmlSourceUnit;

public class CmlIncrementalBuilder extends IncrementalProjectBuilder {


	
	public CmlIncrementalBuilder()
	{
		
	}
	/*
	 * Run the type checker.
	 */
	private static void setProblem(IMarker marker, String text, int... more)
			throws CoreException {
		marker.setAttribute(IMarker.MESSAGE, text);
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		if (more.length == 2)
		{
			marker.setAttribute(IMarker.CHAR_START, more[0]);
			marker.setAttribute(IMarker.CHAR_END, more[1]);
		}
	}

	private static void setWarning(IMarker marker, String text, int... more) throws CoreException
	{
		marker.setAttribute(IMarker.MESSAGE,text);
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
		if (more.length == 2)
		{
			marker.setAttribute(IMarker.CHAR_START, more[0]);
			marker.setAttribute(IMarker.CHAR_END, more[1]);
		}
	}


	/*
	 * For each error remove the parent errors so we only see the leafs.
	 * 
	 */
	private static List<CMLTypeError> filterErrros(List<CMLTypeError> errs)
	{
		Map<INode, List<CMLTypeError>> nodeToErrorMap = new HashMap<INode,List<CMLTypeError>>();

		for(CMLTypeError error : errs)
			if (error.getOffendingNode() != null)
			{
				List<CMLTypeError> l = 	nodeToErrorMap.get(error.getOffendingNode());
				if (l == null) { l = new LinkedList<CMLTypeError>(); nodeToErrorMap.put(error.getOffendingNode(),l); }
				l.add(error);
			}

		List<INode>  nodesToClearErrorsFor = new LinkedList<INode>();
		for(List<CMLTypeError> errors : nodeToErrorMap.values())
		{
			for(CMLTypeError error : errors)
			{
				INode parent = error.getOffendingNode().parent();
				while(parent != null)
				{
					if (nodeToErrorMap.containsKey(parent))
						nodesToClearErrorsFor.add(parent);
					parent = parent.parent();
				}
			}
		}
		
		for(INode n : nodesToClearErrorsFor)
			nodeToErrorMap.put(n,new LinkedList<CMLTypeError>());
		
		List<CMLTypeError> res = new ArrayList<TypeIssueHandler.CMLTypeError>();
		for(Entry<INode,List<CMLTypeError>> e : nodeToErrorMap.entrySet())
			res.addAll(e.getValue());


		return res;
	}

	private synchronized static boolean typeCheck(IProject project, Map<PSource,IFile> sourceToFileMap)
	{
		if (project == null) return false;
		if (sourceToFileMap == null) return false;
		Thread.currentThread().setName("Type Checker");
		Registry reg = RegistryFactory.getInstance(project.getName()).getRegistry();
		TypeIssueHandler issueHandler = VanillaFactory.newCollectingIssueHandle(reg);
		CmlTypeChecker typeChecker = VanillaFactory.newTypeChecker(sourceToFileMap.keySet(), issueHandler);
		try {
			boolean result =  typeChecker.typeCheck();
			// set error markers
			List<CMLTypeError> errorsThatMatter = filterErrros(issueHandler.getTypeErrors());
			for(CMLTypeError error : errorsThatMatter)
			{
				INode offendingNode = error.getOffendingNode();
				if (offendingNode != null)
				{
					PSource source = ( offendingNode instanceof PSource ? (PSource)offendingNode : offendingNode.getAncestor(PSource.class));
					if (source != null)
					{
						IFile file = sourceToFileMap.get(source);
						if (file != null)
						{
							IMarker errorMarker = file.createMarker(IMarker.PROBLEM);
							LexLocation loc = error.getLocation();
							String offStr = offendingNode == null ? "" : ""+offendingNode.getClass();
							if (loc != null)
								setProblem(errorMarker,error.getDescription()+offStr, loc.startOffset, loc.endOffset);
							else
							{
								setProblem(errorMarker,error.getDescription(), 1,1);
								setWarning(project.createMarker(IMarker.PROBLEM), "AstNode: "+offendingNode+" has null location.");
							}
						}
						else
							System.out.println("No IFile resource found for source: "+source);
					}
					else
						System.out.println("Could not find source for: "+offendingNode);
				}
				else
					System.out.println("Error messages with null node: "+error);
			}

			// set warning markers
			for(CMLTypeWarning warning : issueHandler.getTypeWarnings())
			{
				INode offendingNode = warning.getOffendingNode();
				PSource source = offendingNode.getAncestor(PSource.class);
				IFile file = sourceToFileMap.get(source);
				IMarker errorMarker = file.createMarker(IMarker.PROBLEM);
				LexLocation loc = warning.getLocation();
				if (loc != null )
					setWarning(errorMarker,warning.getDescription(), loc.startOffset, loc.endOffset);

			}

			return result;
		} catch (Exception e)
		{	
			try {
				IMarker projectMarker = project.createMarker(IMarker.PROBLEM);
				setProblem(projectMarker, "Type checking on this project failed.");
			} catch (CoreException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			return false;
		}

	}




	private IProject[] buildit(IProgressMonitor monitor) throws CoreException {

		// get project
		IProject project = getProject();

		
		// Remove all markers from project
		project.deleteMarkers(IMarker.PROBLEM, true,
				IResource.DEPTH_INFINITE);

		
		// Remove all errors in the registry for this project
		String projectName = project.getName();
		Registry tcReg = RegistryFactory.getInstance(projectName).getRegistry();
		tcReg.prune(CMLIssueList.class);
		
		monitor.beginTask("Building project: "+projectName, 5);
		
		// Create a visitor
		CmlBuildVisitor buildVisitor = new CmlBuildVisitor(monitor);
		
		
		// run the parser on every cml file in the project
		project.accept(buildVisitor);
		

		// Type Check all sources in this project
		Collection<CmlSourceUnit> allSourceUnits = CmlSourceUnit.getAllSourceUnits();
		Map<PSource,IFile> sourceToFileMap = new HashMap<PSource,IFile>();
		for(CmlSourceUnit sourceUnit : allSourceUnits)
		{
			monitor.subTask("Type checking, adding source "+sourceUnit);
			if (sourceUnit.isParsedOk() && 
					sourceUnit.getFile().getProject() == project 
					&& sourceUnit.getSourceAst() != null)
				sourceToFileMap.put(sourceUnit.getSourceAst(), sourceUnit.getFile());
		}
		monitor.subTask("Type checking");
		typeCheck(project,sourceToFileMap);


		// Return the projects that should be build also as result of rebuilding
		// this
		return null;

	}

	
	public static void addBuilderToProject(IProject project) {

		   // Cannot modify closed projects.
		   if (!project.isOpen())
		      return;

		   // Get the description.
		   IProjectDescription description;
		   try {
		      description = project.getDescription();
		   }
		   catch (CoreException e) {
		      e.printStackTrace();
		      return;
		   }

		   // Look for builder already associated.
		   ICommand[] cmds = description.getBuildSpec();
		   for (int j = 0; j < cmds.length; j++)
		      if (cmds[j].getBuilderName().equals(ICmlCoreConstants.BUILDER_ID))
		         return;

		   // Associate builder with project.
		   ICommand newCmd = description.newCommand();
		   newCmd.setBuilderName(ICmlCoreConstants.BUILDER_ID);
		   List<ICommand> newCmds = new ArrayList<ICommand>();
		   newCmds.addAll(Arrays.asList(cmds));
		   newCmds.add(newCmd);
		   description.setBuildSpec(
		      (ICommand[]) newCmds.toArray(
		         new ICommand[newCmds.size()]));
		   try {
		      project.setDescription(description, null);
		   }
		   catch (CoreException e) {
		      e.printStackTrace();
		   }
		}

	public static void removeBuilderFromProject(IProject project) {

		   // Cannot modify closed projects.
		   if (!project.isOpen())
		      return;

		   // Get the description.
		   IProjectDescription description;
		   try {
		      description = project.getDescription();
		   }
		   catch (CoreException e) {
		      e.printStackTrace();
		      return;
		   }

		   // Look for builder.
		   int index = -1;
		   ICommand[] cmds = description.getBuildSpec();
		   for (int j = 0; j < cmds.length; j++) {
		      if (cmds[j].getBuilderName().equals(ICmlCoreConstants.BUILDER_ID)) {
		         index = j;
		         break;
		      }
		   }
		   if (index == -1)
		      return;

		   // Remove builder from project.
		   List<ICommand> newCmds = new ArrayList<ICommand>();
		   newCmds.addAll(Arrays.asList(cmds));
		   newCmds.remove(index);
		   description.setBuildSpec(
		      (ICommand[]) newCmds.toArray(
		         new ICommand[newCmds.size()]));
		   try {
		      project.setDescription(description, null);
		   }
		   catch (CoreException e) {
		      e.printStackTrace();
		   }
		}

	
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		super.clean(monitor);
	}
	
	@Override
	protected void startupOnInitialize() {
		super.startupOnInitialize();
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args,
			IProgressMonitor monitor) throws CoreException {
		return buildit(monitor);
	}

}
