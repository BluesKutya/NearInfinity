// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.NearInfinity;
import infinity.datatype.*;
import infinity.gui.*;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.graphics.TisResource;
import infinity.resource.wed.Overlay;
import infinity.resource.wed.WedResource;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

public final class ViewerGraphics extends ChildFrame implements Runnable
{
  private final AreResource areaFile;
  private JLabel label;

  public ViewerGraphics(AreResource areaFile)
  {
    super("Area Viewer: " + areaFile.getName(), true);
    this.areaFile = areaFile;
    new Thread(this).start();
  }

// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    try {
      ResourceRef wedRef = (ResourceRef)areaFile.getAttribute("WED resource");
      WedResource wedFile = new WedResource(
              ResourceFactory.getInstance().getResourceEntry(wedRef.getResourceName()));
      Overlay overlay = (Overlay)wedFile.getAttribute("Overlay");
      ResourceRef tisRef = (ResourceRef)overlay.getAttribute("Tileset");
      int width = ((DecNumber)overlay.getAttribute("Width")).getValue();
      int height = ((DecNumber)overlay.getAttribute("Height")).getValue();
      int mapOffset = ((HexNumber)overlay.getAttribute("Tilemap offset")).getValue();
      int lookupOffset = ((HexNumber)overlay.getAttribute("Tilemap lookup offset")).getValue();
      int mapIndex = 0, lookupIndex = 0;
      for (int i = 0; i < overlay.getRowCount(); i++) {
        StructEntry entry = overlay.getStructEntryAt(i);
        if (entry.getOffset() == mapOffset)
          mapIndex = i;
        else if (entry.getOffset() == lookupOffset)
          lookupIndex = i;
      }

      blocker.setBlocked(true);
//      TisResource tisFile = new TisResource(ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName()));
//      BufferedImage image = tisFile.drawImage(width, height, mapIndex, lookupIndex, overlay);
//      tisFile.close();
      BufferedImage image = TisResource.drawImage(
              ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName()),
              width, height, mapIndex, lookupIndex, overlay);

      Container pane = getContentPane();
      pane.setLayout(new BorderLayout());
      label = new JLabel(new ImageIcon(image));
      label.addMouseListener(new AreaMouseListener());
      pane.add(new JScrollPane(label), BorderLayout.CENTER);
      setSize(NearInfinity.getInstance().getSize());
      Center.center(this, NearInfinity.getInstance().getBounds());
      blocker.setBlocked(false);
      setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
      blocker.setBlocked(false);
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), e.toString(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
    } catch (OutOfMemoryError e) {
      e.printStackTrace();
      blocker.setBlocked(false);
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), e.toString(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

// --------------------- End Interface Runnable ---------------------

  protected void windowClosing() throws Exception
  {
    if (label != null && label.getIcon() != null)
      ((ImageIcon)label.getIcon()).getImage().flush();
    dispose();
  }

  private final class AreaMouseListener extends MouseAdapter
  {
	    private final AreaPopupMenu pmenu = new AreaPopupMenu();

	    public void mouseReleased(MouseEvent e)
	    {
	    	if (e.isPopupTrigger()) {
	    		pmenu.show(e.getComponent(), e.getX(), e.getY());
	    	}
	    }
  }

  private final class AreaPopupMenu extends JPopupMenu implements ActionListener
  {
    private final JMenuItem mi_save = new JMenuItem("Save as...");

    AreaPopupMenu()
    {
      add(mi_save);
      mi_save.addActionListener(this);
    }

    public void show(Component invoker, int x, int y)
    {
      super.show(invoker, x, y);
    }

    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == mi_save) {
    	  JFileChooser  fc = new JFileChooser();
          fc.setDialogTitle("Save map");
          fc.addChoosableFileFilter(new FileNameExtensionFilter("GIF Images", "gif"));
          fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
          fc.addChoosableFileFilter(new FileNameExtensionFilter("JPEG Images", "jpg", "jpeg"));
          fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
          fc.setAcceptAllFileFilterUsed(false);
          if (fc.showSaveDialog(getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION)
          {
        	  File output = fc.getSelectedFile();
        	  FileNameExtensionFilter fFilter = (FileNameExtensionFilter) fc.getFileFilter();
        	  if (!output.getName().endsWith("." + fFilter.getExtensions()[0]))
        		  output = new File(output.getAbsolutePath() + "." + fFilter.getExtensions()[0]);
        	  try {
        		  ImageIO.write((RenderedImage) ((ImageIcon)label.getIcon()).getImage(), fFilter.getExtensions()[0], output);
        	  } catch (IOException e) {
        		  e.printStackTrace();
        	  }        	  
          }
      }
    }
  }
}

