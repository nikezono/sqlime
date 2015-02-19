# coding: utf-8

#
# KanaKanjiDictSQLite3Converter.rb
# Author:nikezono(nikezono@gmail.com)
# Mecab辞書をSQLite3データベースに変換するスクリプト
#
require 'csv'
require 'json'
require 'sqlite3'

db = SQLite3::Database.new("./kanakanjidict.db")

p "SQLite3 Database file Connect Done"

# 初期化
db.execute "DROP TABLE IF EXISTS candidate"
db.execute <<EOS
CREATE VIRTUAL TABLE candidate USING fts4(
  word TEXT,
  yomigana TEXT,
  score INTEGER
);
EOS

# テーブル作成クエリ
p "candidate TABLE was created"

csv = CSV.read "./naist-jdic.csv",
  :quote_char => "\x00", :encoding=>"euc-jp"
p "Dictionary Load Done"

# MecabDictからデータ取得して、逐次DBに投入
queries = {}
csv.each do |row|
  yomigana       = row[11].encode("utf-8")

  if yomigana and !(/'/.match(yomigana))
    candidate      = row[0].encode("utf-8")
    frequency      = row[3].encode("utf-8").to_i
    yomigana      = yomigana.tr('ァ-ン','ぁ-ん') # ひらがな化
    yomigana_separated = yomigana.split('').join(' ')
    query = "INSERT INTO candidate VALUES('#{candidate}','#{yomigana_separated}','#{frequency}')"
    unless queries["#{candidate}:#{yomigana}"]
      queries["#{candidate}:#{yomigana}"] = query
    else
      #p row
      #p queries["#{candidate}:#{yomigana}"]
    end
  end
end
p "query prepared."

db.transaction do
  queries.each {|key,query| db.execute query }
end

p "Done.#{queries.size}s."
db.execute "INSERT INTO candidate(candidate) VALUES('optimize')"

db.close()
