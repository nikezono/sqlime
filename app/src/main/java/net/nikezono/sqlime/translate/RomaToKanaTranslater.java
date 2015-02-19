package net.nikezono.sqlime.translate;

/**
 * Created by nikezono on 2013/10/29.
 *
 * @todo とてもきたないのでリファクタリング
 */

import java.util.Arrays;
import java.lang.String;
import java.util.Map;
import java.util.HashMap;


public class RomaToKanaTranslater {

    static public String kanaToRoman(String query){
        StringBuilder roman = new StringBuilder();
        roman.setLength(0);
        for(int i=0;i<query.length();i++){
            String kana = query.substring(i,i+1);
            if(kanaToRomanMap.containsKey(kana)) {
                roman.append(kanaToRomanMap.get(kana));
            }else{
                roman.append(query);
            }
        }
        return roman.toString();
    }

    static public String romaToKana(String query){
        String converted = "";

        // 一文字ずつ検出
        int lengthOfQuery = query.length();
        for(int i = 0;i<lengthOfQuery;i++){
            Boolean isVowel = detectVowel("" + query.charAt(i)); // 該当文字が小文字かどうか

            if(isVowel){

                // 一文字目が母音であるときと、convertedの最後の文字が子音でないとき（日本語や記号など)は即変換
                if(i == 0 || !lastIsConst(converted)){

                    String singleKana = convSingle(String.valueOf(query.charAt(i)));
                    converted += singleKana;

                    // 一文字前と二文字前が同じ(その場合"っ"が入る)
                }else if(i>1 && detectDoubleConstants("" + query.charAt(i-2) + query.charAt(i-1)) && lastIsntNN(converted)){

                    String singleCand = "" + query.charAt(i-1) + query.charAt(i);  // 子音と１つ前の文字でクエリ生成（e,g 'tto' -> 'っと'
                    String singleKana = convSingle(singleCand);
                    converted = converted.substring(0,converted.length()-2); // 同じ文字である二文字削除
                    converted += "っ" + singleKana; //ひらがなを追加

                    // 一文字前と二文字前が特殊文字列(e.g 'sh', 'th')のとき、'two' -> 'とぉ'みたいにする
                }else if(i>1 && detectSpecialStrings("" + query.charAt(i-2) + query.charAt(i-1)) && lastIsntNN(converted)){

                    // 特殊文字列の前,三文字前が二文字前と同じとき、"っ"入れる（e.g ssyu -> っしゅ)
                    if(i>2 && detectDoubleConstants("" + query.charAt(i-3) + query.charAt(i-2))){

                        String singleCand = "" + query.charAt(i - 2) + query.charAt(i - 1) + query.charAt(i);  // 子音と１つ前の文字でクエリ生成（e,g 'two' -> 'とぉ'
                        String singleKana = "っ" + convSingle(singleCand);
                        converted = converted.substring(0, converted.length() - 3); // 遡って二文字削除
                        converted += singleKana; //ひらがなを追加

                    }else {

                        String singleCand = "" + query.charAt(i - 2) + query.charAt(i - 1) + query.charAt(i);  // 子音と１つ前の文字でクエリ生成（e,g 'two' -> 'とぉ'
                        String singleKana = convSingle(singleCand);
                        converted = converted.substring(0, converted.length() - 2); // 遡って二文字削除
                        converted += singleKana; //ひらがなを追加
                    }

                    // ただの子音+母音の並びである（はず）
                }else{

                    String singleCand = "" + query.charAt(i-1) + query.charAt(i); // 子音と１つ前の文字でクエリ生成（e,g 'ergba' -> 'ba')
                    String singleKana = convSingle(singleCand); // 単文字ひらがな変換結果を取得
                    converted = converted.substring(0,converted.length()-1); // スルーされていた末尾を削除
                    converted += singleKana; //ひらがなを追加
                }

                // 母音でない文字のとき、一文字前がnなら、「ん」が入る場合がある
            }else if(i != 0 && (detectNCharactor(""+query.charAt(i-1)+query.charAt(i)))){

                converted = converted.substring(0,converted.length()-1);
                converted += "ん" + query.charAt(i);


                // n+nなら"ん"になる。ただし、n+n+nのときは、「ん」判定が二回起きて一文字しか生成できないので弾く
            }else if(i != 0 && (query.charAt(i-1) == 'n' && query.charAt(i) == 'n') && ( (converted.substring(converted.length()-1).equals( "ん")) == false )){
                converted = converted.substring(0,converted.length()-1); //n削除
                converted += "ん";

                // それ以外の特殊な記号の際の日本語変換
            }else if(isSpecialKey(query.charAt(i))){
                String singleKana = convSingle(""+query.charAt(i)); // 単文字ひらがな変換結果を取得
                converted += singleKana; //ひらがなを追加

                // それ以外のとき(子音のみor既に日本語)はそのままアルファベットを突っ込む
            }else{
                converted += query.charAt(i);
            }
        }

        return converted;
    }

