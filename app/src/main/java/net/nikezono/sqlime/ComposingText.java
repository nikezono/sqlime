package net.nikezono.sqlime;

import com.google.common.base.Strings;

import net.nikezono.sqlime.translate.RomaToKanaTranslater;


import hugo.weaving.DebugLog;

/**
 * ComposingText
 * Created by nikezono on 2015/02/15.
 */
public class ComposingText {

    private StringBuilder mInputtedString; // キーボードから入力された単語列
    private StringBuilder mConvertedString; // 日本語
    private StringBuilder mNotConvertedAlphabets; // 変換出来ていないアルファベット列

    public ComposingText(){
        mInputtedString = new StringBuilder();
        mConvertedString = new StringBuilder();
        mNotConvertedAlphabets = new StringBuilder();
    }

    // 一文字入力
    @DebugLog
    public void input(String s){
        mInputtedString.append(s);
        convert(s);
    }

    // ローマ字かな変換を行い結果を保存
    public void convert(String s) {
        String query;
        String converted;
        if(mNotConvertedAlphabets.length() > 0){
            query = mNotConvertedAlphabets+s;
            converted = RomaToKanaTranslater.romaToKana(query);
        }else{
            query = s;
            converted = RomaToKanaTranslater.romaToKana(s);
        }
        if(converted.equals(query)){
            mNotConvertedAlphabets.append(s);
        }else{
            mConvertedString.append(converted);
            mNotConvertedAlphabets.setLength(0);
        }
    }

    // 初期化
    public void clear() {
        mInputtedString.setLength(0);
        mConvertedString.setLength(0);
        mNotConvertedAlphabets.setLength(0);
    }

    // Backspace
    public void backspace(){
        int length = mNotConvertedAlphabets.length();
        if(length>0) {
            mNotConvertedAlphabets.deleteCharAt(length-1);
            return;
        }
        length = mConvertedString.length();
        if(length>0) {
            mConvertedString.deleteCharAt(length - 1);
        }
    }
    public Boolean hasComposingText(){
        return !Strings.isNullOrEmpty(mInputtedString.toString());
    }
    public String getLastConverted(){
        if(!hasComposingText()) return "";
        String converted = getConvertedString();
        return getConvertedString().substring(converted.length()-1);
    }
    public String getLastInputted(){
        if(!hasComposingText()) return "";
        return mInputtedString.substring(mInputtedString.length()-1);
    }
    public String getInputtedString(){
        return mInputtedString.toString();
    }
    public String getConvertedString(){
        return mConvertedString.toString() + mNotConvertedAlphabets.toString();
    }


}
