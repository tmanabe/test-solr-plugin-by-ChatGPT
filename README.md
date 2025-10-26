# test-solr-plugin-by-ChatGPT

ChatGPT が意外と Lucene / Solr に詳しいので、Solr プラグインを書かせてみたものです。


## 要件

### 非機能
- Gradle
- Solr 9.4.0

### 機能
- DocValues に条件式をつける
    - 条件の ID を葉
    - `{AND, OR, NOT}` を内部節点
- クエリで
    - 真の条件の ID のリストを渡すと、
    - マッチする条件式のついたドキュメントのみを返す
