/*
This file is part of TONO.

    TONO is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    TONO is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with TONO.  If not, see <http://www.gnu.org/licenses/>.
*/

import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;

public class KnotDataViewer extends JComponent
{
  private int width;
  private int height;
  
  private float[] x,y,z;
  private int[] xr,yr,zr;
  private float xmin, xmax, ymin, ymax, zmin, zmax;
  private float radius;
  private int radiusr, radiusrx, radiusry;
  
  private int minimumViewRadius = 4;
  
  private static Color[] depthCols = new Color[32];
          {
            for(int i=0; i<depthCols.length; i++)
              depthCols[i] = new Color(Color.HSBtoRGB((depthCols.length-(float)i-10)/depthCols.length, 0.5f, 0.5f));
          }

  public KnotDataViewer(int width, int height, KnotData kd)
  {
    xmin = ymin = zmin = xmax = ymax = zmax = 0.0f;
    update(kd);
    setSize(width, height);
  }
  
  public void setSize(int width, int height)
  {
    if(this.width==width && this.height==height) return; // No update needed

    this.width=width;
    this.height=height;
    
	super.setSize(width,height);
	setMinimumSize(new Dimension(width,height));
	setMaximumSize(new Dimension(width,height));
	setPreferredSize(new Dimension(width,height));
	
	updateRealCoords();
	
	repaint();
  }

  public void update(KnotData kd)
  {
    // Determine the necessary size of the x,y,z arrays
    int totalAtoms = 0;
    for(int i=0; i<kd.numStrands(); i++)
      totalAtoms += kd.get(i).length;
    x = new float[totalAtoms];
    y = new float[totalAtoms];
    z = new float[totalAtoms];
    xr = new int[totalAtoms];
    yr = new int[totalAtoms];
    zr = new int[totalAtoms];

    radius = kd.getAtomRadius();

    KnotData.KnotAtom curAtom;
    if(kd.numStrands()>0)
    {
      curAtom = kd.get(0,0);

      // Initialise the min/max monitors
      xmin = xmax = curAtom.x;
      ymin = ymax = curAtom.y;
      zmin = zmax = curAtom.z;
    }

    int count=0;
    for(int i=0; i<kd.numStrands(); i++)
      for(int j=0; j<kd.get(i).length; j++)
      {
        curAtom = kd.get(i, j);
        x[count] = curAtom.x;
        y[count] = curAtom.y;
        z[count] = curAtom.z;
        if(curAtom.x>xmax) xmax = curAtom.x;
        if(curAtom.x<xmin) xmin = curAtom.x;
        if(curAtom.y>ymax) ymax = curAtom.y;
        if(curAtom.y<ymin) ymin = curAtom.y;
        if(curAtom.z>zmax) zmax = curAtom.z;
        if(curAtom.z<zmin) zmin = curAtom.z;
        count++;
      }

    // Now alter xmax, ymax, xmin, ymin so that they end up giving a wider scope than they would
    float extrax = xmax-xmin/10;
    float extray = ymax-ymin/10;
    xmin -= extrax;
    xmax += extrax;
    ymin -= extray;
    ymax += extray;

    updateRealCoords();
  }
  
  private void updateRealCoords()
  {
    float xscale = width            / (xmax - xmin);
    float yscale = height           / (ymax - ymin);
    float zscale = depthCols.length * 0.7f / (zmax - zmin);
    
//    xscale = Math.max(xscale, yscale); // We need to have equal scaling for each dimension
//    radiusr = (int)(radius * xscale);
    radiusrx = Math.max((int)(radius * xscale), 2);
    radiusry = Math.max((int)(radius * yscale), 2);

/*
    if(radiusr<minimumViewRadius)
    {
      xscale = minimumViewRadius/(float)radius;
      radiusr = minimumViewRadius;
    }
*/

    for(int i=0; i<x.length; i++)
    {
      // The xr, yr, zr co-ordinates are the co-ordinates in the actual screen area (0-width,0-height)
      xr[i] = (int)Math.floor((x[i]-xmin) * xscale);
      yr[i] = (int)Math.floor((y[i]-ymin) * yscale);
      zr[i] = (int)Math.floor((z[i]-zmin) * zscale) % depthCols.length;
    }
    
    // Sort the real co-ords in order of z dimension (so that they get drawn in the correct order)
    // NOT IMPLEMENTED YET
    sort();
    
    repaint();
  }
  
  // Functions for quicksorting the real co-ords in order of z-dimension
  private void sort()
  {
    sort(0, xr.length-1);
  }
  private void sort(int start, int end)
  {
    int p;
    if(end > start)
    {
      p = partition(start, end);
      sort(start, p-1);
      sort(p+1, end);
    }
  }
  private int compare(int i, int j)
  {
    if(zr[i]==zr[j]) return 0;
    else if(zr[i]>zr[j]) return 1;
    else return -1;
  }
  private int partition(int start, int end)
  {
    int left, right;
    int partitionElement = end;
    left = start-1;
    right = end;
    for(;;)
    {
      while(compare(partitionElement, ++left) == 1)
        if(left==end) break;
      while(compare(partitionElement, --right) == -1)
        if(right==start) break;
      if(left>=right) break;
      swap(left, right);
    }
    swap(left, end);
    return left;
  }
  private void swap(int i, int j)
  {
    // Swaps the xr, yr, zr coords
    int temp = xr[i];
    xr[i] = xr[j];
    xr[j] = temp;
    temp = yr[i];
    yr[i] = yr[j];
    yr[j] = temp;
    temp = zr[i];
    zr[i] = zr[j];
    zr[j] = temp;
  }
  // End of: Functions for quicksorting the real co-ords in order of z-dimension

  public void paint(Graphics g)
  {
	Graphics2D g2D = (Graphics2D)g;
    g2D.setPaint(Color.black);
    g2D.fillRect(0,0,width-1, height-1);

    for(int i=0; i<x.length; i++)
    {
      g2D.setPaint(depthCols[zr[i]]);
      g2D.fillOval(xr[i]-radiusrx, yr[i]-radiusry, radiusrx+radiusrx, radiusry+radiusry);
    }    
    g2D.setPaint(Color.red);
    g2D.drawRect(0,0,width-1, height-1);
  } // End of: paint()
  
}
