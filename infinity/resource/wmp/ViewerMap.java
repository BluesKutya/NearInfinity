// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.NearInfinity;
import infinity.datatype.*;
import infinity.gui.ViewerUtil;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.graphics.BamResource;
import infinity.resource.key.ResourceEntry;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

public final class ViewerMap extends JPanel implements ListSelectionListener
{
  private static final ImageIcon areaIcon = Icons.getIcon("Stop16.gif");
  private final BufferedImage map;
  private int xCoord = -1, yCoord, pixels[] = null, iHeight, iWidth;
  BamResource icons = null;
  MapEntry wmpMap;
  JPanel areas;
  JScrollPane mapScroll;
  
  public ViewerMap(MapEntry wmpMap)
  {
	this.wmpMap = wmpMap;
    ResourceRef iconRef = (ResourceRef)wmpMap.getAttribute("Map icons");
    if (iconRef != null) {
      ResourceEntry iconEntry = ResourceFactory.getInstance().getResourceEntry(iconRef.getResourceName());
      if (iconEntry != null)
        icons = (BamResource)ResourceFactory.getResource(iconEntry);
    }
    JLabel mapLabel = ViewerUtil.makeImagePanel((ResourceRef)wmpMap.getAttribute("Map"));
    mapLabel.addMouseListener(new MapMouseListener());
    map = (BufferedImage)((ImageIcon)mapLabel.getIcon()).getImage();
    drawIcons();
    areas = ViewerUtil.makeListPanel("Areas", wmpMap, AreaEntry.class, "Name",
                                            new WmpAreaListRenderer(icons), this);
    mapScroll = new JScrollPane(mapLabel);
    mapScroll.setBorder(BorderFactory.createEmptyBorder());

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapScroll, areas);
    split.setDividerLocation(NearInfinity.getInstance().getWidth() - 475);
    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public void drawIcons()
  {
	  Graphics2D graph = map.createGraphics();
	  graph.setFont(new Font("Tahoma", Font.BOLD, 10));
	  FontMetrics fm = graph.getFontMetrics();
	  int pile;

	  for (StructEntry areaEntry : wmpMap.getList())
		  if (areaEntry instanceof AreaEntry)
		  {
			  int frameNr = icons.getFrameNr(((DecNumber)((AbstractStruct) areaEntry).getAttribute("Icon number")).getValue(), 0);
			  Image icon = icons.getFrame(frameNr);
			  Point center = icons.getFrameCenter(frameNr);
			  int iW = icon.getWidth(this);
			  int iH = icon.getHeight(this);
			  int iX = ((DecNumber)((AbstractStruct) areaEntry).getAttribute("Coordinate: X")).getValue();
			  int iY = ((DecNumber)((AbstractStruct) areaEntry).getAttribute("Coordinate: Y")).getValue();
			  pile = 0;
			  for (StructEntry prevArea : wmpMap.getList())
				  if (prevArea instanceof AreaEntry)
				  {
					  if (prevArea == areaEntry)
						  break;
					  else if (iX == ((DecNumber)((AbstractStruct) prevArea).getAttribute("Coordinate: X")).getValue() &&
							  iY == ((DecNumber)((AbstractStruct) prevArea).getAttribute("Coordinate: Y")).getValue())
					  {
						  pile++;
						  drawCode(graph, fm, areaEntry, "Current area", iX, iY, iW, iH, center, pile);
						  if (!drawName(graph, fm, areaEntry, "Tooltip", iX, iY, iW, iH, center, pile, prevArea))
							  drawName(graph, fm, areaEntry, "Name", iX, iY, iW, iH, center, pile, prevArea);
					  }
				  }
			  if (pile == 0)
			  {
				  map.getGraphics().drawImage(doTransparent(icon, icons.getTransparent()), iX - center.x, iY - center.y, null);
				  drawCode(graph, fm, areaEntry, "Current area", iX, iY, iW, iH, center, pile);
				  if (!drawName(graph, fm, areaEntry, "Tooltip", iX, iY, iW, iH, center, pile, null))
					  drawName(graph, fm, areaEntry, "Name", iX, iY, iW, iH, center, pile, null);
			  }
		  }
  }

  private Boolean drawName(Graphics2D graph, FontMetrics fm, StructEntry areaEntry, String attrName, int iX, int iY, int iW, int iH, Point center, int pile, StructEntry prevArea)
  {
	  StringRef nameRef = (StringRef) ((AbstractStruct) areaEntry).getAttribute(attrName);
	  if (nameRef.getValue() > 0)
	  {
		  if (prevArea != null &&
				  ((StringRef) ((AbstractStruct) prevArea).getAttribute(attrName)).getValue() == nameRef.getValue())
		  {
			  return true;
		  }
		  String name = nameRef.toString();
		  this.drawString(graph, name, iX + iW / 2 - center.x - fm.stringWidth(name) / 2,
				  iY + iH - center.y + fm.getAscent() * (pile + 1), Color.BLACK, Color.WHITE);
		  return true;
	  }
	  return false;
  }

