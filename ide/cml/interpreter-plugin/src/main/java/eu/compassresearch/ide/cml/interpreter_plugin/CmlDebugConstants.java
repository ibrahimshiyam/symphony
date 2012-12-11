package eu.compassresearch.ide.cml.interpreter_plugin;

public enum CmlDebugConstants {
	
	ATTR_LAUNCH_CONFIGURATION_TYPE("eu.compassresearch.ide.cml.interpreter_plugin.launchConfigurationType"),
	ATTR_DEBUGGER_CONFIG_PARAMS("eu.compassresearch.ide.cml.interpreter_plugin.debugger_config_params"),
	ID_CML_DEBUG_MODEL("eu.compassresearch.ide.cml.interpreter_plugin.modelId");
	
	
	private final String str;      

    private CmlDebugConstants(String s) {
    	str = s;
    }
    
    public String toString(){
       return str;
    }
}
