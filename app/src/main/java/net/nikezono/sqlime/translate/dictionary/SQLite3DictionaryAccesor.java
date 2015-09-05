package net.nikezono.sqlime.translate.dictionary;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.googlecode.kanaxs.KanaUtil;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import net.nikezono.sqlime.CandidateWord;

import java.util.ArrayList;

import hugo.weaving.DebugLog;

/**
 * Created by nikezono on 2015/02/25.
 * @see "http://www.sqlite.org/fts3.html"
 */
public class SQLite3DictionaryAccesor extends SQLiteAssetHelper {

    private static SQLiteDatabase mDatabase;
    private ArrayList<CandidateWord> mResultSet;

    private static final int DB_VERSION = 4; // @todo autoincrement
    private static final String DB_NAME = "kanakanjidict.db";

    public SQLite3DictionaryAccesor(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        setForcedUpgrade(DB_VERSION);
        mDatabase = this.getReadableDatabase();
        mResultSet = new ArrayList<CandidateWord>();
    }

    public ArrayList<CandidateWord> getWordsByYomigana(String yomigana) {
        return getWordsByYomigana(yomigana, 30, 0);
    }

    // @todo 汚い
    // @todo Viewが末端までスクロールされたらページングする
    @DebugLog
    public ArrayList<CandidateWord> getWordsByYomigana(String yomigana, int limit, int offset) {
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

            String query = select_statement
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
                Integer leftId = cursor.getInt(1);
                mResultSet.add(new CandidateWord(word,leftId));
                cursor.moveToNext();
            }

            if(mResultSet.isEmpty()) {
                mResultSet.add(new CandidateWord(yomigana,0));
                String katakana = KanaUtil.toKatakanaCase(yomigana);
                mResultSet.add(new CandidateWord(katakana,0));
                mResultSet.add(new CandidateWord(KanaUtil.toHankanaCase(katakana),0));
            }
            return mResultSet;
        } catch (SQLiteException ex) {
            return new ArrayList<CandidateWord>();
        }
    }


    // @todo 汚い
    // @todo Viewが末端までスクロールされたらページングする
    public ArrayList<CandidateWord> getWordsByLastInput(CandidateWord seed) {
        return getWordsByLastInput(seed, 15, 0);
    }
    @DebugLog
    public ArrayList<CandidateWord> getWordsByLastInput(CandidateWord seed, int limit, int offset) {
        try {
            mDatabase.beginTransaction();


            String query = predict_statement
                    .replace("?", seed.getLeftId().toString())
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
                Integer leftId = cursor.getInt(1);
                mResultSet.add(new CandidateWord(word,leftId));
                cursor.moveToNext();
            }
            return mResultSet;
        } catch (SQLiteException ex) {
            return new ArrayList<CandidateWord>();
        }
    }

    private final static String select_statement =
            "SELECT word, left_id FROM candidate " +
            "WHERE yomigana MATCH '^?' " +
            "ORDER BY length(word), score ASC " +
            "LIMIT $ " +
            "OFFSET #";

    private final static String predict_statement =
            "SELECT candidate.word, candidate.left_id " +
            "FROM candidate," +
            "(SELECT right_id AS mright_id, score AS mscore FROM matrix WHERE left_id = ? ORDER BY matrix.score ASC LIMIT 15) AS sq " +
            "WHERE candidate.right_id = sq.mright_id " +
            "ORDER BY sq.mscore+candidate.score ASC " +
            "LIMIT $ " +
            "OFFSET #;";
}