  private Boolean drawCode(Graphics2D graph, FontMetrics fm, StructEntry areaEntry, String attrName, int iX, int iY, int iW, int iH, Point center, int pile)
  {
	  if (!((AbstractStruct) areaEntry).getAttribute(attrName).toString().equalsIgnoreCase("NONE"))
	  {
		  String name = ((AbstractStruct) areaEntry).getAttribute(attrName).toString().split("\\.")[0];
		  this.drawString(graph, name, iX + iW / 2 - center.x - fm.stringWidth(name) / 2,
				  iY + iH / 2 - center.y + fm.getAscent() * pile, Color.BLACK, Color.YELLOW);
		  return true;
	  }
	  return false;
  }
  
  private void drawString(Graphics2D graph, String str, int x, int y, Color bgColor, Color frColor)
  {
	  graph.setColor(bgColor);
	  graph.drawString(str, x - 1, y - 1);
	  graph.drawString(str, x + 1, y - 1);
	  graph.drawString(str, x - 1, y + 1);
	  graph.drawString(str, x + 1, y + 1);
	  graph.setColor(frColor);
	  graph.drawString(str, x, y);
  }

// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    if (!event.getValueIsAdjusting()) {
      JList list = (JList)event.getSource();
      AreaEntry areaEntry = (AreaEntry)list.getSelectedValue();
      if (areaEntry.getAttribute("Current area").toString().equalsIgnoreCase("NONE"))
    	return;
      if (xCoord != -1)
        map.setRGB(xCoord, yCoord, iWidth, iHeight, pixels, 0, iWidth);
      int iconNr = ((DecNumber)areaEntry.getAttribute("Icon number")).getValue();
      int frameNr = icons.getFrameNr(iconNr, icons.getFrameCount(iconNr) > 10 ? 10 : 0);
      Image icon = icons.getFrame(frameNr);
	  Point center = icons.getFrameCenter(frameNr);
      iWidth = icon.getWidth(this);
      iHeight = icon.getHeight(this);
      xCoord = ((DecNumber)areaEntry.getAttribute("Coordinate: X")).getValue() - center.x;
      yCoord = ((DecNumber)areaEntry.getAttribute("Coordinate: Y")).getValue() - center.y;
      pixels = map.getRGB(xCoord, yCoord, iWidth, iHeight, null, 0, iWidth);
      map.getGraphics().drawImage(doTransparent(icon, icons.getTransparent()), xCoord, yCoord, null);
      repaint();
      if (mapScroll != null)
      {
    	  JScrollBar vScroll = mapScroll.getVerticalScrollBar();
    	  vScroll.setValue(yCoord - vScroll.getHeight() / 2);
    	  JScrollBar hScroll = mapScroll.getHorizontalScrollBar();
    	  hScroll.setValue(xCoord - hScroll.getWidth() / 2);
      }
    }
  }

// --------------------- End Interface ListSelectionListener ---------------------

  private Image doTransparent(Image imgSrc, int transparentColor)
  {
	  int height = imgSrc.getHeight(null);
	  int width = imgSrc.getWidth(null);
	  BufferedImage retImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	  int pixels[] = null;

	  pixels = ((BufferedImage) imgSrc).getRGB(0, 0, width, height, null, 0, width);
	  for (int x = 0; x < width; x++)
		  for (int y = 0; y < height; y++)
			  if ((pixels[x + y * width] & 0xffffff) == transparentColor)
				  pixels[x + y * width] &= 0xffffff;

	  retImage.setRGB(0, 0, width, height, pixels, 0, width);

	  return retImage;
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class WmpAreaListRenderer extends DefaultListCellRenderer
  {
    private final BamResource icons;

    private WmpAreaListRenderer(BamResource icons)
    {
      this.icons = icons;
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus)
    {
      JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      AbstractStruct struct = (AbstractStruct)value;
      label.setText(struct.getAttribute("Name").toString());
      DecNumber animNr = (DecNumber)struct.getAttribute("Icon number");
      setIcon(null);
      if (icons != null)
        setIcon(new ImageIcon(icons.getFrame(icons.getFrameNr(animNr.getValue(), 0))));
      return label;
    }
  }

  private final class MapMouseListener extends MouseAdapter
  {
	    private final MapPopupMenu pmenu = new MapPopupMenu();

	    public void mouseReleased(MouseEvent e)
	    {
	    	if (e.isPopupTrigger()) {
	    		pmenu.show(e.getComponent(), e.getX(), e.getY());
	    	}
	    }
  }

  private final class MapPopupMenu extends JPopupMenu implements ActionListener
  {
    private final JMenuItem mi_save = new JMenuItem("Save as...");

    MapPopupMenu()
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
        		  ImageIO.write(map, fFilter.getExtensions()[0], output);
        	  } catch (IOException e) {
        		  // TODO Auto-generated catch block
        		  e.printStackTrace();
        	  }        	  
          }
      }
    }
  }
}

