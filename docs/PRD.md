# CatatRapi — System Overview & PRD

**Tanggal:** 19 Oktober 2026  
**Penulis:** @Deta Aprilka

---

## Bagian 1: System Overview

### 1.1 Executive Summary & Value Proposition

**CatatRapi** adalah aplikasi *Personal Wealth Manager* Android yang menghubungkan arus kas harian, tabungan target, dan investasi dalam satu model data, sehingga setiap rupiah memiliki status yang jelas: bebas pakai, dialokasikan, atau diinvestasikan.

#### Masalah yang Diselesaikan
* **Saldo menyesatkan.** Aplikasi pencatat biasa hanya menampilkan total saldo rekening. Dana yang sebenarnya sudah "dijanjikan" untuk tujuan tertentu (dana darurat, liburan, cicilan) tetap terlihat bisa dibelanjakan, sehingga pengguna *overspending* tanpa sadar.
* **Silo fitur.** Pencatat pengeluaran, aplikasi tabungan, dan aplikasi investasi berdiri terpisah. Pengguna harus merekonsiliasi angka secara manual, dan tidak ada yang menunjukkan dampak belanja hari ini terhadap tanggal tercapainya target.
* **Kekayaan bersih tidak terlihat.** Investasi dihitung terpisah dari kas, sehingga pengguna tidak punya satu angka *Net Worth* yang akurat.

#### Nilai Jual Utama

| Pilar | Interkoneksi Otomatis | Dampak bagi Pengguna |
| :--- | :--- | :--- |
| **Finance Tracker** | Setiap transaksi memperbarui *Spendable Balance* | Tahu persis berapa yang aman dibelanjakan |
| **Goal-Based Saving** | Pengeluaran yang membengkak memicu *Impact Warning* dan menghitung ulang setoran | Melihat langsung berapa hari target mundur akibat belanja |
| **Investment Management** | Pembelian aset memotong kas dan menambah posisi portofolio dalam satu transaksi atomik | *Net Worth* akurat tanpa input ganda |

**Proposisi inti:** perilaku belanja $\rightarrow$ kecepatan mencapai target $\rightarrow$ pertumbuhan investasi dihitung sebagai satu sistem, bukan tiga catatan terpisah.

---

### 1.2 Target Audience & User Personas

**Target utama:** pengguna Android usia 20–35 tahun di Indonesia dengan penghasilan rutin bulanan, yang sudah punya kebiasaan mencatat sederhana namun belum terhubung ke tujuan keuangan.

| Atribut | Persona A: Raka (Pekerja Awal) | Persona B: Dinda (First-Time Investor) |
| :--- | :--- | :--- |
| **Profil** | 24 tahun, staf administrasi, gaji Rp6.500.000/bulan | 28 tahun, analis junior, gaji Rp9.000.000/bulan |
| **Tujuan** | Menabung Rp15.000.000 untuk DP motor dalam 10 bulan, dana darurat 3 bulan | Merapikan portofolio (reksadana, emas, saham) dan memantau kekayaan bersih |
| **Pain point** | Saldo rekening terlihat besar sehingga jajan dan langganan tak terkendali; tabungan selalu "sisa" | Beli aset di banyak aplikasi, tidak tahu harga beli rata-rata dan untung/rugi sebenarnya; kas dan investasi tercatat terpisah |
| **Perilaku** | Mencatat 3–5 transaksi/hari, sering lupa menyisihkan tabungan | Rutin DCA bulanan, mengecek harga mingguan |
| **Fitur kunci** | *Spendable Balance*, *Dynamic Saving Rate*, *Impact Warning*, *Auto-Sweep Surplus* | *Asset Conversion*, *Average Cost*, *Floating P/L*, *Net Worth* |
| **Kriteria sukses** | Target tercapai tepat waktu (status `ON_TRACK`) | Satu layar menunjukkan *Net Worth* dan distribusi aset yang akurat |

---

### 1.3 High-Level System Architecture

CatatRapi memakai *Clean Architecture* dengan MVVM: dependensi hanya mengarah ke dalam ($\text{UI} \rightarrow \text{Domain} \leftarrow \text{Data}$), dan seluruh data yang ditampilkan berasal dari Room, bukan langsung dari jaringan.

#### Diagram Arsitektur (Room sebagai Single Source of Truth)

```text
+-----------------------------------------------------------------------------------+
| UI Layer                                                                          |
|  +-----------------------+     +-----------------------+   +-------------------+  |
|  |    Compose Screens    | --> |       ViewModel       |   | Navigation dan DI |  |
|  | Material 3, Vico chart|     |  StateFlow, UI state  |   | NavHost, Hilt/Koin|  |
|  +-----------------------+     +-----------+-----------+   +-------------------+  |
+--------------------------------------------|--------------------------------------+
                                             |
                                             v
+-----------------------------------------------------------------------------------+
| Domain Layer                               |                                      |
|  +-----------------------+     +-----------+-----------+   +-------------------+  |
|  |     Domain Model      |     |        UseCase        |-->|RepositoryInterface|  |
|  |  Wallet, Goal, Asset  |     |Aturan bisnis dan rumus|   |Kontrak akses data |  |
|  +-----------------------+     +-----------------------+   +---------+---------+  |
+----------------------------------------------------------------------|------------+
                                                                       | implements
                                                                       v
+-----------------------------------------------------------------------------------+
| Data Layer                                                           |            |
|  +-----------------------+     +-----------------------+   +---------+---------+  |
|  |   Retrofit + OkHttp   |     |     Room Database     |<--|  Repository Impl  |  |
|  |Harga pasar, cache off.|     | Single Source of Truth|   |Gabung Room & Remot|  |
|  +-----------+-----------+     +-----------------------+   +-------------------+  |
|              ^                                                       |            |
|              +------------- respons API disimpan ke Room ------------+            |
+-----------------------------------------------------------------------------------+
```

`ViewModel` memanggil `UseCase`, `UseCase` memanggil `Repository Interface`, dan implementasinya menggabungkan Room dengan Retrofit.

| Layer | Tanggung Jawab | Aturan |
| :--- | :--- | :--- |
| **UI (Compose)** | Merender *state*, menangkap *event* pengguna | *Stateless*; mengonsumsi `StateFlow` lewat `collectAsStateWithLifecycle()`; tanpa logika bisnis |
| **ViewModel** | Menyimpan *UI state*, memanggil `UseCase` di `viewModelScope` | Tidak mengenal Room atau Retrofit; *state* dimodelkan sebagai `sealed interface` (`Loading`, `Success`, `Error`) |
| **Domain (UseCase)** | Seluruh rumus dan aturan bisnis (Bagian 2.2); orkestrasi lintas modul | Kotlin murni, tanpa dependensi Android; satu `UseCase` = satu aksi bisnis |
| **Data (Repository)** | Menggabungkan sumber data lokal dan *remote* | Room = *Single Source of Truth*; UI tidak pernah membaca langsung dari Retrofit |

