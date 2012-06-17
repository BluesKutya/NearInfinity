// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

import infinity.NearInfinity;
import infinity.datatype.ResourceRef;
import infinity.datatype.StringRef;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.bcs.Compiler;
import infinity.resource.bcs.Decompiler;
import infinity.resource.key.ResourceEntry;
import infinity.search.DialogSearcher;
import infinity.util.StringResource;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

final class Viewer extends JTabbedPane implements ChangeListener, TableModelListener
{
	private final DlgResource dlg;
	private FlatViewer flatViewer;
	private TreeViewer treeViewer;
	private static int viewType;
	private static int hSplitPos = NearInfinity.getInstance().getWidth() / 2;
	private static int wSplitPos = NearInfinity.getInstance().getHeight() / 2;

	Viewer(DlgResource dlg)
	{
		this.dlg = dlg;
		dlg.updateViewerLists();
		
		flatViewer = new FlatViewer();
		treeViewer = new TreeViewer();
		this.addTab("Flat", flatViewer);
		this.addTab("Tree", treeViewer);
		this.addChangeListener(this);
		this.setSelectedIndex(viewType);
	}

	public void showStateWithStructEntry(StructEntry entry)
	{
		flatViewer.showStateWithStructEntry(entry);
		treeViewer.showStateWithStructEntry(entry);
	}


	public void stateChanged(ChangeEvent e)
	{
		viewType = this.getSelectedIndex();
	}

	public void tableChanged(TableModelEvent e)
	{
		dlg.updateViewerLists();
		flatViewer.tableChanged(e);
	}
	
	private final class TreeViewer extends JSplitPane implements ComponentListener, TreeSelectionListener
	{
		private final DialogTreeModel treeModel;
		private final JTree dialogTree;
		private final JSplitPane dialogDetail;
		private final DlgPanel dialogTextPanel, dialogTriggerPanel, dialogActionPanel;
		

		TreeViewer()
		{
			treeModel  = new DialogTreeModel();
			dialogTree = new JTree(treeModel);
		    dialogTree.setCellRenderer(new DialogTreeRenderer());
		    dialogTree.putClientProperty("JTree.lineStyle", "Angled");
			dialogTree.clearSelection();
		    dialogTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			dialogTree.setRootVisible(false);
			dialogTree.setShowsRootHandles(true);
			dialogTree.addTreeSelectionListener(this);

			dialogTextPanel    = new DlgPanel("Text", true);
			dialogTriggerPanel = new DlgPanel("Trigger", false);
			dialogActionPanel  = new DlgPanel("Action", false);
			
			JPanel atrPanel = new JPanel();
			atrPanel.setLayout(new GridLayout(1, 2, 6, 6));
			atrPanel.add(dialogTriggerPanel);
			atrPanel.add(dialogActionPanel);
			
			dialogDetail = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dialogTextPanel, atrPanel);

			dialogDetail.setBorder(BorderFactory.createLoweredBevelBorder());
			dialogDetail.setDividerLocation(wSplitPos);
			this.setRightComponent(dialogDetail);

			this.setLeftComponent(new JScrollPane(dialogTree));
			this.setDividerLocation(hSplitPos);
			
			dialogDetail.addComponentListener(this);
			atrPanel.addComponentListener(this);
		}
		
