package eu.compassresearch.ide.interpreter.views;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import eu.compassresearch.core.interpreter.debug.TransitionDTO;
import eu.compassresearch.ide.interpreter.CmlUtil;
import eu.compassresearch.ide.interpreter.model.CmlDebugTarget;

public class CmlEventOptionView extends ViewPart implements IDebugEventSetListener ,IDoubleClickListener, ISelectionChangedListener
{
	ListViewer viewer;
	List<String> options = new LinkedList<String>();
	private Map<StyledText, List<StyleRange>> lastSelectedRanges = new HashMap<StyledText, List<StyleRange>>();
	CmlDebugTarget target;

	public CmlEventOptionView()
	{
		DebugPlugin.getDefault().addDebugEventListener(this);
		//FIXME this is very unsafe
	}
	
	@Override
	public void handleDebugEvents(final DebugEvent[] events)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				for(DebugEvent e : events)
				{
					if(e.getKind() == DebugEvent.SUSPEND)
					{
						target = (CmlDebugTarget)DebugPlugin.getDefault().getLaunchManager().getDebugTargets()[0];
						List<TransitionDTO> transitions = target.getLastState().getTransitions();
						Collections.sort(transitions, new Comparator<TransitionDTO>(){

							@Override
							public int compare(TransitionDTO o1, TransitionDTO o2)
							{
								if (o1.getName().equals("tock"))
									return -1;
								else if (o2.getName().equals("tock"))
									return 1;
								else
									return o1.getName().compareToIgnoreCase(o2.getName());
							}

						});
						viewer.setInput(transitions);
						viewer.setSelection(new StructuredSelection(transitions.get(0)));
					}
					else if(e.getKind() == DebugEvent.TERMINATE)
					{
						viewer.setInput(null);
					}
				}
			}
		});
	}
	
	@Override
	public String getTitle()
	{
		return "Event Options";
	}

	@Override
	public void setFocus()
	{
	}

	@Override
	public void createPartControl(final org.eclipse.swt.widgets.Composite parent)
	{
		// Composite composite = new Composite(parent, SWT.NONE);
		viewer = new ListViewer(parent);
		viewer.addDoubleClickListener(this);
		viewer.addSelectionChangedListener(this);
		viewer.setContentProvider(new IStructuredContentProvider()
		{

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput)
			{
//				System.out.println("Input changed: old=" + oldInput + ", new="
//						+ newInput);
			}

			@Override
			public void dispose()
			{

			}

			@SuppressWarnings("rawtypes")
			@Override
			public Object[] getElements(Object inputElement)
			{
				return ((List) inputElement).toArray();
			}
		});
	}

	private void selectChoice(TransitionDTO event)
	{
		target.select(event);
		finish();
	}
	
	private void finish()
	{
		Display.getDefault().syncExec(new Runnable()
		{
			@Override
			public void run()
			{
				CmlUtil.clearSelections(lastSelectedRanges);
				viewer.setInput(null);
				viewer.refresh();
			}
		});
	}

	
	@Override
	public void doubleClick(DoubleClickEvent event)
	{
		IStructuredSelection selection = null;
		TransitionDTO choice = null;

		if (event.getSelection() instanceof IStructuredSelection)
		{
			selection = (IStructuredSelection) event.getSelection();
			choice = (TransitionDTO) selection.getFirstElement();
		}

		selectChoice(choice);
		
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		if (event.getSelection() instanceof IStructuredSelection)
		{
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();

			TransitionDTO choice = (TransitionDTO) selection.getFirstElement();
			CmlUtil.clearSelections(lastSelectedRanges);
			CmlUtil.setSelectionFromLocations(choice.getLocations(), lastSelectedRanges);
			// get a random one and set the
			StyledText st = lastSelectedRanges.keySet().iterator().next();
			CmlUtil.showLocation(st, choice.getLocations().get(0));
		}
	}
}
