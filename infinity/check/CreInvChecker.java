// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.ResourceRef;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.cre.CreResource;
import infinity.resource.cre.Item;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public final class CreInvChecker implements Runnable, ActionListener, ListSelectionListener
{
  private static final String CHECKTYPES[] = {"Items Not in Inventory", 
	  "Item attribute"};
  private final ChildFrame selectframe = new ChildFrame("Creature check", true);
  private final JButton bstart = new JButton("Check", Icons.getIcon("Find16.gif"));
  private final JButton bcancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
  private final List<StructEntry> items = new ArrayList<StructEntry>();
  private final List<StructEntry> slots = new ArrayList<StructEntry>();
  private final JCheckBox[] typeButtons;
  private ChildFrame resultFrame;
  private JButton bopen, bopennew;
  private SortableTable table;

  public CreInvChecker(Component parent)
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
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
        ((AbstractStruct)NearInfinity.getInstance().getViewable()).getViewer().selectEntry(
                ((Item)table.getValueAt(row, 2)).getName());
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        Resource resource = ResourceFactory.getResource(resourceEntry);
        new ViewFrame(resultFrame, resource);
        ((AbstractStruct)resource).getViewer().selectEntry(((Item)table.getValueAt(row, 2)).getName());
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
    List<ResourceEntry> creFiles = ResourceFactory.getInstance().getResources("CRE");
    creFiles.addAll(ResourceFactory.getInstance().getResources("CHR"));
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
                                                   "Checking inventories...", null, 0, creFiles.size());
    table = new SortableTable(new String[]{"File", "Name", "Item", "Message"},
                              new Class[]{Object.class, Object.class, Object.class, Object.class},
                              new int[]{100, 100, 200, 200});
    for (int i = 0; i < creFiles.size(); i++) {
      ResourceEntry entry = creFiles.get(i);
      try {
    	if (typeButtons[0].isSelected())
    	  checkCreatureInventory((CreResource) ResourceFactory.getResource(entry));
    	if (typeButtons[1].isSelected())
      	  checkItemAttribute((CreResource) ResourceFactory.getResource(entry));
      } catch (Exception e) {
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

    if (table.getRowCount() == 0)
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No hits found",
                                    "Info", JOptionPane.INFORMATION_MESSAGE);
    else {
      resultFrame = new ChildFrame("Result of CRE inventory check", true);
      resultFrame.setIconImage(Icons.getIcon("Refresh16.gif").getImage());
      bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
      bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
      JLabel count = new JLabel(table.getRowCount() + " hit(s) found", JLabel.CENTER);
      count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
      bopen.setMnemonic('o');
      bopennew.setMnemonic('n');
      resultFrame.getRootPane().setDefaultButton(bopennew);
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      panel.add(bopen);
      panel.add(bopennew);
      JScrollPane scrollTable = new JScrollPane(table);
      scrollTable.getViewport().setBackground(table.getBackground());
      JPanel pane = (JPanel)resultFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(count, BorderLayout.NORTH);
      pane.add(scrollTable, BorderLayout.CENTER);
      pane.add(panel, BorderLayout.SOUTH);
      bopen.setEnabled(false);
      bopennew.setEnabled(false);
      table.setFont(BrowserMenuBar.getInstance().getScriptFont());
      table.getSelectionModel().addListSelectionListener(this);
      table.addMouseListener(new MouseAdapter()
      {
        public void mouseReleased(MouseEvent event)
        {
          if (event.getClickCount() == 2) {
            int row = table.getSelectedRow();
            if (row != -1) {
              ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
              Resource resource = ResourceFactory.getResource(resourceEntry);
              new ViewFrame(resultFrame, resource);
              ((AbstractStruct)resource).getViewer().selectEntry(((Item) table.getValueAt(row, 2)).getName());
            }
          }
        }
      });
      bopen.addActionListener(this);
      bopennew.addActionListener(this);
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      resultFrame.pack();
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
      resultFrame.setVisible(true);
    }
    blocker.setBlocked(false);
//    for (int i = 0; i < table.getRowCount(); i++) {
//      CreInvError error = (CreInvError)table.getTableItemAt(i);
//      System.out.println(error.resourceEntry + " (" + error.resourceEntry.getSearchString() + ") -> " + error.itemRef.getAttribute("Item"));
//    }
  }

// --------------------- End Interface Runnable ---------------------

  private void checkCreatureInventory(CreResource cre)
  {
    HexNumber slots_offset = (HexNumber)cre.getAttribute("Item slots offset");
    items.clear();
    slots.clear();
    for (int i = 0; i < cre.getRowCount(); i++) {
      StructEntry entry = cre.getStructEntryAt(i);
      if (entry instanceof Item)
        items.add(entry);
      else if (entry.getOffset() >= slots_offset.getValue() + cre.getOffset() &&
               entry instanceof DecNumber
               && !entry.getName().equals("Weapon slot selected")
               && !entry.getName().equals("Weapon ability selected"))
        slots.add(entry);
    }
    for (int i = 0; i < slots.size(); i++) {
      DecNumber slot = (DecNumber)slots.get(i);
      if (slot.getValue() >= 0 && slot.getValue() < items.size())
        items.set(slot.getValue(), slots_offset); // Dummy object
    }
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i) != slots_offset) {
        Item item = (Item)items.get(i);
        table.addTableItem(new CreInvError(cre.getResourceEntry(), item, "Item not in inventory"));
      }
    }
  }

  private void checkItemAttribute(CreResource cre)
  {
	  List<StructEntry> list = cre.getList();
	  for (int i = 0; i < list.size(); i++)
		  if (list.get(i) instanceof Item)
		  {
			  Item item = (Item) list.get(i);
			  if (((ResourceRef) item.getAttribute("Item")).getResourceName().equalsIgnoreCase("None.ITM"))
			  {
				  table.addTableItem(new CreInvError(cre.getResourceEntry(), item, 
						  "Empty item ref"));
				  continue;
			  }
		      
			  StructEntry wear = item.getAttribute("Wear");
			  if (((DecNumber) wear).getValue() != 0)
			  {
				  table.addTableItem(new CreInvError(cre.getResourceEntry(), item, 
						  "Wear is: " + ((DecNumber) wear).getValue()));
			  }

			  for (int j = 4; j < 8*((Flag) item.getAttribute("Flags")).getSize(); j++)
				  if (((Flag) item.getAttribute("Flags")).isFlagSet(j))
				  {
					  table.addTableItem(new CreInvError(cre.getResourceEntry(), item, 
							  "Item flag is: " + item.getAttribute("Flags").toString()));
					  break;
				  }
		  }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class CreInvError implements TableItem
  {
    private final ResourceEntry resourceEntry;
    private final Item itemRef;
	private final String message;

    private CreInvError(ResourceEntry resourceEntry, Item itemRef, String message)
    {
      this.resourceEntry = resourceEntry;
      this.itemRef = itemRef;
      this.message = message;
    }

    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return resourceEntry.getSearchString();
      else if (columnIndex == 2)
        return itemRef;
      else
    	return message;
    }
  }
}

