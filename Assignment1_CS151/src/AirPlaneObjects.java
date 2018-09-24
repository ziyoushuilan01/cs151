package reservationPackage;
import java.util.*;
public class AirPlaneObjects {
	public class Plane {
		private Seat[][] firstChart, ecoChart;
		private int firstRest, ecoRest;
		
		public Plane(int firstRow, int firstCol, int ecoRow, int ecoCol) {
			this.firstChart = fillSeats(firstRow, firstCol);
			this.ecoChart = fillSeats(ecoRow, ecoCol);
			this.firstRest = firstRow * firstCol;
			this.ecoRest = ecoRow * ecoCol;
		}
		
		private Seat[][] fillSeats(int row, int col) {
			Seat[][] seats = new Seat[row][col];
			for (int i = 0; i < row; i++) {
				for (int j = 0; j < col; j++) {
					char label = (char)(j + 'A');
					if (j == 0 || j == col - 1) {
						seats[i][j] = new Seat(i, j, label, 'W');
					} else if (j == col / 2 - 1 || j == col / 2) {
						seats[i][j] = new Seat(i, j, label, 'A');
					} else {
						seats[i][j] = new Seat(i, j, label, 'C');
					}
				}
			}
			return seats;
		}
	}
	
	public class Seat{
		private boolean isTaken;
		private int row, col;
		private char label, pref;
		public Seat(int row, int col, char label, char pref) {
			this.isTaken = false;
			this.row = row;
			this.col = col;
			this.label = label;
			this.pref = pref;
		}
	}
	
	public class Individual {
		private String name;
		private char preference;
		private boolean isEconomy;
		private int row, col;
		
		public Individual(String name, char preference, boolean isEconomy) {
			this.name = name;
			this.preference = preference;
			this.isEconomy = isEconomy;
			this.row = -1;
			this.col = -1;
		}
		
		private boolean reserve(Plane plane) {
			if (isEconomy && plane.ecoRest == 0 || !isEconomy && plane.firstRest == 0) {
				return false;
			} else {
				Seat[][] chart = isEconomy ? plane.ecoChart : plane.firstChart;
				for (int i = 0; i < chart.length; i++) {
					for (int j = 0; j < chart[0].length; j++) {
						if (!chart[i][j].isTaken && chart[i][j].pref == preference) {
							row = i;
							col = j;
							if (isEconomy) {
								plane.ecoRest--;
							} else {
								plane.firstRest--;
							}
							break;
						}
					}
				}
				return true;
			}			
		}
		
		private boolean cancel(Plane plane) {
			if (row == -1 && col == -1) {
				return false;
			} else {
				if (isEconomy) {
					plane.ecoChart[row][col].isTaken = false;
					plane.ecoRest++;
				} else {
					plane.firstChart[row][col].isTaken = false;
					plane.firstRest++;
				}
				row = -1;
				col = -1;
				return true;
			}
		}
	}
	
	public class Group {
		private String groupName;
		private List<String> names;
		private List<int[]> indices;
		private boolean isEconomy;
		
		private boolean reserve(Plane plane) {
			Seat[][] chart;
			if (isEconomy) {
				if (plane.ecoRest < names.size()) {
					return false;
				} else {
					chart = plane.ecoChart;
					plane.ecoRest -= names.size();
				}
			} else {
				if (plane.firstRest < names.size()) {
					return false;
				} else {
					chart = plane.firstChart;
					plane.firstRest -= names.size();
				}
			}
			
			/* Try to find a row with consecutive seats that can place all passengers in this group.
			   If not able to find such a line, find out all the consecutive seats and place as many
			   passengers as possible in one time */
			int needed = names.size();
			List<List<int[]>> emptySlots = new ArrayList<>(); // each slot records the starting empty seat and the number of adjacent empty seats
			for (int i = 0; i < chart.length; i++) {
				emptySlots.add(new ArrayList<>());
				int consecutiveSeats = 0;
				for (int j = 0; j < chart[0].length; j++) {
					if (!chart[i][j].isTaken) {
						consecutiveSeats++;
					}
					if (consecutiveSeats == needed) {
						for (int k = needed - 1; k >= 0; k--) {
							indices.add(new int[]{i, j - k});
							chart[i][j-k].isTaken = true;
						}
						return true;
					}
					if (consecutiveSeats > 0 && (j == chart[0].length - 1 || chart[i][j].isTaken)) {
						if (chart[i][j].isTaken) {
							emptySlots.get(i).add(new int[]{j - consecutiveSeats, consecutiveSeats});
						} else if (j == chart[0].length - 1) {
							emptySlots.get(i).add(new int[]{j + 1 - consecutiveSeats, consecutiveSeats});
						}
					}
				}
				
			}
			findMaxAndAssign(emptySlots, chart, indices, needed);
			return true;
		}
		
		// assign the largest number of adjacent seats if unable to assign all seats in a row
		private void findMaxAndAssign(List<List<int[]>> emptySlots, Seat[][] chart, List<int[]> indices, int needed) {
			int max = 0, startRow = -1, startCol = -1;
			int[] toRemoveSlot = null;
			for (int i = 0; i < emptySlots.size(); i++) {
				for (int[] slot : emptySlots.get(i)) {
					if (slot[1] >= needed) {
						assign(chart, indices, needed, i, slot[0]);
						return;
					} else {
						if (slot[1] > max) {
							max = slot[1];
							startRow = i;
							startCol = slot[0];
							toRemoveSlot = slot;
						}
					}
				}
			}
			assign(chart, indices, max, startRow, startCol);
			emptySlots.get(startRow).remove(toRemoveSlot);
			findMaxAndAssign(emptySlots, chart, indices, needed - max);
			return;
		}
		
		// assign seats to passengers, 
		private void assign(Seat[][] chart, List<int[]> indices, int needed, int row, int startCol) {
			for (int col = startCol; col < startCol + needed; col++) {
				chart[row][col].isTaken = true;
				indices.add(new int[]{row, col});
			}
			return;
		}
		
		private boolean cancel(Plane plane) {
			if (indices.isEmpty()) {
				return false;
			} else {
				Seat[][] chart;
				if (isEconomy) {
					chart = plane.ecoChart;
					plane.ecoRest += names.size();
				} else {
					chart = plane.firstChart;
					plane.firstRest += names.size();
				}
				for (int[] index : indices) {
					chart[index[0]][index[1]].isTaken = false;
				}
				indices.clear();
				return true;
			}
		}
		
	}
}