		public void showStateWithStructEntry(StructEntry entry)
		{
			TreePath path = null;

			if (entry instanceof State || entry instanceof Transition)
				path = treeModel.getPath(entry);
			else if (entry instanceof StateTrigger) {
				int triggerOffset = ((StateTrigger) entry).getOffset();
				int nr = 0;
				for (StateTrigger trig : dlg.getStaTriList()) {
					if (trig.getOffset() == triggerOffset)
						break;
					nr++;
				}

				for (State state : dlg.getStateList())
					if (state.getTriggerIndex() == nr)
					{
						path = treeModel.getPath(state);
						break;
					}
			}
			else if (entry instanceof ResponseTrigger) {
				int triggerOffset = ((ResponseTrigger) entry).getOffset();
				int nr = 0;
				for (ResponseTrigger trig : dlg.getTransTriList())
				{
					if (trig.getOffset() == triggerOffset)
						break;
					nr++;
				}

				for (Transition trans : dlg.getTransList())
					if (trans.getTriggerIndex() == nr && trans.getFlag().isFlagSet(1))
					{
						path = treeModel.getPath(trans);
						break;
					}
			}
			else if (entry instanceof Action)
			{
				int actionOffset = ((Action) entry).getOffset();
				int nr = 0;
				for (Action action : dlg.getActionList())
				{
					if (action.getOffset() == actionOffset)
						break;
					nr++;
				}

				for (Transition trans : dlg.getTransList())
					if (trans.getActionIndex() == nr && trans.getFlag().isFlagSet(2))
					{
						path = treeModel.getPath(trans);
						break;
					}
			}
			else if (entry instanceof StringRef) {
				// this can happen with the dlg search
				// check all states and transitions
				int strref = ((StringRef) entry).getValue();
				boolean found = false;
				for (State state : dlg.getStateList())
					if (state.getResponse().getValue() == strref)
					{
						found = true;
						path = treeModel.getPath(state);
						break;
					}
				if (!found)
					for (Transition trans : dlg.getTransList())
						if (trans.getAssociatedText().getValue() == strref)
						{
							path = treeModel.getPath(trans);
							break;
						}
			}

			if (path != null)
			{
				dialogTree.expandPath(path);
				dialogTree.addSelectionPath(path);
			}
		}

		public void valueChanged(TreeSelectionEvent e)
		{
			State state;
			Transition trans;
			
			if (e.getNewLeadSelectionPath().getLastPathComponent() instanceof State)
			{
				state = (State)e.getNewLeadSelectionPath().getLastPathComponent();
				dialogTextPanel.display(state.getSuperStruct(), state, state.getNumber());
				dialogActionPanel.clearDisplay();
				if (state.getTriggerIndex() != 0xffffffff)
					dialogTriggerPanel.display(state.getSuperStruct(), ((DlgResource) state.getSuperStruct()).getStaTriList().get(state.getTriggerIndex()), state.getTriggerIndex());
				else
					dialogTriggerPanel.clearDisplay();

				if (dlg == state.getSuperStruct())
					flatViewer.showStateWithStructEntry(state);
			}
			else if (e.getNewLeadSelectionPath().getLastPathComponent() instanceof Transition)
			{
				trans = (Transition)e.getNewLeadSelectionPath().getLastPathComponent();
				dialogTextPanel.display(trans.getSuperStruct(), trans, trans.getNumber());
				if (trans.getFlag().isFlagSet(1))
					dialogTriggerPanel.display(trans.getSuperStruct(), ((DlgResource) trans.getSuperStruct()).getTransTriList().get(trans.getTriggerIndex()), trans.getTriggerIndex());
				else
					dialogTriggerPanel.clearDisplay();

				if (trans.getFlag().isFlagSet(2))
					dialogActionPanel.display(trans.getSuperStruct(), ((DlgResource) trans.getSuperStruct()).getActionList().get(trans.getActionIndex()),
							trans.getActionIndex());
				else
					dialogActionPanel.clearDisplay();

				if (dlg == trans.getSuperStruct())
					flatViewer.showStateWithStructEntry(trans);
			}
			return;
		}

		public void componentResized(ComponentEvent e)
		{
			hSplitPos = this.getDividerLocation();
			wSplitPos = dialogDetail.getDividerLocation();
		}

		public void componentMoved(ComponentEvent e)
		{
		}

		public void componentShown(ComponentEvent e)
		{
		}

		public void componentHidden(ComponentEvent e)
		{
		}
		
		private final class DialogTreeModel implements TreeModel
		{
			private final List<TreeModelListener> treeModelListeners = new ArrayList<TreeModelListener>();
			private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("ROOT");

			public Object getRoot()
			{
				return root;
			}

			public Object getChild(Object parent, int index)
			{
				Transition trans;
				DlgResource dlgRes;
				
			    if (parent instanceof State)
			    	return ((DlgResource) ((State) parent).getSuperStruct()).getTransList().get(((State) parent).getFirstTrans() + index);
			    else if (parent instanceof Transition)
			    {
//			    	return ((DlgResource) ((Transition) parent).getSuperStruct()).getStateList().get(((Transition) parent).getNextDialogState());
			    	trans = (Transition) parent;
			    	if (trans.getName() == "None" || dlg.getName().compareToIgnoreCase(trans.getNextDialog().getName()) == 0)
			    		return ((DlgResource) trans.getSuperStruct()).getStateList().get(trans.getNextDialogState());
			    	else
			    	{
			    		dlgRes = (DlgResource) ResourceFactory.getResource(ResourceFactory.getInstance().getResourceEntry(trans.getNextDialog().getResourceName()));
			    		return dlgRes.getStateList().get(trans.getNextDialogState());
			    	}
			    }
			    else if (parent instanceof DefaultMutableTreeNode)
			    {
					for (State state : dlg.getStateList())
						if (state.getTriggerIndex() == index)
							return state;
			    }
			    return null;
			}