    static public String hiraToKomoji(String s) {
        String ret = komojiMap.get(s);
        return ret != null ? ret : s;
    }
    static public String appendHanDakuOn(String s) {
        String ret = handakuonMap.get(s);
        return ret != null ? ret : s;
    }
    static public String appendDakuon(String s) {
        String ret = dakuonMap.get(s);
        return ret != null ? ret : s;
    }
    static public boolean isDakuOn (String s) {
        return dakuonMap.values().contains(s);
    }

    static public boolean isHanDakuOn (String s) {
        return handakuonMap.values().contains(s);
    }

    static public boolean isKomoji (String s) {
        return komojiMap.values().contains(s);
    }
    static public String toKomojiDakuonHandakuon(String s){
        if (isDakuOn(s)){
            String r = dakuonReverseMap.get(s);
            if (handakuonMap.keySet().contains(r)){
                return handakuonMap.get(r);
            }else{
                return dakuonReverseMap.get(s);
            }
        }else if (isHanDakuOn(s)){
            return handakuonReverseMap.get(s);
        }else if (isKomoji(s)){
            String r = komojiReverseMap.get(s);
            if (dakuonMap.keySet().contains(r)){
                return dakuonMap.get(r);
            }else{
                return r;
            }
        }else{
            if (komojiMap.keySet().contains(s)){
                return komojiMap.get(s);
            }else if (dakuonMap.keySet().contains(s)){
                return dakuonMap.get(s);
            }else if (handakuonMap.keySet().contains(s)){
                return handakuonMap.get(s);
            }
        }
        return s;
    }

    // a -> A / A -> a
    static public Character toggleAlphabet (Character c) {
        if (c != Character.toLowerCase(c)){
            return Character.toLowerCase(c);
        }else if(c != Character.toUpperCase(c)){
            return Character.toUpperCase(c);
        }
        return c;
    }

    // 入力された文字がローマ字式の母音(or 長音"-")かどうか識別する
    static private boolean detectVowel(String query){
        return Arrays.asList(vowels).contains(query);
    }
    static private final String[] vowels = {"a","i","u","e","o","-"};

    // 二文字を引数に取り、同じ文字であった場合trueを返す
    static private boolean detectDoubleConstants(String query){
        return (query.charAt(0) == query.charAt(1) && query.charAt(0) != 'n');
    }

    // 二文字を引数に取り、特殊文字列かどうか判別する
    static private boolean detectSpecialStrings(String query){
        return Arrays.asList(specials).contains(query);
    }
    //
    // 特殊文字列
    static private final String[] specials = {"sh","ch","ky","kw","zy","by","cy","dy","dw","dh","fy","gy","hy","lw","ly","my","ny","py","ry","sy","th","tw","ty","vy","wh","xy","xw","ts"};

