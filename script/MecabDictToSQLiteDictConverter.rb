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
db.execute "DROP TABLE IF EXISTS matrix"
db.execute <<EOS
CREATE VIRTUAL TABLE candidate USING fts4(
  word TEXT,
  yomigana TEXT,
  score INTEGER,
  left_id INTEGER,
  right_id INTEGER,
  part_of_speech TEXT,
  pos_category1 TEXT,
  pos_category2 TEXT
);
EOS

# テーブル作成クエリ
p "candidate TABLE was created"

db.transaction do
  CSV.foreach("./naist-jdic.csv",
    quote_char: "\x00",
    encoding: "euc-jp"
  ) do |row|

    yomigana_kata       = row[11].encode("utf-8")

    if yomigana_kata and !(/'/.match(yomigana_kata))
      candidate      = row[0].encode("utf-8")
      left_id        = row[1].encode("utf-8").to_i
      right_id       = row[2].encode("utf-8").to_i
      frequency      = row[3].encode("utf-8").to_i
      part_of_speech = row[4].encode("utf-8")
      pos_category1  = row[5].encode("utf-8")
      pos_category2  = row[6].encode("utf-8")
      yomigana       = yomigana_kata.tr('ァ-ン','ぁ-ん') # ひらがな化
      yomigana_separated = yomigana.split('').join(' ')

      next if candidate == yomigana

      db.execute <<-SQL
        INSERT INTO candidate(
          word, yomigana, score, left_id, right_id,
          part_of_speech, pos_category1, pos_category2
        )VALUES
        (
          '#{candidate}', '#{yomigana_separated}', #{frequency},
          #{left_id}, #{right_id}, '#{part_of_speech}',
          '#{pos_category1}', '#{pos_category2}'
        );
      SQL

    end
  end
end

p "csv loaded"

db.execute <<-SQL

CREATE TABLE matrix(
  left_id INTEGER,
  right_id INTEGER,
  score INTEGER
);

SQL

p "matrix table created."

db.transaction do
  File.open('matrix.def') do |file|
    file.each_line do |line|
      matrix = line.split(' ').map {|item|item.to_i}
      next if matrix.length != 3

      db.execute <<-SQL
        INSERT INTO matrix(left_id, right_id, score)
        VALUES(#{matrix[0]}, #{matrix[1]}, #{matrix[2]});
      SQL
    end
  end
end
p "matrix.def loaded"

db.execute <<-SQL
  CREATE INDEX left_id  ON matrix(left_id);
SQL
db.execute <<-SQL
  CREATE INDEX right_id ON matrix(right_id);
SQL
db.execute <<-SQL
  CREATE INDEX left_score ON matrix(left_id, score);
SQL

p "index for matrix created."

db.close()
