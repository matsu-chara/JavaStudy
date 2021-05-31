https://koduki.github.io/docs/book-introduction-of-jfr/site/01/02-other_tools.html

## JPLIS(javaagent)

Java Programming Language Instrumentation Servicesの略

クラスロード時にエージェントがクラス情報を書き換えるAPI
- 情報取得
- プロファイリング用アスペクトの処理(traceとか）を埋め込む

## ツール

- java visualVM
  - jmxの情報をGUIで見られる
  - previewでJFR対応あり
  - JMCがhotspotに同梱されるようになって影が薄いらしい
- jdk mission control
  - https://github.com/JDKMissionControl/jmc
  - JFRのvisualizeとしてはほぼ一択
- JFR tool
  - JDK12から導入された新しいツール
  - JFRをjsonやxmlにできる
- jcmd
  - JFRの操作, プロセスの取得, スレッドダンプの取得などができる

![img.png](img.png)

https://koduki.github.io/docs/book-introduction-of-jfr/site/01/02-other_tools.html

もう古い
- jconsole
  - visualVMでおｋ
- hprof
  - オーバーヘッドが大きいので本番向きではない（開発・テストならIDEのものでよい）
- jps
  - psコマンドで良い（が、javaだけにフィルターされるので便利といえば便利）
- jstat
  - GCを含むヒープを見れる
- jstack
  - thread dumpを取得する
  - 定期実行をするスクリプトを組むか、障害時に数回叩いてスレッドの情報を取得する
- jmap
  - heap dumpを取得する
  - ヒープサマリー、ヒストグラム、統計情報などがみられる
  - fullGC注意
