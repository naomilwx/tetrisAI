/*
 * Possible feature extensions: 
 *  Difference between max and min height
 *  Surface "smoothness" difference between neighbouring heights
 *  Potential Rows: The number of rows located above the Highest Hole and in use by more than 8 
cells. ??? taken from http://ijcsi.org/papers/IJCSI-8-1-22-31.pdf
 * */
public class PlayerSkeleton {
	private double holeWeight = -7.899265427351652;
	private double heightWeight = -4.500158825082766;
	private double clearedWeight = 3.4181268101392694;
	private double wellsWeight = -3.3855972247263626;
	private double rtWeight = -3.2178882868487753;
	private double ctWeight = -9.348695305445199;
	
	public static final int NUM_OF_DIMENSIONS = 6;
//	private int landingHeight = 0;
//	private int[] top;
	private static int CLEARED_INDEX = 0;
	private static int LANDING_INDEX = 1;
	
	public PlayerSkeleton(){
	}
	public PlayerSkeleton(double position[]){
		holeWeight = position[0];
		heightWeight = position[1];
		clearedWeight = position[2];
		wellsWeight = position[3];
		rtWeight = position[4];
		ctWeight = position[5];
	}
	//implement this function to have a working system
	public void copy2DArray(int[][] arrayFrom, int[][]arrayTo){
		for(int i = 0; i<arrayFrom.length; i++){
			for(int j = 0; j < arrayFrom[i].length; j++){
				arrayTo[i][j] = arrayFrom[i][j];
			}
		}
	}
	public int pickMove(State s, int[][] legalMoves) {
		//TODO:
		int piece = s.getNextPiece();
		double highest = Double.NEGATIVE_INFINITY;
		int bestMove = 0;
		int[][] gameField = s.getField();

		for(int i = 0; i<legalMoves.length; i++){
			int orient = legalMoves[i][State.ORIENT];
			int slot = legalMoves[i][State.SLOT];
			int[][] gameTry = new int[State.ROWS][State.COLS];
			copy2DArray(gameField, gameTry);
			int[] top = s.getTop().clone();
			int[] moveResult = tryMove(gameTry, top, s, piece, orient, slot);
			if(moveResult != null){
				int rowsCleared = moveResult[CLEARED_INDEX];
				int landingHeight = moveResult[LANDING_INDEX];
				double eval = evaluateMove(gameTry, rowsCleared, top, landingHeight);
				if(eval > highest){
					highest = eval;
					bestMove = i;
				}
			}
		}
		return bestMove;
	}
	double evaluateMove(int[][] result, int rowsCleared, int[] top, int landingHeight){
		double utility = clearedWeight * rowsCleared 
				+ holeWeight * getNumberOfHoles(result, top)
				+ heightWeight * landingHeight
				+ wellsWeight * getWells(result, top)
				+ rtWeight * getNumRowTransitions(result)
				+ ctWeight * getNumColTransitions(result);
		return utility;
	}
	int getNumberOfHoles(int[][] result, int[] top){
		int totalHoles = 0;
		for(int col = 0; col < State.COLS; col++){
			for(int row = 0; row < top[col] - 1; row++){
				if(result[row][col] == 0){
					totalHoles += 1;
				}
			}
		}
		return totalHoles;
	}
	int getMaximumHeight(int[] top){
		int highest = 0;
		for(int i = 0; i < top.length; i++){
			if(top[i] > highest){
				highest = top[i];
			}
		}
		return highest;
	}
	int getTopWellDepth(int col, int topRow, int[][] result){
		int total = 0;
		for(int i = topRow; i >= 0; i--){
			if(result[i][col] > 0){
				break;
			}else{
				total += 1;
			}
		}
//		System.out.println("col "+col + " depth "+ total);
		return total;
	}
	
