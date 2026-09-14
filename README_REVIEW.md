# Code Review

Kód se zkompiluje, testy procházejí a aplikaci je možné spustit pomocí docker compose tak, jak je popsáno v README. Při
testování endpointu jsem zjistil, že nové klienty se mi sice podařilo v Raynetu vytvořit, ale při změně title se už
název klienta v Raynetu neaktualizoval. Navíc jsem si hned všiml, že u nového klienta se v Raynetu neuložil ani telefon,
ani email. Při procházení kódu a dalším testování jsem zjistil, že když do jednoho CSV souboru vložím dva klienty se
stejným regNumber, nastane v aplikaci chyba. Chyba nastane také tehdy, když nevyplním regNumber nebo title. Emaily po
dokončení akce se posílají.

## Nalezené chyby

Chyby jsem rozdělil do kategorií Critical, High, Medium a Low, přičemž i v rámci jednotlivých kategorií jsem se je
snažil řadit podle priority (nahoře nejprioritnější). Některé nalezené problémy by však bylo možné vyřešit i najednou v
rámci určité změny designu stávající aplikace.

### Critical

Functional – Nefunguje update již existujících klientů uložených v lokální DB tabulce Client. Chyba je v metodě
ClientServiceImpl.updateClient a testy ji neodhalily kvůli slabým assertům (viz níže).

Security – I když to specifikace přímo neuvádí, aplikace by rozhodně měla řešit zabezpečení endpointu /uploadData,
protože ten přímo modifikuje data v produkčním systému. Jelikož má aplikace sloužit k machine-to-machine komunikaci,
bylo by vhodné zvážit zabezpečení například pomocí OAuth Client Credentials flow nebo pomocí nějakého sdíleného secretu
mezi aplikací a volajícím.

Functional – Aplikace přijímá CSV soubor s poli regNumber, title, email a phone. Email a phone se však pouze uloží do
databáze a nikdy se nepropíšou do Raynetu. Přitom podle dokumentace endpoint pro vytvoření klienta akceptuje "
addresses[].contactInfo (email, tel1)" pro nastavení těchto informací u klienta. Pro update existujícího klienta by bylo
nutné využít endpoint "POST /company/{companyId}/address/{addressId}/" (pro edit, vytvoření ma svůj endpoint).

Architecture / Design – Nad třídou ClientImporterCSVImpl je umístěna anotace Transactional, takže upload celého souboru
včetně API volání do Raynetu probíhá v jedné transakci. Tím pádem je nejenže zbytečně dlouho otevřená jedna velká
transakce, ale navíc pokud uprostřed zpracování souboru nastane chyba způsobující transaction rollback, do DB se nic
necommitne, přestože volání do Raynetu už proběhla. Zároveň by se anotace Transactional dala odebrat i z
RCRMServiceImpl. Jako opravu bych navrhoval design aplikace změnit a rozdělit ho na část, která vykonává práci uvnitř
aplikace, a na upsert do Raynetu, který by řešil jediný writer. Tím bychom se vyhnuli concurrency problémům (několika,
které tu uvádím). Tento design můžeme probrat detailněji na pohovoru.

### High

Concurrency – Podle dokumentace Raynetu smí jeden klient držet maximálně 4 současně aktivní spojení s Raynet API.
Jelikož upload probíhá asynchronně a velikost thread poolu není nijak explicitně omezena, použije se defaultní nastavení
ThreadPoolTaskExecutor, které dovolí až 8 souběžných upload operací. Navíc je třeba počítat s tím, že jedno připojení
potřebuje i cron job ve chvíli, kdy běží. Jako fix by bylo vhodné vytvořit @Bean ThreadPoolTaskExecutor a nastavit jeho
pool size na maximálně 3, aby neprobíhalo více uploadů zároveň.

