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

import java.util.*;
import java.io.*;

public class KnotData implements Serializable, Cloneable
{
  private Vector strands; // Will hold a collection of KnotAtom[] arrays
  private KnotAtom[] curStr, curStr2; // Used to hold the "current strand" for various purposes
  private Vector nn; // A vector - for each strand it contains a Vector[] array, each of which lists an atom's nearby neighbours

  private float atomRadius = 1.01f;
  private float leashLength = 1.51f;
  private float deltaParameter = 0.11f; // Affects how much space is left between corrected overlaps
  private float etaParameter = 0.21f; // Affects how close things need to be to be classed as neighbours
  private int skippedParameter = 1;
  
  private StringBuffer algoProgress = new StringBuffer();
  
  public KnotData() // Creates an EMPTY knot - only for placeholder use!
  {
    strands = new Vector(1,1);
    nn = new Vector(1,1);
  }
  public KnotData(InputStream inStream) throws IOException
  {
    BufferedReader bReader = new BufferedReader(new InputStreamReader(inStream));
    strands = new Vector(1,1);
    nn = new Vector(1,1);
    String curLine;
    Vector atoms = new Vector(); // Temporary holder - gets converted into array
    StringTokenizer tokenizer;
    float[] coords; // Temporary holder for co-ordinates

    // Read the parameters from the data file's first line
    curLine=bReader.readLine();
    if(curLine==null)
      throw new IOException("Data stream is empty!");
    tokenizer = new StringTokenizer(curLine, "\t", false); // Chop it up using tab delimiters
    if(tokenizer.hasMoreTokens())
      atomRadius = (Float.valueOf(tokenizer.nextToken())).floatValue();
    if(tokenizer.hasMoreTokens())
      leashLength = (Float.valueOf(tokenizer.nextToken())).floatValue();
    if(tokenizer.hasMoreTokens())
      deltaParameter = (Float.valueOf(tokenizer.nextToken())).floatValue();
    if(tokenizer.hasMoreTokens())
      etaParameter = (Float.valueOf(tokenizer.nextToken())).floatValue();
    if(tokenizer.hasMoreTokens())
      skippedParameter = (Integer.valueOf(tokenizer.nextToken())).intValue();


    bReader.readLine(); // Discard the second line - it provides a gap between the params and the atomic coords

    while(true) // This loop iterates through the lines of the input file
    {
      curLine=bReader.readLine();
      if(!(curLine==null || curLine.equals("") || curLine.startsWith("\t"))) // If it contains data then let's use it
      {
        tokenizer = new StringTokenizer(curLine, "\t", false); // Chop it up using tab delimiters
        coords = new float[6];
        Arrays.fill(coords, 0.0f);
        for(int i=0; i<coords.length; i++)
        {
          if(!tokenizer.hasMoreTokens())
            break;
          coords[i] = (Float.valueOf(tokenizer.nextToken())).floatValue();
        }
        atoms.add(new KnotAtom(coords[0],coords[1],coords[2],coords[3],coords[4],coords[5]));
      } // End of line-ain't-blank
      else               // Else a blank line indicates a new strand is begun 
      if(atoms.size()>0) // (but make sure there's an old strand first!)
      {
        curStr = new KnotAtom[atoms.size()];
        for(int i=0; i<curStr.length; i++)
          curStr[i] = (KnotAtom)atoms.get(i);
        strands.add(curStr);
        nn.add(new Vector[curStr.length]);
////        System.out.println("Added new strand to vector! Length="+atoms.size());
        atoms = new Vector(); // Empty out the vector which holds the currently-being-generated strand
      }
      if(curLine==null) // End of file
        break;
    } // End of the thing that loops through the input stream
    
    
  } // End of constructor using InputStream

  public KnotAtom[] get(int strandNum)
  {
    if(strandNum<0 || strandNum>= strands.size())
      throw new IllegalArgumentException("Illegal argument: KnotData.get(" + strandNum + ")");
    return (KnotAtom[])(strands.get(strandNum));
  }
  public KnotAtom get(int strandNum, int atomNum)
  {
    KnotAtom[] temp = get(strandNum);
    if(atomNum<0 || atomNum>temp.length)
      throw new IllegalArgumentException("Illegal argument: KnotData.get(" + strandNum + ","+atomNum+")");
    return temp[atomNum];
  }
  public int numStrands()
  {
    return strands.size();
  }