			public int getChildCount(Object parent)
			{
				int c = 0;
				
			    if (parent instanceof State)
			    	return ((State) parent).getTransCount();
			    else if (parent instanceof Transition)
			    	return 1;
			    else if (parent instanceof DefaultMutableTreeNode)
			    {
					for (State state : dlg.getStateList())
						if (state.getTriggerIndex() != -1)
							c++;
					return c;
			    }
			    return 0;
			}

			public boolean isLeaf(Object node)
			{
				if (node instanceof Transition)
					return ((Transition) node).getFlag().isFlagSet(3);

				return false;
			}

			public void valueForPathChanged(TreePath path, Object newValue)
			{
			    throw new IllegalArgumentException(); // Not allowed
			}

			public int getIndexOfChild(Object parent, Object child) {
				if (parent instanceof State && child instanceof Transition)
					if (((State) parent).getFirstTrans() <= ((Transition) child).getNumber() 
					&& ((Transition) child).getNumber() < ((State) parent).getFirstTrans() + ((State) parent).getTransCount())
						return (((Transition) child).getNumber() - ((State) parent).getFirstTrans());
					else
						return -1;
				else if (parent instanceof Transition && child instanceof State)
					if (((Transition) parent).getNextDialogState() == ((State) child).getNumber())
						return 1;
					else
						return -1;
				return -1;
			}

			public void addTreeModelListener(TreeModelListener l) 
			{
			    treeModelListeners.add(l);
			}

			public void removeTreeModelListener(TreeModelListener l)
			{
			    treeModelListeners.remove(l);
			}

			public TreePath getPath(Object entry)
			{
				List<Object> path = new ArrayList<Object>();
				
				path.add(entry);
				Object parent = getParent(entry);
				while (parent != null) {
					path.add(parent);
					parent = getParent(parent);
				}
				path.add(root);
				Collections.reverse(path);
				return new TreePath(path.toArray());
			}
			
			public Object getParent(Object entry)
			{
			    if (entry instanceof State)
			    	if (((State) entry).getTriggerIndex() != -1)
			    		return null;
			    	else for (Transition trans : dlg.getTransList())
			    		if (dlg.getName().compareToIgnoreCase(trans.getNextDialog().getName()) == 0 
			    		&& trans.getNextDialogState() == ((State) entry).getNumber() && !trans.getFlag().isFlagSet(3))
			    			return trans;
			    if (entry instanceof Transition)
					for (State state : dlg.getStateList())
						if (state.getFirstTrans() <= ((Transition) entry).getNumber()
						&& ((Transition) entry).getNumber() < (state.getFirstTrans() + state.getTransCount()))
							return state;
				return null;
			}
		}
		
