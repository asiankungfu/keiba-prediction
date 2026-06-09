# JRA-VAN 実データ取り込み 設計書

デモ（同梱CSV）から、**JRA-VAN Data Lab. の実データ**へ切り替えるための設計をまとめる。
アプリ本体（推論・画面・API）は変更せず、`RaceDataSource` の実装を差し替えるだけで実データに移行できる構成。

> 利用には **JRA-VAN Data Lab. の会員契約**と **Windows環境**が必要。
> 競馬情報サイトのスクレイピングは規約違反のため使用しない。

## 1. 全体構成

```
[JRA-VAN サーバ]
      │ JV-Data（バイナリ）
      ▼
  JV-Link (Windows ActiveX/COM)
      │ JACOB (Java-COM Bridge)
      ▼
  JvLinkRaceDataSource  ──parse──▶  JvDataParser ──map──▶ Race / RaceEntry / Horse
      │
      ▼
  DataIngestionRunner（既存）──save──▶ PostgreSQL
      │
      ▼
  PredictionService（既存・無変更）──▶ 勝率算出・画面・API
```

ポイント: 取得とパースを `ingest` 層に閉じ込めるため、**service 層から上は一切変更しない**。

## 2. 必要なもの

| 項目 | 内容 |
|------|------|
| 契約 | JRA-VAN Data Lab. 会員（利用キー = サービスID を取得） |
| OS | Windows（JV-Link は ActiveX/COM のため Linux/Mac 不可） |
| ライブラリ | JV-Link（JRA-VAN SDK 同梱）／ JACOB（jacob.jar + jacob-x64.dll） |
| 設定 | `keiba.datasource=jvlink`、サービスIDを環境変数で渡す |

JACOB は再配布制約があるため、リポジトリには含めず `lib/` に各自配置（`.gitignore` 済み）。

## 3. JV-Link 呼び出しフロー（JV-Data 仕様準拠）

```
JVInit("UNKNOWN")                         // 初期化（サービスID登録は JVSetServiceKey）
JVSetUIProperties() / JVSetServiceKey()   // 利用キー設定
status = JVOpen(dataspec, fromtime, opt)  // 蓄積系データの読み出し開始
loop:
    ret = JVRead(buff, size, filename)    // 1レコードずつ読み出し
    if ret == 0: break                    // 全件終了
    if ret == -1: 取得待ち / リトライ
    recordType = buff[0..1]               // レコード種別ID（先頭2バイト）
    dispatch(recordType, buff)            // 種別ごとに構造体へパース
JVClose()
```

速報系（オッズ・直前情報）は `JVRTOpen` を用いる。

## 4. 使用するデータ種別（dataspec / レコード種別）

| 種別ID | 内容 | 用途 |
|--------|------|------|
| `RA` | レース詳細 | レース名・距離・馬場・グレード（→ `Race`） |
| `SE` | 馬毎レース情報 | 出走馬・騎手・斤量・着順（→ `RaceEntry` / 結果） |
| `UM` | 競走馬マスタ | 血統登録番号・馬名・性・生年（→ `Horse`） |
| `HR` | 払戻 | 確定オッズ・配当（任意・評価用） |
| `O1` | 単勝・複勝オッズ（速報系） | 直前オッズ（任意） |

`dataspec` には取得対象（例: `RACE` 一式）を指定し、`fromtime`（例: `20240101000000`）以降の差分を取得する。

## 5. マッピング設計

| JV-Data フィールド | 変換先 |
|------|------|
| RA: RaceInfo.YoubiCD / RaceName | `Race.name`, `Race.raceDate`, `Race.course`, `Race.distanceMeters`, `Race.surface`, `Race.grade` |
| UM: KettoNum / Bamei | `Horse.registrationNumber`, `Horse.name`, `Horse.sex`, `Horse.birthYear` |
| SE: Umaban / KisyuName / Futan / KakuteiJyuni | `RaceEntry.horseNo`, `jockeyName`, `weightKg`, `finishPosition` |
| 集計（SE 過去走） | `RaceEntry.careerWinRate`, `jockeyWinRate`, `conditionAptitude` |

特徴量（通算勝率・騎手勝率・条件適性）は、過去の `SE` レコードを血統登録番号・騎手で集計して
取り込み時に算出する（集計は `FeatureAggregator` を新設）。

## 6. 追加・変更するクラス

| クラス | 役割 | 種別 |
|--------|------|------|
| `JvLinkRaceDataSource` | JV-Link 呼び出し（JVInit→JVOpen→JVRead→JVClose） | 既存スケルトンを実装 |
| `JvLinkClient` | JACOB で COM を薄くラップ（テスト用にIF化） | 新設 |
| `JvDataParser` | バイナリ→種別ごとの構造体 | 新設 |
| `FeatureAggregator` | 過去走から特徴量を集計 | 新設 |
| `RaceDataSource` / `DataIngestionRunner` / service 以上 | **変更なし** | 既存 |

`JvLinkClient` をインターフェースにして JACOB 依存を1点に閉じ込めることで、
パーサ・集計ロジックは COM 無しで単体テストできる。

## 7. バッチ運用

- 取り込みは Spring Batch もしくは `@Scheduled` で定期実行（例: 毎日早朝に前日分の差分取得）。
- 取得済み `fromtime` を保存し、**差分取得**で重複・負荷を抑える。
- レース当日は速報系（`JVRTOpen`）でオッズ・出走取消を反映。

## 8. 宝塚記念で使うまでの手順（実データ）

1. JRA-VAN Data Lab. に会員登録し、サービスIDを取得。
2. Windows機に JV-Link（SDK）と JACOB を導入、`lib/` に配置。
3. `keiba.datasource=jvlink`、`JVLINK_SERVICE_ID` を設定して起動。
4. `RA/SE/UM` を取得 →（必要なら）過去走で特徴量を再学習（`scripts/train_and_evaluate.py` を実データ版に）。
5. 宝塚記念の出走確定後、当日に速報系で最終オッズ・出走を反映して予想を表示。

> 注意: 本アプリの勝率は推定値であり的中・利益を保証しない。馬券購入は自己責任。
