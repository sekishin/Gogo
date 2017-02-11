package data.strategy.sample;

import sys.game.GameBoard;
import sys.game.GameCompSub;
import sys.game.GameHand;
import sys.game.GamePlayer;
import sys.game.GameState;
import sys.struct.GogoHand;
import sys.user.GogoCompSub;

public class Dummy_99 extends GogoCompSub {
  
//====================================================================
//  コンストラクタ
//====================================================================

  public Dummy_99(GamePlayer player) {
    super(player);
    name = "Dummy99";    // プログラマが入力
    
  }

//--------------------------------------------------------------------
//  コンピュータの着手
//--------------------------------------------------------------------

  public synchronized GameHand calc_hand(GameState state, GameHand hand) {
    //--  着手の決定
    return deside_hand();
  }

//----------------------------------------------------------------
//  着手の決定
//----------------------------------------------------------------

  public GameHand deside_hand() {
    GogoHand hand = new GogoHand();
    hand.set_hand(0, 0);  // 左上をデフォルトのマスとする
    return hand;
  }

}