  public class KnotAtom implements Serializable
  {
    public float x,y,z,xf,yf,zf; // The co-ordinates, and the co-ordinates of the force to be applied
    KnotAtom(float x,float y)  {   this(x,y,0,0,0,0);   }
    KnotAtom(float x,float y,float z)  {   this(x,y,z,0,0,0);   }
    KnotAtom(float x,float y,float z,float xf)  {   this(x,y,z,xf,0,0);   }
    KnotAtom(float x,float y,float z,float xf,float yf)  {   this(x,y,z,xf,yf,0);   }
    KnotAtom(float x,float y,float z,float xf,float yf,float zf)
    {
      this.x=x;      this.y=y;      this.z=z;      this.xf=xf;      this.yf=yf;      this.zf=zf;
    }
    public String toString()
    {
      return ""+x+"\t"+y+"\t"+z+"\t"+xf+"\t"+yf+"\t"+zf;
    }
  } // End of: class KnotAtom implemets Serializable
 
 
  
  public String toString()
  {
    StringBuffer ret = new StringBuffer(atomRadius+"\t"+leashLength+"\t"+deltaParameter+"\t"+etaParameter+"\t"+skippedParameter+"\nThis second row of the datafile is ignored. The row above specifies(in this order): atom radius, leash length, deltaParameter, etaParameter, skippedParameter\n");
    Iterator i = strands.iterator();
    while(i.hasNext())
    {
      curStr = (KnotAtom[])i.next();
      for(int j=0; j<curStr.length; j++)
      {
        ret.append(curStr[j].toString());
        ret.append("\n");
      }
      ret.append("\n");
    }
    return ret.toString();
  } // End of KnotData's toString() method

  public void controlLeashesCL()
  {
    controlLeashesCL(atomRadius, leashLength);
  }
  public void controlLeashesCL(float radius, float dl)
  {
    for(int i=0; i<numStrands(); i++)
      controlLeashesCL(i, radius, dl);
  }
  public void controlLeashesCL(int strandNum, float radius, float dl)
  {
    controlLeashesCL(strandNum, radius, dl, (int)Math.floor(Math.random() * get(strandNum).length), Math.random()>=0.5);
  }
  synchronized public void controlLeashesCL(int strandNum, float radius, float dl, int startAt, boolean goUpwards)
  {
//    algoProgress.append("-controlLeashesCL("+strandNum+", "+radius+", "+dl+", "+startAt+", "+goUpwards+")\n");
    float d, ex, ey, ez, ddOver2, dx, dy, dz;
    curStr = get(strandNum);
    int iPlus1;
    int i = startAt;
    for(int j=0; j<curStr.length; j++)
    {
      iPlus1 = i+1;
      
      if(iPlus1<curStr.length) // In this program the two end atoms don't interact - i.e. we cannot loop
      {
      
        // CL calculations follow, as specified in the spaghetti paper
        // d is the 3-dimensional gap between two adjacent loci - and actually "d" is the magnitude
        dx = curStr[iPlus1].x - curStr[i].x;
        dy = curStr[iPlus1].y - curStr[i].y;
        dz = curStr[iPlus1].z - curStr[i].z;
        d = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if(d > dl && d!=0.0f)
        {
          // e is a unit vector along the line joining the two loci
          ex = dx/d;
          ey = dy/d;
          ez = dz/d;
          ddOver2 = (dl - d)/2;
          // So now we move the two loci to their properly-repelled positions
          curStr[i].x -= ddOver2 * ex;
          curStr[i].y -= ddOver2 * ey;
          curStr[i].z -= ddOver2 * ez;
          curStr[iPlus1].x += ddOver2 * ex;
          curStr[iPlus1].y += ddOver2 * ey;
          curStr[iPlus1].z += ddOver2 * ez;
        }
      }
      
      // Iterate up or down, depending on the chosen direction
      if(goUpwards) i=iPlus1; else i--;
      // Wraparound
      if(i == -1)
        i = curStr.length-1;
      else if(i>=curStr.length)
        i = 0;
    }
    
    
  } // End of controlLeashesCL(int strandNum, float radius, float dl, int startAt, boolean goUpwards)


