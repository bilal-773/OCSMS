# OCSMS — Setup Guide for IntelliJ IDEA
## (Recommended method — works without manually installing Maven or setting PATH)

---

## Step 1 — Install Java 17+ JDK

> ⚠️ Your machine has only **Java 8 JRE** — JavaFX 21 requires **Java 17 JDK minimum**.

1. Go to: **https://adoptium.net/temurin/releases/**
2. Select: **Java 17**, **Windows**, **x64**, **JDK**
3. Download and run the `.msi` installer
4. ✅ Check **"Set JAVA_HOME"** and **"Add to PATH"** during installation
5. Restart your PC

**Verify:**
```
java -version
```
Should show: `openjdk version "17..."`

---

## Step 2 — Install IntelliJ IDEA (Community Edition — FREE)

1. Go to: **https://www.jetbrains.com/idea/download/**
2. Download **Community Edition** (free)
3. Install with defaults

---

## Step 3 — Open the Project

1. Open **IntelliJ IDEA**
2. Click **"Open"** → browse to:
   ```
   C:\Users\M Bilal\Desktop\SDA CODE\ocsms
   ```
3. IntelliJ detects `pom.xml` → click **"Open as Maven Project"**
4. Wait for Maven to **download all dependencies** (first time ~2–5 min, needs internet)

---

## Step 4 — Configure Project SDK

1. Go to **File → Project Structure → Project**
2. Set **SDK** to: `Java 17` (or whatever JDK you installed)
3. Set **Language Level** to: `17`
4. Click **OK**

---

## Step 5 — Run the Application

**Option A — Maven Run Configuration (recommended):**
1. Open the **Maven** panel (right side of IntelliJ)
2. Expand **Plugins → javafx**
3. Double-click **`javafx:run`**

**Option B — Run Main directly:**
1. Open `src/main/java/com/ocsms/Main.java`
2. Click the ▶ green arrow next to `public static void main`
3. If you see a "Module not found" error, add VM arguments:
   ```
   --module-path "C:\path\to\javafx\lib" --add-modules javafx.controls,javafx.fxml
   ```
   *(IntelliJ with JavaFX plugin handles this automatically)*

**Option C — Terminal (if Java 17 + Maven installed):**
```bash
cd "C:\Users\M Bilal\Desktop\SDA CODE\ocsms"
mvn clean javafx:run
```

---

## Step 6 — Login & Test

Use these demo credentials:

| Role | Roll Number | Password |
|---|---|---|
| 🎓 Student | `21F-3456` | `pass123` |
| 🏛️ Society Admin | `20F-5001` | `admin123` |
| 👨‍🏫 Faculty Advisor | `FA001` | `faculty123` |
| 🔐 University Admin | `ADMIN001` | `admin@nu` |

---

## Demo Flow (for presentation)

### Flow 1 — Membership (login as Student → Admin)
1. Login as **Student** (`21F-3456 / pass123`)
2. Go to **Societies** → click **SOFTEC** → click **Apply for Membership**
3. Enter motivation (min 20 chars) → Submit
4. Logout → Login as **Admin** (`20F-5001 / admin123`)
5. Go to **Membership** → see pending application
6. Click application → click **✔ Approve**
7. Notice notification badge updates

### Flow 2 — Event Registration (login as Student)
1. Login as **Student**
2. Go to **Events** → click **SOFTEC 2025**
3. Click **Register for Event**
4. See confirmation status ✔

### Flow 3 — Attendance + Certificate (login as Admin)
1. Login as **Admin** (`20F-5001 / admin123`)
2. Go to **Attendance & Certificates**
3. Select event from dropdown
4. Check present checkboxes ✓
5. Click **Save Attendance**
6. Click **Generate Certificates** → PDF saved to `/certificates/`

---

## Troubleshooting

| Problem | Solution |
|---|---|
| `JAVA_HOME not set` | Reinstall JDK 17, check "Set JAVA_HOME" during install |
| `mvn not recognized` | Open in IntelliJ IDEA instead |
| `Module not found` | Add `--add-modules javafx.controls,javafx.fxml` to VM args |
| `Unsupported class version` | Your Java is too old — needs Java 17+ |
| `Could not find or load main class` | Right-click `Main.java` → Run |
| PDF not generated | Check write permissions, run as Administrator |

---

*All data is in-memory — resets on restart (by design, no DB server needed for demo)*