Concurrency – Třída RCRMServiceImpl si drží čítač rateLimitCount, kterým hlídá, zda se již nevyčerpal denní limit 24k
API volání. Úprava tohoto čítače však není atomická a limit se kontroluje před vykonáním API volání, ale upravuje se až
po jeho dokončení. Jako potenciální řešení vidím, aby byl čítač typu AtomicInteger a dekrement se prováděl ještě před
voláním API (pokud nějaké volání zbývá). Čítač by se resetoval každou půlnoc scheduled jobem.

Concurrency / Robustness – Pokud by aplikace spadla ve chvíli, kdy běží cron job ClientUpsertTask, nedošlo by k uvolnění
zámku a po restartu aplikace by se už žádný další job nespustil. Řešením je nastavit pro lock nějakou expirační dobu (
buď pomocí anotace, nebo povolit získání locku v případě, že záznam v tabulce cron_lock má locked_at nižší než expirační
threshold).

Functional – Email se vždy odešle po zpracování celého CSV souboru se zprávou "Successfully processed all clients
imports.", a to bez ohledu na to, zda se opravdu vše podařilo, jen část, nebo nic. To může být velmi zavádějící, pokud
by se pak uživatel podíval do Raynet UI a některé importované klienty tam neviděl.

Architecture / Design – Měla by vzniknout samostatná třída fungující jako klient pro komunikaci s Raynet API. V
současném designu se o vše stará service třída RCRMServiceImpl.

Tests – Slabé pokrytí aplikace testy. Aplikace nemá dokonce ani žádné integrační testy. Navíc jsou stávající unit testy
slabé a při assertování používají any(SomeClass.class) místo očekávané hodnoty. Právě toto slabé assertování způsobilo,
že se neodhalila chyba při aktualizaci existujícího klienta.

Functional – regNumber a title se při validaci kontrolují jen na maximální délku, ale nekontroluje se, zda jsou vůbec
vyplněny. Když nevyplníme regNumber, dostaneme Internal Server Error se zprávou "Záznam nelze upravit. Jedná se o
vlastní firmu.". Při nevyplnění title nastává v aplikaci rovněž chyba.

### Medium

Functional / Robustness - V RCRMServiceImpl se jednou používá url + "/company/" a jindy url + "company/". Tato
nekonzistence sice zjevně nezpusobuje chybu (ověřeno testováním) a web server si s tím poradí při parsování, ale není
dobré na to spoléhat a mělo by to být sjednoceno.

Concurrency / Naming – Cron job používá metodu acquireLock(), která má zavádějící název, protože ve skutečnosti žádný
lock nezískává, pouze čte, zda existuje záznam v tabulce cron_lock. Kontrola existence a vložení nejsou atomické, takže
by teoreticky mohly lock získat dva joby souběžně. Vzhledem k tomu, že job běží jednou za hodinu, to však není
pravděpodobné.

Architecture / Design – Validace souboru je umístěna v ClientCsvBean. Kdybychom chtěli přidat další podporovaný formát
pro import, nemohli bychom tyto validace znovu využít. Validace by měly ležet ve zvláštní třídě.

Architecture / Design – JPA entita Client se používá přímo pro sestavování requestů do Raynet API. Bylo by lepší mít
samostatnou entitu pro request model a mapper, který by prováděl mapping "RaynetCompanyRequest toRequest(Client
client)".

Functional – Client.hashCode a CronLock.hashCode používají id.intValue(), což může způsobit NullPointerException, pokud
těmto JPA entitám ještě nebylo při uložení do databáze přiřazeno ID.

### Low

Functional – Validace telefonního čísla je příliš přísná. Mohla by akceptovat i další validní formáty, například "+420
553 401 547".

Architecture – ClientUpsertTask má "@Scheduled(cron = "0 0 * * * *")" nakonfigurováno přímo v kódu. Bylo by lepší
umožnit konfiguraci tohoto času z property souboru.