  public void findNeighboursFN() // R = radius of spheres, E = small parameter of bitmoreneighbourhoodness
  {
    findNeighboursFN(atomRadius, etaParameter, skippedParameter);
  }
  synchronized public void findNeighboursFN(float R, float E, int skipped) // R = radius of spheres, E = small parameter of bitmoreneighbourhoodness
  {
//    algoProgress.append("-findNeighboursFN("+R+", "+E+", "+skipped+")\n");
    Vector[] curNn;
    int i,j,k,l;
    float dx, dy, dz;
    float R2plusE = R * 2 + E;
    for(i=0; i<strands.size(); i++)
    {
      curStr = get(i);
      curNn  = (Vector[])nn.get(i);
      for(j=0; j<curStr.length; j++)
      {
        // Empty out the vector which contains our nearest neighbours
        curNn[j] = new Vector();
        // Iterate through the strands and through the strand atoms and find the distance
        for(k=0; k<strands.size(); k++)
        {
          curStr2 = get(k);
          for(l=0; l<curStr2.length; l++)
          {
            if(i==k && Math.abs(j-l)<skipped) continue; // We don't worry about things that are very close along the same string
            dx = curStr[j].x - curStr2[l].x;
            dy = curStr[j].y - curStr2[l].y;
            dz = curStr[j].z - curStr2[l].z;
            if(Math.sqrt(dx*dx + dy*dy + dz*dz) < R2plusE) // If distance is small enough...
              curNn[j].add(curStr2[l]);                    // ...add KnotAtom reference to our nn list
          }
        }
      }
    }
  } // End of synchronized public void findNeighboursFN()


  // The removeOverlapsRO function(s) return true if any overlaps were found, false otherwise
  public boolean removeOverlapsRO()
  {
    return removeOverlapsRO(atomRadius, deltaParameter);
  }
  public boolean removeOverlapsRO(float radius, float delta)
  {
    boolean ret = false;
    for(int i=0; i<numStrands(); i++)
      ret |= removeOverlapsRO(i, radius, delta);
    return ret;
  }
  public boolean removeOverlapsRO(int strandNum, float radius, float delta)
  {
    return removeOverlapsRO(strandNum, radius, delta, (int)Math.floor(Math.random() * get(strandNum).length), Math.random()>=0.5);
  }
  synchronized public boolean removeOverlapsRO(int strandNum, float radius, float delta, int startAt, boolean goUpwards)
  {
//    algoProgress.append("-removeOverlapsRO("+strandNum+", "+radius+", "+delta+", "+startAt+", "+goUpwards+")");
    boolean ret = false;
    curStr = get(strandNum);
    Vector[] curNn = (Vector[])(nn.get(strandNum));
    Iterator iter;
    KnotAtom neighbour;
    float dx, dy, dz, d, ex, ey, ez, ddOver2;
    float R2 = radius * 2;
    float R2plusDelta = 2 * radius + delta;
    int i = startAt;
    for(int j=0; j<curStr.length; j++)
    {
      // Check if the current node is actually overlapping with any of the nodes it its "nn" list
      iter = curNn[i].iterator();
      while(iter.hasNext())
      {
        neighbour = (KnotAtom)iter.next();
        // Find distance
        dx = curStr[i].x - neighbour.x;
        dy = curStr[i].y - neighbour.y;
        dz = curStr[i].z - neighbour.z;
        d = dx*dx + dy*dy + dz*dz;
        if(d==0)
          d=0.000001f;
        if(d < R2)             // If there is a true overlap then remedy it
        {
          ret = true;
          ex = dx/d;
          ey = dy/d;
          ez = dz/d;
          ddOver2 = (R2plusDelta - d)/2;
          curStr[i].x -= ddOver2 * ex;
          curStr[i].y -= ddOver2 * ey;
          curStr[i].z -= ddOver2 * ez;
          neighbour.x += ddOver2 * ex;
          neighbour.y += ddOver2 * ey;
          neighbour.z += ddOver2 * ez;
        }
      }
      
      // Iterate up or down, depending on the chosen direction
      if(goUpwards) i=i++; else i--;
      // Wraparound
      if(i == -1)
        i = curStr.length-1;
      else if(i>=curStr.length)
        i = 0;
    }
//    algoProgress.append(" - returns "+ret+"\n");
    return ret;
  } // End of synchronized public boolean removeOverlapsRO(int strandNum, float radius, float delta, int startAt, boolean goUpwards)

  synchronized public void applyForces()
  {
//    algoProgress.append("-applyForces()");
    Iterator iter = strands.iterator();
    while(iter.hasNext())
    {
      curStr = (KnotAtom[])iter.next();
      for(int i=0; i<curStr.length; i++)
      {
        curStr[i].x += curStr[i].xf;
        curStr[i].y += curStr[i].yf;
        curStr[i].z += curStr[i].zf;
      }
    }
  }

