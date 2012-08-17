// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2012
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.datatype.*;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.are.*;
import infinity.resource.are.Container;
import infinity.resource.cre.CreResource;
import infinity.resource.graphics.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.List;

public final class AreaChecker implements Runnable, ListSelectionListener, ActionListener
{
	private static final String CHECKTYPES[] = {"Actors expiry time", "Actors position",
		"Contained items", "Connectivity of areas"};
	private final ChildFrame selectframe = new ChildFrame("Area check", true);
	private final JButton bstart = new JButton("Check", Icons.getIcon("Find16.gif"));
	private final JButton bcancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
	private final JCheckBox[] typeButtons;
	private ChildFrame resultFrame;
	private JButton bopen, bopennew;
	private SortableTable errorTable;
	private HashSet<String> exclude = new HashSet<String>();
	private static final Boolean Passable[] = {false, true, true, true, true, true, true, true,
			  								   false, true, false, true, false, false, true, true}; 


	public AreaChecker(Component parent)
	{
		typeButtons = new JCheckBox[CHECKTYPES.length];
		JPanel boxPanel = new JPanel(new GridLayout(0, 1));
		for (int i = 0; i < typeButtons.length; i++) {
			typeButtons[i] = new JCheckBox(CHECKTYPES[i], true);
			boxPanel.add(typeButtons[i]);
		}
		bstart.setMnemonic('s');
		bcancel.setMnemonic('c');
		bstart.addActionListener(this);
		bcancel.addActionListener(this);
		selectframe.getRootPane().setDefaultButton(bstart);
		selectframe.setIconImage(Icons.getIcon("Find16.gif").getImage());
		boxPanel.setBorder(BorderFactory.createTitledBorder("Select test to check:"));

		JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bpanel.add(bstart);
		bpanel.add(bcancel);

		JPanel mainpanel = new JPanel(new BorderLayout());
		mainpanel.add(boxPanel, BorderLayout.CENTER);
		mainpanel.add(bpanel, BorderLayout.SOUTH);
		mainpanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

		JPanel pane = (JPanel)selectframe.getContentPane();
		pane.setLayout(new BorderLayout());
		pane.add(mainpanel, BorderLayout.CENTER);

		selectframe.pack();
		Center.center(selectframe, parent.getBounds());
		selectframe.setVisible(true);
	}

	// --------------------- Begin Interface ActionListener ---------------------

	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource() == bstart) {
			selectframe.setVisible(false);
			for (int i = 0; i < typeButtons.length; i++)
				if (typeButtons[i].isSelected()) {
					new Thread(this).start();
					return;
				}
		}
		else if (event.getSource() == bcancel)
			selectframe.setVisible(false);
		else if (event.getSource() == bopen) {
			int row = errorTable.getSelectedRow();
			if (row != -1) {
				ResourceEntry resourceEntry = (ResourceEntry)errorTable.getValueAt(row, 0);
				NearInfinity.getInstance().showResourceEntry(resourceEntry);
			}
		}
		else if (event.getSource() == bopennew) {
			int row = errorTable.getSelectedRow();
			if (row != -1) {
				ResourceEntry resourceEntry = (ResourceEntry)errorTable.getValueAt(row, 0);
				Resource resource = ResourceFactory.getResource(resourceEntry);
				new ViewFrame(resultFrame, resource);
				((AbstractStruct)resource).getViewer().selectEntry((String)errorTable.getValueAt(row, 1));
			}
		}
	}

	// --------------------- End Interface ActionListener ---------------------


	// --------------------- Begin Interface ListSelectionListener ---------------------

	public void valueChanged(ListSelectionEvent event)
	{
		bopen.setEnabled(true);
		bopennew.setEnabled(true);
	}

	// --------------------- End Interface ListSelectionListener ---------------------


	// --------------------- Begin Interface Runnable ---------------------

	public void run()
	{
		WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
		blocker.setBlocked(true);
		BmpResource searchMap = null;
		List<ResourceEntry> files = ResourceFactory.getInstance().getResources("ARE");
		ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
				"Checking areas...", null, 0, files.size());
		errorTable = new SortableTable(new String[]{"Area", "Object", "Message"},
				new Class[]{Object.class, Object.class, Object.class},
				new int[]{100, 200, 200});
		long startTime = System.currentTimeMillis();

		exclude.clear();
		exclude.add("BIRD");
		exclude.add("EAGLE");
		exclude.add("SEAGULL");
		exclude.add("VULTURE");
		exclude.add("DOOM_GUARD");
		exclude.add("DOOM_GUARD_LARGER");
		exclude.add("BAT_INSIDE");
		exclude.add("BAT_OUTSIDE");

//		exclude.add("CAT");
//		exclude.add("MOOSE");
//		exclude.add("RABBIT");
		exclude.add("SQUIRREL");
		exclude.add("RAT");
