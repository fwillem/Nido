V2
# Instruction Management
> **Configuration**: This project uses a global configuration file.
> **Load Order**: 
> 1. Read `../GEMINI_Z_GLOBAL.md` (Global Rules)
> 2. Read `GEMINI.md` (This Project Rules)

## UI/Output Protocol
- Before the answer write **LOCAL AGENTS vX** (where X is the integer version number found at the top of this file `GEMINI.md`).

## Mandatory Verification
- **ALWAYS** run the following command BEFORE asking the user to check modifications:
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; ./gradlew :app:assembleDebug
  ```
- **IF EXIT CODE IS 1 (FAIL)**: You MUST investigate the failure (check logs, run specific compile tasks) and fix the errors yourself. Do NOT pass a broken build to the user.