#### Alur Data
1. **Baca:** Compose mengamati `StateFlow` dari `ViewModel`, yang diturunkan dari `Flow` DAO Room. Setiap perubahan tabel otomatis memperbarui UI.
2. **Tulis:** Event UI $\rightarrow$ `ViewModel` $\rightarrow$ `UseCase` $\rightarrow$ `Repository` $\rightarrow$ `DAO`. Operasi lintas tabel (mis. beli aset) dibungkus satu transaksi Room (Bagian 2.3).
3. **Sinkronisasi harga pasar:** `Repository` memanggil Retrofit, menyimpan respons ke tabel *cache* Room, lalu UI membaca ulang dari Room. Bila jaringan gagal, UI tetap menampilkan harga terakhir beserta penanda waktu.
4. **Latar belakang:** `WorkManager` menjalankan sinkronisasi harga dan evaluasi batas anggaran lewat `UseCase` yang sama dengan jalur *foreground*.

---

## Bagian 2: Product Requirements Document (PRD)

### 2.1 Functional Requirements & User Stories

Empat modul berikut saling terhubung: Modul 1 menjadi sumber saldo, Modul 2 dan 3 mengalokasikan saldo tersebut, dan Modul 4 mengagregasi ketiganya. Prioritas: $\text{P0} =$ wajib untuk rilis, $\text{P1} =$ penting, $\text{P2} =$ pelengkap.

#### Modul 1: Core Finance Tracker & Wallet Management

| ID | User Story | Acceptance Criteria | Prioritas |
| :--- | :--- | :--- | :---: |
| **FT-01** | Sebagai pengguna, saya ingin membuat beberapa dompet (kas, bank, e-wallet) agar saldo setiap rekening tercatat terpisah. | Dompet memiliki nama, tipe, dan saldo awal; total saldo $=$ jumlah semua dompet aktif; dompet dapat diarsipkan tanpa menghapus riwayat | P0 |
| **FT-02** | Sebagai pengguna, saya ingin mencatat pemasukan dan pengeluaran dengan kategori agar arus kas harian terdokumentasi. | Input selesai maksimal 3 ketuk; nominal $> 0$; transaksi memperbarui saldo dompet dan *Spendable Balance* dalam satu transaksi database | P0 |
| **FT-03** | Sebagai pengguna, saya ingin melihat Total Saldo dan *Spendable Balance* berdampingan agar tahu dana yang benar-benar boleh dipakai. | $\text{Spendable} = \text{Total Saldo} - \text{Dana Teralokasi}$ (lihat 2.2.1); nilai berubah *real-time* saat transaksi, setoran target, atau pembelian aset | P0 |
| **FT-04** | Sebagai pengguna, saya ingin mentransfer dana antar dompet agar perpindahan internal tidak dihitung sebagai pengeluaran. | Transfer menghasilkan dua mutasi atomik; tidak masuk laporan pemasukan/pengeluaran; biaya admin opsional tercatat sebagai pengeluaran | P0 |
| **FT-05** | Sebagai pengguna, saya ingin mengatur batas anggaran per kategori agar belanja terkendali. | Notifikasi pada $80\%$ dan $100\%$ batas; evaluasi berjalan lewat `WorkManager` harian dan saat transaksi baru | P1 |
| **FT-06** | Sebagai pengguna, saya ingin mengedit atau menghapus transaksi agar kesalahan input dapat dikoreksi. | Saldo dan kontribusi terkait dihitung ulang; transaksi bertipe konversi aset atau setoran target tidak dapat dihapus langsung (harus lewat modulnya) | P1 |
| **FT-07** | Sebagai pengguna, saya ingin transaksi berulang (gaji, langganan) agar tidak input manual tiap bulan. | Jadwal harian/mingguan/bulanan; dibuat otomatis oleh `WorkManager`; dapat dijeda | P2 |

#### Modul 2: Goal-Based Saving Engine

| ID | User Story | Acceptance Criteria | Prioritas |
| :--- | :--- | :--- | :---: |
| **GS-01** | Sebagai pengguna, saya ingin membuat target tabungan (nama, nominal, tenggat) agar punya tujuan terukur. | Validasi nominal $> 0$ dan tenggat di masa depan; setiap target memiliki status `ON_TRACK`, `DELAYED`, atau `ACHIEVED` | P0 |
| **GS-02** | Sebagai pengguna, saya ingin menyetor dana ke target dari dompet tertentu agar saldo target terpisah dari saldo bebas pakai. | Setoran mengurangi *Spendable Balance*, menambah `GoalContributions`, dan tidak mengubah Total Saldo; operasi atomik | P0 |
| **GS-03** | Sebagai pengguna, saya ingin melihat kebutuhan setoran harian/mingguan yang menyesuaikan sisa hari agar tahu seberapa cepat harus menabung. | *Dynamic Saving Rate* (2.2.2) dihitung ulang setiap perubahan saldo target atau tanggal; pembagian dengan nol ditangani saat tenggat terlewat | P0 |
| **GS-04** | Sebagai pengguna, saya ingin melihat *Impact Warning* saat pengeluaran harian membengkak agar sadar dampaknya ke target. | Peringatan muncul bila pengeluaran hari berjalan melebihi rata-rata harian yang diizinkan dan ETA mundur; menampilkan selisih hari keterlambatan konkret | P0 |
| **GS-05** | Sebagai pengguna, saya ingin *One-Tap Auto-Sweep Surplus* agar sisa dana akhir periode otomatis masuk ke target. | Satu ketukan memindahkan surplus ($\text{Spendable} - \text{buffer minimum}$) ke target prioritas; ada layar konfirmasi dengan nominal; dapat di-*undo* 10 detik | P1 |
| **GS-06** | Sebagai pengguna, saya ingin menarik atau membatalkan alokasi target agar dana kembali bisa dipakai. | Penarikan tercatat sebagai kontribusi negatif; saldo kembali ke *Spendable*; target tetap tersimpan dengan riwayat | P1 |
| **GS-07** | Sebagai pengguna, saya ingin memprioritaskan beberapa target agar surplus dibagi sesuai urutan. | Urutan prioritas dapat diatur *drag-and-drop*; *Auto-Sweep* mengisi target berprioritas tertinggi hingga penuh | P2 |

