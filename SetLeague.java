import sys.game.GameComp;
import sys.main.GogoPlayer;

public class SetLeague
{

  public GameComp[] member_list1;
  public GameComp[] member_list2;

  int list_num;     // リーグ人数
  String file_dir;  // 戦譜の保存場所

  public SetLeague()
  {
    list_num = 2;
    file_dir = "data/record/user";

    member_list1 = new GameComp[list_num];

    //-- strategy list 1
    // 着手確認する戦略を指定
    // member_list1[0] = new data.strategy.sample.Dummy_00(null);
    member_list1[0] = new data.strategy.user.s14t242.User_s14t242_01(null);

    // 対戦相手(ここは変更しないこと)
    member_list1[1] = new data.strategy.sample.Dummy_99(null);
  }

}
