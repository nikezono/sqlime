package net.nikezono.sqlime.translate.dictionary;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;

import hugo.weaving.DebugLog;

/**
 * Created by nikezono on 2015/02/25.
 * @see "http://www.sqlite.org/fts3.html"
 */
public class SQLite3DictionaryAccesor extends SQLiteAssetHelper {

    private static SQLiteDatabase mDatabase;
    private ArrayList<String> mResultSet;

    private static final int DB_VERSION = 4; // @todo autoincrement
    private static final String DB_NAME = "kanakanjidict.db";

    public SQLite3DictionaryAccesor(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        setForcedUpgrade(DB_VERSION);
        mDatabase = this.getReadableDatabase();
        mResultSet = new ArrayList<String>();
    }

    public ArrayList<String> getWordsByYomigana(String yomigana) {
        return getWordsByYomigana(yomigana, 15, 0);
    }

    // @todo 汚い
    // @todo Viewが末端までスクロールされたらページングする
    @DebugLog
    public ArrayList<String> getWordsByYomigana(String yomigana, int limit, int offset) {
        try {
            mDatabase.beginTransaction();
            int length = yomigana.length();
            StringBuilder spaced = new StringBuilder();
            for (int i = 0; length > i; i++) {
                spaced.append(yomigana.charAt(i));
                if (i + 1 != length) {
                    spaced.append(" NEAR/1 ");
                }
            }

            String query = statement
                    .replace("?", spaced)
                    .replace("$", Integer.toString(limit))
                    .replace("#", Integer.toString(offset));
            Log.d("query", query);
            Cursor cursor = mDatabase.rawQuery(query, null);
            mDatabase.endTransaction();

            //カーソル開始位置を先頭にする
            cursor.moveToFirst();

            mResultSet.clear();
            int length2 = cursor.getCount();
            //（.moveToFirstの部分はとばして）for文を回す
            for (int i = 1; i <= length2; i++) {
                //SQL文の結果から、必要な値を取り出す
                String word = cursor.getString(0);
                String yomi = cursor.getString(1);
                int score = cursor.getInt(2);
                mResultSet.add(word);
                cursor.moveToNext();
            }
            return mResultSet;
        } catch (SQLiteException ex) {
            return new ArrayList<String>();
        }
    }

    private final static String statement =
            "SELECT word,yomigana,score FROM candidate " +
            "WHERE yomigana MATCH '^?' " +
            "ORDER BY score ASC " +
            "LIMIT $ " +
            "OFFSET #";

}