#### Modul 3: Investment Portfolio Tracker

| ID | User Story | Acceptance Criteria | Prioritas |
| :--- | :--- | :--- | :---: |
| **IV-01** | Sebagai investor pemula, saya ingin menambahkan aset (saham, reksadana, emas, kripto) ke portofolio agar semua posisi terpusat. | Aset memiliki tipe, kode/nama, dan satuan (lot/unit/gram/koin); kuantitas mendukung desimal | P0 |
| **IV-02** | Sebagai pengguna, saya ingin membeli aset dengan *Asset Conversion* agar kas terpotong otomatis. | Satu transaksi atomik: dompet sumber berkurang sebesar $\text{nilai beli} + \text{biaya}$, posisi bertambah, `InvestmentLogs` bertambah; ditolak bila *Spendable* tidak cukup | P0 |
| **IV-03** | Sebagai pengguna, saya ingin harga beli rata-rata dihitung otomatis pada pembelian berulang (DCA). | *Average Buy Price* mengikuti rumus 2.2.4; presisi desimal memakai `BigDecimal` atau `Long` satuan terkecil | P0 |
| **IV-04** | Sebagai pengguna, saya ingin menjual aset dan hasilnya kembali ke dompet agar kas dan portofolio konsisten. | Penjualan menambah kas sebesar hasil bersih; *Average Buy Price* tidak berubah; *realized P/L* tercatat; $\text{kuantitas jual} \le \text{kuantitas dimiliki}$ | P0 |
| **IV-05** | Sebagai pengguna, saya ingin melihat *Floating Profit/Loss* dan ROI per aset agar kinerja terlihat jelas. | $\text{Nilai} = (\text{harga pasar} - \text{average buy price}) \times \text{kuantitas}$; warna hijau/merah; tampil beserta waktu pembaruan harga | P0 |
| **IV-06** | Sebagai pengguna, saya ingin memperbarui harga aset secara manual agar valuasi tetap jalan tanpa API. | Harga manual disimpan dengan penanda `MANUAL`; digantikan harga API bila tersedia (Fase 3) | P1 |
| **IV-07** | Sebagai pengguna, saya ingin harga pasar diperbarui otomatis agar valuasi akurat tanpa input manual. | Sinkronisasi periodik lewat `WorkManager`; kegagalan jaringan tidak menghapus harga terakhir | P1 |

#### Modul 4: Unified Dashboard & Net Worth Analytics

| ID | User Story | Acceptance Criteria | Prioritas |
| :--- | :--- | :--- | :---: |
| **DB-01** | Sebagai pengguna, saya ingin melihat *Net Worth* di Beranda agar tahu kekayaan bersih secara utuh. | $\text{Net Worth} = \text{Total Saldo dompet (sudah termasuk dana target)} + \text{nilai pasar investasi}$ (2.2.4); dana target tidak dihitung dua kali; muncul $< 1\text{ detik}$ dari data lokal | P0 |
| **DB-02** | Sebagai pengguna, saya ingin melihat distribusi aset (kas, target, saham, reksadana, emas, kripto) agar paham komposisi. | Grafik donat dengan persentase; total persentase $= 100\%$ | P0 |
| **DB-03** | Sebagai pengguna, saya ingin ringkasan bulanan pemasukan, pengeluaran, dan tingkat tabungan agar mengevaluasi kebiasaan. | Filter bulan; pengeluaran per kategori; $\text{Savings Rate} = (\text{pemasukan} - \text{pengeluaran}) / \text{pemasukan}$ | P1 |
| **DB-04** | Sebagai pengguna, saya ingin melihat tren *Net Worth* dari waktu ke waktu. | *Snapshot Net Worth* harian disimpan; grafik garis dengan rentang 1B/3B/1T/Semua | P1 |
| **DB-05** | Sebagai pengguna, saya ingin mengekspor laporan bulanan agar dapat diarsipkan. | Ekspor ke CSV atau PDF melalui *Storage Access Framework* | P2 |

---

### 2.2 Core Business Logic & Mathematical Formulas

Semua rumus berikut diimplementasikan sebagai `UseCase` murni Kotlin (tanpa dependensi Android) agar mudah diuji unit. Nominal uang disimpan sebagai `Long` dalam satuan rupiah, dan kuantitas aset sebagai `BigDecimal`, untuk menghindari galat *floating point*.

**Prinsip konsistensi antar-modul:** dana target hanya *dialokasikan* (tetap berada di dompet), sedangkan dana investasi *dikonversi* (keluar dari dompet menjadi posisi aset). Karena itu Total Saldo tidak berubah saat setoran target, tetapi berkurang saat pembelian aset.

#### 2.2.1 Spendable Balance (Saldo Bebas Pakai)

$$W = \sum_{i \in \text{dompet aktif}} \text{saldo}_i$$

$$G = \sum_{j \in \text{target aktif}} S_j$$

$$\text{Spendable} = W - G - B_{\min}$$

* $W =$ Total Saldo seluruh dompet aktif; $G =$ total dana yang dialokasikan ke semua target; $S_j =$ saldo terkumpul target $j$.
* $B_{\min} =$ *buffer* minimum yang dapat diatur pengguna (default $0$).
* $\text{Spendable} < 0$ ditampilkan sebagai peringatan "alokasi melebihi saldo", bukan dibulatkan ke nol.
* Pengeluaran biasa mengurangi $W$; setoran target menambah $G$; pembelian aset mengurangi $W$. Ketiganya mengurangi *Spendable* secara langsung.

#### 2.2.2 Dynamic Saving Rate (Kebutuhan Setoran)

$$D_{\text{sisa}} = \max\left(1, \ \text{tenggat} - \text{hari ini}\right) \quad \text{(dalam hari)}$$

$$R_{\text{harian}} = \frac{\max(0, \ T - S)}{D_{\text{sisa}}}$$

$$R_{\text{mingguan}} = \frac{\max(0, \ T - S)}{\lceil D_{\text{sisa}} / 7 \rceil}$$

* $T =$ nominal target, $S =$ saldo terkumpul saat ini.
* Angka dihitung ulang setiap hari dan setiap kali $S$ berubah, sehingga setoran yang terlewat otomatis menaikkan kebutuhan hari berikutnya.
* Bila tenggat sudah lewat dan $S < T$, status menjadi `DELAYED` dan UI meminta pengguna menetapkan tenggat baru.

#### 2.2.3 Saving Velocity, ETA, dan Status

$$v = \frac{\sum_{k \in \text{30 hari terakhir}} c_k}{\min(30, \ d_{\text{berjalan}})}$$

