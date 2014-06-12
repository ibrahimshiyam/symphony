package eu.compassresearch.pog.tests;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.node.INode;
import org.overture.pog.pub.IProofObligationList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import eu.compassresearch.core.analysis.pog.utility.PogPubUtil;
import eu.compassresearch.pog.tests.utils.TestFileProvider;
import eu.compassresearch.pog.tests.utils.TestInputHelper;
import eu.compassresearch.pog.tests.utils.TestResultHelper;

@RunWith(Parameterized.class)
public class BugRegressionTest
{

	private String modelPath;
	private String resultPath;

	@Before
	public void setup()
	{
	}

	public BugRegressionTest(String _,String model, String result)
	{
		this.modelPath = model;
		this.resultPath = result;
	}

	@Parameters(name = "{index} : {0}")
	public static Collection<Object[]> testData()
	{
		return TestFileProvider.bugRegs();
	}

	@Test
	public void testWithCompare() throws AnalysisException, IOException,
			URISyntaxException
	{

		List<INode> ast = TestInputHelper.getAstFromName(modelPath);
		IProofObligationList ipol = PogPubUtil.generateProofObligations(ast);

		Gson gson = new Gson();
		String json = IOUtils.toString(new FileReader(resultPath));
		Type datasetListType = new TypeToken<Collection<PoResult>>()
		{
		}.getType();
		List<PoResult> results = gson.fromJson(json, datasetListType);

		TestResultHelper.checkSameElements(results, ipol);

	}

}