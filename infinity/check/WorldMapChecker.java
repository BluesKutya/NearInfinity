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
import infinity.resource.wmp.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public final class WorldMapChecker implements Runnable, ListSelectionListener, ActionListener
{
	private ChildFrame resultFrame;
	private JButton bopen, bopennew;
	private SortableTable errorTable;


	public WorldMapChecker()
	{
		new Thread(this).start();
	}

	// --------------------- Begin Interface ActionListener ---------------------

	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource() == bopen) {
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
		List<ResourceEntry> files = ResourceFactory.getInstance().getResources("WMP", true);

		ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
				"Checking worldmap...", null, 0, files.size());

		errorTable = new SortableTable(new String[]{"Entry", "Map", "Area", "Message"},
				new Class[]{Object.class, Object.class, Object.class, Object.class},
				new int[]{100, 100, 200, 200});
		long startTime = System.currentTimeMillis();

		for (int i = 0; i < files.size(); i++) 
		{
			ResourceEntry entry = files.get(i);
			Resource mRes = ResourceFactory.getResource(entry);

			List<StructEntry> mList = ((WmpResource) mRes).getList();
			for (int j = 0; j < mList.size(); j++)
				if (mList.get(j) instanceof MapEntry)
					checkAreasConnectivity(entry, (AbstractStruct) mList.get(j));

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

	private void checkAreasConnectivity(ResourceEntry entry, AbstractStruct mapEntry)
	{
		AreaEntry aEntry;
		AreaEntry[] aCache;
		String aName;
		ResourceRef rref;

		List<StructEntry> aList = ((AbstractStruct) mapEntry).getList();
		aCache = new AreaEntry[((SectionCount) ((AbstractStruct) mapEntry).getAttribute("# areas")).getValue()];

		for (int i = 0; i < aList.size(); i++)
			if (aList.get(i) instanceof AreaEntry)
			{
				aEntry = (AreaEntry) aList.get(i);
				aName = aEntry.getName();
				aCache[Integer.decode(aName.substring(aName.lastIndexOf(" ")+1, aName.length()))] = aEntry;
			}

		for (int i = 0; i < aCache.length; i++)
		{
			checkArea(entry, mapEntry, aCache[i], "Current area");
			checkArea(entry, mapEntry, aCache[i], "Original area");

			List<StructEntry> lList = ((AbstractStruct) aCache[i]).getList();

			for (int j = 0; j < lList.size(); j++)
				if (lList.get(j) instanceof AreaLink)
				{
					AreaLink aLink = (AreaLink) lList.get(j);
					DecNumber tgtAreaNum = (DecNumber) aLink.getAttribute("Target area");
					aEntry = aCache[tgtAreaNum.getValue()];
					if (!checkAreaEntrance(aEntry.getAttribute("Current area"),
							aLink.getAttribute("Target entrance")))
					{
						errorTable.addTableItem(new AreaTableLine(entry, mapEntry, aCache[i],
								"No target entrance " + aLink.getAttribute("Target entrance") +
								" in area " + tgtAreaNum.getValue() +
								" (" + aEntry.getAttribute("Current area").toString() + ")"));
					}

					int backTime = getBackTime(aEntry, i);

					if (backTime == -1)
					{
						errorTable.addTableItem(new AreaTableLine(entry, mapEntry, aCache[i],
								"No backward way from area " + tgtAreaNum.getValue() +
								" (" + aEntry.getAttribute("Current area").toString() + ")"));
					}
					else if (backTime != ((DecNumber) aLink.getAttribute("Distance scale")).getValue())
					{
						errorTable.addTableItem(new AreaTableLine(entry, mapEntry, aCache[i],
								"Different time ("+ ((DecNumber) aLink.getAttribute("Distance scale")).getValue() +
								"><" + backTime + ") between area " + tgtAreaNum.getValue() +
								" (" + aEntry.getAttribute("Current area").toString() + ")"));
					}

					checkArea(entry, mapEntry, aLink, "Random encounter area 1");
					checkArea(entry, mapEntry, aLink, "Random encounter area 2");
					checkArea(entry, mapEntry, aLink, "Random encounter area 3");
					checkArea(entry, mapEntry, aLink, "Random encounter area 4");
					checkArea(entry, mapEntry, aLink, "Random encounter area 5");
				}
		}
		
	}
	
	private void checkArea(ResourceEntry entry, AbstractStruct mapEntry, 
			AbstractStruct aEntry, String attrName)
	{
		ResourceRef rref;

		rref = (ResourceRef) aEntry.getAttribute(attrName);
		if (!checkExistanceArea(rref.getResourceName()))
			errorTable.addTableItem(new AreaTableLine(entry, mapEntry, aEntry,
					attrName + " not exists " + rref.getResourceName()));
	}

	private int getBackTime(AreaEntry start, int finish)
	{
		List<StructEntry> lList = start.getList();

		for (int i = 0; i < lList.size(); i++)
			if (lList.get(i) instanceof AreaLink)
			{
				AreaLink aLink = (AreaLink) lList.get(i);
				if(((DecNumber) aLink.getAttribute("Target area")).getValue() == finish)
					return ((DecNumber) aLink.getAttribute("Distance scale")).getValue();
			}
		
		return -1;
	}

	private boolean checkAreaEntrance(StructEntry areaEntry, StructEntry entranceEntry)
	{
		boolean isFound = false;
		String entName;
		AreResource dArea;
		
		entName  = entranceEntry.toString();
		if (entName.isEmpty())
			return true;
		
		dArea = (AreResource) ResourceFactory.getResource(ResourceFactory.getInstance().
				getResourceEntry(((ResourceRef) areaEntry).getResourceName()));

		List<StructEntry> list = dArea.getList();
		for (int i = 0; i < list.size(); i++)
			if (list.get(i) instanceof Entrance &&
					entName.equalsIgnoreCase(((Entrance) list.get(i)).getAttribute("Name").toString()))
				isFound = true;

		return isFound;
	}

	private boolean checkExistanceArea(String areaName)
	{
		return (areaName.equalsIgnoreCase("None.ARE") ||
				ResourceFactory.getInstance().resourceExists(areaName));
	}

	// --------------------- End Interface Runnable ---------------------


	// -------------------------- INNER CLASSES --------------------------

	private static final class AreaTableLine implements TableItem
	{
		private final ResourceEntry resourceEntry;
	    private final AbstractStruct mapEntry, areaEntry;
		private final String error;

		private AreaTableLine(ResourceEntry resourceEntry, AbstractStruct mapEntry,
				AbstractStruct areaEntry, String error)
		{
			this.resourceEntry = resourceEntry;
		    this.mapEntry = mapEntry;
		    this.areaEntry = areaEntry;
			this.error = error;
		}

		public Object getObjectAt(int columnIndex)
		{
			if (columnIndex == 0)
				return resourceEntry;
	        else if (columnIndex == 1)
		        return mapEntry.getName();
			else if (columnIndex == 2)
				return areaEntry.getName() + " (" + areaEntry.getAttribute("Current area") + ")";
			else if (columnIndex == 3)
				return error;
			return resourceEntry.getSearchString();
		}
	}
}