$$\text{ETA} = \text{hari ini} + \left\lceil \frac{T - S}{v} \right\rceil \quad (v > 0)$$

$$\text{status} = \begin{cases} \text{ACHIEVED} & S \ge T \\ \text{ON\_TRACK} & S < T, \ v > 0, \ \text{ETA} \le \text{tenggat} \\ \text{DELAYED} & \text{selain itu (termasuk } v \le 0 \text{)} \end{cases}$$

* $c_k =$ kontribusi bersih (setoran dikurangi penarikan) pada hari $k$; $d_{\text{berjalan}} =$ jumlah hari sejak target dibuat (minimal $1$).
* *Saving Velocity* $v$ adalah rata-rata setoran bersih per hari; $v \le 0$ (belum ada setoran atau penarikan melebihi setoran) berarti ETA tidak terdefinisi dan status otomatis `DELAYED`.

**Impact Warning.** Misalkan $\mu$ adalah rata-rata pengeluaran harian 30 hari terakhir (tidak termasuk transfer dan konversi aset), dan $x$ adalah total pengeluaran hari ini:

$$\Delta = \max(0, \ x - \mu)$$

$$\text{Hari mundur} = \left\lceil \frac{T - S + \Delta}{v} \right\rceil - \left\lceil \frac{T - S}{v} \right\rceil \quad (v > 0)$$

* Peringatan ditampilkan bila $v > 0$, $\Delta > 0$, dan $\text{Hari mundur} \ge 1$, dengan teks konkret, misalnya *"Belanja hari ini Rp250.000 di atas rata-rata; target Motor mundur 3 hari"*.
* Bila status berubah dari `ON_TRACK` ke `DELAYED` akibat $\Delta$, peringatan berprioritas tinggi dan memicu notifikasi.
* Asumsi: $\Delta$ dianggap setoran yang seharusnya masuk ke target. Parameter $\mu$ dapat diganti dengan batas harian yang diatur pengguna (atau dinonaktifkan bila histori transaksi $< 7\text{ hari}$ dan belum ada batas harian manual).

**One-Tap Auto-Sweep Surplus:**

$$\text{Sweep} = \max(0, \ \text{Spendable} - B_{\text{sweep}})$$

* *Sweep* dibatasi hingga $T - S$ target berprioritas tertinggi; sisanya mengalir ke target berikutnya, dan hanya *Spendable* yang tersisa yang tidak dialokasikan. $B_{\text{sweep}}$ adalah *buffer* akhir periode (default $=$ rata-rata pengeluaran 3 hari, yaitu $3\mu$).

#### 2.2.4 Investasi dan Net Worth

**Pembelian berulang (Dollar Cost Averaging).** Biaya transaksi $F$ dimasukkan ke dalam *cost basis* (pembagian `BigDecimal` menggunakan `RoundingMode.HALF_UP` dengan skala minimal 8 digit desimal):

$$\bar{P}_{\text{baru}} = \frac{Q_{\text{lama}} \cdot \bar{P}_{\text{lama}} + Q_{\text{beli}} \cdot P_{\text{beli}} + F}{Q_{\text{lama}} + Q_{\text{beli}}}$$

$$Q_{\text{baru}} = Q_{\text{lama}} + Q_{\text{beli}}$$

$$\text{Kas keluar} = Q_{\text{beli}} \cdot P_{\text{beli}} + F \quad \Rightarrow \quad \text{Kas keluar} \le \text{Spendable}$$

**Penjualan.** *Average Buy Price* $\bar{P}$ tidak berubah selama posisi masih tersisa; hanya kuantitas yang berkurang (bila seluruh posisi terjual habis sehingga $Q_{\text{baru}} = 0$, nilai $\bar{P}$ di-reset ke $0$):

$$\text{Realized P/L} = Q_{\text{jual}} \cdot (P_{\text{jual}} - \bar{P}) - F_{\text{jual}}$$

**Floating Profit/Loss dan ROI** ($P_{\text{mkt}} =$ harga pasar terakhir):

$$\text{Floating P/L} = Q \cdot (P_{\text{mkt}} - \bar{P})$$

$$\text{ROI} = \frac{P_{\text{mkt}} - \bar{P}}{\bar{P}} \times 100\%$$

**Total Net Worth.** Dana target tidak ditambahkan lagi karena sudah menjadi bagian dari $W$:

$$\text{Net Worth} = W + \sum_{a \in \text{aset}} Q_a \cdot P_{\text{mkt},a}$$

$$\text{Net Worth} = \underbrace{\text{Spendable} + B_{\min}}_{\text{bebas}} + \underbrace{G}_{\text{target}} + \underbrace{\sum_{a} Q_a \cdot P_{\text{mkt},a}}_{\text{investasi}}$$

Identitas kedua dipakai sebagai uji konsistensi: kedua bentuk harus menghasilkan angka identik. Bila harga pasar tidak tersedia, $P_{\text{mkt}}$ memakai harga terakhir yang tersimpan (lihat 2.5).

---

### 2.3 Database Schema & Entity Relationship (Room)

Enam entitas utama plus tiga tabel pendukung. Seluruh nominal uang bertipe `Long` (rupiah); kuantitas dan harga aset bertipe `BigDecimal` yang disimpan sebagai `TEXT` lewat `TypeConverter`; waktu disimpan sebagai *epoch* milidetik (`Long`).

#### Skema Tabel

##### 1. `Wallets`
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `Long` | PK, `autoGenerate` |
| `name` | `String` | Nama dompet |
| `type` | `String` | `CASH`, `BANK`, `EWALLET` |
| `balance` | `Long` | Saldo saat ini; hanya diubah di dalam transaksi database |
| `is_archived` | `Boolean` | Dompet arsip tidak dihitung ke $W$; dompet hanya dapat diarsipkan bila $W_{\text{baru}} \ge G$ (total alokasi target aktif) |
| `created_at` | `Long` | Epoch ms |

##### 2. `Transactions`
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `Long` | PK, `autoGenerate` |
| `wallet_id` | `Long` | FK $\rightarrow$ `Wallets.id` (`RESTRICT`); dompet utama/sumber |
| `to_wallet_id` | `Long?` | FK $\rightarrow$ `Wallets.id`; hanya untuk `TRANSFER` |
| `type` | `String` | `INCOME`, `EXPENSE`, `TRANSFER`, `INVESTMENT_BUY`, `INVESTMENT_SELL` |
| `amount` | `Long` | Selalu $> 0$; arah ditentukan oleh `type` |
| `fee` | `Long` | Biaya admin opsional (default $0$); pada `TRANSFER` memotong dompet asal dan dihitung sebagai pengeluaran kategori `ADMIN_FEE` |
| `category` | `String?` | Kunci kategori (enum bawaan); `null` untuk transfer |
| `note` | `String?` | Catatan bebas |
| `occurred_at` | `Long` | Waktu transaksi |
| `created_at` | `Long` | Epoch ms |

