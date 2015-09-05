package net.nikezono.sqlime;

/**
 * CandidateWord
 * Mecab辞書基準の単語エントリ
 * Created by nikezono on 2015/09/05.
 */
public class CandidateWord {

    private String mWord;
    private Integer mLeftId;

    public CandidateWord(String word, Integer leftId){
        mWord = word;
        mLeftId = leftId;
    }

    public String getWord(){
        return mWord;
    }
    public Integer getLeftId(){
        return mLeftId;
    }

    @Override
    public String toString() {
        return mWord;
    }
}
