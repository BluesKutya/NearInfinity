// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.gui.TextListPanel;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.other.PlainTextResource;
import infinity.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;

public final class Kit2daBitmap extends Datatype implements Editable
{
  private static final LongIntegerHashMap<KitlistEntry> kitsNumber = new LongIntegerHashMap<KitlistEntry>();
//  private static final LongIntegerHashMap<KitlistEntry> kitsUnusable = new LongIntegerHashMap<KitlistEntry>();
  private TextListPanel list;
//  private boolean useUnusable = true;
  private long value;

  private static void parseKitlist()
  {
    try {
      PlainTextResource kitlist = new PlainTextResource(
              ResourceFactory.getInstance().getResourceEntry("KITLIST.2DA"));
      StringTokenizer st = new StringTokenizer(kitlist.getText(), "\n");
      if (st.hasMoreTokens())
        st.nextToken();
      if (st.hasMoreTokens())
        st.nextToken();
      if (st.hasMoreTokens())
        st.nextToken();
      if (st.hasMoreTokens())
        st.nextToken();
      while (st.hasMoreTokens())
        parseKitlistLine(st.nextToken());
    } catch (Exception e) {
      e.printStackTrace();
    }
    kitsNumber.put((long)0, new KitlistEntry((long)0, "NO_KIT"));
    kitsNumber.put((long)0x00000001, new KitlistEntry((long)0x00000001, "BERSERKER"));
    kitsNumber.put((long)0x00000002, new KitlistEntry((long)0x00000002, "WIZARDSLAYER"));
    kitsNumber.put((long)0x00000004, new KitlistEntry((long)0x00000004, "KENSAI"));
    kitsNumber.put((long)0x00000008, new KitlistEntry((long)0x00000008, "CAVALIER"));
    kitsNumber.put((long)0x00000010, new KitlistEntry((long)0x00000010, "INQUISITOR"));
    kitsNumber.put((long)0x00000020, new KitlistEntry((long)0x00000020, "UNDEAD_HUNTER"));
    kitsNumber.put((long)0x00000040, new KitlistEntry((long)0x00000040, "ABJURER"));
    kitsNumber.put((long)0x00000080, new KitlistEntry((long)0x00000080, "CONJURER"));
    kitsNumber.put((long)0x00000100, new KitlistEntry((long)0x00000100, "DIVINER"));
    kitsNumber.put((long)0x00000200, new KitlistEntry((long)0x00000200, "ENCHANTER"));
    kitsNumber.put((long)0x00000400, new KitlistEntry((long)0x00000400, "ILLUSIONIST"));
    kitsNumber.put((long)0x00000800, new KitlistEntry((long)0x00000800, "INVOKER"));
    kitsNumber.put((long)0x00001000, new KitlistEntry((long)0x00001000, "NECROMANCER"));
    kitsNumber.put((long)0x00002000, new KitlistEntry((long)0x00002000, "TRANSMUTER"));
    kitsNumber.put((long)0x00004000, new KitlistEntry((long)0x00004000, "TRUECLASS"));
    kitsNumber.put((long)0x00008000, new KitlistEntry((long)0x00008000, "FERALAN"));
    kitsNumber.put((long)0x00010000, new KitlistEntry((long)0x00010000, "STALKER"));
    kitsNumber.put((long)0x00020000, new KitlistEntry((long)0x00020000, "BEASTMASTER"));
    kitsNumber.put((long)0x00040000, new KitlistEntry((long)0x00040000, "ASSASIN"));
    kitsNumber.put((long)0x00080000, new KitlistEntry((long)0x00080000, "BOUNTY_HUNTER"));
    kitsNumber.put((long)0x00100000, new KitlistEntry((long)0x00100000, "SWASHBUCKLER"));
    kitsNumber.put((long)0x00200000, new KitlistEntry((long)0x00200000, "BLADE"));
    kitsNumber.put((long)0x00400000, new KitlistEntry((long)0x00400000, "JESTER"));
    kitsNumber.put((long)0x00800000, new KitlistEntry((long)0x00800000, "SKALD"));
    kitsNumber.put((long)0x01000000, new KitlistEntry((long)0x01000000, "TALOS"));
    kitsNumber.put((long)0x02000000, new KitlistEntry((long)0x02000000, "HELM"));
    kitsNumber.put((long)0x04000000, new KitlistEntry((long)0x04000000, "LATHANDER"));
    kitsNumber.put((long)0x08000000, new KitlistEntry((long)0x08000000, "TOTEMIC_DRUID"));
    kitsNumber.put((long)0x10000000, new KitlistEntry((long)0x10000000, "SHAPESHIFTER"));
    kitsNumber.put((long)0x20000000, new KitlistEntry((long)0x20000000, "BEAST_FRIEND"));
    kitsNumber.put((long)0x40000000, new KitlistEntry((long)0x40000000, "BARBARIAN"));
    kitsNumber.put((long)0x80000000, new KitlistEntry((long)0x80000000, "WILDMAGE"));
//    kitsUnusable.put((long)0, new KitlistEntry((long)0, "NO_KIT"));
  }

