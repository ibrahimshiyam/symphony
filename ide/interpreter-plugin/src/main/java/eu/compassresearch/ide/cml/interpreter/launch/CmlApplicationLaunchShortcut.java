package eu.compassresearch.ide.cml.interpreter.launch;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.overture.ide.core.resources.IVdmProject;

import eu.compassresearch.ast.definitions.AProcessDefinition;
import eu.compassresearch.ast.program.PSource;
import eu.compassresearch.core.interpreter.debug.CmlInterpreterLaunchConfigurationConstants;
import eu.compassresearch.ide.cml.interpreter.CmlUtil;
import eu.compassresearch.ide.cml.interpreter.ICmlDebugConstants;
import eu.compassresearch.ide.core.resources.ICmlSourceUnit;
import eu.compassresearch.ide.ui.editor.core.CmlEditor;

public class CmlApplicationLaunchShortcut implements ILaunchShortcut2
{

	@Override
	public void launch(ISelection selection, String mode) {
		
		if(selection instanceof TreeSelection)
		{
			TreeSelection treeSelection = (TreeSelection)selection;
			//find the associated CmlSourceUnit for this selected file. 
			searchAndLaunch(treeSelection.getFirstElement(),mode);
		}
		
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		System.out.println(editor.toString());		
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		
		if(selection instanceof TreeSelection)
		{
			TreeSelection treeSelection = (TreeSelection)selection;
			//find the associated CmlSourceUnit for this selected file. 
			IFile file = (IFile)treeSelection.getFirstElement();
			
			List<ILaunchConfiguration> foundConfs = findLaunchConfigurationsByFile(file);
			
			return foundConfs.toArray(new ILaunchConfiguration[foundConfs.size()]);
		}
		else 
			return null;
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
		return null;
	}
	
	@Override
	public IResource getLaunchableResource(ISelection selection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResource getLaunchableResource(IEditorPart editorpart) {
		return null;
	}

	/**
	 * Protected Methods
	 */
	protected void searchAndLaunch(Object file, String mode) {
		
		IFile ifile = (IFile) file;
		ICmlSourceUnit source = (ICmlSourceUnit) ifile.getAdapter(ICmlSourceUnit.class);
		
		//Open the file that are going being debugged
		try {
			IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), ifile);
		} catch (PartInitException e1) {
			e1.printStackTrace();
		}

		IVdmProject vdmProject = (IVdmProject)ifile.getProject().getAdapter(IVdmProject.class);
		
		if(vdmProject != null && vdmProject.getModel().isParseCorrect() && source != null ) //&& vdmProject.getModel().isTypeCorrect())
		{
			PSource ast = source.getSourceAst();
		
			List<PSource> sourceList = new LinkedList<PSource>();
			sourceList.add(ast);
			List<AProcessDefinition> defsInFile = CmlUtil.GetGlobalProcessesFromSource(sourceList); 

			if(defsInFile.size() == 1)
			{
				String processName = defsInFile.get(0).getName().getName();
				launch((IFile)file,processName,mode);
			}
			else if(defsInFile.size() > 1)
			{

				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(null, new LabelProvider()
				{
					@Override
					public String getText(Object element) {

						if(element instanceof AProcessDefinition)
							return ((AProcessDefinition)element).getName().getName();
						else			
							return null;
					}

				}, new BaseWorkbenchContentProvider()
				{
					@Override
					public boolean hasChildren(Object element)
					{
						if (element instanceof AProcessDefinition)
						{
							return false;
						} else
						{
							return super.hasChildren(element);
						}
					}

					@Override
					public Object[] getElements(Object element)
					{
						List<AProcessDefinition> pdefs = (List<AProcessDefinition>)element;
						return pdefs.toArray();


					}

				});
				dialog.setTitle("Process Selection");
				dialog.setMessage("Select a process:");
				dialog.setComparator(new ViewerComparator());

				dialog.setInput( defsInFile);

				if (dialog.open() == Window.OK)
				{
					if (dialog.getFirstResult() != null
							&& dialog.getFirstResult() instanceof AProcessDefinition)
						//&& ((IProject) dialog.getFirstResult()).getAdapter(IVdmProject.class) != null)
					{
						String processName = ((AProcessDefinition) dialog.getFirstResult()).getName().getName();
						launch((IFile)file,processName,mode);
					}
				}
			}
		}
		else
		//If no ast is attached then there are either parser or type errors
		{
			if(!vdmProject.getModel().isParseCorrect())
				MessageDialog.openError(null, "Launch failure", "The Cml model is not parsed correctly and therefore cannot be launched. This could be a glitch, try to close and open the source.");
			else if(!vdmProject.getModel().isTypeCorrect())
				MessageDialog.openError(null, "Launch failure", "The Cml model is not typecheck correctly and therefore cannot be launched. This could be a glitch, try to close and open the source.");
			else
				MessageDialog.openError(null,"Launch failure", "The Cml model is not loaded correctly and therefore cannot be launched");
		}
	
	}

    protected void launch(IFile sourceUnit,String processName, String mode) {
        try {
            ILaunchConfiguration config = findLaunchConfiguration(sourceUnit,processName, mode);
            
            if (config != null) {
             config.launch(mode, null);
            }
        } catch (CoreException e) {
            /* Handle exceptions*/
        	e.printStackTrace();
        }
    }
    
    protected List<ILaunchConfiguration> findLaunchConfigurationsByFile(IFile file) 

    {
    	ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    	ILaunchConfigurationType ctype = 
    			launchManager.getLaunchConfigurationType(ICmlDebugConstants.ATTR_LAUNCH_CONFIGURATION_TYPE);
    	
    	
    	//Get the current project which this file lives in
    	
    	List<ILaunchConfiguration> result = new LinkedList<ILaunchConfiguration>();
    	
    	try {
			for(ILaunchConfiguration lc : launchManager.getLaunchConfigurations(ctype))
			{
				String projectName = lc.getAttribute(ICmlLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
				if(file.getProject().getName().equals(projectName))
					result.add(lc);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
    	
    	return result;
    }
    
    protected ILaunchConfiguration findLaunchConfiguration(IFile sourceUnit,String processName, String mode) throws CoreException
    {
    	List<ILaunchConfiguration> confs = findLaunchConfigurationsByFile(sourceUnit); 
    	
    	ILaunchConfiguration result = null;
    	
    	for(ILaunchConfiguration lc : confs)
    	{
    		String foundProcessName = lc.getAttribute(CmlInterpreterLaunchConfigurationConstants.PROCESS_NAME.toString(), "");
    		if(foundProcessName.equals(processName))
    			result = lc;
    	}
    	
    	//create a new one
    	if(result == null)
    	{
    		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        	ILaunchConfigurationType ctype = 
        			launchManager.getLaunchConfigurationType(ICmlDebugConstants.ATTR_LAUNCH_CONFIGURATION_TYPE);
        	
        	
        	ILaunchConfigurationWorkingCopy lcwc = ctype.newInstance(null, launchManager.generateLaunchConfigurationName("Quick Launch"));
        	
        	lcwc.setAttribute(CmlInterpreterLaunchConfigurationConstants.PROCESS_NAME.toString(), processName);
        	lcwc.setAttribute(ICmlLaunchConfigurationConstants.ATTR_PROJECT_NAME, 
        			sourceUnit.getProject().getName());
        	lcwc.setAttribute(CmlInterpreterLaunchConfigurationConstants.CML_SOURCES_PATH.toString(),
        			CmlUtil.getCmlSourcesPathsFromProject(sourceUnit.getProject()));
        	lcwc.doSave();		
        	result = lcwc;
    	}
    	
    	return result;
    }
}
