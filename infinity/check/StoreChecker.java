// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2012
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.datatype.*;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;
import infinity.resource.other.PlainTextResource;
import infinity.resource.sto.*;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

public final class StoreChecker implements Runnable, ListSelectionListener,	ActionListener 
{
	private ChildFrame resultFrame;
	private JButton bopen, bopennew;
	private SortableTable errorTable;
	String cureSpells[][];
	float priceDiscount = 1;

	public StoreChecker() {
		new Thread(this).start();
	}

	public void actionPerformed(ActionEvent event) {
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
				((AbstractStruct)resource).getViewer().selectEntry(((StructEntry) errorTable.getValueAt(row, 1)).getOffset());
			}
		}
	}

	public void valueChanged(ListSelectionEvent event) {
		bopen.setEnabled(true);
		bopennew.setEnabled(true);
	}

	public void run() {
		WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
		blocker.setBlocked(true);

		List<ResourceEntry> files = ResourceFactory.getInstance().getResources("STO", true);
		ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
				"Checking stores...", null, 0, files.size());

		errorTable = new SortableTable(new String[]{"Store", "Attribute", "Message"},
				new Class[]{Object.class, Object.class, Object.class},
				new int[]{100, 100, 200});

		ResourceEntry spellRef = ResourceFactory.getInstance().getResourceEntry("SPELDESC.2DA");
		if (spellRef != null)
			cureSpells = ((PlainTextResource) ResourceFactory.getResource(spellRef)).extract2DA();

		priceDiscount = calculate_discount();

		for (int i = 0; i < files.size(); i++) 
		{
			ResourceEntry entry = files.get(i);
			Resource storeRes = ResourceFactory.getResource(entry);

			check_store_rumors(entry, storeRes);

			check_store_flag(entry, storeRes);

			if (((AbstractStruct) storeRes).getAttribute("Version").toString().equalsIgnoreCase("V1.0")
					&& ((Bitmap) ((AbstractStruct) storeRes).getAttribute("Type")).getValue() != 5
					|| ((AbstractStruct) storeRes).getAttribute("Version").toString().equalsIgnoreCase("V1.1")
					|| ((AbstractStruct) storeRes).getAttribute("Version").toString().equalsIgnoreCase("V9.0")
					&& ((Bitmap) ((AbstractStruct) storeRes).getAttribute("Type")).getValue() != 4)
			{
				check_store_state(entry, storeRes);
			}
			else if (!ResourceFactory.getInstance().resourceExists(entry.getResourceName().split("\\.")[0] + ".ITM"))
				errorTable.addTableItem(new StoreTableLine(entry, ((AbstractStruct) storeRes).getAttribute("Type"),
						"Container: No ITM resource"));

			check_store_item(entry, storeRes);

//			if (((SectionCount) ((AbstractStruct) storeRes).getAttribute("# cures for sale")).getValue() != 0)
			if (((Flag) ((AbstractStruct) storeRes).getAttribute("Flags")).isFlagSet(5))
				check_store_cures(entry, storeRes);

//			if (((SectionCount) ((AbstractStruct) storeRes).getAttribute("# drinks for sale")).getValue() != 0)
			if (((Flag) ((AbstractStruct) storeRes).getAttribute("Flags")).isFlagSet(6))
				check_store_drinks(entry, storeRes);

			progress.setProgress(i + 1);
			if (progress.isCanceled()) {
				JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation canceled",
						"Info", JOptionPane.INFORMATION_MESSAGE);
				blocker.setBlocked(false);
				return;
			}
		}

		if (errorTable.getRowCount() == 0)
			JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors found",
					"Info", JOptionPane.INFORMATION_MESSAGE);
		else
		{
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
							((AbstractStruct)resource).getViewer().selectEntry(((StructEntry) errorTable.getValueAt(row, 1)).getOffset());
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

	private float calculate_discount()
	{
		int minchrmod = 999999, minrepmod = 999999;

		try
		{
			ResourceEntry chrmodRef = ResourceFactory.getInstance().getResourceEntry("CHRMODST.2DA");

			if (chrmodRef != null)
			{
				String chrmodst[][] = ((PlainTextResource) ResourceFactory.getResource(chrmodRef)).extract2DA();

				for (int i = 1; i < chrmodst[1].length; i++)
					if (Integer.parseInt(chrmodst[1][i]) < minchrmod)
						minchrmod = Integer.parseInt(chrmodst[1][i]);

				ResourceEntry repmodRef = ResourceFactory.getInstance().getResourceEntry("REPMODST.2DA");

				if (repmodRef != null)
				{
					String repmodst[][] = ((PlainTextResource) ResourceFactory.getResource(repmodRef)).extract2DA();

					for (int i = 1; i < repmodst[1].length; i++)
						if (Integer.parseInt(repmodst[1][i]) < minrepmod)
							minrepmod = Integer.parseInt(repmodst[1][i]);

					if(minchrmod != 999999 && minrepmod != 999999)
						return (float) ((minrepmod + minchrmod)/100.0);
				}
			}
		}
		catch(Exception e)
		{
			return 1;
		}
		return 1;
	}

	private void check_store_rumors(ResourceEntry entry, Resource storeRes)
	{
		if ( ((Flag) ((AbstractStruct) storeRes).getAttribute("Flags")).isFlagSet(4) )
		{
			ResourceRef rref = (ResourceRef) ((AbstractStruct) storeRes).getAttribute("Rumors (donations)");
			if(rref == null || rref.getResourceName().equalsIgnoreCase("None.DLG"))
				errorTable.addTableItem(new StoreTableLine(entry, rref, "No valid rumors DLG resource"));
			else if (!ResourceFactory.getInstance().resourceExists(rref.getResourceName()))
				errorTable.addTableItem(new StoreTableLine(entry, rref, "Invalid rumors DLG resource"));
		}

		if ( ((Flag) ((AbstractStruct) storeRes).getAttribute("Flags")).isFlagSet(6) )
		{
			ResourceRef rref = (ResourceRef) ((AbstractStruct) storeRes).getAttribute("Rumors (drinks)");
			if(rref == null || rref.getResourceName().equalsIgnoreCase("None.DLG"))
				errorTable.addTableItem(new StoreTableLine(entry, rref, "No valid rumors DLG resource"));
			else if (!ResourceFactory.getInstance().resourceExists(rref.getResourceName()))
				errorTable.addTableItem(new StoreTableLine(entry, rref, "Invalid rumors DLG resource"));
		}
	}

	private void check_store_flag(ResourceEntry entry, Resource storeRes)
	{
		Flag storeFlag = (Flag) ((AbstractStruct) storeRes).getAttribute("Flags");

		switch(((Bitmap) ((AbstractStruct) storeRes).getAttribute("Type")).getValue())
		{
		case 0: //Store
			if (!storeFlag.isFlagSet(0))
				errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
						"Store: but selling flag not set"));
			else if (((DecNumber) ((AbstractStruct) storeRes).getAttribute("Sell markup")).getValue() == 0)
				errorTable.addTableItem(new StoreTableLine(entry, ((AbstractStruct) storeRes).getAttribute("Sell markup"),
						"Store: no selling price rate set."));

			if (!storeFlag.isFlagSet(1))
				errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
						"Store: but purchasing flag not set"));
			else if (((DecNumber) ((AbstractStruct) storeRes).getAttribute("Buy markup")).getValue() == 0)
				errorTable.addTableItem(new StoreTableLine(entry, ((AbstractStruct) storeRes).getAttribute("Buy markup"),
						"Store: no purchasing price rate set."));

			if (storeFlag.isFlagSet(0) && storeFlag.isFlagSet(1) &&
					((DecNumber) ((AbstractStruct) storeRes).getAttribute("Sell markup")).getValue() * priceDiscount <
					((DecNumber) ((AbstractStruct) storeRes).getAttribute("Buy markup")).getValue())
			{
				errorTable.addTableItem(new StoreTableLine(entry, ((AbstractStruct) storeRes).getAttribute("Sell markup"),	
						"Store sells cheaper than it buys"));
			}
			break;
		case 1: //Tavern
			if (!storeFlag.isFlagSet(6))
				errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
						"Tavern: but drink flag not set"));
			break;
		case 2: //Inn
			if (((AbstractStruct) storeRes).getAttribute("Available rooms").toString().equalsIgnoreCase("( No rooms available )"))
				errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
						"Inn: but no rooms available"));
			break;
		case 3: //Temple
			if (!storeFlag.isFlagSet(4))
				errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
						"Temple: but donate flag not set"));

			if (!storeFlag.isFlagSet(5))
				errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
						"Temple: but cures flag not set"));
			break;
		case 4: //Container IWD2
		case 5: //Container
			if (!storeFlag.isFlagSet(0))
				errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
						"Container: but selling flag not set"));

			if (!storeFlag.isFlagSet(1))
				errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
						"Container: but purchasing flag not set"));

			if (((DecNumber) ((AbstractStruct) storeRes).getAttribute("Depreciation rate")).getValue() != 0 ||
					((DecNumber) ((AbstractStruct) storeRes).getAttribute("Sell markup")).getValue() != 0 ||
					((DecNumber) ((AbstractStruct) storeRes).getAttribute("Buy markup")).getValue() != 0 )
			{
				errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
						"Container has selling/buying/rate price set."));
			}
			break;
		}
	}

	private void check_store_state(ResourceEntry entry, Resource storeRes)
	{
		Flag storeFlag = (Flag) ((AbstractStruct) storeRes).getAttribute("Flags");
		SectionCount itemCounter;
		// Sale
		itemCounter = (SectionCount) ((AbstractStruct) storeRes).getAttribute("# items for sale");
		if ( storeFlag.isFlagSet(0) && itemCounter.getValue() == 0)
			errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
					"Selling flag set, but no stock."));

		if ( !storeFlag.isFlagSet(0) && itemCounter.getValue() != 0)
			errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
					"Selling flag not set, but there is stock."));
		// Buy
		itemCounter = (SectionCount) ((AbstractStruct) storeRes).getAttribute("# items purchased");
		if ( storeFlag.isFlagSet(1) && itemCounter.getValue() == 0)
			errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
					"Purchasing flag set, but purchases list is empty."));

		if ( !storeFlag.isFlagSet(1) && itemCounter.getValue() != 0)
			errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
					"Purchasing flag not set, but purchases list is full."));
		// Cures
		itemCounter = (SectionCount) ((AbstractStruct) storeRes).getAttribute("# cures for sale");
		if ( storeFlag.isFlagSet(5) && itemCounter.getValue() == 0)
			errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
					"Cure flag set, but no cures."));

		if ( !storeFlag.isFlagSet(5) && itemCounter.getValue() != 0)
			errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
					"Cure flag not set, but there are cures."));
		// Drinks		
		itemCounter = (SectionCount) ((AbstractStruct) storeRes).getAttribute("# drinks for sale");
		if ( storeFlag.isFlagSet(6) && itemCounter.getValue() == 0)
			errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
					"Drink flag set, but no drinks."));

		if ( !storeFlag.isFlagSet(6) && itemCounter.getValue() != 0)
			errorTable.addTableItem(new StoreTableLine(entry, storeFlag,
					"Drink flag not set, but there are drinks."));
	}

	private void check_store_item(ResourceEntry entry, Resource storeRes)
	{
		List<StructEntry> list = ((StoResource) storeRes).getList();
		for (int i = 0; i < list.size(); i++)
			if (list.get(i) instanceof ItemSale || list.get(i) instanceof ItemSale11)
			{
				ResourceRef itemRes = (ResourceRef) ((AbstractStruct) list.get(i)).getAttribute("Item");
				if (!ResourceFactory.getInstance().resourceExists(itemRes.getResourceName()))
					errorTable.addTableItem(new StoreTableLine(entry, list.get(i),
							"Non existent item for sale: " + itemRes.getResourceName()));

				StructEntry wear = ((AbstractStruct) list.get(i)).getAttribute("Wear");
				if (((DecNumber) wear).getValue() != 0)
					errorTable.addTableItem(new StoreTableLine(entry, list.get(i), 
							"Wear is: " + ((DecNumber) wear).getValue()));

				for (int j = 1; j < 8*((Flag) ((AbstractStruct) list.get(i)).getAttribute("Flags")).getSize(); j++)
					if (((Flag) ((AbstractStruct) list.get(i)).getAttribute("Flags")).isFlagSet(j))
					{
						errorTable.addTableItem(new StoreTableLine(entry, list.get(i), 
								"Item flag is: " + ((AbstractStruct) list.get(i)).getAttribute("Flags").toString()));
						break;
					}

				int stock_num = ((DecNumber) ((AbstractStruct) list.get(i)).getAttribute("# in stock")).getValue();

				switch(((Bitmap) ((AbstractStruct) list.get(i)).getAttribute("Infinite supply?")).getValue())
				{
				case 0:
					if (stock_num == 0)
						errorTable.addTableItem(new StoreTableLine(entry, list.get(i), "Zero stock for stored item"));
					break;
				case 1:
					if (stock_num != 0)
						errorTable.addTableItem(new StoreTableLine(entry, list.get(i), "Infinite flag set for stocked item"));
					break;
				}
			}
	}

	private void check_store_cures(ResourceEntry entry, Resource storeRes)
	{
		List<StructEntry> list = ((StoResource) storeRes).getList();
		for (int i = 0; i < list.size(); i++)
			if (list.get(i) instanceof Cure)
			{
				ResourceRef cureRes = (ResourceRef) ((AbstractStruct) list.get(i)).getAttribute("Spell");
				if (!ResourceFactory.getInstance().resourceExists(cureRes.getResourceName()))
					errorTable.addTableItem(new StoreTableLine(entry, list.get(i), "Invalid cure spell"));

				Boolean found = false;
				for (int j = 0; j < cureSpells.length; j++)
					if (cureSpells[j][0].equalsIgnoreCase(cureRes.getResourceName().split("\\.")[0]))
						found = true;

				if (!found)
					errorTable.addTableItem(new StoreTableLine(entry, list.get(i), "Cure spell has no entry in SPELDESC.2DA"));
			}
	}

	private void check_store_drinks(ResourceEntry entry, Resource storeRes)
	{
		List<StructEntry> list = ((StoResource) storeRes).getList();
		for (int i = 0; i < list.size(); i++)
			if (list.get(i) instanceof Drink)
			{
				ResourceRef rumorRes = (ResourceRef) ((AbstractStruct) list.get(i)).getAttribute("Rumor");
				if (rumorRes != null && !rumorRes.getResourceName().equalsIgnoreCase("None.DLG") &&
						!ResourceFactory.getInstance().resourceExists(rumorRes.getResourceName()))
					errorTable.addTableItem(new StoreTableLine(entry, list.get(i), "Invalid rumors DLG resource"));
			}
	}

	// -------------------------- INNER CLASSES --------------------------

	private static final class StoreTableLine implements TableItem
	{
		private final ResourceEntry resourceEntry;
		private final StructEntry structEntry;
		private final String message;

		private StoreTableLine(ResourceEntry resourceEntry, StructEntry structEntry, String message)
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
				return structEntry;
			else if (columnIndex == 2)
				return message;
			return resourceEntry.getSearchString();
		}
	}
}
