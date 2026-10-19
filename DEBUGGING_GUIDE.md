# Debugging Guide: Exceptions in IntelliJ IDEA

Line numbers refer to the code as delivered. If you edit a file, look for the quoted statement instead. Many key lines also carry a `// BREAKPOINT` comment in the source.

Paths are relative to `src/main/java/com/example/exceptions/`.

---

## 1. Debugger controls (Windows/Linux keymap)

| Action | Shortcut | What it does | When to use it in this demo |
|---|---|---|---|
| Toggle line breakpoint | `Ctrl+F8` (or click the gutter) | Red dot: pause *before* this line runs | Every scenario |
| **Step Over** | `F8` | Run the current line, stop on the next line *in this method* | Walk through `withdraw` line by line |
| **Step Into** | `F7` | Go *inside* the method called on this line | Enter `validator.ensureSufficientFunds(...)` |
| Smart Step Into | `Shift+F7` | Choose which call to enter when a line has several | `AccountResponse.from(accountService.getAccount(id))` |
| **Step Out** | `Shift+F8` | Finish the current method and return to the caller | Leave a JDK/Spring method you stepped into by mistake |
| **Resume Program** | `F9` | Run until the next breakpoint | Jump from the throw to the global handler |
| **Evaluate Expression** | `Alt+F8` | Run any Java expression in the paused frame | `account.getBalance().compareTo(amount)` |
| View Breakpoints | `Ctrl+Shift+F8` | List all breakpoints; add **exception breakpoints** | Section 3 |
| Mute Breakpoints | (icon in Debug toolbar) | Temporarily disable all breakpoints | Run the app "normally" between demos |
| Reset Frame (Drop Frame) | (icon in Frames panel) | Pop the current frame and re-run the method | Replay a method without re-sending the request |

macOS: `F8`/`F7`/`F9` are the same if you use the default macOS keymap with function keys. Evaluate Expression is `⌥F8`, and View Breakpoints is `⇧⌘F8`.

### The panels you will point at

- **Frames** (left of the Debug tool window): the **call stack**. The top frame is the method currently executing; each frame below it is the caller that is still waiting for it to return. Library frames (Spring, Tomcat, reflection) are greyed or collapsed. Toggle them with the funnel icon *"Hide frames from libraries"*.
- **Variables** (right): local variables, parameters and `this` for the *selected* frame. Click a lower frame to see the caller's variables. The caller's state is still alive because that method has not finished.
- **Threads** dropdown (above Frames): web requests run on Tomcat threads named `http-nio-8080-exec-N`; demo `main` programs run on `main`.
- **Console**: program output and log lines.

---

## 2. Line breakpoint vs exception breakpoint

| | Line breakpoint | Exception breakpoint |
|---|---|---|
| Set by | Clicking the gutter on a specific line | `Ctrl+Shift+F8` → `+` → **Java Exception Breakpoints** → choose a class |
| Pauses | *Before* that line executes, every time it is reached | At the exact moment an exception of that type (or a subclass) is **thrown**, wherever that is |
| You need to know | Where the problem is | Only the exception type |
| Typical use | Watching a known flow | "Something throws a NumberFormatException somewhere, show me where" |
| Visual | Red dot ● | Red lightning ⚡ in the breakpoints list |

**Demo of the difference** (do it live in under a minute):

1. Line breakpoint on `TransactionValidator.java:42` (`if (account.getBalance().compareTo(amount) < 0) {`). Send a **successful** withdrawal of `10.00`. The debugger **stops**, because the line runs on every withdrawal.
2. Remove it. Add an exception breakpoint for `InsufficientFundsException` with **Caught** ticked. Send the same successful withdrawal: **no stop**. Send a withdrawal of `250.00`: it **stops on line 43**, the `throw` statement, because only now is an exception created and thrown.

### Caught vs uncaught exception breakpoints

In the exception breakpoint dialog there are two checkboxes:

- **Caught exception**: pause even if some `catch` block somewhere up the stack will handle it.
- **Uncaught exception**: pause only if *nothing* will catch it, so it would kill the thread.

> ⚠️ **Important for Spring:** in a web request, *every* exception is eventually caught by Spring's `DispatcherServlet`/`InvocableHandlerMethod`. So for REST demos an exception breakpoint with only **Uncaught** ticked will **never fire**. Tick **Caught**.
> In the plain `UnhandledExceptionDemo` main program, nothing catches the exception, so **Uncaught** fires.

**Avoid noise:** the JVM and Spring throw and catch many exceptions internally (class loading, for example). On an exception breakpoint for a broad type such as `IllegalArgumentException` or `Exception`, open the breakpoint's settings → **Class filters** → add `com.example.exceptions.*`. Then it only stops when the exception passes through *our* code. (Specific custom types such as `InsufficientFundsException` need no filter.)