		public final class DialogTreeRenderer extends DefaultTreeCellRenderer
		{
			public Component getTreeCellRendererComponent(JTree tree, Object node, boolean sel, boolean expanded,
					boolean leaf, int row, boolean hasFocus)
			{
				if (node instanceof State)
				{
					super.getTreeCellRendererComponent(tree, node, sel, expanded, leaf, row, hasFocus);
					if (dlg == ((AbstractStruct) node).getSuperStruct())
						setIcon(Icons.getIcon("balloon-white-left-icon.png"));
					else
						setIcon(Icons.getIcon("face-icon.png"));
					setText(((State) node).getResponse().toString());
					return this;
				}
				else if (node instanceof Transition)
				{
					super.getTreeCellRendererComponent(tree, node, sel, expanded, leaf, row, hasFocus);
					if (((Transition) node).getFlag().isFlagSet(0))
					{
						if (((Transition) node).getFlag().isFlagSet(3))
							setIcon(Icons.getIcon("stop.png"));
						else if (((Transition) node).getFlag().isFlagSet(4))
							setIcon(Icons.getIcon("balloon-pencil-icon.png"));
						else
							setIcon(Icons.getIcon("balloon-right-icon.png"));
						setText(((Transition) node).getAssociatedText().toString());
					}
					else if (((Transition) node).getFlag().isFlagSet(3))
					{
						setIcon(Icons.getIcon("stop.png"));
						setText("<END DIALOG>");
					}
					else if (((Transition) node).getFlag().isFlagSet(4))
					{
						setIcon(Icons.getIcon("journal_note.png"));
						setText(((Transition) node).getJournalEntry().toString());
					}
					else
					{
						setIcon(Icons.getIcon("bottom.png"));
						setText("<CONTINUE>");
					}
					return this;
				}
				else
					return super.getTreeCellRendererComponent(tree, node, sel, expanded, leaf, row, hasFocus);
			}
		}
	}

	private final class FlatViewer extends JPanel implements ActionListener, ItemListener, TableModelListener
	{
		private final ButtonPopupMenu bfind;
		private final DlgPanel stateTextPanel, stateTriggerPanel, transTextPanel, transTriggerPanel, transActionPanel;
		private final JButton bnextstate = new JButton(Icons.getIcon("Forward16.gif"));
		private final JButton bprevstate = new JButton(Icons.getIcon("Back16.gif"));
		private final JButton bnexttrans = new JButton(Icons.getIcon("Forward16.gif"));
		private final JButton bprevtrans = new JButton(Icons.getIcon("Back16.gif"));
		private final JButton bselect = new JButton("Select", Icons.getIcon("Redo16.gif"));
		private final JButton bundo = new JButton("Undo", Icons.getIcon("Undo16.gif"));
		private final JMenuItem ifindall = new JMenuItem("in all DLG files");
		private final JMenuItem ifindthis = new JMenuItem("in this file only");
		private final JPanel outerpanel;
		private final JTextField tfState = new JTextField(4);
		private final JTextField tfResponse = new JTextField(4);
		private final Stack<State> lastStates = new Stack<State>();
		private final Stack<Transition> lastTransitions = new Stack<Transition>();
		private final TitledBorder bostate = new TitledBorder("State");
		private final TitledBorder botrans = new TitledBorder("Response");
		private State currentstate;
		private Transition currenttransition;
		private boolean alive = true;
		private DlgResource undoDlg;

		FlatViewer()
		{
			bfind = new ButtonPopupMenu("Find...", new JMenuItem[]{ifindall, ifindthis});
			bfind.addItemListener(this);
			bfind.setIcon(Icons.getIcon("Find16.gif"));

			dlg.addTableModelListener(this);
			bnextstate.setMargin(new Insets(bnextstate.getMargin().top, 0, bnextstate.getMargin().bottom, 0));
			bprevstate.setMargin(bnextstate.getMargin());
			bnexttrans.setMargin(bnextstate.getMargin());
			bprevtrans.setMargin(bnextstate.getMargin());
			int width = (int)tfState.getPreferredSize().getWidth();
			int height = (int)bnextstate.getPreferredSize().getHeight();
			tfState.setPreferredSize(new Dimension(width, height));
			tfResponse.setPreferredSize(new Dimension(width, height));
			tfState.setHorizontalAlignment(JTextField.CENTER);
			tfResponse.setHorizontalAlignment(JTextField.CENTER);
			tfState.addActionListener(this);
			tfResponse.addActionListener(this);
			bnextstate.addActionListener(this);
			bprevstate.addActionListener(this);
			bnexttrans.addActionListener(this);
			bprevtrans.addActionListener(this);
			bselect.addActionListener(this);
			bundo.addActionListener(this);
			bfind.addActionListener(this);
			stateTextPanel = new DlgPanel("Text", true);
			stateTriggerPanel = new DlgPanel("Trigger", false);
			transTextPanel = new DlgPanel("Text", true);
			transTriggerPanel = new DlgPanel("Trigger", false);
			transActionPanel = new DlgPanel("Action", false);

			JPanel statepanel = new JPanel();
			statepanel.setLayout(new GridLayout(2, 1, 6, 6));
			statepanel.add(stateTextPanel);
			statepanel.add(stateTriggerPanel);
			statepanel.setBorder(bostate);

			JPanel transpanel2 = new JPanel();
			transpanel2.setLayout(new GridLayout(1, 2, 6, 6));
			transpanel2.add(transTriggerPanel);
			transpanel2.add(transActionPanel);
			JPanel transpanel = new JPanel();
			transpanel.setLayout(new GridLayout(2, 1, 6, 6));
			transpanel.add(transTextPanel);
			transpanel.add(transpanel2);
			transpanel.setBorder(botrans);

			outerpanel = new JPanel();
			outerpanel.setLayout(new GridLayout(2, 1, 6, 6));
			outerpanel.add(statepanel);
			outerpanel.add(transpanel);

			JPanel bpanel = new JPanel();
			bpanel.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));
			bpanel.add(new JLabel("State:"));
			bpanel.add(tfState);
			bpanel.add(bprevstate);
			bpanel.add(bnextstate);
			bpanel.add(new JLabel(" Response:"));
			bpanel.add(tfResponse);
			bpanel.add(bprevtrans);
			bpanel.add(bnexttrans);
			bpanel.add(bselect);
			bpanel.add(bundo);
			bpanel.add(bfind);

			setLayout(new BorderLayout());
			add(outerpanel, BorderLayout.CENTER);
			add(bpanel, BorderLayout.SOUTH);
			outerpanel.setBorder(BorderFactory.createLoweredBevelBorder());

			if (dlg.getStateList().size() > 0) {
				showState(0);
				showTransition(currentstate.getFirstTrans());
			}
			else {
				bprevstate.setEnabled(false);
				bnextstate.setEnabled(false);
				bprevtrans.setEnabled(false);
				bnexttrans.setEnabled(false);
				bselect.setEnabled(false);
			}
			bundo.setEnabled(false);
		}

		public void setUndoDlg(DlgResource dlg) {
			this.undoDlg = dlg;
			bundo.setEnabled(true);
		}

		// --------------------- Begin Interface ActionListener ---------------------

		public void actionPerformed(ActionEvent event)
		{
			if (!alive) return;
			if (event.getSource() == bundo) {
				if(lastStates.empty() && (undoDlg != null)) {
					showExternState(undoDlg, -1, true);
					return;
				}
				State oldstate = lastStates.pop();
				Transition oldtrans = lastTransitions.pop();
				if (lastStates.empty() && (undoDlg == null)) {
					bundo.setEnabled(false);
				}
				//bundo.setEnabled(lastStates.size() > 0);
				if (oldstate != currentstate)
					showState(oldstate.getNumber());
				if (oldtrans != currenttransition)
					showTransition(oldtrans.getNumber());
			}
			else {
				int newstate = currentstate.getNumber();
				int newtrans = currenttransition.getNumber();
				if (event.getSource() == bnextstate)
					newstate++;
				else if (event.getSource() == bprevstate)
					newstate--;
				else if (event.getSource() == bnexttrans)
					newtrans++;
				else if (event.getSource() == bprevtrans)
					newtrans--;
				else if (event.getSource() == tfState) {
					try {
						int number = Integer.parseInt(tfState.getText());
						if (number > 0 && number <= dlg.getStateList().size())
							newstate = number - 1;
						else
							tfState.setText(String.valueOf(currentstate.getNumber() + 1));
					} catch (Exception e) {
						tfState.setText(String.valueOf(currentstate.getNumber() + 1));
					}
				}
				else if (event.getSource() == tfResponse) {
					try {
						int number = Integer.parseInt(tfResponse.getText());
						if (number > 0 && number <= currentstate.getTransCount())
							newtrans = currentstate.getFirstTrans() + number - 1;
						else
							tfResponse.setText(
									String.valueOf(currenttransition.getNumber() - currentstate.getFirstTrans() + 1));
					} catch (Exception e) {
						tfResponse.setText(
								String.valueOf(currenttransition.getNumber() - currentstate.getFirstTrans() + 1));
					}
				}
				else if (event.getSource() == bselect) {
					ResourceRef next_dlg = currenttransition.getNextDialog();
					if (dlg.getResourceEntry().toString().equalsIgnoreCase(next_dlg.toString())) {
						lastStates.push(currentstate);
						lastTransitions.push(currenttransition);
						bundo.setEnabled(true);
						newstate = currenttransition.getNextDialogState();
					}
					else {
						DlgResource newdlg = (DlgResource)ResourceFactory.getResource(
								ResourceFactory.getInstance().getResourceEntry(next_dlg.toString()));
						showExternState(newdlg, currenttransition.getNextDialogState(), false);
					}
				}
				if (alive) {
					if (newstate != currentstate.getNumber()) {
						showState(newstate);
						showTransition(dlg.getStateList().get(newstate).getFirstTrans());
					}
					else if (newtrans != currenttransition.getNumber())
						showTransition(newtrans);
				}
			}
		}

		// --------------------- End Interface ActionListener ---------------------


		// --------------------- Begin Interface ItemListener ---------------------

		public void itemStateChanged(ItemEvent event)
		{
			if (event.getSource() == bfind) {
				if (bfind.getSelectedItem() == ifindall) {
					List<ResourceEntry> files = ResourceFactory.getInstance().getResources("DLG");
					new DialogSearcher(files, getTopLevelAncestor());
				}
				else if (bfind.getSelectedItem() == ifindthis) {
					List<ResourceEntry> files = new ArrayList<ResourceEntry>();
					files.add(dlg.getResourceEntry());
					new DialogSearcher(files, getTopLevelAncestor());
				}
			}
		}

		// --------------------- End Interface ItemListener ---------------------


		// --------------------- Begin Interface TableModelListener ---------------------

		public void tableChanged(TableModelEvent e)
		{
			showState(currentstate.getNumber());
			showTransition(currenttransition.getNumber());
		}

		// --------------------- End Interface TableModelListener ---------------------

		// for quickly jump to the corresponding state while only having a StructEntry
		public void showStateWithStructEntry(StructEntry entry)
		{
			int stateNrToShow = 0;
			int transNrToShow = 0;

			// we can have states, triggers, transitions and actions
			if (entry instanceof State) {
				stateNrToShow = ((State) entry).getNumber();
				transNrToShow = ((State) entry).getFirstTrans();
			}
			else if (entry instanceof Transition) {
				int transnr = ((Transition) entry).getNumber();
				stateNrToShow = findStateForTrans(transnr);
				transNrToShow = transnr;
			}
			else if (entry instanceof StateTrigger) {
				int triggerOffset = ((StateTrigger) entry).getOffset();
				int nr = 0;
				for (StateTrigger trig : dlg.getStaTriList()) {
					if (trig.getOffset() == triggerOffset)
						break;
					nr++;
				}

				for (State state : dlg.getStateList()) {
					if (state.getTriggerIndex() == nr) {
						stateNrToShow = state.getNumber();
						transNrToShow = state.getFirstTrans();
						break;
					}
				}
			}
			else if (entry instanceof ResponseTrigger) {
				int triggerOffset = ((ResponseTrigger) entry).getOffset();
				int nr = 0;
				for (ResponseTrigger trig : dlg.getTransTriList()) {
					if (trig.getOffset() == triggerOffset)
						break;
					nr++;
				}

				for (Transition trans : dlg.getTransList()) {
					if (trans.getTriggerIndex() == nr && trans.getFlag().isFlagSet(1)) {
						transNrToShow = trans.getNumber();
						stateNrToShow = findStateForTrans(transNrToShow);
					}
				}
			}
			else if (entry instanceof Action) {
				int actionOffset = ((Action) entry).getOffset();
				int nr = 0;
				for (Action action : dlg.getActionList()) {
					if (action.getOffset() == actionOffset)
						break;
					nr++;
				}

				for (Transition trans : dlg.getTransList()) {
					if (trans.getActionIndex() == nr && trans.getFlag().isFlagSet(2)) {
						transNrToShow = trans.getNumber();
						stateNrToShow = findStateForTrans(transNrToShow);
					}
				}
			}
			else if (entry instanceof StringRef) {
				// this can happen with the dlg search
				// check all states and transitions
				int strref = ((StringRef) entry).getValue();
				boolean found = false;
				for (State state : dlg.getStateList()) {
					if (state.getResponse().getValue() == strref) {
						stateNrToShow = state.getNumber();
						transNrToShow = state.getFirstTrans();
						found = true;
					}
				}
				if (!found) {
					for (Transition trans : dlg.getTransList()) {
						if (trans.getAssociatedText().getValue() == strref) {
							transNrToShow = trans.getNumber();
							stateNrToShow = findStateForTrans(transNrToShow);
						}
					}
				}
			}

			showState(stateNrToShow);
			showTransition(transNrToShow);
		}

		private int findStateForTrans(int transnr) {
			for (State state : dlg.getStateList()) {
				if ((transnr >= state.getFirstTrans())
						&& (transnr < (state.getFirstTrans() + state.getTransCount()))) {
					return state.getNumber();
				}
			}
			// default
			return 0;
		}

		private void showState(int nr)
		{
			if (currentstate != null)
				currentstate.removeTableModelListener(this);
			currentstate = dlg.getStateList().get(nr);
			currentstate.addTableModelListener(this);
			bostate.setTitle("State " + (nr + 1) + '/' + dlg.getStateList().size());
			stateTextPanel.display(currentstate.getSuperStruct(), currentstate, nr);
			tfState.setText(String.valueOf(nr + 1));
			outerpanel.repaint();
			if (currentstate.getTriggerIndex() != 0xffffffff)
				stateTriggerPanel.display(dlg, dlg.getStaTriList().get(currentstate.getTriggerIndex()),
						currentstate.getTriggerIndex());
			else
				stateTriggerPanel.clearDisplay();
			bprevstate.setEnabled(nr > 0);
			bnextstate.setEnabled(nr + 1 < dlg.getStateList().size());
		}

		private void showTransition(int nr)
		{
			if (currenttransition != null)
				currenttransition.removeTableModelListener(this);
			currenttransition = dlg.getTransList().get(nr);
			currenttransition.addTableModelListener(this);
			botrans.setTitle("Response " + (nr - currentstate.getFirstTrans() + 1) +
					'/' + currentstate.getTransCount());
			tfResponse.setText(String.valueOf(nr - currentstate.getFirstTrans() + 1));
			outerpanel.repaint();
			transTextPanel.display(dlg, currenttransition, nr);
			if (currenttransition.getFlag().isFlagSet(1))
				transTriggerPanel.display(dlg, dlg.getTransTriList().get(currenttransition.getTriggerIndex()),
						currenttransition.getTriggerIndex());
			else
				transTriggerPanel.clearDisplay();
			if (currenttransition.getFlag().isFlagSet(2))
				transActionPanel.display(dlg, dlg.getActionList().get(currenttransition.getActionIndex()),
						currenttransition.getActionIndex());
			else
				transActionPanel.clearDisplay();
			bselect.setEnabled(!currenttransition.getFlag().isFlagSet(3));
			bprevtrans.setEnabled(nr > currentstate.getFirstTrans());
			bnexttrans.setEnabled(nr - currentstate.getFirstTrans() + 1 < currentstate.getTransCount());
		}

		private void showExternState(DlgResource newdlg, int state, boolean isUndo) {

			alive = false;
			Container window = getTopLevelAncestor();
			if (window instanceof ViewFrame && window.isVisible())
				((ViewFrame) window).setViewable(newdlg);
			else
				NearInfinity.getInstance().setViewable(newdlg);

			FlatViewer newdlg_viewer = (FlatViewer) newdlg.getDetailViewer();
			if (isUndo) {
				newdlg_viewer.alive = true;
				newdlg_viewer.repaint(); // only necessary when dlg is in extra window
			}
			else {
				newdlg_viewer.setUndoDlg(dlg);
				newdlg_viewer.showState(state);
				newdlg_viewer.showTransition(newdlg_viewer.currentstate.getFirstTrans());
			}

			// make sure the viewer tab is selected
			JTabbedPane parent = (JTabbedPane) newdlg_viewer.getParent();
			parent.getModel().setSelectedIndex(parent.indexOfComponent(newdlg_viewer));
		}

	}
	// -------------------------- INNER CLASSES --------------------------

	private final class DlgPanel extends JPanel implements ActionListener
	{
		private final JButton bView = new JButton(Icons.getIcon("Zoom16.gif"));
		private final JButton bGoto = new JButton(Icons.getIcon("RowInsertAfter16.gif"));
		private final JButton bPlay = new JButton(Icons.getIcon("Volume16.gif"));
		private final ScriptTextArea textArea = new ScriptTextArea();
		private final JLabel label = new JLabel();
		private final String title;
		private AbstractStruct struct;
		private StructEntry structEntry;
		private AbstractStruct refDlg;

		private DlgPanel(String title, boolean viewable)
		{
			this.title = title;
			bView.setMargin(new Insets(0, 0, 0, 0));
			bView.addActionListener(this);
			bGoto.setMargin(bView.getMargin());
			bGoto.addActionListener(this);
			bPlay.setMargin(bView.getMargin());
			bPlay.addActionListener(this);
			bView.setToolTipText("View/Edit");
			bGoto.setToolTipText("Select attribute");
			bPlay.setToolTipText("Open associated sound");
			textArea.setEditable(false);
			if (viewable) {
				textArea.setLineWrap(true);
				textArea.setWrapStyleWord(true);
			}
			textArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			textArea.setFont(BrowserMenuBar.getInstance().getScriptFont());
			JScrollPane scroll = new JScrollPane(textArea);

			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			setLayout(gbl);

			gbc.insets = new Insets(0, 3, 0, 0);
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.WEST;
			gbl.setConstraints(bGoto, gbc);
			add(bGoto);
			if (viewable) {
				gbl.setConstraints(bView, gbc);
				add(bView);
				gbl.setConstraints(bPlay, gbc);
				add(bPlay);
			}

			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.insets.right = 3;
			gbl.setConstraints(label, gbc);
			add(label);

			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(scroll, gbc);
			add(scroll);
		}

		private void display(AbstractStruct refdlg, State state, int number)
		{
			if (dlg == refdlg)
				label.setText(title + " (" + number + ')');
			else
				label.setText("(" + refdlg.getName() +") "+ title + " (" + number + ')');
			bView.setEnabled(true);
			bGoto.setEnabled(true);
			refDlg = refdlg;
			struct = state;
			structEntry = state;
			StringRef response = state.getResponse();
			textArea.setText(response.toString() + "\n(StrRef: " + response.getValue() + ')');
			bPlay.setEnabled(StringResource.getResource(response.getValue()) != null);
			textArea.setCaretPosition(0);
		}

		private void display(AbstractStruct refdlg, Transition trans, int number)
		{
			if (dlg == refdlg)
				label.setText(title + " (" + number + ')');
			else
				label.setText("(" + refdlg.getName() +") "+ title + " (" + number + ')');
			bView.setEnabled(true);
			bGoto.setEnabled(true);
			refDlg = refdlg;
			struct = trans;
			structEntry = trans;
			StringRef assText = trans.getAssociatedText();
			StringRef jouText = trans.getJournalEntry();
			String text = "";
			if (trans.getFlag().isFlagSet(0))
				text = assText.toString() + "\n(StrRef: " + assText.getValue() + ")\n";
			if (trans.getFlag().isFlagSet(4))
				text += "\nJournal entry:\n" + jouText.toString() + "\n(StrRef: " + jouText.getValue() + ')';
			bPlay.setEnabled(StringResource.getResource(assText.getValue()) != null);
			textArea.setText(text);
			textArea.setCaretPosition(0);
		}

		private void display(AbstractStruct refdlg, AbstractCode trigger, int number)
		{
			if (dlg == refdlg)
				label.setText(title + " (" + number + ')');
			else
				label.setText("(" + refdlg.getName() +") "+ title + " (" + number + ')');
			bView.setEnabled(false);
			bPlay.setEnabled(false);
			bGoto.setEnabled(true);
			refDlg = refdlg;
			structEntry = trigger;
			String code = Compiler.getInstance().compileDialogCode(trigger.toString(), trigger instanceof Action);
			try {
				if (Compiler.getInstance().getErrors().size() == 0) {
					if (trigger instanceof Action)
						textArea.setText(Decompiler.decompileDialogAction(code, true));
					else
						textArea.setText(Decompiler.decompileDialogTrigger(code, true));
				}
				else
					textArea.setText(trigger.toString());
			} catch (Exception e) {
				textArea.setText(trigger.toString());
			}
			textArea.setCaretPosition(0);
		}

		private void clearDisplay()
		{
			label.setText(title + " (-)");
			textArea.setText("");
			bView.setEnabled(false);
			bGoto.setEnabled(false);
			struct = null;
			structEntry = null;
		}

		public void actionPerformed(ActionEvent event)
		{
			if (event.getSource() == bView)
				new ViewFrame(getTopLevelAncestor(), struct);
			else if (event.getSource() == bGoto)
			{
				if (dlg != refDlg)
					new ViewFrame(getTopLevelAncestor(), refDlg);
				
				refDlg.getViewer().selectEntry(structEntry.getName());
			}
			else if (event.getSource() == bPlay) {
				StringRef text = null;
				if (struct instanceof State)
					text = ((State)struct).getResponse();
				else if (struct instanceof Transition)
					text = ((Transition)struct).getAssociatedText();
				if (text != null) {
					String resourceName = StringResource.getResource(text.getValue()) + ".WAV";
					if (resourceName != null) {
						ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(resourceName);
						new ViewFrame(getTopLevelAncestor(), ResourceFactory.getResource(entry));
					}
				}
			}
		}
	}
}
