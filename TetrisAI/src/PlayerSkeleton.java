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
	private double lookaheadFactor = 0.9;
	int[] pieceHistory;
	int totalPieces;
	
	private static boolean LOOKAHEAD = true; //Set this to false to turn off lookahead
	public static final int NUM_OF_DIMENSIONS = 6;

	private static int CLEARED_INDEX = 0;
	private static int LANDING_INDEX = 1;
	private static int BEST_MOVE_INDEX = 1;
	private static int BEST_MOVE_EVAL_INDEX = 0;
	
	public PlayerSkeleton(){
		pieceHistory = new int[State.N_PIECES];
		totalPieces = 0;
	}
	public PlayerSkeleton(double position[]){
		/*
		 * Initialise with array of weights. Use this to train player
		 * */
		holeWeight = position[0];
		heightWeight = position[1];
		clearedWeight = position[2];
		wellsWeight = position[3];
		rtWeight = position[4];
		ctWeight = position[5];
		pieceHistory = new int[State.N_PIECES];
		totalPieces = 0;
		if(position.length == 7){
			lookaheadFactor = position[6];
		}
	}

	public void copy2DArray(int[][] arrayFrom, int[][]arrayTo){
		/*
		 * Does a deep copy of a 2D array
		 * */
		for(int i = 0; i<arrayFrom.length; i++){
			for(int j = 0; j < arrayFrom[i].length; j++){
				arrayTo[i][j] = arrayFrom[i][j];
			}
		}
	}
	
	public double lookAheadPieceWeight(int piece){
		return (double)pieceHistory[piece]/totalPieces;
	}
	
	public int pickMove(State s, int[][] legalMoves) {
		int piece = s.getNextPiece();
		pieceHistory[piece] += 1;
		totalPieces += 1;
		
		int[][] gameField = s.getField();
		int turn = s.getTurnNumber() + 1;
		double[] evaluatedMove = evaluateMovesForPiece(gameField, s.getTop(), turn, piece, LOOKAHEAD);
		int bestMove = (int)evaluatedMove[BEST_MOVE_INDEX];
		return bestMove;
	}
	
	
	public double[] evaluateMovesForPiece(int[][] field, int[] originalTop, int turn, int piece, boolean lookAhead){
		//field and originalTop is cloned in this function. 
		//so the state of the gameboard is not modified by this function
		/*
		 * Evaluates the best possible move, based on the utility for the given piece
		 * @param field: 2D array representing the gamestate
		 * @param originalTop: 1D array representing the top (height of each row)
		 * @param turn: number of turns made before current move
		 * @param piece: the piece to move
		 * @param looakahead: boolean flag to indicate whether to look 1 step ahead for the evaluation
		 * */
		int[][] legalMoves = State.legalMoves[piece];
		double highest = Double.NEGATIVE_INFINITY;
		int bestMove = 0;
		for(int move = 0; move < legalMoves.length; move++){
			int orient = legalMoves[move][State.ORIENT];
			int slot = legalMoves[move][State.SLOT];
			int[][] gameTry = new int[State.ROWS][State.COLS];
			copy2DArray(field, gameTry);
			int [] top = originalTop.clone();
			int[] moveResult = tryMove(gameTry, top, turn, piece, orient, slot);//at this point gameTry has been mutated to the state after the first move
			turn  += 1;
			if(moveResult != null){
				int rowsCleared = moveResult[CLEARED_INDEX];
				int landingHeight = moveResult[LANDING_INDEX];
				double eval = evaluateMoveResult(gameTry, rowsCleared, top, landingHeight);
				if(lookAhead){
					eval += evaluateNextMove(gameTry, top, turn);
					turn += 1;
				}
				if(eval > highest){
					highest = eval;
					bestMove = move;
				}
			}
		}
		double[] moveResult = new double[2];
		moveResult[BEST_MOVE_EVAL_INDEX] = highest;
		moveResult[BEST_MOVE_INDEX] = bestMove;
		return moveResult;
	}
	
	public double evaluateNextMove(int[][] field, int[] top, int turn){
		//returns the utility of the best possible next move (lookahead)
		/*
		 * @param field: gamestate
		 * @param top: height of each colum
		 * @param turn: number of turns befor move
		 * */
		double total = 0;
		for(int i = 0; i < State.N_PIECES; i++){
			double[] bestMoveResult = evaluateMovesForPiece(field, top, turn, i, false);
			double pieceWeight = lookAheadPieceWeight(i);
			if(pieceWeight > 0){
				total += bestMoveResult[BEST_MOVE_EVAL_INDEX]
					* pieceWeight;
			}
			//At this point field is NOT mutated
		}
		return total * lookaheadFactor;
	}
	
	double evaluateMoveResult(int[][] result, int rowsCleared, int[] top, int landingHeight){
		/*
		 * Returns the utility value of the current game state
		 * @param result: gamestate
		 * 
		 * */
		double utility = clearedWeight * rowsCleared 
				+ holeWeight * getNumberOfHoles(result, top)
				+ heightWeight * landingHeight
				+ wellsWeight * getWells(result, top)
				+ rtWeight * getNumRowTransitions(result)
				+ ctWeight * getNumColTransitions(result);
		return utility;
	}
	
	int getNumberOfHoles(int[][] result, int[] top){
		/*
		 * Feature: Returns the number of holes in the game board
		 * */
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
		/*
		 * Feature: Returns the maximum column height of the game board
		 * */
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
		return total;
	}
	
	int cumulateWellDepth(int col, int[][] result, int highest){
		int total = 0;
		int currDepth = 0;
		int level = 0;
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
		/*
		 * Feature: "Well depth". With heavy penalty for deeper wells
		 * */
		//Based on the interesting calculation in eltetris
		int total = 0;
		int highest = getMaximumHeight(top);
		for(int col = 0; col < State.COLS; col ++){
			total += cumulateWellDepth(col, result, highest);
		}
		return total;
	}
	
	int getNumRowTransitions(int[][] result){
		/*
		 * Feature: # of row transitions
		 * */
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
		/*
		 * Feature: # of column transitions
		 * */
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
	
	private int[] tryMove(int[][] field, int[] top, int turn, int nextPiece, int orient, int slot){
		//NOTE: TryMove mutates field and top!
		//Returns array containing the number of rows cleared, the array top and landing height
		//Returns null if game is lost
		int rowsCleared = 0;
		int[][][] pBottom = State.getpBottom();
		int[][][] pTop = State.getpTop();
		int [][] pHeight = State.getpHeight();
		int[][] pWidth = State.getpWidth();
		int height = top[slot]-pBottom[nextPiece][orient][0];
		
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
//		double[] arr = {-5.003646727088223, -6.832461269940281, -3.750335096683833, -2.6719579714199857, -1.8484064582162885, -10.28934027117086};
//		double[] arr = {-15.0, -7.657493030930552, 15.0, -4.106062205383264, -3.522734028676709, -15.0};
//		PlayerSkeleton p = new PlayerSkeleton(arr);
		PlayerSkeleton p = new PlayerSkeleton();
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