Tests – ClientServiceImplTest by mohl pro opakující se stringy použít konstanty a mohl by si zavést nějakou private
helper metodu pro vytváření ClientCsvBean.

Tests – Metoda testProcessCsv_InvalidCsvData by se měla spíše jmenovat testProcessCsv_PartlyInvalidCsvData.

Tests – Měla by se sjednotit konvence pro pojmenování testů. Někde se používá testX_Scenario, jinde testCamelCase nebo
jen testX.

Formatting – Třída RCRMServiceImpl není správně naformátovaná.

Code quality – RCRMServiceImpl obsahuje několik IDE warningů: konstanta RATE_LIMIT_TRESHOLD může být final, Unchecked
assignment, Unchecked cast atd.

## Opravené chyby

1. Oprava funkcionality pro update klienta – jedná se o základní a zásadní funkcionalitu aplikace. Bez této opravy má
   aplikace velmi omezené použití a umožňuje pouze vytvářet nové klienty. PR se
   změnami: https://github.com/ondraholis/raynet-csv-importer/pull/1
2. Přidání autentizace pro endpoint /uploadData – aplikace zabezpečení endpointu vůbec neřešila a kdokoli přes něj mohl
   posílat data do produkční instance Raynetu, což představuje velké bezpečnostní riziko. PR se
   změnami: https://github.com/ondraholis/raynet-csv-importer/pull/2

## Neopravené chyby

Zbylé chyby, které jsem identifikoval výše, jsem neopravoval, protože jsem je považoval za o něco méně prioritní než dvě
zmíněné chyby, které jsem si k opravě vybral. Kdybych měl v opravách pokračovat, opravil bych nejdříve funkcionalitu
tak, aby upsert klienta zahrnoval i email a telefon klienta. Poté bych pokračoval změnou designu aplikace, která by
vyřešila několik concurrency issues – oddělil bych akce vykonávané v rámci aplikace od zápisu do Raynetu a pro zápis do
Raynetu bych navrhoval mít jen jediného writera, protože Raynet API má stejně limity na počet současných připojení.

## Časové rozvržení prací

1. 40 minut – spuštění aplikace včetně vytvoření účtu v Raynetu a SendGridu + trocha troubleshootingu při prvním
   spuštění.
2. 1 hod 30 min – review kódu, u kterého jsem si dělal poznámky k problémům, na které jsem narazil, + testování
   endpointu na složitější edge cases.
3. 1 hod – nechal jsem AI udělat review jednotlivých částí: funkční chyby, bezpečnost, konkurence, výkon, testovací
   pokrytí a architektura, a jeho výstup jsem si procházel.
4. 1 hod 30 min – oprava dvou vybraných chyb plus otestování aplikace po každé opravě.
5. 1 hod 45 min – psaní reportu a vyznačení, co ze zadané specifikace aplikace splňuje a co ne.

Celkem 6 hod a 15 minut.

## Použití AI

AI jsem použil k review kódu aplikace, kterou jsem dostal. Nechal jsem ho udělat review jednotlivých částí – funkční
chyby, bezpečnost, konkurence, výkon, testovací pokrytí a architektura – a chtěl jsem, aby mi identifikované chyby
rozdělil do kategorií podle priority (Critical, High, Medium, Low). Na základě jeho výstupu jsem si kód prošel znovu a
všiml si ještě dalších věcí navíc. Na základě toho všeho jsem pak sepsal jednotlivé nalezené chyby s krátkým komentářem
do reportu, ve kterém jsem chyby seřadil podle priority. AI jsem následně nechal, aby mi uhladil test výsledného reportu
a zkontroloval, zda jsem na něco nezapomněl.

AI mi pomáhalo i při implementaci, ať už při brainstormingu před zahájením implementace, při kódování, psaní testů, nebo
při review úprav. Vždy jsem ale řekl, co a jak chci v dalším kroku udělat. Jeho úpravy jsem zkontroloval a případně
poupravil.