    // 'n'の扱いはイレギュラーなので、'ん'が最後に来る場合、specialな文字列として扱われる可能性がある
    static private boolean lastIsntNN(String converted){
        return (converted.charAt(converted.length()-2) != 'ん');
    }

    // 最後の文字が子音かどうかの判定
    static private boolean lastIsConst(String converted){
        return (Arrays.asList(constants).contains("" + converted.charAt(converted.length()-1)));
    }

    static private final String[] constants =  {"b","c","d","f","g","h","j","k","l","m","n","p","q","r","s","t","v","w","x","y","z"};

    // 二文字を引数に取り、「ん」に関わるか判断する
    // n+以下の文字のとき「ん+アルファベット」になる('e.g nja -> んじゃ')
    // 子音とyとnが例外
    static private boolean detectNCharactor(String query){
        return ( query.charAt(0) == 'n' && Arrays.asList(nAlphabets).contains("" + query.charAt(1)));
    }

    private static final String[] nAlphabets =  {"b","c","d","f","g","h","j","k","l","m","p","q","r","s","t","v","w","x","z"};


    // 特殊な記号（日本語と相互変換する可能性があるもの ex: ',' -> '、' ）
    private static boolean isSpecialKey(char key){
        return Arrays.asList(specialKeys).contains("" + key);
    }

    private static final String[] specialKeys = {".",","};

    // 一文字の変換
    static private String convSingle(String query){
        String res = romaToKanaMap.get(query);
        if(res == null){
            return query;
        }else{
            return res;
        }
    }


