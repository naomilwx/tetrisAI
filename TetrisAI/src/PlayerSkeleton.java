
public class PlayerSkeleton {
	private int holeWeight;
	private int heightWeight;
	private int clearedWeight;
	private int wellsWeight;
	private int rtWeight;
	private int ctWeight;
	
	private int landingHeight = 0;
	private int[] top;
	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		//TODO:
		int piece = s.getNextPiece();
		int highest = 0;
		int bestMove = -1;
		int[][] gameField = s.getField();

		for(int i = 0; i<legalMoves.length; i++){
			int orient = legalMoves[i][State.ORIENT];
			int slot = legalMoves[i][State.SLOT];
			int[][] gameTry = gameField.clone();
			int rowsCleared = tryMove(gameTry, s, piece, orient, slot);
			if(rowsCleared >= 0){
				int eval = evaluateMove(gameTry, rowsCleared);
				if(eval > highest){
					bestMove = i;
				}
			}
		}
		return bestMove;
	}
	int evaluateMove(int[][] result, int rowsCleared){
		int utility = clearedWeight * rowsCleared 
				+ holeWeight * getNumberOfHoles(result)
				+ heightWeight * landingHeight
				+ wellsWeight * getWells(result)
				+ rtWeight * getNumRowTransitions(result)
				+ ctWeight * getNumColTransitions(result);
		return utility;
	}
	int getNumberOfHoles(int[][] result){
		//TODO:
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
	
	
	int getWells(int[][] result){
		//TODO:
		return 0;
	}
	
	int getNumRowTransitions(int[][] result){
		//TODO:
		return 0;
	}
	
	int getNumColTransitions(int[][] result){
		//TODO:
		return 0;
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
					for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
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
							top[c]--;
							while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
						}
					}
				}
		return rowsCleared;
	}
	/*
	 * public boolean makeMove(int orient, int slot) {
		turn++;
		//height if the first column makes contact
		int height = top[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}
		//check if game ended
		if(height+pHeight[nextPiece][orient] >= ROWS) {
			lost = true;
			return false;
		}

		
		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			
			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = turn;
			}
		}
		
		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+pTop[nextPiece][orient][c];
		}
		
		int rowsCleared = 0;
		
		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < COLS; c++) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				rowsCleared++;
				cleared++;
				//for each column
				for(int c = 0; c < COLS; c++) {

					//slide down all bricks
					for(int i = r; i < top[c]; i++) {
						field[i][c] = field[i+1][c];
					}
					//lower the top
					top[c]--;
					while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
				}
			}
		}
		//pick a new piece
		nextPiece = randomPiece();
		return true;
	}
	 * */
	
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
