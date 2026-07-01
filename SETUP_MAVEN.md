# Maven Setup Guide para sa Latte and Letters

## ✅ SETUP COMPLETE!

Maven 3.9.15 has been successfully installed to:
```
tools/apache-maven-3.9.15/
```

Java 17 is already installed and ready to use.

You can now run the project using:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-latte-and-letters.ps1
```

---

## Original Problem (SOLVED)
Ang Maven ay hindi naka-install o naka-configure sa iyong system.

## Solusyon 1: I-download ang Maven sa Project Folder (Recommended)

1. **Download Maven**
   - Pumunta sa: https://maven.apache.org/download.cgi
   - Download ang "Binary zip archive" (apache-maven-3.9.x-bin.zip)

2. **Extract sa Project**
   ```
   C:\Users\Joanna Requitud\Documents\LU_Latte and Letters_Project\tools\apache-maven-3.9.15\
   ```
   
   Dapat may ganito ang structure:
   ```
   tools/
     apache-maven-3.9.15/
       bin/
         mvn.cmd
       conf/
       lib/
   ```

3. **I-check ang Java Installation**
   
   Buksan ang PowerShell at i-type:
   ```powershell
   java -version
   ```
   
   Dapat may output na Java 17 or higher. Kung wala:
   - Download Java 17: https://adoptium.net/temurin/releases/
   - I-install at i-restart ang terminal

4. **Subukan Ulit ang Run Script**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\run-latte-and-letters.ps1
   ```

## Solusyon 2: I-install sa System PATH

1. **Download Maven**
   - https://maven.apache.org/download.cgi
   - Extract sa: `C:\Program Files\Apache\apache-maven-3.9.15`

2. **I-add sa System PATH**
   - Press Windows Key, i-type: "Environment Variables"
   - Click "Edit the system environment variables"
   - Click "Environment Variables" button
   - Sa "System variables", hanapin ang "Path"
   - Click "Edit"
   - Click "New"
   - I-add: `C:\Program Files\Apache\apache-maven-3.9.15\bin`
   - Click OK sa lahat

3. **Restart ang PowerShell**
   - I-close lahat ng terminal windows
   - Buksan ulit ang PowerShell

4. **I-test**
   ```powershell
   mvn --version
   ```

## Solusyon 3: Gumamit ng Maven Wrapper (Para sa advanced users)

Pwede ring i-add ang Maven Wrapper sa project para hindi na kailangan ng separate installation.

## Checklist Bago Mag-run

- [ ] Java 17 installed (`java -version`)
- [ ] Maven installed (`mvn --version` o may `tools/apache-maven-3.9.x/`)
- [ ] MySQL server running
- [ ] Database `latte_and_letters` created (run `database/schema.sql`)
- [ ] Demo data loaded (run `database/demo-data.sql`)

## Pag nag-error pa rin

1. Check kung tumatakbo ang MySQL:
   ```powershell
   Get-Service MySQL*
   ```

2. Subukan i-test ang database connection sa MySQL Workbench

3. I-check kung tama ang password sa `run-latte-and-letters.ps1`

## Mga Common Errors

### "mvn is not recognized"
- Maven ay hindi naka-install o hindi nasa PATH
- Solution: Sundin ang mga solusyon sa itaas

### "Could not find or load main class"
- Java ay hindi naka-install o wrong version
- Solution: I-install Java 17

### "Access denied for user"
- Wrong MySQL password o user
- Solution: I-check ang credentials sa MySQL Workbench

### "Unknown database 'latte_and_letters'"
- Database ay hindi pa na-create
- Solution: I-run ang `database/schema.sql` sa MySQL Workbench
