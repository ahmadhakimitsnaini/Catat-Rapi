# CatatRapi — Personal Wealth Manager

**CatatRapi** adalah aplikasi *Personal Wealth Manager* berbasis Android (*Offline-First*) yang mengintegrasikan pencatatan arus kas harian, tabungan berbasis target (*Goal-Based Saving*), dan manajemen portofolio investasi ke dalam satu model data terpadu. Setiap rupiah yang Anda miliki selalu memiliki status yang jelas: **Bebas Pakai (*Spendable*)**, **Dialokasikan (*Allocated*)**, atau **Diinvestasikan (*Invested*)**.

---

## Daftar Isi
1. [Latar Belakang & Proposisi Nilai](#1-latar-belakang--proposisi-nilai)
2. [Konsep Inti: Alokasi vs Konversi](#2-konsep-inti-alokasi-vs-konversi)
3. [Arsitektur Sistem & Alur Data](#3-arsitektur-sistem--alur-data)
4. [Flowchart Sistem & Alur Kerja Antar-Modul](#4-flowchart-sistem--alur-kerja-antar-modul)
5. [Fitur Utama (4 Pilar Modul)](#5-fitur-utama-4-pilar-modul)
6. [Skema Database (Room ERD)](#6-skema-database-room-erd)
7. [Tech Stack & Keamanan](#7-tech-stack--keamanan)
8. [Roadmap Pengembangan](#8-roadmap-pengembangan)
9. [Dokumentasi Terkait](#9-dokumentasi-terkait)

---

## 1. Latar Belakang & Proposisi Nilai

Kebanyakan aplikasi pencatat keuangan memisahkan antara saldo kas, tabungan tujuan, dan portofolio investasi, yang memicu tiga masalah utama:
* **Saldo Menyesatkan (*Illusion of Balance*):** Total saldo rekening terlihat besar padahal sebagian dana sudah "dijanjikan" untuk dana darurat, cicilan, atau liburan—memicu *overspending* tanpa sadar.
* **Silo Fitur:** Pengeluaran hari ini tidak memberikan umpan balik langsung terhadap mundurnya tanggal tercapainya target tabungan (*ETA*).
* **Kekayaan Bersih (*Net Worth*) Tidak Terlihat:** Kas dan aset investasi tercatat di aplikasi berbeda sehingga sulit memantau pertumbuhan kekayaan secara utuh.

### Nilai Jual Utama CatatRapi

| Pilar Sistem | Interkoneksi Otomatis | Dampak Nyata bagi Pengguna |
| :--- | :--- | :--- |
| **Core Finance Tracker** | Setiap transaksi langsung memperbarui *Spendable Balance* | Tahu persis berapa nominal uang yang benar-benar aman dibelanjakan hari ini |
| **Goal-Based Saving Engine** | Pengeluaran berlebih memicu *Impact Warning* & menghitung ulang *Dynamic Saving Rate* | Melihat langsung berapa hari target tabungan mundur akibat belanja berlebih |
| **Investment Portfolio Tracker** | Pembelian aset (*Asset Conversion*) memotong kas dan menambah unit portofolio secara atomik | *Average Buy Price* (DCA), *Floating P/L*, dan *Net Worth* selalu akurat tanpa input ganda |

---

## 2. Konsep Inti: Alokasi vs Konversi

Untuk menjaga konsistensi akuntansi dan mencegah penghitungan ganda (*double-counting*) pada **Net Worth**, CatatRapi membedakan perpindahan dana menjadi dua mekanisme:

1. **Alokasi (*Saving Goals*):** Dana yang disetor ke Target Tabungan **tetap berada di dalam dompet fisik/rekening** ($W$ tidak berubah), namun dikunci sehingga mengurangi **Spendable Balance**.
2. **Konversi (*Investment Portfolio*):** Dana yang dibelikan aset investasi **keluar dari saldo dompet** ($W$ berkurang) dan berubah wujud menjadi kuantitas unit aset ($Q_a \times P_{\text{mkt},a}$).

$$\text{Spendable Balance} = W - G - B_{\min}$$

$$\text{Net Worth} = \underbrace{\left(\text{Spendable} + B_{\min}\right)}_{\text{Kas Bebas}} + \underbrace{G}_{\text{Dana Target}} + \underbrace{\sum_{a \in \text{Aset}} Q_a \cdot P_{\text{mkt},a}}_{\text{Nilai Pasar Investasi}}$$

```mermaid
flowchart LR
    subgraph W["Total Saldo Dompet Aktif (W)"]
        direction TB
        S["Spendable Balance\n(Saldo Bebas Pakai)"]
        B["Buffer Minimum (B_min)"]
        G["Dana Teralokasi Target (G)\nΣ GoalContributions"]
    end

    subgraph INV["Portofolio Investasi"]
        P["Nilai Pasar Aset\nΣ (Kuantitas × Harga Pasar)"]
    end

    S <-- "Alokasi / Tarik Dana\n(W Tetap)" --> G
    S -- "Beli Aset (Asset Conversion)\n(W Berkurang)" --> P
    P -- "Jual Aset + Realized P/L\n(W Bertambah)" --> S

    W & INV --> NW(["Total Net Worth\n(Kekayaan Bersih Utuh)"])
```

---

## 3. Arsitektur Sistem & Alur Data

CatatRapi dibangun menggunakan **Clean Architecture** dengan pola **MVVM (Model-View-ViewModel)** dan prinsip **Offline-First**. Ketergantungan kode hanya mengarah ke dalam ($\text{UI} \rightarrow \text{Domain} \leftarrow \text{Data}$), dan **Room Database** bertindak sebagai *Single Source of Truth* (SSOT).

```mermaid
flowchart TD
    subgraph UI["UI Layer (Presentation)"]
        CS["Jetpack Compose Screens\n(Material 3 + Vico Charts)"]
        VM["ViewModel\n(StateFlow & UI State)"]
        CS -- "User Event / Action" --> VM
        VM -- "StateFlow (collectAsStateWithLifecycle)" --> CS
    end

    subgraph DOMAIN["Domain Layer (Pure Kotlin)"]
        UC["UseCases\n(Rumus Keuangan & Orkestrasi Bisnis)"]
        DM["Domain Models\n(Wallet, Goal, Asset, NetWorth)"]
        RI["Repository Interfaces\n(Kontrak Akses Data)"]
        VM --> UC
        UC --> RI
        UC -.-> DM
    end

    subgraph DATA["Data Layer (Offline-First)"]
        REPO["Repository Implementations"]
        ROOM[("Room Database + SQLCipher\n(Single Source of Truth)")]
        API["Retrofit + OkHttp\n(Market Price API)"]
        WM["WorkManager\n(Background Sync & Evaluasi Harian)"]

        REPO -. "Implements" .-> RI
        REPO -- "@Transaction / Read Flow" --> ROOM
        ROOM -- "Reactive Flow" --> REPO
        REPO -- "Fetch Market Price" --> API
        API -- "Simpan Harga & Timestamp ke Cache" --> ROOM
        WM -- "Trigger Periodik" --> UC
    end
```

---

## 4. Flowchart Sistem & Alur Kerja Antar-Modul

Diagram berikut menggambarkan bagaimana aktivitas harian pengguna (mencatat pengeluaran, menabung ke target, atau membeli aset investasi) mengalir secara atomik dan memperbarui indikator kesehatan finansial secara *real-time*:

```mermaid
flowchart TD
    START(["Pengguna Membuka Aplikasi"]) --> AUTH{"Autentikasi Biometrik\n/ PIN Berhasil?"}
    AUTH -- "Gagal" --> LOCK["Layar Terkunci\n(BiometricPrompt)"]
    LOCK --> AUTH
    AUTH -- "Berhasil" --> DASH["Dashboard Utama\n(Net Worth, Spendable, Goals, Distribusi Aset)"]

    DASH --> ACT{"Pilih Aksi Transaksi"}

    %% Alur 1: Pengeluaran Harian & Impact Warning
    ACT -- "1. Catat Pengeluaran" --> EXP["Input Nominal & Kategori\n(@Transaction Room)"]
    EXP --> UPD_W1["Kurangi Wallets.balance (W)\n& Spendable Balance"]
    UPD_W1 --> CHK_BUDGET{"Melewati 80% / 100%\nCategoryBudgets?"}
    CHK_BUDGET -- "Ya" --> NOTIF_B["Kirim Notifikasi Batas Anggaran"]
    CHK_BUDGET -- "Tidak" --> CHK_IMPACT
    NOTIF_B --> CHK_IMPACT{"Pengeluaran Hari Ini (x)\n> Rata-rata 30 Hari (μ)?"}
    CHK_IMPACT -- "Ya (Δ > 0 & Hari Mundur >= 1)" --> WARN["Tampilkan Banner Impact Warning\n& Hitung Mundurnya ETA Target"]
    CHK_IMPACT -- "Tidak" --> RECALC_NW

    %% Alur 2: Goal-Based Saving & Auto-Sweep
    ACT -- "2. Setor Target / Auto-Sweep" --> GOAL_CHK{"Nominal <= Spendable?"}
    GOAL_CHK -- "Tidak" --> ERR1["Tolak Transaksi:\nSaldo Bebas Pakai Tidak Cukup"]
    GOAL_CHK -- "Ya" --> GOAL_TX["Insert GoalContributions\n(W Tetap, G Bertambah, Spendable Turun)"]
    GOAL_TX --> GOAL_RATE["Hitung Ulang Dynamic Saving Rate,\nSaving Velocity (v), & Status Target"]
    GOAL_RATE --> RECALC_NW

    %% Alur 3: Investasi (Asset Conversion)
    ACT -- "3. Beli Aset Investasi" --> INV_CHK{"(Qty × Harga + Fee)\n<= Spendable?"}
    INV_CHK -- "Tidak" --> ERR1
    INV_CHK -- "Ya" --> INV_TX["@Transaction Atomik:\n1. Insert Transactions (INVESTMENT_BUY)\n2. Kurangi Wallets.balance (W)\n3. Upsert Portfolios (Qty & Average Buy Price DCA)\n4. Insert InvestmentLogs"]
    INV_TX --> RECALC_NW

    WARN --> RECALC_NW["Perbarui StateFlow Dashboard:\nSpendable, Distribusi Aset & Total Net Worth"]
    RECALC_NW --> DASH
```

---

## 5. Fitur Utama (4 Pilar Modul)

### Modul 1: Core Finance Tracker & Wallet Management
* **Multi-Wallet Management (`FT-01`):** Kelola dompet Kas, Bank, dan E-Wallet secara terpisah dengan fitur pengarsipan aman.
* **Spendable Balance Real-Time (`FT-02`, `FT-03`):** Memisahkan Total Saldo ($W$) dengan Saldo Bebas Pakai ($\text{Spendable} = W - G - B_{\min}$).
* **Transfer Atomik (`FT-04`):** Perpindahan dana antar-dompet dalam satu transaksi database tanpa mengacaukan laporan pengeluaran (kecuali biaya admin).
* **Anggaran Kategori & Transaksi Berulang (`FT-05`, `FT-07`):** Peringatan otomatis pada batas anggaran $80\%$ dan $100\%$, serta pencatatan gaji/langganan otomatis via `WorkManager`.

### Modul 2: Goal-Based Saving Engine
* **Dynamic Saving Rate (`GS-01` – `GS-03`):** Menghitung kebutuhan setoran harian dan mingguan secara dinamis mengikuti sisa hari menuju tenggat.
* **Saving Velocity, ETA & Impact Warning (`GS-04`):** Memantau kecepatan menabung 30 hari terakhir ($v$) dan memberi peringatan konkret berapa hari target mundur ketika belanja harian membengkak ($\Delta > 0$).
* **One-Tap Auto-Sweep Surplus (`GS-05`, `GS-07`):** Memindahkan sisa dana bebas pakai di akhir periode ($\text{Spendable} - B_{\text{sweep}}$) ke target berprioritas tertinggi dalam satu ketukan (dilengkapi *Undo* 10 detik).

### Modul 3: Investment Portfolio Tracker
* **Multi-Asset Portfolio (`IV-01`):** Mendukung Saham (`lot`), Reksadana (`unit`), Emas (`gram`), dan Kripto (`koin`) dengan presisi desimal `BigDecimal`.
* **Atomic Asset Conversion & DCA (`IV-02` – `IV-04`):** Pembelian aset langsung memotong saldo dompet dan menghitung ulang *Average Buy Price* (termasuk biaya transaksi) secara otomatis. Penjualan aset mengembalikan kas dan mencatat *Realized P/L*.
* **Offline-First Valuation (`IV-05` – `IV-07`):** Menampilkan *Floating Profit/Loss* dan ROI menggunakan sinkronisasi API harga pasar atau input manual dengan mekanisme *fallback* bertingkat saat tanpa internet.

### Modul 4: Unified Dashboard & Net Worth Analytics
* **Total Net Worth & Distribusi Aset (`DB-01`, `DB-02`):** Satu angka kekayaan bersih akurat beserta grafik donat komposisi kekayaan (Kas Bebas, Dana Target, dan Kelas Aset Investasi).
* **Laporan Visual & Tren Kekayaan (`DB-03` – `DB-05`):** Grafik tren *Net Worth* historis (`1B/3B/1T/Semua`), evaluasi *Savings Rate* bulanan, serta ekspor laporan ke CSV/PDF.

---

## 6. Skema Database (Room ERD)

Seluruh nominal uang disimpan sebagai `Long` (Rupiah) dan kuantitas/harga aset sebagai `BigDecimal` (disimpan sebagai `TEXT` melalui `TypeConverter`) untuk menghindari galat *floating-point*.

```mermaid
erDiagram
    Wallets ||--o{ Transactions : "wallet_id (RESTRICT)"
    Wallets ||--o{ RecurringTransactions : "wallet_id (RESTRICT)"
    Wallets ||--o{ GoalContributions : "wallet_id (SET_NULL)"
    Wallets ||--o{ InvestmentLogs : "wallet_id (RESTRICT)"
    SavingGoals ||--|{ GoalContributions : "goal_id (CASCADE)"
    Portfolios ||--|{ InvestmentLogs : "portfolio_id (RESTRICT)"
    Transactions ||--o| InvestmentLogs : "transaction_id (1:1)"

    Wallets {
        Long id PK
        String name
        String type "CASH | BANK | EWALLET"
        Long balance
        Boolean is_archived
        Long created_at
    }

    Transactions {
        Long id PK
        Long wallet_id FK
        Long to_wallet_id FK "Nullable (TRANSFER)"
        String type "INCOME | EXPENSE | TRANSFER | INVESTMENT_BUY | INVESTMENT_SELL"
        Long amount
        Long fee "Default 0"
        String category
        String note
        Long occurred_at
        Long created_at
    }

    SavingGoals {
        Long id PK
        String name
        Long target_amount
        Long deadline
        Int priority
        String status "ON_TRACK | DELAYED | ACHIEVED"
        Boolean is_archived
        Long created_at
    }

    GoalContributions {
        Long id PK
        Long goal_id FK
        Long wallet_id FK
        Long amount "+Deposit / -Withdraw"
        String type "DEPOSIT | WITHDRAW | AUTO_SWEEP"
        Long created_at
    }

    Portfolios {
        Long id PK
        String asset_type "STOCK | MUTUAL_FUND | GOLD | CRYPTO"
        String symbol
        String name
        String unit "lot | unit | gram | koin"
        BigDecimal quantity
        BigDecimal avg_buy_price
        BigDecimal last_price
        Long last_price_at
        String price_source "MANUAL | API"
    }

    InvestmentLogs {
        Long id PK
        Long portfolio_id FK
        Long wallet_id FK
        Long transaction_id FK
        String type "BUY | SELL"
        BigDecimal quantity
        BigDecimal price
        Long fee
        Long realized_pl
        Long created_at
    }

    CategoryBudgets {
        Long id PK
        String category
        String year_month "YYYY-MM"
        Long limit_amount
        Boolean notified_80
        Boolean notified_100
        Long updated_at
    }

    RecurringTransactions {
        Long id PK
        Long wallet_id FK
        String type "INCOME | EXPENSE"
        Long amount
        String category
        String note
        String frequency "DAILY | WEEKLY | MONTHLY"
        Long next_run_at
        Long last_run_at
        Boolean is_active
        Long created_at
    }

    NetWorthSnapshots {
        Long date PK "Epoch Day"
        Long cash
        Long investment
        Long net_worth
    }
```

---

## 7. Tech Stack & Keamanan

| Kategori | Teknologi / Library | Keterangan |
| :--- | :--- | :--- |
| **Bahasa & Concurrency** | Kotlin, Coroutines, `StateFlow` | Pemrograman reaktif & *asynchronous* non-blocking |
| **Minimum SDK** | Android 8.0 (`minSdk 26`) | Kompatibilitas modern & dukungan penuh API tanggal/waktu |
| **UI Toolkit** | Jetpack Compose (Material 3) | Deklaratif, *stateless*, mendukung tema gelap/terang & *font scaling* |
| **Visualisasi Grafik** | Vico Chart | Grafik donat distribusi aset, batang arus kas, & garis tren *Net Worth* |
| **Dependency Injection** | Hilt (Dagger) | Injeksi dependensi terstandarisasi pada `ViewModel`, `Repository`, dan `WorkManager` |
| **Local Database** | Room + Paging 3 | Penyimpanan *Offline-First* dengan performa $\le 500\text{ ms}$ untuk $10.000$ transaksi |
| **Networking** | Retrofit + OkHttp | Sinkronisasi harga pasar investasi dengan *offline caching* |
| **Background Processing** | WorkManager | Sinkronisasi harga periodik, evaluasi anggaran harian, & transaksi berulang |
| **Keamanan Lokal** | `BiometricPrompt`, SQLCipher, Android Keystore, `FLAG_SECURE` | Enkripsi database penuh, kunci biometrik/PIN, dan pencegahan *screenshot* di layar finansial |

---

## 8. Roadmap Pengembangan

```mermaid
flowchart LR
    F1["Fase 1: MVP Core (Sprint 1–4)\n• Setup Clean Arch, Compose, Room, Hilt\n• Multi-Wallet & Transaksi Atomik\n• Spendable Balance & Transfer\n• Goal-Based Saving & Dynamic Rate\n• Impact Warning, Auto-Sweep & Biometrik"]
    G1{"Quality Gate 1\nUji Unit Rumus 2.2\n(>90% Coverage)"}
    F2["Fase 2: Investasi & Net Worth (Sprint 5–7)\n• Portofolio Multi-Aset & Harga Manual\n• Asset Conversion Atomik (Beli/Jual)\n• Average Buy Price (DCA) & Floating P/L\n• Total Net Worth, Distribusi Aset & Analytics"]
    G2{"Quality Gate 2\nUji @Transaction\n& Identitas Net Worth"}
    F3["Fase 3: Otomasi & Rilis (Sprint 8–10)\n• Integrasi Market Price API (Retrofit)\n• WorkManager (Sync Harga & Recurring Tx)\n• Notifikasi Anggaran & Ekspor CSV/PDF\n• Benchmark 10k Transaksi & Hardening"]

    F1 --> G1 --> F2 --> G2 --> F3
```

---

## 9. Dokumentasi Terkait

*  **[Product Requirements Document (PRD)](docs/PRD.md):** Spesifikasi lengkap produk, *user stories*, rumus matematika bisnis, skema tabel Room, spesifikasi layar UI/UX, dan *Non-Functional Requirements*.
*  **[Master Implementation Todos](docs/todos.md):** Daftar tugas (*checklist*) teknis bertahap dari Sprint 1 hingga Sprint 10.
