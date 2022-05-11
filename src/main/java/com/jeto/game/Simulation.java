package com.jeto.game;

public class Simulation {
   private final int width;
   private final int height;

   private int[][] board;

   public Simulation(int width, int height) {
	  this.width = width;
	  this.height = height;

	  this.board = new int[width][height];
   }

   public int getWidth() {
	  return width;
   }

   public int getHeight() {
	  return height;
   }

   public void printBoard() {
	  for (int y = 0; y < height; y++) {
		 StringBuilder line = new StringBuilder("|");
		 for (int x = 0; x < width; x++) {
			if (this.board[x][y] == 0) {
			   line.append(".");
			} else {
			   line.append("*");
			}
		 }
		 line.append("|");
		 System.out.println(line);
	  }
	  System.out.println("---\n");
   }

   public void setAlive(int x, int y) {
	  setState(x, y, 1);
   }

   public void setDead(int x, int y) {
	  setState(x, y, 0);
   }

   public int countAliveNeibs(int x, int y) {
	  int count = 0;

	  count += getState(x - 1, y - 1);
	  count += getState(x, y - 1);
	  count += getState(x + 1, y - 1);

	  count += getState(x - 1, y);
	  count += getState(x + 1, y);

	  count += getState(x - 1, y + 1);
	  count += getState(x, y + 1);
	  count += getState(x + 1, y + 1);

	  return count;
   }

   public int getState(int x, int y) {
	  if (x < 0 || x >= width || y < 0 || y >= height) {
		 return 0;
	  }

	  return this.board[x][y];
   }

   public void setState(int x, int y, int state) {
	  if (x < 0 || x >= width || y < 0 || y >= height) {
		 return;
	  }
	  this.board[x][y] = state;
   }

   public boolean isDead(int x, int y) {
	  return getState(x, y) == 0;
   }

   public void step() {
	  int[][] newBoard = new int[width][height];

	  for (int y = 0; y < height; y++) {
		 for (int x = 0; x < width; x++) {
			int countAliveNeibs = countAliveNeibs(x, y);

			// Если клетка жива
			if (!isDead(x, y)) {
			   // И у нее меньше 2х соседей
			   if (countAliveNeibs < 2) {
				  // Умирает
				  newBoard[x][y] = 0;
				  // Если 2 или 3 соседа - живет
			   } else if (countAliveNeibs == 2 || countAliveNeibs == 3) {
				  newBoard[x][y] = 1;
			   } else {
				  // Если больше 3х соседей, то умирает
				  newBoard[x][y] = 0;
			   }
			} else {
			   if (countAliveNeibs == 3) {
				  newBoard[x][y] = 1;
			   }
			}
		 }
	  }

	  this.board = newBoard;
   }

   public static void main(String[] args) {
	  Simulation simulation = new Simulation(8, 5);

	  for (int i = 0; i < 3; i++) {
		 simulation.printBoard();
		 simulation.step();
	  }
   }
}