---

## 3. Setting up an exception breakpoint (step by step)

1. `Ctrl+Shift+F8` (Run → View Breakpoints…)
2. Click **+** → **Java Exception Breakpoints**
3. Type `InsufficientFundsException` → OK
4. On the right: ✅ *Enabled*, ✅ *Suspend: All* (default), ✅ **Caught exception**, ✅ Uncaught exception
5. Optional: *Condition* `requestedAmount.compareTo(new java.math.BigDecimal("1000")) > 0` to stop only for large amounts.
6. Done. It appears under "Java Exception Breakpoints" with a ⚡ icon.

---

## 4. Scenario A: try/catch in a plain Java program (warm-up)

**File:** `examples/TryCatchDemo.java`

| # | Breakpoint | Line | Statement |
|---|---|---|---|
| 1 | Before the exception | 26 | `int monthly = monthlyRepayment(loanPounds, months);` |
| 2 | Throwing statement | 38 | `return loanPounds / months;` |
| 3 | Catch block | 30 | `System.out.println("  catch: " + e.getMessage());` |

**Run:** green ▶ next to `main` → **Debug 'TryCatchDemo.main()'**.

1. **First stop at line 26** (months = 24). Variables: `loanPounds = 12000`, `months = 24`. Press `F9`, which stops at line 38, then `F9` again. This call succeeds, so the catch breakpoint is **not** hit.
2. **Second stop at line 26** (months = 0). Say: *"Now `months` is 0."* Press `F7` (Step Into) to enter `monthlyRepayment`. Frames now shows `monthlyRepayment` on top of `describeRepayment` on top of `main`.
3. At line 38, open **Evaluate Expression** (`Alt+F8`) → `months` → `0`. Press `F8` (Step Over). The division throws, and the debugger lands **directly on line 30, inside the catch block**. Line 27 (`result = ...`) and line 28 were skipped, and `monthlyRepayment`'s frame has disappeared from Frames.
4. In Variables expand `e`: `detailMessage = "/ by zero"`. Evaluate `e.getStackTrace()[0]`, which shows `TryCatchDemo.monthlyRepayment(TryCatchDemo.java:38)`.
5. `F8` twice: line 33 (`after try/catch`) runs, so execution **continues** after the catch.

---

## 5. Scenario B: service exception travels to the global handler ⭐ (main demo)

**Request:** `POST /api/accounts/ACC-1002/withdraw` with `{"amount":250.00}` (Bob has 100.00).

| # | Purpose | File | Line | Statement |
|---|---|---|---|---|
| 1 | Entry into controller | `controller/AccountController.java` | 63 | `Account account = accountService.withdraw(id, request.amount());` |
| 2 | Just before the exception | `service/AccountService.java` | 94 | `validator.ensureSufficientFunds(account, amount);` |
| 3 | The throwing statement | `service/TransactionValidator.java` | 43 | `throw new InsufficientFundsException(...)` |
| 4 | Line that must be skipped | `service/AccountService.java` | 95 | `account.debit(amount);` |
| 5 | Global handler | `handler/GlobalExceptionHandler.java` | 41 | `log.warn("Insufficient funds: ...` (first line of `handleInsufficientFunds`) |

**Steps**

1. Start `ExceptionDemoApplication` with **Debug** (bug icon). Wait for `Started ExceptionDemoApplication`.
2. In a terminal:
   ```bash
   curl -i -X POST http://localhost:8080/api/accounts/ACC-1002/withdraw -H "Content-Type: application/json" -d '{"amount":250.00}'
   ```
   curl will appear to hang. That is expected, because the server thread is paused.
3. **Stop 1, AccountController:63.** Variables: `id = "ACC-1002"`, `request = AmountRequest[amount=250.00]`. Threads: `http-nio-8080-exec-1`. Say: *"Spring has already turned the JSON into a record for us."* Press `F9`.
4. **Stop 2, AccountService:94.** Variables: `account` → expand → `balance = 100.00`; `amount = 250.00`. Evaluate (`Alt+F8`): `account.getBalance().compareTo(amount)` → `-1`. Say: *"-1 means balance is less than amount, so the next call will throw."* Frames shows `withdraw` ← `AccountController.withdraw` ← (library frames) ← `DispatcherServlet.doDispatch` ...
5. Press `F7` (Step Into) → you are in `ensureSufficientFunds`. `F8` → you are on **Stop 3, line 43**, the `throw`.
6. **This is the key moment.** Point at Frames:
   ```
   ensureSufficientFunds:43, TransactionValidator
   withdraw:94, AccountService
   withdraw:63, AccountController
   ... (reflection / Spring frames)
   doDispatch:..., DispatcherServlet
   ```
   Say: *"Three of our methods are on the stack. None of them has finished."*
