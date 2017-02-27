package data.strategy.user.s14t242;

import sys.game.GameBoard;
import sys.game.GameCompSub;
import sys.game.GameHand;
import sys.game.GamePlayer;
import sys.game.GameState;
import sys.struct.GogoHand;
import sys.user.GogoCompSub;
import java.util.ArrayList;
import java.util.List;


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

		//--  評価値の計算
		calc_values(theState, theBoard);
		//negamax(theState, role, 1);
		// 先手後手、取石数、手数(序盤・中盤・終盤)で評価関数を変える
		show_value();
		//--  着手の決定
		return deside_hand(theState);

	}

	int negamax(GameState nowState, int mycolor, int depth) {
		GameBoard nowBoard = nowState.board;
		int[][] cell = nowBoard.get_cell_all();
		int nowTurn = nowState.turn;
		GogoHand tmpHand = new GogoHand();
		GameState nextState;
		int[][] eval = new int[size][size];
		init_values(nowState, nowBoard);
		eval = values.clone();
		if ( depth == 1 ) { return calc_values(nowState, nowBoard); }
		for ( int i = 0; i < size; i++ ) {
			for ( int j =0 ; j < size; j++ ) {
				if ( cell[i][j] != nowBoard.SPACE ) {
					eval[i][j] = NOT_ENMPTY;
					continue;
				}
				if ( check_taboo(cell, mycolor, i, j) ) {
					eval[i][j] = TABOO;
					continue;
				}
				tmpHand.set_hand(i, j);
				nextState = nowState.test_hand(tmpHand);
				eval[i][j] = -negamax(nextState, mycolor, depth-1);
				System.out.print("D:" + depth + " I:" + i + " J:" + j);
				System.out.println(" SCORE:" + eval[i][j]);
				//if ( nowTurn != mycolor ) { eval[i][j] *= -1; }
			}
		}
		values = eval.clone();
		show_value();
		return max(eval);

	}

	int max(int[][] array) {
		int value = Integer.MIN_VALUE;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( array[i][j] == -TABOO || array[i][j] == - NOT_ENMPTY ) { continue; }
				if ( array[i][j] > value ) { value = array[i][j]; }
			}
		}
		return value;
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

	public int calc_values(GameState prev, GameBoard board) {
		int [][] cell = board.get_cell_all();  // 盤面情報
		int mycolor = prev.turn;                  // 自分の石の色
		int now_gotton_stones_count = get_mystone(prev, mycolor);	// 取った石の個数
		int now_stolen_stones_count = get_enemystone(prev, mycolor);	// 取られた石の個数
		int now_stolen_run_count = check_rem_count(prev, mycolor);
		GogoHand tmp_hand = new GogoHand();
		GameState next_state;
		GameBoard next_board;
		boolean now_enemy_run_5_exist = check_run_5_exist(prev, mycolor*-1);	// 相手の五連があるか
		int[][] eval = new int[size][size];

		System.out.println(mycolor + " " + mycolor*-1);
		System.out.println(now_gotton_stones_count + " " + now_stolen_stones_count + " " +now_stolen_run_count);

		//-- 石の確認
		init_values(prev, board);
		eval = values.clone();

		//--  各マスの評価値
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				// 埋まっているマス
				if (eval[i][j] == -2) {
					eval[i][j] = NOT_ENMPTY;
					continue;
				}
				// 三々の禁じ手は打たない
				if ( check_taboo(cell, mycolor, i, j) ) {
					eval[i][j] = TABOO;
					continue;
				}

				// 更新盤面の生成
				tmp_hand.set_hand(i, j);
				next_state = prev.test_hand(tmp_hand);
				next_board = next_state.board;

				//-- 敗北確定阻止(現ターン)
				// 五連崩し 100000000
				if ( now_enemy_run_5_exist ) {
					if ( ! check_run_5_exist(next_state, mycolor*-1) ) {
						eval[i][j] += 100000000;
					}
				}

				//-- 勝利確定(現ターン)
				// 五取 40000000
				if ( get_mystone(next_state, mycolor) >= 10 ) {
					eval[i][j] += 40000000;
					continue;
				}
				// 完五連 20000000
				if ( check_perfect_run_5_exist(next_state, mycolor) ) {
					eval[i][j] += 20000000;
					continue;
				}
				//eval[i][j] = check_my_point(next_state, prev) + check_avoid_point(next_state, prev);

				//-- 敗北確定阻止(次ターン)
				// 五取阻止 10000000
				if ( ! check_next_enemy_rem_5(next_state, mycolor) ) {
					eval[i][j] += 10000000;
				}
				// 五連阻止 4000000
				if ( ! check_next_enemy_run_5(next_state, mycolor) ) {
					eval[i][j] += 4000000;
				}

				//-- 勝利確定(2ターン後)
				// 四連 2000000
				if ( check_run_4_exist(next_state, mycolor) ) {
					eval[i][j] += 2000000;
				}
				// 四四 1000000
				if ( check_44_exist(next_state, mycolor) ) {
					eval[i][j] += 1000000;
				}
				// 三四 400000
				if ( check_34_exist(next_state, mycolor) ) {
					eval[i][j] += 400000;
				}

				//-- 勝利可能性(2ターン後)
				// 仮五連 200000
				if ( check_run_5_exist(next_state, mycolor) ) {
					eval[i][j] += 200000;
				}

				//-- 敗北確定阻止(3ターン後)
				// 四連阻止 100000
				if ( check_run(cell, mycolor*-1, i, j, 4, true, true) ) {
					eval[i][j] += 100000;
				}
				// 三四阻止 40000
				if ( ! check_next_enemy_34(next_state, mycolor) ) {
					eval[i][j] += 40000;
				}
				// 四四阻止 20000
				if ( ! check_next_enemy_44(next_state, mycolor) ) {
					eval[i][j] += 20000;
				}

				//-- 勝利確定(4ターン後)
				// 一飛び三三 10000
				if ( check_33_exist(next_state, mycolor) ) {
					eval[i][j] += 10000;
				}

				//-- 敗北確定阻止(5ターン後)
				// 一飛び三三阻止 4000
				if ( ! check_next_enemy_33(next_state, mycolor) ) {
					eval[i][j] += 4000;
				}


				//-- 敗北近傍阻止
				// 三連阻止 1000
				if ( check_run(cell, mycolor*-1, i, j, 3, true, true) ) {
					eval[i][j] += 400;
				}
				//石取阻止 2000
				if ( check_rem_count(next_state, mycolor) <= check_rem_count(prev, mycolor)
				&& ! check_next_enemy_rem(next_state, mycolor, now_stolen_stones_count) ) {
					eval[i][j] += 2000;
				}

				//-- 勝利近傍
				// 石取 400
				if ( get_mystone(next_state, mycolor) > now_gotton_stones_count ) {
					eval[i][j] += 1000;
				}
				// 三連 200
				if ( check_run_3_exist(next_state, mycolor) ) {
					eval[i][j] += 200;
				}

				//-- 調整
				// 盤内評価値(中央付近ほど高い) 2~9
				eval[i][j] += get_weight_value(cell, i, j, mycolor);
			}
		}
		values = eval.clone();
		//show_value();
		return max(eval);
	}

	int check_avoid_point(GameState next, GameState now) {
		int point = 0;
		int color = next.turn;
		boolean run_5_exist = true;
		boolean run_4_exist = true;
		boolean run_3_exist = true;
		boolean run_44_exist = true;
		boolean run_34_exist = true;
		boolean run_33_exist = true;
		boolean get_stone = check_rem_count(next, color*-1) <= check_rem_count(now, color*-1)
			&& ! check_next_enemy_rem(next, color*-1, get_mystone(now, color) );
		if ( get_stone ) { point += 2000; }
		int[][] cell = next.board.get_cell_all();
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != now.board.SPACE ) { continue; }
				// 5連阻止
				if ( run_5_exist && check_run(cell, color, i, j, 5, false, false) ) {
					run_5_exist = false;
				}
				// 4連阻止
				if ( run_4_exist && check_run(cell, color, i, j, 4, true, true) ) {
					run_4_exist = false;
				}
				// 3連阻止
				if ( run_3_exist && check_run(cell, color, i, j, 3, true, true) ) {
					run_3_exist = false;
				}
				// 44阻止
				if ( run_44_exist ) {
					int run = check_run_num(cell, color, i, j, 4);
					int tobi = check_tobi_4_num(cell, color, i, j);
					int tome = check_stop_num(cell, color, i, j, 4);
					if (  run+tobi+tome >= 2 ) {
						run_44_exist = false;
					}
				}
				// 34阻止
				if ( run_34_exist ) {
					int run_3 = check_run_num(cell, color, i, j, 3);
					int tobi_3 = check_1tobi_3_num(cell, color, i, j);
					int run_4 = check_run_num(cell, color, i, j, 4);
					int tobi_4 = check_tobi_4_num(cell, color, i, j);
					int tome_4 = check_stop_num(cell, color, i, j, 4);
					if (  run_3 <= 1 && run_3+tobi_3 >= 1  && run_4+tobi_4+tome_4 >= 1 ) {
						run_34_exist = false;
					}
				}
				// 33阻止
				if ( run_33_exist ) {
					int run = check_run_num(cell, color, i, j, 3);
					int tobi = check_1tobi_3_num(cell, color, i, j);
					if ( run <= 1 && run+tobi >= 2 ) {
						run_33_exist = false;
					}
				}
			}
		}
		if ( run_5_exist ) { point += 10000000; }
		if ( run_4_exist ) { point += 100000; }
		if ( run_3_exist ) { point += 400;}
		if ( run_44_exist ) { point += 20000; }
		if ( run_34_exist ) { point += 40000; }
		if ( run_33_exist ) { point += 10000; }
		return point;
	}

	int check_my_point(GameState next, GameState now) {
		int point = 0;
		int color = now.turn;
		boolean run_5_exist = false;
		boolean run_4_exist = false;
		boolean run_3_exist = false;
		boolean run_44_exist = false;
		boolean run_34_exist = false;
		boolean run_33_exist = false;
		boolean get_stone = get_mystone(next, color) > get_mystone(next, color);
		if ( get_stone ) { point += 1000; }
		int[][] cell = next.board.get_cell_all();
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != color ) { continue; }
				// 5連
				if ( ! run_5_exist && check_run(cell, color, i, j, 5, false, false) ) {
					run_5_exist = true;
					point += 200000;
				}
				// 4連
				if ( ! run_4_exist && check_run(cell, color, i, j, 4, true, true) ) {
					run_4_exist = true;
					point += 2000000;
				}
				// 3連
				if ( ! run_3_exist && check_run(cell, color, i, j, 3, true, true) ) {
					run_3_exist = true;
					point += 200;
				}
				// 44
				if ( ! run_44_exist ) {
					int run = check_run_num(cell, color, i, j, 4);
					int tobi = check_tobi_4_num(cell, color, i, j);
					int tome = check_stop_num(cell, color, i, j, 4);
					if (  run+tobi+tome >= 2 ) {
						run_44_exist = true;
						point += 1000000;
					}
				}
				// 34
				if ( ! run_34_exist ) {
					int run_3 = check_run_num(cell, color, i, j, 3);
					int tobi_3 = check_1tobi_3_num(cell, color, i, j);
					int run_4 = check_run_num(cell, color, i, j, 4);
					int tobi_4 = check_tobi_4_num(cell, color, i, j);
					int tome_4 = check_stop_num(cell, color, i, j, 4);
					if (  run_3 <= 1 && run_3+tobi_3 >= 1  && run_4+tobi_4+tome_4 >= 1 ) {
						run_34_exist = true;
						point += 400000;
					}
				}
				// 33
				if ( ! run_33_exist ) {
					int run = check_run_num(cell, color, i, j, 3);
					int tobi = check_1tobi_3_num(cell, color, i, j);
					if ( run <= 1 && run+tobi >= 2 ) {
						run_33_exist = true;
						point += 10000;
					}
				}
			}
		}
		return point;
	}

	//----------------------------------------------------------------
	//  取られる連の数を確認
	//----------------------------------------------------------------
	int check_rem_count(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();
		int count = 0;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != now.board.SPACE ) { continue; }
				for ( int dx = -1; dx <= 1; dx++ ) {
					for ( int dy = -1; dy <= 1; dy++ ) {
						if ( dx == 0 && dy == 0 ) { continue; }
						if ( check_rem_dir(cell, color, i, j, dx, dy) ) {
							count++;
						}
					}
				}
			}
		}
		return count;
	}

	//----------------------------------------------------------------
	//  取られる連か判定
	//----------------------------------------------------------------
	boolean check_next_rem(GameState now, int color, int i, int j) {
		int[][] cell = now.board.get_cell_all();

		if ( ! check_run(cell, color, i, j, 2, true, true)
		&& (check_run(cell, color, i, j, 2, true, false)
		|| check_run(cell, color, i, j, 2, false, true)) ) {
			return true;
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
	//  一飛び三三があるか確認
	//----------------------------------------------------------------
	boolean check_33_exist(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != color ) { continue; }
				int run = check_run_num(cell, color, i, j, 3);
				int tobi = check_1tobi_3_num(cell, color, i, j);
				if (  run <= 1 && run+tobi >= 2 ) { return true; }
			}
		}
		return false;
	}
	//----------------------------------------------------------------
	//  次の相手のターンで一飛び三三になるか確認
	//----------------------------------------------------------------
	boolean check_next_enemy_33(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();
		GogoHand tmp_hand = new GogoHand();
		GameState next_state;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != now.board.SPACE ) { continue; }
				tmp_hand.set_hand(i, j);
				next_state = now.test_hand(tmp_hand);
				if ( check_33_exist(next_state, color*-1) ) { return true; }
			}
		}
		return false;
	}
	//----------------------------------------------------------------
	//  三四があるか確認
	//----------------------------------------------------------------
	boolean check_34_exist(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != color ) { continue; }
				int run_3 = check_run_num(cell, color, i, j, 3);
				int tobi_3 = check_1tobi_3_num(cell, color, i, j);
				int run_4 = check_run_num(cell, color, i, j, 4);
				int tobi_4 = check_tobi_4_num(cell, color, i, j);
				int tome_4 = check_stop_num(cell, color, i, j, 4);
				if (  run_3 <= 1 && run_3+tobi_3 >= 1  && run_4+tobi_4+tome_4 >= 1 ) {
					return true;
				}
			}
		}

		return false;
	}
	//----------------------------------------------------------------
	//  次の相手のターンで三四になるか確認
	//----------------------------------------------------------------
	boolean check_next_enemy_34(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();
		GogoHand tmp_hand = new GogoHand();
		GameState next_state;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != now.board.SPACE ) { continue; }
				tmp_hand.set_hand(i, j);
				next_state = now.test_hand(tmp_hand);
				if ( check_34_exist(next_state, color*-1) ) { return true; }
			}
		}
		return false;
	}
	//----------------------------------------------------------------
	//  四四があるか確認
	//----------------------------------------------------------------
	boolean check_44_exist(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != color ) { continue; }
				int run = check_run_num(cell, color, i, j, 4);
				int tobi = check_tobi_4_num(cell, color, i, j);
				int tome = check_stop_num(cell, color, i, j, 4);
				if (  run+tobi+tome >= 2 ) {
					return true;
				}
			}
		}
		return false;
	}
	//----------------------------------------------------------------
	//  次の相手のターンで四四になるか確認
	//----------------------------------------------------------------
	boolean check_next_enemy_44(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();
		GogoHand tmp_hand = new GogoHand();
		GameState next_state;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != now.board.SPACE ) { continue; }
				tmp_hand.set_hand(i, j);
				next_state = now.test_hand(tmp_hand);
				if ( check_44_exist(next_state, color*-1) ) { return true; }
			}
		}
		return false;
	}
	//----------------------------------------------------------------
	//  次の相手のターンで石が取られるか確認
	//----------------------------------------------------------------
	boolean check_next_enemy_rem(GameState now, int color, int count) {
		int[][] cell = now.board.get_cell_all();
		GogoHand tmp_hand = new GogoHand();
		GameState next_state;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != now.board.SPACE ) { continue; }
				tmp_hand.set_hand(i, j);
				next_state = now.test_hand(tmp_hand);
				if ( get_enemystone(next_state, color) > count ) { return true; }
			}
		}
		return false;
	}

	//----------------------------------------------------------------
	//  三連があるか確認
	//----------------------------------------------------------------
	boolean check_run_3_exist(GameState now, int color) {
		int[][] board = now.board.get_cell_all();  // 盤面情報
		for ( int i = 0; i < size; i++) {
			for ( int j = 0; j < size; j++ ) {
				if ( board[i][j] != color ) { continue; }
				if ( check_run(board, color, i, j, 3, true, true) ) { return true; }
			}
		}
		return false;
	}
	//----------------------------------------------------------------
	//  次の相手のターンで四連になるか判定
	//----------------------------------------------------------------
	boolean check_next_enemy_run_4(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();
		GogoHand tmp_hand = new GogoHand();
		GameState next_state;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != now.board.SPACE ) { continue; }
				tmp_hand.set_hand(i, j);
				next_state = now.test_hand(tmp_hand);
				if ( check_run_4_exist(next_state, color*-1) ) { return true; }
			}
		}
		return false;
	}

	//----------------------------------------------------------------
	//  四連があるか確認
	//----------------------------------------------------------------
	boolean check_run_4_exist(GameState now, int color) {
		int[][] board = now.board.get_cell_all();  // 盤面情報
		for ( int i = 0; i < size; i++) {
			for ( int j = 0; j < size; j++ ) {
				if ( board[i][j] != color ) { continue; }
				if ( check_run(board, color, i, j, 4, true, true) ) { return true; }
			}
		}
		return false;
	}
	//----------------------------------------------------------------
	//  次の相手のターンで五連になるか判定
	//----------------------------------------------------------------
	boolean check_next_enemy_run_5(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();
		GogoHand tmp_hand = new GogoHand();
		GameState next_state;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != now.board.SPACE ) { continue; }
				tmp_hand.set_hand(i, j);
				next_state = now.test_hand(tmp_hand);
				if ( check_run_5_exist(next_state, color*-1) ) { return true; }
			}
		}
		return false;
	}
	//----------------------------------------------------------------
	//  次の相手のターンで五取になるか判定
	//----------------------------------------------------------------
	boolean check_next_enemy_rem_5(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();
		GogoHand tmp_hand = new GogoHand();
		GameState next_state;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != now.board.SPACE ) { continue; }
				tmp_hand.set_hand(i, j);
				next_state = now.test_hand(tmp_hand);
				if ( get_enemystone(next_state, color) >= 10 ) { return true; }
			}
		}
		return false;
	}

	//----------------------------------------------------------------
	//  五連があるか確認
	//----------------------------------------------------------------
	boolean check_run_5_exist(GameState now, int color) {
		int[][] board = now.board.get_cell_all();  // 盤面情報
		for ( int i = 0; i < size; i++) {
			for ( int j = 0; j < size; j++ ) {
				if ( board[i][j] != color ) { continue; }
				if ( check_run(board, color, i, j, 5, false, false) ) { return true; }
			}
		}
		return false;
	}

	//----------------------------------------------------------------
	//  完五連か判定
	//----------------------------------------------------------------
	boolean check_perfect_run_5_exist(GameState now, int color) {
		int[][] cell = now.board.get_cell_all();  // 盤面情報
		GogoHand tmp_hand = new GogoHand();
		GameState next_state;
		if ( ! check_run_5_exist(now, color) ) { return false; }
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				if ( cell[i][j] != now.board.SPACE ) { continue; }
				if ( ! check_rem(cell, color, i, j) ) { continue; }
				tmp_hand.set_hand(i, j);
				next_state = now.test_hand(tmp_hand);
				if ( ! check_run_5_exist(next_state, color) ) { return false; }
			}
		}
		return true;
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
	int check_stop_num(int[][] board, int color, int i, int j, int len) {
		int count = 0;
		if ( ! check_run_dir(board, color, i, j, 0, -1, len, true, true) ) {
			if ( check_run_dir(board, color, i, j, 0, -1, len, true, false) ) { count++; }	// 上下
			if ( check_run_dir(board, color, i, j, 0, -1, len, false, true) ) { count++; }	// 上下
		}
		if ( ! check_run_dir(board, color, i, j, -1, -1, len, true, true) ) {
			if ( check_run_dir(board, color, i, j, -1, -1, len, true, false) ) { count++; }	// 右下がり
			if ( check_run_dir(board, color, i, j, -1, -1, len, false, true) ) { count++; }	// 右下がり
		}
		if ( ! check_run_dir(board, color, i, j, -1, 0, len, true, true) ) {
			if ( check_run_dir(board, color, i, j, -1, 0, len, true, false) ) { count++; }	// 	左右
			if ( check_run_dir(board, color, i, j, -1, 0, len, false, true) ) { count++; }	// 	左右
		}
		if ( ! check_run_dir(board, color, i, j, -1, +1, len, true, true) ) {
			if ( check_run_dir(board, color, i, j, -1, +1, len, true, false) ) { count++; }	// 右上がり
			if ( check_run_dir(board, color, i, j, -1, +1, len, false, true) ) { count++; }	// 右上がり
		}
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
	int get_weight_value(int[][] board, int i, int j, int color) {
		int point;
		int[][] weight = {
			{3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3},
			{3, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 3},
			{3, 5, 7, 7, 7, 7, 7, 7, 7, 7, 7, 5, 3},
			{3, 5, 7, 8, 8, 8, 8, 8, 8, 8, 7, 5, 3},
			{3, 5, 7, 8, 8, 8, 8, 8, 8, 8, 7, 5, 3},
			{3, 5, 7, 8, 8, 8, 8, 8, 8, 8, 7, 5, 3},
			{3, 5, 7, 8, 8, 8, 9, 8, 8, 8, 7, 5, 3},
			{3, 5, 7, 8, 8, 8, 8, 8, 8, 8, 7, 5, 3},
			{3, 5, 7, 8, 8, 8, 8, 8, 8, 8, 7, 5, 3},
			{3, 5, 7, 8, 8, 8, 8, 8, 8, 8, 7, 5, 3},
			{3, 5, 7, 7, 7, 7, 7, 7, 7, 7, 7, 5, 3},
			{3, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 3},
			{3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3}
		};
		point = weight[i][j];
		if ( check_keima(board, color, i, j) && ! check_round(board, i, j, color) ) { point += 20; }
		if ( check_round(board, i, j, color*-1) ) { point += 10; }
		return point;
	}
	//----------------------------------------------------------------
	//  周囲8マスに指定した色の石があるか
	//----------------------------------------------------------------
	boolean check_round(int[][] cell, int i, int j, int color) {
		for ( int dx = -1; dx <= 1; dx++ ) {
			for ( int dy = -1; dy <= 1; dy++ ) {
				if ( dx == 0 && dy == 0 ) { continue; }
				int x = i + dx;
				int y = j + dy;
				if ( x < 0 || y < 0 || x >= size || y >= size ) { continue; }
				if ( cell[x][y] == color ) { return true; }
			}
		}
		return false;
	}
	boolean check_round2(int[][] cell, int i, int j) {
		for ( int dx = -2; dx <= 2; dx++ ) {
			for ( int dy = -2; dy <= 2; dy++ ) {
				if ( dx == 0 && dy == 0 ) { continue; }
				int x = i + dx;
				int y = j + dy;
				if ( x < 0 || y < 0 || x >= size || y >= size ) { continue; }
				if ( cell[x][y] != 0 ) { return true; }
			}
		}
		return false;
	}
	//----------------------------------------------------------------
	//  飛び四の判定
	//----------------------------------------------------------------
	int check_tobi_4_num(int[][] board, int color, int i, int j) {
		int count = 0;
		if ( check_tobi_4_dir(board, color, i, j, 0, -1) ) { count++; }
		if ( check_tobi_4_dir(board, color, i, j, -1, -1) ) { count++; }
		if ( check_tobi_4_dir(board, color, i, j, -1, 0) ) { count++; }
		if ( check_tobi_4_dir(board, color, i, j, -1, +1) ) { count++; }
		return count;
	}

	boolean check_tobi_4(int[][] board, int color, int i, int j) {
		if ( check_tobi_4_dir(board, color, i, j, 0, -1) ) { return true; }
		if ( check_tobi_4_dir(board, color, i, j, -1, -1) ) { return true; }
		if ( check_tobi_4_dir(board, color, i, j, -1, 0) ) { return true; }
		if ( check_tobi_4_dir(board, color, i, j, -1, +1) ) { return true; }
		return false;
	}

	boolean check_tobi_4_dir(int[][] board, int color, int i, int j, int dx, int dy) {
		// 5つの並びに自石が4つと空マス1つ
		// 開始地点の決定
		for ( int k = 0; k <= 4; k++ ) {
			int startX = i + k * dx;
			int startY = j + k * dy;
			if ( startX - dx >= 0 && startY -dy >= 0 && startX - dx < size && startY - dy < size) {
				if ( board[startX-dx][startY-dy] == color ) { continue; }
			}
			int myStone = 0;
			int empty = 0;
			// 石の個数を確認
			for ( int l = 0; l < 5; l++ ) {
				int x = startX + l * dx;
				int y = startY + l * dy;
				if ( x < 0 || y < 0 || x >= size || y >= size ) { break; }
				if ( x == i && y == j ) { myStone++; }
				else if ( board[x][y] == color ) { myStone++; }
				else if ( board[x][y] == 0 ) { empty++; }
				if ( myStone == 0 && empty == 1 ) { break; }
				if ( myStone == 4 && empty == 0 ) { break; }
			}
			int x = startX + 5 * dx;
			int y = startY + 5 * dy;
			if ( x >= 0 && y >= 0 && x < size && y < size ) {
				if ( board[x][y] == color ) { continue; }
			}
			if ( myStone == 4 && empty == 1 ) { return true; }
		}
		return false;
	}

	//----------------------------------------------------------------
	//  2飛び三の判定
	//----------------------------------------------------------------
	boolean check_2tobi_3(int[][] board, int color, int i, int j) {
		if ( check_2tobi_3_dir(board, color, i, j, 0, -1) ) { return true; }
		if ( check_2tobi_3_dir(board, color, i, j, -1, -1) ) { return true; }
		if ( check_2tobi_3_dir(board, color, i, j, -1, 0) ) { return true; }
		if ( check_2tobi_3_dir(board, color, i, j, -1, +1) ) { return true; }
		return false;
	}

	boolean check_2tobi_3_dir(int[][] board, int color, int i, int j, int dx, int dy) {
		// 5つの並びの両端が自石、中3マスの内自石が1つと空マスが2つ
		// 開始地点の決定
		for ( int k = 0; k <= 4; k++ ) {
			int startX = i + k * dx;
			int startY = j + k * dy;
			int myStone = 0;
			int empty = 0;
			// 石の個数を確認
			for ( int l = 0; l < 5; l++ ) {
				int x = startX + l * dx;
				int y = startY + l * dy;
				if ( x < 0 || y < 0 || x >= size || y >= size ) { break; }
				if ( x == i && y == j ) { myStone++; }
				else if ( board[x][y] == color ) { myStone++; }
				else if ( board[x][y] == 0 ) { empty++; }
				if ( myStone == 0 && empty == 1 ) { break; }
				if ( myStone == 3 && empty <= 1 ) { break; }
			}
			if ( myStone == 3 && empty == 2 ) { return true; }
		}
		return false;
	}
	//----------------------------------------------------------------
	//  1飛び三の判定
	//----------------------------------------------------------------
	int check_1tobi_3_num(int[][] board, int color, int i, int j) {
		int count = 0;
		if ( check_1tobi_3_dir(board, color, i, j, 0, -1) ) { count++; }
		if ( check_1tobi_3_dir(board, color, i, j, -1, -1) ) { count++; }
		if ( check_1tobi_3_dir(board, color, i, j, -1, 0) ) { count++; }
		if ( check_1tobi_3_dir(board, color, i, j, -1, +1) ) { count++; }
		return count;
	}

	boolean check_1tobi_3(int[][] board, int color, int i, int j) {
		if ( check_1tobi_3_dir(board, color, i, j, 0, -1) ) { return true; }
		if ( check_1tobi_3_dir(board, color, i, j, -1, -1) ) { return true; }
		if ( check_1tobi_3_dir(board, color, i, j, -1, 0) ) { return true; }
		if ( check_1tobi_3_dir(board, color, i, j, -1, +1) ) { return true; }
		return false;
	}

	boolean check_1tobi_3_dir(int[][] board, int color, int i, int j, int dx, int dy) {
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
				if ( myStone == 0 && empty == 1 ) { break; }
				if ( myStone == 3 && empty == 0 ) { break; }
			}
			if ( myStone == 3 && empty == 1 ) { return true; }
		}
		return false;
	}

	//--------------------------------------------------------------------
	//  自分の取石数を取得
	//--------------------------------------------------------------------
	public int get_mystone(GameState prev, int turn) {
		return prev.get_pocket(turn).point;
	}

	//--------------------------------------------------------------------
	//  相手の取石数を取得
	//--------------------------------------------------------------------
	public int get_enemystone(GameState prev, int turn) {
		return prev.get_pocket(turn*-1).point;
	}

	//----------------------------------------------------------------
	//  評価盤面の表示
	//----------------------------------------------------------------
	public void show_value() {
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) {
				System.out.printf("%9d ", values[j][i]);
			}
			System.out.println("");
		}
		System.out.println("");
	}

	//----------------------------------------------------------------
	//  着手の決定
	//----------------------------------------------------------------
	public GameHand deside_hand(GameState now) {
		GogoHand hand = new GogoHand();
		hand.set_hand(0, 0);  // 左上をデフォルトのマスとする
		int value = -1;       // 評価値のデフォルト
		List<GogoHand> hand_list = new ArrayList<GogoHand>();
		GogoHand tmp;
		GameState calc_state;
		//--  評価値が最大となるマス
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				tmp = new GogoHand();
				if (value <= values[i][j]) {
					tmp.set_hand(i, j);
					if ( value < values[i][j] ) {
						value = values[i][j];
						hand_list.clear();
						hand = tmp;
					}
					hand_list.add(tmp);
				}
			}
		}
		if ( hand_list.size() == 1 ) { return hand; }
		int[][] eval = new int[size][size];
		GameState next_state;
		int[][] cell;
		value = -1;       // 評価値のデフォルト
		tmp = new GogoHand();

		for ( int k = 0; k < hand_list.size(); k++ ) {
			next_state = now.test_hand(hand_list.get(k));
			cell = next_state.board.get_cell_all();
			for ( int i = 0; i < size; i++ ) {
				for ( int j = 0; j < size; j++ ) {
					//if ( ! check_round2(cell, i, j) ) { continue; }
					if ( ! check_round(cell, i, j, 1) && ! check_round(cell, i, j, -1) ) { continue; }
					if ( cell[i][j] != next_state.board.SPACE ) {
						eval[i][j] = NOT_ENMPTY;
						continue;
					}
					if ( check_taboo(cell, next_state.turn, i, j) ) {
						eval[i][j] = TABOO;
						continue;
					}
					System.out.println(hand_list.size() + " " + i +" "+ j);
					tmp.set_hand(i, j);
					calc_state = next_state.test_hand(tmp);
					eval[i][j] = calc_values(calc_state, calc_state.board);
					if ( eval[i][j] > value ) {
						value = eval[i][j];
						//break;
					}
				}
			}
			if ( value < max(eval) ) {
				hand = hand_list.get(k);
				value = max(eval);
			}
		}
		return hand;
	}

}
