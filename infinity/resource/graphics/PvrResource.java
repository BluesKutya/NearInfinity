// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2012 Jurgen
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.util.Byteconvert;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class PvrResource implements Resource, ActionListener, Closeable
{
	private final ResourceEntry entry;
	private byte tileData[];
	private JButton bexport;
	private JPanel panel;
	private BufferedImage image;

	public PvrResource(ResourceEntry entry) throws Exception
	{
		this.entry = entry;
		tileData = entry.getResourceData();
	}

	public JComponent makeViewer(ViewableContainer container)
	{
		bexport = new JButton("Export...", Icons.getIcon("Export16.gif"));
		bexport.setMnemonic('e');
		bexport.addActionListener(this);

		image = getImage();
	    JScrollPane scroll = new JScrollPane(new JLabel(new ImageIcon(image)));

		JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bpanel.add(bexport);

		panel = new JPanel(new BorderLayout());
		panel.add(scroll, BorderLayout.CENTER);
		panel.add(bpanel, BorderLayout.SOUTH);
		scroll.setBorder(BorderFactory.createLoweredBevelBorder());

		return panel;
	}

	private int[] unpack(byte[] tileData, int offset)
	{
		int argb[] = new int[16];
		short codes[] = new short[16];
		int a =  tileData[offset] & 0xFF | ( tileData[offset+1] & 0xFF ) << 8;
		short red = (short) ( ( a >> 11 ) & 0x1f );
		short green = (short) ( ( a >> 5 ) & 0x3f );
		short blue = (short) ( a & 0x1f );
		codes[0] = (short) (( red << 3 ) | ( red >> 2 ));
		codes[1] = (short) (( green << 2 ) | ( green >> 4 ));
		codes[2] = (short) (( blue << 3 ) | ( blue >> 2 ));
		codes[3] = 0xFF;
		int b = tileData[offset+2] & 0xFF | ( tileData[offset+3] & 0xFF ) << 8;
		red = (short) ( ( b >> 11 ) & 0x1f );
		green = (short) ( ( b >> 5 ) & 0x3f );
		blue = (short) ( b & 0x1f );
		codes[4] = (short) (( red << 3 ) | ( red >> 2 ));
		codes[5] = (short) (( green << 2 ) | ( green >> 4 ));
		codes[6] = (short) (( blue << 3 ) | ( blue >> 2 ));
		codes[7] = 0xFF;
		
		for( int i = 0; i < 3; i++ )
		{
			short c = codes[i];
			short d = codes[4 + i];

			if( a <= b )
			{
				codes[8 + i] = (short) ( ( ( c + d )/2 ) & 0xFF);
				codes[12 + i] = 0;
			}
			else
			{
				codes[8 + i] = (short) ( ( ( 2*c + d )/3 ) & 0xFF);
				codes[12 + i] = (short) ( ( ( c + 2*d )/3 ) & 0xFF);
			}
		}
		
		codes[8 + 3] = 0xFF;
		codes[12 + 3] = (short) (( a <= b ) ? 0 : 255);
		
		short indices[] = new short[16];
		for( int i = 0; i < 4; i++ )
		{
			short packed = (short) (tileData[offset + 4 + i] & 0xFF);
			
			indices[4*i] = (short) (packed & 0x3);
			indices[4*i+1] = (short) (( packed >> 2 ) & 0x3);
			indices[4*i+2] = (short) (( packed >> 4 ) & 0x3);
			indices[4*i+3] = (short) (( packed >> 6 ) & 0x3);
		}

		for( int i = 0; i < 16; i++ )
		{
			byte off = (byte) (4*indices[i]);
			argb[i] = codes[off + 2] | codes[off + 1] << 8 | codes[off] << 16 | codes[off+3] << 24; 
		}
		return argb;
	}
	
	public BufferedImage getImage()
	{
		int pvrFormat = Byteconvert.convertInt(tileData, 0);
		int pixelFormat = Byteconvert.convertInt(tileData, 8);
		if (pvrFormat != 0x03525650 || pixelFormat != 0x07)
			return null;
		int tileHeight = Byteconvert.convertInt(tileData, 24);
		int tileWidth = Byteconvert.convertInt(tileData, 28);
	    BufferedImage image = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB);
	    int offset = 52;
	    for (int y = 0; y < tileHeight; y += 4)
	    	for (int x = 0; x < tileWidth; x += 4)
	    	{
	    		int[] udata = unpack(tileData, offset);
				for (int py = 0; py < 4; py++)
					for (int px = 0; px < 4; px++)
					{
						int sx = x + px;
						int sy = y + py;
						if( sx < tileWidth && sy < tileHeight )
				    		image.setRGB(sx, sy, udata[py * 4 + px]);
					}
				offset += 8;
	    	}
		return image;
	}

	public void close() throws Exception
	{
		image.flush();
	}

	public void actionPerformed(ActionEvent event)
	{
	    if (event.getSource() == bexport)
	        ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
	}

	public ResourceEntry getResourceEntry()
	{
		return entry;
	}

}