7. Press `F9` (Resume). **Breakpoint 4 at line 95 is NOT hit.** The debugger goes straight to **Stop 5, GlobalExceptionHandler:41**. Say: *"`account.debit` never ran. The stack unwound: the validator, service and controller frames were popped one after another, because none of them had a catch block for this exception."*
8. At the handler, Frames now shows `handleInsufficientFunds` ← `ExceptionHandlerExceptionResolver`/`HandlerExceptionResolverComposite` ← `DispatcherServlet.processHandlerException` ← `doDispatch`. **Our service and controller frames are gone.** Variables: `ex.balance = 100.00`, `ex.requestedAmount = 250.00`, `request.requestURI = "/api/accounts/ACC-1002/withdraw"`.
9. Evaluate `ex.getStackTrace()[0]` → `TransactionValidator.ensureSufficientFunds(TransactionValidator.java:43)`. Say: *"The exception remembers where it was created, even though those frames no longer exist."*
10. `F9`. curl now prints `HTTP/1.1 409` and the JSON. The balance is unchanged: `curl http://localhost:8080/api/accounts/ACC-1002/balance` → `100.00`.

**Same flow using an exception breakpoint instead:** remove breakpoints 1–3, add a Java Exception Breakpoint for `InsufficientFundsException` (Caught ✅), and send the request. The debugger stops on `TransactionValidator:43` without you having to know where the throw is.

### Other global-handler scenarios

| Request | Breakpoint at the throw | Handler breakpoint (`GlobalExceptionHandler`) | Expected |
|---|---|---|---|
| `GET /api/accounts/ACC-9999` | `AccountService.java:63` `throw new AccountNotFoundException(accountId);` | line 34 `handleAccountNotFound` | 404 |
| `POST /api/transfers` same account | `TransactionValidator.java:52` | line 48 `handleInvalidTransfer` | 400 |
| `POST .../deposit` `{"amount":-50}` | `TransactionValidator.java:31` | line 55 `handleIllegalArgument` | 400 |
| `GET /api/demo/unchecked` | `DemoController.java:43` `new BigDecimal(amount)` | line 55 `handleIllegalArgument` | 400 |
| `GET /api/demo/unexpected` | `DemoController.java:84` `branchCode.toUpperCase()` | line 87 `handleUnexpected` | 500 |

For `/api/demo/unchecked` the actual `throw` happens **inside the JDK** (`BigDecimal.<init>`). With a `NumberFormatException` exception breakpoint the debugger opens `BigDecimal.java` (decompiled or from JDK sources). Look at Frames: one frame below is `DemoController.unchecked:43`, which is our line. Click it to see `amount = "ten-pounds"`.

For `/api/demo/unexpected` evaluate `branchCode` → `null`. That is the cause of the NPE, and the Java 14+ message names it: `because "branchCode" is null`. The console shows the full stack trace logged by `handleUnexpected`. curl only gets the generic message.

---

## 6. Scenario C: checked exception handled locally (Approach A)

**Request:** `curl -i http://localhost:8080/api/demo/checked`

| # | File | Line | Statement |
|---|---|---|---|
| 1 | `controller/DemoController.java` | 60 | `String statement = statementService.loadStatement(accountId);` |
| 2 | `service/StatementService.java` | 42 | `try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {` |
| 3 | `controller/DemoController.java` | 64 | `log.info("No statement file for {}: {}", ...)` (inside `catch (FileNotFoundException e)`) |
| 4 | `handler/GlobalExceptionHandler.java` | 87 | `handleUnexpected`: should **not** be hit |

1. Stop 1: `accountId = "ACC-9999"`. `F7` into `loadStatement`.
2. Stop 2: Evaluate `file.toAbsolutePath()` → `...\statements\ACC-9999.txt`, then `java.nio.file.Files.exists(file)` → `false`.
3. `F8` → lands on Stop 3 in the **controller's** catch block. The service frame is gone. `e.getMessage()` → `statements\ACC-9999.txt (The system cannot find the file specified)`.
4. `F9` → the global handler is **not** reached, because we handled it locally. curl prints 404.

Then try `?accountId=ACC-1001` and step over the same lines. No catch block runs, and the response is 200.

---

## 7. Scenario D: an uncaught exception in a plain program

**File:** `examples/UnhandledExceptionDemo.java`

