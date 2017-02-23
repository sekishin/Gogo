package data.strategy.user.s14t242;

import sys.game.GameBoard;
import sys.game.GameCompSub;
import sys.game.GameHand;
import sys.game.GamePlayer;
import sys.game.GameState;
import sys.struct.GogoHand;
import sys.user.GogoCompSub;

public class User_s14t242_02 extends GogoCompSub {

	//====================================================================
	//  コンストラクタ
	//====================================================================

	public User_s14t242_02(GamePlayer player) {
		super(player);
		name = "s14t242";    // プログラマが入力

	}

	//--------------------------------------------------------------------
	//  コンピュータの着手
	//--------------------------------------------------------------------

	public synchronized GameHand calc_hand(GameState state, GameHand hand) {
		theState = state;
		theBoard = state.board;
		lastHand = hand;

		//--  置石チェック
		init_values(theState, theBoard);

		//--  評価値の計算
		calc_values(theState, theBoard);
		// 先手後手、取石数、手数(序盤・中盤・終盤)で評価関数を変える

		//--  着手の決定
		return deside_hand();

	}

	//----------------------------------------------------------------
	//  置石チェック
	//----------------------------------------------------------------

	public void init_values(GameState prev, GameBoard board) {
		this.size = board.SX;
		values = new int[size][size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (board.get_cell(i, j) != board.SPACE) {
					values[i][j] = -2;
				} else {
					if (values[i][j] == -2) {
						values[i][j] = 0;
					}
				}
			}
		}
	}

	//----------------------------------------------------------------
	//  評価値の計算
	//----------------------------------------------------------------

	public void calc_values(GameState prev, GameBoard board) {
		int mycolor = role;
		negamax(prev, mycolor, 1);
	}

	int negamax(GameState nowState, int mycolor, int depth) {
		GameBoard nowBoard = nowState.board;
		int[][] cell = nowBoard.get_cell_all();
		int nowTurn = nowState.turn;
		GogoHand tmpHand = new GogoHand();
		GameState nextState;
		int[][] eval = new int[size][size];

		if ( depth == 1 ) { return evaluation(nowState); }
		for ( int i = 0; i < size; i++ ) {
			for ( int j =0 ; j < size; j++ ) {
				if ( cell[i][j] != nowBoard.SPACE ) { continue; }
				tmpHand.set_hand(i, j);
				nextState = nowState.test_hand(tmpHand);
				eval[i][j] = negamax(nextState, mycolor, depth-1);
				if ( nowTurn != mycolor ) { eval[i][j] *= -1; }
			}
		}
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				values[i][j] = eval[i][j];
			}
		}
		return max(eval);
	}

	int evaluation(GameState prev) {
		GameBoard board = prev.board;
		int gettenStones = get_mystone(prev);	// 取った石の個数
		int stolenStones = get_enemystone(prev);	// 取られた石の個数
		GogoHand tmpHand = new GogoHand();
		GameState tmpState;
		int mycolor = prev.turn;
		int[][] cell = board.get_cell_all();
		init_values(prev, board);
		System.out.println(mycolor + " " + mycolor*-1);
		System.out.println(gettenStones + " " + stolenStones);
		//--  各マスの評価値
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				// 埋まっているマスはスルー
				if (values[i][j] == -2) { continue; }
				//--  適当な評価の例
				// 三々の禁じ手は打たない → -1
				if ( check_33(cell, mycolor, i, j) ) {
					values[i][j] = -1;
					continue;
				}
				// 相手の五連を崩す → 1000;
				if ( check_run_5(cell, mycolor*-1) ) {
					if ( check_rem(cell, mycolor*-1, i, j) ) {
						tmpHand.set_hand(i, j);
						tmpState = prev.test_hand(tmpHand);
						if ( ! check_run_5(tmpState.board.get_cell_all(), mycolor*-1) ) {
							values[i][j] = 1000;
							continue;
						}
					}
				}
				// 勝利(五取) → 950;
				if ( gettenStones == 8 && check_rem(cell, mycolor*-1, i, j) ) {
					values[i][j] = 950;
					continue;
				}
				// 勝利(五連) → 900;
				if ( check_run(cell, mycolor, i, j, 5, false) || check_run2(cell, mycolor, i, j, 5, false) ) {
					values[i][j] = 900;
					continue;
				}
				// 敗北阻止(五取) → 850;
				if ( stolenStones == 8 && check_rem_all(cell, mycolor) ) {
					tmpHand.set_hand(i, j);
					tmpState = prev.test_hand(tmpHand);
					if ( ! check_rem_all(tmpState.board.get_cell_all(), mycolor) ) {
						values[i][j] = 850;
						continue;
					}
				}
				// 敗北阻止(五連) → 800;
				if ( check_run(cell, mycolor*-1, i, j, 5, false) || check_run2(cell, mycolor*-1, i, j, 5, false) ) {
					values[i][j] = 800;
					continue;
				}
				// 相手の四連を止める → 700;
				if ( check_run(cell, mycolor*-1, i, j, 4, true) || check_run2(cell, mycolor*-1, i, j, 4, true) ) {
					values[i][j] = 700;
					continue;
				}
				// 自分の石を守る → 650;
				if ( check_rem_all(cell, mycolor) ) {
					if ( check_rem(cell, mycolor, i, j) ) {
						values[i][j] = 650;
						continue;
					}
					tmpHand.set_hand(i, j);
					tmpState = prev.test_hand(tmpHand);
					if ( ! check_rem_all(tmpState.board.get_cell_all(), mycolor) ) {
						values[i][j] = 650;
						continue;
					}
				}
				// 自分の三四を作る
				if ( check_34(cell, mycolor, i, j) ) {
					values[i][j] = 630;
					continue;
				}

				// 自分の四連を作る → 600;
				if ( check_run(cell, mycolor, i, j, 4, true)  || check_run2(cell, mycolor, i, j, 4, true) ) {
					values[i][j] = 600;
					continue;
				}
				// 相手の石を取る → 550;
				if ( check_rem(cell, mycolor*-1, i, j) ) {
					values[i][j] = 550;
					continue;
				}
				// 自分の三連を作る → 450;
				if ( check_run(cell, mycolor, i, j, 3, true) || check_run2(cell, mycolor, i, j, 3, true) ) {
					values[i][j] = 450;
					continue;
				}
				// 自分の飛び三を作る → 500
				if ( check_tobi_3(cell, mycolor, i, j) ) {
					values[i][j] = 500;
					continue;
				}
				// 相手の三連を防ぐ → 400;
				if ( check_run(cell, mycolor*-1, i, j, 3, true)  || check_run2(cell, mycolor*-1, i, j, 3, true)) {
					values[i][j] = 400;
					continue;
				}
				// ランダム
				if (values[i][j] == 0) {
					values[i][j] = get_weight_value(i, j);
					if ( check_run2(cell, mycolor, i, j, 2, true) ) {
						values[i][j] = 2;
					}
					if ( values[i][j] == 10 && check_keima(cell, mycolor, i, j) ) {
						values[i][j] = 15;
					}
				}
				// 四々や四三の判定
				// 飛び三や飛び四の判定
				// 三をどちらで止めるか
			}
		}
		show_value();
		return max(values);

	}

	int max(int[][] array) {
		int value = Integer.MIN_VALUE;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( array[i][j] > value ) { value = array[i][j]; }
			}
		}
		return value;
	}

	//----------------------------------------------------------------
	//  飛び三の判定
	//----------------------------------------------------------------
	boolean check_tobi_3(int[][] board, int color, int i, int j) {
		if ( check_tobi_3_dir(board, color, i, j, 0, -1) ) { return true; }
		if ( check_tobi_3_dir(board, color, i, j, -1, -1) ) { return true; }
		if ( check_tobi_3_dir(board, color, i, j, -1, 0) ) { return true; }
		if ( check_tobi_3_dir(board, color, i, j, -1, +1) ) { return true; }
		return false;
	}

	boolean check_tobi_3_dir(int[][] board, int color, int i, int j, int dx, int dy) {
		// 6つの並びの両端が空マス、中の4つの並びに自石が3つと空マス1つ
		for ( int k = 1; k <= 4; k++) {
			int startX = i + dx * k;
			int endX = i + dx * (k-5);
			int startY = j + dy * k;
			int endY = j + dy * (k-5);
			// 両端が空マスか判定
			if ( startX < 0 || startY < 0 || startX >= size || startY >= size ) { continue; }
			if ( endX < 0 || endY < 0 || endX >= size || endY >= size ) { continue; }
			if ( board[startX][startY] != 0 || board[endX][endY] != 0 ) { continue; }
			// 中の4マスを調査
			int myStone = 0;
			int empty = 0;
			for ( int l = 1; l <= 4; l++ ) {
				int x = startX - dx * l;
				int y = startY - dy * l;
				if ( x == i && y == j ) { myStone++; }
				else if ( board[x][y] == color ) { myStone++; }
				else if ( board[x][y] == 0 ) { empty++; }
			}
			if ( myStone == 3 && empty == 1 ) { return true; }
		}
		return false;
	}

	//----------------------------------------------------------------
	//  桂馬位置の確認
	//----------------------------------------------------------------
	boolean check_keima(int[][] board, int color, int i, int j) {
		for ( int dx = -2; dx <= 2; dx++ ) {
			if ( dx == 0 ) { continue; }
			for ( int dy = -2; dy <= 2; dy++ ) {
				if ( dy == 0 ) { continue; }
				if ( dx == dy || dx == -dy ) { continue; }
				int x = i + dx;
				int y = j + dy;
				if ( x < 0 || y < 0 || x >= size || y >= size ) { continue; }
				if ( board[x][y] == color ) { return true; }
			}
		}
		return false;
	}

	//----------------------------------------------------------------
	//  禁じ手の判定
	//----------------------------------------------------------------
	boolean check_taboo(int[][] board, int color, int i, int j) {
		return check_33(board, color, i, j);
	}

	boolean check_33(int[][] board, int color, int i, int j) {
		if ( check_33_L(board, color, i, j) ) { System.out.println("L" + i + j); return true; }
		if ( check_33_T(board, color, i, j) ) { System.out.println("T" + i + j); return true; }
		if ( check_33_X(board, color, i, j) ) { System.out.println("X" + i + j); return true; }
		return false;
	}

	boolean check_33_L(int[][] board, int color, int i, int j) {
		for ( int dx1 = -1; dx1 <= +1; dx1++ ) {
			for ( int dy1 = -1; dy1 <= +1; dy1++ ) {
				if ( dx1 == 0 && dy1 == 0 ) { continue; }
				if ( ! check_run_dir(board, color, i, j, dx1, dy1, 3, true) ) { continue; }
				for ( int dx2 = -1; dx2 <= +1; dx2++ ) {
					for ( int dy2 = -1; dy2 <= +1; dy2++ ) {
						if ( dx2 == 0 && dy2 == 0 ) { continue; }
						if ( dx1 == dx2 && dy1 == dy2 ) { continue; }
						if ( dx1 == -dx2 && dy1 == -dy2 ) { continue; }
						if ( check_run_dir(board, color, i, j, dx2, dy2, 3, true) ) { return true; }
					}
				}
			}
		}
		return false;
	}

	boolean check_33_T(int[][] board, int color, int i, int j) {

		if ( check_run_dir(board, color, i, j, 0, -1, 2, true) && check_run_dir(board, color, i, j, 0, +1, 2, true) ) {
			if ( check_run_dir(board, color, i, j, -1, -1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, -1, +1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, -1, 0, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, +1, 0, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, +1, -1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, +1, +1, 3, true) ) { return true; }
		}

		if ( check_run_dir(board, color, i, j, -1, 0, 2, true) && check_run_dir(board, color, i, j, +1, 0, 2, true) ) {
			if ( check_run_dir(board, color, i, j, -1, -1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, 0, -1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, +1, -1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, -1, +1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, 0, +1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, +1, +1, 3, true) ) { return true; }
		}

		if ( check_run_dir(board, color, i, j, -1, -1, 2, true) && check_run_dir(board, color, i, j, +1, +1, 2, true) ) {
			if ( check_run_dir(board, color, i, j, +1, 0, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, +1, -1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, 0, -1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, -1, 0, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, -1, +1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, 0, +1, 3, true) ) { return true; }
		}

		if ( check_run_dir(board, color, i, j, -1, +1, 2, true) && check_run_dir(board, color, i, j, +1, -1, 2, true) ) {
			if ( check_run_dir(board, color, i, j, 0, -1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, -1, -1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, -1, 0, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, 0, +1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, +1, +1, 3, true) ) { return true; }
			if ( check_run_dir(board, color, i, j, +1, 0, 3, true) ) { return true; }
		}
		return false;
	}

	boolean check_33_X(int[][] board, int color, int i, int j) {
		int count = 0;

		if ( check_run2_dir(board, color, i, j, +1, +1, 3, true) ) { count++; }
		if ( check_run2_dir(board, color, i, j, +1, 0, 3, true) ) { count++; }
		if ( check_run2_dir(board, color, i, j, +1, -1, 3, true) ) { count++; }
		if ( check_run2_dir(board, color, i, j, 0, -1, 3, true)  ) { count++; }
		return count >= 2;
	}
	//----------------------------------------------------------------
	//  五連があるか確認
	//----------------------------------------------------------------
	boolean check_run_5(int[][] board, int color) {
		for ( int i = 0; i < size; i++) {
			for ( int j = 0; j < size; j++ ) {
				if ( board[i][j] != color ) { continue; }
				if ( check_run(board, color, i, j, 5, false) ) { return true; }
			}
		}
		return false;
	}

	//----------------------------------------------------------------
	//  三四判定
	//----------------------------------------------------------------
	boolean check_34(int[][] board, int color, int i, int j) {
		return check_run2(board, color, i, j, 3, true) && check_run2(board, color, i, j, 4, true);
	}

	//----------------------------------------------------------------
	//  石が取れるか確認
	//----------------------------------------------------------------
	boolean check_rem_all(int[][] board, int color) {
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( values[i][j] == -2 ) { continue; }
				if ( check_rem(board, color, i, j) ) { return true; }
			}
		}
		return false;
	}

	//----------------------------------------------------------------
	//  連の全周チェック
	//----------------------------------------------------------------

	boolean check_run(int[][] board, int color, int i, int j, int len, boolean stop) {
		for ( int dx = -1; dx <= 1; dx++ ) {
			for ( int dy = -1; dy <= 1; dy++ ) {
				if ( dx == 0 && dy == 0 ) { continue; }
				if ( check_run_dir(board, color, i, j, dx, dy, len, stop) ) { return true; }
			}
		}
		return false;
	}

	//----------------------------------------------------------------
	//  連の方向チェック
	//----------------------------------------------------------------
	boolean check_run_dir(int[][] board, int color, int i, int j, int dx, int dy, int len, boolean stop) {
		// 5連未満の連なら開始地点側で止められているか判定
		int x = i + dx * -1;
		int y = j + dy * -1;
		// 盤外判定
		if ( x >= 0 && y >= 0 && x < size && y < size ) {
			// 止められているか判定
			if ( board[x][y] == -color && stop && len < 5 ) { return false; }
			// 長連か判定
			if ( board[x][y] == color ) { return false; }
		}
		// 連判定
		for ( int k = 1; k < len; k++ ) {
			x = i + k * dx;
			y = j + k * dy;
			// 盤外判定
			if ( x < 0 || y < 0 || x >= size || y >= size ) { return false; }
			// 自分の石があるか確認
			if ( board[x][y] != color ) { return false; }
		}
		// 終了地点の次を判定
		x = i + len * dx;
		y = j + len * dy;
		if ( stop && len < 5 ) {
			// 次の地点が盤内か判定
			if ( x < 0 || y < 0 || x >= size || y >= size  ) { return false; }
			// 止連か判定
			if ( board[x][y] == color*-1) { return false; }
			// 長連か判定
			if ( board[x][y] == color ) { return false; }
		}
		return true;
	}

	//----------------------------------------------------------------
	//  間において連になるか確認
	//----------------------------------------------------------------
	boolean check_run2(int[][] board, int color, int i, int j, int len, boolean stop) {
		if ( check_run2_dir(board, color, i, j, 0, -1, len, stop) ) { return true; }
		if ( check_run2_dir(board, color, i, j, -1, -1, len, stop) ) { return true; }
		if ( check_run2_dir(board, color, i, j, -1, 0, len, stop) ) { return true; }
		if ( check_run2_dir(board, color, i, j, -1, +1, len, stop) ) { return true; }
		return false;
	}

	boolean check_run2_dir(int[][] board, int color, int i, int j, int dx, int dy, int len, boolean stop) {
		int count = 1;
		int x, y;
		// 進行方向側を確認
		if ( stop ) {
			x = i + dx * -1;
			y = j + dy * -1;
			if ( x >= 0 && y >= 0 && x < size && y < size) {
				if ( board[x][y] == color*-1 ) { return false; }
			}
		}
		for ( int k = 1; k <= len; k++ ) {
			x = i + k * dx;
			y = j + k * dy;
			// 盤外判定
			if ( x < 0 || y < 0 || x >= size || y >= size ) {
				if ( stop ) { return false; }
				else { break; }
			}
			// 自分の石があるか確認
			if ( board[x][y] == color ) {
				if ( k != len ) { count++; }
				else { return false; }
			}
			else if ( board[x][y] == 0 ) { break; }
			else {
				if ( len >= 5 || ! stop ) { break; }
				if ( stop ) { return false; }
			}
		}

		// 進行方向とは逆方向を確認
		for ( int k = 1; k <= len; k++ ) {
			x = i - k * dx;
			y = j - k * dy;
			// 盤外判定
			if ( x < 0 || y < 0 || x >= size || y >= size ) {
				if ( stop ) { return false; }
				else { break; }
			}
			// 自分の石があるか確認
			if ( board[x][y] == color ) {
				if ( k != len ) { count++; }
				else { return false; }
			}
			else if ( board[x][y] == 0 ) { break; }
			else {
				if ( len >= 5 || ! stop ) { break; }
				if ( stop ) { return false; }
			}
		}
		return count == len;
	}
	//----------------------------------------------------------------
	//  取の全周チェック(ダブルの判定は無し)
	//----------------------------------------------------------------

	boolean check_rem(int [][] board, int color, int i, int j) {
		for ( int dx = -1; dx <= 1; dx++ ) {
			for ( int dy = -1; dy <= 1; dy++ ) {
				if ( dx == 0 && dy == 0 ) { continue; }
				if ( check_rem_dir(board, color, i, j, dx, dy) ) { return true; }
			}
		}
		return false;
	}

	//----------------------------------------------------------------
	//  取の方向チェック
	//----------------------------------------------------------------
	boolean check_rem_dir(int[][] board, int color, int i, int j, int dx, int dy) {
		int len = 3;
		for ( int k = 1; k <= len; k++ ) {
			int x = i+k*dx;
			int y = j+k*dy;
			if ( x < 0 || y < 0 || x >= size || y >= size ) { return false; }
			if ( board[i+k*dx][j+k*dy] != color ) { return false; }
			if (k == len-1) { color *= -1; }
		}
		return true;
	}

	//----------------------------------------------------------------
	//  盤面評価値
	//----------------------------------------------------------------
	int get_weight_value(int i, int j) {
		if ( i > 3 && j > 3 && i <= 8 && j <= 8 ) {
			return 10;
		}
		return (int) Math.round(Math.random() * 7);
	}

	//----------------------------------------------------------------
	//  評価盤面の表示
	//----------------------------------------------------------------
	public void show_value() {
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				System.out.printf("%3d ", values[j][i]);
			}
			System.out.println("");
		}
		System.out.println("");
	}

	//----------------------------------------------------------------
	//  着手の決定
	//----------------------------------------------------------------

	public GameHand deside_hand() {
		GogoHand hand = new GogoHand();
		hand.set_hand(0, 0);  // 左上をデフォルトのマスとする
		int value = -1;       // 評価値のデフォルト
		//--  評価値が最大となるマス
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (value < values[i][j]) {
					hand.set_hand(i, j);
					value = values[i][j];
				}
				if ( value == values[i][j] ) {
					int aaa = (int) Math.round(Math.random() * 100);
					//System.out.println(aaa);
					if ( aaa % 2 == 0 ) {
						hand.set_hand(i, j);
						value = values[i][j];
					}
				}
			}
		}
		return hand;
	}

}