  private static void parseKitlistLine(String s)
  {
    StringTokenizer st = new StringTokenizer(s);
    int number = Integer.parseInt(st.nextToken());
    String name = st.nextToken();
//    st.nextToken();
//    st.nextToken();
//    st.nextToken();
//    st.nextToken();
//    st.nextToken();
//    String unusableSt = st.nextToken();
//    long unusable;
//    if (unusableSt.substring(0, 2).equalsIgnoreCase("0x"))
//      unusable = Long.parseLong(unusableSt.substring(2), 16);
//    else
//      unusable = Long.parseLong(unusableSt);
    kitsNumber.put((long)number | 0x4000, new KitlistEntry((long)number | 0x4000, name));
//    kitsUnusable.put(unusable, new KitlistEntry(unusable, name));
  }

  public static void resetKitlist()
  {
    kitsNumber.clear();
//    kitsUnusable.clear();
  }

  public Kit2daBitmap(byte buffer[], int offset)
  {
    super(offset, 4, "Kit");
    if (kitsNumber.size() == 0)
      parseKitlist();
//    if (buffer[offset + 3] == 0x40) {
//      useUnusable = false;
//      value = (long)buffer[offset + 2];
//    }
//    else {
      value = (long)(Byteconvert.convertUnsignedShort(buffer, offset + 2) +
                     0x10000 * Byteconvert.convertUnsignedShort(buffer, offset));
//     if (value < 0)
//       value += 4294967296L;
//   }
  }

// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(final ActionListener container)
  {
    LongIntegerHashMap idsmap = kitsNumber;
//    if (useUnusable)
//      idsmap = kitsUnusable;
    if (list == null) {
      long keys[] = idsmap.keys();
      List<KitlistEntry> items = new ArrayList<KitlistEntry>(keys.length);
      for (long id : keys) {
        items.add((KitlistEntry)idsmap.get(id));
      }
      list = new TextListPanel(items);
      list.addMouseListener(new MouseAdapter()
      {
        public void mouseClicked(MouseEvent event)
        {
          if (event.getClickCount() == 2)
            container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
        }
      });
    }
    Object selected = idsmap.get(value);
    if (selected != null)
      list.setSelectedValue(selected, true);

    JButton bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(list, gbc);
    panel.add(list);

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets.left = 6;
    gbl.setConstraints(bUpdate, gbc);
    panel.add(bUpdate);

    panel.setMinimumSize(DIM_MEDIUM);
    panel.setPreferredSize(DIM_MEDIUM);
    return panel;
  }

  public void select()
  {
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  public boolean updateValue(AbstractStruct struct)
  {
    KitlistEntry selected = (KitlistEntry)list.getSelectedValue();
    value = selected.number;
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
//    if (useUnusable) {
//      if (value > 2147483648L)
//        value -= 4294967296L;
      byte buffer[] = Byteconvert.convertBack((int)value);
      os.write((int)buffer[2]);
      os.write((int)buffer[3]);
      os.write((int)buffer[0]);
      os.write((int)buffer[1]);
//    }
//    else
//      Filewriter.writeBytes(os, new byte[]{0x00, 0x00, (byte)value, 0x40});
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    Object o;
//    if (useUnusable)
//      o = kitsUnusable.get(value);
//    else
    o = kitsNumber.get(value);
    if (o == null)
      return "Unknown - " + value;
    else
      return o.toString();
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class KitlistEntry
  {
    private final long number;
    private final String name;

    private KitlistEntry(long number, String name)
    {
      this.number = number;
      this.name = name;
    }

    public String toString()
    {
    	if ((number & 0x4000) != 0 && number != 0x4000 || number == 0)
    		return name + " - " + (number & 0xBFFF);
    	else
    		return name + " - 0x" + Integer.toHexString((int)number);
    }
  }
}

