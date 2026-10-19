# Presentation Script: Java Exception Handling

**Length:** about 30 minutes plus questions. **Format:** IntelliJ IDEA screenshare plus a terminal for curl.

Text in *"quotes"* is what you say. Adapt the wording so it sounds like you, but keep the technical terms. They are what your senior is listening for.

---

## 0. Before you start (5 minutes before the call)

- [ ] `mvn clean test` passes (33 tests, BUILD SUCCESS).
- [ ] No other app on port 8080 (stop any `java -jar` you ran earlier).
- [ ] In IntelliJ: **View → Appearance → Presentation Mode** off, but increase the font: **Settings → Editor → Font → 18**.
- [ ] Remove all old breakpoints: `Ctrl+Shift+F8` → select all → `−`.
- [ ] Open a terminal (IntelliJ's terminal, Git Bash) and paste these so you can press ↑ to reuse them:
  ```bash
  curl -i -X POST http://localhost:8080/api/accounts/ACC-1002/withdraw -H "Content-Type: application/json" -d '{"amount":250.00}'
  curl -i http://localhost:8080/api/accounts/ACC-9999
  curl -i http://localhost:8080/api/demo/checked
  ```
- [ ] Have `README.md` open in a tab with the Mermaid diagram preview showing (IntelliJ renders Mermaid in Markdown preview; enable it in Settings → Languages & Frameworks → Markdown if asked).

## Timeline

| # | Section | Minutes | Running total |
|---|---|---|---|
| 1 | Exception hierarchy | 3 | 3 |
| 2 | Checked vs unchecked | 4 | 7 |
| 3 | try / catch / finally | 4 | 11 |
| 4 | throw vs throws | 2 | 13 |
| 5 | Custom exceptions | 3 | 16 |
| 6 | Spring Boot REST error handling | 4 | 20 |
| 7 | Exception propagation | 2 | 22 |
| 8 | Live debugging | 6 | 28 |
| 9 | Automated tests | 3 | 31 |
| 10 | Best practices and wrap-up | 2 | 33 |

If you are running short of time, skip §3's `MultipleCatchDemo` and §9's controller test. Never skip §8.

---

## Opening (30 seconds)

*"Thanks for making the time. I've built a small banking and payments app to show how I understand Java exceptions: what they are, how they move through the code, how we handle them in a Spring Boot REST API, and how I debug them. Everything runs: there are 33 tests, and every example I show has a test proving it behaves the way I describe. I'll go from the basics to the Spring part and finish with live debugging."*

**File to open:** `README.md`. Scroll to the **project structure** block.

*"Quick tour: the `controller`, `service`, `exception` and `handler` packages are production-style code. The `examples` package is demonstration-only. Those are plain `main` methods that break things on purpose, and I've labelled them that way so nobody mistakes them for patterns to copy."*

---

## 1. The exception hierarchy (3 min)

**File to open:** `examples/ExceptionHierarchyDemo.java`

**Code to highlight:** `printHierarchy` and `classify` (lines ~45–64), then run `main`.

**What I should say:**

*"An exception in Java is just an object, an instance of a class that extends `Throwable`. When something goes wrong, the code creates one of these objects and throws it. That stops the normal flow, and Java looks for code that can handle it."*

*"Under `Throwable` there are two branches. `Error` is for problems with the JVM itself, like running out of memory. `Exception` is for problems our application can reasonably deal with. Under `Exception` there's a special subclass called `RuntimeException`."*

*"Rather than draw this from memory, this class asks the JVM. `printHierarchy` walks `getSuperclass()` up to `Object`."*

▶ **Run** `main`.

**Expected output:**
```
ArithmeticException -> RuntimeException -> Exception -> Throwable         [unchecked]
FileNotFoundException -> IOException -> Exception -> Throwable            [checked]
StackOverflowError -> VirtualMachineError -> Error -> Throwable           [Error - unchecked, do not catch]
AccountNotFoundException -> RuntimeException -> Exception -> Throwable    [unchecked]
InsufficientFundsException -> Exception -> Throwable                      [checked]
```

*"The rule for checked vs unchecked is purely about the hierarchy: if a class extends `RuntimeException` or `Error`, it's unchecked. Everything else is checked. The last two lines are my own custom exceptions, and you can see I've deliberately put them on different branches. I'll explain why in a minute."*

**Analogy:** *"Think of `Throwable` as the incident-report form. `Error` is 'the building is on fire'. `Exception` is 'a customer's card was declined'. You handle the second at the till. You don't handle the first, you evacuate."*

**Now scroll to Part 2 of the output:**
```
processPayment: started
applyFee: started
calculateFee: dividing 30 by 0
Caught in main: java.lang.ArithmeticException: / by zero
    at ...calculateFee(ExceptionHierarchyDemo.java:80)
    at ...applyFee(ExceptionHierarchyDemo.java:73)
    at ...processPayment(ExceptionHierarchyDemo.java:67)
    at ...main(ExceptionHierarchyDemo.java:31)
Program continues normally after the catch block.
```

*"This shows two things. First, an exception **interrupts** execution: 'applyFee: finished' and 'processPayment: finished' never print. Second, it **propagates**: it was thrown three methods deep, nobody in between caught it, and it travelled up to `main`. The stack trace reads top-down from where it was thrown to where the call started."*

**Then open** `examples/UnhandledExceptionDemo.java` and run it.

**Expected output:**
```
Starting end-of-day interest run...
Exception in thread "main" java.lang.IllegalStateException: Interest rate has not been configured
	at ...currentInterestRate(UnhandledExceptionDemo.java:37)
	at ...calculateInterest(UnhandledExceptionDemo.java:32)
	at ...main(UnhandledExceptionDemo.java:27)
Process finished with exit code 1
```

*"And this is what happens when nobody catches it: it leaves `main`, the thread's default handler prints the stack trace, and the process exits with code 1. 'Interest run finished' never prints."*

**Likely senior questions:**
- *Q: What's the difference between Exception and RuntimeException?* A: `RuntimeException` is a subclass of `Exception`. The compiler forces you to handle or declare `Exception` subclasses (checked), but not `RuntimeException` subclasses (unchecked).
- *Q: Why shouldn't you normally catch Throwable?* A: It includes `Error`s like `OutOfMemoryError`. Catching them hides serious JVM problems, and the JVM may not be in a state where your recovery code can even run. It also catches things you never intended, like a `StackOverflowError` from a bug.

---

## 2. Checked vs unchecked (4 min)

### 2a. Unchecked

**File to open:** `examples/UncheckedExceptionExamples.java`

**Code to highlight:** `triggerNullPointer` (line 58), `triggerIllegalArgument` (line 120), `triggerIllegalState` / `PaymentCard.charge` (line 148).

**What I should say:**

*"These are six common unchecked exceptions. Each `trigger` method causes one, and each `safe` method shows the prevention. They're unchecked because they're usually a programming mistake or bad input, so the right fix is to prevent them, not to wrap everything in try/catch."*

Point at line 58: *"Here the customer has no email, so `email()` returns null, and calling `toLowerCase()` on null throws a `NullPointerException`. Since Java 14 the message tells you exactly which expression was null."*

Point at line 120: *"For `IllegalArgumentException` I used a genuinely invalid argument that still compiles: `setScale(2, 42)`. 42 isn't a valid rounding mode, and the JDK checks that and throws. Compare that with `IllegalStateException` down here: the card's argument is fine, but the card object isn't activated yet. So `IllegalArgument` means 'bad input' and `IllegalState` means 'right input, wrong time'."*

▶ **Run** `main`.

**Expected output (abridged):**
```
== NullPointerException ==
   caught: java.lang.NullPointerException: Cannot invoke "String.toLowerCase()" because the return value of "...Customer.email()" is null
   prevented -> no-email-on-file
== ArithmeticException ==
   caught: java.lang.ArithmeticException: / by zero
...
== IllegalArgumentException ==
   caught: java.lang.IllegalArgumentException: Invalid rounding mode
== IllegalStateException ==
   caught: java.lang.IllegalStateException: Card must be activated before it can be charged
```

*"`main` catches `RuntimeException` broadly, but only so all six run in one go. That's demo-only. I wouldn't write that in a service."*

### 2b. Checked

**File to open:** `examples/CheckedExceptionExamples.java`

**Code to highlight:** the `throws` clauses on `readPaymentFile`, `connectToCoreBankingDatabase` and `loadPaymentGateway`.

*"Checked exceptions represent failures outside our code's control: a file that isn't there, a database that's down, a class that isn't on the classpath. The compiler forces every caller to deal with them."*

🎬 **Live trick:** delete `throws SQLException` from `connectToCoreBankingDatabase` (line 79). IntelliJ underlines `getConnection` in red: *"Unhandled exception: java.sql.SQLException"*. Press `Ctrl+Z`.

*"That red line is the whole idea of checked exceptions: the compiler won't let me forget."*

*"The SQLException is real, by the way. I don't need a database: there's no JDBC driver for the URL `jdbc:corebank:`, so `DriverManager` throws 'No suitable driver found'."*

▶ **Run** `main`.

**Expected output:**
```
FileNotFoundException: payments\2026-10-19-batch.csv (The system cannot find the path specified)
IOException (AccessDeniedException): C:\Users\...\bank-export-...
SQLException: No suitable driver found for jdbc:corebank://db.internal:5432/accounts (SQLState=08001)
ClassNotFoundException: com.example.payments.LegacySwiftGateway
```
(On macOS/Linux the IOException subclass may differ, but it is still an `IOException`.)

### 2c. Errors

**File to open:** `examples/ErrorExamples.java`

*"Errors I only demonstrate in a controlled way. The `StackOverflowError` is a recursion bug: no base case. It only fills one thread's stack, which is released as it unwinds, so it's harmless. For `OutOfMemoryError` I don't actually fill the heap. I ask for an array of `Integer.MAX_VALUE` longs, which the JVM refuses straight away, before allocating anything. I catch them here just to print what happened. In real code you'd fix the cause instead: add the base case, fix the leak, or give the JVM more memory."*

▶ **Run.** Expected:
```
caught java.lang.StackOverflowError - recursion with no base case filled the thread stack (the stack trace keeps only the last 1024 frames)
fixed version: 1126.84
caught java.lang.OutOfMemoryError: Requested array size exceeds VM limit
```

**Likely senior questions:**
- *Q: Why are some exceptions checked?* A: Java's designers wanted failures that are foreseeable but outside the programmer's control (I/O, network, databases) to be impossible to ignore. Checked exceptions make the possibility part of the method's contract.
- *Q: Many people say checked exceptions were a mistake. What do you think?* A: They're noisy, they don't work well with lambdas, and they leak implementation details up through layers. That's why Spring translates `SQLException` into unchecked `DataAccessException`s. I use checked exceptions sparingly, only for an expected business outcome the immediate caller should decide on, like insufficient funds.

---

## 3. try / catch / finally (4 min)

### 3a. try/catch

**File to open:** `examples/TryCatchDemo.java`. **Highlight** lines 23–34.

*"Monthly loan repayment. The **try** block says 'attempt this'. If `monthlyRepayment` throws, Java skips the rest of the try (line 27 never runs) and checks each catch from top to bottom. It picks the **first** one whose type matches, meaning the exception is an instance of that type or a subclass. After the catch block finishes, execution carries on **after** the whole try/catch, at line 33. It doesn't go back into the try."*

▶ **Run.** Expected:
```
  try: calculating for 24 months
  try: finished
  after try/catch: execution continues here
Monthly repayment: 500
  try: calculating for 0 months
  catch: / by zero
  after try/catch: execution continues here
Cannot calculate a repayment over 0 months
main finished normally
```

### 3b. Multiple catch blocks

**File to open:** `examples/MultipleCatchDemo.java`. **Highlight** the three catch blocks (lines 42–49).

*"Parsing a payment instruction like 'ACC-1001:250.00' can fail in four different ways, and I want different messages. The order matters: `NumberFormatException` is a subclass of `IllegalArgumentException`, so it must come first. If I swap them…"*

🎬 Cut the `NumberFormatException` catch block and paste it below the `IllegalArgumentException` block. Red: *"Exception 'java.lang.NumberFormatException' has already been caught"*. `Ctrl+Z`.

*"The compiler stops me, because the specific block would be unreachable. The last one is a Java 7 multi-catch, for two unrelated exceptions with the same handling."*

▶ **Run.** Expected:
```
ACC-1001:250.00 -> ACCEPTED 250.00
ACC-1001:abc -> REJECTED: amount is not a number
ACC-1001:-5 -> REJECTED: amount must be positive
ACC-1001 -> REJECTED: malformed instruction (ArrayIndexOutOfBoundsException)
null -> REJECTED: malformed instruction (NullPointerException)
```

### 3c. finally

**File to open:** `examples/FinallyDemo.java`. **Highlight** `processPayment` (lines 44–56) and `statusWithReturnInTry` (59–66).

*"`finally` runs no matter how the try block ends: normally, by an exception, or even by a `return`. That makes it the place to release locks or write audit entries. In `statusWithReturnInTry`, the return value is computed first, then finally runs, then the method actually returns."*

*"There are exceptions to that: if `System.exit` is called, the JVM crashes or is killed, or the thread never leaves the try, then finally doesn't run. You can see that if I run with `--exit`."*

▶ **Run.** Expected:
```
-- successful payment --
  try: paid PAY-1
  finally: lock released for PAY-1
-- failed payment --
  finally: lock released for PAY-2
main caught: Payment PAY-2 has invalid amount -10
-- return inside try --
returned: COMPLETED
Audit log: [lock PAY-1, paid PAY-1, unlock PAY-1, lock PAY-2, unlock PAY-2, finally ran before return]
```

*"Notice that for PAY-2, finally ran **before** main caught the exception. finally runs while the exception is on its way up."*

(Optional: Run → Edit Configurations → Program arguments `--exit` → run → the "finally: you will never see this line" message does not print.)

### 3d. try-with-resources

**File to open:** `examples/TryWithResourcesDemo.java`. **Highlight** `sumStatement` (line 50) and `auditedTransfer` (line 84).

*"For anything that has to be closed (files, connections, streams), I use try-with-resources. Anything that implements `AutoCloseable` can go in the brackets, and Java calls `close()` automatically, even if there's an exception. With two resources, they close in reverse order."*

*"It's better than closing manually in finally for three reasons: you can't forget; you don't need null checks; and if both the body and `close()` throw, you keep the original exception and the close failure is attached as a **suppressed** exception, instead of the close failure hiding the real problem."*

▶ **Run.** Expected:
```
Total: 2496.80
  open database
  open audit-log
  body: moving money
  close audit-log
  close database
main caught: transfer failed in body
  suppressed: could not flush audit session database
```

**Likely senior questions:**
- *Q: Can you have try without catch?* A: Yes: `try/finally`, or try-with-resources with no catch. The exception still propagates; you're just guaranteeing cleanup.
- *Q: Can a finally block fail to execute?* A: Yes: `System.exit`/`Runtime.halt`, JVM crash or `kill -9`, infinite loop or deadlock in the try, or a daemon thread when the JVM exits.
- *Q: What happens if a catch block throws another exception?* A: The new exception propagates and the original one is lost, unless you pass it as the cause. Finally still runs. I show this in `ExceptionChainingDemo`.
- *Q: What if finally throws?* A: Its exception replaces whatever was propagating, so the original is lost. Another reason to prefer try-with-resources.

---

## 4. throw vs throws (2 min)

**File to open:** `examples/ThrowVsThrowsDemo.java`. **Highlight** line 42 (`throw new IllegalArgumentException(`) and line 52 (`throws IOException`).

*"`throw` is a **statement** inside a method body. It's me saying 'stop, here's an exception object'. It's followed by one object. `throws` is part of the **method signature**. It's a warning label: 'calling me might produce these exceptions and I'm not handling them'. It's followed by types."*

*"`validateCardPayment` uses `throw`: I check the business rule and raise the exception myself. `loadExchangeRates` uses `throws`: it doesn't throw anything itself. `FileReader` might, and I'm choosing to pass that up to my caller. `requireRatesFile` at the bottom does both: when you `throw` a checked exception, you also have to declare it."*

**Analogy:** *"`throw` is the referee blowing the whistle. `throws` is the sign at the stadium entrance saying matches may be stopped."*

▶ **Run.** Expected:
```
throw demo  -> Card payment of 5000.00 exceeds the single-payment limit of 2500.00
throws demo -> FileNotFoundException: rates\GBP-EUR.csv (The system cannot find the path specified)
```

**Likely senior question:** *Q: Do you need `throws` for unchecked exceptions?* A: No. You can add it as documentation, but the compiler ignores it. I document them with `@throws` in Javadoc instead, as in `TransactionValidator.requirePositiveAmount`.

---

## 5. Custom exceptions (3 min)

**Files to open:** `exception/InsufficientFundsException.java`, then `exception/AccountNotFoundException.java`, then `service/TransactionValidator.java`.

**Code to highlight:** `extends Exception` vs `extends RuntimeException`; the fields in `InsufficientFundsException`; the three `throw` statements in `TransactionValidator`.

*"I have three domain exceptions. The key decision was checked vs unchecked."*

*"`InsufficientFundsException` is **checked**. Not having enough money isn't a bug. It's a normal business outcome that any caller of `withdraw` or `transfer` should consciously decide how to handle: show a message, offer an overdraft, try a smaller amount. Making it checked means the compiler won't let anyone forget."*

*"`AccountNotFoundException` and `InvalidTransferException` are **unchecked**. They mean the client sent a bad id or a bad request. No layer in between can fix that, so forcing every method to declare them would just be noise. They go straight up to the global handler."*

*"The custom exceptions carry context. `InsufficientFundsException` has the account id, the balance and the requested amount. The message deliberately **doesn't** include the balance, because that message goes to the client. The balance is there for logging."*

*"Why custom exceptions at all? **Readability**: `catch (InsufficientFundsException e)` says exactly what went wrong, `catch (RuntimeException e)` doesn't. **Maintainability**: the global handler maps each type to a status in one place. **Error handling**: callers can react to specific business problems without parsing message strings."*

**File to open:** `service/AccountService.java`. **Highlight** `withdraw` (lines 91–97) and `transfer` (lines 103–113).

*"Here's where they're used. Notice the service doesn't catch anything. It declares `throws InsufficientFundsException` and lets it propagate. And in `transfer`, every check happens **before** any money moves, so a failed transfer can never leave one account debited and the other not credited. That's prevention: validating first instead of cleaning up after."*

*"One more thing: `getAccount` throws when the account is missing, but `findAccount` returns an `Optional`. I use exceptions only when 'not found' is genuinely an error for that caller, not for normal control flow."*

**Likely senior questions:**
- *Q: When should you create custom exceptions?* A: When a business rule is broken and callers or handlers need to tell that case apart from others, and when you need extra context fields. Not for every possible error. If `IllegalArgumentException` says it clearly enough, use that.
- *Q: Why not one `BankingException` with an error code?* A: That's a valid design for larger systems (a base class with subclasses is common). With three cases, separate types are simpler and let `@ExceptionHandler` map by type.

---

## 6. Spring Boot REST error handling (4 min)

### 6a. Global handling, Approach B

**File to open:** `controller/AccountController.java`. **Highlight** `withdraw` (lines 60–65).

*"The controller has no try/catch at all. It declares `throws InsufficientFundsException` because it's checked, and lets everything propagate. That keeps the controller focused on the happy path."*

**File to open:** `handler/GlobalExceptionHandler.java`. **Highlight** the class annotation, then each `@ExceptionHandler`.

*"`@RestControllerAdvice` makes this class apply to every controller. Each `@ExceptionHandler` method says 'when this exception type escapes a controller, call me'. When several handlers could match, Spring picks the closest type in the hierarchy. For example, `NumberFormatException` lands in the `IllegalArgumentException` handler because there's no closer one, and it never reaches the `Exception` catch-all."*

*"The mapping is: AccountNotFound → 404, InsufficientFunds → 409 Conflict, because the request is valid but conflicts with the account's current state. InvalidTransfer and IllegalArgument → 400, anything else → 500. I also added handlers for malformed JSON, unknown paths and wrong HTTP methods, so even framework errors use the same JSON shape."*

**Highlight** `handleUnexpected`: *"The catch-all logs the **full stack trace** on the server, but the client only gets a generic message. We never leak stack traces, class names or SQL to the client. As a second line of defence, `application.properties` turns off Spring's own stack-trace output."*

**File to open:** `dto/ApiError.java`. *"Every error has the same shape: timestamp, status, error, message, path. It's a Java 21 record, so it's immutable and Jackson serialises it automatically."*

**Start the app** (Debug ▶ on `ExceptionDemoApplication`, breakpoints muted) and run:

```bash
curl -i http://localhost:8080/api/accounts/ACC-9999
```
**Expected:**
```
HTTP/1.1 404
{"timestamp":"...","status":404,"error":"Not Found","message":"Account ACC-9999 was not found","path":"/api/accounts/ACC-9999"}
```
```bash
curl -i -X POST http://localhost:8080/api/transfers -H "Content-Type: application/json" -d '{"fromAccountId":"ACC-1001","toAccountId":"ACC-1001","amount":10}'
```
**Expected:** `HTTP/1.1 400` … `"message":"Cannot transfer money from account ACC-1001 to itself"`
```bash
curl -i http://localhost:8080/api/demo/unexpected
```
**Expected:** `HTTP/1.1 500` … `"message":"An unexpected error occurred. Please try again later."`. Point at the IntelliJ console, where the full NPE stack trace is logged.

### 6b. Local handling, Approach A

**File to open:** `controller/DemoController.java`. **Highlight** `checked` (lines 56–75).

*"For contrast, this endpoint catches locally. `loadStatement` throws a checked `IOException`. Here I decided that a missing statement file is a 404 with a specific message, and any other I/O failure is a 503, 'temporarily unavailable'. Again, the specific `FileNotFoundException` comes first. This is a decision only this endpoint can make, so local handling makes sense."*

```bash
curl -i http://localhost:8080/api/demo/checked
```
**Expected:** `HTTP/1.1 404` … `"message":"No statement is available for account ACC-9999"`

*"My policy: helpers and validators **throw**, the service **propagates**, the controller **propagates** by default, and the global handler **translates** and **logs**, once. I only catch locally when an endpoint needs different behaviour or can actually recover."*

**Likely senior questions:**
- *Q: What happens if a REST controller throws an exception?* A: It propagates out of the controller method to Spring's `DispatcherServlet`, which asks its `HandlerExceptionResolver`s to handle it. The `ExceptionHandlerExceptionResolver` finds the best-matching `@ExceptionHandler` in the controller itself or in a `@ControllerAdvice`. If nothing matches, Spring's defaults apply. Unknown exceptions become a 500 through Boot's `/error` page.
- *Q: How does @RestControllerAdvice work?* A: It's `@ControllerAdvice` plus `@ResponseBody`. Spring detects the bean at startup, registers its `@ExceptionHandler` methods, and applies them to all controllers (or a subset if you set `basePackages`/`assignableTypes`). Return values are written to the response body as JSON.
- *Q: @ExceptionHandler vs try/catch?* A: try/catch is local, explicit and repeated in every method that needs it. `@ExceptionHandler` is declarative and centralised: one mapping applies everywhere, controllers stay clean, and the responses are consistent. try/catch can recover and continue the method; an exception handler can only produce the error response.
- *Q: Java exception vs HTTP error status?* A: An exception is an object inside the JVM, meant for developers, with a type, stack trace and cause. An HTTP status is part of the network response, meant for clients, a number plus a safe body. The handler translates one into the other. The client never sees the Java exception.
- *Q: Advantages and disadvantages of local vs global?* A: See the table in README §6. Global gives consistency and no duplication but sits further from the code. Local is explicit and can recover, but it gets duplicated and inconsistent.

---

## 7. Exception propagation (2 min)

**File to open:** `README.md` → §8, Markdown preview, sequence diagram.

*"Here's the whole journey for an overdrawn withdrawal. Client → DispatcherServlet → controller → service → validator. The validator throws. The service has no catch, so its frame is popped and `account.debit` never runs. The controller has no catch, so its frame is popped too. The exception reaches the DispatcherServlet, which hands it to the resolver, which calls our handler, which returns a 409."*

*"This is **stack unwinding**. Each method call has a frame on the thread's stack with its local variables. When an exception is thrown, the JVM checks the current frame for a matching catch. If there isn't one, it runs any finally block, discards the frame and checks the caller. The methods that get unwound never finish. Their remaining statements are skipped and their local variables are gone. The exception object itself captured the stack trace when it was created, which is why the log still shows those frames."*

*"Let me prove it in the debugger."*

**Likely senior question:** *Q: What happens to the call stack when an exception propagates?* A: Frames are popped one by one from the top until a frame with a matching catch is found. `finally` blocks run as their frames unwind. If no frame matches, the thread terminates and the uncaught-exception handler prints the trace. In a web request, Spring catches it before that happens.

---

## 8. Live debugging (6 min) ⭐

Full details: `DEBUGGING_GUIDE.md` §5. Here is the short version to perform.

**Set breakpoints (unmute breakpoints first):**

| File | Line | Statement |
|---|---|---|
| `controller/AccountController.java` | 63 | `Account account = accountService.withdraw(id, request.amount());` |
| `service/AccountService.java` | 94 | `validator.ensureSufficientFunds(account, amount);` |
| `service/AccountService.java` | 95 | `account.debit(amount);` |
| `handler/GlobalExceptionHandler.java` | 41 | first line of `handleInsufficientFunds` |

**Run in the terminal:**
```bash
curl -i -X POST http://localhost:8080/api/accounts/ACC-1002/withdraw -H "Content-Type: application/json" -d '{"amount":250.00}'
```

**What to do and say:**

1. **Stops at AccountController:63.** *"The request is paused on a Tomcat thread, see `http-nio-8080-exec` in the Threads dropdown. curl is waiting. Variables: `id` is ACC-1002, and Spring has already converted the JSON into my `AmountRequest` record."* → `F9`.
2. **Stops at AccountService:94.** Expand `account`: *"Balance 100.00, amount 250.00."* `Alt+F8` → `account.getBalance().compareTo(amount)` → `-1`. *"So the next call will throw."*
3. `F7` (Step Into) → inside `ensureSufficientFunds`. `F8` → on the `throw` line (43). **Point at Frames:** *"Validator, service, controller: three of my methods on the stack, none of them finished. Below them are Spring's frames, down to `DispatcherServlet.doDispatch`."*
4. `F9`. *"Watch: I have a breakpoint on `account.debit` on line 95, and it's **skipped**."* The debugger stops in **GlobalExceptionHandler**. *"The service and controller frames are gone from the Frames panel. They were unwound. Now Spring's resolver has called my handler."* Expand `ex`: `balance = 100.00`, `requestedAmount = 250.00`.
5. `Alt+F8` → `ex.getStackTrace()[0]` → `TransactionValidator.ensureSufficientFunds(TransactionValidator.java:43)`. *"The exception still remembers where it was born."*
6. `F9`. Show curl: `HTTP/1.1 409`. Then `curl http://localhost:8080/api/accounts/ACC-1002/balance` → `100.00`. *"No money moved."*

**Line breakpoint vs exception breakpoint (1 minute):**

7. Remove all breakpoints. `Ctrl+Shift+F8` → `+` → **Java Exception Breakpoints** → `InsufficientFundsException` → tick **Caught exception**.
8. *"Now I don't tell IntelliJ where to stop, only what to stop on."* Send a **successful** withdrawal (`{"amount":10.00}`): no pause. Send the 250.00 one: it stops directly on `TransactionValidator:43`.
9. *"One gotcha: in Spring, if I'd only ticked 'Uncaught', this would never fire, because Spring's DispatcherServlet catches every exception from a controller. From the JVM's point of view it's always caught."*

**If a breakpoint doesn't fire** (say it calmly, it shows experience): *"Usually that's because I started with Run instead of Debug, breakpoints are muted, an old instance is still holding port 8080, or I changed code without restarting."* (Full checklist in DEBUGGING_GUIDE §11.)

**Likely senior questions:**
- *Q: Difference between Step Into and Step Over?* A: Step Over runs the whole line, including any method calls, and stops on the next line. Step Into goes inside the method being called. Step Out finishes the current method and returns to the caller.
- *Q: Why did F8 on the throw line jump somewhere unexpected?* A: Step Over after a throw moves to wherever the exception is caught, and in Spring that's inside framework code. That's why I use Resume (F9) with a breakpoint in the handler.

---

## 9. Automated tests (3 min)

**File to open:** `src/test/java/.../service/AccountServiceTest.java`. **Highlight** `withdrawMoreThanBalance` and `failedTransferIsAtomic`.

*"Plain JUnit 5, no Spring, so they run in milliseconds. `@BeforeEach` creates a fresh service, so the tests are independent and deterministic."*

*"`assertThrows` is the key tool for exception testing. It runs the lambda. If nothing is thrown, or the wrong type, the test fails. If the right type is thrown, it **returns** the exception, so I can also check the fields and message. Here I also assert the balance didn't change. Testing that the failure has no side effects matters as much as testing that it throws."*

*"Before `assertThrows` people wrote try/fail/catch, which is easy to get wrong. If you forget the `fail()`, the test passes even when nothing is thrown."*

**File to open:** `controller/AccountControllerTest.java`. *"This is a `@WebMvcTest`: only the web layer plus my global handler. The service is a Mockito mock (`@MockitoBean`, which replaces the deprecated `@MockBean`), so I can make it throw exactly the exception I want and check the handler maps it to the right status and JSON. One test checks that a 500 doesn't leak the internal message."*

**File to open:** `controller/BankingApiIntegrationTest.java`. *"This one has no mocks: real controller, service, validator and handler, through MockMvc. It proves the full propagation chain works."*

▶ Right-click `src/test/java` → **Run 'All Tests'**. **Expected:** 33 tests passed, green.

**Likely senior question:** *Q: Why not just test that the API returns an error?* A: I test at both levels. Unit tests pin down the business rule and exception type. Web tests pin down the HTTP contract. If one breaks, I know which layer is wrong.

---

## 10. Best practices and wrap-up (2 min)

*"To wrap up, the rules I followed:"*

1. *"Catch specific exceptions, at the level that can actually do something about them."*
2. *"Never swallow an exception. Every catch either responds meaningfully or logs, and logging happens once."*
3. *"Prevent rather than catch. Validate inputs, check state, use Optional for 'not found'."*
4. *"Don't use exceptions for normal control flow."*
5. *"Chain when you translate, so the root cause isn't lost."*
6. *"try-with-resources for anything closeable."*
7. *"Never expose stack traces to clients."*
8. *"Don't catch `Error` or `Throwable`."*

**Optional, if there's time:** open `examples/ExceptionChainingDemo.java` and run it. Point at `Caused by:` in the output. *"If a catch block throws a new exception without passing the cause, you lose the original. The second half of the output shows `Cause is null`."*

*"That's everything. Happy to take questions or dig into any part of the code."*

---

## 11. Senior question bank (with answers)

**1. What is the difference between Exception and RuntimeException?**
`RuntimeException` extends `Exception`. Subclasses of `Exception` that are *not* `RuntimeException`s are **checked**: the compiler requires catch-or-declare. `RuntimeException`s are **unchecked**. Use `RuntimeException` for programming errors and invalid input, and checked `Exception` for foreseeable conditions the caller must handle.

**2. Why are some exceptions checked?**
To make foreseeable failures outside the programmer's control (I/O, network, databases, reflection) part of a method's contract, so callers can't ignore them by accident.

**3. Can a finally block fail to execute?**
Yes: `System.exit()`/`Runtime.halt()`, JVM crash or the process being killed, power loss, an infinite loop or deadlock inside try/catch, or a daemon thread when the JVM shuts down. A finally block can also *start* but not *complete* if it throws itself.

**4. Can you have try without catch?**
Yes. `try { } finally { }` and try-with-resources without catch are both legal. The exception still propagates after cleanup.

**5. What happens if a catch block throws another exception?**
The new exception propagates from the catch. Remaining catch blocks of the same try are not considered. `finally` still runs. The original is lost unless you chain it: `throw new X("...", e)`.

**6. What happens if you catch Exception instead of a specific exception?**
You also catch every unchecked exception, including bugs like `NullPointerException`, and treat them all the same. That hides bugs, makes the code's intent unclear and can lead to wrong recovery. It's acceptable only at a top-level boundary that logs and returns a generic failure, like our 500 handler.

**7. Why shouldn't you normally catch Throwable?**
It includes `Error`s (`OutOfMemoryError`, `StackOverflowError`, `LinkageError`) that indicate the JVM is in trouble. Recovery code may fail or make things worse, and catching them hides serious problems.

**8. What is exception chaining?**
Wrapping a lower-level exception as the **cause** of a higher-level one (`new PaymentProcessingException(msg, ioException)`). Callers get a meaningful domain exception, and the stack trace shows `Caused by:` with the original. Read it with `getCause()`.

**9. When should you create custom exceptions?**
When a domain rule is broken and callers or handlers need to distinguish that case (`InsufficientFundsException` → 409), or you need extra context fields. Don't create one when a standard exception (`IllegalArgumentException`, `IllegalStateException`) already describes it clearly.

**10. What is the difference between throw and throws?**
`throw` is a statement that throws one exception object now. `throws` is a method-signature clause listing exception types that may propagate to the caller.

**11. What happens if a REST controller throws an exception?**
It propagates to `DispatcherServlet`, which consults its `HandlerExceptionResolver`s. `ExceptionHandlerExceptionResolver` calls the best-matching `@ExceptionHandler`, local to the controller first, then `@ControllerAdvice`. `ResponseStatusExceptionResolver` and `DefaultHandlerExceptionResolver` handle `@ResponseStatus` and standard Spring exceptions. If none resolves it, the servlet container forwards to Spring Boot's `/error`, which returns a 500.

**12. How does @RestControllerAdvice work?**
It's a `@Component` combining `@ControllerAdvice` and `@ResponseBody`. At startup Spring finds it and indexes its `@ExceptionHandler` methods by exception type. When a controller throws, the closest matching type in the class hierarchy wins. The method's return value (`ResponseEntity<ApiError>`) becomes the response.

**13. What is the difference between @ExceptionHandler and try/catch?**
try/catch is imperative and local to one method; it can recover and let the method continue. `@ExceptionHandler` is declarative and central; it only produces the error response after the controller method has been abandoned, but it applies consistently to every endpoint.

**14. What is the difference between a Java exception and an HTTP error status?**
A Java exception is an in-process object for developers (type, message, stack trace, cause). An HTTP status is part of the network response for clients. The application decides how to translate one into the other. Different exceptions can map to the same status, and the client never sees the Java object.

**15. What happens to the call stack when an exception propagates?**
The JVM searches the current frame for a matching catch. If there is none, it runs the frame's finally, pops the frame and repeats in the caller. Unwound methods never complete and their locals are discarded. If the stack empties, the thread dies and the uncaught-exception handler prints the stack trace that was captured when the exception was constructed.

**Bonus questions you might get**

- **Why BigDecimal and not double?** `double` is binary floating point and can't represent 0.1 exactly. Money needs exact decimal arithmetic. Compare with `compareTo`, because `equals` treats `1.0` and `1.00` as different.
- **Why 409 for insufficient funds and not 400?** The request is well-formed and valid; it conflicts with the current state of the account. A later identical request might succeed. Some teams use 422. The important thing is to be consistent.
- **Is `synchronized` enough for concurrency?** For this single-instance, in-memory demo, yes: it makes the check and the debit atomic. With a database or multiple instances you'd use transactions and row locking or optimistic locking.
- **Why does your `IllegalArgumentException` handler return the message?** Our own validation messages are safe and helpful. The caveat is that framework code can throw `IllegalArgumentException` too. In a larger system I'd introduce a dedicated `ValidationException` and make the generic handler return a fixed message.
- **What does `@Transactional` do with checked exceptions?** By default it rolls back only on unchecked exceptions and Errors. For a checked exception like `InsufficientFundsException`, you'd need `rollbackFor`. Here validation happens before any change, so there's nothing to roll back.
