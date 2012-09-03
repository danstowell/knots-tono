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

import java.io.*;

public class KnotDataTester
{
  private static String filePath="KnotDataTest01";
  public static void main(String[] args)
  {
    if(args.length==1)
      filePath = args[0];
    else if(args.length>1)
    {
      System.out.println("Only one argument please: the data file path");
      System.exit(1);
    }
    
    try
    {
      FileInputStream fis = new FileInputStream(filePath);
      KnotData kd = new KnotData(fis);
      
      System.out.println(kd);
      kd.iterate();
      System.out.println(kd.getAlgoProgress());
      System.out.println(kd);
    }
    catch(IOException e)
    {
      System.out.println("IOException while initialising KnotData: " + e);
    }
  }
}
