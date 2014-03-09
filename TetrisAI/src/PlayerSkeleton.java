
public class PlayerSkeleton {
	private double holeWeight = -7.899265427351652;
	private double heightWeight = -4.500158825082766;
	private double clearedWeight = 3.4181268101392694;
	private double wellsWeight = -3.3855972247263626;
	private double rtWeight = -3.2178882868487753;
	private double ctWeight = -9.348695305445199;
	
	private int landingHeight = 0;
	private int[] top;
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
			int rowsCleared = tryMove(gameTry, s, piece, orient, slot);
			if(rowsCleared >= 0){
				double eval = evaluateMove(gameTry, rowsCleared);
				if(eval > highest){
					highest = eval;
					bestMove = i;
				}
			}
		}
		return bestMove;
	}
	double evaluateMove(int[][] result, int rowsCleared){
		double utility = clearedWeight * rowsCleared 
				+ holeWeight * getNumberOfHoles(result)
				+ heightWeight * landingHeight
				+ wellsWeight * getWells(result)
				+ rtWeight * getNumRowTransitions(result)
				+ ctWeight * getNumColTransitions(result);
		return utility;
	}
	int getNumberOfHoles(int[][] result){
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
	
	int getTopWellDepth(int col, int topRow, int[][] result){
		int total = 0;
		for(int i = topRow; i >= 0; i--){
			if(result[i][col] > 0){
				break;
			}else{
				total += 1;
			}
		}
		return total;
	}
	int cumulateWellDepth(int col, int[][] result){
		int total = 0;
		int currDepth = 0;
		int level = 0;
		for(int i = top[col] - 1; i >= 0; i--){
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
	int getWells(int[][] result){
		//Based on the interesting calculation in eltetris
		//TODO:
		int total = 0;
		for(int col = 0; col < State.COLS; col ++){
			total += cumulateWellDepth(col, result);
		}
		return total;
	}
	
	int getNumRowTransitions(int[][] result){
		int total = 0;
		for(int row = 0; row < State.ROWS; row++){
			if(result[row][0] > 0){
				total += 1;
			}
			for(int col = 1; col < State.COLS - 1; col++){
				if(result[row][col]> 0 && result[row][col + 1] == 0 
						|| result[row][col] == 0 && result[row][col + 1] > 0){
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
				if(result[row][col] != result[row + 1][col]){
					total += 1;
				}
			}
		}
		return total;
	}
	
	private int tryMove(int[][] field, State s, int nextPiece, int orient, int slot){
		//Returns the number of rows cleared. -1 means game over.
		int rowsCleared = 0;
		top = s.getTop().clone();
		int[][][] pBottom = State.getpBottom();
		int[][][] pTop = State.getpTop();
		int [][] pHeight = State.getpHeight();
		int[][] pWidth = State.getpWidth();
		int height = top[slot]-pBottom[nextPiece][orient][0];
		int turn = s.getTurnNumber() + 1;
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
//			System.out.println("slot " + top[slot+c] + " offset "+pBottom[nextPiece][orient][c]);
//			System.out.println(c +" compare height "+ height + " "+(top[slot+c]-pBottom[nextPiece][orient][c]) + " bottom "+ pBottom[nextPiece][orient][c]);
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}
		//check if game ended
		int pieceHeight = pHeight[nextPiece][orient];
		landingHeight = height + pieceHeight/2;
		if(height+pieceHeight >= State.ROWS) {
			return -1;
		}
		
		//for each column in the piece - fill in the appropriate blocks
				for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
					//from bottom to top of brick
					for(int h = (height+pBottom[nextPiece][orient][i]); h < height+pTop[nextPiece][orient][i]; h++) {
//						System.out.println(i + " height "+ h + " max "+ height + " "+ pBottom[nextPiece][orient][i]);
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
		return rowsCleared;
	}
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}