  private int iterationsSoFar = 0;
  public void iterate()
  {
//    algoProgress.append("ITERATION "+iterationsSoFar+":\n");
    if((iterationsSoFar % 200) == 0)
      findNeighboursFN();
    iterationsSoFar++;
 
    // SONO = Shrink On No Overlap - therefore keep runnning RO until we have no overlaps, then run CL
    while(removeOverlapsRO())
    {
    }
    controlLeashesCL();

    // This stuff concerning forces (like a little motor inside each atom) is not in SONO. I added it.
    applyForces();
    
  }

  public String getAlgoProgress()
  {
    return algoProgress.toString();
  }
  public void resetAlgoProgress()
  {
    algoProgress = new StringBuffer();
  }

  public float getAtomRadius()
  {
    return atomRadius;
  }

  public KnotData getCopy()
  {
    try
    {
      return (KnotData)clone();
    }
    catch(CloneNotSupportedException e)
    {
      return null;
    }
  }
  
  synchronized public void interpolate()  // Inserts an atom imbetween each atom in a strand
  {
    nn = new Vector(1,1);
    for(int i=0; i<strands.size(); i++)
    {
      curStr = get(i);
      KnotAtom[] newStr = new KnotAtom[(curStr.length*2) - 1];
      newStr[0] = curStr[0];
      for(int j=1; j<curStr.length; j++)
      {
        newStr[j*2] = curStr[j];
        newStr[(j*2)-1] = new KnotAtom((curStr[j].x+curStr[j-1].x)/2,
                                       (curStr[j].y+curStr[j-1].y)/2,
                                       (curStr[j].z+curStr[j-1].z)/2,
                                       (curStr[j].xf+curStr[j-1].xf)/2,
                                       (curStr[j].yf+curStr[j-1].yf)/2,
                                       (curStr[j].zf+curStr[j-1].zf)/2
                                       );
      }
      strands.setElementAt(newStr, i);
      nn.add(new Vector[newStr.length]);
    }
    
    findNeighboursFN();
  }
  
  synchronized public void rethread() // Recreates the knot with correctly-spaced atoms along its path
  {
    nn = new Vector(1,1);
    float dx, dy, dz, segmentlength, unitvecx, unitvecy, unitvecz;
    KnotAtom[] newStr2;
    for(int i=0; i<strands.size(); i++)
    {
      curStr = get(i);
      if(curStr.length < 2)
        continue;
      Vector newStr = new Vector();

      int lowerpoint = 0;
      float posonseg = 0.0f;
      dx = curStr[0].x-curStr[1].x;
      dy = curStr[0].y-curStr[1].y;
      dz = curStr[0].z-curStr[1].z;
      segmentlength = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
      unitvecx = dx/segmentlength;
      unitvecy = dy/segmentlength;
      unitvecz = dz/segmentlength;
      do
      {
        System.out.println("Strand "+i+": Adding a point between "+lowerpoint+" and "+(lowerpoint+1));
        newStr.add(new KnotAtom(curStr[lowerpoint].x + posonseg * unitvecx,
                                curStr[lowerpoint].y + posonseg * unitvecy,
                                curStr[lowerpoint].z + posonseg * unitvecz ));
        posonseg += leashLength;
        if(posonseg >= segmentlength)
        {
          while(posonseg >= segmentlength)
          {
            posonseg -= segmentlength;
            lowerpoint++;
          }
          if(lowerpoint>= curStr.length-1)
            break;
          dx = curStr[lowerpoint].x-curStr[lowerpoint+1].x;
          dy = curStr[lowerpoint].y-curStr[lowerpoint+1].y;
          dz = curStr[lowerpoint].z-curStr[lowerpoint+1].z;
          segmentlength = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
          unitvecx = dx/segmentlength;
          unitvecy = dy/segmentlength;
          unitvecz = dz/segmentlength;
        }
      }
      while(true); //lowerpoint < (curStr.length-1));
      
      System.out.println("Finished a strand!");

      newStr2 = new KnotAtom[newStr.size()];
      for(int j=0; j<newStr.size(); j++)
        newStr2[j] = (KnotAtom)(newStr.get(j));

      strands.setElementAt(newStr2, i);
      nn.add(new Vector[newStr.size()]);
    }
    
    findNeighboursFN();
  }
}