##### 3. `SavingGoals`
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `Long` | PK, `autoGenerate` |
| `name` | `String` | Nama target |
| `target_amount` | `Long` | $T_j > 0$ |
| `deadline` | `Long` | Tenggat (epoch ms) |
| `priority` | `Int` | $1 =$ tertinggi; dipakai *Auto-Sweep* |
| `status` | `String` | `ON_TRACK`, `DELAYED`, `ACHIEVED` (di-*cache*; dihitung ulang oleh `UseCase`) |
| `is_archived` | `Boolean` | *Soft delete* |
| `created_at` | `Long` | Epoch ms |

> **Catatan:** Saldo terkumpul $S$ tidak disimpan sebagai kolom, melainkan dihitung `SUM(amount)` dari `GoalContributions` agar tidak ada selisih data (indeks pada `goal_id`).

##### 4. `GoalContributions`
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `Long` | PK, `autoGenerate` |
| `goal_id` | `Long` | FK $\rightarrow$ `SavingGoals.id` (`CASCADE`) |
| `wallet_id` | `Long?` | FK $\rightarrow$ `Wallets.id` (`SET_NULL`); dompet asal, hanya untuk jejak audit |
| `amount` | `Long` | Bertanda: setoran positif, penarikan negatif |
| `type` | `String` | `DEPOSIT`, `WITHDRAW`, `AUTO_SWEEP` |
| `created_at` | `Long` | Epoch ms |

##### 5. `Portfolios`
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `Long` | PK, `autoGenerate` |
| `asset_type` | `String` | `STOCK`, `MUTUAL_FUND`, `GOLD`, `CRYPTO` |
| `symbol` | `String` | Kode aset; unik bersama `asset_type` |
| `name` | `String` | Nama aset |
| `unit` | `String` | `lot`, `unit`, `gram`, `koin` |
| `quantity` | `BigDecimal` | $Q_i \ge 0$ |
| `avg_buy_price` | `BigDecimal` | Harga beli rata-rata (sudah termasuk biaya) |
| `last_price` | `BigDecimal?` | Harga pasar terakhir |
| `last_price_at` | `Long?` | Waktu harga terakhir diperoleh |
| `price_source` | `String` | `MANUAL` atau `API` |

##### 6. `InvestmentLogs`
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `Long` | PK, `autoGenerate` |
| `portfolio_id` | `Long` | FK $\rightarrow$ `Portfolios.id` (`RESTRICT`) |
| `wallet_id` | `Long` | FK $\rightarrow$ `Wallets.id` (`RESTRICT`); dompet sumber/tujuan kas |
| `transaction_id` | `Long` | FK $\rightarrow$ `Transactions.id`; mutasi kas pasangan (unik) |
| `type` | `String` | `BUY` atau `SELL` |
| `quantity` | `BigDecimal` | Jumlah transaksi |
| `price` | `BigDecimal` | Harga per unit |
| `fee` | `Long` | Biaya transaksi |
| `realized_pl` | `Long?` | Hanya untuk `SELL` |
| `created_at` | `Long` | Epoch ms |

##### 7. `NetWorthSnapshots` (Pendukung DB-04)
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `date` | `Long` | PK, *epoch day* |
| `cash` | `Long` | Total Saldo dompet aktif ($W$) pada akhir hari |
| `investment` | `Long` | Total nilai pasar investasi pada akhir hari |
| `net_worth` | `Long` | `cash + investment` |

##### 8. `CategoryBudgets` (Pendukung FT-05)
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `Long` | PK, `autoGenerate` |
| `category` | `String` | Kunci kategori pengeluaran; unik bersama `year_month` |
| `year_month` | `String` | Periode anggaran dalam format `YYYY-MM` (mis. `2026-10`) |
| `limit_amount` | `Long` | Batas anggaran bulanan ($> 0$) |
| `notified_80` | `Boolean` | Penanda notifikasi ambang $80\%$ sudah dikirim pada periode ini |
| `notified_100` | `Boolean` | Penanda notifikasi ambang $100\%$ sudah dikirim pada periode ini |
| `updated_at` | `Long` | Epoch ms |

##### 9. `RecurringTransactions` (Pendukung FT-07)
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `Long` | PK, `autoGenerate` |
| `wallet_id` | `Long` | FK $\rightarrow$ `Wallets.id` (`RESTRICT`); dompet sumber/tujuan |
| `type` | `String` | `INCOME` atau `EXPENSE` |
| `amount` | `Long` | Nominal transaksi ($> 0$) |
| `category` | `String` | Kunci kategori |
| `note` | `String?` | Catatan/nama langganan (mis. "Gaji", "Spotify") |
| `frequency` | `String` | `DAILY`, `WEEKLY`, `MONTHLY` |
| `next_run_at` | `Long` | Waktu eksekusi berikutnya (epoch ms); dievaluasi oleh `WorkManager` |
| `last_run_at` | `Long?` | Waktu eksekusi terakhir |
| `is_active` | `Boolean` | Status aktif/dijeda |
| `created_at` | `Long` | Epoch ms |

---

#### Relasi Antar Tabel

| Relasi | Kardinalitas | Perilaku |
| :--- | :---: | :--- |
| `Wallets` $\rightarrow$ `Transactions` | $1 : N$ | Dompet tidak dapat dihapus bila punya transaksi; gunakan arsip |
| `Wallets` $\rightarrow$ `RecurringTransactions` | $1 : N$ | Jadwal berulang terikat ke dompet aktif (`RESTRICT`); otomatis dijeda bila dompet diarsipkan |
| `SavingGoals` $\rightarrow$ `GoalContributions` | $1 : N$ | Hapus target menghapus kontribusinya (`CASCADE`) |
| `Wallets` $\rightarrow$ `GoalContributions` | $1 : N$ (opsional) | Hanya jejak asal dana; tidak memengaruhi saldo dompet |
| `Portfolios` $\rightarrow$ `InvestmentLogs` | $1 : N$ | Log tidak dihapus selama posisi ada |
| `InvestmentLogs` $\rightarrow$ `Transactions` | $1 : 1$ | Setiap log memiliki tepat satu mutasi kas |

---

