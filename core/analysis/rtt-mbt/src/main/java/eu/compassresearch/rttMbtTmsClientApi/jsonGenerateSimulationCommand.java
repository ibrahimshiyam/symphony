/**
 * 
 */
package eu.compassresearch.rttMbtTmsClientApi;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 * @author uwe
 *
 */
public class jsonGenerateSimulationCommand extends jsonCommand {

	private String testProcName;

	public jsonGenerateSimulationCommand(RttMbtClient client) {
		super(client);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String getJsonCommandString() {
		// check if project name is properly assigned
		if (client.getRttProjectPath() == null) {
			System.err.println("[ERROR]: project name not assigned!");
			return null;
		}

		// add parameters
		Map params = new LinkedHashMap();
		params.put("user", client.getUserName());
		params.put("user-id", client.getUserId());
		params.put("project-name", client.toUnixPath(client.removeLocalWorkspace(client.getRttProjectPath())));
		params.put("test-procedure-path", client.toUnixPath(testProcName));
		// create command
		JSONObject cmd = new JSONObject();
		cmd.put("generate-simulation-command", params);
		return cmd.toJSONString();
	}

	public JSONObject getParameters(JSONObject reply) {
		if (reply == null) {
			return null;
		}
		return (JSONObject)reply.get("simulation-generation-result");
	}

	public void handleParameters(JSONObject parameters) {
		// get the parameter list
		if (parameters == null) {
			return;
		}
		// get the result
		String checkResult = (String)parameters.get("result");
		if (!(checkResult.equals("PASS"))) {
			resultValue = false;
		} else {
			resultValue = true;
		}
	}

	public String getTestProcName() {
		return testProcName;
	}

	public void setTestProcName(String testProcName) {
		this.testProcName = testProcName;
	}
}
