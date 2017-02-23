package data.strategy.user.s14t242;

import sys.game.GameBoard;
import sys.game.GameCompSub;
import sys.game.GameHand;
import sys.game.GamePlayer;
import sys.game.GameState;
import sys.struct.GogoHand;
import sys.user.GogoCompSub;

public class User_s14t242_03 extends GogoCompSub {
	int TABOO = -1;	// 禁じ手の評価値
	int NOT_ENMPTY = -2;	// 空マスではない評価値

	//====================================================================
	//  コンストラクタ
	//====================================================================

	public User_s14t242_03(GamePlayer player) {
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
		int [][] cell = board.get_cell_all();  // 盤面情報
		int mycolor = role;                  // 自分の石の色
		int gettenStones = get_mystone(prev);	// 取った石の個数
		int stolenStones = get_enemystone(prev);	// 取られた石の個数
		GogoHand tmpHand = new GogoHand();
		GameState tmpState;
		boolean enemy_run_5 = check_run_5(cell, mycolor*-1);	// 相手の五連があるか

		System.out.println(gettenStones + " " + stolenStones);

		//--  各マスの評価値
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				// 埋まっているマス
				if (values[i][j] == -2) {
					values[i][j] = NOT_ENMPTY;
					continue;
				}
				//--  適当な評価の例
				// 三々の禁じ手は打たない
				if ( check_taboo(cell, mycolor, i, j) ) {
					values[i][j] = TABOO;
					continue;
				}
				// 相手の五連を崩す → 10000;
				if ( enemy_run_5 ) {
					if ( check_rem(cell, mycolor*-1, i, j) ) {
						tmpHand.set_hand(i, j);
						tmpState = prev.test_hand(tmpHand);
						if ( ! check_run_5(tmpState.board.get_cell_all(), mycolor*-1) ) {
							values[i][j] += 10000;
						}
					}
				}
				// 勝利(五取) → 4000;
				if ( check_rem(cell, mycolor*-1, i, j) ) {
					tmpHand.set_hand(i, j);
					tmpState = prev.test_hand(tmpHand);
					if ( get_mystone(tmpState) >= 10 ) {
						values[i][j] += 4000;
					}
				}
				// 勝利(五連) → 2000;
				if ( check_run(cell, mycolor, i, j, 5, false, false) ) {
					values[i][j] += 2000;
				}
				// 敗北阻止(五取) → 1000;
				tmpHand.set_hand(i, j);
				tmpState = prev.test_hand(tmpHand);
				if ( ! check_rem_5(tmpState) ) {
					values[i][j] += 1000;
				}
				// 敗北阻止(五連) → 400;
				if ( check_run(cell, mycolor*-1, i, j, 5, false, false) ) {
					values[i][j] += 400;
				}
				// 自分の四連を作る → 200;
				if ( check_run(cell, mycolor, i, j, 4, true, true) ) {
					values[i][j] += 200;
				}
				// 相手の四連を止める → 100;
				if ( check_run(cell, mycolor*-1, i, j, 4, true, true) ) {
					values[i][j] += 100;
				}
				// 相手の石を取る → 40;
				if ( check_rem(cell, mycolor*-1, i, j) ) {
					values[i][j] += 40;
				}
				// 自分の三連を作る → 20;
				if ( check_run(cell, mycolor, i, j, 3, true, true) ) {
					values[i][j] += 20;
				}
				// 自分の石を守る → 10;
				// 盤内評価値 → 0~9
			}
		}
		show_value();
	}
	//----------------------------------------------------------------
	//  五取の判定
	//----------------------------------------------------------------
	boolean check_rem_5(GameState prev) {
		int[][] cell = prev.board.get_cell_all();  // 盤面情報
		GogoHand tmpHand = new GogoHand();
		GameState tmpState;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != prev.board.SPACE ) { continue; }
				tmpHand.set_hand(i, j);
				tmpState = prev.test_hand(tmpHand);
				if ( get_enemystone(tmpState) >= 10 ) { return true; }
			}
		}
		return false;

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
		return check_run_num(board, color, i, j, 3) >= 2;
	}


	//----------------------------------------------------------------
	//  五連があるか確認
	//----------------------------------------------------------------
	boolean check_run_5(int[][] board, int color) {
		for ( int i = 0; i < size; i++) {
			for ( int j = 0; j < size; j++ ) {
				if ( board[i][j] != color ) { continue; }
				if ( check_run(board, color, i, j, 5, false, false) ) { return true; }
			}
		}
		return false;
	}

	//----------------------------------------------------------------
	//  三四判定
	//----------------------------------------------------------------
	boolean check_34(int[][] board, int color, int i, int j) {
		return check_run(board, color, i, j, 3, true, true) && check_run(board, color, i, j, 4, true, true);
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
	//  連の個数チェック
	//----------------------------------------------------------------
	int check_run_num(int[][] board, int color, int i, int j, int len) {
		int count = 0;
		if ( check_run_dir(board, color, i, j, 0, -1, len, true, true) ) { count++; }	// 上下
		if ( check_run_dir(board, color, i, j, -1, -1, len, true, true) ) { count++; }	// 右下がり
		if ( check_run_dir(board, color, i, j, -1, 0, len, true, true) ) { count++; }	// 	左右
		if ( check_run_dir(board, color, i, j, -1, +1, len, true, true) ) { count++; }	// 右上がり
		return count;
	}
	//----------------------------------------------------------------
	//  連の全周チェック
	//----------------------------------------------------------------
	boolean check_run(int[][] board, int color, int i, int j, int len, boolean stop_dir1, boolean stop_dir2) {
		if ( check_run_dir(board, color, i, j, 0, -1, len, stop_dir1, stop_dir2) ) { return true; }	// 上下
		if ( check_run_dir(board, color, i, j, -1, -1, len, stop_dir1, stop_dir2) ) { return true; }	// 右下がり
		if ( check_run_dir(board, color, i, j, -1, 0, len, stop_dir1, stop_dir2) ) { return true; }	// 	左右
		if ( check_run_dir(board, color, i, j, -1, +1, len, stop_dir1, stop_dir2) ) { return true; }	// 右上がり
		return false;
	}

	//----------------------------------------------------------------
	//  連の方向チェック
	//----------------------------------------------------------------

	boolean check_run_dir(int[][] board, int color, int i, int j, int dx, int dy, int len, boolean stop_dir1, boolean stop_dir2) {
		int count = 1;
		int x, y;
		// 進行方向側(dir1)を確認
		for ( int k = 1; k <= len; k++ ) {
			x = i + k * dx;
			y = j + k * dy;
			// 盤外判定
			if ( x < 0 || y < 0 || x >= size || y >= size ) {
				// 端連は止められていると判断
				if ( stop_dir1 && len < 5 ) { return false; }
				else { break; }
			}
			// 自分の石があるか確認
			if ( board[x][y] == color ) {
				// k==lenなら長連と判断
				if ( k != len ) { count++; }
				else { return false; }
			}
			// 空マスなら反復から脱出
			else if ( board[x][y] == 0 ) { break; }
			// 相手の石なら止められていると判断
			else {
				if ( stop_dir1 && len < 5 ) { return false; }
				else { break; }
			}
		}

		// 進行方向とは逆方向(dir2)を確認
		for ( int k = 1; k <= len; k++ ) {
			x = i - k * dx;
			y = j - k * dy;
			// 盤外判定
			if ( x < 0 || y < 0 || x >= size || y >= size ) {
				if ( stop_dir2 && len < 5 ) { return false; }
				else { break; }
			}
			// 自分の石があるか確認
			if ( board[x][y] == color ) {
				if ( k != len ) { count++; }
				else { return false; }
			}
			else if ( board[x][y] == 0 ) { break; }
			else {
				if ( stop_dir2 && len < 5 ) { return false; }
				else { break; }
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
		int[][] weight = {
			{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
			{0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
			{0, 2, 3, 4, 4, 4, 4, 4, 4, 4, 3, 2, 0},
			{0, 2, 4, 5, 0, 0, 0, 0, 0, 5, 4, 2, 0},
			{0, 2, 4, 6, 7, 9, 8, 9, 8, 6, 4, 2, 0},
			{0, 2, 4, 6, 9, 8, 7, 7, 9, 8, 4, 2, 0},
			{0, 2, 4, 6, 7, 7, 9, 8, 7, 6, 4, 2, 0},
			{0, 2, 4, 6, 9, 8, 7, 7, 9, 8, 4, 2, 0},
			{0, 2, 4, 6, 7, 9, 7, 9, 8, 6, 4, 2, 0},
			{0, 2, 4, 5, 6, 6, 6, 6, 6, 5, 4, 2, 0},
			{0, 2, 3, 4, 4, 4, 4, 4, 4, 4, 3, 2, 0},
			{0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
			{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
		};
		return weight[i][j];
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
			}
		}
		return hand;
	}

}
