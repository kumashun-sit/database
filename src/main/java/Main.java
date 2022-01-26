import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Main {

    static boolean recommendChecker = false; //おすすめする曲があるかどうかを判定するフラグ

    //MySQLとの接続
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    "jdbc:mysql://localhost/soundcloud",
                    "root",
                    "password"
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    //曲名の入力
    public static String inputSongName() throws IOException {

        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(is);

        System.out.print("聴いている曲 -> ");
        return br.readLine();

    }

    //曲名から曲IDへの変換と曲データの有無のチェック
    public static String convertSongID(Statement smt, String songName) throws SQLException {

        String songID = null;

        String sql = "SELECT 曲ID FROM 曲リスト WHERE 曲名='" + songName + "'";
        ResultSet rs = smt.executeQuery(sql);

        while (rs.next()) {
            songID = rs.getString("曲ID");
        }

        if (songID==null) {
            System.out.println("データベースに曲データが見つかりません.");
            System.exit(0);
        }

        return songID;
    }

    //曲IDから曲名への変換
    public static String convertSongName(Statement smt, String songID) throws SQLException {

        String songName = null;

        String sql = "SELECT 曲名 FROM 曲リスト WHERE 曲ID='" + songID + "'";
        ResultSet rs = smt.executeQuery(sql);

        while (rs.next()) {
            songName = rs.getString("曲名");
        }

        return songName;
    }

    //曲IDから作曲者のユーザー名への変換
    public static String convertComposerName(Statement smt, String songID) throws SQLException {

        String userName = null;

        String sql = "SELECT ユーザー名 FROM ユーザーリスト \n" +
                "WHERE ユーザーID=(SELECT 作曲者のユーザーID FROM 曲リスト WHERE 曲ID='" + songID + "')";

        ResultSet rs = smt.executeQuery(sql);

        while (rs.next()) {
            userName = rs.getString("ユーザー名");
        }

        return userName;

    }

    //おすすめする曲の検索
    public static void recommendSong(Statement smt, String songID) throws SQLException {

        List<String> recommendSongIDList = new ArrayList<>();

        String sql = "SELECT DISTINCT 視聴履歴.曲ID FROM 視聴履歴 JOIN (SELECT 視聴日時, ユーザーID FROM 視聴履歴\n" +
                "WHERE 曲ID ='"+ songID +"') AS 検索用テーブル ON 視聴履歴.ユーザーID = 検索用テーブル.ユーザーID\n" +
                "WHERE 視聴履歴.視聴日時 > 検索用テーブル.視聴日時 GROUP BY 視聴履歴.ユーザーID";

        ResultSet rs = smt.executeQuery(sql);

        while (rs.next()) {
            recommendSongIDList.add(rs.getString("曲ID"));
        }

        if (recommendSongIDList.size()!=0) {
            recommendChecker = true;

            for (int i=0; i<recommendSongIDList.size(); i++) {
                System.out.println( i+1 + ". "
                        + convertComposerName(smt, recommendSongIDList.get(i)) + " - "
                        + convertSongName(smt, recommendSongIDList.get(i)) );
            }
        }

    }


    public static void main(String[] args) throws SQLException, IOException {

        Connection con = getConnection();
        Statement smt = con.createStatement();

        String songName = inputSongName();
        String songID = convertSongID(smt, songName);

        //---------------------------------------------------------------------------//

        System.out.println("\n" + convertComposerName(smt, songID) + " - " +
                songName + " を聴いている人におすすめする曲\n");

        recommendSong(smt, songID);

        if (!recommendChecker) System.out.println("おすすめする曲が見つかりませんでした.");

    }

}