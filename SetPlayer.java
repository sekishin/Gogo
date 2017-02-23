import sys.main.GogoPlayer;

public class SetPlayer
{

  public GogoPlayer player1;
  public GogoPlayer player2;

  public SetPlayer()
  {

    //--  プレイヤ１の指定
    //player1 = new GogoPlayer( null, +1, player1.PERS );    // 人間
    player1 = new GogoPlayer( null, +1, player1.COMP );  // コンピュータ

    //----  戦略バイナリの指定
    //player1.set_comp(new  data.strategy.sample.Dummy_00(player1));
    //player1.set_comp(new  data.strategy.sample.Dummy_10(player1));
    //player1.set_comp(new  data.strategy.sample.Dummy_20(player1));
    //player1.set_comp(new  data.strategy.sample.Dummy_30(player1));
    //player1.set_comp(new  data.strategy.sample.Dummy_40(player1));
    //player1.set_comp(new  data.strategy.sample.Dummy_50(player1));

    player1.set_comp(new data.strategy.user.s14t242.User_s14t242_03(player1));

    //--  プレイヤ２の指定
    //player2 = new GogoPlayer( null, -1, player2.PERS );    // 人間
    player2 = new GogoPlayer( null, -1, player2.COMP );  // コンピュータ

    //----  戦略バイナリの指定
    //player2.set_comp(new  data.strategy.sample.Dummy_00(player2));
    //player2.set_comp(new  data.strategy.sample.Dummy_10(player2));
    //player2.set_comp(new  data.strategy.sample.Dummy_20(player2));
    //player2.set_comp(new  data.strategy.sample.Dummy_30(player2));
    player2.set_comp(new  data.strategy.sample.Dummy_40(player2));
    //player2.set_comp(new  data.strategy.sample.Dummy_50(player2));

    //player2.set_comp(new data.strategy.user.s14t242.User_s14t242_01(player2));

  }
}