	int cumulateWellDepth(int col, int[][] result, int highest){
		int total = 0;
		int currDepth = 0;
		int level = 0;
//		int highest = getMaximumHeight(top);
		for(int i = highest - 1; i >= 0; i--){
			level += 1;
			if(result[i][col] > 0){
				currDepth = 0;
				continue;
			}else{
				if(hasNeighbours(col, i, result)){
					if(currDepth == 0){
						currDepth = getTopWellDepth(col, i, result);
						level = 0;
						total += (currDepth * (currDepth + 1))/2; 
					}else{
						total += ((currDepth - level) * (currDepth - level + 1)) / 2;
					}
				}
			}
		}
		return total;
	}
	boolean hasNeighbours(int col, int height, int[][] result){
		if(col == 0){
			return result[height][col + 1] > 0;
		}else if(col == State.COLS -1){
			return result[height][col - 1] > 0;
		}else{
			return result[height][col + 1] > 0 && result[height][col - 1] > 0;
		}
	}
	int getWells(int[][] result, int[] top){
		//Based on the interesting calculation in eltetris
		//TODO:
		int total = 0;
		int highest = getMaximumHeight(top);
		for(int col = 0; col < State.COLS; col ++){
			total += cumulateWellDepth(col, result, highest);
		}
//		System.out.println("wells "+ total);
		return total;
	}
	
	int getNumRowTransitions(int[][] result){
		int total = 0;
		for(int row = 0; row < State.ROWS; row++){
			if(result[row][0] > 0){
				total += 1;
			}
			for(int col = 1; col < State.COLS - 1; col++){
				if((result[row][col]> 0 && result[row][col + 1] == 0) 
						|| (result[row][col] == 0 && result[row][col + 1] > 0)){
					total += 1;
				}
			}
		}
		return total;
	}
	
	int getNumColTransitions(int[][] result){
		int total = 0;
		for(int col = 0; col < State.COLS; col++){
			if(result[0][col] > 0){
				total += 1;
			}
			for(int row = 1; row < State.ROWS - 1; row++){
				if((result[row][col] > 0 && result[row + 1][col] == 0) 
						|| (result[row][col] == 0 && result[row + 1][col] > 0)){
					total += 1;
				}
			}
		}
		return total;
	}
	
	private int[] tryMove(int[][] field, int[] top, State s, int nextPiece, int orient, int slot){
		//Returns array containing the number of rows cleared, the array top and landing height
		//Returns null if game is lost
		int rowsCleared = 0;
//		top = s.getTop().clone();
		int[][][] pBottom = State.getpBottom();
		int[][][] pTop = State.getpTop();
		int [][] pHeight = State.getpHeight();
		int[][] pWidth = State.getpWidth();
		int height = top[slot]-pBottom[nextPiece][orient][0];
		int turn = s.getTurnNumber() + 1;
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}
		//check if game ended
		int pieceHeight = pHeight[nextPiece][orient];
		int landingHeight = height + pieceHeight/2;
		if(height+pieceHeight >= State.ROWS) {
			return null;
		}
		
		//for each column in the piece - fill in the appropriate blocks
				for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
					//from bottom to top of brick
					for(int h = (height+pBottom[nextPiece][orient][i]); h < height+pTop[nextPiece][orient][i]; h++) {
						field[h][i+slot] = turn;
					}
				}
				
				//adjust top
				for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
					top[slot+c]=height+pTop[nextPiece][orient][c];
				}
								
				//check for full rows - starting at the top
				for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
					//check all columns in the row
					boolean full = true;
					for(int c = 0; c < State.COLS; c++) {
						if(field[r][c] == 0) {
							full = false;
							break;
						}
					}
					//if the row was full - remove it and slide above stuff down
					if(full) {
						rowsCleared++;
						//for each column
						for(int c = 0; c < State.COLS; c++) {

							//slide down all bricks
							for(int i = r; i < top[c]; i++) {
								field[i][c] = field[i+1][c];
							}
							//lower the top
							top[c] -= 1;
							while(top[c]>=1 && field[top[c]-1][c]==0)	top[c] -= 1;
						}
					}
				}
		int[] retArr = {rowsCleared, landingHeight};
		return retArr;
	}
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		double[] arr = {-15.0, -7.657493030930552, 15.0, -4.106062205383264, -3.522734028676709, -15.0};
		PlayerSkeleton p = new PlayerSkeleton(arr);
//		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			System.out.println(s.getRowsCleared()+" rows.");
//			s.draw();
//			s.drawNext(0,0);
//			try {
//				Thread.sleep(300);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
		s.draw();
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}
