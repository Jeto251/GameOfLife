package com.jeto.game;

public class Utils {

   public static int binToInt(String str) {
      return Integer.parseInt(str, 2);
   }

   public static String intToBin(int i) {
      return String.format("%9s", Integer.toBinaryString(i)).replace(' ', '0');
   }
}