1. Add an exception breakpoint for `IllegalStateException` with only **Uncaught** ticked, and class filter `com.example.exceptions.*`.
2. Debug `main`. The debugger stops on line 37 `throw new IllegalStateException(...)`. IntelliJ knows no frame will catch it.
3. Frames: `currentInterestRate` ← `calculateInterest` ← `main`. Variables: `configuredRate = null`.
4. `F9` → console shows `Exception in thread "main" java.lang.IllegalStateException ...` and `Process finished with exit code 1`. Line 28 never printed.

---

## 8. Scenario E: finally always runs

**File:** `examples/FinallyDemo.java`. Breakpoint on line 53 (`AUDIT_LOG.add("unlock " + paymentId);`).

Debug `main`. It stops **twice**: once for `PAY-1` (success) and once for `PAY-2` (exception in flight). On the second stop, look at Frames and Variables: there is no exception variable to see, but the exception is "in flight". After the finally block ends (`F8` past line 54), the debugger jumps to `main`'s catch on line 29. Say: *"finally ran, and then the exception carried on propagating."*

## 9. Scenario F: StackOverflowError, safely

**File:** `examples/ErrorExamples.java`. Breakpoint on line 44 with **Condition** `balance.compareTo(new java.math.BigDecimal("1100")) > 0` (otherwise it would stop thousands of times). Debug `main`. When it stops, look at the Frames panel: dozens of identical `compoundInterestBroken:44` frames. **Mute breakpoints** and press `F9` to let it overflow. The program prints that it caught the error and carries on.

## 10. Debugging a failing test

1. Open `service/AccountServiceTest.java` (under `src/test/java`).
2. Breakpoint on `TransactionValidator.java:43`.
3. Click the gutter icon next to `withdrawMoreThanBalance` → **Debug**.
4. It stops on the throw. `F9` → the exception returns to `assertThrows`, which catches it and passes the test.

Use IntelliJ's own test runner (not `mvn test` in the terminal) when debugging: Maven runs tests in a separate forked JVM that IntelliJ's debugger is not attached to.

---

## 11. "My breakpoint does not trigger": checklist

| Cause | How to tell | Fix |
|---|---|---|
| Started with **Run** instead of **Debug** | Tool window says "Run", no Frames panel | Stop and start with the 🐞 Debug button |
| **Breakpoints muted** | Mute icon highlighted, dots appear greyed with a line | Click Mute Breakpoints again |
| **Another instance already on port 8080** (e.g. a `java -jar` or an earlier run) | Console: `Port 8080 was already in use`, and your requests go to the *other* process | Stop the old process (`Stop` in IntelliJ, or close the terminal running it), then debug again |
| Code changed but app not restarted | Line numbers drift; dot shows a ✗ "No executable code" | Restart the debug session (or Build → Recompile + HotSwap for small method-body changes) |
| Line has **no executable code** (blank, comment, `}` or a field declaration) | Dot has a ✗ or is hollow | Put the breakpoint on a real statement |
| That code path is not executed | e.g. breakpoint in `catch` but the request succeeded | Send the failing request (check the curl body and URL) |
| **Exception breakpoint has only "Uncaught"** in a Spring app | Never fires for REST requests | Tick **Caught exception** |
| Class filter excludes the location | Breakpoint settings → Class filters | Remove or adjust the filter |
| **Condition** is never true, or has an error | Breakpoint icon shows a "?"; IntelliJ shows an error popup | Fix or remove the condition |
| Breakpoint on a lambda line | IntelliJ offers "Line / Lambda / All" | Choose the lambda or All |
| Test run with `mvn test` in a terminal | Forked JVM, not debugged | Use the IDE test runner's Debug |
| Breakpoint set in the decompiled `.class` instead of the source | Tab title shows `.class` | Open the `.java` file (`Ctrl+N`) |
| Suspend policy "None" | Breakpoint just logs | Set Suspend to All or Thread |

---

## 12. What to say about the call stack while paused

- Each method call pushes a **stack frame** holding its parameters and local variables.
- When an exception is thrown, the JVM looks in the **current frame** for a matching `catch`. If there is none, it **pops** the frame (running any `finally` blocks on the way) and looks in the caller. This is **stack unwinding**.
- Methods whose frames are popped **never finish**. The statements after the call that threw (like `account.debit`) are never executed and their local variables are discarded.
- If unwinding reaches the bottom of the thread's stack with no catch, the thread terminates and the default uncaught-exception handler prints the stack trace. In a web request that never happens to our code, because Spring's `DispatcherServlet` catches the exception and hands it to the `@ExceptionHandler`.
- The exception object captured the full stack trace **when it was created**, which is why it can still report frames that no longer exist.