#### Penggunaan `@Transaction` saat Dana Berpindah Antar Modul

Setiap operasi yang menyentuh lebih dari satu tabel dijalankan atomik: bila satu langkah gagal, seluruh perubahan dibatalkan (*rollback*). Implementasi memakai `@Transaction` pada fungsi DAO, atau `database.withTransaction { }` pada `Repository` bila melibatkan beberapa DAO.

| Operasi | Langkah dalam Satu Transaksi | Validasi Sebelum Commit |
| :--- | :--- | :--- |
| **Catat pengeluaran/pemasukan** | Insert `Transactions` $\rightarrow$ update `Wallets.balance` $\rightarrow$ evaluasi ambang `CategoryBudgets` (khusus `EXPENSE`) | $\text{Nominal} > 0$; dompet tidak diarsipkan |
| **Transfer antar dompet** | Insert 1 `Transactions` (`TRANSFER`, `fee`) $\rightarrow$ kurangi dompet asal sebesar `amount + fee` $\rightarrow$ tambah dompet tujuan sebesar `amount` | $\text{Dompet asal} \neq \text{tujuan}$; saldo dompet asal $\ge \text{amount} + \text{fee}$ |
| **Arsipkan dompet** | Set `Wallets.is_archived = true` $\rightarrow$ nonaktifkan `RecurringTransactions` terkait (`is_active = false`) | $W - \text{balance}_{\text{dompet}} \ge G$ (total alokasi target aktif) |
| **Setor ke target** | Insert `GoalContributions` $\rightarrow$ hitung ulang status `SavingGoals` | $\text{Nominal} \le \text{Spendable}$ (dihitung di dalam transaksi) |
| **Auto-Sweep Surplus** | Beberapa insert `GoalContributions` sesuai prioritas $\rightarrow$ update status tiap target | $\text{Total sweep} \le \text{Spendable} - B_{\text{sweep}}$ |
| **Beli aset (Asset Conversion)** | Insert `Transactions` (`INVESTMENT_BUY`) $\rightarrow$ kurangi `Wallets.balance` $\rightarrow$ upsert `Portfolios` (`quantity` dan `avg_buy_price` baru) $\rightarrow$ insert `InvestmentLogs` | $\text{Kas keluar} \le \text{Spendable}$ |
| **Jual aset** | Insert `Transactions` (`INVESTMENT_SELL`) $\rightarrow$ tambah `Wallets.balance` $\rightarrow$ kurangi `Portfolios.quantity` (reset `avg_buy_price = 0` bila `quantity == 0`) $\rightarrow$ insert `InvestmentLogs` dengan `realized_pl` | $\text{Kuantitas jual} \le \text{quantity}$ |
| **Eksekusi transaksi berulang** | Insert `Transactions` $\rightarrow$ update `Wallets.balance` $\rightarrow$ majukan `RecurringTransactions.next_run_at` & catat `last_run_at` | `is_active == true`; dompet tidak diarsipkan; saldo cukup (untuk `EXPENSE`) |

##### Contoh Kerangka Implementasi Pembelian Aset:

```kotlin
@Dao
abstract class InvestmentDao {
    @Transaction
    open suspend fun buyAsset(cmd: BuyAssetCommand) {
        val spendable = spendableBalance() // W - G - B_min, dibaca di dalam transaksi
        require(cmd.totalCost <= spendable) { "Saldo bebas pakai tidak cukup" }

        val txId = insertTransaction(cmd.toCashOutflow()) // INVESTMENT_BUY
        decreaseWalletBalance(cmd.walletId, cmd.totalCost)
        upsertPosition(cmd) // hitung avg_buy_price baru (2.2.4)
        insertLog(cmd.toLog(transactionId = txId))
    }
}
```

Pengecekan *Spendable* dilakukan di dalam transaksi yang sama dengan penulisan agar dua operasi bersamaan tidak dapat sama-sama lolos validasi.

---

### 2.4 UI/UX Flow & Screen Specifications

Navigasi utama memakai *Bottom Navigation* dengan empat tab: **Dashboard**, **Cashflow**, **Goals & Wealth**, dan **Analytics**. Tombol aksi cepat (FAB) **"+ Transaksi"** tersedia di Dashboard dan Cashflow. Setiap layar mengikuti pola `Loading` $\rightarrow$ `Success` $\rightarrow$ `Error/Empty`, dan menampilkan penanda *"data terakhir diperbarui"* bila memuat harga pasar.

#### Tab 1: Dashboard (Beranda & Net Worth)

| Komponen | Informasi yang Ditampilkan | Interaksi |
| :--- | :--- | :--- |
| **Kartu Net Worth** | Total *Net Worth*, perubahan vs bulan lalu (nominal dan $\%$), penanda kesegaran harga | Ketuk $\rightarrow$ rincian komposisi |
| **Kartu Saldo** | Total Saldo dan *Spendable Balance* berdampingan; selisihnya dijelaskan sebagai "dialokasikan ke target" | Ketuk *Spendable* $\rightarrow$ daftar alokasi per target |
| **Ringkasan target** | 3 target prioritas teratas: *progress bar*, status `ON_TRACK` / `DELAYED`, ETA | Ketuk $\rightarrow$ detail target |
| **Banner Impact Warning** | Dampak belanja hari ini terhadap ETA target (2.2.3) | Ketuk $\rightarrow$ lihat target terdampak; dapat ditutup |
| **Aksi cepat** | *Auto-Sweep Surplus*, Beli Aset, Transfer | Memunculkan dialog konfirmasi bernominal |
| **Distribusi aset** | Grafik donat: kas bebas, dana target, saham, reksadana, emas, kripto | Ketuk irisan $\rightarrow$ rincian |
| **Transaksi terbaru** | 5 transaksi terakhir | Ketuk $\rightarrow$ detail/edit |

#### Tab 2: Cashflow (Riwayat Transaksi & Dompet)

| Komponen | Informasi yang Ditampilkan | Interaksi |
| :--- | :--- | :--- |
| **Carousel dompet** | Nama, tipe, saldo tiap dompet; total di atas | Ketuk $\rightarrow$ riwayat dompet; tambah/arsipkan dompet |
| **Ringkasan periode** | Pemasukan, pengeluaran, selisih bulan berjalan; indikator anggaran per kategori | Ganti bulan lewat selektor |
| **Daftar transaksi** | Dikelompokkan per tanggal; ikon kategori, nominal berwarna (masuk/keluar), dompet; transaksi konversi aset dan setoran target diberi lencana | Cari, filter (dompet, kategori, tipe), geser untuk edit/hapus |
| **Form transaksi** | Tipe, nominal, kategori, dompet, tanggal, catatan | Validasi *inline*; tampilkan *Spendable* setelah transaksi sebelum disimpan |

