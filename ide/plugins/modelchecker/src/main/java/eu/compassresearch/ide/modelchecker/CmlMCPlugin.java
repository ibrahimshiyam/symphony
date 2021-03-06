package eu.compassresearch.ide.modelchecker;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import eu.compassresearch.core.analysis.modelchecker.api.FormulaIntegrationException;
import eu.compassresearch.core.analysis.modelchecker.api.FormulaIntegrator;
import eu.compassresearch.core.analysis.modelchecker.graphBuilder.util.ProcessController;

public class CmlMCPlugin extends AbstractUIPlugin implements IStartup{
	
	public static final String PLUGIN_ID = "eu.compassresearch.ide.modelchecker";
	public static final String SAT_IMG_ID = "sat.png";
	public static final String UNSAT_IMG_ID = "unsat.png";
    
	// The shared instance
	private static CmlMCPlugin plugin;
	public static boolean FORMULA_OK = true;
	public static boolean DOT_OK = true;
	public static final String formulaNotInstalledMsg = "Microsoft FORMULA could not be started.\n Please download and install it from \n" + 
	"www.research.microsoft.com/en-us/um/redmond/projects/formula \n\n" + "If you have installed FORMULA, include the <FORMULA_INSTALL_FOLDER>\\Base System\\ folder  in your PATH environment variable. \n\n" + "The CML model checker depends on FORMULA to work.";
	public static final String dotNotInstalledMsg = "The dot.exe program was not found.\n" + "It is distributed with GraphViz (www.graphviz.org). Please download and install GraphViz.\n" + "If you have installed GraphViz, include the <GRAPHVIZ_INSTALL_FOLDER>\\bin\\ folder  in your PATH environment variable. \n\n" + "The dot.exe program is necessary to build the counterexample.";
	
    	
	public void earlyStartup() {
		//this line is useful to start Formula (if it is installed)
		//FORMULA_OK = FormulaIntegrator.checkFormulaInstallation();
    	checkAuxiliarySoftware();
	}
	
	
	
	@Override
	public void shutdown() throws CoreException {
		
	}

	public CmlMCPlugin() {
		super();
		CmlMCPlugin.plugin = this;
	}

	

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		//checkAuxiliarySoftware();
	}
	
	public void checkAuxiliarySoftware(){
		if(!FormulaIntegrator.checkFormulaInstallation()){
    		FORMULA_OK = false;
    		//logWarningMessage(formulaNotInstalledMsg);
    	}
		if(!ProcessController.checkDotInstallation()){
			DOT_OK = false;
			//logWarningMessage(dotNotInstalledMsg);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		shutDownAuxiliarySoftware();
	}

	private void shutDownAuxiliarySoftware(){
			try {
				if (FormulaIntegrator.hasInstance())
					FormulaIntegrator.getInstance().finalize();
			} catch (FormulaIntegrationException e) {
				log(e);
			} catch (Throwable e) {
				log(e);
			}
	}
	@Override
    protected void initializeImageRegistry(ImageRegistry registry) {
        super.initializeImageRegistry(registry);
        Bundle bundle = Platform.getBundle(PLUGIN_ID);

        ImageDescriptor myImageSat = ImageDescriptor.createFromURL(
              FileLocator.find(bundle,new Path("icons/sat.png"),null));
        ImageDescriptor myImageUnsat = ImageDescriptor.createFromURL(
                FileLocator.find(bundle,new Path("icons/unsat.png"),null));
        registry.put(SAT_IMG_ID, myImageSat);
        registry.put(UNSAT_IMG_ID, myImageUnsat);
    }
	
    public static CmlMCPlugin getDefault() {
		return plugin;
	}
    public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }
    
    public static void log(Throwable exception){
		getDefault().getLog().log(new Status(IStatus.ERROR, MCConstants.PLUGIN_ID, CmlMCPlugin.class.getSimpleName(), exception));
	}

	public static void log(String message, Exception exception){
		getDefault().getLog().log(new Status(IStatus.ERROR, MCConstants.PLUGIN_ID, message, exception));
	}


	public static void logWarningMessage(String message){
		getDefault().getLog().log(new Status(IStatus.WARNING, MCConstants.PLUGIN_ID, message));
	}

    
  }
