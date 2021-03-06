package eu.compassresearch.ide.ui.navigator;

import org.eclipse.jface.viewers.StyledString;
import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.definitions.AAssignmentDefinition;
import org.overture.ast.definitions.AExplicitOperationDefinition;
import org.overture.ast.definitions.AImplicitOperationDefinition;
import org.overture.ast.definitions.AStateDefinition;
import org.overture.ast.definitions.AValueDefinition;
import org.overture.ast.node.INode;
import org.overture.ast.types.AOperationType;
import org.overture.ast.types.AVoidType;
import org.overture.ast.types.PType;
import org.overture.ide.ui.internal.viewsupport.VdmElementLabels;

import eu.compassresearch.ast.analysis.AnswerCMLAdaptor;
import eu.compassresearch.ast.definitions.AActionDefinition;
import eu.compassresearch.ast.definitions.AChannelDefinition;
import eu.compassresearch.ast.definitions.AChansetDefinition;
import eu.compassresearch.ast.definitions.ANamesetDefinition;
import eu.compassresearch.ast.definitions.AProcessDefinition;
import eu.compassresearch.ast.process.AActionProcess;

public class CmlElementLabels extends VdmElementLabels
{
	static TextLabelCreator textLabelCreator = new TextLabelCreator();

	public static StyledString getStyledTextLabel(Object element, long flags)
	{

		if (element instanceof INode)
		{
			StyledString result = null;
			try
			{
				result = ((INode) element).apply(textLabelCreator);
			} catch (AnalysisException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (result != null)
			{
				return result;
			}
		}

		return VdmElementLabels.getStyledTextLabel(element, flags);
	}

	private static class TextLabelCreator extends
			AnswerCMLAdaptor<StyledString>
	{
		@Override
		public StyledString caseAExplicitOperationDefinition(
				AExplicitOperationDefinition node) throws AnalysisException
		{
			StyledString result = new StyledString();

			result.append(node.getName().getSimpleName());

			if (node.getType() instanceof AOperationType)
			{
				AOperationType type = (AOperationType) node.getType();
				if (type.getParameters().size() == 0)
				{
					result.append("() ");
				} else
				{
					result.append("(");
					int i = 0;
					while (i < type.getParameters().size() - 1)
					{
						PType definition = (PType) type.getParameters().get(i);
						result.append(getSimpleTypeString(definition) + ", ");

						i++;
					}
					PType definition = (PType) type.getParameters().get(i);
					result.append(getSimpleTypeString(definition) + ")");
				}

				if (type.getResult() instanceof AVoidType)
				{
					result.append(" : ()", StyledString.DECORATIONS_STYLER);
				} else
				{
					result.append(" : " + getSimpleTypeString(type.getResult()), StyledString.DECORATIONS_STYLER);
				}
			}

			return result;
		}

		@Override
		public StyledString caseAImplicitOperationDefinition(
				AImplicitOperationDefinition node) throws AnalysisException
		{
			StyledString result = new StyledString();

			result.append(node.getName().getSimpleName());

			if (node.getType() instanceof AOperationType)
			{
				AOperationType type = (AOperationType) node.getType();
				if (type.getParameters().size() == 0)
				{
					result.append("() ");
				} else
				{
					result.append("(");
					int i = 0;
					while (i < type.getParameters().size() - 1)
					{
						PType definition = (PType) type.getParameters().get(i);
						result.append(getSimpleTypeString(definition) + ", ");

						i++;
					}
					PType definition = (PType) type.getParameters().get(i);
					result.append(getSimpleTypeString(definition) + ")");
				}

				if (type.getResult() instanceof AVoidType)
				{
					result.append(" : ()", StyledString.DECORATIONS_STYLER);
				} else
				{
					result.append(" : " + getSimpleTypeString(type.getResult()), StyledString.DECORATIONS_STYLER);
				}

			}

			return result;

		}

		@Override
		public StyledString caseAActionDefinition(AActionDefinition node)
				throws AnalysisException
		{
			StyledString result = new StyledString();
			result.append(node.getName().getName());
			return result;
		}

		public StyledString caseAValueDefinition(AValueDefinition node)
				throws AnalysisException
		{
			StyledString result = new StyledString();
			result.append(node.getPattern().toString());
			if (node.getType() != null)
			{
				if (node.getType().getLocation().getModule().toLowerCase().equals("default"))
				{
					result.append(" : " + getSimpleTypeString(node.getType()), StyledString.DECORATIONS_STYLER);
				} else
				{
					result.append(" : " + // node.getType().getLocation().getModule() + "`"
							// +
							getSimpleTypeString(node.getType()), StyledString.DECORATIONS_STYLER);
				}
			}
			return result;
		}

		@Override
		public StyledString caseAChannelDefinition(AChannelDefinition node)
				throws AnalysisException
		{
			StyledString result = new StyledString();
			result.append(node.getName().getName());
			if (node.getType() != null)
			{
				final String type = getSimpleTypeString(node.getType());
				if (!type.isEmpty())
				{
					result.append(" : " + type, StyledString.DECORATIONS_STYLER);
				}
			}

			return result;
		}

		@Override
		public StyledString caseAProcessDefinition(AProcessDefinition node)
				throws AnalysisException
		{
			StyledString result = new StyledString();

			result.append(node.getName().getName());
			if (node.getProcess() instanceof AActionProcess)
			{
				result.append(" : "
						+ truncateString(35, ((AActionProcess) node.getProcess()).getAction().toString()), StyledString.DECORATIONS_STYLER);
			}
			return result;
		}

		private String truncateString(int length, String string)
		{
			string = string.replace('\n', ' ');
			if (string.length() > length - 3)
			{
				return string.substring(0, length - 3) + "...";
			}
			return string;
		}

		@Override
		public StyledString caseAStateDefinition(AStateDefinition node)
				throws AnalysisException
		{
			StyledString result = new StyledString();
			try
			{// FIXME this try catch have to be removed and the state object should not be made by the parser if it
				// doesn't contain anything but null pointers
				result.append(node.getName().getSimpleName());
				if (node.getType().getLocation().getModule().toLowerCase().equals("default"))
				{
					result.append(" : " + getSimpleTypeString(node.getType()), StyledString.DECORATIONS_STYLER);
				} else
				{
					result.append(" : "
							+ node.getType().getLocation().getModule() + "`"
							+ getSimpleTypeString(node.getType()), StyledString.DECORATIONS_STYLER);
				}
			} catch (Exception e)
			{
				result.append("Parser did not populate class: "
						+ node.getClass().getName());
			}
			return result;
		}

		@Override
		public StyledString caseAAssignmentDefinition(AAssignmentDefinition node)
				throws AnalysisException
		{

			StyledString result = new StyledString();
			result.append(node.getName().getSimpleName());
			result.append(" : " + getSimpleTypeString(node.getType()), StyledString.DECORATIONS_STYLER);
			return result;
		}

		@Override
		public StyledString caseANamesetDefinition(ANamesetDefinition node)
				throws AnalysisException
		{
			StyledString result = new StyledString();
			result.append(node.getIdentifier().getName());
			result.append(" : " + node.getNamesetExpression());// getSimpleTypeString(node.getType()),
																// StyledString.DECORATIONS_STYLER);
			return result;
		}

		@Override
		public StyledString caseAChansetDefinition(AChansetDefinition node)
				throws AnalysisException
		{
			StyledString result = new StyledString();
			result.append(node.getIdentifier().toString());
			result.append(" : "
					+ node.getChansetExpression().toString().replace('[', '{').replace(']', '}'), StyledString.DECORATIONS_STYLER);
			return result;
		}

		@Override
		public StyledString createNewReturnValue(INode node)
				throws AnalysisException
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public StyledString createNewReturnValue(Object node)
				throws AnalysisException
		{
			// TODO Auto-generated method stub
			return null;
		}

	}

}
