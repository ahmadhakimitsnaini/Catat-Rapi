# CatatRapi — Master Implementation Todos & Roadmap

Dokumen ini berisi daftar tugas (*todos*) terstruktur untuk seluruh tahap pengerjaan dan fitur aplikasi **CatatRapi** berdasarkan [PRD.md](file:///Users/apple/Documents/01.Sit%20Of%20Learning/semester%203/1%20-%20Pemograman%20Mobile/CatatRapi/docs/PRD.md) dan hasil analisa teknis.

**Legenda Prioritas:**
- `[P0]` Wajib untuk rilis (Core / Blocker)
- `[P1]` Penting (High Value)
- `[P2]` Pelengkap (Enhancement)

---

## Fase 1: MVP Core (Finance Tracker & Goal-Based Saving)
*Target: Sprint 1 – Sprint 4 | Fokus: Arsitektur dasar, manajemen dompet & transaksi, tabungan target, dan keamanan lokal.*

### Sprint 1: Project Setup, Clean Architecture & Core Database
#### 1.1 Konfigurasi Gradle & Dependensi
- [ ] `[P0]` Ubah `minSdk` dari `24` menjadi `26` (Android 8.0) di `app/build.gradle.kts` (**NFR-R4**).
- [ ] `[P0]` Tambahkan konfigurasi Kotlin & plugin KSP (`com.google.devtools.ksp`) di `gradle/libs.versions.toml` dan `build.gradle.kts`.
- [ ] `[P0]` Konfigurasi Jetpack Compose (Compose BOM, Material 3, UI Tooling, Activity Compose, Lifecycle Runtime Compose).
- [ ] `[P0]` Tambahkan Navigation Compose (`androidx.navigation:navigation-compose`).
- [ ] `[P0]` Tambahkan Dependency Injection (Hilt: `hilt-android`, `hilt-compiler`, `hilt-navigation-compose`).
- [ ] `[P0]` Tambahkan Room Database (`room-runtime`, `room-ktx`, `room-compiler`, `room-paging`, `room-testing`).
- [ ] `[P0]` Tambahkan Paging 3 (`paging-runtime-ktx`, `paging-compose`) untuk daftar transaksi (**NFR-P5**).
- [ ] `[P0]` Tambahkan pustaka grafik Vico Chart untuk Compose (`com.patrykandpatrick.vico`).
- [ ] `[P0]` Tambahkan dependensi keamanan & latar belakang: `androidx.biometric`, `androidx.datastore:datastore-preferences`, `net.zetetic:sqlcipher-android`, `androidx.sqlite:sqlite-ktx`, dan `androidx.work:work-runtime-ktx`.

#### 1.2 Struktur Paket Clean Architecture ($\text{UI} \rightarrow \text{Domain} \leftarrow \text{Data}$)
- [ ] `[P0]` Buat struktur paket modular di `com.example.catatrapi`:
  - `data/local/entity`, `data/local/dao`, `data/local/converter`, `data/local/db`
  - `data/remote/api`, `data/remote/dto`
  - `data/repository`, `data/worker`
  - `domain/model`, `domain/repository`, `domain/usecase`
  - `ui/theme`, `ui/navigation`, `ui/components`
  - `ui/dashboard`, `ui/cashflow`, `ui/goals`, `ui/portfolio`, `ui/analytics`, `ui/onboarding`, `ui/auth`
  - `di/` (Modul Hilt: `DatabaseModule`, `RepositoryModule`, `NetworkModule`, `DispatcherModule`)
- [ ] `[P0]` Setup tema Material 3 (`Color.kt`, `Type.kt`, `Theme.kt`) dengan palet finansial (hijau untung/pemasukan, merah rugi/pengeluaran, kuning peringatan) dan dukungan kontras & *font scaling* (**NFR-R5**).
- [ ] `[P0]` Buat utilitas pemformatan mata uang Rupiah (`Rp1.250.000`) dan tanggal Bahasa Indonesia (**NFR-R5**).

#### 1.3 Skema Database Awal & TypeConverter (Room)
- [ ] `[P0]` Buat `BigDecimalTypeConverter` (konversi `BigDecimal` $\leftrightarrow$ `String` murni/`PlainString` agar bebas galat *floating point*).
- [ ] `[P0]` Buat entitas `WalletEntity` (`Wallets`): `id`, `name`, `type` (`CASH`, `BANK`, `EWALLET`), `balance`, `is_archived`, `created_at`.
- [ ] `[P0]` Buat entitas `TransactionEntity` (`Transactions`): `id`, `wallet_id` (FK `RESTRICT`), `to_wallet_id` (FK nullable), `type` (`INCOME`, `EXPENSE`, `TRANSFER`, `INVESTMENT_BUY`, `INVESTMENT_SELL`), `amount`, `fee`, `category`, `note`, `occurred_at`, `created_at` beserta indeks pada `wallet_id` dan `occurred_at` (**NFR-P6**).
- [ ] `[P0]` Buat `WalletDao` dan `TransactionDao` beserta kontrak query reaktif (`Flow`).
- [ ] `[P0]` Inisialisasi `CatatRapiDatabase` dengan dukungan skema ekspor (`exportSchema = true`) untuk pengujian migrasi (**NFR-R2**).

---

### Sprint 2: Core Finance Tracker & Wallet Management (Modul 1)
#### 2.1 Domain Layer — Wallet & Cashflow
- [ ] `[P0]` Buat model domain `Wallet`, `WalletType`, `Transaction`, `TransactionType`, dan enum `TransactionCategory` (termasuk `ADMIN_FEE`).
- [ ] `[P0]` Buat kontrak `WalletRepository` dan `TransactionRepository` di layer Domain, beserta implementasinya di layer Data.
- [ ] `[P0]` **FT-01:** Implementasi `CreateWalletUseCase`, `UpdateWalletUseCase`, `GetActiveWalletsUseCase`, dan `GetTotalWalletBalanceUseCase` ($W = \sum \text{saldo dompet aktif}$).
- [ ] `[P0]` **FT-01 & GS-02:** Implementasi `ArchiveWalletUseCase` dengan validasi atomik: tolak pengarsipan bila $W - \text{balance}_{\text{dompet}} < G$ (total alokasi target aktif), dan otomatis jeda `RecurringTransactions` terkait.
- [ ] `[P0]` **FT-02:** Implementasi `RecordTransactionUseCase` (Pemasukan/Pengeluaran) dalam `@Transaction` Room: validasi $\text{nominal} > 0$, dompet aktif, insert `Transactions`, dan update `Wallets.balance`.
- [ ] `[P0]` **FT-03 (Rumus 2.2.1):** Implementasi `CalculateSpendableBalanceUseCase`:
  - Rumus: $\text{Spendable} = W - G - B_{\min}$.
  - Pertahankan nilai negatif ($\text{Spendable} < 0$) sebagai status peringatan *"alokasi melebihi saldo"*, jangan bulatkan ke nol.
- [ ] `[P0]` **FT-04:** Implementasi `TransferBetweenWalletsUseCase` dalam `@Transaction`:
  - Validasi dompet asal $\neq$ tujuan dan $\text{saldo asal} \ge \text{amount} + \text{fee}$.
  - Insert 1 baris `TRANSFER` (dengan `fee`), kurangi dompet asal sebesar `amount + fee`, tambah dompet tujuan sebesar `amount`.
  - Pastikan nominal pokok transfer tidak masuk perhitungan pemasukan/pengeluaran bulanan, sedangkan `fee > 0` dihitung sebagai pengeluaran biaya admin.
- [ ] `[P1]` **FT-06:** Implementasi `EditTransactionUseCase` dan `DeleteTransactionUseCase`:
  - Hitung ulang saldo dompet terkait secara atomik.
  - Blokir penghapusan/pengeditan langsung untuk tipe `INVESTMENT_BUY` dan `INVESTMENT_SELL` dari layar Cashflow (arahkan lewat modul Portofolio).

#### 2.2 UI Layer — Onboarding & Tab 2: Cashflow
- [ ] `[P0]` Buat alur **Onboarding**: form pembuatan dompet pertama dengan saldo awal $\rightarrow$ opsional buat target pertama $\rightarrow$ masuk ke Dashboard.
- [ ] `[P0]` Buat kerangka navigasi utama (`MainScaffold`) dengan **Bottom Navigation 4 Tab**: *Dashboard*, *Cashflow*, *Goals & Wealth*, *Analytics*, serta tombol aksi cepat (FAB) **"+ Transaksi"** di Dashboard & Cashflow.
- [ ] `[P0]` Buat komponen **Carousel Dompet** di Tab Cashflow: tampilkan nama, tipe, saldo per dompet, Total Saldo di atas, serta aksi tambah/arsipkan dompet (**FT-01**).
- [ ] `[P0]` Buat **Daftar Transaksi** dengan Paging 3 / LazyColumn dikelompokkan per tanggal, fitur pencarian, filter (dompet, kategori, tipe), lencana khusus konversi aset, dan *swipe* untuk edit/hapus (**FT-02, FT-06, NFR-P5**).
- [ ] `[P0]` Buat **Form Input Transaksi & Transfer** (maksimal 3 ketuk untuk input cepat): validasi *inline* dan tampilkan pratinjau *Spendable Balance* setelah transaksi sebelum tombol simpan ditekan (**FT-02, FT-03, FT-04**).

---

### Sprint 3: Goal-Based Saving Engine (Modul 2 — Bagian 1)
#### 3.1 Data & Domain Layer — Saving Goals
- [ ] `[P0]` Buat entitas `SavingGoalEntity` (`SavingGoals`): `id`, `name`, `target_amount`, `deadline`, `priority`, `status` (`ON_TRACK`, `DELAYED`, `ACHIEVED`), `is_archived`, `created_at`.
- [ ] `[P0]` Buat entitas `GoalContributionEntity` (`GoalContributions`): `id`, `goal_id` (FK `CASCADE`), `wallet_id` (FK `SET_NULL`), `amount` (positif/negatif), `type` (`DEPOSIT`, `WITHDRAW`, `AUTO_SWEEP`), `created_at` dengan indeks pada `goal_id` (**NFR-P6**).
- [ ] `[P0]` Buat `SavingGoalDao`: query saldo terkumpul target $S_j = \text{SUM(amount)}$ dari `GoalContributions` dan total alokasi seluruh target aktif $G = \sum S_j$.
- [ ] `[P0]` **GS-01:** Implementasi `CreateSavingGoalUseCase`: validasi $T > 0$ dan tenggat di masa depan.
- [ ] `[P0]` **GS-02:** Implementasi `ContributeToGoalUseCase` dalam `@Transaction`:
  - Validasi di dalam transaksi: $\text{nominal setoran} \le \text{Spendable}$.
  - Insert `GoalContributions` (`DEPOSIT`), kurangi *Spendable Balance* (tanpa mengubah Total Saldo $W$), dan hitung ulang status target.
- [ ] `[P1]` **GS-06:** Implementasi `WithdrawFromGoalUseCase` dalam `@Transaction`:
  - Validasi $\text{nominal tarik} \le S_j$.
  - Insert `GoalContributions` negatif (`WITHDRAW`), kembalikan dana ke *Spendable*, dan hitung ulang status target.
- [ ] `[P0]` **GS-03 (Rumus 2.2.2):** Implementasi `CalculateDynamicSavingRateUseCase`:
  - $D_{\text{sisa}} = \max(1, \ \text{tenggat} - \text{hari ini})$.
  - $R_{\text{harian}} = \max(0, T - S) / D_{\text{sisa}}$ dan $R_{\text{mingguan}} = \max(0, T - S) / \lceil D_{\text{sisa}} / 7 \rceil$.
  - Tangani pembagian nol & ubah status ke `DELAYED` bila tenggat terlewat dan $S < T$.
- [ ] `[P0]` **Rumus 2.2.3 (Saving Velocity & ETA):** Implementasi `EvaluateGoalStatusUseCase`:
  - Hitung *Saving Velocity* 30 hari terakhir: $v = \frac{\sum c_k}{\min(30, d_{\text{berjalan}})}$.
  - Jika $S \ge T \rightarrow \text{ACHIEVED}$.
  - Jika $v \le 0$ (belum menabung atau penarikan bersih negatif) $\rightarrow$ ETA tidak terdefinisi dan status `DELAYED`.
  - Jika $S < T$, $v > 0$, dan $\text{ETA} \le \text{tenggat} \rightarrow \text{ON\_TRACK}$ (selain itu `DELAYED`).

#### 3.2 UI Layer — Tab 3: Segmen Goals
- [ ] `[P0]` Buat layar **Daftar Target (Segmen Goals)**: tampilkan nama, dana terkumpul vs target ($S / T$), *progress bar*, sisa hari, dan badge status (`ON_TRACK`, `DELAYED`, `ACHIEVED`).
- [ ] `[P0]` Buat layar **Detail Target**: tampilkan *Dynamic Saving Rate* harian & mingguan, *Saving Velocity*, perbandingan ETA vs tenggat, riwayat kontribusi, serta tombol **Setor**, **Tarik**, dan **Auto-Sweep** (**GS-01, GS-02, GS-03, GS-06**).
- [ ] `[P0]` Buat fitur **Slider Simulasi Dampak** di Detail Target: *"Jika belanja bertambah Rp X"* $\rightarrow$ tampilkan perubahan ETA secara langsung.
- [ ] `[P2]` **GS-07:** Tambahkan fitur pengaturan urutan prioritas target (*drag-and-drop* / ubah `priority`) untuk menentukan urutan penerima *Auto-Sweep*.

---

### Sprint 4: Impact Warning, Auto-Sweep, Budgets, Security & MVP Dashboard
#### 4.1 Impact Warning, Auto-Sweep & Category Budgets
- [ ] `[P0]` **GS-04 (Rumus 2.2.3):** Implementasi `CalculateImpactWarningUseCase`:
  - Hitung rata-rata pengeluaran harian 30 hari terakhir ($\mu$, tidak termasuk transfer & konversi aset) atau gunakan batas harian manual; nonaktifkan bila histori $< 7\text{ hari}$ tanpa batas manual atau $v \le 0$.
  - Hitung $\Delta = \max(0, x - \mu)$ dan $\text{Hari mundur} = \lceil \frac{T - S + \Delta}{v} \rceil - \lceil \frac{T - S}{v} \rceil$.
  - Hasilkan peringatan konkret bila $\Delta > 0$ dan $\text{Hari mundur} \ge 1$; picu notifikasi prioritas tinggi bila status jatuh dari `ON_TRACK` ke `DELAYED`.
- [ ] `[P1]` **GS-05 (Rumus 2.2.3):** Implementasi `AutoSweepSurplusUseCase`:
  - Hitung $\text{Sweep} = \max(0, \text{Spendable} - B_{\text{sweep}})$ dengan default $B_{\text{sweep}} = 3\mu$.
  - Alokasikan surplus secara berurutan ke target aktif berdasarkan `priority` tertinggi hingga penuh ($T_j - S_j$).
  - Sediakan dialog konfirmasi bernominal dan mekanisme *Snackbar Undo* selama 10 detik.
- [ ] `[P1]` **FT-05:** Buat entitas `CategoryBudgetEntity` (`CategoryBudgets` dengan indeks unik `(category, year_month)`), `CategoryBudgetDao`, dan `EvaluateCategoryBudgetUseCase` untuk memicu peringatan pada ambang $80\%$ dan $100\%$.

#### 4.2 Keamanan Data Lokal (Security NFR)
- [ ] `[P0]` **NFR-S1:** Implementasi `BiometricAuthManager` dengan `BiometricPrompt` (sidik jari/wajah) dan *fallback* `DEVICE_CREDENTIAL` (PIN/pola), serta penguncian otomatis saat aplikasi kembali dari *background* melewati ambang waktu.
- [ ] `[P0]` **NFR-S2:** Integrasikan SQLCipher (`SupportOpenHelperFactory`) pada `CatatRapiDatabase` dengan kunci yang dienkripsi melalui Android Keystore.
- [ ] `[P0]` **NFR-S3:** Buat `EncryptedPreferencesDataStore` berbasis Android Keystore untuk menyimpan pengaturan buffer ($B_{\min}$, $B_{\text{sweep}}$) dan konfigurasi keamanan.
- [ ] `[P0]` **NFR-S4 & NFR-S5:** Aktifkan `WindowManager.LayoutParams.FLAG_SECURE` pada `MainActivity` dan set `android:allowBackup="false"` di `AndroidManifest.xml`.

#### 4.3 Dashboard Dasar & Quality Gate 1
- [ ] `[P0]` Buat **Tab 1: Dashboard (MVP)**: Kartu Saldo (Total Saldo vs *Spendable Balance* berdampingan), Ringkasan 3 Target Prioritas Teratas, Banner *Impact Warning*, Tombol Aksi Cepat (*Auto-Sweep*, Transfer), dan 5 Transaksi Terbaru.
- [ ] `[P0]` **Quality Gate 1:** Tulis unit test lengkap ($>90\%$ coverage) untuk seluruh rumus `2.2.1`, `2.2.2`, dan `2.2.3` (termasuk kasus tepi: pembagian nol, tenggat lewat, $v \le 0$, $\text{Spendable} < 0$) serta verifikasi **NFR-P1 s.d. NFR-P3**.

---

## Fase 2: Investment Manual Input & Net Worth Analytics
*Target: Sprint 5 – Sprint 7 | Fokus: Portofolio multi-aset, konversi aset atomik, DCA, dan kalkulasi Net Worth utuh.*

### Sprint 5: Struktur Portofolio & Harga Manual (Modul 3 — Bagian 1)
- [ ] `[P0]` Buat entitas `PortfolioEntity` (`Portfolios`): `id`, `asset_type` (`STOCK`, `MUTUAL_FUND`, `GOLD`, `CRYPTO`), `symbol` (unik bersama `asset_type`), `name`, `unit` (`lot`, `unit`, `gram`, `koin`), `quantity` (`BigDecimal`), `avg_buy_price` (`BigDecimal`), `last_price`, `last_price_at`, `price_source` (`MANUAL` / `API`).
- [ ] `[P0]` Buat entitas `InvestmentLogEntity` (`InvestmentLogs`): `id`, `portfolio_id` (FK `RESTRICT`), `wallet_id` (FK `RESTRICT`), `transaction_id` (FK unik $\rightarrow$ `Transactions.id`), `type` (`BUY` / `SELL`), `quantity`, `price`, `fee`, `realized_pl`, `created_at` dengan indeks pada `portfolio_id` (**NFR-P6**).
- [ ] `[P0]` Buat migrasi database Room (`MIGRATION_1_2`) beserta pengujian `MigrationTestHelper` (**NFR-R2**).
- [ ] `[P0]` **IV-01:** Implementasi `AddPortfolioAssetUseCase` dan daftar posisi portofolio dengan dukungan kuantitas desimal (`BigDecimal`).
- [ ] `[P1]` **IV-06:** Implementasi `UpdateManualAssetPriceUseCase` untuk memperbarui `last_price`, `last_price_at`, dan menandai `price_source = MANUAL`.

---

### Sprint 6: Asset Conversion, DCA, Jual Aset & Floating P/L (Modul 3 — Bagian 2)
- [ ] `[P0]` **IV-02 & IV-03 (Rumus 2.2.4 — Beli Aset & DCA):** Implementasi `BuyInvestmentAssetUseCase` & `@Transaction` di DAO/Repository:
  - Hitung $\text{Kas keluar} = Q_{\text{beli}} \cdot P_{\text{beli}} + F$.
  - Validasi di dalam transaksi: $\text{Kas keluar} \le \text{Spendable}$.
  - Hitung *Average Buy Price* baru: $\bar{P}_{\text{baru}} = \frac{Q_{\text{lama}} \cdot \bar{P}_{\text{lama}} + Q_{\text{beli}} \cdot P_{\text{beli}} + F}{Q_{\text{lama}} + Q_{\text{beli}}}$ menggunakan `RoundingMode.HALF_UP` (skala minimal 8 digit).
  - Eksekusi atomik: Insert `Transactions` (`INVESTMENT_BUY`) $\rightarrow$ kurangi `Wallets.balance` $\rightarrow$ upsert `Portfolios` $\rightarrow$ insert `InvestmentLogs`.
- [ ] `[P0]` **IV-04 (Rumus 2.2.4 — Jual Aset):** Implementasi `SellInvestmentAssetUseCase` dalam `@Transaction`:
  - Validasi $Q_{\text{jual}} \le Q_{\text{dimiliki}}$.
  - Hitung $\text{Realized P/L} = Q_{\text{jual}} \cdot (P_{\text{jual}} - \bar{P}) - F_{\text{jual}}$.
  - Pertahankan $\bar{P}$ (`avg_buy_price`) bila $Q_{\text{baru}} > 0$, atau reset `avg_buy_price = BigDecimal.ZERO` bila seluruh posisi terjual habis ($Q_{\text{baru}} == 0$).
  - Eksekusi atomik: Insert `Transactions` (`INVESTMENT_SELL`) $\rightarrow$ tambah `Wallets.balance` sebesar hasil bersih $\rightarrow$ kurangi `Portfolios.quantity` $\rightarrow$ insert `InvestmentLogs`.
- [ ] `[P0]` **IV-05 (Rumus 2.2.4 — Floating P/L & ROI):** Implementasi `CalculatePortfolioValuationUseCase`:
  - $\text{Floating P/L} = Q \cdot (P_{\text{mkt}} - \bar{P})$ dan $\text{ROI} = \frac{P_{\text{mkt}} - \bar{P}}{\bar{P}} \times 100\%$.
  - Terapkan urutan *fallback* harga (**Bagian 2.5**): harga API terbaru $\rightarrow$ harga API tersimpan $\rightarrow$ harga manual terakhir $\rightarrow$ `avg_buy_price` (ditandai `ESTIMATE`).
- [ ] `[P0]` Buat UI **Tab 3: Segmen Portofolio**: Ringkasan nilai pasar & total *Floating P/L*, Daftar Aset (indikator warna hijau/merah, lencana `MANUAL`/`API` & waktu harga), Form Beli/Jual dengan pratinjau $\bar{P}_{\text{baru}}$ & *Spendable*, serta layar Detail Aset (`InvestmentLogs`).

---

### Sprint 7: Unified Net Worth, Asset Distribution & Analytics (Modul 4)
- [ ] `[P0]` Buat entitas `NetWorthSnapshotEntity` (`NetWorthSnapshots`: `date` PK epoch day, `cash`, `investment`, `net_worth`) dan `NetWorthSnapshotDao`.
- [ ] `[P0]` **DB-01 (Rumus 2.2.4 — Total Net Worth):** Implementasi `CalculateNetWorthUseCase` di `Dispatchers.Default` (**NFR-P4**):
  - Rumus 1: $\text{Net Worth} = W + \sum (Q_a \cdot P_{\text{mkt},a})$.
  - Rumus 2 (Uji Konsistensi): $\text{Net Worth} = (\text{Spendable} + B_{\min}) + G + \sum (Q_a \cdot P_{\text{mkt},a})$.
  - Pastikan dana target ($G$) tidak dihitung dua kali dan waktu muat $< 1\text{ detik}$.
- [ ] `[P0]` **DB-02:** Implementasi `GetAssetDistributionUseCase` dan komponen **Grafik Donat Distribusi Aset** di Dashboard (Kas Bebas, Dana Target, Saham, Reksadana, Emas, Kripto; total $= 100\%$).
- [ ] `[P0]` Lengkapi **Kartu Net Worth** di Dashboard: tampilkan perubahan vs bulan lalu (nominal & $\%$), serta label peringatan *"harga tertunda"* bila usia `last_price_at` $> 24\text{ jam}$.
- [ ] `[P1]` **DB-03 & DB-04:** Buat UI **Tab 4: Analytics**:
  - Selektor bulan & perbandingan bulan sebelumnya.
  - Grafik batang Pemasukan vs Pengeluaran mingguan/harian (Vico Chart).
  - Grafik donat & peringkat Pengeluaran per Kategori.
  - Kalkulasi & tren 6 bulan $\text{Savings Rate} = (\text{pemasukan} - \text{pengeluaran}) / \text{pemasukan}$.
  - Grafik garis **Tren Net Worth** dari `NetWorthSnapshots` dengan filter rentang `1B`, `3B`, `1T`, `Semua`.
  - Ringkasan kinerja investasi (*Floating P/L* & ROI per kelas aset).
- [ ] `[P0]` **Quality Gate 2:** Tulis *integration test* `@Transaction` Room (termasuk simulasi kegagalan di tengah operasi beli/jual aset untuk menjamin *rollback* sempurna **NFR-R1**) dan uji identitas rumus *Net Worth* (`2.2.4`).

---

## Fase 3: Automated Market API, Background Worker & Release
*Target: Sprint 8 – Sprint 10 | Fokus: Integrasi API harga pasar, otomasi WorkManager, ekspor laporan, dan persiapan rilis.*

### Sprint 8: Market Price API Integration & Offline Caching
- [ ] `[P1]` Tetapkan penyedia API harga pasar untuk Saham Indonesia, Reksadana, Emas, dan Kripto.
- [ ] `[P1]` Konfigurasi `Retrofit` + `OkHttp` dengan HTTPS wajib, *cache* HTTP, dan tanpa pengiriman data keuangan pribadi — hanya mengirimkan simbol aset (**NFR-S6, NFR-S7**).
- [ ] `[P1]` **IV-07:** Implementasi sinkronisasi harga di `PortfolioRepository`: ambil harga dari API $\rightarrow$ simpan ke kolom `last_price`, `last_price_at`, dan `price_source = API` di Room $\rightarrow$ UI membaca ulang secara reaktif dari Room.
- [ ] `[P1]` Pastikan kegagalan jaringan tidak menghapus/menolkan harga terakhir di Room dan tetap mengikuti hierarki *fallback* (**Bagian 2.5**).

---

### Sprint 9: WorkManager Automation, Recurring Transactions & Notifications
- [ ] `[P2]` **FT-07:** Buat entitas `RecurringTransactionEntity` (`RecurringTransactions` dengan indeks pada `next_run_at`), `RecurringTransactionDao`, serta UI pengelolaan jadwal transaksi berulang (`DAILY`, `WEEKLY`, `MONTHLY`, jeda/aktifkan).
- [ ] `[P1]` Buat `MarketPriceSyncWorker` (`CoroutineWorker`): sinkronisasi harga pasar periodik dengan constraint `NetworkType.CONNECTED` dan *exponential backoff* (**IV-07**).
- [ ] `[P1]` Buat `DailyFinancialEvaluationWorker` (`CoroutineWorker`) untuk menjalankan tugas harian di latar belakang:
  - Evaluasi batas anggaran kategori ($80\%$ dan $100\%$) dan kirim notifikasi lokal (**FT-05**).
  - Eksekusi jadwal `RecurringTransactions` yang sudah jatuh tempo (`next_run_at <= now`) dalam `@Transaction` (**FT-07**).
  - Rekam *snapshot* harian ke tabel `NetWorthSnapshots` (**DB-04**).
  - Evaluasi ulang status seluruh `SavingGoals` (`ON_TRACK` / `DELAYED`) saat pergantian hari (**GS-03**).
- [ ] `[P1]` Buat `NotificationHelper` dengan saluran notifikasi (*Notification Channels*) terpisah untuk Peringatan Anggaran, *Impact Warning* Target, dan Transaksi Berulang.

---

### Sprint 10: Export Reports, Performance Benchmarking & Hardening
- [ ] `[P2]` **DB-05:** Implementasi fitur ekspor laporan bulanan ke format **CSV** dan **PDF** menggunakan *Storage Access Framework* (SAF).
- [ ] `[P2]` **NFR-S5:** Tambahkan fitur ekspor/impor cadangan database terenkripsi atas tindakan eksplisit pengguna.
- [ ] `[P0]` **Uji Performa (NFR-P1 s.d. NFR-P6):**
  - Uji *dataset* $10.000$ transaksi untuk memastikan waktu muat layar $\le 500\text{ ms}$ (**NFR-P2**), penyimpanan transaksi $< 200\text{ ms}$ (**NFR-P3**), *cold start* $< 2\text{ detik}$ (**NFR-P1**), dan *scrolling* $60\text{ fps}$ tanpa *jank* (**NFR-P5**).
- [ ] `[P0]` **Keandalan & Privasi Akhir (NFR-R1 s.d. NFR-R6):**
  - Verifikasi seluruh *MigrationTest* dari versi skema awal hingga akhir (**NFR-R2**).
  - Pastikan cakupan unit test `UseCase` keuangan $> 90\%$ (**NFR-R3**).
  - Konfigurasi ProGuard / R8 untuk *release build* dan pastikan tidak ada pelaporan *crash* yang memuat nominal/isi transaksi (**NFR-R6, NFR-S7**).