    // 単語変換表
    private static final Map<String, String> romaToKanaMap = new HashMap<String, String>();
    static {
        //あ行
        romaToKanaMap.put("a","あ");
        romaToKanaMap.put("i","い");
        romaToKanaMap.put("u","う");
        romaToKanaMap.put("e","え");
        romaToKanaMap.put("o","お");
        //か行
        romaToKanaMap.put("ka","か");
        romaToKanaMap.put("ki","き");
        romaToKanaMap.put("ku","く");
        romaToKanaMap.put("ke","け");
        romaToKanaMap.put("ko","こ");
        //が行
        romaToKanaMap.put("ga","が");
        romaToKanaMap.put("gi","ぎ");
        romaToKanaMap.put("gu","ぐ");
        romaToKanaMap.put("ge","げ");
        romaToKanaMap.put("go","ご");
        //さ行
        romaToKanaMap.put("sa","さ");
        romaToKanaMap.put("shi","し");
        romaToKanaMap.put("si","し");
        romaToKanaMap.put("ci","し");
        romaToKanaMap.put("su","す");
        romaToKanaMap.put("se","せ");
        romaToKanaMap.put("so","そ");
        //た行
        romaToKanaMap.put("ta","た");
        romaToKanaMap.put("ti","ち");
        romaToKanaMap.put("chi","ち");
        romaToKanaMap.put("tu","つ");
        romaToKanaMap.put("tsu","つ");
        romaToKanaMap.put("te","て");
        romaToKanaMap.put("to","と");
        //だ行
        romaToKanaMap.put("da","だ");
        romaToKanaMap.put("di","ぢ");
        romaToKanaMap.put("du","づ");
        romaToKanaMap.put("de","で");
        romaToKanaMap.put("do","ど");
        //どぁ行
        romaToKanaMap.put("dwa","どぁ");
        romaToKanaMap.put("dwi","どぃ");
        romaToKanaMap.put("dwu","どぅ");
        romaToKanaMap.put("dwe","どぇ");
        romaToKanaMap.put("dwo","どぉ");
        //な行
        romaToKanaMap.put("na","な");
        romaToKanaMap.put("ni","に");
        romaToKanaMap.put("nu","ぬ");
        romaToKanaMap.put("ne","ね");
        romaToKanaMap.put("no","の");
        //は行
        romaToKanaMap.put("ha","は");
        romaToKanaMap.put("hi","ひ");
        romaToKanaMap.put("hu","ふ");
        romaToKanaMap.put("he","へ");
        romaToKanaMap.put("ho","ほ");
        //ば行
        romaToKanaMap.put("ba","ば");
        romaToKanaMap.put("bi","び");
        romaToKanaMap.put("bu","ぶ");
        romaToKanaMap.put("be","べ");
        romaToKanaMap.put("bo","ぼ");
        //ま行
        romaToKanaMap.put("ma","ま");
        romaToKanaMap.put("mi","み");
        romaToKanaMap.put("mu","む");
        romaToKanaMap.put("me","め");
        romaToKanaMap.put("mo","も");
        //や行
        romaToKanaMap.put("ya","や");
        romaToKanaMap.put("yu","ゆ");
        romaToKanaMap.put("yo","よ");
        //ら行
        romaToKanaMap.put("ra","ら");
        romaToKanaMap.put("ri","り");
        romaToKanaMap.put("ru","る");
        romaToKanaMap.put("re","れ");
        romaToKanaMap.put("ro","ろ");
        //わ行
        romaToKanaMap.put("wa","わ");
        romaToKanaMap.put("wi","うぃ");
        romaToKanaMap.put("wu","う");
        romaToKanaMap.put("we","うぇ");
        romaToKanaMap.put("wo","を");
        //きゃ行
        romaToKanaMap.put("kya","きゃ");
        romaToKanaMap.put("kyi","きぃ");
        romaToKanaMap.put("kyu","きゅ");
        romaToKanaMap.put("kye","きぇ");
        romaToKanaMap.put("kyo","きょ");
        //しゃ行
        romaToKanaMap.put("sha","しゃ");
        romaToKanaMap.put("shu","しゅ");
        romaToKanaMap.put("she","しぇ");
        romaToKanaMap.put("sho","しょ");
        //ざ行
        romaToKanaMap.put("za","ざ");
        romaToKanaMap.put("zi","じ");
        romaToKanaMap.put("zu","ず");
        romaToKanaMap.put("ze","ぜ");
        romaToKanaMap.put("zo","ぞ");
        //ヴァ行
        romaToKanaMap.put("va","ヴぁ");
        romaToKanaMap.put("vi","ヴぃ");
        romaToKanaMap.put("vu","ヴ");
        romaToKanaMap.put("ve","ヴぇ");
        romaToKanaMap.put("vo","ヴぉ");
        //くぁ行
        romaToKanaMap.put("qa","くぁ");
        romaToKanaMap.put("qi","くぃ");
        romaToKanaMap.put("qu","く");
        romaToKanaMap.put("qe","くぇ");
        romaToKanaMap.put("qo","くぉ");
        romaToKanaMap.put("kwa","くぁ");
        romaToKanaMap.put("kwi","くぃ");
        romaToKanaMap.put("kwu","kwう");
        romaToKanaMap.put("kwe","くぇ");
        romaToKanaMap.put("kwo","くぉ");
        //じゃ行
        romaToKanaMap.put("ja","じゃ");
        romaToKanaMap.put("ji","じ");
        romaToKanaMap.put("ju","じゅ");
        romaToKanaMap.put("je","じぇ");
        romaToKanaMap.put("jo","じょ");
        romaToKanaMap.put("zya","じゃ");
        romaToKanaMap.put("zyi","じぃ");
        romaToKanaMap.put("zyu","じゅ");
        romaToKanaMap.put("zye","じぇ");
        romaToKanaMap.put("zyo","じょ");
        //ちゃ行
        romaToKanaMap.put("tya","ちゃ");
        romaToKanaMap.put("tyi","ちぃ");
        romaToKanaMap.put("tyu","ちゅ");
        romaToKanaMap.put("tye","ちぇ");
        romaToKanaMap.put("tyo","ちょ");
        romaToKanaMap.put("cha","ちゃ");
        romaToKanaMap.put("chu","ちゅ");
        romaToKanaMap.put("che","ちぇ");
        romaToKanaMap.put("cho","ちょ");
        //にゃ行
        romaToKanaMap.put("nya","にゃ");
        romaToKanaMap.put("nyi","にぃ");
        romaToKanaMap.put("nyu","にゅ");
        romaToKanaMap.put("nye","にぇ");
        romaToKanaMap.put("nyo","にょ");
        //ひゃ行
        romaToKanaMap.put("hya","ひゃ");
        romaToKanaMap.put("hyi","ひぃ");
        romaToKanaMap.put("hyu","ひゅ");
        romaToKanaMap.put("hye","ひぇ");
        romaToKanaMap.put("hyo","ひょ");
        //みゃ行
        romaToKanaMap.put("mya","みゃ");
        romaToKanaMap.put("myi","みぃ");
        romaToKanaMap.put("myu","みゅ");
        romaToKanaMap.put("mye","みぇ");
        romaToKanaMap.put("myo","みょ");
        //りゃ行
        romaToKanaMap.put("rya","りゃ");
        romaToKanaMap.put("ryi","りぃ");
        romaToKanaMap.put("ryu","りゅ");
        romaToKanaMap.put("rye","りぇ");
        romaToKanaMap.put("ryo","りょ");
        //びゃ行
        romaToKanaMap.put("bya","びゃ");
        romaToKanaMap.put("byi","びぃ");
        romaToKanaMap.put("byu","びゅ");
        romaToKanaMap.put("bye","びぇ");
        romaToKanaMap.put("byo","びょ");
        //ぴゃ行
        romaToKanaMap.put("pya","ぴゃ");
        romaToKanaMap.put("pyi","ぴぃ");
        romaToKanaMap.put("pyu","ぴゅ");
        romaToKanaMap.put("pye","ぴぇ");
        romaToKanaMap.put("pyo","ぴょ");
        //ぢゃ行
        romaToKanaMap.put("dya","ぢゃ");
        romaToKanaMap.put("dyi","ぢぃ");
        romaToKanaMap.put("dyu","ぢゅ");
        romaToKanaMap.put("dye","ぢぇ");
        romaToKanaMap.put("dyo","ぢょ");
        //ふぁ行
        romaToKanaMap.put("fa","ふぁ");
        romaToKanaMap.put("fi","ふぃ");
        romaToKanaMap.put("fu","ふ");
        romaToKanaMap.put("fe","ふぇ");
        romaToKanaMap.put("fo","ふぉ");
        //ぱ行
        romaToKanaMap.put("pa","ぱ");
        romaToKanaMap.put("pi","ぴ");
        romaToKanaMap.put("pu","ぷ");
        romaToKanaMap.put("pe","ぺ");
        romaToKanaMap.put("po","ぽ");
        //ぎゃ行
        romaToKanaMap.put("gya","ぎゃ");
        romaToKanaMap.put("gyi","ぎぃ");
        romaToKanaMap.put("gyu","ぎゅ");
        romaToKanaMap.put("gye","ぎぇ");
        romaToKanaMap.put("gyo","ぎょ");
        //しゃ行
        romaToKanaMap.put("sya","しゃ");
        romaToKanaMap.put("syi","しぃ");
        romaToKanaMap.put("syu","しゅ");
        romaToKanaMap.put("sye","しぇ");
        romaToKanaMap.put("syo","しょ");
        //小文字
        romaToKanaMap.put("xa","ぁ");
        romaToKanaMap.put("xi","ぃ");
        romaToKanaMap.put("xu","ぅ");
        romaToKanaMap.put("xe","ぇ");
        romaToKanaMap.put("xo","ぉ");
        romaToKanaMap.put("la","ぁ");
        romaToKanaMap.put("li","ぃ");
        romaToKanaMap.put("lu","ぅ");
        romaToKanaMap.put("le","ぇ");
        romaToKanaMap.put("lo","ぉ");
        romaToKanaMap.put("xya","ゃ");
        romaToKanaMap.put("xyi","ぃ");
        romaToKanaMap.put("xyu","ゅ");
        romaToKanaMap.put("xye","ぇ");
        romaToKanaMap.put("xyo","ょ");
        romaToKanaMap.put("xwa","ゎ");
        romaToKanaMap.put("lya","ゃ");
        romaToKanaMap.put("lyi","ぃ");
        romaToKanaMap.put("lyu","ゅ");
        romaToKanaMap.put("lye","ぇ");
        romaToKanaMap.put("lyo","ょ");
        romaToKanaMap.put("lwa","ゎ");
        //その他
        romaToKanaMap.put("fyu","ふゅ");

        //長音と特殊記号
        romaToKanaMap.put("-","ー");
        romaToKanaMap.put(",","、");
        romaToKanaMap.put(".","。");

    }