#### Tab 3: Goals & Wealth (Tabungan Target & Portofolio Investasi)

Dua segmen: **Goals** dan **Portofolio**.

| Segmen | Komponen | Informasi yang Ditampilkan |
| :--- | :--- | :--- |
| **Goals** | **Daftar target** | Nama, terkumpul / target, *progress bar*, sisa hari, status |
| **Goals** | **Detail target** | *Dynamic Saving Rate* harian dan mingguan, *Saving Velocity*, ETA vs tenggat, riwayat kontribusi, tombol Setor, Tarik, *Auto-Sweep* |
| **Goals** | **Simulasi dampak** | Slider *"jika belanja bertambah Rp $X$"* menampilkan perubahan ETA secara langsung |
| **Portofolio** | **Ringkasan** | Total nilai pasar, *Floating P/L* total, ROI total |
| **Portofolio** | **Daftar aset** | Simbol, kuantitas, *Average Buy Price*, harga pasar, *Floating P/L* dan ROI (hijau/merah), penanda `MANUAL` atau waktu pembaruan API |
| **Portofolio** | **Form beli/jual** | Aset, kuantitas, harga, biaya, dompet sumber; pratinjau *Average Buy Price* baru dan kas keluar; ditolak bila melebihi *Spendable* |
| **Portofolio** | **Detail aset** | Riwayat `InvestmentLogs`, *realized P/L*, grafik harga |

#### Tab 4: Analytics (Laporan Visual Bulanan)

| Komponen | Informasi yang Ditampilkan |
| :--- | :--- |
| **Selektor bulan** | Navigasi bulan dan perbandingan dengan bulan sebelumnya |
| **Pemasukan vs pengeluaran** | Grafik batang per minggu atau per hari |
| **Pengeluaran per kategori** | Grafik donat dan daftar peringkat kategori, termasuk persentase |
| **Savings Rate** | $(\text{Pemasukan} - \text{pengeluaran}) / \text{pemasukan}$, dengan tren 6 bulan |
| **Tren Net Worth** | Grafik garis dari `NetWorthSnapshots` (1B/3B/1T/Semua) |
| **Kinerja investasi** | *Floating P/L* dan ROI per kelas aset |
| **Ekspor** | Tombol ekspor CSV/PDF (P2) |

#### Alur Pengguna Kritis
1. **Onboarding:** buat dompet pertama dengan saldo awal $\rightarrow$ opsional tetapkan target pertama $\rightarrow$ Dashboard.
2. **Belanja harian:** FAB $\rightarrow$ form transaksi $\rightarrow$ simpan $\rightarrow$ *Spendable* diperbarui $\rightarrow$ bila $\Delta > 0$, banner *Impact Warning* muncul di Dashboard.
3. **Menabung:** Detail target $\rightarrow$ Setor atau *Auto-Sweep* $\rightarrow$ konfirmasi nominal $\rightarrow$ *Spendable* turun, *progress* naik, ETA dihitung ulang.
4. **Berinvestasi:** Portofolio $\rightarrow$ Beli $\rightarrow$ pratinjau *Average Buy Price* dan kas keluar $\rightarrow$ konfirmasi $\rightarrow$ kas terpotong, posisi dan *Net Worth* diperbarui.

---

### 2.5 Non-Functional Requirements (NFR)

#### Performa

| ID | Persyaratan | Target (Perangkat Acuan: Android 8+, RAM 3 GB) |
| :--- | :--- | :--- |
| **NFR-P1** | *Cold start* hingga Dashboard menampilkan data lokal | $< 2\text{ detik}$ |
| **NFR-P2** | Waktu muat layar dari Room (Dashboard, Cashflow, Goals) | $\le 500\text{ ms}$ untuk $10.000$ transaksi |
| **NFR-P3** | Penyimpanan transaksi (termasuk transaksi atomik lintas tabel) | $< 200\text{ ms}$ |
| **NFR-P4** | Kalkulasi *Net Worth* dan *Spendable* | Dihitung di `Dispatchers.Default` atau lewat *query* agregat; tidak memblokir *main thread* |
| **NFR-P5** | *Scroll* daftar transaksi | Paging 3 atau *lazy loading*; tanpa *jank* (target $60\text{ fps}$) |
| **NFR-P6** | *Query* agregat | Indeks pada `wallet_id`, `occurred_at`, `goal_id`, `portfolio_id`, `(category, year_month)`, `next_run_at` |

> Target angka di atas adalah acuan awal yang perlu divalidasi dengan *benchmark* pada perangkat nyata sebelum dikunci.

#### Offline-First dan Valuasi Investasi Tanpa Internet

Seluruh fitur Modul 1, 2, dan 3 harus berfungsi penuh tanpa internet. Jaringan hanya dibutuhkan untuk memperbarui harga pasar.

* **Harga terakhir tetap dipakai.** Valuasi memakai `last_price` dari Room. Bila jaringan gagal, nilai tidak dihapus atau dinolkan.
* **Penanda usia data.** Setiap harga menampilkan waktu `last_price_at`. Bila lebih tua dari 24 jam, UI menampilkan label *"harga tertunda"* dan *Net Worth* diberi keterangan *"berdasarkan harga per [tanggal]"*.
* **Fallback bertingkat.** Urutan sumber harga: API terbaru $\rightarrow$ harga API tersimpan $\rightarrow$ harga manual terakhir $\rightarrow$ harga beli rata-rata (hanya bila belum ada harga sama sekali, ditandai `ESTIMATE`).
* **Sinkronisasi.** `Repository` memanggil API, menyimpan hasil ke Room, lalu UI membaca ulang dari Room. `WorkManager` menjalankan sinkronisasi periodik dengan *constraint* `NetworkType.CONNECTED`, ulang dengan *exponential backoff*.
* **Cache HTTP.** OkHttp *cache* diaktifkan sebagai lapisan tambahan, bukan sumber kebenaran.

#### Keamanan Data Lokal