//		exclude.add("STATIC_PEASANT_MAN_MATTE");
//		exclude.add("STATIC_PEASANT_WOMAN_MATTE");

		for (int i = 0; i < files.size(); i++) 
		{
			try
			{
				ResourceEntry entry = files.get(i);
				Resource area = ResourceFactory.getResource(entry);

				if (typeButtons[3].isSelected())
					checkAreasConnectivity(entry, area);

				if (typeButtons[1].isSelected())
				{
					String[] name = ((AreResource) area).getAttribute("WED resource").toString().split("\\.");
					ResourceEntry searchEntry = ResourceFactory.getInstance().getResourceEntry(name[0]+"SR.BMP");

					if (searchEntry == null)
					{
						searchMap = null;
						errorTable.addTableItem(new AreaTableLine(entry,
								((AreResource) area).getAttribute("WED resource"), 
								"Area don't have search map"));
					}
					else
					{
						searchMap = (BmpResource)ResourceFactory.getResource(searchEntry);
					}
				}

				List<StructEntry> list = ((AreResource) area).getList();
				for (int j = 0; j < list.size(); j++)
				{
					if (list.get(j) instanceof Actor)
					{
						Actor actor = (Actor) list.get(j);
						StructEntry time = actor.getAttribute("Expiry time");
						if (typeButtons[0].isSelected() && ((DecNumber) time).getValue() != -1)
							errorTable.addTableItem(new AreaTableLine(entry, actor, 
									"Actor expiry time is: " + ((DecNumber) time).getValue()));

						if (searchMap != null)
							checkActorPosition(entry, actor, searchMap);
					}

					if (typeButtons[2].isSelected() && list.get(j) instanceof Container)
						checkContainedItem(entry, list.get(j));
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			progress.setProgress(i + 1);
			if (progress.isCanceled()) {
				JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation canceled",
						"Info", JOptionPane.INFORMATION_MESSAGE);
				blocker.setBlocked(false);
				return;
			}
		}
		System.out.println("Check took " + (System.currentTimeMillis() - startTime) + "ms");
		if (errorTable.getRowCount() == 0)
			JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors found",
					"Info", JOptionPane.INFORMATION_MESSAGE);
		else {
			errorTable.tableComplete();
			resultFrame = new ChildFrame("Result", true);
			resultFrame.setIconImage(Icons.getIcon("Find16.gif").getImage());
			bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
			bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
			bopen.setMnemonic('o');
			bopennew.setMnemonic('n');
			resultFrame.getRootPane().setDefaultButton(bopennew);
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			panel.add(bopen);
			panel.add(bopennew);
			JLabel count = new JLabel(errorTable.getRowCount() + " errors found", JLabel.CENTER);
			count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
			JScrollPane scrollTable = new JScrollPane(errorTable);
			scrollTable.getViewport().setBackground(errorTable.getBackground());
			JPanel pane = (JPanel)resultFrame.getContentPane();
			pane.setLayout(new BorderLayout(0, 3));
			pane.add(count, BorderLayout.NORTH);
			pane.add(scrollTable, BorderLayout.CENTER);
			pane.add(panel, BorderLayout.SOUTH);
			bopen.setEnabled(false);
			bopennew.setEnabled(false);
			errorTable.setFont(BrowserMenuBar.getInstance().getScriptFont());
			errorTable.addMouseListener(new MouseAdapter()
			{
				public void mouseReleased(MouseEvent event)
				{
					if (event.getClickCount() == 2) {
						int row = errorTable.getSelectedRow();
						if (row != -1) {
							ResourceEntry resourceEntry = (ResourceEntry)errorTable.getValueAt(row, 0);
							Resource resource = ResourceFactory.getResource(resourceEntry);
							new ViewFrame(resultFrame, resource);
							((AbstractStruct)resource).getViewer().selectEntry((String)errorTable.getValueAt(row, 1));
						}
					}
				}
			});
			bopen.addActionListener(this);
			bopennew.addActionListener(this);
			pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			errorTable.getSelectionModel().addListSelectionListener(this);
			resultFrame.pack();
			Center.center(resultFrame, NearInfinity.getInstance().getBounds());
			resultFrame.setVisible(true);
		}
		blocker.setBlocked(false);
	}

	private void checkAreasConnectivity(ResourceEntry entry, Resource area)
	{
		ResourceRef darearef, rref;
		ITEPoint ttriger;
		
		checkNearestArea(entry, "Area north");
		checkNearestArea(entry, "Area east");
		checkNearestArea(entry, "Area south");
		checkNearestArea(entry, "Area west");
		
		List<StructEntry> list = ((AreResource) area).getList();
		for (int i = 0; i < list.size(); i++)
			if (list.get(i) instanceof ITEPoint)
			{
				ttriger = (ITEPoint) list.get(i);
				if (((Bitmap) ttriger.getAttribute("Type")).getValue() == 2)
				{
					darearef = (ResourceRef) ttriger.getAttribute("Destination area");
					if (!ResourceFactory.getInstance().resourceExists(darearef.getResourceName()))
					{
						rref  = (ResourceRef) ttriger.getAttribute("Script");
						if (!ResourceFactory.getInstance().resourceExists(rref.getResourceName()))
							errorTable.addTableItem(new AreaTableLine(entry, ttriger,
									"No target area " + darearef.getResourceName()));
					}
					else
						checkAreaEntrance(entry, ttriger, darearef);
				}
			}
	}

	private void checkAreaEntrance(ResourceEntry entry, ITEPoint tTriger, ResourceRef dAreaRef)
	{
		boolean isFound = false;
		TextString entName;
		AreResource dArea;
		
		entName  = (TextString) tTriger.getAttribute("Entrance name");
		dArea = (AreResource) ResourceFactory.getResource(ResourceFactory.getInstance().
				getResourceEntry(dAreaRef.getResourceName()));

		List<StructEntry> list = dArea.getList();
		for (int i = 0; i < list.size(); i++)
			if (list.get(i) instanceof Entrance &&
					entName.toString().equalsIgnoreCase(((Entrance) list.get(i)).getAttribute("Name").toString()))
				isFound = true;

		if (!isFound)
			errorTable.addTableItem(new AreaTableLine(entry, tTriger,
					"No target entrance " + entName.toString() + " in area " + dAreaRef.getResourceName()));
	}

	private void checkNearestArea(ResourceEntry entry, String direction)
	{
		ResourceRef rref;
		
		rref = (ResourceRef) ((AreResource) ResourceFactory.getResource(entry)).getAttribute(direction);
		if (rref != null && !rref.getResourceName().equalsIgnoreCase("None.ARE") &&
				!ResourceFactory.getInstance().resourceExists(rref.getResourceName()))
			errorTable.addTableItem(new AreaTableLine(entry, rref, "No target area " + rref.getResourceName()));
	}

	private void checkActorPosition(ResourceEntry entry, Actor actor, BmpResource searchMap)
	{
		String anim = actor.getAttribute("Animation").toString();
		ResourceEntry creEntry;
		CreResource creature = null;
		
		if (actor.getAttribute("Character") instanceof ResourceRef &&
				actor.getAttribute("Character").toString() != "None")
		{
			creEntry = ResourceFactory.getInstance().getResourceEntry(
					((ResourceRef) actor.getAttribute("Character")).getResourceName());

			creature = (CreResource) ResourceFactory.getResource(creEntry);
		}
		else if (actor.getAttribute("CRE file") != null)
			creature = (CreResource) actor.getAttribute("CRE file");
		
		if (creature == null || ((IdsFlag) creature.getAttribute("Status")).isFlagSet(0) || 
				((IdsFlag) creature.getAttribute("Status")).isFlagSet(11))
			return;
		
		if (!exclude.contains(anim.substring(0, anim.indexOf(" "))))
		{
			int x = ((DecNumber) actor.getAttribute("Position: X")).getValue()/16;
			int y = ((DecNumber) actor.getAttribute("Position: Y")).getValue()/12;
			if (!Passable[searchMap.getPalette().getIndex(searchMap.getImage().getRGB(x, y)&0xFFFFFF)])
				errorTable.addTableItem(new AreaTableLine(entry, actor, "Impassable actor position (" +
						creature.getAttribute("Name").toString() + ")"));
	
		}
	}

	private void checkContainedItem(ResourceEntry entry, StructEntry container)
	{
		List<StructEntry> cont_list = ((AbstractStruct) container).getList();
		for (int i = 0; i < cont_list.size(); i++)
			if (cont_list.get(i) instanceof Item)
			{
				Item item = (Item) cont_list.get(i);
				StructEntry wear = item.getAttribute("Wear");
				if (((DecNumber) wear).getValue() != 0)
				{
					errorTable.addTableItem(new AreaTableLine(entry, item, 
							"Wear is: " + ((DecNumber) wear).getValue()));
				}

				for (int j = 1; j < 8*((Flag) item.getAttribute("Flags")).getSize(); j++)
					if (((Flag) item.getAttribute("Flags")).isFlagSet(j))
					{
						errorTable.addTableItem(new AreaTableLine(entry, container, 
								"Item flag is: " + item.getAttribute("Flags").toString()));
						break;
					}
			}
	}
	// --------------------- End Interface Runnable ---------------------


	// -------------------------- INNER CLASSES --------------------------

	private static final class AreaTableLine implements TableItem
	{
		private final ResourceEntry resourceEntry;
	    private final StructEntry structEntry;
		private final String message;

		private AreaTableLine(ResourceEntry resourceEntry, StructEntry structEntry, String message)
		{
			this.resourceEntry = resourceEntry;
		    this.structEntry = structEntry;
			this.message = message;
		}

		public Object getObjectAt(int columnIndex)
		{
			if (columnIndex == 0)
				return resourceEntry;
	        else if (columnIndex == 1)
		        return structEntry.getName();
			else if (columnIndex == 2)
				return message;
			return resourceEntry.getSearchString();
		}
	}
}

