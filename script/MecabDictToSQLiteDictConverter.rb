# coding: utf-8

#
# KanaKanjiDictSQLite3Converter.rb
# Author:nikezono(nikezono@gmail.com)
# Convert MeCab Dictionary To SQLite3 Database
#
require 'csv'
require 'json'
require 'sqlite3'

db = SQLite3::Database.new("./kanakanjidict.db")

p "SQLite3 Database file Connect Done"

# 初期化
db.execute "DROP TABLE IF EXISTS candidate"
db.execute "DROP TABLE IF EXISTS candidate_detail"
db.execute "DROP TABLE IF EXISTS matrix"
db.execute <<EOS
CREATE VIRTUAL TABLE candidate USING fts4(
  id INTEGER,
  word TEXT,
  yomigana TEXT,
  score INTEGER,
  part_of_speech TEXT,
  pos_category1 TEXT,
  pos_category2 TEXT
);
EOS
db.execute <<EOS
CREATE TABLE candidate_detail(
  candidate_id INTEGER,
  lscore INTEGER,
  left_id INTEGER,
  right_id INTEGER
);
EOS

# テーブル作成クエリ
p "candidate TABLE was created"

id = 0
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

      lscore          = ((frequency.to_f / yomigana.size) * 100).to_i

      next if candidate == yomigana
      id = id+1


      db.execute <<-SQL
        INSERT INTO candidate(
          id, word, yomigana, score,
          part_of_speech, pos_category1, pos_category2
        )VALUES
        (
          #{id}, '#{candidate}', '#{yomigana_separated}', #{frequency},
          '#{part_of_speech}', '#{pos_category1}', '#{pos_category2}'
        );
      SQL

      db.execute <<-SQL
        INSERT INTO candidate_detail(
          candidate_id, lscore, left_id, right_id
        )VALUES(
          #{id}, #{lscore}, #{left_id}, #{right_id}
        );
      SQL

    end
  end
end

p "csv loaded"


db.execute "CREATE INDEX candidate_id ON candidate_detail(candidate_id)"
db.execute "CREATE INDEX left_id ON candidate_detail(left_id)"
db.execute "CREATE INDEX right_id ON candidate_detail(right_id)"
db.execute "CREATE INDEX lscore ON candidate_detail(lscore)"

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
  CREATE INDEX left_mat  ON matrix(left_id);
SQL
db.execute <<-SQL
  CREATE INDEX right_mat ON matrix(right_id);
SQL
db.execute <<-SQL
  CREATE INDEX left_score ON matrix(left_id, score);
SQL

p "index for matrix created."

db.close()