    private static final Map<String, String> komojiMap = new HashMap<String, String>();
    static {
        komojiMap.put("あ","ぁ");
        komojiMap.put("い","ぃ");
        komojiMap.put("う","ぅ");
        komojiMap.put("え","ぇ");
        komojiMap.put("お","ぉ");
        komojiMap.put("つ","っ");
        komojiMap.put("や","ゃ");
        komojiMap.put("ゆ","ゅ");
        komojiMap.put("よ","ょ");
    }
    private static final Map<String, String> komojiReverseMap = new HashMap<String, String>();
    static {
        for (Map.Entry entry : komojiMap.entrySet()){
            komojiReverseMap.put(entry.getValue().toString(), entry.getKey().toString());
        }
    }
    private static final Map<String, String> dakuonMap = new HashMap<String, String>();
    static {
        dakuonMap.put("か", "が");
        dakuonMap.put("き", "ぎ");
        dakuonMap.put("く", "ぐ");
        dakuonMap.put("け", "げ");
        dakuonMap.put("こ", "ご");
        dakuonMap.put("さ", "ざ");
        dakuonMap.put("し", "じ");
        dakuonMap.put("す", "ず");
        dakuonMap.put("せ", "ぜ");
        dakuonMap.put("そ", "ぞ");
        dakuonMap.put("た", "だ");
        dakuonMap.put("ち", "ぢ");
        dakuonMap.put("つ", "づ");
        dakuonMap.put("て", "で");
        dakuonMap.put("と", "ど");
        dakuonMap.put("は", "ば");
        dakuonMap.put("ひ", "び");
        dakuonMap.put("ふ", "ぶ");
        dakuonMap.put("へ", "べ");
        dakuonMap.put("ほ", "ぼ");
    }
    private static final Map<String, String> dakuonReverseMap = new HashMap<String, String>();
    static {
        for (Map.Entry entry : dakuonMap.entrySet()){
            dakuonReverseMap.put(entry.getValue().toString(), entry.getKey().toString());
        }
    }
    private static final Map<String, String> handakuonMap = new HashMap<String, String>();
    static {
        handakuonMap.put("は", "ぱ");
        handakuonMap.put("ひ", "ぴ");
        handakuonMap.put("ふ", "ぷ");
        handakuonMap.put("へ", "ぺ");
        handakuonMap.put("ほ", "ぽ");
    }
    private static final Map<String, String> handakuonReverseMap = new HashMap<String, String>();
    static {
        for (Map.Entry entry : handakuonMap.entrySet()){
            handakuonReverseMap.put(entry.getValue().toString(), entry.getKey().toString());
        }
    }

    private static final Map<String, String> kanaToRomanMap = new HashMap<String, String>();
    static {
        for (Map.Entry entry : romaToKanaMap.entrySet()) {
            kanaToRomanMap.put(entry.getValue().toString(),entry.getKey().toString());
        }
    }

}