| ID | Persyaratan | Implementasi |
| :--- | :--- | :--- |
| **NFR-S1** | Autentikasi saat membuka aplikasi | `BiometricPrompt` (sidik jari/wajah) dengan *fallback* PIN/pola perangkat (`DEVICE_CREDENTIAL`); kunci ulang setelah aplikasi di latar belakang melewati ambang waktu yang dapat diatur |
| **NFR-S2** | Enkripsi database | Room dengan SQLCipher (`SupportFactory`); kunci disimpan terenkripsi, diturunkan dari Android Keystore |
| **NFR-S3** | Enkripsi preferensi | DataStore dengan enkripsi berbasis Keystore untuk data sensitif; PIN tidak pernah disimpan polos |
| **NFR-S4** | Perlindungan layar | `FLAG_SECURE` pada layar finansial agar tidak muncul di tangkapan layar atau *recent apps* |
| **NFR-S5** | Cadangan | `allowBackup=false` untuk berkas database; ekspor/impor terenkripsi hanya atas tindakan pengguna |
| **NFR-S6** | Komunikasi jaringan | HTTPS saja; tidak ada data keuangan pribadi yang dikirim ke server (hanya simbol aset untuk permintaan harga); pertimbangkan *certificate pinning* |
| **NFR-S7** | Privasi | Tanpa akun wajib; tanpa analitik yang memuat nominal atau isi transaksi |

#### Keandalan dan Kualitas

| ID | Persyaratan | Target |
| :--- | :--- | :--- |
| **NFR-R1** | Integritas data | Semua operasi lintas tabel dalam satu transaksi Room; uji penyimpanan gagal di tengah operasi harus tidak meninggalkan perubahan parsial |
| **NFR-R2** | Migrasi database | Setiap perubahan skema disertai `Migration` dan `MigrationTestHelper`; `fallbackToDestructiveMigration` dilarang |
| **NFR-R3** | Cakupan uji unit `UseCase` keuangan | $> 90\%$ untuk rumus 2.2 (termasuk kasus tepi: pembagian nol, tenggat lewat, kuantitas desimal) |
| **NFR-R4** | Kompatibilitas | `minSdk 26` (Android 8.0), `targetSdk` mengikuti ketentuan Google Play terbaru |
| **NFR-R5** | Aksesibilitas dan lokalisasi | Material 3, kontras memadai, dukungan *font scaling*; format Rupiah (`Rp1.250.000`) dan bahasa Indonesia sebagai default |
| **NFR-R6** | Pemantauan *crash* | Pelaporan *crash* tanpa memuat data keuangan |

---

### 2.6 Development Roadmap (Sprint Phases)

Pengembangan dibagi tiga fase rilis; tiap fase menghasilkan aplikasi yang dapat dipakai, dan fase berikutnya hanya dimulai setelah *gate* kualitas terpenuhi. Estimasi *sprint* (2 minggu) di bawah adalah asumsi awal untuk tim kecil dan perlu dikonfirmasi setelah kapasitas tim diketahui.

#### Alur Fase & Quality Gates

```text
+--------------------------+          +--------------------------+          +--------------------------+
|     Fase 1: MVP Core     |  Gate 1  | Fase 2: Investasi Manual |  Gate 2  |   Fase 3: API Otomatis   |
| ------------------------ | -------> | ------------------------ | -------> | ------------------------ |
| - Wallet dan transaksi   | MVP      | - Portofolio input manual| Net Worth| - Retrofit cache offline |
| - Spendable Balance      | stabil   | - Asset Conversion       | akurat   | - Sinkron via WorkManager|
| - Goal, Dynamic Saving   | (Uji     | - Average Cost, Float P/L| (Rumus   | - Batas anggaran + notif |
| - Impact Warn, AutoSweep | rumus    | - Net Worth & distribusi | 2.2.4    | - Analytics dan ekspor   |
| - Biometrik + enkripsi DB| 2.2)     | - Snapshot Net Worth     | lulus)   | - Hardening & Play Store |
+--------------------------+          +--------------------------+          +--------------------------+
```

*Gate 1* dan *Gate 2* memastikan rumus keuangan terbukti benar sebelum modul yang bergantung padanya dibangun.

#### Detail Cakupan Sprint

| Fase | Sprint (Estimasi) | Cakupan | Kriteria Keluar (Exit Criteria) |
| :--- | :---: | :--- | :--- |
| **Fase 1: MVP Core (Tracker + Goals)** | 1–4 | **S1:** Setup Clean Architecture, Hilt, Room, tema Material 3, skema `Wallets` dan `Transactions`.<br>**S2:** FT-01 s.d. FT-04, *Spendable Balance*.<br>**S3:** `SavingGoals` dan `GoalContributions`, GS-01 s.d. GS-03.<br>**S4:** *Impact Warning*, *Auto-Sweep*, anggaran dasar (`CategoryBudgets`, FT-05), `BiometricPrompt`, enkripsi database, Dashboard dasar. | Semua P0 Modul 1 dan 2 lulus; uji unit rumus 2.2.1 s.d. 2.2.3 hijau; NFR-P1 hingga NFR-P3 terpenuhi. |
| **Fase 2: Investment Manual Input & Net Worth** | 5–7 | **S5:** `Portfolios` dan `InvestmentLogs`, IV-01, IV-06 (harga manual).<br>**S6:** *Asset Conversion* beli/jual atomik, *Average Cost*, *Floating P/L* (IV-02 s.d. IV-05).<br>**S7:** *Net Worth*, distribusi aset, `NetWorthSnapshots`, Analytics dasar (DB-01 s.d. DB-04). | Semua P0 Modul 3 dan 4 lulus; uji integrasi `@Transaction` termasuk skenario gagal di tengah operasi; identitas *Net Worth* (2.2.4) lolos uji. |
| **Fase 3: Automated Market API & Background Worker** | 8–10 | **S8:** Integrasi Retrofit/OkHttp ke penyedia harga pasar, *cache* Room, *fallback* offline (IV-07).<br>**S9:** `WorkManager` untuk sinkronisasi harga, evaluasi anggaran, dan transaksi berulang (`RecurringTransactions`, FT-07); notifikasi.<br>**S10:** Ekspor (DB-05), uji performa, *hardening* keamanan, beta tertutup lalu rilis. | Valuasi tetap berfungsi tanpa internet; NFR-R1 s.d. NFR-R3 terpenuhi; tidak ada *crash blocker* pada beta. |

#### Risiko dan Dependensi
* **Penyedia data harga.** Pilihan API untuk saham, reksadana, emas, dan kripto (cakupan Indonesia, ketentuan lisensi, batas permintaan) belum ditetapkan dan harus diputuskan sebelum Sprint 8. Fase 2 sengaja memakai input manual agar tidak terblokir.
* **Presisi angka.** Kesalahan pembulatan pada *Average Buy Price* harus dicegah sejak skema Fase 2 (`BigDecimal`).
* **Migrasi skema.** Penambahan tabel di Fase 2 dan 3 mengharuskan migrasi Room teruji sejak Fase 1.